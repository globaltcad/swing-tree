package examples.sequencer;

import com.formdev.flatlaf.FlatDarkLaf;
import examples.sequencer.SequencerViewModel.Track;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.style.ComponentStyleDelegate;
import swingtree.threading.EventProcessor;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.util.function.Function;

import static swingtree.UI.*;

/**
 *  <b>PULSE — a neumorphic step sequencer.</b>
 *  <p>
 *  A little drum machine whose entire visual identity is carved out of light and
 *  shadow: there are almost no borders and no flat fills anywhere — every control
 *  is the <i>same</i> slate colour as the surface it sits on, and its shape is
 *  implied purely by a soft highlight on the top-left and a soft shadow on the
 *  bottom-right. This is <i>Neumorphism</i> ("soft UI"), and SwingTree's
 *  {@link ComponentStyleDelegate#shadow(String, swingtree.api.Configurator) named-shadow}
 *  API is what makes it possible on top of plain Swing.
 *  <p>
 *  The single most expressive trick here is the <b>step pad</b>: an inactive pad
 *  is <i>raised</i> (its shadows go outward), and the instant you toggle it on it
 *  becomes <i>pressed in</i> (the very same shadows flip to {@code isInset}) and
 *  lights up in its track's colour. Sixty-four of them invert and glow purely
 *  through shading — no images, no icons.
 *  <p>
 *  Three SwingTree ideas carry the app, each worth focusing on while reading:
 *  <ol>
 *    <li><b>Pure-data MVI.</b> {@link SequencerViewModel} is one immutable value
 *        holding the whole groove; the view only zooms lenses into it and binds
 *        widgets. A toggled pad is a new {@code ValueSet}, hence a new
 *        {@code Track}, hence a new view model.</li>
 *    <li><b>A modelled, re-armed playhead.</b> Exactly like the breathing demo,
 *        the moving playhead is a chain of one-step {@code Animatable}s. When the
 *        view model advances to a new step, {@link #armStep()} fires that step's
 *        drum voices and arms the next step — so the loop is tempo-accurate and
 *        reacts to a knob twist on the very next sixteenth.</li>
 *    <li><b>Reactive styling &amp; custom painting.</b> Pads restyle themselves
 *        from {@code withRepaintOn(..)} signals, and the rotary knobs draw their
 *        own value arc and pointer with a {@code painter(Layer.CONTENT, ..)}.</li>
 *  </ol>
 *  <p>
 *  The {@link DrumKit} is a tiny, Swing-free audio side-effect layer; if no audio
 *  device is present the sequencer simply runs as a silent, visual instrument.
 *  <p>
 *  Run {@link #main(String[])} to open the window.
 */
public final class SequencerView extends Panel {

    // ── The neumorphic palette: everything is built from these few slate tones ──
    private static final Color BASE      = new Color(46, 49, 58);    // the surface colour of *everything*
    private static final Color SHEEN_HI  = new Color(57, 61, 73);    // lit edge of the surface sheen (top-left)
    private static final Color SHEEN_LO  = new Color(38, 40, 48);    // shaded edge of the surface sheen (bottom-right)
    // Soft, *translucent* shadows are the secret to a believable soft-UI look: a
    // gentle white highlight up-left and a low-alpha (never near-black!) shadow
    // down-right. Keeping the dark side faint is what stops it looking harsh.
    private static final Color HI = new Color(255, 255, 255, 34);
    private static final Color LO = new Color(0, 0, 0, 80);
    private static final Color TEXT = new Color(202, 208, 220);
    private static final Color DIM  = new Color(120, 128, 148);

    private static final UI.Color ACCENT_PLAY = UI.Color.ofHsb(150, 0.55, 0.95); // mint
    private static final UI.Color ACCENT_COOL = UI.Color.ofHsb(205, 0.45, 0.92); // steel blue
    private static final UI.Color ACCENT_HOT  = UI.Color.ofHsb( 26, 0.80, 1.00); // amber down-beat

    private final Var<SequencerViewModel> vm;
    private final DrumKit kit = new DrumKit();

    // Lenses kept as fields: the re-arming onChange subscription on `currentStep`
    // is a *raw* subscription SwingTree never sees, so this lens must be strongly
    // held or it gets garbage-collected and the loop freezes (skill §9c).
    private final Var<Integer> currentStep;
    private final Var<Double>  stepProgress;
    private final Var<Boolean> playing;

    public SequencerView(Var<SequencerViewModel> vm) {
        this.vm           = vm;
        this.currentStep  = vm.zoomTo(SequencerViewModel::currentStep,  SequencerViewModel::withCurrentStep);
        this.stepProgress = vm.zoomTo(SequencerViewModel::stepProgress, SequencerViewModel::withStepProgress);
        this.playing      = vm.zoomTo(SequencerViewModel::playing,      SequencerViewModel::withPlaying);

        Var<Tuple<Track>> tracks = vm.zoomTo(SequencerViewModel::tracks, SequencerViewModel::withTracks);

        // The re-arming chain: every time the playhead lands on a new step (while
        // playing) we sound that step and schedule the next one.
        Viewable.cast(currentStep).onChange(From.VIEW_MODEL, it -> armStep());

        of(this).withLayout("fill, wrap 1, ins 0").withPrefSize(1000, 730)
        .withStyle(it -> it
            .backgroundColor(BASE)
            .gradient("ambient", g -> g
                .type(GradientType.RADIAL)
                .boundary(ComponentBoundary.OUTER_TO_EXTERIOR)
                .size(Math.max(it.componentWidth(), it.componentHeight()))
                .offset(it.componentWidth() * 0.30, it.componentHeight() * 0.12)
                .colors(new Color(58, 62, 74), new Color(33, 35, 43))
                .clipTo(ComponentArea.BODY)
            )
        )
        .add("growx", header())
        .add("growx", transport())
        .add("grow, push", console(tracks));
    }

    // ── Playback wiring ────────────────────────────────────────────────────────

    private void togglePlay() {
        if (vm.get().playing()) {
            vm.update(m -> m.withPlaying(false));      // pause: let the current step run out
        } else {
            vm.update(m -> m.withPlaying(true));
            armStep();                                  // kick the loop off on the current step
        }
    }

    private void stop() {
        vm.update(m -> m.withPlaying(false).withCurrentStep(0).withStepProgress(0));
    }

    /** Sounds the current step's active voices and schedules the next step. */
    private void armStep() {
        SequencerViewModel m = vm.get();
        if (!m.playing())
            return;
        int step = m.currentStep();
        for (Track t : m.tracks())
            if (!t.muted() && t.isActive(step))
                kit.trigger(t.voice(), t.volume() * m.masterVolume());
        UI.animate(vm, SequencerViewModel::stepAnimation);
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private UIForAnySwing<?, ?> header() {
        return panel("fill, ins 20 28 8 28", "[grow][]")
            .add("left",
                box("ins 0, wrap 1, gap 0")
                .add(label("P U L S E").withStyle(it -> it
                    .foregroundColor(TEXT)
                    .componentFont(f -> f.size(27).weight(2f).family("SansSerif").spacing(0.4f))
                ))
                .add(label("a neumorphic step sequencer  ·  tap the pads, twist the knobs, hit play")
                    .withStyle(it -> it.foregroundColor(DIM).componentFont(f -> f.size(12).family("SansSerif")))
                )
            )
            .add("right",
                label(kit.isAvailable() ? "♪  sound on" : "visual only")
                .withStyle(it -> it.foregroundColor(DIM).componentFont(f -> f.size(11).family("SansSerif")))
            );
    }

    // ── Transport: a raised strip of round buttons, knobs and a pulsing LED ────

    private UIForAnySwing<?, ?> transport() {
        Var<Double> bpm    = vm.zoomTo(m -> (double) m.bpm(), (m, d) -> m.withBpm((int) Math.round(d)));
        Var<Double> swing  = vm.zoomTo(SequencerViewModel::swing,        SequencerViewModel::withSwing);
        Var<Double> volume = vm.zoomTo(SequencerViewModel::masterVolume, SequencerViewModel::withMasterVolume);

        return panel("ins 0").withLayout("ins 18 26 18 26, gap 18, fillx", "[][][]22[][][]push[][]")
            .withStyle(it -> raised(it, 30, 7, 16))
            .add("aligny center", roundButton(playing.viewAsString(p -> p ? "❚❚" : "▶"), playing, ACCENT_PLAY, 82, this::togglePlay))
            .add("aligny center", roundButton(Var.of("■"), Var.of(false), ACCENT_COOL, 62, this::stop))
            .add("aligny center", roundButton(Var.of("✕"), Var.of(false), ACCENT_COOL, 62, () -> vm.update(SequencerViewModel::clearPattern)))
            .add(knob("TEMPO",  bpm,    60, 200, ACCENT_HOT,  d -> Math.round(d) + " bpm"))
            .add(knob("SWING",  swing,   0,   1, ACCENT_COOL, d -> Math.round(d * 100) + "%"))
            .add(knob("VOLUME", volume,  0,   1, ACCENT_PLAY, d -> Math.round(d * 100) + "%"))
            .add(beatLed())
            .add("gapleft 8",
                label(currentStep.viewAsString(s -> String.format("STEP %02d", s + 1)))
                .withStyle(it -> it.foregroundColor(DIM).componentFont(f -> f.size(12).weight(2f).family("Monospaced")))
            );
    }

    private UIForAnySwing<?, ?> roundButton(Val<String> glyph, Val<Boolean> lit, UI.Color accent, int size, Runnable onClick) {
        return button(glyph)
            .withPrefSize(size, size)
            .withRepaintOn(lit)
            .withCursor(Cursor.HAND)
            .withStyle(it -> {
                final double w = it.componentWidth(), h = it.componentHeight();
                boolean on = lit.get();
                it = raisedOrInset(it, 999, 7, 14, on);
                // A radial cap, brightest toward the top-left, domes the face so
                // the button reads as a rounded physical cap rather than a flat disc.
                it = it.gradient(Layer.CONTENT, "cap", g -> g
                    .type(GradientType.RADIAL).boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                    .offset(w * 0.34, h * 0.30).size(w * 0.85)
                    .colors(withAlpha(Color.WHITE, on ? 10 : 30), withAlpha(Color.BLACK, on ? 12 : 34))
                    .clipTo(ComponentArea.BODY));
                if (on) {
                    // The glow sits on the BORDER layer so it paints *over* the
                    // inset shadow (which lives on the CONTENT layer) — otherwise
                    // the pressed-in shadow would swallow the light.
                    it = it.gradient(Layer.BORDER, "lit", g -> g
                        .type(GradientType.RADIAL).boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(w / 2.0, h / 2.0).size(w * 0.72)
                        .colors(accent.blend(Color.WHITE, 0.4), withAlpha(accent, 70), withAlpha(accent, 0))
                        .clipTo(ComponentArea.BODY));
                    it = it.border(1.4, withAlpha(accent, 180));
                }
                return it
                    .foregroundColor(on ? Color.WHITE : TEXT)
                    .componentFont(f -> f.size(size > 70 ? 24 : 18).weight(2f).family("SansSerif").color(on ? Color.WHITE : TEXT));
            })
            .onClick(it -> onClick.run());
    }

    // ── A self-painting neumorphic rotary knob, adjusted by vertical drag ──────

    private UIForAnySwing<?, ?> knob(String caption, Var<Double> value, double min, double max, UI.Color accent, Function<Double, String> fmt) {
        final double[] dragStart = { 0 };
        final int dia = 84;
        final int margin = 15;
        return box().withLayout("wrap 1, ins 0, gap 3", "[center]")
            .add("center",
                box().withPrefSize(dia, dia)
                .withRepaintOn(value)
                .withCursor(Cursor.HAND)
                .withStyle(it -> {
                    final double w = it.componentWidth(), h = it.componentHeight();
                    final double cx = w / 2.0, cy = h / 2.0;
                    final double r  = Math.min(w, h) / 2.0;
                    final double frac = (value.get() - min) / (max - min);
                    final double ang  = Math.toRadians(135 + frac * 270);
                    // The groove/arc are kept well inside the body so the rounded
                    // BODY clip never bites into them (that was the "pixely" edge).
                    final double gr = r - margin - 5;
                    return raised(it, 999, 7, margin)
                        // a soft inner cap highlight, brightest toward the top-left
                        .gradient(Layer.CONTENT, "cap", g -> g
                            .type(GradientType.RADIAL).boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                            .offset(cx - r * 0.22, cy - r * 0.30).size(r * 1.35)
                            .colors(withAlpha(Color.WHITE, 26), withAlpha(Color.BLACK, 28))
                            .clipTo(ComponentArea.BODY))
                        .painter(Layer.CONTENT, ComponentArea.BODY, g -> {
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g.setColor(new Color(0, 0, 0, 75));                          // recessed groove
                            g.draw(new java.awt.geom.Arc2D.Double(cx - gr, cy - gr, 2 * gr, 2 * gr, 225, -270, java.awt.geom.Arc2D.OPEN));
                            g.setColor(accent.blend(Color.WHITE, 0.12));                 // value fill
                            g.draw(new java.awt.geom.Arc2D.Double(cx - gr, cy - gr, 2 * gr, 2 * gr, 225, -270 * frac, java.awt.geom.Arc2D.OPEN));
                            final double px = cx + Math.cos(ang) * gr;                   // pointer dot
                            final double py = cy + Math.sin(ang) * gr;
                            g.setColor(accent.blend(Color.WHITE, 0.5));
                            g.fill(new java.awt.geom.Ellipse2D.Double(px - 4.5, py - 4.5, 9, 9));
                        });
                })
                .onMousePress(e -> dragStart[0] = value.get())
                .onMouseDrag(e -> {
                    double delta = -e.deltaYSinceStart() / 150.0 * (max - min);
                    value.set(Math.max(min, Math.min(max, dragStart[0] + delta)));
                })
            )
            .add("center",
                label(value.viewAsString(fmt))
                .withStyle(it -> it.foregroundColor(TEXT).componentFont(f -> f.size(13).weight(2f).family("SansSerif")))
            )
            .add("center",
                label(caption)
                .withStyle(it -> it.foregroundColor(DIM).componentFont(f -> f.size(10).weight(2f).family("SansSerif").spacing(0.15f)))
            );
    }

    private UIForAnySwing<?, ?> beatLed() {
        return box().withPrefSize(30, 30)
            .withRepaintOn(stepProgress, currentStep, playing)
            .withStyle(it -> {
                final double w = it.componentWidth(), h = it.componentHeight();
                boolean play     = playing.get();
                boolean downbeat = currentStep.get() % 4 == 0;
                double  pulse    = play ? Math.max(0, 1 - stepProgress.get()) : 0;  // brightest at each step's onset
                UI.Color base    = downbeat ? ACCENT_HOT : ACCENT_COOL;
                UI.Color lit     = base.blend(Color.WHITE, 0.2 + 0.6 * pulse);
                final int alpha  = (int) (45 + 200 * pulse);
                return inset(it, 999, 4, 9)
                    .gradient(Layer.CONTENT, "led", g -> g
                        .type(GradientType.RADIAL).boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(w / 2.0, h / 2.0).size(w * 0.42)
                        .colors(withAlpha(lit, alpha), withAlpha(base, 0))
                        .clipTo(ComponentArea.BODY));
            });
    }

    // ── The console: a sunken tray holding one row per track ───────────────────

    private UIForAnySwing<?, ?> console(Var<Tuple<Track>> tracks) {
        return panel("fill, ins 14 18 18 18")
            .add("grow, push",
                panel("fill, wrap 1, ins 22, gap 12")
                .withStyle(it -> inset(it, 28, 8, 10))
                .addAll(tracks, this::trackRow)
            );
    }

    private UIForAnySwing<?, ?> trackRow(Var<Track> entry) {
        UI.Color accent    = UI.Color.ofHsb(entry.get().hue(), 0.62, 0.95);
        UI.Color accentLit = accent.blend(Color.WHITE, 0.30);
        Val<Boolean> muted = entry.viewAs(Boolean.class, Track::muted);

        return panel().withLayout("ins 0, gap 0", "[140!]12[grow]")
            .withStyle( it -> it.borderRadius(16))
            .add("growy", trackHeader(entry, accent, accentLit, muted))
            .add("growx",
                box("ins 0, gap 0")
                .apply(row -> {
                    for (int s = 0; s < SequencerViewModel.STEPS; s++) {
                        String gap = (s % 4 == 3 && s != SequencerViewModel.STEPS - 1) ? "gapright 14" : "";
                        row.add(gap, pad(entry, s, accent, accentLit));
                    }
                })
            );
    }

    private UIForAnySwing<?, ?> trackHeader(Var<Track> entry, UI.Color accent, UI.Color accentLit, Val<Boolean> muted) {
        return box().withRepaintOn(muted).withCursor(Cursor.HAND)
            .withLayout("fill, ins 0 16 0 14, gap 11", "[]12[grow]", "[center]")
            .withStyle(it -> raisedOrInset(it, 16, 5, 11, muted.get()))
            .add("aligny center",
                box().withPrefSize(16, 16).withStyle(it -> it
                    .borderRadius(999).margin(2)
                    .backgroundColor(muted.get() ? withAlpha(accent, 90) : accent)
                    .border(1, withAlpha(accentLit, muted.get() ? 80 : 180))
                )
            )
            .add("growx, aligny center",
                label(entry.get().name()).withRepaintOn(muted).withStyle(it -> it
                    .foregroundColor(muted.get() ? DIM : TEXT)
                    .componentFont(f -> f.size(15).weight(2f).family("SansSerif"))
                )
            )
            .onMouseClick(e -> entry.update(Track::toggleMuted));
    }

    private UIForAnySwing<?, ?> pad(Var<Track> entry, int step, UI.Color accent, UI.Color accentLit) {
        final boolean downbeat = step % 4 == 0;
        Val<Boolean> on   = entry.viewAs(Boolean.class, t -> t.isActive(step));
        Val<Boolean> head = currentStep.viewAs(Boolean.class, cs -> cs == step);

        return box().withPrefSize(44, 44)
            .withRepaintOn(on, head)
            .withCursor(Cursor.HAND)
            .withStyle(it -> {
                final double w = it.componentWidth(), h = it.componentHeight();
                final double cx = w / 2.0, cy = h / 2.0;
                boolean active = on.get();
                boolean atHead = head.get();
                it = raisedOrInset(it, 12, 3, 8, active);
                if (active) {
                    it = it.gradient(Layer.BACKGROUND, "fill", g -> g
                        .type(GradientType.RADIAL).boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(cx, cy).size(w * 0.62)
                        .colors(accentLit, withAlpha(accent, 55))
                        .clipTo(ComponentArea.BODY));
                    it = it.border(1, withAlpha(accent, 160));
                } else if (downbeat) {
                    it = it.border(1, withAlpha(Color.WHITE, 18));   // faint rim marks the down-beats
                }
                if (atHead) {
                    it = it.gradient(Layer.BORDER, "head", g -> g
                        .type(GradientType.RADIAL).boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(cx, cy).size(w * 0.95)
                        .colors(active ? withAlpha(Color.WHITE, 205) : withAlpha(accentLit, 130), withAlpha(accent, 0))
                        .clipTo(ComponentArea.BODY));
                    it = it.border(active ? 1.8 : 1.2, active ? Color.WHITE : withAlpha(accentLit, 175));
                }
                return it;
            })
            .onMouseClick(e -> entry.update(t -> t.toggle(step)));
    }

    // ── Neumorphic style helpers — the soul of the soft-UI look ────────────────
    //   A raised element casts a *soft, translucent* highlight up-left and an even
    //   fainter shadow down-right; the same two flipped to inset make it look
    //   pressed in. On top of the shadows we lay a gentle diagonal "sheen"
    //   gradient — that subtle light-to-dark across the face is what gives an
    //   element its rounded *volume* instead of looking like a flat sticker.
    //   Note the margin: an outward shadow lives *inside* the component bounds,
    //   so we reserve a margin ring wide enough that Swing does not clip it away.

    private static <C extends JComponent> ComponentStyleDelegate<C> neu(
        ComponentStyleDelegate<C> it, int radius, int depth, int margin, boolean inset
    ) {
        int brightOffset = depth;                                  // highlight throws further…
        int darkOffset   = (int) Math.round(depth * 0.55);         // …than the (smaller) shadow
        int blur         = (int) Math.round(depth * 1.9 + 2);      // generous, soft falloff
        int spread       = -(int) Math.round(depth * 0.55 + 1);    // negative → the shadow hugs the edge
        return it
            .backgroundColor(BASE)
            .borderRadius(radius)
            .margin(margin)
            .gradient("sheen", g -> g
                .type(GradientType.LINEAR)
                .span(inset ? Span.BOTTOM_RIGHT_TO_TOP_LEFT : Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .colors(SHEEN_HI, SHEEN_LO)
                .clipTo(ComponentArea.BODY)
            )
            .shadow("bright", s -> s.color(HI).offset(-brightOffset, -brightOffset).type(UI.ShadowType.PENUMBRA))
            .shadow("dark",   s -> s.color(LO).offset(darkOffset, darkOffset).type(UI.ShadowType.PENUMBRA))
            .shadowBlurRadius(blur)
            .shadowSpreadRadius(spread)
            .shadowIsInset(inset);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> raised(ComponentStyleDelegate<C> it, int radius, int depth, int margin) {
        return neu(it, radius, depth, margin, false);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> inset(ComponentStyleDelegate<C> it, int radius, int depth, int margin) {
        return neu(it, radius, depth, margin, true);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> raisedOrInset(ComponentStyleDelegate<C> it, int radius, int depth, int margin, boolean pressed) {
        return neu(it, radius, depth, margin, pressed);
    }

    private static Color withAlpha(Color c, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // ── main ───────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        Var<SequencerViewModel> vm = Var.of(new SequencerViewModel());
        UI.show("PULSE — a neumorphic step sequencer", frame -> new SequencerView(vm));
        EventProcessor.DECOUPLED.join();
    }
}
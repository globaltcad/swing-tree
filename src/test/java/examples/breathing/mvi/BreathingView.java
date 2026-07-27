package examples.breathing.mvi;
import java.util.Locale;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.layout.FlowCell;
import swingtree.threading.EventProcessor;

import java.awt.Color;

import static swingtree.UI.*;

/**
 *  <b>Tranquil — a Breathing Companion.</b>
 *  <p>
 *  A small, calming demo application whose <i>whole reason for existing</i> is
 *  to show off <b>SwingTree animations</b> inside a clean
 *  <i>Model-View-Lenses</i> (MVL) architecture. A glowing orb swells as you
 *  breathe in, rests, shrinks as you breathe out, and rests again — guiding the
 *  user through a configurable breathing exercise.
 *  <p>
 *  Three things are worth focusing on while reading this view:
 *  <ol>
 *      <li><b>The animation is modelled, not scripted.</b> The view never
 *          touches a {@code Timer}. It hands the pure
 *          {@link BreathingViewModel#breathAnimation()} {@code Animatable} to
 *          {@code UI.animate(..)} and lets the immutable view model describe
 *          every frame. See the {@code onChange} re-arming below — when one
 *          phase ends, the view simply animates the next.</li>
 *      <li><b>Property lenses everywhere.</b> {@code zoomTo(..)} and
 *          {@code viewAs(..)} focus the single root
 *          {@code Var<BreathingViewModel>} down to individual fields, each
 *          bi-directionally bound and firing change events only when its own
 *          slice actually changes.</li>
 *      <li><b>Reactive styling.</b> The orb is drawn entirely from view-model
 *          state through the property bound {@code withStyle(vm, (m, it) -> ..)},
 *          which hands the current view model item to the style lambda and
 *          repaints the orb automatically on every change.</li>
 *  </ol>
 *  <p>
 *  <b>Why is {@link #phase} a field?</b> Sprouts lenses and views observe
 *  their parent property only <i>weakly</i>, so an unreferenced lens may be
 *  garbage-collected and silently stop updating. SwingTree's own component
 *  bindings (e.g. {@code label(..)}, {@code slider(..)}, {@code withStyle(prop, ..)})
 *  keep a strong reference internally, so the lenses passed to them are safe.
 *  The {@link #phase} lens, however, is consumed by a <i>raw</i>
 *  {@code Viewable.cast(phase).onChange(..)} subscription — SwingTree never
 *  sees it — so this view must hold the strong reference itself. Without it,
 *  the re-arming listener would be collected and the breathing session would
 *  freeze after the first phase.
 *  <p>
 *  Run {@link #main(String[])} to open the window.
 */
public final class BreathingView extends Panel {

    /**
     *  The orb's <i>preferred</i> box, and the size it falls back to once the
     *  layout stacks. On a wide window it grows well past this: it is added with
     *  a {@code grow} constraint and its gradients are sized relative to the box
     *  it ends up in.
     */
    private static final int ORB_BOX = 320;

    // ── The responsive span table — the whole "convergence" config ───────────
    //
    // A size category is a fraction of the grid's reference width, so these are
    // relative bands rather than pixel breakpoints: orb beside controls from
    // LARGE upwards, one stacked column below that.
    //
    // Both cards are MigLayout panels with a "fill" constraint, which the flow
    // layout detects on its own, so each stretches to the height of the taller
    // one while they share a row — that is what lets the orb grow.

    private static final int PAGE_REFERENCE_WIDTH = 900;

    private static final FlowCell ORB_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(7).veryLarge(8).oversize(8) );

    private static final FlowCell CONTROLS_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(5).veryLarge(4).oversize(4) );

    private final Var<BreathingViewModel> vm;

    /**
     *  The {@code phase} lens, kept alive for the lifetime of this view so that
     *  the re-arming {@code onChange} subscription below is not garbage-collected.
     */
    private final Var<BreathPhase> phase;

    public BreathingView( Var<BreathingViewModel> vm ) {
        this.vm    = vm;
        this.phase = vm.zoomTo(BreathingViewModel::phase, BreathingViewModel::withPhase);

        // ── Property lenses / views ──────────────────────────────────────────
        // These are safe as locals: each is handed to a SwingTree binding
        // (label / progressBar), which keeps a strong reference.
        Val<String> instruction   = vm.viewAsString(BreathingView::instructionFor);
        Val<String> cycleInfo     = vm.viewAsString(m -> "Cycle " + m.currentCycle() + " of " + m.settings().totalCycles());
        Val<Double> phaseProgress = vm.viewAsDouble(BreathingViewModel::phaseProgress);
        Val<Double> sessionDone   = vm.viewAsDouble(BreathingViewModel::sessionProgress);

        // ── The animation re-arming chain ────────────────────────────────────
        // Whenever the modelled animation advances the view model into a new
        // phase, this listener fires and animates the *next* phase. Four tiny
        // per-phase Animatables thus chain into one continuous, looping session
        // — and pausing is simply "stop re-arming".
        Viewable.cast(phase).onChange(From.VIEW_MODEL, it -> {
            if ( vm.get().running() )
                UI.animate(vm, BreathingViewModel::breathAnimation);
        });

        of(this).withLayout("fill, wrap 1, insets 0")
        .withPrefSize(1080, 660)
        .withStyle( it -> it
            .gradient("backdrop", g -> g
                .type(GradientType.RADIAL)
                .boundary(ComponentBoundary.OUTER_TO_EXTERIOR)
                .size(Math.max(it.componentWidth(), it.componentHeight()))
                .offset(it.componentWidth() * 0.5, it.componentHeight() * 0.32)
                .colors(new Color(30, 35, 62), new Color(8, 9, 20))
                .clipTo(ComponentArea.BODY)
            )
        )
        .add("growx, wmin 0", header())
        .add("grow, push, wmin 0", body(instruction, cycleInfo, phaseProgress, sessionDone));
    }

    // ── The page: a scrolling 12-column grid holding the two cards ───────────
    //
    // A flow grid gives every row the height of its tallest child, so once the
    // cards stack the page outgrows the window — hence the scroll pane.

    private UIForAnySwing<?,?> body(
        Val<String> instruction,
        Val<String> cycleInfo,
        Val<Double> phaseProgress,
        Val<Double> sessionDone
    ) {
        return scrollPane( conf -> conf.fitWidth(true) )
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withVerticalScrollIncrement(24)
            .withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)).borderWidth(0).padding(0) )
            .add(
                panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 0, 0)
                .withMinSize(0, 0) // a grid reports the SUM of its children's minimums
                .withPrefSize(PAGE_REFERENCE_WIDTH, 0) // the reference width, see above
                .withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
                .add(ORB_SPAN,      orbStage(instruction, cycleInfo, phaseProgress, sessionDone))
                .add(CONTROLS_SPAN, controlPanel())
            );
    }

    // ── Header ───────────────────────────────────────────────────────────────

    // Title and subtitle as two labels rather than one html(..) string: html
    // re-wraps instead of ellipsizing, so it just gets clipped when it runs out
    // of room.

    private static UIForAnySwing<?,?> header() {
        return panel("fill, insets 16 26 16 26, gap 0")
            .withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 95)) )
            .add("split 2",
                label("Tranquil").withStyle( it -> it
                    .foregroundColor(new Color(0xF3, 0xF4, 0xFF))
                    .componentFont( f -> f.size(21).family("Serif") )
                )
            )
            .add("pushx, growx, wmin 0",
                label("  —  a breathing companion, animated with SwingTree")
                .withStyle( it -> it
                    .foregroundColor(new Color(154, 166, 200))
                    .componentFont( f -> f.size(12).family("SansSerif").posture(0.15f) )
                )
            );
    }

    // ── The orb stage: instruction, the breathing orb, and progress ──────────

    private UIForAnySwing<?,?> orbStage(
        Val<String> instruction,
        Val<String> cycleInfo,
        Val<Double> phaseProgress,
        Val<Double> sessionDone
    ) {
        // Every row may shrink below the width of its own text, so the stage can
        // follow the grid down to a phone. The orb is added with "grow" so that it
        // expands into whatever room the row leaves it — see `breathingOrb()`.
        return panel("fill, wrap 1, insets 20").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
            .add("center, gapbottom 6, wmin 0",
                label(instruction)
                .withStyle( it -> it
                    .foregroundColor(new Color(232, 236, 252))
                    .componentFont( f -> f.size(24).family("Serif").posture(0.08f) )
                )
            )
            .add("center, grow, push, wmin 0",
                breathingOrb()
            )
            .add("center, gaptop 4, wmin 0",
                label(cycleInfo)
                .withStyle( it -> it
                    .foregroundColor(new Color(150, 162, 198))
                    .componentFont( f -> f.size(13).family("SansSerif") )
                )
            )
            .add("center, growx, width 160::360, wmin 0, gaptop 8",
                progressBar(Align.HORIZONTAL, phaseProgress)
            )
            .add("center, growx, width 160::360, wmin 0, gaptop 2",
                label(sessionDone.viewAsString( d -> "Session " + Math.round(d * 100) + "% complete" ))
                .withStyle( it -> it
                    .foregroundColor(new Color(120, 132, 168))
                    .componentFont( f -> f.size(11).family("SansSerif") )
                )
            );
    }

    // ── The breathing orb itself — pure reactive styling ─────────────────────

    private UIForAnySwing<?,?> breathingOrb() {
        return box().withPrefSize(ORB_BOX, ORB_BOX)
            // The orb is styled straight from the view model item, which also
            // repaints it automatically on every change — including the animated
            // scale and phase progress driving the breathing motion.
            .withStyle( vm, (m, it) -> {
                double  s      = m.orbScale();
                double  cx     = it.componentWidth()  * 0.5;
                double  cy     = it.componentHeight() * 0.5;
                // Sized relative to the box rather than in absolute pixels, so the
                // orb stays a circle that fits whether it is stretched across a wide
                // window or squeezed onto a phone. Every ring is capped at `reach`,
                // the largest circle the box can hold: past that `clipTo(BODY)` cuts
                // it off and it reads as the rectangle it was clipped to.
                double  unit   = Math.min(it.componentWidth(), it.componentHeight());
                double  reach  = unit * 0.5;
                double  radius = Math.min(unit * (0.207 + 0.293 * s), reach);
                // The glow colour smoothly morphs towards the next phase's
                // colour as the current phase progresses.
                UI.Color glow  = phaseColor(m.phase()).blend(phaseColor(m.phase().next()), m.phaseProgress());

                int     remaining = (int) Math.ceil(m.settings().secondsFor(m.phase()) * (1 - m.phaseProgress()));
                String  orbText   = !m.running()
                                        ? ( m.isSessionComplete() && m.cyclesCompleted() > 0 ? "✓" : "❀" )
                                        : String.valueOf(Math.max(1, remaining));

                return it
                    // The breathing disc — a radial gradient that grows and shrinks.
                    .gradient(Layer.BACKGROUND, "core", g -> g
                        .type(GradientType.RADIAL)
                        .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(cx, cy)
                        .size(radius)
                        .colors(
                            glow.blend(Color.WHITE, 0.62),
                            glow,
                            withAlpha(glow, 0)
                        )
                        .clipTo(ComponentArea.BODY)
                    )
                    // A wide, faint halo that breathes along with the disc.
                    .gradient(Layer.BORDER, "halo", g -> g
                        .type(GradientType.RADIAL)
                        .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(cx, cy)
                        .size(Math.min(radius * 1.6, reach))
                        .colors(
                            withAlpha(glow, 20 + (int) (60 * s)),
                            withAlpha(glow, 0)
                        )
                        .clipTo(ComponentArea.BODY)
                    )
                    // A soft outer bloom whose reach tracks the breath. Not a
                    // shadow: a shadow is painted around the component's *rectangle*,
                    // which shows as an outline once the box stops being square.
                    // A radial gradient blooms in a circle whatever the box shape.
                    .gradient(Layer.BORDER, "bloom", g -> g
                        .type(GradientType.RADIAL)
                        .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(cx, cy)
                        .size(Math.min(radius * 2.2, reach))
                        .colors(
                            withAlpha(glow, 14 + (int) (30 * s)),
                            withAlpha(glow, 0)
                        )
                        .clipTo(ComponentArea.BODY)
                    )
                    // The countdown number, sized to the breath.
                    .text( t -> t
                        .content(orbText)
                        .placement(Placement.CENTER)
                        .clipTo(ComponentArea.BODY)
                        .font( f -> f
                            .size((int) (unit * (0.083 + 0.074 * s)))
                            .weight(2)
                            .color(new Color(255, 255, 255, m.running() ? 235 : 150))
                            .family("Serif")
                        )
                    );
            });
    }

    // ── The control panel: presets, sliders, transport buttons, session log ──

    private UIForAnySwing<?,?> controlPanel() {

        // ── Lenses focused all the way down to individual settings ──
        // Safe as locals: each is consumed by a SwingTree binding that retains it.
        Var<BreathSettings> settings = vm.zoomTo(BreathingViewModel::settings, BreathingViewModel::withSettings);
        Var<Double> inhale  = settings.zoomTo(BreathSettings::inhaleSeconds,  BreathSettings::withInhaleSeconds);
        Var<Double> holdIn  = settings.zoomTo(BreathSettings::holdInSeconds,  BreathSettings::withHoldInSeconds);
        Var<Double> exhale  = settings.zoomTo(BreathSettings::exhaleSeconds,  BreathSettings::withExhaleSeconds);
        Var<Double> holdOut = settings.zoomTo(BreathSettings::holdOutSeconds, BreathSettings::withHoldOutSeconds);
        Var<Double> cycles  = settings.zoomTo(
                                    s -> (double) s.totalCycles(),
                                    (s, d) -> s.withTotalCycles((int) Math.round(d))
                                );
        Var<Tuple<CompletedCycle>> log = vm.zoomTo(BreathingViewModel::log, BreathingViewModel::withLog);

        // True while the session is idle — used to lock down session-resetting controls.
        Val<Boolean> idle = vm.viewAs(Boolean.class, m -> !m.running());

        return panel("fill, wrap 1, insets 22").withStyle( it -> it
                .backgroundColor(new Color(0, 0, 0, 95))
            )
            .add("growx, wmin 0",
                sectionTitle("Pattern")
            )
            .add("growx, gaptop 4, wmin 0",
                panel("fill, insets 0").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
                .add("grow, wmin 0", presetButton("Box",   BreathSettings.box(),            idle))
                .add("grow, wmin 0", presetButton("4-7-8", BreathSettings.fourSevenEight(), idle))
                .add("grow, wmin 0", presetButton("Calm",  BreathSettings.deepCalm(),       idle))
            )
            .add("growx, gaptop 16, wmin 0",
                sectionTitle("Timing")
            )
            .add("growx, wmin 0", sliderRow("Inhale",  inhale,  1, 12))
            .add("growx, wmin 0", sliderRow("Hold",    holdIn,  0, 12))
            .add("growx, wmin 0", sliderRow("Exhale",  exhale,  1, 12))
            .add("growx, wmin 0", sliderRow("Rest",    holdOut, 0, 12))
            .add("growx, wmin 0", sliderRow("Cycles",  cycles,  1, 12))
            .add("growx, gaptop 18, wmin 0",
                panel("fill, insets 0").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
                .add("grow, wmin 0",
                    button(vm.viewAsString(BreathingView::transportLabel))
                    .withStyle( it -> it
                        .backgroundColor(new Color(120, 176, 238, 210))
                        .foregroundColor(Color.WHITE)
                        .borderRadius(20).padding(9, 14, 9, 14)
                    )
                    .onClick( it -> {
                        if ( vm.get().running() ) {
                            vm.update(BreathingViewModel::pause);
                        } else {
                            vm.update(BreathingViewModel::begin);
                            UI.animate(vm, BreathingViewModel::breathAnimation);
                        }
                    })
                )
                .add("grow, gapleft 8, wmin 0",
                    button("↺  Reset")
                    .isEnabledIf(idle)
                    .withStyle( it -> it
                        .backgroundColor(new Color(255, 255, 255, 22))
                        .foregroundColor(new Color(220, 226, 244))
                        .borderRadius(20).padding(9, 14, 9, 14)
                    )
                    .onClick( it -> vm.update(BreathingViewModel::freshSession) )
                )
            )
            .add("growx, gaptop 18, wmin 0",
                sectionTitle("Completed cycles")
            )
            .add("grow, push, wmin 0",
                scrollPanels().withPrefHeight(150)
                .addAll(log, (Var<CompletedCycle> entry) -> cycleCard(entry))
            );
    }

    // ── Small reusable view fragments ────────────────────────────────────────

    private UIForAnySwing<?,?> presetButton(
        String label, BreathSettings preset, Val<Boolean> idle
    ) {
        return button(label)
            .isEnabledIf(idle)
            .withStyle( it -> it
                .backgroundColor(new Color(255, 255, 255, 18))
                .foregroundColor(new Color(222, 228, 246))
                .borderRadius(16).padding(8, 6, 8, 6).margin(0, 3, 0, 3)
            )
            .onClick( it -> vm.update( m -> m.applyPreset(preset) ) );
    }

    private static UIForAnySwing<?,?> sliderRow( String name, Var<Double> value, double min, double max ) {
        return panel("fill, insets 3 0 3 0").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
            .add("width 58!",
                label(name).withStyle( it -> it
                    .foregroundColor(new Color(176, 186, 214))
                    .componentFont( f -> f.size(12).family("SansSerif") )
                )
            )
            .add("growx, pushx, wmin 0",
                slider(Align.HORIZONTAL, min, max, value)
            )
            .add("width 42!",
                label(value.viewAsString( d -> formatSeconds(name, d) ))
                .withStyle( it -> it
                    .foregroundColor(new Color(232, 236, 252))
                    .componentFont( f -> f.size(12).family("SansSerif") )
                )
            );
    }

    private static UIForAnySwing<?,?> cycleCard( Var<CompletedCycle> entry ) {
        return panel("fill, insets 7 12 7 12").withStyle( it -> it
                .backgroundColor(new Color(255, 255, 255, 16))
                .borderRadius(14).margin(3, 0, 3, 0)
            )
            .add("pushx, growx, wmin 0",
                label(entry.viewAsString( c -> "❀  Cycle " + c.index() ))
                .withStyle( it -> it
                    .foregroundColor(new Color(224, 230, 248))
                    .componentFont( f -> f.size(13).family("Serif") )
                )
            )
            .add("shrinkx",
                label(entry.viewAsString( c -> "~" + Math.round(c.durationSeconds()) + "s" ))
                .withStyle( it -> it
                    .foregroundColor(new Color(146, 158, 196))
                    .componentFont( f -> f.size(12).family("SansSerif") )
                )
            );
    }

    private static UIForAnySwing<?,?> sectionTitle( String text ) {
        return label(text.toUpperCase(Locale.ROOT)).withStyle( it -> it
            .foregroundColor(new Color(132, 146, 188))
            .componentFont( f -> f.size(11).weight(2).family("SansSerif") )
        );
    }

    // ── Pure helper functions ────────────────────────────────────────────────

    private static String instructionFor( BreathingViewModel m ) {
        if ( m.running() )
            return m.phase().instruction();
        if ( m.isSessionComplete() && m.cyclesCompleted() > 0 )
            return "Session complete — well done";
        if ( m.cyclesCompleted() > 0 )
            return "Paused — breathe naturally";
        return "Find a comfortable seat";
    }

    private static String transportLabel( BreathingViewModel m ) {
        if ( m.running() )           return "❚❚  Pause";
        if ( m.isSessionComplete() && m.cyclesCompleted() > 0 ) return "↺  Begin again";
        if ( m.cyclesCompleted() > 0 ) return "▶  Resume";
        return "▶  Begin";
    }

    private static String formatSeconds( String name, double value ) {
        if ( "Cycles".equals(name) )
            return String.valueOf((int) Math.round(value));
        return String.format("%.1fs", value);
    }

    private static UI.Color phaseColor( BreathPhase phase ) {
        switch ( phase ) {
            case INHALE:   return UI.Color.ofRgb(108, 178, 240); // sky blue
            case HOLD_IN:  return UI.Color.ofRgb(118, 214, 200); // teal
            case EXHALE:   return UI.Color.ofRgb(208, 150, 228); // lavender
            case HOLD_OUT:
            default:       return UI.Color.ofRgb(116, 134, 222); // periwinkle
        }
    }

    private static Color withAlpha( Color c, int alpha ) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // ── main ─────────────────────────────────────────────────────────────────

    public static void main( String[] args ) {
        FlatDarkLaf.setup();
        Var<BreathingViewModel> vm = Var.of(new BreathingViewModel());
        UI.show("Tranquil — A Breathing Companion", frame -> new BreathingView(vm));
        EventProcessor.DECOUPLED.join();
    }
}
package examples.breathing.mvi;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
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
 *          state inside {@code withStyle(..)}, repainted via
 *          {@code withRepaintOn(..)} on its two animated properties.</li>
 *  </ol>
 *  <p>
 *  <b>Why is {@link #phase} a field?</b> Sprouts lenses and views observe
 *  their parent property only <i>weakly</i>, so an unreferenced lens may be
 *  garbage-collected and silently stop updating. SwingTree's own component
 *  bindings (e.g. {@code label(..)}, {@code slider(..)}, {@code withRepaintOn(..)})
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

    private static final int ORB_BOX = 460;

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
        // (label / progressBar / withRepaintOn), which keeps a strong reference.
        Val<String> instruction   = vm.viewAsString(BreathingView::instructionFor);
        Val<String> cycleInfo     = vm.viewAsString(m -> "Cycle " + m.currentCycle() + " of " + m.settings().totalCycles());
        Val<Double> orbScale      = vm.viewAsDouble(BreathingViewModel::orbScale);
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
        .withPrefSize(1080, 720)
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
        .add("growx", header())
        .add("grow, push",
            panel("fill, insets 0").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
            .add("grow, push",
                orbStage(instruction, cycleInfo, orbScale, phaseProgress, sessionDone)
            )
            .add("growy, width 340!",
                controlPanel()
            )
        );
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> header() {
        return panel("fill, insets 16 26 16 26")
            .withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 95)) )
            .add("pushx, growx",
                html(
                    "<span style='color:#f3f4ff;font-family:serif;font-size:21px;'>Tranquil</span>" +
                    "<span style='color:#9aa6c8;font-style:italic;font-size:12px;'>" +
                    "&nbsp;&nbsp;—&nbsp;&nbsp;a breathing companion, animated with SwingTree</span>"
                )
            );
    }

    // ── The orb stage: instruction, the breathing orb, and progress ──────────

    private UIForAnySwing<?,?> orbStage(
        Val<String> instruction,
        Val<String> cycleInfo,
        Val<Double> orbScale,
        Val<Double> phaseProgress,
        Val<Double> sessionDone
    ) {
        return panel("fill, wrap 1, insets 30").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
            .add("center, gapbottom 6",
                label(instruction)
                .withStyle( it -> it
                    .foregroundColor(new Color(232, 236, 252))
                    .componentFont( f -> f.size(24).family("Serif").posture(0.08f) )
                )
            )
            .add("center, push",
                breathingOrb(orbScale, phaseProgress)
            )
            .add("center, gaptop 4",
                label(cycleInfo)
                .withStyle( it -> it
                    .foregroundColor(new Color(150, 162, 198))
                    .componentFont( f -> f.size(13).family("SansSerif") )
                )
            )
            .add("center, growx, width 360!, gaptop 8",
                progressBar(Align.HORIZONTAL, phaseProgress)
            )
            .add("center, growx, width 360!, gaptop 2",
                label(sessionDone.viewAsString( d -> "Session " + Math.round(d * 100) + "% complete" ))
                .withStyle( it -> it
                    .foregroundColor(new Color(120, 132, 168))
                    .componentFont( f -> f.size(11).family("SansSerif") )
                )
            );
    }

    // ── The breathing orb itself — pure reactive styling ─────────────────────

    private UIForAnySwing<?,?> breathingOrb( Val<Double> orbScale, Val<Double> phaseProgress ) {
        return box().withPrefSize(ORB_BOX, ORB_BOX)
            // Repaint the orb whenever either animated property changes. During
            // a hold the scale is constant, so the phase-progress view is what
            // keeps the countdown and the colour morph alive.
            .withRepaintOn(orbScale, phaseProgress)
            .withStyle( it -> {
                BreathingViewModel m = vm.get();
                double  s      = m.orbScale();
                double  cx     = it.componentWidth()  * 0.5;
                double  cy     = it.componentHeight() * 0.5;
                double  radius = 95 + 250 * s;
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
                        .size(radius * 1.6)
                        .colors(
                            withAlpha(glow, 20 + (int) (60 * s)),
                            withAlpha(glow, 0)
                        )
                        .clipTo(ComponentArea.BODY)
                    )
                    // A soft outer bloom whose blur tracks the breath.
                    .shadowColor(withAlpha(glow, 55 + (int) (120 * s)))
                    .shadowBlurRadius((int) (16 + 78 * s))
                    .shadowSpreadRadius((int) (2 + 24 * s))
                    .shadowIsInset(false)
                    // The countdown number, sized to the breath.
                    .text( t -> t
                        .content(orbText)
                        .placement(Placement.CENTER)
                        .clipTo(ComponentArea.BODY)
                        .font( f -> f
                            .size((int) (38 + 34 * s))
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
            .add("growx",
                sectionTitle("Pattern")
            )
            .add("growx, gaptop 4",
                panel("fill, insets 0").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
                .add("grow", presetButton("Box",   BreathSettings.box(),            idle))
                .add("grow", presetButton("4-7-8", BreathSettings.fourSevenEight(), idle))
                .add("grow", presetButton("Calm",  BreathSettings.deepCalm(),       idle))
            )
            .add("growx, gaptop 16",
                sectionTitle("Timing")
            )
            .add("growx", sliderRow("Inhale",  inhale,  1, 12))
            .add("growx", sliderRow("Hold",    holdIn,  0, 12))
            .add("growx", sliderRow("Exhale",  exhale,  1, 12))
            .add("growx", sliderRow("Rest",    holdOut, 0, 12))
            .add("growx", sliderRow("Cycles",  cycles,  1, 12))
            .add("growx, gaptop 18",
                panel("fill, insets 0").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
                .add("grow",
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
                .add("grow, gapleft 8",
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
            .add("growx, gaptop 18",
                sectionTitle("Completed cycles")
            )
            .add("grow, push",
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
            .add("growx, pushx",
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
            .add("pushx, growx",
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
        return label(text.toUpperCase()).withStyle( it -> it
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
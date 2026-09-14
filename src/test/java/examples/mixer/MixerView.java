package examples.mixer;

import com.formdev.flatlaf.FlatDarkLaf;
import examples.laf.SwingTreeLookAndFeel.Surface;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForLabel;
import swingtree.UIForMenu;
import swingtree.UIForPanel;
import swingtree.api.model.SliderTicks;
import swingtree.layout.FlowCell;
import swingtree.style.ComponentStyleDelegate;
import swingtree.threading.EventProcessor;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static swingtree.UI.*;

/**
 *  <b>Harbor Room — a small studio mixing console.</b>
 *  <p>
 *  Four input strips, a master bus, a tape transport, a channel EQ, a vintage compressor plugin,
 *  headphone monitoring and a keyboard controller: some forty sliders, which is what a mixing desk
 *  is made of. Every one of them is bound to a lens into a single immutable {@link MixerViewModel}.
 *  <p>
 *  Things worth trying, each of which exercises one promise a SwingTree slider makes:
 *  <ul>
 *      <li><b>Press play and grab the timeline.</b> The song keeps playing and the application keeps
 *          moving the playhead, but the knob stays under your mouse, and the song continues from
 *          where you let go. Same for the master fader in "Read automation" mode, except that
 *          letting go writes nothing there, because that fader is bound read-only.</li>
 *      <li><b>Switch on "Heavy DSP load"</b> (Engine menu or status bar) and drag a fader quickly.
 *          The application thread falls behind, and the numbers the fader wrote come back to it
 *          late. The knob must not jump back, and the number you let go at must stay.</li>
 *      <li><b>Recall a snapshot</b> from the Session menu. Faders land between their tick marks
 *          and sends at 127, beyond their last tick mark at 120. Click a knob without moving it:
 *          nothing may change. Press an arrow key on a send at 127: it must not move left.</li>
 *      <li><b>Link two strips</b> ("L") and move one fader: the other follows.</li>
 *      <li><b>Move the loop points</b>: each one is the limit of the other. <b>Change the song or the
 *          metre</b>: the timeline, its bar numbers and both loop sliders are laid out again.</li>
 *      <li><b>Lower the hearing protection limit</b>: the loud speaker icon travels along the
 *          headphone scale, and the headphone volume is pulled down with it.</li>
 *      <li><b>Pick a strip's name</b> to show it in the EQ: every EQ slider jumps to that strip.
 *          <b>Change the label language</b> (View menu): the frequency and limiter numbers follow.</li>
 *      <li><b>Use the compressor plugin's buttons.</b> Its editor is old Swing code which calls
 *          {@code setMaximum(..)} on its sliders directly. The whole-number threshold slider takes
 *          the new range, the fractional ratio slider refuses it at once.</li>
 *      <li><b>Bend the pitch wheel</b> and let go: it springs back. Switch the wheels to ribbons.</li>
 *      <li><b>Switch the look and feel</b> (Look menu), and check that every scale above still reads.</li>
 *      <li><b>Make the window narrow.</b> The rack moves below the desk, the strips go two to a row,
 *          and every slider has to lay out its tick marks and labels again for its new width.</li>
 *  </ul>
 */
public final class MixerView extends Panel
{
    private static final int STRIPS = 4;

    private final Var<MixerViewModel> vm;

    /**
     *  The look and feel chosen in the Look menu. It is a preference about this program's chrome rather than
     *  mix state, so it is not part of the view model. {@link LookSwitcher} owns what happens when it changes.
     */
    private final Var<Look> look;

    /**
     *  The boolean lenses behind the radio menu items. A lens is observed only weakly by the property it
     *  was zoomed out of, so one that nothing holds would quietly stop reporting.
     */
    private final List<Var<Boolean>> radioLenses = new ArrayList<>();

    /** The two sliders of the compressor plugin, which its "legacy" editor code reconfigures directly. */
    private JSlider pluginThresholdSlider;
    private JSlider pluginRatioSlider;

    public MixerView( Var<MixerViewModel> vm, Var<Look> look ) {
        this.vm   = vm;
        this.look = look;

        of(this).group(Surface.TRANSPARENT).withLayout("fill, wrap 1, ins 0, gap 0", "[grow]", "[][grow][]")
        .withPrefSize(1600, 1000)
        .add(GROW_X, menuBar())
        .add(GROW.and(PUSH).and("wmin 0, hmin 0"), page())
        .add(GROW_X.and("wmin 0"), statusBar());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The page: nested responsive grids
    // ════════════════════════════════════════════════════════════════════════
    //
    //  The console converges from a wide studio monitor down to a narrow window without a
    //  single line of state: no form factor in the view model, no resize listener. Every
    //  container below is a 12-column grid (withFlowLayout), and every child declares with
    //  AUTO_SPAN how many of the 12 columns it takes in each size category of its grid.
    //
    //  A size category is the width of a grid as a fraction of its REFERENCE width, which
    //  is the preferred width set on the grid: below 1/5 very small, below 2/5 small, below
    //  3/5 medium, below 4/5 large, up to 1 very large, beyond 1 oversize. So the numbers
    //  below are relative bands, and each nested grid has a reference width of its own:
    //
    //    grid       reference   what its children span, by the size category of that grid
    //    page       1600        transport 12 always; desk 8 beside rack 4 from very large up,
    //                           both 12 below that, so the rack moves under the desk
    //    transport  1700        controls and song 3 from very large up, 6 at large and medium;
    //                           tempo and timecode 3 from very large up, 6 at large;
    //                           everything 12 below that, and timeline and loop always 12
    //    desk       1050        strips 2 and master 4 from very large up; strips 3 at large;
    //                           strips 6 at medium and small; master 12 below very large;
    //                           everything 12 when very small
    //    rack        900        cards 6, two per row, when oversize; one per row otherwise
    //
    //  Three rules keep the grids honest. A grid reports the SUM of its children's minimum
    //  widths as its own minimum, so every grid gets withMinSize(0, 0). A grid never stretches
    //  a row to the height of its container, so the page grid sits in a scroll pane which
    //  fits it to the width and scrolls the height. And a nested grid declares its reference
    //  width as withPrefSize(width, 0), which is only safe because its parent is a grid too:
    //  a MigLayout parent would take that height of 0 literally.

    private static final int PAGE_REFERENCE_WIDTH      = 1600;
    private static final int TRANSPORT_REFERENCE_WIDTH = 1700;
    private static final int DESK_REFERENCE_WIDTH      = 1050;
    private static final int RACK_REFERENCE_WIDTH      =  900;

    private static final FlowCell FULL_ROW = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(12).veryLarge(12).oversize(12) );

    private static final FlowCell DESK_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(12).veryLarge(8).oversize(8) );

    private static final FlowCell RACK_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(12).veryLarge(4).oversize(4) );

    private static final FlowCell TRANSPORT_CONTROLS_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(6).large(6).veryLarge(3).oversize(3) );

    private static final FlowCell TRANSPORT_WIDE_PIECE_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(6).veryLarge(3).oversize(3) );

    private static final FlowCell STRIP_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(6).medium(6).large(3).veryLarge(2).oversize(2) );

    private static final FlowCell MASTER_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(12).veryLarge(4).oversize(4) );

    private static final FlowCell RACK_CARD_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(12).veryLarge(12).oversize(6) );

    private UIForAnySwing<?, ?> page() {
        return scrollPane(conf -> conf.fitWidth(true)).group(Surface.TRANSPARENT)
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withVerticalScrollIncrement(24)
            .add(
                grid(PAGE_REFERENCE_WIDTH, 12)
                .add(FULL_ROW,  transportDeck())
                .add(DESK_SPAN, desk())
                .add(RACK_SPAN, rack())
            );
    }

    private static UIForPanel<JPanel> grid( int referenceWidth, int gap ) {
        return panel().group(Surface.TRANSPARENT)
                .withFlowLayout(UI.HorizontalAlignment.LEFT, gap, gap)
                .withMinSize(0, 0)
                .withPrefSize(referenceWidth, 0);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Menus
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, JMenuBar> menuBar() {
        Var<Boolean> scalesVisible = vm.zoomTo(MixerViewModel::scalesVisible, MixerViewModel::withScalesVisible);
        Var<Boolean> faderSnap     = vm.zoomTo(MixerViewModel::faderSnap,     MixerViewModel::withFaderSnap);
        Var<Boolean> underLoad     = vm.zoomTo(MixerViewModel::engineUnderLoad, MixerViewModel::withEngineLoad);
        Var<LabelLanguage> language = vm.zoomTo(MixerViewModel::language, MixerViewModel::withLanguage);

        UIForMenu<JMenu> session = menu("Session");
        for ( SessionPreset preset : SessionPreset.values() )
            session = session.add(menuItem("Recall snapshot: " + preset).onClick( it -> vm.update(m -> m.applying(preset)) ));

        UIForMenu<JMenu> numbers = menu("Label numbers");
        ButtonGroup languages = new ButtonGroup();
        for ( LabelLanguage choice : LabelLanguage.values() )
            numbers = numbers.add(radioItem(choice.toString(), language, choice, languages));

        UIForMenu<JMenu> looks = menu("Look");
        ButtonGroup lookGroup = new ButtonGroup();
        for ( Look choice : Look.all() ) {
            if ( choice.isSwingTree() && !Look.all().get(Look.all().indexOf(choice) - 1).isSwingTree() )
                looks = looks.add(separator());
            looks = looks.add(radioItem(choice.toString(), look, choice, lookGroup));
        }

        return of(new JMenuBar())
            .add(session)
            .add(menu("View")
                .add(checkBoxMenuItem("Show scale labels on faders and EQ", scalesVisible))
                .add(checkBoxMenuItem("Snap faders to whole decibels", faderSnap))
                .add(numbers)
            )
            .add(looks)
            .add(menu("Engine")
                .add(checkBoxMenuItem("Simulate heavy DSP load", underLoad))
            );
    }

    /**
     *  A radio menu item bound through a boolean lens. {@code radioButtonMenuItem(anEnum, aVar)} would
     *  only ever read the property: a menu item is not a toggle button, so that overload has no write-back.
     */
    private <E> UIForAnySwing<?, ?> radioItem( String text, Var<E> property, E value, ButtonGroup group ) {
        Var<Boolean> isChosen = property.zoomTo(current -> current == value, (current, chosen) -> chosen ? value : current);
        radioLenses.add(isChosen);
        return radioButtonMenuItem(text, isChosen).peek(group::add);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The transport: play, song, metre, tempo, timeline and loop
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, ?> transportDeck() {
        Var<Transport>     transport  = vm.zoomTo(MixerViewModel::transport, MixerViewModel::withTransport);
        Var<Song>          song       = vm.zoomTo(m -> m.transport().song(), MixerViewModel::withSongLoaded);
        Var<TimeSignature> signature  = transport.zoomTo(Transport::signature, Transport::withSignatureChangedTo);
        Var<Integer>       bpm        = transport.zoomTo(Transport::bpm,        Transport::withBpm);
        Var<Long>          position   = transport.zoomTo(Transport::position,   Transport::withPositionAt);
        Var<Long>          loopStart  = transport.zoomTo(Transport::loopStart,  Transport::withLoopStartAt);
        Var<Long>          loopEnd    = transport.zoomTo(Transport::loopEnd,    Transport::withLoopEndAt);
        Var<Boolean>       looping    = transport.zoomTo(Transport::looping,    Transport::withLooping);
        Var<Boolean>       snapToBars = transport.zoomTo(Transport::snapToBars, Transport::withSnapToBars);

        Val<Long>                   songStart     = Val.of(0L);
        Viewable<Long>              songEnd       = transport.viewAs(Long.class, Transport::end);
        Viewable<SliderTicks<Long>> timelineTicks = transport.viewAs(SliderTicks.classTyped(Long.class), t -> Scales.timeline(t.signature(), t.end(), t.snapToBars()));
        Viewable<SliderTicks<Long>> loopTicks     = transport.viewAs(SliderTicks.classTyped(Long.class), t -> Scales.loopPoint(t.signature()));

        return grid(TRANSPORT_REFERENCE_WIDTH, 10).group(Surface.RAIL).withStyle(it -> rail(it).padding(10, 16, 10, 16))
            .add(TRANSPORT_CONTROLS_SPAN,
                box("ins 0, gap 10, aligny center", "[][][][]")
                .add(button(transport.viewAsString(t -> t.playing() ? "■  Stop" : "▶  Play"))
                     .withStyle(it -> it.componentFont(f -> f.size(13).weight(2f)))
                     .onClick( it -> vm.update(m -> m.withTransport(m.transport().playedOrStopped())) ))
                .add(button("⏮").withTooltip("Back to the start")
                     .onClick( it -> vm.update(m -> m.withTransport(m.transport().withPositionAt(0))) ))
                .add(checkBox("Loop", looping))
                .add(checkBox("Snap to bars", snapToBars))
            )
            .add(TRANSPORT_CONTROLS_SPAN,
                box("fillx, ins 0, gap 10, aligny center", "[][grow, fill, 90::][][76::]")
                .add(caption("Song"))
                .add(GROW_X.and("wmin 0"), comboBox(song))
                .add(caption("Metre"))
                .add(comboBox(signature))
            )
            .add(TRANSPORT_WIDE_PIECE_SPAN,
                box("fillx, ins 0, gap 10", "[][grow, fill][60!]")
                .add(caption("Tempo"))
                .add(GROW_X.and("wmin 0"),
                    slider(UI.Axis.HORIZONTAL, 40, 240, bpm).withTicks(Scales.tempo()).withStyle(MixerView::scaleFont))
                .add(readout(bpm.viewAsString(b -> b + " BPM")))
            )
            .add(TRANSPORT_WIDE_PIECE_SPAN,
                box("fillx, ins 0, aligny center", "push[]")
                .add(label(transport.viewAsString(Transport::timecode))
                     .withStyle(it -> it.componentFont(f -> f.family(Font.MONOSPACED).size(20).weight(2f).color(Ink.accent()))))
            )
            .add(FULL_ROW,
                box("fillx, ins 0, gap 10", "[64!][grow]")
                .add(caption("Timeline"))
                .add(GROW_X.and("wmin 0"),
                    slider(UI.Axis.HORIZONTAL, songStart, songEnd, position)
                    .withTicks(timelineTicks)
                    .withStyle(MixerView::scaleFont))
            )
            .add(FULL_ROW,
                box("fillx, ins 0, gap 10", "[64!][grow][][grow][110!]")
                .add(caption("Loop from"))
                .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, songStart, loopEnd, loopStart).withTicks(loopTicks))
                .add(caption("to"))
                .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, loopStart, songEnd, loopEnd).withTicks(loopTicks))
                .add(readout(transport.viewAsString(t ->
                        "bars " + (t.loopStart() / t.signature().ticksPerBar() + 1) +
                        " – "   + (t.loopEnd()   / t.signature().ticksPerBar() + 1))))
            );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The desk: four input strips and the master strip
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, ?> desk() {
        UIForPanel<JPanel> desk = grid(DESK_REFERENCE_WIDTH, 10);
        for ( int index = 0; index < STRIPS; index++ )
            desk = desk.add(STRIP_SPAN, strip(index));
        return desk.add(MASTER_SPAN, masterStrip());
    }

    /** The rack beside or below the desk: EQ, compressor, headphones and keyboard controller. */
    private UIForAnySwing<?, ?> rack() {
        return grid(RACK_REFERENCE_WIDTH, 10)
                .add(RACK_CARD_SPAN, equaliserCard())
                .add(RACK_CARD_SPAN, compressorCard())
                .add(RACK_CARD_SPAN, monitorCard())
                .add(RACK_CARD_SPAN, keyboardCard());
    }

    // ── One input strip ─────────────────────────────────────────────────────

    private UIForAnySwing<?, ?> strip( int index ) {
        Var<Channel> strip  = vm.zoomTo(m -> m.channel(index), (m, changed) -> m.withChannelAt(index, changed));
        Var<Integer> trim   = strip.zoomTo(Channel::trimDb,  Channel::withTrimDb);
        Var<Byte>    send   = strip.zoomTo(Channel::send,    Channel::withSend);
        Var<Float>   pan    = strip.zoomTo(Channel::pan,     Channel::withPan);
        Var<Double>  fader  = strip.zoomTo(Channel::faderDb, Channel::withFaderDb);
        Var<Boolean> muted  = strip.zoomTo(Channel::muted,   Channel::withMuted);
        Var<Boolean> soloed = strip.zoomTo(Channel::soloed,  Channel::withSoloed);
        Var<Boolean> linked = strip.zoomTo(Channel::linked,  Channel::withLinked);
        Var<Boolean> inEq   = vm.zoomTo(m -> m.selectedIndex() == index, (m, chosen) -> chosen ? m.withSelectedIndex(index) : m);

        Viewable<SliderTicks<Double>> faderTicks = vm.viewAs(SliderTicks.classTyped(Double.class), m -> Scales.fader(m.scalesVisible(), m.faderSnap()));
        Viewable<Double>              level      = vm.viewAsDouble(m -> m.meterDbOf(index));
        Viewable<Integer>             colour     = strip.viewAsInt(Channel::colorRgb);

        return panel("fill, wrap 1, ins 12 10 10 10, gap 3", "[grow, fill]", "[][][]8[][]6[][]6[][]8[grow, fill][]")
            .group(Surface.CARD)
            .withStyle(colour, (rgb, it) -> card(it)
                .painter(UI.Layer.FOREGROUND, g -> {
                    g.setColor(new Color(rgb));
                    g.fillRoundRect(12, 3, it.componentWidth() - 24, 4, 4, 4);
                })
            )
            .add(GROW_X, toggleButton(strip.viewAsString(Channel::name), inEq)
                        .withTooltip("Show this strip in the EQ")
                        .withStyle(it -> it.componentFont(f -> f.size(13).weight(2f))))
            .add(GROW_X,
                box("fillx, ins 0, gap 3", "[grow, fill][grow, fill][grow, fill]")
                .add(toggleButton("M", muted).withTooltip("Mute"))
                .add(toggleButton("S", soloed).withTooltip("Solo"))
                .add(toggleButton("L", linked).withTooltip("Link this fader to the other linked faders"))
            )
            .add(readout(level.viewAsString(MixerView::meterText)))
            .add(caption("Trim"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, -20, 20, trim).withTicks(Scales.trim()).withStyle(MixerView::scaleFont))
            .add(caption("Reverb send"))
            .add(GROW_X.and("wmin 0"),
                slider(UI.Axis.HORIZONTAL, (byte) 0, (byte) 127, send)
                .withTicks(Scales.send())
                .withStyle(MixerView::scaleFont)
                .onChange( it -> vm.update(m -> m.loggingSendOf(index)) )
            )
            .add(caption("Pan"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, -1.0f, 1.0f, pan).withTicks(Scales.pan()).withStyle(MixerView::scaleFont))
            .add(GROW.and("h 160:340:"),
                box("fill, ins 0, gap 6", "[grow, fill][10!]", "[grow, fill]")
                .add(GROW.and("hmin 120"), slider(UI.Axis.VERTICAL, Channel.SILENCE_DB, Channel.FADER_MAX_DB, fader).withTicks(faderTicks).withStyle(MixerView::scaleFont))
                .add(GROW_Y, meter(level))
            )
            .add(readout(fader.viewAsString(MixerView::decibels)));
    }

    // ── The master strip ────────────────────────────────────────────────────

    private UIForAnySwing<?, ?> masterStrip() {
        Var<Double>         masterDb   = vm.zoomTo(MixerViewModel::masterDb,         MixerViewModel::withMasterDb);
        Var<AutomationMode> automation = vm.zoomTo(MixerViewModel::automation,       MixerViewModel::withAutomation);
        Var<Float>          width      = vm.zoomTo(MixerViewModel::stereoWidth,      MixerViewModel::withStereoWidth);
        Var<Double>         ceiling    = vm.zoomTo(MixerViewModel::limiterCeilingDb, MixerViewModel::withLimiterCeilingDb);

        Viewable<Double>              automationDb   = vm.viewAsDouble(MixerViewModel::automationDb);
        Viewable<Double>              level          = vm.viewAsDouble(MixerViewModel::masterMeterDb);
        Viewable<Boolean>             isManual       = automation.viewAs(Boolean.class, mode -> mode == AutomationMode.MANUAL);
        Viewable<Boolean>             isReading      = automation.viewAs(Boolean.class, mode -> mode == AutomationMode.READ);
        Viewable<SliderTicks<Double>> faderTicks     = vm.viewAs(SliderTicks.classTyped(Double.class), m -> Scales.fader(m.scalesVisible(), m.faderSnap()));
        Viewable<SliderTicks<Double>> ceilingTicks   = vm.viewAs(SliderTicks.classTyped(Double.class), m -> Scales.limiterCeiling(m.language()));

        return panel("fill, wrap 1, ins 12 10 10 10, gap 3", "[grow, fill]", "[][]6[][]6[][][]8[grow, fill][]")
            .group(Surface.CARD)
            .withStyle(it -> card(it)
                .painter(UI.Layer.FOREGROUND, g -> {
                    g.setColor(Ink.text());
                    g.fillRoundRect(12, 3, it.componentWidth() - 24, 4, 4, 4);
                })
            )
            .add(label("Master").withStyle(it -> it.componentFont(f -> f.size(14).weight(2f))))
            .add(readout(level.viewAsString(MixerView::meterText)))
            .add(caption("Stereo width"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, 0.0f, 2.0f, width).withTicks(Scales.stereoWidth()).withStyle(MixerView::scaleFont))
            .add(caption("Limiter ceiling (dB)"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, -3.0, 0.0, ceiling).withTicks(ceilingTicks).withStyle(MixerView::scaleFont))
            .add(GROW_X.and("wmin 0"), comboBox(automation))
            .add(GROW.and("h 160:340:"),
                box("fill, ins 0, gap 6, hidemode 3", "[grow, fill][12!]", "[grow, fill]")
                .add("cell 0 0, grow, hmin 120",
                    slider(UI.Axis.VERTICAL, Channel.SILENCE_DB, Channel.FADER_MAX_DB, masterDb)
                    .withTicks(faderTicks).withStyle(MixerView::scaleFont)
                    .isVisibleIf(isManual))
                .add("cell 0 0, grow, hmin 120",
                    slider(UI.Axis.VERTICAL, Channel.SILENCE_DB, Channel.FADER_MAX_DB, automationDb)
                    .withTicks(faderTicks).withStyle(MixerView::scaleFont)
                    .withTooltip("Follows the recorded fade. Grab it while playing: it holds still, and lets go of nothing.")
                    .isVisibleIf(isReading))
                .add("cell 1 0, growy", meter(level))
            )
            .add(readout(vm.viewAsString(m -> decibels(m.effectiveMasterDb()))));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The rack
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, ?> equaliserCard() {
        Var<Channel> selected  = vm.zoomTo(MixerViewModel::selectedChannel, MixerViewModel::withSelectedChannelChanged);
        Var<Double>  lowGain   = selected.zoomTo(Channel::lowGainDb,      Channel::withLowGainDb);
        Var<Double>  midGain   = selected.zoomTo(Channel::midGainDb,      Channel::withMidGainDb);
        Var<Integer> midFreq   = selected.zoomTo(Channel::midFrequencyHz, Channel::withMidFrequencyHz);
        Var<Double>  highGain  = selected.zoomTo(Channel::highGainDb,     Channel::withHighGainDb);

        Viewable<SliderTicks<Double>>  gainTicks = vm.viewAs(SliderTicks.classTyped(Double.class),  m -> Scales.eqGain(m.scalesVisible()));
        Viewable<SliderTicks<Integer>> freqTicks = vm.viewAs(SliderTicks.classTyped(Integer.class), m -> Scales.midFrequency(m.language()));

        return rackCard(selected.viewAsString(c -> "EQ · " + c.name()), "Pick a strip's name to bring it here.")
            .add(GROW_X.and("span, h 120!"),
                box().withStyle(selected, (channel, it) -> it
                    .borderRadius(8)
                    .painter(UI.Layer.CONTENT, g -> paintEqCurve(g, it.componentWidth(), it.componentHeight(), channel))
                )
            )
            .add(caption("Low shelf"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, -12.0, 12.0, lowGain).withTicks(gainTicks).withStyle(MixerView::scaleFont))
            .add(readout(lowGain.viewAsString(MixerView::decibels)))
            .add(caption("Mid gain"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, -12.0, 12.0, midGain).withTicks(gainTicks).withStyle(MixerView::scaleFont))
            .add(readout(midGain.viewAsString(MixerView::decibels)))
            .add(caption("Mid frequency"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, 200, 5000, midFreq).withTicks(freqTicks).withStyle(MixerView::scaleFont))
            .add(readout(midFreq.viewAsString(hz -> String.format(Locale.ROOT, "%d Hz", hz))))
            .add(caption("High shelf"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, -12.0, 12.0, highGain).withTicks(gainTicks).withStyle(MixerView::scaleFont))
            .add(readout(highGain.viewAsString(MixerView::decibels)));
    }

    private UIForAnySwing<?, ?> compressorCard() {
        Var<Integer> threshold = vm.zoomTo(MixerViewModel::compressorThresholdDb, MixerViewModel::withCompressorThresholdDb);
        Var<Double>  ratio     = vm.zoomTo(MixerViewModel::compressorRatio,       MixerViewModel::withCompressorRatio);

        return rackCard(Val.of("Opto-2 Compressor"), "Its old Swing editor sets slider ranges itself.")
            .add(caption("Threshold (dB)"))
            .add(GROW_X.and("wmin 0"),
                slider(UI.Axis.HORIZONTAL, -40, 0, threshold)
                .withTicks(Scales.compressorThreshold())
                .withStyle(MixerView::scaleFont)
                .peek(s -> pluginThresholdSlider = s))
            .add(readout(threshold.viewAsString(db -> db + " dB")))
            .add(caption("Ratio"))
            .add(GROW_X.and("wmin 0"),
                slider(UI.Axis.HORIZONTAL, 1.0, 10.0, ratio)
                .withTicks(Scales.compressorRatio())
                .withStyle(MixerView::scaleFont)
                .peek(s -> pluginRatioSlider = s))
            .add(readout(ratio.viewAsString(r -> String.format(Locale.ROOT, "%.1f:1", r))))
            .add(SPAN.and("growx"),
                box("fillx, ins 4 0 0 0, gap 6", "[grow, fill, 0::][grow, fill, 0::][grow, fill, 0::]")
                .add(button("+10 dB").withTooltip("The plugin calls setMaximum(10) on its threshold slider")
                     .onClick( it -> UI.run(() -> pluginThresholdSlider.setMaximum(10)) ))
                .add(button("Factory").withTooltip("The plugin calls setMinimum(-40) and setMaximum(0)")
                     .onClick( it -> UI.run(() -> { pluginThresholdSlider.setMinimum(-40); pluginThresholdSlider.setMaximum(0); }) ))
                .add(button("Rescale").withTooltip("The plugin doubles the JSlider maximum of its fractional ratio slider")
                     .onClick( it -> UI.run(() -> pluginRatioSlider.setMaximum(pluginRatioSlider.getMaximum() * 2)) ))
            );
    }

    private UIForAnySwing<?, ?> monitorCard() {
        Var<Integer> volume = vm.zoomTo(MixerViewModel::monitorVolume, MixerViewModel::withMonitorVolume);
        Var<Integer> limit  = vm.zoomTo(MixerViewModel::monitorLimit,  MixerViewModel::withMonitorLimitAt);
        Viewable<SliderTicks<Integer>> volumeTicks = limit.viewAs(SliderTicks.classTyped(Integer.class), Scales::monitorVolume);

        return rackCard(Val.of("Headphones"), "The hearing protection limit is the end of the volume scale.")
            .add(caption("Volume"))
            .add(GROW_X.and("wmin 0"),
                slider(UI.Axis.HORIZONTAL, Val.of(0), limit, volume).withTicks(volumeTicks).withStyle(MixerView::scaleFont))
            .add(readout(volume.viewAsString(v -> v + "%")))
            .add(caption("Limit"))
            .add(GROW_X.and("wmin 0"), slider(UI.Axis.HORIZONTAL, 50, 100, limit).withTicks(Scales.monitorLimit()).withStyle(MixerView::scaleFont))
            .add(button("Dim").withTooltip("Drop the headphones to 30% of their volume")
                 .onClick( it -> vm.update(MixerViewModel::dimmed) ));
    }

    private UIForAnySwing<?, ?> keyboardCard() {
        Var<Short>      bend       = vm.zoomTo(MixerViewModel::pitchBend,  MixerViewModel::withPitchBendFromWheel);
        Var<Byte>       modulation = vm.zoomTo(MixerViewModel::modulation, MixerViewModel::withModulation);
        Var<WheelStyle> wheelStyle = vm.zoomTo(MixerViewModel::wheelStyle, MixerViewModel::withWheelStyle);
        Viewable<UI.Axis> axis     = wheelStyle.viewAs(UI.Axis.class, WheelStyle::axis);

        return rackCard(Val.of("Keyboard controller"), "The pitch wheel springs back to the middle when you let go.")
            .add(caption("Controls"))
            .add(SPAN.and("growx"), comboBox(wheelStyle))
            .add(SPAN.and("growx, h 230!"),
                box("fill, wrap 2, ins 0, gap 14 4", "[grow, fill][grow, fill]", "[][grow, fill]")
                .add(caption("Pitch bend"))
                .add(caption("Modulation"))
                .add(GROW.and("wmin 0, hmin 0"),
                    slider(UI.Axis.VERTICAL, (short) -8192, (short) 8191, bend)
                    .withTicks(Scales.pitchWheel())
                    .withOrientation(axis)
                    .withStyle(MixerView::scaleFont)
                    .peek(wheel -> wheel.setFocusable(false))
                    .onMousePress(   it -> vm.update(MixerViewModel::grabbingPitchWheel) )
                    .onMouseRelease( it -> vm.update(MixerViewModel::releasingPitchWheel) )
                    .onChange(       it -> vm.update(MixerViewModel::loggingPitchBend) ))
                .add(GROW.and("wmin 0, hmin 0"),
                    slider(UI.Axis.VERTICAL, (byte) 0, (byte) 127, modulation)
                    .withTicks(Scales.modWheel())
                    .withOrientation(axis)
                    .withStyle(MixerView::scaleFont)
                    .onChange( it -> vm.update(MixerViewModel::loggingModulation) ))
            )
            .add(SPAN.and("growx"), caption("MIDI out"))
            .add(SPAN.and("growx, h 190!"),
                label(vm.viewAsString(m -> midiLogHtml(m.midiLog())))
                .withStyle(it -> it
                    .borderRadius(8)
                    .padding(6, 10, 6, 10)
                    .painter(UI.Layer.BACKGROUND, g -> {
                        g.setColor(Ink.well());
                        g.fillRoundRect(0, 0, it.componentWidth(), it.componentHeight(), 12, 12);
                    })
                    .componentFont(f -> f.family(Font.MONOSPACED).size(11))
                )
                .peek(log -> log.setVerticalAlignment(JLabel.BOTTOM))
            );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Status bar
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, ?> statusBar() {
        Var<Boolean> underLoad = vm.zoomTo(MixerViewModel::engineUnderLoad, MixerViewModel::withEngineLoad);
        return panel("fillx, ins 6 16 8 16, gap 14", "[grow][][]").group(Surface.RAIL).withStyle(MixerView::rail)
            .add(GROW_X.and("wmin 0"), label(vm.viewAsString(MixerViewModel::status)).withStyle(it -> it.componentFont(f -> f.size(12))))
            .add(checkBox("Heavy DSP load", underLoad))
            .add(readout(vm.viewAsString(m -> String.format(Locale.ROOT, "DSP %3.0f%%", m.dspLoad() * 100))));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Small building blocks
    // ════════════════════════════════════════════════════════════════════════

    private static UIForPanel<JPanel> rackCard( Val<String> title, String subtitle ) {
        return panel("fillx, wrap 3, ins 12 14 14 14, gap 6 4", "[100!][grow, fill][72!]")
            .group(Surface.CARD)
            .withStyle(MixerView::card)
            .add(SPAN.and("growx, wmin 0"), label(title).withStyle(it -> it.componentFont(f -> f.size(15).weight(2f))))
            .add(SPAN.and("growx, wmin 0, gapbottom 6"), caption(subtitle));
    }

    private static UIForLabel<JLabel> caption( String text ) {
        return label(text).withStyle(it -> it.componentFont(f -> f.size(11).color(Ink.muted())));
    }

    private static UIForLabel<JLabel> readout( Val<String> text ) {
        return label(text).withStyle(it -> it.componentFont(f -> f.family(Font.MONOSPACED).size(12)));
    }

    /** The labels of a slider take its font, so this one style sizes every scale on the console. */
    private static ComponentStyleDelegate<JSlider> scaleFont( ComponentStyleDelegate<JSlider> it ) {
        return it.componentFont(f -> f.size(10));
    }

    /**
     *  A card: under the SwingTree look and feel the {@code Surface.CARD} group tag already makes the
     *  panel one, and an inline style would override the preset. Every other look and feel gets a
     *  hairline and rounded corners in its own colours.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> card( ComponentStyleDelegate<C> it ) {
        if ( Ink.swingTreeIsActive() )
            return it;
        return it.backgroundColor(Ink.card()).border(1, Ink.hairline()).borderRadius(12);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> rail( ComponentStyleDelegate<C> it ) {
        if ( Ink.swingTreeIsActive() )
            return it;
        return it.backgroundColor(Ink.card()).borderAt(UI.Edge.BOTTOM, 1, Ink.hairline());
    }

    private static UIForAnySwing<?, ?> meter( Val<Double> levelDb ) {
        return box().withMinWidth(10)
            .withStyle(levelDb, (db, it) -> it
                .painter(UI.Layer.CONTENT, g -> paintMeter(g, it.componentWidth(), it.componentHeight(), db))
            );
    }

    private static void paintMeter( Graphics2D g, int width, int height, double db ) {
        g.setColor(Ink.well());
        g.fillRoundRect(0, 0, width, height, 6, 6);
        double fraction = (db - Channel.SILENCE_DB) / (Channel.FADER_MAX_DB - Channel.SILENCE_DB);
        int lit = (int) Math.round(Math.max(0, Math.min(1, fraction)) * (height - 4));
        int segment = 4;
        for ( int y = 0; y < lit; y += segment ) {
            double segmentDb = Channel.SILENCE_DB + (y / (double) (height - 4)) * (Channel.FADER_MAX_DB - Channel.SILENCE_DB);
            g.setColor(segmentDb > -3 ? Ink.METER_RED : segmentDb > -12 ? Ink.METER_AMBER : Ink.METER_GREEN);
            g.fillRect(2, height - 2 - y - (segment - 1), width - 4, segment - 1);
        }
    }

    /** The response of the three EQ bands, drawn over a logarithmic frequency axis from 20 Hz to 20 kHz. */
    private static void paintEqCurve( Graphics2D g, int width, int height, Channel channel ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Ink.well());
        g.fillRoundRect(0, 0, width, height, 14, 14);
        g.setColor(Ink.withAlpha(Ink.muted(), 90));
        for ( int decade = 1; decade <= 3; decade++ ) {
            int x = (int) Math.round(width * decade / 3.0);
            g.drawLine(x, 6, x, height - 6);
        }
        g.drawLine(8, height / 2, width - 8, height / 2);

        Path2D curve = new Path2D.Double();
        for ( int x = 0; x <= width; x += 2 ) {
            double hz = 20 * Math.pow(1000, x / (double) width);
            double gain = channel.lowGainDb()  * shelf(120, hz, false)
                        + channel.midGainDb()  * bell(channel.midFrequencyHz(), hz)
                        + channel.highGainDb() * shelf(8000, hz, true);
            double y = height / 2.0 - gain / 18.0 * (height / 2.0 - 8);
            if ( x == 0 ) curve.moveTo(x, y); else curve.lineTo(x, y);
        }
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Ink.accent());
        g.draw(curve);
    }

    private static double shelf( double cornerHz, double hz, boolean high ) {
        double ratio = high ? hz / cornerHz : cornerHz / hz;
        return 1.0 / (1.0 + Math.pow(1.0 / ratio, 2));
    }

    private static double bell( double centreHz, double hz ) {
        double octaves = Math.log(hz / centreHz) / Math.log(2);
        return Math.exp(-octaves * octaves * 1.4);
    }

    private static String decibels( double db ) {
        return db <= Channel.SILENCE_DB ? "-∞ dB" : String.format(Locale.ROOT, "%+.1f dB", db);
    }

    private static String meterText( double db ) {
        return db <= Channel.SILENCE_DB ? "peak  -∞" : String.format(Locale.ROOT, "peak %5.1f", db);
    }

    private static String midiLogHtml( sprouts.Tuple<String> lines ) {
        StringBuilder html = new StringBuilder("<html>");
        if ( lines.isEmpty() )
            html.append("Move a send, the pitch wheel or the mod wheel.");
        for ( String line : lines )
            html.append(line.replace("&", "&amp;").replace("<", "&lt;").replace(" ", "&nbsp;")).append("<br>");
        return html.append("</html>").toString();
    }

    // ════════════════════════════════════════════════════════════════════════

    public static void main( String[] args ) {
        FlatDarkLaf.setup();
        Var<MixerViewModel> vm   = Var.of(MixerViewModel.initial());
        Var<Look>           look = Var.of(Look.FLATLAF_DARK);
        MixerEngine engine = new MixerEngine(vm, EventProcessor.DECOUPLED);
        UI.show("Harbor Room — Studio Console", frame ->
            new LookSwitcher(look, () -> UI.use(EventProcessor.DECOUPLED, () -> new MixerView(vm, look))).host()
        );
        engine.start();
        EventProcessor.DECOUPLED.join();
    }
}

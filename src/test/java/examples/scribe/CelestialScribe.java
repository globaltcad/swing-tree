package examples.scribe;

import com.formdev.flatlaf.FlatDarkLaf;
import examples.scribe.CosmosViewModel.Mood;
import examples.scribe.CosmosViewModel.Star;
import org.jspecify.annotations.Nullable;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.api.Layout;
import swingtree.layout.Bounds;
import swingtree.layout.Position;
import swingtree.style.StyledString;
import swingtree.threading.EventProcessor;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static swingtree.UI.*;

/**
 *  <b>The Celestial Scribe</b> — a little demo application that brings together
 *  three of SwingTree's most expressive super-powers under one roof:
 *
 *  <ul>
 *    <li><b>Styled text that flows around children.</b> The manuscript on the
 *        parchment is rendered via the {@code TextConf} API and wraps
 *        elegantly around every star panel, treating them as text obstacles
 *        via {@code obstaclesFromChildren}.</li>
 *    <li><b>{@code Layout.none()} driven entirely from the view model.</b>
 *        The parchment's layout is a {@code Val<Layout>} derived from the
 *        list of {@link Star}s: every position on screen is a direct
 *        function of {@link CosmosViewModel#stars()}. Dragging a star
 *        simply updates the view model; the GUI catches up automatically.</li>
 *    <li><b>Reactive styling and data-oriented MVI.</b> The whole
 *        application state lives in a single immutable
 *        {@link CosmosViewModel}. Mood switches, star editing and selection
 *        are all withers on that object — the view merely renders it.</li>
 *  </ul>
 *
 *  Run {@link #main(String[])} to open the window.
 */
public final class CelestialScribe extends Panel {

    public CelestialScribe( Var<CosmosViewModel> vm ) {

        // ── Property lenses ──────────────────────────────────────────────
        Var<Tuple<Star>> stars      = vm.zoomTo(CosmosViewModel::stars,      CosmosViewModel::withStars);
        Var<String>      manuscript = vm.zoomTo(CosmosViewModel::manuscript, CosmosViewModel::withManuscript);
        Var<Mood>        mood       = vm.zoomTo(CosmosViewModel::mood,       CosmosViewModel::withMood);

        of(this).withLayout("fill, wrap 1, insets 0")
        .withPrefSize(1180, 780)
        .withStyle( it -> it
            .gradient("backdrop", g -> g
                .type(GradientType.RADIAL)
                .boundary(ComponentBoundary.OUTER_TO_EXTERIOR)
                .size(Math.max(it.componentWidth(), it.componentHeight()))
                .offset(it.componentWidth() * 0.25, it.componentHeight() * 0.15)
                .colors(backdropCenter(mood.get()), backdropEdge(mood.get()))
                .clipTo(ComponentArea.BODY)
            )
        )
        .add("growx",
            topBar(vm, mood)
        )
        .add("grow, push",
            splitPane(Align.HORIZONTAL).withDividerAt(760)
            .add(
                parchment(vm, stars, manuscript, mood)
            )
            .add(
                inspector(vm)
            )
        );
    }

    // ── Top bar ──────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> topBar( Var<CosmosViewModel> vm, Var<Mood> mood ) {
        return panel("fill, insets 14 24 14 24")
            .withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 110)) )
            .add("pushx, growx",
                html(
                    "<span style='color:#f8f8ff;font-family:serif;font-size:20px;'>The Celestial Scribe</span>" +
                    "<span style='color:#cbd5e1;font-style:italic;font-size:12px;'>  —  a movable manuscript of lights</span>"
                )
            )
            .add("shrinkx",
                button(mood.viewAsString(m -> "☾  Mood: " + m.pretty()))
                .withStyle( it -> it.backgroundColor(new Color(255, 255, 255, 25)).borderRadius(18).padding(6, 14, 6, 14).margin(0, 4, 0, 4).foregroundColor(Color.WHITE))
                .onClick( it -> vm.update(CosmosViewModel::nextMood) )
            )
            .add("shrinkx",
                button("✦  Add Star")
                .withStyle( it -> it.backgroundColor(new Color(120, 190, 255, 200)).borderRadius(18).padding(6, 14, 6, 14).margin(0, 4, 0, 4))
                .onClick( it -> vm.update(CosmosViewModel::addStar) )
            )
            .add("shrinkx",
                button("×  Remove Selected")
                .withStyle( it -> it.backgroundColor(new Color(255, 140, 160, 200)).borderRadius(18).padding(6, 14, 6, 14).margin(0, 4, 0, 4))
                .onClick( it -> vm.update(CosmosViewModel::removeSelected) )
            );
    }

    // ── Parchment: the page of text with the stars scattered across it ───

    private static UIForAnySwing<?,?> parchment(
        Var<CosmosViewModel> vm,
        Var<Tuple<Star>>     stars,
        Var<String>          manuscript,
        Var<Mood>            mood
    ) {
        // A reactive layout: positions are a pure function of the star tuple.
        // Every wither on the view model re-derives this and reinstalls the
        // Layout.none() with the new child bounds — no imperative setBounds()
        // anywhere in the application code.
        Val<Layout> parchmentLayout = stars.viewAs(Layout.class, CelestialScribe::layoutFor);

        return panel("fill, insets 18").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
            .add("grow, push",
                scrollPane().add(
                    box().withPrefSize(680, 1120)
                    .withLayout(parchmentLayout)
                    .withRepaintOn(stars, manuscript, mood)
                    .withStyle( it -> it
                        .margin(8)
                        .padding(40, 46, 40, 46)
                        .backgroundColor(pageColor(mood.get()))
                        .borderRadius(14)
                        .shadowColor(new Color(0, 0, 0, 140))
                        .shadowBlurRadius(22)
                        .shadowSpreadRadius(-4)
                        .shadowOffset(0, 8)
                        .gradient("grain", g -> g
                            .colors(pageColor(mood.get()), pageColor(mood.get()).shade(-0.06))
                            .span(Span.TOP_TO_BOTTOM)
                            .clipTo(ComponentArea.BODY)
                        )
                        .text( t -> t
                            .content(buildStyledManuscript(manuscript.get(), mood.get()))
                            .placement(Placement.TOP_LEFT)
                            .placementBoundary(ComponentBoundary.INTERIOR_TO_CONTENT)
                            .wrapLines(true)
                            .autoPreferredHeight(false)
                            .obstaclesFromChildren(ComponentBoundary.EXTERIOR_TO_BORDER)
                            .font( f -> f.size(14).color(inkColor(mood.get())).family("Serif") )
                        )
                    )
                    .addAll(stars, (Var<Star> starVar) -> starPanel(vm, starVar))
                )
            );
    }

    // ── A single star: draggable, self-styling, updates the view model ───

    private static UIForAnySwing<?,?> starPanel( Var<CosmosViewModel> vm, Var<Star> starVar ) {

        UUID id = starVar.get().id();
        Val<Double> hue = starVar.viewAsDouble(Star::hue);
        Val<Double> brightness = starVar.viewAsDouble(Star::brightness);
        Val<String> name = starVar.viewAsString(Star::name);

        return panel().id("star-" + id)
            // Seed the component with the VM's current bounds. From that moment
            // on, the Val<Layout> on the parent keeps it in sync.
            .withBounds(starVar.get().bounds())
            .withCursor(Cursor.MOVE)
            .withRepaintOn(hue, brightness)
            .withStyle( it -> {
                double   hueDeg = hue.get() * 360;
                UI.Color core   = UI.Color.ofHsb(hueDeg, 0.35, 1.00);
                Color    mid    = UI.Color.ofHsb(hueDeg, 0.65, 0.90);
                Color    rim    = UI.Color.ofHsb(hueDeg, 0.80, 0.70);
                boolean selected = id.equals(vm.get().selectedStarId());
                return it
                    .borderRadius(1000)
                    .backgroundColor(new Color(0, 0, 0, 0))
                    .shadowColor(withAlpha(mid, (int) (120 + 100 * brightness.get())))
                    .shadowBlurRadius((int) (14 + 22 * brightness.get()))
                    .shadowSpreadRadius(2)
                    .shadowIsInset(false)
                    .gradient(Layer.BACKGROUND, "corona", g -> g
                        .type(GradientType.RADIAL)
                        .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .clipTo(ComponentArea.BODY)
                        .size(Math.min(it.componentWidth(), it.componentHeight()) * brightness.get() + 12)
                        .colors(
                            core.blend(Color.WHITE, 0.15 * brightness.get()),
                            withAlpha(mid, (int) (200 * brightness.get())),
                            withAlpha(rim, 0)
                        )
                    )
                    .border(selected ? 2.5 : 1.0, selected ? Color.WHITE : withAlpha(rim, 180))
                    .text( t -> t
                        .content(name.get())
                        .placement(Placement.CENTER)
                        .clipTo(ComponentArea.BODY)
                        .font( f -> f.size(11).weight(2).color(Color.WHITE).family("Serif") )
                    );
            })
            .onMousePress( e -> {
                e.forComponent(Component::repaint);
                vm.update( v -> v.selectStar(id) );
            })
            .onMouseDrag( e -> {
                Position dragStart = e.initialComponentPosition();
                double nx = e.deltaXSinceStart() + dragStart.x();
                double ny = e.deltaYSinceStart() + dragStart.y();
                starVar.update( s -> s.withBounds(
                    s.bounds()
                            .withX(Math.max(0, nx))
                            .withY(Math.max(0, ny))
                ));
            });
    }

    // ── Inspector side-panel: edits the selected star ────────────────────

    private static UIForAnySwing<?,?> inspector( Var<CosmosViewModel> vm ) {
        Var<Star> selectedStar = vm.zoomToNullable(Star.class, CosmosViewModel::selectedStar, CosmosViewModel::withSelectedStar);
        Val<String> selectedName = selectedStar.viewAsString((@Nullable Star s) -> s == null ? "— no star selected —" : s.name());

        Var<String> name = selectedStar.zoomTo("", Star::name, Star::withName);
        Var<Double> hue = selectedStar.zoomTo(0.0, Star::hue, Star::withHue);
        Var<Double> brightness = selectedStar.zoomTo(1.0, Star::brightness, Star::withBrightness);
        Var<Bounds> bounds = selectedStar.zoomTo(Bounds.none(), Star::bounds, Star::withBounds);
        Var<Double> size = bounds.zoomTo(80.0,
                            b -> b.size().width().orElse(80f).doubleValue(),
                            (b, w) -> b.withSize(w, w)
                        );

        return panel("fill, wrap 1, insets 18").withStyle( it -> it
                .backgroundColor(new Color(0, 0, 0, 90))
            )
            .add("growx",
                html("<span style='color:#fff;font-family:serif;font-size:16px;'>✦ Inspector</span>")
            )
            .add("growx, gaptop 8",
                label(selectedName)
                .withStyle( it -> it.foregroundColor(new Color(220, 225, 240)).componentFont(f -> f.size(14).family("Serif").posture(0.1f)) )
            )
            .add("growx, gaptop 14",
                panel("fill, wrap 1, insets 0").withStyle( it -> it
                    .padding(12).borderRadius(24)
                )
                .add("growx",
                    panel("fill, wrap 1, insets 0")
                    .add("growx", label("Name"))
                    .add("growx", textField(name))
                )
                .add("growx, gaptop 10",
                    panel("fill, wrap 1, insets 0")
                    .add("growx", label("Hue"))
                    .add("growx", slider(Align.HORIZONTAL, 0.0, 1.0, hue))
                )
                .add("growx, gaptop 10",
                    panel("fill, wrap 1, insets 0")
                    .add("growx", label("Brightness"))
                    .add("growx", slider(Align.HORIZONTAL, 0.0, 1.0, brightness))
                )
                .add("growx, gaptop 10",
                    panel("fill, wrap 1, insets 0")
                    .add("growx", label("Size"))
                    .add("growx", slider(Align.HORIZONTAL, 30.0, 160.0, size))
                )
            )
            .add("growx, gaptop 18",
                label(vm.viewAsString(v -> "Stars on the page: " + v.stars().size()))
                .withStyle( it -> it.foregroundColor(new Color(180, 190, 210)).componentFont(f -> f.size(11).family("SansSerif")) )
            )
            .add("growx, gaptop 4",
                label(vm.viewAsString(v -> "Mood: " + v.mood().pretty()))
                .withStyle( it -> it.foregroundColor(new Color(180, 190, 210)).componentFont(f -> f.size(11).family("SansSerif")) )
            )
            .add("push, growy", box());
    }

    // ── Layout derivation: tuple-of-stars → Layout.none() with bounds ────

    private static Layout layoutFor( Tuple<Star> stars ) {
        Layout.None none = Layout.none();
        for ( int i = 0; i < stars.size(); i++ )
            none = none.withChildBound(i, stars.get(i).bounds());
        return none;
    }

    // ── Manuscript styling: turns plain markdown-ish text into ──────────
    //    a Tuple<StyledString> that adapts to the current Mood.

    private static final Pattern INLINE = Pattern.compile("(\\*\\*([^*]+)\\*\\*)|(\\*([^*]+)\\*)");

    private static Tuple<StyledString> buildStyledManuscript( String raw, Mood mood ) {
        Color ink      = inkColor(mood);
        Color heading  = headingColor(mood);
        Color subtle   = withAlpha(ink, 165);
        List<StyledString> out = new ArrayList<>();

        String[] lines = raw.split("\n", -1);
        for ( int li = 0; li < lines.length; li++ ) {
            String line = lines[li];
            boolean last = li == lines.length - 1;
            String suffix = last ? "" : "\n";

            if ( line.startsWith("# ") ) {
                out.add(StyledString.of(f -> f.size(30).weight(2).color(heading).family("Serif"), line.substring(2) + suffix));
            } else if ( line.startsWith("## ") ) {
                out.add(StyledString.of(f -> f.size(20).weight(2).color(heading).family("Serif"), line.substring(3) + suffix));
            } else if ( line.startsWith("*") && line.endsWith("*") && line.length() > 2 && !line.startsWith("**") ) {
                // A whole italic line — used for the subtitle
                String body = line.substring(1, line.length() - 1);
                out.add(StyledString.of(f -> f.size(12).color(subtle).family("Serif").posture(0.2f), body + suffix));
            } else if ( line.isEmpty() ) {
                out.add(StyledString.of(f -> f.size(14).family("Serif"), suffix));
            } else {
                appendInline(out, line + suffix, ink);
            }
        }
        return Tuple.of(StyledString.class, out);
    }

    private static void appendInline( List<StyledString> out, String line, Color ink ) {
        Matcher m = INLINE.matcher(line);
        int cursor = 0;
        while ( m.find() ) {
            if ( m.start() > cursor ) {
                final String plain = line.substring(cursor, m.start());
                out.add(StyledString.of(f -> f.size(14).color(ink).family("Serif"), plain));
            }
            if ( m.group(1) != null ) {
                final String bold = m.group(2);
                out.add(StyledString.of(f -> f.size(14).color(ink).family("Serif").weight(2), bold));
            } else {
                final String italic = m.group(4);
                out.add(StyledString.of(f -> f.size(14).color(ink).family("Serif").posture(0.25f), italic));
            }
            cursor = m.end();
        }
        if ( cursor < line.length() ) {
            final String tail = line.substring(cursor);
            out.add(StyledString.of(f -> f.size(14).color(ink).family("Serif"), tail));
        }
    }

    // ── Palette helpers ──────────────────────────────────────────────────

    private static UI.Color pageColor( Mood m ) {
        switch ( m ) {
            case DAWN:     return UI.Color.ofRgb(250, 232, 205);  // warm ivory
            case DUSK:     return UI.Color.ofRgb(237, 220, 230);  // cool lavender-ivory
            case MIDNIGHT:
            default:       return UI.Color.ofRgb( 18,  22,  46);  // deep navy parchment
        }
    }

    private static Color inkColor( Mood m ) {
        switch ( m ) {
            case DAWN:     return new Color( 55,  38,  24);
            case DUSK:     return new Color( 52,  32,  66);
            case MIDNIGHT:
            default:       return new Color(220, 228, 255);  // silvery ink
        }
    }

    private static Color headingColor( Mood m ) {
        switch ( m ) {
            case DAWN:     return new Color(134,  61,  30);
            case DUSK:     return new Color( 94,  44, 120);
            case MIDNIGHT:
            default:       return new Color(255, 214, 120);  // candle-gold
        }
    }

    private static Color backdropCenter( Mood m ) {
        switch ( m ) {
            case DAWN:     return new Color(120,  70,  80);
            case DUSK:     return new Color( 58,  44, 100);
            case MIDNIGHT:
            default:       return new Color( 16,  20,  50);
        }
    }

    private static Color backdropEdge( Mood m ) {
        switch ( m ) {
            case DAWN:     return new Color( 30,  15,  40);
            case DUSK:     return new Color( 16,  12,  40);
            case MIDNIGHT:
            default:       return new Color(  3,   4,  12);
        }
    }

    private static Color withAlpha( Color c, int alpha ) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // ── main ─────────────────────────────────────────────────────────────

    public static void main( String[] args ) {
        FlatDarkLaf.setup();
        Var<CosmosViewModel> vm = Var.of(new CosmosViewModel());
        UI.show("The Celestial Scribe", frame -> new CelestialScribe(vm));
        EventProcessor.DECOUPLED.join();
    }
}
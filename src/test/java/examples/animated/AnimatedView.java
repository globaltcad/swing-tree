package examples.animated;
import java.util.Locale;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.animation.LifeTime;
import swingtree.layout.FlowCell;
import swingtree.threading.EventProcessor;

import javax.swing.JFrame;
import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

/**
 *  <h2>Animation Playground</h2>
 *
 *  <p>A small, themed demo dedicated to one thing only: showing off
 *  <b>SwingTree's animation API</b> across a handful of focused recipes.
 *  Each recipe in the left sidebar swaps the stage on the right and the
 *  code peek at the bottom — the same primitives every time
 *  (<code>animateFor(..)</code>, <code>status.progress()</code>,
 *  <code>status.fadeIn()</code>, <code>status.pulse()</code>,
 *  <code>it.paint(status, g -&gt; ...)</code>), used for very different effects.</p>
 *
 *  <p>The whole UI is driven by a single <code>Var&lt;Recipe&gt;</code>
 *  property; switching recipes is one <code>onClick(..)</code> away and the
 *  stage rebinds itself via the property-bound <code>add(Val, viewSupplier)</code>
 *  overload — a tiny taste of the MVI / MVL pattern documented in the
 *  <i>Functional MVVM</i> wiki guide.</p>
 *
 *  <p>The playground is also <b>convergent</b>: the body is a responsive
 *  12-column grid ({@code withFlowLayout()} plus {@code AUTO_SPAN(..)}), so the
 *  recipe sidebar sits beside the stage while there is room and folds into a
 *  single scrolling column when there is not — no breakpoint state, no resize
 *  listener, no second version of the view.</p>
 */
public final class AnimatedView extends Panel
{
    // ── Palette ──────────────────────────────────────────────────────────────

    private static final Color BG          = new Color( 24,  26,  38);
    private static final Color BG_SOFT     = new Color( 34,  38,  54);
    private static final Color BG_SIDEBAR  = new Color( 18,  20,  30);
    private static final Color BG_CARD     = new Color( 42,  46,  64);
    private static final Color STAGE_BG    = new Color( 28,  31,  46);
    private static final Color BG_CODE     = new Color( 12,  14,  22);
    private static final Color INK         = new Color(232, 236, 252);
    private static final Color INK_FAINT   = new Color(150, 162, 198);
    private static final Color ACCENT      = new Color(120, 176, 238);
    private static final Color ACCENT_WARM = new Color(255, 180, 100);
    private static final Color ACCENT_HOT  = new Color(255, 110, 140);
    private static final Color ACCENT_LIME = new Color(160, 224, 130);

    // ── The recipes available in the sidebar ─────────────────────────────────

    private enum Recipe {
        COLOR_PULSE   ("Color Pulse",        "On-hover background fade using status.fadeIn() / status.fadeOut()."),
        RING_BUFFER   ("Ring-Buffer Text",   "Shift a string circularly while progress goes from 0 to 1."),
        SLIDE_SNAP    ("Slide & Snap",       "Drive setBounds(..) from progress for a snap-to-edge handle."),
        CLICK_RIPPLE  ("Click Ripple",       "Custom rendering via it.paint(status, g -> ...) — expanding circles."),
        EASING        ("Easing Comparison",  "Same duration, three curves: linear vs. ease-in vs. ease-out."),
        SEQUENCE      ("Multi-Stage",        "One 2-second animation split into three visual phases via slice(..)."),
        AMBIENT       ("Ambient Loop",       "An infinite UI.animateFor(..).asLongAs(_ -> true) driving the header glow.");

        final String label;
        final String blurb;
        Recipe(String label, String blurb) { this.label = label; this.blurb = blurb; }
    }

    // ── The responsive span table — the whole "convergence" config ───────────
    //
    // A size category is a fraction of the grid's *reference width*, so these are
    // relative bands rather than pixel breakpoints. Below MEDIUM the stage would
    // be squeezed into ~300px next to the sidebar, so both claim all 12 columns
    // and the page becomes one scrolling column.

    private static final FlowCell SIDEBAR_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(4).veryLarge(3).oversize(3) );

    private static final FlowCell STAGE_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(8).veryLarge(9).oversize(9) );

    // Every grid declares the width at which it considers itself "full", since
    // that is what its size categories are measured against. Declaring it is also
    // what lets grids nest: the recipe list below would otherwise report the width
    // of all seven buttons in a single row as the reference width of *this* grid.
    private static final int PAGE_REFERENCE_WIDTH = 1000;

    public AnimatedView(JFrame frame) {
        Var<Recipe> selected = Var.of(Recipe.COLOR_PULSE);

        of(this).withLayout("fill, wrap 1, insets 0, gap 0").withPrefSize(1000, 700)
        .withStyle( it -> it.backgroundColor(BG) )
        .add("growx, wmin 0",     header())
        .add("grow, push, wmin 0", body(selected));
    }

    // ── The page: a scrolling 12-column grid holding the two cards ───────────
    //
    // A flow grid gives every row the height of its tallest child, so once the
    // cards stack the page outgrows the window — hence the scroll pane.

    private static UIForAnySwing<?,?> body(Var<Recipe> selected) {
        return scrollPane( conf -> conf.fitWidth(true) )
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withVerticalScrollIncrement(24)
            .withStyle( it -> it.backgroundColor(BG).borderWidth(0).padding(0) )
            .add(
                panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 18, 18)
                .withMinSize(0, 0) // a grid reports the SUM of its children's minimums
                .withPrefSize(PAGE_REFERENCE_WIDTH, 0) // the reference width, see above
                .withStyle( it -> it.backgroundColor(BG) )
                .add(SIDEBAR_SPAN, sidebar(selected))
                .add(STAGE_SPAN,   mainCard(selected))
            );
    }

    // ── Header — single title strip, no gimmicks ─────────────────────────────
    //
    // Title and subtitle as two labels rather than one html(..) string: html
    // re-wraps instead of ellipsizing, so it just gets clipped when it runs out
    // of room.

    private static UIForAnySwing<?,?> header() {
        return panel("fill, insets 16 26 16 26, gap 0")
            .withStyle( it -> it
                .backgroundColor(BG_SIDEBAR)
                .borderAt(Edge.BOTTOM, 1, new Color(60, 70, 100))
            )
            .add("shrinkx",
                label("Animation Playground")
                .withStyle( it -> it
                    .foregroundColor(new Color(0xF3, 0xF4, 0xFF))
                    .componentFont( f -> f.family("Serif").size(21) )
                )
            )
            .add("pushx, growx, wmin 0",
                label("  —  a tour of SwingTree's animation primitives")
                .withStyle( it -> it
                    .foregroundColor(INK_FAINT)
                    .componentFont( f -> f.family("SansSerif").size(12).posture(0.15f) )
                )
            )
            .add("shrinkx, gapleft 12",
                label("● live")
                .withStyle( it -> it
                    .foregroundColor(ACCENT_LIME)
                    .componentFont( f -> f.family("Monospaced").size(11).posture(0.1f) )
                )
            );
    }

    // ── Sidebar: a grid of recipes, nested inside the page grid ──────────────
    //
    // As a sidebar this list gets about a third of its reference width, which
    // lands it in SMALL, where every button claims a full row — a vertical list.
    // Once the page stacks, the same list is handed the whole window width and
    // the buttons spread out two, three and finally four across. Hence a
    // reference width much wider than the sidebar itself ever gets.

    private static final int RECIPES_REFERENCE_WIDTH = 700;

    private static final FlowCell HEADING_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(12).veryLarge(12).oversize(12) );

    private static final FlowCell RECIPE_SPAN = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(6).large(6).veryLarge(4).oversize(3) );

    private static UIForAnySwing<?,?> sidebar(Var<Recipe> selected) {
        return panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 6, 6)
            .withPrefSize(RECIPES_REFERENCE_WIDTH, 0)
            .withMinSize(0, 0)
            .withStyle( it -> it.backgroundColor(BG_SIDEBAR).borderRadius(14).padding(8) )
            .add(HEADING_SPAN,
                label("RECIPES").withStyle( it -> it
                    .foregroundColor(INK_FAINT)
                    .componentFont( f -> f.family("SansSerif").size(10).weight(2).spacing(0.12f) )
                    .padding(2, 6, 2, 6)
                )
            )
            .apply( ui -> {
                for (Recipe r : Recipe.values())
                    ui.add(RECIPE_SPAN, recipeButton(r, selected));
            });
    }

    private static UIForAnySwing<?,?> recipeButton(Recipe recipe, Var<Recipe> selected) {
        Val<Boolean> active = selected.viewAs(Boolean.class, r -> r == recipe);
        return button(recipe.label)
            .withStyle( active, (on, it) -> it
                .backgroundColor(on ? new Color(120, 176, 238, 55) : new Color(255, 255, 255, 10))
                .foregroundColor(on ? Color.WHITE : INK)
                .borderRadius(10)
                .padding(10, 14, 10, 14)
                .margin(0)
                .borderAt(Edge.LEFT, 3, on ? ACCENT : new Color(0, 0, 0, 0))
                .componentFont( f -> f.family("SansSerif").size(13).weight(on ? 2 : 1) )
            )
            .onClick( it -> selected.set(recipe) );
    }

    // ── Main card: title strip · stage · code panel — all in one container ──

    private static UIForAnySwing<?,?> mainCard(Var<Recipe> selected) {
        return panel("fill, wrap 1, insets 0, gap 0")
            .withStyle( it -> it
                .backgroundColor(BG_SOFT)
                .borderRadius(14)
                .shadowColor(new Color(0, 0, 0, 120))
                .shadowBlurRadius(14)
                .shadowSpreadRadius(-4)
                .shadowOffset(0, 6)
            )
            // Title strip
            .add("growx",
                panel("fill, insets 14 20 14 20, gap 0")
                .withStyle( it -> it.backgroundColor(BG_CARD)
                    .borderRadiusAt(Corner.TOP_LEFT, 14, 14)
                    .borderRadiusAt(Corner.TOP_RIGHT, 14, 14)
                )
                .add("pushx, growx, wmin 0, wrap",
                    label(selected.viewAsString(r -> r.label))
                    .withStyle( it -> it
                        .foregroundColor(INK)
                        .componentFont( f -> f.family("Serif").size(18).weight(2) )
                    )
                )
                .add("pushx, growx, wmin 0",
                    label(selected.viewAsString(r -> r.blurb))
                    .withStyle( it -> it
                        .foregroundColor(INK_FAINT)
                        .componentFont( f -> f.family("SansSerif").size(12).posture(0.05f) )
                    )
                )
            )
            // Stage (binds to selected — rebuilds only this region on switch).
            // "height 350::" is a floor: the stages prefer between ~140 and ~342px,
            // and without it the card would jump on every recipe switch.
            .add("grow, push, height 350::, wmin 0",
                panel("fill, insets 0")
                .withStyle( it -> it.backgroundColor(STAGE_BG) )
                .add("grow, push, wmin 0", selected, AnimatedView::stageFor)
            )
            // Code panel — fixed-height scroll region so long snippets scroll
            // instead of pushing the layout out
            .add("growx",
                panel("fill, wrap 1, insets 0, gap 0")
                .withStyle( it -> it.backgroundColor(BG_CODE)
                    .borderRadiusAt(Corner.BOTTOM_LEFT, 14, 14)
                    .borderRadiusAt(Corner.BOTTOM_RIGHT, 14, 14)
                )
                // Fake editor "tab" header
                .add("growx",
                    panel("fill, insets 6 14 6 14")
                    .withStyle( it -> it
                        .backgroundColor(BG_CARD)
                        .borderAt(Edge.BOTTOM, 1, new Color(80, 90, 120, 80))
                    )
                    .add("shrinkx, wmin 0",
                        label(selected.viewAsString(r -> "// " + r.label.toLowerCase(Locale.ROOT).replace(' ', '-') + ".java"))
                        .withStyle( it -> it
                            .foregroundColor(INK_FAINT)
                            .componentFont( f -> f.family("Monospaced").size(10).posture(0.15f) )
                        )
                    )
                )
                .add("growx, height 140!, wmin 0",
                    scrollPane()
                    // A scroll pane inherits its view's preferred width, and the
                    // widest snippet line is ~500px of monospaced text — which would
                    // otherwise decide every size category of the grid above.
                    .withPrefSize(380, 140)
                    .withStyle( it -> it
                        .padding(0) // -> removes the look and feel border
                        .borderRadius(0)
                        .backgroundColor(BG_CODE)
                    )
                    .withVerticalScrollIncrement(12)
                    .add(
                        textArea(selected.viewAsString(AnimatedView::snippetFor))
                        .isEditableIf(false)
                        .peek( ta -> {
                            ta.setLineWrap(false);
                        })
                        .withStyle( it -> it
                            .padding(12,16,14,16)
                            .foregroundColor(new Color(190, 215, 250))
                            .componentFont( f -> f.family("Monospaced").size(12) )
                            .backgroundColor(BG_CODE)
                        )
                    )
                )
            );
    }

    // ── Stage dispatch ───────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> stageFor(Recipe recipe) {
        switch (recipe) {
            case COLOR_PULSE:  return colorPulseStage();
            case RING_BUFFER:  return ringBufferStage();
            case SLIDE_SNAP:   return slideSnapStage();
            case CLICK_RIPPLE: return clickRippleStage();
            case EASING:       return easingComparisonStage();
            case SEQUENCE:     return multiStageStage();
            case AMBIENT:
            default:           return ambientLoopStage();
        }
    }

    // ── Recipe 1: Color Pulse — withTransitionalStyle drives a colour ───────
    //
    // Note: we cannot just call setForeground(..) from a per-frame lambda here —
    // the styler below re-runs on every repaint and would immediately reset
    // the colour. The right pattern is to drive the colour *through* the styler:
    // a Var<Boolean> "hovered" flag is animated between 0 and 1 by
    // withTransitionalStyle(..), and the styler blends the two endpoint colours
    // by the current animation progress.

    private static UIForAnySwing<?,?> colorPulseStage() {
        Var<Boolean> hovered = Var.of(false);
        return centeredStage(
            label("Hover the card").withStyle(AnimatedView::stageSubtitle),
            panel("fill, insets 0").withPrefSize(280, 140)
            .withStyle( it -> it.backgroundColor(BG_CARD).borderRadius(18).padding(28) )
            .add("center",
                label("◉")
                .withTransitionalStyle(hovered, LifeTime.of(0.4, TimeUnit.SECONDS), (state, it) -> it
                    .componentFont( f -> f
                        .size(56)
                        .color(blend(ACCENT, ACCENT_HOT, state.progress()))
                    )
                )
                .onMouseEnter( it -> hovered.set(true)  )
                .onMouseExit(  it -> hovered.set(false) )
            )
        );
    }

    // ── Recipe 2: Ring-Buffer Text — circular character shift ────────────────

    private static UIForAnySwing<?,?> ringBufferStage() {
        String text = "  SwingTree animations are one method call away. They run on the EDT, are cheap, and compose.  ";
        return centeredStage(
            label("Click the strip — it ticks for ~6 seconds").withStyle(AnimatedView::stageSubtitle),
            label(text)
            .withStyle( it -> it
                .backgroundColor(BG_CARD)
                .foregroundColor(ACCENT_LIME)
                .componentFont( f -> f.family("Monospaced").size(14) )
                .padding(18, 22, 18, 22)
                .borderRadius(10)
            )
            .onMouseClick( it ->
                it.animateFor(6, TimeUnit.SECONDS, s -> {
                    int shift = (int) (text.length() * s.progress()) % text.length();
                    it.getComponent().setText(text.substring(shift) + text.substring(0, shift));
                })
            )
        );
    }

    // ── Recipe 3: Slide & Snap — absolute positioning driven by progress ─────

    private static UIForAnySwing<?,?> slideSnapStage() {
        Var<Boolean> right = Var.of(false);
        return centeredStage(
            label("Toggle the switch — the handle eases to the other side").withStyle(AnimatedView::stageSubtitle),
            box("fill").withPrefSize(360, 60)
            .peek( b -> b.setLayout(null) )
            .withStyle( it -> it.backgroundColor(BG_CARD).borderRadius(30) )
            .add(
                toggleButton("●")
                .withStyle( it -> it
                    .backgroundColor(ACCENT)
                    .foregroundColor(Color.WHITE)
                    .borderRadius(28)
                    .componentFont( f -> f.size(22).weight(2) )
                )
                .peek( b -> UI.runLater(() -> b.setBounds(8, 8, 44, 44)) )
                .onClick( it -> {
                    right.set(it.get().isSelected());
                    int trackW = it.getParent().getWidth();
                    int handle = 44;
                    int from   = it.getComponent().getX();
                    int to     = right.get() ? (trackW - handle - 8) : 8;
                    it.animateFor(0.45, TimeUnit.SECONDS, s -> {
                        double eased = easeInOutCubic(s.progress());
                        int x = (int) (from + (to - from) * eased);
                        it.getComponent().setBounds(x, 8, handle, handle);
                    });
                })
            )
        );
    }

    // ── Recipe 4: Click Ripple — it.paint(status, g -> ...) ──────────────────

    private static UIForAnySwing<?,?> clickRippleStage() {
        return centeredStage(
            label("Click anywhere on the canvas").withStyle(AnimatedView::stageSubtitle),
            panel("fill, insets 0").withPrefSize(380, 220)
            .withStyle( it -> it.backgroundColor(BG_CARD).borderRadius(14) )
            .onMouseClick( it -> it.animateFor(1.2, TimeUnit.SECONDS, s -> {
                it.paint(s, g -> {
                    g.setColor(new Color(120, 176, 238, (int) (200 * s.fadeOut())));
                    for ( int i = 0; i < 5; i++ ) {
                        double r = 280 * s.fadeIn() * ( 1 - i * 0.18 );
                        double x = it.mouseX() - r / 2;
                        double y = it.mouseY() - r / 2;
                        g.drawOval((int) x, (int) y, (int) r, (int) r);
                    }
                });
            }))
        );
    }

    // ── Recipe 5: Easing Comparison — three balls, three curves ──────────────

    private static UIForAnySwing<?,?> easingComparisonStage() {
        Var<Double> p = Var.of(0.0);
        return centeredStage(
            label("Press \"Race\" — same duration, three different curves").withStyle(AnimatedView::stageSubtitle),
            panel("fill, wrap 1, insets 0, gap 10").withStyle( it -> it.backgroundColor(new Color(0, 0, 0, 0)) )
            .add("growx", easeRow("linear",     ACCENT,      p, x -> x))
            .add("growx", easeRow("ease-in",    ACCENT_WARM, p, x -> x * x * x))
            .add("growx", easeRow("ease-out",   ACCENT_HOT,  p, x -> 1 - Math.pow(1 - x, 3)))
            .add("growx", easeRow("ease-in-out",ACCENT_LIME, p, AnimatedView::easeInOutCubic))
            .add("center, gaptop 8",
                button("▶  Race")
                .withStyle( it -> it
                    .backgroundColor(ACCENT)
                    .foregroundColor(Color.WHITE)
                    .borderRadius(20)
                    .padding(8, 18, 8, 18)
                )
                .onClick( it ->
                    UI.animateFor(1.6, TimeUnit.SECONDS).go( s -> p.set(s.progress()) )
                )
            )
        );
    }

    private static UIForAnySwing<?,?> easeRow(
            String name, Color color, Val<Double> rawProgress,
            java.util.function.DoubleUnaryOperator curve
    ) {
        return panel("fill, insets 0").withPrefHeight(40)
            .withStyle( it -> it.backgroundColor(BG_CARD).borderRadius(8).padding(4, 10, 4, 10) )
            .add("width 90!",
                label(name).withStyle( it -> it
                    .foregroundColor(INK_FAINT)
                    .componentFont( f -> f.family("Monospaced").size(11) )
                )
            )
            .add("grow, push",
                box().withPrefHeight(28)
                .withStyle( rawProgress, (progress, it) -> {
                    double eased = curve.applyAsDouble(progress);
                    int w = it.componentWidth();
                    int dot = 22;
                    double x = (w - dot) * eased;
                    return it
                        .backgroundColor(new Color(0, 0, 0, 0))
                        .painter(Layer.CONTENT, g -> {
                            g.setColor(new Color(255, 255, 255, 25));
                            g.fillRoundRect(0, 12, w, 4, 4, 4);
                            g.setColor(color);
                            g.fillOval((int) x, 3, dot, dot);
                        });
                })
            );
    }

    // ── Recipe 6: Multi-Stage — one timer, three phases via slice() ──────────

    private static UIForAnySwing<?,?> multiStageStage() {
        Var<Double> p = Var.of(0.0);
        return centeredStage(
            label("One 2 s animation, sliced into grow → spin → fade phases").withStyle(AnimatedView::stageSubtitle),
            panel("fill, wrap 1, insets 0").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
            .add("center, push",
                box().withPrefSize(180, 180)
                .withStyle( p, (prog, it) -> {
                    // Phase 1 (0.0–0.35): grow from 0 to full
                    double growT  = clamp((prog - 0.00) / 0.35);
                    // Phase 2 (0.35–0.75): full-size pulse + hue shift
                    double pulseT = clamp((prog - 0.35) / 0.40);
                    // Phase 3 (0.75–1.00): fade out
                    double fadeT  = clamp((prog - 0.75) / 0.25);

                    double scale  = easeOutBack(growT) * (1 - 0.15 * Math.sin(pulseT * Math.PI));
                    int alpha     = (int) (255 * (1 - fadeT));
                    Color core    = blend(ACCENT, ACCENT_HOT, pulseT);
                    int size      = (int) (160 * Math.max(0, scale));

                    return it
                        .backgroundColor(new Color(0, 0, 0, 0))
                        .painter(Layer.CONTENT, g -> {
                            int cx = it.componentWidth() / 2;
                            int cy = it.componentHeight() / 2;
                            Color c = new Color(core.getRed(), core.getGreen(), core.getBlue(),
                                                Math.max(0, Math.min(255, alpha)));
                            g.setColor(c);
                            g.fillRoundRect(cx - size / 2, cy - size / 2, size, size, 24, 24);
                        });
                })
            )
            .add("center",
                button("▶  Play sequence")
                .withStyle( it -> it
                    .backgroundColor(ACCENT)
                    .foregroundColor(Color.WHITE)
                    .borderRadius(20)
                    .padding(8, 18, 8, 18)
                )
                .onClick( it ->
                    UI.animateFor(2.0, TimeUnit.SECONDS).go( s -> p.set(s.progress()) )
                )
            )
        );
    }

    // ── Recipe 7: Ambient loop — animateFor(..).asLongAs(_ -> true) ──────────

    private static UIForAnySwing<?,?> ambientLoopStage() {
        Var<Double> phase = Var.of(0.0);
        UI.animateFor(3, TimeUnit.SECONDS).asLongAs( s -> true ).go( s -> phase.set(s.cycle()) );
        return centeredStage(
            label("This loop has no off-switch — that's the point").withStyle(AnimatedView::stageSubtitle),
            box().withPrefSize(260, 260)
            .withStyle( phase, (t, it) -> {
                double cx = it.componentWidth()  * 0.5;
                double cy = it.componentHeight() * 0.5;
                double radius = 60 + 40 * t;
                Color glow = blend(ACCENT, ACCENT_WARM, t);
                return it
                    .backgroundColor(new Color(0, 0, 0, 0))
                    .gradient(Layer.BACKGROUND, "halo", g -> g
                        .type(GradientType.RADIAL)
                        .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                        .offset(cx, cy).size(radius)
                        .colors(glow.brighter(), glow, new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0))
                        .clipTo(ComponentArea.BODY)
                    )
                    .shadowColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 90 + (int)(120 * t)))
                    .shadowBlurRadius((int) (12 + 40 * t))
                    .shadowSpreadRadius(2)
                    .shadowIsInset(false);
            })
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> centeredStage(UIForAnySwing<?,?> subtitle, UIForAnySwing<?,?> body) {
        // "wmin 0" all the way down, or the longest caption on any stage becomes
        // the window's minimum width and the grid never reaches its narrow bands.
        return panel("fill, wrap 1, insets 24").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
            .add("center, gapbottom 14, wmin 0", subtitle)
            .add("center, grow, push, wmin 0",   body);
    }

    private static swingtree.style.ComponentStyleDelegate<javax.swing.JLabel> stageSubtitle(
            swingtree.style.ComponentStyleDelegate<javax.swing.JLabel> it
    ) {
        return it
            .foregroundColor(INK_FAINT)
            .componentFont( f -> f.size(13).family("SansSerif").posture(0.1f) );
    }

    private static Color blend(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(a.getRed()   * (1 - t) + b.getRed()   * t);
        int g = (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl= (int) Math.round(a.getBlue()  * (1 - t) + b.getBlue()  * t);
        return new Color(r, g, bl);
    }

    private static double clamp(double x) { return Math.max(0, Math.min(1, x)); }

    private static double easeInOutCubic(double t) {
        return t < 0.5
                ? 4 * t * t * t
                : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        double u  = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    /**
     *  The snippet of code displayed in the code panel for the selected recipe.
     *  Each snippet is intentionally close to the real code in this file so a
     *  curious reader can read the panel, then jump to the matching recipe
     *  method to see the full context.
     */
    private static String snippetFor(Recipe r) {
        switch (r) {
            case COLOR_PULSE: return join(
                "Var<Boolean> hovered = Var.of(false);",
                "",
                "label(\"◉\")",
                "  .withTransitionalStyle(",
                "      hovered, LifeTime.of(0.4, SECONDS),",
                "      (state, it) -> it.componentFont( f -> f",
                "          .size(56)",
                "          .color(blend(ACCENT, ACCENT_HOT, state.progress()))",
                "      )",
                "  )",
                "  .onMouseEnter( it -> hovered.set(true)  )",
                "  .onMouseExit(  it -> hovered.set(false) );"
            );
            case RING_BUFFER: return join(
                "String text = \"  SwingTree animations are one method call away. ...  \";",
                "",
                "label(text).onMouseClick( it -> it.animateFor(6, SECONDS, s -> {",
                "    int shift = (int) (text.length() * s.progress()) % text.length();",
                "    String shifted = text.substring(shift) + text.substring(0, shift);",
                "    it.getComponent().setText(shifted);",
                "}));"
            );
            case SLIDE_SNAP: return join(
                "toggleButton(\"●\").onClick( it -> {",
                "    int trackW = it.getParent().getWidth();",
                "    int from   = it.getComponent().getX();",
                "    int to     = it.get().isSelected() ? (trackW - 44 - 8) : 8;",
                "    it.animateFor(0.45, SECONDS, s -> {",
                "        double eased = easeInOutCubic(s.progress());",
                "        int x = (int) (from + (to - from) * eased);",
                "        it.getComponent().setBounds(x, 8, 44, 44);",
                "    });",
                "});"
            );
            case CLICK_RIPPLE: return join(
                "panel().onMouseClick( it -> it.animateFor(1.2, SECONDS, s -> {",
                "    it.paint(s, g -> {",
                "        g.setColor(new Color(120, 176, 238, (int)(200 * s.fadeOut())));",
                "        for (int i = 0; i < 5; i++) {",
                "            double r = 280 * s.fadeIn() * (1 - i * 0.18);",
                "            double x = it.mouseX() - r / 2;",
                "            double y = it.mouseY() - r / 2;",
                "            g.drawOval((int) x, (int) y, (int) r, (int) r);",
                "        }",
                "    });",
                "}));"
            );
            case EASING: return join(
                "Var<Double> p = Var.of(0.0);",
                "",
                "// One timer drives every row.",
                "button(\"Race\").onClick( it ->",
                "    UI.animateFor(1.6, SECONDS).go( s -> p.set(s.progress()) )",
                ");",
                "",
                "// Each row picks its own curve from the same progress value,",
                "// bound to the style so it repaints automatically:",
                "box().withStyle( p, (progress, it) -> it",
                "    .painter(Layer.CONTENT, g -> {",
                "        double eased = curve.applyAsDouble(progress);",
                "        int x = (int) ((it.componentWidth() - 22) * eased);",
                "        g.setColor(rowColor);",
                "        g.fillOval(x, 3, 22, 22);",
                "    })",
                ");"
            );
            case SEQUENCE: return join(
                "Var<Double> p = Var.of(0.0);",
                "",
                "// One 2-second timer split into three visual phases.",
                "button(\"Play\").onClick( it ->",
                "    UI.animateFor(2.0, SECONDS).go( s -> p.set(s.progress()) )",
                ");",
                "",
                "box().withStyle( p, (prog, it) -> {",
                "    double growT  = clamp((prog - 0.00) / 0.35);  // phase 1",
                "    double pulseT = clamp((prog - 0.35) / 0.40);  // phase 2",
                "    double fadeT  = clamp((prog - 0.75) / 0.25);  // phase 3",
                "    double scale  = easeOutBack(growT) * (1 - 0.15 * Math.sin(pulseT * Math.PI));",
                "    int    alpha  = (int) (255 * (1 - fadeT));",
                "    Color  core   = blend(ACCENT, ACCENT_HOT, pulseT);",
                "    return it.painter(Layer.CONTENT, g -> draw(g, scale, core, alpha));",
                "});"
            );
            case AMBIENT:
            default: return join(
                "Var<Double> phase = Var.of(0.0);",
                "",
                "// asLongAs( _ -> true ) — runs forever, no off-switch.",
                "UI.animateFor(3, SECONDS)",
                "  .asLongAs( s -> true )",
                "  .go( s -> phase.set(s.cycle()) );",
                "",
                "// Any style bound to the phase property will",
                "// breathe along with the timer:",
                "box().withStyle( phase, (t, it) -> it",
                "    .shadowBlurRadius((int) (12 + 40 * t))",
                "    .shadowColor(blend(ACCENT, ACCENT_WARM, t))",
                ");"
            );
        }
    }

    /** Tiny helper so each snippet reads as a list of lines, not a string-concat soup. */
    private static String join(String... lines) {
        return String.join("\n", lines);
    }

    // ── main ────────────────────────────────────────────────────────────────

    public static void main(String... args) {
        FlatDarkLaf.setup();
        UI.show("Animation Playground", f -> new AnimatedView(f));
        EventProcessor.DECOUPLED.join();
    }
}
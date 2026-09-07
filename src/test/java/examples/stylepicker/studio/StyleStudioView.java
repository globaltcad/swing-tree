package examples.stylepicker.studio;
import java.time.ZoneId;
import java.util.Locale;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForPanel;
import swingtree.animation.LifeTime;
import swingtree.threading.EventProcessor;

import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

/**
 *  <b>The SwingTree Style Studio</b> — a successor to the {@code BoxShadowPicker}
 *  that designs a whole reusable <i>look-and-feel</i> rather than a single
 *  component style.
 *
 *  <h2>The big idea</h2>
 *  The studio edits a {@link StyleConfig}: a complete style for every
 *  {@link StyleTarget} — both the high-level {@link Look} groups (frame, card,
 *  header, …) <i>and</i> Swing component types ({@code JButton}, {@code JSlider},
 *  …). A coherent style sheet needs both, so the selector hosts both. The
 *  studio's own chrome and a live showcase are tagged with those groups and
 *  built inside {@code UI.use(sheet, …)}, so pressing <b>Apply</b> restyles the
 *  tool itself, and untagged widgets in the showcase prove the type rules. The
 *  same config exports as a ready-to-paste {@link swingtree.style.StyleSheet}
 *  subclass (see {@link LookCode}).
 *
 *  <h2>Why MVI / lenses</h2>
 *  All state lives in one immutable {@link StyleStudioViewModel}. Because every
 *  edit produces a fresh value, wrapping the draft in a {@link History} buys
 *  per-edit <b>undo / redo</b> for free, while each <b>Apply</b> records a
 *  {@link Checkpoint}. Editing is <i>not</i> eager: the live preview only changes
 *  on Apply.
 *
 *  <h2>Decluttering</h2>
 *  Each settings panel is <b>foldable</b> (click its header) and starts folded
 *  when that aspect is untouched, so the editor only shows what you've actually
 *  styled. Within a panel, {@code isVisibleIf(..)} hides detail that is currently
 *  irrelevant (e.g. gradient options until the gradient is enabled).
 */
public final class StyleStudioView extends Panel {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final LifeTime FOLD = LifeTime.of(0.3, TimeUnit.SECONDS);

    // Lenses consumed only by raw onChange subscriptions must be held strongly
    // (skill §9c): `applied` drives the live sheet, `target` re-syncs the folds.
    private final Var<StyleConfig> applied;
    private final Var<StyleTarget> target;

    // Per-section fold state. Plain view state (persists across target switches);
    // re-seeded from "is this aspect set?" whenever the target changes.
    private final Var<Boolean> colorsOpen, typoOpen, spacingOpen, borderOpen, gradientOpen, shadowOpen, noiseOpen;

    public StyleStudioView(Var<StyleStudioViewModel> vm, LookSheet sheet) {

        this.target  = vm.zoomTo(StyleStudioViewModel::selectedTarget, StyleStudioViewModel::withSelectedTarget);
        this.applied = vm.zoomTo(StyleStudioViewModel::applied,        StyleStudioViewModel::withApplied);
        Var<GroupStyle> current = vm.zoomTo(StyleStudioViewModel::currentGroupStyle, StyleStudioViewModel::withEditedCurrentGroup);
        Var<Tuple<Checkpoint>> checkpoints = vm.zoomTo(StyleStudioViewModel::checkpoints, StyleStudioViewModel::withCheckpoints);

        Val<Boolean> canUndo = vm.viewAs(Boolean.class, StyleStudioViewModel::canUndo);
        Val<Boolean> canRedo = vm.viewAs(Boolean.class, StyleStudioViewModel::canRedo);
        Val<String>  code    = vm.viewAsString(m -> LookCode.generate(m.draft()));

        GroupStyle init = vm.get().currentGroupStyle();
        colorsOpen   = Var.of(colorsSet(init));
        typoOpen     = Var.of(init.typo().isSet());
        spacingOpen  = Var.of(spacingSet(init));
        borderOpen   = Var.of(init.border().isSet());
        gradientOpen = Var.of(init.gradient().on());
        shadowOpen   = Var.of(init.shadow().on());
        noiseOpen    = Var.of(init.noise().on());

        // When the user picks a different target, re-seed the fold defaults so
        // the editor again shows only the aspects that target has styled.
        Viewable.cast(target).onChange(From.ALL, it -> syncFolds(vm.get().currentGroupStyle()));
        // Keep the live sheet locked to the applied config.
        Viewable.cast(applied).onChange(From.ALL, it ->
            UI.run(() -> sheet.setConfig(it.currentValue().orElseThrowUnchecked()))
        );

        UI.use(sheet, () ->
            of(this).group(Look.FRAME)
            .withLayout("fill, wrap 1, insets 0, gap 0")
            .withPrefSize(1180, 820)
            .add("growx", header(vm, canUndo, canRedo))
            .add("grow, push",
                splitPane(Axis.HORIZONTAL).withDivisionOf(0.46)
                .add(editor(current))
                .add(rightTabs(vm, checkpoints, code))
            )
            .add("growx", footer(vm))
        );

        sheet.setConfig(vm.get().applied());
    }

    private void syncFolds(GroupStyle gs) {
        colorsOpen.set(colorsSet(gs));
        typoOpen.set(gs.typo().isSet());
        spacingOpen.set(spacingSet(gs));
        borderOpen.set(gs.border().isSet());
        gradientOpen.set(gs.gradient().on());
        shadowOpen.set(gs.shadow().on());
        noiseOpen.set(gs.noise().on());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Header
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> header(Var<StyleStudioViewModel> vm, Val<Boolean> canUndo, Val<Boolean> canRedo) {
        return panel("fill, ins 0", "[grow][]").group(Look.HEADER)
            .add("growx",
                box("fill, wrap 1, ins 0").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add(label("SwingTree · Style Studio").group(Look.HEADING))
                .add(label("Design a look-and-feel as a reusable StyleSheet — then watch this very window wear it.")
                        .group(Look.CAPTION))
            )
            .add("right",
                box("ins 0").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add(button("↶  Undo").group(Look.BUTTON).isEnabledIf(canUndo)
                        .onClick(it -> vm.update(StyleStudioViewModel::undo)))
                .add(button("↷  Redo").group(Look.BUTTON).isEnabledIf(canRedo)
                        .onClick(it -> vm.update(StyleStudioViewModel::redo)))
                .add(button("✔  Apply").group(Look.PRIMARY_BUTTON)
                        .onClick(it -> vm.update(m -> m.apply(LocalTime.now(ZoneId.systemDefault()).format(CLOCK)))))
            );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Editor — full-coverage, foldable style editor for the selected target
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> editor(Var<GroupStyle> current) {

        Var<ColorSet>   colors   = current.zoomTo(GroupStyle::colors,   GroupStyle::withColors);
        Var<Typo>       typo     = current.zoomTo(GroupStyle::typo,     GroupStyle::withTypo);
        Var<Pad>        padding  = current.zoomTo(GroupStyle::padding,  GroupStyle::withPadding);
        Var<Pad>        margin   = current.zoomTo(GroupStyle::margin,   GroupStyle::withMargin);
        Var<BorderConf> border   = current.zoomTo(GroupStyle::border,   GroupStyle::withBorder);
        Var<Grad>       gradient = current.zoomTo(GroupStyle::gradient, GroupStyle::withGradient);
        Var<Shade>      shadow   = current.zoomTo(GroupStyle::shadow,   GroupStyle::withShadow);
        Var<Grain>      noise    = current.zoomTo(GroupStyle::noise,    GroupStyle::withNoise);

        // "Is this aspect styled?" — drives both the fold dot and the dim/highlight.
        Val<Boolean> colorsSetV   = current.viewAs(Boolean.class, StyleStudioView::colorsSet);
        Val<Boolean> typoSetV     = current.viewAs(Boolean.class, gs -> gs.typo().isSet());
        Val<Boolean> spacingSetV  = current.viewAs(Boolean.class, StyleStudioView::spacingSet);
        Val<Boolean> borderSetV   = current.viewAs(Boolean.class, gs -> gs.border().isSet());
        Val<Boolean> gradientSetV = current.viewAs(Boolean.class, gs -> gs.gradient().on());
        Val<Boolean> shadowSetV   = current.viewAs(Boolean.class, gs -> gs.shadow().on());
        Val<Boolean> noiseSetV    = current.viewAs(Boolean.class, gs -> gs.noise().on());

        return panel("fill, wrap 1, ins 12").group(Look.SURFACE)
            .add("growx",
                panel("fillx, ins 0", "[shrink][grow]").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add(label("Styling:"))
                .add("growx", comboBox(target, StyleTarget.all()).group(Look.INPUT))
            )
            .add("growx, shrinky, gaptop 2",
                label("Groups are high-level roles you tag; types match every widget of a class.")
                .group(Look.CAPTION)
            )
            .add("grow, push",
                scrollPane(it -> it.fitWidth(true))
                .add(
                    panel("fill, wrap 1, ins 0").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                    .add("growx", foldable("Colors",            colorsOpen,   colorsSetV,   colorSection(colors)))
                    .add("growx", foldable("Typography",        typoOpen,     typoSetV,     typoSection(typo)))
                    .add("growx", foldable("Spacing",           spacingOpen,  spacingSetV,  spacingSection(padding, margin)))
                    .add("growx", foldable("Border & corners",  borderOpen,   borderSetV,   borderSection(border)))
                    .add("growx", foldable("Gradient overlay",  gradientOpen, gradientSetV, gradientSection(gradient)))
                    .add("growx", foldable("Shadow",            shadowOpen,   shadowSetV,   shadowSection(shadow)))
                    .add("growx", foldable("Noise overlay",     noiseOpen,    noiseSetV,    noiseSection(noise)))
                )
            );
    }

    /** A titled panel that folds (animated height) when its header is clicked. */
    private UIForAnySwing<?,?> foldable(String title, Var<Boolean> open, Val<Boolean> isSet, UIForAnySwing<?,?> body) {
        return panel("fill, wrap 1, ins 0").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
            .add("growx",
                panel("fillx, ins 0", "[grow][shrink]").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add("growx",
                    button(open.viewAsString(o -> (o ? "▾   " : "▸   ") + title))
                    .withStyle(it -> it
                        .backgroundColor(new Color(0, 0, 0, 14))
                        .borderRadius(8).padding(7, 12, 7, 12).margin(3, 0, 1, 0)
                        .foregroundColor(new Color(45, 55, 75))
                        .cursor(UI.Cursor.HAND))
                    .peek(b -> b.setHorizontalAlignment(SwingConstants.LEFT))
                    .onClick(it -> open.set(From.VIEW, !open.get()))
                )
                .add(label(isSet.viewAsString(set -> set ? "●" : ""))
                    .withStyle(it -> it.foregroundColor(new Color(59, 130, 246)).padding(0, 8, 0, 6)))
            )
            .add("growx, gapleft 6",
                panel("fill, wrap 1, ins 0 6 4 6").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add("growx", body)
                .withTransitionalStyle(open, FOLD, (status, style) -> {
                    double f = status.fadeIn();
                    if (f >= 1.0) return style;                       // fully open: natural height, fits dynamic content
                    double h = style.componentPrefHeight() * f;
                    return style.minHeight(h).maxHeight(h);
                })
            );
    }

    // ── Section bodies (no titles — the foldable header supplies the title) ──

    private UIForPanel<JPanel> body() {
        return panel("fill, wrap 1, ins 4, hidemode 3").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)));
    }

    private UIForAnySwing<?,?> colorSection(Var<ColorSet> colors) {
        Var<Color> bg = colors.zoomToNullable(Color.class, ColorSet::background, ColorSet::withBackground);
        Var<Color> fd = colors.zoomToNullable(Color.class, ColorSet::foundation, ColorSet::withFoundation);
        Var<Color> fg = colors.zoomToNullable(Color.class, ColorSet::foreground, ColorSet::withForeground);
        return body()
            .add("growx", colorRow("Background", bg))
            .add("growx", colorRow("Foundation", fd))
            .add("growx", colorRow("Foreground", fg));
    }

    private UIForAnySwing<?,?> typoSection(Var<Typo> typo) {
        Var<String> family  = typo.zoomTo(Typo::family,  Typo::withFamily);
        Var<Integer> size   = typo.zoomTo(Typo::size,    Typo::withSize);
        Var<Double> weight  = typo.zoomTo(Typo::weight,  Typo::withWeight);
        Var<Double> posture = typo.zoomTo(Typo::posture, Typo::withPosture);
        Var<Double> spacing = typo.zoomTo(Typo::spacing, Typo::withSpacing);
        Var<Color>  color   = typo.zoomToNullable(Color.class, Typo::color, Typo::withColor);
        return body()
            .add("growx", comboRow("Family", comboBox(family, Tuple.of("", "SansSerif", "Serif", "Monospaced"))))
            .add("growx", intRow("Size",    size,    0, 48))
            .add("growx", dblRow("Weight",  weight,  0.0, 4.0))
            .add("growx", dblRow("Posture", posture, 0.0, 0.5))
            .add("growx", dblRow("Spacing", spacing, -0.2, 0.5))
            .add("growx", colorRow("Font color", color));
    }

    private UIForAnySwing<?,?> spacingSection(Var<Pad> padding, Var<Pad> margin) {
        return body()
            .add("growx", padEditor("Padding", padding))
            .add("growx", padEditor("Margin",  margin));
    }

    /**
     *  Per-edge / per-corner border editor. A focus combo (EVERY + each side /
     *  corner) decides whether a slider edits all four at once or just one —
     *  the same affordance the old BoxShadowPicker used. The per-edge colour row
     *  is hidden via {@code isVisibleIf} while that edge has no width (nothing to
     *  colour).
     */
    private UIForAnySwing<?,?> borderSection(Var<BorderConf> border) {
        Var<UI.Edge>   edgeFocus   = border.zoomTo(BorderConf::edgeFocus,   BorderConf::withEdgeFocus);
        Var<Integer>   edgeWidth   = border.zoomTo(BorderConf::focusedWidth, BorderConf::withFocusedWidth);
        Var<Color>     edgeColor   = border.zoomToNullable(Color.class, BorderConf::focusedColor, BorderConf::withFocusedColor);
        Var<UI.Corner> cornerFocus = border.zoomTo(BorderConf::cornerFocus, BorderConf::withCornerFocus);
        Var<Integer>   arcWidth    = border.zoomTo(BorderConf::focusedArcWidth,  BorderConf::withFocusedArcWidth);
        Var<Integer>   arcHeight   = border.zoomTo(BorderConf::focusedArcHeight, BorderConf::withFocusedArcHeight);

        Val<Boolean> hasWidth = edgeWidth.viewAs(Boolean.class, w -> w > 0);

        return body()
            .add("growx",
                panel("fill, wrap 1, ins 16 8 8 8, hidemode 3").withBorderTitled("Edges")
                .withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add("growx", comboRow("Edge", comboBox(edgeFocus, StyleStudioView::pretty)))
                .add("growx", intRow("Width", edgeWidth, 0, 16))
                .add("growx", colorRow("Color", edgeColor).isVisibleIf(hasWidth))
            )
            .add("growx, gaptop 8",
                panel("fill, wrap 1, ins 16 8 8 8").withBorderTitled("Corners")
                .withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add("growx", comboRow("Corner", comboBox(cornerFocus, StyleStudioView::pretty)))
                .add("growx", intRow("Arc width",  arcWidth,  0, 48))
                .add("growx", intRow("Arc height", arcHeight, 0, 48))
            );
    }

    private UIForAnySwing<?,?> gradientSection(Var<Grad> gradient) {
        Var<Boolean> on   = gradient.zoomTo(Grad::on,   Grad::withOn);
        Var<UI.GradientType> type = gradient.zoomTo(Grad::type, Grad::withType);
        Var<UI.Span> span = gradient.zoomTo(Grad::span, Grad::withSpan);
        Var<Color> c1     = gradient.zoomTo(Grad::color1, Grad::withColor1);
        Var<Color> c2     = gradient.zoomTo(Grad::color2, Grad::withColor2);
        return body()
            .add("growx", checkBox("Enable gradient", on))
            .add("growx", comboRow("Type", comboBox(type)).isVisibleIf(on))
            .add("growx", comboRow("Span", comboBox(span)).isVisibleIf(on))
            .add("growx", colorRow("Color 1", c1).isVisibleIf(on))
            .add("growx", colorRow("Color 2", c2).isVisibleIf(on));
    }

    private UIForAnySwing<?,?> shadowSection(Var<Shade> shadow) {
        Var<Boolean> on   = shadow.zoomTo(Shade::on,    Shade::withOn);
        Var<Color> color  = shadow.zoomTo(Shade::color, Shade::withColor);
        Var<Integer> blur = shadow.zoomTo(Shade::blur,  Shade::withBlur);
        Var<Integer> spr  = shadow.zoomTo(Shade::spread, Shade::withSpread);
        Var<Integer> ox   = shadow.zoomTo(Shade::offsetX, Shade::withOffsetX);
        Var<Integer> oy   = shadow.zoomTo(Shade::offsetY, Shade::withOffsetY);
        Var<Boolean> inset= shadow.zoomTo(Shade::inset, Shade::withInset);
        return body()
            .add("growx", checkBox("Enable shadow", on))
            .add("growx", colorRow("Color", color).isVisibleIf(on))
            .add("growx", intRow("Blur",     blur, 0, 60).isVisibleIf(on))
            .add("growx", intRow("Spread",   spr, -20, 40).isVisibleIf(on))
            .add("growx", intRow("Offset X", ox, -40, 40).isVisibleIf(on))
            .add("growx", intRow("Offset Y", oy, -40, 40).isVisibleIf(on))
            .add("growx", checkBox("Inset", inset).isVisibleIf(on));
    }

    private UIForAnySwing<?,?> noiseSection(Var<Grain> noise) {
        Var<Boolean> on   = noise.zoomTo(Grain::on, Grain::withOn);
        Var<UI.NoiseType> fn = noise.zoomTo(Grain::function, Grain::withFunction);
        Var<Color> c1     = noise.zoomTo(Grain::color1, Grain::withColor1);
        Var<Color> c2     = noise.zoomTo(Grain::color2, Grain::withColor2);
        Var<Double> scale = noise.zoomTo(Grain::scale, Grain::withScale);
        return body()
            .add("growx", checkBox("Enable noise", on))
            .add("growx", comboRow("Function", comboBox(fn)).isVisibleIf(on))
            .add("growx", colorRow("Color 1", c1).isVisibleIf(on))
            .add("growx", colorRow("Color 2", c2).isVisibleIf(on))
            .add("growx", dblRow("Scale", scale, 0.1, 8.0).isVisibleIf(on));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Right side — live showcase, applied checkpoints, generated code
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> rightTabs(Var<StyleStudioViewModel> vm, Var<Tuple<Checkpoint>> checkpoints, Val<String> code) {
        return tabbedPane()
            .add(tab("Live preview").add(showcase()))
            .add(tab("Checkpoints").add(checkpointsView(vm, checkpoints)))
            .add(tab("StyleSheet code").add(codeView(code)));
    }

    private UIForAnySwing<?,?> showcase() {
        return scrollPane(it -> it.fitWidth(true)).add(
            panel("fill, wrap 1, ins 16").group(Look.SURFACE)
            // ── Group-tagged card ──
            .add("growx",
                panel("fill, wrap 1, ins 0").group(Look.CARD)
                .add(label("Card heading").group(Look.HEADING))
                .add(label("Card, heading and this paragraph are components tagged with a Look "
                         + "group — no per-component styling.").group(Look.TEXT))
                .add(label("a muted caption · drawn with the CAPTION group").group(Look.CAPTION))
                .add("growx, gaptop 8", box().group(Look.SEPARATOR).withPrefHeight(2))
                .add("gaptop 8",
                    box("ins 0").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                    .add(button("Secondary").group(Look.BUTTON))
                    .add(button("Primary action").group(Look.PRIMARY_BUTTON))
                )
                .add("growx, gaptop 8", label("An INPUT-group field:").group(Look.CAPTION))
                .add("growx", textField("Type here…").group(Look.INPUT))
            )
            // ── Type-tagged card: NO group tags, only type(..) rules touch these ──
            .add("growx, gaptop 12",
                panel("fill, wrap 2, ins 0", "[grow][grow]").group(Look.CARD)
                .add("span", label("Styled purely by type").group(Look.HEADING))
                .add("span", label("These carry no group tag — only your type(..) rules style them.").group(Look.CAPTION))
                .add("growx", checkBox("A check box", Var.of(true)))
                .add("growx", button("Plain JButton"))
                .add("span, growx", comboBox(Var.of("Combo box"), Tuple.of("Combo box", "Item B", "Item C")))
                .add("span, growx", slider(Axis.HORIZONTAL, 0, 100).withValue(45))
                .add("span, growx", textField("Plain JTextField"))
            )
            // ── A list ──
            .add("growx, gaptop 12", label("A little list").group(Look.HEADING))
            .add("growx",
                panel("fill, wrap 1, ins 0").group(Look.SURFACE)
                .add("growx", listRow("Aurora Vox", "Crystal Prelude"))
                .add("growx", listRow("Glass Mountain", "Velvet Static"))
                .add("growx", listRow("Hiro & The Tide", "Lanterns Over Kyoto"))
            )
        );
    }

    private UIForAnySwing<?,?> listRow(String a, String b) {
        return panel("fillx, ins 0", "[grow][shrink]").group(Look.LIST_ROW)
            .add("growx", label(b).group(Look.TEXT))
            .add(label(a).group(Look.CAPTION));
    }

    private UIForAnySwing<?,?> checkpointsView(Var<StyleStudioViewModel> vm, Var<Tuple<Checkpoint>> checkpoints) {
        return panel("fill, wrap 1, ins 12").group(Look.SURFACE)
            .add(label("Each Apply records a checkpoint. Restore loads one back into the editable draft.")
                    .group(Look.CAPTION))
            .add("grow, push",
                scrollPanels()
                .addAll(checkpoints, (Var<Checkpoint> entry) -> checkpointCard(vm, entry))
            );
    }

    private UIForAnySwing<?,?> checkpointCard(Var<StyleStudioViewModel> vm, Var<Checkpoint> entry) {
        Checkpoint cp = entry.get();
        return panel("fillx, ins 0", "[grow][shrink]").group(Look.LIST_ROW)
            .add("growx",
                box("fill, wrap 1, ins 0").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add(label("Checkpoint #" + cp.number()).group(Look.TEXT))
                .add(label("applied at " + cp.label()).group(Look.CAPTION))
            )
            .add(button("Restore").group(Look.BUTTON)
                    .onClick(it -> vm.update(m -> m.restore(cp))));
    }

    private UIForAnySwing<?,?> codeView(Val<String> code) {
        return panel("fill, wrap 1, ins 12").group(Look.SURFACE)
            .add("growx",
                box("fillx, ins 0", "[grow][shrink]").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
                .add(label("A ready-to-paste StyleSheet subclass — updates as you edit.").group(Look.CAPTION))
                .add(button("Copy").group(Look.PRIMARY_BUTTON).onClick(it -> {
                    Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                    cb.setContents(new StringSelection(code.get()), null);
                }))
            )
            .add("grow, push",
                scrollPane().add(
                    textArea(code).isEditableIf(false)
                    .withStyle(it -> it.font(new Font("Monospaced", Font.PLAIN, 13)))
                )
            );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Footer
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> footer(Var<StyleStudioViewModel> vm) {
        return panel("fill, ins 0", "[grow][]").group(Look.FOOTER)
            .add("growx", label(vm.viewAsString(m -> "Styling: " + m.selectedTarget().pretty())))
            .add(label(vm.viewAsString(m ->
                "undo " + m.history().undoCount() + " · redo " + m.history().redoCount()
                + " · checkpoints " + m.checkpoints().size())));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Reusable editor fragments
    // ════════════════════════════════════════════════════════════════════════

    private static UIForPanel<JPanel> intRow(String name, Var<Integer> value, int min, int max) {
        return panel("fillx, ins 0", "[90::][grow][36!]").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
            .add(label(name + ":"))
            .add("growx", slider(Axis.HORIZONTAL, min, max, value))
            .add(label(value.viewAsString()));
    }

    private static UIForPanel<JPanel> dblRow(String name, Var<Double> value, double min, double max) {
        return panel("fillx, ins 0", "[90::][grow][36!]").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
            .add(label(name + ":"))
            .add("growx", slider(Axis.HORIZONTAL, min, max, value))
            .add(label(value.viewAsString(d -> String.format("%.2f", d))));
    }

    private static UIForPanel<JPanel> comboRow(String name, UIForAnySwing<?,?> combo) {
        return panel("fillx, ins 0", "[90::][grow]").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
            .add(label(name + ":"))
            .add("growx", combo);
    }

    private static UIForAnySwing<?,?> padEditor(String name, Var<Pad> pad) {
        Var<Integer> top    = pad.zoomTo(Pad::top,    Pad::withTop);
        Var<Integer> right  = pad.zoomTo(Pad::right,  Pad::withRight);
        Var<Integer> bottom = pad.zoomTo(Pad::bottom, Pad::withBottom);
        Var<Integer> left   = pad.zoomTo(Pad::left,   Pad::withLeft);
        return panel("fill, wrap 1, ins 16 8 8 8").withBorderTitled(name)
            .withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
            .add("growx", intRow("Top",    top,    0, 48))
            .add("growx", intRow("Right",  right,  0, 48))
            .add("growx", intRow("Bottom", bottom, 0, 48))
            .add("growx", intRow("Left",   left,   0, 48));
    }

    /** A colour editor row: an editable hex field, a clickable swatch (opens a chooser) and a clear button. */
    private static UIForPanel<JPanel> colorRow(String name, Var<Color> color) {
        return panel("fillx, ins 0", "[90::][grow][26!][22!]").withStyle(it -> it.backgroundColor(new Color(0,0,0,0)))
            .add(label(name + ":"))
            .add("growx",
                textField(color.viewAsString(c -> c == null ? "" : hex(c)))
                .onContentChange(it -> parseHex(it.get().getText()).ifPresent(c -> color.set(From.VIEW, c)))
            )
            .add(
                box().withPrefSize(24, 18)
                .withCursor(UI.Cursor.HAND)
                .withStyle(color, (c, it) ->
                    c == null
                        ? it.backgroundColor(new Color(0,0,0,0)).border(1, Color.GRAY).borderRadius(4)
                        : it.backgroundColor(c).border(1, new Color(0,0,0,80)).borderRadius(4)
                )
                .onMouseClick(it -> {
                    Color cur = color.orElseNull();
                    int alpha = cur == null ? 255 : cur.getAlpha();
                    Color picked = JColorChooser.showDialog(it.get(), "Pick " + name, cur == null ? Color.WHITE : cur);
                    if (picked != null)
                        color.set(From.VIEW, new Color(picked.getRed(), picked.getGreen(), picked.getBlue(), alpha));
                })
            )
            .add(button("✕").onClick(it -> color.set(From.VIEW, null)));
    }

    // ── Small helpers ────────────────────────────────────────────────────────

    private static boolean colorsSet(GroupStyle gs) {
        ColorSet c = gs.colors();
        return c.background() != null || c.foundation() != null || c.foreground() != null;
    }
    private static boolean spacingSet(GroupStyle gs) {
        return !gs.padding().isZero() || !gs.margin().isZero();
    }

    private static String pretty(Enum<?> e) {
        String[] parts = e.name().split("_", -1);
        StringBuilder sb = new StringBuilder();
        for (String p : parts)
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase(Locale.ROOT)).append(' ');
        return sb.toString().trim();
    }

    private static String hex(Color c) {
        return c.getAlpha() == 255
            ? String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue())
            : String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    private static Optional<Color> parseHex(String text) {
        String s = text.trim();
        if (s.startsWith("#")) s = s.substring(1);
        try {
            if (s.length() == 6) {
                int rgb = Integer.parseInt(s, 16);
                return Optional.of(new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            }
            if (s.length() == 8) {
                long v = Long.parseLong(s, 16);
                return Optional.of(new Color((int)((v >> 24) & 0xFF), (int)((v >> 16) & 0xFF),
                                             (int)((v >> 8) & 0xFF), (int)(v & 0xFF)));
            }
        } catch (NumberFormatException ignored) { /* Not a parseable hex colour; fall through to empty. */ }
        return Optional.empty();
    }

    // ════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        FlatLightLaf.setup();
        Var<StyleStudioViewModel> vm = Var.of(StyleStudioViewModel.initial());
        LookSheet sheet = new LookSheet();
        UI.show("SwingTree · Style Studio", f -> new StyleStudioView(vm, sheet));
        EventProcessor.DECOUPLED.join();
    }
}
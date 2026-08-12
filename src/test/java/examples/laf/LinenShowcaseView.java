package examples.laf;

import sprouts.Var;
import swingtree.UI;
import swingtree.style.StyleSheet;
import swingtree.threading.EventProcessor;

import swingtree.UIForAnySwing;
import swingtree.UIForPanel;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static swingtree.UI.*;

/**
 *  A compact showcase application for the {@link LinenLookAndFeel}.
 *  <p>
 *  The view is a single {@link javax.swing.JTabbedPane} with one tab per
 *  family of components Linen ships a UI delegate for. A final tab
 *  demonstrates how an application can layer its own styling on top of
 *  Linen via a {@link StyleSheet} — without touching any LAF source.
 *  <p>
 *  Run it with:
 *  <pre>{@code
 *    java -cp <classpath> examples.laf.LinenShowcaseView
 *  }</pre>
 *
 *  @see LinenLookAndFeel
 */
public final class LinenShowcaseView extends JPanel
{
    /**
     *  Group tag for buttons that should render with the application's
     *  brand accent rather than Linen's neutral cream surface — see the
     *  {@link AccentSheet} below.
     */
    public enum Tag { PRIMARY, DANGER }

    /**
     *  Application entry point: installs the {@link LinenLookAndFeel},
     *  then renders this view inside a SwingTree-managed window.
     */
    public static void main(String... args) {
        UI.show("Linen — a SwingTree-backed Look and Feel", frame -> createView());
        EventProcessor.DECOUPLED.join();
    }

    /**
     *  Installs the {@link LinenLookAndFeel} and builds the showcase under the
     *  {@link AccentSheet}, which is what {@link #main(String...)} shows. It exists so
     *  that callers outside this package - the resize benchmark among them - can build
     *  the very same view, since the style sheet it needs is not visible to them.
     *
     *  @return The showcase view, styled the way the application shows it.
     */
    public static JPanel createView() {
        try {
            UIManager.setLookAndFeel(new LinenLookAndFeel());
        } catch (Exception e) {
            throw new RuntimeException("Could not install LinenLookAndFeel", e);
        }
        return UI.use(new AccentSheet(), LinenShowcaseView::new);
    }

    public LinenShowcaseView() {
        Var<String>  typed     = Var.of("Type something...");
        Var<String>  readOnly  = Var.of("This field is disabled");
        Var<String>  secret    = Var.of("hunter2");
        Var<String>  notes     = Var.of(
                "Multi-line text areas inherit the same\n"
              + "rounded surface, focus accent and\n"
              + "comfortable padding as single-line fields.\n"
              + "\n"
              + "Tab between the inputs to see how the\n"
              + "focus border grows without shifting the\n"
              + "surrounding layout."
        );
        Var<Boolean> agree     = Var.of(true);
        Var<Boolean> subscribe = Var.of(false);
        Var<Integer> progress  = Var.of(62);
        Var<Integer> volume    = Var.of(40);
        Var<String>  skin      = Var.of("Linen");

        UI.of(this).withLayout(FILL.and(WRAP(1)).and(INS(0, 0, 0, 0)))
        .withPrefSize(720, 620)

        // ── Menu bar (top) ───────────────────────────────────────────────
        .add(GROW_X, buildMenuBar())

        // ── Header ───────────────────────────────────────────────────────
        .add("growx, gapleft 20, gapright 20, gaptop 16",
            panel(FILL.and(WRAP(1)))
            .add(label("Linen").withStyle(it -> it
                .componentFont(f -> f.size(26).weight(2f).color(LinenPalette.TEXT))
            ))
            .add(label("a soft, neutral Look-and-Feel built on the SwingTree style engine")
                 .withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED))
            )
        )

        // ── Tabs ─────────────────────────────────────────────────────────
        .add("grow, push, gapleft 20, gapright 20, gaptop 12, gapbottom 20",
            tabbedPane()
            .add(tab("Buttons")  .add(buttonsTab()))
            .add(tab("Choices")  .add(choicesTab(agree, subscribe, skin)))
            .add(tab("Text")     .add(textTab(typed, secret, notes, readOnly)))
            .add(tab("Display")  .add(displayTab(progress, volume)))
            .add(tab("Data")     .add(dataTab()))
            .add(tab("Layout")   .add(layoutTab()))
            .add(tab("Style")    .add(styleTab()))
        );
    }

    // ── Tab bodies ────────────────────────────────────────────────────────

    private static UIForPanel<JPanel> buttonsTab() {
        return panel(FILL.and(WRAP(1)).and(INS(16)).and(GAP_REL(12)))
            .add(GROW_X, panel(FLOW_X.and(INS(0)).and(GAP_REL(10)))
                .add(button("Save").peek(b -> b.setToolTipText("Hover to see Linen's tooltip style")))
                .add(button("Cancel"))
                .add(button("Disabled").peek(b -> b.setEnabled(false)))
                .add(toggleButton("Toggle"))
            )
            .add(GROW_X, panel(FLOW_X.and(INS(0)).and(GAP_REL(10)))
                .add(button("Primary").group(Tag.PRIMARY))
                .add(button("Danger").group(Tag.DANGER))
            )
            .add(label("Buttons compensate the growing focus border via shrinking margin, "
                       + "so tabbing through them doesn't shift the layout.")
                 .withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)));
    }

    private static UIForPanel<JPanel> choicesTab(Var<Boolean> agree, Var<Boolean> subscribe, Var<String> skin) {
        return panel(FILL.and(WRAP(2)).and(INS(16)).and(GAP_REL(10)))
            .add(checkBox("Agree to terms", agree))
            .add(checkBox("Subscribe to updates", subscribe))
            .add(radioButton("Light",  "Light", skin))
            .add(radioButton("Linen",  "Linen", skin))
            .add(radioButton("Dark",   "Dark",  skin))
            .add(checkBox("Disabled checkbox").isEnabledIf(false))
            .add("span, growx, gaptop 8", separator())
            .add("span", panel(FLOW_X.and(INS(0)).and(GAP_REL(10)))
                .add(label("Skin:"))
                .add(GROW_X, comboBox("Linen", "Sand", "Bone", "Driftwood"))
                .add(label("Quantity:"))
                .add(GROW_X, spinner(8, 1, 100, 1))
            );
    }

    private static UIForPanel<JPanel> textTab(
            Var<String> typed, Var<String> secret, Var<String> notes, Var<String> readOnly
    ) {
        return panel(FILL.and(WRAP(1)).and(INS(16)).and(GAP_REL(10)))
            .add(GROW_X,
                panel(FILL.and(WRAP(1)).and(INS(0)).and(GAP_REL(8)))
                .add(label("Single-line").withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
                .add(GROW_X, textField(typed))
                .add(GROW_X, passwordField(secret))
                .add(GROW_X, textField(readOnly).peek(t -> { t.setEnabled(false); t.setEditable(false); }))
            )
            .add(GROW.and(PUSH),
                panel(FILL.and(WRAP(1)).and(INS(0)).and(GAP_REL(8)))
                .add(
                    label("Multi-line, inside a scroll pane")
                     .withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED))
                )
                .add(GROW.and(PUSH),
                    scrollPane()
                    .add(textArea(notes).peek(t -> { t.setRows(6); t.setLineWrap(true); t.setWrapStyleWord(true); }))
                )
            );
    }

    private static UIForPanel<JPanel> displayTab(Var<Integer> progress, Var<Integer> volume) {
        return panel(FILL.and(WRAP(1)).and(INS(16)).and(GAP_REL(14)))
            .add(label("Progress").withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
            .add(GROW_X,
                progressBar(Align.HORIZONTAL, progress.viewAsDouble(p -> p / 100.0))
                 .peek(p -> p.setStringPainted(true))
            )
            .add(label("Slider").withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
            .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, volume))
            .add(GROW_X, separator())
            .add(
                label("Separators are simple BORDER_SOFT hairlines.")
                .withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED))
            );
    }

    private static UIForPanel<JPanel> dataTab() {
        String[]   fabrics = { "Linen", "Cotton", "Wool", "Hemp", "Silk", "Cashmere", "Tweed", "Velvet" };
        String[]   columns = { "Fabric", "Weave", "€/m" };
        Object[][] rows    = {
                { "Linen",  "Plain",  "12.50" },
                { "Cotton", "Twill",  "9.20"  },
                { "Wool",   "Boucle", "18.40" },
                { "Silk",   "Satin",  "32.00" },
                { "Hemp",   "Canvas", "7.80"  },
        };

        return panel(FILL.and(WRAP(2)).and(INS(16)).and(GAP_REL(12)), "[grow 35][grow 65]")
            .add(GROW.and(PUSH), panel(FILL.and(WRAP(1)).and(INS(0)).and(GAP_REL(6)))
                .add(label("List").withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
                .add(GROW.and(PUSH),
                    scrollPane().add(
                        UI.list(fabrics).peek(l -> l.setSelectedIndex(0))
                    )
                )
            )
            .add(GROW.and(PUSH), panel(FILL.and(WRAP(1)).and(INS(0)).and(GAP_REL(6)))
                .add(label("Table").withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
                .add(GROW.and(PUSH),
                    scrollPane().add(
                        table(m -> m
                            .colNames(columns)
                            .rowCount(() -> rows.length)
                            .colCount(() -> columns.length)
                            .getsEntryAt((row, col) -> rows[row][col])
                        )
                    )
                )
            )
            .add("span, growx, gaptop 6", separator())
            .add("span, growx",
                label("Tree").withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
            .add("span, grow, push",
                scrollPane().add(of(buildMaterialsTree()))
            );
    }

    /**
     *  Builds the small materials tree used in the Data tab. {@link JTree}
     *  has no SwingTree factory of its own; we wrap the constructed
     *  instance with {@code of(..)} at the call site.
     */
    private static JTree buildMaterialsTree() {
        DefaultMutableTreeNode root   = new DefaultMutableTreeNode("Materials");
        DefaultMutableTreeNode plant  = new DefaultMutableTreeNode("Plant");
        DefaultMutableTreeNode animal = new DefaultMutableTreeNode("Animal");
        for (String s : new String[]{ "Linen", "Cotton", "Hemp" })
            plant.add(new DefaultMutableTreeNode(s));
        for (String s : new String[]{ "Wool", "Silk", "Cashmere" })
            animal.add(new DefaultMutableTreeNode(s));
        root.add(plant);
        root.add(animal);
        JTree tree = new JTree(root);
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
        return tree;
    }

    private static UIForPanel<JPanel> layoutTab() {
        return panel(FILL.and(WRAP(1)).and(INS(16)).and(GAP_REL(12)))
            .add(GROW_X,
                toolBar()
                .add(button("New"))
                .add(button("Open"))
                .add(button("Save"))
                .add(separator(Align.VERTICAL))
                .add(button("Cut"))
                .add(button("Copy"))
                .add(button("Paste"))
            )
            .add(label("Tool-bar above; drag the divider below to resize the split panes.")
                 .withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
            .add(GROW.and(PUSH),
                splitPane(Align.HORIZONTAL).withDivisionOf(0.5)
                .add(splitPanePlaceholder("Left pane"))
                .add(splitPanePlaceholder("Right pane"))
            );
    }

    /**
     *  A small cream panel with a centred muted label — used as filler in
     *  the split-pane demo so each side has visible content.
     */
    private static UIForPanel<JPanel> splitPanePlaceholder(String labelText) {
        return panel(FILL.and(INS(20)))
            .add(CENTER,
                label(labelText).withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED))
            );
    }

    /**
     *  Builds a small menu bar that exercises every flavour of menu item
     *  Linen has a UI delegate for: plain item, check item, radio item,
     *  separator and submenu (with arrow). {@link JMenuBar} has no
     *  SwingTree factory of its own, so the bar itself is wrapped with
     *  {@code of(..)} — its contents are built declaratively.
     */
    private static UIForAnySwing<?, JMenuBar> buildMenuBar() {
        Var<Boolean> showGrid   = Var.of(true);
        Var<Boolean> showRulers = Var.of(false);
        Var<Boolean> at50       = Var.of(false);
        Var<Boolean> at100      = Var.of(true);
        Var<Boolean> at200      = Var.of(false);
        ButtonGroup  zoomGroup  = new ButtonGroup();

        return of(new JMenuBar())
            .add(menu("File").peek(m -> m.setMnemonic(KeyEvent.VK_F))
                .add(menuItem("New").peek(mi -> mi.setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK))))
                .add(menuItem("Open…"))
                .add(menuItem("Save"))
                .add(separator())
                .add(menu("Export to")
                    .add(menuItem("PNG"))
                    .add(menuItem("SVG"))
                    .add(menuItem("PDF"))
                )
                .add(separator())
                .add(menuItem("Quit"))
            )
            .add(menu("View").peek(m -> m.setMnemonic(KeyEvent.VK_V))
                .add(checkBoxMenuItem("Show grid",   showGrid))
                .add(checkBoxMenuItem("Show rulers", showRulers))
                .add(separator())
                .add(radioButtonMenuItem("50%",  at50).peek(zoomGroup::add))
                .add(radioButtonMenuItem("100%", at100).peek(zoomGroup::add))
                .add(radioButtonMenuItem("200%", at200).peek(zoomGroup::add))
            )
            .add(menu("Help"));
    }

    private static UIForPanel<JPanel> styleTab() {
        return panel(FILL.and(WRAP(1)).and(INS(16)).and(GAP_REL(12)))
            .add(label("Layer cascade: StyleSheet → Look-and-Feel → inline withStyle(..)")
                 .withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED)))
            .add(GROW_X, panel(FLOW_X.and(INS(0)).and(GAP_REL(10)))
                .add(button("Default button"))
                .add(
                    button("Chip").withStyle(it -> it
                    .borderRadius(20)
                    .paddingHorizontal(20)
                    .backgroundColor(LinenPalette.ACCENT_SOFT)
                    .foregroundColor(LinenPalette.TEXT)
                    .borderColor(LinenPalette.ACCENT)
                ))
                .add(button("Primary").group(Tag.PRIMARY))
                .add(button("Danger").group(Tag.DANGER))
            )
            .add(
                label("Above: a per-component .withStyle(..) override (the chip), "
                       + "two AccentSheet group rules (Primary, Danger) and a default button — "
                       + "all painted by the same Linen UI delegate."
                )
                 .withStyle(it -> it.foregroundColor(LinenPalette.TEXT_MUTED))
            );
    }

    /**
     *  A minimal example of how an application can layer its own styling
     *  on top of Linen via a {@link StyleSheet}. Components tagged with
     *  {@link Tag#PRIMARY} or {@link Tag#DANGER} receive a coloured fill
     *  while still inheriting Linen's rounded corners, shadow, and focus
     *  ring — the cascade does the merging for us.
     */
    private static final class AccentSheet extends StyleSheet
    {
        private static final Color PRIMARY      = new Color(0x36, 0x5C, 0x3B);
        private static final Color PRIMARY_TEXT = new Color(0xFA, 0xF6, 0xEC);
        private static final Color DANGER       = new Color(0x8B, 0x3A, 0x3A);
        private static final Color DANGER_TEXT  = new Color(0xFA, 0xF6, 0xEC);

        @Override
        protected void configure() {
            add(type(AbstractButton.class).group(Tag.PRIMARY), it -> it
                .backgroundColor(PRIMARY)
                .foregroundColor(PRIMARY_TEXT)
            );
            add(type(AbstractButton.class).group(Tag.DANGER), it -> it
                .backgroundColor(DANGER)
                .foregroundColor(DANGER_TEXT)
            );
        }
    }
}
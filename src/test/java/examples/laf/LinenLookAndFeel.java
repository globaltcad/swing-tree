package examples.laf;

import sprouts.From;
import sprouts.Viewable;
import swingtree.SwingTree;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.Color;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

/**
 *  The <i>Linen</i> Look-and-Feel — a small, warm, slightly textured Swing
 *  theme that delegates most of its painting to the
 *  {@linkplain swingtree.style.ComponentExtension SwingTree style engine}.
 *
 *  <h2>Character</h2>
 *  Linen aims for a calm, paper-like aesthetic: cream surfaces, taupe
 *  borders, a deep olive accent for focus and selection, and a barely
 *  perceptible noise grain on panels that suggests woven fabric. The
 *  palette and noise patterns live in {@link LinenPalette}.
 *
 *  <h2>What it covers</h2>
 *  Linen installs SwingTree-aware
 *  {@link javax.swing.plaf.ComponentUI} delegates for the following
 *  component families:
 *  <ul>
 *      <li>{@link javax.swing.JPanel} — {@link LinenPanelUI}</li>
 *      <li>{@link javax.swing.AbstractButton} (covers {@link javax.swing.JButton}
 *          and {@link javax.swing.JToggleButton}) — {@link LinenButtonUI}</li>
 *      <li>{@link javax.swing.JCheckBox} — {@link LinenCheckBoxUI}</li>
 *      <li>{@link javax.swing.JRadioButton} — {@link LinenRadioButtonUI}</li>
 *      <li>{@link javax.swing.JLabel} — {@link LinenLabelUI}</li>
 *      <li>{@link javax.swing.JTextField} — {@link LinenTextFieldUI}</li>
 *      <li>{@link javax.swing.JPasswordField} — {@link LinenPasswordFieldUI}</li>
 *      <li>{@link javax.swing.JTextArea} — {@link LinenTextAreaUI}</li>
 *      <li>{@link javax.swing.JSeparator} — {@link LinenSeparatorUI}</li>
 *      <li>{@link javax.swing.JToolTip} — {@link LinenToolTipUI}</li>
 *      <li>{@link javax.swing.JProgressBar} — {@link LinenProgressBarUI}</li>
 *      <li>{@link javax.swing.JSlider} — {@link LinenSliderUI}</li>
 *      <li>{@link javax.swing.JScrollBar} — {@link LinenScrollBarUI}</li>
 *      <li>{@link javax.swing.JScrollPane} — {@link LinenScrollPaneUI}</li>
 *      <li>{@link javax.swing.JComboBox} — {@link LinenComboBoxUI}</li>
 *      <li>{@link javax.swing.JSpinner} — {@link LinenSpinnerUI}</li>
 *      <li>{@link javax.swing.JTabbedPane} — {@link LinenTabbedPaneUI}</li>
 *      <li>{@link javax.swing.JFormattedTextField} — {@link LinenFormattedTextFieldUI}</li>
 *      <li>{@link javax.swing.JEditorPane} — {@link LinenEditorPaneUI}</li>
 *      <li>{@link javax.swing.JTextPane} — {@link LinenTextPaneUI}</li>
 *      <li>{@link javax.swing.JViewport} — {@link LinenViewportUI}</li>
 *      <li>{@link javax.swing.JList} — {@link LinenListUI}</li>
 *      <li>{@link javax.swing.JTable} + {@link javax.swing.table.JTableHeader}
 *          — {@link LinenTableUI} / {@link LinenTableHeaderUI}</li>
 *      <li>{@link javax.swing.JTree} — {@link LinenTreeUI}</li>
 *      <li>{@link javax.swing.JToolBar} — {@link LinenToolBarUI}</li>
 *      <li>{@link javax.swing.JSplitPane} — {@link LinenSplitPaneUI}</li>
 *      <li>{@link javax.swing.JMenuBar} / {@link javax.swing.JMenu} /
 *          {@link javax.swing.JMenuItem} / {@link javax.swing.JCheckBoxMenuItem} /
 *          {@link javax.swing.JRadioButtonMenuItem} / {@link javax.swing.JPopupMenu}
 *          — full menu system via {@link LinenMenuBarUI}, {@link LinenMenuUI},
 *          {@link LinenMenuItemUI}, {@link LinenCheckBoxMenuItemUI},
 *          {@link LinenRadioButtonMenuItemUI}, {@link LinenPopupMenuUI}</li>
 *  </ul>
 *  Composite component families like {@link javax.swing.JOptionPane},
 *  {@link javax.swing.JFileChooser} and {@link javax.swing.JColorChooser}
 *  are built out of the components above and inherit the Linen look
 *  automatically — no dedicated UI delegate needed.
 *
 *  <h2>Extending</h2>
 *  Because every Linen UI delegate implements
 *  {@link swingtree.api.laf.SwingTreeStyledComponentUI}, Linen's defaults
 *  sit at the <b>second</b> layer of SwingTree's three-layer style cascade
 *  (sheet → look-and-feel → inline). An application is free to override or
 *  augment any rule with a {@link swingtree.style.StyleSheet} or a per-
 *  component {@code .withStyle(..)} call — Linen never clobbers values
 *  declared above or below it.
 *
 *  <h2>Installation</h2>
 *  <pre>{@code
 *    UIManager.setLookAndFeel(new LinenLookAndFeel());
 *  }</pre>
 *
 *  @see LinenPalette
 *  @see LinenPanelUI
 *  @see LinenButtonUI
 *  @see LinenLabelUI
 *  @see LinenTextFieldUI
 */
public final class LinenLookAndFeel extends BasicLookAndFeel
{
    private static final String PKG = "examples.laf.";

    /** Every UIDefaults key that should receive the resolved default font.
     *  Updated atomically when SwingTree publishes a new default font so
     *  the look stays consistent across the whole component tree. */
    private static final List<String> FONT_KEYS = Arrays.asList(
            "defaultFont",
            "Panel.font", "Button.font", "ToggleButton.font",
            "CheckBox.font", "RadioButton.font",
            "Label.font",
            "TextField.font", "PasswordField.font", "TextArea.font",
            "FormattedTextField.font", "EditorPane.font", "TextPane.font",
            "ProgressBar.font", "Slider.font",
            "ScrollBar.font", "ScrollPane.font", "Viewport.font",
            "ComboBox.font", "Spinner.font",
            "TabbedPane.font",
            "ToolBar.font", "SplitPane.font",
            "ToolTip.font",
            "List.font", "Table.font", "TableHeader.font", "Tree.font",
            "MenuBar.font", "Menu.font", "MenuItem.font",
            "CheckBoxMenuItem.font", "RadioButtonMenuItem.font",
            "PopupMenu.font"
    );

    /** Strong reference to the font-view subscription. Sprouts holds
     *  listeners weakly, so dropping this field would silently break
     *  dynamic font tracking. The LAF instance owns the subscription
     *  for the lifetime of its installation. */
    @SuppressWarnings("FieldCanBeLocal")
    private Viewable<FontUIResource> _fontView;

    @Override public String  getName()                 { return "Linen"; }
    @Override public String  getID()                   { return "Linen"; }
    @Override public String  getDescription()          { return "A soft, neutral, textured look-and-feel built on the SwingTree style engine."; }
    @Override public boolean isNativeLookAndFeel()     { return false; }
    @Override public boolean isSupportedLookAndFeel()  { return true;  }

    /**
     *  Subscribe to SwingTree's authoritative default-font property so
     *  that runtime changes (display-DPI events, an explicit
     *  {@link SwingTree#setUiScaleFactor(float)} call, the OS pushing a
     *  new system font) flow through to every open window without a
     *  restart. When the value changes we re-publish it into every
     *  {@code *.font} key Linen owns and refresh each top-level window
     *  via {@link SwingUtilities#updateComponentTreeUI(java.awt.Component)}.
     */
    @Override
    public void initialize() {
        super.initialize();
        _fontView = SwingTree.get().getDefaultFontView();
        _fontView.onChange(From.ALL, it -> SwingUtilities.invokeLater(() -> {
            it.currentValue().ifPresent(LinenLookAndFeel::_propagateFont);
        }));
    }

    @Override
    public void uninitialize() {
        // Drop the strong reference so the subscription becomes eligible
        // for collection alongside the LAF instance itself.
        _fontView = null;
        super.uninitialize();
    }

    /**
     *  Registers Linen's SwingTree-backed {@link javax.swing.plaf.ComponentUI}
     *  delegates for every component family Linen ships a delegate for.
     *  The basic defaults populated by
     *  {@link BasicLookAndFeel#initClassDefaults(UIDefaults)} stay in place
     *  for anything not covered here ({@code JTable}, {@code JTree}, {@code JList}, {@code JMenu*}).
     */
    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        table.put("PanelUI",            PKG + "LinenPanelUI");
        table.put("ButtonUI",           PKG + "LinenButtonUI");
        table.put("ToggleButtonUI",     PKG + "LinenButtonUI");
        table.put("CheckBoxUI",         PKG + "LinenCheckBoxUI");
        table.put("RadioButtonUI",      PKG + "LinenRadioButtonUI");
        table.put("LabelUI",            PKG + "LinenLabelUI");
        table.put("TextFieldUI",        PKG + "LinenTextFieldUI");
        table.put("PasswordFieldUI",    PKG + "LinenPasswordFieldUI");
        table.put("TextAreaUI",         PKG + "LinenTextAreaUI");
        table.put("SeparatorUI",        PKG + "LinenSeparatorUI");
        table.put("ToolTipUI",          PKG + "LinenToolTipUI");
        table.put("ProgressBarUI",      PKG + "LinenProgressBarUI");
        table.put("SliderUI",           PKG + "LinenSliderUI");
        table.put("ScrollBarUI",        PKG + "LinenScrollBarUI");
        table.put("ScrollPaneUI",       PKG + "LinenScrollPaneUI");
        table.put("ComboBoxUI",         PKG + "LinenComboBoxUI");
        table.put("SpinnerUI",          PKG + "LinenSpinnerUI");
        table.put("TabbedPaneUI",       PKG + "LinenTabbedPaneUI");
        table.put("FormattedTextFieldUI", PKG + "LinenFormattedTextFieldUI");
        table.put("EditorPaneUI",       PKG + "LinenEditorPaneUI");
        table.put("TextPaneUI",         PKG + "LinenTextPaneUI");
        table.put("ViewportUI",         PKG + "LinenViewportUI");
        table.put("ListUI",             PKG + "LinenListUI");
        table.put("TableUI",            PKG + "LinenTableUI");
        table.put("TableHeaderUI",      PKG + "LinenTableHeaderUI");
        table.put("TreeUI",             PKG + "LinenTreeUI");
        table.put("ToolBarUI",          PKG + "LinenToolBarUI");
        table.put("SplitPaneUI",        PKG + "LinenSplitPaneUI");
        table.put("MenuBarUI",          PKG + "LinenMenuBarUI");
        table.put("MenuUI",             PKG + "LinenMenuUI");
        table.put("MenuItemUI",         PKG + "LinenMenuItemUI");
        table.put("CheckBoxMenuItemUI", PKG + "LinenCheckBoxMenuItemUI");
        table.put("RadioButtonMenuItemUI", PKG + "LinenRadioButtonMenuItemUI");
        table.put("PopupMenuUI",        PKG + "LinenPopupMenuUI");
    }

    /**
     *  Maps the legacy AWT system-colour keys to Linen's palette so that
     *  components which read raw system colours (e.g. some third-party
     *  widgets) blend in with the rest of the theme.
     */
    @Override
    protected void initSystemColorDefaults(UIDefaults table) {
        super.initSystemColorDefaults(table);
        table.put("control",            ui(LinenPalette.SURFACE));
        table.put("controlText",        ui(LinenPalette.TEXT));
        table.put("controlHighlight",   ui(LinenPalette.SURFACE_HOVER));
        table.put("controlLtHighlight", ui(LinenPalette.SURFACE_HOVER));
        table.put("controlShadow",      ui(LinenPalette.BORDER));
        table.put("controlDkShadow",    ui(LinenPalette.ACCENT));
        table.put("text",               ui(LinenPalette.SURFACE_FIELD));
        table.put("textText",           ui(LinenPalette.TEXT));
        table.put("textHighlight",      ui(LinenPalette.ACCENT_SOFT));
        table.put("textHighlightText",  ui(LinenPalette.TEXT));
        table.put("textInactiveText",   ui(LinenPalette.TEXT_DISABLED));
        table.put("desktop",            ui(LinenPalette.BACKGROUND));
        table.put("window",             ui(LinenPalette.SURFACE));
        table.put("windowBorder",       ui(LinenPalette.BORDER));
        table.put("windowText",         ui(LinenPalette.TEXT));
        table.put("info",               ui(LinenPalette.SURFACE));
        table.put("infoText",           ui(LinenPalette.TEXT));
    }

    /**
     *  Seeds the component-specific defaults. Linen leans on its
     *  SwingTree UI delegates for all painting; the entries below are
     *  the small subset of keys (background, foreground, font, caret,
     *  selection colours) that other code may read directly via
     *  {@link javax.swing.UIManager}.
     */
    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);

        // The single source of truth: SwingTree owns the authoritative,
        // HiDPI-correctly-sized default font for the active display.
        // Calling SwingTree.get() also bootstraps the library if it
        // hadn't been initialised yet, so this works no matter what
        // order setLookAndFeel(..) and UI.show(..) are called in.
        FontUIResource baseFont = SwingTree.get().getDefaultFont();
        // Pin our resolved font under "defaultFont" so that SwingTree's
        // scale-recompute listener — which fires when Label.font (or
        // defaultFont) changes — derives the SAME scale factor from
        // our font as the one we just installed.
        table.put("defaultFont", baseFont);

        // Panel
        table.put("Panel.background",  ui(LinenPalette.BACKGROUND));
        table.put("Panel.foreground",  ui(LinenPalette.TEXT));
        table.put("Panel.font",        baseFont);

        // Button + ToggleButton
        table.put("Button.background",       ui(LinenPalette.SURFACE));
        table.put("Button.foreground",       ui(LinenPalette.TEXT));
        table.put("Button.font",             baseFont);
        table.put("ToggleButton.background", ui(LinenPalette.SURFACE));
        table.put("ToggleButton.foreground", ui(LinenPalette.TEXT));
        table.put("ToggleButton.font",       baseFont);

        // Label
        table.put("Label.foreground",        ui(LinenPalette.TEXT));
        table.put("Label.disabledForeground",ui(LinenPalette.TEXT_DISABLED));
        table.put("Label.font",              baseFont);

        // TextField
        table.put("TextField.background",          ui(LinenPalette.SURFACE_FIELD));
        table.put("TextField.foreground",          ui(LinenPalette.TEXT));
        table.put("TextField.inactiveForeground",  ui(LinenPalette.TEXT_DISABLED));
        table.put("TextField.caretForeground",     ui(LinenPalette.ACCENT));
        table.put("TextField.selectionBackground", ui(LinenPalette.ACCENT_SOFT));
        table.put("TextField.selectionForeground", ui(LinenPalette.TEXT));
        table.put("TextField.font",                baseFont);

        // PasswordField
        table.put("PasswordField.background",          ui(LinenPalette.SURFACE_FIELD));
        table.put("PasswordField.foreground",          ui(LinenPalette.TEXT));
        table.put("PasswordField.inactiveForeground",  ui(LinenPalette.TEXT_DISABLED));
        table.put("PasswordField.caretForeground",     ui(LinenPalette.ACCENT));
        table.put("PasswordField.selectionBackground", ui(LinenPalette.ACCENT_SOFT));
        table.put("PasswordField.selectionForeground", ui(LinenPalette.TEXT));
        table.put("PasswordField.font",                baseFont);

        // TextArea
        table.put("TextArea.background",          ui(LinenPalette.SURFACE_FIELD));
        table.put("TextArea.foreground",          ui(LinenPalette.TEXT));
        table.put("TextArea.inactiveForeground",  ui(LinenPalette.TEXT_DISABLED));
        table.put("TextArea.caretForeground",     ui(LinenPalette.ACCENT));
        table.put("TextArea.selectionBackground", ui(LinenPalette.ACCENT_SOFT));
        table.put("TextArea.selectionForeground", ui(LinenPalette.TEXT));
        table.put("TextArea.font",                baseFont);

        // CheckBox + RadioButton
        table.put("CheckBox.background",      ui(LinenPalette.BACKGROUND));
        table.put("CheckBox.foreground",      ui(LinenPalette.TEXT));
        table.put("CheckBox.font",            baseFont);
        table.put("CheckBox.icon",            LinenIcons.checkBox());
        table.put("RadioButton.background",   ui(LinenPalette.BACKGROUND));
        table.put("RadioButton.foreground",   ui(LinenPalette.TEXT));
        table.put("RadioButton.font",         baseFont);
        table.put("RadioButton.icon",         LinenIcons.radio());

        // ProgressBar
        table.put("ProgressBar.background",    ui(LinenPalette.SURFACE_DISABLED));
        table.put("ProgressBar.foreground",    ui(LinenPalette.ACCENT));
        table.put("ProgressBar.selectionForeground", ui(LinenPalette.SURFACE));
        table.put("ProgressBar.selectionBackground", ui(LinenPalette.TEXT));
        table.put("ProgressBar.font",          baseFont);

        // Slider
        table.put("Slider.background",         ui(LinenPalette.BACKGROUND));
        table.put("Slider.foreground",         ui(LinenPalette.ACCENT));
        table.put("Slider.tickColor",          ui(LinenPalette.TEXT_MUTED));
        table.put("Slider.focus",              ui(LinenPalette.ACCENT));
        table.put("Slider.font",               baseFont);

        // ScrollBar + ScrollPane.
        //
        // NOTE: We deliberately do NOT install a "ScrollBar.width" integer
        // here. Swing's convention is that the value is in raw component
        // pixels, which would be wrong on HiDPI displays. Linen's scrollbar
        // thickness is driven dynamically by LinenScrollBarUI#getPreferredSize,
        // which scales a 12-developer-pixel constant through UI#scale at
        // every layout pass.
        table.put("ScrollBar.background",      ui(LinenPalette.SURFACE_DISABLED));
        table.put("ScrollBar.foreground",      ui(LinenPalette.BORDER));
        table.put("ScrollBar.thumb",           ui(LinenPalette.BORDER));
        table.put("ScrollBar.thumbDarkShadow", ui(LinenPalette.BORDER));
        table.put("ScrollBar.thumbHighlight",  ui(LinenPalette.BORDER));
        table.put("ScrollBar.thumbShadow",     ui(LinenPalette.BORDER));
        table.put("ScrollBar.track",           ui(LinenPalette.SURFACE_DISABLED));
        table.put("ScrollPane.background",     ui(LinenPalette.SURFACE_FIELD));
        table.put("ScrollPane.foreground",     ui(LinenPalette.TEXT));
        table.put("ScrollPane.font",           baseFont);

        // ComboBox
        table.put("ComboBox.background",            ui(LinenPalette.SURFACE_FIELD));
        table.put("ComboBox.foreground",            ui(LinenPalette.TEXT));
        table.put("ComboBox.selectionBackground",   ui(LinenPalette.ACCENT_SOFT));
        table.put("ComboBox.selectionForeground",   ui(LinenPalette.TEXT));
        table.put("ComboBox.disabledBackground",    ui(LinenPalette.SURFACE_DISABLED));
        table.put("ComboBox.disabledForeground",    ui(LinenPalette.TEXT_DISABLED));
        table.put("ComboBox.font",                  baseFont);

        // Spinner
        table.put("Spinner.background",        ui(LinenPalette.SURFACE_FIELD));
        table.put("Spinner.foreground",        ui(LinenPalette.TEXT));
        table.put("Spinner.font",              baseFont);

        // Separator
        table.put("Separator.background",      ui(LinenPalette.BACKGROUND));
        table.put("Separator.foreground",      ui(LinenPalette.BORDER_SOFT));

        // ToolTip
        table.put("ToolTip.background",        ui(LinenPalette.ACCENT));
        table.put("ToolTip.foreground",        new ColorUIResource(0xFA, 0xF6, 0xEC));
        table.put("ToolTip.font",              baseFont);

        // FormattedTextField + EditorPane + TextPane (same vocab as TextField)
        for (String prefix : new String[]{ "FormattedTextField", "EditorPane", "TextPane" }) {
            table.put(prefix + ".background",          ui(LinenPalette.SURFACE_FIELD));
            table.put(prefix + ".foreground",          ui(LinenPalette.TEXT));
            table.put(prefix + ".inactiveForeground",  ui(LinenPalette.TEXT_DISABLED));
            table.put(prefix + ".caretForeground",     ui(LinenPalette.ACCENT));
            table.put(prefix + ".selectionBackground", ui(LinenPalette.ACCENT_SOFT));
            table.put(prefix + ".selectionForeground", ui(LinenPalette.TEXT));
            table.put(prefix + ".font",                baseFont);
        }

        // Viewport
        table.put("Viewport.background",   ui(LinenPalette.SURFACE_FIELD));
        table.put("Viewport.foreground",   ui(LinenPalette.TEXT));
        table.put("Viewport.font",         baseFont);

        // List
        table.put("List.background",           ui(LinenPalette.SURFACE_FIELD));
        table.put("List.foreground",           ui(LinenPalette.TEXT));
        table.put("List.selectionBackground",  ui(LinenPalette.ACCENT_SOFT));
        table.put("List.selectionForeground",  ui(LinenPalette.TEXT));
        table.put("List.focusCellHighlightBorder",
                javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 6));
        table.put("List.font",                 baseFont);

        // Table + TableHeader
        table.put("Table.background",           ui(LinenPalette.SURFACE_FIELD));
        table.put("Table.foreground",           ui(LinenPalette.TEXT));
        table.put("Table.selectionBackground",  ui(LinenPalette.ACCENT_SOFT));
        table.put("Table.selectionForeground",  ui(LinenPalette.TEXT));
        table.put("Table.gridColor",            ui(LinenPalette.BORDER_SOFT));
        table.put("Table.alternateRowColor",    ui(LinenPalette.SURFACE));
        table.put("Table.font",                 baseFont);
        table.put("TableHeader.background",     ui(LinenPalette.SURFACE));
        table.put("TableHeader.foreground",     ui(LinenPalette.TEXT_MUTED));
        table.put("TableHeader.font",           baseFont);

        // Tree
        table.put("Tree.background",           ui(LinenPalette.SURFACE_FIELD));
        table.put("Tree.foreground",           ui(LinenPalette.TEXT));
        table.put("Tree.textBackground",       ui(LinenPalette.SURFACE_FIELD));
        table.put("Tree.textForeground",       ui(LinenPalette.TEXT));
        table.put("Tree.selectionBackground",  ui(LinenPalette.ACCENT_SOFT));
        table.put("Tree.selectionForeground",  ui(LinenPalette.TEXT));
        table.put("Tree.selectionBorderColor", ui(LinenPalette.ACCENT));
        table.put("Tree.line",                 ui(LinenPalette.BORDER_SOFT));
        table.put("Tree.hash",                 ui(LinenPalette.BORDER_SOFT));
        table.put("Tree.expandedIcon",         LinenIcons.treeExpanded());
        table.put("Tree.collapsedIcon",        LinenIcons.treeCollapsed());
        table.put("Tree.font",                 baseFont);

        // ToolBar
        table.put("ToolBar.background",        ui(LinenPalette.SURFACE));
        table.put("ToolBar.foreground",        ui(LinenPalette.TEXT));
        table.put("ToolBar.dockingBackground", ui(LinenPalette.SURFACE_HOVER));
        table.put("ToolBar.floatingBackground",ui(LinenPalette.SURFACE));
        table.put("ToolBar.font",              baseFont);

        // SplitPane
        table.put("SplitPane.background",        ui(LinenPalette.BACKGROUND));
        table.put("SplitPane.shadow",            ui(LinenPalette.BORDER));
        table.put("SplitPane.darkShadow",        ui(LinenPalette.ACCENT));
        table.put("SplitPane.highlight",         ui(LinenPalette.SURFACE_HOVER));
        table.put("SplitPane.dividerSize",       8);
        table.put("SplitPaneDivider.border",     javax.swing.BorderFactory.createEmptyBorder());

        // Menu system
        table.put("MenuBar.background",            ui(LinenPalette.SURFACE));
        table.put("MenuBar.foreground",            ui(LinenPalette.TEXT));
        table.put("MenuBar.font",                  baseFont);
        table.put("Menu.background",               ui(LinenPalette.SURFACE));
        table.put("Menu.foreground",               ui(LinenPalette.TEXT));
        table.put("Menu.selectionBackground",      ui(LinenPalette.ACCENT_SOFT));
        table.put("Menu.selectionForeground",      ui(LinenPalette.TEXT));
        table.put("Menu.disabledForeground",       ui(LinenPalette.TEXT_DISABLED));
        table.put("Menu.acceleratorForeground",    ui(LinenPalette.TEXT_MUTED));
        table.put("Menu.acceleratorSelectionForeground", ui(LinenPalette.TEXT));
        table.put("Menu.arrowIcon",                LinenIcons.submenuArrow());
        table.put("Menu.font",                     baseFont);
        table.put("MenuItem.background",           ui(LinenPalette.SURFACE_FIELD));
        table.put("MenuItem.foreground",           ui(LinenPalette.TEXT));
        table.put("MenuItem.selectionBackground",  ui(LinenPalette.ACCENT_SOFT));
        table.put("MenuItem.selectionForeground",  ui(LinenPalette.TEXT));
        table.put("MenuItem.disabledForeground",   ui(LinenPalette.TEXT_DISABLED));
        table.put("MenuItem.acceleratorForeground",ui(LinenPalette.TEXT_MUTED));
        table.put("MenuItem.acceleratorSelectionForeground", ui(LinenPalette.TEXT));
        table.put("MenuItem.font",                 baseFont);
        table.put("CheckBoxMenuItem.background",   ui(LinenPalette.SURFACE_FIELD));
        table.put("CheckBoxMenuItem.foreground",   ui(LinenPalette.TEXT));
        table.put("CheckBoxMenuItem.selectionBackground", ui(LinenPalette.ACCENT_SOFT));
        table.put("CheckBoxMenuItem.selectionForeground", ui(LinenPalette.TEXT));
        table.put("CheckBoxMenuItem.disabledForeground",  ui(LinenPalette.TEXT_DISABLED));
        table.put("CheckBoxMenuItem.checkIcon",    LinenIcons.checkBox());
        table.put("CheckBoxMenuItem.font",         baseFont);
        table.put("RadioButtonMenuItem.background",ui(LinenPalette.SURFACE_FIELD));
        table.put("RadioButtonMenuItem.foreground",ui(LinenPalette.TEXT));
        table.put("RadioButtonMenuItem.selectionBackground", ui(LinenPalette.ACCENT_SOFT));
        table.put("RadioButtonMenuItem.selectionForeground", ui(LinenPalette.TEXT));
        table.put("RadioButtonMenuItem.disabledForeground",  ui(LinenPalette.TEXT_DISABLED));
        table.put("RadioButtonMenuItem.checkIcon", LinenIcons.radio());
        table.put("RadioButtonMenuItem.font",      baseFont);
        table.put("PopupMenu.background",          ui(LinenPalette.SURFACE_FIELD));
        table.put("PopupMenu.foreground",          ui(LinenPalette.TEXT));
        table.put("PopupMenu.font",                baseFont);

        // TabbedPane
        table.put("TabbedPane.background",         ui(LinenPalette.BACKGROUND));
        table.put("TabbedPane.foreground",         ui(LinenPalette.TEXT));
        table.put("TabbedPane.selected",           ui(LinenPalette.SURFACE_FIELD));
        table.put("TabbedPane.contentAreaColor",   ui(LinenPalette.BACKGROUND));
        table.put("TabbedPane.borderHightlightColor", ui(LinenPalette.BORDER_SOFT));
        table.put("TabbedPane.darkShadow",         ui(LinenPalette.BORDER));
        table.put("TabbedPane.light",              ui(LinenPalette.SURFACE));
        table.put("TabbedPane.highlight",          ui(LinenPalette.SURFACE_HOVER));
        table.put("TabbedPane.shadow",             ui(LinenPalette.BORDER));
        table.put("TabbedPane.focus",              ui(LinenPalette.ACCENT));
        table.put("TabbedPane.font",               baseFont);
    }

    private static ColorUIResource ui(Color c) { return new ColorUIResource(c); }

    /**
     *  Pushes {@code font} into every {@code *.font} key Linen owns and
     *  asks each top-level window to re-install its component tree so the
     *  on-screen text picks up the change. Runs on the EDT (callers must
     *  schedule it via {@link SwingUtilities#invokeLater(Runnable)}).
     *  <p>
     *  Skips the whole propagation pass if the new font is byte-for-byte
     *  identical to {@code Label.font}'s current value — this is what
     *  makes the install-time first publish a no-op (the LAF already put
     *  the same font there a moment earlier) instead of triggering a
     *  redundant tree-rebuild on every install.
     */
    private static void _propagateFont(FontUIResource font) {
        Object current = UIManager.get("Label.font");
        if (font.equals(current))
            return;
        for (String key : FONT_KEYS)
            UIManager.put(key, font);
        for (Window w : Window.getWindows())
            SwingUtilities.updateComponentTreeUI(w);
    }
}
package examples.laf;

import sprouts.From;
import sprouts.Tuple;
import sprouts.Viewable;
import swingtree.SwingTree;
import swingtree.api.Configurator;
import swingtree.api.Styler;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.Color;
import java.awt.Container;
import java.awt.Window;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  A configurable Swing <i>Look and Feel</i> which paints every component through the
 *  {@linkplain swingtree.style.ComponentExtension SwingTree style engine} instead of through
 *  hand written {@link java.awt.Graphics} code.
 *  <p>
 *  Where a traditional look and feel hard-codes its appearance across dozens of
 *  {@link javax.swing.plaf.ComponentUI} classes, this one keeps it in data: a <b>palette</b> of
 *  named colours, a <b>style preset</b> which is a table of {@link Styler} functions keyed by
 *  component type, and a <b>symbol preset</b> which draws the small geometry no style rule can
 *  express. Swapping any of the three changes the whole application, and an application may
 *  override or extend individual rules without forking anything.
 *
 *  <h2>Installing it</h2>
 *  <pre>{@code
 *    SwingTreeLookAndFeel.initializeUsing( it -> it
 *        .stylePreset(SwingTreeLookAndFeel.StylePreset.LINEN)
 *        .symbolPreset(SwingTreeLookAndFeel.SymbolPreset.LINEN)
 *        .overrideStyle(JButton.class, s -> s.borderRadius(2))       // replaces the preset rule
 *        .addStyle(JTextField.class, s -> s.backgroundColor("blue")) // applied on top of it
 *    );
 *  }</pre>
 *  {@link #initializeUsing(Configurator)} also refreshes every window that is already open, so it
 *  doubles as the way to switch themes at runtime. Configuring nothing yields
 *  {@link StylePreset#LINEN} drawn with {@link SymbolPreset#LINEN} symbols.
 *
 *  <h2>How a component's style is resolved</h2>
 *  A rule is registered against a component <i>type</i> and applies to that type and every
 *  subtype of it; the most specific match wins, which is how {@code JCheckBox} can be styled
 *  differently from the {@code AbstractButton} rule it would otherwise inherit. On top of that an
 *  {@link Conf#overrideStyle(Class, Styler)} rule <b>replaces</b> the preset rule for everything
 *  it matches, and every matching {@link Conf#addStyle(Class, Styler)} rule is then applied over
 *  it in registration order. Between two rules for the very same type, the later one wins.
 *  <p>
 *  The cascade sits at the <b>second</b> of SwingTree's three style layers - after an
 *  application's {@link swingtree.style.StyleSheet}, before a per-component {@code withStyle(..)}.
 *  A look and feel therefore beats a style sheet, which is why the semantic roles an application
 *  wants to ask for are declared here and read back out of the component's style groups:
 *  <pre>{@code
 *    UI.button("Ship it").group(SwingTreeLookAndFeel.Variant.PRIMARY)
 *    UI.panel().group(SwingTreeLookAndFeel.Surface.CARD)
 *  }</pre>
 *
 *  @see StylePreset
 *  @see SymbolPreset
 *  @see Conf
 */
public final class SwingTreeLookAndFeel extends BasicLookAndFeel
{
    /**
     *  The configuration the {@link javax.swing.plaf.ComponentUI} delegates read from.
     *  Swing instantiates those delegates reflectively through {@link UIDefaults}, so there is
     *  no constructor to hand the configuration to; the installed look and feel publishes it
     *  here instead. A look and feel is a process-wide singleton in Swing, so this mirrors a
     *  fact of the platform rather than introducing shared state of its own.
     */
    private static volatile SwingTreeLookAndFeel _active = null;

    /** Where the UI delegate classes live, for the {@link UIDefaults} class-name entries. */
    private static final String PKG = "examples.laf.";

    /** Every {@link UIDefaults} key that should receive the resolved default font. Updated
     *  atomically when SwingTree publishes a new one so the look stays consistent across the
     *  whole component tree. */
    private static final List<String> FONT_KEYS = Collections.unmodifiableList(Arrays.asList(
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
    ));

    private final Conf _conf;

    /** Strong reference to the font-view subscription. Sprouts holds listeners weakly, so
     *  dropping this field would silently break dynamic font tracking. */
    @SuppressWarnings("FieldCanBeLocal")
    private Viewable<FontUIResource> _fontView;

    /**
     *  Configures and installs the look and feel, then refreshes every window that is already
     *  open so a call made while the application is running switches its theme in place.
     *
     * @param configurator receives the default configuration and returns the desired one
     */
    public static void initializeUsing( Configurator<Conf> configurator ) {
        Objects.requireNonNull(configurator);
        SwingTreeLookAndFeel laf = new SwingTreeLookAndFeel(configurator);
        try {
            UIManager.setLookAndFeel(laf);
        } catch ( UnsupportedLookAndFeelException e ) {
            throw new IllegalStateException("Failed to install the SwingTree look and feel.", e);
        }
        for ( Window window : Window.getWindows() )
            SwingUtilities.updateComponentTreeUI(window);
    }

    /** Creates the look and feel with its default configuration. */
    public SwingTreeLookAndFeel() {
        _conf = Conf.DEFAULT;
    }

    /**
     *  Creates a configured look and feel, for callers which want to hand it to
     *  {@link UIManager#setLookAndFeel(javax.swing.LookAndFeel)} themselves.
     *
     * @param configurator receives the default configuration and returns the desired one
     */
    public SwingTreeLookAndFeel( Configurator<Conf> configurator ) {
        Objects.requireNonNull(configurator);
        try {
            _conf = Objects.requireNonNull(configurator.configure(Conf.DEFAULT));
        } catch ( Exception e ) {
            throw new IllegalArgumentException("Failed to configure the SwingTree look and feel.", e);
        }
    }

    @Override public String  getName()                { return _conf.name(); }
    @Override public String  getID()                  { return _conf.name(); }
    @Override public String  getDescription()         { return "A configurable look-and-feel rendered by the SwingTree style engine."; }
    @Override public boolean isNativeLookAndFeel()    { return false; }
    @Override public boolean isSupportedLookAndFeel() { return true; }

    /**
     *  Publishes this configuration to the UI delegates and subscribes to SwingTree's
     *  authoritative default-font property, so that runtime changes — a display-DPI event, an
     *  explicit {@link SwingTree#setUiScaleFactor(float)} call, the OS pushing a new system
     *  font — reach every open window without a restart.
     */
    @Override
    public void initialize() {
        _active = this;
        super.initialize();
        _fontView = SwingTree.get().getScaledDefaultFontView();
        _fontView.onChange(From.ALL, it -> SwingUtilities.invokeLater(() ->
                it.currentValue().ifPresent(SwingTreeLookAndFeel::_propagateFont)
        ));
    }

    @Override
    public void uninitialize() {
        // Drop the strong reference so the subscription becomes eligible for collection
        // alongside the look and feel instance itself.
        _fontView = null;
        if ( _active == this )
            _active = null;
        super.uninitialize();
    }

    @Override
    public UIDefaults getDefaults() {
        _active = this;
        return super.getDefaults();
    }

    // ── The delegates' view of the installed configuration ────────────────

    /** @return the configuration of the installed look and feel, or the default one. */
    static Conf conf() {
        SwingTreeLookAndFeel active = _active;
        return active == null ? Conf.DEFAULT : active._conf;
    }

    /**
     *  The colours the installed look and feel is painting with. An application which styles its
     *  own typography or draws something of its own reads them from here rather than hard-coding
     *  them, so that a re-tinted palette reaches its work too.
     *
     * @return the palette of the installed look and feel, or the default one if none is installed
     */
    public static Palette palette() { return conf().palette(); }

    /** @return the symbol set of the installed look and feel. */
    static Symbols symbols() { return conf().symbols(); }

    /**
     *  Whether the installed symbol set draws chrome of its own. When it does not - the blank one -
     *  every delegate falls through to the painting and the sizing its inherited {@code Basic*UI}
     *  would do, which is what turns this look and feel into plain Swing with the style engine
     *  wired in and nothing else.
     *
     * @return {@code true} if the symbol set has chrome of its own
     */
    static boolean drawsOwnChrome() { return conf().symbols().drawsItsOwnChrome(); }

    /**
     *  Whether any rule styles components of the given type. A delegate asks before it makes room
     *  for a style that is not coming.
     *
     * @param componentType the runtime class of the component
     * @return {@code true} if some rule governs it
     */
    static boolean styles( Class<?> componentType ) { return conf().styles(componentType); }

    /**
     *  Installs the style engine on a component, and first hands the component back to Swing's own
     *  defaults if nothing styles its type any more.
     *  <p>
     *  A style rule's colours do not only get painted, they get <em>installed</em>: the engine
     *  calls {@code setForeground(..)} and {@code setBackground(..)} on the component itself. That
     *  outlives the preset that asked for them, because Swing treats a colour it did not install as
     *  the application's and refuses to overwrite it. Switching from a theme to
     *  {@link StylePreset#BLANK} would therefore leave every button carrying the label colour the
     *  previous theme chose to sit on a fill that is no longer painted - white on white. Here the
     *  component is handed the plain look-and-feel defaults for its own class instead, which is
     *  what "nothing styles this" is supposed to look like.
     *  <p>
     *  A component that <em>is</em> styled gives up the border Swing installed on it from
     *  {@code Button.border}, {@code TextField.border} and the rest. Those are bevels drawn from
     *  the {@code control*} colours, and the style engine keeps whatever border it found as the
     *  one to fall back on wherever a rule leaves its own invisible - so under any theme without
     *  outlines they would surface as a two-tone frame around every control. Only a border Swing
     *  itself put there is dropped, so one the application set survives.
     *
     * @param c the component the delegate is being installed on
     */
    static void installStyleOn( JComponent c ) {
        if ( !styles(c.getClass()) )
            _restoreDefaultColours(c);
        else if ( c.getBorder() instanceof UIResource )
            c.setBorder(null);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    /** Re-reads the two colour defaults of a component's own UI class, e.g. "Button.background". */
    private static void _restoreDefaultColours( JComponent c ) {
        String id     = c.getUIClassID();
        String prefix = id.endsWith("UI") ? id.substring(0, id.length() - 2) : id;
        Color  bg     = UIManager.getColor(prefix + ".background");
        Color  fg     = UIManager.getColor(prefix + ".foreground");
        if ( bg != null ) c.setBackground(bg);
        if ( fg != null ) c.setForeground(fg);
    }

    /**
     *  Runs the configured style rules of the component being styled. Every UI delegate's
     *  {@code style(..)} method is nothing but a call to this.
     *
     * @param delegate the style delegate handed to the UI delegate by the style engine
     * @param <C> the component type the delegate was created for
     * @return the styled delegate
     * @throws Exception if one of the configured rules failed
     */
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"}) // component() is the documented hook for LAF state reads
    static <C extends JComponent> ComponentStyleDelegate<C> applyStyle( ComponentStyleDelegate<C> delegate ) throws Exception {
        Styler styler = conf().stylerFor(delegate.component().getClass());
        return (ComponentStyleDelegate<C>) styler.style((ComponentStyleDelegate) delegate);
    }

    // ── UIDefaults ───────────────────────────────────────────────────────

    /**
     *  Registers the SwingTree-backed {@link javax.swing.plaf.ComponentUI} delegates. The
     *  defaults populated by {@link BasicLookAndFeel#initClassDefaults(UIDefaults)} stay in
     *  place for anything not listed here.
     */
    @Override
    protected void initClassDefaults( UIDefaults table ) {
        super.initClassDefaults(table);
        table.put("PanelUI",               PKG + "SwingTreePanelUI");
        table.put("ButtonUI",              PKG + "SwingTreeButtonUI");
        table.put("ToggleButtonUI",        PKG + "SwingTreeButtonUI");
        table.put("CheckBoxUI",            PKG + "SwingTreeCheckBoxUI");
        table.put("RadioButtonUI",         PKG + "SwingTreeRadioButtonUI");
        table.put("LabelUI",               PKG + "SwingTreeLabelUI");
        table.put("TextFieldUI",           PKG + "SwingTreeTextFieldUI");
        table.put("PasswordFieldUI",       PKG + "SwingTreePasswordFieldUI");
        table.put("TextAreaUI",            PKG + "SwingTreeTextAreaUI");
        table.put("SeparatorUI",           PKG + "SwingTreeSeparatorUI");
        table.put("ToolTipUI",             PKG + "SwingTreeToolTipUI");
        table.put("ProgressBarUI",         PKG + "SwingTreeProgressBarUI");
        table.put("SliderUI",              PKG + "SwingTreeSliderUI");
        table.put("ScrollBarUI",           PKG + "SwingTreeScrollBarUI");
        table.put("ScrollPaneUI",          PKG + "SwingTreeScrollPaneUI");
        table.put("ComboBoxUI",            PKG + "SwingTreeComboBoxUI");
        table.put("SpinnerUI",             PKG + "SwingTreeSpinnerUI");
        table.put("TabbedPaneUI",          PKG + "SwingTreeTabbedPaneUI");
        table.put("FormattedTextFieldUI",  PKG + "SwingTreeFormattedTextFieldUI");
        table.put("EditorPaneUI",          PKG + "SwingTreeEditorPaneUI");
        table.put("TextPaneUI",            PKG + "SwingTreeTextPaneUI");
        table.put("ViewportUI",            PKG + "SwingTreeViewportUI");
        table.put("ListUI",                PKG + "SwingTreeListUI");
        table.put("TableUI",               PKG + "SwingTreeTableUI");
        table.put("TableHeaderUI",         PKG + "SwingTreeTableHeaderUI");
        table.put("TreeUI",                PKG + "SwingTreeTreeUI");
        table.put("ToolBarUI",             PKG + "SwingTreeToolBarUI");
        table.put("SplitPaneUI",           PKG + "SwingTreeSplitPaneUI");
        table.put("MenuBarUI",             PKG + "SwingTreeMenuBarUI");
        table.put("MenuUI",                PKG + "SwingTreeMenuUI");
        table.put("MenuItemUI",            PKG + "SwingTreeMenuItemUI");
        table.put("CheckBoxMenuItemUI",    PKG + "SwingTreeCheckBoxMenuItemUI");
        table.put("RadioButtonMenuItemUI", PKG + "SwingTreeRadioButtonMenuItemUI");
        table.put("PopupMenuUI",           PKG + "SwingTreePopupMenuUI");
    }

    /**
     *  Maps the legacy AWT system-colour keys onto the palette so that components which read
     *  raw system colours — some third-party widgets do — blend in with the rest of the theme.
     */
    @Override
    protected void initSystemColorDefaults( UIDefaults table ) {
        super.initSystemColorDefaults(table);
        Palette p = _conf.palette();
        table.put("control",            ui(p.surface()));
        table.put("controlText",        ui(p.text()));
        table.put("controlHighlight",   ui(p.surfaceHover()));
        table.put("controlLtHighlight", ui(p.surfaceHover()));
        table.put("controlShadow",      ui(p.border()));
        table.put("controlDkShadow",    ui(p.accent()));
        table.put("text",               ui(p.surfaceField()));
        table.put("textText",           ui(p.text()));
        table.put("textHighlight",      ui(p.accentSoft()));
        table.put("textHighlightText",  ui(p.text()));
        table.put("textInactiveText",   ui(p.textDisabled()));
        table.put("desktop",            ui(p.background()));
        table.put("window",             ui(p.surface()));
        table.put("windowBorder",       ui(p.border()));
        table.put("windowText",         ui(p.text()));
        table.put("info",               ui(p.surface()));
        table.put("infoText",           ui(p.text()));
    }

    /**
     *  Seeds the component-specific defaults. Painting is left to the UI delegates and the
     *  style engine; the entries below are the small subset of keys (background, foreground,
     *  font, caret, selection colours) that other code may read directly through
     *  {@link UIManager}.
     */
    @Override
    protected void initComponentDefaults( UIDefaults table ) {
        super.initComponentDefaults(table);

        Palette p = _conf.palette();
        Symbols s = _conf.symbols();

        // The single source of truth: SwingTree owns the authoritative, HiDPI-correctly-sized
        // default font for the active display. Calling SwingTree.get() also bootstraps the
        // library if it hadn't been initialised yet, so this works no matter what order
        // setLookAndFeel(..) and UI.show(..) are called in.
        FontUIResource baseFont = SwingTree.get().getScaledDefaultFont();
        // Pin the resolved font under "defaultFont" so that SwingTree's scale-recompute
        // listener — which fires when Label.font (or defaultFont) changes — derives the SAME
        // scale factor from our font as the one just installed.
        table.put("defaultFont", baseFont);

        table.put("Panel.background", ui(p.background()));
        table.put("Panel.foreground", ui(p.text()));
        table.put("Panel.font",       baseFont);

        table.put("Button.background",       ui(p.surface()));
        table.put("Button.foreground",       ui(p.text()));
        table.put("Button.font",             baseFont);
        table.put("ToggleButton.background", ui(p.surface()));
        table.put("ToggleButton.foreground", ui(p.text()));
        table.put("ToggleButton.font",       baseFont);

        table.put("Label.foreground",         ui(p.text()));
        table.put("Label.disabledForeground", ui(p.textDisabled()));
        table.put("Label.font",               baseFont);

        for ( String prefix : new String[]{ "TextField", "PasswordField", "TextArea",
                                            "FormattedTextField", "EditorPane", "TextPane" } ) {
            table.put(prefix + ".background",          ui(p.surfaceField()));
            table.put(prefix + ".foreground",          ui(p.text()));
            table.put(prefix + ".inactiveForeground",  ui(p.textDisabled()));
            table.put(prefix + ".caretForeground",     ui(p.accent()));
            table.put(prefix + ".selectionBackground", ui(p.accentSoft()));
            table.put(prefix + ".selectionForeground", ui(p.text()));
            table.put(prefix + ".font",                baseFont);
        }

        table.put("CheckBox.background",    ui(p.background()));
        table.put("CheckBox.foreground",    ui(p.text()));
        table.put("CheckBox.font",          baseFont);
        table.put("RadioButton.background", ui(p.background()));
        table.put("RadioButton.foreground", ui(p.text()));
        table.put("RadioButton.font",       baseFont);

        table.put("ProgressBar.background",          ui(p.surfaceDisabled()));
        table.put("ProgressBar.foreground",          ui(p.accent()));
        table.put("ProgressBar.selectionForeground", ui(p.surface()));
        table.put("ProgressBar.selectionBackground", ui(p.text()));
        table.put("ProgressBar.font",                baseFont);
        // BasicLookAndFeel's own default here is a two-pixel green line, which is a debugging
        // artefact from 1998 that no palette can make sense of.
        table.put("ProgressBar.border",              BorderFactory.createEmptyBorder());

        table.put("Slider.background", ui(p.background()));
        table.put("Slider.foreground", ui(p.accent()));
        table.put("Slider.tickColor",  ui(p.textMuted()));
        table.put("Slider.focus",      ui(p.accent()));
        table.put("Slider.font",       baseFont);

        // NOTE: no "ScrollBar.width" integer is installed here. Swing's convention is that the
        // value is in raw component pixels, which would be wrong on HiDPI displays. The
        // scrollbar thickness is driven dynamically by the scrollbar delegate's preferred size,
        // which scales the symbol set's developer-pixel constant at every layout pass.
        table.put("ScrollBar.background",      ui(p.surfaceDisabled()));
        table.put("ScrollBar.foreground",      ui(p.border()));
        table.put("ScrollBar.thumb",           ui(p.border()));
        table.put("ScrollBar.thumbDarkShadow", ui(p.border()));
        table.put("ScrollBar.thumbHighlight",  ui(p.border()));
        table.put("ScrollBar.thumbShadow",     ui(p.border()));
        table.put("ScrollBar.track",           ui(p.surfaceDisabled()));
        table.put("ScrollPane.background",     ui(p.surfaceField()));
        table.put("ScrollPane.foreground",     ui(p.text()));
        table.put("ScrollPane.font",           baseFont);

        table.put("ComboBox.background",          ui(p.surfaceField()));
        table.put("ComboBox.foreground",          ui(p.text()));
        table.put("ComboBox.selectionBackground", ui(p.accentSoft()));
        table.put("ComboBox.selectionForeground", ui(p.text()));
        table.put("ComboBox.disabledBackground",  ui(p.surfaceDisabled()));
        table.put("ComboBox.disabledForeground",  ui(p.textDisabled()));
        table.put("ComboBox.font",                baseFont);

        table.put("Spinner.background", ui(p.surfaceField()));
        table.put("Spinner.foreground", ui(p.text()));
        table.put("Spinner.font",       baseFont);

        table.put("Separator.background", ui(p.background()));
        table.put("Separator.foreground", ui(p.borderSoft()));

        table.put("ToolTip.background", ui(p.accent()));
        table.put("ToolTip.foreground", ui(p.onFilled()));
        table.put("ToolTip.font",       baseFont);

        table.put("Viewport.background", ui(p.surfaceField()));
        table.put("Viewport.foreground", ui(p.text()));
        table.put("Viewport.font",       baseFont);

        table.put("List.background",          ui(p.surfaceField()));
        table.put("List.foreground",          ui(p.text()));
        table.put("List.selectionBackground", ui(p.accentSoft()));
        table.put("List.selectionForeground", ui(p.text()));
        table.put("List.focusCellHighlightBorder", BorderFactory.createEmptyBorder(2, 6, 2, 6));
        table.put("List.font",                baseFont);

        table.put("Table.background",          ui(p.surfaceField()));
        table.put("Table.foreground",          ui(p.text()));
        table.put("Table.selectionBackground", ui(p.accentSoft()));
        table.put("Table.selectionForeground", ui(p.text()));
        table.put("Table.gridColor",           ui(p.borderSoft()));
        table.put("Table.alternateRowColor",   ui(p.surface()));
        table.put("Table.font",                baseFont);
        table.put("TableHeader.background",    ui(p.surface()));
        table.put("TableHeader.foreground",    ui(p.textMuted()));
        table.put("TableHeader.font",          baseFont);

        table.put("Tree.background",           ui(p.surfaceField()));
        table.put("Tree.foreground",           ui(p.text()));
        // A tree cell renderer fills its own row with this before drawing the label. The tree
        // underneath is already painted by its style rule, whatever colour that rule chose, so
        // filling again would show as a box behind every label wherever the two disagree.
        table.put("Tree.textBackground",       ui(Palette.TRANSPARENT));
        table.put("Tree.textForeground",       ui(p.text()));
        table.put("Tree.selectionBackground",  ui(p.accentSoft()));
        table.put("Tree.selectionForeground",  ui(p.text()));
        table.put("Tree.selectionBorderColor", ui(p.accent()));
        table.put("Tree.line",                 ui(p.borderSoft()));
        table.put("Tree.hash",                 ui(p.borderSoft()));
        table.put("Tree.font",                 baseFont);

        table.put("ToolBar.background",         ui(p.surface()));
        table.put("ToolBar.foreground",         ui(p.text()));
        table.put("ToolBar.dockingBackground",  ui(p.surfaceHover()));
        table.put("ToolBar.floatingBackground", ui(p.surface()));
        table.put("ToolBar.font",               baseFont);

        table.put("SplitPane.background",    ui(p.background()));
        table.put("SplitPane.shadow",        ui(p.border()));
        table.put("SplitPane.darkShadow",    ui(p.accent()));
        table.put("SplitPane.highlight",     ui(p.surfaceHover()));
        table.put("SplitPane.dividerSize",   s.splitDividerThickness());
        table.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());

        table.put("MenuBar.background", ui(p.surface()));
        table.put("MenuBar.foreground", ui(p.text()));
        table.put("MenuBar.font",       baseFont);
        for ( String prefix : new String[]{ "Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem" } ) {
            table.put(prefix + ".background",          ui("Menu".equals(prefix) ? p.surface() : p.surfaceField()));
            table.put(prefix + ".foreground",          ui(p.text()));
            table.put(prefix + ".selectionBackground", ui(p.accentSoft()));
            table.put(prefix + ".selectionForeground", ui(p.text()));
            table.put(prefix + ".disabledForeground",  ui(p.textDisabled()));
            table.put(prefix + ".font",                baseFont);
        }
        table.put("Menu.acceleratorForeground",              ui(p.textMuted()));
        table.put("Menu.acceleratorSelectionForeground",     ui(p.text()));
        table.put("MenuItem.acceleratorForeground",          ui(p.textMuted()));
        table.put("MenuItem.acceleratorSelectionForeground", ui(p.text()));
        table.put("PopupMenu.background",                    ui(p.surfaceField()));
        table.put("PopupMenu.foreground",                    ui(p.text()));
        table.put("PopupMenu.font",                          baseFont);

        // The glyphs Swing draws through an icon rather than through a UI delegate. The check and
        // the radio mark are installed whatever the symbol set says, because the basic look and
        // feel's own versions of those two are empty stubs and a control nobody can read is not
        // what "no styling" means. The rest have working basic defaults, so a symbol set with no
        // chrome of its own leaves them alone.
        table.put("CheckBox.icon",                 GlyphIcons.checkBox());
        table.put("RadioButton.icon",              GlyphIcons.radio());
        table.put("CheckBoxMenuItem.checkIcon",    GlyphIcons.checkBox());
        table.put("RadioButtonMenuItem.checkIcon", GlyphIcons.radio());
        if ( s.drawsItsOwnChrome() ) {
            table.put("Tree.expandedIcon",  GlyphIcons.treeExpanded());
            table.put("Tree.collapsedIcon", GlyphIcons.treeCollapsed());
            table.put("Menu.arrowIcon",     GlyphIcons.submenuArrow());
        }

        table.put("TabbedPane.background",            ui(p.background()));
        table.put("TabbedPane.foreground",            ui(p.text()));
        table.put("TabbedPane.selected",              ui(p.surfaceField()));
        table.put("TabbedPane.contentAreaColor",      ui(p.background()));
        table.put("TabbedPane.borderHightlightColor", ui(p.borderSoft()));
        table.put("TabbedPane.darkShadow",            ui(p.border()));
        table.put("TabbedPane.light",                 ui(p.surface()));
        table.put("TabbedPane.highlight",             ui(p.surfaceHover()));
        table.put("TabbedPane.shadow",                ui(p.border()));
        table.put("TabbedPane.focus",                 ui(p.accent()));
        table.put("TabbedPane.font",                  baseFont);
    }

    private static ColorUIResource ui( Color c ) { return new ColorUIResource(c); }

    /**
     *  Pushes {@code font} into every {@code *.font} key this look and feel owns and asks each
     *  top-level window to re-install its component tree so the on-screen text picks up the
     *  change. Runs on the EDT.
     *  <p>
     *  Skips the whole pass if the new font is byte-for-byte identical to {@code Label.font}'s
     *  current value — this is what makes the install-time first publish a no-op instead of
     *  triggering a redundant tree rebuild on every install.
     */
    private static void _propagateFont( FontUIResource font ) {
        Object current = UIManager.get("Label.font");
        if ( font.equals(current) )
            return;
        for ( String key : FONT_KEYS )
            UIManager.put(key, font);
        for ( Window w : Window.getWindows() )
            SwingUtilities.updateComponentTreeUI(w);
    }


    // ══ Public configuration API ═════════════════════════════════════════

    /**
     *  The immutable configuration of a {@link SwingTreeLookAndFeel}: which presets it draws
     *  from, the palette those presets read their colours out of, and the application's own
     *  additions to the style rules. Every method returns a new instance, so a configurator
     *  reads as a chain:
     *  <pre>{@code
     *    it -> it.stylePreset(StylePreset.LINEN)
     *            .palette(p -> p.accent(new Color(0x2E, 0x5A, 0x88)))
     *            .addStyle(JButton.class, s -> s.borderRadius(2))
     *  }</pre>
     */
    public static final class Conf
    {
        static final Conf DEFAULT = new Conf(
                StylePreset.LINEN, null, null, null,
                Tuple.of(StyleRule.class), Tuple.of(StyleRule.class)
        );

        private final StylePreset      _stylePreset;
        private final SymbolPreset     _symbolPreset;   // null means "whatever the style preset prefers"
        private final PalettePreset    _palettePreset;  // null means the same
        private final Palette          _palette;        // null means "whatever the palette preset brings"
        private final Tuple<StyleRule> _overrides;
        private final Tuple<StyleRule> _additions;

        /** Memoises the fold of preset, overrides and additions per component class, so that a
         *  style gathered on every paint costs a single map lookup. */
        private final Map<Class<?>, Styler<?>> _resolved = new ConcurrentHashMap<>();

        private Conf(
            StylePreset      stylePreset,
            SymbolPreset     symbolPreset,
            PalettePreset    palettePreset,
            Palette          palette,
            Tuple<StyleRule> overrides,
            Tuple<StyleRule> additions
        ) {
            _stylePreset   = stylePreset;
            _symbolPreset  = symbolPreset;
            _palettePreset = palettePreset;
            _palette       = palette;
            _overrides     = overrides;
            _additions     = additions;
        }

        /**
         *  Chooses the table of style rules the look and feel starts from. A preset also names the
         *  symbol set and the palette it was designed against, so switching it switches the whole
         *  look - unless {@link #symbolPreset(SymbolPreset)} or {@link #palettePreset(PalettePreset)}
         *  has already been called, in which case that choice stands.
         *
         * @param preset the style preset to draw the default rules from
         * @return a new configuration using {@code preset}
         */
        public Conf stylePreset( StylePreset preset ) {
            Objects.requireNonNull(preset);
            return new Conf(preset, _symbolPreset, _palettePreset, _palette, _overrides, _additions);
        }

        /**
         *  Chooses how the small pieces of geometry are drawn - check marks, arrows, slider
         *  handles, scroll thumbs, split-pane grips - and how thick the chrome around them is.
         *
         * @param preset the symbol preset to draw the glyphs with
         * @return a new configuration using {@code preset}
         */
        public Conf symbolPreset( SymbolPreset preset ) {
            Objects.requireNonNull(preset);
            return new Conf(_stylePreset, preset, _palettePreset, _palette, _overrides, _additions);
        }

        /**
         *  Chooses the named colours every rule and every symbol reads from. Because a preset asks
         *  the palette for "the surface a raised control is filled with" rather than for a literal
         *  colour, any palette can be paired with any style preset.
         *  <p>
         *  This also discards a palette built earlier with {@link #palette(Configurator)}: asking
         *  for a different palette wholesale is a decision about all of the colours, not some.
         *
         * @param preset the palette preset to paint with
         * @return a new configuration using {@code preset}
         */
        public Conf palettePreset( PalettePreset preset ) {
            Objects.requireNonNull(preset);
            return new Conf(_stylePreset, _symbolPreset, preset, null, _overrides, _additions);
        }

        /**
         *  Adjusts individual named colours. The configurator receives the palette resolved so far,
         *  so only the colours that actually change need to be named.
         *
         * @param configurator receives the current palette and returns the desired one
         * @return a new configuration using the returned palette
         */
        public Conf palette( Configurator<Palette> configurator ) {
            Objects.requireNonNull(configurator);
            Palette configured;
            try {
                configured = Objects.requireNonNull(configurator.configure(palette()));
            } catch ( Exception e ) {
                throw new IllegalArgumentException("Failed to configure the look and feel palette.", e);
            }
            return new Conf(_stylePreset, _symbolPreset, _palettePreset, configured, _overrides, _additions);
        }

        /**
         *  Replaces the preset's rule for {@code type} and every subtype of it. Use this when the
         *  preset's idea of how a component looks is wrong for the application; use
         *  {@link #addStyle(Class, Styler)} when it is merely incomplete.
         *
         * @param type   the component type the rule applies to, subtypes included
         * @param styler the replacement style rule
         * @param <C> the component type
         * @return a new configuration carrying the rule
         */
        public <C extends JComponent> Conf overrideStyle( Class<C> type, Styler<C> styler ) {
            return new Conf(_stylePreset, _symbolPreset, _palettePreset, _palette,
                            _overrides.add(new StyleRule(type, styler)), _additions);
        }

        /**
         *  Applies {@code styler} on top of whatever rule already governs {@code type} and its
         *  subtypes. Several additions may target the same type; they run in the order they were
         *  registered.
         *
         * @param type   the component type the rule applies to, subtypes included
         * @param styler the style rule to apply on top of the resolved one
         * @param <C> the component type
         * @return a new configuration carrying the rule
         */
        public <C extends JComponent> Conf addStyle( Class<C> type, Styler<C> styler ) {
            return new Conf(_stylePreset, _symbolPreset, _palettePreset, _palette,
                            _overrides, _additions.add(new StyleRule(type, styler)));
        }

        // ── read back by the look and feel and its delegates ──────────────

        Palette palette() {
            if ( _palette != null )
                return _palette;
            PalettePreset preset = _palettePreset != null ? _palettePreset : _stylePreset.preferredPalette();
            return preset.palette();
        }

        Symbols symbols() {
            SymbolPreset preset = _symbolPreset != null ? _symbolPreset : _stylePreset.preferredSymbols();
            return preset.symbols();
        }

        String name() { return _stylePreset.displayName(); }

        /**
         *  Folds the preset rule, the overriding rule and every addition that applies to
         *  {@code componentType} into a single {@link Styler}.
         *
         * @param componentType the runtime class of the component being styled
         * @return the style rule governing that class, never {@code null}
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        Styler<?> stylerFor( Class<?> componentType ) {
            Styler<?> memoised = _resolved.get(componentType);
            if ( memoised != null )
                return memoised;
            Styler resolved = _mostSpecific(_overrides, componentType);
            if ( resolved == null )
                resolved = _mostSpecific(_stylePreset.rules(), componentType);
            if ( resolved == null )
                resolved = Styler.none();
            for ( StyleRule rule : _additions )
                if ( rule.appliesTo(componentType) )
                    resolved = resolved.andThen(rule.styler());
            _resolved.put(componentType, resolved);
            return resolved;
        }

        /**
         *  Whether anything at all styles components of this type. A delegate asks before it makes
         *  room for a style that is not coming: suppressing a button's own content-area fill is
         *  right when a rule is going to paint one and wrong when nothing is, which is exactly the
         *  difference between a theme and {@link StylePreset#BLANK}.
         *
         * @param componentType the runtime class of the component
         * @return {@code true} if some rule governs it
         */
        boolean styles( Class<?> componentType ) { return stylerFor(componentType) != Styler.none(); }

        /** @return the rule of the most derived matching type, the last registered one winning ties. */
        private static Styler<?> _mostSpecific( Tuple<StyleRule> rules, Class<?> componentType ) {
            StyleRule best = null;
            for ( StyleRule rule : rules ) {
                if ( !rule.appliesTo(componentType) )
                    continue;
                if ( best == null || best.type().isAssignableFrom(rule.type()) )
                    best = rule;
            }
            return best == null ? null : best.styler();
        }
    }

    /**
     *  The named colours every preset paints with. A palette is immutable; each method returns
     *  a new one, which is what makes {@code p -> p.accent(..).border(..)} work.
     *  <p>
     *  The names are semantic rather than literal — {@link #surface(Color) surface} is "the
     *  colour a raised control is filled with", not "light grey" — so a preset written against
     *  them keeps working when the palette is re-tinted.
     */
    public static final class Palette
    {
        /** Fully transparent: what a control paints instead of a surface when it must let
         *  whatever it sits on show through untouched. */
        public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

        private enum Slot {
            BACKGROUND, SURFACE, SURFACE_HOVER, SURFACE_PRESSED, SURFACE_DISABLED, SURFACE_FIELD,
            BORDER, BORDER_SOFT,
            TEXT, TEXT_MUTED, TEXT_DISABLED,
            ACCENT, ACCENT_SOFT,
            TEXTURE_LIGHT, TEXTURE_DARK,
            PRIMARY, PRIMARY_HOVER, PRIMARY_PRESSED,
            DANGER, DANGER_HOVER, DANGER_PRESSED,
            ON_FILLED
        }

        private final EnumMap<Slot, Color> _colors;

        private Palette( EnumMap<Slot, Color> colors ) { _colors = colors; }

        /** @return a palette in which every colour is a neutral grey, to be filled in by a preset. */
        static Palette neutral() {
            EnumMap<Slot, Color> colors = new EnumMap<>(Slot.class);
            for ( Slot slot : Slot.values() )
                colors.put(slot, Color.GRAY);
            return new Palette(colors);
        }

        private Palette _with( Slot slot, Color color ) {
            Objects.requireNonNull(color);
            EnumMap<Slot, Color> changed = new EnumMap<>(_colors);
            changed.put(slot, color);
            return new Palette(changed);
        }

        private Color _get( Slot slot ) { return _colors.get(slot); }

        /** @param c the window background, the ground everything else stands on
         *  @return a new palette */
        public Palette background( Color c ) { return _with(Slot.BACKGROUND, c); }
        /** @param c the fill of a raised control at rest  @return a new palette */
        public Palette surface( Color c ) { return _with(Slot.SURFACE, c); }
        /** @param c the fill of a control with the pointer over it  @return a new palette */
        public Palette surfaceHover( Color c ) { return _with(Slot.SURFACE_HOVER, c); }
        /** @param c the fill of a control while pressed or selected  @return a new palette */
        public Palette surfacePressed( Color c ) { return _with(Slot.SURFACE_PRESSED, c); }
        /** @param c the fill of a control which cannot be used  @return a new palette */
        public Palette surfaceDisabled( Color c ) { return _with(Slot.SURFACE_DISABLED, c); }
        /** @param c the fill of an editable field or a scrollable page  @return a new palette */
        public Palette surfaceField( Color c ) { return _with(Slot.SURFACE_FIELD, c); }
        /** @param c the ordinary border colour  @return a new palette */
        public Palette border( Color c ) { return _with(Slot.BORDER, c); }
        /** @param c the colour of an inner divider, lighter than the border  @return a new palette */
        public Palette borderSoft( Color c ) { return _with(Slot.BORDER_SOFT, c); }
        /** @param c the primary text colour  @return a new palette */
        public Palette text( Color c ) { return _with(Slot.TEXT, c); }
        /** @param c the secondary text colour: captions, headings, accelerators  @return a new palette */
        public Palette textMuted( Color c ) { return _with(Slot.TEXT_MUTED, c); }
        /** @param c the text colour of something which cannot be used  @return a new palette */
        public Palette textDisabled( Color c ) { return _with(Slot.TEXT_DISABLED, c); }
        /** @param c the accent: focus rings, carets, selected borders, slider fills  @return a new palette */
        public Palette accent( Color c ) { return _with(Slot.ACCENT, c); }
        /** @param c the accent used as a selection background behind text  @return a new palette */
        public Palette accentSoft( Color c ) { return _with(Slot.ACCENT_SOFT, c); }
        /** @param c the lighter of the two specks the background grain is made of  @return a new palette */
        public Palette textureLight( Color c ) { return _with(Slot.TEXTURE_LIGHT, c); }
        /** @param c the darker of the two specks the background grain is made of  @return a new palette */
        public Palette textureDark( Color c ) { return _with(Slot.TEXTURE_DARK, c); }
        /** @param c the fill of the one affirmative control on a form  @return a new palette */
        public Palette primary( Color c ) { return _with(Slot.PRIMARY, c); }
        /** @param c the affirmative fill with the pointer over it  @return a new palette */
        public Palette primaryHover( Color c ) { return _with(Slot.PRIMARY_HOVER, c); }
        /** @param c the affirmative fill while pressed or selected  @return a new palette */
        public Palette primaryPressed( Color c ) { return _with(Slot.PRIMARY_PRESSED, c); }
        /** @param c the fill of a destructive control  @return a new palette */
        public Palette danger( Color c ) { return _with(Slot.DANGER, c); }
        /** @param c the destructive fill with the pointer over it  @return a new palette */
        public Palette dangerHover( Color c ) { return _with(Slot.DANGER_HOVER, c); }
        /** @param c the destructive fill while pressed or selected  @return a new palette */
        public Palette dangerPressed( Color c ) { return _with(Slot.DANGER_PRESSED, c); }
        /** @param c the text laid over a filled control, and over a tooltip  @return a new palette */
        public Palette onFilled( Color c ) { return _with(Slot.ON_FILLED, c); }

        /** @return the window background, the ground everything else stands on */
        public Color background()      { return _get(Slot.BACKGROUND); }
        /** @return the fill of a raised control at rest */
        public Color surface()         { return _get(Slot.SURFACE); }
        /** @return the fill of a control with the pointer over it */
        public Color surfaceHover()    { return _get(Slot.SURFACE_HOVER); }
        /** @return the fill of a control while pressed or selected */
        public Color surfacePressed()  { return _get(Slot.SURFACE_PRESSED); }
        /** @return the fill of a control which cannot be used */
        public Color surfaceDisabled() { return _get(Slot.SURFACE_DISABLED); }
        /** @return the fill of an editable field or a scrollable page */
        public Color surfaceField()    { return _get(Slot.SURFACE_FIELD); }
        /** @return the ordinary border colour */
        public Color border()          { return _get(Slot.BORDER); }
        /** @return the colour of an inner divider, lighter than the border */
        public Color borderSoft()      { return _get(Slot.BORDER_SOFT); }
        /** @return the primary text colour */
        public Color text()            { return _get(Slot.TEXT); }
        /** @return the secondary text colour: captions, headings, accelerators */
        public Color textMuted()       { return _get(Slot.TEXT_MUTED); }
        /** @return the text colour of something which cannot be used */
        public Color textDisabled()    { return _get(Slot.TEXT_DISABLED); }
        /** @return the accent: focus rings, carets, selected borders, slider fills */
        public Color accent()          { return _get(Slot.ACCENT); }
        /** @return the accent used as a selection background behind text */
        public Color accentSoft()      { return _get(Slot.ACCENT_SOFT); }
        /** @return the lighter of the two specks the background grain is made of */
        public Color textureLight()    { return _get(Slot.TEXTURE_LIGHT); }
        /** @return the darker of the two specks the background grain is made of */
        public Color textureDark()     { return _get(Slot.TEXTURE_DARK); }
        /** @return the fill of the one affirmative control on a form */
        public Color primary()         { return _get(Slot.PRIMARY); }
        /** @return the affirmative fill with the pointer over it */
        public Color primaryHover()    { return _get(Slot.PRIMARY_HOVER); }
        /** @return the affirmative fill while pressed or selected */
        public Color primaryPressed()  { return _get(Slot.PRIMARY_PRESSED); }
        /** @return the fill of a destructive control */
        public Color danger()          { return _get(Slot.DANGER); }
        /** @return the destructive fill with the pointer over it */
        public Color dangerHover()     { return _get(Slot.DANGER_HOVER); }
        /** @return the destructive fill while pressed or selected */
        public Color dangerPressed()   { return _get(Slot.DANGER_PRESSED); }
        /** @return the text laid over a filled control, and over a tooltip */
        public Color onFilled()        { return _get(Slot.ON_FILLED); }

        /** @param alpha how opaque the returned accent should be, 0 to 255
         *  @return the accent colour at the given opacity */
        public Color accentAt( int alpha ) {
            Color a = accent();
            return new Color(a.getRed(), a.getGreen(), a.getBlue(), alpha);
        }
    }

    /**
     *  The tables of {@link Styler} rules a {@link SwingTreeLookAndFeel} can be built from. A
     *  preset also names the {@link SymbolPreset} and {@link PalettePreset} it was designed
     *  against; those are what a {@link Conf} falls back to when the application does not choose.
     */
    public enum StylePreset
    {
        /**
         *  No rules at all - the look and feel becomes plain Swing with the style engine wired in
         *  and nothing else, which is what an {@code index.html} with no stylesheet looks like.
         *  <p>
         *  Every delegate notices that nothing styles its component type and stands down: a button
         *  keeps its own content-area fill, a scroll pane keeps its own border, a table keeps
         *  Swing's row height. Pair it with {@link SymbolPreset#BLANK} - which this preset asks for
         *  by default - and nothing is painted by this look and feel at all. It is the starting
         *  point for an application that wants to build its whole appearance itself out of
         *  {@link Conf#addStyle(Class, Styler)} rules.
         */
        BLANK {
            @Override Tuple<StyleRule>     rules()            { return Tuple.of(StyleRule.class); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.BLANK; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.BLANK; }
            @Override String               displayName()      { return "Blank"; }
        },
        /**
         *  A calm, paper-like theme: cream surfaces, taupe borders, a deep olive accent for focus
         *  and selection, and a barely perceptible woven grain on the window background.
         */
        LINEN {
            @Override Tuple<StyleRule>     rules()            { return LinenPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.LINEN; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.LINEN; }
            @Override String               displayName()      { return "Linen"; }
        },
        /**
         *  Neumorphism, or "soft UI": everything is the same colour as the window it sits on, and
         *  is told apart from it only by being lit - a pale highlight up and to the left, a soft
         *  shadow down and to the right. Pressing something turns the light around and it sinks
         *  in. Generous radii, no borders anywhere, and text a shade softer than black.
         */
        SOFT_UI {
            @Override Tuple<StyleRule>     rules()            { return SoftUiPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.SOFT; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.CLAY; }
            @Override String               displayName()      { return "Soft UI"; }
        },
        /**
         *  Frutiger Aero, the wet, glassy optimism of the middle 2000s: saturated fills under a
         *  hard gloss that breaks across the middle, sky gradients behind everything, crisp
         *  outlines and a drop shadow on every raised thing.
         */
        FRUTIGER_AERO {
            @Override Tuple<StyleRule>     rules()            { return FrutigerAeroPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.GLOSSY; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.AERO; }
            @Override String               displayName()      { return "Frutiger Aero"; }
        },
        /**
         *  Material: flat fills with no gradient anywhere, a small 4-pixel radius, and depth said
         *  entirely with elevation shadows. Buttons are outlined until they are the affirmative one
         *  and then filled; text fields are filled boxes under a rule that thickens into the accent
         *  when the field takes focus.
         */
        MATERIAL {
            @Override Tuple<StyleRule>     rules()            { return MaterialPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.MATERIAL; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.MATERIAL; }
            @Override String               displayName()      { return "Material"; }
        },
        /**
         *  Flat design: no shadow, no gradient, no bevel and no rounded corner anywhere. With
         *  depth given up, colour does all the work - a control goes grey, then pale accent under
         *  the pointer, then full accent with an inverted label when it is pressed - and the hard
         *  rule around an input is what is left to say that it can be typed into.
         */
        FLAT {
            @Override Tuple<StyleRule>     rules()            { return FlatDesignPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.FLAT; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.VIVID; }
            @Override String               displayName()      { return "Flat"; }
        },
        /**
         *  Skeuomorphism: every control pretends to be made of something. The window is a leather
         *  bench, cards are paper lying on it, and anything you can press is a milled metal plate
         *  - a grain, a top-to-bottom gradient and a one-pixel bevel, all three at once. Anything
         *  you type into is a hole cut into the surface instead, and pressing a plate turns it
         *  into exactly that hole.
         */
        SKEUOMORPHIC {
            @Override Tuple<StyleRule>     rules()            { return SkeuomorphicPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.CARVED; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.WORKSHOP; }
            @Override String               displayName()      { return "Skeuomorphic"; }
        },
        /**
         *  Glassmorphism: frosted panes floating over something vivid. Nothing is opaque - a
         *  surface is a wash of white with the window behind it actually blurred where it shows
         *  through, a bright hairline along its bevel and a wide soft shadow underneath. The
         *  window is a gradient rather than a colour, because glass with nothing behind it is
         *  just a pale rectangle.
         */
        GLASSMORPHIC {
            @Override Tuple<StyleRule>     rules()            { return GlassmorphicPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.GLASS; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.AURORA; }
            @Override String               displayName()      { return "Glassmorphic"; }
        },
        /**
         *  Polymorphism: a theme with no fixed appearance, only rules for arriving at one. It
         *  reads what the palette leaves it to work with, how tall each control is and how deeply
         *  each surface is nested, and derives everything from those three. Switching the palette
         *  under it does not re-tint it, it rewrites it - which means it has to be seen in at
         *  least two palettes to be seen at all.
         */
        POLYMORPHIC {
            @Override Tuple<StyleRule>     rules()            { return PolymorphicPreset.rules(); }
            @Override public SymbolPreset  preferredSymbols() { return SymbolPreset.ADAPTIVE; }
            @Override public PalettePreset preferredPalette() { return PalettePreset.MATERIAL; }
            @Override String               displayName()      { return "Polymorphic"; }
        };

        /** @return the preset's style rules, most general first. */
        abstract Tuple<StyleRule> rules();

        /**
         *  The symbol set the rules were designed against, which a {@link Conf} uses unless the
         *  application chooses one. Public because an application offering the user a choice of
         *  presets wants to move the other two along with it.
         *
         * @return the preferred symbol preset
         */
        public abstract SymbolPreset preferredSymbols();

        /**
         *  The palette the rules were designed against, used and useful for the same reason as
         *  {@link #preferredSymbols()}.
         *
         * @return the preferred palette preset
         */
        public abstract PalettePreset preferredPalette();

        /** @return the name the look and feel reports to {@link UIManager}. */
        abstract String displayName();

        @Override public String toString() { return displayName(); }
    }

    /**
     *  How the small pieces of geometry are drawn - a check mark, a radio dot, a drop-down arrow,
     *  a slider handle, a scroll thumb, the grip on a split-pane divider - and how thick the chrome
     *  they live in is. Independent of {@link StylePreset}: the same symbols work in any palette,
     *  because they take their colours from it.
     */
    public enum SymbolPreset
    {
        /**
         *  No symbols. Every delegate falls through to the painting and the sizing its inherited
         *  {@code Basic*UI} would do, so the check marks, arrows, thumbs and grips are Swing's own.
         */
        BLANK {
            @Override Symbols symbols() { return BlankSymbols.INSTANCE; }
            @Override String  displayName() { return "Blank"; }
        },
        /** Thin strokes, round caps, round dots and no fills to speak of: drawn the way a pen
         *  draws, so the geometry reads at a glance and stays crisp at any scale factor. */
        LINEN {
            @Override Symbols symbols() { return LinenSymbols.INSTANCE; }
            @Override String  displayName() { return "Linen"; }
        },
        /** Extruded: every glyph is the surface colour, lit from the top left and shadowed at the
         *  bottom right, so it reads as pressed out of the panel rather than drawn onto it. */
        SOFT {
            @Override Symbols symbols() { return SoftSymbols.INSTANCE; }
            @Override String  displayName() { return "Soft"; }
        },
        /** Glass: saturated fills under a hard gloss that breaks across the middle, with a crisp
         *  outline and a highlight along the top edge. */
        GLOSSY {
            @Override Symbols symbols() { return GlossySymbols.INSTANCE; }
            @Override String  displayName() { return "Glossy"; }
        },
        /** Bold and geometric: filled shapes rather than outlined ones, solid triangles for arrows,
         *  and thick strokes that stay legible at a glance. */
        MATERIAL {
            @Override Symbols symbols() { return MaterialSymbols.INSTANCE; }
            @Override String  displayName() { return "Material"; }
        },
        /** Rectangles and solid triangles: no radius, no rim, no halo and no shade, so a control
         *  that is on is the same shape as one that is off, filled. */
        FLAT {
            @Override Symbols symbols() { return FlatSymbols.INSTANCE; }
            @Override String  displayName() { return "Flat"; }
        },
        /** Cut into the surface or screwed onto it: every mark is drawn twice, dark on the line
         *  and light one pixel below it, where the far wall of the groove catches the light. */
        CARVED {
            @Override Symbols symbols() { return CarvedSymbols.INSTANCE; }
            @Override String  displayName() { return "Carved"; }
        },
        /** Cut from the same glass as everything else: a shape that is off is a wash you can see
         *  the ground through, one that is on is the accent behind a brighter rim. */
        GLASS {
            @Override Symbols symbols() { return GlassSymbols.INSTANCE; }
            @Override String  displayName() { return "Glass"; }
        },
        /** Not a set of its own but a choice between three of the others, remade from the palette
         *  in force on every call. */
        ADAPTIVE {
            @Override Symbols symbols() { return AdaptiveSymbols.INSTANCE; }
            @Override String  displayName() { return "Adaptive"; }
        };

        /** @return the symbol painter this preset stands for. */
        abstract Symbols symbols();

        /** @return the name to show a user choosing between presets. */
        abstract String displayName();

        @Override public String toString() { return displayName(); }
    }

    /**
     *  The named colours a {@link StylePreset} paints with. Because a rule asks for "the surface a
     *  raised control is filled with" rather than for a literal colour, any palette can be paired
     *  with any style preset - which is what makes a dark Linen or a Material in paper tones a
     *  one-line change rather than a fork.
     */
    public enum PalettePreset
    {
        /** What a browser shows a page with no stylesheet: a white sheet, black text, grey chrome
         *  and one plain blue for anything active. */
        BLANK {
            @Override Palette palette() { return Palettes.BLANK; }
            @Override String  displayName() { return "Blank"; }
        },
        /** Aged paper, raw linen and weathered taupe stone, with a deep olive accent. */
        LINEN {
            @Override Palette palette() { return Palettes.LINEN; }
            @Override String  displayName() { return "Linen"; }
        },
        /** A dark room: near-black surfaces a few steps apart, cool grey text, and a bright blue
         *  that is the only saturated thing on screen. */
        MIDNIGHT {
            @Override Palette palette() { return Palettes.MIDNIGHT; }
            @Override String  displayName() { return "Midnight"; }
        },
        /** Sky, water and glass: pale cyan surfaces, a deep sea-blue accent, and the saturated
         *  grass green that every piece of software from 2007 used for its affirmative button. */
        AERO {
            @Override Palette palette() { return Palettes.AERO; }
            @Override String  displayName() { return "Aero"; }
        },
        /** One single cool grey for the window and everything standing on it, which is what soft UI
         *  needs: with nothing to tell them apart by colour, the light has to do all the work. */
        CLAY {
            @Override Palette palette() { return Palettes.CLAY; }
            @Override String  displayName() { return "Clay"; }
        },
        /** White cards on an off-white ground, near-black text, greys in even steps and one indigo
         *  carrying every accent. */
        MATERIAL {
            @Override Palette palette() { return Palettes.MATERIAL; }
            @Override String  displayName() { return "Material"; }
        },
        /** Bold unmixed colour on a plain grey sheet: azure, forest green and pillar-box red, none
         *  of them a shade of any other. */
        VIVID {
            @Override Palette palette() { return Palettes.VIVID; }
            @Override String  displayName() { return "Vivid"; }
        },
        /** A bench in a workshop: worn leather, card stock, writing paper, brass and felt. */
        WORKSHOP {
            @Override Palette palette() { return Palettes.WORKSHOP; }
            @Override String  displayName() { return "Workshop"; }
        },
        /** Night sky through a frosted pane: a deep indigo ground with a violet and a magenta
         *  bloom in it, and white for everything the glass is made of. */
        AURORA {
            @Override Palette palette() { return Palettes.AURORA; }
            @Override String  displayName() { return "Aurora"; }
        };

        /** @return the colours this preset stands for. */
        abstract Palette palette();

        /** @return the name to show a user choosing between presets. */
        abstract String displayName();

        @Override public String toString() { return displayName(); }
    }

    /**
     *  The semantic roles a button-like control can be tagged with, read back out of the
     *  component's SwingTree style groups:
     *  <pre>{@code
     *    UI.button("Ship it").group(SwingTreeLookAndFeel.Variant.PRIMARY)
     *    UI.button("Strike out").group(SwingTreeLookAndFeel.Variant.DANGER)
     *    UI.button("Undo").group(SwingTreeLookAndFeel.Variant.QUIET)
     *  }</pre>
     *  A variant only decides <i>which</i> colours a control is painted in; the radius, the
     *  padding and the focus border are shared, so the roles stay visibly part of one family.
     *  <p>
     *  These live on the look and feel rather than in an application's own style sheet because
     *  the look-and-feel layer of SwingTree's cascade is resolved <em>after</em> the style
     *  sheet: a rule such as {@code add(type(JButton.class).group(Brand.PRIMARY), ..)} would be
     *  silently overwritten by the look and feel's unconditional background. Asking for a role
     *  instead means the look and feel answers in its own palette, so a re-tint reaches the
     *  brand colours too.
     */
    public enum Variant
    {
        /** The default: a raised surface. Applied when nothing else is tagged. */
        NEUTRAL,
        /** The one affirmative action on a form. */
        PRIMARY,
        /** A destructive action. */
        DANGER,
        /** A tool-bar or in-place control: no surface and no border until the pointer arrives,
         *  so a row of them reads as text rather than as a wall of boxes. */
        QUIET;

        /** Cached because {@link #of} runs inside a style rule, which is re-evaluated on every
         *  paint; {@code values()} would clone the array each time. */
        private static final Variant[] VALUES = values();

        /**
         *  Reads the variant a component was tagged with.
         *
         * @param component the component being styled
         * @return the first variant the component belongs to, or {@link #NEUTRAL}
         */
        static Variant of( JComponent component ) {
            ComponentExtension<?> extension = ComponentExtension.from(component);
            for ( Variant variant : VALUES )
                if ( variant != NEUTRAL && extension.belongsToGroup(variant) )
                    return variant;
            return NEUTRAL;
        }

        /** @return {@code true} for the roles that carry a surface and a shadow at rest. */
        boolean isRaised() { return this != QUIET; }

        /** @return {@code true} for the roles painted in a strong colour rather than in the
         *          ordinary surface colour. */
        boolean isFilled() { return this == PRIMARY || this == DANGER; }
    }

    /**
     *  The kinds of surface a panel or a scroll region can be tagged as:
     *  <pre>{@code
     *    UI.panel().group(SwingTreeLookAndFeel.Surface.CARD)        // a raised sheet of paper
     *    UI.panel().group(SwingTreeLookAndFeel.Surface.RAIL)        // a flat strip: tool bar, status line
     *    UI.panel().group(SwingTreeLookAndFeel.Surface.TRANSPARENT) // structure only, nothing painted
     *  }</pre>
     *  A real window needs at least two: the ground everything stands on, and the cards
     *  standing on it. Only the fill and the grain belong to the surface — padding, spacing and
     *  per-edge accents stay free for the application to set.
     */
    public enum Surface
    {
        /** The ground: the window background, with whatever grain the preset gives it. The default. */
        WINDOW,
        /** A sheet of paper lying on the window: lighter, rounded, hairlined and softly
         *  shadowed, with no grain so that it reads as raised. */
        CARD,
        /** A flat strip of the card colour with no radius and no shadow: tool bars, status
         *  lines, side rails and table headings. */
        RAIL,
        /** Nothing at all. For panels that exist only to group and lay out their children,
         *  where a second fill over the first would just muddy the card it sits in. */
        TRANSPARENT;

        /** Cached for the same reason as {@link Variant#VALUES}. */
        private static final Surface[] VALUES = values();

        /**
         *  Reads the surface a component was tagged with.
         *  <p>
         *  A {@link JViewport} is never part of an application's declaration — a scroll pane
         *  creates its own — so it cannot carry a tag and inherits the one on the scroll pane
         *  around it instead. That is what keeps the strip of viewport below a short page from
         *  being painted a different colour than the page.
         *
         * @param component the component being styled
         * @return the first surface the component belongs to, or {@link #WINDOW}
         */
        static Surface of( JComponent component ) {
            if ( _carriesAPopup(component) )
                return TRANSPARENT;
            JComponent tagged = component;
            if ( component instanceof JViewport && component.getParent() instanceof JScrollPane )
                tagged = (JScrollPane) component.getParent();
            ComponentExtension<?> extension = ComponentExtension.from(tagged);
            for ( Surface surface : VALUES )
                if ( surface != WINDOW && extension.belongsToGroup(surface) )
                    return surface;
            return WINDOW;
        }

        /**
         *  Whether Swing put this component here to carry a popup.
         *  <p>
         *  A popup is not shown as itself: {@code PopupFactory} wraps it in a plain
         *  {@link javax.swing.JPanel} and hangs that in the window's
         *  {@linkplain JLayeredPane#POPUP_LAYER popup layer}. The application never declared that
         *  panel and cannot tag it, so a look and feel that paints every panel puts an opaque
         *  rectangle exactly where the popup's own margin, radius and shadow were supposed to let
         *  the window show through.
         */
        private static boolean _carriesAPopup( JComponent component ) {
            Container parent = component.getParent();
            if ( parent instanceof JLayeredPane
              && ((JLayeredPane) parent).getLayer(component) >= JLayeredPane.POPUP_LAYER )
                return true;
            return component.getComponentCount() == 1
                && ( component.getComponent(0) instanceof JPopupMenu
                  || component.getComponent(0) instanceof JToolTip );
        }
    }
}

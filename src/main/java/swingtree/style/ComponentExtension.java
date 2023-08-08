package swingtree.style;

import swingtree.SwingTree;
import swingtree.UI;
import swingtree.animation.AnimationState;
import swingtree.animation.LifeTime;
import swingtree.api.Painter;
import swingtree.api.Styler;
import swingtree.components.JBox;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.LabelUI;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/**
 *  Is attached to UI components in the form of a client property.
 *  It exists to give Swing-Tree components some custom style and animation capabilities.
 */
public final class ComponentExtension<C extends JComponent>
{
    /**
     * Returns the {@link ComponentExtension} associated with the given component.
     * If the component does not have an extension, a new one is created and associated with the component.
     */
    public static <C extends JComponent> ComponentExtension<C> from( C comp ) {
        ComponentExtension<C> ext = (ComponentExtension<C>) comp.getClientProperty( ComponentExtension.class );
        if ( ext == null ) {
            ext = new ComponentExtension<>(comp);
            comp.putClientProperty( ComponentExtension.class, ext );
        }
        return ext;
    }

    public static void makeSureComponentHasExtension( JComponent comp ) { from(comp); }

    private final C _owner;

    private final Map<LifeTime, swingtree.api.Painter>   _animationPainters = new LinkedHashMap<>(0);
    private final Map<LifeTime, Styler<C>> _animationStylers  = new LinkedHashMap<>(0);

    private final List<String> _styleGroups = new ArrayList<>(0);

    private StylePainter<C> _currentStylePainter = null;
    private ComponentUI _styleLaF = null;
    private ComponentUI _formerLaF = null;
    private Styler<C> _styling = Styler.none();
    private StyleSheet _styleSheet = null;

    private Color _initialBackgroundColor = null;

    private Shape _mainClip = null;


    private ComponentExtension( C owner ) {
        _owner = Objects.requireNonNull(owner);
    }

    private boolean _customLookAndFeelIsInstalled() { return _styleLaF != null; }

    public void addStyling( Styler<C> styler ) {
        Objects.requireNonNull(styler);

        _styling = _styling.andThen( s -> styler.style(new ComponentStyleDelegate<>(_owner, s.style())) );

        establishStyle();
    }

    public void establishStyle() {
        _applyStyleToComponentState(_calculateStyle());
    }

    private void _establishCurrentMainPaintClip(Graphics g) {
        if ( _mainClip == null )
            _mainClip = g.getClip();
    }

    public void setStyleGroups( String... styleName ) {
        Objects.requireNonNull(styleName);
        if ( !_styleGroups.isEmpty() )
            throw new IllegalStateException("Style groups already specified!");

        _styleGroups.addAll( java.util.Arrays.asList(styleName) );
    }

    public List<String> getStyleGroups() { return Collections.unmodifiableList(_styleGroups); }

    public void clearAnimationRenderer() {
        _animationPainters.clear();
        _animationStylers.clear();
    }

    public void addAnimationPainter( AnimationState state, swingtree.api.Painter painter ) {
        _animationPainters.put(Objects.requireNonNull(state.lifetime()), Objects.requireNonNull(painter));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    public void addAnimationStyler( AnimationState state, Styler<C> styler ) {
        _animationStylers.put(Objects.requireNonNull(state.lifetime()), Objects.requireNonNull(styler));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    public PanelUI createJBoxUI() {
        return new PanelStyler() {
            @Override
            public void installUI(JComponent c) {
                JBox b = (JBox)c;
                installDefaults(b);
            }
            @Override
            public void uninstallUI(JComponent c) {
                JBox b = (JBox)c;
                uninstallDefaults(b);
            }

            private void installDefaults(JBox b) {
                LookAndFeel.installColorsAndFont(b,
                        "Box.background",
                        "Box.foreground",
                        "Box.font");
                LookAndFeel.installBorder(b,"Box.border");
                LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
            }

            private void uninstallDefaults(JBox b) {
                LookAndFeel.uninstallBorder(b);
            }
        };
    }

    private StylePainter<C> _createStylePainter() {
        Style style = _applyStyleToComponentState(_calculateStyle());
        return style.equals(Style.none()) ? null : new StylePainter<>(_owner, style);
    }

    private Optional<StylePainter<C>> _getOrCreateStylePainter() {
        if ( _currentStylePainter == null )
            _currentStylePainter = _createStylePainter();

        return Optional.ofNullable(_currentStylePainter);
    }

    public void paintBackgroundStyle( Graphics g )
    {
        if ( _customLookAndFeelIsInstalled() )
            return; // We render through the custom installed UI!

        if ( _componentIsDeclaredInUI(_owner) )
            _paintBackground(g);
        else
            _currentStylePainter = null; // custom style rendering unfortunately not possible for this component :/
    }

    private void _paintBackground( Graphics g )
    {
        _mainClip = null;
        _establishCurrentMainPaintClip(g);

        _currentStylePainter = _createStylePainter();
        if ( _currentStylePainter != null )
            _currentStylePainter.renderBackgroundStyle( (Graphics2D) g );
    }


    public void _renderAnimations( Graphics2D g2d )
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        if ( StylePainter.DO_ANTIALIASING() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // Animations are last: they are rendered on top of everything else:
        for ( Map.Entry<LifeTime, Painter> entry : new ArrayList<>(_animationPainters.entrySet()) )
            if ( entry.getKey().isExpired() )
                _animationPainters.remove(entry.getKey());
            else {
                try {
                    entry.getValue().paint(g2d);
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a painter should not prevent everything else from being painted!
                }
            }

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

    }

    public void paintForegroundStyle( Graphics2D g2d ) {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

        // We remember the clip:
        Shape formerClip = g2d.getClip();

        if ( _currentStylePainter != null )
            _currentStylePainter.paintForegroundStyle(g2d);

        // We restore the clip:
        if ( g2d.getClip() != formerClip )
            g2d.setClip(formerClip);

        // Enable antialiasing again:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
    }

    private Style _calculateStyle() {
        _styleSheet = _styleSheet != null ? _styleSheet : SwingTree.get().getStyleSheet().orElse(null);
        Style style = _styleSheet == null ? Style.none() : _styleSheet.applyTo( _owner );
        style = _styling.style(new ComponentStyleDelegate<>(_owner, style)).style();

        // Animations styles are last: they override everything else:
        for ( Map.Entry<LifeTime, Styler<C>> entry : new ArrayList<>(_animationStylers.entrySet()) )
            if ( entry.getKey().isExpired() )
                _animationStylers.remove(entry.getKey());
            else {
                try {
                    style = entry.getValue().style(new ComponentStyleDelegate<>(_owner, style)).style();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a styler should not prevent other stylers from being applied!
                }
            }

        return _applyDPIScaling(style);
    }

    private static Style _applyDPIScaling( Style style ) {
        if ( UI.scale() == 1f )
            return style;

        return style.scale( UI.scale() );
    }

    private Style _applyStyleToComponentState( Style style )
    {
        Objects.requireNonNull(style);

        final Style.Report styleReport = style.getReport();

        boolean isNotStyled                     = styleReport.isNotStyled();
        boolean onlyDimensionalityIsStyled      = styleReport.onlyDimensionalityIsStyled();
        boolean styleCanBeRenderedThroughBorder = BorderStyleAndAnimationRenderer.canFullyPaint(styleReport);

        if ( isNotStyled || onlyDimensionalityIsStyled ) {
            _uninstallCustomLaF();
            if ( _animationStylers.isEmpty() && _animationPainters.isEmpty() )
                _uninstallCustomBorderBasedStyleAndAnimationRenderer();
            if ( _initialBackgroundColor != null ) {
                _owner.setBackground(_initialBackgroundColor);
                _initialBackgroundColor = null;
            }
            if ( isNotStyled )
                return style;
        }

        boolean hasBorderRadius = style.border().hasAnyNonZeroArcs();
        boolean hasBackground   = style.background().color().isPresent();

        if ( hasBackground && !Objects.equals( _owner.getBackground(), style.background().color().get() ) ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            _owner.setBackground( style.background().color().get() );
        }

        // If the style has a border radius set we need to make sure that we have a background color:
        if ( hasBorderRadius && !hasBackground ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            style = style.backgroundColor(_initialBackgroundColor);
        }

        if ( style.foreground().color().isPresent() && !Objects.equals( _owner.getForeground(), style.foreground().color().get() ) )
            _owner.setForeground( style.foreground().color().get() );

        if ( style.dimensionality().minWidth().isPresent() || style.dimensionality().minHeight().isPresent() ) {
            Dimension minSize = _owner.getMinimumSize();

            int minWidth  = style.dimensionality().minWidth().orElse(minSize == null ? 0 : minSize.width);
            int minHeight = style.dimensionality().minHeight().orElse(minSize == null ? 0 : minSize.height);

            Dimension newMinSize = new Dimension(minWidth, minHeight);

            if ( ! newMinSize.equals(minSize) )
                _owner.setMinimumSize(newMinSize);
        }

        if ( style.dimensionality().maxWidth().isPresent() || style.dimensionality().maxHeight().isPresent() ) {
            Dimension maxSize = _owner.getMaximumSize();

            int maxWidth  = style.dimensionality().maxWidth().orElse(maxSize == null  ? Integer.MAX_VALUE : maxSize.width);
            int maxHeight = style.dimensionality().maxHeight().orElse(maxSize == null ? Integer.MAX_VALUE : maxSize.height);

            Dimension newMaxSize = new Dimension(maxWidth, maxHeight);

            if ( ! newMaxSize.equals(maxSize) )
                _owner.setMaximumSize(newMaxSize);
        }

        if ( style.dimensionality().preferredWidth().isPresent() || style.dimensionality().preferredHeight().isPresent() ) {
            Dimension prefSize = _owner.getPreferredSize();

            int prefWidth  = style.dimensionality().preferredWidth().orElse(prefSize == null ? 0 : prefSize.width);
            int prefHeight = style.dimensionality().preferredHeight().orElse(prefSize == null ? 0 : prefSize.height);

            Dimension newPrefSize = new Dimension(prefWidth, prefHeight);

            if ( !newPrefSize.equals(prefSize) )
                _owner.setPreferredSize(newPrefSize);
        }

        if ( style.dimensionality().width().isPresent() || style.dimensionality().height().isPresent() ) {
            Dimension size = _owner.getSize();

            int width  = style.dimensionality().width().orElse(size == null ? 0 : size.width);
            int height = style.dimensionality().height().orElse(size == null ? 0 : size.height);

            Dimension newSize = new Dimension(width, height);

            if ( ! newSize.equals(size) )
                _owner.setSize(newSize);
        }

        if ( _owner instanceof JTextComponent ) {
            JTextComponent tc = (JTextComponent) _owner;
            if ( style.font().selectionColor().isPresent() && ! Objects.equals( tc.getSelectionColor(), style.font().selectionColor().get() ) )
                tc.setSelectionColor(style.font().selectionColor().get());
        }

        if ( _owner instanceof JComboBox ) {
            int bottom = style.margin().bottom().orElse(0);
            // We adjust the position of the popup menu:
            try {
                Point location = _owner.getLocationOnScreen();
                int x = location.x;
                int y = location.y + _owner.getHeight() - bottom;
                JComboBox<?> comboBox = (JComboBox<?>) _owner;
                JPopupMenu popup = (JPopupMenu) comboBox.getAccessibleContext().getAccessibleChild(0);
                Point oldLocation = popup.getLocation();
                if ( popup.isShowing() && (oldLocation.x != x || oldLocation.y != y) )
                    popup.setLocation(x, y);
            } catch ( Exception e ) {
                // ignore
            }
        }

        style.font()
             .createDerivedFrom(_owner.getFont())
             .ifPresent( newFont -> {
                    if ( !newFont.equals(_owner.getFont()) )
                        _owner.setFont( newFont );
                });

        style.cursor().ifPresent( cursor -> {
            if ( !Objects.equals( _owner.getCursor(), cursor ) )
                _owner.setCursor( cursor );
        });

        style.font().horizontalAlignment().ifPresent( alignment -> {
            if ( _owner instanceof JLabel ) {
                JLabel label = (JLabel) _owner;
                if ( !Objects.equals( label.getHorizontalAlignment(), alignment.forSwing() ) )
                    label.setHorizontalAlignment( alignment.forSwing() );
            }
            if ( _owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) _owner;
                if ( !Objects.equals( button.getHorizontalAlignment(), alignment.forSwing() ) )
                    button.setHorizontalAlignment( alignment.forSwing() );
            }
            if ( _owner instanceof JTextField ) {
                JTextField textField = (JTextField) _owner;
                if ( !Objects.equals( textField.getHorizontalAlignment(), alignment.forSwing() ) )
                    textField.setHorizontalAlignment( alignment.forSwing() );
            }
        });

        if ( !onlyDimensionalityIsStyled ) {
            _installCustomBorderBasedStyleAndAnimationRenderer();
            if ( !styleCanBeRenderedThroughBorder )
                _establishLookAndFeel(style);
        }

        if ( style.hasCustomForegroundPainters() )
            _makeAllChildrenTransparent(_owner);

        if ( style.hasActiveBackgroundGradients() && _owner.isOpaque() )
            _owner.setOpaque(false);

        return style;
    }

    /**
     *  Note that the foreground painter is intended to paint over all children of the component, <br>
     *  which is why it will be called at the end of {@code JComponent::paintChildren(Graphics)}.
     *  <br>
     *  However, there is a problem with this approach! <br>
     *  If not all children are transparent, the result of the foreground painter can be overwritten
     *  by {@link JComponent#paintImmediately(int, int, int, int)} when certain events occur
     *  (like a child component is a text field with a blinking cursor, or a button with hover effect).
     *  This type of repaint does unfortunately not call {@code JComponent::paintChildren(Graphics)},
     *  in fact it completely ignores bypasses the rendering of this current component!
     *  In order to ensure that the stuff painted by the foreground painter is not overwritten
     *  in these types of cases,
     *  we make all children transparent (non-opaque) so that the foreground painter is always visible.
     *
     * @param c The component to make all children transparent.
     */
    private void _makeAllChildrenTransparent( JComponent c ) {
        if ( c.isOpaque() )
            c.setOpaque(false);

        for ( Component child : c.getComponents() ) {
            if ( child instanceof JComponent ) {
                JComponent jChild = (JComponent) child;
                _makeAllChildrenTransparent(jChild);
            }
        }
    }

    private void _establishLookAndFeel( Style style ) {

        // For panels mostly:
        boolean weNeedToOverrideLaF = false;
        boolean hasBorderRadius = style.border().hasAnyNonZeroArcs();
        boolean hasMargin = style.margin().isPositive();
        boolean hasBackgroundPainter = style.hasCustomBackgroundPainters();
        boolean hasBackgroundShades  = style.hasCustomGradients();

        if ( hasBorderRadius )
            weNeedToOverrideLaF = true;

        if ( hasMargin )
            weNeedToOverrideLaF = true;

        if ( hasBackgroundPainter )
            weNeedToOverrideLaF = true;

        if ( hasBackgroundShades )
            weNeedToOverrideLaF = true;

        if ( style.hasCustomBackgroundPainters() )
            weNeedToOverrideLaF = true;

        if ( style.anyVisibleShadows() )
            weNeedToOverrideLaF = true;

        if ( weNeedToOverrideLaF ) {
            boolean foundationIsTransparent = style.background()
                                                    .foundationColor()
                                                    .map( c -> c.getAlpha() < 255 )
                                                    .orElse(
                                                        Optional
                                                        .ofNullable(_owner.getBackground())
                                                        .map( c -> c.getAlpha() < 255 )
                                                        .orElse(true)
                                                    );

            _owner.setOpaque( !hasBorderRadius && !hasMargin && !foundationIsTransparent );
            /* ^
                If our style reveals what is behind it, then we need
                to make the component non-opaque so that the previous rendering get's flushed out!
             */
            boolean success = _installCustomLaF();
            if ( !success && _owner.isOpaque() ) {
                _owner.setOpaque(false);
            }

            if ( _owner instanceof AbstractButton ) {
                AbstractButton b = (AbstractButton) _owner;
                b.setContentAreaFilled(!hasBackgroundShades && !hasBackgroundPainter);
            }
        }
        else if ( _customLookAndFeelIsInstalled() )
            _uninstallCustomLaF();
    }

    private boolean _installCustomLaF() {
        // First we check if we already have a custom LaF installed:
        if ( _customLookAndFeelIsInstalled() )
            return true;

        if ( _owner instanceof JPanel ) {
            JPanel p = (JPanel) _owner;
            _formerLaF = p.getUI();
            PanelStyler laf = PanelStyler.INSTANCE;
            p.setUI(laf);
            _styleLaF = laf;
            if ( _formerLaF instanceof BasicPanelUI ) {
                BasicPanelUI panelUI = (BasicPanelUI) _formerLaF;
                panelUI.installUI(p);
                // We make the former LaF believe that it is still in charge of the component.
            }
            return true;
        }
        if ( _owner instanceof JBox ) {
            JBox p = (JBox) _owner;
            _formerLaF = p.getUI();
            //PanelUI laf = createJBoxUI();
            //p.setUI(laf);
            _styleLaF = _formerLaF;
            return true;
        }
        else if ( _owner instanceof AbstractButton ) {
            AbstractButton b = (AbstractButton) _owner;
            _formerLaF = b.getUI();
            ButtonStyler laf = new ButtonStyler(b.getUI());
            b.setUI(laf);
            if ( _formerLaF instanceof BasicButtonUI ) {
                BasicButtonUI buttonUI = (BasicButtonUI) _formerLaF;
                buttonUI.installUI(b);
                // We make the former LaF believe that it is still in charge of the component.
            }
            _styleLaF = laf;
            return true;
        }
        else if ( _owner instanceof JLabel ) {
            JLabel l = (JLabel) _owner;
            _formerLaF = l.getUI();
            LabelStyler laf = new LabelStyler(l.getUI());
            l.setUI(laf);
            if ( _formerLaF instanceof BasicLabelUI ) {
                BasicLabelUI labelUI = (BasicLabelUI) _formerLaF;
                labelUI.installUI(l);
                // We make the former LaF believe that it is still in charge of the component.
            }
            _styleLaF = laf;
            return true;
        }

        return false;
    }

    private void _uninstallCustomLaF() {
        if ( _customLookAndFeelIsInstalled() ) {
            if ( _owner instanceof JPanel ) {
                JPanel p = (JPanel) _owner;
                p.setUI((PanelUI) _formerLaF);
                _styleLaF = null;
            }
            if ( _owner instanceof JBox ) {
                //JBox p = (JBox) _owner;
                //p.setUI((PanelUI) _formerLaF);
                _styleLaF = null;
            }
            else if ( _owner instanceof AbstractButton ) {
                AbstractButton b = (AbstractButton) _owner;
                b.setUI((ButtonUI) _formerLaF);
                _styleLaF = null;
            }
            else if ( _owner instanceof JLabel ) {
                JLabel l = (JLabel) _owner;
                l.setUI((LabelUI) _formerLaF);
                _styleLaF = null;
            }
        }
    }

    private void _installCustomBorderBasedStyleAndAnimationRenderer() {
        Border currentBorder = _owner.getBorder();
        if ( !(currentBorder instanceof ComponentExtension.BorderStyleAndAnimationRenderer) )
            _owner.setBorder(new BorderStyleAndAnimationRenderer<>(this, currentBorder));
    }

    private void _uninstallCustomBorderBasedStyleAndAnimationRenderer() {
        Border currentBorder = _owner.getBorder();
        if ( currentBorder instanceof ComponentExtension.BorderStyleAndAnimationRenderer ) {
            BorderStyleAndAnimationRenderer<?> border = (BorderStyleAndAnimationRenderer<?>) currentBorder;
            _owner.setBorder(border.getFormerBorder());
        }
    }

    static boolean _componentIsDeclaredInUI(JComponent comp ) {
        // The component must be a subtype of one of the classes enclosed in this UI class!
        // Let's get all the classes declared in UI:
        Class<?>[] declaredInUI = UI.class.getDeclaredClasses();
        // We want to ensure that the component is a sub-type of any of the classes declared in UI.
        Class<?> clazz = comp.getClass();
        boolean isSwingTreeComponent = false;
        while ( clazz != null ) {
            for ( Class<?> c : declaredInUI )
                if ( c.isAssignableFrom(clazz) ) {
                    isSwingTreeComponent = true;
                    break;
                }

            clazz = clazz.getSuperclass();
        }
        return isSwingTreeComponent;
    }


    static final class BorderStyleAndAnimationRenderer<C extends JComponent> implements Border
    {
        static boolean canFullyPaint(Style.Report report) {
            boolean simple = report.onlyDimensionalityAndOrLayoutIsStyled();
            if ( simple ) return true;
            return report.noBorderStyle          &&
                   report.noBackgroundStyle      &&
                   report.noForegroundStyle      &&
                   ( report.noShadowStyle || report.allShadowsAreBorderShadows     ) &&
                   ( report.noPainters    || report.allPaintersAreBorderPainters   ) &&
                   ( report.noShades      || report.allGradientsAreBorderGradients ) &&
                   ( report.noGrounds     || report.allImagesAreBorderImages       );

        }

        private final ComponentExtension<C> _compExt;
        private final Border _formerBorder;
        private final boolean _borderWasNotPainted;
        private Insets _currentInsets;
        private Insets _currentMarginInsets = new Insets(0,0,0,0);
        private Insets _currentPaddingInsets = new Insets(0,0,0,0);

        BorderStyleAndAnimationRenderer(ComponentExtension<C> compExt, Border formerBorder) {
            _compExt = compExt;
            _currentInsets = null;
            _formerBorder = formerBorder;
            if ( _compExt._owner instanceof AbstractButton ) {
                AbstractButton b = (AbstractButton) _compExt._owner;
                _borderWasNotPainted = !b.isBorderPainted();
                b.setBorderPainted(true);
            }
            else
                _borderWasNotPainted = false;
        }

        Border getFormerBorder() { return _formerBorder; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            _checkIfInsetsChanged();

            _compExt._establishCurrentMainPaintClip(g);

            // We remember the clip:
            Shape formerClip = g.getClip();

            g.setClip(_compExt._mainClip);

            if ( _compExt._currentStylePainter != null ) {
                _paintThisStyleAPIBasedBorder((Graphics2D) g);
                if ( _formerBorder != null && !_borderWasNotPainted ) {
                    Style.Report report = _compExt._currentStylePainter.getStyle().getReport();
                    if ( canFullyPaint(report) )
                        _paintFormerBorder(c, g, x, y, width, height);
                }
            }
            else if ( _formerBorder != null && !_borderWasNotPainted )
                _paintFormerBorder(c, g, x, y, width, height);

            if ( g.getClip() != formerClip )
                g.setClip(formerClip);

            _compExt._renderAnimations( (Graphics2D) g );

            if ( g.getClip() != formerClip )
                g.setClip(formerClip);
        }

        private void _paintFormerBorder(Component c, Graphics g, int x, int y, int width, int height) {
            try {
                Insets insets = _currentMarginInsets == null ? new Insets(0, 0, 0, 0) : _currentMarginInsets;
                _formerBorder.paintBorder(
                        c, g,
                        x + insets.left,
                        y + insets.top,
                        width - insets.left - insets.right,
                        height - insets.top - insets.bottom
                );
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }

        private void _paintThisStyleAPIBasedBorder(Graphics2D g) {
            try {
                _compExt._currentStylePainter.paintBorderStyle(g);
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            _checkIfInsetsChanged();
            return _currentInsets;
        }

        private void _checkIfInsetsChanged() {
            Insets insets = _calculateInsets();
            if ( !insets.equals(_currentInsets) ) {
                _currentInsets = insets;
                _compExt._owner.revalidate();
            }
        }

        private Insets _calculateInsets() {
            _currentMarginInsets = _compExt._getOrCreateStylePainter()
                                            .map(StylePainter::calculateMarginInsets)
                                            .orElse(_currentMarginInsets);

            _currentPaddingInsets = _compExt._getOrCreateStylePainter()
                                            .map(StylePainter::calculatePaddingInsets)
                                            .orElse(_currentPaddingInsets);

            return _compExt._getOrCreateStylePainter()
                            .map( r ->
                                r.calculateBorderInsets(
                                    _formerBorder == null
                                        ? new Insets(0,0,0,0)
                                        : _formerBorder.getBorderInsets(_compExt._owner)
                                )
                            )
                            .orElseGet(()->
                                _formerBorder == null
                                    ? new Insets(0,0,0,0)
                                    : _formerBorder.getBorderInsets(_compExt._owner)
                            );
        }

        public Insets getCurrentMarginInsets() { return _currentMarginInsets; }

        public Insets getCurrentPaddingInsets() { return _currentPaddingInsets; }

        @Override public boolean isBorderOpaque() { return false; }

    }

    static class PanelStyler extends BasicPanelUI
    {
        static final PanelStyler INSTANCE = new PanelStyler();

        private PanelStyler() {}

        @Override public void paint( Graphics g, JComponent c ) { ComponentExtension.from(c)._paintBackground(g); }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class ButtonStyler extends BasicButtonUI
    {
        private final ButtonUI _formerUI;

        ButtonStyler(ButtonUI formerUI) { _formerUI = formerUI; }

        @Override public void paint( Graphics g, JComponent c ) {
            ComponentExtension.from(c)._paintBackground(g);
            _paintComponentThroughFormerIU(_formerUI, g, c);
        }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class LabelStyler extends BasicLabelUI
    {
        private final LabelUI _formerUI;

        private LabelStyler(LabelUI formerUI) { _formerUI = formerUI; }

        @Override public void paint( Graphics g, JComponent c ) {
            ComponentExtension.from(c)._paintBackground(g);
            _paintComponentThroughFormerIU(_formerUI, g, c);
        }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    private static void _paintComponentThroughFormerIU(ComponentUI formerUI, Graphics g, JComponent c) {
        try {
            if ( formerUI != null )
                formerUI.update(g, c);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    private static boolean _contains(JComponent c, int x, int y, Supplier<Boolean> superContains) {
        Border border = c.getBorder();
        if ( border instanceof BorderStyleAndAnimationRenderer ) {
            BorderStyleAndAnimationRenderer<?> b = (BorderStyleAndAnimationRenderer<?>) border;
            Insets margins = b.getCurrentMarginInsets();
            int width  = c.getWidth();
            int height = c.getHeight();
            return (x >= margins.left) && (x < width - margins.right) && (y >= margins.top) && (y < height - margins.bottom);
        }
        return superContains.get();
    }

}

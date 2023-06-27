package swingtree;

import swingtree.animation.AnimationState;
import swingtree.animation.LifeTime;
import swingtree.style.Painter;
import swingtree.style.*;

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
 *  It exists to give Swing-Tree components some custom rendering
 *  in a declarative fashion.
 */
public class ComponentExtension<C extends JComponent>
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

    static void makeSureComponentHasExtension( JComponent comp ) { from(comp); }

    private final C _owner;

    private final Map<LifeTime, Painter>   _animationPainters = new LinkedHashMap<>(0);
    private final Map<LifeTime, Styler<C>> _animationStylers  = new LinkedHashMap<>(0);

    private final List<String> _styleGroups = new ArrayList<>(0);

    private StyleRenderer<C> _currentRenderer = null;
    private ComponentUI _styleLaF = null;
    private ComponentUI _formerLaF = null;
    private Styler<C> _styling = Styler.none();
    private StyleSheet _styleSheet = null;

    private Color _initialBackgroundColor = null;

    private ComponentExtension( C owner ) {
        _owner = Objects.requireNonNull(owner);
    }

    private boolean _customLookAndFeelIsInstalled() { return _styleLaF != null; }

    void addStyling( Styler<C> styler ) {
        Objects.requireNonNull(styler);
        checkIfIsDeclaredInUI();
        _styling = _styling.andThen( s -> styler.style(new StyleDelegate<>(_owner, s.style())) );

        establishStyle();
    }

    void establishStyle() {
        _applyStyleToComponentState(_calculateStyle());
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

    void addAnimationPainter( AnimationState state, Painter painter ) {
        _animationPainters.put(Objects.requireNonNull(state.lifetime()), Objects.requireNonNull(painter));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    void addAnimationStyler( AnimationState state, Styler<C> styler ) {
        _animationStylers.put(Objects.requireNonNull(state.lifetime()), Objects.requireNonNull(styler));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    private StyleRenderer<C> _createRenderer() {
        Style style = _applyStyleToComponentState(_calculateStyle());
        return style.equals(Style.none()) ? null : new StyleRenderer<>(_owner, style);
    }

    private Optional<StyleRenderer<C>> _getOrCreateRenderer() {
        if ( _currentRenderer == null )
            _currentRenderer = _createRenderer();

        return Optional.ofNullable(_currentRenderer);
    }

    void renderBaseStyle( Graphics g )
    {
        if ( _customLookAndFeelIsInstalled() )
            return; // We render through the custom installed UI!

        if ( _componentIsDeclaredInUI(_owner) )
            _renderBaseStyle(g);
        else
            _currentRenderer = null; // custom style rendering unfortunately not possible for this component :/
    }

    private void _renderBaseStyle(Graphics g ) {
        _currentRenderer = _createRenderer();
        if ( _currentRenderer != null )
            _currentRenderer.renderBaseStyle((Graphics2D) g);
    }


    public void _renderAnimations(Graphics2D g2d)
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Enable antialiasing again:
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

    public void renderForegroundStyle(Graphics2D g2d) {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

        if ( _currentRenderer != null )
            _currentRenderer.renderForegroundStyle(g2d);

        // Enable antialiasing again:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
    }

    private Style _calculateStyle() {
        _styleSheet = _styleSheet != null ? _styleSheet : UI.SETTINGS().getStyleSheet().orElse(null);
        Style style = _styleSheet == null ? Style.none() : _styleSheet.run( _owner );
        style = _styling.style(new StyleDelegate<>(_owner, style)).style();

        // Animations styles are last: they override everything else:
        for ( Map.Entry<LifeTime, Styler<C>> entry : new ArrayList<>(_animationStylers.entrySet()) )
            if ( entry.getKey().isExpired() )
                _animationStylers.remove(entry.getKey());
            else {
                try {
                    style = entry.getValue().style(new StyleDelegate<>(_owner, style)).style();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a styler should not prevent other stylers from being applied!
                }
            }

        return style;
    }

    private Style _applyStyleToComponentState( Style style )
    {
        Objects.requireNonNull(style);

        if ( Style.none().equals(style) ) {
            _uninstallCustomLaF();
            if ( _animationStylers.isEmpty() && _animationPainters.isEmpty() )
                _uninstallCustomBorderBasedStyleAndAnimationRenderer();
            if ( _initialBackgroundColor != null ) {
                _owner.setBackground(_initialBackgroundColor);
                _initialBackgroundColor = null;
            }
            return style;
        }

        boolean hasBorderRadius = style.border().hasAnyNonZeroArcs();

        if ( style.background().color().isPresent() && !Objects.equals( _owner.getBackground(), style.background().color().get() ) ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            _owner.setBackground( style.background().color().get() );
        }

        // If the style has a border radius set we need to make sure that we have a background color:
        if ( hasBorderRadius && !style.background().color().isPresent() ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            style = style.backgroundColor(_initialBackgroundColor);
        }

        if ( style.border().color().isPresent() && style.border().widths().average() > 0 ) {
            if ( !style.background().foundationColor().isPresent() )
                style = style.foundationColor( _owner.getBackground() );
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

            if ( ! newPrefSize.equals(prefSize) )
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

        _installCustomBorderBasedStyleAndAnimationRenderer();

        _establishLookAndFeel( style );

        if ( style.foreground().hasPainters() )
            _makeAllChildrenTransparent(_owner);

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
        boolean hasBackgroundPainter = style.background().hasCustomPainters();
        boolean hasBackgroundShades = style.background().hasCustomShades();

        if ( hasBorderRadius )
            weNeedToOverrideLaF = true;

        if ( hasMargin )
            weNeedToOverrideLaF = true;

        if ( hasBackgroundPainter )
            weNeedToOverrideLaF = true;

        if ( hasBackgroundShades )
            weNeedToOverrideLaF = true;

        if ( style.background().hasCustomPainters() )
            weNeedToOverrideLaF = true;

        if ( style.anyVisibleShadows() )
            weNeedToOverrideLaF = true;

        if ( weNeedToOverrideLaF ) {
            boolean foundationIsTransparent = style.background()
                                                    .foundationColor()
                                                    .map( c -> c.getAlpha() < 255 )
                                                    .orElse(
                                                        Optional.ofNullable(_owner.getBackground())
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
        else if ( _styleLaF != null )
            _uninstallCustomLaF();
    }

    private boolean _installCustomLaF() {
        // First we check if we already have a custom LaF installed:
        if ( _styleLaF != null )
            return true;

        if ( _owner instanceof JPanel ) {
            JPanel p = (JPanel) _owner;
            _formerLaF = p.getUI();
            PanelStyler laf = PanelStyler.INSTANCE;
            p.setUI(laf);
            _styleLaF = laf;
            return true;
        }
        else if ( _owner instanceof AbstractButton ) {
            AbstractButton b = (AbstractButton) _owner;
            _formerLaF = b.getUI();
            ButtonStyler laf = new ButtonStyler(b.getUI());
            b.setUI(laf);
            _styleLaF = laf;
            return true;
        }
        else if ( _owner instanceof JLabel ) {
            JLabel l = (JLabel) _owner;
            _formerLaF = l.getUI();
            LabelStyler laf = new LabelStyler(l.getUI());
            l.setUI(laf);
            _styleLaF = laf;
            return true;
        }

        return false;
    }

    private void _uninstallCustomLaF() {
        if ( _styleLaF != null ) {
            if ( _owner instanceof JPanel ) {
                JPanel p = (JPanel) _owner;
                p.setUI((PanelUI) _formerLaF);
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
            _owner.setBorder(border.getDelegate());
        }
    }

    private void checkIfIsDeclaredInUI() {
        boolean isSwingTreeComponent = _componentIsDeclaredInUI(_owner);
        if ( !isSwingTreeComponent ) // We throw an exception if the component is not a sub-type of any of the classes declared in UI.
            throw new RuntimeException(
                    "Custom (declarative) rendering is only allowed/supported for Swing-Tree components declared in UI."
                );
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


    static class BorderStyleAndAnimationRenderer<C extends JComponent> implements Border
    {
        private final ComponentExtension<C> _compExt;
        private final Border _formerBorder;
        private final boolean _borderWasNotPainted;
        private Insets _currentInsets;
        private Insets _currentMarginInsets = new Insets(0,0,0,0);

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

        public Border getDelegate() { return _formerBorder; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            _checkIfInsetsChanged();
            // We remember the clip:
            Shape formerClip = g.getClip();
            g.setClip(null);

            if ( _compExt._currentRenderer != null )
                _compExt._currentRenderer.renderBorderStyle((Graphics2D) g);
            else if ( _formerBorder != null && !_borderWasNotPainted )
                _formerBorder.paintBorder(c, g, x, y, width, height);

            if ( formerClip != null )
                g.setClip(formerClip);

            _compExt._renderAnimations( (Graphics2D) g );
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
            _currentMarginInsets = _compExt._getOrCreateRenderer()
                                            .map( r -> r.calculateMarginInsets() )
                                            .orElse(_currentMarginInsets);

            return _compExt._getOrCreateRenderer()
                            .map(r ->
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

        @Override public boolean isBorderOpaque() { return false; }
    }

    static class PanelStyler extends BasicPanelUI
    {
        static final PanelStyler INSTANCE = new PanelStyler();

        private PanelStyler() {}

        @Override public void paint( Graphics g, JComponent c ) { ComponentExtension.from(c)._renderBaseStyle(g); }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class ButtonStyler extends BasicButtonUI
    {
        private final ButtonUI _formerUI;

        ButtonStyler(ButtonUI formerUI) { _formerUI = formerUI; }

        @Override public void paint( Graphics g, JComponent c ) {
            ComponentExtension.from(c)._renderBaseStyle(g);
            if ( _formerUI != null )
                _formerUI.update(g, c);
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
            ComponentExtension.from(c)._renderBaseStyle(g);
            if ( _formerUI != null )
                _formerUI.update(g, c);
        }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
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

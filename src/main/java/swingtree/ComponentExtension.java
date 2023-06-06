package swingtree;

import swingtree.style.Painter;
import swingtree.style.Style;
import swingtree.style.StyleDelegate;
import swingtree.style.StyleRenderer;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;

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

    private final List<Painter> _animationPainters = new ArrayList<>(0);

    private final List<String> _styleGroups = new ArrayList<>(0);

    private StyleRenderer<C> _currentRenderer = null;
    private ComponentUI _styleLaF = null;
    private ComponentUI _formerLaF = null;
    private Function<StyleDelegate<C>, Style> _styling = null;


    private ComponentExtension( C owner ) {
        _owner = Objects.requireNonNull(owner);
    }

    private boolean _customLookAndFeelIsInstalled() { return _styleLaF != null; }

    void addStyling( Function<StyleDelegate<C>, Style> styler ) {
        Objects.requireNonNull(styler);
        checkIfIsDeclaredInUI();
        if ( _styling == null )
            _styling = styler;
        else
            _styling = _styling.andThen(s -> styler.apply(new StyleDelegate<>(_owner, s)));

        _calculateStyle().ifPresent(this::_applyStyleToComponentState);
    }

    public void setStyleGroups( String... styleName ) {
        Objects.requireNonNull(styleName);
        if ( !_styleGroups.isEmpty() )
            throw new IllegalStateException("Style groups already specified!");

        _styleGroups.addAll( java.util.Arrays.asList(styleName) );
    }

    public List<String> getStyleGroups() { return Collections.unmodifiableList(_styleGroups); }

    public void clearAnimationRenderer() { _animationPainters.clear(); }

    void addAnimationPainter( Painter painter ) { _animationPainters.add(Objects.requireNonNull(painter)); }

    private StyleRenderer<C> _createRenderer() {
        Style style = _calculateStyle().map( s -> _applyStyleToComponentState( s ) ).orElse(null);
        return style == null ? null : new StyleRenderer<>(_owner, style);
    }

    private Optional<StyleRenderer<C>> _getOrCreateRenderer() {
        if ( _currentRenderer == null )
            _currentRenderer = _createRenderer();

        return Optional.ofNullable(_currentRenderer);
    }

    void renderComponent( Graphics g, Runnable superPaint )
    {
        if ( _customLookAndFeelIsInstalled() ) {
            superPaint.run(); // We render through the custom installed UI!
            return;
        }

        if ( _componentIsDeclaredInUI(_owner) ) {
            _currentRenderer = _createRenderer();
            if ( _currentRenderer != null )
                _currentRenderer.renderBaseStyle((Graphics2D) g);
        }
        else
            _currentRenderer = null; // custom style rendering unfortunately not possible for this component :/

        superPaint.run();
        _renderAnimations( (Graphics2D) g );
    }

    void renderComponent( Graphics g ) {
        _currentRenderer = _createRenderer();
        if ( _currentRenderer != null )
            _currentRenderer.renderBaseStyle((Graphics2D) g);
        _renderAnimations( (Graphics2D) g );
    }


    public void _renderAnimations(Graphics2D g2d)
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Enable antialiasing again:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // Animations are last: they are rendered on top of everything else:
        for ( Painter painter : _animationPainters )
            painter.paint(g2d);

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

    }

    public void renderForeground(Graphics2D g2d) {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

        if ( _currentRenderer != null )
            _currentRenderer.renderForegroundStyle(g2d);

        // Enable antialiasing again:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
    }

    private Optional<Style> _calculateStyle() {
        Optional<Style> style = UI.SETTINGS()
                                    .getStyleSheet()
                                    .map( ss -> ss.run(_owner) );
        if ( _styling == null )
            return style;
        else
            return Optional.of( _styling.apply(new StyleDelegate<>(_owner, style.orElse(Style.none()))) );
    }

    private Style _applyStyleToComponentState( Style style )
    {
        Objects.requireNonNull(style);
        /*
            Note that in SwingTree we do not override the UI classes of Swing to apply styles.
            Instead, we use the "ComponentExtension" class to render styles on components.
            This is because we don't want to means with the current LaF of the application
            and instead simply allow users to carefully replace the LaF with a custom one.
            So when the user has set a border style, we remove the border of the component!
            And if the user has set a background color, we make sure that the component
            is not opaque, so that the background color is visible.
            ... and so on.
        */
        boolean hasBorderRadius = style.border().hasAnyNonZeroArcs();
        // If the style has a border radius set we need to make sure that we have a background color:
        if ( hasBorderRadius && !style.background().color().isPresent() )
            style = style.backgroundColor( _owner.getBackground() );

        if ( style.border().color().isPresent() && style.border().widths().average() > 0 ) {
            if ( !style.background().foundationColor().isPresent() )
                style = style.foundationColor( _owner.getBackground() );
        }

        if ( style.foreground().color().isPresent() && !Objects.equals( _owner.getForeground(), style.foreground().color().get() ) )
            _owner.setForeground( style.foreground().color().get() );

        if ( style.background().color().isPresent() && !Objects.equals( _owner.getBackground(), style.background().color().get() ) )
            _owner.setBackground( style.background().color().get() );

        if ( _owner instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) _owner;
            if ( style.font().selectionColor().isPresent() && ! Objects.equals( tc.getSelectionColor(), style.font().selectionColor().get() ) )
                tc.setSelectionColor(style.font().selectionColor().get());
        }

        style.font()
             .createDerivedFrom(_owner.getFont())
             .ifPresent( newFont -> {
                    if ( !newFont.equals(_owner.getFont()) )
                        _owner.setFont( newFont );
                });

        _applyPadding( style );

        _establishLookAndFeel( style );

        if ( style.foreground().painter().isPresent() )
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

        if ( hasBorderRadius )
            weNeedToOverrideLaF = true;

        if ( hasMargin )
            weNeedToOverrideLaF = true;

        if ( style.background().painter().isPresent() )
            weNeedToOverrideLaF = true;

        if ( style.shadow().color().isPresent() )
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
            if ( !success && _owner instanceof AbstractButton ) {
                AbstractButton b = (AbstractButton) _owner;
                b.setContentAreaFilled(false);
                //b.setBorderPainted(false);
            }
        }
        else if ( _styleLaF != null ) {
            _uninstallCustomLaF();
        }
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
        //else if ( _owner instanceof AbstractButton ) {
        //    AbstractButton b = (AbstractButton) _owner;
        //    _formerLaF = b.getUI();
        //    ButtonStyler laf = new ButtonStyler(b.getUI());
        //    b.setUI(new ButtonStyler(b.getUI()));
        //    _styleLaF = laf;
        //}

        return false;
    }

    private void _uninstallCustomLaF() {
        if ( _styleLaF != null ) {
            if ( _owner instanceof JPanel ) {
                JPanel p = (JPanel) _owner;
                p.setUI((PanelUI) _formerLaF);
                _styleLaF = null;
            }
            //else if ( _owner instanceof AbstractButton ) {
            //    AbstractButton b = (AbstractButton) _owner;
            //    b.setUI((ButtonUI) _formerLaF);
            //    _styleLaF = null;
            //}
        }
    }

    private void _applyPadding( Style style ) {
        Border currentBorder = _owner.getBorder();
        if ( !(currentBorder instanceof CustomBorder) )
            _owner.setBorder(new CustomBorder<>(this, currentBorder));
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


    static class CustomBorder<C extends JComponent> implements Border
    {
        private final ComponentExtension<C> _compExt;
        private final Border _formerBorder;
        private Insets _currentInsets;

        CustomBorder(ComponentExtension<C> compExt, Border formerBorder) {
            _compExt = compExt;
            _currentInsets = null;
            _formerBorder = formerBorder;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            _checkIfInsetsChanged();
            if ( _compExt._currentRenderer != null )
                _compExt._currentRenderer.renderBorderStyle((Graphics2D) g);
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
            return _compExt._getOrCreateRenderer()
                            .map(r -> r.calculateBorderInsets(_formerBorder == null ? new Insets(0,0,0,0) : _formerBorder.getBorderInsets(_compExt._owner)))
                            .orElse(new Insets(0, 0, 0, 0));
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    static class PanelStyler extends BasicPanelUI
    {
        static final PanelStyler INSTANCE = new PanelStyler();

        private PanelStyler() {}

        @Override
        public void paint( Graphics g, JComponent c ) { ComponentExtension.from(c).renderComponent(g); }
        @Override
        public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public int getBaseline( JComponent c, int width, int height ) {
            super.getBaseline(c, width, height);
            Border border = c.getBorder();
            if ( border instanceof AbstractBorder )
                return ((AbstractBorder)border).getBaseline(c, width, height);

            return -1;
        }

        @Override
        public Component.BaselineResizeBehavior getBaselineResizeBehavior( JComponent c ) {
            super.getBaselineResizeBehavior(c);
            Border border = c.getBorder();
            if ( border instanceof AbstractBorder )
                return ((AbstractBorder)border).getBaselineResizeBehavior(c);

            return Component.BaselineResizeBehavior.OTHER;
        }
    }

}

package swingtree;

import net.miginfocom.swing.MigLayout;
import swingtree.style.Painter;
import swingtree.style.Style;
import swingtree.style.StyleDelegate;
import swingtree.style.StyleRenderer;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.*;
import java.util.List;
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

    private boolean _customLookAndFeelIsInstalled() {
        return _styleLaF != null;
    }

    void addStyling( Function<StyleDelegate<C>, Style> styler ) {
        Objects.requireNonNull(styler);
        checkIfIsDeclaredInUI();
        if ( _styling == null )
            _styling = styler;
        else
            _styling = _styling.andThen(s -> styler.apply(new StyleDelegate<>(_owner, s)));

        _calculateStyle().ifPresent( s -> _applyStyleToComponentState(s, null) );
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

    private StyleRenderer<C> _createRenderer(Graphics2D g2d) {
        Style style = _calculateStyle().map( s -> _applyStyleToComponentState( s, g2d) ).orElse(null);
        return style == null ? null : new StyleRenderer<>(g2d, _owner, style);
    }


    void renderComponent( Graphics g, Runnable superPaint )
    {
        if ( _customLookAndFeelIsInstalled() ) {
            superPaint.run(); // We render through the custom installed UI!
            return;
        }

        if ( _componentIsDeclaredInUI(_owner) ) {
            _currentRenderer = _createRenderer((Graphics2D) g);
            if ( _currentRenderer != null )
                _currentRenderer.renderBaseStyle();
        }
        else
            _currentRenderer = null; // custom style rendering unfortunately not possible for this component :/

        superPaint.run();
        _renderAnimations( (Graphics2D) g );
    }

    void renderComponent( Graphics g ) {
        _currentRenderer = _createRenderer((Graphics2D) g);
        if ( _currentRenderer != null )
            _currentRenderer.renderBaseStyle();
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
            _currentRenderer.renderForegroundStyle();

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

    private Style _applyStyleToComponentState( Style style, Graphics2D g2d )
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

        if ( style.border().widths().average() >= 0 && !BorderFactory.createEmptyBorder().equals(_owner.getBorder()) )
            _owner.setBorder( BorderFactory.createEmptyBorder() );

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

                     if ( g2d != null && !newFont.equals(g2d.getFont()) )
                         g2d.setFont( newFont );
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

        else if ( style.border().widths().average() == 0 && _owner instanceof AbstractButton )
            ((AbstractButton) _owner).setBorderPainted(false);

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
                if ( _owner instanceof AbstractButton ) {
                    AbstractButton b = (AbstractButton) _owner;
                    b.setContentAreaFilled(false);
                    b.setBorderPainted(false);
                }
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
        boolean hasBorder        = style.border().widths().average() > 0;
        boolean insTopPresent    = style.margin().top().isPresent()    || style.padding().top().isPresent();
        boolean insLeftPresent   = style.margin().left().isPresent()   || style.padding().left().isPresent();
        boolean insBottomPresent = style.margin().bottom().isPresent() || style.padding().bottom().isPresent();
        boolean insRightPresent  = style.margin().right().isPresent()  || style.padding().right().isPresent();
        int insTop    = Math.max(style.margin().top().orElse(0),    0) + style.padding().top().orElse(0);
        int insLeft   = Math.max(style.margin().left().orElse(0),   0) + style.padding().left().orElse(0);
        int insBottom = Math.max(style.margin().bottom().orElse(0), 0) + style.padding().bottom().orElse(0);
        int insRight  = Math.max(style.margin().right().orElse(0),  0) + style.padding().right().orElse(0);
        boolean anyPadding = insTopPresent || insLeftPresent || insBottomPresent || insRightPresent;
        if ( anyPadding ) {
            // Let's adjust for border width:
            if ( hasBorder ) {
                insTop    += style.border().widths().top().orElse(0);
                insLeft   += style.border().widths().left().orElse(0);
                insBottom += style.border().widths().bottom().orElse(0);
                insRight  += style.border().widths().right().orElse(0);
            }
            Insets insets = Optional.ofNullable(_owner.getInsets()).orElse( new Insets(0,0,0,0) );
            boolean alreadyEqual = insets.top == insTop && insets.left == insLeft && insets.bottom == insBottom && insets.right == insRight;
            if ( !alreadyEqual ) {
                if ( insTopPresent || hasBorder )
                    insets.top = insTop;
                if ( insLeftPresent || hasBorder )
                    insets.left = insLeft;
                if ( insBottomPresent || hasBorder )
                    insets.bottom = insBottom;
                if ( insRightPresent || hasBorder )
                    insets.right = insRight;

                // We have to let the layout manager know about the new insets,
                // let's go through the various layout managers:
                LayoutManager lm = _owner.getLayout();
                if ( lm instanceof MigLayout ) {
                    MigLayout migLayout = (MigLayout) lm;
                    String lc = Optional.ofNullable(migLayout.getLayoutConstraints()).map(Object::toString).orElse("");
                    Optional<Insets> found = _parseInsets( lc );
                    if ( found.isPresent() ) {
                        if ( found.get().equals(insets) )
                            return;

                        Insets old = found.get();
                        if ( insTop < 0 )
                            insets.top = old.top;
                        if ( insLeft < 0 )
                            insets.left = old.left;
                        if ( insBottom < 0 )
                            insets.bottom = old.bottom;
                        if ( insRight < 0 )
                            insets.right = old.right;
                    }

                    String newConstr = "insets " + insets.top + "px " + insets.left + "px " + insets.bottom + "px " + insets.right + "px";
                    if ( lc.isEmpty() )
                        migLayout.setLayoutConstraints( newConstr );
                    else if ( !lc.contains("ins") )
                        migLayout.setLayoutConstraints( lc + ", " + newConstr );
                    else {
                        lc = lc.replace("insets", "ins");
                        String newLayoutConstraints = lc.replaceAll("ins [a-z0-9 ]+", newConstr);
                        migLayout.setLayoutConstraints( newLayoutConstraints );
                    }
                    // Now we need to make sure the layout manager is updated:
                    _owner.revalidate();
                }
                else if ( lm instanceof GridBagLayout ) {
                    GridBagLayout gridBagLayout = (GridBagLayout) lm;
                    GridBagConstraints gbc = gridBagLayout.getConstraints( _owner );
                    // First let's check if the insets are already equal:
                    if ( gbc.insets.equals(insets) )
                        return;

                    gbc.insets = insets;
                    gridBagLayout.setConstraints( _owner, gbc );
                    _owner.revalidate();
                } else {
                    /*
                        One hacky way to set the insets is to set the border!
                        But we don't want to do this if the user has set their own border.
                        So we are going to check if the current border is null or an empty border,
                        and then we are going to set the border to an empty border with the insets.
                     */
                    Border border = _owner.getBorder();
                    if ( border == null || border instanceof EmptyBorder ) {
                        // First let's check if the insets are already equal:
                        if ( border != null ) {
                            EmptyBorder emptyBorder = (EmptyBorder) border;
                            if ( emptyBorder.getBorderInsets().equals(insets) )
                                return;
                        }
                        _owner.setBorder(
                                BorderFactory.createEmptyBorder( insets.top, insets.left, insets.bottom, insets.right )
                            );
                        _owner.revalidate();
                    }
                }
            }
        }
    }

    private Optional<Insets> _parseInsets( String layoutConstraints ) {
        if ( layoutConstraints == null || layoutConstraints.isEmpty() )
            return Optional.empty();

        // Now we tokenize the layout constraints:
        String[] tokens = layoutConstraints.split(",");
        for ( String token : tokens ) {
            token = token.trim();
            if ( token.startsWith("ins") ) {
                String[] insets = token.split(" ");
                int[] insetsInt = new int[4];
                for ( int i = 0; i < insetsInt.length; i++ ) {
                    String numberStr = _stripAlphabeticalChars( insets[ 1 + ( i % ( insets.length - 1 ) ) ] );
                    insetsInt[i] = Integer.parseInt( numberStr );
                }

                return Optional.of( new Insets( insetsInt[0], insetsInt[1], insetsInt[2], insetsInt[3] ) );
            }
        }
        return Optional.empty();
    }

    private String _stripAlphabeticalChars( String str ) {
        return str.replaceAll("[a-zA-Z]", "");
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

package swingtree;

import net.miginfocom.swing.MigLayout;
import swingtree.style.Style;
import swingtree.style.StyleDelegate;
import swingtree.style.StyleRenderer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
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

    private Function<StyleDelegate<C>, Style> _styling = null;

    private List<Consumer<Graphics2D>> _animationRenderers;

    private String[] _styleGroups = new String[0];


    private ComponentExtension( C owner ) {
        _owner = Objects.requireNonNull(owner);
    }

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
        _styleGroups = Objects.requireNonNull(styleName);
    }

    public List<String> getStyleGroups() {
        return java.util.Arrays.asList(_styleGroups);
    }

    public void clearAnimationRenderer() {
        _animationRenderers = null;
    }

    void addAnimationRenderer( Consumer<Graphics2D> renderer ) {
        Objects.requireNonNull(renderer);
        if ( _animationRenderers == null )
            _animationRenderers = new java.util.ArrayList<>();

        _animationRenderers.add(renderer);
    }

    void render( Graphics g, Runnable superPaint ) {
        Objects.requireNonNull(g);
        Objects.requireNonNull(superPaint);

        Graphics2D g2d = (Graphics2D) g;
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        {
            _calculateStyle().ifPresent( style -> {
                style = _applyStyleToComponentState( style );
                if ( _componentIsDeclaredInUI(_owner) )
                    new StyleRenderer<>( g2d, _owner ).renderStyle( style );
            });
        }
        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

        superPaint.run();

        // Enable antialiasing again:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // Animations are last: they are rendered on top of everything else:
        if ( _animationRenderers != null )
            for ( Consumer<Graphics2D> renderer : _animationRenderers)
                renderer.accept(g2d);

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    private Optional<Style> _calculateStyle() {
        Optional<Style> style = UI.SETTINGS()
                                    .getStyleSheet()
                                    .map( ss -> ss.run(_owner) );
        if ( _styling == null )
            return style;
        else
            return Optional.of( _styling.apply(new StyleDelegate<>(_owner, style.orElse(Style.blank()))) );
    }

    private Style _applyStyleToComponentState( Style style )
    {
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
        boolean hasBorderRadius = style.border().arcWidth() > 0 || style.border().arcHeight() > 0;
        // If the style has a border radius set we need to make sure that we have a background color:
        if ( hasBorderRadius && !style.background().color().isPresent() )
            style = style.backgroundColor( _owner.getBackground() );

        if ( style.border().width() >= 0 && !BorderFactory.createEmptyBorder().equals(_owner.getBorder()) )
            _owner.setBorder( BorderFactory.createEmptyBorder() );

        if ( style.border().color().isPresent() && style.border().width() > 0 ) {
            if ( !style.background().foundationColor().isPresent() )
                style = style.foundationColor( _owner.getBackground() );
        }

        if ( _owner.isOpaque() ) {
            if ( style.background().color().isPresent() )
                _owner.setOpaque(false);

            if ( style.background().foundationColor().isPresent() )
                _owner.setOpaque(false);

            if ( style.background().renderer().isPresent() )
                _owner.setOpaque(false);

            if ( style.shadow().color().isPresent() )
                _owner.setOpaque(false);
        }
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

        return style;
    }

    private void _applyPadding(Style style ) {
        boolean hasBorder        = style.border().width() > 0;
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
                int borderWidth = style.border().width();
                insTop    += borderWidth;
                insLeft   += borderWidth;
                insBottom += borderWidth;
                insRight  += borderWidth;
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

                    String newConstr = "insets " + insets.top + " " + insets.left + " " + insets.bottom + " " + insets.right;
                    if ( lc.isEmpty() )
                        migLayout.setLayoutConstraints( newConstr );
                    else if ( !lc.contains("ins") )
                        migLayout.setLayoutConstraints( lc + ", " + newConstr );
                    else {
                        lc = lc.replace("insets", "ins");
                        String newLayoutConstraints = lc.replaceAll("ins [0-9]+", newConstr);
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
                for ( int i = 0; i < insetsInt.length; i++ )
                    insetsInt[i] = Integer.parseInt( insets[ 1 + ( i % ( insets.length - 1 ) ) ] );

                return Optional.of( new Insets( insetsInt[0], insetsInt[1], insetsInt[2], insetsInt[3] ) );
            }
        }
        return Optional.empty();
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

}

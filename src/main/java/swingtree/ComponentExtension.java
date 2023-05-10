package swingtree;

import swingtree.style.StyleRenderer;
import swingtree.style.Style;
import swingtree.style.StyleDelegate;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.Objects;
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

    private Function<StyleDelegate<C>, Style> _styling = StyleDelegate::style;

    private List<Consumer<Graphics2D>> _animationRenderers;

    private String[] _styleGroups = new String[0];


    private ComponentExtension( C owner ) {
        _owner = Objects.requireNonNull(owner);
    }

    void addStyling( Function<StyleDelegate<C>, Style> styler ) {
        checkIfIsDeclaredInUI();
        _styling = _styling.andThen(s -> Objects.requireNonNull(styler).apply(new StyleDelegate<>(_owner, s)));
        Style style = _calculateStyle();
        _applyStyleToComponentState( style );
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
            Style style = _calculateStyle();
            _applyStyleToComponentState( style );
            if ( _componentIsDeclaredInUI(_owner) )
                new StyleRenderer<>( g2d, _owner ).renderStyle( style );
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

    private Style _calculateStyle() {
        Style style = UI.SETTINGS().getStyleSheet().map( ss -> ss.run(_owner) ).orElse(Style.blank());
        return _styling.apply(new StyleDelegate<>(_owner, style));
    }

    private void _applyStyleToComponentState( Style style ) {

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
        if ( style.border().thickness() >= 0 )
            _owner.setBorder( BorderFactory.createEmptyBorder() );

        if ( style.border().color().isPresent() && style.border().thickness() > 0 ) {
            if ( !style.background().foundationColor().isPresent() )
                style = style.foundationColor( _owner.getBackground() );
        }

        if ( style.background().color().isPresent() )
            _owner.setOpaque( false );

        if ( style.background().foundationColor().isPresent() )
            _owner.setOpaque( false );

        if ( style.background().renderer().isPresent() )
            _owner.setOpaque( false );

        if ( style.shadow().color().isPresent() )
            _owner.setOpaque( false );

        if ( _owner instanceof JTextComponent) {
            if (style.font().selectionColor().isPresent())
                ((JTextComponent) _owner).setSelectionColor(style.font().selectionColor().get());
        }

        _owner.setFont( style.font().createDerivedFrom(_owner.getFont()) );
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

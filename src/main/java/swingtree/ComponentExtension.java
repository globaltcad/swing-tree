package swingtree;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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

    private final JComponent _owner;

    private Consumer<RenderDelegate<C>> _backgroundRenderer;
    private Consumer<RenderDelegate<C>> _foregroundRenderer;

    private List<Consumer<Graphics2D>> _animationRenderers;


    private ComponentExtension( JComponent owner ) {
        _owner = Objects.requireNonNull(owner);
    }

    void setBackgroundRenderer( Consumer<RenderDelegate<C>> renderer ) {
        checkIfIsDeclaredInUI();
        _backgroundRenderer = Objects.requireNonNull(renderer);
    }

    void setForegroundRenderer( Consumer<RenderDelegate<C>> renderer ) {
        checkIfIsDeclaredInUI();
        _foregroundRenderer = Objects.requireNonNull(renderer);
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

    void render( C comp, Graphics g, Runnable superPaint ) {
        Objects.requireNonNull(comp);
        Objects.requireNonNull(g);
        Objects.requireNonNull(superPaint);

        Graphics2D g2d = (Graphics2D) g;

        if ( _backgroundRenderer != null )
            _backgroundRenderer.accept( new RenderDelegate<>(g2d, comp) );

        superPaint.run();

        if ( _foregroundRenderer != null )
            _foregroundRenderer.accept( new RenderDelegate<>(g2d, comp) );

        // Animations are last: they are rendered on top of everything else:
        if ( _animationRenderers != null )
            for ( Consumer<Graphics2D> renderer : _animationRenderers)
                renderer.accept(g2d);
    }

    private void checkIfIsDeclaredInUI() {
        boolean isSwingTreeComponent = _checkIfIsDeclaredInUI(_owner);
        if ( !isSwingTreeComponent ) // We throw an exception if the component is not a sub-type of any of the classes declared in UI.
            throw new RuntimeException(
                    "Custom (declarative) rendering is only allowed/supported for Swing-Tree components declared in UI."
            );
    }

    static boolean _checkIfIsDeclaredInUI( JComponent comp ) {
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

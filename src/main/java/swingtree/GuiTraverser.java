package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *  Traverses the component tree starting from a given component and finds all components
 *  that match a supplied predicate and type.
 */
final class GuiTraverser
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GuiTraverser.class);
    private final Component _current;


    GuiTraverser( final Component current ) {
        Objects.requireNonNull(current);
        _current = current;
    }

    <C extends Component> Stream<C> find( final Class<C> type, final Predicate<C> predicate ) {
        return _find( c -> {
                   boolean isType = type.isAssignableFrom(c.getClass());
                   if ( !isType )
                       return false;
                   try {
                       return predicate.test(type.cast(c));
                   } catch (Exception e) {
                       log.error(
                               "An exception occurred while testing a component of type '{}'!",
                               type.getSimpleName(), e
                            );
                       return false;
                   }
               })
               .map( type::cast );
    }

    private Stream<Component> _find( Predicate<Component> predicate ) {
        List<Component> roots = traverseUpwardsAndFindAllRoots(_current, new ArrayList<>());
        return roots.stream()
                    .flatMap( c -> _traverseDownwardsAndFind(c, predicate).stream() );
    }

    private List<Component> traverseUpwardsAndFindAllRoots(
        final Component component,
        final List<Component> roots
    ) {
        Component parent = _findRootParentOf(component);
        roots.add(parent);
        Container grandParent = parent.getParent();
        if ( grandParent != null ) {
            return traverseUpwardsAndFindAllRoots(grandParent, roots);
        }
        else
            return roots;
    }

    private Component _findRootParentOf( final Component component ) {
        Container parent = component.getParent();
        if ( _acknowledgesParenthood( parent, component ) )
            return _findRootParentOf( parent );
        else
            return component;
    }

    private boolean _acknowledgesParenthood( final Component parent, final Component child ) {
        if ( parent instanceof Container ) {
            Container container = (Container) parent;
            for ( Component component : container.getComponents() )
                if ( component == child )
                    return true;
        }
        return false;
    }

    private List<Component> _traverseDownwardsAndFind( final Component cmp, final Predicate<Component> predicate )
    {
        List<Component> found = new ArrayList<>();
        _traverseDownwardsAndFind(cmp, predicate, found);
        return found;
    }

    private void _traverseDownwardsAndFind(
        final @Nullable Component cmp,
        final Predicate<Component> predicate,
        final List<Component> found
    ) {
        if( cmp == null )
            return; // Not a container, return
        // Add this component
        if ( predicate.test(cmp) && !found.contains(cmp) )
            found.add(cmp);

        if ( cmp instanceof Container ) { // A container, let's traverse it.
            Container container = (Container) cmp;
            // Go visit and add all children
            for ( Component subComponent : container.getComponents() )
                _traverseDownwardsAndFind(subComponent, predicate, found);
        }
    }
}

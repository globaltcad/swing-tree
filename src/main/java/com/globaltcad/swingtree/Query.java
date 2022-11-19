package com.globaltcad.swingtree;

import java.awt.*;
import java.util.*;
import java.util.List;

final class Query
{
    private final Component _current;
    private final Map<String, List<Component>> _tree = new LinkedHashMap<>();

    Query(Component current) { _current = current; }

    <C extends Component> OptionalUI<C> find(Class<C> type, String id) {
        if ( !_tree.containsKey(id) ) {
            _tree.clear();
            List<Component> roots = traverseUpwards(_current, new ArrayList<>());
            roots.stream().forEach(this::_traverseDownwardsAndFillTree);
        }
        return _tree.getOrDefault(id, new ArrayList<>())
                .stream()
                .filter( c -> type.isAssignableFrom(c.getClass()) )
                .map( c -> (C) c )
                .findFirst()
                .map(OptionalUI::ofNullable)
                .get();
    }

    private List<Component> traverseUpwards(Component component, List<Component> roots)
    {
        Component parent = _findRootParentOf(component);
        roots.add(parent);
        if ( parent.getParent() != null ) {
            return traverseUpwards(parent.getParent(), roots);
        }
        else
            return roots;
    }

    private Component _findRootParentOf( Component component ) {
        Container parent = component.getParent();
        if ( _acknowledgesParenthood( parent, component ) )
            return _findRootParentOf( parent );
        else
            return component;
    }

    private boolean _acknowledgesParenthood( Component parent, Component child ) {
        if ( parent instanceof Container ) {
            Container container = (Container) parent;
            for ( Component component : container.getComponents() )
                if ( component == child )
                    return true;
        }
        return false;
    }

    private void _traverseDownwardsAndFillTree( Component cmp )
    {
        if( cmp == null ) return; // Not a container, return
        // Add this component
        List<Component> found = _tree.computeIfAbsent(cmp.getName(), k -> new ArrayList<>());
        if ( !found.contains(cmp) )
            found.add(cmp);

        if ( cmp instanceof Container ) { // A container, let's traverse it.
            Container container = (Container) cmp;
            // Go visit and add all children
            for ( Component subComponent : container.getComponents() )
                _traverseDownwardsAndFillTree(subComponent);
        }
    }
}

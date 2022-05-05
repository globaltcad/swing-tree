package com.globaltcad.swingtree;

import java.awt.*;
import java.util.*;
import java.util.List;

class Query
{
    private final Component _current;
    private final Map<String, List<Component>> _tree = new LinkedHashMap<>();

    Query(Component current) { _current = current; }

    <C extends Component> Optional<C> find(Class<C> type, String id) {
        if ( !_tree.containsKey(id) ) {
            _tree.clear();
            Component root = traverseUpwards(_current);
            traverseDownwards(root);
        }
        return _tree.getOrDefault(id, new ArrayList<>())
                    .stream()
                    .filter( c -> type.isAssignableFrom(c.getClass()) )
                    .map( c -> (C) c )
                    .findFirst();
    }

    private Component traverseUpwards(Component component) {
        Container parent = component.getParent();
        if ( parent != null )
            return traverseUpwards(parent);
        else
            return component;
    }

    private void traverseDownwards(Component cmp) {
        // Add this component
        if ( cmp != null ) {
            List<Component> found = _tree.computeIfAbsent(cmp.getName(), k -> new ArrayList<>());
            if ( !found.contains(cmp) )
                found.add(cmp);
        }
        Container container = (Container) cmp;
        if( container == null ) return; // Not a container, return
        // Go visit and add all children
        for ( Component subComponent : container.getComponents() )
            traverseDownwards(subComponent);
    }
}

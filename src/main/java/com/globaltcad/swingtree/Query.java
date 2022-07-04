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
        if( cmp == null ) return; // Not a container, return
        // Add this component
        List<Component> found = _tree.computeIfAbsent(cmp.getName(), k -> new ArrayList<>());
        if ( !found.contains(cmp) )
            found.add(cmp);

        if ( cmp instanceof Container ) { // A container, let's traverse it.
            Container container = (Container) cmp;
            // Go visit and add all children
            for ( Component subComponent : container.getComponents() )
                traverseDownwards(subComponent);
        }
    }
}

package com.globaltcad.swingtree.utility;

import com.alexandriasoftware.swing.JSplitButton;
import com.globaltcad.swingtree.UIForSplitButton;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Utility {

    public static <B extends JSplitButton> String getSplitButtonText(UIForSplitButton<B> ui) {
        return ui.getComponent().getText();
    }

    public static <B extends JSplitButton> JPopupMenu getSplitButtonPopup(UIForSplitButton<B> ui) {
        return ui.getComponent().getPopupMenu();
    }

    public static <B extends JSplitButton> void click(UIForSplitButton<B> ui) {
        ui.getComponent().doClick();
    }


    public static class Query
    {
        private final Component _current;
        private final Map<String, java.util.List<Component>> _tree = new LinkedHashMap<>();

        public Query(Component current) { _current = current; }

        public <C extends Component> Optional<C> find(Class<C> type, String id) {
            if ( !_tree.containsKey(id) ) {
                _tree.clear();
                Component root = _traverseUpwards(_current);
                _traverseDownwards(root);
            }
            return _tree.getOrDefault(id, new ArrayList<>())
                    .stream()
                    .filter( c -> type.isAssignableFrom(c.getClass()) )
                    .map( c -> (C) c )
                    .findFirst();
        }

        private Component _traverseUpwards(Component component) {
            Container parent = component.getParent();
            if ( parent != null )
                return _traverseUpwards(parent);
            else
                return component;
        }

        private void _traverseDownwards(Component cmp) {
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
                _traverseDownwards(subComponent);
        }
    }


}

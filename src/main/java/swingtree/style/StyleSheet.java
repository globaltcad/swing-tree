package swingtree.style;

import swingtree.UI;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class StyleSheet
{
    private final Map<StyleTrait, List<StyleTrait>> _traitGraph = new LinkedHashMap<>();
    private final Map<StyleTrait, Function<Style, Style>> _traitStylers = new LinkedHashMap<>();
    private final List<StyleTrait> _rootTraits = new java.util.ArrayList<>();
    private final List<StyleTrait> _leafTraits = new java.util.ArrayList<>();
    private final List<List<StyleTrait>> _traitPaths = new java.util.ArrayList<>();

    private boolean _traitGraphBuilt = false;

    protected StyleSheet() {
        declaration();
        _buildTraitGraph();
    }

    protected StyleTrait id( String id ) { return new StyleTrait().id(id); }

    protected StyleTrait group( String name ) { return new StyleTrait().group(name); }

    protected StyleTrait type( Class<?> type ) { return new StyleTrait().type(type); }

    protected void apply(StyleTrait rule, Function<Style, Style> traitStyler ) {
        // First let's make sure the trait does not already exist.
        if ( _traitStylers.containsKey(rule) )
            throw new IllegalArgumentException("The trait " + rule.group() + " already exists in this style sheet.");

        // Now let's add the trait to the style sheet.
        _traitStylers.put(rule, traitStyler);
        // And let's add the trait to the trait graph.
        _traitGraph.put(rule, new java.util.ArrayList<>());
    }

    protected abstract void declaration();

    public Style run( JComponent toBeStyled ) {
        return run(toBeStyled, UI.style());
    }

    public Style run( JComponent toBeStyled, Style startingStyle ) {
        if ( !_traitGraphBuilt )
            throw new IllegalStateException("The trait graph has not been built yet.");

        // Now we run the starting style through the trait graph.
        // We do this by finding valid trait paths from the root traits to the leaf traits.
        List<StyleTrait> validTraits = new java.util.ArrayList<>();
        for ( List<StyleTrait> traitPath : _traitPaths ) {
            List<String> inheritedStyleNames = new java.util.ArrayList<>();
            int lastValidTrait = -1;
            int i = 0;
            for ( StyleTrait trait : traitPath ) {
                boolean valid = trait.isApplicableTo(toBeStyled, inheritedStyleNames);
                inheritedStyleNames.add(trait.group());
                if ( valid ) lastValidTrait = i;
                i++;
            }
            if ( lastValidTrait >= 0 ) {
                // We add the path up to the last valid trait to the list of valid traits.
                validTraits.addAll(traitPath.subList(0, lastValidTrait + 1));
            }
        }

        // Now we apply the valid traits to the starting style.
        for ( StyleTrait trait : validTraits )
            startingStyle = _traitStylers.get(trait).apply(startingStyle);

        return startingStyle;
    }

    private void _buildTraitGraph() {
        if ( _traitGraphBuilt )
            throw new IllegalStateException("The trait graph has already been built.");
        /*
            We compare each trait to every other trait to see if it inherits from it.
            If it does, we add the trait to the other trait's list of extension (the value in the map).
        */
        for ( StyleTrait trait : _traitGraph.keySet() )
            for ( StyleTrait other : _traitGraph.keySet() )
                if ( trait != other && trait.thisInherits(other) )
                    _traitGraph.get(other).add(trait);

        /*
            Now we have a graph of traits that inherit from other traits.
            We can use this graph to determine the order in which we apply the traits to a style.
            But first we need to make sure there are no cycles in the graph.

            We do this by performing a depth-first search on the graph.
        */
        for ( StyleTrait trait : _traitGraph.keySet() ) {
            // We use a set to keep track of the traits we've already visited.
            java.util.Set<StyleTrait> visited = new java.util.HashSet<>();
            // We use a stack to keep track of the traits we still need to visit.
            java.util.Stack<StyleTrait> stack = new java.util.Stack<>();

            // We start by adding the current trait to the stack.
            stack.push(trait);

            // While there are still traits to visit...
            while ( !stack.isEmpty() ) {
                // We pop the next trait off the stack.
                StyleTrait current = stack.pop();

                // If we've already visited this trait, then we have a cycle.
                if ( visited.contains(current) )
                    throw new IllegalStateException("The style sheet contains a cycle in the trait graph.");

                // Otherwise, we add the trait to the visited set.
                visited.add(current);

                // And we add all the trait's extensions to the stack.
                stack.addAll(_traitGraph.get(current));
            }
        }
        _findRootAndLeaveTraits();
        _traitGraphBuilt = true;
    }

    private void _findRootAndLeaveTraits() {
        /*
            We find the root traits by finding the traits, which are the head of the graph
            or the traits that have no extensions.
        */
        for ( StyleTrait trait : _traitGraph.keySet() )
            if ( _traitGraph.get(trait).isEmpty() )
                _rootTraits.add(trait);

        /*
            We find the leaf traits by finding the traits, which have extensions,
            but are not referenced by any other trait as an extension.
         */
        for ( StyleTrait trait : _traitGraph.keySet() ) {
            boolean isLeaf = true;
            for ( StyleTrait other : _traitGraph.keySet() )
                if ( _traitGraph.get(other).contains(trait) ) {
                    isLeaf = false;
                    break;
                }
            if ( isLeaf )
                _leafTraits.add(trait);
        }
        /*
            Finally we can calculate all the possible paths from the root traits to the leaf traits.
        */
        _traitPaths.addAll(_findPaths());
    }

    private List<List<StyleTrait>> _findPaths() {
        List<List<StyleTrait>> paths = new java.util.ArrayList<>();
        for ( StyleTrait root : _leafTraits ) {
            List<StyleTrait> path = new java.util.ArrayList<>();
            _traverse(root, path);
            paths.add(path);
        }
        return paths;
    }

    private void _traverse( StyleTrait current, List<StyleTrait> path ) {
        path.add(current);
        if ( _traitGraph.get(current).isEmpty() )
            return;

        for ( StyleTrait extension : _traitGraph.get(current) )
            _traverse(extension, path);
    }

}

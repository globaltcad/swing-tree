package swingtree.style;

import swingtree.api.Styler;

import javax.swing.JComponent;
import java.util.*;
import java.util.function.Function;

/**
 *  An abstract class that can be extended to create custom source code based
 *  style sheets for your Swing application.
 *  <br><br>
 *  A style sheet object is a collection of {@link StyleTrait}s and corresponding {@link Styler} lambdas
 *  which are used to style components in a functional manner.
 *  Implement the {@link #configure()} method and
 *  use the {@link #add(StyleTrait, Styler)} method to
 *  add {@link StyleTrait}s and corresponding {@link Styler} lambdas
 *  to the style sheet.
 *  There are also various factory methods for creating {@link StyleTrait}s
 *  in the form of {@link #id(String)}, {@link #group(String)}, {@link #group(Enum)}, {@link #type(Class)}.
 *  This is designed to make your style sheet code more readable and maintainable.
 *  <br><br>
 *  Here an example of how this class is
 *  typically used to create a custom style sheet:
 *  <pre>{@code
 *  class MyStyleSheet extends StyleSheet {
 *    {@literal @}Override
 *    protected void configure() {
 *      add(group("MyButton"), it -> it
 *        .margin(12)
 *        .padding(16)
 *        .backgroundColor(Color.YELLOW)
 *      );
 *      add(type(JLabel.class).id("Foo"), it-> it
 *        .borderRadius(5)
 *        .gradient("Bar", ... )
 *      );
 *      add(group(Group.ERROR), it -> it
 *        .backgroundColor(Color.RED)
 *        .borderColor(Color.YELLOW)
 *        .borderWidth(2)
 *      );
 *    }
 *  }
 *  }</pre>
 *  Note that the {@link #configure()} method is called once
 *  in the constructor of the style sheet and after that the style sheet may not be modified.
 */
public abstract class StyleSheet
{
    private final Function<JComponent, Style> _defaultStyle;
    private final Map<StyleTrait<?>, Styler<?>> _traitStylers = new LinkedHashMap<>();
    private StyleTrait<?>[][] _traitPaths = {}; // The paths are calculated from the above map and used to apply the styles.

    private boolean _traitGraphBuilt = false;

    protected StyleSheet() {
        this(null);
    }

    protected StyleSheet( StyleSheet parentStyleSheet ) {
        _defaultStyle = c -> {
                            if ( parentStyleSheet == null )
                                return Style.none();
                            else
                                return parentStyleSheet.applyTo( c, Style.none() );
                        };

        reconfigure();
    }

    /**
     *  Essentially (re)initiates the style sheet by clearing all the traits and
     *  then calling the {@link #configure()} method to add new traits to the style sheet.
     *  Use this method if your style sheet has a more advanced meta configuration
     *  which causes the types of traits to change dynamically.
     *  For example, during the new configuration you may want to add
     *  a different set of traits with different {@link Styler}s depending on the current
     *  theme of the application.
     */
    public final void reconfigure() {
        _traitGraphBuilt = false;
        _traitPaths = new StyleTrait<?>[0][];
        _traitStylers.clear();
        try {
            configure(); // The subclass will add traits to this style sheet using the add(..) method.
        } catch ( Exception e ) {
            e.printStackTrace();
            /*
                Exceptions inside a style sheet should not be fatal.
                We just print the stack trace for debugging purposes
                and then continue to prevent the GUI from breaking.
            */
        }
        _buildAndSetStyleTraitPaths();
    }

    /**
     *  A factory method for a {@link StyleTrait} targeting components
     *  with the given id/name (see {@link JComponent#setName(String)}).
     *  This is intended to be used in the {@link #configure()} method of the style sheet.
     *  Note that this method does not set the id/name of the component, it expects there to be a component with
     *  the given id/name already in the component hierarchy so that a corresponding {@link Styler} lambda can be applied to it.
     *  <br><br>
     *  This is intended to be used in the {@link #configure()} method of the style sheet. <br>
     *  Here an example of how to use this method in the {@link #configure()} method:
     *  <pre>{@code
     *      add(id("myButton"), it -> it.backgroundColor(Color.RED));
     *  }</pre>
     *
     * @param id The id/name of the component to target.
     * @return A {@link StyleTrait} targeting components with the given id/name.
     */
    protected StyleTrait<JComponent> id( String id ) { return new StyleTrait<>().id(id); }

    /**
     *  A factory method for a {@link StyleTrait} targeting components
     *  belonging to the given string group (see {@link swingtree.UIForAnySwing#group(String...)}.
     *  A group is conceptually similar to a CSS class, meaning that you can add a group to any component
     *  and then target all components belonging to that group with a single {@link StyleTrait}.
     *  Note that this method does not add the group to any component, it expects there to be a component with
     *  the given group already in the component hierarchy so that a corresponding {@link Styler} lambda can be applied to it.
     *  <br><br>
     *  This is intended to be used in the {@link #configure()} method of the style sheet. <br>
     *  Here an example of how to use this method in the {@link #configure()} method:
     *  <pre>{@code
     *      add(group("myGroup"), it -> it.backgroundColor(Color.RED));
     *  }</pre>
     *  <b>Although using {@link String}s is a convenient way of grouping components,
     *  it is not ideal with respect to compile time safety. Please use {@link #group(Enum)} and {@link swingtree.UIForAnySwing#group(Enum[])}
     *  instead...</b>
     *
     * @param group The group to target in the form of a string.
     * @return A {@link StyleTrait} targeting components belonging to the given group.
     */
    protected StyleTrait<JComponent> group( String group ) { return new StyleTrait<>().group(group); }

    /**
     *  A factory method for a {@link StyleTrait} targeting components
     *  belonging to the given enum group (see {@link swingtree.UIForAnySwing#group(Enum...)}.
     *  A group is conceptually similar to a CSS class, meaning that you can add a group to any component
     *  and then target all components belonging to that group with a single {@link StyleTrait}.
     *  Note that this method does not add the group to any component, it expects there to be a component with
     *  the given group already in the component hierarchy so that a corresponding {@link Styler} lambda can be applied to it.
     *  <br><br>
     *  This is intended to be used in the {@link #configure()} method of the style sheet. <br>
     *  Here an example of how to use this method in the {@link #configure()} method:
     *  <pre>{@code
     *      add(group(Group.ERROR), it -> it.backgroundColor(Color.RED));
     *  }</pre>
     *
     * @param group The group to target in the form of an enum.
     * @return A {@link StyleTrait} targeting components belonging to the given group.
     * @param <E> The type of the enum defining the group to target.
     */
    protected <E extends Enum<E>> StyleTrait<JComponent> group( E group ) { return new StyleTrait<>().group(group); }

    /**
     *  A factory method for a {@link StyleTrait} targeting components
     *  which are of a given type (see {@link JComponent#getClass()}.
     *  Note that this method does not set the type of any component, it expects there to be a component of
     *  the given type already in the component hierarchy so that a corresponding {@link Styler} lambda can be applied to it.
     *  <br><br>
     *  This is intended to be used in the {@link #configure()} method of the style sheet. <br>
     *  Here an example of how to use this method in the {@link #configure()} method:
     *  <pre>{@code
     *      add(type(JButton.class), it -> it.backgroundColor(Color.RED));
     *  }</pre>
     *
     * @param type The type of the component to target.
     * @return A {@link StyleTrait} targeting components of the given type.
     * @param <C> The type of the components to target for styling.
     */
    protected <C extends JComponent> StyleTrait<C> type( Class<C> type ) { return new StyleTrait<>().type(type); }

    /**
     *  Use this to register style rules in you {@link #configure()} implementation by providing a {@link StyleTrait}
     *  targeting the components you want to style (see {@link #id(String)}, {@link #group(String)}, {@link #group(Enum)}, {@link #type(Class)}),
     *  and a corresponding {@link Styler} lambda which will be applied to the components targeted by the {@link StyleTrait}.
     *  <br><br>
     *  Here an example of how to use this method in the {@link #configure()} method:
     *  <pre><code>
     *  {@literal @}Override
     *  protected void configure() {
     *      add(id("arial-button"), it -&gt; it.font(new Font("Arial", Font.BOLD, 12)));
     *      add(type(JButton).group("FooBar"), it -&gt; it.borderRadius(5));
     *      add(group(Group.ERROR), it -&gt; it.backgroundColor(Color.RED));
     *      // ...
     *  }
     *  </code></pre>
     *
     * @param rule The {@link StyleTrait} targeting the components you want to style.
     * @param traitStyler The {@link Styler} lambda which will be applied to the components targeted by the {@link StyleTrait}.
     * @param <C> The type of the components targeted by the {@link StyleTrait}.
     */
    protected <C extends JComponent> void add( StyleTrait<C> rule, Styler<C> traitStyler ) {
        if ( _traitGraphBuilt )
            throw new IllegalStateException(
                    "The trait graph has already been built. " +
                    "You cannot add more traits to a fully built style sheet."
                );

        // First let's make sure the trait does not already exist.
        if ( _traitStylers.containsKey(rule) )
            throw new IllegalArgumentException("The trait " + rule.group() + " already exists in this style sheet.");

        // Finally we fulfill the purpose of this method, we add the trait to the style sheet.
        _traitStylers.put( rule, traitStyler );
    }

    /**
     *  Override this method to configure the style sheet
     *  by adding {@link StyleTrait}s and corresponding {@link Styler} lambdas
     *  to the style sheet through the {@link #add(StyleTrait, Styler)} method. <br>
     *  <br>
     *  Example:
     *  <pre><code>
     *  {@literal @}Override
     *  protected void configure() {
     *      add(type(JComponent.class), it -&gt; it
     *        .backgroundColor(new Color(0.7f, 0.85f, 1f))
     *        .padding(4)
     *        .margin(5)
     *      );
     *      add(type(JButton.class), it -&gt; it
     *         .padding(12)
     *         .margin(16)
     *         .gradient("default", shade -&gt; shade
     *             .strategy(ShadingStrategy.TOP_LEFT_TO_BOTTOM_RIGHT)
     *             .colors(it.component().getBackground().brighter(), Color.CYAN)
     *         )
     *      );
     *      // ...
     *   }
     * </code></pre>
     */
    protected abstract void configure();

    /**
     *  Applies the style sheet to the given component.
     *  Note that the style sheet is already configured at this point,
     *  because the {@link #configure()} method is called in the constructor of the style sheet.
     *  <br><br>
     *  Example:
     *  <pre>{@code
     *      MyStyleSheet styleSheet = new MyStyleSheet();
     *      JComboBox<String> comboBox = new JComboBox<>();
     *      styleSheet.applyTo(comboBox);
     * }</pre>
     *
     * @param toBeStyled The component to apply the style sheet to.
     * @return The {@link Style} that was applied to the component.
     */
    public Style applyTo( JComponent toBeStyled ) { return applyTo( toBeStyled, _defaultStyle.apply(toBeStyled) ); }

    /**
     *  Applies the style sheet to the given component using a starting {@link Style}.
     *  Note that the style sheet is already configured at this point,
     *  because the {@link #configure()} method is called in the constructor of the style sheet.
     *  <br><br>
     *  Example:
     *  <pre>{@code
     *      MyStyleSheet styleSheet = new MyStyleSheet();
     *      JComboBox<String> comboBox = new JComboBox<>();
     *      styleSheet.applyTo(comboBox, Style.none());
     * }</pre>
     *
     * @param toBeStyled The component to apply the style sheet to.
     * @param startingStyle The {@link Style} to start with when applying the style sheet.
     * @return The {@link Style} that was applied to the component.
     */
    Style applyTo( JComponent toBeStyled, Style startingStyle ) {
        if ( !_traitGraphBuilt )
            _buildAndSetStyleTraitPaths();

        // Now we run the starting style through the trait graph.
        // We do this by finding valid trait paths from the root traits to the leaf traits.
        int deepestValidPath = -1;
        List<List<StyleTrait<?>>> validTraitPaths = new java.util.ArrayList<>();
        for ( StyleTrait<?>[] traitPath : _traitPaths ) {
            int lastValidTrait = -1;
            for ( int i = 0; i < traitPath.length; i++ ) {
                boolean valid = traitPath[i].isApplicableTo(toBeStyled);
                if (valid) lastValidTrait = i;
            }
            if ( lastValidTrait >= 0 ) {
                // We add the path up to the last valid trait to the list of valid traits.
                // This is done by slicing the trait path array from 0 to lastValidTrait + 1.
                validTraitPaths.add(Arrays.asList(Arrays.copyOfRange(traitPath, 0, lastValidTrait + 1)));
            }

            if ( lastValidTrait > deepestValidPath )
                deepestValidPath = lastValidTrait;
        }

        // Now we are going to create one common path from the valid trait paths by merging them!
        // So first we add all the traits from path step 0, then 1, then 2, etc.
        List<StyleTrait<?>> subToSuper = new java.util.ArrayList<>(); // The final merged path.
        List<String> inheritedTraits = new java.util.ArrayList<>();
        StyleTrait<?> lastAdded = null;
        for ( int i = 0; i <= deepestValidPath; i++ ) {
            if ( !inheritedTraits.isEmpty() ) {
                for ( String inheritedTrait : new ArrayList<>(inheritedTraits) ) {
                    for ( List<StyleTrait<?>> validTraitPath : validTraitPaths ) {
                        int index = validTraitPath.size() - i - 1;
                        if ( index >= 0 ) {
                            StyleTrait<?> current = validTraitPath.get(index);
                            if ( !subToSuper.contains(current) && current.group().equals(inheritedTrait) )
                                lastAdded = _merge(current, lastAdded, subToSuper, inheritedTraits);
                        }
                    }
                }
            }
            for ( List<StyleTrait<?>> validTraitPath : validTraitPaths ) {
                int index = validTraitPath.size() - i - 1;
                if ( index >= 0 ) {
                    StyleTrait<?> trait = validTraitPath.get(index);
                    if ( !subToSuper.contains(trait) )
                        lastAdded = _merge(trait, lastAdded, subToSuper, inheritedTraits);
                }
            }
        }

        // Now we apply the valid traits to the starting style.
        for ( int i = subToSuper.size() - 1; i >= 0; i-- ) {
            StyleTrait<?> trait = subToSuper.get(i);
            ComponentStyleDelegate delegate = new ComponentStyleDelegate<>(toBeStyled, startingStyle);
            startingStyle = _traitStylers.get(trait).style(delegate).style();
        }

        return startingStyle;
    }

    private StyleTrait<?> _merge(
        StyleTrait<?>       currentTrait,
        StyleTrait<?>       lastAdded,
        List<StyleTrait<?>> subToSuper,
        List<String>        inheritedTraits
    ) {
        boolean lastIsSuper = lastAdded != null && lastAdded.group().isEmpty() && !lastAdded.thisInherits(currentTrait);
        if ( lastIsSuper )
            subToSuper.add(subToSuper.size() - 1, currentTrait);
        else {
            subToSuper.add(currentTrait);
            lastAdded = currentTrait;
        }
        inheritedTraits.remove(currentTrait.group());
        inheritedTraits.addAll(Arrays.asList(currentTrait.toInherit()));
        return lastAdded;
    }

    /**
     *  Establishes a list of trait lists which represent
     *  all the possible paths from the root traits to the leaf traits.
     *  These paths are used to determine the order in which the styles
     *  of the traits are applied to a component.
     */
    private void _buildAndSetStyleTraitPaths() {
        _traitPaths = new GraphPathsBuilder().buildTraitGraphPathsFrom(_traitStylers);
        _traitGraphBuilt = true;
    }

    private static class GraphPathsBuilder
    {
        private final Map<StyleTrait<?>, List<StyleTrait<?>>> _traitGraph = new LinkedHashMap<>();


        private StyleTrait<?>[][] buildTraitGraphPathsFrom(
            Map<StyleTrait<?>, Styler<?>> _traitStylers
        ) {
            // Let's clear the trait graph. Just in case.
            _traitGraph.clear();

        /*
            First we need to initialize the trait graph.
            We compare each trait to every other trait to see if it inherits from it.
            If it does, we add the trait to the other trait's list of extensions (the value in the map).
        */
            for ( StyleTrait<?> trait1 : _traitStylers.keySet() ) {
                List<StyleTrait<?>> traits = _traitGraph.computeIfAbsent(trait1, k -> new java.util.ArrayList<>());
                for ( StyleTrait<?> trait2 : _traitStylers.keySet() )
                    if ( trait2 != trait1 && trait2.thisInherits(trait1) )
                        traits.add(trait2);
            }

        /*
            Now we have a graph of traits that inherit from other traits.
            We can use this graph to determine the order in which we apply the traits to a style.
            But first we need to make sure there are no cycles in the graph.

            We do this by performing a depth-first search on the graph.
        */
            for ( StyleTrait<?> trait : _traitGraph.keySet() ) {
                // We create a stack onto which we will push the traits we visit and then pop them off.
                List<StyleTrait<?>> visited = new java.util.ArrayList<>();
                // We pop the trait off the stack when we return from the recursive call.
                _depthFirstSearch(trait, visited);
            }

            List<List<StyleTrait<?>>> result = _findRootAndLeaveTraits();
            StyleTrait<?>[][] resultArray = new StyleTrait<?>[result.size()][];
            for ( int i = 0; i < result.size(); i++ )
                resultArray[i] = result.get(i).toArray(new StyleTrait<?>[0]);

            return resultArray;
        }

        private void _depthFirstSearch( StyleTrait<?> current, List<StyleTrait<?>> visited ) {
            // If the current trait is already in the visited list, then we have a cycle.
            if ( visited.contains(current) )
                throw new IllegalStateException("The style sheet contains a cycle.");

            // We add the current trait to the visited list.
            visited.add(current);

            // We recursively call the dfs method on each of the current trait's extensions.
            for ( StyleTrait<?> extension : _traitGraph.get(current) )
                _depthFirstSearch(extension, visited);

            // We remove the current trait from the visited list.
            visited.remove(current);
        }

        private List<List<StyleTrait<?>>> _findRootAndLeaveTraits() {
        /*
            We find the root traits by finding the traits, which have extensions,
            but are not referenced by any other trait as an extension.
         */
            List<StyleTrait<?>> rootTraits = new java.util.ArrayList<>();

            for ( StyleTrait<?> trait1 : _traitGraph.keySet() ) {
                boolean isLeaf = true;
                for ( StyleTrait<?> trait2 : _traitGraph.keySet() )
                    if ( _traitGraph.get(trait2).contains(trait1) ) {
                        isLeaf = false;
                        break;
                    }
                if ( isLeaf )
                    rootTraits.add(trait1);
            }
        /*
            Finally we can calculate all the possible paths from the root traits to the leaf traits.
        */
            return _findPathsFor(rootTraits);
        }

        private List<List<StyleTrait<?>>> _findPathsFor(List<StyleTrait<?>> traits) {
            List<List<StyleTrait<?>>> paths = new java.util.ArrayList<>();
            for ( StyleTrait<?> root : traits ) {
                List<StyleTrait<?>> stack = new java.util.ArrayList<>();
                _traverse(root, paths, stack);
            }
            return paths;
        }

        private void _traverse(
                StyleTrait<?> current,
                List<List<StyleTrait<?>>> paths,
                List<StyleTrait<?>> stack
        ) {
            stack.add(current);
            if ( _traitGraph.get(current).isEmpty() ) {
                List<List<StyleTrait<?>>> newPath = Collections.singletonList(new ArrayList<>(stack));
                // We remove the last trait from the stack.
                stack.remove(stack.size() - 1);
                paths.addAll(newPath);
                return;
            }

            for ( StyleTrait<?> extension : _traitGraph.get(current) )
                if ( extension != current )
                    _traverse(extension, paths, stack);

            // We remove the last trait from the stack.
            stack.remove(stack.size() - 1);
        }
    }

}

package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;

import java.util.Collections;
import java.util.Objects;

/**
 *  Reads the top level nodes out of the single value a tree is bound to, and writes them
 *  back into it. A tree bound through {@link UI#tree(sprouts.Var, swingtree.api.Configurator)}
 *  has exactly one top level node, its root. A forest bound through
 *  {@link UI#trees(sprouts.Var, swingtree.api.Configurator)} has as many as its tuple holds,
 *  and no root at all.
 *  <p>
 *  Everything below the top level is the same in both forms, so this is the only place the
 *  two differ, which is what lets one {@link PropertyTreeModel} and one {@link TreePathLens}
 *  serve them both.
 *  <p>
 *  {@link PropertyTreeModel} deliberately does <b>not</b> route its update walk through here.
 *  That walk stops at every value reference identical to the one it had before, and a single
 *  root wrapped in a tuple allocated on the spot would never be.
 *
 * @param <N> The common node type of the tree.
 * @param <R> The type of the value the tree is bound to: one node, or a tuple of them.
 */
interface TreeRoots<N, R>
{
    /** The bound value is the root node itself, so there is exactly one top level node. */
    static <N> TreeRoots<N, N> single( Class<N> nodeType ) {
        Objects.requireNonNull(nodeType);
        return new TreeRoots<N, N>() {
            @Override public Tuple<N> of( @Nullable N boundValue ) {
                return ( boundValue == null
                            ? Tuple.of(nodeType)
                            : Tuple.of(nodeType, Collections.singletonList(boundValue)) );
            }
            @Override public N with( @Nullable N boundValue, Tuple<N> roots ) {
                return roots.first();
            }
            @Override public boolean isForest() {
                return false;
            }
            @Override public String toString() {
                return "single(" + nodeType.getSimpleName() + ")";
            }
        };
    }

    /** The bound value is the tuple of top level nodes, which may hold any number of them. */
    static <N> TreeRoots<N, Tuple<N>> forest( Class<N> nodeType ) {
        Objects.requireNonNull(nodeType);
        return new TreeRoots<N, Tuple<N>>() {
            @Override public Tuple<N> of( @Nullable Tuple<N> boundValue ) {
                return ( boundValue == null ? Tuple.of(nodeType) : boundValue );
            }
            @Override public Tuple<N> with( @Nullable Tuple<N> boundValue, Tuple<N> roots ) {
                return roots;
            }
            @Override public boolean isForest() {
                return true;
            }
            @Override public String toString() {
                return "forest(" + nodeType.getSimpleName() + ")";
            }
        };
    }

    /**
     *  The top level nodes held by the given bound value, which is empty where the property
     *  holding it is.
     *
     * @param boundValue The value the tree is bound to, or {@code null} where it holds nothing.
     * @return Every node at the top level of the tree.
     */
    Tuple<N> of( @Nullable R boundValue );

    /**
     *  The bound value that holds the given top level nodes instead of the ones it had.
     *
     * @param boundValue The value the tree is bound to, or {@code null} where it holds nothing.
     * @param roots The top level nodes to put in its place, exactly one of them for a
     *              tree which is not a forest.
     * @return A new bound value, the old one being immutable.
     */
    R with( @Nullable R boundValue, Tuple<N> roots );

    /**
     *  Tells whether the top level of this tree is a tuple the user bound, rather than one
     *  root node. A forest has no root of its own, so nothing names it and no path leads
     *  to it.
     *
     * @return True for a tree bound to a tuple of top level nodes.
     */
    boolean isForest();
}

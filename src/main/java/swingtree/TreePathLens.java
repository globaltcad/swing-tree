package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Lens;
import sprouts.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  A {@link Lens} focusing on a single node inside a deeply nested, immutable tree,
 *  identified by the path of ids leading down to it. Reading walks down the path by id;
 *  writing walks down and rebuilds the chain bottom up, so a change nine levels deep
 *  produces one new bound value with everything off the path shared rather than copied.
 *  <p>
 *  The descent begins at the top level nodes, which {@link TreeRoots} reads out of the one
 *  value the tree is bound to: one root node for a tree, a whole tuple of them for a forest.
 *  The first id of the path therefore names a top level node in both forms.
 *  <p>
 *  Two properties of this lens matter a great deal:
 *  <ul>
 *      <li><b>It never touches the UI, nor anything the UI thread owns.</b> It needs only
 *      the bound <i>value</i> and the {@link TreeConf}, whose rule lookup is concurrent, so
 *      it may be evaluated on any thread. That is essential, because it runs whenever the
 *      bound property changes, which under a decoupled UI is the application thread.</li>
 *      <li><b>It fails softly.</b> A node which has vanished from the tree yields the last
 *      value this lens did resolve, and a write into it is dropped. A lens whose node is
 *      gone may still be bound to a view on its way out, and disturbing that view is worse
 *      than doing nothing. The tuple item lens follows the same policy.</li>
 *  </ul>
 *
 * @param <I> The identity type of the nodes, which the path is made of.
 * @param <N> The common node type of the tree.
 * @param <R> The type of the value the tree is bound to: one node, or a tuple of them.
 */
final class TreePathLens<I, N, R> implements Lens<R, N>
{
    private static final Logger log = LoggerFactory.getLogger(TreePathLens.class);

    private final TreeConf<I, N>               _conf;
    private final TreeRoots<N, R>              _roots;
    private final Object[]                     _idPath;
    private final AtomicReference<@Nullable N> _lastResolved;

    TreePathLens( TreeConf<I, N> conf, TreeRoots<N, R> roots, Object[] idPath, @Nullable N initial ) {
        _conf         = Objects.requireNonNull(conf);
        _roots        = Objects.requireNonNull(roots);
        _idPath       = Objects.requireNonNull(idPath);
        _lastResolved = new AtomicReference<>(initial);
    }

    @Override
    public N getter( R boundValue ) {
        try {
            @Nullable N found = _resolve(boundValue);
            if ( found != null )
                _lastResolved.set(found);
            else
                found = _lastResolved.get();
            return NullUtil.fakeNonNull(found);
        } catch (Exception e) {
            log.debug(SwingTree.get().logMarker(), "Failed to resolve a tree node lens.", e);
            return NullUtil.fakeNonNull(_lastResolved.get());
        }
    }

    @Override
    public R wither( R boundValue, N newNode ) {
        if ( _idPath.length == 0 )
            return boundValue; // Nothing is focused, so there is nothing to write into.
        try {
            Tuple<N> roots = _roots.of(boundValue);
            int rootIndex = _indexOfIdIn(roots, _idPath[0]);
            if ( rootIndex < 0 )
                return boundValue;
            List<Step> steps = _walk(roots.get(rootIndex));
            if ( steps == null )
                return boundValue; // The node is gone, so the write no longer applies.

            Object rebuilt = newNode;
            for ( int i = steps.size() - 1; i >= 0; i-- ) {
                Step step = steps.get(i);
                if ( !step.rule.isStructurallyWritable() ) {
                    _warnAboutReadOnlyBranch(step);
                    return boundValue;
                }
                Tuple<Object> updatedChildren = step.children.setAt(step.index, rebuilt);
                rebuilt = step.rule.withChildren(step.parent, updatedChildren);
            }
            _lastResolved.set(newNode);
            return _roots.with(boundValue, roots.setAt(rootIndex, _conf.nodeType().cast(rebuilt)));
        } catch (Exception e) {
            log.debug(SwingTree.get().logMarker(), "Failed to write through a tree node lens.", e);
            return boundValue;
        }
    }

    private @Nullable N _resolve( @Nullable R boundValue ) {
        if ( _idPath.length == 0 )
            return null;
        Tuple<N> roots = _roots.of(boundValue);
        int rootIndex = _indexOfIdIn(roots, _idPath[0]);
        if ( rootIndex < 0 )
            return null; // Not a top level node of this value, so this path means nothing in it.
        Object current = roots.get(rootIndex);
        for ( int level = 1; level < _idPath.length; level++ ) {
            TreeNodeConf<N, ?> rule = _conf.ruleFor(current);
            if ( rule == null || !rule.hasChildrenRule() )
                return null;
            Tuple<Object> children = rule.childrenOf(current);
            int index = _indexOfIdIn(children, _idPath[level]);
            if ( index < 0 )
                return null;
            current = children.get(index);
        }
        return _conf.nodeType().cast(current);
    }

    private @Nullable List<Step> _walk( Object topLevelNode ) {
        List<Step> steps = new ArrayList<>(_idPath.length - 1);
        Object current = topLevelNode;
        for ( int level = 1; level < _idPath.length; level++ ) {
            TreeNodeConf<N, ?> rule = _conf.ruleFor(current);
            if ( rule == null || !rule.hasChildrenRule() )
                return null;
            Tuple<Object> children = rule.childrenOf(current);
            int index = _indexOfIdIn(children, _idPath[level]);
            if ( index < 0 )
                return null;
            steps.add(new Step(rule, current, children, index));
            current = children.get(index);
        }
        return steps;
    }

    private int _indexOfIdIn( Tuple<?> children, Object id ) {
        for ( int i = 0; i < children.size(); i++ )
            if ( Objects.equals(_conf.idOf(children.get(i)), id) )
                return i;
        return -1;
    }

    /**
     *  The dedup lives on the configuration rather than on this lens, because a lens is
     *  built afresh for every single edit, so a per lens flag would repeat the same
     *  complaint on every keystroke that ends one.
     */
    private void _warnAboutReadOnlyBranch( Step step ) {
        Class<?> branchType = step.parent.getClass();
        if ( !_conf.shouldWarnAbout(branchType) )
            return;
        log.warn(SwingTree.get().logMarker(),
            "Dropping a write to a tree node, because the branch of type '{}' above it was declared " +
            "with a read only 'children(getter)' rule. Declare it as 'children(getter, wither)' to " +
            "let edits below it travel back up into the bound property.",
            branchType.getSimpleName()
        );
    }

    /** One level of the descent from a top level node down to the focused node. */
    private static final class Step {
        final TreeNodeConf<?, ?> rule;
        final Object             parent;
        final Tuple<Object>      children;
        final int                index;
        Step( TreeNodeConf<?, ?> rule, Object parent, Tuple<Object> children, int index ) {
            this.rule     = rule;
            this.parent   = parent;
            this.children = children;
            this.index    = index;
        }
    }
}

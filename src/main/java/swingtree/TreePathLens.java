package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Lens;
import sprouts.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  A {@link Lens} focusing on a single node inside a deeply nested, immutable tree,
 *  identified by the path of ids leading down to it.
 *  <p>
 *  This is the tree shaped generalisation of the {@code TupleLens} behind
 *  {@code addAll(Var<Tuple<M>>, ..)}: reading walks down the path applying each level's
 *  {@code children(..)} rule and picking the child whose id matches, and writing walks the
 *  same way down and then rebuilds the chain bottom up, so that a change to a node nine
 *  levels deep produces exactly one new root value with everything off the path shared
 *  rather than copied.
 *  <p>
 *  Two properties of this lens matter a great deal:
 *  <ul>
 *      <li><b>It never touches the UI.</b> Resolving a focus needs nothing but the root
 *      <i>value</i>, so this lens may be evaluated on any thread. That is essential, because
 *      it runs whenever the root property changes, which under a decoupled UI happens on the
 *      application thread.</li>
 *      <li><b>It fails softly.</b> A node that has vanished from the tree (deleted, moved
 *      into a branch whose rule has no wither, ...) yields the last value this lens did
 *      resolve, and a write into it is dropped. A lens whose node is gone may still be bound
 *      to a view which is on its way out, and disturbing that view is worse than doing
 *      nothing. The very same policy is what the tuple item lens follows.</li>
 *  </ul>
 *
 * @param <N> The common node type of the tree.
 */
final class TreePathLens<I, N> implements Lens<N, N>
{
    private static final Logger log = LoggerFactory.getLogger(TreePathLens.class);

    private final TreeConf<I, N>             _conf;
    private final Map<Class<?>, Object>      _ruleCache;
    private final Object[]                   _idPath;
    private final AtomicReference<@Nullable N> _lastResolved;
    private boolean                          _warnedAboutReadOnlyWrite = false;

    TreePathLens( TreeConf<I, N> conf, Map<Class<?>, Object> ruleCache, Object[] idPath, @Nullable N initial ) {
        _conf         = Objects.requireNonNull(conf);
        _ruleCache    = Objects.requireNonNull(ruleCache);
        _idPath       = Objects.requireNonNull(idPath);
        _lastResolved = new AtomicReference<>(initial);
    }

    @Override
    public N getter( N rootValue ) {
        try {
            @Nullable N found = _resolve(rootValue);
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
    public N wither( N rootValue, N newNode ) {
        try {
            List<Step> steps = _walk(rootValue);
            if ( steps == null )
                return rootValue; // The node is gone, so the write no longer applies.

            Object rebuilt = newNode;
            for ( int i = steps.size() - 1; i >= 0; i-- ) {
                Step step = steps.get(i);
                if ( !step.rule.isStructurallyWritable() ) {
                    _warnAboutReadOnlyBranch(step);
                    return rootValue;
                }
                Tuple<Object> updatedChildren = step.children.setAt(step.index, rebuilt);
                rebuilt = step.rule.withChildren(step.parent, updatedChildren);
            }
            _lastResolved.set(newNode);
            return _conf.nodeType().cast(rebuilt);
        } catch (Exception e) {
            log.debug(SwingTree.get().logMarker(), "Failed to write through a tree node lens.", e);
            return rootValue;
        }
    }

    /**
     *  Reads the node this lens focuses on out of the given root value,
     *  or {@code null} if the path no longer leads anywhere.
     */
    private @Nullable N _resolve( @Nullable N rootValue ) {
        if ( rootValue == null )
            return null;
        if ( !Objects.equals(_conf.idOf(rootValue), _idPath[0]) )
            return null; // A different root entirely, so this path means nothing in it.
        Object current = rootValue;
        for ( int level = 1; level < _idPath.length; level++ ) {
            TreeNodeConf<N, ?> rule = _conf.ruleFor(current, _ruleCache);
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

    /**
     *  Collects one {@link Step} per level between the root and the focused node,
     *  which is everything the write path needs to rebuild the chain bottom up.
     */
    private @Nullable List<Step> _walk( @Nullable N rootValue ) {
        if ( rootValue == null )
            return null;
        if ( !Objects.equals(_conf.idOf(rootValue), _idPath[0]) )
            return null;
        List<Step> steps = new ArrayList<>(_idPath.length - 1);
        Object current = rootValue;
        for ( int level = 1; level < _idPath.length; level++ ) {
            TreeNodeConf<N, ?> rule = _conf.ruleFor(current, _ruleCache);
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

    private int _indexOfIdIn( Tuple<Object> children, Object id ) {
        for ( int i = 0; i < children.size(); i++ )
            if ( Objects.equals(_conf.idOf(children.get(i)), id) )
                return i;
        return -1;
    }

    private void _warnAboutReadOnlyBranch( Step step ) {
        if ( _warnedAboutReadOnlyBranch() )
            return;
        log.warn(SwingTree.get().logMarker(),
            "Dropping a write to a tree node, because the branch of type '{}' above it was declared " +
            "with a read only 'children(getter)' rule. Declare it as 'children(getter, wither)' to " +
            "let edits below it travel back up into the bound property.",
            step.parent.getClass().getSimpleName()
        );
    }

    private boolean _warnedAboutReadOnlyBranch() {
        boolean warned = _warnedAboutReadOnlyWrite;
        _warnedAboutReadOnlyWrite = true;
        return warned;
    }

    /** One level of the descent from the root down to the focused node. */
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

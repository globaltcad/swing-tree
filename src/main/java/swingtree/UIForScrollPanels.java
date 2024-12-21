package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.*;
import sprouts.impl.TupleDiff;
import sprouts.impl.TupleDiffOwner;
import swingtree.api.mvvm.EntryViewModel;
import swingtree.api.mvvm.ViewSupplier;
import swingtree.components.JScrollPanels;
import swingtree.layout.AddConstraint;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 *  A builder node for {@link JScrollPanels}, a custom SwingTree component,
 *  which is similar to a {@link JList} but with the ability to interact with
 *  the individual components in the list.
 *
 * @param <P> The type of the component which this builder node wraps.
 * @author Daniel Nepp
 */
public class UIForScrollPanels<P extends JScrollPanels> extends UIForAnyScrollPane<UIForScrollPanels<P>, P>
{
    private static final Logger log = LoggerFactory.getLogger(UIForScrollPanels.class);
    private final BuilderState<P> _state;

    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the underlying component is build.
     */
    protected UIForScrollPanels( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }

    @Override
    protected UIForScrollPanels<P> _newBuilderWithState( BuilderState<P> newState ) {
        return new UIForScrollPanels<>(newState);
    }

    @Override
    protected void _addComponentTo(P thisComponent, JComponent addedComponent, @Nullable AddConstraint constraints) {
        Objects.requireNonNull(addedComponent);

        EntryViewModel entry = _entryModel();
        if ( constraints == null )
            thisComponent.addEntry( entry, m -> UI.of(addedComponent) );
        else
            thisComponent.addEntry( constraints, entry, m -> UI.of(addedComponent) );
    }

    private static EntryViewModel _entryModel() {
        Var<Boolean> selected = Var.of(false);
        Var<Integer> position = Var.of(0);
        return new EntryViewModel() {
            @Override public Var<Boolean> isSelected() { return selected; }
            @Override public Var<Integer> position() { return position; }
        };
    }

    @Override
    protected <M> void _addViewableProps(
            Vals<M> models, @Nullable AddConstraint attr, ViewSupplier<M> viewSupplier, P thisComponent
    ) {
        BiFunction<Integer, Vals<M>, @Nullable M> modelFetcher = (i, vals) -> {
            M v = vals.at(i).get();
            if ( v instanceof EntryViewModel ) ((EntryViewModel) v).position().set(i);
            return v;
        };
        BiFunction<Integer, Vals<M>, M> entryFetcher = (i, vals) -> {
            M v = modelFetcher.apply(i, vals);
            return ( v != null ? (M) v : (M)_entryModel() );
        };

        Consumer<Vals<M>> addAll = vals -> {
            boolean allAreEntries = vals.stream().allMatch( v -> v instanceof EntryViewModel );
            if ( allAreEntries ) {
                List<EntryViewModel> entries = (List) vals.toList();
                thisComponent.addAllEntries(attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
            }
            else
                for ( int i = 0; i< vals.size(); i++ ) {
                    int finalI = i;
                    thisComponent.addEntry(
                            _entryModel(),
                            m -> viewSupplier.createViewFor(entryFetcher.apply(finalI,vals))
                        );
                }
        };

        _onShow( models, thisComponent, (c, delegate) -> {
            Vals<M> vals = delegate.currentValues();
            int delegateIndex = delegate.index();
            Change changeType = delegate.changeType();
            // we simply redo all the components.
            switch ( changeType ) {
                case SET:
                case ADD:
                case REMOVE:
                    if ( delegateIndex >= 0 ) {
                        if ( changeType == Change.ADD ) {
                            M m = entryFetcher.apply(delegateIndex, vals);
                            if ( m instanceof EntryViewModel )
                                c.addEntryAt(delegateIndex, null, (EntryViewModel)m, (ViewSupplier<EntryViewModel>) viewSupplier);
                            else
                                c.addEntryAt(delegateIndex, null, _entryModel(), em -> viewSupplier.createViewFor(m));
                        } else if ( changeType == Change.REMOVE )
                            c.removeEntryAt( delegateIndex );
                        else if ( changeType == Change.SET ) {
                            M m = entryFetcher.apply(delegateIndex, vals);
                            if ( m instanceof EntryViewModel )
                                c.setEntryAt(delegateIndex, null, (EntryViewModel)m, (ViewSupplier<EntryViewModel>) viewSupplier);
                            else
                                c.setEntryAt(delegateIndex, null, _entryModel(), em -> viewSupplier.createViewFor(m));
                        }
                        // Now we need to update the positions of all the entries
                        for ( int i = delegateIndex; i < vals.size(); i++ ) {
                            M m = entryFetcher.apply(i, vals);
                            if ( m instanceof EntryViewModel )
                                ((EntryViewModel)m).position().set(i);
                        }
                    } else {
                        c.removeAllEntries();
                        addAll.accept(vals);
                    }
                break;
                case CLEAR: c.removeAllEntries(); break;
                case NONE: break;
                default: 
                    log.error("Unknown type: {}", delegate.changeType(), new Throwable());
                    c.removeAllEntries();
                    addAll.accept(vals);
            }
        });
        addAll.accept(models);
    }

    private static <M> M _modelFetcher(int i, Tuple<M> tuple) {
        M v = tuple.get(i);
        if ( v instanceof EntryViewModel ) ((EntryViewModel) v).position().set(i);
        return v;
    }

    private static <M> M _entryFetcher(int i, Tuple<M> tuple) {
        M v = _modelFetcher(i, tuple);
        return ( v != null ? (M) v : (M)_entryModel() );
    }

    private <M> void _addAllEntriesAt(@Nullable AddConstraint attr, JScrollPanels thisComponent, int index, Tuple<M> tuple, ViewSupplier<M> viewSupplier) {
        boolean allAreEntries = tuple.stream().allMatch( v -> v instanceof EntryViewModel );
        if ( allAreEntries ) {
            List<EntryViewModel> entries = (List) tuple.toList();
            thisComponent.addAllEntriesAt(index, attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
        }
        else
            for ( int i = 0; i< tuple.size(); i++ ) {
                int finalI = i + index;
                thisComponent.addEntryAt(
                   finalI, attr,
                   _entryModel(),
                   m -> viewSupplier.createViewFor(_entryFetcher(finalI,tuple))
                );
            }
    }

    private <M> void _setAllEntriesAt(@Nullable AddConstraint attr, JScrollPanels thisComponent, int index, Tuple<M> tuple, ViewSupplier<M> viewSupplier) {
        boolean allAreEntries = tuple.stream().allMatch( v -> v instanceof EntryViewModel );
        if ( allAreEntries ) {
            List<EntryViewModel> entries = (List) tuple.toList();
            thisComponent.setAllEntriesAt(index, attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
        }
        else
            for ( int i = 0; i< tuple.size(); i++ ) {
                int finalI = i + index;
                thisComponent.setEntryAt(
                   finalI, attr,
                   _entryModel(),
                   m -> viewSupplier.createViewFor(_entryFetcher(finalI,tuple))
                );
            }
    }
    
    @Override
    protected <M> void _addViewableProps(
            Val<Tuple<M>> models, 
            @Nullable AddConstraint attr, 
            ViewSupplier<M> viewSupplier, 
            P thisComponent 
    ) {
        AtomicReference<@Nullable TupleDiff> lastDiffRef = new AtomicReference<>(null);
        if (models.get() instanceof TupleDiffOwner)
            lastDiffRef.set(((TupleDiffOwner)models.get()).differenceFromPrevious().orElse(null));
        _onShow( models, thisComponent, (c, tupleOfModels) -> {
            TupleDiff diff = null;
            TupleDiff lastDiff = lastDiffRef.get();
            if (tupleOfModels instanceof TupleDiffOwner)
                diff = ((TupleDiffOwner)tupleOfModels).differenceFromPrevious().orElse(null);
            lastDiffRef.set(diff);

            if ( diff == null || ( lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) ) {
                c.removeAllEntries();
                _addAllEntriesAt(attr, c, 0, tupleOfModels, viewSupplier);
            } else {
                int index = diff.index().orElse(-1);
                int count = diff.size();
                if ( index < 0 ) {
                    // We do a simple re-build
                    c.removeAllEntries();
                    _addAllEntriesAt(attr, c, 0, tupleOfModels, viewSupplier);
                } else {
                    switch (diff.change()) {
                        case SET:
                            Tuple<M> slice = tupleOfModels.slice(index, index+count);
                            _setAllEntriesAt(attr, c, index, slice, viewSupplier);
                            break;
                        case ADD:
                            _addAllEntriesAt(attr, c, index, tupleOfModels.slice(index, index+count), viewSupplier);
                            break;
                        case REMOVE:
                            c.removeEntriesAt(index, count);
                            break;
                        case RETAIN: // Only keep the elements in the range.
                            // Remove trailing components:
                            c.removeEntriesAt(index + count, c.getNumberOfEntries() - (index + count));
                            // Remove leading components:
                            c.removeEntriesAt(0, index);
                            break;
                        case CLEAR:
                            c.removeAllEntries();
                            break;
                        case NONE:
                            break;
                        default:
                            log.error("Unknown change type: {}", diff.change(), new Throwable());
                            // We do a simple rebuild:
                            c.removeAllEntries();
                            _addAllEntriesAt(attr, c, 0, tupleOfModels, viewSupplier);
                    }
                }
            }
        });
        models.ifPresent( (tupleOfModels) -> {
            thisComponent.removeAllEntries();
            _addAllEntriesAt(attr, thisComponent, 0, tupleOfModels, viewSupplier);
        });
    }
}

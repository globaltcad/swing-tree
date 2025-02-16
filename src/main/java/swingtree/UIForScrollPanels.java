package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.*;
import sprouts.impl.SequenceDiff;
import sprouts.impl.SequenceDiffOwner;
import swingtree.api.mvvm.EntryViewModel;
import swingtree.api.mvvm.ViewSupplier;
import swingtree.components.JScrollPanels;
import swingtree.layout.AddConstraint;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    private static <M> M _modelFetcher(int i, Vals<M> vals) {
        M v = vals.at(i).get();
        if ( v instanceof EntryViewModel ) ((EntryViewModel) v).position().set(i);
        return v;
    }

    private static <M> M _entryFetcher(int i, Vals<M> vals) {
        M v = _modelFetcher(i, vals);
        return ( v != null ? (M) v : (M)_entryModel() );
    }

    @Override
    protected <M> void _addViewableProps(
            Vals<M> models, @Nullable AddConstraint attr, ModelToViewConverter<M> viewSupplier, P thisComponent
    ) {
        BiConsumer<Integer, Vals<M>> addAllAt = (index, vals) -> {
            boolean allAreEntries = vals.stream().allMatch( v -> v instanceof EntryViewModel );
            if ( allAreEntries ) {
                List<EntryViewModel> entries = (List) vals.toList();
                thisComponent.addAllEntriesAt(index, attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
            }
            else
                for ( int i = 0; i< vals.size(); i++ ) {
                    int finalI = i;
                    thisComponent.addEntryAt(
                            finalI + index, attr,
                            _entryModel(),
                            m -> viewSupplier.createViewFor(_entryFetcher(finalI,vals))
                        );
                }
        };

        _onShow( models, thisComponent, (c, delegate) -> {
            viewSupplier.rememberCurrentViewsForReuse();
            Tuple<M> tupleOfModels = Tuple.of(delegate.currentValues().type(), delegate.currentValues());
            int delegateIndex = delegate.index().orElse(-1);
            SequenceChange changeType = delegate.change();
            int removeCount = delegate.oldValues().size();
            int addCount = delegate.newValues().size();
            int maxChange = Math.max(removeCount, addCount);
            _update(c, attr, changeType, delegateIndex, maxChange, tupleOfModels, viewSupplier);
            viewSupplier.clearCurrentViews();
        });
        addAllAt.accept(0,models);
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

    private <M> void _addAllEntriesAt(
            @Nullable AddConstraint attr,
            JScrollPanels thisComponent,
            int index,
            Iterable<M> iterable,
            ViewSupplier<M> viewSupplier
    ) {
        boolean allAreEntries = StreamSupport.stream(iterable.spliterator(), false).allMatch( v -> v instanceof EntryViewModel );
        if ( allAreEntries ) {
            List<EntryViewModel> entries = StreamSupport.stream(iterable.spliterator(), false).map(v -> (EntryViewModel)v).collect(Collectors.toList());
            thisComponent.addAllEntriesAt(index, attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
        }
        else {
            Tuple<M> tuple = (iterable instanceof Tuple) ? (Tuple<M>) iterable : (Tuple<M>) Tuple.of(Object.class, (Iterable<Object>) iterable);
            for ( int i = 0; i< tuple.size(); i++ ) {
                int finalI = i;
                thisComponent.addEntryAt(
                    i + index, attr,
                    _entryModel(),
                    m -> viewSupplier.createViewFor(_entryFetcher(finalI,tuple))
                );
            }
        }
    }

    private <M> void _addAllEntriesAt(
            @Nullable AddConstraint attr,
            JScrollPanels thisComponent,
            int index,
            Tuple<M> models,
            Function<Integer, ViewHandle<M>> lensSupplier,
            ViewSupplier<ViewHandle<M>> viewSupplier
    ) {
        for ( int i = 0; i< models.size(); i++ ) {
            ViewHandle<M> viewable = lensSupplier.apply(index+i);
            thisComponent.addEntryAt(
                    i + index, attr,
                    _entryModel(),
                    m -> viewSupplier.createViewFor(viewable)
            );
        }
    }

    private <M> void _setAllEntriesAt(
        @Nullable AddConstraint attr,
        JScrollPanels thisComponent,
        int index,
        Iterable<M> iterable,
        ViewSupplier<M> viewSupplier
    ) {
        boolean allAreEntries = StreamSupport.stream(iterable.spliterator(), false).allMatch( v -> v instanceof EntryViewModel );
        if ( allAreEntries ) {
            List<EntryViewModel> entries = StreamSupport.stream(iterable.spliterator(), false).map(v -> (EntryViewModel)v).collect(Collectors.toList());
            thisComponent.setAllEntriesAt(index, attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
        }
        else {
            Tuple<M> tuple = (iterable instanceof Tuple) ? (Tuple<M>) iterable : (Tuple<M>) Tuple.of(Object.class, (Iterable<Object>) iterable);
            for (int i = 0; i < tuple.size(); i++) {
                int finalI = i;
                thisComponent.setEntryAt(
                    i + index, attr,
                    _entryModel(),
                    m -> viewSupplier.createViewFor(_entryFetcher(finalI, tuple))
                );
            }
        }
    }

    private <M> void _setAllEntriesAt(
            @Nullable AddConstraint attr,
            JScrollPanels thisComponent,
            int index,
            int size,
            Function<Integer, ViewHandle<M>> lensSupplier,
            ViewSupplier<ViewHandle<M>> viewSupplier
    ) {
        for (int i = 0; i < size; i++) {
            ViewHandle<M> viewable = lensSupplier.apply(index+i);
            thisComponent.setEntryAt(
                    i + index, attr,
                    _entryModel(),
                    m -> viewSupplier.createViewFor(viewable)
            );
        }
    }

    @Override
    protected <M> void _addViewableProps(
            Val<Tuple<M>> models, 
            @Nullable AddConstraint attr, 
            ModelToViewConverter<M> viewSupplier,
            P thisComponent 
    ) {
        AtomicReference<@Nullable SequenceDiff> lastDiffRef = new AtomicReference<>(null);
        if (models.get() instanceof SequenceDiffOwner)
            lastDiffRef.set(((SequenceDiffOwner)models.get()).differenceFromPrevious().orElse(null));
        _onShow( models, thisComponent, (c, tupleOfModels) -> {
            viewSupplier.rememberCurrentViewsForReuse();
            SequenceDiff diff = null;
            SequenceDiff lastDiff = lastDiffRef.get();
            if (tupleOfModels instanceof SequenceDiffOwner)
                diff = ((SequenceDiffOwner)tupleOfModels).differenceFromPrevious().orElse(null);
            lastDiffRef.set(diff);

            if ( diff == null || ( lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) ) {
                c.removeAllEntries();
                _addAllEntriesAt(attr, c, 0, tupleOfModels, viewSupplier);
            } else {
                int index = diff.index().orElse(-1);
                int count = diff.size();
                _update(c, attr, diff.change(), index, count, tupleOfModels, viewSupplier);
            }
            viewSupplier.clearCurrentViews();
        });
        models.ifPresent( (tupleOfModels) -> {
            thisComponent.removeAllEntries();
            _addAllEntriesAt(attr, thisComponent, 0, tupleOfModels, viewSupplier);
        });
    }

    @Override
    protected <M> void _addViewableProps(
            Var<Tuple<M>> propertyOfModels,
            @Nullable AddConstraint attr,
            ModelToViewConverter<ViewHandle<M>> viewSupplier,
            P scrollPanels
    ) {
        Function<Integer, ViewHandle<M>> lensSupplier = index -> ViewHandle.of(propertyOfModels, index, scrollPanels);
        AtomicReference<@Nullable SequenceDiff> lastDiffRef = new AtomicReference<>(null);
        if (propertyOfModels.get() instanceof SequenceDiffOwner)
            lastDiffRef.set(((SequenceDiffOwner)propertyOfModels.get()).differenceFromPrevious().orElse(null));
        _onShowDelegated( propertyOfModels, scrollPanels, (thisComponent, delegate) -> {
            Tuple<M> oldModels = delegate.oldValue().orElseThrowUnchecked();
            Tuple<M> newModels = delegate.currentValue().orElseThrowUnchecked();
            viewSupplier.rememberCurrentViewsForReuse();
            SequenceDiff diff = null;
            SequenceDiff lastDiff = lastDiffRef.get();
            if (newModels instanceof SequenceDiffOwner && oldModels instanceof SequenceDiffOwner) {
                diff = ((SequenceDiffOwner)newModels).differenceFromPrevious().orElse(null);
            }
            lastDiffRef.set(diff);

            if ( diff == null || ( lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) ) {
                thisComponent.removeAllEntries();
                _addAllEntriesAt(attr, thisComponent, 0, newModels, lensSupplier, viewSupplier);
            } else {
                int index = diff.index().orElse(-1);
                int count = diff.size();
                _update(thisComponent, attr, diff.change(), index, count, newModels, lensSupplier, viewSupplier);
            }
            viewSupplier.clearCurrentViews();
        });
        propertyOfModels.ifPresent( (tupleOfModels) -> {
            scrollPanels.removeAllEntries();
            _addAllEntriesAt(attr, scrollPanels, 0, tupleOfModels, lensSupplier, viewSupplier);
        });
    }

    private <M> void _update(
            P c,
            @Nullable AddConstraint attr,
            SequenceChange change,
            int index,
            int count,
            Tuple<M> tupleOfModels,
            ModelToViewConverter<M> viewSupplier
    ) {
        if ( index < 0 ) {
            // We do a simple re-build
            c.removeAllEntries();
            _addAllEntriesAt(attr, c, 0, tupleOfModels, viewSupplier);
        } else {
            switch (change) {
                case SET:
                    Tuple<M> slice = tupleOfModels.sliceAt(index, count);
                    _setAllEntriesAt(attr, c, index, slice, viewSupplier);
                    break;
                case ADD:
                    _addAllEntriesAt(attr, c, index, tupleOfModels.sliceAt(index, count), viewSupplier);
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
                    log.error("Unknown change type: {}", change, new Throwable());
                    // We do a simple rebuild:
                    c.removeAllEntries();
                    _addAllEntriesAt(attr, c, 0, tupleOfModels, viewSupplier);
            }
        }
    }

    private <M> void _update(
            P c,
            @Nullable AddConstraint attr,
            SequenceChange change,
            int index,
            int count,
            Tuple<M> newModels,
            Function<Integer, ViewHandle<M>> lensSupplier,
            ModelToViewConverter<ViewHandle<M>> viewSupplier
    ) {
        if ( index < 0 ) {
            // We do a simple re-build
            c.removeAllEntries();
            _addAllEntriesAt(attr, c, 0, newModels, lensSupplier, viewSupplier);
        } else {
            switch (change) {
                case SET:
                    _setAllEntriesAt(attr, c, index, count, lensSupplier, viewSupplier);
                    break;
                case ADD:
                    _addAllEntriesAt(attr, c, index, newModels.sliceAt(index, count), lensSupplier, viewSupplier);
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
                    log.error("Unknown change type: {}", change, new Throwable());
                    // We do a simple rebuild:
                    c.removeAllEntries();
                    _addAllEntriesAt(attr, c, 0, newModels, lensSupplier, viewSupplier);
            }
        }
    }

}

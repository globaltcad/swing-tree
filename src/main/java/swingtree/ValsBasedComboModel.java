package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Var;
import sprouts.Vals;
import sprouts.Viewables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *  A combo box model whose available options are modelled by a read only
 *  {@link Vals} property list. Note that the UI thread never accesses the
 *  property list itself, because it belongs to the application thread!
 *  Instead, this model maintains a UI thread owned snapshot of the items,
 *  which is refreshed through the change events of the property list.
 *  This is the same threading convention that the selection state
 *  of the {@link AbstractComboModel} follows.
 */
final class ValsBasedComboModel<E extends @Nullable Object> extends AbstractComboModel<E>
{
    private final Vals<E> _items;
    private final Viewables<E> _itemsView; // A strong reference keeps the (weakly parented) view and its change listeners alive.
    private volatile List<E> _itemsSnapshot; // The UI thread owned copy of the state of `_items`.

    ValsBasedComboModel( Vals<E> items ) {
        this(Var.ofNullable(_findCommonType(items), null), items);
    }

    ValsBasedComboModel( Var<E> var, Vals<E> items ) {
        super(var);
        _items         = Objects.requireNonNull(items);
        _itemsSnapshot = _snapshotOf(items.toList());
        _selectedIndex = _indexOf(_getSelectedItemSafely());
        _itemsView = _items.view();
        _itemsView.onChange( it -> {
            List<E> newItems = _snapshotOf(it.currentValues().toList());
            _publishToUIThread(() -> {
                _itemsSnapshot = newItems;
                updateSelectedIndex();
                fireListeners();
            });
        });
    }

    private static <E> List<E> _snapshotOf( List<E> items ) {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    @Override public int getSize() { return _itemsSnapshot.size(); }

    @Override public @Nullable E getElementAt( int index ) {
        List<E> snapshot = _itemsSnapshot;
        if ( index < 0 || index >= snapshot.size() )
            return null;
        return snapshot.get(index);
    }

    @Override public AbstractComboModel<E> withVar( Var<E> newVar ) {
        return new ValsBasedComboModel<>(newVar, _items);
    }

    @Override
    protected void setAt( int index, @Nullable E element ) {
        /*
            Vals are immutable, so we can't modify them.
            So we'll just ignore it.
        */
    }
}

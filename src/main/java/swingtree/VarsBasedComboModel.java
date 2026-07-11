package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Var;
import sprouts.Vars;
import sprouts.Viewables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *  A combo box model whose available options are modelled by a {@link Vars}
 *  property list. Note that the UI thread never accesses the property list itself,
 *  because it belongs to the application thread! Instead, this model maintains
 *  a UI thread owned snapshot of the items, which is refreshed through the change
 *  events of the property list, and conversely, an item edited by the user in the
 *  view is first applied to this snapshot and then handed over to the application
 *  thread, which writes it into the property list.
 *  This is the same threading convention that the selection state
 *  of the {@link AbstractComboModel} follows.
 */
final class VarsBasedComboModel<E extends @Nullable Object> extends AbstractComboModel<E>
{
    private final Vars<E> _items;
    private final Viewables<E> _itemsView; // A strong reference keeps the (weakly parented) view and its change listeners alive.
    private volatile List<E> _itemsSnapshot; // The UI thread owned copy of the state of `_items`.

    VarsBasedComboModel( Vars<E> items ) {
        this(Var.ofNullable(_findCommonType(items), null), items);
    }

    VarsBasedComboModel( Var<E> var, Vars<E> items ) {
        super(var);
        _items         = Objects.requireNonNull(items);
        _itemsSnapshot = _snapshotOf(items.toList());
        _selectedIndex = _indexOf(_getSelectedItemSafely());
        _itemsView = _items.view();
        _itemsView.onChange( it -> _itemListChanged(_snapshotOf(it.currentValues().toList())) );
    }

    private static <E> List<E> _snapshotOf( List<E> items ) {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    private void _itemListChanged( List<E> newItems ) {
        _publishToUIThread(()-> {
            _itemsSnapshot = newItems;
            int newSelection = _indexOf(_getSelectedItemSafely());
            if ( newSelection != _selectedIndex )
                this.setSelectedItem(getElementAt(newSelection));
                // ^ This will fire the listeners for us already
            else
                fireListeners();
        });
    }

    @Override public int getSize() { return _itemsSnapshot.size(); }

    @Override public @Nullable E getElementAt( int index ) {
        List<E> snapshot = _itemsSnapshot;
        if ( index < 0 || index >= snapshot.size() )
            return null;
        return snapshot.get(index);
    }

    @Override public AbstractComboModel<E> withVar(Var<E> newVar) {
        return new VarsBasedComboModel<>(newVar, _items);
    }

    @Override
    protected void setAt(int index, @Nullable E element) {
        /*
            So the UI component tells us a combo option should be changed...
            But does the user of this library want that?
            Well there is a way to find out:
            Is the list we are using here intended to be modified?
            If so, then we should modify it, otherwise we should not.
            The problem is, we don't know if the list is unmodifiable or not.
            We could check if it is an instance of java.util.Collections.UnmodifiableList,
            but that is an internal class, so we can't rely on it.
            So we'll just try to modify it, and if it fails, we'll just ignore it.
         */
        List<E> snapshot = _itemsSnapshot;
        if ( index >= 0 && index < snapshot.size() ) {
            List<E> updatedSnapshot = new ArrayList<>(snapshot);
            updatedSnapshot.set(index, element);
            _itemsSnapshot = Collections.unmodifiableList(updatedSnapshot);
        }
        _publishToAppThread(() -> {
            try {
                if ( index >= 0 && index < _items.size() )
                    _items.at(index).set(NullUtil.fakeNonNull(element));
            } catch ( UnsupportedOperationException ignored ) {
                // ignore, the user of this library doesn't want us to modify the list
            }
        });
    }
}

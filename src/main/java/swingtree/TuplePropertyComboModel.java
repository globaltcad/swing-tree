package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;

/**
 *  A combo box model whose available options are modelled by a {@link Tuple}
 *  based property. Note that the UI thread never accesses the property itself!
 *  Because a {@link Tuple} is deeply immutable, the two threads can simply
 *  publish their state to each other: this model maintains a UI thread owned
 *  snapshot of the tuple, which is refreshed through the property change events
 *  of the item property, and conversely, an item edited by the user in the view
 *  is first applied to this snapshot and then handed over to the application
 *  thread, which writes it into the property.
 *  This is the same threading convention that the selection state
 *  of the {@link AbstractComboModel} follows.
 */
final class TuplePropertyComboModel<E extends @Nullable Object> extends AbstractComboModel<E>
{
	private final Val<Tuple<E>> _items;
	private final Viewable<Tuple<E>> _itemsView; // A strong reference keeps the (weakly parented) view and its change listener alive.
	private volatile @Nullable Tuple<E> _itemsSnapshot; // The UI thread owned copy of the state of `_items`.
	private final boolean _mutable;

	TuplePropertyComboModel(Var<E> selection, Val<Tuple<E>> items ) {
		this( selection, items, false );
	}

	TuplePropertyComboModel(Var<E> selection, Var<Tuple<E>> items ) {
		this( selection, items, true );
	}

	private TuplePropertyComboModel(Var<E> selection, Val<Tuple<E>> items, boolean mutable ) {
		super(selection);
		_items = items;
		_itemsSnapshot = items.orElseNull();
		_mutable = mutable;
		_selectedIndex = _indexOf(_getSelectedItemSafely());
		_itemsView = _items.view();
		_itemsView.onChange(From.ALL, it -> {
			@Nullable Tuple<E> newItems = it.currentValue().orElseNull();
			_publishToUIThread(() -> {
				_itemsSnapshot = newItems;
				updateSelectedIndex();
				fireListeners();
			});
		});
	}

	@Override
	public AbstractComboModel<E> withVar( Var<E> newVar ) {
		return new TuplePropertyComboModel<>(newVar, _items, _mutable);
	}

	@Override protected void setAt(int index, @Nullable E element) {
		if ( _mutable && _items instanceof Var ) {
			Tuple<E> snapshot = _itemsSnapshot;
			if ( snapshot != null && index >= 0 && index < snapshot.size() )
				_itemsSnapshot = snapshot.setAt(index, NullUtil.fakeNonNull(element));
			_publishToAppThread(() ->
				((Var<Tuple<E>>)_items).update(From.VIEW, tuple -> {
					if ( index < 0 || index >= tuple.size() )
						return tuple; // The tuple changed in the meantime; the edit no longer applies.
					return tuple.setAt(index, NullUtil.fakeNonNull(element));
				})
			);
		}
	}

	@Override public int getSize() {
		Tuple<E> snapshot = _itemsSnapshot;
		return snapshot == null ? 0 : snapshot.size();
	}

	@Override public @Nullable E getElementAt( int index ) {
		Tuple<E> snapshot = _itemsSnapshot;
		if ( snapshot == null || index < 0 || index >= snapshot.size() )
			return null;
		return snapshot.get(index);
	}

}

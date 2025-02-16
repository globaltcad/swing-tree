package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;

final class TuplePropertyComboModel<E extends @Nullable Object> extends AbstractComboModel<E>
{
	private final Val<Tuple<E>> _items;
	private final boolean _mutable;

	TuplePropertyComboModel(Var<E> selection, Val<Tuple<E>> items ) {
		super(selection);
		_items = items;
		_selectedIndex = _indexOf(_getSelectedItemSafely());
		_mutable = false;
	}

	TuplePropertyComboModel(Var<E> selection, Var<Tuple<E>> items ) {
		super(selection);
		_items = items;
		_selectedIndex = _indexOf(_getSelectedItemSafely());
		_mutable = true;
	}

	@Override
	public AbstractComboModel<E> withVar( Var<E> newVar ) {
		return new TuplePropertyComboModel<>(newVar, _items);
	}

	@Override protected void setAt(int index, @Nullable E element) {
		if ( _mutable && _items instanceof Var )
			((Var<Tuple<E>>)_items).update(From.VIEW, tuple -> tuple.setAt(index, NullUtil.fakeNonNull(element)));
	}

	@Override public int getSize() {
		return _items.mapTo(Integer.class, Tuple::size).orElse(0);
	}

	@Override public @Nullable E getElementAt( int index ) {
		return (E) _items.mapTo(Object.class, i -> i.get(index) ).orElseNull();
	}

}
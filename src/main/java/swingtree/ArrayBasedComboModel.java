package swingtree;

import sprouts.Var;

import java.util.Objects;

final class ArrayBasedComboModel<E> extends AbstractComboModel<E>
{
	private final E[] _items;


	ArrayBasedComboModel(E[] items) {
		this(Var.ofNullable(_findCommonType( items ), null), items);
	}

	ArrayBasedComboModel(Var<E> selection, E[] items) {
		super(selection);
		_items = Objects.requireNonNull(items);
		_selectedIndex = _indexOf(_selectedItem.orElseNull());
	}


	@Override
	public AbstractComboModel<E> withVar( Var<E> newVar ) {
		return new ArrayBasedComboModel<>(newVar, _items);
	}

	@Override protected void setAt( int index, E element ) { _items[index] = element; }
	@Override public int getSize() { return _items.length; }
	@Override public E getElementAt( int index ) { return _items[index]; }

}

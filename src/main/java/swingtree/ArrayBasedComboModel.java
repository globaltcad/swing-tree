package swingtree;

import sprouts.Var;

class ArrayBasedComboModel<E> extends AbstractComboModel<E>
{
	private final E[] items;

	ArrayBasedComboModel(E[] items) {
		this(Var.ofNullable(_findCommonType( items ), null), items);
	}

	ArrayBasedComboModel(Var<E> selection, E[] items) {
		super(selection);
		this.items = items;
		this._selectedIndex = _indexOf(_selectedItem.orElseNull());
	}

	@Override
	public AbstractComboModel<E> withVar( Var<E> newVar ) {
		return new ArrayBasedComboModel<>(newVar, items);
	}

	@Override protected void setAt(int index, E element) { items[index] = element; }
	@Override public int getSize() { return items.length; }
	@Override public E getElementAt( int index ) { return items[index]; }

}

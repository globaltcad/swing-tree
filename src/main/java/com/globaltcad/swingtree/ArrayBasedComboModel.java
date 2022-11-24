package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Var;

class ArrayBasedComboModel<E> extends AbstractComboModel<E>
{
	private final E[] items;

	ArrayBasedComboModel(E[] items) {
		super(Var.of(_findCommonType( items ), null));
		this.items = items;
		selectedIndex = _indexOf(selectedItem.orElseNull());
	}

	ArrayBasedComboModel(Var<E> selection, E[] items) {
		super(selection);
		this.items = items;
		selectedIndex = _indexOf(selectedItem.orElseNull());
	}

	@Override protected void setAt(int index, E element) { items[index] = element; }
	@Override public int getSize() { return items.length; }
	@Override public E getElementAt( int index ) { return items[index]; }
}

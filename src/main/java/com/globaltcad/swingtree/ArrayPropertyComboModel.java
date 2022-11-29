package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Var;

class ArrayPropertyComboModel<E> extends AbstractComboModel<E>
{
	private final Var<E[]> items;

	ArrayPropertyComboModel(Var<E> selection, Var<E[]> items) {
		super(selection);
		this.items = items;
		this._selectedIndex = _indexOf(_selectedItem.orElseNull());
	}

	@Override
	public AbstractComboModel<E> withVar(Var<E> newVar) {
		return new ArrayPropertyComboModel<>(newVar, items);
	}

	@Override protected void setAt(int index, E element) {
		items.ifPresent( i -> { i[index] = element; items.act(); });
	}
	@Override public int getSize() { return items.map( i -> i.length ).orElse(0); }
	@Override public E getElementAt( int index ) { return items.map( i -> i[index] ).orElseNull(); }

}
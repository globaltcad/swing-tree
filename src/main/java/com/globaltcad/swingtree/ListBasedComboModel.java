package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Var;

import java.util.List;

class ListBasedComboModel<E> extends AbstractComboModel<E>
{
	private final List<E> items;

	ListBasedComboModel(List<E> items) {
		super(Var.of(_findCommonType( (E[]) items.toArray() ), null));
		this.items = items;
		selectedIndex = _indexOf(selectedItem.orElseNull());
	}

	ListBasedComboModel(Var<E> var, List<E> items) {
		super(var);
		this.items = items;
		selectedIndex = _indexOf(selectedItem.orElseNull());
	}

	@Override public int getSize() { return items.size(); }
	@Override public E getElementAt( int index ) { return items.get(index); }
	@Override protected void setAt(int index, E element) {
		/*
			So the UI component tells us a combo option should be changed...
			But does the user of this library want that?
			Well there is a way to find out:
			Is the list we are using here intended to be modified?
			If so, then we should modify it, otherwise we should not.
			The problem is, we don't know if the list is unmodifiable or not.
			We could check if it is an instance of java.util.Collections.UnmodifiableList,
			but that is a internal class, so we can't rely on it.
			So we'll just try to modify it, and if it fails, we'll just ignore it.
		 */
		try {
			items.set(index, element);
		} catch (UnsupportedOperationException ignored) {
			// ignore, the user of this library doesn't want us to modify the list
		}
	}
}

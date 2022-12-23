package swingtree;

import swingtree.api.mvvm.Var;
import swingtree.api.mvvm.Vals;

class ValsBasedComboModel<E> extends AbstractComboModel<E>
{
    private final Vals<E> _items;

    ValsBasedComboModel(Vals<E> items) {
        super(Var.ofNullable(_findCommonType(items), null));
        _items = items;
        _selectedIndex = _indexOf(_selectedItem.orElseNull());
    }

    ValsBasedComboModel(Var<E> var, Vals<E> items) {
        super(var);
        _items = items;
        _selectedIndex = _indexOf(_selectedItem.orElseNull());
    }

    @Override public int getSize() { return _items.size(); }

    @Override public E getElementAt( int index ) { return _items.at(index).orElseNull(); }

    @Override public AbstractComboModel<E> withVar(Var<E> newVar) {
        return new ValsBasedComboModel<>(newVar, _items);
    }

    @Override
    protected void setAt(int index, E element) {
        /*
            Vals are immutable, so we can't modify them.
            So we'll just ignore it.
        */
    }
}

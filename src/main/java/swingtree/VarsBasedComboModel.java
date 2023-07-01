package swingtree;

import sprouts.Var;
import sprouts.Vars;

class VarsBasedComboModel<E> extends AbstractComboModel<E>
{
    private final Vars<E> _items;

    VarsBasedComboModel( Vars<E> items ) {
        super(Var.ofNullable(_findCommonType(items), null));
        _items = items;
        _selectedIndex = _indexOf(_selectedItem.orElseNull());
    }

    VarsBasedComboModel( Var<E> var, Vars<E> items ) {
        super(var);
        _items = items;
        _selectedIndex = _indexOf(_selectedItem.orElseNull());
    }

    @Override public int getSize() { return _items.size(); }

    @Override public E getElementAt( int index ) { return _items.at(index).orElseNull(); }

    @Override public AbstractComboModel<E> withVar(Var<E> newVar) {
        return new VarsBasedComboModel<>(newVar, _items);
    }

    @Override
    protected void setAt(int index, E element) {
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
            _items.at(index).set(element);
        } catch (UnsupportedOperationException ignored) {
            // ignore, the user of this library doesn't want us to modify the list
        }
    }
}

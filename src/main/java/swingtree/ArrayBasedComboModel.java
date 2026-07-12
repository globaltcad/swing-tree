package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Var;

import java.util.Objects;

/**
 *  A combo box model based on a plain array of items which is read live,
 *  meaning that the {@link javax.swing.JComboBox} always sees the current
 *  content of the array whenever it is rendered.
 *  <p>
 *  Note that a plain array is shared mutable state without any change events,
 *  so unlike the property based combo box models, there is no way for this
 *  model to maintain a properly synchronized UI thread owned snapshot of it.
 *  This live read contract is convenient for static or UI thread managed
 *  option arrays, but if your options change dynamically on an application
 *  thread (see {@link swingtree.threading.EventProcessor#DECOUPLED}), prefer
 *  modelling them as a {@link sprouts.Vars} list or a {@link sprouts.Tuple}
 *  based property, which are fully thread safe.
 */
final class ArrayBasedComboModel<E extends @Nullable Object> extends AbstractComboModel<E>
{
	private final E[] _items;


	ArrayBasedComboModel(E[] items) {
		this(Var.ofNullable(_findCommonType( items ), null), items);
	}

	ArrayBasedComboModel(Var<E> selection, E @Nullable[] items) {
		super(selection);
		_items = Objects.requireNonNull(items);
		_selectedIndex = _indexOf(_getSelectedItemSafely());
	}


	@Override
	public AbstractComboModel<E> withVar( Var<E> newVar ) {
		return new ArrayBasedComboModel<>(newVar, _items);
	}

	@Override protected void setAt( int index, @Nullable E element ) { _items[index] = element; }
	@Override public int getSize() { return _items.length; }
	@Override public @Nullable E getElementAt( int index ) { return _items[index]; }

}

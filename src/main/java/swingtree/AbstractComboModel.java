package swingtree;

import org.slf4j.Logger;
import sprouts.From;
import sprouts.Var;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Objects;

/**
 *  A {@link ComboBoxModel} type designed in a way to allow for MVVM style
 *  property binding to the selection state of the model.
 *  This model wraps a {@link sprouts.Var} instance which is used
 *  to dynamically model the selection state of the model.
 *
 * @param <E> The type of the elements which will be stored in this model.
 */
abstract class AbstractComboModel<E> implements ComboBoxModel<E>
{
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractComboModel.class);

	protected int _selectedIndex = -1;
	final Var<E> _selectedItem;
	protected java.util.List<ListDataListener> listeners = new ArrayList<>();

	private boolean _acceptsEditorChanges = true; // This is important to prevent getting feedback loops!

	protected static <E> Class<E> _findCommonType( E[] items ) {
		Iterable<E> iterable = () -> java.util.Arrays.stream(items).iterator();
		return _findCommonType(iterable);
	}

	protected static <E> Class<E> _findCommonType( Iterable<E> items ) {
		if ( items == null ) return (Class<E>) Object.class;
		Class<E> type = null;
		for ( E item : items ) {
			if ( item == null ) continue;
			if ( type == null )
				type = (Class<E>) item.getClass();
			else
				type = (Class<E>) Object.class;
		}
		return type;
	}

	AbstractComboModel( Var<E> selectedItem ) {
		_selectedItem = Objects.requireNonNull(selectedItem);
	}

	final Var<E> _getSelectedItemVar() { return _selectedItem; }

	abstract AbstractComboModel<E> withVar( Var<E> newVar );

	@Override public void setSelectedItem( Object anItem ) {
		if ( anItem != null && !_selectedItem.type().isAssignableFrom(anItem.getClass()) )
			anItem = _convert(anItem.toString());
        E old = _selectedItem.orElseNull();
		Object finalAnItem = anItem;
		doQuietly(()-> {
			_selectedItem.set(From.VIEW, (E) finalAnItem);
			_selectedIndex = _indexOf(finalAnItem);
			if ( !Objects.equals(old, finalAnItem) )
				fireListeners();
		});
	}

	/** {@inheritDoc} */
	@Override public Object getSelectedItem() { return _selectedItem.orElseNull(); }

	/** {@inheritDoc} */
	@Override public void addListDataListener( ListDataListener l ) { listeners.add(l); }

	/** {@inheritDoc} */
	@Override public void removeListDataListener( ListDataListener l ) { listeners.remove(l); }

    void fireListeners() {
		try {
			for ( ListDataListener l : new ArrayList<>(listeners) )
				l.contentsChanged(
					new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize())
				);
		} catch ( Exception e ) {
			log.error("An exception occurred while firing combo box model listeners!", e);
		}
    }

    void doQuietly( Runnable task ) {
    	boolean alreadyWithinQuietTask = !_acceptsEditorChanges;
		_acceptsEditorChanges = false;
		try {
			task.run();
		} catch ( Exception e ) {
			log.error("An exception occurred while running a combo box model task!", e);
		}
		if ( !alreadyWithinQuietTask )
			_acceptsEditorChanges = true;
    }

	abstract protected void setAt( int index, E element );

    void updateSelectedIndex() {
        if ( _selectedIndex >= 0 && !_selectedItem.is(getElementAt(_selectedIndex)) )
            _selectedIndex = _indexOf(_selectedItem.orElseNull());
    }

	void setFromEditor( String o ) {
		if ( !_acceptsEditorChanges )
			return; // The editor of a combo box can have very strange behaviour when it is updated by listeners

		updateSelectedIndex();
		if ( _selectedIndex != -1 ) {
			try {
				E e = _convert(o);
				this.setAt( _selectedIndex, e );
				boolean stateChanged = _selectedItem.orElseNull() != e;
				_selectedItem.set(From.VIEW, e);
				if ( stateChanged )
					doQuietly(this::fireListeners);

			} catch (Exception ignored) {
				// It looks like conversion was not successful
				// So this means the editor input could not be converted to the type of the combo box
				// So we'll just ignore it
			}
		}
	}

	/**
	 *  Tries to convert the given {@link String} to the type of the combo box
	 *  through a number of different ways.
	 * @param o The string to convert
	 * @return The converted object or simply the item of the combo box if no conversion was possible.
	 */
	private E _convert( String o ) {
		// We need to turn the above string into an object of the correct type!
		// First of all, we know our target type:
		Class<E> type = _selectedItem.type();
		// Now we need to convert it to that type, let's try a few things:
		if ( type == Object.class )
			return (E) o; // So apparently the type is intended to be Object, so we'll just return the string

		if ( type == String.class ) // The most elegant case, the type is String, so we'll just return the string
			return (E) o;

		if ( Number.class.isAssignableFrom(type) ) {
			// Ah, a number, let's try to parse it, but first we make it easier.
			o = o.trim();
			if ( o.endsWith("f") || o.endsWith("F") )
				o = o.substring(0, o.length() - 1);

			if ( o.endsWith("d") || o.endsWith("D") )
				o = o.substring(0, o.length() - 1);

			if ( o.endsWith("l") || o.endsWith("L") )
				o = o.substring(0, o.length() - 1);

			try {
				if ( type == Integer.class ) return (E) Integer.valueOf(o);
				if ( type == Double.class  ) return (E) Double.valueOf(o);
				if ( type == Float.class   ) return (E) Float.valueOf(o);
				if ( type == Long.class    ) return (E) Long.valueOf(o);
				if ( type == Short.class   ) return (E) Short.valueOf(o);
				if ( type == Byte.class    ) return (E) Byte.valueOf(o);
			} catch ( NumberFormatException e ) {
				// We failed to parse the number... the input is invalid!
				// So we cannot update the model, and simply return the old value:
				return _selectedItem.orElseNull();
			}
		}
		// What now? Hmmm, let's try Boolean!
		if ( type == Boolean.class ) {
			o = o.trim().toLowerCase();
			if ( o.equals("true") || o.equals("yes") || o.equals("1") )
				return (E) Boolean.TRUE;

			if ( o.equals("false") || o.equals("no") || o.equals("0") )
				return (E) Boolean.FALSE;

			// We failed to parse the boolean... the input is invalid!
			// So we cannot update the model, and simply return the old value:
			return _selectedItem.orElseNull();
		}
		// Ok maybe it's an enum?
		if ( type.isEnum() ) {
			Class<Enum> enumType = (Class<Enum>) type;
			String name = o.trim();
			try { return (E) Enum.valueOf(enumType, name); } catch ( IllegalArgumentException ignored) {}
			name = o.toUpperCase();
			try { return (E) Enum.valueOf(enumType, name); } catch ( IllegalArgumentException ignored) {}
			name = o.toLowerCase();
			try { return (E) Enum.valueOf(enumType, name); } catch ( IllegalArgumentException ignored) {}
			name = name.toUpperCase().replace(' ', '_').replace('-', '_');
			try { return (E) Enum.valueOf(enumType, name); } catch ( IllegalArgumentException ignored) {}
			name = name.toLowerCase().replace(' ', '_').replace('-', '_');
			try { return (E) Enum.valueOf(enumType, name); } catch ( IllegalArgumentException ignored) {}
			// We failed to parse the enum... the input is invalid!
			// So we cannot update the model, and simply return the old value:
			return _selectedItem.orElseNull();
		}
		// Or a character?
		if ( type == Character.class ) {
			if ( o.trim().length() == 1 )
				return (E) Character.valueOf(o.charAt(0));
			// Maybe it's all repeated?
			if ( o.trim().length() > 1 ) {
				char c = o.charAt(0);
				for ( int i = 1; i < o.length(); i++ )
					if ( o.charAt(i) != c )
						return _selectedItem.orElseNull();
				return (E) Character.valueOf(c);
			}
			// We failed to parse the character... the input is invalid!
			// So we cannot update the model, and simply return the old value:
			return _selectedItem.orElseNull();
		}
		// Now it's getting tricky, but we don't give up. How about arrays?
		if ( type.isArray() ) {
			// We need to split the string into elements, and then convert each element
			// to the correct type. We can do this recursively, but first we need to
			// find the type of the elements:
			Class<?> componentType = type.getComponentType();
			// Now we can split the string:
			String[] parts = o.split(",");
			if ( parts.length == 1 )
				parts = o.split(" ");

			// And convert each part to the correct type:
			Object[] array = (Object[]) java.lang.reflect.Array.newInstance(componentType, parts.length);
			for ( int i = 0; i < parts.length; i++ )
				array[i] = _convert(parts[i]);

			// And finally we can return the array:
			return (E) array;
		}
		// Uff! Still nothing?!? Ok let's be creative, maybe we can try to use the constructor:
		try {
			return type.getConstructor(String.class).newInstance(o);
		} catch ( Exception e ) {
			// We failed to instantiate the class... Quite a pity, but at this point, who cares?
		}

		// What else is there? We don't know, so we just return the old value:
		return _selectedItem.orElseNull();
	}

	protected int _indexOf( Object anItem ) {
		for ( int i = 0; i < getSize(); i++ )
			if ( Objects.equals(anItem, getElementAt(i)) )
				return i;

		return _selectedIndex;
	}
}

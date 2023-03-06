package swingtree;

import sprouts.Change;
import sprouts.Vals;
import sprouts.Var;
import swingtree.api.mvvm.Viewable;
import swingtree.api.mvvm.ViewableEntry;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  A builder node for {@link JScrollPanels} a custom Swing-Tree component
 *  which is similar to a {@link JList} but with the ability to interact with
 *  the individual components in the list.
 *  <p>
 *
 * @param <P> The type of the component which this builder node wraps.
 */
public class UIForScrollPanels<P extends JScrollPanels> extends UIForScrollPane<P>
{
	/**
	 * Extensions of the {@link  UIForAbstractSwing} always wrap
	 * a single component for which they are responsible.
	 *
	 * @param component The JComponent type which will be wrapped by this builder node.
	 */
	public UIForScrollPanels( P component ) { super(component); }


	@Override
	protected void _add( JComponent component, Object conf ) {
		Objects.requireNonNull(component);
		JScrollPanels panels = this.getComponent();

		ViewableEntry entry = _entryFrom(component);
		if ( conf == null )
			panels.addEntry(entry);
		else
			panels.addEntry(conf.toString(), entry);
	}

	private ViewableEntry _entryFrom(JComponent component) {
		Var<Boolean> selected = Var.of(false);
		Var<Integer> position = Var.of(0);
		return new ViewableEntry() {
			@Override public Var<Boolean> isSelected() { return selected; }
			@Override public Var<Integer> position() { return position; }
			@Override public <V> V createView(Class<V> viewType) { return (V) component; }
		};
	}

	@Override
	protected void _addViewableProps( Vals<? extends Viewable> viewables, String attr )
	{
		Function<Integer, Viewable> viewableFetcher = i -> {
			Viewable v = viewables.at(i).get();
			if ( v instanceof ViewableEntry ) ((ViewableEntry) v).position().set(i);
			return v;
		};
		Function<Integer, ViewableEntry> entryFetcher = i -> {
			Viewable v = viewableFetcher.apply(i);
			return ( v instanceof ViewableEntry ? (ViewableEntry) v : _entryFrom(v.createView(JComponent.class)) );
		};

		Runnable addAll = () -> {
			boolean allAreEntries = viewables.stream().allMatch( v -> v instanceof ViewableEntry );
			if ( allAreEntries ) {
				List<ViewableEntry> entries = (List<ViewableEntry>) viewables.toList();
				this.getComponent().addAllEntries(attr, entries);
			}
			else
				for ( int i = 0; i< viewables.size(); i++ )
					this.getComponent().addEntry(entryFetcher.apply(i));
		};
		Consumer<Integer> addAt    = i -> getComponent().addEntryAt( i, null, entryFetcher.apply(i) );
		Consumer<Integer> removeAt = i -> getComponent().removeEntryAt( i );
		Consumer<Integer> setAt    = i -> getComponent().setEntryAt( i, null, entryFetcher.apply(i) );

		_onShow( viewables, delegate -> {
			JScrollPanels panels = this.getComponent();
			// we simply redo all the components.
			switch ( delegate.changeType() ) {
				case SET:
				case ADD:
				case REMOVE:
					if ( delegate.index() >= 0 ) {
						if ( delegate.changeType() == Change.ADD )
							addAt.accept(delegate.index());
						else if ( delegate.changeType() == Change.REMOVE )
							removeAt.accept(delegate.index());
						else if ( delegate.changeType() == Change.SET )
							setAt.accept(delegate.index());

						// Now we need to update the positions of all the entries
						for ( int i = delegate.index(); i < viewables.size(); i++ )
							entryFetcher.apply(i).position().set(i);
					} else {
						panels.removeAllEntries();
						addAll.run();
					}
				break;
				case CLEAR: panels.removeAllEntries(); break;
				case NONE: break;
				default: throw new IllegalStateException("Unknown type: "+delegate.changeType());
			}
		});
		addAll.run();
	}
}

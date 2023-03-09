package swingtree;

import sprouts.Change;
import sprouts.Vals;
import sprouts.Var;
import swingtree.api.mvvm.Viewable;
import swingtree.api.mvvm.ViewableEntry;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
		BiFunction<Integer, Vals<Viewable>, Viewable> viewableFetcher = (i, vals) -> {
			Viewable v = vals.at(i).get();
			if ( v instanceof ViewableEntry ) ((ViewableEntry) v).position().set(i);
			return v;
		};
		BiFunction<Integer, Vals<Viewable>, ViewableEntry> entryFetcher = (i, vals) -> {
			Viewable v = viewableFetcher.apply(i, vals);
			return ( v instanceof ViewableEntry ? (ViewableEntry) v : _entryFrom(v.createView(JComponent.class)) );
		};

		Consumer<Vals<Viewable>> addAll = vals -> {
			boolean allAreEntries = vals.stream().allMatch( v -> v instanceof ViewableEntry );
			if ( allAreEntries ) {
				List<ViewableEntry> entries = (List) vals.toList();
				this.getComponent().addAllEntries(attr, entries);
			}
			else
				for ( int i = 0; i< vals.size(); i++ )
					this.getComponent().addEntry(entryFetcher.apply(i,vals));
		};

		_onShow( viewables, delegate -> {
			JScrollPanels panels = this.getComponent();
			Vals<Viewable> vals = (Vals<Viewable>) delegate.vals();
			int delegateIndex = delegate.index();
			Change changeType = delegate.changeType();
			// we simply redo all the components.
			switch ( changeType ) {
				case SET:
				case ADD:
				case REMOVE:
					if ( delegateIndex >= 0 ) {
						if ( changeType == Change.ADD )
							panels.addEntryAt( delegateIndex, null, entryFetcher.apply(delegateIndex, vals) );
						else if ( changeType == Change.REMOVE )
							panels.removeEntryAt( delegateIndex );
						else if ( changeType == Change.SET )
							panels.setEntryAt( delegateIndex, null, entryFetcher.apply(delegateIndex, vals) );

						// Now we need to update the positions of all the entries
						for ( int i = delegateIndex; i < vals.size(); i++ )
							entryFetcher.apply(i, vals).position().set(i);
					} else {
						panels.removeAllEntries();
						addAll.accept(vals);
					}
				break;
				case CLEAR: panels.removeAllEntries(); break;
				case NONE: break;
				default: throw new IllegalStateException("Unknown type: "+delegate.changeType());
			}
		});
		addAll.accept((Vals<Viewable>) viewables);
	}
}

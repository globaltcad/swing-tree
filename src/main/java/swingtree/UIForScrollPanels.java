package swingtree;

import sprouts.Change;
import sprouts.Vals;
import sprouts.Var;
import swingtree.api.mvvm.EntryViewModel;
import swingtree.api.mvvm.Viewable;
import swingtree.api.mvvm.ViewableEntry;
import swingtree.api.mvvm.ViewSupplier;
import swingtree.components.JScrollPanels;

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
	 * Extensions of the {@link  UIForAnySwing} always wrap
	 * a single component for which they are responsible.
	 *
	 * @param component The JComponent type which will be wrapped by this builder node.
	 */
	public UIForScrollPanels( P component ) { super(component); }


	@Override
	protected void _add( JComponent component, Object conf ) {
		Objects.requireNonNull(component);
		JScrollPanels panels = this.getComponent();

		EntryViewModel entry = _entryModel();
		if ( conf == null )
			panels.addEntry(entry, m -> UI.of(component));
		else
			panels.addEntry(conf.toString(), entry, m -> UI.of(component));
	}

	@Deprecated
	private ViewableEntry _entryFrom(JComponent component) {
		Var<Boolean> selected = Var.of(false);
		Var<Integer> position = Var.of(0);
		return new ViewableEntry() {
			@Override public Var<Boolean> isSelected() { return selected; }
			@Override public Var<Integer> position() { return position; }
			@Override public <V> V createView(Class<V> viewType) { return (V) component; }
		};
	}

	private EntryViewModel _entryModel() {
		Var<Boolean> selected = Var.of(false);
		Var<Integer> position = Var.of(0);
		return new EntryViewModel() {
			@Override public Var<Boolean> isSelected() { return selected; }
			@Override public Var<Integer> position() { return position; }
		};
	}

	@Override
	@Deprecated
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

	@Override
	protected <M> void _addViewableProps( Vals<M> viewables, String attr, ViewSupplier<M> viewSupplier)
	{
		BiFunction<Integer, Vals<M>, M> viewableFetcher = (i, vals) -> {
			M v = vals.at(i).get();
			if ( v instanceof EntryViewModel ) ((EntryViewModel) v).position().set(i);
			return v;
		};
		BiFunction<Integer, Vals<M>, M> entryFetcher = (i, vals) -> {
			M v = viewableFetcher.apply(i, vals);
			return ( v != null ? (M) v : (M)_entryModel() );
		};

		Consumer<Vals<M>> addAll = vals -> {
			boolean allAreEntries = vals.stream().allMatch( v -> v instanceof EntryViewModel );
			if ( allAreEntries ) {
				List<EntryViewModel> entries = (List) vals.toList();
				this.getComponent().addAllEntries(attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
			}
			else
				for ( int i = 0; i< vals.size(); i++ ) {
					int finalI = i;
					this.getComponent().addEntry(
							_entryModel(),//entryFetcher.apply(i,vals),
							m -> viewSupplier.createViewFor(entryFetcher.apply(finalI,vals))
						);
				}
		};

		_onShow( viewables, delegate -> {
			JScrollPanels panels = this.getComponent();
			Vals<M> vals = delegate.vals();
			int delegateIndex = delegate.index();
			Change changeType = delegate.changeType();
			// we simply redo all the components.
			switch ( changeType ) {
				case SET:
				case ADD:
				case REMOVE:
					if ( delegateIndex >= 0 ) {
						if ( changeType == Change.ADD ) {
							M m = entryFetcher.apply(delegateIndex, vals);
							if ( m instanceof EntryViewModel )
								panels.addEntryAt(delegateIndex, null, (EntryViewModel)m, (ViewSupplier<EntryViewModel>) viewSupplier);
							else
								panels.addEntryAt(delegateIndex, null, _entryModel(), em -> viewSupplier.createViewFor(m));
						} else if ( changeType == Change.REMOVE )
							panels.removeEntryAt( delegateIndex );
						else if ( changeType == Change.SET ) {
							M m = entryFetcher.apply(delegateIndex, vals);
							if ( m instanceof EntryViewModel )
								panels.setEntryAt(delegateIndex, null, (EntryViewModel)m, (ViewSupplier<EntryViewModel>) viewSupplier);
							else
								panels.setEntryAt(delegateIndex, null, _entryModel(), em -> viewSupplier.createViewFor(m));
						}
						// Now we need to update the positions of all the entries
						for ( int i = delegateIndex; i < vals.size(); i++ ) {
							M m = entryFetcher.apply(i, vals);
							if ( m instanceof EntryViewModel )
								((EntryViewModel)m).position().set(i);
						}
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
		addAll.accept(viewables);
	}
}

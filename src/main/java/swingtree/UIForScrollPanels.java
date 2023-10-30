package swingtree;

import sprouts.Change;
import sprouts.Vals;
import sprouts.Var;
import swingtree.api.mvvm.EntryViewModel;
import swingtree.api.mvvm.ViewSupplier;
import swingtree.components.JScrollPanels;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 *  A builder node for {@link JScrollPanels}, a custom SwingTree component,
 *  which is similar to a {@link JList} but with the ability to interact with
 *  the individual components in the list.
 *  <p>
 *
 * @param <P> The type of the component which this builder node wraps.
 */
public class UIForScrollPanels<P extends JScrollPanels> extends UIForAnyScrollPane<UIForScrollPanels<P>, P>
{
	private final BuilderState<P> _state;

	/**
	 * Extensions of the {@link  UIForAnySwing} always wrap
	 * a single component for which they are responsible.
	 *
	 * @param state The {@link BuilderState} modelling how the underlying component is build.
	 */
	protected UIForScrollPanels( BuilderState<P> state ) {
		Objects.requireNonNull(state);
		_state = state;
	}

	@Override
	protected BuilderState<P> _state() {
		return _state;
	}

	@Override
	protected UIForScrollPanels<P> _with( BuilderState<P> newState ) {
		return new UIForScrollPanels<>(newState);
	}

	@Override
	protected void _doAddComponent(JComponent newComponent, Object conf, P thisComponent ) {
		Objects.requireNonNull(newComponent);

		EntryViewModel entry = _entryModel();
		if ( conf == null )
			thisComponent.addEntry( entry, m -> UI.of(newComponent) );
		else
			thisComponent.addEntry( conf.toString(), entry, m -> UI.of(newComponent) );
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
	protected <M> void _addViewableProps(
			Vals<M> models, String attr, ViewSupplier<M> viewSupplier, P thisComponent
	) {
		BiFunction<Integer, Vals<M>, M> modelFetcher = (i, vals) -> {
			M v = vals.at(i).get();
			if ( v instanceof EntryViewModel ) ((EntryViewModel) v).position().set(i);
			return v;
		};
		BiFunction<Integer, Vals<M>, M> entryFetcher = (i, vals) -> {
			M v = modelFetcher.apply(i, vals);
			return ( v != null ? (M) v : (M)_entryModel() );
		};

		Consumer<Vals<M>> addAll = vals -> {
			boolean allAreEntries = vals.stream().allMatch( v -> v instanceof EntryViewModel );
			if ( allAreEntries ) {
				List<EntryViewModel> entries = (List) vals.toList();
				thisComponent.addAllEntries(attr, entries, (ViewSupplier<EntryViewModel>) viewSupplier);
			}
			else
				for ( int i = 0; i< vals.size(); i++ ) {
					int finalI = i;
					thisComponent.addEntry(
							_entryModel(),
							m -> viewSupplier.createViewFor(entryFetcher.apply(finalI,vals))
						);
				}
		};

		_onShow( models, thisComponent, (c, delegate) -> {
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
								c.addEntryAt(delegateIndex, null, (EntryViewModel)m, (ViewSupplier<EntryViewModel>) viewSupplier);
							else
								c.addEntryAt(delegateIndex, null, _entryModel(), em -> viewSupplier.createViewFor(m));
						} else if ( changeType == Change.REMOVE )
							c.removeEntryAt( delegateIndex );
						else if ( changeType == Change.SET ) {
							M m = entryFetcher.apply(delegateIndex, vals);
							if ( m instanceof EntryViewModel )
								c.setEntryAt(delegateIndex, null, (EntryViewModel)m, (ViewSupplier<EntryViewModel>) viewSupplier);
							else
								c.setEntryAt(delegateIndex, null, _entryModel(), em -> viewSupplier.createViewFor(m));
						}
						// Now we need to update the positions of all the entries
						for ( int i = delegateIndex; i < vals.size(); i++ ) {
							M m = entryFetcher.apply(i, vals);
							if ( m instanceof EntryViewModel )
								((EntryViewModel)m).position().set(i);
						}
					} else {
						c.removeAllEntries();
						addAll.accept(vals);
					}
				break;
				case CLEAR: c.removeAllEntries(); break;
				case NONE: break;
				default: throw new IllegalStateException("Unknown type: "+delegate.changeType());
			}
		});
		addAll.accept(models);
	}
}

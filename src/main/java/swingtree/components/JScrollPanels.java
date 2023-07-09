package swingtree.components;

import net.miginfocom.swing.MigLayout;
import swingtree.UI;
import swingtree.api.mvvm.EntryViewModel;
import swingtree.api.mvvm.ViewSupplier;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 	The {@link JScrollPanels} class is a container for a list of scrollable UI components
 * 	representing view models or simple data models which are dynamically turned into
 * 	views by a {@link ViewSupplier}.
 *  This class exists to compensate for the deficits of the {@link JList} and {@link JTable} components,
 *  which are not able to receive user events like for example button clicks etc...
 * 	<br>
 * 	A {@link JScrollPanels} instance can arrange its entries in a vertical or horizontal manner
 * 	based on the {@link UI.Align} parameter.
 * 	<br><br>
 * 	Instances of this store view model implementations listen to a view model property list
 * 	so that they can dynamically be turned into views by a {@link ViewSupplier} lambda.
 * 	Here a simple example demonstrating the usage of the {@link JScrollPanels} class
 * 	through the Swing-Tree API:
 * 	<pre>{@code
 *    UI.scrollPanels()
 *    .add(viewModel.entries(), entry ->
 *        UI.panel().add(UI.button("Click me! :)"))
 *    )
 * 	}</pre>
 * 	...where {@code entries()} is a method returning a {@link sprouts.Vars} instance
 * 	which contains a list of your sub-view models.
 * 	The second parameter of the {@link swingtree.UIForScrollPanels#add(sprouts.Vals, ViewSupplier)} method is a lambda
 *  which takes a single view model from the list of view models and turns it into a view.
 */
public class JScrollPanels extends JScrollPane
{
	/**
	 * 	Constructs a new {@link JScrollPanels} instance with the provided alignment and size.
	 * 	@param align The alignment of the entries inside this {@link JScrollPanels} instance.
	 * 				 The alignment can be either {@link UI.Align#HORIZONTAL} or {@link UI.Align#VERTICAL}.
	 * @param size The size of the entries in this {@link JScrollPanels} instance.
	 * @return A new {@link JScrollPanels} instance.
	 */
	public static JScrollPanels of(
		UI.Align align, Dimension size
	) {
		Objects.requireNonNull(align);
		return _construct(align, size, Collections.emptyList(), null, m -> UI.panel());
	}

	private static JScrollPanels _construct(
		UI.Align align,
		Dimension shape,
		List<EntryViewModel> models,
		String constraints,
		ViewSupplier<EntryViewModel> viewSupplier
	) {
		UI.Align type = align;
		InternalPanel[] forwardReference = {null};
		List<EntryPanel> entries =
				IntStream.range(0,models.size())
						.mapToObj( i ->
							new EntryPanel(
									()-> _entriesIn(forwardReference[0].getComponents()),
									i,
									models.get(i),
									viewSupplier,
									constraints
								)
						)
						.collect(Collectors.toList());


		InternalPanel internalWrapperPanel = new InternalPanel(entries, shape, type);
		JScrollPanels newJScrollPanels = new JScrollPanels(internalWrapperPanel);
		forwardReference[0] = internalWrapperPanel;

		if ( type == UI.Align.HORIZONTAL )
			newJScrollPanels.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		else
			newJScrollPanels.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		return newJScrollPanels;
	}


	private final InternalPanel _internal; // Wrapper for the actual UI components


	private JScrollPanels(InternalPanel listWrapper) {
		super(listWrapper);
		_internal = listWrapper;
	}

	/**
	 * @return The number of entries which are currently managed by this {@link JScrollPanels}.
	 */
	public int getNumberOfEntries() { return _internal.getComponents().length; }

	/**
	 * 	The {@link JScrollPanels} does not store components statically in the UI tree.
	 * 	Instead, it is a hybrid of the traditional static approach
	 * 	and a renderer based approach (as in the {@link JList}).
	 * 	The lambda passed to this method is responsible for continuously supplying a UI
	 * 	which fits a certain context (which defines if the entry is selected or not among other things).
	 *
	 * @param entryViewModel A view model which ought to be added.
	 * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
	 */
	public <M extends EntryViewModel> void addEntry( M entryViewModel, ViewSupplier<M> viewSupplier) {
		Objects.requireNonNull(entryViewModel);
		EntryPanel entryPanel = _createEntryPanel(null, entryViewModel, viewSupplier, this._internal.getComponents().length);
		this._internal.add(entryPanel);
	}

	/**
	 * 	The {@link JScrollPanels} does not store components statically in the UI tree.
	 * 	Instead, it is a hybrid of the traditional static approach
	 * 	and a renderer based approach (as in the {@link JList}).
	 * 	The view supplier lambda passed to this method is responsible for continuously supplying a UI
	 * 	which fits a certain context (which defines if the entry is selected or not among other things).
	 *
	 * @param constraints The constraints which ought to be applied to the entry.
	 * @param entryViewModel The entry model which ought to be added.
	 * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
	 */
	public <M extends EntryViewModel> void addEntry( String constraints, M entryViewModel, ViewSupplier<M> viewSupplier) {
		Objects.requireNonNull(entryViewModel);
		EntryPanel entryPanel = _createEntryPanel(constraints, entryViewModel, viewSupplier, this._internal.getComponents().length);
		this._internal.add(entryPanel);
		this.validate();
	}

	/**
	 *  Adds multiple entries at once to this {@link JScrollPanels}.
	 * @param constraints The constraints which ought to be applied to the entry.
	 * @param entryViewModels A list of entry models which ought to be added.
	 * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
	 */
	public <M extends EntryViewModel> void addAllEntries( String constraints, List<M> entryViewModels, ViewSupplier<M> viewSupplier) {
		Objects.requireNonNull(entryViewModels);
		List<EntryPanel> entryPanels = IntStream.range(0, entryViewModels.size())
				.mapToObj(
						i -> _createEntryPanel(
								constraints,
								entryViewModels.get(i),
								viewSupplier,
								this._internal.getComponents().length + i
						)
				)
				.collect(Collectors.toList());

		entryPanels.forEach(this._internal::add);
		this.validate();
	}

	/**
	 * 	Use this to remove all entries.
	 */
	public void removeAllEntries() {
		this._internal.removeAll();
		this.validate();
	}

	/**
	 * 	Use this to remove an entry at a certain index.
	 * @param index The index of the entry which ought to be removed.
	 */
	public void removeEntryAt( int index ) {
		this._internal.remove(index);
		this.validate();
	}

	/**
	 * 	Use this to add an entry at a certain index.
	 *
	 *  @param index The index at which the entry ought to be added.
	 *  @param attr The constraints which ought to be applied to the entry, may be null.
	 *  @param entryViewModel The entry view model which ought to be added.
	 *  @param viewSupplier The supplier which is used to create the view for the given entry view model.
	 *  @param <M> The type of the entry view model.
	 */
	public <M extends EntryViewModel> void addEntryAt( int index, String attr, M entryViewModel, ViewSupplier<M> viewSupplier) {
		Objects.requireNonNull(entryViewModel);
		EntryPanel entryPanel = _createEntryPanel(attr, entryViewModel, viewSupplier, index);
		this._internal.add(entryPanel, index);
		this.validate();
	}

	/**
	 * 	Use this to replace an entry at a certain index. <br>
	 * 	Note: This method will replace an existing entry at the given index.
	 *
	 *  @param index The index at which the entry ought to be placed.
	 *  @param attr The constraints which ought to be applied to the entry, may be null.
	 *  @param entryViewModel The entry view model which ought to be added.
	 *  @param viewSupplier The supplier which is used to create the view for the given entry view model.
	 */
	public <M extends EntryViewModel> void setEntryAt( int index, String attr, M entryViewModel, ViewSupplier<M> viewSupplier) {
		Objects.requireNonNull(entryViewModel);
		EntryPanel entryPanel = _createEntryPanel(attr, entryViewModel, viewSupplier, index);
		// We first remove the old entry panel and then add the new one.
		// This is necessary because the layout manager does not allow to replace
		// a component at a certain index.
		this._internal.remove(index);
		// We have to re-add the entry panel at the same index
		// because the layout manager will otherwise add it at the end.
		this._internal.add(entryPanel, index);
		this.validate();
	}

	/**
	 * 	Use this to find an entry component.
	 *
	 * @param type The component type which ought to be found.
	 * @param condition A predicate which ought to return true for this method to yield the found entry panel.
	 * @param <T> The component type which ought to be found.
	 * @return The found entry panel matching the provided type class and predicate lambda.
	 */
	private <T extends JComponent> EntryPanel get(
			Class<T> type, Predicate<EntryPanel> condition
	) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(condition);
		return
			Arrays.stream(this._internal.getComponents())
					.filter( c -> c != null )
					.map( c -> (EntryPanel) c )
					.filter( c -> type.isAssignableFrom(c.getLastState().getClass()) )
					.filter( c -> condition.test(c) )
					.findFirst()
					.orElse(null);
	}

	/**
	 * 	Use this to find an entry component.
	 *
	 * @param type The component type which ought to be found.
	 * @param <T> The component type which ought to be found.
	 * @return The found entry panel matching the provided type class and predicate lambda.
	 */
	public <T extends JComponent> Optional<T> getSelected( Class<T> type ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(type);
		return (Optional<T>) Optional.ofNullable(get(type, EntryPanel::isEntrySelected)).map(e -> e.getLastState() );
	}

	/**
	 * 	Use this to iterate over all panel list entries.
	 *
	 * @param action The action which ought to be applied to all {@link JScrollPanels} entries.
	 */
	public void forEachEntry( Consumer<EntryPanel> action ) {
		Objects.requireNonNull(action);
		Arrays.stream(this._internal.getComponents())
				.map( c -> (EntryPanel) c )
				.forEach(action);
	}

	/**
	 * @param type The type of the entry which ought to be iterated over.
	 * @param action The action which ought to be applied to all {@link JScrollPanels} entries of the given type.
	 * @param <T> The entry value type parameter.
	 */
	public <T extends JComponent> void forEachEntry(Class<T> type, Consumer<EntryPanel> action) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(action);
		Arrays.stream(this._internal.getComponents())
				.map( c -> (EntryPanel) c )
				.filter( e -> type.isAssignableFrom(e.getLastState().getClass()) )
				.forEach(action);
	}

	/**
	 *  Use this to set entries as selected based on a condition lambda (predicate).
	 * @param type The type of the entry which ought to be selected.
	 * @param condition The condition which ought to be met for the entry to be selected.
	 * @param <T> The type of the entry which ought to be selected.
	 */
	public <T extends JComponent> void setSelectedFor(Class<T> type, Predicate<T> condition) {
		forEachEntry( e -> e.setEntrySelected(false) );
		forEachEntry(type, e -> {
			if ( condition.test((T) e.getLastState()) ) e.setEntrySelected(true);
		});
	}

	private <M extends EntryViewModel> EntryPanel _createEntryPanel(
		String constraints,
		M entryProvider,
		ViewSupplier<M> viewSupplier,
		int index
	) {
		Objects.requireNonNull(entryProvider);
		return new EntryPanel(
						()-> _entriesIn(_internal.getComponents()),
						index,
						entryProvider,
						viewSupplier,
						constraints
					);
	}

	/**
	 * 	This panel holds the list panels.
	 * 	It wraps {@link EntryPanel} instances which themselves
	 * 	wrap user provided {@link JPanel} implementations rendering the actual content.
	 */
	private static class InternalPanel extends JPanel implements Scrollable
	{
		private final int W, H;
		private final UI.Align type;
		private final int hGap, vGap;
		private final Dimension size;


		private InternalPanel(
				List<EntryPanel> entryPanels,
				Dimension shape,
				UI.Align type
		) {
			shape = ( shape == null ? new Dimension(120, 100) : shape );
			int n = entryPanels.size() / 2;
			W = (int) shape.getWidth(); // 120
			H = (int) shape.getHeight(); // 100
			this.type = type;
			LayoutManager layout;
			if ( type == UI.Align.HORIZONTAL ) {
				FlowLayout flow = new FlowLayout();
				hGap = flow.getHgap();
				vGap = flow.getVgap();
				layout = flow;
			} else {
				BoxLayout box = new BoxLayout(this, BoxLayout.Y_AXIS);
				hGap = 5;
				vGap = 5;
				layout = box;
			}
			setLayout(layout);
			for ( EntryPanel c : entryPanels ) this.add(c);

			if ( type == UI.Align.HORIZONTAL )
				size = new Dimension(n * W + (n + 1) * hGap, H + 2 * vGap);
			else
				size = new Dimension(W + 2 * hGap, n * H + (n + 1) * vGap);

			for ( EntryPanel c : entryPanels ) {
				c.addMouseListener(
						new MouseAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								entryPanels.forEach( entry -> entry.setEntrySelected(false) );
								c.setEntrySelected(true);
							}
						}
				);
			}
		}

		@Override public Dimension getPreferredScrollableViewportSize() { return size; }

		@Override
		public Dimension getPreferredSize() {
			if ( type == UI.Align.VERTICAL )
				return new Dimension(
							Math.max(W, getParent().getWidth()),
							(int) super.getPreferredSize().getHeight()
						);
			else
				return new Dimension(
							(int) super.getPreferredSize().getWidth(),
							Math.max(H, getParent().getHeight())
						);
		}

		@Override
		public int getScrollableUnitIncrement(
				Rectangle visibleRect, int orientation, int direction
		) {
			return _incrementFrom(orientation);
		}

		@Override
		public int getScrollableBlockIncrement(
				Rectangle visibleRect, int orientation, int direction
		) {
			return _incrementFrom(orientation) / 2;
		}

		private int _incrementFrom(int orientation) { return orientation == JScrollBar.HORIZONTAL ? W + hGap : H + vGap; }

		@Override public boolean getScrollableTracksViewportWidth()  { return false; }
		@Override public boolean getScrollableTracksViewportHeight() { return false; }
	}

	/**
	 * 	Filters the entry panels from the provided components array.
	 */
	private static List<EntryPanel> _entriesIn(Component[] components) {
		return Arrays.stream(components)
				.filter( c -> c instanceof EntryPanel )
				.map( c -> (EntryPanel) c )
				.collect(Collectors.toList());
	}

	/**
	 * 	Instances of this are entries of this {@link JScrollPanels}.
	 * 	{@link EntryPanel}s themselves are wrappers for whatever content should be displayed
	 * 	by the UI provided by {@link ViewSupplier}s wrapped by {@link EntryPanel}s.
	 * 	The {@link ViewSupplier} turn whatever kind of view model the user provides into
	 * 	a {@link JComponent} which is then wrapped by an {@link EntryPanel}.
	 */
	public static class EntryPanel extends JPanel
	{
		private static final Color HIGHLIGHT = Color.GREEN;
		private static final Color LOW_LIGHT = Color.WHITE;
		private final Function<Boolean, JComponent> provider;
		private final EntryViewModel viewable;
		private boolean isSelected;
		private JComponent lastState;


		private <M extends EntryViewModel> EntryPanel(
				Supplier<List<EntryPanel>> components,
				int position,
				M provider,
				ViewSupplier<M> viewSupplier,
				String constraints
		) {
			Objects.requireNonNull(components);
			Objects.requireNonNull(provider);
			// We make the entry panel fit the outer (public) scroll panel.
			this.setLayout(new MigLayout("fill, insets 0", "[grow]"));
			this.viewable = provider;
			this.provider = isSelected -> {
								provider.position().act(position);
								provider.isSelected().act(isSelected);
								return (JComponent) viewSupplier.createViewFor(provider).getComponent();
							};
			this.lastState = this.provider.apply(false);
			this.add(lastState, constraints != null ? constraints : "grow" );
			viewable.isSelected().onSet( it -> _selectThis(components) );
			if ( viewable.isSelected().is(true) )
				_selectThis(components);
			viewable.position().act(position);
		}

		private void _selectThis(
				Supplier<List<EntryPanel>> components
		) {
			SwingUtilities.invokeLater( () -> {
						components.get()
								.stream()
								.forEach( entry -> entry.setEntrySelected(false) );
						setEntrySelected(true);
					}
			);
		}

		public JComponent getLastState() { return this.lastState; }

		public boolean isEntrySelected() { return this.isSelected; }

		public void setEntrySelected(Boolean isHighlighted) {
			if ( this.isSelected != isHighlighted ) {
				this.remove(lastState);
				try {
					this.lastState = this.provider.apply(isHighlighted);
				} catch (Exception e) {
					e.printStackTrace();
				}
				this.setBackground( isHighlighted ? HIGHLIGHT : LOW_LIGHT );
				this.add(lastState, "grow");
				this.validate();
				this.viewable.isSelected().act(isHighlighted);
			}
			this.isSelected = isHighlighted;
		}

		@Override public String toString() { return "EntryPanel[" + this.lastState + "]"; }
	}

}
package swingtree;

import net.miginfocom.swing.MigLayout;
import swingtree.api.mvvm.ViewableEntry;

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
 * 	The {@link JScrollPanels} class is similar to the {@link JList} class
 * 	(which uses a {@link ListCellRenderer} to continuously fetch a live UI component
 * 	for dynamically rendering list entries).
 * 	Contrary to the {@link JList} however, entries of the {@link JScrollPanels}
 * 	will be able to receive user events like for example button clicks etc... <br>
 * 	Instances of this class manage {@link ViewableEntry} implementations which
 * 	will be called dynamically to update the UI.
 */
public class JScrollPanels extends JScrollPane
{
	private final InternalPanel internal; // Wrapper for the actual UI components

	private JScrollPanels(InternalPanel listWrapper) {
		super(listWrapper);
		this.internal = listWrapper;
	}

	/**
	 * @return The number of entries which are currently managed by this {@link JScrollPanels}.
	 */
	public int getNumberOfEntries() { return internal.getComponents().length; }

	/**
	 * 	The {@link JScrollPanels} does not store components statically in the UI tree.
	 * 	Instead, it is a hybrid of the traditional static approach
	 * 	and a renderer based approach (as in the {@link JList}).
	 * 	The lambda passed to this method is responsible for continuously supplying a UI
	 * 	which fits a certain context (which defines if the entry is selected or not among other things).
	 *
	 * @param entryViewModel A provider lambda which ought to turn a context object into a fitting UI.
	 */
	public void addEntry( ViewableEntry entryViewModel ) { addEntry( null, entryViewModel ); }

	/**
	 * 	The {@link JScrollPanels} does not store components statically in the UI tree.
	 * 	Instead, it is a hybrid of the traditional static approach
	 * 	and a renderer based approach (as in the {@link JList}).
	 * 	The lambda passed to this method is responsible for continuously supplying a UI
	 * 	which fits a certain context (which defines if the entry is selected or not among other things).
	 *
	 * @param constraints The constraints which ought to be applied to the entry.
	 * @param entryViewModel A provider lambda which ought to turn a context object into a fitting UI.
	 */
	public void addEntry( String constraints, ViewableEntry entryViewModel ) {
		Objects.requireNonNull(entryViewModel);
		EntryPanel entryPanel = _createEntryPanel(constraints, entryViewModel, this.internal.getComponents().length);
		this.internal.add(entryPanel);
		this.validate();
	}

	/**
	 *  Adds multiple entries at once to this {@link JScrollPanels}.
	 * @param constraints The constraints which ought to be applied to the entry.
	 * @param entryViewModels A list of entry providers which ought to be added.
	 */
	public void addAllEntries( String constraints, List<ViewableEntry> entryViewModels ) {
		Objects.requireNonNull(entryViewModels);
		List<EntryPanel> entryPanels = IntStream.range(0, entryViewModels.size())
													.mapToObj(
														i -> _createEntryPanel(
																	constraints,
																	entryViewModels.get(i),
																	this.internal.getComponents().length + i
																)
													)
													.collect(Collectors.toList());

		entryPanels.forEach(this.internal::add);
		this.validate();
	}

	/**
	 * 	Use this to remove all entries.
	 */
	public void removeAllEntries() {
		this.internal.removeAll();
		this.validate();
	}

	/**
	 * 	Use this to remove an entry at a certain index.
	 * @param index The index of the entry which ought to be removed.
	 */
	public void removeEntryAt( int index ) {
		this.internal.remove(index);
		this.validate();
	}

	/**
	 * 	Use this to add an entry at a certain index.
	 * 		Note: This method will replace an existing entry at the given index.
	 *
	 *  @param index The index at which the entry ought to be added.
	 *  @param attr The constraints which ought to be applied to the entry.
	 *  @param entryViewModel The entry provider which ought to be added.
	 */
	public void addEntryAt( int index, String attr, ViewableEntry entryViewModel ) {
		Objects.requireNonNull(entryViewModel);
		EntryPanel entryPanel = _createEntryPanel(attr, entryViewModel, index);
		this.internal.add(entryPanel, index);
		this.validate();
	}

	/**
	 * 	Use this to replace an entry at a certain index.
	 * 		Note: This method will replace an existing entry at the given index.
	 *
	 *  @param index The index at which the entry ought to be added.
	 *  @param attr The constraints which ought to be applied to the entry.
	 *  @param entryViewModel The entry provider which ought to be added.
	 */
	public void setEntryAt( int index, String attr, ViewableEntry entryViewModel ) {
		Objects.requireNonNull(entryViewModel);
		EntryPanel entryPanel = _createEntryPanel(attr, entryViewModel, index);
		// We first remove the old entry panel and then add the new one.
		// This is necessary because the layout manager does not allow to replace
		// a component at a certain index.
		this.internal.remove(index);
		// We have to re-add the entry panel at the same index
		// because the layout manager will otherwise add it at the end.
		this.internal.add(entryPanel, index);
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
			Arrays.stream(this.internal.getComponents())
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
		return (Optional<T>) Optional.ofNullable(get(type, EntryPanel::isEntrySelected)).map(e -> e.getLastState() );
	}

	/**
	 * 	Use this to iterate over all panel list entries.
	 *
	 * @param action The action which ought to be applied to all {@link JScrollPanels} entries.
	 */
	public void forEachEntry( Consumer<EntryPanel> action ) {
		Arrays.stream(this.internal.getComponents())
				.map( c -> (EntryPanel) c )
				.forEach(action);
	}

	/**
	 * @param <T> The entry value type parameter.
	 */
	public <T extends JComponent> void forEachEntry(Class<T> type, Consumer<EntryPanel> action) {
		Arrays.stream(this.internal.getComponents())
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

	private EntryPanel _createEntryPanel(
		String constraints,
		ViewableEntry entryProvider,
		int index
	) {
		Objects.requireNonNull(entryProvider);
		return new EntryPanel(
				()-> entriesIn(internal.getComponents()),
				index,
				entryProvider,
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

		@Override
		public Dimension getPreferredScrollableViewportSize() { return size; }

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
			return getIncrement(orientation);
		}

		@Override
		public int getScrollableBlockIncrement(
				Rectangle visibleRect, int orientation, int direction
		) {
			return getIncrement(orientation) / 2;
		}

		private int getIncrement(int orientation) {
			return orientation == JScrollBar.HORIZONTAL ? W + hGap : H + vGap;
		}

		@Override public boolean getScrollableTracksViewportWidth()  { return false; }
		@Override public boolean getScrollableTracksViewportHeight() { return false; }
	}

	public static JScrollPanels of(
			UI.Align align, Dimension size
	) {
		return JScrollPanels.construct(align, size, Collections.emptyList(), null);
	}

	private static JScrollPanels construct(
			UI.Align align,
			Dimension shape,
			List<ViewableEntry> components,
			String constraints
	) {
		UI.Align type = align;
		InternalPanel[] forwardReference = {null};
		List<EntryPanel> entries =
				IntStream.range(0,components.size())
						.mapToObj( i ->
							new EntryPanel(
									()-> entriesIn(forwardReference[0].getComponents()),
									i,
									components.get(i),
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

	/**
	 * 	Filters the entry panels from the provided components array.
	 */
	private static List<EntryPanel> entriesIn(Component[] components) {
		return Arrays.stream(components)
				.filter( c -> c instanceof EntryPanel )
				.map( c -> (EntryPanel) c )
				.collect(Collectors.toList());
	}

	/**
	 * 	Instances of this are entries of this {@link JScrollPanels}.
	 * 	{@link EntryPanel}s themselves are wrappers for whatever content should be displayed
	 * 	by the UI provided by {@link ViewableEntry}s wrapped by {@link EntryPanel}s.
	 */
	public static class EntryPanel extends JPanel
	{
		private static final Color HIGHLIGHT = Color.GREEN;
		private static final Color LOW_LIGHT = Color.WHITE;
		private final Function<Boolean, JComponent> provider;
		private final ViewableEntry viewable;
		private boolean isSelected;
		private JComponent lastState;


		private EntryPanel(
				Supplier<List<EntryPanel>> components,
				int position,
				ViewableEntry provider,
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
				return (JComponent) provider.createView(javax.swing.JComponent.class);
			};
			this.lastState = this.provider.apply(false);
			this.add(lastState, constraints != null ? constraints : "grow" );
			viewable.isSelected().onSet( it -> {selectThis(components);});
			if ( viewable.isSelected().is(true) )
				selectThis(components);
			viewable.position().act(position);
		}

		private void selectThis(
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
				this.lastState = this.provider.apply(isHighlighted);
				this.setBackground( isHighlighted ? HIGHLIGHT : LOW_LIGHT );
				this.add(lastState, "grow");
				this.validate();
				this.viewable.isSelected().act(isHighlighted);
			}
			this.isSelected = isHighlighted;
		}

		@Override public String toString() {
			return "EntryPanel[" + this.lastState + "]";
		}
	}

}
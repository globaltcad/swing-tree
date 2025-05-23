package swingtree.components;

import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.From;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.api.mvvm.EntryViewModel;
import swingtree.api.mvvm.ViewSupplier;
import swingtree.layout.AddConstraint;
import swingtree.layout.ResponsiveGridFlowLayout;
import swingtree.style.ComponentExtension;

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
 *  The {@link JScrollPanels} class is a container for a list of scrollable UI components
 *  representing view models or simple data models which are dynamically turned into
 *  views by a {@link ViewSupplier}.
 *  This class exists to compensate for the deficits of the {@link JList} and {@link JTable} components,
 *  whose entries are not able to receive user events like for example mouse events, button clicks etc...
 *  <br>
 *  A {@link JScrollPanels} instance can arrange its entries in a vertical or horizontal manner
 *  based on the {@link UI.Align} parameter.
 *  <br><br>
 *  Instances of this store view model implementations in a view model property list
 *  so that they can dynamically be turned into views by a {@link ViewSupplier} lambda
 *  when the list changes its state. <br>
 *  Here a simple example demonstrating the usage of the {@link JScrollPanels} class
 *  through the Swing-Tree API:
 *  <pre>{@code
 *    UI.scrollPanels()
 *    .addAll(viewModel.entries(), entry ->
 *        UI.panel().add(UI.button("Click me! :)"))
 *    )
 *  }</pre>
 *  ...where {@code entries()} is a method returning a {@link sprouts.Vars} instance
 *  which contains a list of your sub-view models.
 *  The second parameter of the {@link swingtree.UIForScrollPanels#addAll(sprouts.Vals, ViewSupplier)} method is a lambda
 *  which takes a single view model from the list of view models and turns it into a view.
 */
public class JScrollPanels extends UI.ScrollPane
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JScrollPanels.class);

    /**
     * Constructs a new {@link JScrollPanels} instance with the provided alignment and size.
     * @param align The alignment of the entries inside this {@link JScrollPanels} instance.
     *              The alignment can be either {@link UI.Align#HORIZONTAL} or {@link UI.Align#VERTICAL}.
     * @param size The size of the entries in this {@link JScrollPanels} instance.
     * @return A new {@link JScrollPanels} instance.
     */
    public static JScrollPanels of(
        UI.Align align, @Nullable Dimension size
    ) {
        Objects.requireNonNull(align);
        return _construct(align, size, Collections.emptyList(), null, m -> UI.panel());
    }

    private static JScrollPanels _construct(
        UI.Align align,
        @Nullable Dimension shape,
        List<EntryViewModel> models,
        @Nullable AddConstraint constraints,
        ViewSupplier<EntryViewModel> viewSupplier
    ) {
        UI.Align type = align;
        @Nullable InternalPanel[] forwardReference = {null};
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
     *  Allows you to get the number of entries which are currently managed by this {@link JScrollPanels}.
     *  The number of entries is the number of view models which are currently managed by this {@link JScrollPanels}.
     *
     * @return The number of entries which are currently managed by this {@link JScrollPanels}.
     */
    public int getNumberOfEntries() { return _internal.getComponents().length; }

    /**
     *  Exposes the content panel that contains the actual entry components.
     * @return The container of the entry views.
     */
    public JComponent getContentPanel() {
        return _internal;
    }

    /**
     *  The {@link JScrollPanels} does not store components statically in the UI tree.
     *  Instead, it is a hybrid of the traditional static approach
     *  and a renderer based approach (as in the {@link JList}).
     *  The lambda passed to this method is responsible for continuously supplying a UI
     *  which fits a certain context (which defines if the entry is selected or not among other things).
     *
     * @param entryViewModel A view model which ought to be added.
     * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
     * @param <M> The type of the entry view model.
     */
    public <M extends EntryViewModel> void addEntry( M entryViewModel, ViewSupplier<M> viewSupplier) {
        Objects.requireNonNull(entryViewModel);
        addEntry(null, entryViewModel, viewSupplier);
    }

    public <M extends EntryViewModel> void addEntryAt( int index, M entryViewModel, ViewSupplier<M> viewSupplier) {
        Objects.requireNonNull(entryViewModel);
        EntryPanel entryPanel = _createEntryPanel(null, entryViewModel, viewSupplier, index);
        _internal.add(entryPanel, index);
    }

    /**
     *  The {@link JScrollPanels} does not store components statically in the UI tree.
     *  Instead, it is a hybrid of the traditional static approach
     *  and a renderer based approach (as in the {@link JList}).
     *  The view supplier lambda passed to this method is responsible for continuously supplying a UI
     *  which fits a certain context (which defines if the entry is selected or not among other things).
     *
     * @param constraints The constraints which ought to be applied to the entry.
     * @param entryViewModel The entry model which ought to be added.
     * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
     * @param <M> The type of the entry view model.
     */
    public <M extends EntryViewModel> void addEntry( @Nullable AddConstraint constraints, M entryViewModel, ViewSupplier<M> viewSupplier ) {
        Objects.requireNonNull(entryViewModel);
        EntryPanel entryPanel = _createEntryPanel(constraints, entryViewModel, viewSupplier, _internal.getComponents().length);
        _internal.add(entryPanel);
        this.validate();
    }

    /**
     *  Adds multiple entries at once to this {@link JScrollPanels}.
     * @param constraints The constraints which ought to be applied to the entry.
     * @param entryViewModels A list of entry models which ought to be added.
     * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
     * @param <M> The type of the entry view model.
     */
    public <M extends EntryViewModel> void addAllEntries( @Nullable AddConstraint constraints, Iterable<M> entryViewModels, ViewSupplier<M> viewSupplier ) {
        Objects.requireNonNull(entryViewModels);
        addAllEntriesAt(_internal.getComponents().length, constraints, entryViewModels, viewSupplier);
    }

    /**
     *  Adds multiple entries at once to this {@link JScrollPanels} at a certain index.
     * @param index The index at which the entries ought to be added.
     * @param constraints The constraints which ought to be applied to the entry.
     * @param entryViewModels A list of entry models which ought to be added.
     * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
     * @param <M> The type of the entry view model.
     */
    public <M extends EntryViewModel> void addAllEntriesAt( int index, @Nullable AddConstraint constraints, Iterable<M> entryViewModels, ViewSupplier<M> viewSupplier ) {
        Objects.requireNonNull(entryViewModels);
        List<EntryPanel> entryPanels = new ArrayList<>();
        int i = 0;
        for ( M model : entryViewModels ) {
            entryPanels.add(
                    _createEntryPanel(
                            constraints,
                            model,
                            viewSupplier,
                            index + i
                    )
            );
            i++;
        }
        for ( int j = 0; j < entryPanels.size(); j++ ) {
            _internal.add(entryPanels.get(j), index + j);
        }
        this.validate();
    }

    /**
     *   Use this to set all entries at a certain index
     *   to effectively replace the old entries with the new ones.
     * @param index The index at which the entries ought to be set.
     * @param constraints The constraints which ought to be applied to the entry.
     * @param entryViewModels A list of entry models which ought to be added.
     * @param viewSupplier A provider lambda which ought to turn a context object into a fitting UI.
     * @param <M> The type of the entry view model.
     */
    public <M extends EntryViewModel> void setAllEntriesAt( int index, @Nullable AddConstraint constraints, Iterable<M> entryViewModels, ViewSupplier<M> viewSupplier ) {
        Objects.requireNonNull(entryViewModels);
        boolean somethingChanged = false;
        int i = 0;
        for ( M model : entryViewModels ) {
            int localIndex = index + i;
            i++;
            EntryPanel entry = _createEntryPanel(
                                    constraints,
                                    model,
                                    viewSupplier,
                                    localIndex
                                );
            try {
                Component existing = _internal.getComponent(localIndex);
                if ( existing instanceof EntryPanel ) {
                    EntryPanel existingEntry = (EntryPanel) existing;
                    if ( existingEntry.getComponentCount() == 0 ) {
                        log.error("The entry panel '{}' about to be replaced by '{}' in '{}' is missing its view.", existingEntry, entry, JScrollPanels.class, new Throwable());
                    } else if ( entry.getComponentCount() == 0 ) {
                        log.error("The entry panel '{}' that should replace panel '{}' in '{}' is missing its view.", entry, existingEntry, JScrollPanels.class, new Throwable());
                    } else {
                        Component existingView = existingEntry.getComponent(0);
                        Component newView = entry.getComponent(0);
                        if ( existingView == newView )
                            continue; // The view is already there
                    }
                } else {
                    log.error("Encountered illegal type of entry panel in {}.", JScrollPanels.class, new Throwable());
                }
                _internal.remove(localIndex);
                _internal.add(entry, localIndex);
                somethingChanged = true;
            } catch (Exception e) {
                log.error("Encountered an exception while trying to update the component at index '{}'.", localIndex, e);
            }
        }
        if ( somethingChanged ) {
            this.validate();
        }
    }

    /**
     *  Use this to remove all entries.
     */
    public void removeAllEntries() {
        _internal.removeAll();
        this.validate();
    }

    /**
     *  Use this to remove an entry at a certain index.
     * @param index The index of the entry which ought to be removed.
     */
    public void removeEntryAt( int index ) {
        _internal.remove(index);
        this.validate();
    }

    /**
     *  Use this to remove a certain number of entries starting at a certain index.
     * @param index The index at which the entries ought to be removed.
     * @param count The number of entries which ought to be removed.
     */
    public void removeEntriesAt( int index, int count ) {
        IntStream.range(0, count).forEach( i -> _internal.remove(index) );
        this.validate();
    }

    /**
     *  Use this to add an entry at a certain index.
     *
     *  @param index The index at which the entry ought to be added.
     *  @param attr The constraints which ought to be applied to the entry, may be null.
     *  @param entryViewModel The entry view model which ought to be added.
     *  @param viewSupplier The supplier which is used to create the view for the given entry view model.
     *  @param <M> The type of the entry view model.
     */
    public <M extends EntryViewModel> void addEntryAt( int index, @Nullable AddConstraint attr, M entryViewModel, ViewSupplier<M> viewSupplier ) {
        Objects.requireNonNull(entryViewModel);
        EntryPanel entryPanel = _createEntryPanel(attr, entryViewModel, viewSupplier, index);
        _internal.add(entryPanel, index);
        this.validate();
    }

    /**
     *     Use this to replace an entry at a certain index. <br>
     *     Note: This method will replace an existing entry at the given index.
     *
     *  @param index The index at which the entry ought to be placed.
     *  @param attr The constraints which ought to be applied to the entry, may be null.
     *  @param entryViewModel The entry view model which ought to be added.
     *  @param viewSupplier The supplier which is used to create the view for the given entry view model.
     *  @param <M> The type of the entry view model.
     */
    public <M extends EntryViewModel> void setEntryAt( int index, @Nullable AddConstraint attr, M entryViewModel, ViewSupplier<M> viewSupplier ) {
        Objects.requireNonNull(entryViewModel);
        EntryPanel newEntryPanel = _createEntryPanel(attr, entryViewModel, viewSupplier, index);
        Component existing = _internal.getComponent(index);
        if ( existing instanceof EntryPanel ) {
            EntryPanel existingEntry = (EntryPanel) existing;
            if ( existingEntry.getComponentCount() == 0 ) {
                log.error("The entry panel '{}' about to be replaced by '{}' in '{}' is missing its view.", existingEntry, newEntryPanel, JScrollPanels.class, new Throwable());
            } else if ( newEntryPanel.getComponentCount() == 0 ) {
                log.error("The entry panel '{}' that should replace panel '{}' in '{}' is missing its view.", newEntryPanel, existingEntry, JScrollPanels.class, new Throwable());
            } else {
                Component existingView = ((EntryPanel) existing).getComponent(0);
                Component newView = newEntryPanel.getComponent(0);
                if ( existingView == newView )
                    return; // The view is already there
            }
        } else {
            log.error("Encountered illegal type of entry panel in {}.", JScrollPanels.class, new Throwable());
        }
        // We first remove the old entry panel and then add the new one.
        // This is necessary because the layout manager does not allow to replace
        // a component at a certain index.
        _internal.remove(index);
        // We have to re-add the entry panel at the same index
        // because the layout manager will otherwise add it at the end.
        _internal.add(newEntryPanel, index);
        this.validate();
    }

    /**
     *  Use this to find an entry component.
     *
     * @param type The component type which ought to be found.
     * @param condition A predicate which ought to return true for this method to yield the found entry panel.
     * @param <T> The component type which ought to be found.
     * @return The found entry panel matching the provided type class and predicate lambda.
     */
    private <T extends JComponent> @Nullable EntryPanel get(
            Class<T> type, Predicate<EntryPanel> condition
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(condition);
        return
            Arrays.stream(_internal.getComponents())
                    .filter(Objects::nonNull)
                    .map( c -> (EntryPanel) c )
                    .filter( c -> type.isAssignableFrom(c.getLastState().getClass()) )
                    .filter( c -> condition.test(c) )
                    .findFirst()
                    .orElse(null);
    }

    /**
     *  Use this to find an entry component.
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
     *  Use this to iterate over all panel list entries.
     *
     * @param action The action which ought to be applied to all {@link JScrollPanels} entries.
     */
    public void forEachEntry( Consumer<EntryPanel> action ) {
        Objects.requireNonNull(action);
        Arrays.stream(_internal.getComponents())
                .map( c -> (EntryPanel) c )
                .forEach(action);
    }

    /**
     *  Use this to iterate over all panel list entries of a certain type
     *  by supplying a type class and a consumer action.
     *  Neither of the two parameters may be null!
     *
     * @param type The type of the entry which ought to be iterated over.
     * @param action The action which ought to be applied to all {@link JScrollPanels} entries of the given type.
     * @param <T> The entry value type parameter.
     */
    public <T extends JComponent> void forEachEntry(Class<T> type, Consumer<EntryPanel> action) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(action);
        Arrays.stream(_internal.getComponents())
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
        @Nullable AddConstraint constraints,
        M viewModel,
        ViewSupplier<M> viewSupplier,
        int index
    ) {
        Function<Boolean, JComponent> indexedViewSupplier = EntryPanel.providerFrom(index, viewModel, viewSupplier);
        JComponent initialView = indexedViewSupplier.apply(false);
        // Let's see if we can avoid recreating an entry:
        int numberOfExistingComponents = _internal.getComponentCount();
        if ( numberOfExistingComponents > 0 && numberOfExistingComponents > index ) {
            Component found = _internal.getComponent(index);
            if ( !(found instanceof EntryPanel) ) {
                log.error("Encountered illegal child component '{}' in '{}'.", found, JScrollPanels.class, new Throwable());
            } else {
                EntryPanel existingEntry = (EntryPanel) found;
                if ( existingEntry.getComponentCount() == 0 ) {
                    log.error("Existing entry panel '{}' about to be replaced by new view '{}' in '{}' is missing its view.", existingEntry, initialView, JScrollPanels.class, new Throwable());
                } else {
                    Component existingView = existingEntry.getComponent(0);
                    if ( existingView == initialView )
                        return existingEntry; // The view is already there
                }
            }
        }
        Supplier<List<EntryPanel>> components = ()-> _entriesIn(_internal.getComponents());
        return new EntryPanel(
                        components,
                        index,
                        viewModel,
                        initialView,
                        indexedViewSupplier,
                        constraints
                    );
    }

    /**
     *  This panel holds the list panels.
     *  It wraps {@link EntryPanel} instances which themselves
     *  wrap user provided {@link JPanel} implementations rendering the actual content.
     */
    private static class InternalPanel extends JBox implements Scrollable
    {
        private final int _W, _H, _horizontalGap, _verticalGap;
        private final UI.Align _type;
        private final Dimension _size;


        private InternalPanel(
            List<EntryPanel> entryPanels,
            @Nullable Dimension shape,
            UI.Align type
        ) {
            shape = ( shape == null ? new Dimension(120, 100) : shape );
            int n = entryPanels.size() / 2;
            _W = (int) shape.getWidth(); // 120
            _H = (int) shape.getHeight(); // 100
            _type = type;
            LayoutManager layout;
            if ( type == UI.Align.HORIZONTAL ) {
                ResponsiveGridFlowLayout flow = new ResponsiveGridFlowLayout();
                _horizontalGap = flow.horizontalGapSize();
                _verticalGap = flow.verticalGapSize();
                layout = flow;
            } else {
                BoxLayout box = new BoxLayout(this, BoxLayout.Y_AXIS);
                _horizontalGap = 5;
                _verticalGap = 5;
                layout = box;
            }
            setLayout(layout);
            for ( EntryPanel c : entryPanels ) this.add(c);

            if ( type == UI.Align.HORIZONTAL )
                _size = new Dimension(n * _W + (n + 1) * _horizontalGap, _H + 2 * _verticalGap);
            else
                _size = new Dimension(_W + 2 * _horizontalGap, n * _H + (n + 1) * _verticalGap);

            for ( EntryPanel c : entryPanels )
                c.addMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            entryPanels.forEach( entry -> entry.setEntrySelected(false) );
                            c.setEntrySelected(true);
                        }
                    }
                );

            setOpaque(false);
            setBackground(Color.PINK);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return _size; }

        @Override
        public Dimension getPreferredSize() {
            if ( _type == UI.Align.VERTICAL )
                return new Dimension(
                            Math.max(_W, getParent().getWidth()),
                            (int) super.getPreferredSize().getHeight()
                        );
            else
                return new Dimension(
                            (int) super.getPreferredSize().getWidth(),
                            Math.max(_H, getParent().getHeight())
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

        private int _incrementFrom(int orientation) { return orientation == JScrollBar.HORIZONTAL ? _W + _horizontalGap : _H + _verticalGap; }

        @Override public boolean getScrollableTracksViewportWidth()  { return false; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /**
     *  Filters the entry panels from the provided components array.
     */
    private static List<EntryPanel> _entriesIn(Component[] components) {
        return Arrays.stream(components)
                .filter( c -> c instanceof EntryPanel )
                .map( c -> (EntryPanel) c )
                .collect(Collectors.toList());
    }

    /**
     *     Instances of this are entries of this {@link JScrollPanels}.
     *     {@link EntryPanel}s themselves are wrappers for whatever content should be displayed
     *     by the UI provided by {@link ViewSupplier}s wrapped by {@link EntryPanel}s.
     *     The {@link ViewSupplier} turn whatever kind of view model the user provides into
     *     a {@link JComponent} which is then wrapped by an {@link EntryPanel}.
     */
    public static class EntryPanel extends JBox
    {
        private static final Color HIGHLIGHT = Color.GREEN;
        private static final Color LOW_LIGHT = Color.WHITE;
        private final Function<Boolean, JComponent> _provider;
        private final EntryViewModel _viewable;
        private boolean _isSelected;
        private JComponent _lastState;


        static <M extends EntryViewModel> Function<Boolean, JComponent> providerFrom(
            int position,
            M viewModel,
            ViewSupplier<M> viewSupplier
        ) {
            return (Boolean isSelected) -> {
                viewModel.position().set(From.VIEW, position);
                viewModel.isSelected().set(From.VIEW, isSelected);
                UIForAnySwing<?,?> view = null;
                try {
                    view = viewSupplier.createViewFor(viewModel);
                } catch (Exception e) {
                    log.error("Failed to create view for entry " + viewModel, e);
                }
                if ( view == null )
                    view = UI.box(); // We return an empty box if the view is null.
                return view.get((Class) view.getType());
            };
        }

        private <M extends EntryViewModel> EntryPanel(
            Supplier<List<EntryPanel>> components,
            int position,
            M viewModel,
            ViewSupplier<M> viewSupplier,
            @Nullable AddConstraint constraints
        ) {
            this(
                Objects.requireNonNull(components),
                position,
                Objects.requireNonNull(viewModel),
                providerFrom(position, viewModel, viewSupplier),
                constraints
            );
        }

        private <M extends EntryViewModel> EntryPanel(
            Supplier<List<EntryPanel>> components,
            int position,
            M provider,
            Function<Boolean, JComponent> viewSupplier,
            @Nullable AddConstraint constraints
        ) {
            this(
                components,
                position,
                provider,
                viewSupplier.apply(false),
                viewSupplier,
                constraints
            );
        }

        private <M extends EntryViewModel> EntryPanel(
            Supplier<List<EntryPanel>> components,
            int position,
            M provider,
            JComponent lastState,
            Function<Boolean, JComponent> viewSupplier,
            @Nullable AddConstraint constraints
        ) {
            Objects.requireNonNull(components);
            Objects.requireNonNull(provider);
            // We make the entry panel fit the outer (public) scroll panel.
            this.setLayout(new MigLayout("fill, insets 0", "[grow]"));
            _viewable = provider;
            _provider = viewSupplier;
            _lastState = lastState;
            if ( _lastState.getParent() != null ) {
                Container parent = _lastState.getParent();
                log.error(
                            "View supplier in {} class supplied a view which is already tied to another parent {}.",
                            JScrollPanels.class.getSimpleName(), parent, new Throwable()
                        );
            }
            this.add(_lastState, constraints != null ? constraints.toConstraintForLayoutManager() : "grow" );

            ComponentExtension.from(this).storeBoundObservable(
                    _viewable.isSelected().view().onChange(From.VIEW_MODEL, it -> _selectThis(components) )
                );
            if ( _viewable.isSelected().is(true) )
                _selectThis(components);
            _viewable.position().set(From.VIEW, position);
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

        public JComponent getLastState() { return _lastState; }

        public boolean isEntrySelected() { return _isSelected; }

        public void setEntrySelected(Boolean isHighlighted) {
            if ( _isSelected != isHighlighted ) {
                this.remove(_lastState);
                try {
                    _lastState = _provider.apply(isHighlighted);
                } catch (Exception e) {
                    log.error("Failed to create view for entry: " + this, e);
                }
                this.setBackground( isHighlighted ? HIGHLIGHT : LOW_LIGHT );
                this.add(_lastState, "grow");
                this.validate();
                _viewable.isSelected().set(From.VIEW, isHighlighted);
            }
            _isSelected = isHighlighted;
        }

        @Override public String toString() { return "EntryPanel[" + _lastState + "]"; }
    }

}
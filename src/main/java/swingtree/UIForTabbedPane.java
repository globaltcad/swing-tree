package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.*;
import sprouts.impl.SequenceDiff;
import sprouts.impl.SequenceDiffOwner;
import swingtree.api.mvvm.TabSupplier;
import swingtree.style.ComponentExtension;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  A SwingTree builder node designed for configuring {@link JTabbedPane} instances.
 *
 * @param <P> The type of the {@link JTabbedPane} instance that this builder node configures.
 */
public final class UIForTabbedPane<P extends JTabbedPane> extends UIForAnySwing<UIForTabbedPane<P>, P>
{
    private static final Logger log = LoggerFactory.getLogger(UIForTabbedPane.class);

    private static final Tab TAB_ERROR = UI.tab("Error Tab");
    private static final Tab TAB_NULL = UI.tab("Empty Tab");

    private final BuilderState<P> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForTabbedPane( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state.withMutator(thisComponent -> {
            thisComponent.setModel(ExtraState.of(thisComponent));
        });
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }

    @Override
    protected UIForTabbedPane<P> _newBuilderWithState(BuilderState<P> newState ) {
        return new UIForTabbedPane<>(newState);
    }

    /**
     *  Adds an action to be performed when a mouse click is detected on a tab.
     *  The action will receive a {@link TabDelegate} instance which
     *  not only delegates the current tabbed pane and mouse event, but also
     *  tells the action which tab was clicked and whether the clicked tab is selected.
     *
     * @param onClick The action to be performed when a tab is clicked.
     * @return This builder node.
     * @throws NullPointerException if the given action is null.
     */
    public final UIForTabbedPane<P> onTabMouseClick( Action<TabDelegate> onClick ) {
        NullUtil.nullArgCheck(onClick, "onClick", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mouseClicked(MouseEvent e) {
                            int indexOfTab = thisComponent.indexAtLocation(e.getX(), e.getY());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _runInApp(() -> {
                                    try {
                                        onClick.accept(new TabDelegate(thisComponent, e));
                                    } catch (Exception ex) {
                                        log.error("Error while executing action on tab click!", ex);
                                    }
                                });
                        }
                    });
               })
               ._this();
    }

    /**
     *  Adds an action to be performed when a mouse press is detected on a tab.
     *  The action will receive a {@link TabDelegate} instance which
     *  not only delegates the current tabbed pane and mouse event, but also
     *  tells the action which tab was pressed and whether the pressed tab is selected.
     *
     * @param onPress The action to be performed when a tab is pressed.
     * @return This builder node.
     * @throws NullPointerException if the given action is null.
     */
    public final UIForTabbedPane<P> onTabMousePress( Action<TabDelegate> onPress ) {
        NullUtil.nullArgCheck(onPress, "onPress", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mousePressed(MouseEvent e) {
                            int indexOfTab = thisComponent.indexAtLocation(e.getX(), e.getY());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _runInApp(() -> {
                                    try {
                                        onPress.accept(new TabDelegate(thisComponent, e));
                                    } catch (Exception ex) {
                                        log.error("Error while executing action on tab press!", ex);
                                    }
                                });
                        }
                    });
               })
               ._this();
    }

    /**
     *  Adds an action to be performed when a mouse release is detected on a tab.
     *  The action will receive a {@link TabDelegate} instance which
     *  not only delegates the current tabbed pane and mouse event, but also
     *  tells the action which tab was released and whether the released tab is selected.
     *
     * @param onRelease The action to be performed when a tab is released.
     * @return This builder node.
     * @throws NullPointerException if the given action is null.
     */
    public final UIForTabbedPane<P> onTabMouseRelease( Action<TabDelegate> onRelease ) {
        NullUtil.nullArgCheck(onRelease, "onRelease", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mouseReleased(MouseEvent e) {
                            int indexOfTab = thisComponent.indexAtLocation(e.getX(), e.getY());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _runInApp(() -> {
                                    try {
                                        onRelease.accept(new TabDelegate(thisComponent, e));
                                    } catch (Exception ex) {
                                        log.error("Error while executing action on tab release!", ex);
                                    }
                                });
                        }
                    });
               })
               ._this();
    }

    /**
     *  Adds an action to be performed when a mouse enter is detected on a tab.
     *  The action will receive a {@link TabDelegate} instance which
     *  not only delegates the current tabbed pane and mouse event, but also
     *  tells the action which tab was entered and whether the entered tab is selected.
     *
     * @param onEnter The action to be performed when a tab is entered.
     * @return This builder node.
     * @throws NullPointerException if the given action is null.
     */
    public final UIForTabbedPane<P> onTabMouseEnter( Action<TabDelegate> onEnter ) {
        NullUtil.nullArgCheck(onEnter, "onEnter", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) {
                            int indexOfTab = thisComponent.indexAtLocation(e.getX(), e.getY());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _runInApp(() -> {
                                    try {
                                        onEnter.accept(new TabDelegate(thisComponent, e));
                                    } catch (Exception ex) {
                                        log.error("Error while executing action on tab enter!", ex);
                                    }
                                });
                        }
                    });
               })
               ._this();
    }

    /**
     *  Adds an action to be performed when a mouse exit is detected on a tab.
     *  The action will receive a {@link TabDelegate} instance which
     *  not only delegates the current tabbed pane and mouse event, but also
     *  tells the action which tab was exited and whether the exited tab is selected.
     *
     * @param onExit The action to be performed when a tab is exited.
     * @return This builder node.
     * @throws NullPointerException if the given action is null.
     */
    public final UIForTabbedPane<P> onTabMouseExit( Action<TabDelegate> onExit ) {
        NullUtil.nullArgCheck(onExit, "onExit", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mouseExited(MouseEvent e) {
                            int indexOfTab = thisComponent.indexAtLocation(e.getX(), e.getY());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _runInApp(() -> {
                                    try {
                                        onExit.accept(new TabDelegate(thisComponent, e));
                                    } catch (Exception ex) {
                                        log.error("Error while executing action on tab exit!", ex);
                                    }
                                });
                        }
                    });
               })
               ._this();
    }

    /**
     *  Sets the selected tab based on the given index.
     * @param index The index of the tab to select.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withSelectedIndex( int index ) {
        return _with( thisComponent -> {
                   thisComponent.setSelectedIndex(index);
               })
               ._this();
    }

    /**
     *  Dynamically sets the selected tab based on the given index property.
     *  So when the index property changes, the selected tab will change accordingly.
     * @param index The index property of the tab to select.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withSelectedIndex( Val<Integer> index ) {
        NullUtil.nullArgCheck( index, "index", Val.class );
        NullUtil.nullPropertyCheck( index, "index", "Null is not a valid state for modelling a selected index." );
        return _withOnShow( index, (thisComponent,i) -> {
                    thisComponent.setSelectedIndex(i);
               })
                ._with( thisComponent -> {
                    thisComponent.setSelectedIndex(index.get());
                })
               ._this();
    }

    /**
     *  Binds the given index property to the selection index of the tabbed pane,
     *  which means that when the index property changes, the selected tab will change accordingly
     *  and when the user selects a different tab, the index property will be updated accordingly.
     * @param index The index property of the tab to select.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withSelectedIndex( Var<Integer> index ) {
        NullUtil.nullArgCheck( index, "index", Var.class );
        NullUtil.nullPropertyCheck( index, "index", "Null is not a valid state for modelling a selected index." );
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of(thisComponent);
                    if ( state.selectedTabIndex != null && state.selectedTabIndex != index )
                        log.warn(
                            "Trying to bind a new property '"+index+"' " +
                            "to the index of tabbed pane '"+thisComponent+"' " +
                            "even though the previously specified property '"+state.selectedTabIndex+"' is " +
                            "already bound to it. " +
                            "The previous property will be replaced now!",
                            new Throwable()
                        );

                    state.selectedTabIndex = index;
               })
               ._withOnShow( index, (thisComponent,i) -> {
                   ExtraState state = ExtraState.of(thisComponent);
                   thisComponent.setSelectedIndex(i);
                   state.selectionListeners.forEach( l -> l.accept(i) );
               })
               ._with( thisComponent -> {
                   _onChange(thisComponent, e -> _runInApp(()->{
                       ExtraState state = ExtraState.of(thisComponent);
                       index.set(From.VIEW, thisComponent.getSelectedIndex());
                       state.selectionListeners.forEach(l -> l.accept(thisComponent.getSelectedIndex()) );
                   }));
                   thisComponent.setSelectedIndex(index.get());
               })
               ._this();
    }

    /**
     *  Defines the tab placement side based on the given {@link swingtree.UI.Side} enum,
     *  which maps directly to the {@link JTabbedPane#setTabPlacement(int)} method.
     *
     * @param side The position to use for the tabs.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withTabPlacementAt( UI.Side side ) {
        NullUtil.nullArgCheck(side, "side", UI.Side.class );
        return _with( thisComponent -> {
                    thisComponent.setTabPlacement(side.forTabbedPane());
               })
               ._this();
    }

    /**
     *  Binds the supplied property to the tab placement of the tabbed pane.
     *  This means that when the property changes, the tab placement will change accordingly.
     *  The {@link swingtree.UI.Side} enum maps directly to the {@link JTabbedPane#setTabPlacement(int)} method.
     *
     * @param side The position property to use for the tabs.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withTabPlacementAt( Val<UI.Side> side ) {
        NullUtil.nullArgCheck(side, "side", Var.class);
        return _withOnShow( side, (thisComponent,v) -> {
                    thisComponent.setTabPlacement(v.forTabbedPane());
               })
                ._with( thisComponent -> {
                    thisComponent.setTabPlacement(side.get().forTabbedPane());
                })
               ._this();
    }

    /**
     *  Defines the overflow policy based on the given {@link swingtree.UI.OverflowPolicy} enum,
     *  which maps directly to the {@link JTabbedPane#setTabLayoutPolicy(int)} method.
     *  The overflow policy must either be {@link swingtree.UI.OverflowPolicy#SCROLL} or
     *  {@link swingtree.UI.OverflowPolicy#WRAP}.
     *  The {@link swingtree.UI.OverflowPolicy#SCROLL} policy will make the tabs scrollable
     *  when there are too many tabs to fit in the tabbed pane.
     *  The {@link swingtree.UI.OverflowPolicy#WRAP} policy will make the tabs wrap to the next line
     *  when there are too many tabs to fit in the tabbed pane.
     *
     * @param policy The overflow policy to use for the tabs.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withOverflowPolicy( UI.OverflowPolicy policy ) {
        NullUtil.nullArgCheck( policy, "policy", UI.OverflowPolicy.class );
        return _with( thisComponent -> {
                    thisComponent.setTabLayoutPolicy(policy.forTabbedPane());
               })
               ._this();
    }

    /**
     *  Binds the supplied enum property to the overflow policy of the tabbed pane.
     *  When the item of the property changes, the overflow policy will change accordingly.
     *  The {@link swingtree.UI.OverflowPolicy} enum maps directly to the
     *  {@link JTabbedPane#setTabLayoutPolicy(int)} method.
     *
     * @param policy The overflow policy property to use for the tabs.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withOverflowPolicy( Val<UI.OverflowPolicy> policy ) {
        NullUtil.nullArgCheck(policy, "policy", Var.class);
        return _withOnShow( policy, (thisComponent,v) -> {
                    thisComponent.setTabLayoutPolicy(v.forTabbedPane());
               })
                ._with( thisComponent -> {
                    thisComponent.setTabLayoutPolicy(policy.orElseThrowUnchecked().forTabbedPane());
                })
               ._this();
    }

    private Supplier<Integer> _indexFinderFor(
        WeakReference<P> paneRef,
        WeakReference<JComponent> contentRef
    ) {
        return ()->{
            P foundPane = paneRef.get();
            JComponent foundContent = contentRef.get();
            if ( foundPane != null && foundContent != null ) {
                for ( int i = 0; i < foundPane.getTabCount(); i++ ) {
                    if ( foundContent == foundPane.getComponentAt(i) ) return i;
                }
            }
            return -1;
        };
    }

    /**
     *  Adds a tab to the tabbed pane based on the given {@link Tab} configuration.
     *  The tab will be added to the end of the tab list.
     *
     * @param tab The tab to add to the tabbed pane.
     * @return This builder node.
     * @throws NullPointerException if the given tab is null.
     */
    public final UIForTabbedPane<P> add( Tab tab )
    {
        Objects.requireNonNull(tab);
        return _with( thisComponent -> {
            JComponent dummyContent = new JPanel();
            WeakReference<P> paneRef = new WeakReference<>(thisComponent);
            WeakReference<JComponent> contentRef = new WeakReference<>(tab.contents().orElse(dummyContent));
            Supplier<Integer> indexFinder = _indexFinderFor(paneRef, contentRef);
            tab.onSelection()
               .ifPresent(onSelection ->
                   thisComponent.addChangeListener(e -> {
                       JTabbedPane tabbedPane = paneRef.get();
                       if ( tabbedPane == null ) return;
                       int index = indexFinder.get();
                       if ( index >= 0 && index == tabbedPane.getSelectedIndex() )
                           _runInApp(()->{
                               try {
                                   onSelection.accept(new ComponentDelegate<>(tabbedPane, e));
                               } catch (Exception ex) {
                                   log.error("Error while executing action on tab selection!", ex);
                               }
                           });
                   })
               );

            TabMouseClickListener mouseListener = new TabMouseClickListener(thisComponent, indexFinder, tab.onMouseClick().orElse(null));

            // Initial tab setup:
            _doWithoutListeners(thisComponent, ()-> {
                boolean hasSelectionBoolProp = tab.isSelected().isPresent();
                ExtraState.of(thisComponent).doSilentlyIfAlreadyHasSelectionOrIf(hasSelectionBoolProp, ()->{
                    thisComponent.addTab(
                        tab.title().map(Val::orElseNull).orElse(null),
                        tab.icon().map(Val::orElseNull).orElse(null),
                        tab.contents().orElse(dummyContent),
                        tab.tip().map(Val::orElseNull).orElse(null)
                    );
                });
            });
            tab.isEnabled().ifPresent( isEnabled -> thisComponent.setEnabledAt(indexFinder.get(), isEnabled.get()) );
            tab.isSelected().ifPresent( isSelected -> {
                ExtraState state = ExtraState.of(thisComponent);
                _selectTabFromModelling(thisComponent, indexFinder.get(), isSelected.get());
                if ( isSelected instanceof Var && isSelected.isMutable() ) {
                    Var<Boolean> isSelectedMut = (Var<Boolean>) isSelected;
                    state.selectionListeners.add(i -> {
                        boolean isNowSelected = _isSuppliedTabIndexSelected(indexFinder, i);
                        isSelectedMut.set(From.VIEW, isNowSelected);
                    });
                }
            /*
                The above listener will ensure that the isSelected property of the tab is updated when
                the selection index property changes.
             */
            });

            // Now on to binding:
            tab.title()     .ifPresent( title      -> _onShow(title,      thisComponent, (c,t) -> c.setTitleAt(indexFinder.get(), t)) );
            tab.icon()      .ifPresent( icon       -> _onShow(icon,       thisComponent, (c,i) -> c.setIconAt(indexFinder.get(), i)) );
            tab.tip()       .ifPresent( tip        -> _onShow(tip,        thisComponent, (c,t) -> c.setToolTipTextAt(indexFinder.get(), t)) );
            tab.isEnabled() .ifPresent( enabled    -> _onShow(enabled,    thisComponent, (c,e) -> c.setEnabledAt(indexFinder.get(), e)) );
            tab.isSelected().ifPresent( isSelected -> _onShow(isSelected, thisComponent, (c,s) -> _selectTab(c, indexFinder.get(), s) ));

            tab.headerContents().ifPresent( c ->
                    thisComponent
                    .setTabComponentAt(
                        thisComponent.getTabCount()-1,
                        _buildTabHeader( tab, mouseListener )
                    )
                );
        })
        ._this();
    }

    /**
     * Dynamically generates tabs for items in a {@link Vals} list and automatically updates them
     * when the items change. The items are typically view model instances, but can be any type.
     * <p>
     * The provided {@link TabSupplier} lambda is invoked with each item from the list,
     * returning a {@link Tab} to be added to the {@link JTabbedPane} wrapped by this builder.
     * <p>
     * <b>Note:</b> Binding tabs to a {@link Vals} list assumes no other tabs are present.
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     *     UI.panel()
     *      .add(
     *          UI.tabbedPane().addAll(tabs, model ->
     *              switch(model.type()) {
     *                  case LOGIN -> UI.tab("Login").add(..);
     *                  case ABOUT -> UI.tab("About").add(..);
     *                  case SETTINGS -> UI.tab("Settings").add(..);
     *              }
     *          )
     *      )
     * }</pre>
     *
     * @param <M>         The type of items in the {@link Vals} list.
     * @param tabModels   A list of items, typically view model instances.
     * @param tabSupplier A lambda to generate a {@link Tab} for each item.
     * @return This instance, allowing for builder-style method chaining.
     */
    public <M> UIForTabbedPane<P> addAll(Vals<M> tabModels, TabSupplier<M> tabSupplier) {
        Objects.requireNonNull(tabModels, "tabModels");
        Objects.requireNonNull(tabSupplier, "tabSupplier");
        return _with(thisComponent -> {
            if ( thisComponent.getTabCount() > 0 ) {
                log.warn(
                    "Trying to bind a list of tabs to a tabbed pane that already has tabs. \n" +
                    "Manually defined tabs existing along with bound tabs is not supported. \n" +
                    "The manually defined tabs will be removed now!",
                    new Throwable() // Stack trace so that a user can see where this warning was triggered.
                );
                _doWithoutListeners(thisComponent, thisComponent::removeAll);
            }
            _onShow(tabModels, thisComponent, (pane, delegate) -> {
                _updateTabs(pane, delegate, tabSupplier);
            });

            tabModels.forEach(v -> _addTabAt(thisComponent.getTabCount(), v, tabSupplier, thisComponent));
        })._this();
    }

    private <M> void _updateTabs(P pane, ValsDelegate<M> delegate, TabSupplier<M> tabSupplier) {
        Vals<M> newValues = delegate.newValues();
        Vals<M> oldValues = delegate.oldValues();
        int index = delegate.index().orElse(-1);

        switch (delegate.change()) {
            case SET:
                if ( index < 0 ) {
                    // Some things were set, but we don't know where exactly
                    pane.removeAll();
                    delegate.currentValues().forEach(value -> _addTabAt(pane.getTabCount(), value, tabSupplier, pane));
                } else {
                    for (int i = 0; i < newValues.size(); i++) {
                        int position = index + i;
                        _updateTabAt(position, newValues.at(i).orElseNull(), tabSupplier, pane);
                    }
                }
                break;
            case ADD:
                if ( index < 0 ) {
                    // Some things were added, but we don't know where exactly
                    pane.removeAll();
                    delegate.currentValues().forEach(value -> _addTabAt(pane.getTabCount(), value, tabSupplier, pane));
                } else {
                    for (int i = 0; i < newValues.size(); i++) {
                        int position = index + i;
                        _addTabAt(position, newValues.at(i).orElseNull(), tabSupplier, pane);
                    }
                }
                break;
            case REMOVE:
                if ( index < 0 ) {
                    // Some things were removed, but we don't know where exactly
                    pane.removeAll();
                    delegate.currentValues().forEach(value -> _addTabAt(pane.getTabCount(), value, tabSupplier, pane));
                } else {
                    for (int i = 0; i < oldValues.size(); i++) {
                        _removeTabAt(index, pane);
                    }
                }
                break;
            case CLEAR:
                pane.removeAll();
                break;
            case NONE:
                break;
            default:
                log.warn("Unknown change type: {}", delegate.change(), new Throwable());
                // We do a simple rebuild:
                pane.removeAll();
                delegate.currentValues().forEach(value -> _addTabAt(pane.getTabCount(), value, tabSupplier, pane));
        }

        if (pane.getTabCount() != delegate.currentValues().size()) {
            log.warn(
                "Broken binding to view model list detected! \n" +
                "TabbedPane tab count '{}' does not match tab models list of size '{}'. \n" +
                "A possible cause for this is that tabs were {} this '{}' \n" +
                "directly, instead of through the property list binding. \n" +
                "However, this could also be a bug in the UI framework.",
                pane.getComponentCount(),
                delegate.currentValues().size(),
                pane.getTabCount() > delegate.currentValues().size() ? "added to" : "removed from",
                pane,
                new Throwable()
            );
        }
    }

    /**
     * Dynamically generates tabs for items in a {@link Tuple} {@link Val} and automatically updates them
     * when the items change. The tuple items are typically view model instances, but can be any type.
     * <p>
     * The provided {@link TabSupplier} lambda is invoked with each item from the tuple,
     * returning a {@link Tab} to be added to the {@link JTabbedPane} wrapped by this builder.
     * <p>
     * <b>Note:</b> Binding tabs to a {@link Tuple} {@link Val} assumes no other tabs are present.
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     *     UI.panel()
     *      .add(
     *          UI.tabbedPane().addAll(tabs, model ->
     *              switch(model.type()) {
     *                  case LOGIN -> UI.tab("Login").add(..);
     *                  case ABOUT -> UI.tab("About").add(..);
     *                  case SETTINGS -> UI.tab("Settings").add(..);
     *              }
     *          )
     *      )
     * }</pre>
     *
     * @param <M>         The type of items in the form of a {@link Tuple} wrapped by a {@link Val}.
     * @param tabModels   A list of items, typically view model instances.
     * @param tabSupplier A lambda to generate a {@link Tab} for each item.
     * @return This instance, allowing for builder-style method chaining.
     */
    public <M> UIForTabbedPane<P> addAll(Val<Tuple<M>> tabModels, TabSupplier<M> tabSupplier) {
        Objects.requireNonNull(tabModels, "tabModels");
        Objects.requireNonNull(tabSupplier, "tabSupplier");
        return _with(thisComponent -> {
            if ( thisComponent.getTabCount() > 0 ) {
                log.warn(
                    "Trying to bind a tuple of tabs to a tabbed pane that already has tabs. \n" +
                    "Manually defined tabs existing along with bound tabs is not supported. \n" +
                    "The manually defined tabs will be removed now!",
                    new Throwable() // Stack trace so that a user can see where this warning was triggered.
                );
                _doWithoutListeners(thisComponent, thisComponent::removeAll);
            }
            AtomicReference<@Nullable SequenceDiff> lastDiffRef = new AtomicReference<>(null);
            if (tabModels.get() instanceof SequenceDiffOwner)
                lastDiffRef.set(((SequenceDiffOwner)tabModels.get()).differenceFromPrevious().orElse(null));
            _onShow(tabModels, thisComponent, (pane, tupleOfModels) -> {
                _updateTabs(pane, tupleOfModels, lastDiffRef, tabSupplier);
            });

            tabModels.get().forEach(v -> _addTabAt(thisComponent.getTabCount(), v, tabSupplier, thisComponent));
        })._this();
    }

    private  <M> void _updateTabs(P pane, Tuple<M> tupleOfModels, AtomicReference<@Nullable SequenceDiff> lastDiffRef, TabSupplier<M> tabSupplier) {
        SequenceDiff diff = null;
        SequenceDiff lastDiff = lastDiffRef.get();
        if (tupleOfModels instanceof SequenceDiffOwner)
            diff = ((SequenceDiffOwner)tupleOfModels).differenceFromPrevious().orElse(null);
        lastDiffRef.set(diff);

        if ( diff == null || ( lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) ) {
            pane.removeAll();
            tupleOfModels.forEach(value -> _addTabAt(pane.getTabCount(), value, tabSupplier, pane));
        } else {
            switch (diff.change()) {
                case SET:
                    for (int i = 0; i < diff.size(); i++) {
                        int position = diff.index().orElse(0) + i;
                        _updateTabAt(position, tupleOfModels.get(position), tabSupplier, pane);
                    }
                    break;
                case ADD:
                    for (int i = 0; i < diff.size(); i++) {
                        int position = diff.index().orElse(0) + i;
                        _addTabAt(position, tupleOfModels.get(position), tabSupplier, pane);
                    }
                    break;
                case REMOVE:
                    for (int i = 0; i < diff.size(); i++) {
                        _removeTabAt(diff.index().orElse(0), pane);
                    }
                    break;
                case RETAIN:
                    int currentNumberOfTabs = pane.getTabCount();
                    int firstToRemove = diff.index().orElse(0);
                    int lastToRemove = currentNumberOfTabs - (firstToRemove + diff.size());
                    //remove the first n tabs
                    for (int i = 0; i < firstToRemove; i++) {
                        _removeTabAt(0, pane);
                    }
                    // remove the last n tabs
                    for (int i = 0; i < lastToRemove; i++) {
                        _removeTabAt(diff.size(), pane);
                    }
                    break;
                case CLEAR:
                    pane.removeAll();
                    break;
                case NONE:
                    break;
                default:
                    log.warn("Unknown change type: {}", diff.change(), new Throwable());
                    // We do a simple rebuild:
                    pane.removeAll();
                    tupleOfModels.forEach(value -> _addTabAt(pane.getTabCount(), value, tabSupplier, pane));
            }
        }

        if (pane.getTabCount() != tupleOfModels.size()) {
            log.warn(
                "Broken binding to view model tuple detected! \n" +
                "TabbedPane tab count '{}' does not match tab models tuple of size '{}'. \n" +
                "A possible cause for this is that tabs were {} this '{}' \n" +
                "directly, instead of through the property tuple binding. \n" +
                "However, this could also be a bug in the UI framework.",
                pane.getComponentCount(),
                tupleOfModels.size(),
                pane.getTabCount() > tupleOfModels.size() ? "added to" : "removed from",
                pane,
                new Throwable()
            );
        }
    }

    private void _doWithoutListeners( P thisComponent, Runnable r ) {
        ChangeListener[] listeners = thisComponent.getChangeListeners();
        for ( ChangeListener l : listeners ) thisComponent.removeChangeListener(l);
        r.run();
        for ( ChangeListener l : listeners ) thisComponent.addChangeListener(l);
        /*
            This is important because the tabbed pane will fire a change event when a tab is added.
            This is not desirable because the tabbed pane is not yet fully initialized at that point.
        */
    }

    private void _selectTab( P thisComponent, int tabIndex, boolean isSelected ) {
        ExtraState state = ExtraState.of(thisComponent);
        int selectedIndex = ( isSelected ? tabIndex : thisComponent.getSelectedIndex() );
        if ( state.selectedTabIndex != null )
            state.selectedTabIndex.set(From.VIEW, selectedIndex);
        else if ( isSelected )
            thisComponent.setSelectedIndex(selectedIndex);

        state.selectionListeners.forEach(l -> l.accept(selectedIndex));
    }

    private void _selectTabFromModelling( P thisComponent, int tabIndex, boolean isSelected ) {
        int selectedIndex = ( isSelected ? tabIndex : thisComponent.getSelectedIndex() );
        if ( isSelected )
            thisComponent.setSelectedIndex(selectedIndex);
    }

    private JComponent _buildTabHeader( Tab tab, TabMouseClickListener mouseListener )
    {
        return
            tab.title().map( title ->
                // We want both title and user component in the header!
                UI.panel("fill, ins 0").withBackground(new Color(0,0,0,0))
                .applyIfPresent( tab.tip().map( tip -> panel -> panel.withTooltip(tip) ) )
                .peek( it -> {
                    it.addMouseListener(mouseListener);
                    mouseListener.addOwner(it);
                })
                .add("shrink",
                    UI.label(title).withBackground(new Color(0,0,0,0))
                    .applyIfPresent( tab.tip().map( tip -> label -> label.withTooltip(tip) ) )
                    .peek( it -> {
                        it.addMouseListener(mouseListener);
                        mouseListener.addOwner(it);
                    })
                )
                .add("grow", tab.headerContents().orElse(new JPanel()))
                .getComponent()
            )
            .map( p -> (JComponent) p )
            .orElse(tab.headerContents().orElse(new JPanel()));
    }

    private class TabMouseClickListener extends MouseAdapter
    {
        private final List<WeakReference<JComponent>> ownerRefs = new ArrayList<>();
        private final WeakReference<JTabbedPane> paneRef;
        private final Supplier<Integer> indexFinder;
        private final @Nullable Action<ComponentDelegate<JTabbedPane, MouseEvent>> mouseClickAction;


        private TabMouseClickListener(
            JTabbedPane pane,
            Supplier<Integer> indexFinder,
            @Nullable Action<ComponentDelegate<JTabbedPane, MouseEvent>> mouseClickAction
        ) {
            this.paneRef = new WeakReference<>(pane);
            this.indexFinder = Objects.requireNonNull(indexFinder);
            this.mouseClickAction = mouseClickAction;
            if ( mouseClickAction != null ) {
                pane.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        JTabbedPane pane = paneRef.get();
                        if ( pane == null ) return;
                        int indexOfThis = indexOfThisTab();
                        if ( indexOfThis < 0 ) return;
                        int indexClicked = pane.indexAtLocation(e.getX(), e.getY());
                        if ( indexClicked < 0 ) return;
                        if ( indexOfThis == indexClicked )
                            _runInApp(()-> {
                                try {
                                    mouseClickAction.accept(new ComponentDelegate<>(pane, e));
                                } catch (Exception ex) {
                                    log.error("Error while executing action on tab click!", ex);
                                }
                            });
                    }
                });
            }
        }

        private void doAction( JTabbedPane pane, MouseEvent e ) {
            Point p = e.getPoint();
            if ( e.getSource() != pane ) {
               // We need to find the point relative to the tabbed pane:
                p = traversePosition((Component) e.getSource(), pane, p);
            }
            int indexOfThis = indexOfThisTab();
            if ( indexOfThis < 0 ) return;
            int indexClicked = pane.indexAtLocation(p.x, p.y);
            if ( indexClicked < 0 ) return;
            if ( indexOfThis == indexClicked && mouseClickAction != null )
                _runInApp(()-> {
                    try {
                        mouseClickAction.accept(new ComponentDelegate<>(pane, e));
                    } catch (Exception ex) {
                        log.error("Error while executing action on tab click!", ex);
                    }
                });
            if ( indexOfThis < pane.getTabCount() )
                pane.setSelectedIndex(indexOfThis);
        }

        private int indexOfThisTab() {
            return indexFinder.get();
        }

        public void addOwner(JComponent c) { this.ownerRefs.add(new WeakReference<>(c)); }

        @Override
        public void mouseClicked( MouseEvent e ) {
            JTabbedPane pane = this.paneRef.get();
            if ( pane == null ) {
                for ( WeakReference<JComponent> compRef : this.ownerRefs) {
                    JComponent owner = compRef.get();
                    if ( owner != null )
                        owner.removeMouseListener(this);
                }
            }
            else doAction( pane, e );
        }
    }

    /**
     *  If we click on a subcomponent on the header we need to traverse
     *  upwards to find the click position relative to the tabbed pane!
     *  Otherwise we don't know where the click went.
     *
     * @param current The component where we currently have the relative position {@code p}.
     * @param end The component at which we end traversal when it is the same as the current.
     * @param p The relative position to the current component.
     * @return The relative position to the end component!
     */
    private static Point traversePosition( Component current, Component end, Point p ) {
        if ( current == end ) return p;
        Component parent = current.getParent();
        Point relativeToParent = SwingUtilities.convertPoint(current, p, parent);
        return traversePosition(parent, end, relativeToParent);
    }

    /**
     * Adds an {@link Action} to the underlying {@link JTabbedPane}
     * through an {@link javax.swing.event.ChangeListener},
     * which will be called when the state of the tabbed pane changes.
     * For more information see {@link JTabbedPane#addChangeListener(javax.swing.event.ChangeListener)}.
     *
     * @param onChange The {@link Action} that will be called through the underlying change event.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForTabbedPane<P> onChange( Action<ComponentDelegate<P, ChangeEvent>> onChange ) {
        NullUtil.nullArgCheck(onChange, "onChange", Action.class);
        return _with( thisComponent -> {
                    _onChange(thisComponent, e -> _runInApp(()->{
                        try {
                            onChange.accept(new ComponentDelegate<>(thisComponent, e));
                        } catch (Exception ex) {
                            log.error("Error while executing action on tab change!", ex);
                        }
                    }));
                })
                ._this();
    }

    private void _onChange( P thisComponent, Consumer<ChangeEvent> action ) {
        thisComponent.addChangeListener(action::accept);
    }

    private <M> void _addTabAt(int index, @Nullable M m, TabSupplier<M> tabSupplier, P p) {
        Tab tab = _createTab(m, tabSupplier);

        JComponent dummyContent = new JPanel();
        WeakReference<P> paneRef = new WeakReference<>(p);
        WeakReference<JComponent> contentRef = new WeakReference<>(tab.contents().orElse(dummyContent));
        Supplier<Integer> indexFinder = _indexFinderFor(paneRef, contentRef);

        tab.onSelection()
            .ifPresent(onSelection -> {
                OnSelectionMultiplexer.of(p).set(p, index, e -> {
                    JTabbedPane tabbedPane = paneRef.get();
                    if (tabbedPane == null) return;
                    int i = indexFinder.get();
                    if (i >= 0 && i == tabbedPane.getSelectedIndex())
                        _runInApp(() -> {
                            try {
                                onSelection.accept(new ComponentDelegate<>(tabbedPane, e));
                            } catch (Exception ex) {
                                log.error("Error while executing action on tab selection!", ex);
                            }
                        });
                });
            });

        TabMouseClickListener mouseListener = new TabMouseClickListener(p, indexFinder, tab.onMouseClick().orElse(null));

        // Initial tab setup:
        p.insertTab(
            tab.title().map(Val::orElseNull).orElse(null),
            tab.icon().map(Val::orElseNull).orElse(null),
            tab.contents().orElse(dummyContent),
            tab.tip().map(Val::orElseNull).orElse(null),
            index
        );
        tab.isEnabled().ifPresent(isEnabled -> p.setEnabledAt(indexFinder.get(), isEnabled.get()));
        tab.isSelected().ifPresent(isSelected -> {
            ExtraState state = ExtraState.of(p);
            _selectTabFromModelling(p, indexFinder.get(), isSelected.get());
            if (isSelected instanceof Var && isSelected.isMutable()) {
                Var<Boolean> isSelectedMut = (Var<Boolean>) isSelected;
                state.selectionListeners.add(i -> {
                    boolean isNowSelected = _isSuppliedTabIndexSelected(indexFinder, i);
                    isSelectedMut.set(From.VIEW, isNowSelected);
                });
            }
            /*
                The above listener will ensure that the isSelected property of the tab is updated when
                the selection index property changes.
             */
        });

        // Now on to binding:
        tab.title().ifPresent(title -> _onShow(title, p, (c, t) -> c.setTitleAt(indexFinder.get(), t)));
        tab.icon().ifPresent(icon -> _onShow(icon, p, (c, i) -> c.setIconAt(indexFinder.get(), i)));
        tab.tip().ifPresent(tip -> _onShow(tip, p, (c, t) -> c.setToolTipTextAt(indexFinder.get(), t)));
        tab.isEnabled().ifPresent(enabled -> _onShow(enabled, p, (c, e) -> c.setEnabledAt(indexFinder.get(), e)));
        tab.isSelected().ifPresent(isSelected -> _onShow(isSelected, p, (c, s) -> _selectTab(c, indexFinder.get(), s)));

        tab.headerContents().ifPresent(c -> p.setTabComponentAt(index, _buildTabHeader(tab, mouseListener)));
    }

    private static final class OnSelectionMultiplexer implements ChangeListener {
        static OnSelectionMultiplexer of(JTabbedPane pane) {
            ComponentExtension<JTabbedPane> extension = ComponentExtension.from(pane);
            OnSelectionMultiplexer found = extension.getOrSet(OnSelectionMultiplexer.class, ()->new OnSelectionMultiplexer(pane));
            return found;
        }

        private final Map<Integer, ChangeListener> _subListeners = new HashMap<>();

        private OnSelectionMultiplexer(JTabbedPane pane) {
            pane.addChangeListener(this);
        }

        void set(JTabbedPane pane, int index, ChangeListener listener) {
            _subListeners.put(index, listener);
        }

        private void _cleanup(JTabbedPane pane) {
            int numberOfTabs = pane.getTabCount();
            for ( int i : _subListeners.keySet()) {
                if ( i < 0 || i >= numberOfTabs ) {
                    _subListeners.remove(i);
                }
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            _cleanup((JTabbedPane)e.getSource());
            for ( ChangeListener listener : _subListeners.values() ) {
                try {
                    listener.stateChanged(e);
                } catch (Exception ex) {
                    log.error("Error while executing action on tab selection!", ex);
                }
            }
        }
    }

    private static boolean _isSuppliedTabIndexSelected(Supplier<Integer> indexOfCurrent, int newIndex) {
        return newIndex >= 0 && Objects.equals(newIndex, indexOfCurrent.get());
    }

    private <M> void _updateTabAt(int index, @Nullable M m, TabSupplier<M> tabSupplier, P p) {
        _removeTabAt(index, p);
        _addTabAt(index, m, tabSupplier, p);
    }

    private void _removeTabAt(int index, P p) {
        p.removeTabAt(index);
    }

    private <M> Tab _createTab( @Nullable M m, TabSupplier<M> tabSupplier ) {
        if (m == null)
            return UIForTabbedPane.TAB_NULL;

        try {
            Tab tab = tabSupplier.createTabFor(m);
            if ( tab == null ) {
                log.warn("Tab supplier returned null for '{}'.", m, new Throwable());
                return UIForTabbedPane.TAB_NULL;
            }
            return tab;
        } catch (Exception e) {
            log.error("Error while creating tab for '{}'.", m, e);
            return UIForTabbedPane.TAB_ERROR;
        }
    }

    private static class ExtraState extends DefaultSingleSelectionModel
    {
        static ExtraState of( JTabbedPane pane ) {
            return ComponentExtension.from(pane)
                    .getOrSet(ExtraState.class, ExtraState::new);
        }

        final List<Consumer<Integer>> selectionListeners = new ArrayList<>();
        private @Nullable Var<Integer> selectedTabIndex = null;
        private boolean ignoreChanges = false;

        @Override public void setSelectedIndex(int index) {
            if ( ignoreChanges )
                return;
            super.setSelectedIndex(index);
            if ( selectedTabIndex != null )
                selectedTabIndex.set(From.VIEW, index);

            selectionListeners.forEach(l -> l.accept(index));
        }
        @Override public void clearSelection() {
            if ( ignoreChanges )
                return;
            super.clearSelection();
            if ( selectedTabIndex != null )
                selectedTabIndex.set(From.VIEW, -1);
        }

        private void doSilentlyIfAlreadyHasSelectionOrIf(boolean condition, Runnable action) {
            ignoreChanges = ( condition || this.selectedTabIndex != null );
            try {
                action.run();
            } finally {
                ignoreChanges = false;
            }
        }
    }

}

package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.style.ComponentExtension;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TabbedPaneUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  A SwingTree builder node designed for configuring {@link JTabbedPane} instances.
 */
public final class UIForTabbedPane<P extends JTabbedPane> extends UIForAnySwing<UIForTabbedPane<P>, P>
{
    private static Logger log = LoggerFactory.getLogger(UIForTabbedPane.class);

    private final BuilderState<P> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForTabbedPane( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state.with( thisComponent -> {
            thisComponent.setModel(ExtraState.of(thisComponent));
        });
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForTabbedPane<P> _with( BuilderState<P> newState ) {
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
                            int indexOfTab = ExtraState.of(thisComponent).lastHoveredTabIndex;
                            /*                                             ^?
                                The reason why we do not use `ui.tabForCoordinate(thisComponent, e.getX(), e.getY());`
                                here is because when this mouse listener is called the TabbedPaneUI will
                                already have updated the tab index to the one where the mouse cursor is located.

                                Now this is not a problem when the tabs do not change their order in the UI,
                                but guess what, on a mouse based selection, they can actually change their order in the UI
                                specifically when the tabs are stacked on top of each other (due to limited horizontal space)
                                and the one with the mouse position on top of it is not the bottom one.
                                In that scenario the tab UI will rearrange the tabs so that the one with the
                                mouse position on top of it will be the bottom one.
                                And in that specific case calling `ui.tabForCoordinate(thisComponent, e.getX(), e.getY());`
                                will most likely return the wrong tab index!

                                A proper solution to this would be to get this mouse listener to
                                be triggered before the one created by the tabbed pane UI...
                                ...so that the UI can not rearrange the tabs.
                                But good luck trying to do that!
                            */
                            if ( indexOfTab < 0 )
                                indexOfTab = _tabIndexOfPosition(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount ) {
                                int finalIndexOfTab = indexOfTab;
                                _doApp(() -> onClick.accept(new TabDelegate(thisComponent, e, finalIndexOfTab)));
                            }
                        }
                    });
                    if ( ExtraState.of(thisComponent).mouseTabHoverSenser == null ) {
                        ExtraState.of(thisComponent).mouseTabHoverSenser = new MouseTabHoverSenser();
                        thisComponent.addMouseMotionListener(ExtraState.of(thisComponent).mouseTabHoverSenser);
                    }
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
                            int indexOfTab = ExtraState.of(thisComponent).lastHoveredTabIndex;
                            /*                                             ^?
                                The reason why we do not use `ui.tabForCoordinate(thisComponent, e.getX(), e.getY());`
                                here is because when this mouse listener is called the TabbedPaneUI will
                                already have updated the tab index to the one where the mouse cursor is located.

                                Now this is not a problem when the tabs do not change their order in the UI,
                                but guess what, on a mouse based selection, they can actually change their order in the UI
                                specifically when the tabs are stacked on top of each other (due to limited horizontal space)
                                and the one with the mouse position on top of it is not the bottom one.
                                In that scenario the tab UI will rearrange the tabs so that the one with the
                                mouse position on top of it will be the bottom one.
                                And in that specific case calling `ui.tabForCoordinate(thisComponent, e.getX(), e.getY());`
                                will most likely return the wrong tab index!

                                A proper solution to this would be to get this mouse listener to
                                be triggered before the one created by the tabbed pane UI...
                                ...so that the UI can not rearrange the tabs.
                                But good luck trying to do that!
                            */
                            if ( indexOfTab < 0 )
                                indexOfTab = _tabIndexOfPosition(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount ) {
                                int finalIndexOfTab = indexOfTab;
                                _doApp(() -> onPress.accept(new TabDelegate(thisComponent, e, finalIndexOfTab)));
                            }
                        }
                    });
                    if ( ExtraState.of(thisComponent).mouseTabHoverSenser == null ) {
                        ExtraState.of(thisComponent).mouseTabHoverSenser = new MouseTabHoverSenser();
                        thisComponent.addMouseMotionListener(ExtraState.of(thisComponent).mouseTabHoverSenser);
                    }
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
                            int indexOfTab = ExtraState.of(thisComponent).lastHoveredTabIndex;
                            /*                                             ^?
                                The reason why we do not use `ui.tabForCoordinate(thisComponent, e.getX(), e.getY());`
                                here is because when this mouse listener is called the TabbedPaneUI will
                                already have updated the tab index to the one where the mouse cursor is located.

                                Now this is not a problem when the tabs do not change their order in the UI,
                                but guess what, on a mouse based selection, they can actually change their order in the UI
                                specifically when the tabs are stacked on top of each other (due to limited horizontal space)
                                and the one with the mouse position on top of it is not the bottom one.
                                In that scenario the tab UI will rearrange the tabs so that the one with the
                                mouse position on top of it will be the bottom one.
                                And in that specific case calling `ui.tabForCoordinate(thisComponent, e.getX(), e.getY());`
                                will most likely return the wrong tab index!

                                A proper solution to this would be to get this mouse listener to
                                be triggered before the one created by the tabbed pane UI...
                                ...so that the UI can not rearrange the tabs.
                                But good luck trying to do that!
                            */
                            if ( indexOfTab < 0 )
                                indexOfTab = _tabIndexOfPosition(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount ) {
                                int finalIndexOfTab = indexOfTab;
                                _doApp(() -> onRelease.accept(new TabDelegate(thisComponent, e, finalIndexOfTab)));
                            }
                        }
                    });
                    if ( ExtraState.of(thisComponent).mouseTabHoverSenser == null ) {
                        ExtraState.of(thisComponent).mouseTabHoverSenser = new MouseTabHoverSenser();
                        thisComponent.addMouseMotionListener(ExtraState.of(thisComponent).mouseTabHoverSenser);
                    }
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
                            int indexOfTab = _tabIndexOfPosition(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _doApp(() -> onEnter.accept(new TabDelegate(thisComponent, e, indexOfTab)));
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
                            int indexOfTab = _tabIndexOfPosition(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _doApp(() -> onExit.accept(new TabDelegate(thisComponent, e, indexOfTab)));
                        }
                    });
               })
               ._this();
    }

    private static int _tabIndexOfPosition(JTabbedPane pane, Point p ) {
        TabbedPaneUI ui = pane.getUI();
        return ui.tabForCoordinate(pane, p.x, p.y);
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
                   _onChange(thisComponent, e -> _doApp(()->{
                       ExtraState state = ExtraState.of(thisComponent);
                       index.set(From.VIEW, thisComponent.getSelectedIndex());
                       state.selectionListeners.forEach(l -> l.accept(thisComponent.getSelectedIndex()) );
                   }));
                   thisComponent.setSelectedIndex(index.get());
               })
               ._this();
    }

    /**
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
     * @param policy The overflow policy property to use for the tabs.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withOverflowPolicy( Val<UI.OverflowPolicy> policy ) {
        NullUtil.nullArgCheck(policy, "policy", Var.class);
        return _withOnShow( policy, (thisComponent,v) -> {
                    thisComponent.setTabLayoutPolicy(v.forTabbedPane());
               })
                ._with( thisComponent -> {
                    thisComponent.setTabLayoutPolicy(policy.orElseThrow().forTabbedPane());
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

    public final UIForTabbedPane<P> add( Tab tab )
    {
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
                           _doApp(()->onSelection.accept(new ComponentDelegate<>(tabbedPane, e )));
                   })
               );

            TabMouseClickListener mouseListener = new TabMouseClickListener(thisComponent, indexFinder, tab.onMouseClick().orElse(null));

            // Initial tab setup:
            _doWithoutListeners(thisComponent, ()->
                thisComponent.addTab(
                    tab.title().map(Val::orElseNull).orElse(null),
                    tab.icon().map(Val::orElseNull).orElse(null),
                    tab.contents().orElse(dummyContent),
                    tab.tip().map(Val::orElseNull).orElse(null)
                )
            );
            tab.isEnabled().ifPresent( isEnabled -> thisComponent.setEnabledAt(indexFinder.get(), isEnabled.get()) );
            tab.isSelected().ifPresent( isSelected -> {
                ExtraState state = ExtraState.of(thisComponent);
                _selectTab(thisComponent, indexFinder.get(), isSelected.get());
                state.selectionListeners.add(i -> isSelected.set(From.VIEW, Objects.equals(i, indexFinder.get())) );
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
        else
            thisComponent.setSelectedIndex(selectedIndex);

        state.selectionListeners.forEach(l -> l.accept(selectedIndex));
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
        private final Action<ComponentDelegate<JTabbedPane, MouseEvent>> mouseClickAction;


        private TabMouseClickListener(
            JTabbedPane pane,
            Supplier<Integer> indexFinder,
            Action<ComponentDelegate<JTabbedPane, MouseEvent>> mouseClickAction
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
                        int indexClicked = _tabIndexOfPosition(pane, e.getPoint());
                        if ( indexClicked < 0 ) return;
                        if ( indexOfThis == indexClicked )
                            _doApp(()-> mouseClickAction.accept(new ComponentDelegate<>(pane, e )));
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
            int indexClicked = _tabIndexOfPosition( pane, p );
            if ( indexClicked < 0 ) return;
            if ( indexOfThis == indexClicked && mouseClickAction != null )
                _doApp(()-> { mouseClickAction.accept(new ComponentDelegate<>(pane, e)); });
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
                    _onChange(thisComponent, e -> _doApp(()->onChange.accept(new ComponentDelegate<>(thisComponent, e))));
                })
                ._this();
    }

    private void _onChange( P thisComponent, Consumer<ChangeEvent> action ) {
        thisComponent.addChangeListener(action::accept);
    }

    private static class ExtraState extends DefaultSingleSelectionModel
    {
        static ExtraState of( JTabbedPane pane ) {
            return ComponentExtension.from(pane)
                    .getOrSet(ExtraState.class, ExtraState::new);
        }

        final List<Consumer<Integer>> selectionListeners = new ArrayList<>();
        Var<Integer> selectedTabIndex = null;
        MouseTabHoverSenser mouseTabHoverSenser = null;
        int lastHoveredTabIndex = -1;

        @Override public void setSelectedIndex(int index) {
            super.setSelectedIndex(index);
            if ( selectedTabIndex != null )
                selectedTabIndex.set(From.VIEW, index);
        }
        @Override public void clearSelection() {
            super.clearSelection();
            if ( selectedTabIndex != null )
                selectedTabIndex.set(From.VIEW, -1);
        }
    }

    private static class MouseTabHoverSenser extends MouseMotionAdapter {
        @Override public void mouseMoved(MouseEvent e) {
            JTabbedPane pane = (JTabbedPane) e.getSource();
            int index = _tabIndexOfPosition(pane, e.getPoint());
            ExtraState state = ExtraState.of(pane);
            if ( index != state.lastHoveredTabIndex ) {
                state.lastHoveredTabIndex = index;
            }
        }
    }

}

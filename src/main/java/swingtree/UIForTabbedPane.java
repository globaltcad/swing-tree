package swingtree;

import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  A SwingTree builder node designed for configuring {@link JTabbedPane} instances.
 */
public class UIForTabbedPane<P extends JTabbedPane> extends UIForAnySwing<UIForTabbedPane<P>, P>
{
    private final List<Consumer<Integer>> _selectionListeners = new ArrayList<>();
    private Var<Integer> _selectedTabIndex = null;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    protected UIForTabbedPane( P component ) { super(component); }

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
    public final UIForTabbedPane<P> onTabMouseClick(Action<TabDelegate> onClick ) {
        NullUtil.nullArgCheck(onClick, "onClick", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addMouseListener(new MouseAdapter() {
                       @Override public void mouseClicked(MouseEvent e) {
                           int indexOfTab = _indexOfClick(thisComponent, e.getPoint());
                           int tabCount = thisComponent.getTabCount();
                           if ( indexOfTab >= 0 && indexOfTab < tabCount )
                               _doApp(() -> onClick.accept(new TabDelegate(thisComponent, e, () -> getSiblinghood(), indexOfTab)));
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
                            int indexOfTab = _indexOfClick(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _doApp(() -> onPress.accept(new TabDelegate(thisComponent, e, () -> getSiblinghood(), indexOfTab)));
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
                            int indexOfTab = _indexOfClick(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _doApp(() -> onRelease.accept(new TabDelegate(thisComponent, e, () -> getSiblinghood(), indexOfTab)));
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
                            int indexOfTab = _indexOfClick(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _doApp(() -> onEnter.accept(new TabDelegate(thisComponent, e, () -> getSiblinghood(), indexOfTab)));
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
                            int indexOfTab = _indexOfClick(thisComponent, e.getPoint());
                            int tabCount = thisComponent.getTabCount();
                            if ( indexOfTab >= 0 && indexOfTab < tabCount )
                                _doApp(() -> onExit.accept(new TabDelegate(thisComponent, e, () -> getSiblinghood(), indexOfTab)));
                        }
                    });
               })
               ._this();
    }

    private static int _indexOfClick( JTabbedPane pane, Point p ) {
        List<Rectangle> tabBounds = new ArrayList<>();
        for ( int i = 0; i < pane.getTabCount(); i++ )
            tabBounds.add(pane.getBoundsAt(i));

        for ( int i = 0; i < tabBounds.size(); i++ )
            if ( tabBounds.get(i).contains(p) )
                return i;

        return -1;
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
        return _with( thisComponent -> {
                    _onShow( index, i -> thisComponent.setSelectedIndex(i) );
                    thisComponent.setSelectedIndex(index.get());
               })
               ._this();
    }

    /**
     *  Dynamically sets the selected tab based on the given index property.
     *  So when the index property changes, the selected tab will change accordingly.
     * @param index The index property of the tab to select.
     * @return This builder node.
     */
    public final UIForTabbedPane<P> withSelectedIndex( Var<Integer> index ) {
        NullUtil.nullArgCheck( index, "index", Var.class );
        NullUtil.nullPropertyCheck( index, "index", "Null is not a valid state for modelling a selected index." );
        return _with( thisComponent -> {
                    if ( _selectedTabIndex != null )
                        throw new IllegalStateException("A selected index property has already been set for this tabbed pane.");
                    _selectedTabIndex = index;
                    _onShow( index, i -> {
                        thisComponent.setSelectedIndex(i);
                        _selectionListeners.forEach( l -> l.accept(i) );
                    });
                    _onChange(thisComponent, e -> _doApp(()->{
                        index.set(From.VIEW, thisComponent.getSelectedIndex());
                        _selectionListeners.forEach( l -> l.accept(thisComponent.getSelectedIndex()) );
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
        return _with( thisComponent -> {
                    _onShow( side, v -> thisComponent.setTabPlacement(side.orElseThrow().forTabbedPane()) );
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
        return _with( thisComponent -> {
                    _onShow(policy, v -> thisComponent.setTabLayoutPolicy(policy.orElseThrow().forTabbedPane()));
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
                           _doApp(()->onSelection.accept(new ComponentDelegate<>(tabbedPane, e, () -> getSiblinghood())));
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
                _selectTab(thisComponent, indexFinder.get(), isSelected.get());
                _selectionListeners.add( i -> isSelected.set(From.VIEW, Objects.equals(i, indexFinder.get())) );
            /*
                The above listener will ensure that the isSelected property of the tab is updated when
                the selection index property changes.
             */
            });

            // Now on to binding:
            tab.title()     .ifPresent( title      -> _onShow(title,      t -> thisComponent.setTitleAt(indexFinder.get(), t)) );
            tab.icon()      .ifPresent( icon       -> _onShow(icon,       i -> thisComponent.setIconAt(indexFinder.get(), i)) );
            tab.tip()       .ifPresent( tip        -> _onShow(tip,        t -> thisComponent.setToolTipTextAt(indexFinder.get(), t)) );
            tab.isEnabled() .ifPresent( enabled    -> _onShow(enabled,    e -> thisComponent.setEnabledAt(indexFinder.get(), e)) );
            tab.isSelected().ifPresent( isSelected -> _onShow(isSelected, s -> _selectTab(thisComponent, indexFinder.get(), s) ));

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
        int selectedIndex = ( isSelected ? tabIndex : thisComponent.getSelectedIndex() );
        if ( _selectedTabIndex != null )
            _selectedTabIndex.set(From.VIEW, selectedIndex);
        else
            thisComponent.setSelectedIndex(selectedIndex);

        _selectionListeners.forEach(l -> l.accept(selectedIndex));
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
                        int indexClicked = _indexOfClick(pane, e.getPoint());
                        if ( indexClicked < 0 ) return;
                        if ( indexOfThis == indexClicked )
                            _doApp(()-> mouseClickAction.accept(new ComponentDelegate<>(pane, e, () -> getSiblinghood())));
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
            int indexClicked = _indexOfClick( pane, p );
            if ( indexClicked < 0 ) return;
            if ( indexOfThis == indexClicked && mouseClickAction != null )
                _doApp(()-> { mouseClickAction.accept(new ComponentDelegate<>(pane, e, () -> getSiblinghood())); });
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
                    _onChange(thisComponent, e -> _doApp(()->onChange.accept(new ComponentDelegate<>(thisComponent, e, () -> getSiblinghood()))));
                })
                ._this();
    }

    private void _onChange( P thisComponent, Consumer<ChangeEvent> action ) {
        thisComponent.addChangeListener(action::accept);
    }

}

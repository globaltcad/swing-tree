package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  A swing tree builder node for {@link JTabbedPane} instances.
 */
public class UIForTabbedPane<P extends JTabbedPane> extends UIForAbstractSwing<UIForTabbedPane<P>, P>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForTabbedPane( P component ) { super(component); }


    public final UIForTabbedPane<P> withSelectedIndex( int index ) {
        getComponent().setSelectedIndex(index);
        return this;
    }

    public final UIForTabbedPane<P> withSelectedIndex( Val<Integer> index ) {
        NullUtil.nullArgCheck( index, "index", Val.class );
        NullUtil.nullPropertyCheck( index, "index", "Null is not a valid state for modelling a selected index." );
        _onShow( index, i -> getComponent().setSelectedIndex(i) );
        return withSelectedIndex(index.get());
    }

    public final UIForTabbedPane<P> withSelectedIndex( Var<Integer> index ) {
        NullUtil.nullArgCheck( index, "index", Var.class );
        NullUtil.nullPropertyCheck( index, "index", "Null is not a valid state for modelling a selected index." );
        _onShow( index, i -> getComponent().setSelectedIndex(i) );
        _onChange( e -> _doApp(()->index.act(getComponent().getSelectedIndex())) );
        return withSelectedIndex(index.get());
    }

    public final UIForTabbedPane<P> with( UI.Position position ) {
        NullUtil.nullArgCheck( position, "position", UI.Position.class );
        getComponent().setTabPlacement(position.forTabbedPane());
        return this;
    }

    public final UIForTabbedPane<P> withPosition( Val<UI.Position> position ) {
        NullUtil.nullArgCheck(position, "position", Var.class);
        _onShow(position, v -> with(position.orElseThrow()));
        return with(position.get());
    }

    public final UIForTabbedPane<P> with( UI.OverflowPolicy policy ) {
        NullUtil.nullArgCheck( policy, "policy", UI.OverflowPolicy.class );
        getComponent().setTabLayoutPolicy(policy.forTabbedPane());
        return this;
    }

    public final UIForTabbedPane<P> withOverflowPolicy( Val<UI.OverflowPolicy> policy ) {
        NullUtil.nullArgCheck(policy, "policy", Var.class);
        _onShow(policy, v -> with(policy.orElseThrow()));
        return with(policy.orElseThrow());
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

    public final UIForTabbedPane<P> add( Tab tab ) {

        JComponent dummyContent = new JPanel();
        WeakReference<P> paneRef = new WeakReference<>(getComponent());
        WeakReference<JComponent> contentRef = new WeakReference<>(tab.contents().orElse(dummyContent));
        Supplier<Integer> indexFinder = _indexFinderFor(paneRef, contentRef);
        tab.onSelection()
           .ifPresent(
               onSelection ->
                   getComponent().addChangeListener(e -> {
                       JTabbedPane tabbedPane = paneRef.get();
                       if ( tabbedPane == null ) return;
                       int index = indexFinder.get();
                       if (index >= 0 && index == tabbedPane.getSelectedIndex())
                           _doApp(()->onSelection.accept(new SimpleDelegate<>(tabbedPane, e, this::getSiblinghood)));
                   })
           );

        TabMouseClickListener mouseListener = new TabMouseClickListener(getComponent(), indexFinder, tab.onMouseClick().orElse(null));

        getComponent().addTab(
                  tab.title().orElse(null),
                  tab.icon().orElse(null),
                  tab.contents().orElse(dummyContent),
                  tab.tip().orElse(null)
              );

        tab.headerContents().ifPresent( c -> {
            getComponent()
            .setTabComponentAt(
                getComponent().getTabCount()-1,
                _buildTabHeader( tab, mouseListener )
            );
        });
        return this;
    }

    private JComponent _buildTabHeader(Tab tab, TabMouseClickListener mouseListener )
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
        private final UIAction<SimpleDelegate<JTabbedPane, MouseEvent>> mouseClickAction;


        private TabMouseClickListener(
            JTabbedPane pane,
            Supplier<Integer> indexFinder,
            UIAction<SimpleDelegate<JTabbedPane, MouseEvent>> mouseClickAction
        ) {
            this.paneRef = new WeakReference<>(pane);
            this.indexFinder = indexFinder;
            this.mouseClickAction = mouseClickAction;
            if ( mouseClickAction != null ) {
                pane.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        JTabbedPane currentPane = paneRef.get();
                        int index = indexFinder.get();
                        if ( index < 0 ) return;
                        if ( currentPane != null && index == currentPane.getSelectedIndex() )
                            _doApp(()-> mouseClickAction.accept(new SimpleDelegate<>(currentPane, e, UIForTabbedPane.this::getSiblinghood)));
                    }
                });
            }
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

        private void doAction(JTabbedPane pane, MouseEvent e) {
            _doApp(()-> {
                int index = indexFinder.get();
                if ( index < 0 ) return;
                if ( index == pane.getSelectedIndex() && mouseClickAction != null )
                    mouseClickAction.accept(new SimpleDelegate<>(pane, e, UIForTabbedPane.this::getSiblinghood));
                if ( index < pane.getTabCount() )
                    pane.setSelectedIndex(index);
            });
        }
    }

    /**
     * Adds an {@link UIAction} to the underlying {@link JTabbedPane}
     * through an {@link javax.swing.event.ChangeListener},
     * which will be called when the state of the tabbed pane changes.
     * For more information see {@link JTabbedPane#addChangeListener(javax.swing.event.ChangeListener)}.
     *
     * @param onChange The {@link UIAction} that will be called through the underlying change event.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForTabbedPane<P> onChange( UIAction<SimpleDelegate<P, ChangeEvent>> onChange ) {
        NullUtil.nullArgCheck(onChange, "onChange", UIAction.class);
        P pane = getComponent();
        _onChange(e -> _doApp(()->onChange.accept(new SimpleDelegate<>(pane, e, this::getSiblinghood))));
        return this;
    }

    private void _onChange( Consumer<ChangeEvent> action ) {
        getComponent().addChangeListener(action::accept);
    }


}

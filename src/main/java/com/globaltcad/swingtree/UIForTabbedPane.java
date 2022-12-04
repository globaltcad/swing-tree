package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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


    public final UIForTabbedPane<P> withPosition( Val<UI.Position> position ) {
        NullUtil.nullArgCheck(position, "position", Var.class);
        _onShow(position, v -> getComponent().setTabPlacement(position.orElseThrow().forTabbedPane()));
        return this;
    }

    public final UIForTabbedPane<P> withOverflowPolicy( Val<UI.OverflowPolicy> policy ) {
        NullUtil.nullArgCheck(policy, "policy", Var.class);
        _onShow(policy, v -> getComponent().setTabLayoutPolicy(policy.orElseThrow().forTabbedPane()));
        return this;
    }

    public final UIForTabbedPane<P> add( Tab tab ) {

        P pane = getComponent();
        final int index = pane.getTabCount();

        tab.onSelection()
           .ifPresent( onSelection ->
                   pane.addChangeListener(e -> {
                   if (index == pane.getSelectedIndex())
                       _doApp(()->onSelection.accept(new SimpleDelegate<>(pane, e, this::getSiblinghood)));
               })
           );

        tab.onMouseClick()
            .ifPresent( onMouseClick ->
                    pane.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        if ( index == pane.getSelectedIndex() )
                            _doApp(()->onMouseClick.accept(new SimpleDelegate<>(pane, e, ()->getSiblinghood())));
                    }
                })
            );

        pane.addTab(
                  tab.title().orElse(null),
                  tab.icon().orElse(null),
                  tab.contents().orElse(null),
                  tab.tip().orElse(null)
              );
        tab.headerContents().ifPresent( c -> _buildTabHeader( tab, index ) );
        return this;
    }

    private void _buildTabHeader( Tab tab, int index ) {

        P pane = getComponent();

        MouseListener mouseListener =
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        _doApp(()-> {
                            if (index == pane.getSelectedIndex())
                                tab.onMouseClick().ifPresent( onMouseClick ->
                                    onMouseClick.accept(new SimpleDelegate<>(pane, e, () -> getSiblinghood()))
                                );
                            pane.setSelectedIndex(index);
                        });
                    }
                };

        JComponent header =
                tab.title().map( title ->
                    // We want both title and user component in the header!
                    UI.panel("fill, ins 0").withBackground(new Color(0,0,0,0))
                    .applyIfPresent( tab.tip().map( tip -> panel -> panel.withTooltip(tip) ) )
                    .peek( it -> it.addMouseListener(mouseListener) )
                    .add("shrink",
                        UI.label(title).withBackground(new Color(0,0,0,0))
                        .applyIfPresent( tab.tip().map( tip -> label -> label.withTooltip(tip) ) )
                        .peek( it -> it.addMouseListener(mouseListener) )
                    )
                    .add("grow", tab.headerContents().orElse(new JPanel()))
                    .getComponent()
                )
                .map( p -> (JComponent) p )
                .orElse(tab.headerContents().orElse(new JPanel()));

        pane.setTabComponentAt( index, header );
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
        pane.addChangeListener(e -> _doApp(()->onChange.accept(new SimpleDelegate<>(pane, e, this::getSiblinghood))));
        return this;
    }

}

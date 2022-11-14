package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

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

    public final UIForTabbedPane<P> add( Tab tab ) {
        final int index = _component.getTabCount();

        tab.onSelection()
           .ifPresent( onSelection ->
               _component.addChangeListener(e -> {
                   if (index == _component.getSelectedIndex())
                       onSelection.accept(new SimpleDelegate<>(_component, e, () -> getSiblinghood()));
               })
           );

        tab.onMouseClick()
            .ifPresent( onMouseClick ->
                _component.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        if ( index == _component.getSelectedIndex() )
                            onMouseClick.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
                    }
                })
            );

        _component.addTab(
                        tab.title().orElse(null),
                        tab.icon().orElse(null),
                        tab.contents().orElse(null),
                        tab.tip().orElse(null)
                    );
        tab.headerContents().ifPresent( c -> _buildTabHeader( tab, index ) );
        return this;
    }

    private void _buildTabHeader( Tab tab, int index ) {

        MouseListener mouseListener =
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        if ( index == _component.getSelectedIndex() )
                            tab.onMouseClick().ifPresent( onMouseClick -> {
                                onMouseClick.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
                            });
                        _component.setSelectedIndex( index );
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

        _component.setTabComponentAt( index, header );
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
    public final UIForTabbedPane<P> onChange(UIAction<SimpleDelegate<P, ChangeEvent>> onChange) {
        LogUtil.nullArgCheck(onChange, "onChange", UIAction.class);
        _component.addChangeListener(e -> onChange.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())));
        return this;
    }

}

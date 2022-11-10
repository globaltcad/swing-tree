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
    public UIForTabbedPane(P component) { super(component); }

    public final UIForTabbedPane<P> add(Tab tab) {
        final int index = _component.getTabCount();
        if ( tab.onSelection() != null ) {
            _component.addChangeListener(e -> {
                if ( index == _component.getSelectedIndex() )
                    tab.onSelection().accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
            });
        }
        MouseListener mouseListener =
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        if ( index == _component.getSelectedIndex() )
                            tab.onMouseClick().accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
                    }
                };

        if ( tab.onMouseClick() != null )
            _component.addMouseListener( mouseListener );

        _component.addTab(tab.title(), tab.icon(), tab.contents(), tab.tip());
        if ( tab.headerContents() != null ) {
            JComponent header = tab.headerContents();
            if ( tab.title() != null ) {
                // We want both title and user component in the header!
                header =
                    UI.panel("fill, ins 0").withBackground(new Color(0,0,0,0))
                    .doIf( tab.tip() != null, it->it.withTooltip(tab.tip()) )
                    .doIf( tab.onMouseClick() != null, it->it.getComponent().addMouseListener(mouseListener) )
                    .add("shrink",
                        UI.label(tab.title()).withBackground(new Color(0,0,0,0))
                        .doIf( tab.tip()!=null, it->it.withTooltip(tab.tip()) )
                        .doIf( tab.onMouseClick() != null, it->it.getComponent().addMouseListener(mouseListener) )
                    )
                    .add("grow", tab.headerContents())
                    .getComponent();
            }
            _component.setTabComponentAt(index, header);
        }
        return this;
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

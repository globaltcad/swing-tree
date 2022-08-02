package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        if ( tab.onSelection() != null ) {
            int index = _component.getTabCount();
            _component.addChangeListener(e -> {
                if ( index == _component.getSelectedIndex() )
                    tab.onSelection().accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
            });
        }
        _component.addTab(tab.title(), tab.icon(), tab.contents(), tab.tip());
        return this;
    }

    public final UIForTabbedPane<P> onChange(UIAction<SimpleDelegate<P, ChangeEvent>> onChange) {
        LogUtil.nullArgCheck(onChange, "onChange", UIAction.class);
        _component.addChangeListener(e -> onChange.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())));
        return this;
    }

}

package swingtree;

import sprouts.Val;

import javax.swing.*;

public class UIForToolBar<T extends JToolBar> extends UIForAnySwing<UIForToolBar<T>, T>
{
    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForToolBar(T component) { super(component); }


    public final UIForToolBar<T> with(UI.Align alignment) {
        NullUtil.nullArgCheck(alignment, "alignment", UI.Align.class);
        getComponent().setOrientation(alignment.forToolBar());
        return this;
    }

    public final UIForToolBar<T> withAlignment( Val<UI.Align> alignment ) {
        NullUtil.nullArgCheck(alignment, "alignment", Val.class);
        NullUtil.nullPropertyCheck(alignment, "alignment", "Null is not a valid alignment.");
        _onShow( alignment, a -> with(a) );
        return with(alignment.get());
    }

}

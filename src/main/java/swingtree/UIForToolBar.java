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
    protected UIForToolBar(T component) { super(component); }

    /**
     * @param alignment The {@link UI.Align} value mapping to the {@link JToolBar}'s orientation.
     *                  See {@link JToolBar#setOrientation(int)}.
     * @return This builder node.
     */
    public final UIForToolBar<T> withOrientation( UI.Align alignment ) {
        NullUtil.nullArgCheck(alignment, "alignment", UI.Align.class);
        return _with( thisComponent -> {
                    thisComponent.setOrientation(alignment.forToolBar());
                })
                ._this();
    }

    /**
     * @param alignment The {@link UI.Align} property mapping to the {@link JToolBar}'s orientation.
     *                  See {@link JToolBar#setOrientation(int)}.
     * @return This builder node.
     */
    public final UIForToolBar<T> withOrientation( Val<UI.Align> alignment ) {
        NullUtil.nullArgCheck(alignment, "alignment", Val.class);
        NullUtil.nullPropertyCheck(alignment, "alignment", "Null is not a valid alignment.");
        return _with( thisComponent -> {
                    _onShow( alignment, a -> thisComponent.setOrientation(a.forToolBar()) );
                    thisComponent.setOrientation(alignment.orElseThrow().forToolBar());
                })
                ._this();
    }

}

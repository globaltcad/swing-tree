package swingtree;

import sprouts.Val;

import javax.swing.*;

public final class UIForToolBar<T extends JToolBar> extends UIForAnySwing<UIForToolBar<T>, T>
{
    private final BuilderState<T> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    UIForToolBar(T component) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<T> _state() {
        return _state;
    }

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
        return _withOnShow( alignment, (c,v) -> {
                    c.setOrientation(v.forToolBar());
                })
                ._with( thisComponent -> {
                    thisComponent.setOrientation(alignment.orElseThrow().forToolBar());
                })
                ._this();
    }

}

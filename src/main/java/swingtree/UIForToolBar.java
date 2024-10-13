package swingtree;

import sprouts.Val;

import javax.swing.*;
import java.util.Objects;

public final class UIForToolBar<T extends JToolBar> extends UIForAnySwing<UIForToolBar<T>, T>
{
    private final BuilderState<T> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForToolBar( BuilderState<T> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<T> _state() {
        return _state;
    }
    
    @Override
    protected UIForToolBar<T> _newBuilderWithState(BuilderState<T> newState ) {
        return new UIForToolBar<>(newState);
    }

    /**
     *  Use this to set the orientation of the {@link JToolBar},
     *  which is a layout mode that is either horizontal or vertical.
     *  It translates to a call to {@link JToolBar#setOrientation(int)}.
     *
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
     *  Dynamically determines the orientation of the {@link JToolBar} based on the value of the given {@link Val},
     *  which means that whenever the value of the {@link Val} changes, the orientation of the {@link JToolBar} will change.
     *  This translates to a call to {@link JToolBar#setOrientation(int)}.
     *  The orientation must have either the value HORIZONTAL or VERTICAL.
     *
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
                    thisComponent.setOrientation(alignment.orElseThrowUnchecked().forToolBar());
                })
                ._this();
    }

}

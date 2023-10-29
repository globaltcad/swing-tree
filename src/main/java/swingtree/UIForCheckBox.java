package swingtree;

import sprouts.Val;

import javax.swing.JCheckBox;

/**
 *  A SwingTree builder node designed for configuring {@link JCheckBox} instances.
 */
public final class UIForCheckBox<B extends JCheckBox> extends UIForAnyButton<UIForCheckBox<B>, B>
{
    private final BuilderState<B> _state;

    UIForCheckBox( B component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<B> _state() {
        return _state;
    }


    public UIForCheckBox<B> borderIsPaintedFlatIf(boolean borderPainted ) {
        return _with( thisComponent -> {
                   thisComponent.setBorderPaintedFlat(borderPainted);
               })
               ._this();
    }

    public UIForCheckBox<B> borderIsPaintedFlatIf(Val<Boolean> val ) {
        return _withOnShow( val, (thisComponent,v) -> {
                    thisComponent.setBorderPaintedFlat(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setBorderPaintedFlat( val.get() );
                })
               ._this();
    }

}

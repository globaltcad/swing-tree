package examples.mvvm;

import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;

import javax.swing.text.View;

public class SomeSettingsViewModel
{
    public enum Type
    {
        X, Y, Z
    }

    public enum Orientation
    {
        CLOCKWISE, COUNTER_CLOCKWISE
    }

    private final Var<Boolean> hasType = Var.of(false);
    private final Var<Type> type = Var.of(Type.Z);
    private final Var<Boolean> somethingEnabled = Var.of(false);

    private final Var<Boolean> somethingVisible = Var.of(false);

    private final Var<Boolean> flipped = Var.of(false);

    private final Var<Orientation> orientation = Var.of(Orientation.CLOCKWISE);
    private final Var<Double> speed = Var.of(42.0);
    private final Var<Boolean> speedIsValid = Var.of(true);

    public SomeSettingsViewModel() {
        Viewable.cast(hasType).onChange(From.VIEW, it -> verifyEnabled());
        Viewable.cast(speed).onChange(From.VIEW, it -> speed().fireChange(From.VIEW_MODEL) );
    }

    public Var<Boolean> hasType() { return hasType; }
    public Var<Type> type() { return type; }
    public Var<Boolean> somethingEnabled() { return somethingEnabled; }
    public Var<Boolean> somethingVisible() { return somethingVisible; }
    public Var<Boolean> flipped() { return flipped; }
    public Var<Orientation> orientation() { return orientation; }
    public Var<Double> speed() { return speed; }
    public Var<Boolean> speedIsValid() { return speedIsValid; }

    public void verifyEnabled() {
        if ( hasType.get() )
            somethingEnabled.set(true);
        else
            somethingEnabled.set(false);
    }

    public void apply() {
        // TODO!
    }

}

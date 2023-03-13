package example;

import sprouts.Var;

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

    private final Var<Boolean> hasType = Var.of(false).onAct(it-> verifyEnabled());
    private final Var<Type> type = Var.of(Type.Z);
    private final Var<Boolean> somethingEnabled = Var.of(false);

    private final Var<Boolean> somethingVisible = Var.of(false);

    private final Var<Boolean> flipped = Var.of(false);

    private final Var<Orientation> orientation = Var.of(Orientation.CLOCKWISE);
    private final Var<Double> speed = Var.of(42.0).onAct( it -> speed().fireSet() );
    private final Var<Boolean> speedIsValid = Var.of(true);

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

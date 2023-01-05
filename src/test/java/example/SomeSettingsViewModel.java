package example;

import swingtree.api.mvvm.Var;

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

    public Var<Boolean> hasType() { return hasType; }
    public Var<Type> type() { return type; }
    public Var<Boolean> somethingEnabled() { return somethingEnabled; }
    public Var<Boolean> somethingVisible() { return somethingVisible; }
    public Var<Boolean> flipped() { return flipped; }
    public Var<Orientation> orientation() { return orientation; }

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

package example.styles;

import sprouts.Var;

public class IpdAttributeVM<T>
{
    private final Var<T> value;

    private final Var<Boolean> isKnown = Var.of(true);

    private final Var<Boolean> isEnabled = Var.of(true);

    private final Var<Boolean> isValid = Var.of(true);
    private final Var<Boolean> wasUserModified = Var.of(false);

    public IpdAttributeVM(boolean isKnown, Var<T> value) {
        this.isKnown.set(isKnown);
        this.value = value;
    }

    public final Var<T> value() { return this.value; }

    public final Var<Boolean> isKnown() { return this.isKnown; }

    public final Var<Boolean> isEnabled() { return this.isEnabled; }

    public final Var<Boolean> isValid() { return this.isValid; }
}

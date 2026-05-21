package examples.team.mvvm;

import sprouts.Var;

/**
 *  An occupation as a small classical-MVVM view model: each field is a
 *  mutable {@link Var}; consumers observe and mutate them directly.
 *  Contrast with the MVI / MVL flavor in the sibling package, where the
 *  same data is modelled as an immutable record and edited through lenses.
 */
final class Occupation {

    private final Var<String> name;
    private final Var<String> description;

    Occupation(String name, String description) {
        this.name        = Var.of(name);
        this.description = Var.of(description);
    }

    public Var<String> name()        { return name;        }
    public Var<String> description() { return description; }
}
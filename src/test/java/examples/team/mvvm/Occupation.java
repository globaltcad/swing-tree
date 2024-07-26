package examples.team.mvvm;

import sprouts.Var;

final class Occupation {
    private final Var<String> name = Var.of("");
    private final Var<String> description = Var.of("");

    Occupation(String name, String description) {
        this.name.set(name);
        this.description.set(description);
    }

    public Var<String> name() {
        return name;
    }

    public Var<String> description() {
        return description;
    }
}

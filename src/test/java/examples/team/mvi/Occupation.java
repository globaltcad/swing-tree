package examples.team.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Occupation {

    private final String name;
    private final String description;

    public Occupation() { this("", ""); }

    public static Occupation of(String name, String description) {
        return new Occupation(name, description);
    }
}
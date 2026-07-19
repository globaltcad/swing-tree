package examples.team.mvi;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;

import java.util.UUID;

/**
 *  A team member, modelled as an immutable value object.
 *
 *  <p>Implements {@link HasId} so SwingTree's <code>addAll(Var&lt;Tuple&gt;, ...)</code>
 *  knows which sub-view belongs to which person — without an explicit id, two
 *  members with the same name and age would be indistinguishable to the
 *  binding machinery (because records / value objects derive identity from
 *  their contents).</p>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Person implements HasId<UUID> {

    private final UUID       id;
    private final String     name;
    private final int        age;
    private final String     bio;
    private final Occupation occupation;
    private final Gender     gender;

    public Person() {
        this(UUID.randomUUID(), "", 0, "", new Occupation(), Gender.DIVERSE);
    }

    public static Person of(String name, int age, String bio, Occupation occupation, Gender gender) {
        return new Person(UUID.randomUUID(), name, age, bio, occupation, gender);
    }

    /** Initials extracted from {@link #name()} — at most two letters, upper-cased. */
    public String initials() {
        if ( name == null || name.isEmpty() ) return "?";
        String[] parts = name.trim().split("\\s+", -1);
        if ( parts.length == 1 ) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }
}
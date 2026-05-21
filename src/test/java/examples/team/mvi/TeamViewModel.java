package examples.team.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import sprouts.Tuple;

import java.util.Optional;
import java.util.UUID;

/**
 *  The top-level immutable state of the people directory view.
 *
 *  <p>It pairs the {@link Team} domain object with one purely UI-side piece
 *  of state: which member is currently selected for editing in the right
 *  pane. Keeping the selection out of {@link Team} keeps {@code Team} a
 *  clean value object that's safe to persist or send over the wire.</p>
 *
 *  <p>Construction is via {@link #initialDirectory()} which builds a seed
 *  team of five plausible-looking members and pre-selects the lead.</p>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class TeamViewModel {

    private final Team           team;
    private final @Nullable UUID selectedMemberId;

    /** Builds a five-member seed team, with one member pre-selected as lead. */
    public static TeamViewModel initialDirectory() {
        Person marry = Person.of("Marry Tisdale",  32,
                "Loves whiteboards and quiet mornings.",
                Occupation.of("Architect",     "Designs the long-lived shape of the system."),
                Gender.FEMALE);
        Person sam   = Person.of("Sam Okafor",     28,
                "Will find the one corner case you forgot.",
                Occupation.of("Quality Engineer", "Owns the test plan and the bug board."),
                Gender.MALE);
        Person alex  = Person.of("Alex Rivera",    27,
                "Half engineer, half historian of regressions.",
                Occupation.of("Engineer",      "Builds, ships, then refactors."),
                Gender.DIVERSE);
        Person jin   = Person.of("Jin Park",       35,
                "Translates fuzzy ideas into clear specs.",
                Occupation.of("Product Lead",  "Owns the why; helps the team own the how."),
                Gender.MALE);
        Person lina  = Person.of("Lina Fischer",   30,
                "Has strong opinions about kerning.",
                Occupation.of("Designer",      "Cares about how the product feels."),
                Gender.FEMALE);

        Team team = new Team("Research", marry.id(),
                Tuple.of(Person.class, marry, sam, alex, jin, lina));

        return new TeamViewModel(team, marry.id());
    }

    /** The currently-selected member, if any. */
    public Optional<Person> selectedMember() {
        if ( selectedMemberId == null ) return Optional.empty();
        return team.members().stream()
                   .filter(p -> selectedMemberId.equals(p.id()))
                   .findFirst();
    }

    public TeamViewModel select(@Nullable UUID id) {
        return withSelectedMemberId(id);
    }

    public TeamViewModel promote(UUID id) {
        return withTeam(team.withLeadId(id));
    }

    public TeamViewModel addBlankMember() {
        Person fresh = new Person().withName("New member");
        TeamViewModel after = withTeam(team.addMember(fresh));
        return after.withSelectedMemberId(fresh.id());
    }

    public TeamViewModel removeMember(UUID id) {
        Team   newTeam     = team.removeMember(id);
        UUID   newSelected = id.equals(selectedMemberId) ? firstMemberId(newTeam) : selectedMemberId;
        return new TeamViewModel(newTeam, newSelected);
    }

    private static @Nullable UUID firstMemberId(Team t) {
        return t.members().isEmpty() ? null : t.members().get(0).id();
    }
}
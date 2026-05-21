package examples.team.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;

import java.util.Optional;
import java.util.UUID;

/**
 *  A team — a name plus a tuple of {@link Person} members, with one of those
 *  members optionally designated as the lead by id.
 *
 *  <p>Storing the lead as a {@code UUID} reference into the members tuple
 *  (rather than as a separate slot) keeps the model normalised: every
 *  person lives in exactly one place, and "promote to lead" is just a
 *  one-line update on {@link #leadId()}.</p>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Team {

    private final String         name;
    private final UUID           leadId;
    private final Tuple<Person>  members;

    public Team() { this("", null, Tuple.of(Person.class)); }

    /** The lead member, looked up by {@link #leadId()}, or empty if there is no lead. */
    public Optional<Person> lead() {
        if ( leadId == null ) return Optional.empty();
        return members.stream().filter(p -> leadId.equals(p.id())).findFirst();
    }

    public boolean isLead(Person p) {
        return p != null && p.id().equals(leadId);
    }

    public Team addMember(Person p) {
        return withMembers(members.add(p));
    }

    public Team removeMember(UUID id) {
        Tuple<Person> kept = members.removeIf(p -> p.id().equals(id));
        UUID newLead = id.equals(leadId) ? null : leadId;
        return new Team(name, newLead, kept);
    }
}

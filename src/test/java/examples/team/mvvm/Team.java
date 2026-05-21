package examples.team.mvvm;

import org.jspecify.annotations.Nullable;
import sprouts.Var;
import sprouts.Vars;

/**
 *  A team in the classical MVVM flavor — a small graph of mutable
 *  {@link Var} / {@link Vars} properties owned by the view model itself.
 *
 *  <p>This is the deliberate twin of the immutable, record-based
 *  {@code examples.team.mvi.Team} in the sibling package: both render an
 *  almost-identical UI, but the wiring is different. Here the roster is a
 *  {@link Vars Vars&lt;Person&gt;} observable list, the lead is a nullable
 *  {@code Var<Person>}, and member fields are mutated in place by the UI.</p>
 */
final class Team {

    private final Var<String>    name;
    private final Vars<Person>   members         = Vars.of(Person.class);
    private final Var<@Nullable Person> lead             = Var.ofNull(Person.class);
    private final Var<@Nullable Person> selectedMember   = Var.ofNull(Person.class);

    public Team(String name) {
        this.name = Var.of(name);
    }

    public Var<String>           name()           { return name;           }
    public Vars<Person>          members()        { return members;        }
    public Var<@Nullable Person> lead()           { return lead;           }
    public Var<@Nullable Person> selectedMember() { return selectedMember; }

    // ── Mutation helpers ────────────────────────────────────────────────────
    //
    // Each helper applies a small atomic change to one or more properties.
    // The UI observes the properties and updates itself; no explicit refresh
    // calls are needed anywhere.

    public void addMember(Person p) {
        members.add(p);
    }

    public void addBlankMember() {
        Person fresh = new Person("New member", 0, "", new Occupation("", ""), Gender.DIVERSE);
        members.add(fresh);
        selectedMember.set(fresh);
    }

    public void removeMember(Person p) {
        members.remove(p);
        if ( p.equals(lead.orElseNull()) )           lead.set(null);
        if ( p.equals(selectedMember.orElseNull()) ) selectedMember.set(members.isEmpty() ? null : members.at(0).get());
    }

    public void promote(Person p) {
        lead.set(p);
    }
}
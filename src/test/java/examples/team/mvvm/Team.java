package examples.team.mvvm;

import sprouts.Var;

final class Team {
    private final Var<String> id = Var.of("");
    private final Person lead;
    private final Person firstMember;
    private final Person secondMember;

    public Team(String id, Person lead, Person firstMember, Person secondMember) {
        this.id.set(id);
        this.lead = lead;
        this.firstMember = firstMember;
        this.secondMember = secondMember;
    }

    public Var<String> id() {
        return id;
    }

    public Person lead() {
        return lead;
    }

    public Person firstMember() {
        return firstMember;
    }

    public Person secondMember() {
        return secondMember;
    }
}

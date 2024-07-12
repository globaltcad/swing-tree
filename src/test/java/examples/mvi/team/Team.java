package examples.mvi.team;

final class Team {
    private final String id;
    private final Person lead;
    private final Person firstMember;
    private final Person secondMember;

    public Team(String id, Person lead, Person firstMember, Person secondMember) {
        this.id = id;
        this.lead = lead;
        this.firstMember = firstMember;
        this.secondMember = secondMember;
    }

    public String getId() {
        return id;
    }

    public Person getLead() {
        return lead;
    }

    public Person getFirstMember() {
        return firstMember;
    }

    public Person getSecondMember() {
        return secondMember;
    }

    public Team withId(String id) {
        return new Team(id, lead, firstMember, secondMember);
    }

    public Team withLead(Person lead) {
        return new Team(id, lead, firstMember, secondMember);
    }

    public Team withFirstMember(Person firstMember) {
        return new Team(id, lead, firstMember, secondMember);
    }

    public Team withSecondMember(Person secondMember) {
        return new Team(id, lead, firstMember, secondMember);
    }
}

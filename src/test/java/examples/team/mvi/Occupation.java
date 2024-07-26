package examples.team.mvi;

final class Occupation {
    private final String name;
    private final String description;

    Occupation(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Occupation withName(String name) {
        return new Occupation(name, description);
    }

    public Occupation withDescription(String description) {
        return new Occupation(name, description);
    }
}

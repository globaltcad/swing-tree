package examples.team.mvvm;

import java.awt.Color;

enum Gender {
    FEMALE  ("She/her",   new Color(244, 132, 168)),
    MALE    ("He/him",    new Color(120, 176, 238)),
    DIVERSE ("They/them", new Color(160, 220, 170));

    private final String pronouns;
    private final Color  accent;

    Gender(String pronouns, Color accent) {
        this.pronouns = pronouns;
        this.accent   = accent;
    }

    public String pronouns() { return pronouns; }
    public Color  accent()   { return accent;   }
}
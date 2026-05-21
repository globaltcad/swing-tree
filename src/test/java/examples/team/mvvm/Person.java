package examples.team.mvvm;

import sprouts.Var;

import java.util.UUID;

/**
 *  A team member as a classical-MVVM view model: every field is a mutable
 *  {@link Var}. The {@link #id} is final and immutable — it's the stable
 *  identity used by the UI to tell one card from another when names happen
 *  to collide.
 */
final class Person {

    private final UUID         id;
    private final Var<String>  name;
    private final Var<Integer> age;
    private final Var<String>  bio;
    private final Occupation   occupation;
    private final Var<Gender>  gender;

    public Person(String name, int age, String bio, Occupation occupation, Gender gender) {
        this.id         = UUID.randomUUID();
        this.name       = Var.of(name);
        this.age        = Var.of(age);
        this.bio        = Var.of(bio);
        this.occupation = occupation;
        this.gender     = Var.of(gender);
    }

    public UUID id()                 { return id;         }
    public Var<String>  name()       { return name;       }
    public Var<Integer> age()        { return age;        }
    public Var<String>  bio()        { return bio;        }
    public Occupation   occupation() { return occupation; }
    public Var<Gender>  gender()     { return gender;     }

    /** Initials extracted from {@link #name()} — at most two letters, upper-cased. */
    public String initials() {
        String n = name.get();
        if ( n == null || n.isEmpty() ) return "?";
        String[] parts = n.trim().split("\\s+");
        if ( parts.length == 1 ) return parts[0].substring(0, 1).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
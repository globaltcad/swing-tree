package examples.team.mvvm;

import sprouts.Var;

final class Person {
    private final Var<String> name = Var.of("");
    private final Var<Integer> age = Var.of(0);
    private final Occupation occupation;
    private final Var<Gender> gender = Var.of(Gender.DIVERSE);

    public Person(String name, int age, Occupation occupation, Gender gender) {
        this.name.set(name);
        this.age.set(age);
        this.occupation = occupation;
        this.gender.set(gender);
    }

    public Var<String> name() {
        return name;
    }

    public Var<Integer> age() {
        return age;
    }

    public Occupation occupation() {
        return occupation;
    }

    public Var<Gender> gender() {
        return gender;
    }
}

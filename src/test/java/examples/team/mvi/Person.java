package examples.team.mvi;

final class Person {
    private final String name;
    private final int age;
    private final Occupation occupation;
    private final Gender gender;


    public Person(String name, int age, Occupation occupation, Gender gender) {
        this.name = name;
        this.age = age;
        this.occupation = occupation;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Occupation getOccupation() {
        return occupation;
    }

    public Person withName(String name) {
        return new Person(name, age, occupation, gender);
    }

    public Person withAge(int age) {
        return new Person(name, age, occupation, gender);
    }

    public Person withOccupation(Occupation occupation) {
        return new Person(name, age, occupation, gender);
    }

    public Person withGender(Gender gender) {
        return new Person(name, age, occupation, gender);
    }
}

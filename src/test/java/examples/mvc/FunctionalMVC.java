package examples.mvc;

import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;

import javax.swing.JPanel;

public class FunctionalMVC
{
    private static class Occupation {
        private final String name;
        private final String description;

        private Occupation(String name, String description) {
            this.name = name;
            this.description = description;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Occupation withName(String name) { return new Occupation(name, description); }
        public Occupation withDescription(String description) { return new Occupation(name, description); }
    }

    public enum Gender {
        MALE, FEMALE, DIVERSE
    }

    private static class Person {
        private final String name;
        private final int    age;
        private final Occupation occupation;
        private final Gender gender;


        public Person(String name, int age, Occupation occupation, Gender gender ) {
            this.name = name;
            this.age = age;
            this.occupation = occupation;
            this.gender = gender;
        }
        public String getName() { return name; }
        public int getAge() { return age; }
        public Occupation getOccupation() { return occupation; }
        public Person withName(String name ) { return new Person(name, age, occupation, gender); }
        public Person withAge(int age ) { return new Person(name, age, occupation, gender); }
        public Person withOccupation(Occupation occupation ) { return new Person(name, age, occupation, gender); }
        public Person withGender(Gender gender) { return new Person(name, age, occupation, gender); }
    }

    private static class Team {
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
        public String getId() { return id; }
        public Person getLead() { return lead; }
        public Person getFirstMember() { return firstMember; }
        public Person getSecondMember() { return secondMember; }
        public Team withId(String id ) { return new Team(id, lead, firstMember, secondMember); }
        public Team withLead( Person lead ) { return new Team(id, lead, firstMember, secondMember); }
        public Team withFirstMember( Person firstMember ) { return new Team(id, lead, firstMember, secondMember); }
        public Team withSecondMember( Person secondMember ) { return new Team(id, lead, firstMember, secondMember); }
    }


    public static void main(String... args) {
        Var<Team> team = Var.ofNull(Team.class);
        team.set(
            new Team(
                "Dev",
                new Person("Marry", 32, new Occupation("Architect", "Cool"), Gender.FEMALE),
                new Person("Sam", 28, new Occupation("Quality", "Cool"), Gender.MALE),
                new Person("Alex", 27, new Occupation("engineering", "Cool"), Gender.DIVERSE)
            )
        );
        UI.show( f -> createGuiFor(team));
    }

    public static JPanel createView() {
        Var<Team> team = Var.ofNull(Team.class);
        team.set(
                new Team(
                        "Dev",
                        new Person("Marry", 32, new Occupation("Architect", "Cool"), Gender.FEMALE),
                        new Person("Sam", 28, new Occupation("Quality", "Cool"), Gender.MALE),
                        new Person("Alex", 27, new Occupation("engineering", "Cool"), Gender.DIVERSE)
                )
        );
        return createGuiFor(team);
    }

    private static JPanel createGuiFor( Var<Team> team ) {
        return
            UI.panel("fillx").withPrefSize(600, 650)
            .add("growx",
                UI.panel("fillx, flowx")
                .add("growx, span, wrap",
                    UI.panel().withStyle( it -> it.borderAt(UI.Edge.BOTTOM, 3, UI.Color.BLACK) )
                    .add(UI.html("<h1>Team:</h1>"))
                    .add(UI.label(team.get().getId()).withFontSize(20))
                )
                .add("growx, span, wrap",
                    UI.panel("fill, wrap 1")
                    .add(UI.label("Lead: "))
                    .add("growx",personUIFrom(team.zoomToNullable(Person.class, Team::getLead, Team::withLead)))
                )
                .add("growx",
                    UI.panel("fill, wrap 1")
                    .add(UI.label("First Member: "))
                    .add("growx",personUIFrom(team.zoomToNullable(Person.class, Team::getFirstMember, Team::withFirstMember)))
                )
                .add("growx",
                    UI.panel("fill, wrap 1")
                    .add(UI.label("Second Member: "))
                    .add("growx",personUIFrom(team.zoomToNullable(Person.class, Team::getSecondMember, Team::withSecondMember)))
                )
            )
            .get(JPanel.class);
    }

    private static UIForAnySwing<?,?> personUIFrom(Var<Person> person) {
        return UI.panel("fill, wrap 1").withMaxWidth(620)
                .withStyle( it -> it
                    .backgroundColor(UI.Color.LAVENDER)
                    .borderRadius(12)
                    .padding(24)
                    .border(1, UI.Color.BLACK)
                )
                .add("grow", UI.label(person.get().getName()))
                .add("growx",
                    UI.panel()
                    .add(UI.label("Name: "))
                    .add("growx, pushx",
                        UI.textField(person.get().getName())
                        .onKeyTyped( it ->
                            person.set(person.get().withName(it.get().getText()))
                        )
                    )
                    .add(UI.label("Age: "))
                    .add("growx, pushx, wrap",
                        UI.numericTextField(person.zoomTo(Person::getAge, Person::withAge))
                        .onKeyTyped( it -> {
                            try {
                                person.set(person.get().withAge(Integer.parseInt(it.get().getText())));
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        })
                    )
                    .add("span, wrap, gap top 12",
                        UI.label("Occupation: ")
                    )
                    .add("span, gap left 12",
                        occupationUIFrom(person.zoomTo(Person::getOccupation, Person::withOccupation))
                    )
                );

    }

    private static UIForAnySwing<?,?> occupationUIFrom(Var<Occupation> occupation) {
        return
            UI.panel("wrap 2")
            .add(UI.label("Name: "))
            .add("growx, pushx",
                UI.textField(occupation.zoomTo("", Occupation::getName, Occupation::withName))
                .onKeyTyped( it ->
                    occupation.set(occupation.get().withName(it.get().getText()))
                )
            )
            .add("span, wrap", UI.label("Description: "))
            .add("span, right",
                UI.html("<i>"+occupation.get().getDescription()+"</i>")
                .withMaxWidth(224)
            );
    }

}

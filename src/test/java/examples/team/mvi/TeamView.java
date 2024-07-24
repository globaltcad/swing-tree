package examples.team.mvi;

import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;

import javax.swing.JPanel;

/**
 *  A demonstration of the MVI design pattern using SwingTree
 *  and an underlying data structure representing a team
 *  for which the user can edit the members and their occupations.
 */
public class TeamView
{
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

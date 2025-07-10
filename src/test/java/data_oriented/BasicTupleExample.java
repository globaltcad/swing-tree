package data_oriented;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.*;
import swingtree.UI;

import java.util.UUID;

import static swingtree.UI.*;

public class BasicTupleExample {

    @With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode
    static class Team{final String name; final Tuple<Person> members;}
    @With @Getter @Accessors(fluent = true)  @AllArgsConstructor @EqualsAndHashCode
    static class Person implements HasId<UUID> {final UUID id; final String forename; String surname; final Address address;}
    @With @Getter @Accessors(fluent = true)  @AllArgsConstructor @EqualsAndHashCode
    static class Address{final String street; final int postalCode;}


    @Getter
    static class TeamView extends Panel {
        public TeamView(Var<Team> team) {
            Var<String>        name    = team.zoomTo(Team::name,    Team::withName);
            Var<Tuple<Person>> members = team.zoomTo(Team::members, Team::withMembers);

            of(this).withLayout(FILL.and(WRAP(2)))
                .add(GROW, label("Team - "), textField(name))
                .addAll(GROW, members, (Var<Person> person)->{
                    return of(new PersonView(person));
                });
        }
    }

    @Getter static class PersonView extends Panel {
        public PersonView( Var<Person> person ) {
            Var<String>  forename   = person.zoomTo(Person::forename, Person::withForename);
            Var<String>  surname    = person.zoomTo(Person::surname,  Person::withSurname);
            Var<Address> address    = person.zoomTo(Person::address,  Person::withAddress);
            Var<String>  street     = address.zoomTo(Address::street,     Address::withStreet);
            Var<Integer> postalCode = address.zoomTo(Address::postalCode, Address::withPostalCode);

            of(this).withLayout(FILL.and(WRAP(2)))
            .add(SPAN, label("Name:"))
            .add(GROW, textField(forename), textField(surname))
            .add(SPAN, label("Address:"))
            .add(GROW, textField(street), numericTextField(postalCode));
        }
    }

    public static void main(String... args) {
        Var<Team> team = Var.of(new Team("",
                Tuple.of(
                    new Person(UUID.randomUUID(), "", "", new Address("", 0)),
                    new Person(UUID.randomUUID(), "", "", new Address("", 0)),
                    new Person(UUID.randomUUID(), "", "", new Address("", 0)),
                    new Person(UUID.randomUUID(), "", "", new Address("", 0))
                )
            ));
        UI.show(f->new TeamView(team));
        Viewable.cast(team).onChange(From.ALL, it -> {
            System.out.println(it.currentValue());
        });
    }
}

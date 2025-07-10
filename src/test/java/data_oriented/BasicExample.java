package data_oriented;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;


import static swingtree.UI.*;

public class BasicExample {

    @With @Getter @Accessors(fluent = true)  @AllArgsConstructor @EqualsAndHashCode
    static class Person {final String forename; String surname; final Address address;}
    @With @Getter @Accessors(fluent = true)  @AllArgsConstructor @EqualsAndHashCode
    static class Address{final String street; final int postalCode;}


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
        Var<Person> person = Var.of(new Person("", "", new Address("", 0)));
        UI.show(f->new PersonView(person));
        Viewable.cast(person).onChange(From.ALL, it -> {
            System.out.println(it.currentValue());
        });
    }

}

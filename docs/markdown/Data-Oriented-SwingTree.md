
# Data Oriented Programming & SwingTree

---
SwingTree is built with modern Java features and development
techniques in mind. More specifically: **Data Oriented Programming**

In this guide, we first explore **Data Oriented Programming (DOP)**,
a hybrid paradigm that blends object-oriented programming (OOP) with
functional programming (FP) concepts.
Modern Java (especially from version 21 onward) is steadily
embracing this way of programming through features like **records**,
**pattern matching**, **sealed types**, and more.

More specifically, DOP based modeling produces software
that is concise, more performant, and easier to reason about.
Here, we show you how this paradigm can be applied to GUI development
with **SwingTree**, using **Sprouts** to maintain a seamless bridge
between immutable data and dynamic as well as reactive UI components
through **the lens pattern.**

## So, what is Data Oriented Programming?

Generally speaking, Data Oriented Programming is about **using objects with value 
semantics instead of stateful and reference identity-based objects**. 
More specifically, it is about using these so-called
*value objects*, like records or value classes, as your domain models and then run them
through a series of transformations, which constitute your business logic.

However, DOP is not anti-OOP, it simply reimagines most objects with a heavy 
emphasis on **value semantics**, immutability, and based around algebraic data types.
The goal is to take the composability and predictability of functional programming 
and bring it into the world of Java, using:

- **Records**: concise, immutable data carriers.
- **Pattern Matching**: more powerful, expressive data decomposition.
- **Sealed Interfaces**: controlled type hierarchies, enabling transparent polymorphism.
- **Virtual Threads and Structured Concurrency**: improving dataflow-based concurrency.

## The Why!

### Why is it called “Data” Oriented?

At first glance, all programming seems data-oriented.
Aren’t we programmers already working with "data" all the time???

Well... the answer is: **not quite**. Not unless you’re:

- Regularly using immutable types like records,
- Writing domain models with all `final` fields,
- Overriding `equals()` and `hashCode()` for deep equality,
- Comparing things based on value and not reference equality.

### Why is that?

Because traditional Java objects are **abstractions over *places***
in system memory, instead of the data that they hold!
**Consider two styles:**

#### Place Oriented:

```java
import lombok.Getter;
import lombok.Setter;

@Getter @Setter class Person {
    private String forename;
    private String surname;
    private Address address;
}
@Getter @Setter class Address {
    private String street;
    private int postalCode;
}
```

#### Data (Value) Oriented:

```java
import lombok.With;

@With record Person(
    String forename, 
    String surname, 
    Address address
) {}
@With record Address(
    String street, 
    int postalCode
) {}
```

From the above code snippets we can extract the following table of properties:

| **Object**     | **Identity**    | **Updates**     | **Location** | **Owners**    | **Encapsulation**                  |
|----------------|-----------------|-----------------|--------------|---------------|------------------------------------|
| Place Oriented | Reference Based | Destructive     | Defined      | Should be Few | Important to protect mutable parts |
| Data Oriented  | Value Based     | Non-Destructive | Undefined    | Any           | Usually Pointless                  |
Explanation:

Place Oriented objects imply the existence of a singular place in 
system memory, identified by a reference to it (reference identity).
The data at this place can change destructively (it can be overridden), 
requiring additional protection using classical OOP design 
techniques like encapsulation.
For example, mutable objects like the ones we see in old Java
code are centered around getters and setters, 
which can modify internal state instead of exposing mutable
variables directly to the outside world...

Data-Oriented objects, also referred to as "Value Objects",
on the other hand, do not make claims about
their location in memory and how updates are handled because **they
give up reference identity in exchange for value identity.**
If an object is mutable, then this mutability requires
a specific location in memory. The location then becomes the
identity of the object.

For immutable objects, on the other hand, we can safely express their
identity as their values, effectively making the reference identity obsolete.

Not only does this simplify how we developers think about objects,
it also gives the runtime a lot more freedom when
it comes to memory management, as it can move, copy and share these
value objects much more easily.

Modern Java has great support for value-based objects through records.
In upcoming versions it will expand value semantics further
through project Valhalla, which allows us to give up reference
identity entirely!

## Sum Types and Pattern Matching

Java has actively evolved towards working data/value objects
through dedicated languages features like pattern matching and records.
Pattern matching allows you to deconstruct your data without boilerplate
code and in a way where you can write operations to be more functional and
pipeline-like.

Another important feature adjacent to this is the introduction of **algebraic data types**
through records, sealed interfaces and then again records implementing these sealed interfaces!
ADTs sound convoluted, but they are really just a tool for modeling data.
There are two essential algebraic data types:
1. sum types (where a value can be one of several types)
2. and product types (where a value is a combination of multiple fields).

Sum types are particularly useful in DOP for expressing different 
possibilities within your data model in a clean and type-safe way.

You can think of it as polymorphism but for value types!

Take a look at this example:

**Sealed Type in Java:**
```java
sealed interface Shape { 
    double area();
    
    record Circle(double radius) implements Shape {/*implementation*/}
    record Rectangle(double width, double height) implements Shape {/*implementation*/}
    record Line(double length, double width) implements Shape {/*implementation*/}
    record Custom(DynamicShape aTraditionalInterface) implements Shape {/*implementation*/}
}

record Cursor(double x, double x, Shape shape){}

static BufferedImage render(Cursor cursor) {
    return switch (cursoir.shape()) {
        case Circle c -> renderCircle(c.radiues());
        case Rectangle r -> renderRectangle(r.width(), r.height());
        case Line l -> renderLine(cursor.length(), cursor.width());
        case Custom(var shape) -> shape.render();
    };
}
```

The above code snippet the sealed `Shape` interface defines a 
closed hierarchy in which all possible subclasses are known at compile time. 
This feature enhances type safety and enables the compiler to perform exhaustive checks, 
reducing the likelihood of unhandled cases. In the context of DOP, sealed types facilitate 
the modeling of data variants explicitly, making the code more predictable 
and easier to maintain.

You can think of sealed interface-based sum types as transparent polymorphism
especially well suited for value types (which is where it makes the most sense).
Traditional polymorphism relies on hiding mutable states in inheritance hierarchies.
In contrast, DOP embraces a more transparent polymorphism where it is ok to deliberately
down-cast through pattern matching instead of requiring all operations to be implemented
on the types themselves (although that is still an option, of course!).
This approach allows for behavior to be defined externally, promoting separation of 
concerns and enhancing code clarity.

**Then where to put operations? `area` vs `render`**

Deciding where to place functionality depends on the nature of the operation:

- Intrinsic Behavior: If the behavior is inherent to the data type, and it is a behavior that 
  makes sense at all usage sites, then it makes sense to define it within the type. 
  A classical example is a computed property, like the area of a shape (which depends on other variables).
- Extrinsic Behavior (e.g., render()): If the behavior involves external systems or 
  contexts (like rendering to a screen), it's better to define it outside the data type. 
  This separation keeps the data model clean and focused, adhering to the principles of DOP.

---

## Why is Data Oriented Programming a Good Idea?

When systems are modeled using raw data instead of abstractions over places 
(immutable values rather than mutable stateful objects), then a number of 
systemic advantages emerge!

**This includes:**
- Reduction of code complexity due to less side effects and simpler data flow
- It becomes much easier to write concurrent and async code
- Structural sharing reducing memory consumption
- Better CPU cache friendliness

In the link below, we explore the implications of data-oriented 
programming in more detail:

> [The Benefits of Data Oriented Programming](Data-Oriented-Programming-Benefits.md)

Now with all of this out of the way...
Let's finally see how to combine this paradigm with SwingTree!

---

## Using Sprouts

### 🌱 The Lens Pattern and Zooming into State

In SwingTree, we use the **Sprouts** library to connect the GUI and our domain models.
More specifically, it allows us to leverage the **lens pattern** to manage and 
interact with states based on immutable data structures. 
Generally speaking, a lens provides a focused view into a specific part of an 
immutable data structure, allowing both retrieval and updates to be done atomically. <br>
Sprout's `Var` properties support this out of the box!

The `Var::zoomTo(Function,BiFunction)` method is a practical implementation of this pattern. 
It creates a new `Var` that focuses on a specific field within the item of the
property it is derived from. This derived lens property is a more fine-grained 
handle for observing the state and updates of a specific nested field.

For example:

```java
Var<Person> person = Var.of(new Person("Thomas", "Schultz"));
Var<String> forename = person.zoomTo(Person::forename, Person::withForename);
```

Here, `forename` becomes a reactive handle of the `forename` field of the `Person` 
object in the parent property. The state of this so-called "lens property" is maintained and owned 
by the parent property, the `Var<Person> person`!
This means that any changes to the lens based `Var<String> forename` result in the creation of a 
new `Person` instance with the updated `forename` inside the `Var<Person> person`.
So a call to `forename.set("Lisa")` will lead to `person.get().forename().equals("Lisa")` being `true`.

This approach ensures that:

- **State is centralized**: The root `Var` (e.g., `person`) is the single source of truth.
- **Updates are composable**: Changes propagate through the lens hierarchy, maintaining consistency.
- **Immutability is preserved**: Each update results in a new `Person` object, avoiding side effects.

And finally, the most important benefit: **Frontend interoperability!**

--- 

GUIs are something the user changes in place, which means that at
their core, they imply an interaction with a mutable data structure.
And this is where we can use our lens properties instead of having to 
write mutable objects. **Essentially, this pattern frees us from
GUI code imposing place oriented data structures onto our backends.**

Take a look at this example view:

```java
import lombok.*;
import sprouts.Var;
import static swingtree.UI.*;

@With record Person(String forename, String surname, Address address) { }
@With record Address(String street, int postalCode) { }

@Getter class PersonView extends Panel {
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
    var person = Var.of(new Person("", "", new Address("", 0)));
    UI.show(new PersonView(person));
    Viewable.cast(person).onChange(From.ALL, it -> {
        System.out.println(it.currentValue());
    });
}
```

### 🔄 Automatic UI Binding

SwingTree declarations can automatically create bindings between Sprout's `Var`
properties and UI components to simplify development of the average GUI interface.
When a `Var` changes, then the UI updates accordingly, and vice versa.

In the `PersonView` for example, a call to:

```java
textField(forename)
```
...creates a `JTextField` whose text is bidirectionally bound to the `forename` lens property.
Here the user input updates are passed to the `Var`, and any programmatic changes to the `Var` are 
reflected back to the state of the UI. This bidirectional binding streamlines the synchronization 
between the UI and the underlying data model.


### 📋 Dynamic Collections with Tuple and Var

Managing collections of immutable objects is also straightforward by 
combining Sprouts's `Tuple` and `Var` types. 
A `Tuple` is an immutable list-like collection of ordered items, 
and when wrapped in a `Var`, **it becomes a reactive collection**.

Consider the following `Team` record:

```java
@With record Team(
        String name, 
        Tuple<Person> members
) {}
```

Just like we did before with the fields of a `Person`, we can also
zoom into the `members` of a `Team`:

```java
var team = Var.of(new Team("Research", Tuple.of(tim, lilly, lisa)));
Var<Tuple<Person>> members = team.zoomTo(Team::members, Team::withMembers);
```

This creates lens property of the `members` field of the `Team` object. 
Using this property, you can update and observer all `Person` instances
and even apply functional operations on them, like so:

```java
members.update(all -> all.map(person -> person.withAge(42)));
```

You can think of this tuple based property as an **observable list**.

### 🧩 Composable Views with addAll

SwingTree supports binding to tuple based properties through the `addAll` method,
which maintains dynamic UI components for each item in the tuple. 
It automatically handles the addition and removal of components as the underlying tuple changes.

```java
UI.panel(UI.FILL)
.addAll(UI.GROW, members, (Var<Person> person) -> {
    return UI.of(new PersonView(person));
})
```

This code dynamically generates a `PersonView` for each `Person` in the `members` collection.
As the `members` `Var` updates (e.g., adding or removing `Person` instances), 
the UI reflects these changes in real-time:

```java
import lombok.*;
import sprouts.HasId;
import sprouts.Var;
import sprouts.Tuple;
import static swingtree.UI.*;

@With record Team(String name, Tuple<Person> members) {}
@With record Person(UUID id, String forename, String surname, Address address) implements HasId<UUID> {}
@With record Address(String street, int postalCode) {}


@Getter
static class TeamView extends Panel {
    public TeamView(Var<Team> team) {
        Var<String>        name    = team.zoomTo(Team::name,    Team::withName);
        Var<Tuple<Person>> members = team.zoomTo(Team::members, Team::withMembers);

        of(this).withLayout(FILL.and(WRAP(2)))
            .add(GROW, label("Team - "), textField(name))
            .add(
                panel(FILL)
                .addAll(GROW, members, (Var<Person> person)->{
                    return of(new PersonView(person));
                })
            );
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
    var team = Var.of(new Team("",
            Tuple.of(
                new Person(UUID.randomUUID(), "", "", new Address("", 0)),
                new Person(UUID.randomUUID(), "", "", new Address("", 0)),
                new Person(UUID.randomUUID(), "", "", new Address("", 0)),
                new Person(UUID.randomUUID(), "", "", new Address("", 0))
            )
        ));
    UI.show(new TeamView(team));
    Viewable.cast(team).onChange(From.ALL, it -> {
        System.out.println(it.currentValue());
    });
}
```

Note that in this example there is an important different to the previous
example with respect to the `Person` type: **This time, there is a `UUID` in the person!**

More specifically, the `Person` record, now implements `sprouts.HasId<UUID>`.

This is an important requirement that SwingTree needs in order to facilitate the creation
and maintenance of components bound to tuple items. Remember, the big difference between
value objects and regular (mutable) objects is that values define their identity in terms
of their contents. This means that two value objects with the same contents are equal.
But this creates a problem when two items in a tuple suddenly become equal despite being
represented and bound to different GUI components. In that case, the GUI components no longer
know what to target. By implementing `sprouts.HasId`, we tell SwingTree to use the `id()`
attribute as a constant identifier for the binding mechanism.

---

## Final Thoughts

### 🧠 Embracing Data-Oriented Design

Data-Oriented Programming (DOP) encourages modeling applications around immutable data structures and pure functions. 
This approach offers several benefits:

- **Predictability**: Immutable data leads to more predictable and testable code.
- **Concurrency**: Stateless functions and immutable data simplify concurrent programming.
- **Maintainability**: Clear data models and separation of concerns enhance code maintainability.

### 🎨 Building Reactive UIs with SwingTree

SwingTree, combined with Sprouts, provides a powerful framework for building reactive and declarative UIs in Java:

- **Declarative syntax**: Define UI components and their behavior in a clear and concise manner.
- **Reactive data binding**: Automatically synchronize the UI with the underlying data model.
- **Immutable state management**: Leverage the benefits of immutability throughout the application.


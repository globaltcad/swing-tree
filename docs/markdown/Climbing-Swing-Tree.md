
# Climbing The Swing Tree #

<img src="../img/tutorial/climbing-swing-tree.png" style = "float: right; width: 30%; margin: 2em; -webkit-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); -moz-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75);">

Welcome to this Swing Tree tutorial! <br>
Here you will learn how to climb the tree, and Swing.

You do not need any prior knowledge of Swing to follow this tutorial.


## Planting a Seed ##

The first thing you need to do is to import the swing tree dependency into your project.
You can do this by adding the following to your `build.gradle` file
with a valid version number:

```groovy
 dependencies {
     compile 'io.github.globaltcad:swing-tree:x.y.z'
 }
```

Then inside your code you can import Swing-Tree by adding the following
import statement:

```java
 import swingtree.UI;
```

This is all you need to start building Swing UIs with the tree. :tada:

## Growing a Stem ##

No matter which UI framework you use, all UIs are built up from
a tree of components. Swing is no different, which is why the Swing-Tree
library allows you to build Swing UIs in a declarative as well as
HTML-like fashion.

This means that you can build your UI by describing and composing
components in a nested tree-like fashion and then let Swing-Tree
convert this tree into a Swing UI on the fly.

Here a little example UI that demonstrates this:

```java
 UI.show(
     UI.panel("wrap 1")
     .add(UI.label("Welcome to Swing-Tree!"))
     .add(
         UI.panel("wrap 2")
         .add(UI.label("Enter name:"))
         .add(UI.textField("John Doe"))
         .add(UI.label("Enter age:"))
         .add(UI.textField("42"))
     )
     .add(UI.button("Click to Swing!"))
 );
```

This code will create a Swing UI that looks like this:

<img src="../img/tutorial/first-step.png" style = "width: 10em; margin: 0.5em; -webkit-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); -moz-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75);">

As you can see, building UIs with Swing-Tree feels very similar 
to building HTML pages with HTML tags even though everything is
done in plain old Java code. This is made possible based on 2 
simple design patterns: 

1. The Composite Pattern
2. The Builder Pattern

## Growing Branches ##

The next important step to building Swing UIs with Swing-Tree is
mastering layout managers. 
Swing-Tree has a general purpose layout manager built into it
which is set as the default layout manager if you do not specify
a different one. 

If you know Swing you may already have heard of it:
**The one and only MigLayout**.

MigLayout is a very powerful layout manager that allows you to
specify the layout of your UI based on simple String keywords.
You have already seen this in the example above where we used
the `wrap 1` and `wrap 2` keywords to specify the number of
columns in the layout.

So let's consider the following example:

```java
 UI.show(
     UI.panel("wrap 4, fill, debug")
     .add(UI.label("A"))
     .add(UI.label("B"))
     .add(UI.label("C"))
     .add(UI.label("D"))
     .add(UI.label("E"))
     .add(UI.label("F"))
 );
```
Here we have specified the `wrap 4` keyword to tell the layout
manager to create 4 columns. 
This code will create a Swing UI that looks like this:

<img src="../img/tutorial/wrapping-up-letters-1.png" style = "width: 10em; margin: 0.5em; -webkit-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); -moz-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75);">

Note that we have added the `fill` and `debug` keywords to the
layout manager. The `fill` keyword tells the layout manager
to fill the available space in the container (in this case the window).
The `debug` keyword tells the layout manager to draw some nice borders around
the components to make it easier to see how exactly the layout manager
is arranging the components.

---

For more control over the layout you can also specify the
layout constraints for each component individually.
All you have to do is pass a `String` as the first argument the `add` method.

Here another little example UI:

```java
 UI.show(
     UI.panel("fill, debug")
     .add(UI.label("A"))
     .add("wrap", UI.label("B"))
     .add("span", UI.label("I should span the whole width!"))
     .add(UI.label("C"))
     .add(UI.label("D"))
     .add("wrap", UI.label("E"))
     .add(UI.label("F"))
 );
```

Which will create a Swing UI that looks like this:

<img src="../img/tutorial/wrapping-up-letters-2.png" style = "width: 17em; margin: 0.5em; -webkit-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); -moz-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75);">

Now this is a little bit more complicated than the previous example,
but understanding how the layout manager works is the key to
building beautiful Swing UIs.

Instead of specifying the layout constraints for the whole
container we have now specified the layout constraints for
each component individually.

The `wrap` keyword tells the layout manager to start a new row which
causes the next component to be placed on the next row.
You can think of it as a line break in HTML
or when you use the `println` method in Java instead of `print`.

The `span` keyword on the other hand tells the layout manager
to span the component over all existing columns and then
start a new row.
If you don't want to span over all columns you can specify
the number of columns you want to span over as the second
argument like this: `span 2`.

---

You can find more information about `MigLayout` and all of it's
cool layout keywords on the official
website: [http://www.miglayout.com/](http://www.miglayout.com/)

And if you don't want to use `MigLayout` don't worry,
Swing-Tree is based on Swing and therefore allows you to use
any other layout manager you want.
You can specify the layout manager for a component by using
the `withLayout` method.

## Growing Sprouts ##

Now that we have learned how to build Swing UIs with Swing-Tree
we can start looking at some of the more advanced features
that Swing-Tree provides.

Besides being able to build Swing UIs in a declarative fashion
you can also do clean `MVVM`/`MVC` and `MVP` application development.
These fancy `MVVM`/`MVC`/`MVP` shortcuts all stand for `Model`-`View`-`ViewModel/Controller/Presenter` 
which are all different design patterns for achieving the same
fundamental goal: 

**Separating the UI from the business logic!**

In Swing-Tree you can achieve this separation by using the 
**Sprouts property collection API**.
Properties are a simple yet powerful concept that allow you to
dynamically bind UI components to your business logic and vice versa.

Let's consider the following business logic, we will call it "view model" 
in accordance with the `MVVM` design and naming conventions:

```java
import sprouts.Var;

public class PersonViewModel {
    private final Var<String> firstName = Var.of("Joseph");
    private final Var<String> lastName = Var.of("Armstrong");
    private final Var<String> fullName  = Var.of("");
	
    public PersonViewModel() {
        firstName.onAct( it -> fullName.set(it + " " + lastName.get()) );
        lastName.onAct( it -> fullName.set(firstName.get() + " " + it) );
        fullName.set(firstName.get() + " " + lastName.get());
    }
    
    public Var<String> firstName() { return firstName; }
    public Var<String> lastName()  { return lastName;  }
    public Val<String> fullName()  { return fullName;  }
}
```

Properties are represented by the `sprouts.Var` and `sprouts.Val` classes.
They are wrapper objects for any kind of value whose
type you can specify using the generic type parameter.
The most important property type is `Var` type.
Which has both getters and setters for the wrapped value. 
The `Val` type on the other hand is an immutable property / read-only view of a `Var`
which allows you to design your view model API in a way which does
not leak mutable state to the outside world.

Now let's consider the following Swing UI:

```java
 var vm = new PersonViewModel();
 UI.show(
	UI.panel("wrap 2")
	.add(UI.label("First Name:"))
	.add("grow", UI.textField(vm.firstName()))
	.add(UI.label("Last Name:"))
	.add("grow", UI.textField(vm.lastName()))
	.add("span", UI.separator())
	.add("wrap", UI.label("Full Name:"))
	.add("span, grow", UI.textField(vm.fullName()))
 );
```

This will look like this:

<img src="../img/tutorial/growing-sprouts.png" style = "width: 17em; margin: 0.5em; -webkit-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); -moz-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75);">

Swing-Tree will bind the `firstName` and `lastName` properties
of the `PersonViewModel` to the `JTextField` components
and will automatically update the `JTextField` components
whenever the `firstName` or `lastName` properties change.
Conversely, whenever the user changes the text in the `JTextField`
components the `firstName` and `lastName` properties will be updated
as well, which will in turn trigger the `onAct` callbacks!

The powerful thing about this example is that we managed 
to affect the state of the UI (the full name) without
actually depending on the UI at all,
meaning there is not even a single reference to a Swing component
in the `PersonViewModel` class!

Not only does this allow us to write unit tests for our business logic
you can also easily swap out the Swing UI for a different UI
implementation without having to change the business logic at all!

How cool is that? :)




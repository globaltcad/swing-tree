
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

Then inside your code you can import 
Swing-Tree by adding the following
import statement:

```java
 import swingtree.UI;
```

This is all you need to start building Swing UIs with the tree. :tada:

## Growing a Stem ##

No matter which UI framework you use, **all UIs essentially consist
of a tree structure made up of components**. <br>
Swing is no different, which is why the SwingTree
library allows you to **build Swing UIs in a declarative manner**,
just like you would declare your UI structure in HTML or in other XML-based
UI frameworks.

This is done by describing your UI using regular code
designed around **method chaining** and **nesting based composition** 
which then gets converted into a Swing component tree 
and ultimately Swing UI on the fly.

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
done in plain old Java code. 
All of this is made possible by harnessing the power of 
builder style method chaining
and composition. 
Swing Tree combines this to give you a quasi XML-based UI framework
with compile time type safety, turing completeness and all the
other good stuff that comes with Java (or any other JVM language you prefer).

*"But wait"*, I hear you say:
"*how do I even access the GUI components if they are hidden away behind the SwingTree API?!?*"

Don't worry, you can access the actual components in SwingTree
by using the `peek` method, which exposes the underlying component
anywhere you want in the builder API of SwingTree.

So in the above example this would look something like this:

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
        .peek( p -> {
            if ( someCondition )
                p.setBackground(Color.YELLOW)
            if ( someOtherCondition )
                ... // Some other procedural stuff    
        })
    )
    .add(UI.button("Click to Swing!"))
 );
```

As you can see here, we can easily modify the underlying `JPanel`
instance, even though it is actually nested inside another outer panel!

For more details with respect to declarative UI building
check out the [advanced declarations tutorial](Advanced-Declarations.md).

## Growing Branches ##

The next important step to building Swing UIs with Swing-Tree is
mastering **layout managers**. 
Swing-Tree has a general purpose layout manager built into it
which is set as the default layout manager if you do not specify
a different one. 

If you know Swing you may already have heard of it:
**The one and only, all mighty, MigLayout**.

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
but trust me, understanding how the above layout mechanic works is the key to
building Swing UIs with the most ease.

**Instead of specifying the layout constraints for the whole
container we have now specified the layout constraints for
each component individually.**

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
which are all different design patterns for achieving essentially
the same fundamental goal: 

**Separating the UI from the business logic!**

In Swing-Tree you can achieve this separation by using the 
**Sprouts property collection API**.
Properties are a simple yet powerful concept, they are wrapper types
for the field variables of your view models which allow your UI to register change listeners on them.
These listeners thereby make it possible to dynamically update UI components 
when your business logic mutates the properties, and also to
update the properties when the user interacts with the UI.
This **bidirectional observer/listener pattern** is called **data binding**,
and it is the fundamental building block of `MVVM` application development.

Let's consider the following business logic, which we will call "**view model**" 
from now on in accordance with the `MVVM` design and naming conventions:

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
They can wrap any kind of value whose type you can specify using the generic 
type parameter. 
In this example we have 3 properties wrapping a String each.
The most important property type is the `Var` type.
It has both getters and setters for the wrapped value. 
The `Val` type on the other hand is an immutable property / read-only view of a `Var`.
Note that `Var` is a subtype of `Val`, which allows you to 
design your view model API in a way which does
not leak mutable state to the outside world. :partying_face:

Now let's consider the corresponding Swing UI:

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
whenever the `firstName` or `lastName` properties change (their `set` methods are called).
Conversely, whenever the user changes the text in the `JTextField`
components the `firstName` and `lastName` properties will be updated
as well, which will in turn trigger the `onAct` callbacks!
In this example the `onAct` change listener will set the `fullName` property,
which will automatically translate to the corresponding `JTextField` component in the UI.

The powerful thing about this example is that we managed 
to affect the state of the UI (the full name) without
actually depending on the UI at all,
meaning that **there is not even a single reference to a Swing component**
in the `PersonViewModel` class!

Not only does this allow us to write **unit tests** for our business logic
we can now also **easily swap out the Swing UI for a different UI**
implementation without having to change the business logic at all!

How cool is that? :)

If you want to dive deeper into doing MVVM in Swing-Tree,
check out the [MVVM tutorial](./Advanced-MVVM.md).

## Growing Leaves ##

The next step in mastering Swing-Tree is to learn how to
register user events in your declarative Swing UIs.
For different types of Swing components you can register
different types of user events.
But for all Swing components you can register the same
basic set of events like for example:

```java
  UI.panel("fill").withPrefSize(400, 400)
  .onMouseEnter( it -> System.out.println("Mouse entered panel") )
  .onMouseExit( it -> System.out.println("Mouse exited panel") )
  .onMousePress( it -> System.out.println("Mouse pressed panel") )
  .onMouseRelease( it -> System.out.println("Mouse released panel") )
  .onMouseClick( it -> System.out.println("Mouse clicked panel") )
  .onMouseDrag( it -> System.out.println("Mouse dragged panel") )
  .onFocusGain( it -> System.out.println("Panel gained focus") )
  .onFocusLoss( it -> System.out.println("Panel lost focus") )
  ...
``` 

Note that every event handler receives a special `it` parameter!
This is a delegate for both the Swing component and the current
event state. Not only can you use this delegate to access the 
Swing component, and the current event state, but you can also
use it to query the whole UI tree for other components,
schedule animations, and much much more!

Just keep reading and you will see what I mean! :)

(For more information about advanced event handling, [click here](./Advanced-Event-Handling.md))

## Blooming Flowers ##

<img src="../img/tutorial/blooming-flowers.png" style = "float: right; width: 30%; margin: 2em; -webkit-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); -moz-box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75); box-shadow: 0px 0px 18px -2px rgba(0,0,0,0.75);">

An important aspect of modern UI development is the ability to
**customize how your UI is rendered**. <br>
Swing-Tree allows you to customize the looks 
of your UI using its **functional style API**.

Traditionally, the looks of a plain old Swing UI is predominantly
determined by the **"look and feel" (short LaF)** that is installed through the `UIManager`.
Although it is often possible to configure a particular look and feel 
to a limited degree,
like for example specifying the background and foreground colors 
of specific Swing components, it is almost impossible to change
non-trivial aspects of a component in regular Swing, like shadows, gradients, borders, etc.
**SwingTree solves this problem by carefully
intercepting the painting process of Swing components**
so that you can customize their looks on top of the current default LaF.
This customization is done using the **SwingTree style API**.

Here, take a look this code:
```java
UI.show(
    panel("fill, wrap 2")
    .withStyle( it -> it
        .margin(24).padding(24)
        .backgroundColor(new Color(57, 221, 255,255))
        .borderRadius(32)
        .shadowBlurRadius(5)
        .shadowColor(new Color(0,0,0, 128))
    )
    .add("span, alignx center",
        box("ins 12")
        .add(html("<h1>A Well Rounded View</h1><p>Built using the SwingTree style API.</p>"))
    )
    .add(
        panel("ins 12", "[grow]")
        .withStyle( it -> it
            .backgroundColor(new Color(255, 255, 255,255))
            .borderRadius(24)
        )
        .add("span, wrap", label("Isn't this a well rounded view?"))
        .add("growx",
            button("Yes").withStyle( it -> it.borderRadius(24) ),
            button("No")
        )
    )
    .add("top",
        box("ins 24")
        .add(html("<i>SwingTree can override the default<br>Look and feel based style<br>according to your needs.</i>"))
    )
);
```
Which will look like this:

![Styling](../img/tutorial/a-well-rounded-view.png)

Here you can see that we can use the `withStyle` method
to **supply a `Styler` lambda giving us access to the
style API of a Swing component**.
The style API consists of a `StyleDelegate` which 
in the above example is the `it` parameter.
The `StyleDelegate` exposes both the Swing component
and the current `Style` configuration object.

You can configure all kinds of style properties like
**background** and **foreground colors, border colors, border thickness,
border radius, shadow color, shadow blur radius, shading gradients etc.**

All of these styles will of course be rendered
with DPI awareness and HiDPI support out of the box!

This API is functional and immutable,
meaning that the methods on the `StyleDelegate` object
will return a new `StyleDelegate` object with the corresponding
`Style` properties changed.
This makes it easy to compose `Styler` lambdas
to combine different styles for different components.

Also note that all `Styler` lambdas are executed eagerly 
every time a Swing component is painted.
So feel free to use any kind of logic in your `Styler` lambdas
to dynamically change the style of a component based on its state.
The `StyleDelegate` also exposes the current component through the `component()` method,
so you can use it to query the component's state and properties
(like for example checking if a toggle button is selected or not and then styling according to that information).

## Harvesting Fruit ##

The final step in mastering SwingTree are animations.
Yes you read that right, SwingTree supports animations
and advanced custom rendering for all Swing components 
out of the box!

Check out this basic example:

![Green Hover](../img/tutorial/green-hover.gif)

Which is based on the following code:

```java
  button("I turn green when you hover over me")
  .onMouseEnter( it -> 
      it.animateFor(0.5, TimeUnit.SECONDS, state -> {
          double highlight = 1 - state.progress() * 0.5;
          it.setBackgroundColor(highlight, 1, highlight);
      })
  )
  .onMouseExit( it ->
      it.animateFor(0.5, TimeUnit.SECONDS, state -> {
          double highlight = 0.5 + state.progress() * 0.5;
          it.setBackgroundColor(highlight, 1f, highlight);
      })
  )
```

As you can see, animations are very easy to create, 
especially when you use the `animateFor` method
on the previously
mentioned event/component delegate object, 
which in the above example
is the `it` variable.

One thing that might confuses you is the fact that there are 
2 nested lambdas in the 2 `onMouseEnter` and `onMouseExit` callbacks.
The **outer lambda is the event handler** which is called once whenever
the mouse enters or leaves the button. **The inner lambda is the actual
animation** which is called repeatedly until the animation
is finished (which happens after 0.5 seconds in this case).
So the inner lambda is called any number of times 
but usually not more than **~60 times per second**.

Another confusing thing might be the `state` parameter
of the animation callback. This is an `AnimationState` object
which contains information about 
**the state of the current animation update**.
This object exists to tell you how far along the animation is,
and to provide you with some useful methods for designing
animations with seemless and controlled transitions.

You might be wondering: How does all of this work under the hood? 
Well, in essence it is based on a custom `Timer` implementation,
some system time bookkeeping,
and a nice API for creating and scheduling animation lambdas,
that's all. :)

The next example demonstrates how to do custom rendering
in your animations:

![Custom Rendering](../img/tutorial/click-ripples.gif)

Which is based on this code:

```java
  button("I have a click ripple effect")
  .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, state -> {
      it.paint(state, g -> {
          g.setColor(new Color(0.1f, 0.25f, 0.5f, (float) state.fadeOut()));
          for ( int i = 0; i < 5; i++ ) {
              double r = 300 * state.fadeIn() * ( 1 - i * 0.2 ) * it.getScale();
              double x = it.mouseX() - r / 2;
              double y = it.mouseY() - r / 2;
              g.drawOval((int) x, (int) y, (int) r, (int) r);
          }
      });
  }))
```

Note that the `paint` method is called repeatedly
until the animation is finished. The `paint` method
accepts a lambda which is called with a `Graphics2D` object
that you can use to draw on the component.

I hope you are starting to see the power of this.
Just to give you an idea of what you can do with this,
here is a more complex example:

![Button Animation](../img/tutorial/fancy-animation.gif)

```java
  button("I show many little mouse move explosions when you move your mouse over me")
  .withPrefHeight(100)
  .onMouseMove( it -> it.animateFor(1, TimeUnit.SECONDS, state -> {
      double r = 30 * state.fadeIn() * it.getScale();
      double x = it.mouseX() - r / 2.0;
      double y = it.mouseY() - r / 2.0;
      it.paint(state, g -> {
          g.setColor(new Color(1f, 1f, 0f, (float) state.fadeOut()));
          g.fillOval((int) x, (int) y, (int) r, (int) r);
      });
  }))
  .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, state -> {
      double r = 300 * state.fadeIn() * it.getScale();
      double x = it.mouseX() - r / 2;
      double y = it.mouseY() - r / 2;
      it.paint(state, g -> {
          g.setColor(new Color(1f, 1f, 0f, (float) state.fadeOut()));
          g.fillOval((int) x, (int) y, (int) r, (int) r);
      });
  }))
```

The above example shows how to create a button
that has both a mouse move and a mouse click animation
which are very similar to each other.
The only difference is that the radius of the expanding circle
is far larger when the user clicks the button.

---

# Conclusion #

I hope you enjoyed this tutorial and that you are now
ready to start building your own Swing UIs with Swing-Tree.

Here some more reading material to get you started:

- [Simple Dialogs](./Simple-Dialogs.md)
- [Writing Tables](./Writing-Tables.md)
- [Advanced MVVM](./Advanced-MVVM.md)

If you want to learn more about Swing-Tree
check out the [API documentation](../jdocs/index.html)
and the [examples](../../src/test/java/examples/README.md).



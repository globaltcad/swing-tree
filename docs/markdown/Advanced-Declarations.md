
# Advanced UI Declarations #

The problem with declarative code is that it is often not as
adaptive as imperative code. And this might be a reason why you could
be sceptical about using SwingTree for your UIs.

But fear not, the SwingTree API is designed to be just as flexible
as imperative code by exposing a set of useful methods for 
creating and manipulating UIs more dynamically.

## Unwrapping Components ##

The simplest way to switch from declarative to imperative is the `peek` method, 
which was already mentioned in the [main introduction](Climbing-Swing-Tree.md).

So for example:

```java 
//...
.add(
    UI.panel("wrap 2")
    .add(UI.label("A:"), UI.textField("a"))
    .add(UI.label("B:"), UI.textField("b"))
    .peek( p -> {
        if ( someCondition )
            p.setBackground(Color.YELLOW)
        if ( someOtherCondition )
        ... // Some other procedural stuff    
    })
    .add(/*...*/)
)
//...
```

This is most commonly used to set properties of the component,
or copy the reference to the underlying component for later use.

## Wrapping Components ##

Usually the various factory methods in the `UI` namespace,
like `UI.label(..)`, `UI.button(..)`, etc. are
sufficient for creating your GUIs,
however sometimes you may also need to add custom GUI components 
for which SwingTree will obviously not have a factory method.

In that case you can use the various `UI.of(..)` methods to wrap any 
kind of custom component (It must at least be a `JComponent` subclass).

```java
//...
.add(
    UI.of(new MyCustomTextArea())
    .onTextChange( it -> ... )
)
```

## Conditional UI ##

One of the most common ways in which a GUI is dynamic
is that certain components are only added to the rest of
the component tree if a certain condition is true.
In a procedural context, one would simply
write an `if`-`else` branch, but
this is tricky if your UI is one big SwingTree based declaration.

This is why the `applyIf` method exists!

Take a look at the following code snippet:

```java 
//...
.add(
    UI.panel("wrap 2")
    .add(UI.label("A:"), UI.textField("a"))
    .add(UI.label("B:"), UI.textField("b"))
    .applyIf( someConditionIsTrue, ui -> ui
        .add(UI.label("C:"), UI.textField("c"))
        .add(UI.label("D:"), UI.textField("d"))
    )
    .add(/*...*/)
)
//...
```

The `applyIf` method takes a boolean condition and a lambda
which is only executed if the condition is true.
The lambda receives the current UI builder as a parameter,
which allows you to continue building the UI as usual.

## Optional UI ##

Another common use case is that a certain component
is only added to the UI if a certain value is present.

This is why the `applyIfPresent` method exists!

Consider the following example:

```java
// In your view model:
public Optional<MySubModel> getMySubModel() {
    return Optional.ofNullable(this.model);
}
// In your view
...
.add(
    UI.panel()
    .applyIfPresent(viewModel.getMySubModel().map( model -> ui -> ui
        .add(UI.label("Hello Sub Model!"))
        .add(UI.label("Name:"), UI.textField(model.getName()))
        .add(UI.label("Age:"), UI.textField(model.getAge()))
        // ... whatever else you want to add ...
    ))
)
...
```
Now this example is a bit more complex, so let's break it down: 

The `applyIfPresent` method takes an `Optional<Consumer<I>>` as parameter, 
where `I` is the type of the UI builder.
This allows you to map the optional value to a consumer which is only executed if the value is present.
If the optional value is present, the consumer is executed with the 
current UI builder as a parameter, which allows you to continue building the UI as usual.








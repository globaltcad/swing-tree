
# Advanced Event Handling #

Although SwingTree exposes a wide variety of 
different types of component events for which you can register
event handlers, there are cases where you need a more fine-grained
control over the event handling process, which is what this document
is all about.

## Custom Events ##

Advanced event handling requires custom events
which are all based on one fundamental type, the `sprouts.Observable` interface. <br>
A classic representation of the observer pattern.
It defines a thing that allows for the registration of `Observer`s
which are notified when something happens.

As such, the `Observable` is a very generic type that may be implemented
by anything resembling the observer pattern.

The most common and basic type of `Observable` is the `Event` type,
which is a trigger-able event which you can instantiate using `Event.create()`.
But there are other types of `Observable`s as well, such as the `Val` and `Var`
properties, which are wrapper types used for modelling the state of your view models.

If you want to know more about design patterns for decoupling your UI
from its business logic, then [check out this guide about MVI/MVL based modelling](Functional-MVVM.md),
or [click here](Advanced-MVVM.md) for more information about classic MVVM.

## View Events ##

A view event is an event whose handling ought to be executed by the
GUI thread instead of the application thread.
This is what the `onView` method is for.
It has the following signature:

`<E extends Observable> I onView(E event, Action<ComponentDelegate<C, E>> action)`

The `onView` method allows you to attach a component `Action` (event handler) to
a custom event type, a subtype of the formerly mentioned `Observable` interface.
Whenever the event is triggered, the action is executed by the GUI thread.

> You may be confused by the `I` type, but this is just a type variable
> defining the type of the builder node itself, which is used for method chaining.

But enough type theory, let's see some examples.
Let's say your view model consists of a `Var` property called `name`
as well as an `Event` called `receivedLike`
and both of these are exposed as part of the view model API
through the `getName` and `getReceivedLike` methods respectively
the view for said model might look like this:

```java
UI.panel("fill, insets 3", "[grow]")
.add("grow",
    UI.label(vm.getName()),
    .onView(vm.getName(), it ->{
        int length = it.get().getText().length();
        it.get().setPreferredSize(new Dimension(length * 10, 20));
    })
    .onView(vm.getReceivedLike(), it ->
        it.animateFor(3, TimeUnit.SECONDS, state -> {
          double r = state.progress();
          double g = 1 - state.progress();
          double b = state.pulse();
          it.setForeground(new Color(r, g, b));
        })
    )
)
```

The first `onView` call attaches an event handler to the `name` property
which is triggered whenever the value of the property changes.
The event handler is executed by the GUI thread, and it simply
sets the preferred size of the label to be proportional to the length of the name.

The second `onView` call attaches an event handler to the `receivedLike` event
which is presumably triggered whenever the user clicks a like button or something like that.
Then the event handler animates the foreground color of the label for 3 seconds,
all of which is executed by the GUI thread.

Note that the `onView` method is intended to be used
for view model events which are supposed to have an effect on the view.
That is precisely the reason why this method executes the event handler
by the GUI thread instead of the application thread.    

## Application Events ##

An application event is an event whose handling ought to be executed by the
application thread instead of the GUI thread.
This is what the `on` method is for.
Take a look at its signature:

`<E extends Observable> I on(E event, Action<ComponentDelegate<C, E>> action)`

As you might have already guessed, the `on` method is virtually identical
to the `onView` method, except that the event handler is executed by the
application thread instead of the GUI thread.

It, too, allows you to attach a component `Action` (event handler) to
a custom event type, a subtype of the formerly mentioned `Observable` interface.
Whenever the event is triggered, the action is executed by the application thread.

> Again, note that the `I` type is merely defining the type of
> the builder node itself, which is used for method chaining.

Let's see an example of this as well.
This example is a little bit different from the previous one,
as the `on` method is not intended to be used for view model events.
Instead, it is intended to be used for any other type of event
like for example custom user input events, like a custom touch event.

Consider the following example:
```java
UI.panel().withPrefSize(100, 100)
.on(CustomEventSystem.touchGesture(), it -> ..some App update.. )
```
In this example we use an imaginary `CustomEventSystem` to register a touch gesture event handler
which will be called on the application thread when the touch gesture event is fired.
Although neither Swing nor SwingTree have a touch gesture event system, this example illustrates
how one could easily integrate such a custom event system into the SwingTree UI tree.




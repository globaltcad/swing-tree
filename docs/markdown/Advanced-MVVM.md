
# Introduction #

The core principle of MVVM is he usage of so-called properties
to model the state and business logic of your user interface!
Sor for example, if you have a text field in your user interface,
and based on a certain condition you want the text field to be
disabled or invisible, you would create a boolean property
(`Var<Boolean> flag = Var.of(true);`)
in your view model, and then bind the text field to that property
by passing the property to the correct Swing-Tree text field builder node...

Let's take a look at how this works in practice!

Here is a simple view model:

```java
public class MyViewModel {
    private final Var<Boolean> userCanEnterSomething = Var.of(true);
    
    public Val<Boolean> userCanEnterSomething() { return userCanEnterSomething; }
}
```

And here the corresponding view:

```java
import static swingtree.UI.*;

public class MyView extends JPanel {
    public MyView(MyViewModel vm) {
        of(this)
        .add(label("Welcome to a Swing-Tree!"))
        .add(
            textField("Enter something here")
            .isEnabledIf(vm.userCanEnterSomething())
        );
    }
    // For running the example
    public static void main(String... args) {
        UI.show(new MyView(new MyViewModel()));
    }
}
```

As you can see, the text field is bound to the property
and will be enabled or disabled when you call the `set` method
on the property.
Not only does this fancy kind of binding mechanism work for other kinds of flags like
the **visibility**, **focusability**, **editable**...
but it also works for the **text** of a text field, the **value** of a slider,
the **selected** state of a checkbox, the **selected item** of a combo box...
and so on and so forth.

Cool, right? :)

# How it works #

This might look like magic, but it's actually quite simple how
this binding mechanism works under the hood:

Every type of property allows you to register 2 kinds of listeners to
observe changes to the property:

1. An application action listener you can register using the `onSet` method, 
   which is notified when the property is changed by calling the `set` method.
2. A user action listener you can register using the `onAct` method, 
   which is notified when the property is changed by calling the `act` method.

The `act` method is a special setter method that represents changes performed by the user, 
so when the user enters text into a text field, the `act` method is called by 
the text field automatically.

The `set` method on the other hand represents changes performed by the application inside a view model
as part of the business logic.

This clean distinction allows for more control over the flow of information 
in your application
and it avoids certain kinds of infinite loops, 
redundant updates, and other nasty things.

# Lists of Properties #

Besides the single properties, Swing-Tree also supports lists of properties
in the for of the `Vals` and `Vars` types.
These types are just like the `Val` and `Var` types, but they represent
a list of properties instead of a single property.
Not only can you register listeners to observe changes to the list of properties,
but you can also register listeners to observe changes to individual properties.

The only substantial difference between these simple properties and
these property list types is that the list types
do not distinguish between application and user actions.
Instead there is simply a single `onChange` method that 
allows for the registration of change listeners.

# Advanced Views and Models #

The `MVVM` pattern makes sense is easily understand when you have a simple,
but it can get a bit more complicated when you have a user interface that consists 
of sub-views and sub-view models which may need to be highly dynamic.

This is where properties unleash there full power, because
believe it or not, but properties can actually be used to wrap and model
so called ***viewable sub-view models*** which automatically supply ***sub-views***!

Show, don't tell! Here is an example:

**Sub-View Model 1:**

```java
public class SubViewModel1 implements Viewable
{
    private final Var<String> username = Var.of("");
    
    public Var<String> username() { return username; }
   
    @Override public <V> V createView( Class<V> viewType ) {
        return viewType.cast(new SubView1(this));
    }
}
```

**Sub-View Model 2:**

```java
public class SubViewModel2 implements Viewable
{
    private final Var<Boolean> isChecked = Var.of(false);
    
    public Var<Boolean> isChecked() { return isChecked; }
   
    @Override public <V> V createView( Class<V> viewType ) {
        return viewType.cast(new SubView2(this));
    }
}
```

**Main View Model:**

```java
public class ViewModel //implements Viewable <-- This could be a sub-view model too!
{
    private final Var<Viewable> subViewModel = Var.of(new SubViewModel1());
    
    public Var<Viewable> subViewModel() { return subViewModel; }
   
    public void toggleSubViewModel() {
        if ( subViewModel.get() instanceof SubViewModel1 )
            subViewModel.set(new SubViewModel2());
        else
            subViewModel.set(new SubViewModel1());
    }
    
    public JPanel createView() { return new View(this); }
}
```

**Main View:**

```java
import static swingtree.UI.*;

public class View extends JPanel {
    View(ViewModel vm) {
        of(this).withLayout("fill, wrap 1")
        .add(
            button("Toggle Sub View Model")
            .onClick(vm::toggleSubViewModel)
        )
        .add(separator())
        .add(vm.subViewModel()); // <-- This is where the magic happens!
    }
    // For running the example
    public static void main(String... args) {
        UI.show(new ViewModel().createView());
    }
}
```

As you can see, the main view model has a property of type `Var<Viewable>`,
which is bound to the main view via the `add` method.
This means that whenever the main view model changes the value of the property,
the main view will automatically update itself by replacing the old sub-view
with the new sub-view.

The view knows how to create the sub-view by calling the `createView` method
on the sub-view model, which is why the sub-view model implements the `Viewable` interface.

This is a very powerful concept which has several extremely important benefit!

- You can create highly dynamic, modular, and reusable user interfaces that can be easily extended and modified
- It's composition based, which makes it very easy to model any kind of complex business logic
- Compositions of view models and sub-view models are still fully decoupled from the view, 
  meaning tht your business logic is testable an not polluted by any kind of view related code
- It is possible to have polymorphic relationships between view models and sub-view models, 
  making your code modular, extensible, and reusable.

In the above example we are using the `Var` type for the sub-view model property,
but you can also use the `Vars` type to model a list of sub-view models
whose views are all added and displayed in the "super-view" automatically
and dynamically.
So when you remove or add a sub-view model to a view bound property list, 
the view will automatically remove or add the corresponding sub-view.










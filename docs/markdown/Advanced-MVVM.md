
# Introduction #

The core principle of MVVM is he usage of so-called *properties*
to model the state and business logic of your user interface!
These are observable wrapper objects with getter, setter, and listener methods
that allow your view to observe changes to the state of your view model.
So for example, if you have a text field in your user interface,
and based on a certain condition you want the text field to be
disabled or invisible, you would create a boolean property
(`Var<Boolean> flag = Var.of(true);`)
in your view model, and then bind the text field to that property
by passing the property to the correct Swing-Tree text field builder node...

Let's take a look at how this works in practice!

Here is a simple view model:

```java
public class MyViewModel {
    private final Var<Boolean> userMayEnterSomething = Var.of(true);
    
    public Val<Boolean> userMayEnterSomething() { return userMayEnterSomething; }
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
            .isEnabledIf(vm.userMayEnterSomething())
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
on the property as part of the business logic of your application.
Not only does this fancy kind of binding mechanism work for other kinds of flags like
the **visibility**, **focusability**, **editable**...
but it also works for the **text** of a text field, the **value** of a slider,
the **selected** state of a checkbox, the **selected item** of a combo box...
and so on and so forth.

Cool, right? :)

# How it works #

This might look like magic, but it's actually quite simple how
this binding mechanism works under the hood:

Every type of property allows you to register change observers which observe events
through 2 different kinds of channels, a view change event channel and a view model change event channel.

1. An **application action listener** is registered through the `onChange(From.VIEW_MODEL, ..)` method call, 
   which is notified when the property is changed by calling `set(From.VIEW_MODEL, ..)`, which 
   is what the application logic does when it changes the value of the property.
2. A **user action listener** is registered through the `onChange(From.VIEW, ..)` method call, 
   which is notified when the property is changed by calling `set(From.VIEW, ..)`, which
   is what the view does when the user interacts with it.

The `set(From.VIEW, ..)` call specifies that the change is performed by the view (usually the user), 
so when the user enters text into a text field, the `set` method is called through the `From.VIEW` channel
by the SwingTree text field automatically.

The regular `set` method call or the explicit `set(From.VIEW_MODEL, ..)` call
on the other hand represents changes performed by the application inside a view model
as part of the business logic.

This clean distinction allows for more control over the flow of information 
in your application,
and it avoids certain kinds of infinite feedback loops, 
redundant updates, and other nasty side effects.

# Lists of Properties #

Besides the formerly described single item properties, Swing-Tree also supports lists of properties
in the form of the `Vals` and `Vars` types.
These types are just like the `Val` and `Var` types, but they represent
lists of properties instead of a single item.
Not only can you register listeners to observe changes to the list of properties,
but you can also register listeners to observe changes to individual properties.

The only substantial difference between these simple properties and
these property list types is that the list types
do not distinguish between application and user actions.
Instead, there is simply a single `onChange` method that 
allows for the registration of generic change listeners by your views.

# Advanced Views and Models #

The `MVVM` pattern is easily understood in simple cases like the one above,
but it can get a bit more complicated when you have a user interface that consists 
of sub-views and sub-view models which are composed dynamically based on
complex business logic.

This is where properties unleash their full power, because
believe it or not, properties can actually be used to wrap and model
so called ***sub-view models*** with dynamically changing **sub-views**!

Show, don't tell! Here is an example:

**Sub-View Model 1:**

```java
public class SubViewModel1
{
    private final Var<String> username = Var.of("");
    
    public Var<String> username() { return username; }
}
```

**Sub-View Model 2:**

```java
public class SubViewModel2
{
    private final Var<Boolean> isChecked = Var.of(false);
    
    public Var<Boolean> isChecked() { return isChecked; }
}
```

**Main View Model:**

```java
public class ViewModel
{
    private final Var<Object> subViewModel = Var.of(new SubViewModel1());
    
    public Var<Object> subViewModel() { return subViewModel; }
   
    public void toggleSubViewModel() {
        if ( subViewModel.get() instanceof SubViewModel1 )
            subViewModel.set(new SubViewModel2());
        else if ( subViewModel.get() instanceof SubViewModel2 )
            subViewModel.set(new SubViewModel1());
    }
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
        .add(vm.subViewModel(), // <-- This is where the magic happens!
            subViewModel -> { // <-- This is the view supplier
                if ( subViewModel instanceof SubViewModel1 )
                    return new SubView1((SubViewModel1)subViewModel);
                else if ( subViewModel instanceof SubViewModel2 )
                    return new SubView2((SubViewModel2)subViewModel);
                else
                    throw new RuntimeException("Unknown sub-view model!");
            }
        );
    }
    // For running the example
    public static void main(String... args) {
        UI.show(new View(new ViewModel()));
    }
}
```

As you can see, the main view model has a property of type `Var<Object>`,
which is bound to the main view via the `add` method.
This means that whenever the main view model changes the value of the property,
the main view will automatically update itself by replacing the old sub-view
with the new sub-view using the supplied view supplier.
The view supplier is a function that takes the current value of the property
and turns it into a sub-view.

The main view will replace the old sub-view with the newly supplied sub-view
automatically and dynamically whenever the property changes.

This is a very powerful concept which has several extremely important benefit!

- You can create highly dynamic, modular, and reusable user interfaces that can be easily extended and modified
- It's composition based, which makes it very easy to model any kind of complex business logic
- Compositions of view models and sub-view models are still fully decoupled from the view, 
  meaning tht your business logic is testable and not polluted by any kind of view related code
- It is possible to have polymorphic relationships between view models and sub-view models, 
  making your code modular, extensible, and reusable.

In the above example we are using the `Var` type for the sub-view model property,
but you can also use the `Vars` type to model a list of sub-view models
whose views are all added and displayed in the "super-view" automatically
and dynamically.
So when you remove or add a sub-view model to a view bound property list, 
the view will automatically remove or add the corresponding sub-view.

## Where is the Model? ##

You may have noticed that the above examples do not include model classes.
The reason is simply that they are too simple to require one.
But in a real-world application you would want to maintain a model
that holds the business logic and data of the application like for
example a model for application settings, database entities or
network data. The model would typically be updated by the view model,
whereas the view does not interact with the model directly.










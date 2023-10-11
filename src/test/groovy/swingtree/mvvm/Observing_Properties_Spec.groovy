package swingtree.mvvm

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Val
import sprouts.Var

@Title("Property Change Listeners")
@Narrative('''

    The core motivation behind the creation of the SwingTree library
    is to provide a simple and powerful way to model the state 
    as well as business logic of your UI without actually depending on it.
    
    To make the decoupling between your UI and the UIs state and logic 
    possible you need to bind Sprouts properties to UI components.
    This is done through the use of change listeners and event listeners
    and so called `Channel`s, which are used to distinguish between
    different types of events.
    
''')
@Subject([Val, Var])
class Observing_Properties_Spec extends Specification
{

    def 'Properties can be listened to by subscribing to them using the `onChange(..)` method.'()
    {
        reportInfo"""
            A property informs a change observer
            when their state is changed through the `set(T)` method. 
            However, it may also inform when `fire(From.VIEW_MODEL)` 
            is called explicitly on a particular property.
            This *rebroadcasting* is often useful
            as it allows you to manually decide yourself when
            the state of a property is "ready" for display in the UI.
        """

        given : 'We create a mutable property...'
            Var<String> mutable = Var.of("Tempeh")
        and : 'Something we want to have a side effect on:'
            var list = []
        when : 'We subscribe to the property using the `onChange(..)` method.'
            mutable.onChange(From.VIEW_MODEL, it -> list.add(it.orElseNull()) )
        and : 'We change the value of the property.'
            mutable.set("Tofu")
        then : 'The side effect is executed.'
            list == ["Tofu"]
        when : 'We trigger the side effect manually.'
            mutable.fire(From.VIEW_MODEL)
        then : 'The side effect is executed again.'
            list.size() == 2
            list[0] == "Tofu"
            list[1] == "Tofu"
    }

    def 'The `withID(..)` method produces a new property with all bindings inherited.'()
    {
        reportInfo """
            The wither methods on properties are used to create new property instances
            with the same value and bindings (observer) as the original property
            but without any side effects of the original property (the bindings).
            So if you add bindings to a withered property they will not affect the original property.
        """

        given : 'We create a property...'
            Var<String> property = Var.of("Hello World")
        and : 'we bind 1 subscriber to the property.'
            var list1 = []
            property.onChange(From.VIEW_MODEL, it -> list1.add(it.orElseNull()) )
        and : 'We create a new property with a different id.'
            Val<String> property2 = property.withId("XY")
        and : 'Another subscriber to the new property.'
            var list2 = []
            property2.onChange(From.VIEW_MODEL, it -> list2.add(it.orElseNull()) )

        when : 'We change the value of the original property.'
            property.set("Tofu")
        then : 'The subscriber of the original property is triggered but not the subscriber of the new property.'
            list1 == ["Tofu"]
            list2 == []

        when : 'We change the value of the new property.'
            property2.set("Tempeh")
        then : 'Both subscribers are receive the effect.'
            list1 == ["Tofu", "Tempeh"]
            list2 == ["Tempeh"]
    }

    def 'Changing the value of a property through the `.set(From.VIEW, T)` method will also affect its views'()
    {
        reportInfo """
            Note that you should use `.set(From.VIEW, T)` inside your view to change 
            the value of the original property.
            It is different from a regular `set(T)` (=`.set(From.VIEW_MODEL, T)`) in 
            that the `set(T)` method
            runs the mutation through the `From.VIEW_MODEL` channel.
            This the difference here is the purpose and origin of the mutation,
            `VIEW` changes are usually caused by user actions and `VIEW_MODEL`
            changes are caused by the application logic.
            Irrespective as to how the value of the original property is changed,
            the views will be updated.
        """
        given : 'We create a property...'
            Var<String> food = Var.of("Animal Crossing")
        and : 'We create a view of the property.'
            Var<Integer> words = food.viewAsInt( f -> f.split(" ").length )
        expect : 'The view has the expected value.'
            words.get() == 2

        when : 'We change the value of the food property through the `.set(From.VIEW, T)` method.'
            food.set(From.VIEW, "Faster Than Light")
        then : 'The view is updated.'
            words.get() == 3
    }

    def 'Use `set(From.VIEW, T)` on our properties to change the property state from the frontend.'()
    {
        reportInfo """
            SwingTree was designed to support MVVM for Swing,
            unfortunately however raw Swing makes it very difficult to implement MVVM
            as the models of Swing components are not observable.
            A JButton mode for example does not have a property that you can bind to.
            Instead what we need are precise updates to the UI components without
            triggering any looping event callbacks.
            This is why the concept of "channels" was introduced.
            You may call `set(From.VIEW, ..)` on a property to change its state
            from the frontend, meaning that only observers registered through the
            same channel will be notified.
            So in this case the change will only be noticed by observers
            registered using `onChange(From.VIEW, ..)`. 
            Note that on the other hand, the regular `set(T)` method is 
            equivalent to `set(From.VIEW_MODEL, T)`, meaning that it will
            notify observers registered using `onChange(From.VIEW_MODEL, ..)`
            instead of `onChange(From.VIEW, ..)`.
        """
        given : 'A simple property with a view and model actions.'
            var viewListener = []
            var modelListener = []
            var anyListener = []
            var property = Var.of(":)")
            property.onChange(From.VIEW, it -> viewListener << it.orElseThrow() )
            property.onChange(From.VIEW_MODEL, it -> modelListener << it.orElseNull() )
            property.onChange(From.ALL, it -> anyListener << it.orElseThrow() )

        when : 'We change the state of the property multiple times using the `set(Channel, T)` method.'
            property.set(From.VIEW, ":(")
            property.set(From.VIEW_MODEL, ":|")
            property.set(From.ALL, ":D")

        then : 'The `VIEW` actions were triggered once.'
            viewListener == [":(", ":D"]
        and : 'The `VIEW_MODEL` actions were also triggered once.'
            modelListener == [":|", ":D"]
        and : 'The `ALL` actions were triggered three times.'
            anyListener == [":(", ":|", ":D"]
    }

    def 'Subscribing to the `From.ALL` channel will notify you of all changes.'()
    {
        reportInfo """
            The `From.ALL` channel is a special channel that will notify you of all changes
            to the property, regardless of which channel was used to trigger the change.
            This is useful if you want to react to all changes to a property.
            
            Your view for example might want to react to all changes to a property
            to update the UI accordingly.
        """
        given : 'A simple property with a view and model actions.'
            var showListener = []
            var modelListener = []
            var property = Var.of(":)")
                                .onChange(From.ALL, it ->{
                                    modelListener << it.orElseThrow()
                                })
            property.onChange(From.VIEW_MODEL, it -> showListener << it.orElseNull() )

        when : 'We change the state of the property using the "set(T)" method.'
            property.set(":(")
        then : 'The "onSet" actions are triggered.'
            showListener == [":("]
        and : 'The view model actions are not triggered.'
            modelListener == [":("]

        when : 'We change the state of the property by calling `set(From.VIEW, ..)`.'
            property.set(From.VIEW, ":|")
        then : 'The `VIEW_MODEL` actions are NOT triggered, because the `.set(From.VIEW, T)` method only triggers `VIEW` actions.'
            showListener == [":("]
        and : 'The view model actions are triggered as expected.'
            modelListener == [":(", ":|"]
    }

}

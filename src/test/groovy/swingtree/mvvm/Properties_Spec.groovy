package swingtree.mvvm

import swingtree.UI
import swingtree.api.UIAction
import swingtree.api.mvvm.Mutation
import swingtree.api.mvvm.Val
import swingtree.api.mvvm.ValDelegate
import swingtree.api.mvvm.Vals
import swingtree.api.mvvm.ValsDelegate
import swingtree.api.mvvm.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.api.mvvm.Vars

import java.util.function.Consumer

@Title("Properties")
@Narrative('''

    Properties are a powerful tool to model the state 
    as well as business logic of your UI without actually depending on it.
    This is especially useful for testing your UIs logic.
    Therefore properties are a root concept in the Swing-Tree library.
    The decoupling between your UI and the UIs state and logic 
    is achieved by binding properties to UI components.
    This specification shows you how to model UI state 
    and business logic using properties 
    and how to bind them to UI components.
    
''')
class Properties_Spec extends Specification
{
    def 'Properties are simple wrappers around a value'()
    {
        given : 'We create a property using the "of" factory method.'
            Var<String> property = Var.of("Hello World")

        expect : 'The property has the same value as the value we passed to the factory method.'
            property.orElseNull() == "Hello World"
    }

    def 'They can be bound to the UI by passing them to a builder node.'()
    {
        reportInfo """
            Binding goes both ways, so when the property changes state through the "set" method
            it will update the UI
            using the "show()" method on the property (which you can also call manually for UI updates), 
            and when the UI is changed by the user, it will update the property for us
            and trigger the property action (if present).
        """
        given : 'A simple boolean property modelling a toggle state.'
            boolean userDidToggle = false
            Val<Boolean> toggled = Var.of(false)
                                        .withAction({userDidToggle = true})

        when : 'We create a toggle button using the "toggleButton" factory method and pass the property to it.'
            var ui = UI.toggleButton("Toggle Me!").isSelectedIf(toggled)

        then : 'The button should be updated when the property changes.'
            ui.component.selected == false

        when : 'We change and then show the property value...'
            toggled.set(true)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            ui.component.selected == true

        when : 'We try to un-toggle the toggle button in the UI...'
            ui.component.doClick()
        then : 'The property should be toggled off again.'
            toggled.orElseNull() == false
        and :
            userDidToggle == true
    }

    def 'There are 2 types of properties, an immutable property, and its mutable sub-type.'()
    {
        reportInfo """
            Mutable properties are called "Var" and immutable properties are called "Val".
            This distinction exists so that you can better encapsulating the mutable parts
            of you business logic and UI state.
            So if you want your UI to only display but not change a
            property expose Val, if on the other hand it should
            be able to change the state of the property, use Var!
        """
        given : 'We create a mutable property...'
            Var<Integer> mutable = Var.of(42)
        expect : 'The property stores the value 42.'
            mutable.orElseNull() == 42
        and : "It has the expected type."
            mutable.type() == Integer.class

        when : 'We change the value of the mutable property.'
            mutable.set(69)
        then : 'The property stores the new value.'
            mutable.orElseNull() == 69

        when : 'We now downcast the mutable property to an immutable property.'
            Val<Integer> immutable = mutable
        then : 'The immutable property will only expose the "get()" method, but not "set(..)".'
            immutable.orElseNull() == 69
    }

    def 'Properties can be bound by subscribing to them using the "onShow(..)" method.'()
    {
        reportInfo"""
            Note that bound Swing-Tree properties only have side effects
            when they are deliberately triggered to execute their side effects.
            This is important to allow you to decide yourself when
            the state of a property is "ready" for display in the UI.
        """

        given : 'We create a mutable property...'
            Var<String> mutable = Var.of("Tempeh")
        and : 'Something we want to have a side effect on:'
            var list = []
        when : 'We subscribe to the property using the "onShow(..)" method.'
            mutable.onShow { list.add(it) }
        and : 'We change the value of the property.'
            mutable.set("Tofu")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The side effect is executed.'
            list == ["Tofu"]
    }

    def 'Properties not only have a value but also a type and id!'()
    {
        given : 'We create a property with an id...'
            Val<String> property = Var.ofNullable(String, "Hello World").withID("XY")
        expect : 'The property has the expected id.'
            property.id() == "XY"
        and : 'The property has the expected type.'
            property.type() == String.class
    }

    def 'A property can only wrap null if we specify a type class.'()
    {
        given : 'We create a property with a type class...'
            Val<String> property = Var.ofNullable(String, null)
        expect : 'The property has the expected type.'
            property.type() == String.class
        and : 'The property is empty.'
            property.isEmpty()

        when : 'We create a property without a type class...'
            Val<?> property2 = Var.of(null)
        then : 'The factory method will throw an exception.'
            thrown(NullPointerException)
    }

    def 'The "withID(..)" method produces a new property with all bindings inherited.'()
    {
        reportInfo """
            The wither methods on properties are used to create new property instances
            with the same value and bindings as the original property
            but without side effects of the original property.
            So if you add bindings to a withered property they will not affect the original property.
        """

        given : 'We create a property...'
            Var<String> property = Var.of("Hello World")
        and : 'we bind 1 subscriber to the property.'
            var list1 = []
            property.onShow { list1.add(it) }
        and : 'We create a new property with a different id.'
            Val<String> property2 = property.withID("XY")
        and : 'Another subscriber to the new property.'
            var list2 = []
            property2.onShow { list2.add(it) }

        when : 'We change the value of the original property.'
            property.set("Tofu")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The subscriber of the original property is triggered but not the subscriber of the new property.'
            list1 == ["Tofu"]
            list2 == []

        when : 'We change the value of the new property.'
            property2.set("Tempeh")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'Both subscribers are receive the effect.'
            list1 == ["Tofu", "Tempeh"]
            list2 == ["Tempeh"]
    }

    def 'Properties are similar to the "Optional" class, you can map them and see if they are empty or not.'()
    {
        given : 'We create a property...'
            Val<String> property = Val.of("Hello World")
        expect : 'We can map the property to another property.'
            property.map { it.length() } == Val.of(11)
        and : 'We can check if the property is empty.'
            property.isEmpty() == false

        when : 'We create a property that is empty...'
            Val<String> empty = Val.ofNullable(String, null)
        then : 'The property is empty, regardless of how we map it.'
            empty.map( it -> it.length() ) == Val.ofNullable(Void, null)
    }

    def 'Use the "viewAs" method to create a dynamically updated view of a property.'()
    {
        reportInfo """
            The "viewAs" method is used to create a dynamically updated view of a property.
            In essence it is a property observing another property and updating its value
            whenever the observed property changes.
        """
        given : 'We create a property...'
            Var<String> property = Var.of("Hello World")
        and : 'We create an integer view of the property.'
            Val<Integer> view = property.viewAs(Integer, { it.length() })
        expect : 'The view has the expected value.'
            view.orElseNull() == 11

        when : 'We change the value of the property.'
            property.set("Tofu")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The view is updated.'
            view.orElseNull() == 4
    }

    def 'There are various kinds of convenience methods for creating live view of properties.'()
    {
        given : 'We create a property...'
            Var<String> food = Var.of("Channa Masala")
        and : 'Different kinds of views:'
            Var<Integer> words = food.viewAsInt( f -> f.split(" ").length )
            Var<Integer> words2 = words.view({it * 2})
            Var<Double> average = food.viewAsDouble( f -> f.chars().average().orElse(0) )
            Var<Boolean> isLong = food.viewAs(Boolean, f -> f.length() > 14 )
            Var<String> firstWord = food.view( f -> f.split(" ")[0] )
            Var<String> lastWord = food.view( f -> f.split(" ")[f.split(" ").length-1] )
        expect : 'The views have the expected values.'
            words.get() == 2
            words2.get() == 4
            average.get().round(2) == 92.92d
            isLong.get() == false
            firstWord.get() == "Channa"
            lastWord.get() == "Masala"

        when : 'We change the value of the property.'
            food.set("Tofu Tempeh Saitan")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The views are updated.'
            words.get() == 3
            words2.get() == 6
            average.get().round(2) == 94.28d
            isLong.get() == true
            firstWord.get() == "Tofu"
            lastWord.get() == "Saitan"
    }

    def 'Changing the value of a property through the "act" method will also affect its views'()
    {
        reportInfo """
            Note that the "act" method is used by the view to change the value of the original property.
            It is conceptually similar to the "set" method with the simple difference
            that it represents a user action.
            Irrespective as to how the value of the original property is changed,
            the views will be updated.
        """
        given : 'We create a property...'
            Var<String> food = Var.of("Animal Crossing")
        and : 'We create a view of the property.'
            Var<Integer> words = food.viewAsInt( f -> f.split(" ").length )
        expect : 'The view has the expected value.'
            words.get() == 2

        when : 'We change the value of the food property through the "act" method.'
            food.act("Faster Than Light")
        then : 'The view is updated.'
            words.get() == 3
    }

    def 'The "ifPresent" method allows us to see if a property has a value or not.'()
    {
        given : 'We create a property...'
            Val<String> property = Val.of("Hello World")
        and : 'We create a consumer that will be called if the property has a value.'
            var list = []
            Consumer<String> consumer = { list.add(it) }
        when : 'We call the "ifPresent(..)" method on the property.'
            property.ifPresent( consumer )
        then : 'The consumer is called.'
            list == ["Hello World"]
    }

    def 'An empty property will throw an exception if you try to access its value.'()
    {
        given : 'We create a property...'
            Val<Long> property = Val.ofNullable(Long, null)
        when : 'We try to access the value of the property.'
            property.orElseThrow()
        then : 'The property will throw an exception.'
            thrown(NoSuchElementException)
    }

    def 'The "orElseNull" method should be used instead of "orElseThrow" if you are fine with null values.'()
    {
        reportInfo """
            Note that accessing the value of an empty property using the "orElseThrow" method
            will throw an exception if it is null!
            Use "orElseNull" if you are fine with your property being empty 
            and to also make this intend clear.
        """
        given : 'We create a property...'
            Val<Long> property = Val.ofNullable(Long, null)
        when : 'We try to access the value of the property.'
            property.orElseThrow()
        then : 'The property will throw an exception.'
            thrown(NoSuchElementException)
    }

    def 'The "get" method will throw an exception if there is no element present.'()
    {
        reportInfo """
            Note that accessing the value of an empty property using the "get" method
            will throw an exception.
            It is recommended to use the "orElseNull" method instead, because the "get"
            method is intended to be used for non-nullable types, or when it
            is clear that the property is not empty!
        """
        given : 'We create a property...'
            Val<Long> property = Val.ofNullable(Long, null)
        when : 'We try to access the value of the property.'
            property.get()
        then : 'The property will throw an exception.'
            thrown(NoSuchElementException)
    }

    def 'The equality and hash code of a property are based on its value, type and id!'()
    {
        given : 'We create various kinds of properties...'
            Var<Integer> num = Var.of(1)
            Var<Long>    num2 = Var.of(1L)
            Var<String>  str = Var.of("Hello World")
            Var<String>  str2 = Var.ofNullable(String, null)
            Var<String>  str3 = Var.ofNullable(String, null)
            Var<Boolean> bool = Var.ofNullable(Boolean, null)
            Var<int[]> arr1 = Var.of(new int[]{1,2,3})
            Var<int[]> arr2 = Var.of(new int[]{1,2,3})
        expect : 'The properties are equal if they have the same value, type and id.'
            num.equals(num2) == false
            num.equals(str)  == false
            num.equals(str2) == false
        and : 'If they are empty they are equal if they have the same type and id.'
            str2.equals(str3) == true
            str2.equals(bool) == false
        and : 'Properties are value oriented so arrays are equal if they have the same values.'
            arr1.equals(arr2) == true
        and : 'All of this is also true for their hash codes:'
            num.hashCode() != num2.hashCode()
            num.hashCode() != str.hashCode()
            num.hashCode() != str2.hashCode()
            str2.hashCode() == str3.hashCode()
            str2.hashCode() != bool.hashCode()
            arr1.hashCode() == arr2.hashCode()
    }

    def 'Properties expose a state history in their property actions.'()
    {
        reportInfo """
            When the view changes the state of a property and then triggers a model action, 
            the property will remember its previous state and store it in an internal history.
            This history will be exposed to the property action, and it can be used to undo 
            or redo changes to the property.
        """
        given : 'We create a reference to catch the action delegate and a property with an action to set the reference...'
            var delegate = null
            Var<String> property = Var.of("Hello World")
                                        .withAction( it ->{
                                            delegate = it
                                        })
        when : 'We change the property and trigger the action.'
            property.set("Tofu").act()
        then : 'The history contains the previous state of the property.'
            delegate.history() == [Var.of("Hello World")]

        when : 'We change the property a few times and trigger the action again.'
            property
                .set("Tempeh")
                .set("Tempeh") // Duplicate "changes" will not be recorded in the history.
                .set("Saitan")
                .act()
        then : 'The history contains the previous states of the property.'
            delegate.history() == [Var.of("Tempeh"), Var.of("Tofu"), Var.of("Hello World")]
    }

    def 'The delegate of a property action exposes the current as well as the previous state of the property.'()
    {
        given : 'We create a reference to catch the action delegate and a property with an action to set the reference...'
            var delegate = null
            Var<String> property = Var.of("Hello World")
                                        .withAction( it ->{
                                            delegate = it
                                        })
        when : 'We change the property and trigger the action.'
            property.set("Tofu").act()
        then : 'The delegate exposes the current and previous state of the property.'
            delegate.current() == Var.of("Tofu")
            delegate.previous() == Var.of("Hello World")
    }

    def 'We can search for a previous state of a property in its history.'()
    {
        reportInfo """
            You can query the history of a property for a previous state of the property
            using the "previous(int)" method.
            It returns an optional that contains the previous state if it is found.
            Note that the index passed to the method is relative to the current state,
            so a value of 0 will return the current state, a value of 1 will return the
            previous state, and so on. 
        """
        given : 'We create a reference to catch the action delegate and a property with an action to set the reference...'
            var delegate = null
            Var<String> property = Var.of("Apple")
                                        .withAction( it ->{
                                            delegate = it
                                        })
        when : 'We change the property a few times and trigger the action again.'
            property
                .set("Banana")
                .set("Cherry")
                .act()
        then : 'We can check the presents of previous states in the property history.'
            delegate.previous(0).isPresent()
            delegate.previous(1).isPresent()
            delegate.previous(2).isPresent()
            delegate.previous(3).isPresent() == false
        and : 'We can check the value of previous states in the property history.'
            delegate.previous(0).get() == Var.of("Cherry")
            delegate.previous(1).get() == Var.of("Banana")
            delegate.previous(2).get() == Var.of("Apple")
        and : 'Alternatively we can use the "get(int)" method which takes negative indices.'
            delegate.get(-0).get() == Var.of("Cherry")
            delegate.get(-1).get() == Var.of("Banana")
            delegate.get(-2).get() == Var.of("Apple")
    }

    def 'The UI uses the "act(T)" method to change the property state to avoid feedback looping.'()
    {
        reportInfo """
            Unfortunately Swing does not allow us to implement models for all types of UI components.
            A JButton for example does not have a model that we can use to bind to a property.
            Instead Swing-Tree has to perform precise updates to the UI components without
            triggering any events.
            Therefore the UI uses the "act(T)" method to change the property state and triggers the
            action of the property. On the contrary the "set(T)" method is used to change the state
            of a property without triggering the action, but it will trigger the "onShow" actions
            of the property. This is so that the UI can update itself when the user changes the
            state of a property.
        """
        given : 'A simple property with a view and model actions.'
            var showListener = []
            var modelListener = []
            var property = Var.of(":)")
                                .withAction( it ->{
                                    modelListener << it.current().orElseThrow()
                                })
            property.onShow( it -> showListener << it )

        when : 'We change the state of the property using the "set(T)" method.'
            property.set(":(")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The "onShow" actions are triggered.'
            showListener == [":("]
        and : 'The view model actions are not triggered.'
            modelListener == []

        when : 'We change the state of the property using the "act(T)" method.'
            property.act(":|")
        then : 'The "onShow" actions are NOT triggered, because the "act" method performs an "act on your view model"!'
            showListener == [":("]
        and : 'The view model actions are triggered.'
            modelListener == [":|"]
    }

    def 'A property constructed using the "of" factory method, does not allow null values.'()
    {
        reportInfo """
            The "of" factory method is used to create a property that does not allow null values.
            If you try to set a null value, the property will throw an exception.
        """
        given : 'A property constructed using the "of" factory method.'
            var property = Var.of("Hello World")
        when : 'We try to set a null value.'
            property.set(null)
        then : 'An exception is thrown.'
            thrown(NullPointerException)
    }

    def 'The string representation of a property will give you all the information you need.'()
    {
        reportInfo """
            The string representation of a property will tell you the 
            the current state, type and id of the property.
        """
        given : 'Some simple non-null properties.'
            var v1 = Var.of("Apple")
            var v2 = Var.of("Berry").withID("fruit")
            var v3 = Var.of(42)
            var v4 = Var.of(42).withID("number")
            var v5 = Var.of(99f).withID("ninety-nine")
        and : 'Nullable properties:'
            var v6 = Var.ofNullable(String, null)
            var v7 = Var.ofNullable(Long, 5L).withID("maybe long")
            var v8 = Var.ofNullable(Integer, 7).withID("maybe int")

        expect :
            v1.toString() == '"Apple" ( type = String, id = "?" )'
            v2.toString() == '"Berry" ( type = String, id = "fruit" )'
            v3.toString() == '42 ( type = Integer, id = "?" )'
            v4.toString() == '42 ( type = Integer, id = "number" )'
            v5.toString() == '99.0 ( type = Float, id = "ninety-nine" )'
        and : 'Nullable properties have a "?" in the type:'
            v6.toString() == 'null ( type = String?, id = "?" )'
            v7.toString() == '5 ( type = Long?, id = "maybe long" )'
            v8.toString() == '7 ( type = Integer?, id = "maybe int" )'

    }

    def 'A property can be converted to an Optional.'()
    {
        reportInfo """
            A property can be converted to an Optional using the "toOptional()" method.
            This is useful when you want to use the Optional API to query the state of the property.
        """
        given : 'A property with a non-null value.'
            var property = Var.of("Hello World")
        when : 'We convert the property to an Optional.'
            var optional = property.toOptional()
        then : 'The Optional contains the current state of the property.'
            optional.isPresent()
            optional.get() == "Hello World"

        when : 'The try the same using a nullable property.'
            property = Var.ofNullable(String, null)
            optional = property.toOptional()
        then : 'The Optional is empty.'
            optional.isEmpty()
    }

    def 'Multiple properties can be modelled through the "Vars" and "Vals" classes.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Apple", "Banana")
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of("Cherry", "Date")
        expect : 'Both the "Vars" and "Vals" have two properties.'
            vars.size() == 2
            vars.at(0).get() == "Apple"
            vars.at(1).get() == "Banana"
            vals.size() == 2
            vals.at(0).get() == "Cherry"
            vals.at(1).get() == "Date"
        and : 'You can also use the "First" and "last" methods.'
            vars.first().get() == "Apple"
            vars.last().get() == "Banana"
            vals.first().get() == "Cherry"
            vals.last().get() == "Date"
        and : 'They also have the correct type.'
            vars.type() == String
            vals.type() == String

        and : 'The "Vals" class has no methods for mutation, it is read only (basically a tuple).'
            Vals.metaClass.getMethods().findAll{ it.name == "set" }.size() == 0
            Vals.metaClass.getMethods().findAll{ it.name == "add" }.size() == 0
            Vals.metaClass.getMethods().findAll{ it.name == "remove" }.size() == 0
        and : 'Both property lists are not empty'
            !vars.isEmpty() && vars.isNotEmpty()
            !vals.isEmpty() && vals.isNotEmpty()
        and : 'Both property lists are iterable'
            vars.each{ it }
            vals.each{ it }

        when : 'We change the state of the "Vars" properties.'
            vars.at(0).set("Apricot")
            vars.at(1).set("Blueberry")
        then : 'The "Vars" properties have changed.'
            vars.at(0).get() == "Apricot"
            vars.at(1).get() == "Blueberry"

        when : 'We use the "aetAt" method to change the state of the "Vars" properties.'
            vars.setAt(0, "Tim")
            vars.setAt(1, "Tom")
        then : 'The "Vars" properties have changed.'
            vars.at(0).get() == "Tim"
            vars.at(1).get() == "Tom"
    }

    def 'You can remove n leading or trailing entries from a property list.'()
    {
        reportInfo """
            A very common use case is to remove the first or last entry from a list.
            Not only can you do this with the "removeFirst()" and "removeLast()" methods,
            but you can also remove n entries from the start or end of the list
            through the "removeFirst(int)" and "removeLast(int)" methods.
        """
        given : 'A "Vars" class with six properties.'
            var vars = Vars.of("Racoon", "Squirrel", "Turtle", "Piglet", "Rooster", "Rabbit")
        expect : 'The "Vars" class has six properties.'
            vars.size() == 6
        when : 'We remove the first entry from the list.'
            vars.removeFirst()
        then : 'The "Vars" class has five properties.'
            vars.size() == 5
        and : 'The first entry has been removed.'
            vars.at(0).get() == "Squirrel"
        when : 'We remove the last entry from the list.'
            vars.removeLast()
        then : 'The "Vars" class has four properties.'
            vars.size() == 4
        and : 'The last entry has been removed.'
            vars.at(3).get() == "Rooster"
        when : 'We remove the first two entries from the list.'
            vars.removeFirst(2)
        then : 'The "Vars" class has two properties.'
            vars.size() == 2
        and : 'The first two entries have been removed.'
            vars.at(0).get() == "Piglet"
            vars.at(1).get() == "Rooster"
        when : 'We remove the last two entries from the list.'
            vars.removeLast(2)
        then : 'The "Vars" class has no properties.'
            vars.size() == 0
    }

    def 'The properties of one property list can be added to another property list.'()
    {
        reportInfo """
            The properties of one property list can be added to another property list.
            This is useful when you want to combine two property lists into one.
        """
        given : 'A "Vars" class with three properties.'
            var vars1 = Vars.of("Racoon", "Squirrel", "Turtle")
        and : 'A "Vars" class with three properties.'
            var vars2 = Vars.of("Piglet", "Rooster", "Rabbit")
        expect : 'The "Vars" classes have three properties.'
            vars1.size() == 3
            vars2.size() == 3
        when : 'We add the properties of the second "Vars" class to the first "Vars" class.'
            vars1.addAll(vars2)
        then : 'The "Vars" class has six properties.'
            vars1.size() == 6
        and : 'The properties of the second "Vars" class have been added to the first "Vars" class.'
            vars1.at(0).get() == "Racoon"
            vars1.at(1).get() == "Squirrel"
            vars1.at(2).get() == "Turtle"
            vars1.at(3).get() == "Piglet"
            vars1.at(4).get() == "Rooster"
            vars1.at(5).get() == "Rabbit"
    }

    def 'The "Vars" ist a list of properties which can grow and shrink.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        expect : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Dal Biji"
        when : 'We add a new property to the "Vars" class.'
            vars.add("Chapati")
        then : 'The "Vars" class has three properties.'
            vars.size() == 3
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Dal Biji"
            vars.at(2).get() == "Chapati"
        when : 'We remove a property from the "Vars" class.'
            vars.remove("Dal Biji")
        then : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Chapati"
    }

    def 'Both the "Vars" and immutable "Vals" types can be used for functional programming.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of("Chapati", "Papad")

        when : 'We use the "map" method to transform all the properties.'
            var mappedVars = vars.map{ it.toUpperCase() }
            var mappedVals = vals.map{ it.toUpperCase() }
        then : 'The properties have been transformed.'
            mappedVars.at(0).get() == "KACHORI"
            mappedVars.at(1).get() == "DAL BIJI"
            mappedVals.at(0).get() == "CHAPATI"
            mappedVals.at(1).get() == "PAPAD"
    }

    def 'Map a "Vals" instance from one type of properties to another.'()
    {
        given : 'A "Vals" class with two properties.'
            var vals = Vals.of("Chapati", "Papad")
        when : 'We use the "mapTo" method to transform all the properties.'
            var mappedVals = vals.mapTo(Integer, { it.length() })
        then : 'The properties have been transformed.'
            mappedVals.at(0).get() == 7
            mappedVals.at(1).get() == 5
    }


    def 'Map a "Vars" instance from one type of properties to another.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        when : 'We use the "mapTo" method to transform all the properties.'
            var mappedVars = vars.mapTo(Integer, { it.length() })
        then : 'The properties have been transformed.'
            mappedVars.at(0).get() == 7
            mappedVars.at(1).get() == 8
    }

    def 'You can create the "Vars"/"Vals" property lists from property instances.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(Var.of("Chana"), Var.of("Dal"))
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of(Val.of("Chapati"), Val.of("Papad"))
        expect : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Chana"
            vars.at(1).get() == "Dal"
        and : 'The "Vals" class has two properties.'
            vals.size() == 2
            vals.at(0).get() == "Chapati"
            vals.at(1).get() == "Papad"
    }

    def 'Just like a regular "Var" property you can register "show" listeners on "Vars".'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" listener on the "Vars" class.'
            vars.onShow{ changes << it.index() }

        when : 'We modify the property in various ways...'
            vars.addAt(1, 1)
            vars.setAt(0, 2)
            vars.removeAt(1)
            vars.add(3)

        then : 'The "show" listener has been called four times.'
            changes.size() == 4
        and : 'The "show" listener has been called with the correct indices.'
            changes == [1, 0, 1, 2]
    }

    def 'The display action of a property or list of properties will not be called if they report "canBeRemoved()"'()
    {
        given : 'A single property and list of two properties.'
            var prop = Var.of(7)
            var list = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" listeners on both objects.'
            prop.onShowThis(new UIAction<ValDelegate<Integer>>() {
                @Override
                void accept(ValDelegate<Integer> delegate) {
                    changes << "Something happened to the property."
                }
                @Override boolean canBeRemoved() { return true }
            })
            list.onShow(new UIAction<ValsDelegate<Integer>>() {
                @Override
                void accept(ValsDelegate<Integer> delegate) {
                    changes << "Something happened to the list."
                }
                @Override boolean canBeRemoved() { return true }
            })

        when : 'We modify the properties in various ways...'
            prop.set(42)
            list.add(1)
            list.removeAt(1)
            list.setAt(0, 2)
            list.addAt(1, 3)
            list.remove(3)

        then : 'The "show" listener has been called zero times.'
            changes.size() == 0
    }

    def 'The listeners registered in property lists will be informed what type of modification occurred.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" listener on the "Vars" class.'
            vars.onShow{ changes << it.type() }

        when : 'We modify the property in various ways...'
            vars.addAt(1, 1)
            vars.setAt(0, 2)
            vars.removeAt(1)
            vars.add(3)

        then : 'The "show" listener has been called four times.'
            changes.size() == 4
        and : 'The "show" listener has been called with the correct indices.'
            changes == [Mutation.ADD, Mutation.SET, Mutation.REMOVE, Mutation.ADD]
    }

}

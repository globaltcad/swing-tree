package com.globaltcad.swingtree.mvvm

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.api.mvvm.Val
import com.globaltcad.swingtree.api.mvvm.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

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
            property.get() == "Hello World"
    }

    def 'They can be bound to the UI by passing them to a builder node.'()
    {
        reportInfo """
            Binding goes both ways, so when the property changes we can update the UI
            using the "show()" method on the property, and when
            the UI is changed by the user, it will update the property for us
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
            toggled.set(true).show()
        then : 'The button should be updated.'
            ui.component.selected == true

        when : 'We try to un-toggle the toggle button in the UI...'
            ui.component.doClick()
        then : 'The property should be toggled off again.'
            toggled.get() == false
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
            mutable.get() == 42
        and : "It has the expected type."
            mutable.type() == Integer.class

        when : 'We change the value of the mutable property.'
            mutable.set(69)
        then : 'The property stores the new value.'
            mutable.get() == 69

        when : 'We now downcast the mutable property to an immutable property.'
            Val<Integer> immutable = mutable
        then : 'The immutable property will only expose the "get()" method, but not "set(..)".'
            immutable.get() == 69
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
        then : 'Nothing happens initially...'
            list == []

        when : 'We call the "show()" method on the property however...'
            mutable.show()
        then : 'The side effect is executed.'
            list == ["Tofu"]
    }

    def 'Properties not only have a value but also a type and id!'()
    {
        given : 'We create a named property...'
            Val<String> property = Var.of(String, "Hello World").withID("XY")
        expect : 'The property has the expected name.'
            property.id() == "XY"
        and : 'The property has the expected type.'
            property.type() == String.class
    }

    def 'A property can only wrap null if we specify a type class.'()
    {
        given : 'We create a property with a type class...'
            Val<String> property = Var.of(String, null)
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

        when : 'We change and "show" the value of the original property.'
            property.set("Tofu").show()
        then : 'The subscriber of the original property is triggered but not the subscriber of the new property.'
            list1 == ["Tofu"]
            list2 == []

        when : 'We change and "show" the value of the new property.'
            property2.set("Tempeh").show()
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
            Val<String> empty = Val.of(String, null)
        then : 'The property is empty, regardless of how we map it.'
            empty.map( it -> it.length() ) == Val.of(Void, null)
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
            Val<Long> property = Val.of(Long, null)
        when : 'We try to access the value of the property.'
            property.get()
        then : 'The property will throw an exception.'
            thrown(NoSuchElementException)
    }

    def 'The "orElseThrow" method should be used instead of "get".'()
    {
        reportInfo """
            Note that accessing the value of an empty property using the "get" method
            will throw an exception.
            Use this method instead to make the intend of your code more clear.
        """
        given : 'We create a property...'
            Val<Long> property = Val.of(Long, null)
        when : 'We try to access the value of the property.'
            property.orElseThrow()
        then : 'The property will throw an exception.'
            thrown(NoSuchElementException)
    }

    def 'The equality and hash code of a property are based on its value, type and id!'()
    {
        given : 'We create various kinds of properties...'
            Var<Integer> num = Var.of(1)
            Var<Long>    num2 = Var.of(1L)
            Var<String>  str = Var.of("Hello World")
            Var<String>  str2 = Var.of(String, null)
            Var<String>  str3 = Var.of(String, null)
            Var<Boolean> bool = Var.of(Boolean, null)
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
}

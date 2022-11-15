package com.globaltcad.swingtree.mvvm

import com.globaltcad.swingtree.api.mvvm.Val
import com.globaltcad.swingtree.api.mvvm.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

@Title("Properties")
@Narrative('''

    Properties are a powerful tool to model the state 
    as well as business logic of your UI without actually depending on it.
    This is especially useful for testing your UIs logic.
    Therefore properties are a root concept in the Swing-Tree library.
    The decoupling between your UI and the UIs state and logic 
    is achieved by binding properties to UI components.
    This specification shows you how to use model UI state 
    and business logic with properties 
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

    def 'Properties can be bound by subscribing to them using the "onView(..)" method.'()
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
        when : 'We subscribe to the property using the "onView(..)" method.'
            mutable.onView { list.add(it) }
        and : 'We change the value of the property.'
            mutable.set("Tofu")
        then : 'Nothing happens initially...'
            list == []

        when : 'We call the "view()" method on the property however...'
            mutable.view()
        then : 'The side effect is executed.'
            list == ["Tofu"]
    }

    def 'Properties not only have a value but also a type and id!'()
    {
        given : 'We create a named property...'
            Val<String> property = Var.of(String, "Hello World", "name")
        expect : 'The property has the expected name.'
            property.id() == "name"
        and : 'The property has the expected type.'
            property.type() == String.class
    }

}

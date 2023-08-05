package swingtree.mvvm

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import sprouts.Val
import sprouts.Vals
import sprouts.Var
import sprouts.Vars

import java.util.function.Consumer

@Title("Property Null and Mutability Safety")
@Narrative('''

    Properties are a core concept in Swing-Tree. They are in essence
    just wrapper classes used to represent the state of your views.
    Because of this, it is important that properties are
    are nullable only when they need to be, and that they are
    immutable only when they need to be.
    
    Furthermore, using nullable properties in Swing-Tree
    will be prohibited it is does not make sense.
    For example, you may not use null to model the text of a label,
    instead, the empty String "" must be used!
    
''')
@Subject([Val, Var, Vals, Vars])
class Property_Safety_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'The "get" method of a property throws an exception if the property is null.'()
    {
        reportInfo """
            The "get" method of a property throws an exception if the property is null
            for the same reason that the "get" method of the Optional class throws an
            exception if the Optional is empty. It is a way to force the developer to
            handle the case where the property is null. If the developer does not want
            to handle the case where the property is null, then the developer should
            use the "orElse" method instead.
            The "get" method is only intended to be used in a context where
            the property is expected to have a value or is fully non-nullable altogether. 
            Use the "orElse" or "orElseNull" method to clearly indicate that the property
            may be null.
        """

        given : 'A nullable property.'
            def nullable = Var.ofNullable(Long, null)

        when : 'The property is accessed...'
            nullable.get()

        then : '...an exception is thrown.'
            thrown(Exception)
    }

    def 'A property can only wrap null if we specify a type class.'()
    {
        reportInfo """
            This is necessary simply
            because we cannot know what type of object the property should wrap
            based on the supplied null. Generics are erased at runtime, so
            we do not know what type of object the property should wrap.
        """
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

    def 'A "Val" instance is immutable.'( Consumer<Val<Integer>> code )
    {
        reportInfo """
            A "Val" instance is immutable because it is a wrapper around a
            value that is not intended to be changed. If the value is
            intended to be changed, then the developer should use a "Var"
            type instead.
            The "Val" interface is a supertype of the "Var" interface, so it is
            mainly used as a way to protect mutable properties from
            being accidentally changed outside of the intended scope.
            Creating a Val from its factory method "of" will always return
            an immutable instance.
        """
        given : 'A "Val" instance wrapping a simple integer.'
            def val = Val.of(42)

        when : 'The value is changed...'
            code(val)

        then : '...an exception is thrown.'
            thrown(Exception)

        where : 'The following ways of trying to modify the state of the property should fail:'
           code << [
                   { it.set(7) },
                   { it.act(9) }
                ]
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

    def 'The "orElseNull" method should be used instead of "orElseThrow" if you are fine with null items.'()
    {
        reportInfo """
            Note that accessing the value of an empty property using the "orElseThrow" method
            will throw an exception if it is null!
            Use "orElseNull" if you are fine with your property being empty 
            and to also make this intend clear.
        """
        given : 'We create a property...'
            Val<Long> property = Val.ofNullable(Long, null)
        expect : 'The property is empty.'
            property.orElseNull() == null
            property.isEmpty()
            !property.isPresent()
        when : 'We try to access the value of the property through "orElseThroe".'
            property.orElseThrow()
        then : 'The property will throw an exception.'
            thrown(NoSuchElementException)
    }

    def '"Vals", a list of properties, is immutable.'( Consumer<Vals<Integer>> code )
    {
        given : 'A "Vals" instance wrapping a simple integer.'
            def vals = Vals.of(42)

        when : 'The value is changed...'
            code(vals)

        then : '...an exception is thrown.'
            thrown(Exception)

        where : 'The following ways of trying to modify the state of the property should fail:'
           code << [
                   { it.add(7)      },
                   { it.addAt(0, 7) },
                   { it.removeAt(9) },
                   { it.sort()      },
                   { it.clear()     }
                ]
    }

    def 'A "Var" may only wrap null if it is created as nullable.'()
    {
        reportInfo """
            A "Var" may only wrap null if it is explicitly created as nullable
            property through the "ofNullable" factory method. If the developer
            wants to create a "Var" that is not nullable, then they
            should use the "of" factory method instead.
            The "Var" interface is a subtype of the "Val" interface, 
            which simply adds the ability to change the value of the property.
        """
        given : '2 mutable properties, a nullable and a non-nullable property.'
            def nullable = Var.ofNullable(Long, 73L)
            def nonNull  = Var.of(42L)

        when : 'The nullable property is set to null...'
            nullable.set(null)

        then : '...it succeeds.'
            nullable.orElseNull() == null

        when : 'We want to store a non-null item in the nullable property...'
            nullable.set(7L)

        then : '...that also succeeds, because the property can wrap both null and non-null items.'
            nullable.get() == 7L

        when : 'On the other hand, the non-nullable property is set to null...'
            nonNull.set(null)

        then : '...an exception is thrown.'
            thrown(Exception)
    }

    def 'Swing-Tree will not allow you to model a check box selection using a nullable properties.'()
    {
        reportInfo """
            When you try to bind a nullable property to the state of a UI component for which
            null does not make sense, Swing-Tree will throw an exception.
            For example, if you try to bind a nullable property to the "selected" state of a
            "JCheckBox", then Swing-Tree will throw an exception because a "JCheckBox" cannot
            be in a "null" state.
        """
        given : 'A nullable property.'
            def property = Var.ofNullable(Boolean, null)
        when : 'We try to bind the property to a "JCheckBox".'
            UI.checkBox("Check Me Out!", property)
        then : 'Swing-Tree will throw an exception.'
            thrown(Exception)
    }

    def 'You may not model the text of a "JLabel" using a nullable property.'()
    {
        reportInfo """
            When you try to bind a nullable property to the text of a UI component for which
            null does not make sense, Swing-Tree will throw an exception.
            A good example for this is text, ergo Strings, where the absence of content
            is represented by an empty String, not null.
            Here you will see that if you try to bind a nullable property to the text of a
            "JLabel", then Swing-Tree will throw an exception because a "JLabel" text
            cannot be in a "null" state.
        """
        given : 'A nullable property.'
            def property = Var.ofNullable(String, null)
        when : 'We try to bind the property to a "JLabel".'
            UI.label(property)
        then : 'Swing-Tree will throw an exception.'
            var e = thrown(Exception)
        and :
            e.message == "Property 'text' of type 'String' may not be null, but it was. Please use an empty string instead of null!"
    }

}

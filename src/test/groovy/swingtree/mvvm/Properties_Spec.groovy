package swingtree.mvvm

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Val
import sprouts.Var

import java.util.function.Consumer

@Title("Properties")
@Narrative('''

    SwingTree includes support for writing UIs using the MVVM pattern,
    by shipping with a set of observable property types as part of the 
    included **Sprouts property API**,
    which is designed from the ground up to be used for modelling Swing UIs.
    
    Properties are a powerful tool because they make it possible to
    model the state and logic of your UIs without actually depending on them,
    which is especially useful for testing, debugging and refactoring.
    
    This specification introduces you to their API and shows you how to use them.
    
    (For more information about the raw properties themselves
    check out their [repository](https://github.com/globaltcad/sprouts))
    
''')
@Subject([Val, Var])
class Properties_Spec extends Specification
{
    def 'Properties are simple wrappers around a value'()
    {
        given : 'We create a property using the "of" factory method.'
            Var<String> property = Var.of("Hello World")

        expect : 'The property has the same value as the value we passed to the factory method.'
            property.orElseNull() == "Hello World"
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

    def 'Properties not only have a value but also a type and id!'()
    {
        given : 'We create a property with an id...'
            Val<String> property = Var.ofNullable(String, "Hello World").withId("XY")
        expect : 'The property has the expected id.'
            property.id() == "XY"
        and : 'The property has the expected type.'
            property.type() == String.class
    }

    def 'Properties are similar to the `Optional` class, you can map them and see if they are empty or not.'()
    {
        given : 'We create a property...'
            Val<String> property = Val.of("Hello World")
        expect : 'We can map the property to another property.'
            property.mapTo(Integer, it -> it.length() ) == Val.of(11)
        and : 'We can check if the property is empty.'
            property.isEmpty() == false

        when : 'We create a property that is empty...'
            Val<String> empty = Val.ofNullable(String, null)
        then : 'The property is empty, regardless of how we map it.'
            empty.mapTo(Integer, it -> it.length() ) == Val.ofNullable(Integer, null)
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
        then : 'The view is updated.'
            view.orElseNull() == 4
    }

    def 'There are various kinds of convenience methods for creating live view of properties.'()
    {
        given : 'We create a property...'
            Var<String> food = Var.of("Channa Masala")
        and : 'Different kinds of views:'
            Var<Integer> words    = food.viewAsInt( f -> f.split(" ").length )
            Var<Integer> words2   = words.view({it * 2})
            Var<Double>  average  = food.viewAsDouble( f -> f.chars().average().orElse(0) )
            Var<Boolean> isLong   = food.viewAs(Boolean, f -> f.length() > 14 )
            Var<String> firstWord = food.view( f -> f.split(" ")[0] )
            Var<String> lastWord  = food.view( f -> f.split(" ")[f.split(" ").length-1] )
        expect : 'The views have the expected values.'
            words.get() == 2
            words2.get() == 4
            average.get().round(2) == 92.92d
            isLong.get() == false
            firstWord.get() == "Channa"
            lastWord.get() == "Masala"

        when : 'We change the value of the property.'
            food.set("Tofu Tempeh Saitan")
        then : 'The views are updated.'
            words.get() == 3
            words2.get() == 6
            average.get().round(2) == 94.28d
            isLong.get() == true
            firstWord.get() == "Tofu"
            lastWord.get() == "Saitan"
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

    def 'The `get()` method will throw an exception if there is no element present.'()
    {
        reportInfo """
            Note that accessing the value of an empty property using the `get()` method
            will throw an exception.
            It is recommended to use the `orElseNull()` method instead, because the `get()`
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

    def 'A property constructed using the `of` factory method, does not allow null items.'()
    {
        reportInfo """
            The `of(..)` factory method is used to create a property that does not allow null items.
            If you try to set an item to null, the property will throw an exception.
        """
        given : 'A property constructed using the "of" factory method.'
            var property = Var.of("Hello World")
        when : 'We try to set null.'
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
            var v2 = Var.of("Berry").withId("fruit")
            var v3 = Var.of(42)
            var v4 = Var.of(42).withId("number")
            var v5 = Var.of(99f).withId("ninety_nine")
        and : 'Nullable properties:'
            var v6 = Var.ofNullable(String, null)
            var v7 = Var.ofNullable(Long, 5L).withId("maybe_long")
            var v8 = Var.ofNullable(Integer, 7).withId("maybe_int")

        expect :
            v1.toString() == 'Var<String>["Apple"]'
            v2.toString() == 'Var<String>[fruit="Berry"]'
            v3.toString() == 'Var<Integer>[42]'
            v4.toString() == 'Var<Integer>[number=42]'
            v5.toString() == 'Var<Float>[ninety_nine=99.0]'
        and : 'Nullable properties have a "?" in the type:'
            v6.toString() == 'Var<String?>[null]'
            v7.toString() == 'Var<Long?>[maybe_long=5]'
            v8.toString() == 'Var<Integer?>[maybe_int=7]'

    }

    def 'A property can be converted to an `Optional`.'()
    {
        reportInfo """
            A property can be converted to an `Optional` using the `toOptional()` method.
            This is useful when you want to use the Optional API to query the state of the property.
        """
        given : 'A property with a non-null item.'
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
            !optional.isPresent()
    }

    def 'Conveniently compare the item of a property with another item using "is", "isOneOf" or "isNot"'()
    {
        reportInfo """
            Properties are all about the item they hold, so there needs to be a convenient way
            to check whether the item of a property is equal to another item.
            The "is" method is used to check if the item of a property is equal to another item
            and the "isNot" method is the exact opposite, it checks if the item of a property
            is NOT equal to another item.
            The "isOneOf" method is used to check if the item of a property is equal to one of the
            items in a varargs list.
        """
        given : 'We create a property with a non-null item.'
            var property = Var.of("Hello World")
        when : 'We compare the item of the property with another item using the above mentioned methods.'
            var is1 = property.is("Hello World")
            var is2 = property.is("Hello World!")
            var isNot1 = property.isNot("Hello World")
            var isNot2 = property.isNot("Hello World!")
            var isOneOf1 = property.isOneOf("Hello World", "Goodbye World")
            var isOneOf2 = property.isOneOf("Hello World!", "Goodbye World")
        then : 'The results are as expected.'
            is1
            !is2
            !isNot1
            isNot2
            isOneOf1
            !isOneOf2
    }

    def 'Conveniently compare properties with another item using "is", "isOneOf" or "isNot"'()
    {
        reportInfo """
            Properties represent the items that they hold, so when comparing them with each other
            you are actually comparing the items they hold.
            The "is" method can be used to check if the item of a property is equal to the item of another property
            and the "isNot" method is the exact opposite, it checks if the item of a property
            is NOT equal to the item of another property.
            The "isOneOf" method is used to check if the item of a property is equal to one of the
            items in a varargs list of properties.
        """
        given : 'We create a property with a non-null item.'
            var property1 = Var.of("Hello World")
            var property2 = Var.of("Hello World!")
            var property3 = Var.of("Goodbye World")
        when : 'We compare the item of the property with another item using the above mentioned methods.'
            var is1 = property1.is(Var.of("Hello World"))
            var is2 = property1.is(property3)
            var isNot1 = property1.isNot(Var.of("Hello World"))
            var isNot2 = property1.isNot(property3)
            var isOneOf1 = property1.isOneOf(property2, property3)
            var isOneOf2 = property1.isOneOf(property3, Var.of("Hello World"), property2)
        then : 'The results are as expected.'
            is1
            !is2
            !isNot1
            isNot2
            !isOneOf1
            isOneOf2
    }

}

package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.Bounds
import swingtree.layout.Location
import swingtree.layout.Size

@Title("Utility Primitives")
@Narrative('''

    The SwingTree library contains a set of simple value objects
    that are used as basic building blocks for your UI declarations.
    These are primitive utility classes for modelling simple things
    like size, position and bounds of UI components.
    
    In this specification we will explore the basic primitives
    and ensure that they behave as expected.

''')
@Subject([Bounds, Size, Location])
class Primitives_Spec extends Specification
{
    def 'You can merge two bounds together to form one encompassing bounds.'()
    {
        given:
            var bounds1 = Bounds.of(10, 10, 100, 100)
            var bounds2 = Bounds.of(110, 110, 100, 100)

        when:
            var merged = bounds1.merge(bounds2)

        then:
            merged.location().x() == 10
            merged.location().y() == 10
            merged.size().width().get() == 200
            merged.size().height().get() == 200
    }

    def 'Use `hasSize(int,int)` to check if a `Bounds` object has the given size.'()
    {
        given:
            var bounds = Bounds.of(10, 10, 100, 100)

        expect:
            bounds.hasSize(100, 100)
            !bounds.hasSize(101, 100)
            !bounds.hasSize(100, 101)
    }

    def 'The `withSize(int,int)` method returns a new `Bounds` object with the given size.'()
    {
        given:
            var bounds = Bounds.of(10, 10, 100, 100)

        when:
            var newBounds = bounds.withSize(200, 200)

        then:
            newBounds.size().width().get() == 200
            newBounds.size().height().get() == 200
    }

    def 'Use `withX(int)` and `withY(int)` to change the location of a `Bounds` object.'()
    {
        given:
            var bounds = Bounds.of(10, 10, 100, 100)

        when:
            var newBounds = bounds.withX(24).withY(13)

        then:
            newBounds.location().x() == 24
            newBounds.location().y() == 13
    }
}

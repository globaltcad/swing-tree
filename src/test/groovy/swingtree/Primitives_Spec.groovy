package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.Bounds
import swingtree.layout.Position
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
@Subject([Bounds, Size, Position])
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

    def 'The `hasWidth()` and `hasHeight()` methods can be used to check the width and height of a `Bounds` object.'()
    {
        reportInfo """
            Note that a `Bounds` object may be incomplete, i.e. it may not
            necessarily have both width and height. This is because the `Size`
            of a `Bounds` object may not have both dimensions set to a positive
            or zero value.
        """
        given: 'We create a hand full of different `Bound` objects:'
            var bounds1 = Bounds.of(10, 10, 100, 0)
            var bounds2 = Bounds.of(5, 80, 0, 200)
            var bounds3 = Bounds.of(0, 10, 200, 80)
            var bounds4 = Bounds.of(20, 90, 400, 200)
        expect:
            bounds1.hasWidth() && !bounds1.hasHeight()
            !bounds2.hasWidth() && bounds2.hasHeight()
            bounds3.hasWidth() && bounds3.hasHeight()
            bounds4.hasWidth() && bounds4.hasHeight()
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

    def 'Use `withWidth(int)` and `withHeight(int)` to change the size of a `Bounds` object.'()
    {
        given:
            var bounds = Bounds.of(10, 10, 100, 100)

        when:
            var newBounds = bounds.withWidth(200).withHeight(200)

        then:
            newBounds.size().width().get() == 200
            newBounds.size().height().get() == 200
    }

    def 'A `Size` object can be created with a width and height.'()
    {
        given:
            var size = Size.of(100, 200)
        expect:
            size.width().get() == 100
            size.height().get() == 200
    }

    def 'A `Size` with a negative width and height produces an `unknown` size.'()
    {
        expect:
            Size.of(-1, -1) == Size.unknown()
    }

    def 'The `Size` object exposes its width and height as `Optional` float.'()
    {
        reportInfo """
            The `Optional` type is used to represent the possibility of a value
            being present or absent. This is useful when a value may not be
            available or may be unknown. In the case of a `Size` object, the
            width and height may be unknown if the values are negative.
            So to represent this, the `Size` object returns an `Optional` float
            for both width and height values.
        """
        given:
            var size1 = Size.of(42, 189)
            var size2 = Size.of(-1, -1)
        expect:
            size1.width().get() == 42
            size1.height().get() == 189
        and:
            !size2.width().isPresent()
            !size2.height().isPresent()
    }

    def 'You can use `scale(float)` to scale a `Size` object by a given factor.'()
    {
        given:
            var size = Size.of(100, 200)
        when:
            var scaled = size.scale(2.5f)
        then:
            scaled.width().get() == 250
            scaled.height().get() == 500

        when : 'Scaling by a negative factor will produce an unknown size.'
            var scaled2 = size.scale(-1.5f)
        then:
            scaled2 == Size.unknown()
    }

    def 'Use `hasPositiveWidth()` and `hasPositiveHeight()` to check if a `Size` object has positive width and height.'()
    {
        given:
            var size1 = Size.of(42, 278)
            var size2 = Size.of(-21, 189)
            var size3 = Size.of(79, -19)
            var size4 = Size.unknown()
            var size5 = Size.of(0, 12)
            var size6 = Size.of(24, 0)
        expect:
            size1.hasPositiveWidth() && size1.hasPositiveHeight()
            !size2.hasPositiveWidth() && size2.hasPositiveHeight()
            size3.hasPositiveWidth() && !size3.hasPositiveHeight()
            !size4.hasPositiveWidth() && !size4.hasPositiveHeight()
            !size5.hasPositiveWidth() && size5.hasPositiveHeight()
            size6.hasPositiveWidth() && !size6.hasPositiveHeight()
    }

    def 'A `Location` object can be created with an x and y coordinate.'()
    {
        given:
            var location = Position.of(128, 62)
        expect:
            location.x() == 128
            location.y() == 62
    }

    def 'A `Location` can have negative x and y coordinates.'()
    {
        given:
            var location = Position.of(-1, -7)
        expect:
            location.x() == -1
            location.y() == -7
    }

    def 'Two `Bounds` objects are equal if they have the same location and size.'()
    {
        given:
            var bounds1 = Bounds.of(1, 2, 10, 20)
            var bounds2 = Bounds.of(1, 2, 10, 20)
            var bounds3 = Bounds.of(5, 2, 12, 5)
            var bounds4 = Bounds.of(5, 2, 12, 5)
        expect:
            bounds1 == bounds2
            bounds3 == bounds4
            bounds1 != bounds3
        and : 'The `hashCode()` method is consistent with the `equals()` method.'
            bounds1.hashCode() == bounds2.hashCode()
            bounds3.hashCode() == bounds4.hashCode()
            bounds1.hashCode() != bounds3.hashCode()
    }

    def 'You can check if a location is inside a `Bounds` object.'()
    {
        given :
            var bounds1 = Bounds.of(1,  2, 15, 20)
            var bounds2 = Bounds.of(1,  2, 0, 20)
            var bounds3 = Bounds.of(1,  2, 15, 0)
        expect :
            bounds1.contains(1, 2)
            !bounds1.contains(0.99, 2)
            !bounds1.contains(1, 1.999)
            bounds1.contains(15.999, 21.999)
            !bounds1.contains(16, 21.999)
            !bounds1.contains(15.999, 22)
        and :
            !bounds2.contains(1, 2)
            !bounds2.contains(0.99, 2)
            !bounds2.contains(1, 1.999)
            !bounds2.contains(1, 21.999)
            !bounds2.contains(16, 21.999)
            !bounds2.contains(1, 22)
        and :
            !bounds3.contains(1, 2)
            !bounds3.contains(0.99, 2)
            !bounds3.contains(1, 1.999)
            !bounds3.contains(15.999, 2)
            !bounds3.contains(16, 2)
            !bounds3.contains(15.999, 2)
    }

    def 'Use `equal(double,double,double,double)` to check if a bounds instance is equal to the supplied arguments.'()
    {
        given:
            var bounds = Bounds.of(3, -6, 70, 7)
        expect:
            bounds.equals(3, -6, 70, 7)
            !bounds.equals(4, -6, 70, 7)
            !bounds.equals(3.003, -6, 70, 7)
            !bounds.equals(3, -5.99, 70, 7)
            !bounds.equals(3, -6, 70.023, 7)
            !bounds.equals(3, -6, 70, 6.999)
    }

    def 'Two `Size` objects can be added together using the `plus(Size)` method.'(
        float width, float height, float width2, float height2, float sumWidth, float sumHeight
    ) {
        given:
            var size1 = Size.of(width, height)
            var size2 = Size.of(width2, height2)
        when:
            var sum = size1.plus(size2)
        then:
            sumWidth  < 0 ? !sum.width().isPresent()  : sum.width().get()  == sumWidth
            sumHeight < 0 ? !sum.height().isPresent() : sum.height().get() == sumHeight
        where :
            width | height | width2 | height2 | sumWidth | sumHeight
             10   |  20    |  30    |  40     | 40       |  60
             10   |  20    |  30    | -40     | 40       |  20
             10   |  20    | -1     |  40     | 10       |  60
             10   |  20    | -1     | -1      | 10       |  20
    }

    def 'Two `Size` objects can be added together using the `plus(double, double)` method.'(
        float width, float height, float width2, float height2, float sumWidth, float sumHeight
    ) {
        given:
            var size1 = Size.of(width, height)
        when:
            var sum = size1.plus(width2, height2)
        then:
            sumWidth  < 0 ? !sum.width().isPresent()  : sum.width().get()  == sumWidth
            sumHeight < 0 ? !sum.height().isPresent() : sum.height().get() == sumHeight
        where :
            width | height | width2 | height2 | sumWidth | sumHeight
             10   |  20    |  30    |  40     |  40      |  60
             10   |  20    |  30    | -40     |  40      |  -1
             10   |  20    | -5     |  40     |   5      |  60
             10   |  20    | -5     | -5      |   5      |  15
    }

    def 'Two `Size` objects can be subtracted using the `minus(Size)` method.'()
    {
        given:
            var size1 = Size.of(50, 20)
            var size2 = Size.of(30, 40)
        when:
            var diff = size1.minus(size2)
        then:
            diff.width().get() == 20
            !diff.height().isPresent()
    }

}

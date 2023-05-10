package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI

@Title("Styling Components")
@Narrative('''
    This specification demonstrates how you can use the styling
    API to style Swing components in declarative SwingTree code.
''')
class Individual_Component_Styling_Spec extends Specification
{
    def 'Styling components is based on a functional styler lambda.'()
    {
        reportInfo """
            Fun-Fact: 
            Styling in SwingTree is fully functional, which means 
            that the `Style` settings objects are all immutable. 
            They are not modified in place, but instead transformed
            by so called "styler" lambdas.
            Not only does this architecture make it easy to compose, reuse and share
            styles, but it also makes it possible to have a complex style
            inheritance hierarchy without the need for very complex code.
            In practice, this means that your styler lambdas become part
            of a giant tree of styler lambdas, which is then applied to
            the component tree in a single pass.
            How cool is that? :)
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.style()
                              .foundationColor("green")
                              .backgroundColor("cyan")
                              .borderColor("blue")
                              .borderWidth(5)
                              .shadowColor("black")
                              .shadowSpreadRadius(10)
                              .shadowOffset(10)
                        )
        expect :
            panel != null
    }
}

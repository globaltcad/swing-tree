package swingtree.styles

import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI

import javax.swing.*

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

    def 'The margins defined in the style API will be applied to the layout manager.'()
    {
        reportInfo """
            The default layout manager for SwingTree is MigLayout.
            It is a very powerful layout manager which is also supported by the styling API.
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.style()
                              .marginRight(42)
                              .marginLeft(64)
                        )
                        .get(JPanel)
        expect :
            panel != null
            panel.layout != null
            panel.layout instanceof MigLayout
            ((MigLayout)panel.layout).getLayoutConstraints().contains("insets 0px 64px 0px 42px")
    }


    def 'The insets of the layout manager are based on the sum of the margin and padding for a given edge of the component bounds.'()
    {
        reportInfo """
            Swing does not have a concept of padding and margin.
            Without a proper layout manager it does not even support the configuration of insets.
            However, because we are using MigLayout, we can model the padding and margin of a component
            by using the layout constraints of the layout manager
            and some custom rendering code.
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.style()
                              .marginTop(11)
                              .marginRight(42)
                              .marginLeft(64)
                              .padRight(10)
                              .padLeft(20)
                              .padBottom(30)
                        )
                        .get(JPanel)
        expect :
            panel != null
            panel.layout != null
            panel.layout instanceof MigLayout
            ((MigLayout)panel.layout).getLayoutConstraints().contains("insets 11px 84px 30px 52px")
    }

    def 'The Styling API will make sure that the layout manager accounts for the border width!'()
    {
        reportInfo """
            A border is a very common feature of Swing components and when it comes to styling
            your UI elements should not overlap with the border.
            This is why the styling API will make sure that the layout manager accounts for the border width,
            meaning that the insets of the layout manager will be increased by the border width.
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.style()
                              .marginTop(7)
                              .marginRight(2)
                              .padLeft(14)
                              .borderWidth(5)
                        )
                        .get(JPanel)
        expect :
            panel != null
            panel.layout != null
            panel.layout instanceof MigLayout
            ((MigLayout)panel.layout).getLayoutConstraints().contains("insets 12px 19px 5px 7px")
    }
}

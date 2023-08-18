package swingtree.styles

import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI

@Title("Layout Styling")
@Narrative('''

    SwingTree allows you to define the layout of your components 
    through the styling API, which is a functional DSL
    that not only allows you to define how a component should be painted,
    like for example the background color, the font, the border widths, etc.
    but also allows you to define the miglayout constraints for the component.
    
    The examples defined in this specification will demonstrate how to use the styling API
    to define the layout of your components.

''')
class Layout_Styling extends Specification
{
    def 'The x and y alignment values of a component can be configured through the styling API'()
    {
        reportInfo """
            The x, and y alignment values are float values that can be used to align a component
            within its parent container. The x alignment value is used to align the component
            horizontally, and the y alignment value is used to align the component vertically.
            
            Note that not all layout managers support the alignment values, and that the alignment
            values are only used when the component is smaller than the parent container.
        """
        given :
            var ui =
                    UI.panel("fill")
                    .add(
                        UI.button().withStyle( it -> it
                            .alignmentX(0.27f)
                            .alignmentY(0.73f)
                        )
                    )
        and : 'We unpack the Swing tree:'
            var panel = ui.component
            var button = panel.getComponent(0)

        expect : 'The alignment values of the button are set to the values we specified in the styling API:'
            button.alignmentX == 0.27f
            button.alignmentY == 0.73f

        and : """
            Although the vanilla MigLayout does not support the alignment values,
            the SwingTree layout will inform the MigLayout of the alignment values.
            So we can check and see that the MigLayout has 
            the alignment values encoded into the component constraints of the button:
        """
            ((CC)((MigLayout)panel.layout).constraintMap[button]).horizontal.getAlign().value == 27f
            ((CC)((MigLayout)panel.layout).constraintMap[button]).vertical.getAlign().value == 73f
    }
}

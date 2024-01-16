package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI

import javax.swing.JTextField

@Title("Opaque or not Opaque")
@Narrative('''

    This specification focuses on the opaqueness of Swing components and
    how the SwingTree style engine relates to it.
    The opaqueness flag of a component is an interesting property.
    You might think that it merely controls whether the background of
    the component is painted or not. 
    
    But it is actually more than that.
    
    This flag is at the center of some important rendering optimizations in Swing.
    If a component is reporting itself as opaque, then Swing assumes that a repaint
    only needs to be done on that component and not on any of its ancestors.
    This is a big deal because it means that Swing can avoid repainting a lot of
    components in the hierarchy.
    
    This sound relatively straightforward, but it becomes a lot more complicated together
    with the SwingTree style API and its underlying styling engine.
    So for example, if a component has rounded corners, and no foundational background
    color, then it is technically not opaque.
    Leaving it opaque will cause strange rendering artifacts, due to the parent component
    not being repainted.
    
    In this specification we will explore the different scenarios and how the SwingTree
    style engine deals with them.

''')
class Opaqueness_Styles_Spec extends Specification
{
    def 'A component styled to have round corners will no longer be opaque.'()
    {
        given :
            var ui =
                    UI.textField()
                    .withStyle(it -> it
                        .borderRadius(16)
                    )

        and :
            var textField = ui.get(JTextField)

        expect :
            textField.isOpaque() == false
    }

    def 'A component styled to have round corners together with a foundation color will stay opaque.'()
    {
        given :
            var ui =
                    UI.textField()
                    .withStyle(it -> it
                        .borderRadius(16)
                        .foundationColor("blue")
                    )

        and :
            var textField = ui.get(JTextField)

        expect :
            textField.isOpaque() == true
    }

}


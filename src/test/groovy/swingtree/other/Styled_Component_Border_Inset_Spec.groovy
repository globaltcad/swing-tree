package swingtree.other

import spock.lang.Specification
import swingtree.SwingTree
import swingtree.UI
import utility.Utility

import javax.swing.*
import java.awt.*

class Styled_Component_Border_Inset_Spec extends Specification
{

    def 'A heavily styled text field will have the correct border insets!'(
        float uiScale
    ) {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'Now we create a text field UI with a custom styler lambda and a button.'
            var seed = Utility.loadImage("img/seed.png")
            var trees = Utility.loadImage("img/trees.png")
            var ui =
                    UI.textField("I am fancy! :)").withLayout("fill, ins 0").withPrefSize(190, 25)
                    .withStyle( it -> it
                        .fontSize(16)
                        .padding(0, 0, 0, 26)
                        .marginRight(25)
                        .paddingRight(-20)
                        .image(image -> image
                            .layer(UI.Layer.BORDER)
                            .image(seed)
                            .placement(UI.Placement.LEFT)
                            .width(30).autoFit(true)
                            .padding(3)
                        )
                    )
                    .add("right",
                        UI.button(19, 19, new ImageIcon(trees))
                        .withStyle( it -> it
                            .margin(0)
                            .cursor(UI.Cursor.HAND)
                        )
                        .makePlain()
                    );
        and : 'We unpack the component and its border:'
            var component = ui.getComponent()
            var border = component.getBorder()


        expect : 'The insets are as expected.'
            border.getBorderInsets(component) == new Insets(2, 2 + (int)(26*uiScale), 2, (int)(25*uiScale - 20*uiScale))
            border.getMarginInsets() == new Insets(0,0,0,(int)(25*uiScale))
            border.getPaddingInsets() == new Insets(0, (int)(26*uiScale), 0, (int)(-20*uiScale))
            border.getFullPaddingInsets() == new Insets(0, (int)(26*uiScale), 0, (int)(25*uiScale - 20*uiScale))

        where :
            uiScale << [1, 2, 3]
    }

}

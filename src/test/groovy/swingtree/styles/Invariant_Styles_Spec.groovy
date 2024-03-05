package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.api.Styler
import swingtree.components.JBox
import utility.Utility

import java.awt.Color

@Title('Invariant Styles')
@Narrative("""
    This test demonstrates how various pairs of different styles 
    produce identical images when rendered into a BufferedImage.
""")
@Subject([UI, Styler])
class Invariant_Styles_Spec extends Specification
{
    def 'Diagonally linear gradients can be invariant in certain cases.'( float uiScale )
    {
        reportInfo """
            This test demonstrates how various pairs of different gradient styles 
            produce identical images when rendered into a BufferedImage.
            More specifically we examine diagonal linear gradients.
            Diagonally linear menus that a gradient transitions from one corner
            of a component to the opposite corner.
        """
        given : """
            We first set the UI scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We declare to variables for storing pairs of identical widgets with different styles.'
            var ui1
            var ui2
        when : 'We create two widgets with different styles.'
            ui1 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.RED, Color.BLUE)
                            .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                        )
                    )
        and :
            ui2 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.BLUE, Color.RED)
                            .span(UI.Span.BOTTOM_RIGHT_TO_TOP_LEFT)
                        )
                    )

        and : 'We render the two widgets into a BufferedImage.'
            var image1 = Utility.renderSingleComponent(ui1.get(JBox))
            var image2 = Utility.renderSingleComponent(ui2.get(JBox))

        then : 'They both have the expected size:'
            image1.width  == Math.round( 280 * uiScale )
            image1.height == Math.round( 38  * uiScale )
            image2.width  == Math.round( 280 * uiScale )
            image2.height == Math.round( 38  * uiScale )
        and : 'The images are identical even though they have different styles:'
            Utility.similarityBetween(image1, image2) > 99.9

        when : 'We create another pair of widgets with opposite gradient transitions.'
            ui1 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .prefSize(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.PINK, Color.GREEN)
                            .span(UI.Span.BOTTOM_LEFT_TO_TOP_RIGHT)
                        )
                    )
        and :
            ui2 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .prefSize(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.GREEN, Color.PINK)
                            .span(UI.Span.TOP_RIGHT_TO_BOTTOM_LEFT)
                        )
                    )

        and : 'We render the two widgets into a BufferedImage.'
            image1 = Utility.renderSingleComponent(ui1.get(JBox))
            image2 = Utility.renderSingleComponent(ui2.get(JBox))

        then :  'They both have the expected size:'
            image1.width  == Math.round( 280 * uiScale )
            image1.height == Math.round( 38  * uiScale )
            image2.width  == Math.round( 280 * uiScale )
            image2.height == Math.round( 38  * uiScale )
        and : 'The images are identical even though they have different styles:'
            Utility.similarityBetween(image1, image2) > 99.9

        where : 'We test this using the following scaling values:'
            uiScale << [1f, 1.25f, 1.75f, 2f]
    }

    def 'Vertically and horizontally linear gradients can be invariant in certain cases.'( float uiScale )
    {
        reportInfo """
            This test demonstrates how various pairs of different styles 
            describing linear gradients produce identical images when rendered into a BufferedImage.
            Their invariant emerges from the order of the colors and the direction of the gradients
            transitioning from one color to the other being reversed.
        """
        given : """
            We first set the UI scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We declare to variables for storing pairs of identical widgets with different styles.'
            var ui1
            var ui2
        when : 'We create two widgets with different styles.'
            ui1 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.RED, Color.BLUE)
                            .span(UI.Span.LEFT_TO_RIGHT)
                        )
                    )
        and :
            ui2 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.BLUE, Color.RED)
                            .span(UI.Span.RIGHT_TO_LEFT)
                        )
                    )
        and : 'We render the two widgets into a BufferedImage.'
            var image1 = Utility.renderSingleComponent(ui1.get(JBox))
            var image2 = Utility.renderSingleComponent(ui2.get(JBox))

        then : 'They both have the expected size:'
            image1.width  == Math.round( 280 * uiScale )
            image1.height == Math.round( 38  * uiScale )
            image2.width  == Math.round( 280 * uiScale )
            image2.height == Math.round( 38  * uiScale )
        and : 'The images are identical even though they have different styles:'
            Utility.similarityBetween(image1, image2) > 99.9

        when : 'We create another pair of widgets with opposite gradient transitions.'
            ui1 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .prefSize(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.PINK, Color.GREEN)
                            .span(UI.Span.TOP_TO_BOTTOM)
                        )
                    )
        and :
            ui2 = UI.box()
                    .withStyle( it -> it
                        .size(280, 38)
                        .prefSize(280, 38)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.GREEN, Color.PINK)
                            .span(UI.Span.BOTTOM_TO_TOP)
                        )
                    )
        and : 'We render the two widgets into a BufferedImage.'
            image1 = Utility.renderSingleComponent(ui1.get(JBox))
            image2 = Utility.renderSingleComponent(ui2.get(JBox))

        then :  'They both have the expected size:'
            image1.width  == Math.round( 280 * uiScale )
            image1.height == Math.round( 38  * uiScale )
            image2.width  == Math.round( 280 * uiScale )
            image2.height == Math.round( 38  * uiScale )
        and : 'The images are identical even though they have different styles:'
            Utility.similarityBetween(image1, image2) > 99.9

        where : 'We test this using the following scaling values:'
            uiScale << [1f, 1.25f, 1.75f, 2f]
    }

    def 'A diagonally aligned radial gradient with an offset focus point can be invariant in certain cases.'( float uiScale )
    {
        reportInfo """
            This test demonstrates how various pairs of different styles 
            describing radial gradients produce identical images when rendered into a BufferedImage.
            Their invariant emerges from the order of the colors and the direction of the gradients
            transitioning from one color to the other being reversed.
        """
        given : """
            We first set the UI scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We declare to variables for storing pairs of identical widgets with different styles.'
            var ui1
            var ui2
        when : 'We create two widgets with different styles.'
            ui1 = UI.box()
                    .withStyle( it -> it
                        .size(280, 138)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.BLACK, Color.CYAN, Color.GREEN, Color.MAGENTA)
                            .boundary(UI.ComponentBoundary.CENTER_TO_CONTENT)
                            .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                            .type(UI.GradientType.RADIAL)
                            .focus(26,16)
                        )
                    )
        and :
            ui2 = UI.box()
                    .withStyle( it -> it
                        .size(280, 138)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.BLACK, Color.CYAN, Color.GREEN, Color.MAGENTA)
                            .boundary(UI.ComponentBoundary.CENTER_TO_CONTENT)
                            .span(UI.Span.BOTTOM_RIGHT_TO_TOP_LEFT)
                            .type(UI.GradientType.RADIAL)
                            .focus(26,16)
                        )
                    )

        and : 'We render the two widgets into a BufferedImage.'
            var image1 = Utility.renderSingleComponent(ui1.get(JBox))
            var image2 = Utility.renderSingleComponent(ui2.get(JBox))

        then : 'They both have the expected size:'
            image1.width  == Math.round( 280 * uiScale )
            image1.height == Math.round( 138 * uiScale )
            image2.width  == Math.round( 280 * uiScale )
            image2.height == Math.round( 138 * uiScale )
        and : 'The images are identical even though they have different styles:'
            Utility.similarityBetween(image1, image2) > 99.9

        when : 'We create another pair of widgets with opposite gradient transitions.'
            ui1 = UI.box()
                    .withStyle( it -> it
                        .size(280, 138)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.PINK, Color.GREEN, Color.BLUE)
                            .boundary(UI.ComponentBoundary.CENTER_TO_CONTENT)
                            .span(UI.Span.BOTTOM_LEFT_TO_TOP_RIGHT)
                            .type(UI.GradientType.RADIAL)
                            .focus(16,16)
                        )
                    )
        and :
            ui2 = UI.box()
                    .withStyle( it -> it
                        .size(280, 138)
                        .borderRadius(20)
                        .margin(10)
                        .gradient( gradConf -> gradConf
                            .colors(Color.PINK, Color.GREEN, Color.BLUE)
                            .boundary(UI.ComponentBoundary.CENTER_TO_CONTENT)
                            .span(UI.Span.TOP_RIGHT_TO_BOTTOM_LEFT)
                            .type(UI.GradientType.RADIAL)
                            .focus(16,16)
                        )
                    )

        and : 'We render the two widgets into a BufferedImage.'
            image1 = Utility.renderSingleComponent(ui1.get(JBox))
            image2 = Utility.renderSingleComponent(ui2.get(JBox))

        then :  'They both have the expected size:'
            image1.width  == Math.round( 280 * uiScale )
            image1.height == Math.round( 138 * uiScale )
            image2.width  == Math.round( 280 * uiScale )
            image2.height == Math.round( 138 * uiScale )
        and : 'The images are identical even though they have different styles:'
            Utility.similarityBetween(image1, image2) > 99.9

        where : 'We test this using the following scaling values:'
            uiScale << [1f, 1.25f, 1.75f, 2f]
    }

}
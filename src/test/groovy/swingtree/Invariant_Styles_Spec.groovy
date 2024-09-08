package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.Configurator
import swingtree.api.Styler
import swingtree.components.JBox
import swingtree.style.ComponentExtension
import swingtree.style.FontConf
import utility.Utility

import javax.swing.*
import java.awt.*
import java.time.DayOfWeek

@Title('Invariant Styles')
@Narrative("""
    This test demonstrates how various pairs of different styles 
    produce identical component states as well as identical
    images when rendered into a BufferedImage.
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

    def 'Two labels with the same font style configuration will have equal font objects.'(
        Configurator<FontConf> styler
    ) {
        given: 'Two labels with the same font style configuration'
            var ui1 = UI.label("A").withStyle(it->it.componentFont(styler))
            var ui2 = UI.label("B").withStyle(it->it.componentFont(styler))
        and : 'We build the two labels and also create a plain reference label:'
            var label1 = ui1.get(JLabel)
            var label2 = ui2.get(JLabel)
            var label3 = new JLabel("C")
        when: 'We unpack the font objects from the labels.'
            var font1 = label1.getFont()
            var font2 = label2.getFont()
            var font3 = label3.getFont()
        then: 'The first two labels have the same font object.'
            font1 == font2
        and : 'The third label on the other hand has a different font object.'
            font3 != font1
            font3 != font2
        where : 'We test the following font configurations:'
            styler << [
                {f -> f.size(1).family("Arial").style(UI.FontStyle.BOLD)},
                {f -> f.size(2).family("Ubuntu").style(UI.FontStyle.BOLD_ITALIC)},
                {f -> f.size(3).family("SansSerif").style(UI.FontStyle.ITALIC)},
                {f -> f.gradient(g->g.colors(Color.RED, Color.BLUE).span(UI.Span.TOP_TO_BOTTOM))},
                {f -> f.size(82)
                       .family("Dialog")
                       .weight(2.75)
                       .color(Color.WHITE)
                       .noise(noise -> noise
                               .colors(Color.DARK_GRAY, Color.CYAN)
                               .function(UI.NoiseType.CELLS)
                               .scale(1.25)
                       )
                       .backgroundNoise(noise -> noise
                               .colors(Color.BLACK, Color.BLUE)
                               .function(UI.NoiseType.CELLS)
                               .scale(1.25)
                       )
                },
                {f -> f.size(82)
                       .family("Noto Sans")
                       .weight(1.25)
                       .color(Color.BLUE)
                       .noise(noise -> noise
                           .colors(Color.DARK_GRAY, Color.CYAN)
                           .function(UI.NoiseType.SMOOTH_SPOTS)
                           .scale(1.15)
                       )
                       .backgroundGradient(grad -> grad
                           .colors(Color.GREEN, Color.MAGENTA, Color.ORANGE)
                           .boundary(UI.ComponentBoundary.CENTER_TO_CONTENT)
                           .span(UI.Span.BOTTOM_TO_TOP)
                           .type(UI.GradientType.CONIC)
                           .focus(26,16)
                       )
                }
            ]
    }

    def 'The view state hash code function on the SingTree internal ComponentExtension hashes view related state.'(
        boolean isEqual, UIForAnySwing ui1, UIForAnySwing ui2
    ) {
        given : 'We build the actual components of the UI declarations:'
            var comp1 = ui1.get(ui1.getType())
            var comp2 = ui2.get(ui2.getType())
        when : 'We calculate the hash codes of the two components.'
            var hash1 = ComponentExtension.from(comp1).viewStateHashCode()
            var hash2 = ComponentExtension.from(comp2).viewStateHashCode()
        then : 'The hash codes are equal or not depending on the expected result.'
            (hash1 == hash2) == isEqual
        where : 'We test the following UI declarations:'
            isEqual  | ui1                                                | ui2

            true     | UI.textField("")                                   | UI.textField("")
            false    | UI.textField("x")                                  | UI.textField("y")

            true     | UI.toggleButton("◌")                               | UI.toggleButton("◌")
            false    | UI.toggleButton("◌")                               | UI.toggleButton("⬤")

            true     | UI.slider(UI.Align.HORIZONTAL, 0, 0, 0)            | UI.slider(UI.Align.HORIZONTAL, 0, 0, 0)
            false    | UI.slider(UI.Align.HORIZONTAL, 0, 0, 0)            | UI.slider(UI.Align.VERTICAL,   0, 0, 0)
            false    | UI.slider(UI.Align.HORIZONTAL, 0, 0, 0)            | UI.slider(UI.Align.HORIZONTAL, -10, 0, 0)
            false    | UI.slider(UI.Align.HORIZONTAL, 0, 0, 0)            | UI.slider(UI.Align.HORIZONTAL, -10, 0, 10)

            true     | UI.slider(UI.Align.HORIZONTAL, 0, 20, 2)           | UI.slider(UI.Align.HORIZONTAL, 0, 20, 2)
            false    | UI.slider(UI.Align.HORIZONTAL, 0, 20, 2)           | UI.slider(UI.Align.HORIZONTAL, 0, 20, 3)

            true     | UI.panel().withBackground(UI.Color.RED)            | UI.panel().withBackgroundColor("red")
            false    | UI.panel().withBackground(UI.Color.RED)            | UI.panel().withBackground(UI.Color.BLUE)

            true     | UI.menu().withBackgroundColor("oak")               | UI.menu().withBackgroundColor("oak")
            false    | UI.menu().withBackgroundColor("oak")               | UI.menu().withBackgroundColor("tan")

            true     | UI.textArea("Hello").withForeground(UI.Color.PINK) | UI.textArea("Hello").withForegroundColor("pink")
            false    | UI.textArea("Hello").withForeground(UI.Color.PINK) | UI.textArea("Hello").withForeground(UI.Color.BLUE)

            true     | UI.html("Hello").withForeground(UI.Color.PINK)     | UI.html("Hello").withForeground(UI.Color.PINK)
            false    | UI.html("Hello").withForeground(UI.Color.PINK)     | UI.html("Hello").withForeground(UI.Color.BLUE)

            true     | UI.button("⬤").withStyle(it->it.border(1, "green")) | UI.button("⬤").withStyle(it->it.border(1, "green"))
            false    | UI.button("⬤").withStyle(it->it.border(1, "green")) | UI.button("⬤").withStyle(it->it.border(1, "blue"))

            true     | UI.list("A", "B", "C").withSelection(Var.of("A"))  | UI.list("A", "B", "C").withSelection(Var.of("A"))
            false    | UI.list("A", "B", "C").withSelection(Var.of("A"))  | UI.list("A", "B", "C").withSelection(Var.of("B"))

            true     | UI.comboBox(Var.of(DayOfWeek.FRIDAY))              | UI.comboBox(Var.of(DayOfWeek.FRIDAY))
            false    | UI.comboBox(Var.of(DayOfWeek.FRIDAY))              | UI.comboBox(Var.of(DayOfWeek.SATURDAY))

            true     | UI.spinner(Var.of(5))                              | UI.spinner(Var.of(5))
            false    | UI.spinner(Var.of(5))                              | UI.spinner(Var.of(6))

            true     | UI.spinner(2, -10, 10)                             | UI.spinner(2, -10, 10)
            false    | UI.spinner(2, -10, 10)                             | UI.spinner(3, -10, 10)
            true     | UI.spinner(2, -10, 10)                             | UI.spinner(2, -15, 10) // they look the same
            true     | UI.spinner(2, -10, 10)                             | UI.spinner(2, -15, 15) // they look the same

            true     | UI.progressBar(0, 100, 50)                         | UI.progressBar(0, 100, 50)
            false    | UI.progressBar(0, 100, 50)                         | UI.progressBar(0, 100, 60)
            false    | UI.progressBar(0, 100, 50)                         | UI.progressBar(0, 90,  50)
            false    | UI.progressBar(0, 100, 50)                         | UI.progressBar(10, 100, 50)

            true     | UI.checkBox("A").isSelectedIf(true)                | UI.checkBox("A").isSelectedIf(true)
            false    | UI.checkBox("B").isSelectedIf(true)                | UI.checkBox("B").isSelectedIf(false)
            false    | UI.checkBox("C").isSelectedIf(true)                | UI.checkBox("").isSelectedIf(true)

    }

}

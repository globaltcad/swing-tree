package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.Configurator
import swingtree.style.FontConf
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.JToggleButton
import javax.swing.text.AttributeSet
import javax.swing.text.Element
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import java.awt.Font

@Title("Fonts")
@Narrative('''

    As part of the `UI` namespace, SwingTree ships with a custom `Font` class,
    which is an immutable value object containing a lot of properties and methods for 
    working with fonts in Swing applications.

''')
@Subject([UI.Font, UI, UIFactoryMethods])
class Font_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'Use the `UI.font(String)` method to parse a font string.'()
    {
        reportInfo """
            The `UI.font(String)` decodes a font string based on the following
            format: `family-style-size`. The `style` part is optional and can
            be one of the following: PLAIN, BOLD, ITALIC, or BOLD_ITALIC.
            You can also omit the size, in which case the default size of 12
            multiplied by the current look and feel's scaling factor is used.
        """
        given:
            var font1 = UI.font('Ubuntu-PLAIN-14').toAwtFont()
            var font2 = UI.font('Dancing Script-ITALIC-24').toAwtFont()
        expect :
            font1.family == 'Ubuntu'
            font1.style == Font.PLAIN
            font1.size == 14
        and :
            font2.family == 'Dancing Script'
            font2.style == Font.ITALIC
            font2.size == 24
    }

    def 'The `UI.font(String)` method allows you to access fonts from a system property.'()
    {
        given: 'First we setup a system property'
            System.setProperty('my.fonts.AlloyInk', 'Alloy Ink-ITALIC-42')
        when: 'We use the `UI.font(String)` method to access the font'
            var font = UI.font('my.fonts.AlloyInk').toAwtFont()
        then: 'The font is correctly parsed'
            font.family == 'a Alloy Ink'
            font.style == Font.ITALIC
            font.size == 42
    }

    def 'Simply pass the desired font name to the `UI.font(String)` to get a properly sized font object.'(
        float scalingFactor
    ) {
        reportInfo """
            The `UI.font(String)` method allows you to quickly instantiate a `Font` object
            with the given font family name that has a default size of 12 multiplied by the
            current look and feel's scaling factor.
        """
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(scalingFactor))
        and : 'A new font object based on the Buggie font family.'
            var font = UI.font('Buggie').toAwtFont()
        expect: 'The font object has the correct family and size.'
            font.family == 'Buggie'
            font.size == (int)(12 * scalingFactor)
            font.style == Font.PLAIN
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }

    def 'The `UI.font(String)` factory method can fetch fonts from system properties and apply the correct scaling factor.'(
        float scalingFactor
    ) {
        reportInfo """
            The `UI.font(String)` not only parses font strings, but it can also fetch font
            definitions from system properties if parsing fails. When fetching fonts from
            system properties, the correct scaling factor is also applied to the font size.
        """
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(scalingFactor))
        and : 'We setup a system property for a font'
            System.setProperty('my.fonts.TestFont', 'Ubuntu-BOLD-42')
        when : 'We fetch the font using the UI.font factory method'
            var font = UI.font('my.fonts.TestFont').toAwtFont()
        then : 'The font has the correct family, style, and scaled size'
            font.family == 'Ubuntu'
            font.style == Font.BOLD
            font.size == Math.round(42 * scalingFactor)
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }

    def 'The `UI.font(String)` factory method can create a font merely bz specifying the family.'(
        float scalingFactor
    ) {
        reportInfo """
            The `UI.font(String)` factory method can create a font merely by specifying
            the font family name. In this case, the default size of 12 multiplied by
            the current look and feel's scaling factor is used.
        """
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(scalingFactor))
        when : 'We create a font using only the family name'
            var font = UI.font('Dancing Script').toAwtFont()
        then : 'The font has the correct family and scaled size'
            font.family == 'Dancing Script'
            font.style == Font.PLAIN
            font.size == Math.round(12 * scalingFactor)
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }

    def 'A SwingTree `Font` object has value semantics.'()
    {
        reportInfo """
            Two `UI.Font`s with the same contents will be considered
            equal, and if they do not have the same contents, then they are
            not reported as equal by the `equal` and `hashCode` methods...
        """
        given : 'We create two reference fonts:'
            var f1 = UI.Font.of("Ubuntu", UI.FontStyle.BOLD, 14)
            var f2 = UI.Font.of("Dialog", UI.FontStyle.ITALIC, 13)
        expect :
            f1 != f2
            f1.conf() != f2.conf()

        when :
            f2 = f2.withName("Ubuntu").withSize(14).withStyle(UI.FontStyle.BOLD)
        then :
            f1 == f2
            f1.conf() == f2.conf()
            f1.hashCode() == f2.hashCode()
            f1.conf().hashCode() == f2.conf().hashCode()
    }

    def 'A SwingTree `Font` object has insightful String representations.'()
    {
        given : 'We create two reference fonts:'
            var f1 = UI.Font.of("Ubuntu", UI.FontStyle.PLAIN, 14)
            var f2 = UI.Font.of("Dialog", UI.FontStyle.ITALIC, 11)
            var f3 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 17).with((Configurator<FontConf>){FontConf c -> c.strikeThrough(true)})
        expect :
            f1.toString() == "UI.Font[" +
                                    "family=Ubuntu, size=14, posture=0.0, weight=0.0, spacing=0.0, underlined=false, " +
                                    "strikeThrough=false, selectionColor=?, transform=?, paint=FontPaintConf[NONE], " +
                                    "backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?" +
                                "]"
        and :
            f2.toString() == "UI.Font[" +
                                    "family=Dialog, size=11, posture=0.2, weight=0.0, spacing=0.0, underlined=false, " +
                                    "strikeThrough=false, selectionColor=?, transform=?, paint=FontPaintConf[NONE], " +
                                    "backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?" +
                                "]"
        and :
            f3.toString() == "UI.Font[" +
                                    "family=Buggie, size=17, posture=0.2, weight=2.0, spacing=0.0, underlined=false, " +
                                    "strikeThrough=true, selectionColor=?, transform=?, paint=FontPaintConf[NONE], " +
                                    "backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?" +
                                "]"

        when : 'We create a font with "unknown" posture and weight...'
            var f4 = f3.with((Configurator<FontConf>){FontConf c -> c.weight(-11).posture(-12)})
        then : 'The string representation will represent such unknown properties with a "?":'
            f4.toString() == "UI.Font[" +
                                "family=Buggie, size=17, posture=?, weight=?, spacing=0.0, underlined=false, " +
                                "strikeThrough=true, selectionColor=?, transform=?, paint=FontPaintConf[NONE], " +
                                "backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?" +
                            "]"
    }

    def 'A SwingTree `UI.Font` can define text placement information for labels.'()
    {
        reportInfo """
            On top of what a regular `java.awt.Font` has to offer in terms of properties,
            a SwingTree `UI.Font` also offers to configure text layout information in the form
            of vertical and horizontal alignment modes!
            These additional properties are used by some components, like labels for example,
            to place you text at a desired location in the component bounds.
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
                          .verticalAlignment(UI.VerticalAlignment.CENTER)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.alignment(UI.Alignment.TOP_CENTER)
            })
            var font3 = UI.Font.of("Dialog", UI.FontStyle.PLAIN, 17).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
                          .verticalAlignment(UI.VerticalAlignment.BOTTOM)
            })
        and : 'A UI declaration where is font is assigned to a label:'
            var panel =
                    UI.panel()
                    .add(UI.label("How").withFont(font1))
                    .add(UI.label("are").withFont(font2))
                    .add(UI.label("you?").withFont(font3))
                    .get(JPanel)
        and :
            var label1 = panel.components[0] as JLabel
            var label2 = panel.components[1] as JLabel
            var label3 = panel.components[2] as JLabel

        expect : 'The components report the correct alignment codes!'
            label1.getHorizontalAlignment() == JLabel.LEFT
            label1.getVerticalAlignment() == JLabel.CENTER

            label2.getHorizontalAlignment() == JLabel.CENTER
            label2.getVerticalAlignment() == JLabel.TOP

            label3.getHorizontalAlignment() == JLabel.RIGHT
            label3.getVerticalAlignment() == JLabel.BOTTOM
    }

    def 'A SwingTree `UI.Font` can define text placement information for button types.'()
    {
        reportInfo """
            On top of what a regular `java.awt.Font` has to offer in terms of properties,
            a SwingTree `UI.Font` also offers to configure text layout information in the form
            of vertical and horizontal alignment modes!
            These additional properties are used by some components, like buttons for example,
            to place you text at a desired location in the component bounds.
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
                          .verticalAlignment(UI.VerticalAlignment.CENTER)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.CENTER)
                            .verticalAlignment(UI.VerticalAlignment.TOP)
            })
            var font3 = UI.Font.of("Dialog", UI.FontStyle.PLAIN, 17).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
                          .verticalAlignment(UI.VerticalAlignment.BOTTOM)
            })
        and : 'A UI declaration where is font is assigned to a label:'
            var panel =
                    UI.panel()
                    .add(UI.button("How").withFont(font1))
                    .add(UI.toggleButton("are").withFont(font2))
                    .add(UI.checkBox("you?").withFont(font3))
                    .get(JPanel)
        and :
            var button = panel.components[0] as JButton
            var toggle = panel.components[1] as JToggleButton
            var check = panel.components[2] as JCheckBox

        expect : 'The button components report the correct alignment codes!'
            button.getHorizontalAlignment() == JLabel.LEFT
            button.getVerticalAlignment() == JLabel.CENTER

            toggle.getHorizontalAlignment() == JLabel.CENTER
            toggle.getVerticalAlignment() == JLabel.TOP

            check.getHorizontalAlignment() == JLabel.RIGHT
            check.getVerticalAlignment() == JLabel.BOTTOM
    }

    def 'A SwingTree `UI.Font` can define text placement information for text fields.'()
    {
        reportInfo """
            On top of what a regular `java.awt.Font` has to offer in terms of properties,
            a SwingTree `UI.Font` also offers to configure text layout information in the form
            of vertical and horizontal alignment modes!
            These additional properties are used by some components, like text fields for example,
            to place you text at a desired location in the component bounds.
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.alignment(UI.Alignment.CENTER_LEFT)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.CENTER)
                            .verticalAlignment(UI.VerticalAlignment.TOP)
            })
            var font3 = UI.Font.of("Dialog", UI.FontStyle.PLAIN, 17).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
                          .verticalAlignment(UI.VerticalAlignment.BOTTOM)
            })
        and : 'A UI declaration where is font is assigned to a label:'
            var panel =
                    UI.panel()
                    .add(UI.textField("How").withFont(font1))
                    .add(UI.textField("are").withFont(font2))
                    .add(UI.textField("you?").withFont(font3))
                    .get(JPanel)
        and :
            var textField1 = panel.components[0] as JTextField
            var textField2 = panel.components[1] as JTextField
            var textField3 = panel.components[2] as JTextField

        expect : 'The text fields report the correct alignment codes!'
            textField1.getHorizontalAlignment() == JLabel.LEFT
            textField2.getHorizontalAlignment() == JLabel.CENTER
            textField3.getHorizontalAlignment() == JLabel.RIGHT
    }

    // ---

    def 'A SwingTree `UI.Font` can define text placement information for labels reactively.'()
    {
        reportInfo """
            On top of what a regular `java.awt.Font` has to offer in terms of properties,
            a SwingTree `UI.Font` also offers to configure text layout information in the form
            of vertical and horizontal alignment modes!
            These additional properties are used by some components, like labels for example,
            to place you text at a desired location in the component bounds.
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
                          .verticalAlignment(UI.VerticalAlignment.CENTER)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.alignment(UI.Alignment.TOP_CENTER)
            })
            var font3 = UI.Font.of("Dialog", UI.FontStyle.PLAIN, 17).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
                          .verticalAlignment(UI.VerticalAlignment.BOTTOM)
            })
            var fontProp1 = Var.of(font1)
            var fontProp2 = Var.of(font2)
            var fontProp3 = Var.of(font3)
        and : 'A UI declaration where is font is assigned to a label:'
            var panel =
                    UI.panel()
                    .add(UI.label("How").withFont(fontProp1))
                    .add(UI.label("are").withFont(fontProp2))
                    .add(UI.label("you?").withFont(fontProp3))
                    .get(JPanel)
        and :
            var label1 = panel.components[0] as JLabel
            var label2 = panel.components[1] as JLabel
            var label3 = panel.components[2] as JLabel

        expect : 'The components report the correct alignment codes!'
            label1.getHorizontalAlignment() == JLabel.LEFT
            label1.getVerticalAlignment() == JLabel.CENTER

            label2.getHorizontalAlignment() == JLabel.CENTER
            label2.getVerticalAlignment() == JLabel.TOP

            label3.getHorizontalAlignment() == JLabel.RIGHT
            label3.getVerticalAlignment() == JLabel.BOTTOM

        when : 'We swap the fonts...'
            fontProp1.set(font2)
            fontProp2.set(font3)
            fontProp3.set(font1)
            UI.sync()
        then : 'The alignments are updated accordingly!'
            label1.getHorizontalAlignment() == JLabel.CENTER
            label1.getVerticalAlignment() == JLabel.TOP

            label2.getHorizontalAlignment() == JLabel.RIGHT
            label2.getVerticalAlignment() == JLabel.BOTTOM

            label3.getHorizontalAlignment() == JLabel.LEFT
            label3.getVerticalAlignment() == JLabel.CENTER
    }

    def 'A SwingTree `UI.Font` can define text placement information for button types reactively.'()
    {
        reportInfo """
            On top of what a regular `java.awt.Font` has to offer in terms of properties,
            a SwingTree `UI.Font` also offers to configure text layout information in the form
            of vertical and horizontal alignment modes!
            These additional properties are used by some components, like buttons for example,
            to place you text at a desired location in the component bounds.
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
                          .verticalAlignment(UI.VerticalAlignment.CENTER)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.CENTER)
                            .verticalAlignment(UI.VerticalAlignment.TOP)
            })
            var font3 = UI.Font.of("Dialog", 17).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
                          .verticalAlignment(UI.VerticalAlignment.BOTTOM)
            })
            var fontProp1 = Var.of(font1)
            var fontProp2 = Var.of(font2)
            var fontProp3 = Var.of(font3)
        and : 'A UI declaration where is font is assigned to a label:'
            var panel =
                    UI.panel()
                    .add(UI.button("How").withFont(fontProp1))
                    .add(UI.toggleButton("are").withFont(fontProp2))
                    .add(UI.checkBox("you?").withFont(fontProp3))
                    .get(JPanel)
        and :
            var button = panel.components[0] as JButton
            var toggle = panel.components[1] as JToggleButton
            var check = panel.components[2] as JCheckBox

        expect : 'The button components report the correct alignment codes!'
            button.getHorizontalAlignment() == JLabel.LEFT
            button.getVerticalAlignment() == JLabel.CENTER

            toggle.getHorizontalAlignment() == JLabel.CENTER
            toggle.getVerticalAlignment() == JLabel.TOP

            check.getHorizontalAlignment() == JLabel.RIGHT
            check.getVerticalAlignment() == JLabel.BOTTOM

        when : 'We swap the fonts...'
            fontProp1.set(font2)
            fontProp2.set(font3)
            fontProp3.set(font1)
            UI.sync()
        then : 'The alignments are updated accordingly!'
            check.getHorizontalAlignment() == JLabel.LEFT
            check.getVerticalAlignment() == JLabel.CENTER

            button.getHorizontalAlignment() == JLabel.CENTER
            button.getVerticalAlignment() == JLabel.TOP

            toggle.getHorizontalAlignment() == JLabel.RIGHT
            toggle.getVerticalAlignment() == JLabel.BOTTOM
    }

    def 'A SwingTree `UI.Font` can define text placement information for text fields reactively.'()
    {
        reportInfo """
            On top of what a regular `java.awt.Font` has to offer in terms of properties,
            a SwingTree `UI.Font` also offers to configure text layout information in the form
            of vertical and horizontal alignment modes!
            These additional properties are used by some components, like text fields for example,
            to place you text at a desired location in the component bounds.
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
                          .verticalAlignment(UI.VerticalAlignment.CENTER)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.CENTER)
                            .verticalAlignment(UI.VerticalAlignment.TOP)
            })
            var font3 = UI.Font.of("Dialog", UI.FontStyle.PLAIN, 17).with((Configurator<FontConf>){ FontConf it ->
                return it.alignment(UI.Alignment.BOTTOM_RIGHT)
            })
            var fontProp1 = Var.of(font1)
            var fontProp2 = Var.of(font2)
            var fontProp3 = Var.of(font3)
        and : 'A UI declaration where is font is assigned to a label:'
            var panel =
                    UI.panel()
                    .add(UI.textField("How").withFont(fontProp1))
                    .add(UI.textField("are").withFont(fontProp2))
                    .add(UI.textField("you?").withFont(fontProp3))
                    .get(JPanel)
        and :
            var textField1 = panel.components[0] as JTextField
            var textField2 = panel.components[1] as JTextField
            var textField3 = panel.components[2] as JTextField

        expect : 'The text fields report the correct alignment codes!'
            textField1.getHorizontalAlignment() == JLabel.LEFT
            textField2.getHorizontalAlignment() == JLabel.CENTER
            textField3.getHorizontalAlignment() == JLabel.RIGHT

        when : 'We swap the fonts...'
            fontProp1.set(font2)
            fontProp2.set(font3)
            fontProp3.set(font1)
            UI.sync()
        then : 'The alignments are updated accordingly!'
            textField1.getHorizontalAlignment() == JLabel.CENTER
            textField2.getHorizontalAlignment() == JLabel.RIGHT
            textField3.getHorizontalAlignment() == JLabel.LEFT
    }

    // ---

    def 'A SwingTree UI.Font can define text placement information for JTextPane components.'()
    {
        reportInfo """
            The JTextPane component supports paragraph alignment through StyledDocument attributes.
            SwingTree UI.Font alignment properties are applied to JTextPane using StyleConstants.setAlignment().
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.CENTER)
            })
            var font3 = UI.Font.of("Dialog", 17).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
            })
            var font4 = UI.Font.of("Arial", UI.FontStyle.PLAIN, 12).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.TRAILING)
            })
        and : 'A UI declaration where fonts are assigned to text panes:'
            var panel =
                    UI.panel()
                    .add(UI.textPane().withText("Left aligned text").withFont(font1))
                    .add(UI.textPane().withText("Center aligned text").withFont(font2))
                    .add(UI.textPane().withText("Right aligned text").withFont(font3))
                    .add(UI.textPane().withText("Justified text").withFont(font4))
                    .get(JPanel)
        and :
            var textPane1 = panel.components[0] as JTextPane
            var textPane2 = panel.components[1] as JTextPane
            var textPane3 = panel.components[2] as JTextPane
            var textPane4 = panel.components[3] as JTextPane

        expect : 'The text panes report the correct alignment through their styled documents!'
            getTextPaneAlignment(textPane1) == StyleConstants.ALIGN_LEFT
            getTextPaneAlignment(textPane2) == StyleConstants.ALIGN_CENTER
            getTextPaneAlignment(textPane3) == StyleConstants.ALIGN_RIGHT
            getTextPaneAlignment(textPane4) == StyleConstants.ALIGN_JUSTIFIED
    }

    def 'A SwingTree UI.Font can define text placement information for JTextPane components reactively.'()
    {
        reportInfo """
            JTextPane alignment should update reactively when the font property changes.
            This ensures dynamic alignment updates work properly with reactive programming.
        """
        given : 'We create various fonts with custom alignments:'
            var font1 = UI.Font.of("Buggie", UI.FontStyle.BOLD_ITALIC, 7).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
            })
            var font2 = UI.Font.of("Ubuntu", UI.FontStyle.ITALIC, 13).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.CENTER)
            })
            var font3 = UI.Font.of("Dialog", UI.FontStyle.PLAIN, 17).with((Configurator<FontConf>){ FontConf it ->
                return it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
            })
            var fontProp = Var.of(font1)
        and : 'A UI declaration with a reactive font property:'
            var textPane = UI.textPane().withText("Dynamic alignment text").withFont(fontProp).get(JTextPane)

        expect : 'The text pane starts with left alignment'
            getTextPaneAlignment(textPane) == StyleConstants.ALIGN_LEFT

        when : 'We change the font property to center alignment'
            fontProp.set(font2)
            UI.sync()
        then : 'The text pane alignment updates to center'
            getTextPaneAlignment(textPane) == StyleConstants.ALIGN_CENTER

        when : 'We change the font property to right alignment'
            fontProp.set(font3)
            UI.sync()
        then : 'The text pane alignment updates to right'
            getTextPaneAlignment(textPane) == StyleConstants.ALIGN_RIGHT

        when : 'We change back to left alignment'
            fontProp.set(font1)
            UI.sync()
        then : 'The text pane alignment returns to left'
            getTextPaneAlignment(textPane) == StyleConstants.ALIGN_LEFT
    }

    def 'JTextPane alignment handles edge cases correctly.'()
    {
        reportInfo """
            Test edge cases like empty text, very long text, and alignment changes with existing content.
        """
        given : 'We create fonts with different alignments:'
            var leftFont = UI.Font.of("Arial", 12).with((Configurator<FontConf>){
                it.horizontalAlignment(UI.HorizontalAlignment.LEFT)
            })
            var centerFont = UI.Font.of("Arial", 12).with((Configurator<FontConf>){
                it.horizontalAlignment(UI.HorizontalAlignment.CENTER)
            })
            var fontProp = Var.of(leftFont)
        and : 'A text pane with empty content'
            var emptyTextPane = UI.textPane().withText("").withFont(fontProp).get(JTextPane)
        and : 'A text pane with very long content'
            var longText = "This is a very long text that should wrap and demonstrate alignment behavior " * 10
            var longTextPane = UI.textPane().withText(longText).withFont(fontProp).get(JTextPane)

        expect : 'Empty text pane has correct initial alignment'
            getTextPaneAlignment(emptyTextPane) == StyleConstants.ALIGN_LEFT
            getTextPaneAlignment(longTextPane) == StyleConstants.ALIGN_LEFT

        when : 'We change alignment on empty text pane'
            fontProp.set(centerFont)
            UI.sync()
        then : 'Empty text pane alignment updates correctly'
            getTextPaneAlignment(emptyTextPane) == StyleConstants.ALIGN_CENTER
            getTextPaneAlignment(longTextPane) == StyleConstants.ALIGN_CENTER
    }

    def 'JTextPane alignment is preserved when text content changes.'()
    {
        reportInfo """
            Verify that alignment settings persist when the text content of JTextPane is modified.
        """
        given : 'A font with right alignment'
            var rightFont = UI.Font.of("Arial", UI.FontStyle.PLAIN, 12).with((Configurator<FontConf>){
                it.horizontalAlignment(UI.HorizontalAlignment.RIGHT)
            })
        and : 'A text pane with initial content'
            var textPane = UI.textPane().withText("Initial text").withFont(rightFont).get(JTextPane)

        expect : 'Text pane starts with right alignment'
            getTextPaneAlignment(textPane) == StyleConstants.ALIGN_RIGHT

        when : 'We modify the text content'
            textPane.setText("Modified text content")
        then : 'Alignment is preserved after text modification'
            getTextPaneAlignment(textPane) == StyleConstants.ALIGN_RIGHT

        when : 'We append more text'
            textPane.setText(textPane.text + " with additional content")
        then : 'Alignment remains right-aligned'
            getTextPaneAlignment(textPane) == StyleConstants.ALIGN_RIGHT
    }

    // ---

    def 'Text alignment for JTextPane components can be configured through the style API.'()
    {
        reportInfo """
            The JTextPane component supports paragraph alignment through StyledDocument attributes.
            If you specify the alignment in the style API, then SwingTree will correctly install this property for you.
        """
        given : 'A UI declaration where text pane have their text alignment specified through the Style API:'
            var panel =
                    UI.panel()
                    .add(UI.textPane().withText("Left aligned text").withStyle({it.fontAlignment(UI.HorizontalAlignment.LEFT)}))
                    .add(UI.textPane().withText("Center aligned text").withStyle({it.fontAlignment(UI.HorizontalAlignment.CENTER)}))
                    .add(UI.textPane().withText("Right aligned text").withStyle({it.fontAlignment(UI.HorizontalAlignment.RIGHT)}))
                    .add(UI.textPane().withText("Justified text").withStyle({it.fontAlignment(UI.HorizontalAlignment.TRAILING)}))
                    .get(JPanel)
        and :
            var textPane1 = panel.components[0] as JTextPane
            var textPane2 = panel.components[1] as JTextPane
            var textPane3 = panel.components[2] as JTextPane
            var textPane4 = panel.components[3] as JTextPane

        expect : 'The text panes report the correct alignment through their styled documents!'
            getTextPaneAlignment(textPane1) == StyleConstants.ALIGN_LEFT
            getTextPaneAlignment(textPane2) == StyleConstants.ALIGN_CENTER
            getTextPaneAlignment(textPane3) == StyleConstants.ALIGN_RIGHT
            getTextPaneAlignment(textPane4) == StyleConstants.ALIGN_JUSTIFIED
    }


    // Helper method to extract alignment from JTextPane
    private static int getTextPaneAlignment(JTextPane textPane) {
        StyledDocument doc = textPane.getStyledDocument()
        Element paragraph = doc.getParagraphElement(0)
        AttributeSet attr = paragraph.getAttributes()
        return StyleConstants.getAlignment(attr)
    }
}

package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.api.IconDeclaration

import javax.swing.JLabel
import javax.swing.SwingConstants

@Title("Labels")
@Narrative('''
    
    This spec demonstrates the use of labels
    in Swing-Tree.
    
''')
class Label_Spec extends Specification
{

    def 'A label UI can be created using the UI.label(..) factory method.'()
    {
        when : 'We create a label UI node...'
            var ui = UI.label("Test")
        and : 'We build the component.'
            var label = ui.get(JLabel)

        then : 'The component is a label.'
            label instanceof JLabel
        and : 'The label has the specified text.'
            label.text == "Test"
    }

    def 'The icon of a label may be specified using an `IconDeclaration`.'()
    {
        reportInfo """
            In larger applications you should consider using the `IconDeclaration`
            type instead of the `Icon` or `ImageIcon` classes directly.
            Implementations of the `IconDeclaration` interface are merely value objects
            describing the resource location of an icon, but not the icon itself.
            
            An ideal usage pattern would be an enum implementing the `IconDeclaration`
            interface, where each enum constant represents a different icon.
            Here an example:
            
            ```
            public enum Icons implements IconDeclaration
            {
                ADD("icons/add.png"),
                REMOVE("icons/remove.png"),
                EDIT("icons/edit.png"),
                SAVE("icons/save.png"),
                CANCEL("icons/cancel.png"),
                REFRESH("icons/refresh.png");
                // ...
                private final String path;
                
                Icons(String path) { this.path = path; }
                
                @Override public String path() {
                    return path;
                }
            }
            ```
           
            The reason for this design approach is the fact that traditional Swing icons
            are heavy objects whose loading and caching may or may not succeed.
            
            This is especially important in case of unit tests for your view models,
            where the icon may not be available at all, but you still want to test
            the behaviour of your view model.
        """
        given : 'We create an `IconDeclaration` as a simple path provider.'
            IconDeclaration iconDeclaration = IconDeclaration.of("img/seed.png")
        and : 'We create a new label ui node with the icon declaration.'
            var ui =
                    UI.label("Test")
                    .withIcon(iconDeclaration)
        and : 'We build the component.'
            var label = ui.get(JLabel)

        expect : 'The icon should be loaded and displayed.'
            label.icon != null
            label.icon.iconHeight == 512
            label.icon.iconWidth == 512
        and : 'The icon should be the same as the one we specified.'
            label.icon === iconDeclaration.find().get()
        and : 'The label should have the specified text.'
            label.text == "Test"
    }

    def 'Create labels with custom horizontal and vertical alignment.'() {
        given : 'We create a label with custom alignment.'
            var ui =
                    UI.label("Test1")
                    .withHorizontalAlignment(UI.HorizontalAlignment.CENTER)
                    .withVerticalAlignment(UI.VerticalAlignment.TOP)
        and : 'We build the component.'
            var label = ui.get(JLabel)

        expect : 'The label should have the specified alignment.'
            label.horizontalAlignment == SwingConstants.CENTER
            label.verticalAlignment == SwingConstants.TOP

        when : 'We use the `withAlignment` method to create another label...'
            ui =
                UI.label("Test2")
                .withAlignment(UI.Alignment.TOP_RIGHT)
        and : 'We build the component.'
            label = ui.get(JLabel)

        then : 'Both alignments should be set.'
            label.horizontalAlignment == SwingConstants.RIGHT
            label.verticalAlignment == SwingConstants.TOP
    }

    def 'Create labels with custom horizontal and vertical text position.'() {
        given : 'We create a label with custom text position.'
            var ui =
                    UI.label("Test1")
                    .withHorizontalTextPosition(UI.HorizontalAlignment.CENTER)
                    .withVerticalTextPosition(UI.VerticalAlignment.TOP)
        and : 'We build the component.'
            var label = ui.get(JLabel)

        expect : 'The label should have the specified text position.'
            label.horizontalTextPosition == SwingConstants.CENTER
            label.verticalTextPosition == SwingConstants.TOP

        when : 'We use the `withTextPosition` method to create another label...'
            ui =
                UI.label("Test2")
                .withTextPosition(UI.Alignment.TOP_RIGHT)
        and : 'Once again we build the component.'
            label = ui.get(JLabel)

        then : 'Both text positions should be set.'
            label.horizontalTextPosition == SwingConstants.RIGHT
            label.verticalTextPosition == SwingConstants.TOP
    }
}

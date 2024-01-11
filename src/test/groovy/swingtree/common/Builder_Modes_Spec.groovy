package swingtree.common

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
import swingtree.UIForAnySwing

import javax.swing.JComponent
import javax.swing.JPanel

@Title("Builder, Factory or Wrapper?")
@Narrative('''

    SwingTree offers a fluent API for assembling Swing components
    into a GUI tree, hence the name SwingTree. 
    It is designed around the idea of using method chaining 
    and statement nesting to facilitate the creation of a GUI tree.
    This is a common pattern in many libraries, and is often referred to as a
    Builder pattern, which is ruffly the what SwingTree consists of.
    
    But you may be surprised to hear that one SwingTree builder
    is not like the other. There are actually three different
    builder modes! Although they all look the same and their declarations
    will lead to the creation of the same GUI tree, they differ in terms
    of their purpose and how they ought not to be used.
    
    The three modes are:

    * Declarative Only Factory Builder Mode
    * Declarative Only Builder Mode
    * Free Builder Mode
    
    Sounds confusing! So let's break it down.
    
    The first two modes are declarative only, which means that the
    methods designed for chaining first dispose of the current builder and
    then return a new instance of the builder. This disposed builder
    is then disabled and can no longer be used to build the GUI tree.
    This is done to prevent the builder from being used in any other way
    than for building the GUI tree declaratively.
    
    The difference between the two declarative modes is that the
    factory builder mode behaves very similar to the stream API
    in that all of the operations defined by the chain of method 
    calls are composed into a single function that is executed
    when the component is being accessed. 
    Every time the component is accessed, a new component is created
    and then sent through the chain of build operations.
    Most builders in SwingTree are factory builders, 
    given that they are not created with a component instance
    passed to their constructor.
    
    The last mode is the free builder mode, which is the most unrestricted
    of the three modes. It is spanned when switching to procedural
    code using the various `apply` methods on the builder.
    Make sure not to overuse this mode, as writing
    GUI code procedurally is usually messier than declarative design.
    
    If this is still confusing, don't worry.
    The following examples will make it clear 
    where each mode is used and how they differ.
    
    You actually do not need to know about the different modes
    to use SwingTree, but it is good to know about them
    to understand the design of the library.

''')
@Subject([UIForAnySwing])
class Builder_Modes_Spec extends Specification
{
    def 'A regular builder is a factory builder'( UIForAnySwing<?,?> anyUiBuilder )
    {
        reportInfo """
            If you use SwingTree as intended you will resort to using
            the various factory methods in the `UI` namespace to create
            your builder instances.
            
            The once which do not take a component instance as an argument
            are the once which will return a factory builder instances.
        """
        given : 'We first demonstrate the factory builder mode using the `panel` method'
            var panel = UI.panel()
        expect : 'The builder will produce a `JPanel` instance'
            panel.get(JPanel) instanceof JPanel
        and : 'Every time we access the panel, a new instance is created'
            panel.get(JPanel) !== panel.get(JPanel)
            panel.get(JPanel) !== panel.get(JPanel)
            panel.get(JPanel) !== panel.get(JPanel)

        and : 'This is also true for any other UI builder created using such kinds of factory methods:'
            anyUiBuilder.get(JComponent) !== anyUiBuilder.get(JComponent)

        where : 'We can observe the same behavior for many other factory methods, including:'
            anyUiBuilder << [
                UI.panel(),
                UI.scrollPane(),
                UI.table(),
                UI.button(),
                UI.label("Hey! :)"),
                UI.textField(),
                UI.textArea("A B C"),
                UI.comboBox(),
                UI.list(),
                UI.toggleButton(),
                UI.radioButton("Toggle Me!"),
            ]
    }

    def 'Builder instances created using an `of(..)` factory method are declarative only wrappers.'()
    {
        reportInfo """
            The `of` factory methods are used to create builder instances
            which are wrappers around an existing component instance 
            provided as an argument to their respective factory methods.
            This means that the builder will not create a new component
            when it is accessed, but instead return the same component
            instance every time.
            
            This is useful when you want to build a GUI tree around
            an existing component instance.
        """
        given : 'We have a plain old Swing component instance, a `JPanel` in this case.'
            var panel = new JPanel()
        and : 'We create a builder instance using the `of` factory method.'
            var builder = UI.of(panel)
        expect : 'The builder will return the same component instance every time it is accessed.'
            builder.get(JPanel) === panel
            builder.get(JPanel) === panel
            builder.get(JPanel) === panel
    }
}

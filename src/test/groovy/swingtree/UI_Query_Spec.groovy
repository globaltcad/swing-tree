package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Event
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.*
import java.awt.*

@Title("Swing Tree UI Query")
@Narrative('''

    The Swing-Tree API allows you to easily build UIs and query them for
    specific components inside component event action lambdas!
    This allows you to build event driven UIs which react to user input flexibly.
    But note that mutating GUI state that way can lead to code where reasoning
    about side effects is generally harder, and so such code is usually harder to maintain.
    Therefore consider modelling your UIs state and behavior using MVI/MVL/MVVM
    design patterns... (using property binding).

    In the following specifications we explore the various `find...` methods exposed
    on the SwingTree event delegates. These methods let you locate components by
    their string id, by an enum based id, by a custom predicate, or by their style
    group(s) - either restricted to a particular `JComponent` subtype or across all
    components in the tree.

''')
class UI_Query_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'We can query the swing tree using the String id (name) of components.'()
    {
        reportInfo """
            The most common way to look up a component in the SwingTree is by giving
            it a unique string id (which under the hood is mapped to the Swing component's
            `name` property) and then using the `find(Class, String)` method on the
            event delegate (or on a `Utility.Query` instance) to look it up again later.

            This is the bread-and-butter pattern for "find another component from inside
            an event handler" style code. In the example below, button "B3" reaches
            across the entire tree (across tabs, scroll panes and split panes) and
            mutates a sibling button as well as a text area and a text field located
            in a completely different branch of the UI tree.

            Note that the result of `find(...)` is wrapped in an `OptionalUI` which
            is a thread-aware variant of `Optional`. This means that you should not
            assume the component is present and instead use methods like `ifPresent(...)`
            to safely interact with it.
        """
        given : 'we have a fancy UI tree with components identified by string ids:'
            var root =
            UI.panel("fill, insets 3", "[grow][shrink]")
            .add("grow",
                UI.splitPane(UI.Axis.HORIZONTAL)
                .withDividerAt(50)
                .add(
                    UI.scrollPane()
                    .add(
                        UI.tabbedPane()
                        .add(UI.tab("1").add(UI.panel("fill").add("grow", UI.button("A").id("B1"))))
                        .add(UI.tab("2").add(UI.panel("fill").add("grow", UI.button("B").id("B2"))))
                        .add(UI.tab("3").add(
                            UI.panel("fill").add("grow",
                                UI.button("C").id("B3")
                                .onClick( it -> { // The delegate lets us query the tree!
                                    it.find(JButton, "B2").ifPresent(b -> b.setText("I got hacked by B3"))
                                    it.find(JTextArea, "TA1").ifPresent(a -> a.setText("I got hacked by B3"))
                                    it.find(JTextField, "TF1").ifPresent(a -> a.setText("I got hacked by B3"))
                                })
                            )
                        ))
                    )
                )
                .add(
                    UI.scrollPane()
                    .add(
                        UI.panel("fill")
                        .add("grow,  wrap",
                            UI.scrollPane().add( UI.textArea("I am a text area!").id("TA1") )
                        )
                        .add("shrinky, growx",
                            UI.textField("I am a text field").id("TF1")
                        )
                    )
                )
            )
            .get(JPanel)

        expect : 'We can query the tree for different kinds of components by their string id.'
            new Utility.Query(root).find(JButton, "B1").isPresent()
            new Utility.Query(root).find(JButton, "B2").isPresent()
            new Utility.Query(root).find(JButton, "B3").isPresent()
            new Utility.Query(root).find(JTextArea, "TA1").isPresent()
            new Utility.Query(root).find(JTextField, "TF1").isPresent()

        when : 'We click on the button whose action handler performs cross-tree queries...'
            UI.runNow(()->new Utility.Query(root).find(JButton, "B3").get().doClick())

        then : '...the text fields and text areas reachable through `find(...)` are updated.'
            new Utility.Query(root).find(JButton, "B1").get().text == "A"
            new Utility.Query(root).find(JButton, "B2").get().text == "I got hacked by B3"
            new Utility.Query(root).find(JButton, "B3").get().text == "C"
            new Utility.Query(root).find(JTextArea, "TA1").get().text == "I got hacked by B3"
            new Utility.Query(root).find(JTextField, "TF1").get().text == "I got hacked by B3"
    }

    def 'We can query the swing tree using an enum constant as the id of a component.'()
    {
        reportInfo """
            String based ids are simple but they are also fragile because they are not
            checked at compile time. As an alternative SwingTree allows you to use any
            `enum` constant as the id of a component. This is much safer because the
            compiler will catch typos and refactoring tools can rename the enum constants
            for you.

            The corresponding `find(Class, Enum)` overload mirrors the string based one
            and is invoked here from inside the action handler of a custom event.
        """
        given : 'A few result holders, an event used to drive the action handler'
            var saveFound  = [false]
            var cancelFound= [false]
            var nameFound  = [false]
            var typeMismatch = [true] // We will try to find the SAVE button as a JTextField
            var event = Event.create()
        and : 'A small UI whose components are identified by enum constants:'
            var ui =
                UI.panel("fill")
                .add(UI.button("Save").id(Ids.SAVE))
                .add(UI.button("Cancel").id(Ids.CANCEL))
                .add(UI.textField("name").id(Ids.NAME))
                .on(event, it -> {
                    saveFound[0]   = it.find(JButton, Ids.SAVE).isPresent()
                    cancelFound[0] = it.find(JButton, Ids.CANCEL).isPresent()
                    nameFound[0]   = it.find(JTextField, Ids.NAME).isPresent()
                    typeMismatch[0]= it.find(JTextField, Ids.SAVE).isPresent()
                })
        and : 'We trigger the UI to be built:'
            ui.get(JPanel)

        when : 'We fire the event so the action handler runs.'
            event.fire()

        then : 'Each component is reachable through its corresponding enum constant.'
            saveFound[0]
            cancelFound[0]
            nameFound[0]
        and : 'A type mismatch results in an empty optional, even if a component with that id exists.'
            !typeMismatch[0]
    }

    def 'We can find a single component using a custom predicate.'()
    {
        reportInfo """
            When neither a string id nor an enum based id fits your needs you can fall
            back to the most general overload: `find(Class, Predicate)`. It traverses
            the component tree and returns the first component of the given type for
            which the predicate returns true.

            This is particularly useful when you want to locate a component by some
            of its already-set properties (text, tooltip, custom client property, ...)
            rather than by an explicit identifier.
        """
        given : 'Result holders and an event to trigger the query:'
            var world = [""]
            var startsWithS = [""]
            var notFound = [true]
            var event = Event.create()
        and : 'A small UI containing a few labels with distinct texts:'
            var ui =
                UI.panel("fill, wrap 1")
                .add(UI.label("Hello"))
                .add(UI.label("World"))
                .add(UI.label("Swing-Tree"))
                .on(event, it -> {
                    world[0]       = it.find(JLabel, l -> l.text == "World").map(l -> l.text).orElse("")
                    startsWithS[0] = it.find(JLabel, l -> l.text.startsWith("S")).map(l -> l.text).orElse("")
                    notFound[0]    = it.find(JLabel, l -> l.text == "Goodbye").isPresent()
                })
            ui.get(JPanel)

        when : 'We fire the event to drive the queries.'
            event.fire()

        then : 'The first matching label is returned for each predicate.'
            world[0] == "World"
            startsWithS[0] == "Swing-Tree"
        and : 'A predicate without any match yields an empty `OptionalUI`.'
            !notFound[0]
    }

    def 'We can find all components of a particular type matching a predicate.'()
    {
        reportInfo """
            While `find(...)` returns the first match, `findAll(Class, Predicate)`
            returns *every* component of the given type for which the predicate matches.
            The result is a `Tuple` (an immutable list) which can be empty if nothing
            matches.

            This is useful for collecting groups of components without having to
            assign explicit ids or style groups to each one. It also demonstrates
            that the type filter is applied first, so components of an unrelated
            type are excluded even if they would otherwise satisfy the predicate.
        """
        given : 'Result holders and an event to drive the queries:'
            var saveButtonTexts = []
            var emptyResultSize = [-1]
            var event = Event.create()
        and : 'A panel containing a mix of buttons (and a label) with different texts:'
            var ui =
                UI.panel("fill, wrap 1")
                .add(UI.button("Save"))
                .add(UI.button("Save As"))
                .add(UI.button("Save and Close"))
                .add(UI.button("Discard"))
                .add(UI.label("Save")) // Same text but different type!
                .on(event, it -> {
                    saveButtonTexts.addAll(
                        it.findAll(JButton, b -> b.text.contains("Save")).collect(b -> b.text)
                    )
                    emptyResultSize[0] = it.findAll(JButton, b -> b.text == "Open").size()
                })
            ui.get(JPanel)

        when : 'We fire the event to drive the queries.'
            event.fire()

        then : 'Only the three matching buttons are returned, the label is excluded by the type filter.'
            saveButtonTexts == ["Save", "Save As", "Save and Close"]
        and : 'A predicate matching no component returns an empty tuple.'
            emptyResultSize[0] == 0
    }

    def 'We can query a swing tree using the group tags of components.'()
    {
        reportInfo """
            Apart from a single string id, every SwingTree component can also be
            assigned to one or more *style groups*. A style group is a tag intended
            to be shared between many components, which makes group based queries
            ideal for collecting all components related to the same feature, screen
            or styling rule.

            The `findAllByGroup(Class, String)` overload restricts the search to a
            particular `JComponent` subtype, which is what this scenario demonstrates.
            We attach the same style group to several labels and text fields and
            then collect them in response to a custom event.
        """
        given :
            var traceA = []
            var traceB = []
            var traceC = []
            var traceD = []
		and : 'We create a custom event which we will use to query the tree.'
            var event = Event.create()
        and :
            var ui =
		        UI.panel("fill", "[][grow]")
		        .withBackground(Color.WHITE)
		        .add(UI.label("I am a generic SwingTree UI!"))
		        .add("grow",
		        	UI.panel("fill, insets 0","[grow][shrink]")
		        	.onMouseClick( e -> {/* does something */} )
		        	.add("cell 0 0",
		        		UI.boldLabel("Bold Label")
		        	)
		        	.add("cell 0 1, grow, pushy",
		        		UI.panel("fill, insets 0","[grow][shrink]")
		        		.add("cell 0 0, aligny top, grow x",
		        			UI.panel("fill, insets 9","grow")
		        			.withBackground(new Color(128, 234, 255))
		        			.add( "span",
		        				UI.label("<html><div style=\"width:275px;\"> Hey! :) </div></html>")
		        			)
		        			.add("shrink x",      UI.label("First Name").group("A", "C"))
		        			.add("grow",          UI.textField("John").group("A", "B"))
		        			.add("gap unrelated", UI.label("Last Name").group("B", "D"))
		        			.add("wrap, grow",    UI.textField("Smith").group("C"))
		        			.add("shrink",        UI.label("Address").group("C", "D"))
		        			.add("span, grow",    UI.textField("City").group("A", "C", "B"))
		        			.add( "span, grow",   UI.label("Here is a text field:").group("C", "A"))
		        			.add("span, grow",    UI.textField("Field").group("D", "B"))
		        		)
		        		.add("cell 1 0",
		        			UI.button("Some button!").group("A", "B", "C", "D")
		        			.withCursor(UI.Cursor.HAND)
		        			.makePlain()
		        			.onClick( e -> {/* does something */} )
		        		)
		        		.withBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY))
		        	)
		        	.add("cell 0 2, grow",
		        		UI.panel("fill, insets 0 0 0 0","[grow][grow][grow]")
		        		.withBackground(Color.WHITE)
		        		.add("cell 1 0", UI.label("Label A").group("C"))
		        		.add("cell 2 0", UI.label("Label B").group("D"))
		        		.add("cell 3 0", UI.label("Label C").group("A"))
		        	)
		        	.add("cell 0 3, span 2, grow",
		        		UI.label("...here the UI comes to an end...").group("A", "B", "C", "D")
                        .withForeground(Color.LIGHT_GRAY)
		        	)
		        	.withBackground(Color.WHITE)
		        )
                .on(event, it -> {
                    traceA.addAll(it.findAllByGroup(JLabel, "A").collect(c -> c.text))
                    traceB.addAll(it.findAllByGroup(JLabel, "B").collect(c -> c.text))
                    traceC.addAll(it.findAllByGroup(JTextField, "C").collect(c -> c.text))
                    traceD.addAll(it.findAllByGroup(JTextField, "D").collect(c -> c.text))
                })
		and : 'We trigger the UI to be built!'
			ui.get(JPanel)

        when : 'We fire the event and the action handler queries the tree by group tags.'
            event.fire()

        then : 'Only components of the requested type **and** group are returned.'
            traceA == ["First Name", "Here is a text field:", "Label C", "...here the UI comes to an end..."]
            traceB == ["Last Name", "...here the UI comes to an end..."]
            traceC == ["Smith", "City"]
            traceD == ["Field"]
    }

    def 'We can query a swing tree by group tag without restricting the component type.'()
    {
        reportInfo """
            The `findAllByGroup(String)` overload is the type-agnostic counterpart of
            `findAllByGroup(Class, String)`. It returns *every* `JComponent` belonging
            to a given style group, regardless of its concrete type.

            This is useful when a style group is used as a logical "feature tag"
            spanning multiple component types - for example "all components belonging
            to the login form" or "all components belonging to the toolbar".
        """
        given : 'Result holders and an event to drive the queries:'
            var loginSize  = [-1]
            var actionSize = [-1]
            var actionsAllButtons = [false]
            var emptySize  = [-1]
            var event = Event.create()
        and : 'A small UI mixing labels, buttons and text fields under shared groups:'
            var ui =
                UI.panel("fill, wrap 1")
                .add(UI.label("Username").group("login"))
                .add(UI.textField("john").group("login"))
                .add(UI.label("Password").group("login"))
                .add(UI.passwordField().group("login"))
                .add(UI.button("Login").group("login", "actions"))
                .add(UI.button("Cancel").group("actions"))
                .on(event, it -> {
                    var login = it.findAllByGroup("login")
                    loginSize[0] = login.size()
                    var actions = it.findAllByGroup("actions")
                    actionSize[0] = actions.size()
                    actionsAllButtons[0] = actions.stream().allMatch(c -> c instanceof JButton)
                    emptySize[0] = it.findAllByGroup("nope").size()
                })
            ui.get(JPanel)

        when : 'We fire the event to drive the queries.'
            event.fire()

        then : 'All five login related components are returned, regardless of type.'
            loginSize[0] == 5
        and : 'Both buttons in the "actions" group are returned, even though one of them is also part of the login group.'
            actionSize[0] == 2
            actionsAllButtons[0]
        and : 'A group which does not exist in the tree results in an empty tuple.'
            emptySize[0] == 0
    }

    def 'We can query a swing tree using enum constants as group tags.'()
    {
        reportInfo """
            Style groups can also be expressed as `enum` constants. This is the
            type-safe equivalent of using strings and is recommended for larger
            applications where you want the compiler to catch typos and renames.

            Both the type-restricted overload `findAllByGroup(Class, Enum)` and the
            type-agnostic overload `findAllByGroup(Enum)` accept an enum constant
            in place of the string group name.
        """
        given : 'Result holders and an event to drive the queries:'
            var headerLabels = [-1]
            var footerButtons= [-1]
            var bodyFields   = [-1]
            var headerAll    = [-1]
            var footerAll    = [-1]
            var bodyAll      = [-1]
            var unusedAll    = [-1]
            var unusedTyped  = [-1]
            var event = Event.create()
        and : 'A UI where style groups are expressed as enum constants:'
            var ui =
                UI.panel("fill, wrap 1")
                .add(UI.label("Title").group(Groups.HEADER))
                .add(UI.label("Subtitle").group(Groups.HEADER))
                .add(UI.button("Submit").group(Groups.FOOTER))
                .add(UI.button("Reset").group(Groups.FOOTER))
                .add(UI.textField("body").group(Groups.BODY))
                .on(event, it -> {
                    headerLabels[0] = it.findAllByGroup(JLabel, Groups.HEADER).size()
                    footerButtons[0]= it.findAllByGroup(JButton, Groups.FOOTER).size()
                    bodyFields[0]   = it.findAllByGroup(JTextField, Groups.BODY).size()
                    headerAll[0]    = it.findAllByGroup(Groups.HEADER).size()
                    footerAll[0]    = it.findAllByGroup(Groups.FOOTER).size()
                    bodyAll[0]      = it.findAllByGroup(Groups.BODY).size()
                    unusedAll[0]    = it.findAllByGroup(Groups.UNUSED).size()
                    unusedTyped[0]  = it.findAllByGroup(JLabel, Groups.UNUSED).size()
                })
            ui.get(JPanel)

        when : 'We fire the event to drive the queries.'
            event.fire()

        then : 'Restricting by type returns only the matching components.'
            headerLabels[0] == 2
            footerButtons[0] == 2
            bodyFields[0] == 1
        and : 'The type-agnostic overload returns all components in the group regardless of type.'
            headerAll[0] == 2
            footerAll[0] == 2
            bodyAll[0] == 1
        and : 'A non-existent group results in an empty tuple either way.'
            unusedAll[0] == 0
            unusedTyped[0] == 0
    }

    enum Ids { SAVE, CANCEL, NAME }
    enum Groups { HEADER, BODY, FOOTER, UNUSED }
}
package swingtree.combo

import sprouts.Val
import sprouts.Vals
import swingtree.threading.EventProcessor
import swingtree.UI
import swingtree.SwingTree
import swingtree.UIForCombo
import sprouts.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Vars

import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JTextField
import java.awt.Color
import java.time.DayOfWeek
import java.util.function.Supplier

import static swingtree.UI.comboBox

@Title("Drop Downs, aka Combo Boxes")
@Narrative("""

    In Swing drop downs are called combo boxes and they are used to select
    one item from a list of options.
     
    This specification will show you how you can create them and
    how you can interact with them using SwingTree.

""")
@Subject([UIForCombo])
class Combo_Box_Specification extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    enum Animal {
        CAT, DOG, COW, PIG
    }

    def 'Swing tree is a wrapper around Swing, which means you can create the combo box yourself.'()
    {
        given : 'We pass the combo box to the Swing-Tree factory method.'
            var ui = UI.of(new JComboBox<>(new DefaultComboBoxModel<>(new String[]{"A", "B", "C"})))
        and : 'We unpack the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The underlying component is a combo box.'
            combo instanceof JComboBox
        and : 'It has the expected state:'
            combo.itemCount == 3
            combo.getItemAt(0) == "A"
            combo.getItemAt(1) == "B"
            combo.getItemAt(2) == "C"
            combo.model instanceof DefaultComboBoxModel
            combo.model.size == 3
            combo.model.getElementAt(0) == "A"
            combo.model.getElementAt(1) == "B"
            combo.model.getElementAt(2) == "C"
    }

    def 'The "comboBox" factory method allows you to easily create a combo box from an array.'()
    {
        given : 'We pass the combo box to the Swing-Tree factory method.'
            var ui = UI.comboBox("A", "B", "C")
        and : 'We unpack the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The underlying component is a combo box.'
            combo instanceof JComboBox
        and : 'It has the expected state:'
            combo.itemCount == 3
            combo.getItemAt(0) == "A"
            combo.getItemAt(1) == "B"
            combo.getItemAt(2) == "C"
        and : 'It is backed by a built in model.'
            !(combo.model instanceof DefaultComboBoxModel)
            combo.model.size == 3
            combo.model.getElementAt(0) == "A"
            combo.model.getElementAt(1) == "B"
            combo.model.getElementAt(2) == "C"
    }

    def 'A combo box created by Swing-Tree uses the provided item array as data model.'()
    {
        reportInfo """
           You don't have to create a ComboBoxModel implementation to model the state
           of your combo box! If you pass an array or a list to a combo box
           factory method it will serve as a model of your combo box items.  
        """
        given : 'We create an array and pass it to the combo box factory method.'
            var items = new String[]{"A" , "B" , "C"}
            var ui = UI.comboBox(items)
        and : 'We build a combo box component.'
            var combo = ui.get(JComboBox)
        when : 'We modify the array...'
            items[0] = "D"
        then : 'The combo box is updated as well.'
            combo.itemCount == 3
            combo.getItemAt(0) == "D"
            combo.getItemAt(1) == "B"
            combo.getItemAt(2) == "C"
    }

    def 'A combo box created by Swing-Tree uses the provided item list as data model.'()
    {
        reportInfo """
           You don't have to create a ComboBoxModel implementation to model the state
           of your combo box! If you pass an array or a list to a combo box
           factory method it will serve as a model of your combo box items.  
        """
        given : 'We create an array and pass it to the combo box factory method.'
            var items = ["A" , "B" , "C"]
            var ui = UI.comboBox(items)
        and : 'We build a combo box component.'
            var combo = ui.get(JComboBox)
        when : 'We modify the list by adding an element in the middle...'
            items.add(1, "D")
        then : 'The combo box is updated as well.'
            combo.itemCount == 4
            combo.getItemAt(0) == "A"
            combo.getItemAt(1) == "D"
            combo.getItemAt(2) == "B"
            combo.getItemAt(3) == "C"
    }

    def 'A combo box can be made editable in a declarative way.'()
    {
        given : 'We create a combo box and make it editable.'
            var ui = UI.comboBox("A", "B", "C")
                            .isEditableIf(true)
                            .withSelectedItem("B")
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is editable.'
            combo.isEditable()
        and : 'The selected item is set.'
            combo.getSelectedItem() == "B"

        when : 'We simulate the user typing "XY" into the combo box.'
            UI.runNow( () -> combo.editor.item = "XY" )
            UI.sync()
        then : 'The combo box is updated.'
            combo.getSelectedItem() == "XY"
        and : 'This change is reflected in the model.'
            combo.model.size == 3
            combo.model.getElementAt(0) == "A"
            combo.model.getElementAt(1) == "XY"
            combo.model.getElementAt(2) == "C"
    }

    def 'The options of an editable combo box are only editable if their items list is modifyable.'()
    {
        given : 'We create a combo box and make it editable.'
            var ui = UI.comboBox(Collections.unmodifiableList(["A", "B", "C"]))
                            .isEditableIf(true)
                            .withSelectedItem("B")
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is editable.'
            combo.isEditable()
        and : 'The selected item is set.'
            combo.getSelectedItem() == "B"

        when : 'We simulate the user typing "XY" into the combo box.'
            UI.runNow( () -> combo.editor.item = "XY" )
            UI.sync()
        then : 'The combo box is updated.'
            combo.getSelectedItem() == "XY"
        and : 'This change is NOT reflected in the model.'
            combo.model.size == 3
            combo.model.getElementAt(0) == "A"
            combo.model.getElementAt(1) == "B"
            combo.model.getElementAt(2) == "C"
    }

    def 'You can model both the current selection state as well as options of your combo box using a property and an array.'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using a property and a list. The property will be used to model
           the current selection, and the list will be used to model the options.
        """
        given : 'We create our "model", a property and an array.'
            var selection = Var.of(42)
            var options = new Integer[]{ 73 , 42 , 17 }
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is initialized with the current selection.'
            combo.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            combo.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            combo.itemCount == 3
            combo.getItemAt(0) == 73
            combo.getItemAt(1) == 42
            combo.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            combo.getSelectedItem() == 17
        and : 'The combo box options are still the same.'
            combo.itemCount == 3
            combo.getItemAt(0) == 73
            combo.getItemAt(1) == 42
            combo.getItemAt(2) == 17

        when : 'We change one of the options.'
            options[0] = 99
        and : 'We select this option...'
            combo.setSelectedItem(99)
        then : 'The selection is updated.'
            selection.get() == 99
        and : 'The combo box also reports the correct selection index!'
            combo.getSelectedIndex() == 0
    }


    def 'You can model both the current selection state as well as options of your combo box using a property and a list.'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using a property and a list. The property will be used to model
           the current selection, and the list will be used to model the options.
        """
        given : 'We create our "model", a property and a list.'
            var selection = Var.of(42)
            var options = [73, 42, 17]
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is initialized with the current selection.'
            combo.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            combo.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            combo.itemCount == 3
            combo.getItemAt(0) == 73
            combo.getItemAt(1) == 42
            combo.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            combo.getSelectedItem() == 17
        and : 'The combo box options are still the same.'
            combo.itemCount == 3
            combo.getItemAt(0) == 73
            combo.getItemAt(1) == 42
            combo.getItemAt(2) == 17

        when : 'We add another option somewhere in the middle.'
            options.add(1, 99)
        then : 'The combo box options are updated.'
            combo.itemCount == 4
            combo.getItemAt(0) == 73
            combo.getItemAt(1) == 99
            combo.getItemAt(2) == 42
            combo.getItemAt(3) == 17
        and : 'The selection is still the same.'
            combo.getSelectedItem() == 17
        and : 'The combo box also reports the correct selection index!'
            combo.getSelectedIndex() == 3
    }


    def 'You can model both the current selection state as well as options of your combo box using 2 properties.'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using 2 properties. One modelling the current selection, and another one
           storing an array to model all available options. 
        """
        given : 'We create our "model", 2 properties.'
            var selection = Var.of(42)
            var options = Var.of([73, 42, 17] as Integer[])
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is initialized with the current selection.'
            combo.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            combo.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            combo.itemCount == 3
            combo.getItemAt(0) == 73
            combo.getItemAt(1) == 42
            combo.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            combo.getSelectedItem() == 17

        when : 'We change the options property.'
            options.set([99, 17] as Integer[])
        then : 'The combo box options are updated.'
            combo.itemCount == 2
            combo.getItemAt(0) == 99
            combo.getItemAt(1) == 17
    }


    def 'You can model the options of your combo boxes using "Vars".'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using a standalone property as well as a "Vars" instance
           representing multiple properties. 
           The single property models the current selection, and the "Vars"
           store a list of all available options. 
        """
        given : 'We create our "model", a property and a "Vars" instance.'
            var selection = Var.of(42)
            var options = Vars.of(73, 42, 17)
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is initialized with the current selection.'
            combo.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            combo.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            combo.itemCount == 3
            combo.getItemAt(0) == 73
            combo.getItemAt(1) == 42
            combo.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            combo.getSelectedItem() == 17

        when : 'We change the options property.'
            options.clear().addAll(99, 17)
        then : 'The combo box options are updated.'
            combo.itemCount == 2
            combo.getItemAt(0) == 99
            combo.getItemAt(1) == 17

        when : 'We add another option somewhere in the middle.'
            options.addAll(16, 42)
        then : 'The combo box options are updated.'
            combo.itemCount == 4
            combo.getItemAt(0) == 99
            combo.getItemAt(1) == 17
            combo.getItemAt(2) == 16
            combo.getItemAt(3) == 42
    }

    def 'An editable combo box will try to parse user input to match bound properties.'()
    {
        reportInfo """
            When you bind a selection property to an editable combo box, the combo box
            will try to parse user input so that it can be converted to the type of the
            selection property. 
            If the String can be parsed, the combo box will update the selection property
            to the parsed value.
            On the other hand, if the String cannot be parsed, the combo box will not
            update the selection property.
        """
        given : 'We create a property of type Integer.'
            var selection = Var.of(42)
        and : 'We create a combo box that is bound to the property.'
            var ui =
                        UI.comboBox(selection, 73, 42, 17)
                        .isEditableIf(true)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is initialized with the current selection.'
            combo.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            combo.getSelectedIndex() == 1

        when : 'We simulate the user editing the combo box.'
            combo.setSelectedItem('99')
        then : 'The combo box updates the selection property to the parsed value.'
            selection.get() == 99
    }


    def 'An editable combo box will try to parse any kind of user input to match bound properties.'(
        String input, Var<Object> selection, Object[] items, Object expectedSelection
    ) {
        reportInfo """
            When you bind a selection property to an editable combo box, the combo box
            will try to parse user input so that it can be converted to the type of the
            selection property. 
            If the String can be parsed, the combo box will update the selection property
            to the parsed value.
            On the other hand, if the String cannot be parsed, the combo box will not
            update the selection property.
        """
        given : 'We create a combo box that is bound to the property.'
            var ui =
                        UI.comboBox(selection, items)
                        .isEditableIf(true)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)
        expect : 'The combo box is initialized with the current selection.'
            combo.getSelectedItem() == selection.get()

        when : 'We simulate the user editing the combo box.'
            combo.setSelectedItem(input)
            UI.sync()
        then : 'The combo box updates the selection property to the parsed value.'
            selection.get() == expectedSelection

        where : 'We use the following data table to populate the used variables:'
            input   |   selection      |  items           | expectedSelection
            '99'    |   Var.of(42)     |  [73, 42, 17]    | 99
            '42'    |   Var.of(42)     |  [73, 42, 17]    | 42
            'true'  |   Var.of(false)  |  [true, false]   | true
            'false' |   Var.of(false)  |  [true, false]   | false
            'yes'   |   Var.of(false)  |  [true, false]   | true
            'no'    |   Var.of(false)  |  [true, false]   | false
            '3.14'  |   Var.of(3.14)   |  [3.14, 2.71]    | 3.14
            '2.71'  |   Var.of(3.14)   |  [3.14, 2.71]    | 2.71
            'foo'   |   Var.of('foo')  |  ['foo', 'bar']  | 'foo'
            'bar'   |   Var.of('foo')  |  ['foo', 'bar']  | 'bar'
            '666L'  |   Var.of(666L)   |  [666L, 777L]    | 666L
            '777L'  |   Var.of(666L)   |  [666L, 777L]    | 777L
    }

    def 'Changing properties in you view model automatically updates the combo box.'()
    {
        given :
            var selected = Var.of(4);
            var options = Vars.of(1, 2, 4, 6, 8, 12, 16)
        and :
            var ui =
                comboBox(options)
				.withSelectedItem(selected)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)

		when : 'We change the selected property to 1...'
		    selected.set(1)
            UI.sync()
		then : 'The combo box has been updated as expected!'
		    combo.selectedItem == 1
		    combo.editor.item == 1
    }

    def 'Changing properties in you view model automatically updates an editable combo box.'()
    {
        given :
            var selected = Var.of(4);
            var options = Vars.of(1, 2, 4, 6, 8, 12, 16)
        and :
            var ui =
                comboBox(options).isEditableIf(true)
				.withSelectedItem(selected)
        and : 'We get the combo box.'
            var combo = ui.get(JComboBox)

		when : 'We change the selected property to 1...'
		    selected.set(1)
            UI.sync()
		then : 'The combo box has been updated as expected!'
		    combo.selectedItem == 1
		    combo.editor.item == 1
    }

    def 'Create combo box UIs with simple text render functions.'(
        Supplier<UIForCombo<?, JComboBox<?>>> uiSupplier
    ) {
        reportInfo """
            The type of items a combo box holds need to be presented
            to the user in a human readable way. The most common and convenient
            way to do this is to use a simple text representation of the items.
            This can be achieved by providing a function that converts the items
            to strings.
            
            In this example we are using the following enum type
            to model the items of the combo box:
            ```
            enum Animal {
                CAT, DOG, COW, PIG
            }
            ```
            We are going to use these constant to
            as a basis for various kinds of ways to model the combo box
            state and also how to render them.
        """
        given : 'We create a combo box from the UI supplier.'
            var ui = uiSupplier.get()
        and : 'We build a combo box component.'
            var combo = ui.get(JComboBox)
        expect : 'There are all 4 options available.'
            combo.itemCount == 4
            combo.getItemAt(0) == Animal.CAT
            combo.getItemAt(1) == Animal.DOG
            combo.getItemAt(2) == Animal.COW
            combo.getItemAt(3) == Animal.PIG

        and : 'We check if the renderer exists and is working.'
            combo.renderer != null

        when : 'We call the renderer for each item.'
            var renderer = combo.renderer
            var fakeJList = new JList<Animal>()
            var rendered = UI.runAndGet(()->[
                renderer.getListCellRendererComponent(fakeJList, Animal.CAT, 0, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, Animal.DOG, 1, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, Animal.COW, 2, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, Animal.PIG, 3, false, false).text
            ])
        then : 'The renderer returns the expected text representations.'
            rendered[0] == "cat"
            rendered[1] == "dog"
            rendered[2] == "cow"
            rendered[3] == "pig"
        when : 'We do not record the rendered results, but the components...'
            rendered = UI.runAndGet(()->[
                renderer.getListCellRendererComponent(fakeJList, Animal.CAT, 0, false, false),
                renderer.getListCellRendererComponent(fakeJList, Animal.DOG, 1, false, false),
                renderer.getListCellRendererComponent(fakeJList, Animal.COW, 2, false, false),
                renderer.getListCellRendererComponent(fakeJList, Animal.PIG, 3, false, false)
            ])
        then : 'All components will report the last rendered text.'
            rendered[0].text == "pig"
            rendered[1].text == "pig"
            rendered[2].text == "pig"
            rendered[3].text == "pig"
        and : 'That is because they are all the same component instance.'
            rendered[0] === rendered[1]
            rendered[1] === rendered[2]
            rendered[2] === rendered[3]

        where : """
            We are using the following factory methods from the `UI` namespace.
            Note that the `UI.comboBox` method is overloaded and can take
            all kinds of arguments to model the state of the combo box.
        """
            uiSupplier << [
                { UI.comboBox(Animal.values() as List, a -> a.name().toLowerCase()) },
                { UI.comboBox(Vars.of(Animal.CAT, Animal.DOG, Animal.COW, Animal.PIG), a -> a.name().toLowerCase()) },
                { UI.comboBox(Vals.of(Animal.CAT, Animal.DOG, Animal.COW, Animal.PIG), a -> a.name().toLowerCase()) },
                { UI.comboBox(Var.of(Animal.CAT), Animal.values() as List, a -> a.name().toLowerCase()) },
                { UI.comboBox(Var.of(Animal.CAT), Vars.of(Animal.CAT, Animal.DOG, Animal.COW, Animal.PIG), a -> a.name().toLowerCase()) },
                { UI.comboBox(Var.of(Animal.CAT), Vals.of(Animal.CAT, Animal.DOG, Animal.COW, Animal.PIG), a -> a.name().toLowerCase()) },
                { UI.comboBox(Var.of(Animal.CAT), Var.of(Animal.values()), a -> a.name().toLowerCase()) },
                { UI.comboBox(Var.of(Animal.CAT), Val.of(Animal.values()), a -> a.name().toLowerCase()) }
            ]
    }

    def 'Use `withCells(Configurator)` to configure both a renderer and editor for your combobox.'()
    {
        reportInfo """
            The `withCells(Configurator)` method constitutes a useful API point
            which exposes you to a fluent API for configuring which kind of item
            should be rendered using which kind of renderer and editor.
            
            The `Configurator` lambda passed to the `withCell` method receives
            a fluent API for defining which kind of item should be rendered
            using which kind of renderer and editor.
            
            So this may look like this:
            ```java
                .when(Animal.class).as( cell -> ... )
                .when(String.class).as( cell -> ... )
                //...
            ```
            And inside this inner `Configurator` lambda you are exposed
            to a delegate object of a particular cell in the combo box.
            You may update and return this cell with a view component
            used for either rendering, editing or both.
        """
        given : 'We create a combo box for the days of the week and a custom cell configuration.'
            var ui =
                        UI.comboBox(DayOfWeek.values())
                        .withCells(it -> it
                            .when(DayOfWeek.class).as( cell -> cell
                                .view( comp -> comp
                                    .orGetUiIf(cell.isEditing(), {UI.textField().withBackground(Color.MAGENTA)})
                                    .orGetUiIf(!cell.isEditing(), {UI.label("")})
                                    .updateIf(JLabel.class, label -> {
                                        label.text = "Day: " + cell.entryAsString()
                                        return label
                                    })
                                )
                            )
                        )
        and : 'We build the combo box.'
            var combo = ui.get(JComboBox)
        and : 'We get the renderer and editor supplier.'
            var renderer = combo.renderer
            var editor = combo.editor
        expect :
            renderer != null
            editor != null
        and : 'The editor was initialized with the text field having a magenta background.'
            editor.getEditorComponent() instanceof JTextField
            editor.getEditorComponent().background == Color.MAGENTA
        and : 'The renderer was initialized with a label showing the day of the week.'
            var fakeJList = new JList<DayOfWeek>()
            var rendered = UI.runAndGet(()->[
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.MONDAY, 0, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.TUESDAY, 1, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.WEDNESDAY, 2, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.THURSDAY, 3, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.FRIDAY, 4, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.SATURDAY, 5, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.SUNDAY, 6, false, false).text
            ])
        and : 'The renderer returns the expected text representations.'
            rendered[0] == "Day: MONDAY"
            rendered[1] == "Day: TUESDAY"
            rendered[2] == "Day: WEDNESDAY"
            rendered[3] == "Day: THURSDAY"
            rendered[4] == "Day: FRIDAY"
            rendered[5] == "Day: SATURDAY"
            rendered[6] == "Day: SUNDAY"
    }

    def 'Use `withCell(Configurator)` to configure both a renderer and editor for your combobox.'()
    {
        reportInfo """
            The `withCell(Configurator)` method constitutes a useful API point
            which exposes you to a fluent API for configuring how a particular cell
            should be displayed.
            
            The `Configurator` lambda passed to the `withCell` method receives
            a delegate object of a particular cell in the combo box.
            You may update and return this cell with a view component
            used for either rendering, editing or both.
            
            So this may look like this:
            ```java
                .withCell( cell -> cell
                    .view( comp -> comp
                        .orGetUiIf(cell.isEditing(), {UI.textField().withBackground(Color.MAGENTA)})
                        .orGetUiIf(!cell.isEditing(), {UI.label("")})
                        .updateIf(JLabel.class, label -> {
                            label.text = "Day: " + cell.valueAsString().orElse("")
                            return label
                        })
                    )
                )
            ```
            Here you can see that the `Configurator` lambda receives a `cell` object
            which is a delegate object of a particular cell in the combo box.
            The view of this cell is updated with a text field or a label depending
            on whether the cell is currently being edited or not.
        """
        given : 'We create a combo box for the days of the week and a custom cell configuration.'
            var ui =
                        UI.comboBox(DayOfWeek.values())
                        .withCell(cell -> cell
                            .view( comp -> comp
                                .orGetUiIf(cell.isEditing(), {UI.textField().withBackground(Color.MAGENTA)})
                                .orGetUiIf(!cell.isEditing(), {UI.label("")})
                                .updateIf(JLabel.class, label -> {
                                    label.text = "Day: " + cell.entryAsString()
                                    return label
                                })
                            )
                        )
        and : 'We build the combo box.'
            var combo = ui.get(JComboBox)
        and : 'We get the renderer and editor supplier.'
            var renderer = combo.renderer
            var editor = combo.editor
        expect :
            renderer != null
            editor != null
        and : 'The editor was initialized with the text field having a magenta background.'
            editor.getEditorComponent() instanceof JTextField
            editor.getEditorComponent().background == Color.MAGENTA
        and : 'The renderer was initialized with a label showing the day of the week.'
            var fakeJList = new JList<DayOfWeek>()
            var rendered = UI.runAndGet(()->[
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.MONDAY, 0, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.TUESDAY, 1, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.WEDNESDAY, 2, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.THURSDAY, 3, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.FRIDAY, 4, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.SATURDAY, 5, false, false).text,
                renderer.getListCellRendererComponent(fakeJList, DayOfWeek.SUNDAY, 6, false, false).text
            ])
        and : 'The renderer returns the expected text representations.'
            rendered[0] == "Day: MONDAY"
            rendered[1] == "Day: TUESDAY"
            rendered[2] == "Day: WEDNESDAY"
            rendered[3] == "Day: THURSDAY"
            rendered[4] == "Day: FRIDAY"
            rendered[5] == "Day: SATURDAY"
            rendered[6] == "Day: SUNDAY"
    }

}

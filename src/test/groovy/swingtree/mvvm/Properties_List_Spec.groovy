package swingtree.mvvm

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.UIAction
import swingtree.api.mvvm.*

@Title("Lists of Properties")
@Narrative('''

    Just like properties you can create lists of properties
    and then bind them to UI components.
    They are a powerful tool to model the state 
    as well as business logic of your UI without actually depending on it.
    This is especially useful for testing your UIs logic.
    This specification shows how to use the various
    methods exposed by the property lists classes, 
    namely "Vals" (immutable) and "Vars" (mutable).
    
''')
@Subject([Vals, Vars])
class Properties_List_Spec extends Specification
{

    def 'Multiple properties can be modelled through the "Vars" and "Vals" classes.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Apple", "Banana")
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of("Cherry", "Date")
        expect : 'Both the "Vars" and "Vals" have two properties.'
            vars.size() == 2
            vars.at(0).get() == "Apple"
            vars.at(1).get() == "Banana"
            vals.size() == 2
            vals.at(0).get() == "Cherry"
            vals.at(1).get() == "Date"
        and : 'You can also use the "First" and "last" methods.'
            vars.first().get() == "Apple"
            vars.last().get() == "Banana"
            vals.first().get() == "Cherry"
            vals.last().get() == "Date"
        and : 'They also have the correct type.'
            vars.type() == String
            vals.type() == String

        and : 'The "Vals" class has no methods for mutation, it is read only (basically a tuple).'
            Vals.metaClass.getMethods().findAll{ it.name == "set" }.size() == 0
            Vals.metaClass.getMethods().findAll{ it.name == "add" }.size() == 0
            Vals.metaClass.getMethods().findAll{ it.name == "remove" }.size() == 0
        and : 'Both property lists are not empty'
            !vars.isEmpty() && vars.isNotEmpty()
            !vals.isEmpty() && vals.isNotEmpty()
        and : 'Both property lists are iterable'
            vars.each{ it }
            vals.each{ it }

        when : 'We change the state of the "Vars" properties.'
            vars.at(0).set("Apricot")
            vars.at(1).set("Blueberry")
        then : 'The "Vars" properties have changed.'
            vars.at(0).get() == "Apricot"
            vars.at(1).get() == "Blueberry"

        when : 'We use the "aetAt" method to change the state of the "Vars" properties.'
            vars.setAt(0, "Tim")
            vars.setAt(1, "Tom")
        then : 'The "Vars" properties have changed.'
            vars.at(0).get() == "Tim"
            vars.at(1).get() == "Tom"
    }

    def 'You can remove n leading or trailing entries from a property list.'()
    {
        reportInfo """
            A very common use case is to remove the first or last entry from a list.
            Not only can you do this with the "removeFirst()" and "removeLast()" methods,
            but you can also remove n entries from the start or end of the list
            through the "removeFirst(int)" and "removeLast(int)" methods.
        """
        given : 'A "Vars" class with six properties.'
            var vars = Vars.of("Racoon", "Squirrel", "Turtle", "Piglet", "Rooster", "Rabbit")
        expect : 'The "Vars" class has six properties.'
            vars.size() == 6
        when : 'We remove the first entry from the list.'
            vars.removeFirst()
        then : 'The "Vars" class has five properties.'
            vars.size() == 5
        and : 'The first entry has been removed.'
            vars.at(0).get() == "Squirrel"
        when : 'We remove the last entry from the list.'
            vars.removeLast()
        then : 'The "Vars" class has four properties.'
            vars.size() == 4
        and : 'The last entry has been removed.'
            vars.at(3).get() == "Rooster"
        when : 'We remove the first two entries from the list.'
            vars.removeFirst(2)
        then : 'The "Vars" class has two properties.'
            vars.size() == 2
        and : 'The first two entries have been removed.'
            vars.at(0).get() == "Piglet"
            vars.at(1).get() == "Rooster"
        when : 'We remove the last two entries from the list.'
            vars.removeLast(2)
        then : 'The "Vars" class has no properties.'
            vars.size() == 0
    }

    def 'The properties of one property list can be added to another property list.'()
    {
        reportInfo """
            The properties of one property list can be added to another property list.
            This is useful when you want to combine two property lists into one.
        """
        given : 'A "Vars" class with three properties.'
            var vars1 = Vars.of("Racoon", "Squirrel", "Turtle")
        and : 'A "Vars" class with three properties.'
            var vars2 = Vars.of("Piglet", "Rooster", "Rabbit")
        expect : 'The "Vars" classes have three properties.'
            vars1.size() == 3
            vars2.size() == 3
        when : 'We add the properties of the second "Vars" class to the first "Vars" class.'
            vars1.addAll(vars2)
        then : 'The "Vars" class has six properties.'
            vars1.size() == 6
        and : 'The properties of the second "Vars" class have been added to the first "Vars" class.'
            vars1.at(0).get() == "Racoon"
            vars1.at(1).get() == "Squirrel"
            vars1.at(2).get() == "Turtle"
            vars1.at(3).get() == "Piglet"
            vars1.at(4).get() == "Rooster"
            vars1.at(5).get() == "Rabbit"
    }

    def 'The "Vars" is a list of properties which can grow and shrink.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        expect : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Dal Biji"
        when : 'We add a new property to the "Vars" class.'
            vars.add("Chapati")
        then : 'The "Vars" class has three properties.'
            vars.size() == 3
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Dal Biji"
            vars.at(2).get() == "Chapati"
        when : 'We remove a property from the "Vars" class.'
            vars.remove("Dal Biji")
        then : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Chapati"
    }

    def 'Both the "Vars" and immutable "Vals" types can be used for functional programming.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of("Chapati", "Papad")

        when : 'We use the "map" method to transform all the properties.'
            var mappedVars = vars.map{ it.toUpperCase() }
            var mappedVals = vals.map{ it.toUpperCase() }
        then : 'The properties have been transformed.'
            mappedVars.at(0).get() == "KACHORI"
            mappedVars.at(1).get() == "DAL BIJI"
            mappedVals.at(0).get() == "CHAPATI"
            mappedVals.at(1).get() == "PAPAD"
    }

    def 'Map a "Vals" instance from one type of properties to another.'()
    {
        given : 'A "Vals" class with two properties.'
            var vals = Vals.of("Chapati", "Papad")
        when : 'We use the "mapTo" method to transform all the properties.'
            var mappedVals = vals.mapTo(Integer, { it.length() })
        then : 'The properties have been transformed.'
            mappedVals.at(0).get() == 7
            mappedVals.at(1).get() == 5
    }


    def 'Map a "Vars" instance from one type of properties to another.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        when : 'We use the "mapTo" method to transform all the properties.'
            var mappedVars = vars.mapTo(Integer, { it.length() })
        then : 'The properties have been transformed.'
            mappedVars.at(0).get() == 7
            mappedVars.at(1).get() == 8
    }

    def 'You can create the "Vars"/"Vals" property lists from property instances.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(Var.of("Chana"), Var.of("Dal"))
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of(Val.of("Chapati"), Val.of("Papad"))
        expect : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Chana"
            vars.at(1).get() == "Dal"
        and : 'The "Vals" class has two properties.'
            vals.size() == 2
            vals.at(0).get() == "Chapati"
            vals.at(1).get() == "Papad"
    }

    def 'Just like a regular "Var" property you can register "show" listeners on "Vars".'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" listener on the "Vars" class.'
            vars.onShow{ changes << it.index() }

        when : 'We modify the property in various ways...'
            vars.addAt(1, 1)
            vars.setAt(0, 2)
            vars.removeAt(1)
            vars.add(3)

        then : 'The "show" listener has been called four times.'
            changes.size() == 4
        and : 'The "show" listener has been called with the correct indices.'
            changes == [1, 0, 1, 2]
    }

    def 'The display action of a property or list of properties will not be called if they report "canBeRemoved()"'()
    {
        given : 'A single property and list of two properties.'
            var prop = Var.of(7)
            var list = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" listeners on both objects.'
            prop.onShowThis(new UIAction<ValDelegate<Integer>>() {
                @Override
                void accept(ValDelegate<Integer> delegate) {
                    changes << "Something happened to the property."
                }
                @Override boolean canBeRemoved() { return true }
            })
            list.onShow(new UIAction<ValsDelegate<Integer>>() {
                @Override
                void accept(ValsDelegate<Integer> delegate) {
                    changes << "Something happened to the list."
                }
                @Override boolean canBeRemoved() { return true }
            })

        when : 'We modify the properties in various ways...'
            prop.set(42)
            list.add(1)
            list.removeAt(1)
            list.setAt(0, 2)
            list.addAt(1, 3)
            list.remove(3)

        then : 'The "show" listener has been called zero times.'
            changes.size() == 0
    }

    def 'The listeners registered in property lists will be informed what type of modification occurred.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" listener on the "Vars" class.'
            vars.onShow{ changes << it.type() }

        when : 'We modify the property in various ways...'
            vars.addAt(1, 1)
            vars.setAt(0, 2)
            vars.removeAt(1)
            vars.add(3)

        then : 'The "show" listener has been called four times.'
            changes.size() == 4
        and : 'The "show" listener has been called with the correct indices.'
            changes == [Mutation.ADD, Mutation.SET, Mutation.REMOVE, Mutation.ADD]
    }


}

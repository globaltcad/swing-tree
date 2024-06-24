package swingtree.mvvm

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Var
import swingtree.UI

@Title("Automatic Unbinding and Cleaning")
@Narrative("""
    When a view, alongside all of the components it consists of, gets garbage collected,
    the view model should be unbound from the view and cleaned up in order to prevent memory leaks.
    Doing this manually is tedious and error-prone, which is why SwingTree does it automatically for you.
    
    This spec tests whether the automatic unbinding and cleaning works as expected
    when the view is garbage collected.
""")
class Auto_Cleaning_Spec extends Specification
{

    def 'The properties of a view model will loose the bindings to garbage collected views.'()
    {
        reportInfo """
            In this example we use a simple Groovy map as our view model.
            In a real application you would probably use a class 
            to hold all of your view model's properties.
        """
        given : 'We instantiate the view model, a simple map.'
            var vm = [
                    forename:        Var.of("John"),
                    surname:         Var.of("Doe"),
                    favouriteNumber: Var.of(42)
                ]
        when : 'We create a view for our view model...'
                UI.panel("fill, wrap 2")
                .add( UI.label( "Forename:" ) )
                .add( "grow", UI.textField(vm.forename))
                .add( UI.label( "Surname:" ) )
                .add( "grow", UI.passwordField(vm.surname))
                .add( UI.label( "Favourite Number:" ) )
                .add( "grow",
                    UI.slider(UI.Align.HORIZONTAL, 0, 100)
                    .withValue(vm.favouriteNumber)
                )
        and : 'We set the view to null and wait a bit... let the GC do its thing...'
            System.gc()
            Thread.sleep(100)
        then : 'The binding of the view model to the view was garbage collected from the properties.'
            vm.forename.numberOfChangeListeners() == 0
            vm.surname.numberOfChangeListeners() == 0
            vm.favouriteNumber.numberOfChangeListeners() == 0
    }

}

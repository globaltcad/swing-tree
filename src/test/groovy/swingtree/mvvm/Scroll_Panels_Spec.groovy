package swingtree.mvvm

import examples.mvvm.ScrollPanelsViewModel
import spock.lang.Specification
import swingtree.threading.EventProcessor
import swingtree.UI

class Scroll_Panels_Spec extends Specification
{
    def setupSpec() {
        UI.SETTINGS().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'We can instantiate the scroll panels view model and build a view for it.'()
    {
        reportInfo """
            Note that we use a pre-made example view model here.
            Feel free to look at the source code of the view model
            to see what it is doing.
        """
        given : 'We instantiate the view model.'
            var vm = new ScrollPanelsViewModel()
        when : 'We create a view for our view model...'
            var ui = UI.panel("fill, wrap 1")
                        .add( UI.label("Something to scroll:") )
                        .add( UI.separator() )
                        .add( UI.scrollPanels().add(vm.entries()) )
                        .add( UI.separator() )
        then : 'The view was successfully created.'
            ui != null
    }


}

package com.globaltcad.swingtree.examples

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.examples.advanced.AdvancedUI
import com.globaltcad.swingtree.utility.Utility
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Narrative

import javax.swing.*

@Title("Execution and Validation of Example Code")
@Narrative('''

    This specification ensures that the
    various UI examples in the test suite, 
    run successfully and also produce
    UIs with expected states.

''')
class Examples_Spec extends Specification
{

    def 'The advanced UI define in the examples has the expected state.'()
    {
        given : 'We get the UI.'
            var ui = AdvancedUI.of(null)
        expect :
            new Utility.Query(ui).find(JLabel, "APIC-label").isPresent()
            new Utility.Query(ui).find(JLabel, "APIC-label").get().text == "6.0"
        and :
            new Utility.Query(ui).find(JTabbedPane, "APIC-Tabs").isPresent()
            new Utility.Query(ui).find(JTabbedPane, "APIC-Tabs").get().getTabCount() == 9
        and :
            new Utility.Query(ui).find(JSpinner, "Light-Spinner").isPresent()
            new Utility.Query(ui).find(JSpinner, "Light-Spinner").get().getValue() == 0
        and :
            new Utility.Query(ui).find(JSplitButton, "con-split-button").isPresent()
            Utility.getSplitButtonPopup(new Utility.Query(ui).find(JSplitButton, "con-split-button").get()).components.length == 4
    }

}

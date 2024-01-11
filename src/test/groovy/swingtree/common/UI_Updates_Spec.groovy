package swingtree.common


import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import utility.Utility
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.*
import java.time.LocalDateTime

@Title("Dynamic UI Updates")
@Narrative('''

    The Swing-Tree UI builder allows you to easily build UI with periodic updates.
    This is useful for example when you want to build animated UIs or UIs that
    perform some sort of refreshes periodically.
    
''')
class UI_Updates_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'We can register periodically called UI updates!'()
    {
        given :
            var ui =
                 UI.panel()
                .add(
                    UI.label("Label 1").id("L1")
                    .doUpdates(20,it -> {
                        it.component.text = LocalDateTime.now().toString()
                    })
                )
        and : 'We actually build the component:'
            var panel = ui.get(JPanel)


        when :
            Thread.sleep(200)

        then :
            new Utility.Query(panel).find(JLabel, "L1").isPresent()
        and :
            new Utility.Query(panel).find(JLabel, "L1").get().text != "Label 1"
    }

}

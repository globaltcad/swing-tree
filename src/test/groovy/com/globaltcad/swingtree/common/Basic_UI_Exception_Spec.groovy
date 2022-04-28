package com.globaltcad.swingtree.common

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.MenuBuilder
import com.globaltcad.swingtree.SwingBuilder
import com.globaltcad.swingtree.UI
import spock.lang.Specification

import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JRadioButton
import javax.swing.JSeparator
import javax.swing.JSlider
import javax.swing.text.JTextComponent

class Basic_UI_Exception_Spec extends Specification
{
    def 'The given factory methods do not accept null arguments.'(
            Runnable illegalAction
    ) {
        when : 'We execute the error prone code...'
            illegalAction()
        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
        where :
            illegalAction << [
               {UI.of((JMenu)null)},
               {UI.of((JSeparator)null)},
               {UI.of((JMenuItem)null)},
               {UI.of((JPanel)null)},
               {UI.labelWithIcon(null)},
               {UI.labelWithIcon(300,200,null)},
               {UI.buttonWithIcon(null, null,null)},
               {UI.of((SwingBuilder)null)},
               {UI.of((MenuBuilder)null)},
               {UI.of((JCheckBox)null)},
               {UI.of((JRadioButton)null)},
               {UI.of((JTextComponent)null)},
               {UI.of((JComponent)null)},
               {UI.of((JSplitButton)null)},
               {UI.of((JPopupMenu)null)},
               {UI.of((JSeparator)null)},
               {UI.panel().onMouseClick(null)},
               {UI.of(new JComboBox<>()).onMouseClick(null)},
               {UI.of(new JComboBox<>()).onChange(null)},
               {UI.of(new JSlider()).onChange(null)},
               {UI.button().onClick(null)},
               {UI.button().onChange(null)}
            ]
    }

}

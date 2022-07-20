package com.globaltcad.swingtree.common

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.api.MenuBuilder
import com.globaltcad.swingtree.api.SwingBuilder
import spock.lang.Specification

import javax.swing.*
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
                    {UI.label(null)},
                    {UI.label(300,200,null)},
                    {UI.button(null, null,null)},
                    {UI.of((SwingBuilder)null)},
                    {UI.of((MenuBuilder)null)},
                    {UI.of((JCheckBox)null)},
                    {UI.of((JRadioButton)null)},
                    {UI.of((JTextComponent)null)},
                    {UI.of((JComponent)null)},
                    {UI.of((JSplitButton)null)},
                    {UI.of((JPopupMenu)null)},
                    {UI.of((JSeparator)null)},
                    {UI.separator((UI.Align)null)},
                    {UI.of((JTextArea)null)},
                    {UI.of((JLabel)null)},
                    {UI.of((Object)null)},
                    {UI.of((JSplitButton)null)},
                    {UI.splitItem(null)},
                    {UI.splitButton(null)},
                    {UI.checkBox(null)},
                    {UI.radioButton(null)},
                    {UI.menuItem(null)},
                    {UI.splitItem(null)},
                    {UI.splitRadioItem(null)},
                    {UI.textField(null)},
                    {UI.textArea(null)},
                    {UI.panel().onMouseClick(null)},
                    {UI.of(new JComboBox<>()).onMouseClick(null)},
                    {UI.of(new JComboBox<>()).onSelection(null)},
                    {UI.of(new JSlider()).onChange(null)},
                    {UI.button().onClick(null)},
                    {UI.button().onChange(null)},
                    {UI.button((Icon)null)},
                    {UI.button((Icon)null,(Icon)null)},
                    {UI.tabbedPane((UI.Position)null)},
                    {UI.tabbedPane((UI.OverflowPolicy)null)},
                    {UI.tabbedPane((UI.Position)null,(UI.OverflowPolicy)null)},
                    {UI.slider((UI.Align)null)},
                    {UI.slider((UI.Align)null, 1, 10, 3)},
                    {UI.slider(UI.Align.HORIZONTAL).onChange(null)},
                    {UI.comboBox((List)null)},
                    {UI.comboBox((Object[])null)},
                    {UI.comboBox().onSelection(null)}
            ]
    }

}

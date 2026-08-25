package examples.laf;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import java.awt.Component;

/**
 *  Answers "does this control have the keyboard focus?" for the two components where the obvious
 *  question gives the wrong answer.
 *  <p>
 *  A style rule wants to know whether to draw a control as focused, and for most components
 *  {@code component.isFocusOwner()} says so. Composite controls are different: the focus lands on a
 *  descendant the application never sees. An editable combo box hands it to its editor, and a
 *  spinner's editor is itself a wrapper whose text field is the thing that takes focus - so asking
 *  either of them directly returns {@code false} forever, and their border never lights up.
 *
 *  @see LafFocus which makes them repaint when it changes
 */
final class Focus
{
    private Focus() {}

    /**
     *  A non-editable combo box owns the focus itself; an editable one delegates it to its editor.
     *
     * @param combo the combo box being styled
     * @return {@code true} if the focus is on the combo box or on its editor
     */
    static boolean isOn( JComboBox<?> combo ) {
        if ( combo.isFocusOwner() )
            return true;
        if ( combo.isEditable() && combo.getEditor() != null ) {
            Component editor = combo.getEditor().getEditorComponent();
            return editor != null && editor.isFocusOwner();
        }
        return false;
    }

    /**
     *  A spinner's editor is a wrapper component - a {@link JSpinner.DefaultEditor} by default -
     *  containing the actual input control, usually a formatted text field, as a child. This walks
     *  one level in to read the right flag for the spinner's outer border.
     *
     * @param spinner the spinner being styled
     * @return {@code true} if the focus is inside the spinner's editor
     */
    static boolean isOn( JSpinner spinner ) {
        Component editor = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            return ((JSpinner.DefaultEditor) editor).getTextField().isFocusOwner();
        return editor != null && editor.isFocusOwner();
    }
}

package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;
import java.util.Objects;

final class InternalCellEditor extends AbstractCellEditor implements TableCellEditor, TreeCellEditor {

    private final static Class<?>[] argTypes = new Class<?>[]{String.class};
    private java.lang.reflect.@Nullable Constructor<?> constructor;
    private @Nullable Object value;

    /** The Swing component being edited. */
    private @Nullable JComponent editorComponent;
    /**
     * The delegate class which handles all methods sent from the
     * <code>CellEditor</code>.
     */
    private @Nullable EditorDelegate delegate;
    /**
     * An integer specifying the number of clicks needed to start editing.
     * Even if <code>clickCountToStart</code> is defined as zero, it
     * will not initiate until a click occurs.
     */
    private int clickCountToStart = 1;

    private boolean hasDefaultComponent;

    private final Class<? extends JComponent> hostType;

    private boolean isInitialized = false;


    public InternalCellEditor(Class<? extends JComponent> hostType) {
        this.hostType = hostType;
    }

    public void ini(JComponent host, int row, int col) {
        if ( !isInitialized ) {
            isInitialized = true;
            Border defaultBorder = null;
            JComponent defaultEditorComponent = null;
            if ( host instanceof JTable ) {
                JTable table = (JTable) host;
                Class<?> columnDataType = table.getColumnClass(col);
                if ( Boolean.class.isAssignableFrom(columnDataType) )
                    defaultEditorComponent = new JCheckBox();
            }
            if ( defaultEditorComponent == null )
                defaultEditorComponent = new JTextField();

            if (JTable.class.isAssignableFrom(hostType)) {
                defaultBorder = UIManager.getBorder("Table.editorBorder");
                defaultEditorComponent.setName("Table.editor");
            } else if (JTree.class.isAssignableFrom(hostType)) {
                defaultBorder = UIManager.getBorder("Tree.editorBorder");
            } else if (JList.class.isAssignableFrom(hostType)) {
                defaultBorder = UIManager.getBorder("List.editorBorder");
            } else if (JComboBox.class.isAssignableFrom(hostType)) {
                defaultBorder = UIManager.getBorder("ComboBox.editorBorder");
            }
            if (defaultBorder == null)
                defaultBorder = new LineBorder(Color.BLACK);

            defaultEditorComponent.setBorder(defaultBorder);
            if ( defaultEditorComponent instanceof JTextField )
                _setEditor((JTextField) defaultEditorComponent);
            else if ( defaultEditorComponent instanceof JCheckBox )
                setEditor((JCheckBox) defaultEditorComponent);

            hasDefaultComponent = true;
        }
    }

    private void _setUIManagerInfo(JComponent editor) {
        String name = "";
        String info = "";
        if ( JTable.class.isAssignableFrom(hostType) )
            info = "isTableCellEditor";
        else if ( JTree.class.isAssignableFrom(hostType) )
            info = "isTreeCellEditor";
        else if ( JList.class.isAssignableFrom(hostType) )
            info = "isListCellEditor";
        else if ( JComboBox.class.isAssignableFrom(hostType) )
            info = "isComboBoxCellEditor";

        if ( editor instanceof JTextField )
            name = "JTextField";
        else if ( editor instanceof JCheckBox )
            name = "JCheckBox";
        else if ( editor instanceof JComboBox )
            name = "JComboBox";

        if ( !name.isEmpty() && !info.isEmpty() )
            editor.putClientProperty(name+"."+info, Boolean.TRUE);
    }

    public boolean hasDefaultComponent() {
        return hasDefaultComponent;
    }

    private void _setEditor(final JTextField textField) {
        editorComponent = textField;
        this.clickCountToStart = 2;
        delegate = new EditorDelegate() {
            public void setValue(@Nullable Object value) {
                textField.setText((value != null) ? value.toString() : "");
            }

            public Object getCellEditorValue() {
                return textField.getText();
            }
        };
        textField.addActionListener(delegate);
        _setUIManagerInfo(textField);
    }

    public void setEditor(final JTextField textField) {
        _setEditor(textField);
        hasDefaultComponent = false;
    }

    public void setValue(@Nullable Object value) {
        try {
            Objects.requireNonNull(delegate);
            delegate.setValue(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public @Nullable Component getComponent() {
        return editorComponent;
    }

    public @Nullable Component getCustomComponent() {
        if (hasDefaultComponent)
            return null;
        return editorComponent;
    }

    public void setEditor(final JCheckBox checkBox) {
        editorComponent = checkBox;
        delegate = new EditorDelegate() {
            public void setValue(@Nullable Object value) {
                boolean selected = false;
                if (value instanceof Boolean) {
                    selected = ((Boolean)value).booleanValue();
                }
                else if (value instanceof String) {
                    selected = value.equals("true");
                }
                checkBox.setSelected(selected);
            }

            public Object getCellEditorValue() {
                return Boolean.valueOf(checkBox.isSelected());
            }
        };
        checkBox.addActionListener(delegate);
        checkBox.setRequestFocusEnabled(false);
        _setUIManagerInfo(checkBox);
        hasDefaultComponent = false;
    }

    public void setEditor(final JComboBox<?> comboBox) {
        editorComponent = comboBox;
        delegate = new EditorDelegate() {
            public void setValue(@Nullable Object value) {
                comboBox.setSelectedItem(value);
            }

            public Object getCellEditorValue() {
                return comboBox.getSelectedItem();
            }

            public boolean shouldSelectCell(EventObject anEvent) {
                if (anEvent instanceof MouseEvent) {
                    MouseEvent e = (MouseEvent)anEvent;
                    return e.getID() != MouseEvent.MOUSE_DRAGGED;
                }
                return true;
            }
            public boolean stopCellEditing() {
                if (comboBox.isEditable()) {
                    // Commit edited value.
                    comboBox.actionPerformed(new ActionEvent(
                            InternalCellEditor.this, 0, ""));
                }
                return super.stopCellEditing();
            }
        };
        comboBox.addActionListener(delegate);
        _setUIManagerInfo(comboBox);
        hasDefaultComponent = false;
    }

    /**
     * Specifies the number of clicks needed to start editing.
     *
     * @param count  an int specifying the number of clicks needed to start editing
     * @see #getClickCountToStart
     */
    public void setClickCountToStart(int count) {
        clickCountToStart = count;
    }

    /**
     * Returns the number of clicks needed to start editing.
     * @return the number of clicks needed to start editing
     */
    public int getClickCountToStart() {
        return clickCountToStart;
    }

    /**
     * Forwards the message from the <code>CellEditor</code> to
     * the <code>delegate</code>.
     * @see EditorDelegate#getCellEditorValue
     */
    public @Nullable Object getCellEditorValue() {
        if ( JTable.class.isAssignableFrom(hostType) )
            return this.value;
        Objects.requireNonNull(delegate);
        return delegate.getCellEditorValue();
    }

    /**
     * Returns true if <code>anEvent</code> is <b>not</b> a
     * <code>MouseEvent</code>.  Otherwise, it returns true
     * if the necessary number of clicks have occurred, and
     * returns false otherwise.
     *
     * @param   anEvent         the event
     * @return  true  if cell is ready for editing, false otherwise
     * @see #setClickCountToStart
     * @see #shouldSelectCell
     */
    @Override
    public boolean isCellEditable(EventObject anEvent) {
        if (anEvent instanceof MouseEvent) {
            return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
        }
        return true;
    }

    /**
     * Forwards the message from the <code>CellEditor</code> to
     * the <code>delegate</code>.
     * @see EditorDelegate#shouldSelectCell(EventObject)
     */
    public boolean shouldSelectCell(EventObject anEvent) {
        Objects.requireNonNull(delegate);
        return delegate.shouldSelectCell(anEvent);
    }

    /**
     * Forwards the message from the <code>CellEditor</code> to
     * the <code>delegate</code>.
     * @see EditorDelegate#stopCellEditing
     */
    public boolean stopCellEditing() {
        Objects.requireNonNull(delegate);
        if ( JTable.class.isAssignableFrom(hostType) && constructor != null ) {
            @Nullable Object s = delegate.getCellEditorValue();
            try {
                if ("".equals(s)) {
                    if (constructor.getDeclaringClass() == String.class) {
                        value = s;
                    }
                    return super.stopCellEditing();
                }

                value = constructor.newInstance(new Object[]{s});
            }
            catch (Exception e) {
                editorComponent.setBorder(new LineBorder(Color.red));
                return false;
            }
        }
        return delegate.stopCellEditing();
    }

    /**
     * Forwards the message from the <code>CellEditor</code> to
     * the <code>delegate</code>.
     * @see EditorDelegate#cancelCellEditing
     */
    public void cancelCellEditing() {
        Objects.requireNonNull(delegate);
        delegate.cancelCellEditing();
    }

    /** Implements the <code>TreeCellEditor</code> interface. */
    public Component getTreeCellEditorComponent(JTree tree, @Nullable Object value,
                                                boolean isSelected,
                                                boolean expanded,
                                                boolean leaf, int row) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(editorComponent);
        String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, false);
        delegate.setValue(stringValue);
        return editorComponent;
    }

//
//  Implementing the CellEditor Interface
//
    /** Implements the <code>TableCellEditor</code> interface. */
    public Component getTableCellEditorComponent(JTable table, @Nullable Object value,
                                                 boolean isSelected,
                                                 int row, int column) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(editorComponent);
        delegate.setValue(value);
        if (editorComponent instanceof JCheckBox) {
            //in order to avoid a "flashing" effect when clicking a checkbox
            //in a table, it is important for the editor to have as a border
            //the same border that the renderer has, and have as the background
            //the same color as the renderer has. This is primarily only
            //needed for JCheckBox since this editor doesn't fill all the
            //visual space of the table cell, unlike a text field.
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component c = renderer.getTableCellRendererComponent(table, value,
                    isSelected, true, row, column);
            if (c != null) {
                editorComponent.setOpaque(true);
                editorComponent.setBackground(c.getBackground());
                if (c instanceof JComponent) {
                    editorComponent.setBorder(((JComponent)c).getBorder());
                }
            } else {
                editorComponent.setOpaque(false);
            }
        }
        return editorComponent;
    }

    public void updateForTable(JTable table, int column) {
        if ( JTable.class.isAssignableFrom(hostType) ) {
            this.value = null;
            try {
                Class<?> type = table.getColumnClass(column);
                if ( editorComponent instanceof JTextField ) {
                    JTextField tf = (JTextField) editorComponent;
                    int alignment = tf.getHorizontalAlignment();
                    if (Number.class.isAssignableFrom(type)) {
                        if ( alignment != JTextField.RIGHT )
                            tf.setHorizontalAlignment(JTextField.RIGHT);
                    } else {
                        if ( alignment == JTextField.RIGHT )
                            tf.setHorizontalAlignment(JTextField.LEADING);
                    }
                }
                if ( editorComponent instanceof JCheckBox ) {
                    JCheckBox cb = (JCheckBox) editorComponent;
                    if ( Boolean.class.isAssignableFrom(type) ) {
                        if ( cb.getHorizontalAlignment() != JCheckBox.CENTER )
                            cb.setHorizontalAlignment(JCheckBox.CENTER);
                    }
                }
                // Since our obligation is to produce a value which is
                // assignable for the required type it is OK to use the
                // String constructor for columns which are declared
                // to contain Objects. A String is an Object.
                if (type == Object.class) {
                    type = String.class;
                }
                constructor = type.getConstructor(argTypes);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The protected <code>EditorDelegate</code> class.
     */
    protected class EditorDelegate implements ActionListener, ItemListener {

        /**  The value of this cell. */
        protected @Nullable Object value;

        /**
         * Returns the value of this cell.
         * @return the value of this cell
         */
        public @Nullable Object getCellEditorValue() {
            return value;
        }

        /**
         * Sets the value of this cell.
         * @param value the new value of this cell
         */
        public void setValue(@Nullable Object value) {
            this.value = value;
        }

        /**
         * Returns true to indicate that the editing cell may
         * be selected.
         *
         * @param   anEvent         the event
         * @return  true
         * @see #isCellEditable
         */
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        /**
         * Returns true to indicate that editing has begun.
         *
         * @param anEvent          the event
         * @return true to indicate editing has begun
         */
        public boolean startCellEditing(EventObject anEvent) {
            return true;
        }

        /**
         * Stops editing and
         * returns true to indicate that editing has stopped.
         * This method calls <code>fireEditingStopped</code>.
         *
         * @return  true
         */
        public boolean stopCellEditing() {
            fireEditingStopped();
            return true;
        }

        /**
         * Cancels editing.  This method calls <code>fireEditingCanceled</code>.
         */
        public void cancelCellEditing() {
            fireEditingCanceled();
        }

        /**
         * When an action is performed, editing is ended.
         * @param e the action event
         * @see #stopCellEditing
         */
        public void actionPerformed(ActionEvent e) {
            InternalCellEditor.this.stopCellEditing();
        }

        /**
         * When an item's state changes, editing is ended.
         * @param e the action event
         * @see #stopCellEditing
         */
        public void itemStateChanged(ItemEvent e) {
            InternalCellEditor.this.stopCellEditing();
        }
    }

}

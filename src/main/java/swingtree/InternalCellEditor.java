package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Problem;
import sprouts.Result;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellEditor;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;

final class InternalCellEditor extends AbstractCellEditor implements TableCellEditor, TreeCellEditor {

    private final static Class<?>[] argTypes = new Class<?>[]{String.class};
    private static final Logger log = LoggerFactory.getLogger(InternalCellEditor.class);
    private @Nullable Constructor<?> constructor;
    private Result<Object> editorOutputValue;

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
        this.editorOutputValue = Result.of(Object.class);
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
            @Override
            public void setPresentationEntry(@Nullable Object value) {
                textField.setText((value != null) ? value.toString() : "");
            }

            @Override
            public Object getCurrentCellEditorEntry() {
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

    public void setEntry(
        @Nullable Object presentationEntry, // Typically a nicely formatted string
        @Nullable Object originalEntryFromModel, // The original value from the model
        Class<?> targetedEntryType
    ) {
        try {
            Objects.requireNonNull(delegate);
            delegate.setValueAndTarget(presentationEntry, originalEntryFromModel, targetedEntryType);
        } catch (Exception e) {
            log.debug(SwingTree.get().logMarker(), "Failed to internal cell editor value for host type '"+hostType.getName()+"'", e);
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
            @Override
            public void setPresentationEntry(@Nullable Object value) {
                boolean selected = false;
                if (value instanceof Boolean) {
                    selected = (Boolean) value;
                }
                else if (value instanceof String) {
                    selected = value.equals("true");
                }
                checkBox.setSelected(selected);
            }

            @Override
            public Object getCurrentCellEditorEntry() {
                return checkBox.isSelected();
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
            @Override
            public void setPresentationEntry(@Nullable Object value) {
                comboBox.setSelectedItem(value);
            }

            @Override
            public @Nullable Object getCurrentCellEditorEntry() {
                return comboBox.getSelectedItem();
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                if (anEvent instanceof MouseEvent) {
                    MouseEvent e = (MouseEvent)anEvent;
                    return e.getID() != MouseEvent.MOUSE_DRAGGED;
                }
                return true;
            }
            @Override
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
     * @see EditorDelegate#getCellEditorEntryAsTargetType
     */
    @Override
    public @Nullable Object getCellEditorValue() {
        if ( JTable.class.isAssignableFrom(hostType) )
            return this.editorOutputValue.orElseNull();
        Objects.requireNonNull(delegate);
        return delegate.getCellEditorEntryAsTargetType().orElseNull();
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
    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        Objects.requireNonNull(delegate);
        return delegate.shouldSelectCell(anEvent);
    }

    /**
     * Forwards the message from the <code>CellEditor</code> to
     * the <code>delegate</code>.
     * @see EditorDelegate#stopCellEditing
     */
    @Override
    public boolean stopCellEditing() {
        Objects.requireNonNull(delegate);
        if ( JTable.class.isAssignableFrom(hostType) ) {
            Result<Object> newValueResult = delegate.getCellEditorEntryAsTargetType();
            @Nullable Object newValue = newValueResult.orElseNull();
            this.editorOutputValue = newValueResult;
            if ( constructor == null ) {
                return super.stopCellEditing();
            } else {
                try {
                    if ("".equals(newValue)) {
                        return super.stopCellEditing();
                    } else if (newValue != null) {
                        if (constructor.getDeclaringClass().isAssignableFrom(newValue.getClass())) {
                            return super.stopCellEditing();
                        }
                    }

                    this.editorOutputValue = Result.of(constructor.newInstance(new Object[]{newValue}));
                } catch (Exception e) {
                    if (editorComponent != null)
                        editorComponent.setBorder(new LineBorder(Color.red));
                    return false;
                }
            }
        }
        return delegate.stopCellEditing();
    }

    /**
     * Forwards the message from the <code>CellEditor</code> to
     * the <code>delegate</code>.
     * @see EditorDelegate#cancelCellEditing
     */
    @Override
    public void cancelCellEditing() {
        Objects.requireNonNull(delegate);
        delegate.cancelCellEditing();
    }

    /** Implements the <code>TreeCellEditor</code> interface. */
    @Override
    public Component getTreeCellEditorComponent(JTree tree, @Nullable Object entryFromModel,
                                                boolean isSelected,
                                                boolean expanded,
                                                boolean leaf, int row) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(editorComponent);
        String entryAsString = tree.convertValueToText(entryFromModel, isSelected, expanded, leaf, row, false);
        delegate.setValueAndTarget(entryAsString, entryFromModel, entryFromModel == null ? Object.class : entryFromModel.getClass());
        return editorComponent;
    }

//
//  Implementing the CellEditor Interface
//
    /** Implements the <code>TableCellEditor</code> interface. */
    @Override
    public Component getTableCellEditorComponent(JTable table, @Nullable Object entryFromModel,
                                                 boolean isSelected,
                                                 int row, int column) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(editorComponent);
        delegate.setValueAndTarget(entryFromModel, entryFromModel, entryFromModel == null ? Object.class : entryFromModel.getClass());
        if (editorComponent instanceof JCheckBox) {
            //in order to avoid a "flashing" effect when clicking a checkbox
            //in a table, it is important for the editor to have as a border
            //the same border that the renderer has, and have as the background
            //the same color as the renderer has. This is primarily only
            //needed for JCheckBox since this editor doesn't fill all the
            //visual space of the table cell, unlike a text field.
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component c = renderer.getTableCellRendererComponent(table, entryFromModel,
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
            this.editorOutputValue = Result.of(Object.class);
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

                if (type != Object.class) {
                    constructor = type.getConstructor(argTypes);
                }
            }
            catch (Exception e) {
                log.debug(SwingTree.get().logMarker(), "Failed to update internal cell editor for host type '"+hostType.getName()+"'", e);
            }
        }
    }

    /**
     * The protected <code>EditorDelegate</code> class.
     */
    protected abstract class EditorDelegate implements ActionListener, ItemListener {

        private Class<?> targetType = Object.class;
        private @Nullable Object originalValue;

        public final void setValueAndTarget(
            @Nullable Object entryToBePresentedAndEdited,
            @Nullable Object originalEntryFromModel,
            Class<?> targetType
        ) {
            this.targetType = targetType;
            this.originalValue = originalEntryFromModel;
            this.setPresentationEntry(entryToBePresentedAndEdited);
        }

        /**
         * Returns the value of this cell directly from the editor component.
         * @return the value of this cell
         */
        protected abstract @Nullable Object getCurrentCellEditorEntry();

        /**
         *  Tries to convert the value of this cell to the target type
         *  and returns it as a {@link Result}, which may contain problems
         *  if the conversion failed.
         *
         * @return the value of this cell as the target type if possible
         *         or the raw value if conversion is not possible.
         */
        public final Result<@Nullable Object> getCellEditorEntryAsTargetType() {
            Object value = getCurrentCellEditorEntry();
            if ( value == null )
                return Result.of(Object.class);
            if ( targetType == Object.class )
                return Result.of(value);
            if ( targetType.isAssignableFrom(value.getClass()) )
                return Result.of(value);
            try {
                return _tryConvert(value, targetType);
            } catch (Exception e) {
                log.debug(
                        "Failed to convert internal cell editor value " +
                        "from '"+value+"' to target type '"+targetType.getName()+"' " +
                        "for host component type '"+hostType.getName()+"'",
                        e
                    );
            }

            List<Problem> problems = new ArrayList<>();
            problems.add(Problem.of(
                    "Failed to convert internal cell editor value " +
                        "from '"+value+"' to target type '"+targetType.getName()+"' " +
                        "for host component type '"+hostType.getName()+"'"
                    ));
            Object restoredValue = this.originalValue;
            try {
                if ( restoredValue != null )
                    restoredValue = _tryConvert(restoredValue, targetType).orElseNullable(value);
            } catch (Exception e) {
                problems.add(Problem.of(e));
                log.debug(
                        "Failed to convert internal cell editor value received before editing " +
                        "from '"+this.originalValue+"' to target type '"+targetType.getName()+"' " +
                        "for host component type '"+hostType.getName()+"'",
                        e
                    );
            }
            return Result.of(
                    Object.class,
                    restoredValue,
                    problems
                );
        }

        /**
         * Sets the value of this cell.
         * @param value the new value of this cell
         */
        protected abstract void setPresentationEntry(@Nullable Object value);

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
        @Override
        public void actionPerformed(ActionEvent e) {
            InternalCellEditor.this.stopCellEditing();
        }

        /**
         * When an item's state changes, editing is ended.
         * @param e the action event
         * @see #stopCellEditing
         */
        @Override
        public void itemStateChanged(ItemEvent e) {
            InternalCellEditor.this.stopCellEditing();
        }

        private Result<@Nullable Object> _tryConvert(Object value, Class<?> targetType) throws Exception {
            if ( targetType == String.class ) {
                if ( value instanceof String )
                    return Result.of(value);
                else if ( value instanceof Number )
                    return Result.of(value.toString());
                else if ( value instanceof Boolean )
                    return Result.of(value.toString());
                else if ( value instanceof Character )
                    return Result.of(value.toString());
                else
                    return Result.of(value.toString());
            } else if ( targetType == Character.class ) {
                if ( value instanceof Character )
                    return Result.of(value);
                else if ( value instanceof String ) {
                    String str = (String) value;
                    if ( str.length() == 1 )
                        return Result.of(str.charAt(0));
                }
            } else if ( targetType == Boolean.class ) {
                if ( value instanceof Boolean )
                    return Result.of(value);
                else if ( value instanceof String ) {
                    String str = (String) value;
                    if ( str.equalsIgnoreCase("true") )
                        return Result.of(true);
                    if ( str.equalsIgnoreCase("false") )
                        return Result.of(false);
                }
            } else if ( Number.class.isAssignableFrom(targetType) ) {
                if ( value instanceof Number ) {
                    if ( targetType == Integer.class )
                        return Result.of(((Number) value).intValue());
                    if ( targetType == Long.class )
                        return Result.of(((Number) value).longValue());
                    if ( targetType == Float.class )
                        return Result.of(((Number) value).floatValue());
                    if ( targetType == Double.class )
                        return Result.of(((Number) value).doubleValue());
                    if ( targetType == Byte.class )
                        return Result.of(((Number) value).byteValue());
                    if ( targetType == Short.class )
                        return Result.of(((Number) value).shortValue());
                } else if ( value instanceof String ) {
                    String str = (String) value;
                    if ( targetType == Integer.class )
                        return Result.of(Integer.parseInt(str));
                    if ( targetType == Long.class )
                        return Result.of(Long.parseLong(str));
                    if ( targetType == Float.class )
                        return Result.of(Float.parseFloat(str));
                    if ( targetType == Double.class )
                        return Result.of(Double.parseDouble(str));
                    if ( targetType == Byte.class )
                        return Result.of(Byte.parseByte(str));
                    if ( targetType == Short.class )
                        return Result.of(Short.parseShort(str));
                } else if ( value instanceof Boolean ) {
                    if ( targetType == Integer.class )
                        return Result.of(((Boolean) value) ? 1 : 0);
                    if ( targetType == Long.class )
                        return Result.of(((Boolean) value) ? 1L : 0L);
                    if ( targetType == Float.class )
                        return Result.of(((Boolean) value) ? 1.0f : 0.0f);
                    if ( targetType == Double.class )
                        return Result.of(((Boolean) value) ? 1.0 : 0.0);
                    if ( targetType == Byte.class )
                        return Result.of(((Boolean) value) ? (byte) 1 : (byte) 0);
                    if ( targetType == Short.class )
                        return Result.of(((Boolean) value) ? (short) 1 : (short) 0);
                }
            }
            throw new IllegalArgumentException(
                    "Cannot convert value '"+value+"' to target type '"+targetType.getName()+"'."
            );
        }

    }

}

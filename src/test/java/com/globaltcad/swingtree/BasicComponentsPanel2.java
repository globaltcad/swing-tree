package com.globaltcad.swingtree;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;

class BasicComponentsPanel2 extends JPanel {
    private JPasswordField passwordField1;
    private JTextField leadingIconTextField;
    private JTextField trailingIconTextField;
    private JTextField iconsTextField;
    private JTextField compsTextField;
    private JTextField clearTextField;

    BasicComponentsPanel2() {
        FlatLightLaf.setup();
        initComponents();
        confiugure();
    }

    private void initComponents() {

        JComboBox<String> comboBox1 = new JComboBox<>();
        JComboBox<String> comboBox2 = new JComboBox<>();
        JComboBox<String> comboBox3 = new JComboBox<>();
        JComboBox<String> comboBox4 = new JComboBox<>();
        JComboBox<String> comboBox5 = new JComboBox<>();
        JSpinner spinner1 = new JSpinner();
        JTextField textField1 = new JTextField();
        JTextField textField2 = new JTextField();
        JTextField textField3 = new JTextField();
        JTextField textField4 = new JTextField();
        JTextField textField6 = new JTextField();
        JLabel formattedTextFieldLabel = new JLabel();
        JFormattedTextField formattedTextField1 = new JFormattedTextField();
        JFormattedTextField formattedTextField2 = new JFormattedTextField();
        JFormattedTextField formattedTextField3 = new JFormattedTextField();
        JFormattedTextField formattedTextField4 = new JFormattedTextField();
        JFormattedTextField formattedTextField5 = new JFormattedTextField();
        JLabel passwordFieldLabel = new JLabel();
        passwordField1 = new JPasswordField();
        JPasswordField passwordField2 = new JPasswordField();
        JPasswordField passwordField3 = new JPasswordField();
        JPasswordField passwordField4 = new JPasswordField();
        JPasswordField passwordField5 = new JPasswordField();
        JLabel textAreaLabel = new JLabel();
        JScrollPane scrollPane1 = new JScrollPane();
        JTextArea textArea1 = new JTextArea();
        JScrollPane scrollPane2 = new JScrollPane();
        JTextArea textArea2 = new JTextArea();
        JScrollPane scrollPane3 = new JScrollPane();
        JTextArea textArea3 = new JTextArea();
        JScrollPane scrollPane4 = new JScrollPane();
        JTextArea textArea4 = new JTextArea();
        JTextArea textArea5 = new JTextArea();
        JLabel editorPaneLabel = new JLabel();
        JScrollPane scrollPane5 = new JScrollPane();
        JEditorPane editorPane1 = new JEditorPane();
        JScrollPane scrollPane6 = new JScrollPane();
        JEditorPane editorPane2 = new JEditorPane();
        JScrollPane scrollPane7 = new JScrollPane();
        JEditorPane editorPane3 = new JEditorPane();
        JScrollPane scrollPane8 = new JScrollPane();
        JEditorPane editorPane4 = new JEditorPane();
        JEditorPane editorPane5 = new JEditorPane();
        JLabel textPaneLabel = new JLabel();
        JScrollPane scrollPane9 = new JScrollPane();
        JTextPane textPane1 = new JTextPane();
        JScrollPane scrollPane10 = new JScrollPane();
        JTextPane textPane2 = new JTextPane();
        JScrollPane scrollPane11 = new JScrollPane();
        JTextPane textPane3 = new JTextPane();
        JScrollPane scrollPane12 = new JScrollPane();
        JTextPane textPane4 = new JTextPane();
        JTextPane textPane5 = new JTextPane();
        JLabel errorHintsLabel = new JLabel();
        JTextField errorHintsTextField = new JTextField();
        JComboBox<String> errorHintsComboBox = new JComboBox<>();
        JSpinner errorHintsSpinner = new JSpinner();
        JLabel warningHintsLabel = new JLabel();
        JTextField warningHintsTextField = new JTextField();
        JComboBox<String> warningHintsComboBox = new JComboBox<>();
        JSpinner warningHintsSpinner = new JSpinner();
        JLabel iconsLabel = new JLabel();
        leadingIconTextField = new JTextField();
        trailingIconTextField = new JTextField();
        iconsTextField = new JTextField();
        JLabel compsLabel = new JLabel();
        compsTextField = new JTextField();
        clearTextField = new JTextField();
        JLabel fontsLabel = new JLabel();
        JLabel h00Label = new JLabel();
        JLabel h0Label = new JLabel();
        JLabel h1Label = new JLabel();
        JLabel h2Label = new JLabel();
        JLabel h3Label = new JLabel();
        JLabel h4Label = new JLabel();
        JLabel lightLabel = new JLabel();
        JLabel semiboldLabel = new JLabel();
        JLabel fontZoomLabel = new JLabel();
        JLabel largeLabel = new JLabel();
        JLabel defaultLabel = new JLabel();
        JLabel mediumLabel = new JLabel();
        JLabel smallLabel = new JLabel();
        JLabel miniLabel = new JLabel();
        JLabel monospacedLabel = new JLabel();
        JPopupMenu popupMenu1 = new JPopupMenu();
        JMenuItem cutMenuItem = new JMenuItem();
        JMenuItem copyMenuItem = new JMenuItem();
        JMenuItem pasteMenuItem = new JMenuItem();

        //======== this ========
        UI.of(this)
                        .withLayout(
                "insets dialog,hidemode 3",
                // columns
                "[]" +
                        "[sizegroup 1]" +
                        "[sizegroup 1]" +
                        "[sizegroup 1]" +
                        "[]" +
                        "[]",
                // rows
                "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]para" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]0" +
                        "[]"
        )
        .add("cell 0 0", UI.label("JLabel:"))
        .add("cell 1 0", UI.label("Enabled").make( it -> it.setDisplayedMnemonic('E') ))
        .add("cell 2 0", UI.label("Disabled").isEnabledIf(false).make( it -> it.setDisplayedMnemonic('D') ))
        .add("cell 0 1", UI.label("JButton:"))
        .add("cell 1 1", UI.button("Enabled").make( it -> it.setDisplayedMnemonicIndex(0) ))
        .add("cell 2 1", UI.button("Disabled").isEnabledIf(false).make( it -> it.setDisplayedMnemonicIndex(0) ))
        .add("cell 3 1", UI.button("Square").withProperty("JButton.buttonType", "square"))
        .add("cell 4 1", UI.button("Round").withProperty("JButton.buttonType", "roundRect"))
        .add("cell 4 1", UI.button("Help").withProperty("JButton.buttonType", "help"))
        .add("cell 4 1", UI.button("Help").isEnabledIf(false).withProperty("JButton.buttonType", "help"))
        .add("cell 5 1", UI.buttonWithIcon(UIManager.getIcon("Tree.closedIcon")))
        .add("cell 5 1", UI.button("..."))
        .add("cell 5 1", UI.button("\u2026"))
        .add("cell 5 1", UI.button("#"))
        .add("cell 0 2", UI.label("JCheckBox"))
        .add("cell 1 2", UI.checkBox("Enabled").make( it -> it.setMnemonic('A') ))
        .add("cell 2 2", UI.checkBox("Disabled").isEnabledIf(false).make( it -> it.setMnemonic('D') ))
        .add("cell 3 2", UI.checkBox("Selected").isSelectedIf(true))
        .add("cell 4 2", UI.checkBox("Selected disabled").isSelectedIf(true).isEnabledIf(false))
        .add("cell 0 3", UI.label("JRadioButton:"))
        .add("cell 1 3", UI.radioButton("Enabled").make( it -> it.setMnemonic('N') ))
        .add("cell 2 3", UI.radioButton("Disabled").isEnabledIf(false).make( it -> it.setMnemonic('S') ))
        .add("cell 3 3", UI.radioButton("Selected").isSelectedIf(true))
        .add("cell 4 3", UI.radioButton("Selected disabled").isEnabledIf(false).isSelectedIf(true))
        .add("cell 0 4", UI.label("JComboBox:").make( it -> {it.setDisplayedMnemonic('C');it.setLabelFor(comboBox1);}))
        .add("cell 1 4, growx", UI.of(comboBox1).isEnabledIf(true).make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Editable",
                "a",
                "bb",
                "ccc"
        }))))
        .add("cell 2 4, growx", UI.of(comboBox2).isEditableIf(true).isEnabledIf(false).make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Disabled",
                "a",
                "bb",
                "ccc"
        }))))
        .add("cell 3 4, growx", UI.of(comboBox3).make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Not editable",
                "a",
                "bb",
                "ccc"
        })) ))
        .add("cell 4 4, growx", UI.of(comboBox4).isEnabledIf(false).make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Not editable disabled",
                "a",
                "bb",
                "ccc"
        })) ))
        .add("cell 5 4, growx, wmax 100", UI.of(comboBox5).make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Wide popup if text is longer",
                "aa",
                "bbb",
                "cccc"
        })) ))
        .add("cell 0 5", UI.label("JSpinner:").make( it -> {it.setLabelFor(spinner1);it.setDisplayedMnemonic('S');}))
        .add("cell 1 5, growx", UI.of(spinner1))
        .add("cell 2 5, growx", UI.spinner().isEnabledIf(false))
        .add("cell 5 5, growx", UI.comboBox().isEditableIf(true).withProperty("JTextField.placeholderText", "Placeholder"))
        .add("cell 0 6", UI.label("JTextField:").make( it -> {it.setLabelFor(textField1);it.setDisplayedMnemonic('T');} ));

        //---- textField1 ----
        textField1.setText("Editable");
        textField1.setComponentPopupMenu(popupMenu1);
        add(textField1, "cell 1 6,growx");

        //---- textField2 ----
        textField2.setText("Disabled");
        textField2.setEnabled(false);
        add(textField2, "cell 2 6,growx");

        //---- textField3 ----
        textField3.setText("Not editable");
        textField3.setEditable(false);
        add(textField3, "cell 3 6,growx");

        //---- textField4 ----
        textField4.setText("Not editable disabled");
        textField4.setEnabled(false);
        textField4.setEditable(false);
        add(textField4, "cell 4 6,growx");

        //---- textField6 ----
        textField6.putClientProperty("JTextField.placeholderText", "Placeholder");
        add(textField6, "cell 5 6,growx");

        //---- formattedTextFieldLabel ----
        formattedTextFieldLabel.setText("JFormattedTextField:");
        formattedTextFieldLabel.setLabelFor(formattedTextField1);
        formattedTextFieldLabel.setDisplayedMnemonic('O');
        add(formattedTextFieldLabel, "cell 0 7");

        //---- formattedTextField1 ----
        formattedTextField1.setText("Editable");
        formattedTextField1.setComponentPopupMenu(popupMenu1);
        add(formattedTextField1, "cell 1 7,growx");

        //---- formattedTextField2 ----
        formattedTextField2.setText("Disabled");
        formattedTextField2.setEnabled(false);
        add(formattedTextField2, "cell 2 7,growx");

        //---- formattedTextField3 ----
        formattedTextField3.setText("Not editable");
        formattedTextField3.setEditable(false);
        add(formattedTextField3, "cell 3 7,growx");

        //---- formattedTextField4 ----
        formattedTextField4.setText("Not editable disabled");
        formattedTextField4.setEnabled(false);
        formattedTextField4.setEditable(false);
        add(formattedTextField4, "cell 4 7,growx");

        //---- formattedTextField5 ----
        formattedTextField5.putClientProperty("JTextField.placeholderText", "Placeholder");
        add(formattedTextField5, "cell 5 7,growx");

        //---- passwordFieldLabel ----
        passwordFieldLabel.setText("JPasswordField:");
        add(passwordFieldLabel, "cell 0 8");

        //---- passwordField1 ----
        passwordField1.setText("Editable");
        add(passwordField1, "cell 1 8,growx");

        //---- passwordField2 ----
        passwordField2.setText("Disabled");
        passwordField2.setEnabled(false);
        add(passwordField2, "cell 2 8,growx");

        //---- passwordField3 ----
        passwordField3.setText("Not editable");
        passwordField3.setEditable(false);
        add(passwordField3, "cell 3 8,growx");

        //---- passwordField4 ----
        passwordField4.setText("Not editable disabled");
        passwordField4.setEnabled(false);
        passwordField4.setEditable(false);
        add(passwordField4, "cell 4 8,growx");

        //---- passwordField5 ----
        passwordField5.putClientProperty("JTextField.placeholderText", "Placeholder");
        add(passwordField5, "cell 5 8,growx");

        //---- textAreaLabel ----
        textAreaLabel.setText("JTextArea:");
        add(textAreaLabel, "cell 0 9");

        //======== scrollPane1 ========
        {
            scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textArea1 ----
            textArea1.setText("Editable");
            textArea1.setRows(2);
            scrollPane1.setViewportView(textArea1);
        }
        add(scrollPane1, "cell 1 9,growx");

        //======== scrollPane2 ========
        {
            scrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textArea2 ----
            textArea2.setText("Disabled");
            textArea2.setRows(2);
            textArea2.setEnabled(false);
            scrollPane2.setViewportView(textArea2);
        }
        add(scrollPane2, "cell 2 9,growx");

        //======== scrollPane3 ========
        {
            scrollPane3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textArea3 ----
            textArea3.setText("Not editable");
            textArea3.setRows(2);
            textArea3.setEditable(false);
            scrollPane3.setViewportView(textArea3);
        }
        add(scrollPane3, "cell 3 9,growx");

        //======== scrollPane4 ========
        {
            scrollPane4.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane4.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textArea4 ----
            textArea4.setText("Not editable disabled");
            textArea4.setRows(2);
            textArea4.setEditable(false);
            textArea4.setEnabled(false);
            scrollPane4.setViewportView(textArea4);
        }
        add(scrollPane4, "cell 4 9,growx");

        //---- textArea5 ----
        textArea5.setRows(2);
        textArea5.setText("No scroll pane");
        add(textArea5, "cell 5 9,growx");

        //---- editorPaneLabel ----
        editorPaneLabel.setText("JEditorPane");
        add(editorPaneLabel, "cell 0 10");

        //======== scrollPane5 ========
        {
            scrollPane5.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane5.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- editorPane1 ----
            editorPane1.setText("Editable");
            scrollPane5.setViewportView(editorPane1);
        }
        add(scrollPane5, "cell 1 10,growx");

        //======== scrollPane6 ========
        {
            scrollPane6.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane6.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- editorPane2 ----
            editorPane2.setText("Disabled");
            editorPane2.setEnabled(false);
            scrollPane6.setViewportView(editorPane2);
        }
        add(scrollPane6, "cell 2 10,growx");

        //======== scrollPane7 ========
        {
            scrollPane7.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane7.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- editorPane3 ----
            editorPane3.setText("Not editable");
            editorPane3.setEditable(false);
            scrollPane7.setViewportView(editorPane3);
        }
        add(scrollPane7, "cell 3 10,growx");

        //======== scrollPane8 ========
        {
            scrollPane8.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane8.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- editorPane4 ----
            editorPane4.setText("Not editable disabled");
            editorPane4.setEditable(false);
            editorPane4.setEnabled(false);
            scrollPane8.setViewportView(editorPane4);
        }
        add(scrollPane8, "cell 4 10,growx");

        //---- editorPane5 ----
        editorPane5.setText("No scroll pane");
        add(editorPane5, "cell 5 10,growx");

        //---- textPaneLabel ----
        textPaneLabel.setText("JTextPane:");
        add(textPaneLabel, "cell 0 11");

        //======== scrollPane9 ========
        {
            scrollPane9.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane9.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textPane1 ----
            textPane1.setText("Editable");
            scrollPane9.setViewportView(textPane1);
        }
        add(scrollPane9, "cell 1 11,growx");

        //======== scrollPane10 ========
        {
            scrollPane10.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane10.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textPane2 ----
            textPane2.setText("Disabled");
            textPane2.setEnabled(false);
            scrollPane10.setViewportView(textPane2);
        }
        add(scrollPane10, "cell 2 11,growx");

        //======== scrollPane11 ========
        {
            scrollPane11.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane11.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textPane3 ----
            textPane3.setText("Not editable");
            textPane3.setEditable(false);
            scrollPane11.setViewportView(textPane3);
        }
        add(scrollPane11, "cell 3 11,growx");

        //======== scrollPane12 ========
        {
            scrollPane12.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane12.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textPane4 ----
            textPane4.setText("Not editable disabled");
            textPane4.setEditable(false);
            textPane4.setEnabled(false);
            scrollPane12.setViewportView(textPane4);
        }
        add(scrollPane12, "cell 4 11,growx");

        //---- textPane5 ----
        textPane5.setText("No scroll pane");
        add(textPane5, "cell 5 11,growx");

        //---- errorHintsLabel ----
        errorHintsLabel.setText("Error hints:");
        add(errorHintsLabel, "cell 0 12");

        //---- errorHintsTextField ----
        errorHintsTextField.putClientProperty("JComponent.outline", "error");
        add(errorHintsTextField, "cell 1 12,growx");

        //---- errorHintsComboBox ----
        errorHintsComboBox.putClientProperty("JComponent.outline", "error");
        errorHintsComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "Editable"
        }));
        errorHintsComboBox.setEditable(true);
        add(errorHintsComboBox, "cell 2 12,growx");

        //---- errorHintsSpinner ----
        errorHintsSpinner.putClientProperty("JComponent.outline", "error");
        add(errorHintsSpinner, "cell 3 12,growx");

        //---- warningHintsLabel ----
        warningHintsLabel.setText("Warning hints:");
        add(warningHintsLabel, "cell 0 13");

        //---- warningHintsTextField ----
        warningHintsTextField.putClientProperty("JComponent.outline", "warning");
        add(warningHintsTextField, "cell 1 13,growx");

        //---- warningHintsComboBox ----
        warningHintsComboBox.putClientProperty("JComponent.outline", "warning");
        warningHintsComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "Not editable"
        }));
        add(warningHintsComboBox, "cell 2 13,growx");

        //---- warningHintsSpinner ----
        warningHintsSpinner.putClientProperty("JComponent.outline", "warning");
        add(warningHintsSpinner, "cell 3 13,growx");

        //---- iconsLabel ----
        iconsLabel.setText("Leading/trailing icons:");
        add(iconsLabel, "cell 0 14");
        add(leadingIconTextField, "cell 1 14,growx");

        //---- trailingIconTextField ----
        trailingIconTextField.setText("text");
        add(trailingIconTextField, "cell 2 14,growx");

        //---- iconsTextField ----
        iconsTextField.setText("text");
        add(iconsTextField, "cell 3 14,growx");

        //---- compsLabel ----
        compsLabel.setText("Leading/trailing comp.:");
        add(compsLabel, "cell 0 15");
        add(compsTextField, "cell 1 15 2 1,growx");

        //---- clearTextField ----
        clearTextField.setText("clear me");
        add(clearTextField, "cell 3 15,growx");

        //---- fontsLabel ----
        fontsLabel.setText("Typography / Fonts:");
        add(fontsLabel, "cell 0 16");

        //---- h00Label ----
        h00Label.setText("H00");
        h00Label.putClientProperty("FlatLaf.styleClass", "h00");
        add(h00Label, "cell 1 16 5 1");

        //---- h0Label ----
        h0Label.setText("H0");
        h0Label.putClientProperty("FlatLaf.styleClass", "h0");
        add(h0Label, "cell 1 16 5 1");

        //---- h1Label ----
        h1Label.setText("H1");
        h1Label.putClientProperty("FlatLaf.styleClass", "h1");
        add(h1Label, "cell 1 16 5 1");

        //---- h2Label ----
        h2Label.setText("H2");
        h2Label.putClientProperty("FlatLaf.styleClass", "h2");
        add(h2Label, "cell 1 16 5 1");

        //---- h3Label ----
        h3Label.setText("H3");
        h3Label.putClientProperty("FlatLaf.styleClass", "h3");
        add(h3Label, "cell 1 16 5 1");

        //---- h4Label ----
        h4Label.setText("H4");
        h4Label.putClientProperty("FlatLaf.styleClass", "h4");
        add(h4Label, "cell 1 16 5 1");

        //---- lightLabel ----
        lightLabel.setText("light");
        lightLabel.putClientProperty("FlatLaf.style", "font: 200% $light.font");
        add(lightLabel, "cell 1 16 5 1,gapx 30");

        //---- semiboldLabel ----
        semiboldLabel.setText("semibold");
        semiboldLabel.putClientProperty("FlatLaf.style", "font: 200% $semibold.font");
        add(semiboldLabel, "cell 1 16 5 1");

        //---- fontZoomLabel ----
        fontZoomLabel.setText("(200%)");
        fontZoomLabel.putClientProperty("FlatLaf.styleClass", "small");
        fontZoomLabel.setEnabled(false);
        add(fontZoomLabel, "cell 1 16 5 1");

        //---- largeLabel ----
        largeLabel.setText("large");
        largeLabel.putClientProperty("FlatLaf.styleClass", "large");
        add(largeLabel, "cell 1 17 5 1");

        //---- defaultLabel ----
        defaultLabel.setText("default");
        add(defaultLabel, "cell 1 17 5 1");

        //---- mediumLabel ----
        mediumLabel.setText("medium");
        mediumLabel.putClientProperty("FlatLaf.styleClass", "medium");
        add(mediumLabel, "cell 1 17 5 1");

        //---- smallLabel ----
        smallLabel.setText("small");
        smallLabel.putClientProperty("FlatLaf.styleClass", "small");
        add(smallLabel, "cell 1 17 5 1");

        //---- miniLabel ----
        miniLabel.setText("mini");
        miniLabel.putClientProperty("FlatLaf.styleClass", "mini");
        add(miniLabel, "cell 1 17 5 1");

        //---- monospacedLabel ----
        monospacedLabel.setText("monospaced");
        monospacedLabel.putClientProperty("FlatLaf.styleClass", "monospaced");
        add(monospacedLabel, "cell 1 17 5 1,gapx 30");

        //======== popupMenu1 ========
        {

            //---- cutMenuItem ----
            cutMenuItem.setText("Cut");
            cutMenuItem.setMnemonic('C');
            popupMenu1.add(cutMenuItem);

            //---- copyMenuItem ----
            copyMenuItem.setText("Copy");
            copyMenuItem.setMnemonic('O');
            popupMenu1.add(copyMenuItem);

            //---- pasteMenuItem ----
            pasteMenuItem.setText("Paste");
            pasteMenuItem.setMnemonic('P');
            popupMenu1.add(pasteMenuItem);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents

        cutMenuItem.addActionListener(new DefaultEditorKit.CutAction());
        copyMenuItem.addActionListener(new DefaultEditorKit.CopyAction());
        pasteMenuItem.addActionListener(new DefaultEditorKit.PasteAction());
    }

    private void confiugure() {

        // show reveal button for password field
        //   to enable this for all password fields use:
        //   UIManager.put( "PasswordField.showRevealButton", true );
        passwordField1.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");

        // add leading/trailing icons to text fields
        leadingIconTextField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search");
        leadingIconTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
        trailingIconTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSVGIcon("com/formdev/flatlaf/demo/icons/DataTables.svg"));
        iconsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("com/formdev/flatlaf/demo/icons/user.svg"));
        iconsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSVGIcon("com/formdev/flatlaf/demo/icons/bookmarkGroup.svg"));

        // search history button
        JButton searchHistoryButton = new JButton(new FlatSearchWithHistoryIcon(true));
        searchHistoryButton.setToolTipText("Search History");
        searchHistoryButton.addActionListener(e -> {
            JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.add("(empty)");
            popupMenu.show(searchHistoryButton, 0, searchHistoryButton.getHeight());
        });
        compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, searchHistoryButton);

        // match case button
        JToggleButton matchCaseButton = new JToggleButton(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/matchCase.svg"));
        matchCaseButton.setRolloverIcon(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/matchCaseHovered.svg"));
        matchCaseButton.setSelectedIcon(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/matchCaseSelected.svg"));
        matchCaseButton.setToolTipText("Match Case");

        // whole words button
        JToggleButton wordsButton = new JToggleButton(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/words.svg"));
        wordsButton.setRolloverIcon(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/wordsHovered.svg"));
        wordsButton.setSelectedIcon(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/wordsSelected.svg"));
        wordsButton.setToolTipText("Whole Words");

        // regex button
        JToggleButton regexButton = new JToggleButton(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/regex.svg"));
        regexButton.setRolloverIcon(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/regexHovered.svg"));
        regexButton.setSelectedIcon(new FlatSVGIcon("com/formdev/flatlaf/demo/icons/regexSelected.svg"));
        regexButton.setToolTipText("Regular Expression");

        // search toolbar
        JToolBar searchToolbar = new JToolBar();
        searchToolbar.add(matchCaseButton);
        searchToolbar.add(wordsButton);
        searchToolbar.addSeparator();
        searchToolbar.add(regexButton);
        compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, searchToolbar);

        // show clear button (if text field is not empty)
        compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        clearTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
    }

    // Use this to test the UI!
    public static void main(String... args) {
        new UI.TestWindow(JFrame::new, new BasicComponentsPanel2()).getFrame().setSize(new Dimension(800, 600));
    }
}


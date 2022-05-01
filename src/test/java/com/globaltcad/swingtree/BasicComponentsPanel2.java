package com.globaltcad.swingtree;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;

class BasicComponentsPanel2 extends JPanel
{
    private JPasswordField passwordField1;
    private JTextField leadingIconTextField;
    private JTextField trailingIconTextField;
    private JTextField iconsTextField;
    private JTextField compsTextField;
    private JTextField clearTextField;

    BasicComponentsPanel2() {
        FlatLightLaf.setup();
        initComponents();
        configure();
    }

    private void initComponents()
    {
        JComboBox<String> comboBox1 = new JComboBox<>();
        JSpinner spinner1 = new JSpinner();
        JTextField textField1 = new JTextField();
        JFormattedTextField formattedTextField1 = new JFormattedTextField();
        passwordField1 = new JPasswordField();
        leadingIconTextField = new JTextField();
        trailingIconTextField = new JTextField();
        iconsTextField = new JTextField();
        compsTextField = new JTextField();
        clearTextField = new JTextField();
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
                "Editable", "a", "bb", "ccc"
        }))))
        .add("cell 2 4, growx", UI.comboBox().isEditableIf(true).isEnabledIf(false).make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Disabled", "a", "bb", "ccc"
        }))))
        .add("cell 3 4, growx", UI.comboBox().make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Not editable", "a", "bb", "ccc"
        })) ))
        .add("cell 4 4, growx", UI.comboBox().isEnabledIf(false).make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Not editable disabled", "a", "bb", "ccc"
        })) ))
        .add("cell 5 4, growx, wmax 100", UI.comboBox().make( it -> it.setModel(new DefaultComboBoxModel<>(new String[]{
                "Wide popup if text is longer", "aa", "bbb", "cccc"
        })) ))
        .add("cell 0 5", UI.label("JSpinner:").make( it -> {it.setLabelFor(spinner1);it.setDisplayedMnemonic('S');}))
        .add("cell 1 5, growx", UI.of(spinner1))
        .add("cell 2 5, growx", UI.spinner().isEnabledIf(false))
        .add("cell 5 5, growx", UI.comboBox().isEditableIf(true).withProperty("JTextField.placeholderText", "Placeholder"))
        .add("cell 0 6", UI.label("JTextField:").make( it -> {it.setLabelFor(textField1);it.setDisplayedMnemonic('T');} ))
        .add("cell 1 6, growx", UI.of(textField1).withText("Editable").make( it -> it.setComponentPopupMenu(popupMenu1) ))
        .add("cell 2 6, growx", UI.textField("Disabled").isEnabledIf(false))
        .add("cell 3 6, growx", UI.textField("Not editable").isEditableIf(false))
        .add("cell 4 6, growx", UI.textField("Not editable disabled").isEnabledIf(false).isEditableIf(false))
        .add("cell 5 6, growx", UI.textField().withProperty("JTextField.placeholderText", "Placeholder"))
        .add("cell 0 7", UI.label("JFormattedTextField:").make( it -> {it.setLabelFor(formattedTextField1); it.setDisplayedMnemonic('O');} ))
        .add("cell 1 7, growx", UI.of(formattedTextField1).withText("Editable").make( it -> it.setComponentPopupMenu(popupMenu1) ))
        .add("cell 2 7, growx", UI.formattedTextField("Disabled").isEnabledIf(false))
        .add("cell 3 7, growx", UI.formattedTextField("Not editable").isEditableIf(false))
        .add("cell 4 7, growx", UI.formattedTextField("Not editable disabled").isEnabledIf(false).isEditableIf(false))
        .add("cell 5 7", UI.formattedTextField().withProperty("JTextField.placeholderText", "Placeholder"))
        .add("cell 0 8", UI.label("JPasswordField:"))
        .add("cell 1 8, growx", UI.passwordField("Editable"))
        .add("cell 2 8, growx", UI.passwordField("Disabled").isEnabledIf(false))
        .add("cell 3 8, growx", UI.passwordField("Not editable").isEditableIf(false))
        .add("cell 4 8, growx", UI.passwordField("Not editable disabled").isEnabledIf(false).isEditableIf(false))
        .add("cell 5 8, growx", UI.passwordField().withProperty("JTextField.placeholderText", "Placeholder"))
        .add("cell 0 9", UI.label("JTextArea:"))
        .add("cell 1 9, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(
                UI.textArea("Editable").make( area -> area.setRows(2) ).get(JTextArea.class)
            );
        }))
        .add("cell 2 9, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(
                UI.textArea("Disabled").isEnabledIf(false).make( area -> area.setRows(2) ).get(JTextArea.class)
            );
        }))
        .add("cell 3 9, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(
                UI.textArea("Not editable").isEditableIf(false).make( area -> area.setRows(2) ).get(JTextArea.class)
            );
        }))
        .add("cell 4 9, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(
                UI.textArea("Not editable").isEditableIf(false).isEditableIf(false).make( area -> area.setRows(2) ).get(JTextArea.class)
            );
        }))
        .add("cell 5 9, growx", UI.textArea("No scroll pane").make( it -> it.setRows(2) ))
        .add("cell 0 10", UI.label("JEditorPane"))
        .add("cell 1 10, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.editorPane().withText("Editable").get(JEditorPane.class
            ));
        }))
        .add("cell 2 10, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.editorPane().withText("Disabled").isEnabledIf(false).get(JEditorPane.class));
        }))
        .add("cell 3 10, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.editorPane().withText("Not Editable").isEditableIf(false).get(JEditorPane.class));
        }))
        .add("cell 4 10, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.editorPane().withText("Not Editable disabled").isEditableIf(false).isEnabledIf(false).get(JEditorPane.class));
        }))
        .add("cell 5 10, growx", UI.editorPane().withText("No scroll pane"))
        .add("cell 0 11", UI.label("JTextPane:"))
        .add("cell 1 11, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.textPane().withText("Editable").get(JTextPane.class));
        }))
        .add("cell 2 11, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.textPane().withText("Disabled").isEnabledIf(false).get(JTextPane.class));
        }))
        .add("cell 3 11, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.textPane().withText("Not editable").isEditableIf(false).get(JTextPane.class));
        }))
        .add("cell 4 11, growx", UI.scrollPane().make( it -> {
            it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            it.setViewportView(UI.textPane().withText("Not editable disabled").isEditableIf(false).isEnabledIf(false).get(JTextPane.class));
        }))
        .add("cell 5 11, growx", UI.textPane().withText("No scroll pane"))
        .add("cell 0 12", UI.label("Error hints:"))
        .add("cell 1 12, growx", UI.textField().withProperty("JComponent.outline", "error"))
        .add("cell 2 12", UI.comboBox().withProperty("JComponent.outline", "error").isEditableIf(false).make( it -> {
            it.setModel(new DefaultComboBoxModel<>(new String[]{"Editable"}));
        }))
        .add("cell 3 12, growx", UI.spinner().withProperty("JComponent.outline", "error"))
        .add("cell 0 13", UI.label("Warning hints:"))
        .add("cell 1 13, growx", UI.textField().withProperty("JComponent.outline", "warning"))
        .add("cell 2 13, growx", UI.comboBox().withProperty("JComponent.outline", "warning").make(
                it -> it.setModel(new DefaultComboBoxModel<>(new String[]{"Not editable"}))
        ))
        .add("cell 3 13, growx", UI.spinner().withProperty("JComponent.outline", "warning"))
        .add("cell 0 14", UI.label("Leading/trailing icons:"))
        .add("cell 1 14, growx", UI.of(leadingIconTextField))
        .add("cell 2 14, growx", UI.of(trailingIconTextField).withText("text"))
        .add("cell 3 14, growx", UI.of(iconsTextField).withText("text"))
        .add("cell 0 15, growx", UI.label("Leading/trailing comp.:"))
        .add("cell 1 15 2 1, growx", UI.of(compsTextField))
        .add("cell 3 15, growx", UI.of(clearTextField).withText("clear me"))
        .add("cell 0 16", UI.label("Typography / Fonts:"));

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

    private void configure() {

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


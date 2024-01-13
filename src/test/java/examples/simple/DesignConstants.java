package examples.simple;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class DesignConstants
{
    private final int labelMinWidth = 150;
    private final int labelMinHeight = 18;

    private final int rightSideMaxHeight = 27;
    private final int rightSideMaxWidth = 260; // 360


    public JLabel fitLeft(JLabel label) {
        label.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        label.setMinimumSize(new java.awt.Dimension(labelMinWidth, labelMinHeight));
        return label;
    }

    public JComponent fitLeft(JComponent component) {
        component.setMinimumSize(new java.awt.Dimension(labelMinWidth, labelMinHeight));
        return component;
    }

    public JComponent fitRight(JComponent onRightSide) {
        onRightSide.setMaximumSize(new java.awt.Dimension(rightSideMaxWidth, rightSideMaxHeight));
        onRightSide.setMinimumSize(new java.awt.Dimension(120, rightSideMaxHeight));
        onRightSide.setPreferredSize(new java.awt.Dimension(rightSideMaxWidth, rightSideMaxHeight));
        return onRightSide;
    }
}


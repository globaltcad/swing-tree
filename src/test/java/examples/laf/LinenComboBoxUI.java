package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 *  The {@link JComboBox} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  The outer surface mirrors {@link LinenTextFieldUI}: a rounded cream
 *  field with a taupe border that thickens to the accent colour on
 *  focus. The drop-down arrow is replaced with a small chevron drawn
 *  in {@link LinenPalette#ACCENT}, sitting flush against the right edge.
 *  <p>
 *  The popup window (rendered separately by Swing) inherits the panel
 *  and list defaults of the basic LAF; for full popup styling, an
 *  application can override {@code ComboBox.list*} keys in a
 *  {@link swingtree.style.StyleSheet}.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenComboBoxUI
        extends    BasicComboBoxUI
        implements SwingTreeStyledComponentUI<JComboBox<?>>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenComboBoxUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
        // A non-editable combo owns focus itself (BasicComboBoxUI repaints it),
        // but an editable combo's focus lives on its editor, so bridge that to
        // a repaint of the whole combo.
        JComboBox<?> combo = (JComboBox<?>) c;
        if (combo.getEditor() != null)
            LinenFocus.repaintOnFocus(combo, combo.getEditor().getEditorComponent());
    }

    @Override
    public void uninstallUI(JComponent c) {
        JComboBox<?> combo = (JComboBox<?>) c;
        if (combo.getEditor() != null)
            LinenFocus.uninstall(combo, combo.getEditor().getEditorComponent());
        super.uninstallUI(c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((java.awt.Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    protected JButton createArrowButton() { return new ChevronButton(); }

    @Override
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JComboBox<?>> style(ComponentStyleDelegate<JComboBox<?>> it) {
        JComboBox<?> cb = it.component();
        boolean enabled = cb.isEnabled();
        boolean focused = enabled && comboHasFocus(cb);

        Color  surface = enabled ? LinenPalette.SURFACE_FIELD : LinenPalette.SURFACE_DISABLED;
        Color  fg      = enabled ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;
        Color  border  = focused ? LinenPalette.ACCENT : LinenPalette.BORDER;
        double bw      = focused ? 2 : 1;
        double margin  = focused ? 0 : 1;

        return it
                .margin(margin)
                .padding(4, 8, 4, 4)
                .borderRadius(7)
                .borderWidth(bw)
                .borderColor(border)
                .backgroundColor(surface)
                .foregroundColor(fg);
    }

    /**
     *  Reads the right focus flag for the combo's outer border. A non-editable
     *  combo is itself the focus owner; an editable combo delegates focus to
     *  its editor component, so {@code cb.isFocusOwner()} would never be true.
     */
    private static boolean comboHasFocus(JComboBox<?> cb) {
        if (cb.isFocusOwner())
            return true;
        if (cb.isEditable() && cb.getEditor() != null) {
            java.awt.Component ed = cb.getEditor().getEditorComponent();
            return ed != null && ed.isFocusOwner();
        }
        return false;
    }

    /** A flat, transparent chevron button used in place of the basic arrow. */
    private static final class ChevronButton extends JButton {
        ChevronButton() {
            setBorder(null);
            setContentAreaFilled(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }
        @Override public Dimension getPreferredSize() {
            int s = UI.scale(20);
            return new Dimension(s, s);
        }
        @Override public Insets getInsets() { return new Insets(0, 0, 0, 0); }
        @Override public boolean isFocusTraversable() { return false; }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AbstractButton self = this;
                Color colour = self.getModel().isPressed() || self.getModel().isRollover()
                        ? LinenPalette.ACCENT
                        : LinenPalette.TEXT_MUTED;
                if (!self.isEnabled()) colour = LinenPalette.TEXT_DISABLED;
                g2.setColor(colour);
                int w = getWidth(), h = getHeight();
                float cx = w / 2f;
                float cy = h / 2f + UI.scale(1f);
                float a  = UI.scale(4f);
                Path2D.Float p = new Path2D.Float();
                p.moveTo(cx - a, cy - a * 0.5f);
                p.lineTo(cx,       cy + a * 0.5f);
                p.lineTo(cx + a, cy - a * 0.5f);
                g2.setStroke(new java.awt.BasicStroke(Math.max(1.4f, UI.scale(1.6f)),
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.draw(p);
            } finally {
                g2.dispose();
            }
        }
    }
}
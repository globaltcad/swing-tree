package examples.laf;

import swingtree.UI;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link Icon} instances installed into {@link javax.swing.UIManager} for the glyphs Swing
 *  draws through an icon instead of through a UI delegate: the check and radio marks, the tree's
 *  disclosure handles and the submenu arrow.
 *  <p>
 *  Each icon reads the symbol set and the palette when it paints and captures neither, so the
 *  icons installed under one preset go on working after the look and feel is re-initialised under
 *  another. Each is a {@link UIResource} so that the next look and feel replaces it.
 */
final class GlyphIcons
{
    private GlyphIcons() {}

    private static final Icon CHECK_BOX     = new GlyphIcon(Shape.CHECK);
    private static final Icon RADIO         = new GlyphIcon(Shape.RADIO);
    private static final Icon TREE_EXPANDED = new GlyphIcon(Shape.TREE_EXPANDED);
    private static final Icon TREE_COLLAPSED= new GlyphIcon(Shape.TREE_COLLAPSED);
    private static final Icon SUBMENU_ARROW = new GlyphIcon(Shape.SUBMENU_ARROW);

    /** @return the glyph in front of a check box and a check-box menu item. */
    static Icon checkBox() { return CHECK_BOX; }

    /** @return the glyph in front of a radio button and a radio menu item. */
    static Icon radio() { return RADIO; }

    /** @return the disclosure handle of a tree node whose children are showing. */
    static Icon treeExpanded() { return TREE_EXPANDED; }

    /** @return the disclosure handle of a tree node whose children are hidden. */
    static Icon treeCollapsed() { return TREE_COLLAPSED; }

    /** @return the arrow at the right edge of a menu entry that opens a submenu. */
    static Icon submenuArrow() { return SUBMENU_ARROW; }

    /** Which of the symbol set's glyph methods an icon stands for. */
    private enum Shape { CHECK, RADIO, TREE_EXPANDED, TREE_COLLAPSED, SUBMENU_ARROW }

    private static final class GlyphIcon implements Icon, UIResource
    {
        private final Shape _shape;

        GlyphIcon( Shape shape ) { _shape = shape; }

        @Override public int getIconWidth()  { return UI.scale(side()); }
        @Override public int getIconHeight() { return UI.scale(side()); }

        private int side() {
            Symbols symbols = SwingTreeLookAndFeel.symbols();
            return _shape == Shape.CHECK || _shape == Shape.RADIO
                    ? symbols.checkGlyphSize()
                    : symbols.arrowGlyphSize();
        }

        @Override
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Symbols                      symbols = SwingTreeLookAndFeel.symbols();
            SwingTreeLookAndFeel.Palette palette = SwingTreeLookAndFeel.palette();
            int     w        = getIconWidth();
            int     h        = getIconHeight();
            boolean enabled  = c == null || c.isEnabled();
            boolean focused  = c != null && c.hasFocus();
            boolean selected = false, rollover = false, pressed = false;
            if ( c instanceof AbstractButton ) {
                ButtonModel model = ((AbstractButton) c).getModel();
                selected = model.isSelected();
                rollover = model.isRollover();
                pressed  = model.isPressed() && model.isArmed();
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                switch ( _shape ) {
                    case CHECK:
                        symbols.paintCheckGlyph(g2, palette, x, y, w, h, enabled, focused, rollover, pressed, selected);
                        break;
                    case RADIO:
                        symbols.paintRadioGlyph(g2, palette, x, y, w, h, enabled, focused, rollover, pressed, selected);
                        break;
                    case TREE_EXPANDED:
                        symbols.paintDisclosure(g2, palette, x, y, w, h, true, enabled);
                        break;
                    case TREE_COLLAPSED:
                        symbols.paintDisclosure(g2, palette, x, y, w, h, false, enabled);
                        break;
                    case SUBMENU_ARROW:
                    default:
                        symbols.paintSubmenuArrow(g2, palette, x, y, w, h, enabled);
                        break;
                }
            } finally {
                g2.dispose();
            }
        }
    }
}

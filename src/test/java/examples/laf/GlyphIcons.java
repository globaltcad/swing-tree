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
    private static final Icon MENU_CHECK    = new GlyphIcon(Shape.MENU_CHECK);
    private static final Icon MENU_RADIO    = new GlyphIcon(Shape.MENU_RADIO);
    private static final Icon TREE_EXPANDED = new GlyphIcon(Shape.TREE_EXPANDED);
    private static final Icon TREE_COLLAPSED= new GlyphIcon(Shape.TREE_COLLAPSED);
    private static final Icon SUBMENU_ARROW = new GlyphIcon(Shape.SUBMENU_ARROW);
    private static final Icon TREE_LEAF     = new GlyphIcon(Shape.TREE_LEAF);
    private static final Icon TREE_CLOSED   = new GlyphIcon(Shape.TREE_CLOSED);
    private static final Icon TREE_OPEN     = new GlyphIcon(Shape.TREE_OPEN);

    /** @return the glyph in front of a check box and a check-box menu item. */
    static Icon checkBox() { return CHECK_BOX; }

    /** @return the glyph in front of a radio button and a radio menu item. */
    static Icon radio() { return RADIO; }

    /** @return the tick of a check box menu item */
    static Icon menuCheck() { return MENU_CHECK; }

    /** @return the mark of a radio button menu item */
    static Icon menuRadio() { return MENU_RADIO; }

    /** @return the disclosure handle of a tree node whose children are showing. */
    static Icon treeExpanded() { return TREE_EXPANDED; }

    /** @return the disclosure handle of a tree node whose children are hidden. */
    static Icon treeCollapsed() { return TREE_COLLAPSED; }

    /** @return the arrow at the right edge of a menu entry that opens a submenu. */
    static Icon submenuArrow() { return SUBMENU_ARROW; }

    /** @return the icon in front of a tree node that can have no children. */
    static Icon treeLeaf() { return TREE_LEAF; }

    /** @return the icon in front of a tree node whose children are hidden. */
    static Icon treeClosed() { return TREE_CLOSED; }

    /** @return the icon in front of a tree node whose children are showing. */
    static Icon treeOpen() { return TREE_OPEN; }

    /** Which of the symbol set's glyph methods an icon stands for. */
    private enum Shape {
        CHECK, RADIO, MENU_CHECK, MENU_RADIO, TREE_EXPANDED, TREE_COLLAPSED, SUBMENU_ARROW,
        TREE_LEAF, TREE_CLOSED, TREE_OPEN
    }

    private static final class GlyphIcon implements Icon, UIResource
    {
        private final Shape _shape;

        GlyphIcon( Shape shape ) { _shape = shape; }

        @Override public int getIconWidth()  { return UI.scale(side()); }
        @Override public int getIconHeight() { return UI.scale(side()); }

        private int side() {
            Symbols symbols = SwingTreeLookAndFeel.symbols();
            switch ( _shape ) {
                case CHECK:
                case RADIO:       return symbols.checkGlyphSize();
                case MENU_CHECK:
                case MENU_RADIO:  return symbols.menuMarkSize();
                case TREE_LEAF:
                case TREE_CLOSED:
                case TREE_OPEN:   return symbols.treeNodeGlyphSize();
                case TREE_EXPANDED:
                case TREE_COLLAPSED: return symbols.disclosureGlyphSize();
                default:          return symbols.arrowGlyphSize();
            }
        }

        @Override
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Symbols                      symbols = SwingTreeLookAndFeel.symbols();
            SwingTreeLookAndFeel.Palette palette = SwingTreeLookAndFeel.palette();
            int     w        = getIconWidth();
            int     h        = getIconHeight();
            boolean enabled  = c == null || c.isEnabled();
            boolean focused  = c != null && c.hasFocus();
            boolean selected = false, rollover = false, pressed = false, armed = false;
            if ( c instanceof AbstractButton ) {
                ButtonModel model = ((AbstractButton) c).getModel();
                selected = model.isSelected();
                rollover = model.isRollover();
                pressed  = model.isPressed() && model.isArmed();
                armed    = model.isArmed() || ( c instanceof javax.swing.JMenu && selected );
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
                    case MENU_CHECK:
                    case MENU_RADIO:
                        symbols.paintMenuMark(g2, palette, x, y, w, h, _shape == Shape.MENU_RADIO,
                                              enabled, focused, rollover, pressed, selected, armed);
                        break;
                    case TREE_EXPANDED:
                        symbols.paintDisclosure(g2, palette, x, y, w, h, true, enabled);
                        break;
                    case TREE_COLLAPSED:
                        symbols.paintDisclosure(g2, palette, x, y, w, h, false, enabled);
                        break;
                    case TREE_LEAF:
                        symbols.paintTreeNode(g2, palette, x, y, w, h, true, false, enabled);
                        break;
                    case TREE_CLOSED:
                        symbols.paintTreeNode(g2, palette, x, y, w, h, false, false, enabled);
                        break;
                    case TREE_OPEN:
                        symbols.paintTreeNode(g2, palette, x, y, w, h, false, true, enabled);
                        break;
                    case SUBMENU_ARROW:
                    default:
                        symbols.paintSubmenuArrow(g2, palette, x, y, w, h, enabled, armed);
                        break;
                }
            } finally {
                g2.dispose();
            }
        }
    }
}

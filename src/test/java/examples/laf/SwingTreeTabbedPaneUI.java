package examples.laf;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 *  The {@link JTabbedPane} UI delegate. The symbol set draws the tab surfaces and the accent on
 *  the selected one; this delegate decides the spacing and the label colours, replaces the box
 *  {@link BasicTabbedPaneUI} draws around the page with a single hairline, and asks the parent
 *  for the height of a tab strip that has wrapped into several runs.
 */
public final class SwingTreeTabbedPaneUI
        extends    BasicTabbedPaneUI
        implements SwingTreeStyledComponentUI<JTabbedPane>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTabbedPaneUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    // ── Insets and spacing ───────────────────────────────────────────────

    @Override
    protected Insets getTabInsets( int tabPlacement, int tabIndex ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.getTabInsets(tabPlacement, tabIndex);
        Symbols symbols = SwingTreeLookAndFeel.symbols();
        int     v       = UI.scale(symbols.tabPaddingVertical());
        int     h       = UI.scale(symbols.tabPaddingHorizontal());
        return new Insets(v, h, v, h);
    }

    @Override
    protected Insets getContentBorderInsets( int tabPlacement ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.getContentBorderInsets(tabPlacement);
        // The symbol set's edge, on whichever side of the page the tabs sit.
        int n = Math.max(1, UI.scale(SwingTreeLookAndFeel.symbols().tabEdgeThickness()));
        switch ( tabPlacement ) {
            case SwingConstants.LEFT:   return new Insets(0, n, 0, 0);
            case SwingConstants.RIGHT:  return new Insets(0, 0, 0, n);
            case SwingConstants.BOTTOM: return new Insets(0, 0, n, 0);
            case SwingConstants.TOP:
            default:                    return new Insets(n, 0, 0, 0);
        }
    }

    @Override
    protected Insets getTabAreaInsets( int tabPlacement ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.getTabAreaInsets(tabPlacement);
        int gap = UI.scale(SwingTreeLookAndFeel.symbols().tabAreaGap());
        return new Insets(gap, gap, 0, gap);
    }

    @Override
    protected int calculateTabAreaHeight( int tabPlacement, int horizRunCount, int maxTabHeight ) {
        int basic = super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight);
        return SwingTreeLookAndFeel.drawsOwnChrome() ? basic + UI.scale(2) : basic;
    }

    // ── Sizing ───────────────────────────────────────────────────────────

    @Override
    protected LayoutManager createLayoutManager() {
        if ( tabPane.getTabLayoutPolicy() != JTabbedPane.WRAP_TAB_LAYOUT )
            return super.createLayoutManager();
        return new StackAwareLayout();
    }

    /**
     *  Adds the height of the extra tab runs a narrow pane needs to the size the pane asks for.
     *  <p>
     *  {@link TabbedPaneLayout} measures the tab strip at the width the pane would like to have,
     *  so a pane given less than that wraps its tabs into further runs and never asks for the
     *  height those runs occupy. They are laid over the page instead, and the bottom of the page
     *  is pushed out of sight.
     */
    private final class StackAwareLayout extends TabbedPaneLayout
    {
        @Override
        protected Dimension calculateSize( boolean minimum ) {
            Dimension size      = super.calculateSize(minimum);
            int       placement = tabPane.getTabPlacement();
            if ( placement != SwingConstants.TOP && placement != SwingConstants.BOTTOM )
                return size;
            Insets insets       = tabPane.getInsets();
            Insets areaInsets   = getTabAreaInsets(placement);
            int    sideChrome   = insets.left + insets.right + areaInsets.left + areaInsets.right;
            int    grantedWidth = tabPane.getWidth() - sideChrome;
            if ( grantedWidth <= 0 )
                return size;
            int atGrantedWidth = preferredTabAreaHeight(placement, grantedWidth);
            int atAskedWidth   = preferredTabAreaHeight(placement, size.width - sideChrome);
            size.height += Math.max(0, atGrantedWidth - atAskedWidth);
            return size;
        }

        @Override
        public void layoutContainer( Container parent ) {
            int runsBefore = runCount;
            super.layoutContainer(parent);
            if ( runCount != runsBefore )
                SwingUtilities.invokeLater(tabPane::revalidate);
        }
    }

    // ── Tab painting ─────────────────────────────────────────────────────

    /**
     *  Repaints the tab the pointer left and the one it arrived at.
     *  <p>
     *  {@link BasicTabbedPaneUI} records which tab the pointer is over and asks for no repaint when
     *  that changes, so a symbol set's surface for a tab under the pointer is painted only when
     *  something else happens to repaint the strip. Every preset draws one; without this none of
     *  them were reached.
     */
    @Override
    protected void setRolloverTab( int index ) {
        int left = getRolloverTab();
        super.setRolloverTab(index);
        if ( left == index || tabPane == null || !SwingTreeLookAndFeel.drawsOwnChrome() )
            return;
        repaintTab(left);
        repaintTab(index);
    }

    private void repaintTab( int index ) {
        if ( index < 0 || index >= tabPane.getTabCount() )
            return;
        Rectangle bounds = getTabBounds(tabPane, index);
        if ( bounds == null )
            return;
        // A preset may lay a tab's contact shadow a pixel or two outside the rectangle the layout
        // gave it, and a repaint clipped to the rectangle would leave that behind.
        int bleed = UI.scale(2);
        tabPane.repaint(bounds.x - bleed, bounds.y - bleed,
                        bounds.width + bleed * 2, bounds.height + bleed * 2);
    }

    @Override
    protected void paintTabBackground(
        Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected
    ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) {
            super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
            return;
        }
        boolean    rollover = getRolloverTab() == tabIndex && tabPane.isEnabledAt(tabIndex);
        Graphics2D g2       = (Graphics2D) g.create();
        try {
            SwingTreeLookAndFeel.symbols().paintTabSurface(
                    g2, SwingTreeLookAndFeel.palette(), x, y, w, h, isSelected, rollover
            );
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabBorder(
        Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected
    ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) {
            super.paintTabBorder(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
            return;
        }
        if ( !isSelected )
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SwingTreeLookAndFeel.symbols().paintTabAccent(
                    g2, SwingTreeLookAndFeel.palette(), x, y, w, h,
                    tabPlacement, tabPane.isEnabledAt(tabIndex)
            );
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintText(
        Graphics g, int tabPlacement, Font font, FontMetrics metrics,
        int tabIndex, String title, Rectangle textRect, boolean isSelected
    ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) {
            super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
            return;
        }
        SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
        Color colour;
        if ( !tabPane.isEnabledAt(tabIndex) ) colour = p.textDisabled();
        else if ( isSelected )                colour = p.text();
        else                                  colour = p.textMuted();

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font);
            g2.setColor(colour);
            g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        } finally {
            g2.dispose();
        }
    }

    /** The accent on the selected tab already says which one is active. */
    @Override
    protected void paintFocusIndicator(
        Graphics g, int tabPlacement, Rectangle[] rects,
        int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected
    ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            super.paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, isSelected);
    }

    @Override
    protected void paintContentBorder( Graphics g, int tabPlacement, int selectedIndex ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) {
            super.paintContentBorder(g, tabPlacement, selectedIndex);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int n = Math.max(1, UI.scale(SwingTreeLookAndFeel.symbols().tabEdgeThickness()));
            int w = tabPane.getWidth(), h = tabPane.getHeight();
            int tabAreaH = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            int tabAreaW = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
            Rectangle edge;
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: edge = new Rectangle(0, h - tabAreaH - n, w, n); break;
                case SwingConstants.LEFT:   edge = new Rectangle(tabAreaW, 0, n, h);         break;
                case SwingConstants.RIGHT:  edge = new Rectangle(w - tabAreaW - n, 0, n, h); break;
                case SwingConstants.TOP:
                default:                    edge = new Rectangle(0, tabAreaH, w, n);         break;
            }
            SwingTreeLookAndFeel.symbols().paintTabEdge(
                    g2, SwingTreeLookAndFeel.palette(), edge, selectedTabBounds(selectedIndex), tabPlacement
            );
        } finally {
            g2.dispose();
        }
    }

    /** @return where the selected tab is, or {@code null} when the pane has no tabs, so that a
     *          symbol set may leave its edge open under that tab. */
    private @Nullable Rectangle selectedTabBounds( int selectedIndex ) {
        if ( selectedIndex < 0 || selectedIndex >= tabPane.getTabCount() )
            return null;
        return getTabBounds(tabPane, selectedIndex);
    }

    @Override
    public ComponentStyleDelegate<JTabbedPane> style( ComponentStyleDelegate<JTabbedPane> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

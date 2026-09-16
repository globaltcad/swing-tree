package examples.laf;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeTabbedPaneUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTabbedPaneUI(SwingTreeLookAndFeel.installedTheme()); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        _theme.installStyleOn(c);
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

    /** The key Nimbus files the room around a tab's label under, which a look and feel that lays
     *  its tabs out the Nimbus way puts into its defaults. */
    private static final String TAB_MARGINS = "TabbedPane:TabbedPaneTab.contentMargins";

    /** The same for the room around the whole row of tabs, whose bottom Nimbus fills with the edge
     *  along the page. */
    private static final String TAB_AREA_MARGINS = "TabbedPane:TabbedPaneTabArea.contentMargins";

    @Override
    protected Insets getTabInsets( int tabPlacement, int tabIndex ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return super.getTabInsets(tabPlacement, tabIndex);
        Insets margins = UIManager.getInsets(TAB_MARGINS);
        if ( margins != null )
            return new Insets(UI.scale(margins.top), UI.scale(margins.left), UI.scale(margins.bottom), UI.scale(margins.right));
        Symbols symbols = _theme.symbols();
        int     v       = UI.scale(symbols.tabPaddingVertical());
        int     h       = UI.scale(symbols.tabPaddingHorizontal());
        return new Insets(v, h, v, h);
    }

    @Override
    protected Insets getContentBorderInsets( int tabPlacement ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return super.getContentBorderInsets(tabPlacement);
        // The symbol set's edge, on whichever side of the page the tabs sit.
        int n = Math.max(1, UI.scale(_theme.symbols().tabEdgeThickness()));
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
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return super.getTabAreaInsets(tabPlacement);
        Insets margins = UIManager.getInsets(TAB_AREA_MARGINS);
        if ( margins != null ) {
            // The edge along the page is drawn inside these margins, reaching one row up under the
            // tabs, so the area itself stops that far short of them.
            int edge = _theme.symbols().tabEdgeThickness();
            return new Insets(UI.scale(margins.top), UI.scale(margins.left), UI.scale(margins.bottom - edge), UI.scale(margins.right));
        }
        int gap = UI.scale(_theme.symbols().tabAreaGap());
        return new Insets(gap, gap, 0, gap);
    }

    /** Swing adds three pixels to every tab beyond its label and its insets. A tab laid out to
     *  Nimbus's margins is exactly its label and its margins wide. */
    @Override
    protected int calculateTabWidth( int tabPlacement, int tabIndex, FontMetrics metrics ) {
        int basic = super.calculateTabWidth(tabPlacement, tabIndex, metrics);
        return _theme.symbols().drawsItsOwnChrome() && UIManager.getInsets(TAB_MARGINS) != null ? basic - 3 : basic;
    }

    @Override
    protected int calculateTabAreaHeight( int tabPlacement, int horizRunCount, int maxTabHeight ) {
        int basic = super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight);
        boolean laidOutByMargins = UIManager.getInsets(TAB_AREA_MARGINS) != null;
        return _theme.symbols().drawsItsOwnChrome() && !laidOutByMargins ? basic + UI.scale(2) : basic;
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
        if ( left == index || tabPane == null || !_theme.symbols().drawsItsOwnChrome() )
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
        if ( !_theme.symbols().drawsItsOwnChrome() ) {
            super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
            return;
        }
        boolean    rollover = getRolloverTab() == tabIndex && tabPane.isEnabledAt(tabIndex);
        Graphics2D g2       = (Graphics2D) g.create();
        try {
            _theme.symbols().paintTabSurface(
                    g2, _theme.palette(), x, y, w, h, isSelected, rollover
            );
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabBorder(
        Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected
    ) {
        if ( !_theme.symbols().drawsItsOwnChrome() ) {
            super.paintTabBorder(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
            return;
        }
        if ( !isSelected )
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            _theme.symbols().paintTabAccent(
                    g2, _theme.palette(), x, y, w, h,
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
        if ( !_theme.symbols().drawsItsOwnChrome() ) {
            super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
            return;
        }
        Color colour = _theme.symbols().tabText(
                            _theme.palette(), isSelected, tabPane.isEnabledAt(tabIndex)
                        );

        Graphics2D g2 = (Graphics2D) g.create();
        try {
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
        if ( !_theme.symbols().drawsItsOwnChrome() )
            super.paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, isSelected);
    }

    @Override
    protected void paintContentBorder( Graphics g, int tabPlacement, int selectedIndex ) {
        if ( !_theme.symbols().drawsItsOwnChrome() ) {
            super.paintContentBorder(g, tabPlacement, selectedIndex);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int n = Math.max(1, UI.scale(_theme.symbols().tabEdgeThickness()));
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
            _theme.symbols().paintTabEdge(
                    g2, _theme.palette(), edge, selectedTabBounds(selectedIndex), tabPlacement
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
        return _theme.applyStyle(it);
    }
}

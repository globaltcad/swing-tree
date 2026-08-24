package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 *  The {@link JTabbedPane} UI delegate. The tab surfaces and the accent that marks the selected
 *  one are drawn by the configured symbol set; this delegate decides the spacing, the label
 *  colours, and the single hairline that separates the strip of tabs from the page below it.
 *  The four-sided box the inherited delegate draws by default is deliberately suppressed - it
 *  clashes with a rounded selected tab.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeTabbedPaneUI
        extends    BasicTabbedPaneUI
        implements SwingTreeStyledComponentUI<JTabbedPane>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTabbedPaneUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LafPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    // ── Insets and spacing ───────────────────────────────────────────────

    @Override
    protected Insets getTabInsets( int tabPlacement, int tabIndex ) {
        Symbols symbols = SwingTreeLookAndFeel.symbols();
        int     v       = UI.scale(symbols.tabPaddingVertical());
        int     h       = UI.scale(symbols.tabPaddingHorizontal());
        return new Insets(v, h, v, h);
    }

    @Override
    protected Insets getContentBorderInsets( int tabPlacement ) {
        // A hairline on whichever side of the page the tabs sit.
        int n = UI.scale(1);
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
        int gap = UI.scale(SwingTreeLookAndFeel.symbols().tabAreaGap());
        return new Insets(gap, gap, 0, gap);
    }

    @Override
    protected int calculateTabAreaHeight( int tabPlacement, int horizRunCount, int maxTabHeight ) {
        return super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight) + UI.scale(2);
    }

    // ── Tab painting ─────────────────────────────────────────────────────

    @Override
    protected void paintTabBackground(
        Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected
    ) {
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
    ) { /* the accent indicates the active tab */ }

    @Override
    protected void paintContentBorder( Graphics g, int tabPlacement, int selectedIndex ) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int n = Math.max(1, UI.scale(1));
            g2.setColor(SwingTreeLookAndFeel.palette().borderSoft());
            int w = tabPane.getWidth(), h = tabPane.getHeight();
            int tabAreaH = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            int tabAreaW = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g2.fillRect(0, h - tabAreaH - n, w, n); break;
                case SwingConstants.LEFT:   g2.fillRect(tabAreaW, 0, n, h);         break;
                case SwingConstants.RIGHT:  g2.fillRect(w - tabAreaW - n, 0, n, h); break;
                case SwingConstants.TOP:
                default:                    g2.fillRect(0, tabAreaH, w, n);         break;
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public ComponentStyleDelegate<JTabbedPane> style( ComponentStyleDelegate<JTabbedPane> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

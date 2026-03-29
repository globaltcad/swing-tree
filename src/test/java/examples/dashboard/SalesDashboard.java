package examples.dashboard;

import sprouts.Var;
import swingtree.UI;
import swingtree.api.Layout;
import swingtree.layout.MigAddConstraint;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;

/**
 *  A sales performance dashboard that demonstrates SwingTree's <b>reactive layout</b> system.
 *
 *  <p>The entire content area is bound to a single {@code Var<Layout>} property.
 *  Clicking any "View" button calls {@link sprouts.Var#set} on that property,
 *  which instantly rearranges all six dashboard widgets — without recreating
 *  or rebuilding a single component.</p>
 *
 *  <p>Each layout preset specifies both the MigLayout wrap count and per-child
 *  add-constraints via {@link Layout#mig(String) Layout.mig(...).withChildConstraints(...)}.
 *  This means a single {@code Var.set()} call atomically changes how many columns
 *  the grid has AND how many columns each individual widget occupies.</p>
 *
 *  <p>Layout modes:</p>
 *  <ul>
 *    <li><b>Compact</b>  — single column, all six widgets stacked vertically</li>
 *    <li><b>Tablet</b>   — 2-column metrics grid; detail panels each span full width</li>
 *    <li><b>Analytics</b>— 3-column top row; last metric shares a row with Activity;
 *                          Pipeline spans the full width at the bottom</li>
 *    <li><b>Wide</b>     — all four metrics in one row; Activity and Pipeline side-by-side</li>
 *  </ul>
 *
 *  <p>Child order inside the reactive panel (indices 0–5):</p>
 *  <pre>
 *    [0] Revenue card (blue accent)
 *    [1] Active Deals card (green accent)
 *    [2] New Clients card (orange accent)
 *    [3] Win Rate card (purple accent)
 *    [4] Recent Activity feed (amber)
 *    [5] Pipeline Overview (teal)
 *  </pre>
 */
public final class SalesDashboard {

    // ── Page chrome colors ────────────────────────────────────────────────────

    private static final Color BG_PAGE    = new Color(241, 245, 249); // slate-100
    private static final Color BG_HEADER  = new Color( 30,  41,  59); // slate-800
    private static final Color BG_TOOLBAR = new Color(255, 255, 255);
    private static final Color BG_CARD    = new Color(255, 255, 255);

    // ── Accent colors — one per widget, chosen for immediate visual distinction ──

    private static final Color BLUE   = new Color( 59, 130, 246); // Revenue
    private static final Color GREEN  = new Color( 22, 163,  74); // Active Deals
    private static final Color ORANGE = new Color(234,  88,  12); // New Clients
    private static final Color PURPLE = new Color(147,  51, 234); // Win Rate
    private static final Color AMBER  = new Color(217, 119,   6); // Recent Activity header
    private static final Color TEAL   = new Color(  13, 148, 136); // Pipeline header

    // ── Soft panel backgrounds (pastel versions of the accent colors) ─────────

    private static final Color BG_ACTIVITY = new Color(255, 251, 235); // amber-50
    private static final Color BG_PIPELINE = new Color(240, 253, 250); // teal-50

    // ── Reactive layout presets ───────────────────────────────────────────────
    //
    //  Each preset bundles a MigLayout wrap-count with per-child add-constraints.
    //  Assigning any of these to the Var<Layout> property is all it takes to
    //  completely reflow the dashboard.
    //
    //  Child indices:
    //    0 = Revenue   1 = Deals   2 = Clients   3 = Win-Rate
    //    4 = Activity (wide)       5 = Pipeline  (wide)

    private static Layout compactLayout() {
        // Single column — widgets stacked vertically, each taking the full row width.
        return Layout.mig("fill, wrap 1, insets 8 8 8 8")
                .withChildConstraints(
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx")
                );
    }

    private static Layout tabletLayout() {
        // 2-column grid for metric cards; detail panels each span both columns.
        //
        //   Row 1: [ Revenue      ] [ Active Deals ]
        //   Row 2: [ New Clients  ] [ Win Rate     ]
        //   Row 3: [ Recent Activity  (span 2)     ]
        //   Row 4: [ Pipeline Overview (span 2)    ]
        return Layout.mig("fill, wrap 2, insets 8 8 8 8")
                .withChildConstraints(
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx, span 2"),  // Activity — full width
                    MigAddConstraint.of("growx, span 2")   // Pipeline — full width
                );
    }

    private static Layout analyticsLayout() {
        // 3-column layout: the last metric card shares a row with Activity;
        // Pipeline gets the full width at the bottom.
        //
        //   Row 1: [ Revenue    ] [ Active Deals ] [ New Clients      ]
        //   Row 2: [ Win Rate   ] [ Recent Activity (span 2)          ]
        //   Row 3: [ Pipeline Overview         (span 3)               ]
        return Layout.mig("fill, wrap 3, insets 8 8 8 8")
                .withChildConstraints(
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx, span 2"),  // Activity — beside Win Rate
                    MigAddConstraint.of("growx, span 3")   // Pipeline — full width
                );
    }

    private static Layout wideLayout() {
        // All four metrics in a single top row; Activity and Pipeline side-by-side below.
        //
        //   Row 1: [ Revenue ] [ Deals ] [ Clients ] [ Win Rate ]
        //   Row 2: [ Recent Activity (span 2) ][ Pipeline (span 2)]
        return Layout.mig("fill, wrap 4, insets 8 8 8 8")
                .withChildConstraints(
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx"),
                    MigAddConstraint.of("growx, span 2"),  // Activity — left half
                    MigAddConstraint.of("growx, span 2")   // Pipeline — right half
                );
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        /*
         * This is the only piece of state that drives the entire content layout.
         * Calling contentLayout.set(...) on any of the four presets above is
         * sufficient to reflow every widget in the dashboard instantly.
         */
        Var<Layout> contentLayout = Var.of(Layout.class, wideLayout());

        UI.show("Sales Dashboard — Reactive Layout Demo", frame ->
            UI.panel("fill, wrap 1, insets 0, gap 0")
            .withPrefSize(940, 700)
            .withStyle(it -> it.backgroundColor(BG_PAGE))

            // ─────────────────────────── Header ───────────────────────────────
            .add("growx, top",
                UI.panel("fillx, insets 14 22 14 22")
                .withStyle(it -> it.backgroundColor(BG_HEADER))
                .add(
                    UI.html("<html>"
                        + "<span style='color:white;font-size:15px;font-weight:bold'>"
                        + "Sales Performance Dashboard"
                        + "</span></html>")
                )
                .add("pushx", UI.box("ins 0"))
                .add(
                    UI.html("<html>"
                        + "<span style='color:#94a3b8;font-size:10px'>"
                        + "Q4 &middot; " + LocalDateTime.now().getYear()
                        + "</span></html>")
                )
            )

            // ─────────────────────── Layout mode toolbar ──────────────────────
            //  Each button calls Var.set() with a different Layout preset.
            //  That single call rewires both the MigLayout wrap count AND the
            //  per-child span constraints for every widget simultaneously.
            .add("growx",
                UI.panel("fillx, insets 7 16 7 16")
                .withStyle(it -> it
                    .backgroundColor(BG_TOOLBAR)
                    .borderAt(UI.Edge.BOTTOM, 1, new Color(226, 232, 240))
                )
                .add(
                    UI.html("<html><span style='color:#64748b;font-size:10px;font-weight:bold'>"
                        + "VIEW MODE&nbsp;</span></html>")
                )
                .add(
                    UI.button("Compact")
                    .withStyle(it -> it.borderRadius(10).padding(3, 6, 3, 6))
                    .onClick(e -> contentLayout.set(compactLayout()))
                )
                .add(
                    UI.button("Tablet")
                    .withStyle(it -> it.borderRadius(10).padding(3, 6, 3, 6))
                    .onClick(e -> contentLayout.set(tabletLayout()))
                )
                .add(
                    UI.button("Analytics")
                    .withStyle(it -> it.borderRadius(10).padding(3, 6, 3, 6))
                    .onClick(e -> contentLayout.set(analyticsLayout()))
                )
                .add(
                    UI.button("Wide")
                    .withStyle(it -> it.borderRadius(10).padding(3, 6, 3, 6))
                    .onClick(e -> contentLayout.set(wideLayout()))
                )
                .add("pushx", UI.box("ins 0"))
                .add(
                    UI.html("<html><span style='color:#94a3b8;font-size:10px;font-style:italic'>"
                        + "Click a button — watch the widgets rearrange!</span></html>")
                )
            )

            // ─────────────────────── Reactive content panel ───────────────────
            //
            //  UI.panel(contentLayout) binds the panel's LayoutManager directly
            //  to the Var<Layout> property. Whenever the property changes:
            //    1. The MigLayout's wrap count is updated in-place.
            //    2. Per-child add-constraints are pushed onto each child component.
            //    3. A revalidate/repaint is triggered automatically.
            //
            //  No component is ever recreated — only the layout configuration changes.
            .add("grow, push",
                UI.panel(contentLayout)
                .withStyle(it -> it.backgroundColor(BG_PAGE))
                // Widgets are added once with a base "growx" constraint.
                // The reactive layout overrides these with per-child constraints
                // from withChildConstraints(...) on each Var.set() call.
                .add("growx", revenueCard())
                .add("growx", dealsCard())
                .add("growx", clientsCard())
                .add("growx", winRateCard())
                .add("growx", activityPanel())
                .add("growx", pipelinePanel())
            )
            .get(JPanel.class)
        );
    }

    // ── Metric card factories ─────────────────────────────────────────────────

    private static JPanel revenueCard() {
        return metricCard("Revenue", "€128,400", "▲ 12.5% vs last quarter", BLUE);
    }

    private static JPanel dealsCard() {
        return metricCard("Active Deals", "24", "3 closing this week", GREEN);
    }

    private static JPanel clientsCard() {
        return metricCard("New Clients", "8", "▲ 3 since last month", ORANGE);
    }

    private static JPanel winRateCard() {
        return metricCard("Win Rate", "68 %", "Target: 70%", PURPLE);
    }

    /**
     *  A compact metric card with a coloured left accent bar, a numeric value
     *  rendered in the accent colour, and a small descriptive subtitle.
     *
     *  @param title   short label shown at the top in muted grey
     *  @param value   the headline figure shown prominently in the accent colour
     *  @param sub     one-line context (trend, target, note)
     *  @param accent  the accent colour — drives the left border and the value text
     */
    private static JPanel metricCard(String title, String value, String sub, Color accent) {
        String accentHex = String.format("#%02x%02x%02x",
                accent.getRed(), accent.getGreen(), accent.getBlue());
        return UI.panel("fill, wrap 1, insets 14 16 14 16")
            .withPrefHeight(116)
            .withStyle(it -> it
                .backgroundColor(BG_CARD)
                .borderRadius(10)
                .borderAt(UI.Edge.LEFT, 5, accent)
                .shadowBlurRadius(5)
                .shadowSpreadRadius(1)
                .shadowColor(new Color(0, 0, 0, 18))
                .shadowOffset(0, 2)
                .margin(5)
            )
            .add(UI.label(title)
                    .withForeground(new Color(100, 116, 139))
                    .withFontSize(10))
            .add(UI.html("<html><span style='font-size:24px;font-weight:bold;color:"
                    + accentHex + "'>" + value + "</span></html>"))
            .add(UI.label(sub)
                    .withForeground(new Color(148, 163, 184))
                    .withFontSize(10))
            .get(JPanel.class);
    }

    // ── Detail panel factories ────────────────────────────────────────────────

    /**
     *  Recent Activity feed — amber accent, soft yellow background, contains a
     *  scrollable list of recent CRM events. In Tablet and Compact modes this
     *  spans the full row width; in Wide mode it occupies the left half.
     */
    private static JPanel activityPanel() {
        return UI.panel("fill, wrap 1, insets 0")
            .withPrefHeight(200)
            .withStyle(it -> it
                .backgroundColor(BG_ACTIVITY)
                .borderRadius(10)
                .borderAt(UI.Edge.LEFT, 5, AMBER)
                .shadowBlurRadius(5)
                .shadowSpreadRadius(1)
                .shadowColor(new Color(0, 0, 0, 18))
                .shadowOffset(0, 2)
                .margin(5)
            )
            .add("growx",
                UI.panel("fillx, insets 10 16 10 16")
                .withStyle(it -> it
                    .backgroundColor(AMBER)
                    .borderRadius(0)
                )
                .add(UI.html("<html><b style='color:white;font-size:12px'>Recent Activity</b></html>"))
            )
            .add("growx",
                UI.panel("fillx, wrap 1, insets 8 16 8 16")
                .add(activityRow("Deal #2041 — Acme Corp reached proposal stage"))
                .add(activityRow("New contact: Sarah K. added to pipeline"))
                .add(activityRow("Follow-up email sent to TechVentures Inc."))
                .add(activityRow("Deal #2038 — GlobalTech Ltd marked as won  ✓"))
            )
            .get(JPanel.class);
    }

    private static JPanel activityRow(String text) {
        return UI.panel("fillx, insets 4 0 4 0")
            .withStyle(it -> it
                .borderAt(UI.Edge.BOTTOM, 1, new Color(254, 243, 199))
            )
            .add(UI.html("<html><span style='font-size:11px;color:#78716c'>&bull; " + text
                    + "</span></html>"))
            .get(JPanel.class);
    }

    /**
     *  Pipeline Overview — teal accent, soft teal background, shows deal counts
     *  per sales stage in a 2×2 grid. In Compact/Tablet modes this spans the full
     *  row width; in Wide mode it occupies the right half.
     */
    private static JPanel pipelinePanel() {
        return UI.panel("fill, wrap 1, insets 0")
            .withPrefHeight(200)
            .withStyle(it -> it
                .backgroundColor(BG_PIPELINE)
                .borderRadius(10)
                .borderAt(UI.Edge.LEFT, 5, TEAL)
                .shadowBlurRadius(5)
                .shadowSpreadRadius(1)
                .shadowColor(new Color(0, 0, 0, 18))
                .shadowOffset(0, 2)
                .margin(5)
            )
            .add("growx",
                UI.panel("fillx, insets 10 16 10 16")
                .withStyle(it -> it
                    .backgroundColor(TEAL)
                    .borderRadius(0)
                )
                .add(UI.html("<html><b style='color:white;font-size:12px'>Pipeline Overview</b></html>"))
            )
            .add("grow",
                UI.panel("fill, wrap 2, insets 10 16 10 16")
                .add("grow", pipelineStage("Prospecting", 12))
                .add("grow", pipelineStage("Proposal",     7))
                .add("grow", pipelineStage("Negotiation",  5))
                .add("grow", pipelineStage("Closing",       3))
            )
            .get(JPanel.class);
    }

    private static JPanel pipelineStage(String stage, int count) {
        String tealHex = String.format("#%02x%02x%02x",
                TEAL.getRed(), TEAL.getGreen(), TEAL.getBlue());
        return UI.panel("fill, wrap 1, insets 6 8 6 8")
            .withStyle(it -> it
                .backgroundColor(new Color(
                    TEAL.getRed(), TEAL.getGreen(), TEAL.getBlue(), 22))
                .borderRadius(6)
            )
            .add(UI.label(stage)
                    .withForeground(new Color(15, 118, 110))
                    .withFontSize(10))
            .add(UI.html("<html><span style='font-size:18px;font-weight:bold;color:"
                    + tealHex + "'>" + count + " deals</span></html>"))
            .get(JPanel.class);
    }
}
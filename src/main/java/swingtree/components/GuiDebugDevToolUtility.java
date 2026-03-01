package swingtree.components;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.input.Keyboard;
import swingtree.layout.Bounds;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

final class GuiDebugDevToolUtility {

    private static final Logger log = LoggerFactory.getLogger(GuiDebugDevToolUtility.class);
    private static final String SWING_TREE_DEV_TOOLS_SHORTCUT_ACTION_KEY = "SwingTreeDevToolsShortcut";
    private static final UI.Color FOCUS_COLOR = UI.Color.ofRgb(0, 90, 200);
    private static final UI.Color SELECTION_COLOR = UI.Color.ofRgb(0, 142, 83);
    private static final javax.swing.Action toggleDevToolsShortcutAction = new AbstractAction() {
        @Override public void actionPerformed(ActionEvent e) {
            SwingTree.get().setDevToolEnabled(
                !SwingTree.get().isDevToolEnabled()
            );
        }
    };
    private static @Nullable Component focusedDebugComponent = null;
    private static @Nullable Component selectedDebugComponent = null;
    private static @Nullable DebugInfoDialog debugInfoDialog = null;


    private GuiDebugDevToolUtility() {} // A utility class can not be instantiated!

    static void setupGlobalDevToolsShortcutFor(JRootPane rootPane) {
        String keyStrokeString = SwingTree.get().getDevToolKeyStrokeShortcut();
        if ( !keyStrokeString.isEmpty() ) {
            try {
                KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeString);
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, SWING_TREE_DEV_TOOLS_SHORTCUT_ACTION_KEY);
                rootPane.getActionMap().put(SWING_TREE_DEV_TOOLS_SHORTCUT_ACTION_KEY, toggleDevToolsShortcutAction);
            } catch (Exception e) {
                log.error("Error while setting up dev tools shortcut with key stroke '{}': {}", keyStrokeString, e.getMessage(), e);
            }
        }
    }

    static void teardownGlobalDevToolsShortcutFor(JRootPane rootPane) {
        String keyStrokeString = SwingTree.get().getDevToolKeyStrokeShortcut();
        if ( !keyStrokeString.isEmpty() ) {
            try {
                KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeString);
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(keyStroke);
                rootPane.getActionMap().remove(SWING_TREE_DEV_TOOLS_SHORTCUT_ACTION_KEY);
            } catch (Exception e) {
                log.error("Error while tearing down dev tools shortcut with key stroke '{}': {}", keyStrokeString, e.getMessage(), e);
            }
        }
    }

    static void initializeDebugToolFor(JRootPane rootPane) {
        if ( !SwingTree.get().isDevToolEnabled() ) {
            return;
        }
        Component any = rootPane.getContentPane();
        checkDebugging(any);
        summonInfoDialog(rootPane);
    }

    static void processMouseMovementForLiveDebugging(JGlassPane glassPane, JRootPane rootPane, MouseEvent e) {
        if ( !SwingTree.get().isDevToolEnabled() ) {
            return;
        }
        Component deepest = findComponentForLiveDebugging(glassPane, rootPane, e);
        if ( checkDebugging(deepest) ) {
            rootPane.repaint();
        }
    }

    private static java.awt.@Nullable Component findComponentForLiveDebugging(
            JGlassPane glassPane,
            JRootPane rootPane,
            MouseEvent e
    ) {
        Container content = rootPane.getContentPane();

        // Convert glass pane coordinates → content pane coordinates
        Point contentPoint = SwingUtilities.convertPoint(
                glassPane,
                e.getPoint(),
                content
        );

        if (contentPoint.x < 0 || contentPoint.y < 0)
            return null;

        return SwingUtilities.getDeepestComponentAt(
                        content,
                        contentPoint.x,
                        contentPoint.y
                );
    }

    private static boolean checkDebugging(java.awt.@Nullable Component deepest) {
        if (deepest != null && GuiDebugDevToolUtility.focusedDebugComponent != deepest) {
            GuiDebugDevToolUtility.focusedDebugComponent = deepest;
            // The debug component is being highlighted similarly as in inspector mode on a browser...
            if ( GuiDebugDevToolUtility.debugInfoDialog != null ) {
                GuiDebugDevToolUtility.debugInfoDialog.debugState.set(new ComponentDebugInfo(deepest));
            }
            return true;
        }
        return false;
    }

    static void findAndSelectComponentForDebug(JRootPane rootPane, MouseEvent e) {
        if ( !SwingTree.get().isDevToolEnabled() ) {
            return;
        }
        if ( GuiDebugDevToolUtility.focusedDebugComponent == null ) {
            return;
        }
        if (!Keyboard.get().isPressed(Keyboard.Key.CONTROL)) {
            return;
        }
        e.consume();// No other things should react to this event, since we are opening the debug dialog!

        summonInfoDialog(rootPane);
    }

    private static void summonInfoDialog(JRootPane rootPane) {
        if ( GuiDebugDevToolUtility.focusedDebugComponent == null ) {
            return;
        }
        if ( selectedDebugComponent != focusedDebugComponent )
            rootPane.repaint(); // Repaint to clear previous selection highlight

        // Select the currently focused debug component:
        selectedDebugComponent = focusedDebugComponent;

        if ( GuiDebugDevToolUtility.debugInfoDialog == null ) {
            DebugInfoDialog newDialog = new DebugInfoDialog(
                    Var.of(new ComponentDebugInfo(GuiDebugDevToolUtility.focusedDebugComponent)),
                    Var.of(new ComponentDebugInfo(GuiDebugDevToolUtility.selectedDebugComponent))
            );
            newDialog.pack();
            newDialog.setVisible(true);
            newDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    GuiDebugDevToolUtility.debugInfoDialog = null;
                    newDialog.dispose();
                }
            });
            GuiDebugDevToolUtility.debugInfoDialog = newDialog;
        } else {
            GuiDebugDevToolUtility.debugInfoDialog.selectedDebugState.set(new ComponentDebugInfo(GuiDebugDevToolUtility.selectedDebugComponent));
        }
    }

    static void paintDebugOverlay(Graphics2D g2d, JGlassPane glassPane) {
        if (!SwingTree.get().isDevToolEnabled() || focusedDebugComponent == null)
            return;
        if (!focusedDebugComponent.isShowing())
            return;

        // Setup font:
        {
            Font font = g2d.getFont();
            if ( font != null ) {
                g2d.setFont(
                    font.deriveFont(UI.scale(12f)) // size!
                );
            }
        }

        // Debug information should also be allowed to look good! :)
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        if ( selectedDebugComponent != focusedDebugComponent && focusedDebugComponent.isShowing() ) {
            renderDebugOverlayFor((Graphics2D) g2d.create(), glassPane, focusedDebugComponent, FOCUS_COLOR);
        }
        if ( selectedDebugComponent != null && selectedDebugComponent.isShowing() ) {
            renderDebugOverlayFor((Graphics2D) g2d.create(), glassPane, selectedDebugComponent, SELECTION_COLOR);
        }
    }

    private static void renderDebugOverlayFor(
            Graphics2D g2d,
            JGlassPane glassPane,
            java.awt.Component toBeDebugged,
            Color themeColor
    ) {
        Insets insets = new Insets(0,0,0,0);
        if ( toBeDebugged instanceof JComponent ) {
            insets = ((JComponent)toBeDebugged).getInsets(insets);
        }

        final int entireWidth = glassPane.getWidth();
        final int entireHeight = glassPane.getHeight();


        // Convert component bounds → glass pane coordinate space
        final Rectangle bounds = SwingUtilities.convertRectangle(
                toBeDebugged.getParent(),
                toBeDebugged.getBounds(),
                glassPane
        );
        final Rectangle contentBounds = new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom
        );

        // Use alpha composite for transparency
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));

        // Fill overlay
        Color brighter = themeColor.brighter();
        g2d.setColor(new Color(brighter.getRed(), brighter.getGreen(), brighter.getBlue(), 150));
        g2d.fill(bounds);

        // Restore full opacity for border
        g2d.setComposite(oldComposite);

        g2d.setXORMode(Color.WHITE); // To ensure readability over all GUIs

        int strokeWidth = UI.scale(2);
        g2d.setStroke(new BasicStroke(strokeWidth));
        g2d.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 200));
        g2d.draw(bounds);

        g2d.setStroke(new BasicStroke(UI.scale(1), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.drawLine(
                contentBounds.x, 0,
                contentBounds.x, entireHeight
        );
        g2d.drawLine(
                0, contentBounds.y,
                entireWidth, contentBounds.y
        );
        g2d.drawLine(
                contentBounds.x + contentBounds.width, 0,
                contentBounds.x + contentBounds.width, entireHeight
        );
        g2d.drawLine(
                0, contentBounds.y + contentBounds.height,
                entireWidth, contentBounds.y + contentBounds.height
        );

        g2d.setPaintMode(); // Reset XOR mode!!

        // Optional: component info label
        String label = getClassNameWithoutPackage(toBeDebugged.getClass())
                + "  " + bounds.width + "x" + bounds.height;

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getHeight();

        int labelX = 1 + ((bounds.x+textWidth) >= entireWidth ? entireWidth-textWidth-UI.scale(6) : bounds.x);
        int labelY = -1 + (bounds.y - textHeight >= 0 ? bounds.y - 2 : bounds.y + textHeight + 2);

        int labelPadding = strokeWidth / 2;
        Rectangle labelBanner = new Rectangle(
                labelX - labelPadding,
                labelY - textHeight - labelPadding,
                textWidth + UI.scale(6) + labelPadding * 2,
                textHeight + labelPadding * 2
        );
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(lerp(themeColor, Color.BLACK, 0.5));
        g2d.fillRoundRect(labelBanner.x+1, labelBanner.y+1, labelBanner.width, labelBanner.height, 9, 9);
        g2d.setColor(lerp(themeColor, Color.WHITE, 0.5));
        g2d.fillRoundRect(labelBanner.x-1, labelBanner.y-1, labelBanner.width, labelBanner.height, 9, 9);
        g2d.setColor(themeColor);
        g2d.fillRoundRect(labelBanner.x, labelBanner.y, labelBanner.width, labelBanner.height, 8, 8);

        g2d.setColor(Color.WHITE);
        g2d.drawString(label, labelX + 3, labelY - UI.scale(4));
    }

    private static Color lerp(Color a, Color b, double t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bCol = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int aCol = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bCol, aCol);
    }

    private static String stackTraceToString(Tuple<StackTraceElement> stackTraceElements) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement traceElement : stackTraceElements)
            builder = builder.append("\tat ").append(traceElement).append("\n");
        return builder.toString();
    }

    private static String getClassNameWithoutPackage(Class<?> type) {
        String name = type.getName();          // e.g. com.example.Outer$Inner
        int lastDot = name.lastIndexOf('.');
        String rawName = lastDot >= 0 ? name.substring(lastDot + 1) : name;
        return rawName.replace("$", ".");
    }

    private static class DebugInfoDialog extends JDialog {
        private final Viewable<Boolean> isDevToolsEnabled = SwingTree.get().isDevToolEnabledView();
        private final Var<ComponentDebugInfo> debugState;
        private final Var<ComponentDebugInfo> selectedDebugState;

        DebugInfoDialog(Var<ComponentDebugInfo> debugState, Var<ComponentDebugInfo> selectedDebugState) {
            this.debugState = debugState;
            this.selectedDebugState = selectedDebugState;
            this.isDevToolsEnabled.onChange(From.ALL, it -> {
                if ( !it.currentValue().orElse(false) ) {
                    this.dispose();
                    GuiDebugDevToolUtility.debugInfoDialog = null;
                }
            });
            setTitle(getClassNameWithoutPackage(debugState.get().type()));
            this.add(
                    UI.splitPane(UI.Align.HORIZONTAL)
                    .add(
                        buildInfoDisplay(debugState, FOCUS_COLOR)
                    )
                    .add(
                        buildInfoDisplay(selectedDebugState, SELECTION_COLOR)
                    )
                    .get(JSplitPane.class)
            );
        }

        private static JPanel buildInfoDisplay(Var<ComponentDebugInfo> debugState, UI.Color themeColor) {
            return
                UI.panel("fill, wrap 1").withPrefSize(650, 375)
                .withStyle( it -> it
                    .gradient( g -> g
                        .type(UI.GradientType.RADIAL)
                        .offset(-100, 30)
                        .span(UI.Span.TOP_RIGHT_TO_BOTTOM_LEFT)
                        .colors(
                            themeColor.brighterBy(0.70).desaturateBy(0.8),
                            themeColor.brighterBy(0.50).desaturateBy(0.7),
                            themeColor
                        )
                    )
                )
                .add("growx, pushx",
                    UI.box("fill, gap rel 3", "[shrink][shrink][grow]")
                    .add("left",
                        UI.label(
                            debugState.viewAsString(it ->
                                getClassNameWithoutPackage(it.type())
                            )
                        )
                        .withStyle( it -> it
                            .borderRadius(8)
                            .padding(4, 8, 4, 8)
                            .backgroundColor(themeColor)
                            .fontColor(UI.Color.WHITE)
                            .fontWeight(2)
                        )
                    )
                    .add("left",
                        UI.label(
                            debugState.viewAsString(it ->
                                (it.id().isEmpty() ? "" : "id='"+it.id()+"'")
                            )
                        )
                    )
                    .add("right",
                        UI.label(
                            debugState.viewAs(Bounds.class, ComponentDebugInfo::bounds)
                            .viewAsString(it ->
                                    "x="+((int)it.location().x())+", " +
                                    "y="+((int)it.location().y())+", " +
                                    "width="+it.size().width().map(Number::intValue).orElse(-1)+", " +
                                    "height="+it.size().height().map(Number::intValue).orElse(-1
                            )
                        ))
                    )
                )
                .add("push, grow",
                    UI.tabbedPane()
                    .withStyle( it -> it
                        .borderRadiusAt(UI.Corner.TOP_LEFT, 24)
                        .borderRadiusAt(UI.Corner.TOP_RIGHT, 24)
                    )
                    .add(
                        UI.tab("Source Trace")
                        .add(
                            UI.scrollPane().add(
                                UI.textArea(
                                    debugState.viewAsString(it -> it.type().getCanonicalName() +"\n"+ stackTraceToString(it.sourceCodeLocation()))
                                )
                                .isEditableIf(false)
                            )
                        )
                    )
                    .add(
                        UI.tab("Style")
                        .add(
                            UI.scrollPane().add(
                                UI.textArea(
                                    debugState.viewAsString(it -> {
                                        try {
                                            return prettyRecord(it.styleConf().toString());
                                        } catch (Exception e) {
                                            log.error("Error while pretty-printing style conf: {}", e.getMessage(), e);
                                            return it.styleConf().toString();
                                        }
                                    })
                                )
                                .isEditableIf(false)
                            )
                        )
                    )
                    .add(
                        UI.tab("Layout")
                        .add(
                            UI.scrollPane().add(
                                UI.textArea(
                                    debugState.viewAsString(ComponentDebugInfo::layoutInformation)
                                )
                                .isEditableIf(false)
                            )
                        )
                    )
                    .add(
                        UI.tab("toString")
                        .add(
                            UI.scrollPane().add(
                                UI.textArea(
                                    debugState.viewAsString(it -> {
                                        try {
                                            return prettyRecord(it.asString());
                                        } catch (Exception e) {
                                            log.error("Error while pretty-printing style conf: {}", e.getMessage(), e);
                                            return it.asString();
                                        }
                                    })
                                )
                                .isEditableIf(false)
                            )
                        )
                    )
                )
                .get(JPanel.class);
        }
    }

    private static String prettyRecord(String input) {
        if (input.isEmpty())
            return input;

        StringBuilder out = new StringBuilder(input.length() + 128);
        int depth = 0;
        boolean newLine = false;

        java.util.regex.Pattern colorPattern = java.util.regex.Pattern.compile("rgb[a]?\\([^)]*\\)");
        for (int i = 0; i < input.length(); i++) {
            try {
                i = maybeSkip(i, input, "[]", out);
                i = maybeSkip(i, input, "[NONE]", out);
                i = maybeSkip(i, input, "[EMPTY]", out);
                i = maybeSkipRegex(i, input, colorPattern, out);
            } catch (Exception e) {
                log.error("Error while pretty-printing record: {}", e.getMessage(), e);
            }
            char c = input.charAt(i);

            switch (c) {
                case '[' : {
                    out.append(c);
                    depth++;
                    out.append('\n');
                    indent(out, depth);
                    newLine = true;
                    break;
                }
                case ']' : {
                    depth--;
                    out.append('\n');
                    indent(out, depth);
                    out.append(c);
                    newLine = false;
                    break;
                }
                case ',' : {
                    out.append(c);
                    out.append('\n');
                    indent(out, depth);
                    newLine = true;
                    break;
                }
                default : {
                    if (newLine && Character.isWhitespace(c)) {
                        // skip whitespace immediately after newline
                        break;
                    }
                    out.append(c);
                    newLine = false;
                    break;
                }
            }
        }
        return out.toString();
    }

    private static int maybeSkip(int i, String input, String toMatch, StringBuilder out) {
        if ( (i+toMatch.length()) <= input.length() ) {
            // Special handling for 'toMatch' to keep it compact
            if ( input.startsWith(toMatch, i) ) {
                out.append(toMatch);
                return i + toMatch.length();
            }
        }
        return i;
    }

    private static int maybeSkipRegex(int i, String input, java.util.regex.Pattern pattern, StringBuilder out) {
        java.util.regex.Matcher matcher = pattern.matcher(input);
        matcher.region(i, input.length());
        if (matcher.lookingAt()) {
            String match = matcher.group();
            out.append(match);
            return i + match.length();
        }
        return i;
    }

    private static void indent(StringBuilder sb, int depth) {
        for (int i = 0; i < Math.max(0, depth); i++) {
            sb.append("  ");
        }
    }
}

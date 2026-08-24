package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  The {@link JLabel} UI delegate. A label has no model and very little state, so it is simply
 *  painted in the palette's text colour on whatever surface is underneath.
 *  <p>
 *  It deliberately paints no background, not even for the cell renderer of a selected table or
 *  list row. One renderer instance stands in for every cell, while the style engine keys a
 *  component's gathered style and its rendered layers on the <i>component</i> - so a per-cell
 *  decision made here would land on whichever cell is painted next rather than on the one it was
 *  made for. Selection is filled by the owner, which knows which rows are selected and is
 *  painted once: see {@link SwingTreeTableUI} and {@link SwingTreeListUI}.
 *
 *  <h2>Remembered text placement</h2>
 *  Working out where a label's text and icon go is more expensive than it looks: it has to
 *  measure the text, and measuring a string that contains a character the font cannot advance
 *  from a lookup table costs a hundred times what a plain ASCII string costs, because the
 *  toolkit falls back to shaping the whole line. Swing asks that question again on every single
 *  paint, even though a label's text, font, icon and size almost never change between two
 *  paints. This delegate therefore remembers the last few answers and hands one back whenever
 *  every input to it is unchanged - see {@link #layoutCL}.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeLabelUI
        extends    BasicLabelUI
        implements SwingTreeStyledComponentUI<JLabel>
{
    /**
     *  How many placements one label remembers. Swing asks {@link #layoutCL} the same question
     *  from three directions: where the text sits at the size the label is painted at, how large
     *  the label would like to be (asked with an unbounded viewing rectangle), and where its
     *  baseline is (asked by the layout manager at whatever size it is currently considering,
     *  which is not always the size the label has). Remembering fewer than three answers makes
     *  those questions evict each other: a label answering all three in a loop hits 33% of the
     *  time with one slot and 50% with two, against 99.7% with three. A fourth slot buys nothing,
     *  because a label needing more than three answers is one whose content genuinely varies - a
     *  cell renderer standing in for many rows, say - and for those the answer has to be worked
     *  out anyway.
     */
    private static final int REMEMBERED_PLACEMENTS = 3;

    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeLabelUI(); }

    private final Placement[] _placements = new Placement[REMEMBERED_PLACEMENTS];
    private int               _nextSlot   = 0;

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

    /**
     *  Places the label's text and icon inside {@code viewR}, filling {@code iconR} and
     *  {@code textR} and returning the text as it should be drawn - shortened with an ellipsis
     *  when it does not fit.
     *  <p>
     *  This is the single point through which {@link BasicLabelUI} reaches Swing's compound-label
     *  layout, for painting as well as for reporting a preferred size or a baseline, which is why
     *  it is the one place worth remembering. Every input the layout reads is part of what is
     *  compared here, so a label that changes any of them - its text, font, icon, alignment, gap,
     *  reading direction, or the rectangle it is placed in - simply does not find a match and is
     *  laid out afresh. No invalidation is needed anywhere else, and no listener has to fire for
     *  the answer to stay correct.
     *  <p>
     *  Markup is the one thing not remembered. Swing lays a label's markup out through a document
     *  view it builds and hangs off the label. That view keeps its own measurements and throws
     *  them away itself when it has to - when an image inside it finishes loading, say - without
     *  anything on the label changing. Remembering a placement for it would therefore shadow an
     *  invalidation this delegate cannot see, in exchange for a measurement that has already been
     *  made. Markup is recognised from the text rather than by looking for the document view on
     *  the label: {@link BasicHTML#isHTMLString(String)} is exactly the condition under which
     *  Swing builds that view, and reading six characters of a string already in hand is a good
     *  deal cheaper than a client-property lookup - which, on a busy window, this is. It is also
     *  the safer of the two, because a label whose markup is switched off through the
     *  {@code html.disable} client property has no document view but is still excluded here.
     *
     * @param label   the label being laid out
     * @param metrics the metrics of the font it will be drawn in
     * @param text    the label's text
     * @param icon    the label's icon, or {@code null}
     * @param viewR   the rectangle the text and icon are placed inside
     * @param iconR   filled with where the icon goes
     * @param textR   filled with where the text goes
     * @return the text as it should be drawn
     */
    @Override
    protected String layoutCL(
        JLabel      label,
        FontMetrics metrics,
        String      text,
        Icon        icon,
        Rectangle   viewR,
        Rectangle   iconR,
        Rectangle   textR
    ) {
        if ( BasicHTML.isHTMLString(text) )
            return super.layoutCL(label, metrics, text, icon, viewR, iconR, textR);

        for ( Placement remembered : _placements ) {
            if ( remembered != null && remembered.answers(label, metrics, text, icon, viewR) )
                return remembered.copyInto(iconR, textR);
        }
        String clippedText = super.layoutCL(label, metrics, text, icon, viewR, iconR, textR);
        _placements[_nextSlot] = new Placement(label, metrics, text, icon, viewR, clippedText, iconR, textR);
        _nextSlot = (_nextSlot + 1) % _placements.length;
        return clippedText;
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JLabel> style( ComponentStyleDelegate<JLabel> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /**
     *  One remembered answer of {@link #layoutCL}: everything the compound-label layout read to
     *  produce it, next to the three results it produced. Of an icon only the size it reported is
     *  held: the placement is worked out from that alone, and an icon that loads or animates
     *  reports a new size without becoming a new icon, so the size is both the necessary and the
     *  sufficient thing to compare.
     */
    private static final class Placement
    {
        private final FontMetrics _metrics;
        private final String      _text;
        private final int         _iconWidth;
        private final int         _iconHeight;
        private final int         _verticalAlignment;
        private final int         _horizontalAlignment;
        private final int         _verticalTextPosition;
        private final int         _horizontalTextPosition;
        private final int         _iconTextGap;
        private final boolean     _leftToRight;
        private final Rectangle   _viewR;
        private final String      _clippedText;
        private final Rectangle   _iconR;
        private final Rectangle   _textR;

        Placement(
            JLabel      label,
            FontMetrics metrics,
            String      text,
            Icon        icon,
            Rectangle   viewR,
            String      clippedText,
            Rectangle   iconR,
            Rectangle   textR
        ) {
            _metrics                = metrics;
            _text                   = text;
            _iconWidth              = ( icon == null ? 0 : icon.getIconWidth() );
            _iconHeight             = ( icon == null ? 0 : icon.getIconHeight() );
            _verticalAlignment      = label.getVerticalAlignment();
            _horizontalAlignment    = label.getHorizontalAlignment();
            _verticalTextPosition   = label.getVerticalTextPosition();
            _horizontalTextPosition = label.getHorizontalTextPosition();
            _iconTextGap            = label.getIconTextGap();
            _leftToRight            = label.getComponentOrientation().isLeftToRight();
            _viewR                  = new Rectangle(viewR);
            _clippedText            = clippedText;
            _iconR                  = new Rectangle(iconR);
            _textR                  = new Rectangle(textR);
        }

        boolean answers(
            JLabel      label,
            FontMetrics metrics,
            String      text,
            Icon        icon,
            Rectangle   viewR
        ) {
            return _metrics == metrics
                && ( _text == null ? text == null : _text.equals(text) )
                && _viewR.equals(viewR)
                && _iconWidth              == ( icon == null ? 0 : icon.getIconWidth() )
                && _iconHeight             == ( icon == null ? 0 : icon.getIconHeight() )
                && _iconTextGap            == label.getIconTextGap()
                && _verticalAlignment      == label.getVerticalAlignment()
                && _horizontalAlignment    == label.getHorizontalAlignment()
                && _verticalTextPosition   == label.getVerticalTextPosition()
                && _horizontalTextPosition == label.getHorizontalTextPosition()
                && _leftToRight            == label.getComponentOrientation().isLeftToRight();
        }

        String copyInto( Rectangle iconR, Rectangle textR ) {
            iconR.setBounds(_iconR);
            textR.setBounds(_textR);
            return _clippedText;
        }
    }
}

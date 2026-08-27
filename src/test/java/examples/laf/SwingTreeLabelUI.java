package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 *  The {@link JLabel} UI delegate, which also remembers where it last put a label's text - see
 *  {@link #layoutCL}.
 *  <p>
 *  It paints no background, not even for the cell renderer of a selected table or list row: one
 *  renderer instance stands in for every cell while the style engine keys a style on the
 *  <i>component</i>, so a per-cell decision here would land on whichever cell is painted next.
 *  Selection is filled by the owner instead - see {@link SwingTreeTableUI} and
 *  {@link SwingTreeListUI}.
 */
public final class SwingTreeLabelUI
        extends    BasicLabelUI
        implements SwingTreeStyledComponentUI<JLabel>
{
    /**
     *  Swing asks {@link #layoutCL} the same question from three directions - where the text
     *  sits, how large the label wants to be, and where its baseline is - each with a different
     *  viewing rectangle, so fewer than three slots make them evict each other: measured 33% hits
     *  with one slot, 50% with two, 99.7% with three. A fourth buys nothing, because a label
     *  needing more is one whose content genuinely varies.
     */
    private static final int REMEMBERED_PLACEMENTS = 3;

    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeLabelUI(); }

    private final Placement[] _placements = new Placement[REMEMBERED_PLACEMENTS];
    private int               _nextSlot   = 0;

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

    /**
     *  Places the label's text and icon inside {@code viewR} and returns the text as it should be
     *  drawn, shortened with an ellipsis when it does not fit.
     *  <p>
     *  Measuring text is expensive - a string with a character the font cannot advance from a
     *  lookup table costs a hundred times what plain ASCII does - and Swing asks this again on
     *  every paint, although a label's inputs almost never change between two of them. Every one
     *  of those inputs is compared here, so a label that changes any of them simply finds no
     *  match and is laid out afresh; nothing else needs invalidating.
     *  <p>
     *  Markup is never remembered: Swing lays it out through a document view that keeps its own
     *  measurements and discards them on its own - when an image inside it loads, say - so a
     *  remembered placement would shadow an invalidation this delegate cannot see.
     *  {@link BasicHTML#isHTMLString(String)} is exactly the condition under which Swing builds
     *  that view, and is cheaper and safer to ask than the client property, which also misses a
     *  label whose markup is switched off through {@code html.disable}.
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
     *  One remembered answer of {@link #layoutCL}. Of an icon only its size is held: that is all
     *  the placement is worked out from, and an icon that loads or animates reports a new size
     *  without becoming a new icon.
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

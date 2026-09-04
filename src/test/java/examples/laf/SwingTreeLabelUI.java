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
 *  The {@link JLabel} UI delegate. It paints no background, not even behind a selected table or
 *  list cell: {@link SwingTreeTableUI} and {@link SwingTreeListUI} fill those bands themselves,
 *  because one renderer instance cannot carry a colour that differs from cell to cell.
 */
public final class SwingTreeLabelUI
        extends    BasicLabelUI
        implements SwingTreeStyledComponentUI<JLabel>
{
    /**
     *  Swing asks {@link #layoutCL} three questions - where the text sits, how large the label
     *  wants to be, where its baseline is - each with a viewing rectangle of its own, so three
     *  slots are what it takes to stop them evicting each other: 33% hits with one, 50% with two,
     *  99.7% with three, and no further gain from a fourth.
     */
    private static final int REMEMBERED_PLACEMENTS = 3;

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
     *  Measuring text is expensive - a string holding a character the font cannot advance from a
     *  lookup table costs a hundred times what plain ASCII does - and Swing asks again on every
     *  paint although the answer almost never changes. So the last few answers are remembered.
     *  Every input the answer depends on is compared, which means a label that changes one of them
     *  finds no match and is laid out afresh, and nothing has to be invalidated by hand.
     *  <p>
     *  Markup is never remembered. Swing lays markup out through a document view which keeps
     *  measurements of its own and throws them away by itself, when an image inside it finishes
     *  loading for instance, and this delegate cannot see that happen.
     *  {@link BasicHTML#isHTMLString(String)} is the same test Swing uses to decide to build that
     *  view. The client property is the wrong test: it misses a label whose markup was switched
     *  off through {@code html.disable}.
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
     *  One remembered answer of {@link #layoutCL}. Only an icon's size is held, because the size
     *  is all the placement is worked out from, and an icon that finishes loading or animates
     *  reports a new size without becoming a different icon.
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

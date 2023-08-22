package swingtree.style;

import swingtree.api.Styler;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Optional;

/**
 *  A custom {@link Border} implementation which is capable of painting large parts of
 *  the styles defined by SwingTree user through the style API (see {@link swingtree.UIForAnySwing#withStyle(Styler)}).
 *  Not only does it paint borders, shadows and animation lambda but it also
 *  calculates the border insets of the component based on the margins, paddings and border widths
 *  specified by the user of the style API.
 *
 * @param <C> The type of the component that is being styled, animated or sized in a particular way...
 */
final class StyleAndAnimationBorder<C extends JComponent> implements Border
{
    private final ComponentExtension<C> _compExt;
    private final Border _formerBorder;
    private final boolean _borderWasNotPainted;

    private Insets _currentInsets = null;
    private Insets _currentMarginInsets = new Insets(0, 0, 0, 0);
    private Insets _currentPaddingInsets = new Insets(0, 0, 0, 0);


    StyleAndAnimationBorder( ComponentExtension<C> compExt, Border formerBorder ) {
        _compExt = compExt;
        _currentInsets = null;
        _formerBorder = formerBorder;
        if ( _compExt.getOwner() instanceof AbstractButton ) {
            AbstractButton b = (AbstractButton) _compExt.getOwner();
            _borderWasNotPainted = !b.isBorderPainted();
            b.setBorderPainted(true);
        }
        else
            _borderWasNotPainted = false;
    }

    Border getFormerBorder() { return _formerBorder; }

    Insets getCurrentMarginInsets() { return _currentMarginInsets; }

    Insets getCurrentPaddingInsets() { return _currentPaddingInsets; }

    @Override
    public void paintBorder( Component c, Graphics g, int x, int y, int width, int height )
    {
        _compExt.establishStyleAndBeginPainting(g);

        // We remember the clip:
        Shape formerClip = g.getClip();

        g.setClip(_compExt.getMainClip());

        _paintBorderAndBorderLayerStyles( (Graphics2D) g );
        if ( _formerBorder != null && !_borderWasNotPainted ) {
            BorderStyle borderStyle = _compExt.getCurrentStylePainter().getStyle().border();
            if ( !borderStyle.isVisible() )
                _paintFormerBorder(c, g, x, y, width, height);
        }

        if ( g.getClip() != formerClip )
            g.setClip(formerClip);

        _compExt._renderAnimations((Graphics2D) g);

        if ( g.getClip() != formerClip )
            g.setClip(formerClip);
    }

    private void _paintFormerBorder( Component c, Graphics g, int x, int y, int width, int height ) {
        try {
            Insets insets = _currentMarginInsets == null ? new Insets(0, 0, 0, 0) : _currentMarginInsets;
            _formerBorder.paintBorder(
                    c, g,
                    x + insets.left,
                    y + insets.top,
                    width - insets.left - insets.right,
                    height - insets.top - insets.bottom
                );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *  Not only paints the border but also styles which are configured to be painted
     *  on the border layer (see {@link swingtree.UI.Layer#BORDER}).
     *
     * @param g The graphics context that is used for painting.
     */
    private void _paintBorderAndBorderLayerStyles( Graphics2D g ) {
        try {
            _compExt.getCurrentStylePainter().paintBorderStyle( g, _compExt.getOwner() );
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    @Override
    public Insets getBorderInsets( Component c ) {
        if ( _currentInsets == null )
            _compExt.calculateApplyAndInstallStyle(false);
        return _currentInsets;
    }

    void recalculateInsets(Style style)
    {
        _currentMarginInsets  = calculateMarginInsets(style).orElse(_currentMarginInsets);
        _currentPaddingInsets = calculatePaddingInsets(style).orElse(_currentPaddingInsets);

        Insets insets = calculateBorderInsets(style,
                                _formerBorder == null
                                    ? new Insets(0, 0, 0, 0)
                                    : _formerBorder.getBorderInsets(_compExt.getOwner())
                            )
                            .orElseGet(() ->
                                _formerBorder == null
                                    ? new Insets(0, 0, 0, 0)
                                    : _formerBorder.getBorderInsets(_compExt.getOwner())
                            );

        if ( !insets.equals(_currentInsets) ) {
            _currentInsets = insets;
            _compExt.getOwner().revalidate();
        }
    }

    Insets getFormerBorderInsets() {
        if ( _borderWasNotPainted )
            return new Insets(0, 0, 0, 0);
        else
            return _formerBorder == null
                        ? new Insets(0, 0, 0, 0)
                        : _formerBorder.getBorderInsets(_compExt.getOwner());
    }

    @Override
    public boolean isBorderOpaque() { return false; }


    public static Optional<Insets> calculateBorderInsets( Style style, Insets formerInsets )
    {
        int left      = style.margin().left().orElse(formerInsets.left);
        int top       = style.margin().top().orElse(formerInsets.top);
        int right     = style.margin().right().orElse(formerInsets.right);
        int bottom    = style.margin().bottom().orElse(formerInsets.bottom);
        // Add padding:
        left   += style.padding().left().orElse(0);
        top    += style.padding().top().orElse(0);
        right  += style.padding().right().orElse(0);
        bottom += style.padding().bottom().orElse(0);
        // Add border widths:
        left   += Math.max(style.border().widths().left().orElse(0),   0);
        top    += Math.max(style.border().widths().top().orElse(0),    0);
        right  += Math.max(style.border().widths().right().orElse(0),  0);
        bottom += Math.max(style.border().widths().bottom().orElse(0), 0);

        return Optional.of(new Insets(top, left, bottom, right));
    }

    public static Optional<Insets> calculateMarginInsets( Style style )
    {
        int left   = style.margin().left().orElse(0);
        int top    = style.margin().top().orElse(0);
        int right  = style.margin().right().orElse(0);
        int bottom = style.margin().bottom().orElse(0);

        // Add border widths:
        left   += Math.max(style.border().widths().left().orElse(0),   0);
        top    += Math.max(style.border().widths().top().orElse(0),    0);
        right  += Math.max(style.border().widths().right().orElse(0),  0);
        bottom += Math.max(style.border().widths().bottom().orElse(0), 0);

        return Optional.of(new Insets(top, left, bottom, right));
    }

    public static Optional<Insets> calculatePaddingInsets( Style style )
    {
        int left   = style.padding().left().orElse(0);
        int top    = style.padding().top().orElse(0);
        int right  = style.padding().right().orElse(0);
        int bottom = style.padding().bottom().orElse(0);
        return Optional.of(new Insets(top, left, bottom, right));
    }

}

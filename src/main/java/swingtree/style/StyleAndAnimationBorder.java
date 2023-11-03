package swingtree.style;

import swingtree.api.Styler;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 *  A custom {@link Border} implementation which is capable of painting large parts of
 *  the styles defined by SwingTree user through the style API (see {@link swingtree.UIForAnySwing#withStyle(Styler)})
 *  as well as the previously installed {@link Border} of a component,
 *  to which it delegates the painting of the border if the current {@link Style}
 *  does not override the looks of the former border.
 *  Not only does this paint borders, shadows and animation lambda, but it also
 *  calculates the border insets of the component based on the margins, paddings and border widths
 *  specified by the user of the style API. <br>
 *  This class is mostly responsible for making styling compatible with
 *  any plain old Swing component...
 *
 * @param <C> The type of the component that is being styled, animated or sized in a particular way...
 */
final class StyleAndAnimationBorder<C extends JComponent> implements Border
{
    private final ComponentExtension<C> _compExt;
    private final Border                _formerBorder;
    private final boolean               _borderWasNotPainted;

    private Insets _insets;
    private final Insets _marginInsets      = new Insets(0, 0, 0, 0);
    private final Insets _paddingInsets     = new Insets(0, 0, 0, 0);
    private final Insets _fullPaddingInsets = new Insets(0, 0, 0, 0);


    StyleAndAnimationBorder( ComponentExtension<C> compExt, Border formerBorder ) {
        _compExt       = compExt;
        _insets        = null;
        _formerBorder  = formerBorder;
        if ( _compExt.getOwner() instanceof AbstractButton ) {
            AbstractButton b = (AbstractButton) _compExt.getOwner();
            _borderWasNotPainted = !b.isBorderPainted();
            b.setBorderPainted(true);
        }
        else
            _borderWasNotPainted = false;
    }

    Border getFormerBorder() { return _formerBorder; }

    Insets getMarginInsets() { return _marginInsets; }

    Insets getPaddingInsets() { return _paddingInsets; }

    Insets getFullPaddingInsets() { return _fullPaddingInsets; }

    @Override
    public void paintBorder( Component c, Graphics g, int x, int y, int width, int height )
    {
        _compExt.establishStyleAndBeginPainting();

        g.setClip( _compExt.getMainClip() );

        _paintBorderAndBorderLayerStyles( (Graphics2D) g );
        if ( _formerBorder != null && !_borderWasNotPainted ) {
            BorderStyle borderStyle = _compExt.getStyle().border();
            if ( !borderStyle.isVisible() )
                _paintFormerBorder(c, g, x, y, width, height);
        }
        _compExt._renderAnimations((Graphics2D) g);
    }

    private void _paintFormerBorder( Component c, Graphics g, int x, int y, int width, int height ) {
        try {
            _formerBorder.paintBorder(
                    c, g,
                    x + _marginInsets.left,
                    y + _marginInsets.top,
                    width   - _marginInsets.left - _marginInsets.right,
                    height - _marginInsets.top  - _marginInsets.bottom
                );
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            /*
                 Note that if any exceptions happen in the former Border implementation,
                 then we don't want to mess up the execution of the rest of the component painting...
                 Therefore, we catch any exceptions that happen in the above code.

                 Ideally this would be logged in the logging framework of the user
                 who implemented the Border,
                 but we don't know which logging framework that is, so we just print
                 the stack trace to the console so that any developers can see what went wrong.
            */
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
            _compExt._paintBorderStyle( g, _compExt.getOwner() );
        } catch ( Exception ex ) {
            ex.printStackTrace();
            /*
                Note that if any exceptions happen during the border style painting,
                then we don't want to mess up how the rest of the component is painted...
                Therefore, we catch any exceptions that happen in the above code.
            */
        }
    }

    @Override
    public Insets getBorderInsets( Component c ) {
        if ( _insets == null )
            _compExt.calculateApplyAndInstallStyle(false);
        return _insets;
    }

    void recalculateInsets(Style style)
    {
        _calculateMarginInsets(style);
        _calculatePaddingInsets(style);
        _calculateFullPaddingInsets(style);
        _calculateBorderInsets(style,
                _formerBorder == null
                    ? new Insets(0, 0, 0, 0)
                    : _formerBorder.getBorderInsets(_compExt.getOwner())
            );
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


    private void _calculateBorderInsets( Style style, Insets formerInsets )
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

        if (
            _insets == null         ||
            _insets.left   != left  ||
            _insets.top    != top   ||
            _insets.right  != right ||
            _insets.bottom != bottom
        ) {
            if ( _insets == null )
                _insets = new Insets(top, left, bottom, right);
            else
                _insets.set(top, left, bottom, right);

            _compExt.getOwner().revalidate();
        }
    }

    private void _calculateMarginInsets( Style style )
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

        _marginInsets.top    = top;
        _marginInsets.left   = left;
        _marginInsets.right  = right;
        _marginInsets.bottom = bottom;
    }

    private void _calculatePaddingInsets( Style style )
    {
        _paddingInsets.top    = style.padding().left().orElse(0);
        _paddingInsets.left   = style.padding().top().orElse(0);
        _paddingInsets.right  = style.padding().right().orElse(0);
        _paddingInsets.bottom = style.padding().bottom().orElse(0);
    }

    private void _calculateFullPaddingInsets( Style style )
    {
        _fullPaddingInsets.top    = style.padding().left().orElse(0)   + style.margin().left().orElse(0);
        _fullPaddingInsets.left   = style.padding().top().orElse(0)    + style.margin().top().orElse(0);
        _fullPaddingInsets.right  = style.padding().right().orElse(0)  + style.margin().right().orElse(0);
        _fullPaddingInsets.bottom = style.padding().bottom().orElse(0) + style.margin().bottom().orElse(0);
    }

}

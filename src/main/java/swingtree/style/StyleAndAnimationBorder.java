package swingtree.style;

import org.slf4j.Logger;
import swingtree.UI;
import swingtree.api.Styler;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 *  A custom {@link Border} implementation which is capable of painting large parts of
 *  the styles exposed by the style API (see {@link swingtree.UIForAnySwing#withStyle(Styler)})
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
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleAndAnimationBorder.class);

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
        try {
            _compExt.paintBorderAndAnimations((Graphics2D) g, ()->{
                if ( _formerBorder != null && !_borderWasNotPainted ) {
                    BorderStyle borderStyle = _compExt.getStyle().border();
                    if ( !borderStyle.isVisible() )
                        _paintFormerBorder(c, g, x, y, width, height);
                }
            });
        } catch ( Exception ex ) {
            /*
                Note that if any exceptions happen during the border style painting,
                then we don't want to mess up how the rest of the component is painted...
                Therefore, we catch any exceptions that happen in the above code.
            */
            log.error("Exception while painting border style '"+_compExt.getStyle().border()+"': ", ex);
        }
    }

    private void _paintFormerBorder( Component c, Graphics g, int x, int y, int width, int height ) {
        try {
            x = x + _marginInsets.left;
            y = y + _marginInsets.top;
            width  = width  - _marginInsets.left - _marginInsets.right;
            height = height - _marginInsets.top  - _marginInsets.bottom;

            _formerBorder.paintBorder(c, g, x, y, width, height);
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
            log.error("Exception while painting former border '{}': ", _formerBorder, ex);
        }
    }

    @Override
    public Insets getBorderInsets( Component c ) {
        if ( _insets == null )
            _compExt.gatherApplyAndInstallStyle(false);
        return _insets;
    }

    void recalculateInsets(Style style)
    {
        _calculateMarginInsets(style);
        _calculatePaddingInsets(style);
        _calculateFullPaddingInsets(style);
        _calculateBorderInsets(style);
    }

    @Override
    public boolean isBorderOpaque() { return false; }

    public Insets getBaseInsets(boolean adjust)
    {
        if ( _formerBorder == null )
            return new Insets(0, 0, 0, 0);

        boolean usesSwingTreeBorder = _compExt.getStyle().border().isVisible();

        if ( usesSwingTreeBorder )
            return new Insets(0, 0, 0, 0);
        else
        {
            Insets formerInsets = _formerBorder.getBorderInsets(_compExt.getOwner());
            int left   = 0;
            int top    = 0;
            int right  = 0;
            int bottom = 0;
            if ( !adjust ) {
                left   += formerInsets.left;
                top    += formerInsets.top;
                right  += formerInsets.right;
                bottom += formerInsets.bottom;
            } else if (
                UI.currentLookAndFeel().isOneOf(UI.LookAndFeel.NIMBUS) &&
                _compExt.getOwner() instanceof JTextComponent
            ) {
                left   += formerInsets.left;
                top    += formerInsets.top;
                right  += formerInsets.right;
                bottom += formerInsets.bottom;
                left   = left   / 2;
                top    = top    / 2;
                right  = right  / 2;
                bottom = bottom / 2;
            }
            return new Insets(top, left, bottom, right);
        }
    }

    private void _calculateBorderInsets( Style style )
    {
        Insets correction = getBaseInsets(false);

        float left   = correction.left;
        float top    = correction.top;
        float right  = correction.right;
        float bottom = correction.bottom;

        left   = style.margin().left()  .orElse(left  );
        top    = style.margin().top()   .orElse(top   );
        right  = style.margin().right() .orElse(right );
        bottom = style.margin().bottom().orElse(bottom);

        // Add padding:
        left   += style.padding().left().orElse(0f);
        top    += style.padding().top().orElse(0f);
        right  += style.padding().right().orElse(0f);
        bottom += style.padding().bottom().orElse(0f);
        // Add border widths:
        left   += Math.max(style.border().widths().left().orElse(0f),   0);
        top    += Math.max(style.border().widths().top().orElse(0f),    0);
        right  += Math.max(style.border().widths().right().orElse(0f),  0);
        bottom += Math.max(style.border().widths().bottom().orElse(0f), 0);

        if (
            _insets == null         ||
            _insets.left   != left  ||
            _insets.top    != top   ||
            _insets.right  != right ||
            _insets.bottom != bottom
        ) {
            if ( _insets == null )
                _insets = new Insets((int) top, (int) left, (int) bottom, (int) right);
            else
                _insets.set((int) top, (int) left, (int) bottom, (int) right);

            _compExt.getOwner().revalidate();
        }
    }

    private void _calculateMarginInsets( Style style )
    {
        float left   = style.margin().left().orElse(0f);
        float top    = style.margin().top().orElse(0f);
        float right  = style.margin().right().orElse(0f);
        float bottom = style.margin().bottom().orElse(0f);

        // Add border widths:
        left   += Math.max(style.border().widths().left().orElse(0f),   0);
        top    += Math.max(style.border().widths().top().orElse(0f),    0);
        right  += Math.max(style.border().widths().right().orElse(0f),  0);
        bottom += Math.max(style.border().widths().bottom().orElse(0f), 0);

        _marginInsets.top    = (int) top;
        _marginInsets.left   = (int) left;
        _marginInsets.right  = (int) right;
        _marginInsets.bottom = (int) bottom;
    }

    private void _calculatePaddingInsets( Style style )
    {
        _paddingInsets.top    = style.padding().top().map(Number::intValue).orElse(0);
        _paddingInsets.left   = style.padding().left().map(Number::intValue).orElse(0);
        _paddingInsets.right  = style.padding().right().map(Number::intValue).orElse(0);
        _paddingInsets.bottom = style.padding().bottom().map(Number::intValue).orElse(0);
    }

    private void _calculateFullPaddingInsets( Style style )
    {
        _fullPaddingInsets.top    = (int)( style.padding().top().orElse(0f)    + style.margin().top().orElse(0f) );
        _fullPaddingInsets.left   = (int)( style.padding().left().orElse(0f)   + style.margin().left().orElse(0f) );
        _fullPaddingInsets.right  = (int)( style.padding().right().orElse(0f)  + style.margin().right().orElse(0f) );
        _fullPaddingInsets.bottom = (int)( style.padding().bottom().orElse(0f) + style.margin().bottom().orElse(0f) );
    }

}

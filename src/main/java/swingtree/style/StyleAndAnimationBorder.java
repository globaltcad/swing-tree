package swingtree.style;

import swingtree.api.Styler;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

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
    static boolean canFullyPaint(Style.Report report) {
        boolean simple = report.onlyDimensionalityAndOrLayoutIsStyled();
        if (simple) return true;
        return report.noBorderStyle &&
                report.noBaseStyle  &&
                (report.noShadowStyle || report.allShadowsAreBorderShadows) &&
                (report.noPainters    || report.allPaintersAreBorderPainters) &&
                (report.noGradients   || report.allGradientsAreBorderGradients) &&
                (report.noImages      || report.allImagesAreBorderImages);
    }

    private final ComponentExtension<C> _compExt;
    private final Border _formerBorder;
    private final boolean _borderWasNotPainted;
    private Insets _currentInsets;
    private Insets _currentMarginInsets = new Insets(0, 0, 0, 0);
    private Insets _currentPaddingInsets = new Insets(0, 0, 0, 0);

    StyleAndAnimationBorder(ComponentExtension<C> compExt, Border formerBorder) {
        _compExt = compExt;
        _currentInsets = null;
        _formerBorder = formerBorder;
        if (_compExt.getOwner() instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) _compExt.getOwner();
            _borderWasNotPainted = !b.isBorderPainted();
            b.setBorderPainted(true);
        } else
            _borderWasNotPainted = false;
    }

    Border getFormerBorder() {
        return _formerBorder;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        _checkIfInsetsChanged();

        _compExt._establishCurrentMainPaintClip(g);

        // We remember the clip:
        Shape formerClip = g.getClip();

        g.setClip(_compExt._mainClip);

        if ( !_compExt._currentStylePainter.equals(StylePainter.none()) ) {
            _paintThisStyleAPIBasedBorder((Graphics2D) g);
            if (_formerBorder != null && !_borderWasNotPainted) {
                Style.Report report = _compExt._currentStylePainter.getStyle().getReport();
                if (canFullyPaint(report))
                    _paintFormerBorder(c, g, x, y, width, height);
            }
        } else if (_formerBorder != null && !_borderWasNotPainted)
            _paintFormerBorder(c, g, x, y, width, height);

        if (g.getClip() != formerClip)
            g.setClip(formerClip);

        _compExt._renderAnimations((Graphics2D) g);

        if (g.getClip() != formerClip)
            g.setClip(formerClip);
    }

    private void _paintFormerBorder(Component c, Graphics g, int x, int y, int width, int height) {
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

    private void _paintThisStyleAPIBasedBorder(Graphics2D g) {
        try {
            _compExt._currentStylePainter.paintBorderStyle( g, _compExt.getOwner() );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        _checkIfInsetsChanged();
        return _currentInsets;
    }

    private void _checkIfInsetsChanged() {
        Insets insets = _calculateInsets();
        if (!insets.equals(_currentInsets)) {
            _currentInsets = insets;
            _compExt.getOwner().revalidate();
        }
    }

    private Insets _calculateInsets() {
        _currentMarginInsets = _compExt._getOrCreateStylePainter()
                                        .calculateMarginInsets()
                                        .orElse(_currentMarginInsets);

        _currentPaddingInsets = _compExt._getOrCreateStylePainter()
                                        .calculatePaddingInsets()
                                        .orElse(_currentPaddingInsets);

        return _compExt._getOrCreateStylePainter()
                        .calculateBorderInsets(
                            _formerBorder == null
                                    ? new Insets(0, 0, 0, 0)
                                    : _formerBorder.getBorderInsets(_compExt.getOwner())
                        )
                        .orElseGet(() ->
                            _formerBorder == null
                                ? new Insets(0, 0, 0, 0)
                                : _formerBorder.getBorderInsets(_compExt.getOwner())
                        );
    }

    public Insets getCurrentMarginInsets() {
        return _currentMarginInsets;
    }

    public Insets getCurrentPaddingInsets() {
        return _currentPaddingInsets;
    }

    public Insets getFormerBorderInsets() {
        if ( _borderWasNotPainted )
            return new Insets(0, 0, 0, 0);
        else
            return _formerBorder == null
                        ? new Insets(0, 0, 0, 0)
                        : _formerBorder.getBorderInsets(_compExt.getOwner());
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

}

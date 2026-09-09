package examples.laf;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Objects;

/**
 *  The {@link JComboBox} UI delegate. The drop-down list is a popup of Swing's own making and
 *  takes the popup menu and list defaults; the {@code ComboBox.list*} keys are what an
 *  application overrides to change it.
 */
public final class SwingTreeComboBoxUI
        extends    BasicComboBoxUI
        implements SwingTreeStyledComponentUI<JComboBox<?>>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeComboBoxUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
        // Focus on an editable combo box lands on its editor, and BasicComboBoxUI repaints the
        // combo box only for focus that lands on the combo box itself.
        JComboBox<?> combo = (JComboBox<?>) c;
        if ( combo.getEditor() != null )
            LafUtilities.repaintOnFocusChange(combo, combo.getEditor().getEditorComponent());
    }

    @Override
    public void uninstallUI( JComponent c ) {
        JComboBox<?> combo = (JComboBox<?>) c;
        if ( combo.getEditor() != null )
            LafUtilities.uninstallFocusRepaint(combo, combo.getEditor().getEditorComponent());
        super.uninstallUI(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    /**
     *  Where the text of the value on show sits, which a layout manager asks for in order to line
     *  a combo box up with the labels beside it.
     *  <p>
     *  Swing answers it by <b>building a renderer component</b>: it asks the combo box's renderer
     *  to render the prototype value, or the first item when there is no prototype, puts the combo
     *  box's font on the result and asks that for its baseline. A layout manager asks every
     *  component in a baseline aligned row on every pass, so the renderer runs on every pass of
     *  every layout - and a SwingTree renderer runs the application's own cell configurator, so
     *  measuring a combo box executes application code. Measured over the showcase, this one
     *  method was 28% of everything the layout spent on baselines, and two thirds of that was
     *  inside the renderer.
     *  <p>
     *  The answer is remembered against every input Swing reads to produce it. What that does not
     *  cover is a renderer which is not a function of the value it is given - one reading a clock,
     *  say. Swing has the same exposure in the display size it caches next to this, and answers it
     *  by listening for model and property changes; this does not listen, so a renderer like that
     *  would be answered from a stale measurement until the value, the font, the border or the
     *  size changes. That is the price of not installing a listener for a measurement, and it is
     *  named here rather than left to be discovered.
     */
    @Override
    public int getBaseline( JComponent c, int width, int height ) {
        if ( c == null )
            throw new NullPointerException("Component must be non-null");
        if ( width < 0 || height < 0 )
            throw new IllegalArgumentException("Width and height must be >= 0");

        Insets insets = c.getInsets(_scratchInsets);
        for ( Baseline remembered : _baselines ) {
            if ( remembered != null && remembered.answers(comboBox, width, height, insets) )
                return remembered.baseline();
        }
        int baseline = super.getBaseline(c, width, height);
        _baselines[_nextBaseline] = new Baseline(comboBox, width, height, insets, baseline);
        _nextBaseline = ( _nextBaseline + 1 ) % _baselines.length;
        return baseline;
    }

    /**
     *  Three remembered baselines, which is what it takes and no more. A layout manager asks a
     *  component about several sizes in one pass - its minimum, its preferred, and the one it
     *  settles on - and with fewer slots those questions evict each other. Measured over the
     *  showcase across six fresh widths: 12.5% with one slot, 62.5% with two, and 100% with three,
     *  with nothing further gained from a fourth.
     */
    private final Baseline[] _baselines     = new Baseline[3];
    private       int        _nextBaseline  = 0;
    private final Insets     _scratchInsets = new Insets(0, 0, 0, 0);

    /**
     *  One remembered answer of {@link #getBaseline(JComponent, int, int)}, together with every
     *  input Swing reads to arrive at it: the size it was asked about, the border around the
     *  value, whether the value is edited rather than rendered, the font put on the renderer's
     *  result, the renderer itself, and the value it would be handed.
     */
    private static final class Baseline
    {
        private final int                 _width;
        private final int                 _height;
        private final int                 _insetTop;
        private final int                 _insetBottom;
        private final boolean             _editable;
        private final @Nullable Font      _font;
        private final @Nullable Object    _renderer;
        private final @Nullable Object    _value;
        private final int                 _modelSize;
        private final int                 _baseline;

        Baseline( JComboBox<?> combo, int width, int height, Insets insets, int baseline ) {
            _width       = width;
            _height      = height;
            _insetTop    = insets.top;
            _insetBottom = insets.bottom;
            _editable    = combo.isEditable();
            _font        = combo.getFont();
            _renderer    = combo.getRenderer();
            _value       = _measuredValueOf(combo);
            _modelSize   = _sizeOf(combo);
            _baseline    = baseline;
        }

        boolean answers( JComboBox<?> combo, int width, int height, Insets insets ) {
            return _width       == width
                && _height      == height
                && _insetTop    == insets.top
                && _insetBottom == insets.bottom
                && _editable    == combo.isEditable()
                && _modelSize   == _sizeOf(combo)
                && Objects.equals(_font, combo.getFont())
                && _renderer    == combo.getRenderer()
                && Objects.equals(_value, _measuredValueOf(combo));
        }

        int baseline() { return _baseline; }

        /** The value Swing renders to measure: the prototype, or the first item when there is none. */
        private static @Nullable Object _measuredValueOf( JComboBox<?> combo ) {
            Object prototype = combo.getPrototypeDisplayValue();
            if ( prototype != null )
                return prototype;
            ComboBoxModel<?> model = combo.getModel();
            return ( model != null && model.getSize() > 0 ) ? model.getElementAt(0) : null;
        }

        private static int _sizeOf( JComboBox<?> combo ) {
            ComboBoxModel<?> model = combo.getModel();
            return model == null ? -1 : model.getSize();
        }
    }

    /**
     *  Swing fills the strip showing the current value from the {@code ComboBox.background}
     *  default rather than from the component, which puts a square opaque rectangle over the
     *  rounded, possibly translucent surface a style rule has just painted. So it is only filled
     *  when no rule paints that surface.
     */
    @Override
    public void paintCurrentValueBackground( Graphics g, Rectangle bounds, boolean hasFocus ) {
        if ( !SwingTreeLookAndFeel.styles(comboBox.getClass()) )
            super.paintCurrentValueBackground(g, bounds, hasFocus);
    }

    @Override
    protected JButton createArrowButton() {
        return SwingTreeLookAndFeel.drawsOwnChrome() ? new ArrowButton() : super.createArrowButton();
    }

    @Override
    public ComponentStyleDelegate<JComboBox<?>> style( ComponentStyleDelegate<JComboBox<?>> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /** The button carrying the symbol set's drop-down arrow. */
    private static final class ArrowButton extends ActuatorButton
    {
        @Override public Dimension getPreferredSize() {
            int side = UI.scale(SwingTreeLookAndFeel.symbols().comboArrowButtonSize());
            return new Dimension(side, side);
        }

        @Override
        void paintActuator( Graphics2D g, Symbols symbols, SwingTreeLookAndFeel.Palette palette ) {
            ButtonModel model = getModel();
            symbols.paintComboArrow(
                    g, palette, getWidth(), getHeight(),
                    isEnabled(), model.isRollover(), model.isPressed()
            );
        }
    }
}

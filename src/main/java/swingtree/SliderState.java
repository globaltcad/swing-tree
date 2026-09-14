package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.api.model.SliderTicks;
import swingtree.style.ComponentBackend;
import swingtree.threading.EventProcessor;

import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.UIResource;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class SliderState
{
    private static final Logger log = LoggerFactory.getLogger(SliderState.class);

    static SliderState of( JSlider slider, Class<? extends Number> numberType, EventProcessor eventProcessor ) {
        return ComponentBackend.powering(slider)
                .getOrSet(SliderState.class, () -> new SliderState(slider, numberType, eventProcessor));
    }

    private final JSlider                 _slider;
    private final Class<? extends Number> _numberType;
    private final EventProcessor          _eventProcessor;
    private final List<ChangeListener>    _changeActions = new ArrayList<>();

    private Number                       _min;
    private Number                       _max;
    private Number                       _value;
    private @Nullable SliderTicks<?>     _ticks;
    private SliderGrid                   _grid;
    private @Nullable Var<Number>        _valueTarget;
    private @Nullable Val<Number>        _minSource;
    private @Nullable Val<Number>        _maxSource;
    private int                          _lastSeenStep;
    private int                          _writesStillOnTheirWay;
    private boolean                      _isChangingTheSliderItself;

    private SliderState( JSlider slider, Class<? extends Number> numberType, EventProcessor eventProcessor ) {
        _slider         = slider;
        _numberType     = numberType;
        _eventProcessor = eventProcessor;
        BoundedRangeModel model = slider.getModel();
        _min   = SliderGrid.convert(numberType, model.getMinimum());
        _max   = SliderGrid.convert(numberType, model.getMaximum());
        _value = SliderGrid.convert(numberType, model.getValue());
        _grid  = SliderGrid.of(numberType, _min, _max, null);
        _lastSeenStep = model.getValue();
        slider.addChangeListener(new SliderChangeListener());
    }

    void setMin( Number min ) {
        _min = SliderGrid.convert(_numberType, min);
        _layOutForNewRange();
    }

    void setMax( Number max ) {
        _max = SliderGrid.convert(_numberType, max);
        _layOutForNewRange();
    }

    void setValue( Number value ) {
        if ( _slider.getValueIsAdjusting() || _writesStillOnTheirWay > 0 )
            return;
        _value = SliderGrid.convert(_numberType, value);
        _placeKnob();
    }

    void setTicks( SliderTicks<?> ticks ) {
        if ( ticks.equals(_ticks) )
            return;
        _ticks = ticks;
        _grid = SliderGrid.of(_numberType, _min, _max, ticks);
        _placeKnob();
        _installTicks(_grid, ticks);
    }

    void onChange( ChangeListener action ) {
        _changeActions.add(action);
    }

    @SuppressWarnings("unchecked")
    void keepMinWithin( Val<? extends Number> minSource ) {
        _minSource = (Val<Number>) minSource;
    }

    @SuppressWarnings("unchecked")
    void keepMaxWithin( Val<? extends Number> maxSource ) {
        _maxSource = (Val<Number>) maxSource;
    }

    @SuppressWarnings("unchecked")
    void writeUserChangesTo( Var<? extends Number> target ) {
        _valueTarget = (Var<Number>) target;
    }

    private void _layOutForNewRange() {
        SliderGrid grid = SliderGrid.of(_numberType, _min, _max, _ticks);
        boolean gridChanged = !grid.equals(_grid);
        _grid = grid;
        _placeKnob();
        SliderTicks<?> ticks = _ticks;
        if ( ticks != null && gridChanged )
            _installTicks(grid, ticks);
    }

    private void _placeKnob() {
        SliderGrid grid = _grid;
        int step = grid.stepOf(_value);
        BoundedRangeModel model = _slider.getModel();
        if ( model.getMinimum() != grid.intMin() || model.getMaximum() != grid.intMax() || model.getValue() != step )
            _changeTheSliderItself(() -> model.setRangeProperties(step, model.getExtent(), grid.intMin(), grid.intMax(), model.getValueIsAdjusting()));
        _lastSeenStep = model.getValue();
    }

    private void _installTicks( SliderGrid grid, SliderTicks<?> ticks ) {
        Hashtable<Integer, JComponent> labels = _labelsFor(grid, ticks);
        if ( labels.isEmpty() ) {
            _slider.setPaintLabels(false);
            _slider.setLabelTable(labels);
        } else {
            _slider.setLabelTable(labels);
            _slider.setPaintLabels(true);
        }
        _slider.setMajorTickSpacing(grid.majorSpacingInSteps());
        _slider.setMinorTickSpacing(grid.minorSpacingInSteps());
        _slider.setPaintTicks(ticks.hasVisibleTickMarks() && grid.majorSpacingInSteps() > 0);
    }

    @SuppressWarnings("JdkObsolete")
    private <N extends Number> Hashtable<Integer, JComponent> _labelsFor( SliderGrid grid, SliderTicks<N> ticks ) {
        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        if ( ticks.hasLabelsAtMajorTicks() ) {
            int count = grid.majorTickCount();
            Function<N, String> text = ticks.majorTickLabelText().orElse(null);
            int decimals = text == null ? _decimalsToWriteExactly(grid, count) : 0;
            for ( int index = 0; index < count; index++ ) {
                BigDecimal number = grid.numberAtMajorTick(index);
                String labelText = text == null
                                    ? _numberText(number, decimals, ticks.labelLocale())
                                    : _textOf(text, SliderGrid.convert(ticks.numberType(), number));
                labels.put(grid.stepOfMajorTick(index), new TickLabel(_slider, labelText));
            }
        }
        for ( N position : ticks.labelPositions() ) {
            if ( !grid.isInRange(position) )
                continue;
            int step = grid.stepOf(position);
            ticks.labelTextAt(position).ifPresent( text -> labels.put(step, new TickLabel(_slider, text)) );
            ticks.labelIconAt(position).ifPresent( icon -> labels.put(step, new TickLabel(_slider, icon.find().orElse(null))) );
        }
        return labels;
    }

    private static int _decimalsToWriteExactly( SliderGrid grid, int count ) {
        int decimals = 0;
        for ( int index = 0; index < count; index++ )
            decimals = Math.max(decimals, grid.numberAtMajorTick(index).stripTrailingZeros().scale());
        return decimals;
    }

    private static String _numberText( BigDecimal number, int decimals, Locale locale ) {
        BigDecimal rounded = number.setScale(decimals, RoundingMode.HALF_UP);
        if ( Locale.ROOT.equals(locale) )
            return rounded.toPlainString();
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);
        return format.format(rounded);
    }

    private static <N extends Number> String _textOf( Function<N, String> text, N number ) {
        try {
            String result = text.apply(number);
            return result == null ? "" : result;
        } catch ( Exception e ) {
            log.error(SwingTree.get().logMarker(), "Failed to compute the text of the slider label at {}.", number, e);
            return "";
        }
    }

    private void _onSliderChange( ChangeEvent event ) {
        if ( _isChangingTheSliderItself )
            return;
        BoundedRangeModel model = _slider.getModel();
        if ( model.getMinimum() != _grid.intMin() || model.getMaximum() != _grid.intMax() )
            _followRangeSetOnTheSlider(model);
        _onUserChange();
        for ( ChangeListener action : _changeActions )
            action.stateChanged(event);
    }

    private void _followRangeSetOnTheSlider( BoundedRangeModel model ) {
        if ( SliderGrid.isWholeNumberType(_numberType) ) {
            _min = SliderGrid.convert(_numberType, model.getMinimum());
            _max = SliderGrid.convert(_numberType, model.getMaximum());
            _layOutForNewRange();
        }
        else
            _placeKnob();
    }

    private void _onUserChange() {
        int step = _slider.getValue();
        int chosenStep = _chooseStepFor(step);
        _lastSeenStep = step;
        if ( chosenStep != step && !_slider.getValueIsAdjusting() ) {
            _changeTheSliderItself(() -> _slider.setValue(chosenStep));
            _lastSeenStep = chosenStep;
        }
        Number number = _isStepOfTheValue(chosenStep) ? _value : _grid.numberAt(chosenStep);
        _value = number;
        Var<Number> target = _valueTarget;
        if ( target == null )
            return;
        Val<Number> minSource = _minSource;
        Val<Number> maxSource = _maxSource;
        _writesStillOnTheirWay++;
        _eventProcessor.registerAppEvent(() -> {
            target.set(From.VIEW, _keepWithin(number, minSource, maxSource));
            Number settled = target.get();
            _eventProcessor.registerUIEvent(() -> _acknowledgeWrite(settled));
        });
    }

    private void _acknowledgeWrite( Number settled ) {
        _writesStillOnTheirWay--;
        if ( _writesStillOnTheirWay == 0 )
            setValue(settled);
    }

    private int _chooseStepFor( int step ) {
        SliderTicks<?> ticks = _ticks;
        if ( ticks == null || !ticks.isSnappingToTicks() || _isStepOfTheValue(step) )
            return step;
        if ( _slider.getValueIsAdjusting() || step == _lastSeenStep )
            return _grid.nearestTick(step);
        return _grid.nextTickTowards(step, _lastSeenStep);
    }

    private boolean _isStepOfTheValue( int step ) {
        return _grid.isInRange(_value) && _grid.stepOf(_value) == step;
    }

    private static Number _keepWithin( Number number, @Nullable Val<Number> minSource, @Nullable Val<Number> maxSource ) {
        if ( minSource != null ) {
            Number min = minSource.orElseNull();
            if ( min != null && number.doubleValue() < min.doubleValue() )
                return min;
        }
        if ( maxSource != null ) {
            Number max = maxSource.orElseNull();
            if ( max != null && number.doubleValue() > max.doubleValue() )
                return max;
        }
        return number;
    }

    private void _changeTheSliderItself( Runnable change ) {
        boolean wasChangingTheSliderItself = _isChangingTheSliderItself;
        _isChangingTheSliderItself = true;
        try {
            change.run();
        } finally {
            _isChangingTheSliderItself = wasChangingTheSliderItself;
        }
    }

    private final class SliderChangeListener implements ChangeListener {
        @Override
        public void stateChanged( ChangeEvent event ) {
            _onSliderChange(event);
        }
    }

    private static final class TickLabel extends JLabel implements UIResource
    {
        private final @Nullable JSlider _owner;

        TickLabel( JSlider owner, String text ) {
            super(text, JLabel.CENTER);
            _owner = owner;
            setName("Slider.label");
        }

        TickLabel( JSlider owner, @Nullable Icon icon ) {
            super(icon, JLabel.CENTER);
            _owner = owner;
            setName("Slider.label");
        }

        @Override
        public Font getFont() {
            Font font = super.getFont();
            JSlider owner = _owner;
            if ( owner == null || (font != null && !(font instanceof UIResource)) )
                return font;
            return owner.getFont();
        }

        @Override
        public Color getForeground() {
            Color foreground = super.getForeground();
            JSlider owner = _owner;
            if ( owner == null || (foreground != null && !(foreground instanceof UIResource)) )
                return foreground;
            Color ownerForeground = owner.getForeground();
            if ( ownerForeground != null && !(ownerForeground instanceof UIResource) )
                return ownerForeground;
            return foreground;
        }
    }
}

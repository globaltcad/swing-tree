package swingtree;

import sprouts.Val;
import sprouts.Var;

import javax.swing.JSplitPane;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JSplitPane} instances.
 */
public final class UIForSplitPane<P extends JSplitPane> extends UIForAnySwing<UIForSplitPane<P>, P>
{
    private final BuilderState<P> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */

    UIForSplitPane( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForSplitPane<P> _newBuilderWithState(BuilderState<P> newState ) {
        return new UIForSplitPane<>(newState);
    }

    /**
     * Sets the alignment of the split bar in the split pane.
     *
     * @param align The alignment of the split bar in the split pane.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if the provided alignment is null.
     */
    public final UIForSplitPane<P> withOrientation( UI.Align align ) {
        NullUtil.nullArgCheck( align, "split", UI.Align.class );
        return _with( thisComponent -> {
                    thisComponent.setOrientation( align.forSplitPane() );
                })
                ._this();
    }

    /**
     * Sets the alignment of the split bar in the split pane dynamically
     * based on the provided {@link Val} property which will be observed
     * by the split pane.
     *
     * @param align The alignment property of the split bar in the split pane.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if the provided alignment is null or the property is allowed to wrap a null value.
     */
    public final UIForSplitPane<P> withOrientation( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment." );
        return _withOnShow( align, (thisComponent,it) -> {
                    thisComponent.setOrientation( it.forSplitPane() );
                })
                ._with( thisComponent -> {
                    thisComponent.setOrientation( align.orElseThrowUnchecked().forSplitPane() );
                })
                ._this();
    }

    /**
     * Sets the location of the divider. This is passed off to the
     * look and feel implementation, and then listeners are notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via <code>setLastDividerLocation</code>.
     *
     * @param location An int specifying a UI-specific value (typically a
     *        pixel count)
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDividerAt( int location ) {
        return _with( thisComponent -> {
                    thisComponent.setDividerLocation(location);
                })
                ._this();
    }

    /**
     * Sets the location of the divider in the form of a property,
     * which can be dynamically update the divide.
     * This is passed off to the
     * look and feel implementation, and then listeners are notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via <code>setLastDividerLocation</code>.
     *
     * @param location A property dynamically determining a UI-specific value (typically a
     *        pixel count)
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code location} is {@code null}.
     */
    public final UIForSplitPane<P> withDividerAt( Val<Integer> location ) {
        NullUtil.nullArgCheck( location, "location", Val.class );
        NullUtil.nullPropertyCheck( location, "location", "Null is not a valid divider location." );
        return _withOnShow( location, (thisComponent, it) -> {
                    thisComponent.setDividerLocation(it);
                })
                ._with( thisComponent -> {
                    thisComponent.setDividerLocation( location.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     * Sets the size of the divider.
     *
     * @param size An integer giving the size of the divider in pixels
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDividerSize( int size ) {
        return _with( thisComponent -> {
                    thisComponent.setDividerSize(UI.scale(size));
                })
                ._this();
    }

    /**
     * Sets the size of the divider in the form of a property,
     * which can be dynamically update.
     *
     * @param size A property dynamically determining the size of the divider in pixels
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code size} is {@code null}.
     */
    public final UIForSplitPane<P> withDividerSize( Val<Integer> size ) {
        NullUtil.nullArgCheck( size, "size", Val.class );
        NullUtil.nullPropertyCheck( size, "size", "Null is not a valid divider size." );
        return _withOnShow( size, (thisComponent,it) -> {
                    thisComponent.setDividerSize(UI.scale(it));
                })
                ._with( thisComponent -> {
                    thisComponent.setDividerSize( UI.scale(size.orElseThrowUnchecked()) );
                })
                ._this();
    }

    private void _calculateDividerLocationFrom( P p, double percentage ) {
        int loc = (int) (
                        p.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                            ? p.getWidth()  * percentage
                            : p.getHeight() * percentage
                    );
        p.setDividerLocation(loc);
    }

    private double _calculatePercentageFrom( P p ) {
        return p.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                ? (double) p.getDividerLocation() / p.getWidth()
                : (double) p.getDividerLocation() / p.getHeight();
    }

    /**
     * Sets the location of the divider based on a percentage value.
     * So if the split pane split is aligned horizontally, the divider
     * will be set to the percentage of the height of the split pane.
     * If the split pane is aligned vertically, the divider will be set
     * to the percentage of the width of the split pane.
     * <p>
     * Note that a component listener is installed to the split pane's size temporarily,
     * so that the divider location can be calculated when the split pane is sized
     * by the layout manager for the first time.
     * This is because before the layout manager did its thing, there was no way to know the actual
     * location of the divider based on the percentage.
     * <b>
     *     So keep in mind that changes to the divider location immediately after this
     *     method is called will be overridden by said listener!
     * </b>
     * <p>
     * A change of the divider location is ultimately passed off to the
     * look and feel implementation, where listeners are then notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via {@link JSplitPane#setLastDividerLocation(int)}.
     *
     * @param percentage A double value between 0 and 1, representing the percentage of the split pane's
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDivisionOf( double percentage ) {
        return _with( thisComponent -> {
                    _calculateDividerLocationFrom(thisComponent, percentage);
                    /*
                        Before the layout manager did its thing, there was no way to know the actual
                        location of the divider.
                        So we install a listener to the split pane's size, so that we can recalculate
                        the divider location when the split pane is resized.
                        Then it removes itself after the first time it's called.
                     */
                    thisComponent.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentResized( ComponentEvent e ) {
                            _calculateDividerLocationFrom(thisComponent, percentage);
                            thisComponent.removeComponentListener(this);
                        }
                    });
                })
                ._this();
    }

    /**
     * Updates the location of the divider based on a percentage property which means
     * that if the split pane split is aligned horizontally, the divider
     * will be set to the percentage of the height of the split pane and
     * if the split pane is aligned vertically, the divider will be set
     * to the percentage of the width of the split pane.
     * <p>
     * This method binds the property uni-directionally,
     * which means that the property will be observed by the
     * split pane, but the split pane will not change the property
     * (see {@link #withDivisionOf(Var)} for a bidirectional variant).
     * <p>
     * A change of the divider location is ultimately passed off to the
     * look and feel implementation, where listeners are then notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via {@link JSplitPane#setLastDividerLocation(int)}.
     * <p>
     *     Note that the percentage is calculated based on the split pane's
     *     current size, so if the split pane is resized, the divider location
     *     will be recalculated in order to honor the percentage.
     * </p>
     * @param percentage A property dynamically determining a double value between 0 and 1, representing the percentage of the split pane's
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDivisionOf( Val<Double> percentage ) {
        NullUtil.nullArgCheck( percentage, "percentage", Val.class );
        NullUtil.nullPropertyCheck( percentage, "percentage", "Null is not a valid percentage." );
        return _withOnShow( percentage, (thisComponent,v) -> {
                    _calculateDividerLocationFrom(thisComponent, v);
               })
                ._with( thisComponent -> {
                    // Now we need to register a listener to the split pane's size, so that we can recalculate the divider location
                    // when the split pane is resized:
                    thisComponent.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentResized( ComponentEvent e ) {
                            _calculateDividerLocationFrom(thisComponent, percentage.orElseThrowUnchecked());
                        }
                    });
                    _calculateDividerLocationFrom(thisComponent, percentage.orElseThrowUnchecked());
                })
                ._this();
    }

    /**
     * Updates the location of the divider based on a percentage property which means
     * that if the split pane split is aligned horizontally, the divider
     * will be set to the percentage of the height of the split pane.
     * If, however, the split pane is aligned vertically, then the divider will be set
     * to the percentage of the width of the split pane.
     * <p>
     * Note that this binds the property to the location of the divider
     * bidirectionally, which means that the value inside the property will be updated when the
     * divider location is changed by the user and the divider location will be updated when the
     * property changes in the business logic. <br>
     * <p>
     * A change of the divider location is ultimately passed off to the
     * look and feel implementation, where listeners are then notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via {@link JSplitPane#setLastDividerLocation(int)}.
     * <p>
     *     Note that the percentage is calculated based on the split pane's
     *     current size, so if the split pane is resized, the divider location
     *     will be recalculated.
     * <p>
     * @param percentage A property dynamically determining a double value between 0 and 1, representing the percentage of the split pane's
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDivisionOf( Var<Double> percentage ) {
        NullUtil.nullArgCheck( percentage, "percentage", Var.class );
        NullUtil.nullPropertyCheck( percentage, "percentage", "Null is not a valid percentage." );
        return _withOnShow( percentage, (thisComponent,v) -> {
                    _calculateDividerLocationFrom(thisComponent, v);
               })
                ._with( thisComponent -> {
                    _calculateDividerLocationFrom(thisComponent, percentage.orElseThrowUnchecked());
                    // Now we need to register a listener to the split pane's size, so that we can recalculate the divider location
                    // when the split pane is resized:
                    thisComponent.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentResized( ComponentEvent e ) {
                            _calculateDividerLocationFrom(thisComponent, percentage.orElseThrowUnchecked());
                        }
                    });
                    // We listen for slider movement as well, so that we can recalculate the divider location
                    thisComponent.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
                        if ( evt.getNewValue() != null ) {
                            double newPercentage = _calculatePercentageFrom(thisComponent);
                            percentage.set(newPercentage);
                        }
                    });
                })
                ._this();
    }

}

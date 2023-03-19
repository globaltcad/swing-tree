package swingtree.animation;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

class Animator
{
    private final Component _component;
    private final Schedule _schedule;
    private final StopCondition _condition;
    private final Animation _animation;


    Animator(
            Component component,
            Schedule schedule,
            StopCondition condition,
            Animation animation
    ) {
        _component = component;
        _schedule  = Objects.requireNonNull(schedule);
        _condition = Objects.requireNonNull(condition);
        _animation = Objects.requireNonNull(animation);
    }

    private AnimationState _createState( long now, ActionEvent event ) {
        long duration = _schedule.getDurationIn(TimeUnit.MILLISECONDS);
        long howLongIsRunning = Math.max(0, now - _schedule.getStartTimeIn(TimeUnit.MILLISECONDS));
        long howLongCurrentLoop = howLongIsRunning % duration;
        long howManyLoops       = howLongIsRunning / duration;
        double progress         = howLongCurrentLoop / (double) duration;
        return new AnimationState() {
            @Override public double progress() { return progress; }
            @Override public long currentIteration() { return howManyLoops; }
            @Override public Schedule schedule() { return _schedule; }
            @Override public ActionEvent event() { return event; }
        };
    }

    boolean run( long now, ActionEvent event )
    {
        if ( now < _schedule.getStartTimeIn(TimeUnit.MILLISECONDS) )
            return true;

        AnimationState state = _createState(now, event);

        boolean shouldContinue = false;

        try {
            shouldContinue = _condition.check(state);
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        if ( !shouldContinue ) {
            try {
                _animation.finish(state); // An animation may want to do something when it is finished (e.g. reset the component to its original state).
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            return false;
        }

        try {
            _animation.run(state);
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        if ( _component != null ) {
            _component.revalidate();
            _component.repaint();
        }

        return true;
    }
}

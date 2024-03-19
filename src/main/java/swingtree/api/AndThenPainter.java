package swingtree.api;

import org.slf4j.Logger;

import java.awt.Graphics2D;
import java.util.Objects;

final class AndThenPainter implements Painter
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AndThenPainter.class);

    private final Painter _before;
    private final Painter _after;

    AndThenPainter( Painter first, Painter second ) {
        _before = Objects.requireNonNull(first);
        _after  = Objects.requireNonNull(second);
    }

    @Override
    public void paint( Graphics2D g2d ) {
        try {
            _before.paint(g2d);
        } catch ( Exception e ) {
            log.error("Exception in painter: "+_before, e);
            // Exceptions inside a painter should not be fatal.
        }
        try {
            _after.paint(g2d);
        } catch ( Exception e ) {
            log.error("Exception in painter: "+_after, e);
            // Exceptions inside a painter should not cripple the rest of the painting.
        }
        /*
             Note that if any exceptions happen in the above Painter implementations,
             then we don't want to mess up the execution of the rest of the component painting...
             Therefore, we catch any exceptions that happen in the above code.

             Ideally this would be logged in the logging framework of a user of the SwingTree
             library, but we don't know which logging framework that is, so we just print
             the stack trace to the console so that developers can see what went wrong.

             Hi there! If you are reading this, you are probably a developer using the SwingTree
             library, thank you for using it! Good luck finding out what went wrong! :)
        */
    }

    @Override
    public boolean canBeCached() {
        return _before.canBeCached() && _after.canBeCached();
    }

    @Override
    public String toString() {
        return _before + " andThen " + _after;
    }

    @Override
    public int hashCode() {
        int result = _before.hashCode();
        result = 31 * result + _after.hashCode();
        return result;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;
        AndThenPainter that = (AndThenPainter) o;
        return _before.equals(that._before) && _after.equals(that._after);
    }

}

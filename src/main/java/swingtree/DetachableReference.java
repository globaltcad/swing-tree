package swingtree;

import java.lang.ref.WeakReference;

/**
 * A strong reference to the component (This is only used to prevent the component from being garbage collected)
 * which can be detached to allow the component to be garbage collected.
 *
 * @param <T> The type of the component.
 */
final class DetachableReference<T> extends WeakReference<T> {
    private T _strongRef; // This is only used to prevent the component from being garbage collected

    public DetachableReference( T referent ) {
        super(referent);
        _strongRef = referent;
    }

    public void detach() {
        _strongRef = null;
    }
}

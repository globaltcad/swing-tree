package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.From;
import sprouts.Viewable;
import swingtree.style.ComponentExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

/**
 *  A custom event dispatcher for mouse enter and exit events based on the mouse
 *  cursor location exiting or entering a component's bounds even if the
 *  mouse cursor is on a child component.<br>
 *  This is in essence a fix for the default Swing behavior which also dispatches enter/exit
 *  events when the mouse cursor enters or exits the bounds of a child component
 *  which also has a listener for these events. So using this we try to
 *  make the behavior more predictable, reliable and intuitive.
 */
@SuppressWarnings("EnumOrdinal")
final class EnterExitComponentBoundsEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EnterExitComponentBoundsEventDispatcher.class);
    private static final MouseListener EMPTY_NO_OP_MOUSE_LISTENER = new MouseAdapter() { };
    private static @Nullable EnterExitComponentBoundsEventDispatcher INSTANCE = null;

    @SuppressWarnings("FieldCanBeLocal")
    private final Viewable<AWTEvent> mouseEventListener;// IMPORTANT: We need to keep a reference to prevent the binding from being garbage collected!
    @SuppressWarnings("FieldCanBeLocal")
    private final Viewable<AWTEvent> mouseMotionEventListener;// IMPORTANT: We need to keep a reference to prevent the binding from being garbage collected!

    private EnterExitComponentBoundsEventDispatcher() {
        this.mouseEventListener = SwingTree.get().getAwtEventView(AWTEvent.MOUSE_EVENT_MASK);
        this.mouseMotionEventListener = SwingTree.get().getAwtEventView(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        this.mouseEventListener.onChange(From.ALL, it -> it.currentValue().ifPresent(this::onMouseEvent));
        this.mouseMotionEventListener.onChange(From.ALL, it -> it.currentValue().ifPresent(this::onMouseEvent));
    }

    static EnterExitComponentBoundsEventDispatcher get() {
        if ( INSTANCE == null ) {
            INSTANCE = new EnterExitComponentBoundsEventDispatcher();
        }
        return INSTANCE;
    }

    static void clear() {
        INSTANCE = null; // This is called so that we can get new 'Viewable' instances from the 'SwingTree' library context.
    }

    void addMouseEnterListener(UI.ComponentArea area, JComponent component, MouseListener listener) {
        component.addMouseListener(EMPTY_NO_OP_MOUSE_LISTENER); // <- ensures that mouse events are enabled
        ComponentEnterExitListeners listeners = fetchListenersInitialized(component)[area.ordinal()];
        listeners.addEnterListener(listener);
    }

    void addMouseExitListener(UI.ComponentArea area, JComponent component, MouseListener listener) {
        component.addMouseListener(EMPTY_NO_OP_MOUSE_LISTENER); // <- ensures that mouse events are enabled
        ComponentEnterExitListeners listeners = fetchListenersInitialized(component)[area.ordinal()];
        listeners.addExitListener(listener);
    }

    private static ComponentEnterExitListeners[] fetchListenersInitialized(JComponent component) {
        Object foundObject = component.getClientProperty(ComponentEnterExitListeners.class);
        if ( !(foundObject instanceof ComponentEnterExitListeners[]) ) {
            foundObject = EnterExitComponentBoundsEventDispatcher.iniListeners();
            component.putClientProperty(ComponentEnterExitListeners.class, foundObject);
        }
        return (ComponentEnterExitListeners[]) foundObject;
    }

    private static ComponentEnterExitListeners[] iniListeners() {
        ComponentEnterExitListeners[] listenerArray = new ComponentEnterExitListeners[UI.ComponentArea.values().length];
        for (UI.ComponentArea a : UI.ComponentArea.values()) {
            listenerArray[a.ordinal()] = new ComponentEnterExitListeners(a);
        }
        return listenerArray;
    }

    private void onMouseEvent(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;

            Component eventComponent = mouseEvent.getComponent();
            while ( eventComponent instanceof JComponent ) {
                JComponent toReceiveEvent = (JComponent) eventComponent;
                ComponentEnterExitListeners[] componentListenerInfo = fetchListenersInitialized(toReceiveEvent);
                if (componentListenerInfo != null) {
                    for (UI.ComponentArea area : UI.ComponentArea.values()) {
                        ComponentEnterExitListeners currentListeners = componentListenerInfo[area.ordinal()];
                        currentListeners.dispatch(toReceiveEvent, mouseEvent);
                    }
                }
                eventComponent = toReceiveEvent.getParent();
            }
        }
    }

    enum Location {
        INSIDE, OUTSIDE
    }

    /**
     *  Contains the enter and exit listeners for a component as well as
     *  a flag indicating whether the mouse cursor is currently within the
     *  bounds of the component or not.
     */
    private static class ComponentEnterExitListeners {
        private final UI.ComponentArea area;
        private Location location;
        private final List<MouseListener> enterListeners;
        private final List<MouseListener> exitListeners;

        public ComponentEnterExitListeners( UI.ComponentArea area ) {
            this.area = area;
            this.location = Location.OUTSIDE;
            this.enterListeners = new ArrayList<>();
            this.exitListeners  = new ArrayList<>();
        }

        private final boolean areaIsEqualToBounds(Component component) {
            if ( this.area == UI.ComponentArea.ALL ) {
                return true;
            }
            Shape shape = ComponentExtension.from((JComponent) component).getComponentArea(area).orElse(null);
            return shape != null && shape.equals(component.getBounds());
        }

        public void addEnterListener(MouseListener listener) {
            enterListeners.add(listener);
        }

        public void addExitListener(MouseListener listener) {
            exitListeners.add(listener);
        }

        public void dispatch(JComponent component, MouseEvent event) {
            assert isRelated(component, event.getComponent());

            switch (event.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                    if (location == Location.INSIDE)
                        return;

                    location = onMouseEnter(component, event);
                    break;
                case MouseEvent.MOUSE_EXITED:
                    if (location == Location.OUTSIDE)
                        return;

                    if (containsScreenLocation(component, event.getLocationOnScreen()))
                        return;

                    location = onMouseExit(component, event);
                    break;
                case MouseEvent.MOUSE_MOVED:
                    if ( !areaIsEqualToBounds(component) ) {
                        if ( location == Location.INSIDE ) {
                            location = onMouseExit(component, event);
                        } else if ( location == Location.OUTSIDE ) {
                            location = onMouseEnter(component, event);
                        }
                    }
                break;
            }
        }

        private Location determineCurrentLocationOf(MouseEvent event) {
            return ComponentExtension.from((JComponent) event.getComponent())
                    .getComponentArea(area)
                    .filter( shape -> shape.contains(event.getPoint()) )
                    .map( isInsideShape -> Location.INSIDE )
                    .orElse(Location.OUTSIDE);
        }

        private Location onMouseEnter(JComponent component, MouseEvent mouseEvent)
        {
            mouseEvent = withNewSource(mouseEvent, component);

            if (enterListeners.isEmpty())
                return determineCurrentLocationOf(mouseEvent);

            if ( areaIsEqualToBounds(component) ) {
                dispatchEnterToAllListeners(mouseEvent);
                return Location.INSIDE;
            } else {
                Location nextLocation = determineCurrentLocationOf(mouseEvent);
                if ( nextLocation == Location.INSIDE && this.location == Location.OUTSIDE )
                    dispatchEnterToAllListeners(mouseEvent);

                return nextLocation;
            }
        }

        private void dispatchEnterToAllListeners(MouseEvent mouseEvent) {
            for ( MouseListener listener : enterListeners ) {
                try {
                    listener.mouseEntered(mouseEvent);
                } catch (Exception e) {
                    log.error(SwingTree.get().logMarker(), "Failed to process mouseEntered event {}. Error: {}", mouseEvent, e.getMessage(), e);
                }
            }
        }

        private Location onMouseExit(JComponent component, MouseEvent mouseEvent)
        {
            mouseEvent = withNewSource(mouseEvent, component);

            if (exitListeners.isEmpty())
                return determineCurrentLocationOf(mouseEvent);

            if ( areaIsEqualToBounds(component) ) {
                dispatchExitToAllListeners(mouseEvent);
                return Location.OUTSIDE;
            } else {
                Location nextLocation = determineCurrentLocationOf(mouseEvent);
                if ( nextLocation == Location.OUTSIDE && this.location == Location.INSIDE )
                    dispatchExitToAllListeners(mouseEvent);

                return nextLocation;
            }
        }

        private void dispatchExitToAllListeners(MouseEvent mouseEvent) {
            for ( MouseListener listener : exitListeners ) {
                try {
                    listener.mouseExited(mouseEvent);
                } catch (Exception e) {
                    log.error(SwingTree.get().logMarker(), "Failed to process mouseExited event {}. Error: {}", mouseEvent, e.getMessage(), e);
                }
            }
        }
    }

    private static boolean isRelated(Component parent, Component other) {
        Component o = other;
        while (o != null) {
            if (o == parent)
                return true;
            o = o.getParent();
        }
        return false;
    }

    private static boolean containsScreenLocation(Component component, Point screenLocation) {
        if (!component.isShowing())
            return false;
        Point compLocation = component.getLocationOnScreen();
        Dimension compSize = component.getSize();
        int relativeX = screenLocation.x - compLocation.x;
        int relativeY = screenLocation.y - compLocation.y;
        return (relativeX >= 0 && relativeX < compSize.width && relativeY >= 0 && relativeY < compSize.height);
    }

    private static MouseEvent withNewSource(MouseEvent event, JComponent newSource) {
        if ( event.getSource() == newSource )
            return event;

        // We need to convert the mouse position to the new source's coordinate system
        Point mousePositionRelativeToNewComponent = event.getPoint();
        SwingUtilities.convertPointToScreen(mousePositionRelativeToNewComponent, (Component) event.getSource());
        SwingUtilities.convertPointFromScreen(mousePositionRelativeToNewComponent, newSource);
        return new MouseEvent(
            newSource,
            event.getID(),
            event.getWhen(),
            event.getModifiersEx(),
            mousePositionRelativeToNewComponent.x,
            mousePositionRelativeToNewComponent.y,
            event.getClickCount(),
            event.isPopupTrigger(),
            event.getButton()
        );
    }

}

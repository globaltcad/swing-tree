package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *  A custom event dispatcher for mouse enter and exit events based on the mouse
 *  cursor location exiting or entering a component's bounds even if the
 *  mouse cursor is on a child component.<br>
 *  This is in essence a fix for the default Swing behavior which also dispatches enter/exit
 *  events when the mouse cursor enters or exits the bounds of a child component
 *  which also has a listener for these events. So using this we try to
 *  make the behavior more predictable, reliable and intuitive.
 */
final class EnterExitComponentBoundsEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EnterExitComponentBoundsEventDispatcher.class);
    private static final EnterExitComponentBoundsEventDispatcher eventDispatcher = new EnterExitComponentBoundsEventDispatcher();
    private static final MouseListener dispatcherListener = new MouseAdapter() { };

    static void addMouseEnterListener(Component component, MouseListener listener) {
        ComponentListeners listeners = eventDispatcher.listeners.computeIfAbsent(component, k -> {
            // ensures that mouse events are enabled
            k.addMouseListener(dispatcherListener);
            return new ComponentListeners();
        });
        listeners.addEnterListener(listener);
    }

    static void addMouseExitListener(Component component, MouseListener listener) {
        ComponentListeners listeners = eventDispatcher.listeners.computeIfAbsent(component, k -> {
            // ensures that mouse events are enabled
            k.addMouseListener(dispatcherListener);
            return new ComponentListeners();
        });
        listeners.addExitListener(listener);
    }


    private final Map<Component, ComponentListeners> listeners;

    private EnterExitComponentBoundsEventDispatcher() {
        listeners = new WeakHashMap<>();
        Toolkit.getDefaultToolkit().addAWTEventListener(this::onMouseEvent, AWTEvent.MOUSE_EVENT_MASK);
    }

    public void onMouseEvent(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;

            Component c = mouseEvent.getComponent();
            while (c != null) {
                ComponentListeners componentListenerInfo = listeners.get(c);

                if (componentListenerInfo != null)
                    componentListenerInfo.dispatch(c, mouseEvent);

                c = c.getParent();
            }
        }
    }

    private static class ComponentListeners {
        private boolean isEntered;
        private final List<MouseListener> enterListeners;
        private final List<MouseListener> exitListeners;

        public ComponentListeners() {
            isEntered = false;
            enterListeners = new ArrayList<>();
            exitListeners = new ArrayList<>();
        }

        public void addEnterListener(MouseListener listener) {
            enterListeners.add(listener);
        }

        public void addExitListener(MouseListener listener) {
            exitListeners.add(listener);
        }

        public void dispatch(Component component, MouseEvent event) {
            assert isRelated(component, event.getComponent());

            switch (event.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                    if (isEntered)
                        return;

                    onMouseEnter(component, event);

                    isEntered = true;
                    break;
                case MouseEvent.MOUSE_EXITED:
                    if (!isEntered)
                        return;

                    if (containsScreenLocation(component, event.getLocationOnScreen()))
                        return;

                    onMouseExit(component, event);

                    isEntered = false;
                    break;
            }
        }

        private void onMouseEnter(Component component, MouseEvent mouseEvent) {
            if (enterListeners.isEmpty())
                return;

            MouseEvent localMouseEvent = withNewSource(mouseEvent, component);
            for (MouseListener listener : enterListeners) {
                try {
                    listener.mouseEntered(localMouseEvent);
                } catch (Exception e) {
                    log.error("Failed to process mouseEntered event {}. Error: {}", localMouseEvent, e.getMessage(), e);
                }
            }
        }

        private void onMouseExit(Component component, MouseEvent mouseEvent) {
            if (exitListeners.isEmpty())
                return;

            MouseEvent localMouseEvent = withNewSource(mouseEvent, component);
            for (MouseListener listener : exitListeners) {
                try {
                    listener.mouseExited(localMouseEvent);
                } catch (Exception e) {
                    log.error("Failed to process mouseExited event {}. Error: {}", localMouseEvent, e.getMessage(), e);
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
        Point compLocation = component.getLocationOnScreen();
        Dimension compSize = component.getSize();
        int relativeX = screenLocation.x - compLocation.x;
        int relativeY = screenLocation.y - compLocation.y;
        return (relativeX >= 0 && relativeX < compSize.width && relativeY >= 0 && relativeY < compSize.height);
    }

    private static MouseEvent withNewSource(MouseEvent event, Component newSource) {
        return new MouseEvent(
            newSource,
            event.getID(),
            event.getWhen(),
            event.getModifiersEx(),
            event.getX(),
            event.getY(),
            event.getClickCount(),
            event.isPopupTrigger(),
            event.getButton()
        );
    }

}

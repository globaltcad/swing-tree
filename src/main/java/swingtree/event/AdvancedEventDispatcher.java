package swingtree.event;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

public class AdvancedEventDispatcher {

    private static final AdvancedEventDispatcher eventDispatcher = new AdvancedEventDispatcher();

    public static void addMouseEnterListener(Component component, MouseListener listener) {
        List<MouseListener> listeners = eventDispatcher.enterListeners.computeIfAbsent(component, k -> new ArrayList<>());
        listeners.add(listener);
    }

    public static void addMouseExitListener(Component component, MouseListener listener) {
        List<MouseListener> listeners = eventDispatcher.exitListeners.computeIfAbsent(component, k -> new ArrayList<>());
        listeners.add(listener);
    }

    private final Map<Component, List<MouseListener>> enterListeners;
    private final Map<Component, List<MouseListener>> exitListeners;

    private AdvancedEventDispatcher() {
        enterListeners = new HashMap<>();
        exitListeners = new HashMap<>();

        Toolkit.getDefaultToolkit().addAWTEventListener(this::onMouseEvent, AWTEvent.MOUSE_EVENT_MASK);
    }

    public void onMouseEvent(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;

            switch (mouseEvent.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                    onMouseEnter(mouseEvent);
                    break;
                case MouseEvent.MOUSE_EXITED:
                    onMouseExit(mouseEvent);
                    break;
            }

        }
    }

    private void onMouseEnter(MouseEvent mouseEvent) {
        Component component = mouseEvent.getComponent().getParent();

        while (component != null) {
            List<MouseListener> listeners = enterListeners.get(component);

            if (listeners != null) {
                for (MouseListener listener : listeners) {
                    listener.mouseEntered(mouseEvent);
                }
            }

            component = component.getParent();
        }
    }

    private void onMouseExit(MouseEvent mouseEvent) {
        Component component = mouseEvent.getComponent().getParent();

        while (component != null && !containsScreenLocation(component, mouseEvent.getLocationOnScreen())) {
            List<MouseListener> listeners = exitListeners.get(component);

            if (listeners != null) {
                for (MouseListener listener : listeners) {
                    listener.mouseExited(mouseEvent);
                }
            }

            component = component.getParent();
        }
    }

    public static boolean containsScreenLocation(Component component, Point screenLocation) {
        Point compLocation = component.getLocationOnScreen();
        Dimension compSize = component.getSize();
        int relativeX = screenLocation.x - compLocation.x;
        int relativeY = screenLocation.y - compLocation.y;
        return (relativeX >= 0 && relativeX < compSize.width && relativeY >= 0 && relativeY < compSize.height);
    }
}

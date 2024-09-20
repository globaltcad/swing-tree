package swingtree.components;


import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import swingtree.style.ComponentExtension;
import swingtree.style.StylableComponent;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

import static java.awt.AWTEvent.*;
import static java.awt.event.MouseEvent.*;
import static javax.swing.SwingUtilities.*;

/**
 *  A more advanced glass pane implementation than the default Swing
 *  glass pane of a {@link JRootPane} object (A regular {@link JPanel}.
 *  In contrast to the default glass pane ({@link JPanel}) of a {@link JRootPane},
 *  this pane <b>handles any mouse events, without interrupting the controls underneath
 *  the glass pane (in the content pane of the root pane)</b>.
 *  Also, cursors are handled as if the glass pane was invisible
 *  (if no cursor gets explicitly set to the glass pane).
 */
public class JGlassPane extends JPanel implements AWTEventListener, StylableComponent
{
    private static final long serialVersionUID = 1L;

    private final EventListenerList listeners = new EventListenerList();

    protected @Nullable JRootPane rootPane;
    private ActiveDrag activeDrag = ActiveDrag.none();


    public JGlassPane() {
        setLayout(new MigLayout("fill, ins 0"));
        Toolkit.getDefaultToolkit()
                .addAWTEventListener(
                    this,
                    MOUSE_WHEEL_EVENT_MASK | MOUSE_MOTION_EVENT_MASK | MOUSE_EVENT_MASK
                );
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                activeDrag = activeDrag.dragged(e, rootPane);
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                activeDrag = activeDrag.begin(e, rootPane);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                activeDrag.tryDropping(e, rootPane);
                activeDrag = ActiveDrag.none();
                if ( rootPane != null ) {
                    rootPane.repaint();
                }
            }
        });
    }

    public JGlassPane(JRootPane rootPane) {
        this();
        Objects.requireNonNull(rootPane);
        attachToRootPane(rootPane);
    }

    /** {@inheritDoc} */
    @Override public void paintComponent(Graphics g){
        paintBackground(g, super::paintComponent);
    }

    /** {@inheritDoc} */
    @Override public void paintChildren(Graphics g) {
        paintForeground(g, super::paintChildren);
        activeDrag.paint(g);
    }

    @Override public void setUISilently( ComponentUI ui ) {
        this.ui = ui;
    }

    /**
     * Resets the UI property with a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    @Override
    public void updateUI() {
        ComponentExtension.from(this).installCustomUIIfPossible();
        /*
            The JGlassPane is a SwingTree native type, so it also
            enjoys the perks of having a SwingTree based look and feel!
        */
    }

    protected void attachToRootPane(JRootPane rootPane) {
        Objects.requireNonNull(rootPane);
        if ( this.rootPane != null ) this.detachFromRootPane(this.rootPane);
        this.setOpaque(false);
        ( this.rootPane = rootPane ).setGlassPane(this);
        this.setVisible(true);
    }

    protected void detachFromRootPane( @Nullable JRootPane rootPane ) {
        Objects.requireNonNull(rootPane);
        if ( rootPane.getGlassPane() == this ) {
            rootPane.setGlassPane(null);
            setVisible(false);
        }
    }

    public void toRootPane(@Nullable JRootPane pane) {
        if( pane != null )
            attachToRootPane(pane);
        else
            detachFromRootPane(rootPane);
    }

    @Override public final synchronized MouseListener[] getMouseListeners() {
        return listeners.getListeners(MouseListener.class);
    }
    @Override public final synchronized void addMouseListener(MouseListener listener) {
        listeners.add(MouseListener.class,listener);
    }
    @Override public final synchronized void removeMouseListener(MouseListener listener) {
        listeners.remove(MouseListener.class,listener);
    }

    @Override public final synchronized MouseMotionListener[] getMouseMotionListeners() {
        return listeners.getListeners(MouseMotionListener.class);
    }
    @Override public final synchronized void addMouseMotionListener(MouseMotionListener listener) {
        listeners.add(MouseMotionListener.class,listener);
    }
    @Override public final synchronized void removeMouseMotionListener(MouseMotionListener listener) {
        listeners.remove(MouseMotionListener.class,listener);
    }

    @Override public final synchronized MouseWheelListener[] getMouseWheelListeners() {
        return listeners.getListeners(MouseWheelListener.class);
    }
    @Override public final synchronized void addMouseWheelListener(MouseWheelListener listener) {
        listeners.add(MouseWheelListener.class,listener);
    }
    @Override public final synchronized void removeMouseWheelListener(MouseWheelListener listener) {
        listeners.remove(MouseWheelListener.class,listener);
    }

    @Override
    public void eventDispatched( AWTEvent event ) {
        if ( rootPane != null && event instanceof MouseEvent ) {
            MouseEvent mouseEvent = (MouseEvent)event, newMouseEvent;

            Object source = event.getSource();
            if ( source instanceof Component ) {
                Component sourceComponent = (Component) source;
                if ( SwingUtilities.getRootPane(sourceComponent) != rootPane )
                    return; //it's not our root pane (e.g. different window)

                /* change source and coordinate system of event to glass pane, DON'T use setSource on AWTEvent's! */
                newMouseEvent = convertMouseEvent(sourceComponent, mouseEvent, this);
            } else newMouseEvent = convertMouseEvent(null, mouseEvent, this);

            switch( event.getID() ) {
                case MOUSE_CLICKED:
                    for(MouseListener listener:listeners.getListeners(MouseListener.class))
                        listener.mouseClicked(newMouseEvent);
                    break;
                case MOUSE_PRESSED:
                    for(MouseListener listener:listeners.getListeners(MouseListener.class))
                        listener.mousePressed(newMouseEvent);
                    break;
                case MOUSE_RELEASED:
                    for(MouseListener listener:listeners.getListeners(MouseListener.class))
                        listener.mouseReleased(newMouseEvent);
                    break;
                case MOUSE_MOVED:
                    for(MouseMotionListener listener:listeners.getListeners(MouseMotionListener.class))
                        listener.mouseMoved(newMouseEvent);
                    break;
                case MOUSE_ENTERED:
                    for(MouseListener listener:listeners.getListeners(MouseListener.class))
                        listener.mouseEntered(newMouseEvent);
                    break;
                case MOUSE_EXITED:
                    for(MouseListener listener:listeners.getListeners(MouseListener.class))
                        listener.mouseExited(newMouseEvent);
                    break;
                case MOUSE_DRAGGED:
                    for(MouseMotionListener listener:listeners.getListeners(MouseMotionListener.class))
                        listener.mouseDragged(newMouseEvent);
                    break;
                case MOUSE_WHEEL:
                    for(MouseWheelListener listener:listeners.getListeners(MouseWheelListener.class))
                        listener.mouseWheelMoved((MouseWheelEvent)newMouseEvent);
                    break;
            }

            /* consume the original mouse event, if the new mouse event was consumed */
            if ( newMouseEvent.isConsumed() )
                mouseEvent.consume();
        }
    }


    /**
     * If someone sets a new cursor to the GlassPane
     * we expect that they know what they are doing
     * and return the super.contains(x,y)
     * otherwise we return false to respect the cursors
     * for the underneath components
     */
    @Override
    public boolean contains(int x, int y) {
        if ( rootPane == null )
            return false;
        Container container = rootPane.getContentPane();
        Point containerPoint = convertPoint(this, x, y, container);
        if ( containerPoint.y > 0 ) {
            Component component = getDeepestComponentAt(
                                        container,
                                        containerPoint.x,
                                        containerPoint.y
                                    );

            return component == null || component.getCursor() == Cursor.getDefaultCursor();
        }
        else return true;
    }
}

package swingtree.components;


import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.From;
import swingtree.ComponentDelegate;
import swingtree.DragAwayComponentConf;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.layout.Bounds;
import swingtree.layout.Position;
import swingtree.layout.Size;
import swingtree.style.ComponentExtension;
import swingtree.style.StylableComponent;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

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
 *  (if no cursor gets explicitly set to the glass pane).<br>
 *  <p>
 *      This glass pane is also the backbone of how SwingTree renders its
 *      drag and drop operations. It renders an illustration of what is being dragged
 *      on top of the entire component hierarchy of the root pane.<br>
 *      Through this class SwingTree also draws the overlays for the GUI debug dev tool.
 *      The debug dev tool can be enabled by calling {@link SwingTree#setDevToolEnabled(boolean)} or more commonly
 *      by pressing the shortcut Ctrl+Shift+I (similar to the shortcut for the dev tools in web browsers).
 *  </p><br>
 *  <p>
 *      The {@link #isVisible()} flag of this glass pane is self-managed based on
 *      the existence of active named paint jobs registered through the {@link #setPaintJobWithId(String, Painter)} method.
 *      So if there is at least one named paint job registered, then the glass pane will be visible,
 *      and if there are no active named paint jobs, then the glass pane will be invisible.
 *      Paint jobs can be removed by their id through the {@link #removePaintJobWithId(String)} method.<br>
 *      Both the drag and drop visualization and the GUI debug dev tool overlays are implemented as named paint
 *      jobs on this glass pane, so they will automatically show and hide the glass pane when they are active or inactive. <br>
 *  </p>
 *  <p>
 *      Note that both {@link #paintComponent(Graphics)} and {@link #paintChildren(Graphics)} methods are final
 *      and cannot be overridden by subclasses of this glass pane. To do custom painting on this glass pane,
 *      you must do so through the {@link #setPaintJobWithId(String, Painter)} method.<br>
 *      This is important to ensure that existing functionality can not break!
 *  </p>
 */
public class JGlassPane extends JPanel implements StylableComponent
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JGlassPane.class);
    private static final Map<JGlassPane, ActiveDrag> activeDrags = new WeakHashMap<>();

    private final EventListenerList listeners = new EventListenerList();
    private final Map<String, Painter> activePaintJobs = new LinkedHashMap<>();
    protected @Nullable JRootPane rootPane;


    public JGlassPane() {
        this.setVisible(false);
        setLayout(new MigLayout("fill, ins 0"));
        // Binding:
        putClientProperty(UUID.randomUUID(), // IMPORTANT: We need to keep a reference to prevent the binding from being garbage collected!
            SwingTree.get().getAwtEventView(
                    MOUSE_WHEEL_EVENT_MASK | MOUSE_MOTION_EVENT_MASK | MOUSE_EVENT_MASK
                )
                .onChange(From.ALL, it -> {
                    it.currentValue().ifPresent(this::eventDispatched);
                })
        );
        putClientProperty(UUID.randomUUID(), // IMPORTANT: We need to keep a reference to prevent the binding from being garbage collected!
            SwingTree.get().isDevToolEnabledView()
                .onChange(From.ALL, it -> {
                    if ( rootPane != null ) {
                        if (it.currentValue().is(true)) {
                            GuiDebugDevToolUtility.initializeDebugToolFor(rootPane);
                            rootPane.repaint();
                            setPaintJobWithId("devToolOverlay", g2d -> {
                                if (rootPane != null)
                                    GuiDebugDevToolUtility.paintDebugOverlay(g2d, this);
                            });
                        } else {
                            rootPane.repaint();
                            removePaintJobWithId("devToolOverlay");
                        }
                    }
                })
        );

        final DragSource dragSource = DragSource.getDefaultDragSource();
        DragGestureRecognizer[] gestureRecognizer = {null};
        gestureRecognizer[0] = dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, (dragTrigger) -> {
            ActiveDrag activeDrag = getActiveDrag();
            Point dragStart = dragTrigger.getDragOrigin();
            activeDrag = activeDrag.begin(dragStart, rootPane);
            if ( activeDrag.equals(ActiveDrag.none()) )
                return;
            else
                setActiveDrag(activeDrag);
            BufferedImage bufferedImage = activeDrag.currentDragImage();
            if ( !DragSource.isDragImageSupported() )
                bufferedImage = null;
            int offsetX = bufferedImage == null ? 0 : -bufferedImage.getWidth(null) / 2;
            int offsetY = bufferedImage == null ? 0 : -bufferedImage.getHeight(null) / 2;

            UI.DragAction dragAction = activeDrag.dragConf().map(DragAwayComponentConf::dragAction).orElse(UI.DragAction.NONE);

            gestureRecognizer[0].setSourceActions(dragAction.toIntCode());
            Component draggedComponent = activeDrag.draggedComponent();
            dragTrigger.startDrag(
                    activeDrag.dragConf().map(DragAwayComponentConf::cursor).map(UI.Cursor::toAWTCursor).orElse(Cursor.getDefaultCursor()),
                    bufferedImage,
                    new Point(offsetX, offsetY),
                    activeDrag.dragConf().flatMap(DragAwayComponentConf::payload).orElseGet(() -> _findRelevantDataFor(draggedComponent)),
                    new GlassPaneDragSourceListener()
            );
            activeDrag.dragConf().ifPresent(conf -> {
                try {
                    conf.onDragStart().accept(new ComponentDelegate(conf.component(), dragTrigger));
                } catch (Exception ex) {
                    log.error(SwingTree.get().logMarker(),
                            "Error while executing drag start event handlers.",
                            ex
                    );
                }
            });
        });
        dragSource.addDragSourceMotionListener(event -> {
            ActiveDrag activeDrag = getActiveDrag();

            if ( !activeDrag.equals(ActiveDrag.none()) && rootPane != null ) {
                Point dragStartPoint = determineCurrentDragDropLocationInWindow(event.getLocation(), rootPane);
                MouseEvent e = new MouseEvent(JGlassPane.this, MOUSE_DRAGGED, System.currentTimeMillis(), 0, dragStartPoint.x, dragStartPoint.y, 1, false);
                activeDrag.dragConf().ifPresent(conf -> {
                    try {
                        conf.onDragMove().accept(new ComponentDelegate(conf.component(), event));
                    } catch (Exception ex) {
                        log.error(SwingTree.get().logMarker(),
                                "Error while executing drag movement event handlers.",
                                ex
                            );
                    }
                });
                ActiveDrag previousActiveDrag = activeDrag;
                activeDrag = activeDrag.dragged(e);
                setActiveDrag(activeDrag);
                /*
                    Note that if the drag image is not supported by the platform, we
                    do the image rendering ourselves directly on the Graphics object
                    of the glass pane of the root pane.
                    But for this to work we need to repaint the area where the drag image was
                    previously rendered and the area where it will be rendered next.
                */
                repaintRootPaneFor(previousActiveDrag, activeDrag);
            }
        });
        setActiveDrag(ActiveDrag.none());
        this.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                if (rootPane != null )
                    GuiDebugDevToolUtility.processMouseMovementForLiveDebugging(JGlassPane.this, rootPane, e);
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (rootPane != null ) {
                    GuiDebugDevToolUtility.findAndSelectComponentForDebug(rootPane, e);
                }
            }
        });
    }

    public JGlassPane(JRootPane rootPane) {
        this();
        Objects.requireNonNull(rootPane);
        attachToRootPane(rootPane);
    }

    /**
     *  Schedules a permanent paint job on this glass pane with the given name/id and painter,
     *  which will be active until the {@link #removePaintJobWithId(String)} method is called with the same id.
     *  If there is already an active paint job with the same id, then this method does nothing.
     *  Note that paint jobs are executed in the order they were added...
     *  @param id The id of the paint job. It must be unique, otherwise the paint job will not be added.
     *  @param painter The painter that will be executed in the paintComponent method of this glass pane, if the paint job is active.
     */
    protected final void setPaintJobWithId( String id, Painter painter ) {
        if ( !activePaintJobs.containsKey(id) ) {
            if ( activePaintJobs.isEmpty() )
                setVisible(true);
            activePaintJobs.put(id, painter);
            repaint();
        }
    }

    /**
     *  Removes the active paint job with the given id from this glass pane, if it exists.
     *  If there is no active paint job with the given id, then this method does nothing.
     *  Note that paint jobs are executed in the order they were added...
     *  @param id The id of the paint job to be removed.
     */
    protected final void removePaintJobWithId(String id ) {
        if ( activePaintJobs.containsKey(id) ) {
            activePaintJobs.remove(id);
            if ( activePaintJobs.isEmpty() )
                setVisible(false);
            repaint();
        }
    }

    private ActiveDrag getActiveDrag() {
        return Optional.ofNullable(activeDrags.get(this)).orElse(ActiveDrag.none());
    }

    private void setActiveDrag(ActiveDrag activeDrag) {
        // Update the drag state for this glass pane.
        activeDrags.put(this, activeDrag);

        // Determine if there is at least one active drag across all glass panes.
        boolean anyActiveDrag = activeDrags.values().stream()
                .anyMatch(drag -> !drag.equals(ActiveDrag.none()));

        if ( anyActiveDrag ) {
            // Ensure that all relevant glass panes have the drag-visualization paint job
            // so that foreign drags are rendered across windows.
            for ( JGlassPane pane : activeDrags.keySet() ) {
                pane.setPaintJobWithId("activeDragVisualization", pane::paintActiveDrag);
            }
        } else {
            // No active drags anywhere: remove the visualization paint job from all panes.
            for ( JGlassPane pane : activeDrags.keySet() ) {
                pane.removePaintJobWithId("activeDragVisualization");
            }
        }
    }

    private void repaintRootPaneFor(ActiveDrag previousActiveDrag, ActiveDrag currentActiveDrag) {
        if ( rootPane == null )
            return;

        if ( !currentActiveDrag.hasDraggedComponent() )
            return;

        if ( !DragSource.isDragImageSupported() ) {
            /*
                If the drag image is not supported by the platform, we
                do the image rendering ourselves directly on the Graphics object
                of the glass pane of the root pane.
                But for this to work we need to repaint the area where the drag image was
                previously rendered and the area where it will be rendered next.
            */
            Position previousWhereToRender = previousActiveDrag.getRenderPosition();
            Position whereToRenderNext = currentActiveDrag.getRenderPosition();
            Size size = currentActiveDrag.draggedComponentSize();
            Bounds previousArea = Bounds.of(previousWhereToRender, size);
            Bounds nextArea     = Bounds.of(whereToRenderNext, size);
            Bounds mergedArea   = previousArea.merge(nextArea);
            rootPane.repaint(mergedArea.toRectangle());
            /*
                Maybe the local drag operation is currently hovering over another window.
                Let's check that and then repaint the other window's glass pane.
            */
            for (JGlassPane otherWindowGlassPane : activeDrags.keySet()) {
                if ( otherWindowGlassPane != this && otherWindowGlassPane.rootPane != this.rootPane ) {
                    if ( otherWindowGlassPane.rootPane == null )
                        continue;

                    ActiveDrag convertedPreviousActiveDrag = convertActiveDragToOtherGlassPane(this, previousActiveDrag, otherWindowGlassPane);
                    ActiveDrag convertedCurrentActiveDrag = convertActiveDragToOtherGlassPane(this, currentActiveDrag, otherWindowGlassPane);
                    Bounds otherPreviousArea = convertedPreviousActiveDrag.getBounds();
                    Bounds otherNextArea = convertedCurrentActiveDrag.getBounds();
                    Bounds otherMergedArea = otherPreviousArea.merge(otherNextArea);
                    // Let's check if the drag image is in the bounds of this glass pane
                    if ( otherWindowGlassPane.getBounds().intersects(otherMergedArea.toRectangle()) ) {
                        otherWindowGlassPane.rootPane.repaint(otherMergedArea.toRectangle());
                    }
                }
            }
        }
    }

    private Point determineCurrentDragDropLocationInWindow( Point locationOnDesktop, JRootPane rootPane ) {
        int relativeX = 0;
        int relativeY = 0;
        // First we try something reliable:
        Point mousePositionInFrame = getMousePosition();
        if ( mousePositionInFrame != null ) {
            relativeX = mousePositionInFrame.x;
            relativeY = mousePositionInFrame.y;
        } else {
            // We calculate the mouse position in the frame using the drag event
            try {
                Point rootPaneLocationOnDesktop = rootPane.getLocationOnScreen();
                relativeX = locationOnDesktop.x - rootPaneLocationOnDesktop.x;
                relativeY = locationOnDesktop.y - rootPaneLocationOnDesktop.y;
            } catch (Exception e) {
                log.debug(SwingTree.get().logMarker(), "Error while calculating the relative position of a drag.", e);
            }
        }
        return new Point(relativeX, relativeY);
    }

    /** {@inheritDoc} */
    @Override public final void paintComponent(Graphics g){
        paintBackground(g, super::paintComponent);
    }

    /** {@inheritDoc} */
    @Override public final void paintChildren(Graphics g) {
        paintForeground(g, super::paintChildren);
        for (Map.Entry<String, Painter> paintJob : activePaintJobs.entrySet()) {
            Graphics2D g2d = null;
            try {
                g2d = (Graphics2D) g.create();
                paintJob.getValue().paint(g2d);
            } catch (Exception ex) {
                log.error(SwingTree.get().logMarker(),
                    "Error while executing paint job with id '{}' on glass pane of root pane '{}'.",
                    paintJob.getKey(), rootPane, ex
                );
            } finally {
                if (g2d != null) {
                    g2d.dispose();
                }
            }
        }
    }

    private void paintActiveDrag(Graphics g) {
        getActiveDrag().paint(g);
        /*
            Maybe another window has a glass pane with an ongoing drag operation
            which is currently hovering over our window.
            Let's check that and then paint this other drag image
            on our glass pane.
        */
        for (JGlassPane otherWindowGlassPane : activeDrags.keySet()) {
            if ( otherWindowGlassPane != this && otherWindowGlassPane.rootPane != this.rootPane ) {
                ActiveDrag foreignDrag = otherWindowGlassPane.getActiveDrag();
                if ( foreignDrag.equals(ActiveDrag.none()) )
                    continue;
                ActiveDrag foreignDragInLocalCoordinates = convertActiveDragToOtherGlassPane(otherWindowGlassPane, foreignDrag, this);
                Bounds otherBounds = foreignDragInLocalCoordinates.getBounds();
                // Let's check if the drag image is in the bounds of this glass pane
                if ( this.getBounds().intersects(otherBounds.toRectangle()) ) {
                    foreignDragInLocalCoordinates.paint(g);
                }
            }
        }
    }

    private ActiveDrag convertActiveDragToOtherGlassPane(
        JGlassPane sourceGlassPane,
        ActiveDrag sourceDrag,
        JGlassPane targetGlassPane
    ) {
        /*
            We want to convert the other active drag position,
            which is relative to the glass pane, to a position
            relative to this glass pane.
        */
        Point sourcePositionOnDesktop = sourceGlassPane.getLocationOnScreen();
        Point targetPositionOnDesktop = targetGlassPane.getLocationOnScreen();
        Point sourceActiveDragRelativePosition = sourceDrag.getStart().toPoint();
        Point targetActiveDragRelativePosition = new Point(
                sourceActiveDragRelativePosition.x - (targetPositionOnDesktop.x - sourcePositionOnDesktop.x),
                sourceActiveDragRelativePosition.y - (targetPositionOnDesktop.y - sourcePositionOnDesktop.y)
        );
        // Is the other active drag image currently hovering over this glass pane?
        return sourceDrag.withStart(
                        Position.of(
                                targetActiveDragRelativePosition.x,
                                targetActiveDragRelativePosition.y
                        )
                    );
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

    /**
     *  Marries this glass pane to the supplied {@link JRootPane} object
     *  and detaches it from the previous root pane using the {@link #detachFromRootPane(JRootPane)} method,
     *  if a previous root pane was attached.
     *
     * @param rootPane The {@link JRootPane} object to which this glass
     *                 pane should be attached.
     */
    protected void attachToRootPane(JRootPane rootPane) {
        Objects.requireNonNull(rootPane);
        if ( this.rootPane != null ) this.detachFromRootPane(this.rootPane);
        this.setOpaque(false);
        ( this.rootPane = rootPane ).setGlassPane(this);
        GuiDebugDevToolUtility.setupGlobalDevToolsShortcutFor(rootPane);
        this.setVisible(false);
    }

    /**
     *  Detaches this glass pane from the supplied {@link JRootPane} object,
     *  if the {@link JRootPane#getGlassPane()} method returns this glass pane.
     *
     * @param rootPane The {@link JRootPane} object from which this glass pane should be detached.
     */
    protected void detachFromRootPane( @Nullable JRootPane rootPane ) {
        Objects.requireNonNull(rootPane);
        if ( rootPane.getGlassPane() == this ) {
            rootPane.setGlassPane(null);
            setVisible(false);
            GuiDebugDevToolUtility.teardownGlobalDevToolsShortcutFor(rootPane);
        }
    }

    /**
     *  Marries this glass pane to a {@link JRootPane} object.
     *  Note that it is expected that the supplied {@link JRootPane}
     *  is the same root pane that this current glass pane is attached to.
     *  If the supplied {@link JRootPane} is {@code null}, then this glass pane
     *  is detached from the current root pane.
     *
     * @param pane The {@link JRootPane} object to which this glass pane should be attached.
     */
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

    private void eventDispatched( AWTEvent event ) {
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

    class GlassPaneDragSourceListener implements DragSourceListener {
        @Override
        public void dragEnter(DragSourceDragEvent event) {
            getActiveDrag().dragConf().ifPresent(conf -> {
                try {
                    conf.onDragEnter().accept(new ComponentDelegate(conf.component(), event));
                } catch (Exception ex) {
                    log.error(SwingTree.get().logMarker(),
                            "Error while executing drag enter event handlers.",
                            ex
                        );
                }
            });
        }
        @Override
        public void dragOver(DragSourceDragEvent event) {
            getActiveDrag().dragConf().ifPresent(conf -> {
                try {
                    conf.onDragOver().accept(new ComponentDelegate(conf.component(), event));
                } catch (Exception ex) {
                    log.error(SwingTree.get().logMarker(),
                            "Error while executing drag over event handlers.",
                            ex
                        );
                }
            });
        }
        @Override
        public void dropActionChanged(DragSourceDragEvent event) {
            getActiveDrag().dragConf().ifPresent(conf -> {
                try {
                    conf.onDropActionChanged().accept(new ComponentDelegate(conf.component(), event));
                } catch (Exception ex) {
                    log.error(SwingTree.get().logMarker(),
                            "Error while executing drop action changed event handlers.",
                            ex
                        );
                }
            });
        }
        @Override
        public void dragExit(DragSourceEvent event) {
            getActiveDrag().dragConf().ifPresent(conf -> {
                try {
                    conf.onDragExit().accept(new ComponentDelegate(conf.component(), event));
                } catch (Exception ex) {
                    log.error(SwingTree.get().logMarker(),
                            "Error while executing drag exit event handlers.",
                            ex
                        );
                }
            });
        }

        @Override
        public void dragDropEnd(DragSourceDropEvent event) {
            ActiveDrag activeDrag = getActiveDrag();
            if ( activeDrag.equals(ActiveDrag.none()) )
                return;

            activeDrag.dragConf().ifPresent(conf -> {
                try {
                    conf.onDragDropEnd().accept(new ComponentDelegate(conf.component(), event));
                } catch (Exception ex) {
                    log.error(SwingTree.get().logMarker(),
                            "Error while executing drag drop end event handlers.",
                            ex
                    );
                }
            });
            repaintRootPaneFor(activeDrag, activeDrag);
            setActiveDrag(ActiveDrag.none());
        }
    }

    private static Transferable _findRelevantDataFor(@Nullable Component component) {
        try {
            if (component == null)
                return new StringSelection("");
            if (component instanceof TextComponent) {
                String text = ((TextComponent) component).getText();
                if (text == null)
                    return new StringSelection("");
                return new StringSelection(text);
            } else if (component instanceof JTextComponent) {
                String text = ((JTextComponent) component).getText();
                if (text == null)
                    return new StringSelection("");
                return new StringSelection(text);
            } else if (component instanceof AbstractButton) {
                String text = ((AbstractButton) component).getText();
                if (text == null)
                    return new StringSelection("");
            } else if (component instanceof JLabel) {
                String text = ((JLabel) component).getText();
                if (text == null)
                    return new StringSelection("");
                return new StringSelection(text);
            } else if (component instanceof JList) {
                Object selectedValue = ((JList<?>) component).getSelectedValue();
                if (selectedValue == null)
                    return new StringSelection("");
            } else if (component instanceof JTable) {
                JTable table = (JTable) component;
                int row = table.getSelectedRow();
                int column = table.getSelectedColumn();
                Object value = table.getValueAt(row, column);
                if (value == null)
                    return new StringSelection("");
                return new StringSelection(value.toString());
            } else if (component instanceof JTree) {
                JTree tree = (JTree) component;
                Object lastSelectedPathComponent = tree.getLastSelectedPathComponent();
                if (lastSelectedPathComponent == null)
                    return new StringSelection("");
                return new StringSelection(lastSelectedPathComponent.toString());
            } else if (component instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) component;
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex == -1)
                    return new StringSelection("");
                String titleAt = tabbedPane.getTitleAt(selectedIndex);
                if (titleAt == null)
                    return new StringSelection("");
                return new StringSelection(titleAt);
            } else if (component instanceof JSpinner) {
                Object value = ((JSpinner) component).getValue();
                if (value == null)
                    return new StringSelection("");
                return new StringSelection(value.toString());
            } else if (component instanceof JSlider) {
                int value = ((JSlider) component).getValue();
                return new StringSelection(String.valueOf(value));
            } else if (component instanceof JProgressBar) {
                int value = ((JProgressBar) component).getValue();
                return new StringSelection(String.valueOf(value));
            } else if (component instanceof JComboBox) {
                Object selectedItem = ((JComboBox<?>) component).getSelectedItem();
                if (selectedItem == null)
                    return new StringSelection("");
                return new StringSelection(selectedItem.toString());
            }
        } catch (Exception ignored) {
            return new StringSelection("");
        }
        return new StringSelection("");
    }
}

package swingtree.components;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.DragAwayComponentConf;
import swingtree.DragDropEventComponentDelegate;
import swingtree.DragOverEventComponentDelegate;
import swingtree.layout.Bounds;
import swingtree.layout.Location;
import swingtree.layout.Size;
import swingtree.style.ComponentExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Objects;
import java.util.Optional;

import static javax.swing.SwingUtilities.convertPoint;
import static javax.swing.SwingUtilities.getDeepestComponentAt;

final class ActiveDrag {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ActiveDrag.class);

    private static final ActiveDrag NO_DRAG = new ActiveDrag(
                                                        null, null, 0,
                                                        Location.origin(), Location.origin(), Location.origin(),
                                                        null
                                                    );

    public static ActiveDrag none() { return NO_DRAG; }

    private final @Nullable Component                            draggedComponent;
    private final @Nullable BufferedImage                        currentDragImage;
    private final int                                            componentHash;
    private final Location                                       start;
    private final Location                                       offset;
    private final Location                                       localOffset;
    private final @Nullable DragAwayComponentConf<?, MouseEvent> dragConf;

    private ActiveDrag(
        @Nullable Component                            draggedComponent,
        @Nullable BufferedImage                        currentDragImage,
        int                                            componentHash,
        Location                                       start,
        Location                                       offset,
        Location                                       localOffset,
        @Nullable DragAwayComponentConf<?, MouseEvent> dragConf
    ) {
        this.draggedComponent  = draggedComponent;
        this.currentDragImage  = currentDragImage;
        this.componentHash     = componentHash;
        this.start             = Objects.requireNonNull(start);
        this.offset            = Objects.requireNonNull(offset);
        this.localOffset       = Objects.requireNonNull(localOffset);
        this.dragConf          = dragConf;
    }

    public @Nullable BufferedImage currentDragImage() {
        return currentDragImage;
    }

    public Optional<DragAwayComponentConf<?, MouseEvent>> dragConf() {
        return Optional.ofNullable(dragConf);
    }

    public ActiveDrag begin(MouseEvent e, @Nullable JRootPane rootPane)
    {
        if ( rootPane == null )
            return this;
        /*
           We traverse the entire component tree
           to find the deepest component that is under the mouse
           and has a non-default cursor
           if we find such a component, we store it in _toBeDragged
         */
        java.awt.Component component = getDeepestComponentAt(
                                                rootPane.getContentPane(),
                                                e.getX(), e.getY()
                                            );

        if ( !(component instanceof JComponent) )
            return this; // We only support JComponents for dragging

        DragAwayComponentConf<?, MouseEvent> dragConf;
        do {
            dragConf = ComponentExtension.from((JComponent) component).getDragAwayConf(e);
            if ( !dragConf.enabled() ) {
                Component parent = component.getParent();
                if ( parent instanceof JComponent )
                    component = parent;
                else
                    return this;
            }
        }
        while ( !dragConf.enabled() );

        Location mousePosition = Location.of(e.getPoint());
        Location absoluteComponentPosition = Location.of(convertPoint(component, 0,0, rootPane.getContentPane()));

        return this.withDragConf(dragConf)
                    .withDraggedComponent(component)
                     .withStart(mousePosition)
                     .withOffset(Location.origin())
                     .withLocalOffset(mousePosition.minus(absoluteComponentPosition))
                     .renderComponentIntoImage(component, e);
    }


    public ActiveDrag renderComponentIntoImage( Component component, MouseEvent event )
    {
        Objects.requireNonNull(dragConf);

        BufferedImage image = dragConf.customDragImage().map(ActiveDrag::toBufferedImage).orElse(this.currentDragImage);

        int currentComponentHash = 0;
        if ( component instanceof JComponent ) {
            currentComponentHash = ComponentExtension.from((JComponent) component).viewStateHashCode();
            if ( currentComponentHash == this.componentHash && image != null )
                return this;
        }
        else return this;

        if ( dragConf.opacity() <= 0.0 )
            return this;

        boolean weNeedClearing = image != null;

        if ( image == null )
            image = new BufferedImage(
                        component.getWidth(),
                        component.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                    );

        // We wipe the image before rendering the component
        Graphics2D g = image.createGraphics();
        if ( weNeedClearing ) {
            Composite oldComp = g.getComposite();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(oldComp);
        }
        component.paint(g);

        try {
            if ( dragConf.opacity() < 1.0 && dragConf.opacity() > 0.0 ) {
                RescaleOp makeTransparent = new RescaleOp(
                                                new float[]{1.0f, 1.0f, 1.0f, /* alpha scaleFactor */ (float) dragConf.opacity()},
                                                new float[]{0f, 0f, 0f, /* alpha offset */ 0f}, null
                                            );
                image = makeTransparent.filter(image, null);
            }
        } catch (Exception e) {
            log.error("Failed to make the rendering of dragged component transparent.", e);
        }
        g.dispose();

        return new ActiveDrag(draggedComponent, image, currentComponentHash, start, offset, localOffset, dragConf);
    }

    public ActiveDrag dragged(MouseEvent e, @Nullable JRootPane rootPane)
    {
        if ( draggedComponent != null ) {
            Point point = e.getPoint();
            ActiveDrag updatedDrag = this.withOffset(Location.of(point.x - start.x(), point.y - start.y()))
                                         .renderComponentIntoImage(draggedComponent, e);

            if ( rootPane != null && !DragSource.isDragImageSupported() ) {
                /*
                    If the drag image is not supported by the platform, we
                    do the image rendering ourselves directly on the Graphics object
                    of the glass pane of the root pane.
                    But for this to work we need to repaint the area where the drag image was
                    previously rendered and the area where it will be rendered next.
                */
                Location previousWhereToRender = getRenderPosition();
                Location whereToRenderNext     = updatedDrag.getRenderPosition();
                Size size = Size.of(draggedComponent.getSize());
                Bounds previousArea = Bounds.of(previousWhereToRender, size);
                Bounds nextArea     = Bounds.of(whereToRenderNext, size);
                Bounds mergedArea   = previousArea.merge(nextArea);
                rootPane.repaint(mergedArea.toRectangle());
                try {
                    dispatchDragOver(e, rootPane);
                } catch (Exception ex) {
                    log.error("Error while dispatching drag over event.", ex);
                }
            }
            return updatedDrag;
        }
        return this;
    }

    private void dispatchDragOver(MouseEvent e, JRootPane rootPane) {
        // Find the new component under the mouse
        java.awt.Component component = getDeepestComponentAt(
                rootPane.getContentPane(),
                e.getX(), e.getY()
        );
        if ( component instanceof JComponent ) {
            JComponent jComponent = (JComponent) component;
            JComponent draggedJComponent = (JComponent) this.draggedComponent;
            boolean eventConsumed;
            do {
                eventConsumed = ComponentExtension.from(jComponent).dispatchDragOverEvent(e, draggedJComponent);
                if ( !eventConsumed ) {
                    Component parent = jComponent.getParent();
                    if ( parent instanceof JComponent && parent != this.draggedComponent )
                        jComponent = (JComponent) parent;
                    else
                        return;
                }
            }
            while ( !eventConsumed );

            return;
        }
    }

    public void tryDropping(MouseEvent e, @Nullable JRootPane rootPane)
    {
        if ( rootPane == null )
            return;
        try {
            dispatchDragOver(e, rootPane);
            // Find the new component under the mouse
            java.awt.Component component = getDeepestComponentAt(
                                                rootPane.getContentPane(),
                                                e.getX(), e.getY()
                                            );
            if ( component instanceof JComponent ) {
                // We go up the component tree and call all the dragDrop listeners
                JComponent jComponent = (JComponent) component;
                JComponent draggedJComponent = (JComponent) this.draggedComponent;
                boolean eventConsumed;
                do {
                    eventConsumed = ComponentExtension.from(jComponent).dispatchDragDropEvent(e, draggedJComponent);
                    if ( !eventConsumed ) {
                        Component parent = jComponent.getParent();
                        if ( parent instanceof JComponent && parent != this.draggedComponent )
                            jComponent = (JComponent) parent;
                        else
                            break;
                    }
                }
                while ( !eventConsumed );
            }
        } catch (Exception ex) {
            log.error("Error while dispatching drag over event.", ex);
        }
    }

    public void paint(Graphics g){
        if ( !DragSource.isDragImageSupported() ) {
            /*
                If the drag image is not supported by the platform, we
                do the image rendering ourselves directly on the Graphics object
                of the glass pane of the root pane.
            */
            if (draggedComponent != null && currentDragImage != null) {
                Location whereToRender = getRenderPosition();
                g.drawImage(currentDragImage, (int) whereToRender.x(), (int) whereToRender.y(), null);
            }
        }
    }

    private Location getRenderPosition() {
        return Location.of(
                (start.x() + offset.x() - localOffset.x()),
                (start.y() + offset.y() - localOffset.y())
            );
    }

    public ActiveDrag withDraggedComponent(@Nullable Component draggedComponent) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    public ActiveDrag withStart(Location start) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    public ActiveDrag withOffset(Location offset) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    public ActiveDrag withLocalOffset(Location localOffset) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    public ActiveDrag withDragConf(DragAwayComponentConf<?, MouseEvent> dragConf) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    private static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }
}

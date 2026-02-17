package swingtree.components;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.DragAwayComponentConf;
import swingtree.SwingTree;
import swingtree.layout.Bounds;
import swingtree.layout.Position;
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
                                                        Position.origin(), Position.origin(), Position.origin(),
                                                        null
                                                    );

    public static ActiveDrag none() { return NO_DRAG; }

    private final @Nullable Component                draggedComponent;
    private final @Nullable BufferedImage            currentDragImage;
    private final int                                componentHash;
    private final Position start;
    private final Position offset;
    private final Position localOffset;
    private final @Nullable DragAwayComponentConf<?> dragConf;

    private ActiveDrag(
        @Nullable Component                draggedComponent,
        @Nullable BufferedImage            currentDragImage,
        int                                componentHash,
        Position                           start,
        Position                           offset,
        Position                           localOffset,
        @Nullable DragAwayComponentConf<?> dragConf
    ) {
        this.draggedComponent  = draggedComponent;
        this.currentDragImage  = currentDragImage;
        this.componentHash     = componentHash;
        this.start             = Objects.requireNonNull(start);
        this.offset            = Objects.requireNonNull(offset);
        this.localOffset       = Objects.requireNonNull(localOffset);
        this.dragConf          = dragConf;
    }

    public @Nullable Component draggedComponent() {
        return draggedComponent;
    }

    public @Nullable BufferedImage currentDragImage() {
        return currentDragImage;
    }

    public Optional<DragAwayComponentConf<?>> dragConf() {
        return Optional.ofNullable(dragConf);
    }

    public ActiveDrag begin(Point dragStart, @Nullable JRootPane rootPane)
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
                                                dragStart.x, dragStart.y
                                            );

        if ( !(component instanceof JComponent) )
            return this; // We only support JComponents for dragging

        Position mousePosition = Position.of(dragStart);

        Optional<DragAwayComponentConf<JComponent>> dragConf;
        do {
            dragConf = ComponentExtension.from((JComponent) component).getDragAwayConf(mousePosition);
            if ( !dragConf.isPresent() || dragConf.map(it->!it.enabled()).orElse(false) ) {
                Component parent = component.getParent();
                if ( parent instanceof JComponent )
                    component = parent;
                else
                    return this;
            }
        }
        while ( !dragConf.isPresent() || dragConf.map(it->!it.enabled()).orElse(false) );

        Position absoluteComponentPosition = Position.of(convertPoint(component, 0,0, rootPane.getContentPane()));

        return this.withDragConf(dragConf.get())
                    .withDraggedComponent(component)
                     .withStart(mousePosition)
                     .withOffset(Position.origin())
                     .withLocalOffset(mousePosition.minus(absoluteComponentPosition))
                     .renderComponentIntoImage();
    }


    public ActiveDrag renderComponentIntoImage()
    {
        Objects.requireNonNull(dragConf);
        Optional<Image> customDragImage = dragConf.customDragImage();
        BufferedImage image = customDragImage.map(ActiveDrag::toBufferedImage).orElse(this.currentDragImage);
        Component component = Objects.requireNonNull(draggedComponent);

        int currentComponentHash;
        if ( component instanceof JComponent ) {
            currentComponentHash = ComponentExtension.from((JComponent) component).viewStateHashCode();
            if ( currentComponentHash == this.componentHash && image != null )
                return this;
        }
        else return this;

        if ( dragConf.opacity() <= 0.0 )
            return this;

        boolean weNeedClearing = image != null;

        if ( !customDragImage.isPresent() ) {
            // We make the drag image based on the component!
            if (image == null)
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
            g.dispose();
        }

        try {
            if ( dragConf.opacity() < 1.0 && dragConf.opacity() > 0.0 ) {
                RescaleOp makeTransparent = new RescaleOp(
                                                new float[]{1.0f, 1.0f, 1.0f, /* alpha scaleFactor */ (float) dragConf.opacity()},
                                                new float[]{0f, 0f, 0f, /* alpha offset */ 0f}, null
                                            );
                image = makeTransparent.filter(image, null);
            }
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Failed to make the rendering of dragged component transparent.", e);
        }

        return new ActiveDrag(draggedComponent, image, currentComponentHash, start, offset, localOffset, dragConf);
    }

    ActiveDrag dragged(MouseEvent e)
    {
        if ( draggedComponent != null ) {
            Point point = e.getPoint();
            return this.withOffset(Position.of(point.x - start.x(), point.y - start.y()))
                       .renderComponentIntoImage();
        }
        return this;
    }

    boolean hasDraggedComponent() {
        return draggedComponent != null;
    }

    Size draggedComponentSize() {
        if ( currentDragImage != null ) {
            return Size.of(currentDragImage.getWidth(), currentDragImage.getHeight());
        } else if ( draggedComponent != null ) {
            return Size.of(draggedComponent.getSize());
        }
        return Size.unknown();
    }

    void paint(Graphics g){
        if ( !DragSource.isDragImageSupported() ) {
            /*
                If the drag image is not supported by the platform, we
                do the image rendering ourselves directly on the Graphics object
                of the glass pane of the root pane.
            */
            if (draggedComponent != null && currentDragImage != null) {
                Position whereToRender = getRenderPosition();
                g.drawImage(currentDragImage, (int) whereToRender.x(), (int) whereToRender.y(), null);
            }
        }
    }

    Position getRenderPosition() {
        return Position.of(
                (start.x() + offset.x() - localOffset.x()),
                (start.y() + offset.y() - localOffset.y())
            );
    }

    Position getStart() {
        return start;
    }

    Bounds getBounds() {
        return Bounds.of(getRenderPosition(), draggedComponentSize());
    }

    ActiveDrag withDraggedComponent(@Nullable Component draggedComponent) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    ActiveDrag withStart(Position start) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    ActiveDrag withOffset(Position offset) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    ActiveDrag withLocalOffset(Position localOffset) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset, dragConf);
    }

    ActiveDrag withDragConf( DragAwayComponentConf<?> dragConf ) {
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

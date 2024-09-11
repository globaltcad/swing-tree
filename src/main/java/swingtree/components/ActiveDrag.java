package swingtree.components;

import org.jspecify.annotations.Nullable;
import swingtree.layout.Bounds;
import swingtree.layout.Location;
import swingtree.layout.Size;
import swingtree.style.ComponentExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static javax.swing.SwingUtilities.convertPoint;
import static javax.swing.SwingUtilities.getDeepestComponentAt;

final class ActiveDrag {

    private static final ActiveDrag NO_DRAG = new ActiveDrag(
                                                        null, null, 0,
                                                        Location.origin(), Location.origin(), Location.origin()
                                                    );

    public static ActiveDrag none() { return NO_DRAG; }

    private final @Nullable Component     draggedComponent;
    private final @Nullable BufferedImage currentDragImage;
    private final int                     componentHash;
    private final Location                start;
    private final Location                offset;
    private final Location                localOffset;

    private ActiveDrag(
        @Nullable Component     draggedComponent,
        @Nullable BufferedImage currentDragImage,
        int                     componentHash,
        Location                start,
        Location                offset,
        Location                localOffset
    ) {
        this.draggedComponent  = draggedComponent;
        this.currentDragImage  = currentDragImage;
        this.componentHash     = componentHash;
        this.start             = Objects.requireNonNull(start);
        this.offset            = Objects.requireNonNull(offset);
        this.localOffset       = Objects.requireNonNull(localOffset);
    }

    public @Nullable Component draggedComponent() {
        return draggedComponent;
    }

    public @Nullable BufferedImage currentDragImage() {
        return currentDragImage;
    }

    public Location start() {
        return start;
    }

    public Location offset() {
        return offset;
    }

    public Location localOffset() {
        return localOffset;
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
        if ( component == null )
            return this;

        Location mousePosition = Location.of(e.getPoint());
        Location absoluteComponentPosition = Location.of(convertPoint(component, 0,0, rootPane.getContentPane()));

        return this.withDraggedComponent(component)
                     .withStart(mousePosition)
                     .withOffset(Location.origin())
                     .withLocalOffset(mousePosition.minus(absoluteComponentPosition));
    }


    public ActiveDrag renderComponentIntoImage(Component component) {

        BufferedImage image = this.currentDragImage;

        int currentComponentHash = 0;
        if ( component instanceof JComponent ) {
            currentComponentHash = ComponentExtension.from((JComponent) component).viewStateHashCode();
            if ( currentComponentHash == this.componentHash && image != null )
                return this;
        }

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
        g.dispose();

        return new ActiveDrag(draggedComponent, image, currentComponentHash, start, offset, localOffset);
    }

    public ActiveDrag dragged(MouseEvent e, @Nullable JRootPane rootPane)
    {
        if ( draggedComponent != null ) {
            Point point = e.getPoint();
            ActiveDrag updatedDrag = this.withOffset(Location.of(point.x - start.x(), point.y - start.y()))
                                         .renderComponentIntoImage(draggedComponent);
            if ( rootPane != null ) {
                Location previousWhereToRender = getRenderPosition();
                Location whereToRenderNext     = updatedDrag.getRenderPosition();
                Size size = Size.of(draggedComponent.getSize());
                Bounds previousArea = Bounds.of(previousWhereToRender, size);
                Bounds nextArea     = Bounds.of(whereToRenderNext, size);
                Bounds mergedArea   = previousArea.merge(nextArea);
                rootPane.repaint(mergedArea.toRectangle());
            }
            return updatedDrag;
        }
        return this;
    }

    public void paint(Graphics g){
        if ( draggedComponent != null && currentDragImage != null ) {
            Location whereToRender = getRenderPosition();
            g.drawImage(currentDragImage, (int)whereToRender.x(), (int)whereToRender.y(), null);
        }
    }

    private Location getRenderPosition() {
        return Location.of(
                (start.x() + offset.x() - localOffset.x()),
                (start.y() + offset.y() - localOffset.y())
            );
    }

    public ActiveDrag withDraggedComponent(@Nullable Component draggedComponent) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset);
    }

    public ActiveDrag withStart(Location start) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset);
    }

    public ActiveDrag withOffset(Location offset) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset);
    }

    public ActiveDrag withLocalOffset(Location localOffset) {
        return new ActiveDrag(draggedComponent, currentDragImage, componentHash, start, offset, localOffset);
    }

}

package examples.dnd;

import swingtree.UI;
import swingtree.UIForBox;
import swingtree.components.JBox;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

/**
 *  A simple demonstration of drag and drop functionality in SwingTree.
 *  This UI consists of two simple box panels in a horizontal row
 *  as part of a responsive grid flow layout.<br>
 *  When the entire GUI is resized to be less wide, then the boxes will
 *  stack vertically.<br>
 *  Both of these boxes have a light brown background with an inward going shadow
 *  effect which identifies them as containers for items to be dragged.
 *  <br>
 *  The left box has a label "Drag me" which can be dragged away from the box.
 *  The right box is a drop site for the dragged item.
 *  The "drag away" and "drop site" event listeners will animate
 *  the start and end of the drag and drop operation.
 */
public final class DragAndDrop extends Panel
{
    public DragAndDrop()
    {
        UI.of(this).withFlowLayout()
        .withPrefSize(550, 500)
        .withBackgroundColor("light oak")
        .add(AUTO_SPAN(it->it.medium(12).large(6)),
            createBox()
            .add(CENTER,
                label("Drag me").id("draggable").withStyle(it->it.fontSize(24))
                .withDragAway( conf -> conf
                    .onDragStart( it -> {
                        it.parentDelegate( parent -> {
                            parent.animateFor(1, TimeUnit.SECONDS, status -> {
                                double r = 320 * status.fadeOut() * it.getScale();
                                double x = it.getEvent().getDragOrigin().getX() - r / 2;
                                double y = it.getEvent().getDragOrigin().getY() - r / 2;
                                parent.paint(status, g -> {
                                    g.setColor(new Color(1f, 1f, 0f, (float) status.fadeIn()));
                                    g.fillOval((int) x, (int) y, (int) r, (int) r);
                                });
                            });
                        });
                    })
                )
            )
        )
        .add(AUTO_SPAN(it->it.medium(12).large(6)),
            createBox()
        );
    }

    private UIForBox<JBox> createBox()
    {
        return
            box().withPrefSize(300,300)
            .withStyle( it -> it
                .backgroundColor("rgb(220,220,220)")
                .foundationColor("light oak")
                .shadowIsInset(true)
                .shadowColor("black")
                .shadowBlurRadius(3)
                .borderRadius(24)
                .margin(16)
                .padding(16)
            )
            .withDropSite( conf -> conf
                .onDrop( it -> {
                    it.animateFor(1, TimeUnit.SECONDS, status -> {
                        double r = 320 * status.fadeIn() * it.getScale();
                        double x = it.getEvent().getLocation().getX() - r / 2;
                        double y = it.getEvent().getLocation().getY() - r / 2;
                        it.paint(status, g -> {
                            g.setColor(new Color(1f, 1f, 0f, (float) status.fadeOut()));
                            g.fillOval((int) x, (int) y, (int) r, (int) r);
                        });
                    });
                    it.find(JLabel.class, "draggable").ifPresent(label -> {
                        label.getParent().repaint();
                        it.get().add(label);
                    });
                })
            );
    }

    public static void main(String[] args)
    {
        UI.show("Drag and Drop", f -> new DragAndDrop());
    }
}

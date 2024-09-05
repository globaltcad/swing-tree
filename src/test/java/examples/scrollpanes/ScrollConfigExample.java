package examples.scrollpanes;

import swingtree.UI;
import static swingtree.UI.*;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.Dimension;

public final class ScrollConfigExample extends JPanel {

    private static final String TEXT =
            "This is a little story about a long sentence which is unfortunately too long to fit horizontally " +
            "placed on a single line of text in a panel inside a scroll pane.";


    public ScrollConfigExample() {
        of(this).withLayout("wrap, fill").withPrefSize(350, 550)
        .add("shrink",label("Not implementing Scrollable:"))
        .add("grow, push",
            scrollPane()
            .add(
                panel("wrap", "", "[]push[]")
                .withBackground(Color.LIGHT_GRAY)
                .add(html(TEXT))
                .add(html("END"))
            )
        )
        .add("shrink",label("Implementing Scrollable:"))
        .add("grow, push",
            scrollPane()
            .add(
                of(new BoilerplateScrollablePanel()).withLayout("wrap", "", "[]push[]")
                .withBackground(Color.LIGHT_GRAY)
                .add(html(TEXT))
                .add(html("END"))
            )
        )
        .add("shrink",label("Using Scroll Conf:"))
        .add("grow, push",
            scrollPane(it -> it.fitWidth(true) )
            .add(
                panel("wrap", "", "[]push[]")
                .withBackground(Color.LIGHT_GRAY)
                .add(html(TEXT))
                .add(html("END"))
            )
        );
    }

    public static void main(String[] args) {
        UI.show(frame -> new ScrollConfigExample());
    }


    final static class BoilerplateScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return null;
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

}

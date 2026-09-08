package utility;

import swingtree.UI;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *  Stands in for an application's own view class: a component whose constructor does
 *  something besides construct, in a package that is not the library's.
 *  <p>
 *  Counting is the mildest thing a constructor can do. The one which exposed the defect
 *  this helper pins installed a look and feel, so styling a view that contained it replaced
 *  the look and feel the application had just chosen.
 */
public class ConstructionCountingPanel extends UI.Panel
{
    public static final AtomicInteger CONSTRUCTIONS = new AtomicInteger(0);

    public ConstructionCountingPanel() {
        CONSTRUCTIONS.incrementAndGet();
    }
}

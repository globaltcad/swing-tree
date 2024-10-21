package swingtree;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.api.Configurator;
import swingtree.layout.*;

/**
 *  Essentially just a namespace for static layout constants for
 *  the {@link net.miginfocom.swing.MigLayout} {@link java.awt.LayoutManager} type. <br>
 *  This class is not intended to be instantiated! <br>
 *  <br>
 *  The constants as well as static factory methods in this class
 *  are intended to be used like this:
 *  <pre>{@code
 * import static swingtree.UI.*;
 * //...
 * panel(FILL.and(WRAP(2)))
 * .withPrefSize(500, 300)
 * .add(GROW,
 *   panel(FILL_X.and(WRAP(2)),"[shrink][grow]")
 *   .add(label("Username"))
 *   .add(GROW_X,
 *     textField(vm.username())
 *   )
 *   .add(SHRINK_X, label("Password"))
 *   .add(GROW_X,
 *     passwordField(vm.password())
 *   )
 * )
 * .add(GROW,
 *   panel(FILL_X.and(WRAP(2)),"[shrink][grow]")
 *   .add(label("Email"))
 *   .add(GROW_X,
 *     textField(vm.email())
 *   )
 *   .add(SHRINK_X, label("Gender"))
 *   .add(GROW_X,
 *     comboBox(vm.gender())
 *   )
 * )
 * .add(GROW_X,
 *   panel(FILL_X.and(WRAP(1)))
 *   .add(GROW_X,
 *     checkBox("I accept!", vm.termsAccepted())
 *   )
 *   .add(GROW_X,
 *     button("Register")
 *     .onClick( it -> vm.register() )
 *   )
 * )
 * .add(GROW_X,
 *   panel(FILL_X.and(WRAP(1)))
 *   .withBorderTitled("Feedback")
 *   .add(GROW_X,
 *     boldLabel(
 *       vm.feedback()
 *     )
 *     .withForeground(vm.feedbackColor())
 *   )
 * )
 * .add(GROW_X.and(SPAN), button("RESET").onClick( it -> vm.reset() ));
 * }</pre>
 *
 *  In this little example form we can see how the constants are used to
 *  create a form with a grid layout. <br>
 *  The {@link #FILL} constant is used to make the panels fill the entire
 *  width of the parent panel. <br>
 *  The {@link #WRAP(int)} constant is used to make the panels wrap after
 *  n components have been added to them. <br>
 *  The {@link #GROW} constant is used to make the panels grow vertically
 *  to fill the entire height of the parent panel. <br>
 *  ... and so on. <br>
 */
public abstract class UILayoutConstants
{
    private static final Logger log = LoggerFactory.getLogger(UILayoutConstants.class);

    UILayoutConstants() { throw new UnsupportedOperationException(); }

    // Common Mig layout constants:
    public static LayoutConstraint FILL   = LayoutConstraint.of("fill");
    public static LayoutConstraint FILL_X = LayoutConstraint.of("fillx");
    public static LayoutConstraint FILL_Y = LayoutConstraint.of("filly");
    public static LayoutConstraint INS(int insets) { return INSETS(insets); }
    public static LayoutConstraint INSETS(int insets) { return LayoutConstraint.of("insets " + insets); }
    public static LayoutConstraint INS(int top, int left, int bottom, int right) { return INSETS(top, left, bottom, right); }
    public static LayoutConstraint INSETS(int top, int left, int bottom, int right) { return LayoutConstraint.of("insets " + top + " " + left + " " + bottom + " " + right); }
    public static LayoutConstraint WRAP(int times) { return LayoutConstraint.of( "wrap " + times ); }
    public static LayoutConstraint GAP_REL(int size) { return LayoutConstraint.of( "gap rel " + size ); }
    public static LayoutConstraint FLOW_X   = LayoutConstraint.of("flowx");
    public static LayoutConstraint FLOW_Y   = LayoutConstraint.of("flowy");
    public static LayoutConstraint NO_GRID  = LayoutConstraint.of("nogrid");
    public static LayoutConstraint NO_CACHE = LayoutConstraint.of("nocache");
    public static LayoutConstraint DEBUG    = LayoutConstraint.of("debug");

    public static MigAddConstraint WRAP = MigAddConstraint.of("wrap");
    public static MigAddConstraint SPAN = MigAddConstraint.of("SPAN");
    public static MigAddConstraint SPAN(int times ) { return MigAddConstraint.of( "span " + times ); }
    public static MigAddConstraint SPAN(int xTimes, int yTimes ) { return MigAddConstraint.of( "span " + xTimes + " " + yTimes ); }
    public static MigAddConstraint SPAN_X(int times ) { return MigAddConstraint.of( "spanx " + times ); }
    public static MigAddConstraint SPAN_Y(int times ) { return MigAddConstraint.of( "spany " + times ); }
    public static MigAddConstraint GROW   = MigAddConstraint.of("grow");
    public static MigAddConstraint GROW_X = MigAddConstraint.of("growx");
    public static MigAddConstraint GROW_Y = MigAddConstraint.of("growy");
    public static MigAddConstraint GROW(int weight ) { return MigAddConstraint.of( "grow " + weight ); }
    public static MigAddConstraint GROW_X(int weight ) { return MigAddConstraint.of( "growx " + weight ); }
    public static MigAddConstraint GROW_Y(int weight ) { return MigAddConstraint.of( "growy " + weight ); }
    public static MigAddConstraint SHRINK   = MigAddConstraint.of("shrink");
    public static MigAddConstraint SHRINK_X = MigAddConstraint.of("shrinkx");
    public static MigAddConstraint SHRINK_Y = MigAddConstraint.of("shrinky");
    public static MigAddConstraint SHRINK(int weight )  { return MigAddConstraint.of("shrink "+weight); }
    public static MigAddConstraint SHRINK_X(int weight )  { return MigAddConstraint.of("shrinkx "+weight); }
    public static MigAddConstraint SHRINK_Y(int weight )  { return MigAddConstraint.of("shrinky "+weight); }
    public static MigAddConstraint SHRINK_PRIO(int priority )  { return MigAddConstraint.of("shrinkprio "+priority); }
    public static MigAddConstraint PUSH     = MigAddConstraint.of("push");
    public static MigAddConstraint PUSH_X   = MigAddConstraint.of("pushx");
    public static MigAddConstraint PUSH_Y   = MigAddConstraint.of("pushy");
    public static MigAddConstraint PUSH(int weight )  { return MigAddConstraint.of("push "+weight); }
    public static MigAddConstraint PUSH_X(int weight ) { return MigAddConstraint.of("pushx "+weight); }
    public static MigAddConstraint PUSH_Y(int weight ) { return MigAddConstraint.of("pushy "+weight); }
    public static MigAddConstraint SKIP(int cells ) { return MigAddConstraint.of("skip "+cells); }
    public static MigAddConstraint SPLIT(int cells ) { return MigAddConstraint.of("split "+cells); }
    public static MigAddConstraint WIDTH(int min, int pref, int max ) { return MigAddConstraint.of("width "+min+":"+pref+":"+max); }
    public static MigAddConstraint HEIGHT(int min, int pref, int max ) { return MigAddConstraint.of("height "+min+":"+pref+":"+max); }
    public static MigAddConstraint PAD(int size ) { return PAD(size, size, size, size); }
    public static MigAddConstraint PAD(int top, int left, int bottom, int right ) { return MigAddConstraint.of("pad "+top+" "+left+" "+bottom+" "+right); }
    public static MigAddConstraint ALIGN_CENTER = MigAddConstraint.of("align center");
    public static MigAddConstraint ALIGN_LEFT = MigAddConstraint.of("align left");
    public static MigAddConstraint ALIGN_RIGHT = MigAddConstraint.of("align right");
    public static MigAddConstraint ALIGN_X_CENTER = MigAddConstraint.of("alignx center");
    public static MigAddConstraint ALIGN_X_LEFT = MigAddConstraint.of("alignx left");
    public static MigAddConstraint ALIGN_X_RIGHT = MigAddConstraint.of("alignx right");
    public static MigAddConstraint ALIGN_Y_CENTER = MigAddConstraint.of("aligny center");
    public static MigAddConstraint ALIGN_Y_BOTTOM = MigAddConstraint.of("aligny bottom");
    public static MigAddConstraint ALIGN_Y_TOP = MigAddConstraint.of("aligny top");
    public static MigAddConstraint ALIGN(UI.Side pos ) { return MigAddConstraint.of(pos.toMigAlign()); }
    public static MigAddConstraint TOP = MigAddConstraint.of("top");
    public static MigAddConstraint RIGHT = MigAddConstraint.of("right");
    public static MigAddConstraint BOTTOM = MigAddConstraint.of("bottom");
    public static MigAddConstraint LEFT = MigAddConstraint.of("left");
    public static MigAddConstraint CENTER = MigAddConstraint.of("center");
    public static MigAddConstraint GAP_LEFT(int size) { return MigAddConstraint.of("gapleft "+size); }
    public static MigAddConstraint GAP_RIGHT(int size) { return MigAddConstraint.of("gapright "+size); }
    public static MigAddConstraint GAP_TOP(int size) { return MigAddConstraint.of("gaptop "+size); }
    public static MigAddConstraint GAP_BOTTOM(int size) { return MigAddConstraint.of("gapbottom "+size); }
    public static MigAddConstraint GAP_LEFT_PUSH = MigAddConstraint.of("gapleft push");
    public static MigAddConstraint GAP_RIGHT_PUSH = MigAddConstraint.of("gapright push");
    public static MigAddConstraint GAP_TOP_PUSH = MigAddConstraint.of("gaptop push");
    public static MigAddConstraint GAP_BOTTOM_PUSH = MigAddConstraint.of("gapbottom push");
    public static MigAddConstraint DOCK_NORTH = MigAddConstraint.of("dock north");
    public static MigAddConstraint DOCK_SOUTH = MigAddConstraint.of("dock south");
    public static MigAddConstraint DOCK_EAST  = MigAddConstraint.of("dock east");
    public static MigAddConstraint DOCK_WEST  = MigAddConstraint.of("dock west");
    public static MigAddConstraint DOCK(UI.Side pos ) { return MigAddConstraint.of("dock " + pos.toDirectionString()); }

    /**
     *  A factory method for creating a {@link net.miginfocom.layout.LC} instance.
     * @return A {@link net.miginfocom.layout.LC} instance.
     */
    public static LC LC() { return new LC(); }
    public static AC AC() { return new AC(); }
    public static CC CC() { return new CC(); }

    public static FlowCellConf AUTO_SPAN(AutoCellSpanPolicy... autoSpans ) {
        return new FlowCellConf(autoSpans);
    }

    public static FlowCellConf AUTO_SPAN(Configurator<FlowCellConf> configurator) {
        FlowCellConf flowCellConf = new FlowCellConf(new AutoCellSpanPolicy[0]);
        try {
            return configurator.configure(flowCellConf);
        } catch (Exception e) {
            log.error(
                    "Error configuring '"+ FlowCellConf.class.getSimpleName()+"' instance " +
                    "for '"+ ResponsiveGridFlowLayout.class.getSimpleName() + "' layout.",
                    e
            );
        }
        return flowCellConf;
    }

}

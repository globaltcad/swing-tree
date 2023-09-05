package swingtree;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import swingtree.layout.AddConstraint;
import swingtree.layout.LayoutConstraint;

/**
 *  Essentially just a namespace for static layout constants for
 *  the {@link net.miginfocom.swing.MigLayout} {@link java.awt.LayoutManager} type.
 */
public abstract class UILayoutConstants
{
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

    public static AddConstraint WRAP = AddConstraint.of("wrap");
    public static AddConstraint SPAN = AddConstraint.of("SPAN");
    public static AddConstraint SPAN(int times ) { return AddConstraint.of( "span " + times ); }
    public static AddConstraint SPAN(int xTimes, int yTimes ) { return AddConstraint.of( "span " + xTimes + " " + yTimes ); }
    public static AddConstraint SPAN_X(int times ) { return AddConstraint.of( "spanx " + times ); }
    public static AddConstraint SPAN_Y(int times ) { return AddConstraint.of( "spany " + times ); }
    public static AddConstraint GROW   = AddConstraint.of("grow");
    public static AddConstraint GROW_X = AddConstraint.of("growx");
    public static AddConstraint GROW_Y = AddConstraint.of("growy");
    public static AddConstraint GROW(int weight ) { return AddConstraint.of( "grow " + weight ); }
    public static AddConstraint GROW_X(int weight ) { return AddConstraint.of( "growx " + weight ); }
    public static AddConstraint GROW_Y(int weight ) { return AddConstraint.of( "growy " + weight ); }
    public static AddConstraint SHRINK   = AddConstraint.of("shrink");
    public static AddConstraint SHRINK_X = AddConstraint.of("shrinkx");
    public static AddConstraint SHRINK_Y = AddConstraint.of("shrinky");
    public static AddConstraint SHRINK(int weight )  { return AddConstraint.of("shrink "+weight); }
    public static AddConstraint SHRINK_X(int weight )  { return AddConstraint.of("shrinkx "+weight); }
    public static AddConstraint SHRINK_Y(int weight )  { return AddConstraint.of("shrinky "+weight); }
    public static AddConstraint SHRINK_PRIO(int priority )  { return AddConstraint.of("shrinkprio "+priority); }
    public static AddConstraint PUSH     = AddConstraint.of("push");
    public static AddConstraint PUSH_X   = AddConstraint.of("pushx");
    public static AddConstraint PUSH_Y   = AddConstraint.of("pushy");
    public static AddConstraint PUSH(int weight )  { return AddConstraint.of("push "+weight); }
    public static AddConstraint PUSH_X(int weight ) { return AddConstraint.of("pushx "+weight); }
    public static AddConstraint PUSH_Y(int weight ) { return AddConstraint.of("pushy "+weight); }
    public static AddConstraint SKIP(int cells ) { return AddConstraint.of("skip "+cells); }
    public static AddConstraint SPLIT(int cells ) { return AddConstraint.of("split "+cells); }
    public static AddConstraint WIDTH(int min, int pref, int max ) { return AddConstraint.of("width "+min+":"+pref+":"+max); }
    public static AddConstraint HEIGHT(int min, int pref, int max ) { return AddConstraint.of("height "+min+":"+pref+":"+max); }
    public static AddConstraint PAD(int size ) { return PAD(size, size, size, size); }
    public static AddConstraint PAD(int top, int left, int bottom, int right ) { return AddConstraint.of("pad "+top+" "+left+" "+bottom+" "+right); }
    public static AddConstraint ALIGN_CENTER = AddConstraint.of("align center");
    public static AddConstraint ALIGN_LEFT = AddConstraint.of("align left");
    public static AddConstraint ALIGN_RIGHT = AddConstraint.of("align right");
    public static AddConstraint ALIGN_X_CENTER = AddConstraint.of("alignx center");
    public static AddConstraint ALIGN_X_LEFT = AddConstraint.of("alignx left");
    public static AddConstraint ALIGN_X_RIGHT = AddConstraint.of("alignx right");
    public static AddConstraint ALIGN_Y_CENTER = AddConstraint.of("aligny center");
    public static AddConstraint ALIGN_Y_BOTTOM = AddConstraint.of("aligny bottom");
    public static AddConstraint ALIGN_Y_TOP = AddConstraint.of("aligny top");
    public static AddConstraint ALIGN(UI.Side pos ) { return AddConstraint.of(pos.toMigAlign()); }
    public static AddConstraint TOP = AddConstraint.of("top");
    public static AddConstraint RIGHT = AddConstraint.of("right");
    public static AddConstraint BOTTOM = AddConstraint.of("bottom");
    public static AddConstraint LEFT = AddConstraint.of("left");
    public static AddConstraint CENTER = AddConstraint.of("center");
    public static AddConstraint GAP_LEFT_PUSH = AddConstraint.of("gapleft push");
    public static AddConstraint GAP_RIGHT_PUSH = AddConstraint.of("gapright push");
    public static AddConstraint GAP_TOP_PUSH = AddConstraint.of("gaptop push");
    public static AddConstraint GAP_BOTTOM_PUSH = AddConstraint.of("gapbottom push");
    public static AddConstraint DOCK_NORTH = AddConstraint.of("dock north");
    public static AddConstraint DOCK_SOUTH = AddConstraint.of("dock south");
    public static AddConstraint DOCK_EAST  = AddConstraint.of("dock east");
    public static AddConstraint DOCK_WEST  = AddConstraint.of("dock west");
    public static AddConstraint DOCK(UI.Side pos ) { return AddConstraint.of("dock " + pos.toDirectionString()); }

    public static LC LC() { return new LC().fill(); }
    public static AC AC() { return new AC(); }
    public static CC CC() { return new CC(); }
}

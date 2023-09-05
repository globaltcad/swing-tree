package swingtree;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import swingtree.layout.CompAttr;
import swingtree.layout.LayoutAttr;

/**
 *  Essentially just a namespace for static layout constants for
 *  the {@link net.miginfocom.swing.MigLayout} {@link java.awt.LayoutManager} type.
 */
public abstract class UILayoutConstants
{
    UILayoutConstants() { throw new UnsupportedOperationException(); }

    // Common Mig layout constants:
    public static LayoutAttr FILL   = LayoutAttr.of("fill");
    public static LayoutAttr FILL_X = LayoutAttr.of("fillx");
    public static LayoutAttr FILL_Y = LayoutAttr.of("filly");
    public static LayoutAttr INS(int insets) { return INSETS(insets); }
    public static LayoutAttr INSETS(int insets) { return LayoutAttr.of("insets " + insets); }
    public static LayoutAttr INS(int top, int left, int bottom, int right) { return INSETS(top, left, bottom, right); }
    public static LayoutAttr INSETS(int top, int left, int bottom, int right) { return LayoutAttr.of("insets " + top + " " + left + " " + bottom + " " + right); }
    public static LayoutAttr WRAP(int times) { return LayoutAttr.of( "wrap " + times ); }
    public static LayoutAttr GAP_REL(int size) { return LayoutAttr.of( "gap rel " + size ); }
    public static LayoutAttr FLOW_X   = LayoutAttr.of("flowx");
    public static LayoutAttr FLOW_Y   = LayoutAttr.of("flowy");
    public static LayoutAttr NO_GRID  = LayoutAttr.of("nogrid");
    public static LayoutAttr NO_CACHE = LayoutAttr.of("nocache");
    public static LayoutAttr DEBUG    = LayoutAttr.of("debug");

    public static CompAttr WRAP = CompAttr.of("wrap");
    public static CompAttr SPAN = CompAttr.of("SPAN");
    public static CompAttr SPAN( int times ) { return CompAttr.of( "span " + times ); }
    public static CompAttr SPAN( int xTimes, int yTimes ) { return CompAttr.of( "span " + xTimes + " " + yTimes ); }
    public static CompAttr SPAN_X( int times ) { return CompAttr.of( "spanx " + times ); }
    public static CompAttr SPAN_Y( int times ) { return CompAttr.of( "spany " + times ); }
    public static CompAttr GROW   = CompAttr.of("grow");
    public static CompAttr GROW_X = CompAttr.of("growx");
    public static CompAttr GROW_Y = CompAttr.of("growy");
    public static CompAttr GROW( int weight ) { return CompAttr.of( "grow " + weight ); }
    public static CompAttr GROW_X( int weight ) { return CompAttr.of( "growx " + weight ); }
    public static CompAttr GROW_Y( int weight ) { return CompAttr.of( "growy " + weight ); }
    public static CompAttr SHRINK   = CompAttr.of("shrink");
    public static CompAttr SHRINK_X = CompAttr.of("shrinkx");
    public static CompAttr SHRINK_Y = CompAttr.of("shrinky");
    public static CompAttr SHRINK( int weight )  { return CompAttr.of("shrink "+weight); }
    public static CompAttr SHRINK_X( int weight )  { return CompAttr.of("shrinkx "+weight); }
    public static CompAttr SHRINK_Y( int weight )  { return CompAttr.of("shrinky "+weight); }
    public static CompAttr SHRINK_PRIO( int priority )  { return CompAttr.of("shrinkprio "+priority); }
    public static CompAttr PUSH     = CompAttr.of("push");
    public static CompAttr PUSH_X   = CompAttr.of("pushx");
    public static CompAttr PUSH_Y   = CompAttr.of("pushy");
    public static CompAttr PUSH( int weight )  { return CompAttr.of("push "+weight); }
    public static CompAttr PUSH_X( int weight ) { return CompAttr.of("pushx "+weight); }
    public static CompAttr PUSH_Y( int weight ) { return CompAttr.of("pushy "+weight); }
    public static CompAttr SKIP( int cells ) { return CompAttr.of("skip "+cells); }
    public static CompAttr SPLIT( int cells ) { return CompAttr.of("split "+cells); }
    public static CompAttr WIDTH( int min, int pref, int max ) { return CompAttr.of("width "+min+":"+pref+":"+max); }
    public static CompAttr HEIGHT( int min, int pref, int max ) { return CompAttr.of("height "+min+":"+pref+":"+max); }
    public static CompAttr PAD( int size ) { return PAD(size, size, size, size); }
    public static CompAttr PAD( int top, int left, int bottom, int right ) { return CompAttr.of("pad "+top+" "+left+" "+bottom+" "+right); }
    public static CompAttr ALIGN_CENTER = CompAttr.of("align center");
    public static CompAttr ALIGN_LEFT = CompAttr.of("align left");
    public static CompAttr ALIGN_RIGHT = CompAttr.of("align right");
    public static CompAttr ALIGN_X_CENTER = CompAttr.of("alignx center");
    public static CompAttr ALIGN_X_LEFT = CompAttr.of("alignx left");
    public static CompAttr ALIGN_X_RIGHT = CompAttr.of("alignx right");
    public static CompAttr ALIGN_Y_CENTER = CompAttr.of("aligny center");
    public static CompAttr ALIGN_Y_BOTTOM = CompAttr.of("aligny bottom");
    public static CompAttr ALIGN_Y_TOP = CompAttr.of("aligny top");
    public static CompAttr ALIGN( UI.Side pos ) { return CompAttr.of(pos.toMigAlign()); }
    public static CompAttr TOP = CompAttr.of("top");
    public static CompAttr RIGHT = CompAttr.of("right");
    public static CompAttr BOTTOM = CompAttr.of("bottom");
    public static CompAttr LEFT = CompAttr.of("left");
    public static CompAttr CENTER = CompAttr.of("center");
    public static CompAttr GAP_LEFT_PUSH = CompAttr.of("gapleft push");
    public static CompAttr GAP_RIGHT_PUSH = CompAttr.of("gapright push");
    public static CompAttr GAP_TOP_PUSH = CompAttr.of("gaptop push");
    public static CompAttr GAP_BOTTOM_PUSH = CompAttr.of("gapbottom push");
    public static CompAttr DOCK_NORTH = CompAttr.of("dock north");
    public static CompAttr DOCK_SOUTH = CompAttr.of("dock south");
    public static CompAttr DOCK_EAST  = CompAttr.of("dock east");
    public static CompAttr DOCK_WEST  = CompAttr.of("dock west");
    public static CompAttr DOCK( UI.Side pos ) { return CompAttr.of("dock " + pos.toDirectionString()); }

    public static LC LC() { return new LC().fill(); }
    public static AC AC() { return new AC(); }
    public static CC CC() { return new CC(); }
}

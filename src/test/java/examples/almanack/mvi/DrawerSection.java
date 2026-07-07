package examples.almanack.mvi;

/**
 *  Which section of the side drawer is open — or {@link #NONE}, meaning the drawer
 *  is collapsed entirely. Seen through an enum⇄index lens ({@link #index()} /
 *  {@link #fromIndex(int)}) this property <i>is</i> the selection index of the
 *  drawer's tabbed pane, with {@link #NONE} mapping to {@code -1}: the index that
 *  keeps every tab deselected. The toolbar toggle buttons bind the same property
 *  through little boolean lenses (pressed = "this section is the open one",
 *  released = "collapse the drawer").
 */
public enum DrawerSection {

    NONE     ( -1 ),
    NAVIGATOR(  0 ),
    INSPECTOR(  1 ),
    LOG      (  2 );

    private final int index;

    DrawerSection( int index ) { this.index = index; }

    /** The selection index of this section's drawer tab, {@code -1} for {@link #NONE}. */
    public int index() { return index; }

    public static DrawerSection fromIndex( int index ) {
        for ( DrawerSection section : values() )
            if ( section.index == index )
                return section;
        throw new IllegalArgumentException("No drawer section at index " + index);
    }
}

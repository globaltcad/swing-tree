package examples.almanack.mvi;

/**
 *  How every page of the almanack presents itself. One shared property of this type
 *  drives the little Write / Preview / Details sub-tab strip inside <i>every</i>
 *  page at once: expressed as a selection index through an enum⇄index lens, it is
 *  handed to each sub-strip's {@code withSelectedIndex(Var<Integer>)} binding,
 *  while the toolbar radio buttons bind the enum itself — so all the strips and
 *  the radios can never disagree.
 */
public enum EditorMode {

    WRITE  ( "✎ Write",   0 ),
    PREVIEW( "❀ Preview", 1 ),
    DETAILS( "✦ Details", 2 );

    private final String label;
    private final int    index;

    EditorMode( String label, int index ) { this.label = label; this.index = index; }

    public String label() { return label; }

    /** The position of this mode's tab inside every page's sub-tab strip. */
    public int index() { return index; }

    public static EditorMode fromIndex( int index ) {
        for ( EditorMode mode : values() )
            if ( mode.index == index )
                return mode;
        throw new IllegalArgumentException("No editor mode at index " + index);
    }
}

package examples.laf;

import swingtree.style.ComponentExtension;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

/**
 *  The kinds of surface {@link LinenLookAndFeel} knows how to paint a
 *  {@link javax.swing.JPanel} as, tagged onto the panel the ordinary SwingTree
 *  way:
 *  <pre>{@code
 *    UI.panel().group(LinenSurface.CARD)        // a raised sheet of paper
 *    UI.panel().group(LinenSurface.RAIL)        // a flat strip: tool bar, status line
 *    UI.panel().group(LinenSurface.TRANSPARENT) // structure only, nothing painted
 *  }</pre>
 *
 *  <h2>Why the look-and-feel owns this and not the application</h2>
 *  A panel is the one component an application stacks by the dozen, and a real
 *  window needs at least two kinds: the ground it is all standing on, and the
 *  cards standing on it. Linen paints panels itself — it is the whole point of
 *  {@link LinenPanelUI} — and because the look-and-feel layer of SwingTree's
 *  cascade is resolved <em>after</em> the application's {@code StyleSheet}, a
 *  rule such as {@code add(group(Skin.CARD), it -> it.backgroundColor(WHITE))}
 *  would be silently overwritten. Naming the surfaces here instead means the
 *  application asks for the kind of surface it wants, and Linen answers in its
 *  own palette — so a theme change reaches the cards too, which a hard-coded
 *  application colour never would.
 *  <p>
 *  Only the <em>fill</em> and the grain belong to the surface. Padding, spacing,
 *  extra borders and per-edge accents are all left untouched for the
 *  application's style sheet or an inline {@code withStyle(..)} to set, and
 *  {@link #CARD}'s radius, hairline and shadow are defaults that either layer
 *  can still override.
 *
 *  @see LinenPanelUI
 *  @see LinenVariant
 */
public enum LinenSurface
{
    /** The ground: warm cream with Linen's faint woven grain. The default. */
    WINDOW,
    /** A sheet of paper lying on the window — lighter, rounded, hairlined and
     *  softly shadowed, with no grain so that it reads as raised. */
    CARD,
    /** A flat strip of the card colour with no radius and no shadow: tool bars,
     *  status lines, side rails and table headings. */
    RAIL,
    /** Nothing at all. For panels that exist only to group and lay out their
     *  children, where a second cream fill over the first would just muddy the
     *  card it sits in. */
    TRANSPARENT;

    /** Cached because {@link #of} runs inside {@code style(..)}, which is
     *  re-evaluated on every paint; {@code values()} would clone the array each
     *  time. */
    private static final LinenSurface[] VALUES = values();

    /**
     *  Reads the surface a component was tagged with.
     *  <p>
     *  A {@link JViewport} is never part of an application's declaration — a
     *  scroll pane creates its own — so it cannot carry a tag of its own and
     *  inherits the one on the scroll pane around it instead. That is what keeps
     *  the strip of viewport below a short page from being painted a different
     *  colour than the page.
     *
     *  @param component the component being styled
     *  @return the first surface the component belongs to, or {@link #WINDOW}
     */
    public static LinenSurface of( JComponent component ) {
        JComponent tagged = component;
        if ( component instanceof JViewport && component.getParent() instanceof JScrollPane )
            tagged = (JScrollPane) component.getParent();
        ComponentExtension<?> extension = ComponentExtension.from(tagged);
        for ( LinenSurface surface : VALUES )
            if ( surface != WINDOW && extension.belongsToGroup(surface) )
                return surface;
        return WINDOW;
    }
}

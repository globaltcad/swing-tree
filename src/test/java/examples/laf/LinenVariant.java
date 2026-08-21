package examples.laf;

import swingtree.style.ComponentExtension;

import javax.swing.JComponent;
import java.awt.Color;

/**
 *  The semantic button roles {@link LinenLookAndFeel} paints, tagged onto a
 *  component the ordinary SwingTree way:
 *  <pre>{@code
 *    UI.button("Ship it").group(LinenVariant.PRIMARY)
 *    UI.button("Strike out").group(LinenVariant.DANGER)
 *    UI.button("⟲").group(LinenVariant.QUIET)
 *  }</pre>
 *
 *  <h2>Why a LAF-owned enum rather than the application's own style sheet</h2>
 *  SwingTree resolves a style in three layers — style sheet, then look-and-feel,
 *  then inline {@code withStyle(..)} — and each layer may overwrite the one
 *  before it. A LAF that declares {@code backgroundColor(..)} unconditionally
 *  therefore <em>wins</em> against an application rule such as
 *  {@code add(type(JButton.class).group(Brand.PRIMARY), it -> it.backgroundColor(GREEN))},
 *  and the brand colour silently never appears. That is not a bug in the
 *  cascade; it is the reason the
 *  <a href="https://github.com/globaltcad/swing-tree/blob/main/docs/markdown/Building-A-Look-And-Feel.md">look-and-feel
 *  guide</a> tells a LAF with semantic variants to expose them as a public enum
 *  and branch on it inside {@code style(..)}. This is that enum.
 *  <p>
 *  A variant only decides <i>which</i> colours a control is painted in; every
 *  other Linen property — the radius, the padding, the focus border that grows
 *  while the margin shrinks to absorb it — is shared, so the roles stay visibly
 *  part of one family. Anything the LAF does not claim is still free for a style
 *  sheet or an inline {@code withStyle(..)} to set.
 *
 *  @see LinenButtonUI
 *  @see LinenPalette
 */
public enum LinenVariant
{
    /** The default: a raised cream surface. Applied when nothing else is tagged. */
    NEUTRAL,
    /** The one affirmative action on a form — filled with {@link LinenPalette#PRIMARY}. */
    PRIMARY,
    /** A destructive action — filled with {@link LinenPalette#DANGER}. */
    DANGER,
    /** A tool-bar or in-place control: no surface and no border until the pointer
     *  arrives, so a row of them reads as text rather than as a wall of boxes. */
    QUIET;

    /** Cached because {@link #of} runs inside {@code style(..)}, which is
     *  re-evaluated on every paint; {@code values()} would clone the array each
     *  time. */
    private static final LinenVariant[] VALUES = values();

    /**
     *  Reads the variant a component was tagged with.
     *
     *  @param component the component being styled
     *  @return the first variant the component belongs to, or {@link #NEUTRAL}
     */
    public static LinenVariant of( JComponent component ) {
        ComponentExtension<?> extension = ComponentExtension.from(component);
        for ( LinenVariant variant : VALUES )
            if ( variant != NEUTRAL && extension.belongsToGroup(variant) )
                return variant;
        return NEUTRAL;
    }

    /** @return {@code true} for the roles that carry a surface and a shadow at rest. */
    boolean isRaised() { return this != QUIET; }

    /** @return {@code true} for the roles painted in a strong colour rather than in cream. */
    boolean isFilled() { return this == PRIMARY || this == DANGER; }

    /**
     *  The fill for one combination of button states.
     *
     *  @param enabled  whether the button can be pressed at all
     *  @param sunken   whether it is pressed or selected
     *  @param rollover whether the pointer is over it
     *  @return the surface colour to paint
     */
    Color surface( boolean enabled, boolean sunken, boolean rollover ) {
        if ( !enabled )
            return this == QUIET ? LinenPalette.TRANSPARENT : LinenPalette.SURFACE_DISABLED;
        switch ( this ) {
            case PRIMARY:
                return sunken ? LinenPalette.PRIMARY_PRESSED
                     : rollover ? LinenPalette.PRIMARY_HOVER : LinenPalette.PRIMARY;
            case DANGER:
                return sunken ? LinenPalette.DANGER_PRESSED
                     : rollover ? LinenPalette.DANGER_HOVER : LinenPalette.DANGER;
            case QUIET:
                return sunken ? LinenPalette.SURFACE_PRESSED
                     : rollover ? LinenPalette.SURFACE_HOVER : LinenPalette.TRANSPARENT;
            default:
                return sunken ? LinenPalette.SURFACE_PRESSED
                     : rollover ? LinenPalette.SURFACE_HOVER : LinenPalette.SURFACE;
        }
    }

    /**
     *  The label colour that stays legible on {@link #surface}.
     *
     *  @param enabled whether the button can be pressed at all
     *  @return the foreground colour to paint the text in
     */
    Color foreground( boolean enabled ) {
        if ( !enabled )
            return LinenPalette.TEXT_DISABLED;
        return isFilled() ? LinenPalette.ON_FILLED : LinenPalette.TEXT;
    }

    /**
     *  The border colour. A filled variant borders itself in its own fill so the
     *  edge disappears, and every variant switches to the accent while focused —
     *  the border <i>is</i> Linen's focus indicator.
     *
     *  @param enabled  whether the button can be pressed at all
     *  @param focused  whether the button owns the keyboard focus
     *  @param rollover whether the pointer is over it
     *  @return the border colour to paint
     */
    Color border( boolean enabled, boolean focused, boolean rollover ) {
        if ( focused && enabled )
            return LinenPalette.ACCENT;
        if ( !enabled )
            return this == QUIET ? LinenPalette.TRANSPARENT : LinenPalette.BORDER_SOFT;
        switch ( this ) {
            case PRIMARY: return LinenPalette.PRIMARY_PRESSED;
            case DANGER:  return LinenPalette.DANGER_PRESSED;
            case QUIET:   return rollover ? LinenPalette.BORDER : LinenPalette.TRANSPARENT;
            default:      return LinenPalette.BORDER;
        }
    }
}

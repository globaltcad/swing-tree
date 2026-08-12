package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;
import swingtree.UIForBox;

import java.awt.Color;

import static swingtree.UI.*;

/**
 *  A wall of bevelled tiles, built out of the one style ingredient this example exists for:
 *  a rounded border carrying a <i>different colour on each of its four edges</i>.
 *  <p>
 *  That is not decoration for its own sake. A bevel is what a border looks like once you decide
 *  where the light comes from: with a lamp up and to the left, the top and left edges of a
 *  raised tile catch it while the right and bottom edges fall into shadow, and the eye reads the
 *  result as a solid object standing off the surface. Hand the same four colours to the opposite
 *  edges and the very same tile reads as pressed <i>into</i> the surface. Every toolkit of the
 *  1990s was drawn this way, and it is still the cheapest way to give a flat interface depth:
 *  four colours and a border width, with no gradient, no image and no shadow involved.
 *  <p>
 *  For the rendering engine it is a demanding thing to be asked for, because those four edges
 *  have to meet somewhere. Each corner is divided by a miter, and these corners are also
 *  <i>rounded</i>, so every miter has to cross an arc rather than a straight join. This view
 *  puts sixty-four of them on screen at once and asks for them to survive a window drag.
 */
public class BevelUIView extends Panel
{
    private static final int COLUMNS = 8;
    private static final int ROWS    = 8;

    /** The tile faces, in the order they are laid out; the two bevel colours are derived from these. */
    private static final Color[] FACES = {
                new Color(0x8f, 0x9a, 0xa8), new Color(0xa8, 0x8f, 0x92),
                new Color(0x92, 0xa8, 0x8f), new Color(0x9a, 0x8f, 0xa8),
                new Color(0xa8, 0xa2, 0x8f), new Color(0x8f, 0xa8, 0xa4),
                new Color(0xa0, 0x95, 0xa8), new Color(0x95, 0xa8, 0x9a)
            };

    public BevelUIView() {
        FlatLightLaf.setup();
        UI.of(this)
        .withLayout(FILL.and(WRAP(COLUMNS)).and(INS(14)).and(GAP_REL(6)))
        .withStyle( conf -> conf
            .prefSize(960, 640)
            .backgroundColor(new Color(0x6e, 0x74, 0x7e))
        )
        .apply( ui -> {
            for ( int tile = 0; tile < ROWS * COLUMNS; tile++ )
                ui.add(GROW, bevelledTile(FACES[tile % FACES.length], ( tile / COLUMNS + tile ) % 2 == 0));
        });
    }

    /**
     *  One tile, raised or sunken depending on {@code isRaised}, which decides nothing but the
     *  order in which the lit and the shadowed bevel colour are handed to the four edges.
     */
    private static UIForBox<?> bevelledTile( Color face, boolean isRaised ) {
        Color lit        = shifted(face,  0.42f);
        Color shadowed   = shifted(face, -0.45f);
        Color towardsSun = ( isRaised ? lit : shadowed );
        Color awayFromSun= ( isRaised ? shadowed : lit );
        return UI.box()
                .withLayout(FILL.and(INS(0)))
                .withStyle( conf -> conf
                    .backgroundColor(face)
                    .borderRadius(12)
                    .borderWidths(4, 4, 4, 4)
                    .borderColors(towardsSun, awayFromSun, awayFromSun, towardsSun)
                    .minSize(44, 44)
                );
    }

    /** Moves a colour towards white for a positive amount and towards black for a negative one. */
    private static Color shifted( Color color, float amount ) {
        Color target = ( amount >= 0 ? Color.WHITE : Color.BLACK );
        float weight = Math.abs(amount);
        return new Color(
                    Math.round(color.getRed()   + ( target.getRed()   - color.getRed()   ) * weight),
                    Math.round(color.getGreen() + ( target.getGreen() - color.getGreen() ) * weight),
                    Math.round(color.getBlue()  + ( target.getBlue()  - color.getBlue()  ) * weight)
                );
    }

    public static void main( String[] args ) {
        UI.show( f -> new BevelUIView() );
    }
}

package swingtree.style;

/**
 *  This library internal interface marks an immutable style configuration object as "simplifiable",
 *  which means that its state can be reduced to a simpler form that is closer to
 *  the default state of the object (or the "null" state), without changing
 *  what the rendered result would look like. <br>
 *  <br>
 *  This interface is mostly used in the {@link StyleConfLayer} the sub-configuration objects
 *  it is composed of. <br>
 *  An example of the concept of config simplification would be the {@link BorderConf#simplified()}
 *  methode, in which you can see that a border with a color but now border widths is simplified to
 *  a border with no color and no border widths.
 *
 * @param <I> The concrete type of this simplifiable object.
 */
interface Simplifiable<I>
{
    /**
     *  Implementations of this are intended to examine the
     *  state of this configuration instance and return a
     *  simpler, less ambitious variant if possible.
     *  If for example a config object describes the
     *  color and with of a border as (0,red) then this
     *  can be reduced to (0,null) due to the width 0
     *  causing the color to be completely irrelevant.
     *
     * @return A new simplified instance of the same type.
     */
    I simplified();
}

package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.api.Configurator;
import swingtree.api.Styler;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *  A {@link StyledString} is a uniformly styled snippet of text which is typically part of a sequence
 *  of styled strings that together make up a visually rich text in the <i>SwingTree</i> style API.
 *  More specifically, you can pass instances of this to {@link TextConf#content(StyledString...)}
 *  as part of a {@link ComponentStyleDelegate#text(Configurator)} sub-style configuration.<br>
 *  This class has value semantics and is immutable, so any potential {@link Configurator} referenced
 *  by an instance of this class is expected to be a pure function that produces the same output
 *  given the same input and has no side effects. <br>
 *
 * @see swingtree.UIForAnySwing#withStyle(Styler)
 * @see ComponentStyleDelegate#text(Configurator)
 * @see TextConf#content(StyledString...)
 * @see FontConf
 */
public final class StyledString {

    private static final Logger log = LoggerFactory.getLogger(StyledString.class);

    /**
     *  Creates a styled string with the given string but no style information.<br>
     *  The style of the string will be inherited from the context in which it is used within the style engine. <br>
     * @param string The string content of the styled string. It must not be null.
     * @return A new styled string with the given string and no style information.
     * @throws NullPointerException if the given string is null.
     */
    public static StyledString of( String string ) {
        return new StyledString(string, (Object) null);
    }

    /**
     *  Creates a styled string with the given string and style information provided by the given font {@link Configurator}.<br>
     *  The font configurator takes in an inherited {@link FontConf} and produces a new {@link FontConf}
     *  derived to hold the desired style information for the string.
     *  @param fontConfigurator A configurator that takes in an inherited {@link FontConf} and produces a new {@link FontConf}
     *                          derived to hold the desired style information for the string. It must not be null and
     *                          <b>must be a pure function</b> that produces the same output given the same input and has no side effects.
     *  @param string The string content of the styled string. It must not be null.
     *  @return A new styled string with the given string and style information provided by the given font configurator.
     *  @throws NullPointerException if the given string or font configurator is null.
     */
    public static StyledString of( Configurator<FontConf> fontConfigurator, String string ) {
        return new StyledString(string, fontConfigurator);
    }

    private final String _string;
    private final @Nullable Object _configuratorOrFontConf;

    private StyledString(String string, Configurator<FontConf> fontConfigurator) {
        this(Objects.requireNonNull(string), (Object) Objects.requireNonNull(fontConfigurator));
    }

    private StyledString(String string, FontConf fontConf) {
        this(Objects.requireNonNull(string), (Object) Objects.requireNonNull(fontConf));
    }

    private StyledString(String string, @Nullable Object fontConfigurator) {
        _string = Objects.requireNonNull(string);
        _configuratorOrFontConf = fontConfigurator;
    }

    /**
     *  Returns the string content of this styled string.
     *  This is the actual text that will be rendered when this styled string is used in a style configuration. <br>
     * @return The string content of this styled string. It is guaranteed to be non-null.
     */
    public String string() {
        return _string;
    }

    /**
     *  Returns a new styled string with the same style information as this styled string but with a new string content. <br>
     *  This is useful for when you want to reuse the same style information for different string content. <br>
     * @param string The new string content for the styled string. It must not be null.
     * @return A new styled string with the same style information as this styled string but with the given string content.
     * @throws NullPointerException if the given string is null.
     */
    public StyledString withString(String string) {
        return new StyledString(string, _configuratorOrFontConf);
    }

    StyledString mapStyle( Function<FontConf, FontConf> mapper ) {
        if ( _configuratorOrFontConf instanceof FontConf ) {
            try {
                FontConf newConf = mapper.apply((FontConf) _configuratorOrFontConf);
                return new StyledString(_string, newConf);
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(), "Error mapping font conf for string '{}'", _string, e);
            }
        }
        return this;
    }

    Optional<FontConf> fontConf() {
        if ( _configuratorOrFontConf instanceof FontConf ) {
            return Optional.of( (FontConf) _configuratorOrFontConf );
        }
        return Optional.empty();
    }

    StyledString resolveUsing(FontConf startConf) {
        if ( _configuratorOrFontConf instanceof Configurator ) {
            // We run the start conf through the configurator to get the new conf:
            Configurator<FontConf> configurator = (Configurator<FontConf>) _configuratorOrFontConf;
            try {
                FontConf newConf = configurator.configure(startConf);
                return new StyledString(_string, newConf);
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(), "Error configuring font conf for string '{}'", _string, e);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "StyledString[string='" + _string + "', style=" + _configuratorOrFontConf + "]";
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        StyledString that = (StyledString) o;
        return _string.equals(that._string) && Objects.equals(_configuratorOrFontConf, that._configuratorOrFontConf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_string, _configuratorOrFontConf);
    }

}

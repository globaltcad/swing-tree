package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Val;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *  A utility class for message formatting.
 */
final class NullUtil
{
    private NullUtil() {} // This is a utility class!

    /**
     *  Unfortunately, NullAway does not support nullability annotations on type parameters.
     *  It always assumes that type parameters are non-null, irrespective if
     *  the user provides a nullability annotation or not.
     *  This is a problem in the sprouts library, which also uses nullability annotations.
     *  This method is a workaround for this issue.
     *
     * @param var The variable to be faked as non-null.
     * @return The same variable as the input, but with a non-null type.
     * @param <T> The type of the variable.
     */
    @SuppressWarnings("NullAway")
    static <T> T fakeNonNull( @Nullable T var ) {
        return var;
    }

    /**
     *  Formats a {@link String} with placeholders "{}" and replaces them with the
     *  {@link String} representations of the given {@link Object}s.
     *  This is essentially just a delegate to {@link MessageFormatter#arrayFormat(String, Object[])}.
     *
     * @param withPlaceholders The {@link String} which may or may not contain placeholder in the for of "{}".
     * @param toBePutAtPlaceholders Arbitrary {@link Object}s which will be turned into
     *                              {@link String}s instead of the placeholder brackets.
     *
     * @return A {@link String} containing the actual {@link String} representations of th {@link Object}s
     *         instead of the placeholder brackets within the first argument.
     */
    static String format( String withPlaceholders, Object... toBePutAtPlaceholders ) {
        return MessageFormatter.arrayFormat( withPlaceholders, toBePutAtPlaceholders ).getMessage();
    }

    public static <T> void nullArgCheck( @Nullable T var, String thing, Class<?> type, String... notes ) {
        if ( var == null ) {
            String postfix = Arrays.stream(notes).collect(Collectors.joining(" "));
            postfix = ( postfix.trim().equals("") ? "" : " " ) + postfix;
            throw new IllegalArgumentException(
                format(
                        "Argument '{}' of type '{}' was null!{}",
                        thing, type.getSimpleName(), postfix
                )
            );
        }
    }

    public static <T> void nullPropertyCheck( Val<T> property, String thing, String... notes  ) {
        nullArgCheck( property, thing, Val.class, "Properties are not supposed to be null, they may wrap null values though." );
        if ( property.allowsNull() ) {
            Class<T> type = property.type();
            String message = "Property '{}' of type '{}' may not be null, but it was.";
            if ( notes.length > 0 )
                message += " " + String.join(" ", notes);

            throw new IllegalArgumentException( format( message, thing, type.getSimpleName() ) );
        }
    }

}
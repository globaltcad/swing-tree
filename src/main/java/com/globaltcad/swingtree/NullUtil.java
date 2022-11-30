package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;
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
     * @param withPlaceholders The {@link String} which may or may not contain placeholder in the for of "{}".
     * @param toBePutAtPlaceholders Arbitrary {@link Object}s which will be turned into
     *                              {@link String}s instead of the placeholder brackets.
     *
     * @return A {@link String} containing the actual {@link String} representations of th {@link Object}s
     *         instead of the placeholder brackets within the first argument.
     */
    public static String format( String withPlaceholders, Object... toBePutAtPlaceholders ) {
        return MessageFormatter.arrayFormat( withPlaceholders, toBePutAtPlaceholders ).getMessage();
    }

    public static <T> void nullArgCheck( T var, String thing, Class<?> type, String... notes ) {
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
            String message = "Property '{}' of type '{}' is not allowed to contain null!";
            if ( notes.length > 0 )
                message += " " + String.join(" ", notes);

            throw new IllegalArgumentException( format( message, thing, type.getSimpleName() ) );
        }
    }

}
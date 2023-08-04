package swingtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class StringUtils
{
    /**
     * Returns {@code true} if given string is {@code null} or length is zero.
     */
    public static boolean isEmpty( String string ) {
        return string == null || string.isEmpty();
    }

    public static List<String> split(String str, char delim ) {
        return split( str, delim, false, false );
    }

    /**
     * Splits a string at the specified delimiter.
     * If trimming is enabled, then leading and trailing whitespace characters are removed.
     * If excludeEmpty is {@code true}, then only non-empty strings are returned.
     *
     * @since 2
     */
    public static List<String> split( String str, char delim, boolean trim, boolean excludeEmpty ) {
        int delimIndex = str.indexOf( delim );
        if( delimIndex < 0 ) {
            if( trim )
                str = str.trim();
            return !excludeEmpty || !str.isEmpty()
                    ? Collections.singletonList( str )
                    : Collections.emptyList();
        }

        ArrayList<String> strs = new ArrayList<>();
        int index = 0;
        while( delimIndex >= 0 ) {
            add( strs, str, index, delimIndex, trim, excludeEmpty );
            index = delimIndex + 1;
            delimIndex = str.indexOf( delim, index );
        }
        add( strs, str, index, str.length(), trim, excludeEmpty );

        return strs;
    }

    private static void add( List<String> strs, String str, int beginIndex, int endIndex,
                             boolean trim, boolean excludeEmpty )
    {
        if( trim ) {
            beginIndex = trimBegin( str, beginIndex, endIndex );
            endIndex = trimEnd( str, beginIndex, endIndex );
        }

        if( !excludeEmpty || endIndex > beginIndex )
            strs.add( str.substring( beginIndex, endIndex ) );
    }

    private static int trimBegin( String str, int beginIndex, int endIndex ) {
        // skip leading whitespace
        while( beginIndex < endIndex && str.charAt( beginIndex ) <= ' ' )
            beginIndex++;
        return beginIndex;
    }

    private static int trimEnd( String str, int beginIndex, int endIndex ) {
        // skip trailing whitespace
        while( beginIndex < endIndex && str.charAt( endIndex - 1 ) <= ' ' )
            endIndex--;
        return endIndex;
    }
}


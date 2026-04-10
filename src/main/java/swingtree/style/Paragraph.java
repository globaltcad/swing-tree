package swingtree.style;

import sprouts.Tuple;

import java.util.Objects;

final class Paragraph {
    final boolean             isBlankLine;
    final Tuple<StyledString> styledStrings;

    private Paragraph( boolean isBlankLine, Tuple<StyledString> styledStrings ) {
        this.isBlankLine   = isBlankLine;
        this.styledStrings = Objects.requireNonNull(styledStrings);
    }

    static Paragraph of( Tuple<StyledString> styledStrings ) {
        return new Paragraph(false, Objects.requireNonNull(styledStrings));
    }

    static Paragraph blankLine() {
        return new Paragraph(true, Tuple.of(StyledString.class));
    }

    @Override
    public int hashCode() {
        return Objects.hash(isBlankLine, styledStrings);
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof Paragraph) ) return false;
        final Paragraph other = (Paragraph) o;
        return isBlankLine == other.isBlankLine && styledStrings.equals(other.styledStrings);
    }
}
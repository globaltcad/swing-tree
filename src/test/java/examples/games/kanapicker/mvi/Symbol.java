package examples.games.kanapicker.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

@With @Getter @Accessors( fluent = true )
@AllArgsConstructor
public final class Symbol
{
    private final char symbol;
    private final String sound;
    private final String description;
    private final Alphabet alphabet;

    private final Boolean isEnabled;
    private final int successes;

    public Symbol(char symbol, String sound, String description) {
        this(symbol, sound, description, null);
    }

    private Symbol(char symbol, String sound, String description, Alphabet alphabet) {
        this.symbol = symbol;
        this.sound = sound;
        this.description = description;
        this.alphabet = alphabet;
        this.isEnabled = false;
        this.successes = 0;
    }

    public Symbol incrementSuccesses() {
        return withSuccesses(successes + 1);
    }

    @Override
    public String toString() { return String.valueOf(symbol); }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof Symbol) ) return false;
        Symbol symbol = (Symbol) o;
        return this.symbol == symbol.symbol;
    }

    @Override
    public int hashCode() { return symbol; }
}

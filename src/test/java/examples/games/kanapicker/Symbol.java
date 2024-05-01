package examples.games.kanapicker;

import sprouts.Var;

import java.util.Optional;

public class Symbol
{
    private final char _symbol;
    private final String _sound;
    private final String _description;
    private final Alphabet _alphabet;

    private final Var<Boolean> _isEnabled = Var.of(false);
    private int _successes = 0;

    public Symbol(char symbol, String sound, String description) {
        this(symbol, sound, description, null);
    }

    private Symbol(char symbol, String sound, String description, Alphabet alphabet) {
        _symbol = symbol;
        _sound = sound;
        _description = description;
        _alphabet = alphabet;
    }

    public char symbol() { return _symbol; }

    public String sound() { return _sound; }

    public String description() { return _description; }

    public Optional<Alphabet> alphabet() { return Optional.ofNullable(_alphabet); }

    public Var<Boolean> isEnabled() { return _isEnabled; }

    public int successes() { return _successes; }

    public void incrementSuccesses() { _successes++; }

    public Symbol withAlphabet( Alphabet alphabet ) { return new Symbol(_symbol, _sound, _description); }

    @Override
    public String toString() { return String.valueOf(_symbol); }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof Symbol) ) return false;
        Symbol symbol = (Symbol) o;
        return _symbol == symbol._symbol;
    }

    @Override
    public int hashCode() { return _symbol; }
}

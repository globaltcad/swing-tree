package example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Alphabet
{
    private final String _name;
    private final List<Symbol> _symbols = new ArrayList<>();

    public Alphabet( String alphabetName, Symbol... symbols ) {
        _name = alphabetName;
        for ( Symbol symbol : symbols )
            _symbols.add(symbol.withAlphabet(this));
    }

    public String name() { return _name; }

    public List<Symbol> symbols() { return Collections.unmodifiableList(_symbols); }

}

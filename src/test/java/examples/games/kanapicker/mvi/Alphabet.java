package examples.games.kanapicker.mvi;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@With @Getter @Accessors( fluent = true )
@AllArgsConstructor @EqualsAndHashCode @ToString
public final class Alphabet
{
    private final String name;
    private final List<Symbol> symbols;

    public Alphabet( String alphabetName, Symbol... symbols ) {
        name = alphabetName;
        List<Symbol> symbolsList = new ArrayList<>();
        for ( Symbol symbol : symbols )
            symbolsList.add(symbol.withAlphabet(this));
        this.symbols = Collections.unmodifiableList(symbolsList);
    }

    public String name() { return name; }

    public List<Symbol> symbols() { return Collections.unmodifiableList(symbols); }

}

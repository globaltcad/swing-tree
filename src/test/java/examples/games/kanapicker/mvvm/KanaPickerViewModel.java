package examples.games.kanapicker.mvvm;

import sprouts.*;
import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationStatus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class KanaPickerViewModel
{
    private final Event repaint = Event.create();
    private final Var<String> feedback = Var.of("Choose:");
    private final Var<Color>  feedbackColor = Var.of(Color.BLACK);
    private final Var<Integer> feedbackFontSize = Var.of(24);
    private final Var<Symbol> currentSymbol = Var.of(new Symbol('?', "?", "?"));
    private final Var<Boolean> cheatMode = Var.of(false);

    private final Var<Integer> score = Var.of(0);
    private final Val<Integer> level = score.view( s -> s / 10 );

    private final List<Alphabet> alphabets = new ArrayList<>();

    public KanaPickerViewModel() {
        Viewable.cast(cheatMode).onChange(From.VIEW, it -> cheated() );
        alphabets.add(new Alphabet("Hiragana",
            new Symbol('あ', "a", "a"),
            new Symbol('え', "e", "e"),
            new Symbol('い', "i", "i"),
            new Symbol('お', "o", "o"),
            new Symbol('う', "u", "u"),
            new Symbol('か', "ka", "ka"),
            new Symbol('け', "ke", "ke"),
            new Symbol('き', "ki", "ki"),
            new Symbol('こ', "ko", "ko"),
            new Symbol('く', "ku", "ku"),
            new Symbol('さ', "sa", "sa"),
            new Symbol('せ', "se", "se"),
            new Symbol('し', "shi", "shi"),
            new Symbol('そ', "so", "so"),
            new Symbol('す', "su", "su"),
            new Symbol('た', "ta", "ta"),
            new Symbol('て', "te", "te"),
            new Symbol('ち', "chi", "chi"),
            new Symbol('と', "to", "to"),
            new Symbol('つ', "tsu", "tsu"),
            new Symbol('な', "na", "na"),
            new Symbol('ね', "ne", "ne"),
            new Symbol('に', "ni", "ni"),
            new Symbol('の', "no", "no"),
            new Symbol('ぬ', "nu", "nu"),
            new Symbol('は', "ha", "ha"),
            new Symbol('へ', "he", "he"),
            new Symbol('ひ', "hi", "hi"),
            new Symbol('ほ', "ho", "ho"),
            new Symbol('ふ', "fu", "fu"),
            new Symbol('ま', "ma", "ma"),
            new Symbol('め', "me", "me"),
            new Symbol('み', "mi", "mi"),
            new Symbol('も', "mo", "mo"),
            new Symbol('む', "mu", "mu"),
            new Symbol('や', "ya", "ya"),
            new Symbol('?', "?", "?"), // "ye" is not a valid syllable in Japanese
            new Symbol('?', "?", "?"), // "yi" is not a valid syllable in Japanese
            new Symbol('よ', "yo", "yo"),
            new Symbol('ゆ', "yu", "yu"),
            new Symbol('ら', "ra", "ra"),
            new Symbol('れ', "re", "re"),
            new Symbol('り', "ri", "ri"),
            new Symbol('ろ', "ro", "ro"),
            new Symbol('る', "ru", "ru"),
            new Symbol('わ', "wa", "wa"),
            new Symbol('ゑ', "we", "we"),
            new Symbol('ゐ', "wi", "wi"),
            new Symbol('を', "wo", "wo"),
            new Symbol('ん', "n", "n")
        ));
        alphabets.add(new Alphabet("Katakana",
            new Symbol('ア', "a", "a"),
            new Symbol('エ', "e", "e"),
            new Symbol('イ', "i", "i"),
            new Symbol('オ', "o", "o"),
            new Symbol('ウ', "u", "u"),
            new Symbol('カ', "ka", "ka"),
            new Symbol('ケ', "ke", "ke"),
            new Symbol('キ', "ki", "ki"),
            new Symbol('コ', "ko", "ko"),
            new Symbol('ク', "ku", "ku"),
            new Symbol('サ', "sa", "sa"),
            new Symbol('セ', "se", "se"),
            new Symbol('シ', "shi", "shi"),
            new Symbol('ソ', "so", "so"),
            new Symbol('ス', "su", "su"),
            new Symbol('タ', "ta", "ta"),
            new Symbol('テ', "te", "te"),
            new Symbol('チ', "chi", "chi"),
            new Symbol('ト', "to", "to"),
            new Symbol('ツ', "tsu", "tsu"),
            new Symbol('ナ', "na", "na"),
            new Symbol('ネ', "ne", "ne"),
            new Symbol('ニ', "ni", "ni"),
            new Symbol('ノ', "no", "no"),
            new Symbol('ヌ', "nu", "nu"),
            new Symbol('ハ', "ha", "ha"),
            new Symbol('ヘ', "he", "he"),
            new Symbol('ヒ', "hi", "hi"),
            new Symbol('ホ', "ho", "ho"),
            new Symbol('フ', "fu", "fu"),
            new Symbol('マ', "ma", "ma"),
            new Symbol('メ', "me", "me"),
            new Symbol('ミ', "mi", "mi"),
            new Symbol('モ', "mo", "mo"),
            new Symbol('ム', "mu", "mu"),
            new Symbol('ヤ', "ya", "ya"),
            new Symbol('?', "?", "?"), // "ye" is not a valid syllable in Japanese
            new Symbol('?', "?", "?"), // "yi" is not a valid syllable in Japanese
            new Symbol('ヨ', "yo", "yo"),
            new Symbol('ユ', "yu", "yu"),
            new Symbol('ラ', "ra", "ra"),
            new Symbol('レ', "re", "re"),
            new Symbol('リ', "ri", "ri"),
            new Symbol('ロ', "ro", "ro"),
            new Symbol('ル', "ru", "ru"),
            new Symbol('ワ', "wa", "wa"),
            new Symbol('ヱ', "we", "we"),
            new Symbol('ヰ', "wi", "wi"),
            new Symbol('ヲ', "wo", "wo"),
            new Symbol('ン', "n", "n")
        ));
        enableSymbols();
        newRandomSymbol();
    }

    public Event getRepaintEvent() { return repaint; }

    public Var<String> feedback() { return feedback; }

    public Var<Color> feedbackColor() { return feedbackColor; }

    public Var<Integer> feedbackFontSize() { return feedbackFontSize; }

    public Var<Symbol> currentSymbol() { return currentSymbol; }

    public Var<Boolean> cheatMode() { return cheatMode; }

    public Val<Integer> score() { return score; }

    public Val<Integer> level() { return level; }

    private void cheated() {
        feedback.set( "Cheater!" );
        feedbackColor.set( Color.RED );
        score.set(0);
        animateFeedbackAndThen( () -> {} );
    }

    public void selectSymbol( Symbol symbol ) {
        if ( currentSymbol.is(symbol) ) {
            symbol.incrementSuccesses();
            feedback.set( "Yes. Correct!" );
            feedbackColor.set( new Color(30, 128, 0) );
            if ( cheatMode.is(false) )
                score.set(score.get() + 1);
            animateFeedbackAndThen( () -> newRandomSymbol() );
        }
        else {
            feedback.set( "Try again!" );
            feedbackColor.set( Color.RED );
            // The score is not completely reset, we simply fall back to the level we were at before
            score.set( level().get() * 10 );
            animateFeedbackAndThen( () -> {} );
        }
    }

    private void animateFeedbackAndThen(Runnable onEnd) {
        UI.animateFor(0.45, TimeUnit.SECONDS).go(new Animation() {
            @Override
            public void run(AnimationStatus status) {
                feedbackFontSize.set((int) (24 + status.pulse() * 16));
            }
            @Override
            public void finish(AnimationStatus status) {
                feedbackFontSize.set(24);
                onEnd.run();
            }
        });
    }

    public List<Alphabet> getAlphabets() {
        return Collections.unmodifiableList(alphabets);
    }

    public void newRandomSymbol() {
        enableSymbols();

        // We sort all the enabled symbols based on their successes!
        List<Symbol> allEnabledSymbols =
                                alphabets.stream()
                                         .flatMap( alphabet -> alphabet.symbols().stream() )
                                         .filter( symbol -> symbol.isEnabled().is(true) )
                                         .sorted( (a, b) -> b.successes() - a.successes() )
                                         .collect( Collectors.toList() );

        Symbol randomSymbol = null;
        if ( allEnabledSymbols.size() > 0 ) {
            // Now we pick a random number between 0 and 4 and iterate from all the last enabled to the first enabled.
            // We do a 25% chance of picking the current symbol in the iteration.
            // If none was picked we repeat the process until we have a symbol.
            while ( randomSymbol == null )
                for ( int i = allEnabledSymbols.size() - 1; i >= 0; i-- )
                    if ( Math.random() < 0.25 ) {
                        randomSymbol = allEnabledSymbols.get(i);
                        break;
                    }
        }
        else
            throw new IllegalStateException( "No symbols are enabled" );

        feedback.set( "Choose:" );
        feedbackColor.set( Color.BLACK );
        currentSymbol.set( randomSymbol );
        repaint.fire();
    }

    private void enableSymbols() {
        int count = 5 * (level.get()+ 1 );

        for ( Alphabet alphabet : alphabets )
            for ( Symbol symbol : alphabet.symbols() )
                symbol.isEnabled().set(false);

        for ( Alphabet alphabet : alphabets ) {
            for ( Symbol symbol : alphabet.symbols() ) {
                if ( symbol.isEnabled().is(false) && symbol.symbol() != '?' ) {
                    symbol.isEnabled().set(true);
                    count--;
                    if ( count == 0 )
                        return;
                }
            }
        }
    }

}

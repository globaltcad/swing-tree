package examples.games.kanapicker.mvi;

import lombok.*;
import lombok.experimental.Accessors;
import swingtree.UI;
import swingtree.animation.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@With @Getter @Accessors( fluent = true )
@AllArgsConstructor @EqualsAndHashCode @ToString
public final class KanaPickerViewModel
{
    private final String  feedback        ;
    private final Color   feedbackColor   ;
    private final int     feedbackFontSize;
    private final Symbol  currentSymbol   ;
    private final boolean cheatMode       ;
    private final int     score           ;

    private final List<Alphabet> alphabets;

    public KanaPickerViewModel() {
        this(
            "Choose:", UI.Color.BLACK, 24,
            new Symbol('?', "?", "?"),
            false, 0, new ArrayList<>()
        );
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
    }

    public int level() {
        return this.score / 10;
    }

    Animatable<KanaPickerViewModel> cheated() {
        return this
                .withFeedback("Cheater!")
                .withFeedbackColor(Color.RED)
                .withScore(0)
                .animateFeedbackAndThen(m->m);
    }

    public Animatable<KanaPickerViewModel> selectSymbol( Symbol symbol ) {
        KanaPickerViewModel vm = this;
        if ( currentSymbol == symbol ) {
            vm = vm.withCurrentSymbol(symbol.incrementSuccesses());
            vm = vm.withFeedback( "Yes. Correct!" );
            vm = vm.withFeedbackColor( new Color(30, 128, 0) );
            if ( !cheatMode )
                vm = vm.withScore(score + 1);

            return vm.newRandomSymbol().animateFeedbackAndThen( m -> m );
        }
        else {
            vm = vm.withFeedback( "Try again!" );
            vm = vm.withFeedbackColor( Color.RED );
            // The score is not completely reset, we simply fall back to the level we were at before
            vm = vm.withScore( level() * 10 );
            return animateFeedbackAndThen( m->m );
        }
    }

    private Animatable<KanaPickerViewModel> animateFeedbackAndThen(
            Function<KanaPickerViewModel, KanaPickerViewModel> onEnd
    ) {
        return Animatable.of(LifeTime.of(0.45, TimeUnit.SECONDS), this, new AnimationFor<KanaPickerViewModel>() {
            @Override
            public KanaPickerViewModel run(AnimationStatus status, KanaPickerViewModel value) {
                return value.withFeedbackFontSize((int) (24 + status.pulse() * 16));
            }
            @Override
            public KanaPickerViewModel finish(AnimationStatus state, KanaPickerViewModel value) {
                return onEnd.apply(value.withFeedbackFontSize(24));
            }
        });
    }

    public List<Alphabet> getAlphabets() {
        return Collections.unmodifiableList(alphabets);
    }

    public KanaPickerViewModel newRandomSymbol() {
        KanaPickerViewModel vm = enableSymbols();

        // We sort all the enabled symbols based on their successes!
        List<Symbol> allEnabledSymbols =
                                vm.alphabets.stream()
                                         .flatMap( alphabet -> alphabet.symbols().stream() )
                                         .filter( symbol -> symbol.isEnabled() )
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

        vm = vm.withFeedback("Choose:");
        vm = vm.withFeedbackColor(Color.BLACK);
        vm = vm.withCurrentSymbol(randomSymbol);
        return vm;
    }

    KanaPickerViewModel enableSymbols() {
        KanaPickerViewModel vm = this;
        int count = 5 * ( level() + 1 );

        boolean done = false;
        List<Alphabet> updatedAlphabets = new ArrayList<>();
        for ( Alphabet alphabet : vm.alphabets ) {
            List<Symbol> updatedSymbols = new ArrayList<>();
            for ( Symbol symbol : alphabet.symbols() ) {
                if ( done )
                    updatedSymbols.add(symbol);
                else if ( symbol.symbol() != '?' ) {
                    updatedSymbols.add(symbol.withIsEnabled(true));
                    count--;
                    if ( count == 0 )
                        done = true;
                }
                else
                    updatedSymbols.add(symbol.withIsEnabled(false));
            }
            updatedAlphabets.add(alphabet.withSymbols(updatedSymbols));
        }
        return vm.withAlphabets(updatedAlphabets);
    }

}

package examples.games.notepicker.mvi;

import lombok.*;
import lombok.experimental.Accessors;
import sprouts.Event;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.animation.*;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@With @Getter @Accessors( fluent = true )
@AllArgsConstructor @EqualsAndHashCode @ToString
public class NoteGuesserViewModel
{
    // A simple text file which stores nothing but the score number:
    private final static String SAVE_FILE_NAME = ".score.txt";

    private final String  feedback        ; // = Var.of("Choose:");
    private final Color   feedbackColor   ; // = Var.of(Color.WHITE);
    private final int     feedbackFontSize; // = Var.of(24);
    private final int     currentNoteIndex; // = Var.of(0);
    private final boolean playMode        ; // = Var.of(false).onChange(From.VIEW, it -> activatedPlayMode(it.get()) );

    private final int score; // = Var.of(0);

    public static NoteGuesserViewModel ini() {
        return new NoteGuesserViewModel(
                    "Choose:", UI.Color.WHITE,
                    24, 0, false, 0
                )
                .loadScore()
                .newRandomNoteIndex();
    }

    public int level() { return score / 10; }

    private NoteGuesserViewModel loadScore() {
        try {
            String scoreText = new String( Files.readAllBytes(Paths.get(SAVE_FILE_NAME)) );
            return withScore( Integer.parseInt(scoreText) );
        }
        catch (Exception e) {
            System.out.println("No score file found. Starting at 0.");
        }
        return this;
    }

    private void saveScore() {
        try {
            Files.write( Paths.get(SAVE_FILE_NAME), ("" + score).getBytes() );
        }
        catch (Exception e) {
            System.out.println("Failed to save score.");
        }
    }

    private Animatable<NoteGuesserViewModel> activatedPlayMode( boolean isActivated ) {
        return withFeedback( isActivated ? "Have Fun!" : "Choose:" )
               .withFeedbackColor( isActivated ? Color.GREEN : Color.WHITE )
               .animateFeedbackAndThen( m->m );
    }

    public Animatable<NoteGuesserViewModel> selectNoteIndex( int ni )
    {
        if (playMode) return Animatable.of(this);

        NoteGuesserViewModel vm = this;
        if ( currentNoteIndex == ni ) {
            vm = vm.withFeedback("Yes. Correct!")
                   .withFeedbackColor(new Color(30, 128, 0));
            if ( !playMode )
                vm = vm.withScore(score + 1);
            saveScore();
            return vm.animateFeedbackAndThen( m -> m.newRandomNoteIndex() );
        }
        else {
            vm = vm.withFeedback("Try again!")
                   .withFeedbackColor(Color.RED);
            int newScore = score - 10;
            saveScore();
            return vm.withScore(Math.max(0, newScore))
                     .animateFeedbackAndThen( m->m );
        }
    }

    private Animatable<NoteGuesserViewModel> animateFeedbackAndThen(Function<NoteGuesserViewModel, NoteGuesserViewModel> onEnd) {
        return Animatable.of(LifeTime.of(0.45, TimeUnit.SECONDS), this, new AnimationTransformation<NoteGuesserViewModel>() {
            @Override
            public NoteGuesserViewModel run(AnimationStatus status, NoteGuesserViewModel value) {
                return value.withFeedbackFontSize((int) (24 + status.pulse() * 16));
            }
            @Override
            public NoteGuesserViewModel finish(AnimationStatus status, NoteGuesserViewModel value) {
                return onEnd.apply(value.withFeedbackFontSize(24));
            }
        });
    }

    public int numWhiteNotes() { return 7 * 4; }

    public List<List<Note>> getOctaves() {
        List<List<Note>> octaves = new ArrayList<>();
        for ( int oi = 3; oi >= 0; oi-- ) {
            List<Note> notes = new ArrayList<>();
            for ( int i = 0; i < 7; i++ ) {
                int ni = oi * 7 + i;
                String name = noteNameOf(ni);
                notes.add( new Note( name, oi, ni, false ) );
                boolean addBlack = ( i != 0 && i != 3 );
                if ( addBlack ) {
                    String blackName = noteNameOf(ni-1) + "#" + noteNameOf(ni);
                    notes.add( new Note( blackName, oi, ni-1, true ) );
                }
            }
            octaves.add( notes );
        }
        return octaves;
    }

    public boolean isVisibleLine( int ni ) { return ni > 3 && ni < 13 || ni > 15 && ni < 25; }

    public boolean shouldDrawSupportLine( int ni ) {
        int currentNi = currentNoteIndex;
        if ( isVisibleLine(currentNi) ) return false;
        if ( isVisibleLine(ni) ) return false;
        if ( ni == currentNi ) return true;
        if ( ni > currentNi && ni < 4 ) return true;
        if ( ni < currentNi && ni > 24 ) return true;
        return false;
    }

    public NoteGuesserViewModel newRandomNoteIndex() {
        int newNi = -1;
        while ( newNi == -1 || newNi == currentNoteIndex )
            newNi = (int) ( Math.random() * numWhiteNotes() );

        return this.withFeedback("Choose:")
                   .withFeedbackColor(Color.WHITE)
                   .withCurrentNoteIndex(newNi);
    }

    public String noteNameOf( int ni ) {
        int octave = ni / 7;
        ni = ni % 7;
        String name = "";
        if ( ni == 0 ) name = "c";
        if ( ni == 1 ) name = "d";
        if ( ni == 2 ) name = "e";
        if ( ni == 3 ) name = "f";
        if ( ni == 4 ) name = "g";
        if ( ni == 5 ) name = "a";
        if ( ni == 6 ) name = "b";
        if      ( octave == 0 ) name = name.toUpperCase(Locale.ENGLISH);
        else if ( octave == 1 ) name = name.toLowerCase(Locale.ENGLISH);
        else if ( octave == 2 ) name = name.toLowerCase(Locale.ENGLISH) + "1";
        else if ( octave == 3 ) name = name.toLowerCase(Locale.ENGLISH) + "2";
        return name;
    }

    public boolean shouldShowHelpFor(int ni) {
        return ni % Math.pow(2, level()) == 0;
    }

}

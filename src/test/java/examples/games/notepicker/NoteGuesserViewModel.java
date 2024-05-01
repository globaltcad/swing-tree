package examples.games.notepicker;

import sprouts.Event;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationState;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NoteGuesserViewModel
{
    // A simple text file which stores nothing but the score number:
    private final static String SAVE_FILE_NAME = ".score.txt";

    private final Event repaint = Event.create();
    private final Var<String>  feedback = Var.of("Choose:");
    private final Var<Color>   feedbackColor = Var.of(Color.WHITE);
    private final Var<Integer> feedbackFontSize = Var.of(24);
    private final Var<Integer> currentNoteIndex = Var.of(0);
    private final Var<Boolean> playMode = Var.of(false).onChange(From.VIEW, it -> activatedPlayMode(it.get()) );

    private final Var<Integer> score = Var.of(0);
    private final Val<Integer> level = score.view( s -> s / 10 );

    public Event getRepaintEvent() { return repaint; }

    public Var<String> feedback() { return feedback; }

    public Var<Color> feedbackColor() { return feedbackColor; }

    public Var<Integer> feedbackFontSize() { return feedbackFontSize; }

    public Var<Integer> currentNoteIndex() { return currentNoteIndex; }

    public Var<Boolean> playMode() { return playMode; }

    public Val<Integer> score() { return score; }

    public Val<Integer> level() { return level; }


    public NoteGuesserViewModel() {
        loadScore();
        newRandomNoteIndex();
    }

    private void loadScore() {
        try {
            String scoreText = new String( Files.readAllBytes(Paths.get(SAVE_FILE_NAME)) );
            score.set( Integer.parseInt(scoreText) );
        }
        catch (Exception e) {
            System.out.println("No score file found. Starting at 0.");
        }
    }

    private void saveScore() {
        try {
            Files.write( Paths.get(SAVE_FILE_NAME), ("" + score.get()).getBytes() );
        }
        catch (Exception e) {
            System.out.println("Failed to save score.");
        }
    }

    private void activatedPlayMode( boolean isActivated ) {
        feedback.set( isActivated ? "Have Fun!" : "Choose:" );
        feedbackColor.set( isActivated ? Color.GREEN : Color.WHITE );
        repaint.fire();
        animateFeedbackAndThen( () -> {} );
    }

    public void selectNoteIndex( int ni )
    {
        if ( playMode.is(true) ) return;

        if ( currentNoteIndex.is(ni) ) {
            feedback.set( "Yes. Correct!" );
            feedbackColor.set( new Color(30, 128, 0) );
            if ( playMode.is(false) )
                score.set(score.get() + 1);
            animateFeedbackAndThen( () -> newRandomNoteIndex() );
        }
        else {
            feedback.set( "Try again!" );
            feedbackColor.set( Color.RED );
            int newScore = score.get() - 10;
            score.set(Math.max(0, newScore));
            animateFeedbackAndThen( () -> {} );
        }
        saveScore();
    }

    private void animateFeedbackAndThen(Runnable onEnd) {
        UI.animateFor(0.45, TimeUnit.SECONDS).go(new Animation() {
            @Override
            public void run( AnimationState state ) {
                feedbackFontSize.set((int) (24 + state.pulse() * 16));
            }
            @Override
            public void finish( AnimationState state ) {
                feedbackFontSize.set(24);
                onEnd.run();
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
        int currentNi = currentNoteIndex.get();
        if ( isVisibleLine(currentNi) ) return false;
        if ( isVisibleLine(ni) ) return false;
        if ( ni == currentNi ) return true;
        if ( ni > currentNi && ni < 4 ) return true;
        if ( ni < currentNi && ni > 24 ) return true;
        return false;
    }

    public void newRandomNoteIndex() {
        int newNi = -1;
        while ( newNi == -1 || newNi == currentNoteIndex.get() )
            newNi = (int) ( Math.random() * numWhiteNotes() );

        feedback.set( "Choose:" );
        feedbackColor.set( Color.WHITE );
        currentNoteIndex.set( newNi );
        repaint.fire();
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
        if      ( octave == 0 ) name = name.toUpperCase();
        else if ( octave == 1 ) name = name.toLowerCase();
        else if ( octave == 2 ) name = name.toLowerCase() + "1";
        else if ( octave == 3 ) name = name.toLowerCase() + "2";
        return name;
    }

    public boolean shouldShowHelpFor(int ni) {
        return ni % Math.pow(2, level.get()) == 0;
    }

}

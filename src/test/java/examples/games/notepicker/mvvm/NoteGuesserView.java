package examples.games.notepicker.mvvm;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UIForPanel;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static swingtree.UI.*;

/**
 *  The view for the Note Guesser game in which you are presented with a note and you have to guess which note it is
 *  by pressing the correct key on a virtual piano keyboard.
 */
public class NoteGuesserView extends Panel
{
    private static final int INSTRUMENT = 0; // 0 is a piano, 9 is percussion, other channels are for other instruments
    private static final int VOLUME = 80; // between 0 et 127
    private final Color OLD_SHEET_MUSIC_COLOR = color(206, 198, 169,255);

    private final MidiChannel[] channels;


    static class KeyView {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final Note note;

        KeyView(int x, int y, int w, int h, Note note) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.note = note;
        }

        public boolean contains(int x, int y) {
            return x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h;
        }

        public int x() { return x; }
        public int y() { return y; }
        public int w() { return w; }
        public int h() { return h; }
        public Note note() { return note; }
    }

    public NoteGuesserView(NoteGuesserViewModel vm) {
        MidiChannel[] mifiChannels;
        try {
            // * Open a synthesizer
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            mifiChannels = synth.getChannels();
        } catch (Exception e) {
            mifiChannels = null;
            e.printStackTrace();
        }
        channels = mifiChannels;
        FlatLightLaf.setup();
        UIManager.put( "Button.arc", 25 );
        of(this).withBackground(OLD_SHEET_MUSIC_COLOR)
        .withLayout(FILL.and(INS(12)).and(WRAP(1)))
        .withPrefSize( 900, 700 )
        .withMinSize( 900, 700 )
        .add(ALIGN_X_CENTER,
            label("Which note is this?").withFont(Font.of("Arial", FontStyle.BOLD, 24))
        )
        .add(PUSH.and(GROW),
            panel(FILL).withRepaintOn(vm.getRepaintEvent())
            .withStyle( it ->
                it
                 .backgroundColor(OLD_SHEET_MUSIC_COLOR.brighter())
                 .foundationColor(color(255,255,255, 0))
                 .paddingTop(5)
                 .paddingLeft(25)
                 .paddingRight(25)
                 .paddingBottom(5)
                 .borderRadius(25, 25)
                 .borderWidth(2)
                 .borderColor(color(0, 0, 0,255))
                 .shadowColor(color(64,64,64,255))
                 .shadowBlurRadius(6)
                 .shadowSpreadRadius(1)
                 .shadowIsInset(true)
                 .painter(Layer.BACKGROUND, g2d -> {
                     int w = it.component().getWidth() - 25 - 25 - 100;
                     int h = it.component().getHeight() - 20 - 20 - 30;
                     int x = 25 + 50;
                     int y = 15 + 15;
                     noteLines(vm, g2d, x, y, w, h);
                 })
            )
        )
        .add(ALIGN_CENTER,
            panel(FILL_X.and(INS(8)).and(GAP_REL(0)))
            .withStyle( it -> it
                .borderRadius(24)
                .prefWidth(1028)
            )
            .add(SPAN.and(WRAP).and(GROW),
                panel(FILL).withStyle( it -> it
                    .minHeight(60)
                    .backgroundColor(Color.BLACK.brighter())
                    .borderRadiusAt(Corner.TOP_LEFT, 44, 24)
                    .borderRadiusAt(Corner.TOP_RIGHT, 44, 24)
                )
                .add(ALIGN_CENTER,
                    label(vm.feedback())
                    .withFontSize(vm.feedbackFontSize())
                    .withForeground(vm.feedbackColor())
                )
            )
            .add(SPAN.and(WRAP).and(ALIGN_CENTER).and(GROW_X),
                panel(FILL.and(INS(5))).withStyle( it -> it
                   .minHeight(150)
                   .border(3,0,0,0, Color.DARK_GRAY)
                )
                .apply( p -> buildKeyboard(p, vm) )
            )
        )
        .add(ALIGN_CENTER,
            panel().withStyle( it -> it.borderRadius(24) )
            .add(
                button("New Note").isEnabledIf(vm.playMode().view( b -> !b ))
                .onClick( e -> vm.newRandomNoteIndex() )
            )
            .add(
                toggleButton("Just Play!", vm.playMode())
            )
            .add(RIGHT,
                panel(INS(0, 12, 0, 12), "[]12[]")
                .add(
                    label(vm.score().viewAsString( s -> "Score: " + s ))
                    .withFontSize(12)
                    .isEnabledIf(vm.playMode().view( b -> !b )))
                .add(
                    label(vm.level().viewAsString( l -> "Level: " + l ))
                    .withFontSize(12)
                    .isEnabledIf(vm.playMode().view( b -> !b ))
                )
            )
        );
    }

    void buildKeyboard(UIForPanel<JPanel> p, NoteGuesserViewModel vm) {
        List<KeyView> whiteNotes  = new ArrayList<>();
        List<KeyView> blackNotes  = new ArrayList<>();
        List<KeyView> hoveredOver = new ArrayList<>();
        List<KeyView> pressed     = new ArrayList<>();
        p.onMouseMove( it -> {
            boolean isHoveringOverSomething = !hoveredOver.isEmpty();
            hoveredOver.clear();
            int x = it.mouseX();
            int y = it.mouseY();
            boolean found = false;
            for ( KeyView key : whiteNotes ) {
                if ( key.contains(x,y) ) {
                    found = true;
                    if ( !hoveredOver.contains(key) ) {
                        hoveredOver.add(key);
                        p.get(p.getType()).repaint();
                    }
                }
            }
            if ( !found && isHoveringOverSomething )
                p.get(p.getType()).repaint();
        });
        p.onMousePress( it -> {
            int x = it.mouseX();
            int y = it.mouseY();
            for ( KeyView key : whiteNotes ) {
                if ( key.contains(x,y) ) {
                    pressed.add(key);
                    p.get(p.getType()).repaint();
                    vm.selectNoteIndex(key.note.index());
                    play(key.note);
                }
            }
        });
        p.onMouseRelease( it -> {
            if ( !pressed.isEmpty() ) {
                pressed.clear();
                p.get(p.getType()).repaint();
            }
        });
        p.withStyle( it -> it
            .painter(Layer.BACKGROUND, g2d -> {
                int roundness = 12;
                whiteNotes.clear();
                blackNotes.clear();
                int width  = p.get(p.getType()).getWidth();
                int height = p.get(p.getType()).getHeight();
                int x = 0;
                int y = - ( roundness / 2 );
                Map<Integer,List<java.awt.Point>> octaveRanges = new HashMap<>();
                octaveRanges.put(0, new ArrayList<>());
                octaveRanges.put(1, new ArrayList<>());
                octaveRanges.put(2, new ArrayList<>());
                octaveRanges.put(3, new ArrayList<>());
                vm.getOctaves().forEach(octave ->
                    octave.forEach( note -> {
                        int ni = note.index();
                        int oi = note.octave();

                        // This app only supports white keys
                        // So we make dummy black keys for every white key with
                        // a black key in front and to the top left of it (if it exists)
                        int keyWidth  = width / vm.numWhiteNotes();
                        int keyHeight = (int) (height / 1.75);
                        int keyX = x + ni * keyWidth + (keyWidth / 4);
                        int keyY = y;
                        if ( note.isBlack() ) {
                            int key2X = keyX + keyWidth / 2;
                            int key2Y = (int) (keyY - (keyHeight / 2.75));
                            int padding = (int) (0.45 * (keyWidth)/2);
                            blackNotes.add( new KeyView(key2X+padding, key2Y+padding, keyWidth-2*padding, keyHeight-2*padding, note) );
                        } else {
                            whiteNotes.add( new KeyView(keyX, keyY, keyWidth, keyHeight, note) );
                        }

                        int labelX = keyX + keyWidth / 2;
                        int labelY = (int) (keyY + keyHeight * 1.25);
                        octaveRanges.get(oi).add( new java.awt.Point(labelX, labelY) );
                    })
                );
                g2d.setColor(OLD_SHEET_MUSIC_COLOR.brighter());
                g2d.fillRect(0,0,width,height);
                // First we draw the white keys:
                whiteNotes.forEach( key -> {
                    Color keyColor = Color.WHITE;
                    g2d.setColor(keyColor);
                    Shape rec = new RoundRectangle2D.Float(key.x, key.y, key.w, key.h, roundness, roundness);
                    g2d.fill(rec);
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.draw(rec);
                });
                hoveredOver.forEach( key -> {
                    Color keyColor = Color.LIGHTGRAY;
                    g2d.setColor(keyColor);
                    Shape rec = new RoundRectangle2D.Float(key.x, key.y, key.w, key.h, roundness, roundness);
                    g2d.fill(rec);
                    g2d.setColor(Color.GRAY);
                    g2d.draw(rec);

                });
                pressed.forEach( key -> {
                    Color keyColor = Color.GRAY;
                    g2d.setColor(keyColor);
                    Shape rec = new RoundRectangle2D.Float(key.x, key.y, key.w, key.h, roundness, roundness);
                    g2d.fill(rec);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.draw(rec);
                });
                whiteNotes.forEach( key -> {
                    boolean isCheating = vm.playMode().is(true);

                    // Finally we draw all the note labels on the left side of the scale:
                    int ni = key.note.index();
                    String note = vm.noteNameOf(ni);
                    if ( note != null ) {
                        boolean shouldDrawHelp = vm.shouldShowHelpFor(ni) || isCheating;
                        if ( shouldDrawHelp ) {
                            int fontSize = key.h / 6;
                            Font font = Font.of("Arial", FontStyle.BOLD, fontSize);
                            // Now we estimate the height and width of the note label:
                            java.awt.FontMetrics metrics = g2d.getFontMetrics(font.toAwtFont());
                            int noteWidth  = metrics.stringWidth(note);
                            int noteHeight = metrics.getHeight();
                            g2d.setFont(font.toAwtFont());
                            g2d.setColor(Color.BLACK);
                            int noteLabelX = key.x + key.w / 2 - noteWidth / 2;
                            int noteLabelY = key.y + key.h - noteHeight / 2;
                            g2d.drawString(note, noteLabelX, noteLabelY);
                        }
                    }
                });
                // Then we draw the black keys:
                blackNotes.forEach( key -> {
                    Color keyColor = Color.BLACK;
                    g2d.setColor(keyColor);
                    g2d.fill(new RoundRectangle2D.Float(key.x, key.y, key.w, key.h, roundness, roundness));
                });
                // Finally we draw the octave labels:
                octaveRanges.forEach( (oi, points) -> {
                    int labelX = 0;
                    int labelY = 0;
                    for ( java.awt.Point point : points ) {
                        labelX += point.x;
                        labelY += point.y;
                    }
                    labelX /= points.size();
                    labelY /= points.size();
                    String label = "";
                    if ( oi == 0 ) label = "Great Octave";
                    if ( oi == 1 ) label = "Small Octave";
                    if ( oi == 2 ) label = "One-line Octave";
                    if ( oi == 3 ) label = "Two-line Octave";
                    g2d.setFont(Font.of("Arial", FontStyle.BOLD, 16).toAwtFont());
                    java.awt.FontMetrics metrics = g2d.getFontMetrics();
                    int estimatedWidth  = metrics.stringWidth(label);
                    int estimatedHeight = metrics.getHeight();
                    // We draw the label so that labelX and labelY are the center of the label:
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.drawString(label, labelX - estimatedWidth / 2, labelY + estimatedHeight / 2);
                });
            })
        );
    }

    void noteLines(
        NoteGuesserViewModel vm,
        java.awt.Graphics2D g2d,
        int x, int y, int w, int h
    ) {
        Color color = vm.playMode().is(true) ? Color.LIGHTGRAY : Color.BLACK;
        int crop = Math.abs( w - h ) / 2;
        if ( w <= h ) { y += crop; h = w; }

        // Let's start the loop that drwas the lines:
        for ( int ni = 0; ni < (vm.numWhiteNotes()); ni++ ) {
            int lineStartX = x;
            int lineY = y + (h - h * ni / (vm.numWhiteNotes()));
            int lineEndX = x + w;
            int distanceBetween2Lines = 2 * (h / (vm.numWhiteNotes()));

            boolean shouldDrawFullLine = vm.isVisibleLine(ni);

            boolean isActuallyALine = ( ni % 2 == 1 );

            if ( !isActuallyALine ) {
                float alpha = (float) ( 0.175f / Math.pow(2,vm.level().get()) );
                g2d.setStroke(new java.awt.BasicStroke(2));
                g2d.setColor(shouldDrawFullLine ? color : color(0,0,0,alpha));
                g2d.drawLine(lineStartX, lineY, lineEndX, lineY);

                // Now we draw support lines depending on the note index:
                if ( !shouldDrawFullLine && vm.shouldDrawSupportLine(ni) ) {
                    int supportLineLength = (int) (distanceBetween2Lines * 1.95f);
                    int lineCenterX = (lineStartX + lineEndX) / 2 - supportLineLength / 2 - 1;
                    int lineCenterY = lineY;
                    g2d.setColor(color);
                    g2d.drawLine(lineCenterX, lineCenterY, lineCenterX + supportLineLength, lineCenterY);
                }
            }

            // Now, in the middle of the screen we draw the currently (randomly) chosen note:
            if ( vm.currentNoteIndex().is(ni) ) {
                // The note is an oval:
                int ovalW = (int) (distanceBetween2Lines * 1.5f);
                int ovalX = x + w / 2 - ovalW / 2;
                int ovalY = lineY - (distanceBetween2Lines / 2);
                int ovalH = distanceBetween2Lines;
                g2d.setColor(color);
                g2d.fillOval(ovalX, ovalY, ovalW, ovalH);
            }

            boolean isJustPlaying = vm.playMode().is(true);

            // Finally we draw all the note labels on the left side of the scale:
            String note = vm.noteNameOf(ni);
            if ( note != null ) {
                boolean shouldDrawHelp = vm.shouldShowHelpFor(ni) || isJustPlaying;
                if ( shouldDrawHelp ) {
                    int noteLabelX = x - 30 + ( isActuallyALine ? 15 : 0 );
                    int noteLabelY = lineY + distanceBetween2Lines / 5;
                    g2d.setFont(Font.of("Arial", FontStyle.BOLD, distanceBetween2Lines / 2).toAwtFont());
                    g2d.setColor(color);
                    g2d.drawString(note, noteLabelX, noteLabelY);
                }
            }
        }
    }

    private void play(Note note) {
        channels[INSTRUMENT].noteOn(note.midiIndex(), VOLUME );
    }
}

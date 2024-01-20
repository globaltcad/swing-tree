package examples.games;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class NoteGuesserView extends Panel
{
    private final Color OLD_SHEET_MUSIC_COLOR = new Color(206, 198, 169,255);

    public NoteGuesserView(NoteGuesserViewModel vm) {
        FlatLightLaf.setup();
        UIManager.put( "Button.arc", 25 );
        of(this).withBackground(OLD_SHEET_MUSIC_COLOR)
        .withLayout(FILL.and(INS(12, 12, 12, 12)).and(WRAP(1)))
        .withPrefSize( 900, 700 )
        .withMinSize( 900, 700 )
        .add(ALIGN_X_CENTER,
            label("Which note is this?").withFont(new Font("Arial", Font.BOLD, 24))
        )
        .add(PUSH.and(GROW),
            panel(FILL).withRepaintOn(vm.getRepaintEvent())
            .withStyle( it ->
                it
                 .backgroundColor(OLD_SHEET_MUSIC_COLOR.brighter())
                 .foundationColor(new Color(255,255,255, 0))
                 .font("Papyrus", 24)
                 .paddingTop(5)
                 .paddingLeft(25)
                 .paddingRight(25)
                 .paddingBottom(5)
                 .borderRadius(25, 25)
                 .borderWidth(3)
                 .borderColor(new Color(0, 0, 0,255))
                 .shadowColor(new Color(64,64,64,255))
                 .shadowBlurRadius(6)
                 .shadowSpreadRadius(3)
                 .shadowIsInset(true)
                 .painter(UI.Layer.BACKGROUND, g2d -> {
                     int w = it.component().getWidth() - 25 - 25 - 100;
                     int h = it.component().getHeight() - 20 - 20 - 30;
                     int x = 25 + 50;
                     int y = 15 + 15;
                     noteLines(vm, g2d, x, y, w, h);
                 })
            )
        )
        .add(ALIGN_CENTER,
            panel(FILL_X.and(INS(8))).withStyle( it -> it.borderRadius(24) )
            .add(SPAN.and(WRAP).and(GROW),
                panel(FILL).withMinHeight(60)
                .add(ALIGN_CENTER,
                    label(vm.feedback())
                    .withFontSize(vm.feedbackFontSize())
                    .withForeground(vm.feedbackColor())
                )
            )
            .add(SPAN.and(WRAP).and(ALIGN_CENTER).and(ALIGN_Y_TOP).and(GROW_X),
                separator()
            )
            .add(SPAN.and(WRAP).and(ALIGN_CENTER),
                panel(FILL.and(INS(5)))
                .apply( p ->
                    vm.getOctaves().forEach(octave ->
                        octave.forEach( note -> {
                            int ni = note.index();
                            int oi = note.octave();
                            if ( oi == 0 && ni % 7 == 0 ) p.add(label("Great Octave:"));
                            if ( oi == 1 && ni % 7 == 0 ) p.add(label("Small Octave:"));
                            if ( oi == 2 && ni % 7 == 0 ) p.add(label("One-line Octave:"));
                            if ( oi == 3 && ni % 7 == 0 ) p.add(label("Two-line Octave:"));
                            p.add("push, grow, " + ( (ni+1)%7 == 0 ? ", wrap" : "" ),
                                UI.button(note.name()).withMinSize(35,35)
                                .withFontSize(18)
                                .withBackground(OLD_SHEET_MUSIC_COLOR.brighter())
                                .onClick( e -> {
                                    vm.selectNoteIndex(note.index());
                                })
                            );
                        })
                    )
                )
            )
        )
        .add(ALIGN_CENTER,
            panel().withStyle( it -> it.borderRadius(24) )
            .add(
                button("New Note")
                .onClick( e -> vm.newRandomNoteIndex() )
            )
            .add(
                toggleButton("Cheat", vm.cheatMode())
            )
            .add(RIGHT,
                panel(INS(0, 12, 0, 12), "[]12[]")
                .add(label(vm.score().viewAsString( s -> "Score: " + s )).withFontSize(12))
                .add(label(vm.level().viewAsString( l -> "Level: " + l )).withFontSize(12))
            )
        );
    }

    void noteLines(
        NoteGuesserViewModel vm,
        Graphics2D g2d,
        int x, int y, int w, int h
    ) {
        int crop = Math.abs( w - h ) / 2;
        if ( w <= h ) { y += crop; h = w; }

        // Let's start the loop that drwas the lines:
        for ( int ni = 0; ni < (vm.numNotes()); ni++ ) {
            int lineStartX = x;
            int lineY = y + (h - h * ni / (vm.numNotes()));
            int lineEndX = x + w;
            int distanceBetween2Lines = 2 * (h / (vm.numNotes()));

            boolean shouldDrawFullLine = vm.isVisibleLine(ni);

            boolean isActuallyALine = ( ni % 2 == 1 );

            if ( !isActuallyALine ) {
                float alpha = (float) ( 0.175f / Math.pow(2,vm.level().get()) );
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(shouldDrawFullLine ? Color.BLACK : new Color(0,0,0,alpha));
                g2d.drawLine(lineStartX, lineY, lineEndX, lineY);

                // Now we draw support lines depending on the note index:
                if ( !shouldDrawFullLine && vm.shouldDrawSupportLine(ni) ) {
                    int supportLineLength = (int) (distanceBetween2Lines * 1.95f);
                    int lineCenterX = (lineStartX + lineEndX) / 2 - supportLineLength / 2 - 1;
                    int lineCenterY = lineY;
                    g2d.setColor(Color.BLACK);
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
                g2d.setColor(Color.BLACK);
                g2d.fillOval(ovalX, ovalY, ovalW, ovalH);
            }

            boolean isCheating = vm.cheatMode().is(true);

            // Finally we draw all the note labels on the left side of the scale:
            String note = vm.noteNameOf(ni);
            if ( note != null ) {
                boolean shouldDraw = ni % Math.pow(2, vm.level().get()) == 0 || isCheating;
                if ( shouldDraw ) {
                    int noteLabelX = x - 30 + (isActuallyALine ? 15 : 0);
                    int noteLabelY = lineY + distanceBetween2Lines / 5;
                    int fontSize = distanceBetween2Lines / 2;
                    Font derivedFont = g2d.getFont().deriveFont((float) fontSize);
                    derivedFont = derivedFont.deriveFont(derivedFont.getStyle() | Font.BOLD);
                    g2d.setFont(derivedFont);
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(note, noteLabelX, noteLabelY);
                }
            }
        }

    }

    public static void main(String... args) {
        NoteGuesserViewModel vm = new NoteGuesserViewModel();
        NoteGuesserView view = new NoteGuesserView(vm);
        UI.show(view);
    }
}

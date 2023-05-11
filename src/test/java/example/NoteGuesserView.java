package example;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import java.awt.*;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class NoteGuesserView extends Panel
{
    public NoteGuesserView(NoteGuesserViewModel vm) {
        FlatLightLaf.setup();
        of(this)
        .withLayout(FILL.and(INS(15)).and(WRAP(1)))
        .withPrefSize( 900, 700 )
        .withMinSize( 900, 700 )
        .add(ALIGN_X_CENTER,
            label("Which note is this?").withFont(new Font("Arial", Font.BOLD, 24))
        )
        .add(PUSH.and(GROW),
            panel(FILL)
            .withStyle( it ->
                it.style()
                 .backgroundColor(new Color(229, 218, 188,255))
                 .foundationColor(new Color(255,255,255, 0))
                 .padTop(10)
                 .padLeft(15)
                 .padRight(15)
                 .padBottom(10)
                 .borderRadius(25, 25)
                 .borderWidth(3)
                 .borderColor(new Color(0, 0, 0,255))
                 .shadowColor(new Color(64,64,64,255))
                 .shadowBlurRadius(6)
                 .shadowSpreadRadius(5)
                 .shadowIsInset(true)
                 .backgroundRenderer( g2d -> {
                     int w = it.component().getWidth() - 25 - 25 - 100;
                     int h = it.component().getHeight() - 20 - 20 - 50;
                     int x = 25 + 50;
                     int y = 15 + 25;
                     noteLines(vm, g2d, x, y, w, h);
                 })
            )
        )
        .add(ALIGN_CENTER,
            panel(FILL_X.and(INS(24)))
            .add(SPAN.and(WRAP).and(GROW),
                panel(FILL).withMinHeight(60)
                .add(ALIGN_CENTER,
                    label(vm.feedback())
                    .withFontSize(vm.feedbackFontSize())
                    .withForeground(vm.feedbackColor())
                )
            )
            .add(SPAN.and(WRAP).and(ALIGN_CENTER).and(ALIGN_Y_TOP),
                separator().withLength(300)
            )
            .add(SPAN.and(WRAP).and(ALIGN_CENTER),
                panel(FILL)
                .apply( p -> {
                    vm.getOctaves().forEach(octave -> {
                        octave.forEach( note -> {
                            int ni = note.index();
                            int oi = note.octave();
                            if ( oi == 0 && ni % 7 == 0 ) p.add(label("Great Octave:"));
                            if ( oi == 1 && ni % 7 == 0 ) p.add(label("Small Octave:"));
                            if ( oi == 2 && ni % 7 == 0 ) p.add(label("One-line Octave:"));
                            if ( oi == 3 && ni % 7 == 0 ) p.add(label("Two-line Octave:"));
                            p.add("grow, " + ( (ni+1)%7 == 0 ? ", wrap" : "" ),
                                button(note.name())
                                .withFontSize(18)
                                .onClick( e -> {
                                    vm.selectNoteIndex(note.index());
                                })
                            );
                        });
                    });
                })
            )
        )
        .add(ALIGN_CENTER,
            panel()
            .add(
                button("New Note")
                .onClick( e -> vm.newRandomNoteIndex() )
            )
            .add(
                toggleButton("Cheat", vm.cheatMode())
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
            int lineStartY = y + (h - h * ni / (vm.numNotes()));
            int lineEndX = x + w;
            int lineEndY = lineStartY;

            boolean shouldDrawLine = vm.isVisibleLine(ni);

            boolean isActuallyALine = ( ni % 2 == 1 );

            if ( !isActuallyALine ) {
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(shouldDrawLine ? Color.BLACK : new Color(0,0,0,0.2f));
                g2d.drawLine(lineStartX, lineStartY, lineEndX, lineEndY);
            }

            int distanceBetween2Lines = 2 * (h / (vm.numNotes()));
            // Now, in the middle of the screen we draw the currently (randomly) chosen note:
            if ( vm.currentNoteIndex().is(ni) ) {
                // The note is an oval:
                int ovalX = x + w/2 - 20;
                int ovalY = lineStartY - (distanceBetween2Lines / 2);
                int ovalW = (int) (distanceBetween2Lines * 1.5f);
                int ovalH = distanceBetween2Lines;
                g2d.setColor(Color.BLACK);
                g2d.fillOval(ovalX, ovalY, ovalW, ovalH);
            }

            if ( vm.cheatMode().is(true) ) {
                // Finally we draw all the note labels on the left side of the scale:
                String note = vm.noteNameOf(ni);
                if ( note != null ) {
                    int noteLabelX = x - 30 + ( isActuallyALine ? 15 : 0 );
                    int noteLabelY = lineStartY + 8;
                    // First let's make the font a little bit smaller:
                    g2d.setFont(new Font("Arial", Font.BOLD, distanceBetween2Lines / 2));
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

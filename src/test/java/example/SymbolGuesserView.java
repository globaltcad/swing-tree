package example;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class SymbolGuesserView extends Panel
{
    private final Color OLD_SHEET_MUSIC_COLOR = new Color(206, 198, 169,255);

    public SymbolGuesserView(SymbolGuesserViewModel vm) {
        FlatLightLaf.setup();
        UIManager.put( "Button.arc", 25 );
        of(this).withBackground(OLD_SHEET_MUSIC_COLOR)
            .withLayout(FILL.and(INS(12, 0, 12, 0)).and(WRAP(1)))
            .withPrefSize( 900, 700 )
            .withMinSize( 900, 700 )
            .add(ALIGN_X_CENTER,
                label("Which symbol is this?").withFont(new Font("Arial", Font.BOLD, 24))
            )
            .add(ALIGN_CENTER,
                panel(FILL.and(INS(42))).withRepaintIf(vm.getRepaintEvent())
                .withStyle( it ->
                    it.style()
                      .backgroundColor(OLD_SHEET_MUSIC_COLOR.brighter())
                      .foundationColor(new Color(255,255,255, 0))
                      .padTop(18)
                      .padLeft(18)
                      .padRight(18)
                      .padBottom(18)
                      .borderRadius(25, 25)
                      .borderWidth(1)
                      .borderColor(new Color(0, 0, 0,255))
                      .shadowColor(new Color(64,64,64,255))
                      .shadowBlurRadius(7)
                      .shadowSpreadRadius(2)
                      .shadowIsInset(false)
                )
                .add(ALIGN_CENTER,
                    label(vm.currentSymbol().viewAsString( s -> "<html>"+s+"</html>" ))
                    .withFontSize(68)
                )
            )
            .add(ALIGN_CENTER,
                panel(FILL_X.and(INS(8))).withStyle( it -> it.style().borderRadius(24) )
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
                    panel(INS(5))
                    .apply( p -> {
                        for ( int ai = 0; ai < vm.getAlphabets().size(); ai++ ) {
                            Alphabet alphabet = vm.getAlphabets().get(ai);
                            p.add(
                                panel()
                                .apply( p1 -> {
                                    p1.add(WRAP.and(SPAN), label(alphabet.name()).withFontSize(18));
                                    for ( int si = 0; si < alphabet.symbols().size(); si++ ) {
                                        Symbol symbol = alphabet.symbols().get(si);
                                        boolean newLine = (si + 1) % 5 == 0;
                                        p1.add( newLine ? "shrink, wrap" : "shrink",
                                            button(symbol.sound()).isEnabledIf( symbol.symbol() != '?' )
                                            .withFontSize(12)
                                            .withMinSize( 51, 51 ).withMaxSize( 51, 51 )
                                            .onClick( e -> vm.selectSymbol(symbol) )
                                        );
                                    }
                                })
                            );
                        }
                    })
                )
            )
            .add(ALIGN_CENTER,
                panel()
                .add(
                    button("New Note")
                    .onClick( e -> vm.newRandomSymbol() )
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

    public static void main( String... args ) {
        SymbolGuesserViewModel vm = new SymbolGuesserViewModel();
        SymbolGuesserView view = new SymbolGuesserView(vm);
        UI.show(view);
    }
}

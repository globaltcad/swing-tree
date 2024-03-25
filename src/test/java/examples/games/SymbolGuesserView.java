package examples.games;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import javax.swing.*;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class SymbolGuesserView extends Panel
{
    private final java.awt.Color BACKGROUND = new java.awt.Color(255, 255, 255,255);

    public SymbolGuesserView(SymbolGuesserViewModel vm) {
        FlatLightLaf.setup();
        UIManager.put( "Button.arc", 25 );
        of(this).withBackground(BACKGROUND)
        .withLayout(FILL.and(INS(24)).and(WRAP(2)), "[]16[]")
        .add(TOP,
            box(WRAP(1))
            .add(ALIGN_X_CENTER.and(GROW_X),
                panel(INS(12).and(WRAP(1)), "[grow]").withStyle( it -> it.borderRadius(24) )
                .add(ALIGN_X_CENTER,
                    label("Which symbol is this?").withFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24))
                )
                .add(ALIGN_X_CENTER,
                    panel(FILL.and(INS(48))).withRepaintOn(vm.getRepaintEvent())
                    .withStyle( it ->
                        it.backgroundColor(BACKGROUND.brighter())
                          .foundationColor(new java.awt.Color(255,255,255, 0))
                          .margin(22)
                          .padding(22)
                          .borderRadius(37)
                          .borderWidth(1)
                          .borderColor(new java.awt.Color(0, 0, 0,255))
                          .shadowColor(new java.awt.Color(64,64,64,255))
                          .shadowBlurRadius(7)
                          .shadowSpreadRadius(2)
                          .shadowIsInset(false)
                    )
                    .add(ALIGN_CENTER,
                        label(vm.currentSymbol().viewAsString( s -> "<html>"+s+"</html>" ))
                        .withFontSize(68)
                    )
                )
            )
            .add(PUSH_X.and(GROW_X), separator())
            .add(ALIGN_CENTER,
                panel(INS(12)).withStyle( it -> it.borderRadius(12) )
                .add(
                    button("New Symbol")
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
            )
        )
        .add(ALIGN_CENTER,
            panel(FILL_X.and(INS(12))).withStyle( it -> it.borderRadius(24) )
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
                        p.add(TOP,
                            panel()
                            .apply( p1 -> {
                                p1.add(WRAP.and(SPAN), label(alphabet.name()).withFontSize(18));
                                for ( int si = 0; si < alphabet.symbols().size(); si++ ) {
                                    Symbol symbol = alphabet.symbols().get(si);
                                    boolean newLine = (si + 1) % 5 == 0;
                                    p1.add( newLine ? "shrink, wrap" : "shrink",
                                        button(symbol.sound())
                                        .isEnabledIf( symbol.symbol() != '?' )
                                        .isEnabledIf( symbol.isEnabled() )
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
        );
    }

    public static void main( String... args ) {
        SymbolGuesserViewModel vm = new SymbolGuesserViewModel();
        SymbolGuesserView view = new SymbolGuesserView(vm);
        UI.show(view);
    }
}

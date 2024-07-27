package examples.games.kanapicker.mvi;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;

import javax.swing.JPanel;
import javax.swing.UIManager;

import static swingtree.UI.*;

public final class KanaPickerView extends Panel
{
    private final java.awt.Color BACKGROUND = new java.awt.Color(255, 255, 255,255);

    public KanaPickerView(Var<KanaPickerViewModel> vm) {
        Var<Symbol> currentSymbol = vm.zoomTo(KanaPickerViewModel::currentSymbol, KanaPickerViewModel::withCurrentSymbol);
        Var<Boolean> cheatMode = vm.zoomTo(KanaPickerViewModel::cheatMode, KanaPickerViewModel::withCheatMode);
        Var<Integer> score = vm.zoomTo(KanaPickerViewModel::score, KanaPickerViewModel::withScore);
        Val<Integer> level = vm.viewAsInt(KanaPickerViewModel::level);
        Var<String> feedback = vm.zoomTo(KanaPickerViewModel::feedback, KanaPickerViewModel::withFeedback);
        Var<Integer> feedbackFontSize = vm.zoomTo(KanaPickerViewModel::feedbackFontSize, KanaPickerViewModel::withFeedbackFontSize);
        Var<java.awt.Color> feedbackColor = vm.zoomTo(KanaPickerViewModel::feedbackColor, KanaPickerViewModel::withFeedbackColor);
        Var<java.util.List<Alphabet>> alphabets = vm.zoomTo(KanaPickerViewModel::alphabets, KanaPickerViewModel::withAlphabets);
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
                    panel(FILL.and(INS(48))).withRepaintOn(vm)
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
                        label(currentSymbol.viewAsString( s -> "<html>"+s+"</html>" ))
                        .withFontSize(68)
                    )
                )
            )
            .add(PUSH_X.and(GROW_X), separator())
            .add(ALIGN_CENTER,
                panel(INS(12)).withStyle( it -> it.borderRadius(12) )
                .add(
                    button("New Symbol")
                    .onClick( e -> vm.update(KanaPickerViewModel::newRandomSymbol) )
                )
                .add(
                    toggleButton("Cheat", cheatMode).onClick( it -> {
                        UI.animate(vm, KanaPickerViewModel::cheated);
                    })
                )
                .add(RIGHT,
                    panel(INS(0, 12, 0, 12), "[]12[]")
                    .add(label(score.viewAsString( s -> "Score: " + s )).withFontSize(12))
                    .add(label(level.viewAsString( l -> "Level: " + l )).withFontSize(12))
                )
            )
        )
        .add(ALIGN_CENTER,
            panel(FILL_X.and(INS(12))).withStyle( it -> it.borderRadius(24) )
            .add(SPAN.and(WRAP).and(GROW),
                panel(FILL).withMinHeight(60)
                .add(ALIGN_CENTER,
                    label(feedback)
                    .withFontSize(feedbackFontSize)
                    .withForeground(feedbackColor)
                )
            )
            .add(SPAN.and(WRAP).and(ALIGN_CENTER).and(ALIGN_Y_TOP).and(GROW_X),
                separator()
            )
            .add(SPAN.and(WRAP).and(ALIGN_CENTER),
                panel(INS(5)).peek( p -> {
                    buildAlphabetPanel(p, alphabets, vm);
                    alphabets.onChange(From.ALL, it -> {
                        p.removeAll();
                        buildAlphabetPanel(p, alphabets, vm);
                        p.revalidate();
                        p.repaint();
                    });
                })
            )
        );
    }

    private void buildAlphabetPanel(
        JPanel p,
        Var<java.util.List<Alphabet>> alphabets,
        Var<KanaPickerViewModel> vm
    ) {
        for ( int ai = 0; ai < alphabets.get().size(); ai++ ) {
            Alphabet alphabet = alphabets.get().get(ai);
            p.add(
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
                            .onClick( e -> {
                                UI.animate(vm, m->m.selectSymbol(symbol));
                                alphabets.fireChange(From.ALL);
                            })
                        );
                    }
                })
                .get(JPanel.class),
                "top"
            );
        }
    }

    public static void main( String... args ) {
        KanaPickerViewModel vm = new KanaPickerViewModel().enableSymbols().newRandomSymbol();
        Var<KanaPickerViewModel> state = Var.of(vm);
        UI.show( f -> new KanaPickerView(state) );
    }
}

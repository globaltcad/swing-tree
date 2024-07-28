package examples.games.notepicker.mvi;

import sprouts.Var;
import swingtree.UI;

public class Main {
    public static void main(String[] args) {
        Var<NoteGuesserViewModel> vm = Var.of(NoteGuesserViewModel.ini());
        UI.show( f -> new NoteGuesserView(vm) );
    }
}
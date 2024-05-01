package examples.games.notepicker;

import swingtree.UI;

public class Main {
    public static void main(String[] args) {
        NoteGuesserViewModel vm = new NoteGuesserViewModel();
        UI.show( f -> new NoteGuesserView(vm) );
    }
}
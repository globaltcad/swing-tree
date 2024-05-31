package examples.mvvm;

import sprouts.Var;
import sprouts.Vars;

/**
 *  This is the view model to the {@link ListBindingTestView}
 *  which is an example / test view demonstrates how the {@link javax.swing.JList}
 *  component type can be bound to a view model.
 *  The view shows a list of cells showing a colored square and respective name
 *  based on a property list of strings contained inside the view model.
 *  When the list of strings changes, the list of colors
 *  is updated accordingly.
 *  <p>
 *  On the right side, the view has 2 text fields which allow you to
 *  specify a modification range for the list of strings.
 *  <p>
 *  Underneath you can press either "Add" or "Remove" to modify the list
 *  of strings. The "Set Random" button will set a random list of strings
 *  and colors in the specified range.
 *  <p>
 *  This model primarily models the list of color strings and the
 *  two integer based range values.
 */
public class ListBindingTestViewModel
{
    private final Var<Integer> rangeMin = Var.of(0);
    private final Var<Integer> rangeMax = Var.of(0);
    private final Vars<String> colorNames = Vars.of(
                                                "red", "green", "blue", "yellow",
                                                    "orange", "purple"
                                            );

    public Var<Integer> rangeMin() {return rangeMin;}

    public Var<Integer> rangeMax() {return rangeMax;}

    public Vars<String> colorNames() {return colorNames;}

    public void add() {
        int min = rangeMin.get();
        int max = rangeMax.get();
        if (min < max) {
            int howMany = max - min;
            for (int i = 0; i < howMany; i++) {
                colorNames.addAt(min + i, randomColor());
            }
        }
    }

    public void remove() {
        int min = rangeMin.get();
        int max = rangeMax.get();
        if (min < max) {
            int howMany = max - min;
            if ( min == 0 ) {
                colorNames.removeFirst(howMany);
            } else if ( max == colorNames.size() ) {
                colorNames.removeLast(howMany);
            } else {
                for (int i = 0; i < howMany; i++) {
                    colorNames.removeAt(min);
                }
            }
        }
    }

    public void setRandom() {
        int min = rangeMin.get();
        int max = rangeMax.get();
        if (min < max) {
            for (int i = min; i < max; i++) {
                colorNames.setAt(i, randomColor());
            }
        }
    }

    private String randomColor() {
        String[] colors = new String[] {
            "red", "green", "blue", "yellow", "orange", "purple",
            "black", "white", "cyan", "magenta", "pink", "brown",
            "gray", "light gray", "dark gray", "maroon", "navy", "olive",
        };
        return colors[(int)(Math.random() * colors.length)];
    }
}

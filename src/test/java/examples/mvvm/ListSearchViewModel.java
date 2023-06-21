package examples.mvvm;

import sprouts.Var;
import sprouts.Vars;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 *  A view model showcasing how to model
 *  the state and logic of a view with various different types of
 *  UI components like lists, tables and other things...
 */
public class ListSearchViewModel {

    private final Vars<LocalDateTime> lastSearchTimes = Vars.of(LocalDateTime.now(), LocalDateTime.of(2018, 1, 1, 0, 0));
    private final Vars<String> searchTerms = Vars.of("foo", "bar");
    private final Var<String> keyword = Var.of("goo");
    private final Var<Integer> found = Var.of(0);
    private final Var<Boolean> searchEnabled = Var.of(true);
    private final Var<Boolean> searchRunning = Var.of(false);
    private final Var<Border> listBorder = Var.of(Border.class, BorderFactory.createEmptyBorder());
    private final Var<Border> searchBorder = Var.of(Border.class, BorderFactory.createEmptyBorder());

    private final Var<String> searchButtonText = Var.of("Search");

    private final Var<Color> validityColor = Var.of(Color.BLACK);
    private final List<Color> randomColors = new ArrayList<>();


    public Vars<LocalDateTime> lastSearchTimes() {return lastSearchTimes;}

    public Vars<String> searchTerms() {return searchTerms;}

    public Var<String> keyword() {return keyword;}

    public Var<Integer> found() {return found;}

    public Var<Boolean> searchEnabled() {return searchEnabled;}

    public Var<Boolean> searchRunning() {return searchRunning;}

    public Var<Border> listBorder() {return listBorder;}

    public Var<Border> searchBorder() {return searchBorder;}

    public Var<String> searchButtonText() {return searchButtonText;}

    public Var<Color> validityColor() {return validityColor;}

    public List<Color> getRandomColors() {return this.randomColors;}

    public void search() {
        validateKeyword();
        searchRunning.set(true);
        searchButtonText.set("Searching...");
        searchEnabled.set(false);
        searchBorder.set(BorderFactory.createLineBorder(Color.BLUE));
        listBorder.set(BorderFactory.createLineBorder(Color.BLUE));
        found.set(0);
        try {
            Thread.sleep(100);
            found.set(12);
            Thread.sleep(100);
            lastSearchTimes.add(LocalDateTime.now());
            Thread.sleep(100);
            searchTerms.add(keyword.get());
            Thread.sleep(100);
            this.keyword.set("Error: " + this.hashCode());
            Thread.sleep(500);
            randomColors.add(nextPseudoRandomColor());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        found.set(42);
        searchRunning.set(false);
        searchButtonText.set("Search");
        searchEnabled.set(true);
        searchBorder.set(BorderFactory.createEmptyBorder());
        listBorder.set(BorderFactory.createEmptyBorder());
        lastSearchTimes.add(LocalDateTime.now());
        searchTerms.add(keyword.get());
    }

    private Color nextPseudoRandomColor() {
        return new Color((int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255));
    }

    public void validateKeyword() {
        if ( keyword.get().length() < 3 )
            validityColor.set(Color.RED);
        else
            validityColor.set(Color.BLACK);
    }

    public void clearSearch() {
        keyword.set("");
        found.set(0);
    }

    public void clearHistory() {
        lastSearchTimes.clear();
    }

    public void clearTerms() {
        searchTerms.clear();
    }

    public void clearAll() {
        clearSearch();
        clearHistory();
        clearTerms();
        refill();
    }

    public void refill() {
        lastSearchTimes.add(LocalDateTime.now());
        lastSearchTimes.add(LocalDateTime.now());
        lastSearchTimes.add(LocalDateTime.now());
        lastSearchTimes.add(LocalDateTime.now());
        searchTerms.add("foo");
        searchTerms.add("bar");
        // Some more random stuff:
        searchTerms.add("baz");
        searchTerms.add("qux");
        searchTerms.add("quux");
    }
}

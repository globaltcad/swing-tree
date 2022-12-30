package example;

import swingtree.api.mvvm.Var;
import swingtree.api.mvvm.Vars;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.LocalDateTime;


/**
 *  A view model showcasing how to model
 *  the state and logic of a view with various different types of
 *  UI components like lists, tables and other things...
 */
public class ListSearchViewModel {

    private final Vars<LocalDateTime> lastSearchTimes = Vars.of(LocalDateTime.now(), LocalDateTime.of(2018, 1, 1, 0, 0));
    private final Vars<String> searchTerms = Vars.of("foo", "bar");
    private final Var<String> keyword = Var.of("foo");
    private final Var<Integer> found = Var.of(0);
    private final Var<Boolean> searchEnabled = Var.of(true);
    private final Var<Boolean> searchRunning = Var.of(false);
    private final Var<Border> listBorder = Var.of(BorderFactory.createEmptyBorder());
    private final Var<Border> searchBorder = Var.of(BorderFactory.createEmptyBorder());

    private final Var<String> searchButtonText = Var.of("Search");

    private final Var<Color> validityColor = Var.of(Color.BLACK);


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


    public void search() {
        searchRunning.set(true);
        searchButtonText.set("Searching...");
        searchEnabled.set(false);
        searchBorder.set(BorderFactory.createLineBorder(Color.BLUE));
        listBorder.set(BorderFactory.createLineBorder(Color.BLUE));
        found.set(0);
        try {
            Thread.sleep(1000);
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
    }
}

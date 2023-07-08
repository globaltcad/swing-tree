package examples.mvvm;

import sprouts.Var;
import sprouts.Vars;

public class ComposedViewModel
{
    private final Vars<String> entries = Vars.of(String.class);
    private final Vars<SubViewModel> subEntries = Vars.of(SubViewModel.class);
    private final Var<SubViewModel> maybeIExist = Var.ofNull(SubViewModel.class);


    public ComposedViewModel()
    {
        entries.addAll("One", "Two", "Three");
        subEntries.addAll(new SubViewModel("One"), new SubViewModel("Two"), new SubViewModel("Three"));
    }

    public Vars<String> entries() { return entries; }

    public Vars<SubViewModel> subEntries() { return subEntries; }

    public Var<SubViewModel> maybeIExist() { return maybeIExist; }

    public void toggleMaybeIExist() { maybeIExist.set(maybeIExist.isEmpty() ? new SubViewModel("I exist!") : null); }

    public static class SubViewModel
    {
        private final String text;
        private final Var<Boolean> selected = Var.of(false);

        public SubViewModel(String text) { this.text = text; }

        public String text() { return text; }

        public Var<Boolean> isSelected() { return selected; }
    }
}

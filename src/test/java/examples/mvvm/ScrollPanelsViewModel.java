package examples.mvvm;

import sprouts.Var;
import sprouts.Vars;
import swingtree.components.JScrollPanels;
import swingtree.UI;
import swingtree.api.mvvm.ViewableEntry;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  This is the view model of the {@link ScrollPanelsView} example UI,
 *  which demonstrates the usage of the {@link JScrollPanels} class.
 *  The {@link JScrollPanels} class is a custom Swing-Tree component which exists
 *  to compensate for the deficits of the {@link JList} and {@link JTable} components.
 *  Just like the former mentioned components, the {@link JScrollPanels} component is a
 *  container for a list of scrollable components.
 *  However, contrary to the {@link JList} and {@link JTable} components, the {@link JScrollPanels}
 *  allows for user interaction with the contained components, e.g. mouse clicks.
 */
public class ScrollPanelsViewModel
{
	private final Var<String> searchKey = Var.of("").onAct( it -> search() );
	private final Vars<EntryViewModel> entries = Vars.of(EntryViewModel.class);
	private final List<EntryViewModel> allEntries = new ArrayList<>();

	public Vars<EntryViewModel> entries() { return entries; }


	public ScrollPanelsViewModel()
	{
		String[] colorAdjectives = {"Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink", "Brown", "Black", "White"};
		String[] otherAdjectives = {"Big", "Small", "Round", "Square", "Long", "Short", "Fat", "Thin", "Tall", "Short"};
		String[] nouns = {
							"Potatoes", "Chickpeas", "Soybeans", "Tofu", "Seitan", "Tempeh", "Lentils", "Garbanzo Beans",
							"Lentils", "Soy Sauce", "Soybean Oil", "Soybean Meal", "Soybean Sprouts"
						};

		// Let's make use of the power of permutations to create a lot of entries
		for ( String color : colorAdjectives )
			for ( String adjective : otherAdjectives )
				for ( String noun : nouns )
					allEntries.add(new EntryViewModel(color + " " + adjective + " " + noun));

		entries.addAll(allEntries);
	}

	public Var<String> searchKey() { return searchKey; }

	public void addEntryAt(int index) { entries.addAt(index, new EntryViewModel("New entry!")); }

	private void search()
	{
		String key = searchKey.get();
		entries.clear();
		if ( key.isEmpty() )
			entries.addAll(allEntries);
		else
			entries.addAll(
					allEntries.stream()
					.filter(e -> e.text().get().toLowerCase().contains(key.toLowerCase()))
					.collect(Collectors.toList())
				);
	}

	public class EntryViewModel implements ViewableEntry
	{
		private final Var<Boolean> selected = Var.of(false);
		private final Var<Integer> position = Var.of(0);
		private final Var<String> text = Var.of("Hello world!");

		public EntryViewModel(String text) { this.text.set(text); }

		@Override
		public <V> V createView(Class<V> viewType) {
			return viewType.cast(UI.panel("fill")
						.add("pushx", UI.label(text))
						.add(UI.label(position.viewAs(String.class, s -> "Position: " + s)))
						.add(UI.label(selected.viewAs(String.class, s -> "Selected: " + s)))
						.add(UI.button("Delete me!").onClick(it -> {
							System.out.println("Deleting " + this.text.get());
							int i = entries.indexOf(this);
							entries.removeAt(i);
							if ( i != this.position.get() )
								throw new IllegalStateException("Index mismatch: " + i + " != " + this.position.get());
						}))
						.add(UI.button("Duplicate").onClick( it -> {
							int i = entries.indexOf(this);
							entries.addAt(i, new EntryViewModel(this.text.get() + " (copy)"));
						}))
					    .add(UI.button("up").onClick( it -> {
							int i = entries.indexOf(this);
							if ( i > 0 ) {
								entries.removeAt(i);
								entries.addAt(i - 1, this);
							}
						}))
						.add(UI.button("down").onClick( it -> {
							int i = entries.indexOf(this);
							if ( i < entries.size() - 1 ) {
								entries.removeAt(i);
								entries.addAt(i + 1, this);
							}
						}))
						.add(UI.button("replace").onClick( it -> {
							int i = entries.indexOf(this);
							entries.setAt(i, new EntryViewModel("Replaced!"));
						}))
						.getComponent());
		}

		@Override public Var<Boolean> isSelected() { return selected; }

		@Override public Var<Integer> position() { return position; }

		public Var<String> text() { return text; }

		@Override public String toString() { return "Entry@"+Integer.toHexString(this.hashCode())+"["+this.text.get()+"]"; }
	}

}

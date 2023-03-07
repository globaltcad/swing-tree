package example;

import sprouts.Var;
import sprouts.Vars;
import swingtree.UI;
import swingtree.api.mvvm.ViewableEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScrollPanelsViewModel
{
	private final Var<String> searchKey = Var.of("").onAct( it -> search() );
	private final Vars<EntryViewModel> entries = Vars.of(EntryViewModel.class);
	private final List<EntryViewModel> allEntries = new ArrayList<>();

	public Vars<EntryViewModel> entries() { return entries; }


	public ScrollPanelsViewModel()
	{
		allEntries.add(new EntryViewModel("Potatoes"));
		allEntries.add(new EntryViewModel("Seitan"));
		allEntries.add(new EntryViewModel("Garbanzo Bean Flour"));
		allEntries.add(new EntryViewModel("Soybeans"));
		allEntries.add(new EntryViewModel("Chickpeas"));
		allEntries.add(new EntryViewModel("Legume Flour"));
		allEntries.add(new EntryViewModel("Soy Sauce"));
		allEntries.add(new EntryViewModel("Soybean Oil"));
		allEntries.add(new EntryViewModel("Soybean Meal"));
		allEntries.add(new EntryViewModel("Soybean Sprouts"));
		allEntries.add(new EntryViewModel("Pea Protein Isolate"));
		allEntries.add(new EntryViewModel("Tofu"));
		allEntries.add(new EntryViewModel("Soybean Nuts"));
		allEntries.add(new EntryViewModel("Soy Milk"));
		allEntries.add(new EntryViewModel("Soybean Hulls"));
		allEntries.add(new EntryViewModel("Lentils"));
		allEntries.add(new EntryViewModel("Lentil Flour"));
		allEntries.add(new EntryViewModel("Tempeh"));
		allEntries.add(new EntryViewModel("Lentil Sprouts"));
		allEntries.add(new EntryViewModel("Textured Vegetable Protein"));
		allEntries.add(new EntryViewModel("Pea Protein"));
		allEntries.add(new EntryViewModel("Tiny Peas"));
		allEntries.add(new EntryViewModel("Pea Flour"));
		allEntries.add(new EntryViewModel("Pea Sprouts"));
		allEntries.add(new EntryViewModel("Lima Beans"));
		allEntries.add(new EntryViewModel("Lima Bean Flour"));
		allEntries.add(new EntryViewModel("White Beans"));
		allEntries.add(new EntryViewModel("Zucchini"));
		allEntries.add(new EntryViewModel("Yellow Squash"));
		allEntries.add(new EntryViewModel("Fava Beans"));
		allEntries.add(new EntryViewModel("Garbanzo Beans"));
		allEntries.add(new EntryViewModel("Urad Beans"));
		allEntries.add(new EntryViewModel("Mung Beans"));
		allEntries.add(new EntryViewModel("Black Beans"));
		allEntries.add(new EntryViewModel("Kidney Beans"));
		allEntries.add(new EntryViewModel("Pinto Beans"));
		allEntries.add(new EntryViewModel("Black-Eyed Peas"));
		allEntries.add(new EntryViewModel("Red Beans"));
		allEntries.add(new EntryViewModel("Cranberry Beans"));
		allEntries.add(new EntryViewModel("Yangzhou Beans"));
		allEntries.add(new EntryViewModel("Xinjiang Beans"));
		allEntries.add(new EntryViewModel("Tepary Beans"));
		allEntries.add(new EntryViewModel("Hungarian Wax Beans"));
		allEntries.add(new EntryViewModel("Green Beans"));
		allEntries.add(new EntryViewModel("Climbing Beans"));
		allEntries.add(new EntryViewModel("Runner Beans"));
		allEntries.add(new EntryViewModel("String Beans"));
		allEntries.add(new EntryViewModel("Snap Beans"));
		allEntries.add(new EntryViewModel("Butter Beans"));
		allEntries.add(new EntryViewModel("Cannellini Beans"));
		allEntries.add(new EntryViewModel("Edamame"));
		allEntries.add(new EntryViewModel("Fava Beans"));


		allEntries.add(new EntryViewModel("Potatoes"));
		allEntries.add(new EntryViewModel("Seitan"));
		allEntries.add(new EntryViewModel("Garbanzo Bean Flour"));
		allEntries.add(new EntryViewModel("Soybeans"));
		allEntries.add(new EntryViewModel("Chickpeas"));
		allEntries.add(new EntryViewModel("Legume Flour"));
		allEntries.add(new EntryViewModel("Soy Sauce"));
		allEntries.add(new EntryViewModel("Soybean Oil"));
		allEntries.add(new EntryViewModel("Soybean Meal"));
		allEntries.add(new EntryViewModel("Soybean Sprouts"));
		allEntries.add(new EntryViewModel("Pea Protein Isolate"));
		allEntries.add(new EntryViewModel("Tofu"));
		allEntries.add(new EntryViewModel("Soybean Nuts"));
		allEntries.add(new EntryViewModel("Soy Milk"));
		allEntries.add(new EntryViewModel("Soybean Hulls"));
		allEntries.add(new EntryViewModel("Lentils"));
		allEntries.add(new EntryViewModel("Lentil Flour"));
		allEntries.add(new EntryViewModel("Tempeh"));
		allEntries.add(new EntryViewModel("Lentil Sprouts"));
		allEntries.add(new EntryViewModel("Textured Vegetable Protein"));
		allEntries.add(new EntryViewModel("Pea Protein"));
		allEntries.add(new EntryViewModel("Tiny Peas"));
		allEntries.add(new EntryViewModel("Pea Flour"));
		allEntries.add(new EntryViewModel("Pea Sprouts"));
		allEntries.add(new EntryViewModel("Lima Beans"));
		allEntries.add(new EntryViewModel("Lima Bean Flour"));
		allEntries.add(new EntryViewModel("White Beans"));
		allEntries.add(new EntryViewModel("Zucchini"));
		allEntries.add(new EntryViewModel("Yellow Squash"));
		allEntries.add(new EntryViewModel("Fava Beans"));
		allEntries.add(new EntryViewModel("Garbanzo Beans"));
		allEntries.add(new EntryViewModel("Urad Beans"));
		allEntries.add(new EntryViewModel("Mung Beans"));
		allEntries.add(new EntryViewModel("Black Beans"));
		allEntries.add(new EntryViewModel("Kidney Beans"));
		allEntries.add(new EntryViewModel("Pinto Beans"));
		allEntries.add(new EntryViewModel("Black-Eyed Peas"));
		allEntries.add(new EntryViewModel("Red Beans"));
		allEntries.add(new EntryViewModel("Cranberry Beans"));
		allEntries.add(new EntryViewModel("Yangzhou Beans"));
		allEntries.add(new EntryViewModel("Xinjiang Beans"));
		allEntries.add(new EntryViewModel("Tepary Beans"));
		allEntries.add(new EntryViewModel("Hungarian Wax Beans"));
		allEntries.add(new EntryViewModel("Green Beans"));
		allEntries.add(new EntryViewModel("Climbing Beans"));
		allEntries.add(new EntryViewModel("Runner Beans"));
		allEntries.add(new EntryViewModel("String Beans"));
		allEntries.add(new EntryViewModel("Snap Beans"));
		allEntries.add(new EntryViewModel("Butter Beans"));
		allEntries.add(new EntryViewModel("Cannellini Beans"));
		allEntries.add(new EntryViewModel("Edamame"));
		allEntries.add(new EntryViewModel("Fava Beans"));
		entries.addAll(allEntries);
	}

	public Var<String> searchKey() { return searchKey; }

	public void addEntryAt(int index)
	{
		entries.addAt(index, new EntryViewModel("New entry!"));
	}

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

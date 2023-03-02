package example;

import sprouts.Var;
import sprouts.Vars;
import swingtree.UI;
import swingtree.api.mvvm.ViewableEntry;

public class ScrollPanelsViewModel
{
	private final Vars<Entry> entries = Vars.of(Entry.class);

	public Vars<Entry> entries() { return entries; }


	public ScrollPanelsViewModel()
	{
		entries.add(new Entry());
		entries.add(new Entry());
		entries.add(new Entry());
		entries.add(new Entry());
		entries.add(new Entry());
		entries.add(new Entry());
		entries.add(new Entry());
	}

	public void addEntryAt(int index)
	{
		entries.addAt(index, new Entry());
	}

	public class Entry implements ViewableEntry
	{
		private final Var<Boolean> selected = Var.of(false);
		private final Var<Integer> position = Var.of(0);

		@Override
		public <V> V createView(Class<V> viewType) {
			return (V) UI.panel("fill")
						.add(UI.label(position.viewAs(String.class, s -> "Position: " + s)))
						.add(UI.label(selected.viewAs(String.class, s -> "Selected: " + s)))
						.add(UI.button("Delete me!").onClick(it -> entries.remove(this)))
						.getComponent();
		}

		@Override public Var<Boolean> isSelected() { return selected; }

		@Override public Var<Integer> position() { return position; }
	}

}

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

	public static class Entry implements ViewableEntry
	{
		private final Var<Boolean> selected = Var.of(false);
		private final Var<Integer> position = Var.of(0);

		@Override
		public <V> V createView(Class<V> viewType) {
			return (V) UI.panel("fill")
						.add(UI.label("Hello World!"))
						.add(UI.button("Click me!"))
						.getComponent();
		}

		@Override public Var<Boolean> isSelected() { return selected; }

		@Override public Var<Integer> position() { return position; }
	}

}

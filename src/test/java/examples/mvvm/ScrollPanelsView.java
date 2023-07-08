package examples.mvvm;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.threading.EventProcessor;
import swingtree.components.JScrollPanels;
import swingtree.UI;

import javax.swing.*;
import static swingtree.UI.*;

/**
 *  This little example UI demonstrates the usage of the {@link JScrollPanels} class.
 *  The {@link JScrollPanels} class is a custom Swing-Tree component which exists
 *  to compensate for the deficits of the {@link JList} and {@link JTable} components.
 *  Just like the former mentioned components, the {@link JScrollPanels} component is a
 *  container for a list of scrollable components.
 *  However, contrary to the {@link JList} and {@link JTable} components, the {@link JScrollPanels}
 *  allows for user interaction with the contained components, e.g. mouse clicks.
 *  The entries inside {@link JList} and {@link JTable} components
 *  unfortunately cannot be interacted with by the user.
 */
public class ScrollPanelsView extends Panel
{
	public ScrollPanelsView( ScrollPanelsViewModel vm )
	{
		of(this).withLayout("fill")
		.withPrefSize(900, 400)
		.add( "shrink", label("Something to scroll:") )
		.add("grow, wrap, pushx", textField(vm.searchKey()))
		.add( "shrink, span, wrap", separator() )
		.add( "grow, push, span, wrap",
			scrollPanels()
			.add(vm.entries(), evm ->// <-- Here we bind the entries to the scroll panels
				UI.panel("fill")
				.add("pushx", UI.label(evm.text()))
				.add(UI.label(evm.position().viewAs(String.class, s -> "Position: " + s)))
				.add(UI.label(evm.position().viewAs(String.class, s -> "Selected: " + s)))
				.add(UI.button("Delete me!").onClick(it -> {
					System.out.println("Deleting " + evm.text().get());
					int i = evm.entries().indexOf(evm);
					evm.entries().removeAt(i);
					if ( i != evm.position().get() )
						throw new IllegalStateException("Index mismatch: " + i + " != " + evm.position().get());
				}))
				.add(UI.button("Duplicate").onClick( it -> {
					int i = evm.entries().indexOf(evm);
					evm.entries().addAt(i, evm.createNew(evm.text().get() + " (copy)"));
				}))
				.add(UI.button("up").onClick( it -> {
					int i = evm.entries().indexOf(evm);
					if ( i > 0 ) {
						evm.entries().removeAt(i);
						evm.entries().addAt(i - 1, evm);
					}
				}))
				.add(UI.button("down").onClick( it -> {
					int i = evm.entries().indexOf(evm);
					if ( i < evm.entries().size() - 1 ) {
						evm.entries().removeAt(i);
						evm.entries().addAt(i + 1, evm);
					}
				}))
				.add(UI.button("replace").onClick( it -> {
					int i = evm.entries().indexOf(evm);
					evm.entries().setAt(i, evm.createNew("Replaced!"));
				}))
			)
		)
		.add( "shrink, span, wrap", separator() )
		.add( "shrink", button("Add entry").onClick(it -> vm.addEntryAt(0)) );
	}

	public static void main(String[] args)
	{
		FlatLightLaf.setup();
		UI.showUsing(EventProcessor.DECOUPLED, frame -> new ScrollPanelsView(new ScrollPanelsViewModel()));
		EventProcessor.DECOUPLED.join();
	}
}

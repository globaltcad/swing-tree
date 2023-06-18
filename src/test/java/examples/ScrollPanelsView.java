package examples;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.EventProcessor;
import swingtree.JScrollPanels;
import swingtree.UI;

import javax.swing.*;
import static swingtree.UI.*;

/**
 *  This little example UI demonstrates the usage of the {@link swingtree.JScrollPanels} class.
 *  The {@link swingtree.JScrollPanels} class is a custom Swing-Tree component which exists
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
			scrollPanels().add(vm.entries()) // <-- Here we bind the entries to the scroll panels
		)
		.add( "shrink, span, wrap", separator() )
		.add( "shrink", button("Add entry").onClick(it -> vm.addEntryAt(0)) );
	}

	public static void main(String[] args)
	{
		FlatLightLaf.setup();
		UI.showUsing(EventProcessor.DECOUPLED, frame -> new ScrollPanelsView(new ScrollPanelsViewModel()));
		UI.joinDecoupledEventProcessor();
	}
}

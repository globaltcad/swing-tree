package example;

import swingtree.UI;

import javax.swing.*;
import static swingtree.UI.*;

public class ScrollPanelsView extends JPanel
{
	public ScrollPanelsView( ScrollPanelsViewModel vm )
	{
		of(this).withLayout("fill, wrap 1")
		.withPrefSize(600, 400)
		.add( "shrink", label("Something to scroll:") )
		.add( "shrink", separator() )
		.add( "grow, push", scrollPanels().add(vm.entries()) )
		.add( "shrink", separator() )
		.add( "shrink", button("Add entry").onClick(it -> vm.addEntryAt(0)) );
	}

	public static void main(String[] args)
	{
		UI.show(new ScrollPanelsView(new ScrollPanelsViewModel()));
		UI.joinDecoupledEventProcessor();
	}
}

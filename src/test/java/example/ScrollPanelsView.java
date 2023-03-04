package example;

import swingtree.EventProcessor;
import swingtree.UI;

import javax.swing.*;
import static swingtree.UI.*;

public class ScrollPanelsView extends JPanel
{
	public ScrollPanelsView( ScrollPanelsViewModel vm )
	{
		of(this).withLayout("fill")
		.withPrefSize(600, 400)
		.add( "shrink", label("Something to scroll:") )
		.add("grow, wrap, pushx", textField(vm.searchKey()))
		.add( "shrink, span, wrap", separator() )
		.add( "grow, push, span, wrap", scrollPanels().add(vm.entries()) )
		.add( "shrink, span, wrap", separator() )
		.add( "shrink", button("Add entry").onClick(it -> vm.addEntryAt(0)) );
	}

	public static void main(String[] args)
	{
		UI.show(UI.use(EventProcessor.DECOUPLED, ()->new ScrollPanelsView(new ScrollPanelsViewModel())));
		UI.joinDecoupledEventProcessor();
	}
}

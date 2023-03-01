package swingtree;

import sprouts.Vals;
import sprouts.Var;
import swingtree.api.mvvm.Viewable;
import swingtree.api.mvvm.ViewableEntry;

import javax.swing.*;
import java.util.Objects;

public class UIForScrollPanels<P extends JScrollPanels> extends UIForScrollPane<P>
{
	/**
	 * Extensions of the {@link  UIForAbstractSwing} always wrap
	 * a single component for which they are responsible.
	 *
	 * @param component The JComponent type which will be wrapped by this builder node.
	 */
	public UIForScrollPanels( P component ) { super(component); }


	@Override
	protected void _add( JComponent component, Object conf ) {
		Objects.requireNonNull(component);
		JScrollPanels panels = this.getComponent();

		ViewableEntry entry = _entryFrom(component);
		if ( conf == null )
			panels.addEntry(entry);
		else
			panels.addEntry(conf.toString(), entry);
	}

	private ViewableEntry _entryFrom(JComponent component) {
		Var<Boolean> selected = Var.of(false);
		Var<Integer> position = Var.of(0);
		return new ViewableEntry() {
			@Override public Var<Boolean> isSelected() { return selected; }
			@Override public Var<Integer> position() { return position; }
			@Override public <V> V createView(Class<V> viewType) { return (V) component; }
		};
	}

	@Override
	protected void _addViewableProps( Vals<? extends Viewable> viewables, String attr ) {
		Runnable addAll = ()-> viewables.forEach( v -> {
									if ( attr == null )
										add(v.createView(JComponent.class));
									else
										add(attr, v.createView(JComponent.class));
								});

		_onShow( viewables, delegate -> {
			JScrollPanels panels = this.getComponent();
			// we simply redo all the components.
			switch ( delegate.changeType() ) {
				case SET:
				case ADD:
				case REMOVE:
					panels.removeAllEntries();
					addAll.run();
				break;
				case CLEAR: panels.removeAllEntries(); break;
				case NONE: break;
				default: throw new IllegalStateException("Unknown type: "+delegate.changeType());
			}
		});
		addAll.run();
	}
}

package swingtree;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public final class UIForJDialog<D extends JDialog> extends UIForAnyWindow<UIForJDialog<D>, D>
{
	private final BuilderState<D> _state;

	/**
	 * Instances of the {@link AbstractBuilder} as well as its subtypes always wrap
	 * a single component for which they are responsible.
	 *
	 * @param component The component type which will be wrapped by this builder node.
	 */
	UIForJDialog( D component ) {
		_state = new BuilderState<>(component);
	}

	@Override
	protected BuilderState<D> _state() {
		return _state;
	}


	@Override
	protected void _doAddComponent( Component newComponent, Object conf, D thisComponent ) {
		thisComponent.add( conf == null ? null : conf.toString(), newComponent );
	}

	@Override
	public void show() {
		JDialog dialog = getComponent();
		Component[] components = dialog.getComponents();
		dialog.setLocationRelativeTo(null); // Initial centering!v
		dialog.pack(); // Otherwise some components resize strangely or are not shown at all...
		// First let's check if the dialog has an owner:
		Window owner = dialog.getOwner();
		// If there is no owner, we make sure that the window is centered on the screen again but with the component:
		if ( owner == null )
			dialog.setLocationRelativeTo(null);
		else // Otherwise we center the dialog on the owner:
			dialog.setLocationRelativeTo(owner);

		// We set the size to fit the component:
		if ( components.length > 0 ) {
			Dimension size = dialog.getSize();
			if ( size == null ) // The dialog has no size! It is best to set the size to the preferred size of the component:
				size = components[0].getPreferredSize();

			if ( size == null ) // The component has no preferred size! It is best to set the size to the minimum size of the component:
				size = components[0].getMinimumSize();

			if ( size == null ) // The component has no minimum size! Let's just look up the size of the component:
				size = components[0].getSize();

			dialog.setSize(size);
		}

		dialog.setVisible(true);
	}

	@Override
	protected Optional<JRootPane> _getRootPaneOf(D thisWindow) {
		return Optional.ofNullable(thisWindow.getRootPane());
	}

	@Override
	protected void _setTitleOf( D thisWindow, String title ) {
		thisWindow.setTitle(title);
	}
}

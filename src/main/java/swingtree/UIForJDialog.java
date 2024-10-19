package swingtree;

import org.jspecify.annotations.Nullable;
import swingtree.layout.AddConstraint;

import javax.swing.JDialog;
import javax.swing.JRootPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.Objects;
import java.util.Optional;

public final class UIForJDialog<D extends JDialog> extends UIForAnyWindow<UIForJDialog<D>, D>
{
	private final BuilderState<D> _state;

	/**
	 * Instances of the {@link UIForAnyWindow} as well as its subtypes always wrap
	 * a single component for which they are responsible.
	 *
	 * @param state The state object modelling how the component should be built.
	 */
	UIForJDialog( BuilderState<D> state ) {
		Objects.requireNonNull(state);
		_state = state;
	}

	@Override
	protected BuilderState<D> _state() {
		return _state;
	}

	@Override
	protected UIForJDialog<D> _newBuilderWithState(BuilderState<D> newState ) {
		return new UIForJDialog<>(newState);
	}


	@Override
	protected void _addComponentTo(D thisComponent, Component addedComponent, @Nullable AddConstraint constraints) {
		thisComponent.add(addedComponent, constraints == null ? null : constraints.toConstraintForLayoutManager());
	}

	@Override
	public void show() {
		JDialog dialog = get(_state.componentType());
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

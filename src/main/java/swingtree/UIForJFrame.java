package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;

/**
 * A declarative builder for {@link JFrame} components.
 * Use {@link #show()} at the end of your declaration to display the {@link JFrame} window on the screen.
 *
 * @param <F> The type of the {@link JFrame} that this builder is responsible for.
 */
public final class UIForJFrame<F extends JFrame> extends UIForAnyWindow<UIForJFrame<F>, F>
{
	private final BuilderState<F> _state;

	/**
	 * @param state The {@link BuilderState} modelling how the underlying component is built.
	 */
	UIForJFrame( BuilderState<F> state ) {
		Objects.requireNonNull(state);
		_state = state;
	}

	@Override
	protected BuilderState<F> _state() {
		return _state;
	}

	@Override
	protected UIForJFrame<F> _newBuilderWithState(BuilderState<F> newState ) {
		return new UIForJFrame<>(newState);
	}

	@Override
	protected void _addComponentTo(F thisComponent, Component addedComponent, @Nullable Object constraints) {
		thisComponent.add(constraints == null ? null : constraints.toString(), addedComponent);
	}

	@Override
	public void show() {
		JFrame frame = get(_state.componentType());
		Component[] components = frame.getComponents();
		frame.setLocationRelativeTo(null); // Initial centering!v
		frame.pack(); // Otherwise some components resize strangely or are not shown at all...
		// Make sure that the window is centered on the screen again but with the component:
		frame.setLocationRelativeTo(null);
		// We set the size to fit the component:
		if ( components.length > 0 ) {
			Dimension size = frame.getSize();
			if ( size == null ) // The frame has no size! It is best to set the size to the preferred size of the component:
				size = components[0].getPreferredSize();

			if ( size == null ) // The component has no preferred size! It is best to set the size to the minimum size of the component:
				size = components[0].getMinimumSize();

			if ( size == null ) // The component has no minimum size! Let's just look up the size of the component:
				size = components[0].getSize();

			frame.setSize(size);
		}

		frame.setVisible(true);
	}

	@Override protected Optional<JRootPane> _getRootPaneOf(F thisWindow) {
		return Optional.ofNullable(thisWindow.getRootPane());
	}

	@Override
	protected void _setTitleOf(F thisWindow, String title) {
		thisWindow.setTitle(title);
	}

}

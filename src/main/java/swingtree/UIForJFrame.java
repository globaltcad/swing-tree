package swingtree;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class UIForJFrame<F extends JFrame> extends UIForAnyWindow<UIForJFrame<F>, F>
{
	/**
	 * @param component The component type which will be wrapped by this builder node.
	 */
	public UIForJFrame( F component ) {
		super(component);
	}

	@Override
	protected void _doAddComponent( Component component, Object conf, F thisComponent ) {
		thisComponent.add(conf == null ? null : conf.toString(), component);
	}

	@Override
	public void show() {
		JFrame frame = getComponent();
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

	@Override protected Optional<JRootPane> _getRootPane() {
		return Optional.ofNullable(getComponent().getRootPane());
	}

	@Override
	protected void _setTitle(String title) {
		getComponent().setTitle(title);
	}

}

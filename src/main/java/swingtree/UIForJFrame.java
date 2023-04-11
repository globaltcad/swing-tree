package swingtree;

import javax.swing.*;
import java.awt.*;

public class UIForJFrame<F extends JFrame> extends UIForAnyWindow<UIForJFrame<F>, F>
{
	/**
	 * @param component The component type which will be wrapped by this builder node.
	 */
	public UIForJFrame( F component ) {
		super(component);
	}

	@Override
	protected void _add(Component component, Object conf) {
		getComponent().add(conf == null ? null : conf.toString(), component);
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
		if ( components.length > 0 )
			frame.setSize(components[0].getPreferredSize());

		frame.setVisible(true);
	}

	@Override protected JRootPane _getRootPane() { return getComponent().getRootPane(); }

	@Override
	protected void _setTitle(String title) {
		getComponent().setTitle(title);
	}

}

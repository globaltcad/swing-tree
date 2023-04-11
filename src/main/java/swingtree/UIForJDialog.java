package swingtree;

import javax.swing.*;
import java.awt.*;

public class UIForJDialog<D extends JDialog> extends UIForAnyWindow<UIForJDialog<D>, D>
{
	/**
	 * Instances of the {@link AbstractBuilder} as well as its subtypes always wrap
	 * a single component for which they are responsible.
	 *
	 * @param component The component type which will be wrapped by this builder node.
	 */
	public UIForJDialog(D component) { super(component); }

	@Override
	protected void _add(Component component, Object conf) {
		getComponent().add(conf == null ? null : conf.toString(), component);
	}

	@Override
	public void show() {
		JDialog dialog = getComponent();
		Component[] components = dialog.getComponents();
		dialog.setLocationRelativeTo(null); // Initial centering!v
		dialog.pack(); // Otherwise some components resize strangely or are not shown at all...
		// Make sure that the window is centered on the screen again but with the component:
		dialog.setLocationRelativeTo(null);
		// We set the size to fit the component:
		if ( components.length > 0 )
			dialog.setSize(components[0].getPreferredSize());

		dialog.setVisible(true);
	}

	@Override
	protected JRootPane _getRootPane() {
		return getComponent().getRootPane();
	}

	@Override
	protected void _setTitle(String title) {
		getComponent().setTitle(title);
	}
}

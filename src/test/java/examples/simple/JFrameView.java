package examples.simple;

import swingtree.input.Keyboard;

import javax.swing.*;

import static swingtree.UI.*;

/**
 *  A simple demonstration of how to use {@link JFrame} in SwingTree.
 */
public class JFrameView extends JFrame
{
	public JFrameView() {
		of(this)
		.onFocusGain(it -> System.out.println("Focus gained") )
		.onFocusLoss(it -> System.out.println("Focus lost") )
		.onPressed( Keyboard.Key.ENTER, it -> System.out.println("Enter key pressed") )
		.add(
			panel("fill")
			.add(
				button("Hello")
				.onFocusGain( it -> System.out.println("Button focus gained") )
				.onFocusLoss(it -> System.out.println("Button focus lost") )
				.onKeyPressed( it -> System.out.println("Button key pressed: " + it.getEvent().getKeyChar() ) )
				.onKeyReleased( it -> System.out.println("Button key released: " + it.getEvent().getKeyChar()) )
				.onKeyTyped( it -> System.out.println("Button key typed: " + it.getEvent().getKeyChar()) )
				.onTyped( Keyboard.Key.ENTER, it -> System.out.println("Button enter key typed") )
				.onTyped( Keyboard.Key.ESCAPE, it -> System.out.println("Button escape key typed") )
				.onClick( it -> {
					dialog(this, "I am a dialog!")
					.add(label("Something you might need to know."))
					.show();
				})
			)
		)
		.show();
	}

	public static void main(String[] args) {
		new JFrameView();
	}
}

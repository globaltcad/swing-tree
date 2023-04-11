package swingtree;

import sprouts.Val;
import swingtree.input.Keyboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public abstract class UIForAnyWindow<I extends UIForAnyWindow<I,W>, W extends Window> extends AbstractNestedBuilder<I,W,Component>
{
	/**
	 * Instances of the {@link AbstractBuilder} as well as its subtypes always wrap
	 * a single component for which they are responsible.
	 *
	 * @param component The component type which will be wrapped by this builder node.
	 */
	public UIForAnyWindow( W component ) { super(component); }

	/**
	 *  Adds a title to the window. <br>
	 *  Note that the way this is displayed depends on the window type and the
	 *  operating system.
	 *
	 * @param title The title to be shown in the top bar of the window.
	 * @return This builder.
	 */
	public final I withTitle( String title ) {
		_setTitle( title );
		return _this();
	}

	/**
	 *  Binds a text property to the window determining the title displayed in the top bar of the window. <br>
	 *  Note that the way this is displayed depends on the window type and the
	 *  operating system.
	 *
	 * @param title The title property whose text will be shown in the top bar of the window.
	 * @return This builder.
	 */
	public final I withTitle( Val<String> title ) {
		NullUtil.nullArgCheck(title, "title", Val.class);
		NullUtil.nullPropertyCheck(title, "title");
		_onShow( title, v -> _setTitle( v ) );
		return withTitle( title.orElseThrow() );
	}

	/**
	 *  Makes the window visible in the center of the screen.
	 */
	public abstract void show();

	protected abstract JRootPane _getRootPane();

	protected abstract void _setTitle( String title );

	private void _onKeyStroke(int code, Consumer<ActionEvent> action) {
		KeyStroke k = KeyStroke.getKeyStroke(code, 0);
		int w = JComponent.WHEN_IN_FOCUSED_WINDOW;
		_getRootPane().registerKeyboardAction(action::accept, k, w);
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link KeyListener} to the component,
	 * to receive key events triggered when the wrapped component receives a particular
	 * keyboard input matching the provided {@link swingtree.input.Keyboard.Key}.
	 * <br><br>
	 * @param key The {@link swingtree.input.Keyboard.Key} which should be matched to the key event.
	 * @param onKeyPressed The {@link sprouts.Action} which will be executed once the wrapped component received the targeted key press.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onPressed( Keyboard.Key key, sprouts.Action<WindowDelegate<W, ActionEvent>> onKeyPressed ) {
		NullUtil.nullArgCheck(key, "key", Keyboard.Key.class);
		NullUtil.nullArgCheck(onKeyPressed, "onKeyPressed", sprouts.Action.class);
		W window = getComponent();
		_onKeyStroke(key.code, e -> onKeyPressed.accept(_createDelegate(window, null)));
		return _this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.FocusListener}
	 * to the component, to receive those focus events where the wrapped component gains input focus.
	 *
	 * @param onFocus The {@link sprouts.Action} which should be executed once the input focus was gained on the wrapped component.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onFocusGained( sprouts.Action<WindowDelegate<W, FocusEvent>> onFocus ) {
		NullUtil.nullArgCheck(onFocus, "onFocus", sprouts.Action.class);
		W frame = getComponent();
		frame.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) {
				_doApp(()->onFocus.accept(_createDelegate(frame, e)));
			}
		});
		return _this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a focus listener
	 * to receive those focus events where the wrapped component loses input focus.
	 *
	 * @param onFocus The {@link sprouts.Action} which should be executed once the input focus was lost on the wrapped component.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onFocusLost( sprouts.Action<WindowDelegate<W, FocusEvent>> onFocus ) {
		NullUtil.nullArgCheck(onFocus, "onFocus", Action.class);
		W window = getComponent();
		window.addFocusListener(new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) {
				_doApp(()->onFocus.accept(_createDelegate(window, e)));
			}
		});
		return _this();
	}

	private <E> WindowDelegate<W, E> _createDelegate(W window, E event) {
		return new WindowDelegate<W, E>() {
			@Override public W get() { return window; }
			@Override public E getEvent() { return event; }
		};
	}

}

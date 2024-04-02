package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Val;
import swingtree.input.Keyboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *  A SwingTree builder node for configuring any kind of {@link Window} type.
 *  Take a look at the {@link UIForJDialog} and {@link UIForJFrame} classes,
 *  which are specialized subtypes of this class.
 *
 * @param <I> The type of the builder itself.
 * @param <W> The type of the window which is being configured by this builder.
 */
public abstract class UIForAnyWindow<I extends UIForAnyWindow<I,W>, W extends Window> extends UIForAnything<I,W,Component>
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UIForAnyWindow.class);

	/**
	 *  Adds a title to the window. <br>
	 *  Note that the way this is displayed depends on the window type and the
	 *  operating system.
	 *
	 * @param title The title to be shown in the top bar of the window.
	 * @return This builder.
	 */
	public final I withTitle( String title ) {
		return _with( thisWindow -> {
					_setTitleOf( thisWindow, title );
		       })
			   ._this();
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
		return _withOnShow( title, (thisWindow,v) -> {
			       _setTitleOf(thisWindow, v);
		       })
			   ._with( thisWindow -> {
			       _setTitleOf( thisWindow, title.orElseThrow() );
			   })
			   ._this();
	}

	/**
	 *  Sets the {@link UI.OnWindowClose} operation for the window. <br>
	 *  This translates to {@link JFrame#setDefaultCloseOperation(int)} or
	 *  {@link JDialog#setDefaultCloseOperation(int)} depending on the window type.
	 *  The following operations are supported:
	 *  <ul>
	 *      <li>{@link UI.OnWindowClose#DO_NOTHING} - Do nothing when the window is closed.</li>
	 *      <li>{@link UI.OnWindowClose#HIDE} - Hide the window when it is closed.</li>
	 *      <li>{@link UI.OnWindowClose#DISPOSE} - Dispose the window when it is closed.</li>
	 *  </ul>
	 * @param onClose The operation to be executed when the window is closed.
	 * @return This declarative builder instance to enable method chaining.
	 */
	public final I withOnCloseOperation(UI.OnWindowClose onClose ) {
		NullUtil.nullArgCheck(onClose, "onClose", UI.OnWindowClose.class);
		return _with( thisWindow -> {
					if ( thisWindow instanceof JFrame )
						((JFrame)thisWindow).setDefaultCloseOperation(onClose.forSwing());
					else if ( thisWindow instanceof JDialog )
						((JDialog)thisWindow).setDefaultCloseOperation(onClose.forSwing());
					else
						log.warn("Cannot set close operation on window of type: {}", thisWindow.getClass().getName());
		       })
			   ._this();
	}

	/**
	 *  Makes the window visible in the center of the screen.
	 */
	public abstract void show();

	protected abstract Optional<JRootPane> _getRootPaneOf(W thisWindow);

	protected abstract void _setTitleOf( W thisWindow, String title );

	private void _onKeyStroke( int code, Consumer<ActionEvent> action, W thisWindow ) {
		_getRootPaneOf(thisWindow).ifPresent(rootPane -> {
			KeyStroke k = KeyStroke.getKeyStroke(code, 0);
			int w = JComponent.WHEN_IN_FOCUSED_WINDOW;
			rootPane.registerKeyboardAction(action::accept, k, w);
		});
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
		return _with( thisWindow -> {
					_onKeyStroke( key.code, e -> onKeyPressed.accept(_createDelegate(thisWindow, null)), thisWindow );
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.FocusListener}
	 * to the component, to receive those focus events where the wrapped component gains input focus.
	 *
	 * @param onFocus The {@link sprouts.Action} which should be executed once the input focus was gained on the wrapped component.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onFocusGain( sprouts.Action<WindowDelegate<W, FocusEvent>> onFocus ) {
		NullUtil.nullArgCheck(onFocus, "onFocus", sprouts.Action.class);
		return _with( thisWindow -> {
					thisWindow.addFocusListener(new FocusAdapter() {
						@Override public void focusGained(FocusEvent e) {
							_runInApp(()->onFocus.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a focus listener
	 * to receive those focus events where the wrapped component loses input focus.
	 *
	 * @param onFocus The {@link sprouts.Action} which should be executed once the input focus was lost on the wrapped component.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onFocusLoss( sprouts.Action<WindowDelegate<W, FocusEvent>> onFocus ) {
		NullUtil.nullArgCheck(onFocus, "onFocus", Action.class);
		return _with( thisWindow -> {
					thisWindow.addFocusListener(new FocusAdapter() {
						@Override public void focusLost(FocusEvent e) {
							_runInApp(()->onFocus.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowListener#windowClosing(WindowEvent)} events
	 * which are invoked when a window is in the process of being closed.
	 * The close operation can be overridden at this point (see {@link JFrame#DO_NOTHING_ON_CLOSE}). <br>
	 * Note that this kind of event is typically triggered when the user clicks
	 * the close button in the top bar of the window.
	 *
	 * @param onClose The {@link sprouts.Action} which should be invoked when the wrapped component is in the process of being closed.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onClose( sprouts.Action<WindowDelegate<W, WindowEvent>> onClose ) {
		NullUtil.nullArgCheck(onClose, "onClose", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowClosing( WindowEvent e ) {
							_runInApp(()->onClose.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowListener#windowClosed(WindowEvent)} events
	 * which are invoked when a window has been closed. <br>
	 * Note that this kind of event is typically triggered when the user clicks
	 * the close button in the top bar of the window.
	 *
	 * @param onClose The {@link sprouts.Action} which should be invoked when the wrapped component has been closed.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onClosed( sprouts.Action<WindowDelegate<W, WindowEvent>> onClose ) {
		NullUtil.nullArgCheck(onClose, "onClose", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowClosed( WindowEvent e ) {
							_runInApp(()->onClose.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowListener#windowOpened(WindowEvent)} events
	 * which are invoked when a window has been opened. <br>
	 * Note that this kind of event is typically triggered when the user clicks
	 * the close button in the top bar of the window.
	 *
	 * @param onOpen The {@link sprouts.Action} which should be invoked when the wrapped component has been opened.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onOpened( sprouts.Action<WindowDelegate<W, WindowEvent>> onOpen ) {
		NullUtil.nullArgCheck(onOpen, "onOpen", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowOpened( WindowEvent e ) {
							_runInApp(()->onOpen.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowListener#windowIconified(WindowEvent)} events
	 * which are invoked when a window is changed from a normal to a minimized state.
	 * For many platforms, a minimized window is displayed as the icon
	 * specified in the window's iconImage property.
	 * <br>
	 * Minification is usually triggered when the user clicks the minimize button
	 * in the top bar of the window. But this depends on the operating system.
	 *
	 * @param onIconify The {@link sprouts.Action} which should be invoked when the wrapped component has been iconified.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onIconified( sprouts.Action<WindowDelegate<W, WindowEvent>> onIconify ) {
		NullUtil.nullArgCheck(onIconify, "onIconify", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowIconified( WindowEvent e ) {
							_runInApp(()->onIconify.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowListener#windowDeiconified(WindowEvent)} events
	 * which are invoked when a window is changed from a minimized
	 * to a normal state, usually by the user restoring it from the task bar.
	 *
	 * @param onDeiconify The {@link sprouts.Action} which should be invoked when the wrapped component has been deiconified.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onDeiconified( sprouts.Action<WindowDelegate<W, WindowEvent>> onDeiconify ) {
		NullUtil.nullArgCheck(onDeiconify, "onDeiconify", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowDeiconified( WindowEvent e ) {
							_runInApp(()->onDeiconify.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowListener#windowActivated(WindowEvent)} events
	 * which are invoked when the Window is set to be the active Window.
	 * Only a Frame or a Dialog can be the active Window.
	 * The native windowing system may denote the active Window or
	 * its children with special decorations, such as a highlighted title bar.
	 * The active Window is always either the focused Window,
	 * or the first Frame or Dialog that is an owner of the focused Window.
	 * So this kind of event is usually triggered when the user makes the window active
	 * by clicking it.
	 *
	 * @param onActivate The {@link sprouts.Action} which should be invoked when the wrapped component has been activated.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onActivated( sprouts.Action<WindowDelegate<W, WindowEvent>> onActivate ) {
		NullUtil.nullArgCheck(onActivate, "onActivate", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowActivated( WindowEvent e ) {
							_runInApp(()->onActivate.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowListener#windowDeactivated(WindowEvent)} events
	 * which are invoked when a Window is no longer the active Window. Only a Frame or a
	 * Dialog can be the active Window. The native windowing system may denote
	 * the active Window or its children with special decorations, such as a
	 * highlighted title bar. The active Window is always either the focused
	 * Window, or the first Frame or Dialog that is an owner of the focused
	 * Window.
	 * This kind of event typically occurs when the user clicks another window
	 * in the task bar of the operating system.
	 *
	 * @param onDeactivate The {@link sprouts.Action} which should be invoked when the wrapped component has been deactivated.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onDeactivated( sprouts.Action<WindowDelegate<W, WindowEvent>> onDeactivate ) {
		NullUtil.nullArgCheck(onDeactivate, "onDeactivate", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowDeactivated( WindowEvent e ) {
							_runInApp(()->onDeactivate.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowStateListener#windowStateChanged(WindowEvent)} events
	 * which are invoked when a window has been changed. <br>
	 * Note that this kind of event is typically invoked when the window is
	 * iconified, minimized, maximized or restored.
	 *
	 * @param onStateChanged The {@link sprouts.Action} which should be invoked when the wrapped component has been changed.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onStateChanged( sprouts.Action<WindowDelegate<W, WindowEvent>> onStateChanged ) {
		NullUtil.nullArgCheck(onStateChanged, "onStateChanged", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowListener(new WindowAdapter() {
						@Override public void windowStateChanged( WindowEvent e ) {
							_runInApp(()->onStateChanged.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowFocusListener#windowGainedFocus(WindowEvent)} events
	 * which are invoked when the window is set to be gaining input focus, which means
	 * that the Window, or one of its subcomponents, will receive keyboard
	 * events.
	 * This event is typically triggered when the user clicks the window.
	 *
	 * @param onFocusGained The {@link sprouts.Action} which should be invoked when the wrapped component has gained input focus.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onInputFocusGained( sprouts.Action<WindowDelegate<W, WindowEvent>> onFocusGained ) {
		NullUtil.nullArgCheck(onFocusGained, "onFocusGained", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowFocusListener(new WindowFocusListener() {
						@Override public void windowGainedFocus( WindowEvent e ) {
							_runInApp(()->onFocusGained.accept(_createDelegate(thisWindow, e)));
						}
						@Override public void windowLostFocus( WindowEvent e ) {}
					});
		       })
			   ._this();
	}

	/**
	 * Adds the supplied {@link sprouts.Action} wrapped in a {@link java.awt.event.WindowListener}
	 * to the component, to receive {@link WindowFocusListener#windowLostFocus(WindowEvent)} events
	 * which are invoked when the window is set to be losing input focus, which means
	 * that input focus is being transferred to another Window or no Window at all and
	 * that keyboard events will no longer be delivered to the Window or any of
	 * its subcomponents.
	 *
	 * @param onFocusLost The {@link sprouts.Action} which should be invoked when the wrapped component has lost input focus.
	 * @return This very instance, which enables builder-style method chaining.
	 */
	public final I onInputFocusLost( sprouts.Action<WindowDelegate<W, WindowEvent>> onFocusLost ) {
		NullUtil.nullArgCheck(onFocusLost, "onFocusLost", Action.class);
		return _with( thisWindow -> {
					thisWindow.addWindowFocusListener(new WindowFocusListener() {
						@Override public void windowGainedFocus( WindowEvent e ) {}
						@Override public void windowLostFocus( WindowEvent e ) {
							_runInApp(()->onFocusLost.accept(_createDelegate(thisWindow, e)));
						}
					});
		       })
			   ._this();
	}

	private <E> WindowDelegate<W, E> _createDelegate( W window, @Nullable E event ) {
		return new WindowDelegate<W, E>() {
			@Override public W get() { return window; }
			@Override public @Nullable E getEvent() { return event; }
		};
	}

}

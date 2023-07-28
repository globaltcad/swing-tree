package swingtree.threading;

/**
 * 	One of Swing's biggest drawback is that it is single threaded. This means that all
 * 	GUI events are processed on the same thread as the application logic. <br>
 * 	This is problematic for several reasons:
 * 	<ul>
 * 		<li> The GUI is not responsive while the application logic is running.
 * 			 For example, if you have a long running task, the GUI will not be updated
 * 			 until the task is finished.
 * 		<li> The GUI is not thread safe. If you try to update the GUI from a different thread
 * 			 than the GUI thread, you will get an exception.
 * 		<li> The GUI is not decoupled from the application logic. This means that the GUI
 * 			 is tightly coupled to the application logic, which makes it hard to test the GUI.
 * 		<li> Using your GUI thread not only for GUI events and rendering but also for application logic,
 * 	         will reduce the performance of your application.
 * 	</ul>
 *
 * 	Swing-Tree fixes this mess by decoupling the GUI from the application by delegating
 * 	the GUI events to a separate thread.
 * 	This is done through implementations of this {@link EventProcessor} interface.
 * 	<p>
 * 	If you want to do proper decoupling, you can switch to the decoupled event processor like so: <br>
 * 	<pre>{@code
 * 	use(EventProcessor.DECOUPLED, ()->
 *      UI.panel("fill")
 *      .add( "shrink", UI.label( "Username:" ) )
 *      .add( "grow, pushx", UI.textField("User1234..42") )
 *      .add( label( "Password:" ) )
 *      .add( "grow, pushx", UI.passwordField("child-birthday") )
 *      .add( "span",
 *          UI.button("Login!").onClick( it -> {...} )
 *      )
 *  );
 *  }</pre>
 * 	Note that by default, Swing-Tree uses a simple event processor which does not delegate GUI events to a separate thread.
 * 	So if you want to activate this powerful feature you have to change the event processor. <br>
 * 	See {@link EventProcessor#COUPLED}, {@link EventProcessor#COUPLED_STRICT} and {@link EventProcessor#DECOUPLED}.
 */
public interface EventProcessor
{
	/**
	 *  This event processor simply runs the events immediately without caring
	 *  about the thread they are executed on.
	 *  This means that events are executed on the GUI thread, which is the EDT in Swing.
	 *  However, events may also be executed on other threads, which is why this event processor
	 *  is usually used for testing.
	 *  Make sure an application thread is registered through
	 *  {@link DecoupledEventProcessor#join()} at {@link EventProcessor#DECOUPLED}
	 *  when using this processor, otherwise events will not be handled.
	 */
	EventProcessor COUPLED = new LenientAWTEventProcessor();
	/**
	 *  This event processor runs the events immediately on the GUI thread.
	 *  If the current thread is the GUI thread, the events are executed immediately,
	 *  otherwise an exception is thrown, and it's stack trace is printed
	 *  (but the application will not crash).
	 */
	EventProcessor COUPLED_STRICT = new AWTEventProcessor();
	/**
	 *  This event processor makes a distinction between application events and UI events.
	 *  Application events are executed on the application thread, whereas UI events are
	 *  executed on the GUI thread (AWT Event Dispatch Thread).
	 */
	DecoupledEventProcessor DECOUPLED = DecoupledEventProcessor.INSTANCE();

	/**
	 *   Adds the supplied task to an event queue for processing application events.
	 *   The task is executed in the application thread, which may be the GUI thread
	 *   (but shouldn't be if you are doing clean software development).
	 *   This method returns immediately.
	 *
	 * @param runnable The task to be executed in the application thread.
	 */
	void registerAppEvent( Runnable runnable );

	/**
	 *   Adds the supplied task to an event queue for processing application events
	 *   and then waits for the task to be processed on the application thread.
	 *   The task is executed in the application thread, which, depending on your implementation,
	 *   may also be the GUI thread (but shouldn't be if you are doing clean software development).
	 *   This method returns when the task has been processed.
	 *
	 * @param runnable The task to be executed in the application thread.
	 */
	void registerAndRunAppEventNow(Runnable runnable);

	/**
	 *   Adds the supplied task to an event queue for processing UI events.
	 *   The task is executed in the GUI thread, which is the EDT in Swing.
	 *   This method returns immediately.
	 *
	 * @param runnable The task to be executed in the GUI thread.
	 */
	void registerUIEvent(Runnable runnable);

	/**
	 *   Adds the supplied task to an event queue for processing UI events
	 *   and then waits for the task to be processed on the GUI thread.
	 *   The task is executed in the GUI thread, which is the EDT in Swing.
	 *   This method returns when the task has been processed.
	 *
	 * @param runnable The task to be executed in the GUI thread.
	 */
	void registerAndRunUIEventNow(Runnable runnable);

}

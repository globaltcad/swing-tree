package swingtree;

/**
 * 	One of Swing's biggest drawbacks is that it is single threaded. This means that all
 * 	GUI events are processed on the same thread as the application logic.
 * 	This is... <br>
 * 	<ul>
 * 		<li> not very efficient
 * 		<li> not very responsive
 * 		<li> not very scalable
 * 		<li> not very testable
 * 		<li> not very maintainable
 * 		<li> not very extensible
 * 		<li> not very ... (you get the idea)
 * 	</ul>
 *
 * 	Swing-Tree fixes this mess by decoupling the GUI from the application by delegating
 * 	the GUI events to a separate thread.
 * 	This is done through implementations of this {@link EventProcessor} interface.
 * 	By default, Swing-Tree uses a {@link DecoupledEventProcessor} which simply uses the
 * 	executes the GUI events immediately on the GUI thread.
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
 */
public interface EventProcessor
{
	EventProcessor COUPLED   = new CoupledEventProcessor();
	EventProcessor DECOUPLED = DecoupledEventProcessor.INSTANCE();


	/**
	 *   Adds the supplied task to an event queue for processing application events.
	 *   The task is executed in the application thread, which may be the GUI thread
	 *   (but shouldn't be if you are doing clean software development).
	 *   This method returns immediately.
	 *
	 * @param runnable The task to be executed in the application thread.
	 */
	void processAppEvent(Runnable runnable);

	/**
	 *   Adds the supplied task to an event queue for processing application events
	 *   and then waits for the task to be processed on the application thread.
	 *   The task is executed in the application thread, which, depending on your implementation,
	 *   may also be the GUI thread (but shouldn't be if you are doing clean software development).
	 *   This method returns when the task has been processed.
	 *
	 * @param runnable The task to be executed in the application thread.
	 */
	void processAppEventNow(Runnable runnable);

	/**
	 *   Adds the supplied task to an event queue for processing UI events.
	 *   The task is executed in the GUI thread, which is the EDT in Swing.
	 *   This method returns immediately.
	 *
	 * @param runnable The task to be executed in the GUI thread.
	 */
	void processUIEvent(Runnable runnable);

	/**
	 *   Adds the supplied task to an event queue for processing UI events
	 *   and then waits for the task to be processed on the GUI thread.
	 *   The task is executed in the GUI thread, which is the EDT in Swing.
	 *   This method returns when the task has been processed.
	 *
	 * @param runnable The task to be executed in the GUI thread.
	 */
	void processUIEventNow(Runnable runnable);

}

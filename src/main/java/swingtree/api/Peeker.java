package swingtree.api;

import swingtree.UIForAnySwing;

/**
 *  Applies an action to the current component typically as part of {@link UIForAnySwing}
 *  through method {@link UIForAnySwing#peek(Peeker)} with the purpose of
 *  expose the underlying component to the user while preserving a declarative method chaining
 *  based builder API usage pattern.
 * 	<p>
 * 	Consider the following example of a JProgressBar in a JPanel:
 * 	<pre>{@code
 *        UI.panel("fill")
 *        .add("span, grow, wrap",
 *             progressBar(UI.Align.HORIZONTAL, 0, 100)
 *             .withValue(68)
 *             .peek( it -> {
 *                 it.setString("%");
 *                 it.setStringPainted(true);
 *             })
 *             .withBackground(Color.WHITE)
 *        )
 * 	}</pre>
 * 	Here you can see that we can use the {@link Peeker} implementation to access the underlying
 * 	JProgressBar component and set its string and string painted properties
 * 	while still using the declarative method chaining based builder API.<br>
 * 	<br>
 * 	<b>Also consider taking a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of Swing-Tree in general.</b>
 *
 * @param <C> The component type which should be modified.
 */
public interface Peeker<C>
{
    /**
     *   Applies an action to the current component.
     *
     * @param component The component to be modified.
     */
    void accept(C component);
}

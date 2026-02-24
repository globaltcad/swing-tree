package swingtree.api.laf;

import sprouts.Observable;
import sprouts.Val;
import swingtree.SwingTreeConfigurator;
import swingtree.animation.LifeTime;
import swingtree.api.AnimatedStyler;
import swingtree.api.Styler;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.StyleSheet;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 *  This interface is intended to be implemented by the various {@link javax.swing.plaf.ComponentUI} extensions
 *  of a custom <i>Look and Feel</i> which desires to fully integrate with the SwingTree style engine.<br>
 *  For context, it is important to note that <i>SwingTree</i> ships with a rich style rendering engine
 *  and three main ways for configuring styles.<br>
 *  The most prominent once, which are typically used to build an application, are:
 *  <ul>
 *      <li>
 *          <b>Global Styling:</b>
 *          see {@link swingtree.style.StyleSheet},
 *              {@link swingtree.UI#use(StyleSheet, Supplier)},
 *              {@link swingtree.SwingTree#initializeUsing(SwingTreeConfigurator)}
 *      </li>
 *      <li>
 *          <b>Direct Styling in the GUI:</b><br>
 *          see {@link swingtree.UIForAnySwing#withStyle(Styler)}<br>
 *          also {@link swingtree.UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)}<br>
 *          and {@link swingtree.UIForAnySwing#withTransitoryStyle(Observable, LifeTime, AnimatedStyler)}
 *      </li>
 *  </ul>
 *  <b>
 *      The third major way of styling is through the {@link javax.swing.plaf.ComponentUI}
 *      of a custom <i>Look and Feel</i> implementing this marker interface {@link SwingTreeStyledComponentUI}.
 *  </b><br>
 *  <p>
 *      So if you develop your own <i>Look and Feel</i>, then you can make it compatible with <i>SwingTree</i> by having your
 *      {@link javax.swing.plaf.ComponentUI} extensions implement this {@link SwingTreeStyledComponentUI} interface.
 *      <i>SwingTree</i> will cooperate with the {@code ComponentUI} in two major ways:
 *  </p>
 *  <ol>
 *      <li>
 *          Gathering style information to the <i>SwingTree</i> style engine
 *          by invoking the {@link #style(ComponentStyleDelegate)} implementation.
 *      </li>
 *      <li>
 *          Checking if {@link #canForwardPaintingToSwingTree()} returns {@code true} and then expecting
 *          the {@link javax.swing.plaf.ComponentUI#paint(Graphics, JComponent)} to delegate to <i>SwingTree</i>s
 *          {@link swingtree.style.ComponentExtension#paintBackground(Graphics, swingtree.api.Painter)} method.
 *      </li>
 *  </ol>
 *  When SwingTree resolves a style, it will detect that the {@link javax.swing.plaf.ComponentUI}
 *  of a particular {@link JComponent} implements this interface, and then invoke the
 *  {@link #style(ComponentStyleDelegate)} method to gather style information.<br>
 *  That way a custom look and feel can delegate the complex rendering directly to the SwingTree style engine.
 *  You as <i>Look and Feel</i> developer can thereby focus on writing code which conveys the intent of how a
 *  particular type of component ought to look like much more clearly.<br>
 *  <p>
 *      Note that the <b>order</b> in which a <b>renderable style</b> is computed in
 *      SwingTree follows the following order:
 *      <ol>
 *          <li><b>Global Defaults</b> - (see {@link StyleSheet})</li>
 *          <li><b>Component Look and Feel</b> - (this {@link SwingTreeStyledComponentUI})</li>
 *          <li><b>Component Declaration</b> - (see {@link swingtree.UIForAnySwing#withStyle(Styler)})</li>
 *      </ol>
 *      In other words:<br>
 *      A {@link ComponentStyleDelegate} will be sent through
 *      a {@link StyleSheet}, then it will go through this {@link SwingTreeStyledComponentUI}
 *      and finally {@link swingtree.UIForAnySwing#withStyle(Styler)} on a concrete component.
 *  </p><br>
 *  <p>
 *      But for full interoperability you may also want to have {@link #canForwardPaintingToSwingTree()}
 *      always return {@code true}, and then implement {@link javax.swing.plaf.ComponentUI#paint(Graphics, JComponent)}
 *      to delegate to <i>SwingTree</i> like so:
 *      <pre>{@code
 *        @Override
 *        public void paint(
 *            Graphics g,
 *            JComponent comp
 *        ) {
 *            ComponentExtension.from(comp)
 *                .paintBackground(g, g2d->{
 *                    super.paint(g2d, comp);
 *                });
 *        }
 *        @Override
 *        public boolean canForwardPaintingToSwingTree() {
 *            return true;
 *        }
 *      }</pre>
 *      That way, the <i>SwingTree</i>s style engine works reliably for all components!
 *  </p>
 *
 * @param <C> The type of {@link JComponent} for which a particular {@link javax.swing.plaf.ComponentUI} is designed.
 */
public interface SwingTreeStyledComponentUI<C extends JComponent>
{
    /**
     * This method must be implemented through {@link javax.swing.plaf.ComponentUI#installUI(JComponent)}!
     * It configures the specified component appropriately for a <i>SwingTree</i> compatible look and feel.
     * This method is invoked when the <code>ComponentUI</code> instance is being installed
     * as the UI delegate on the specified component.  This method should
     * completely configure the component for the look and feel,
     * including the following:
     * <ol>
     *     <li>Install default property values for color, fonts, borders,
     *         icons, opacity, etc. on the component.  Whenever possible,
     *         property values initialized by the client program should <i>not</i>
     *         be overridden.
     *     <li>Install a <code>LayoutManager</code> on the component if necessary.
     *     <li>Create/add any required subcomponents to the component.
     *     <li>Create/install event listeners on the component.
     *     <li>Create/install a <code>PropertyChangeListener</code> on the component in order
     *         to detect and respond to component property changes appropriately.
     *     <li>Install keyboard UI (mnemonics, traversal, etc.) on the component.
     *     <li>Initialize any appropriate instance data.
     * </ol><br>
     * <p>
     *     <b>IMPORTANT:</b><br>
     *     For full <i>SwingTree</i> interoperability, implementations of this
     *     should invoke {@link swingtree.style.ComponentExtension#gatherApplyAndInstallStyle(boolean)}
     *     to ensure that the <i>SwingTree</i> style of a particular component is installed correctly.<br>
     *     So an implementation may look something like this:
     * </p>
     *  <pre>{@code
     *    // Override
     *    public void installUI(
     *        JComponent comp
     *    ) {
     *        ComponentExtension.from(comp)
     *            .gatherApplyAndInstallStyle(true);
     *    }
     *  }</pre>
     *
     * @param c the component where this UI delegate is being installed
     *
     * @see javax.swing.plaf.ComponentUI#uninstallUI
     * @see javax.swing.JComponent#updateUI
     */
    void installUI(JComponent c);

    /**
     * This method must be implemented through {@link javax.swing.plaf.ComponentUI#paint(Graphics, JComponent)}!
     * It paints the specified component appropriately for the look and feel.
     * This method is invoked from the {@link javax.swing.plaf.ComponentUI#update(Graphics, JComponent)}
     * method when the specified component is being painted. Subclasses should override
     * this method and use the specified <code>Graphics</code> object to
     * render the content of the component.<br>
     * <b>
     *     For full <i>SwingTree</i> interoperability, you
     *     should override {@link #canForwardPaintingToSwingTree()} to return {@code true}
     *     and then forward the paint request to <i>SwingTree</i> like so:
     * </b>
     *  <pre>{@code
     *    // Override
     *    public void paint(
     *        Graphics g,
     *        JComponent comp
     *    ) {
     *        ComponentExtension.from(comp)
     *            .paintBackground(g, g2d->{
     *                super.paint(g2d, comp);
     *            });
     *    }
     *  }</pre>
     *
     * @param g the <code>Graphics</code> context in which to paint
     * @param c the component being painted;
     *          this argument is often ignored,
     *          but might be used if the UI object is stateless
     *          and shared by multiple components
     *
     * @see #canForwardPaintingToSwingTree()
     */
    void paint(Graphics g, JComponent c);


    /**
     *  Receives a {@link ComponentStyleDelegate} and applies style information to it by
     *  transforming it to a new {@link ComponentStyleDelegate}. <br>
     *  <b>
     *      This styling will happen after/on-top of the style supplied by a {@link StyleSheet}
     *      but it will happen before the style directly declared for a specific component instance
     *      using the {@link swingtree.UIForAnySwing#withStyle(Styler)} method.
     *  </b>
     *  <br>
     *  Note that this method is designed to only be invoked by <i>SwingTree</i> internal code.
     *  You should never invoke it yourself.
     *
     * @param delegate The {@link ComponentStyleDelegate} to apply the style to.
     * @return A new {@link ComponentStyleDelegate} that has the style applied.
     * @throws Exception if the style could not be applied by the client code.
     */
    ComponentStyleDelegate<C> style( ComponentStyleDelegate<C> delegate ) throws Exception;

    /**
     *  If you want to achieve full compatibility and interoperability with SwingTree in your <i>Look and Feel</i>
     *  you have to forward {@link javax.swing.plaf.ComponentUI#paint(Graphics, JComponent)} and
     *  {@link javax.swing.plaf.ComponentUI#update(Graphics, JComponent)} draw calls to SwingTrees's
     *  {@link swingtree.style.ComponentExtension#paintBackground(Graphics, swingtree.api.Painter)} method!
     *  To inform SwingTree that you are going to do this, you also have to override <b>this method</b> and
     *  make it return {@code true}. This will turn your {@link javax.swing.plaf.ComponentUI} implementation
     *  into the main way in which <i>SwingTree</i> renders its style onto a specific component type.<br>
     *  So your implementation would look something like this:
     *  <pre>{@code
     *    // Override
     *    public void paint(
     *        Graphics g,
     *        JComponent comp
     *    ) {
     *        ComponentExtension.from(comp)
     *            .paintBackground(g, g2d->{
     *                super.paint(g2d, comp);
     *            });
     *    }
     *    // Override
     *    public boolean canForwardPaintingToSwingTree() {
     *        return true;
     *    }
     *  }</pre>
     *  <p>
     *      <b>WARNING:</b> <br>
     *      If this method returns {@code true}, but the {@link javax.swing.plaf.ComponentUI#paint(Graphics, JComponent)}
     *      implementation does NOT delegate to {@link swingtree.style.ComponentExtension#paintBackground(Graphics, swingtree.api.Painter)},
     *      <b>then you will effectively break the style rendering of your components!</b><br>
     *      This is because in that case, <i>SwingTree</i> relies entirely on your override
     *      to be the sole way of hooking into <i>Swing</i>s component rendering.
     *  </p>
     * @return A flag which informs <i>SwingTree</i> if it can rely on your {@link javax.swing.plaf.ComponentUI#paint(Graphics, JComponent)}
     *         implementation to be used for hooking its style engine into the rendering pipeline of a component.
     */
    default boolean canForwardPaintingToSwingTree() {
        return false;
    }

}

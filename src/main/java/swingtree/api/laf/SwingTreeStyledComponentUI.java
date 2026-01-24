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
import java.util.function.Supplier;

/**
 *  This interface is intended to be implemented by the various {@link javax.swing.plaf.ComponentUI}
 *  extensions of an external Look and Feel which desires to utilize the SwingTree style API.<br>
 *  SwingTree ships with a rich style rendering engine and three main ways for configuring styles.<br>
 *  The most prominent once, which are typically used to build an application, are:
 *  <ul>
 *      <li>
 *          <b>Global Styling:</b>
 *          see {@link swingtree.style.StyleSheet},
 *              {@link swingtree.UI#use(StyleSheet, Supplier)},
 *              {@link swingtree.SwingTree#initialiseUsing(SwingTreeConfigurator)}
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
 *      of a custom look and feel implementing <b>this marker interface {@link SwingTreeStyledComponentUI}</b>.
 *  </b><br>
 *  When SwingTree resolves a style, it will detect that the {@link javax.swing.plaf.ComponentUI}
 *  of a particular {@link JComponent} implements this interface, and then invoke the
 *  {@link #style(ComponentStyleDelegate)} method.<br>
 *  That way a custom look and feel can delegate the complex rendering directly to the SwingTree style engine.
 *  You as <i>look and feel</i> developer can then also focus on writing code which conveys the intent of how a
 *  particular type of component ought to look like much more clearly.<br>
 *  <p>
 *      Note that the <b>order</b> in which a <b>renderable style</b> is computed in
 *      SwingTree follows the following order:
 *      <li>
 *          <ul><b>1. Global Defaults</b> - (see {@link StyleSheet})</ul>
 *          <ul><b>2. Component Look and Feel</b> - (this {@link SwingTreeStyledComponentUI})</ul>
 *          <ul><b>3. Component Declaration</b> - (see {@link swingtree.UIForAnySwing#withStyle(Styler)})</ul>
 *      </li>
 *      In other words:<br>
 *      A {@link ComponentStyleDelegate} will be sent through
 *      a {@link StyleSheet}, then it will go through this {@link SwingTreeStyledComponentUI}
 *      and finally {@link swingtree.UIForAnySwing#withStyle(Styler)} on a concrete component.
 *  </p>
 *
 * @param <C> The type of {@link JComponent} for which a particular {@link javax.swing.plaf.ComponentUI} is designed.
 */
public interface SwingTreeStyledComponentUI<C extends JComponent> {

    /**
     *  Receives a {@link ComponentStyleDelegate} and applies style information to it by
     *  transforming it to a new {@link ComponentStyleDelegate}. <br>
     *  <b>
     *      This styling will happen after/on-top of the style of a {@link StyleSheet}
     *      and it will happen before the style directly declared for a specific component instance.
     *  </b>
     *  <br>
     *  Note that this method deliberately requires the handling of checked exceptions.
     *  This is because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param delegate The {@link ComponentStyleDelegate} to apply the style to.
     * @return A new {@link ComponentStyleDelegate} that has the style applied.
     * @throws Exception if the style could not be applied by the client code.
     */
    ComponentStyleDelegate<C> style( ComponentStyleDelegate<C> delegate ) throws Exception;

}

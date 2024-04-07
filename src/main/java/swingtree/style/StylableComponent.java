package swingtree.style;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 *  Implementations of this interface are SwingTree native components
 *  which enjoy the full support of the style API.
 *  Regular Swing components can be styled on most layers
 *  but not all. The {@link swingtree.UI.Layer#BACKGROUND} and
 *  {@link swingtree.UI.Layer#FOREGROUND} layers are not supported
 *  for some components for which SwingTree tries to install a
 *  custom UI delegate. <br>
 *  This however is prone to side effects and can cause issues
 *  with third party look and feels. <br>
 *  For full support of the style API for your custom components
 *  you should implement this interface.
 */
public interface StylableComponent
{
    /**
     *  Certain style configurations require SwingTree to install a
     *  custom UI delegate. This method is used to set the UI delegate
     *  for the component but without triggering side effects like
     *  the former UI being uninstalled (which itself can cause
     *  a lot of undesired side effects).
     *  <p>
     *  <b>This method is not intended to be called by client code!
     *  It exists for internal use only and unfortunately cannot be
     *  protected or private due to the nature of the Swing API.</b>
     *  <p>
     *  The implementation of this method is expected to look like this:
     *  <pre>
     *  {@literal @}Override
     *  public void setUISilently(ComponentUI ui){
     *      this.ui = ui; // no side effects
     *  }
     *  </pre>
     * @param ui the UI delegate to set for the component
     *           without triggering side effects.
     */
    void setUISilently( ComponentUI ui );

    /**
     *  This method is expected to be implemented as follows
     *  within a component extension which ought to be made compatible
     *  with SwingTree.
     *  <pre>
     *      {@literal @}Override
     *      public void paint(Graphics g){
     *          paintBackground(g, ()-&gt;super.paint(g));
     *      }
     *  </pre>
     * @param g the graphics context to paint on,
     *          obtained from the component's {@link JComponent#paint(Graphics)} method.
     */
    void paint( Graphics g );

    /**
     *  This method is expected to be implemented as follows:
     *  <pre>
     *      {@literal @}Override
     *      public void paintChildren(Graphics g){
     *          paintForeground(g, ()-&gt;super.paintChildren(g));
     *      }
     *  </pre>
     * @param g the graphics context to paint on,
     *          obtained from the component's {@code JComponent::paintChildren(Graphics)} method.
     */
    void paintChildren( Graphics g );

    /**
     *  <b>
     *      This default method is not intended to be overridden by client code!
     *      It delegates the painting to the library internal {@link ComponentExtension}.
     *  </b>
     *
     * @param g The graphics context to paint on.
     * @param superPaint The super.paint() method to call.
     */
    /*final*/ default void paintBackground( Graphics g, Consumer<Graphics> superPaint ) {
        if ( this instanceof JComponent ) {
            ComponentExtension.from((JComponent) this).paintBackgroundIfNeeded( g, superPaint );
        }
        else
            throw new UnsupportedOperationException( "This interface is only intended for JComponent implementations" );
    }

    /**
     *  <b>
     *      This default method is not intended to be overridden by client code!
     *      It delegates the painting to the library internal {@link ComponentExtension}.
     *  </b>
     *
     * @param g The graphics context to paint on.
     * @param superPaint The super.paintChildren() method to call.
     */
    /*final*/ default void paintForeground( Graphics g, Consumer<Graphics> superPaint ) {
        if ( this instanceof JComponent ) {
            ComponentExtension.from((JComponent) this).paintForeground( (Graphics2D) g, superPaint );
        }
        else
            throw new UnsupportedOperationException( "This interface is only intended for JComponent implementations" );
    }

}

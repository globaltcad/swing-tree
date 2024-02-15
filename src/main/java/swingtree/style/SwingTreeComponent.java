package swingtree.style;

import javax.swing.plaf.ComponentUI;

/**
 *  A marker interface for SwingTree components that enjoy full
 *  support for the style API.
 */
public interface SwingTreeComponent
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
     * @param ui the UI delegate to set for the component
     *           without triggering side effects.
     */
    void setUISilently( ComponentUI ui );
}

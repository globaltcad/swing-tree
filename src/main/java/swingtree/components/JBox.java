package swingtree.components;

import net.miginfocom.swing.MigLayout;
import swingtree.style.ComponentExtension;
import swingtree.style.StylableComponent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PanelUI;
import java.awt.Graphics;
import java.awt.LayoutManager;

/**
 * <code>JBox</code> is a generic lightweight container similar to
 * <code>javax.swing.JPanel</code>, but with 2 important differences:
 * <ul>
 *     <li>
 *         The <code>JBox</code> is transparent by default, meaning that it does
 *         not paint its background if it is not explicitly set through the style API.
 *     </li>
 *     <li> It does not have any insets by default. </li>
 * </ul>
 *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
 *  The term <i>box</i> is referring to the purpose of this component, which
 *  is to tightly store and wrap other sub-components seamlessly...</b>
 *  <p>
 *
 * @author Daniel Nepp
 */
public class JBox extends JComponent implements Accessible, StylableComponent
{
    /**
     * @see #getUIClassID
     */
    private static final String uiClassID = "PanelUI";

    /**
     * Creates a new JBox with the specified layout manager and buffering
     * strategy.
     *
     * @param layout  the LayoutManager to use
     * @param isDoubleBuffered  a boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free
     *        updates
     */
    public JBox( LayoutManager layout, boolean isDoubleBuffered ) {
        setLayout(layout);
        setDoubleBuffered(isDoubleBuffered);
        setOpaque(false);
        updateUI();
    }

    /**
     * Create a new buffered JBox with the specified layout manager
     *
     * @param layout  the LayoutManager to use
     */
    public JBox(LayoutManager layout) {
        this(layout, true);
    }

    /**
     * Creates a new <code>JBox</code> with the specified buffering strategy
     * qnd a default <code>MigLayout</code> instance
     * configured to be without insets and gaps between components.
     * If <code>isDoubleBuffered</code> is true, the <code>JBox</code>
     * will use a double buffer.
     *
     * @param isDoubleBuffered  a boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free
     *        updates
     */
    public JBox(boolean isDoubleBuffered) {
        this(new MigLayout("ins 0, hidemode 2, gap 0"), isDoubleBuffered);
    }

    /**
     * Creates a new <code>JBox</code> with a double buffer
     * and a flow layout.
     */
    public JBox() {
        this(true);
    }

    /** {@inheritDoc} */
    @Override public void paint(Graphics g){
        paintBackground(g, super::paint);
    }

    /** {@inheritDoc} */
    @Override public void paintChildren(Graphics g) {
        paintForeground(g, super::paintChildren);
    }

    @Override public void setUISilently( ComponentUI ui ) {
        this.ui = ui;
    }

    /**
     * Resets the UI property with a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    @Override
    public void updateUI() {
        ComponentExtension.from(this).installCustomUIIfPossible();
        /*
            The JBox is a SwingTree native type, so it also
            enjoys the perks of having a SwingTree look and feel!
        */
    }

    /**
     * Returns the look and feel (L&amp;amp;F) object that renders this component.
     *
     * @return the PanelUI object that renders this component
     */
    /*@Override*/
    @SuppressWarnings("MissingOverride")
    public PanelUI getUI() { return (PanelUI) ui; }


    /**
     * Sets the look and feel (L&amp;F) object that renders this component.
     *
     * @param ui  the PanelUI L&amp;F object
     * @see UIDefaults#getUI
     */
    public void setUI( PanelUI ui ) {
        super.setUI(ui);
    }

    /**
     * Returns a string that specifies the name of the L&amp;F class
     * that renders this component.
     *
     * @return "PanelUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @Override public String getUIClassID() { return uiClassID; }

    /**
     * Returns a string representation of this JBox. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JBox.
     */
    @Override
    protected String paramString() { return super.paramString(); }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JBox.
     * For the JBox, the AccessibleContext takes the form of an
     * AccessibleJBox.
     * A new AccessibleJBox instance is created if necessary.
     *
     * @return an AccessibleJBox that serves as the
     *         AccessibleContext of this JBox
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if ( accessibleContext == null )
            accessibleContext = new AccessibleJBox();

        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JBox</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to panel user-interface
     * elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    protected class AccessibleJBox extends JComponent.AccessibleJComponent {

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        @Override
        public AccessibleRole getAccessibleRole() { return AccessibleRole.PANEL; }
    }
}


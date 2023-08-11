package swingtree.style;

import swingtree.components.JBox;
import swingtree.components.JIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;
import java.util.Optional;
import java.util.function.Supplier;

class DynamicLaF
{
    static DynamicLaF none() { return new DynamicLaF(null, null); }


    private ComponentUI _styleLaF;
    private ComponentUI _formerLaF;


    private DynamicLaF(ComponentUI styleLaF, ComponentUI formerLaF) {
        _styleLaF = styleLaF;
        _formerLaF = formerLaF;
    }



    boolean customLookAndFeelIsInstalled() { return _styleLaF != null; }

    DynamicLaF establishLookAndFeelFor( Style style, JComponent owner ) {

        // For panels mostly:
        boolean weNeedToOverrideLaF = false;
        boolean hasBorderRadius = style.border().hasAnyNonZeroArcs();
        boolean hasMargin = style.margin().isPositive();
        boolean hasBackgroundPainter = style.hasCustomBackgroundPainters();
        boolean hasBackgroundShades  = style.hasCustomGradients();

        if ( hasBorderRadius )
            weNeedToOverrideLaF = true;

        if ( hasMargin )
            weNeedToOverrideLaF = true;

        if ( hasBackgroundPainter )
            weNeedToOverrideLaF = true;

        if ( hasBackgroundShades )
            weNeedToOverrideLaF = true;

        if ( style.hasCustomBackgroundPainters() )
            weNeedToOverrideLaF = true;

        if ( style.anyVisibleShadows() )
            weNeedToOverrideLaF = true;

        if ( weNeedToOverrideLaF ) {
            boolean foundationIsTransparent = style.base()
                    .foundationColor()
                    .map( c -> c.getAlpha() < 255 )
                    .orElse(
                            Optional
                                    .ofNullable(owner.getBackground())
                                    .map( c -> c.getAlpha() < 255 )
                                    .orElse(true)
                    );

            if ( owner.isOpaque() )
                owner.setOpaque( !hasBorderRadius && !hasMargin && !foundationIsTransparent );
            /* ^
                If our style reveals what is behind it, then we need
                to make the component non-opaque so that the previous rendering get's flushed out!
             */
            boolean success = _installCustomLaF(owner);
            if ( !success && owner.isOpaque() ) {
                owner.setOpaque(false);
            }

            if ( owner instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) owner;
                b.setContentAreaFilled(!hasBackgroundShades && !hasBackgroundPainter);
            }
        }
        else if ( customLookAndFeelIsInstalled() )
            _uninstallCustomLaF(owner);

        return this;
    }


    private boolean _installCustomLaF( JComponent owner ) {
        // First we check if we already have a custom LaF installed:
        if ( customLookAndFeelIsInstalled() )
            return true;

        if ( owner instanceof JBox ) { // This is a SwinTree component, so it already has a custom LaF.
            JBox p = (JBox) owner;
            _formerLaF = p.getUI();
            //PanelUI laf = createJBoxUI();
            //p.setUI(laf);
            _styleLaF = _formerLaF;
            return true;
        }
        else if ( owner instanceof JIcon ) { // This is a SwinTree component, so it already has a custom LaF.
            JIcon i = (JIcon) owner;
            _formerLaF = i.getUI();
            //LabelUI laf = createJIconUI();
            //i.setUI(laf);
            _styleLaF = _formerLaF;
            return true;
        }
        else if ( owner instanceof JPanel ) {
            JPanel p = (JPanel) owner;
            _formerLaF = p.getUI();
            PanelStyler laf = PanelStyler.INSTANCE;
            p.setUI(laf);
            _styleLaF = laf;
            if ( _formerLaF instanceof BasicPanelUI ) {
                BasicPanelUI panelUI = (BasicPanelUI) _formerLaF;
                panelUI.installUI(p);
                // We make the former LaF believe that it is still in charge of the component.
            }
            return true;
        }
        else if ( owner instanceof AbstractButton ) {
            AbstractButton b = (AbstractButton) owner;
            _formerLaF = b.getUI();
            ButtonStyler laf = new ButtonStyler(b.getUI());
            b.setUI(laf);
            if ( _formerLaF instanceof BasicButtonUI) {
                BasicButtonUI buttonUI = (BasicButtonUI) _formerLaF;
                buttonUI.installUI(b);
                // We make the former LaF believe that it is still in charge of the component.
            }
            _styleLaF = laf;
            return true;
        }
        else if ( owner instanceof JLabel ) {
            JLabel l = (JLabel) owner;
            _formerLaF = l.getUI();
            LabelStyler laf = new LabelStyler(l.getUI());
            l.setUI(laf);
            if ( _formerLaF instanceof BasicLabelUI) {
                BasicLabelUI labelUI = (BasicLabelUI) _formerLaF;
                labelUI.installUI(l);
                // We make the former LaF believe that it is still in charge of the component.
            }
            _styleLaF = laf;
            return true;
        }
        else if ( owner instanceof JTextField && !(owner instanceof JPasswordField) ) {
            JTextField t = (JTextField) owner;
            _formerLaF = t.getUI();
            TextFieldStyler laf = new TextFieldStyler(t.getUI());
            t.setUI(laf);
            if ( _formerLaF instanceof TextUI) {
                TextUI textFieldUI = (TextUI) _formerLaF;
                textFieldUI.installUI(t);
                // We make the former LaF believe that it is still in charge of the component.
            }
            _styleLaF = laf;
            return true;
        }

        return false;
    }

    void _uninstallCustomLaF( JComponent _owner ) {
        if ( customLookAndFeelIsInstalled() ) {
            if ( _owner instanceof JPanel ) {
                JPanel p = (JPanel) _owner;
                p.setUI((PanelUI) _formerLaF);
                _styleLaF = null;
            }
            if ( _owner instanceof JBox ) {
                //JBox p = (JBox) _owner;
                //p.setUI((PanelUI) _formerLaF);
                _styleLaF = null;
            }
            if ( _owner instanceof JIcon ) {
                //JBox p = (JBox) _owner;
                //p.setUI((PanelUI) _formerLaF);
                _styleLaF = null;
            }
            else if ( _owner instanceof AbstractButton ) {
                AbstractButton b = (AbstractButton) _owner;
                b.setUI((ButtonUI) _formerLaF);
                _styleLaF = null;
            }
            else if ( _owner instanceof JLabel ) {
                JLabel l = (JLabel) _owner;
                l.setUI((LabelUI) _formerLaF);
                _styleLaF = null;
            }
            else if ( _owner instanceof JTextField && !(_owner instanceof JPasswordField) ) {
                JTextField t = (JTextField) _owner;
                t.setUI((TextUI) _formerLaF);
                _styleLaF = null;
            }
        }
    }

    public void updateUIFor(JComponent owner )
    {
        if ( owner instanceof JBox )
            ((JBox)owner).setUI(new DynamicLaF.PanelStyler() {
                @Override public void installUI(JComponent c) { installDefaults((JBox)c); }
                @Override public void uninstallUI(JComponent c) { uninstallDefaults((JBox)c); }
                private void installDefaults(JBox b) {
                    LookAndFeel.installColorsAndFont(b, "Box.background", "Box.foreground", "Box.font");
                    LookAndFeel.installBorder(b,"Box.border");
                    LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
                }
                private void uninstallDefaults(JBox b) { LookAndFeel.uninstallBorder(b); }
            });
        else if ( owner instanceof JIcon )
            ((JIcon)owner).setUI(new DynamicLaF.LabelStyler(null));

        // Other types of components are not supported yet!
    }

    static class PanelStyler extends BasicPanelUI
    {
        static final PanelStyler INSTANCE = new PanelStyler();

        PanelStyler() {}

        @Override public void paint(Graphics g, JComponent c ) { ComponentExtension.from(c)._paintBackground(g); }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class ButtonStyler extends BasicButtonUI
    {
        private final ButtonUI _formerUI;

        ButtonStyler(ButtonUI formerUI) { _formerUI = formerUI; }

        @Override public void paint( Graphics g, JComponent c ) {
            ComponentExtension.from(c)._paintBackground(g);
            _paintComponentThroughFormerIU(_formerUI, g, c);
        }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class LabelStyler extends BasicLabelUI
    {
        private final LabelUI _formerUI;

        LabelStyler(LabelUI formerUI) { _formerUI = formerUI; }

        @Override public void paint( Graphics g, JComponent c ) {
            ComponentExtension.from(c)._paintBackground(g);
            if ( _formerUI != null )
                _paintComponentThroughFormerIU(_formerUI, g, c);
            else
                super.paint(g, c);
        }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class TextFieldStyler extends BasicTextFieldUI
    {
        private final TextUI _formerUI;

        TextFieldStyler(TextUI formerUI) { _formerUI = formerUI; }
        @Override protected void paintSafely(Graphics g) {
            if ( !getComponent().isOpaque() )
                paintBackground(g);
            super.paintSafely(g);
        }
        @Override protected void paintBackground(Graphics g) {
            JComponent c = getComponent();
            //if ( c.isOpaque() ) {
                int insetTop    = 0;
                int insetLeft   = 0;
                int insetBottom = 0;
                int insetRight  = 0;
                if ( c.getBorder() instanceof StyleAndAnimationBorder ) {
                    StyleAndAnimationBorder<?> styleBorder = (StyleAndAnimationBorder<?>) c.getBorder();
                    Insets margins = styleBorder.getCurrentMarginInsets();
                    Insets oldLaFBorder = styleBorder.getFormerBorderInsets();
                    insetTop    = margins.top    + oldLaFBorder.top    / 2;
                    insetLeft   = margins.left   + oldLaFBorder.left   / 2;
                    insetBottom = margins.bottom + oldLaFBorder.bottom / 2;
                    insetRight  = margins.right  + oldLaFBorder.right  / 2; /*
                        Here we divide by 2 because in nimbus the border is partially consisting of
                        a shadow going inwards! If we don't divide by 2, the background will
                        not fill the whole inner component area.
                        TODO: investigate how this works in other LaFs.
                    */
                }
                g.setColor(c.getBackground());
                g.fillRect(
                        insetLeft, insetTop,
                        c.getWidth() - insetLeft - insetRight, c.getHeight() - insetTop - insetBottom
                    );
            //}
            ComponentExtension.from(c)._paintBackground(g);
            if ( insetLeft == 0 && insetRight == 0 && insetTop == 0 && insetBottom == 0 )
                _paintComponentThroughFormerIU(_formerUI, g, c);
        }

        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }


    private static void _paintComponentThroughFormerIU(ComponentUI formerUI, Graphics g, JComponent c) {
        try {
            if ( formerUI != null )
                formerUI.update(g, c);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    /**
     *  Determines whether the given point, in the parent's coordinate space, is within this component.
     *  This method accounts for the current SwingTree border and style insets (padding, border widths and margin)
     *  as well as subcomponents outside the inner component area.
     * @param c the component
     * @param x the x coordinate
     * @param y the y coordinate
     * @param superContains the super.contains() method
     * @return true if the point is within the component
     */
    private static boolean _contains(JComponent c, int x, int y, Supplier<Boolean> superContains)
    {
        Border border = c.getBorder();
        if ( border instanceof StyleAndAnimationBorder ) {
            StyleAndAnimationBorder<?> b = (StyleAndAnimationBorder<?>) border;
            Insets margins = b.getCurrentMarginInsets();
            int width  = c.getWidth();
            int height = c.getHeight();
            boolean isInside = (x >= margins.left) && (x < width - margins.right) && (y >= margins.top) && (y < height - margins.bottom);

            if ( isInside )
                return true;
            else
            {/*
                You might be thinking that we should return false here, but that would be wrong in certain cases!
                A child component might be outside the border, but still be a subcomponent of the parent component.
                This is the case for example, when the padding is negative, and the child component is inside the border.
                So, if there are negative paddings, we loop through the subcomponents and see if any of
                them contains the point.                                                                               */

                Insets padding = b.getCurrentPaddingInsets();

                if ( padding.top < 0 || padding.left < 0 || padding.bottom < 0 || padding.right < 0 )
                    for ( Component child : c.getComponents() ) {
                        if ( child instanceof JComponent ) {
                            JComponent jc = (JComponent) child;
                            if ( jc.contains(x, y) )
                                return true;
                        }
                    }
                else
                    return false;
            }
        }
        return superContains.get();
    }

}

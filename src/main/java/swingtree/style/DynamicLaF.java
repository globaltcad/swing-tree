package swingtree.style;

import org.slf4j.Logger;
import swingtree.UI;
import swingtree.components.JBox;
import swingtree.components.JIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.Optional;
import java.util.function.Supplier;

/**
 *   This class is responsible for installing and uninstalling custom look and feel
 *   implementations so that SwingTree can apply custom styles to components.
 */
final class DynamicLaF
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DynamicLaF.class);

    private static final DynamicLaF _NONE = new DynamicLaF(null, null, false);

    static DynamicLaF none() { return _NONE; }


    private final ComponentUI _styleLaF;  // Nullable
    private final ComponentUI _formerLaF; // Nullable
    private final boolean     _overrideWasNeeded;


    private DynamicLaF( ComponentUI styleLaF, ComponentUI formerLaF, boolean overrideWasNeeded ) {
        _styleLaF          = styleLaF;
        _formerLaF         = formerLaF;
        _overrideWasNeeded = overrideWasNeeded;
    }

    boolean overrideWasNeeded() {
        return _overrideWasNeeded;
    }

    boolean customLookAndFeelIsInstalled() {
        return _styleLaF != null;
    }

    DynamicLaF establishLookAndFeelFor(StyleConf styleConf, JComponent owner ) {

        DynamicLaF result = this;

        // For panels mostly:
        boolean weNeedToOverrideLaF = false;

        if ( styleConf.border().hasAnyNonZeroArcs() ) // Border radius
            weNeedToOverrideLaF = true;

        if ( styleConf.margin().isPositive() )
            weNeedToOverrideLaF = true;

        if ( styleConf.hasVisibleGradientsOnLayer(UI.Layer.BACKGROUND) )
            weNeedToOverrideLaF = true;

        if ( styleConf.hasVisibleNoisesOnLayer(UI.Layer.BACKGROUND) )
            weNeedToOverrideLaF = true;

        if ( styleConf.hasPaintersOnLayer(UI.Layer.BACKGROUND) )
            weNeedToOverrideLaF = true;

        if ( styleConf.hasVisibleShadows(UI.Layer.BACKGROUND) )
            weNeedToOverrideLaF = true;

        if ( weNeedToOverrideLaF ) {
            if (owner instanceof JScrollPane) {
                boolean foundationIsTransparent = styleConf
                                                  .base()
                                                  .foundationColor()
                                                  .map( c -> c.getAlpha() < 255 )
                                                  .orElse(
                                                      Optional.ofNullable(owner.getBackground())
                                                          .map( c -> c.getAlpha() < 255 )
                                                          .orElse(true)
                                                  );

                boolean hasBorderRadius = styleConf.border().hasAnyNonZeroArcs();
                boolean hasMargin       = styleConf.margin().isPositive();

                owner.setOpaque(!hasBorderRadius && !hasMargin && !foundationIsTransparent);
                JScrollPane scrollPane = (JScrollPane) owner;
                if ( scrollPane.getViewport() != null )
                    scrollPane.getViewport().setOpaque(owner.isOpaque());
            }
            /* ^
                If our style reveals what is behind it, then we need
                to make the component non-opaque so that the previous rendering get's flushed out!
             */
            try {
                result = _installCustomLaF(owner);
            } catch ( Exception e ) {
                log.error("Failed to install custom LaF for component '"+owner+"'!", e);
            }
        } else if ( customLookAndFeelIsInstalled() ) {
            try {
                result = _uninstallCustomLaF(owner);
            } catch ( Exception e ) {
                log.error("Failed to uninstall custom LaF for component '"+owner+"'!", e);
            }
        }

        if ( _overrideWasNeeded != weNeedToOverrideLaF )
            return new DynamicLaF(result._styleLaF, result._formerLaF, weNeedToOverrideLaF);
        else
            return result;
    }


    private DynamicLaF _installCustomLaF( JComponent owner ) {
        // First we check if we already have a custom LaF installed:
        ComponentUI formerLaF = _formerLaF;
        ComponentUI styleLaF  = _styleLaF;

        if ( !customLookAndFeelIsInstalled() ) {
            if (owner instanceof JBox) { // This is a SwingTree component, so it already has a custom LaF.
                JBox p = (JBox) owner;
                formerLaF = p.getUI();
                styleLaF = formerLaF;
            } else if (owner instanceof JIcon) { // This is a SwingTree component, so it already has a custom LaF.
                JIcon i = (JIcon) owner;
                formerLaF = i.getUI();
                styleLaF = formerLaF;
            } else if (owner instanceof JPanel) {
                JPanel p = (JPanel) owner;
                formerLaF = p.getUI();
                PanelStyler laf = PanelStyler.INSTANCE;
                boolean success = _tryInstallingUISilently(p, laf);
                if ( !success ) {
                    p.setUI(laf);
                    if (formerLaF != null) {
                        PanelUI panelUI = (PanelUI) formerLaF;
                        panelUI.installUI(p);
                        // We make the former LaF believe that it is still in charge of the component.
                    }
                }
                styleLaF = laf;
            } else if (owner instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) owner;
                formerLaF = b.getUI();
                ButtonStyler laf = new ButtonStyler(b.getUI());
                boolean success = _tryInstallingUISilently(b, laf);
                if ( !success ) {
                    b.setUI(laf);
                    if ( formerLaF != null ) {
                        ButtonUI buttonUI = (ButtonUI) formerLaF;
                        buttonUI.installUI(b);
                        // We make the former LaF believe that it is still in charge of the component.
                    }
                }
                styleLaF = laf;
            } else if (owner instanceof JLabel) {
                JLabel l = (JLabel) owner;
                formerLaF = l.getUI();
                LabelStyler laf = new LabelStyler(l.getUI());
                boolean success = _tryInstallingUISilently(l, laf);
                if ( !success ) {
                    l.setUI(laf);
                    if (formerLaF != null) {
                        LabelUI labelUI = (LabelUI) formerLaF;
                        labelUI.installUI(l);
                        // We make the former LaF believe that it is still in charge of the component.
                    }
                }
                styleLaF = laf;
            } else if (owner instanceof JTextField && !(owner instanceof JPasswordField)) {
                JTextField t = (JTextField) owner;
                formerLaF = t.getUI();
                TextFieldStyler laf = new TextFieldStyler(t.getUI());
                boolean success = _tryInstallingUISilently(t, laf);
                if ( !success ) {
                    t.setUI(laf);
                    if ( formerLaF != null ) {
                        TextUI textFieldUI = (TextUI) formerLaF;
                        textFieldUI.installUI(t);
                        // We make the former LaF believe that it is still in charge of the component.
                    }
                }
                styleLaF = laf;
            }
        }

        return new DynamicLaF(formerLaF, styleLaF, true);
    }

    DynamicLaF _uninstallCustomLaF( JComponent _owner )
    {
        ComponentUI styleLaF = _styleLaF;

        if ( customLookAndFeelIsInstalled() ) {
            if ( _owner instanceof JPanel ) {
                JPanel p = (JPanel) _owner;
                boolean success = _tryInstallingUISilently(p, _formerLaF);
                if ( !success )
                    p.setUI((PanelUI) _formerLaF);
                styleLaF = null;
            }
            if ( _owner instanceof JBox ) {
                // The JBox is a SwingTree native type, so it also enjoys the perks of having a SwingTree look and feel!
                styleLaF = null;
            }
            if ( _owner instanceof JIcon ) {
                // The JIcon is a SwingTree native type, so it also enjoys the perks of having a SwingTree look and feel!
                styleLaF = null;
            }
            else if ( _owner instanceof AbstractButton ) {
                AbstractButton b = (AbstractButton) _owner;
                boolean success = _tryInstallingUISilently(b, _formerLaF);
                if ( !success )
                    b.setUI((ButtonUI) _formerLaF);
                styleLaF = null;
            }
            else if ( _owner instanceof JLabel ) {
                JLabel l = (JLabel) _owner;
                boolean success = _tryInstallingUISilently(l, _formerLaF);
                if ( !success )
                    l.setUI((LabelUI) _formerLaF);
                styleLaF = null;
            }
            else if ( _owner instanceof JTextField && !(_owner instanceof JPasswordField) ) {
                JTextField t = (JTextField) _owner;
                boolean success = _tryInstallingUISilently(t, _formerLaF);
                if ( !success )
                    t.setUI((TextUI) _formerLaF);
                styleLaF = null;
            }
        }
        return new DynamicLaF(styleLaF, _formerLaF, false);
    }

    private static boolean _tryInstallingUISilently(
        final JComponent  owner,
        final ComponentUI laf
    ) {
        /*
            We wish installing the UI by simply calling setUI(..) was so easy,
            but it is not due to the fact that this method has a lot of unwanted side effects.
            The biggest side effect is that it triggers a call to 'uninstallUI' on the former UI,
            which in turn triggers more unwanted side effects.
            Believe it or not, the BasicTextUI for example call the removeAll() method on the component
            when it is uninstalled, which is a big problem when you have custom text fields with custom
            subcomponents.
        */
        try {
            if ( owner instanceof StylableComponent) {
                ((StylableComponent) owner).setUISilently(laf);
                laf.installUI(owner);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to install custom SwingTree UI for component '"+owner+"'!", e);
        }
        return false;
    }



    void installCustomUIFor(JComponent owner )
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

        @Override public void paint(Graphics g, JComponent c ) { ComponentExtension.from(c).paintBackground(g, null); }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class ButtonStyler extends BasicButtonUI
    {
        private final ButtonUI _formerUI;

        ButtonStyler(ButtonUI formerUI) {
            _formerUI = ( formerUI instanceof ButtonStyler ) ? ((ButtonStyler)formerUI)._formerUI : formerUI;
        }

        @Override public void paint( Graphics g, JComponent c ) {
            ComponentExtension.from(c).paintBackground(g, ()->{
                _paintComponentThroughFormerUI(_formerUI, g, c);
            });
        }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class LabelStyler extends BasicLabelUI
    {
        private final LabelUI _formerUI;

        LabelStyler(LabelUI formerUI) {
            _formerUI = (formerUI instanceof LabelStyler) ? ((LabelStyler)formerUI)._formerUI : formerUI;
        }

        @Override public void paint( Graphics g, JComponent c ) {
            ComponentExtension.from(c).paintBackground(g, ()->{
                if ( _formerUI != null )
                    _paintComponentThroughFormerUI(_formerUI, g, c);
                else
                    super.paint(g, c);
            });
        }
        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    static class TextFieldStyler extends BasicTextFieldUI
    {
        private final TextUI _formerUI;

        TextFieldStyler(TextUI formerUI) {
            _formerUI = ( formerUI instanceof TextFieldStyler ) ? ((TextFieldStyler)formerUI)._formerUI : formerUI;
        }
        @Override protected void paintSafely(Graphics g) {
            if ( !getComponent().isOpaque() )
                paintBackground(g);

            ComponentExtension.from(getComponent()).paintWithContentAreaClip(g, ()->{
                super.paintSafely(g);// Paints the text
            });
        }
        @Override protected void paintBackground(Graphics g) {
            JComponent c = getComponent();

            Insets margins = ComponentExtension.from(c).getMarginInsets();
            int insetTop    = margins.top   ;
            int insetLeft   = margins.left  ;
            int insetBottom = margins.bottom;
            int insetRight  = margins.right ;

            g.setColor(c.getBackground());
            ComponentExtension.from(getComponent()).paintWithContentAreaClip(g, ()->{
                g.fillRect(
                        insetLeft, insetTop,
                        c.getWidth() - insetLeft - insetRight, c.getHeight() - insetTop - insetBottom
                    );
            });

            boolean shouldPaintFormerUI = ( insetLeft == 0 && insetRight == 0 && insetTop == 0 && insetBottom == 0 );
            ComponentExtension.from(c).paintBackground(g, ()->{
                if ( shouldPaintFormerUI )
                    _paintComponentThroughFormerUI(_formerUI, g, c);
            });
        }

        @Override public void update( Graphics g, JComponent c ) { paint(g, c); }
        @Override
        public boolean contains(JComponent c, int x, int y) { return _contains(c, x, y, ()->super.contains(c, x, y)); }
    }

    private static void _paintComponentThroughFormerUI(
        ComponentUI formerUI, Graphics g, JComponent c
    ) {
        try {
            if ( formerUI != null ) {
                StyleConf styleConf = ComponentExtension.from(c).getStyle();
                boolean hasMargin       = styleConf.margin().isPositive();
                boolean hasBorderRadius = styleConf.border().hasAnyNonZeroArcs();
                if ( !hasMargin && !hasBorderRadius )
                    formerUI.update(g, c);
                else {
                    ComponentExtension.from(c).paintWithContentAreaClip(g, ()->{
                        formerUI.update(g, c);
                    });
                }
            }
        } catch ( Exception ex ) {
            log.error("Failed to paint component through former UI", ex);
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
            Insets margins = b.getMarginInsets();
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

                Insets padding = b.getPaddingInsets();

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

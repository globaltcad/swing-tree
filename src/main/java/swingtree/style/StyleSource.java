package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.animation.LifeSpan;
import swingtree.api.Styler;
import swingtree.api.laf.SwingTreeStyledComponentUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;
import java.util.*;

/**
 *  A style source is a container for a local styler, animation stylers and a style sheet
 *  which are all used to calculate the final {@link StyleConf} configuration of a component. <br>
 *  This object can be thought of as a function of lambdas that takes a {@link JComponent}
 *  and returns a {@link StyleConf} object. <br>
 *
 * @param <C> The type of the component that is being styled, animated or sized in a particular way...
 */
final class StyleSource<C extends JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleSource.class);

    static <C extends JComponent> StyleSource<C> create() {
        return new StyleSource<C>(
                        Styler.none(),
                        new Expirable[0],
                        SwingTree.get().getStyleSheet()
                    );
    }


    private final StyleSheet _styleSheet;
    private final Styler<C> _localStyler;
    private final Expirable<Styler<C>>[] _animationStylers;



    private StyleSource(
        Styler<C>              localStyler,
        Expirable<Styler<C>>[] animationStylers,
        StyleSheet             styleSheet
    ) {
        _localStyler      = Objects.requireNonNull(localStyler);
        _animationStylers = Objects.requireNonNull(animationStylers);
        _styleSheet       = Objects.requireNonNull(styleSheet);
    }

    StyleSheet styleSheet() {
        return _styleSheet;
    }

    public boolean hasNoAnimationStylers() {
        return _animationStylers.length == 0;
    }

    StyleSource<C> withLocalStyler( Styler<C> styler ) {
        Styler<C> compositeStyler = _localStyler.andThen(styler);
        return new StyleSource<>(compositeStyler, _animationStylers, _styleSheet);
    }

    StyleSource<C> withAnimationStyler( LifeSpan lifeSpan, Styler<C> animationStyler ) {
        List<Expirable<Styler<C>>> animationStylers = new ArrayList<>(Arrays.asList(_animationStylers));
        animationStylers.add(new Expirable<>(lifeSpan, animationStyler));
        return new StyleSource<>(_localStyler, animationStylers.toArray(new Expirable[0]), _styleSheet);
    }

    StyleSource<C> withoutAnimationStylers() {
        return new StyleSource<>(_localStyler, new Expirable[0], _styleSheet);
    }

    StyleConf gatherStyleFor( C owner )
    {
        // 0: Some things are inherited from the parent component:
        StyleConf styleConf = Optional.ofNullable(owner.getParent())
                              .map( p -> p instanceof JComponent ? (JComponent) p : null )
                              .map(ComponentExtension::from)
                              .map(ComponentExtension::getStyle)
                              .map(StyleConf::font)
                              .filter( f -> !f.equals(FontConf.none()) )
                              .map( f -> StyleConf.none()._withFont(f) )
                              .orElse(StyleConf.none());

        // 1. Global StyleSheet
        try {
            StyleConf updated = _styleSheet.computeStyleFrom( owner, styleConf );
            Objects.requireNonNull(updated);
            styleConf = updated;
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "An exception occurred while applying the style sheet for component '{}'.", owner, e);
            /*
                 If any exceptions happen in a StyleSheet implementation provided by a user,
                 then we don't want to prevent the other Stylers from doing their job,
                 which is why we catch any exceptions immediately!
            */
        }

        // 2. Look and Feel
        try {
            ComponentUI componentUI = _findComponentUIOf(owner);
            if ( componentUI instanceof SwingTreeStyledComponentUI) {
                SwingTreeStyledComponentUI<C> swingTreeUI = (SwingTreeStyledComponentUI) componentUI;
                ComponentStyleDelegate<C> updated = swingTreeUI.style(new ComponentStyleDelegate<>(owner, styleConf));
                Objects.requireNonNull(updated);
                styleConf = updated.style();
            }
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "An exception occurred while gathering the style information from the 'ComponentUI' of '{}'.", owner, e);
        }

        // 3. Component Local (  `withStyle(it->it.border(2, "green"))`  )
        try {
            ComponentStyleDelegate<C> updated = _localStyler.style(new ComponentStyleDelegate<>(owner, styleConf));
            Objects.requireNonNull(updated);
            styleConf = updated.style();
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "An exception occurred while gathering the local style for component '{}'.", owner, e);
            /*
                 If any exceptions happen in a Styler implementation provided by a user,
                 then we don't want to prevent the other Stylers from doing their job,
                 which is why we catch any exceptions immediately!
            */
        }

        // 4. Component Local Animations:
        // => Animation styles are last: they override everything else:
        for ( Expirable<Styler<C>> expirableStyler : _animationStylers )
            try {
                styleConf = expirableStyler.get().style(new ComponentStyleDelegate<>(owner, styleConf)).style();
            } catch ( Exception e ) {
                log.warn(SwingTree.get().logMarker(), "An exception occurred while gathering an animated style!", e);
                /*
                     If any exceptions happen in a Styler implementation provided by a user,
                     then we don't want to prevent the other Stylers from doing their job,
                     which is why we catch any exceptions immediately!

                     We log as warning because exceptions during
                     styling are not a big deal!

                     Hi there! If you are reading this, you are probably a developer using the SwingTree
                     library, thank you for using it! Good luck finding out what went wrong! :)
                */
            }

        styleConf = styleConf.simplified();

        styleConf = _applyDPIScaling(styleConf);

        styleConf = styleConf.correctedForRounding();

        return styleConf;
    }

    private static StyleConf _applyDPIScaling(StyleConf styleConf) {
        if ( UI.scale() == 1f )
            return styleConf;

        return styleConf.scale( UI.scale() );
    }

    private static @Nullable ComponentUI _findComponentUIOf(JComponent component) {
        // Try to cast to known component types that have getUI() method
        if (component instanceof AbstractButton) {
            return ((AbstractButton) component).getUI();
            /*
                Note, this branch also covers:
                    JButton, JToggleButton, JMenu, JMenuItem,
                    JCheckBox, JCheckBoxMenuItem and JRadioButtonMenuItem
            */
        }
        if (component instanceof JTextComponent) {
            return ((JTextComponent) component).getUI();
            /*
                Note, this branch also covers:
                    JTextField, JPasswordField, JFormattedTextField,
                    JTextArea,
            */
        }
        if (component instanceof JPanel) {
            return ((JPanel) component).getUI();
        }
        if (component instanceof JLabel) {
            return ((JLabel) component).getUI();
        }
        if (component instanceof JScrollBar) {
            return ((JScrollBar) component).getUI();
        }
        if (component instanceof JScrollPane) {
            return ((JScrollPane) component).getUI();
        }
        if (component instanceof JSplitPane) {
            return ((JSplitPane) component).getUI();
        }
        if (component instanceof JTabbedPane) {
            return ((JTabbedPane) component).getUI();
        }
        if (component instanceof JToolBar) {
            return ((JToolBar) component).getUI();
        }
        if (component instanceof JRootPane) {
            return ((JRootPane) component).getUI();
        }
        if (component instanceof JPopupMenu) {
            return ((JPopupMenu) component).getUI();
        }
        if (component instanceof JMenuBar) {
            return ((JMenuBar) component).getUI();
        }
        if (component instanceof JProgressBar) {
            return ((JProgressBar) component).getUI();
        }
        if (component instanceof JSlider) {
            return ((JSlider) component).getUI();
        }
        if (component instanceof JSpinner) {
            return ((JSpinner) component).getUI();
        }
        if (component instanceof JToolTip) {
            return ((JToolTip) component).getUI();
        }
        if (component instanceof JTable) {
            return ((JTable) component).getUI();
        }
        if (component instanceof JTree) {
            return ((JTree) component).getUI();
        }
        if (component instanceof JList<?>) {
            return ((JList<?>) component).getUI();
        }
        if (component instanceof JColorChooser) {
            return ((JColorChooser) component).getUI();
        }
        if (component instanceof JFileChooser) {
            return ((JFileChooser) component).getUI();
        }
        if (component instanceof JDesktopPane) {
            return ((JDesktopPane) component).getUI();
        }
        if (component instanceof JInternalFrame) {
            return ((JInternalFrame) component).getUI();
        }
        if (component instanceof JViewport) {
            return ((JViewport) component).getUI();
        }
        if (component instanceof JComboBox<?>) {
            return ((JComboBox<?>) component).getUI();
        }
        if (component instanceof JOptionPane) {
            return ((JOptionPane) component).getUI();
        }

        return null;
    }

}

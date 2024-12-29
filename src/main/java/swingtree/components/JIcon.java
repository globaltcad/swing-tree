package swingtree.components;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.From;
import sprouts.Val;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.api.IconDeclaration;
import swingtree.layout.Size;
import swingtree.style.ComponentExtension;
import swingtree.style.StylableComponent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import java.awt.Graphics;

/**
 *  A {@link JLabel} subclass specifically designed to display icons only.
 *  Although a {@link JLabel} already provides the ability to display icons,
 *  this class is useful for styling purposes, as it is possible to specifically
 *  target icons inside of {@link swingtree.style.StyleSheet}s.
 */
public class JIcon extends JLabel implements StylableComponent
{
    private static final Logger log = LoggerFactory.getLogger(JIcon.class);

    @SuppressWarnings("UnusedVariable")
    private final @Nullable Val<IconDeclaration> dynamicIcon;
    /*                                                ^
        We need to keep a strong reference to the dynamic icon, otherwise
        it will be garbage collected and the change listener will not update
        the icon when it changes.
    */

    public JIcon(String path) {
        super(_getFromCacheOrLoadFrom(IconDeclaration.of(path)));
        updateUI();
        dynamicIcon = null;
    }

    public JIcon(IconDeclaration declaration) {
        super(_getFromCacheOrLoadFrom(declaration));
        dynamicIcon = null;
    }

    public JIcon(Icon icon) {
        super(icon);
        updateUI();
        dynamicIcon = null;
    }

    public JIcon( Size size, Icon icon ) {
        super(UI.scaleIconTo(size, icon));
        updateUI();
        dynamicIcon = null;
    }

    public JIcon(Icon icon, String text, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        updateUI();
        dynamicIcon = null;
    }

    public JIcon(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        updateUI();
        dynamicIcon = null;
    }

    public JIcon(String path, String text) {
        super(text, _getFromCacheOrLoadFrom(IconDeclaration.of(path)), CENTER);
        updateUI();
        dynamicIcon = null;
    }

    public JIcon( Val<IconDeclaration> declaration ) {
        Viewable.cast(declaration).onChange(From.ALL, it -> {
            UI.runNow(()->{
                setIcon(_getFromCacheOrLoadFrom(it.currentValue().orElseThrowUnchecked()));
            });
        });
        declaration.ifPresent( it -> setIcon(_getFromCacheOrLoadFrom(it)) );
        updateUI();
        dynamicIcon = declaration;
    }

    public JIcon() {
        super();
        updateUI();
        dynamicIcon = null;
    }

    /** {@inheritDoc} */
    @Override public void paintComponent(Graphics g){
        paintBackground(g, super::paintComponent);
    }

    /** {@inheritDoc} */
    @Override public void paintChildren(Graphics g) {
        paintForeground(g, super::paintChildren);
    }

    @Override public void setUISilently( ComponentUI ui ) {
        this.ui = ui;
    }

    @Override
    public void updateUI() {
        ComponentExtension.from(this).installCustomUIIfPossible();
        /*
            The JIcon is a SwingTree native component type, so it also
            enjoys the perks of having a SwingTree based look and feel!
        */
    }

    @SuppressWarnings("NullAway")
    private static @Nullable ImageIcon _getFromCacheOrLoadFrom( IconDeclaration declaration ) {
        if ( !UI.thisIsUIThread() ) {
            log.warn(
                "Loading an icon off the UI thread. " +
                "This may lead to unexpected behavior and should be avoided.",
                new Throwable() // Log the stack trace for debugging purposes.
            );
            return UI.runAndGet(()->_getFromCacheOrLoadFrom(declaration));
        }

        return UI.findIcon(declaration).orElse(null);
    }
}

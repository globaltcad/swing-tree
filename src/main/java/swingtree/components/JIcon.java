package swingtree.components;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.From;
import sprouts.Val;
import swingtree.SwingTree;
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

    /**
     *  Constructs a JIcon with an icon loaded from the specified file path.
     *  @param path The file path to the icon resource.
     */
    public JIcon(String path) {
        super(_getFromCacheOrLoadFrom(IconDeclaration.of(path)));
        updateUI();
        dynamicIcon = null;
    }

    /**
     *  Constructs a JIcon with the specified icon declaration.
     *  @param declaration The icon declaration specifying which icon to display.
     */
    public JIcon(IconDeclaration declaration) {
        super(_getFromCacheOrLoadFrom(declaration));
        dynamicIcon = null;
    }

    /**
     *  Constructs a JIcon with the specified icon.
     *  @param icon The icon to display.
     */
    public JIcon(Icon icon) {
        super(icon);
        updateUI();
        dynamicIcon = null;
    }

    /**
     *  Constructs a JIcon with the specified size and icon.
     *  @param size The desired size for the icon.
     *  @param icon The icon to display.
     */
    public JIcon( Size size, Icon icon ) {
        super(UI.scaleIconTo(size, icon));
        updateUI();
        dynamicIcon = null;
    }

    /**
     *  Constructs a JIcon with the specified icon, text, and horizontal alignment.
     *  @param icon The icon to display.
     *  @param text The text to display alongside the icon.
     *  @param horizontalAlignment The horizontal alignment of the icon and text.
     */
    public JIcon(Icon icon, String text, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        updateUI();
        dynamicIcon = null;
    }

    /**
     *  Constructs a JIcon with the specified text and horizontal alignment.
     *  @param text The text to display.
     *  @param horizontalAlignment The horizontal alignment of the text.
     */
    public JIcon(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        updateUI();
        dynamicIcon = null;
    }

    /**
     *  Constructs a JIcon with the specified icon path and text.
     *  @param path The file path to the icon resource.
     *  @param text The text to display alongside the icon.
     */
    public JIcon(String path, String text) {
        super(text, _getFromCacheOrLoadFrom(IconDeclaration.of(path)), CENTER);
        updateUI();
        dynamicIcon = null;
    }

    /**
     *  Constructs a JIcon with a dynamically bound icon declaration.
     *  The icon will update whenever the declaration property changes.
     *  @param declaration A property holding the icon declaration to display.
     */
    public JIcon( Val<IconDeclaration> declaration ) {
        ComponentExtension.from(this).storeBoundObservable(
                declaration.view().onChange(From.ALL, it -> {
                    UI.runNow(()->{
                        setIcon(_getFromCacheOrLoadFrom(it.currentValue().orElseThrowUnchecked()));
                    });
                })
        );
        declaration.ifPresent( it -> setIcon(_getFromCacheOrLoadFrom(it)) );
        updateUI();
        dynamicIcon = declaration;
    }

    /**
     *  Constructs an empty JIcon with no icon initially displayed.
     */
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
            log.warn(SwingTree.get().logMarker(),
                "Loading an icon off the UI thread. " +
                "This may lead to unexpected behavior and should be avoided.",
                new Throwable("Stack trace for debugging purposes.")
            );
            return UI.runAndGet(()->_getFromCacheOrLoadFrom(declaration));
        }

        return UI.findIcon(declaration).orElse(null);
    }
}

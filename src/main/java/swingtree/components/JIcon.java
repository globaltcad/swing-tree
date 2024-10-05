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
import swingtree.style.SvgIcon;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Map;

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

    public JIcon(Size size, Icon icon) {
        super(_scale(size, icon));
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

    public JIcon(Val<IconDeclaration> declaration) {
        declaration.onChange(From.ALL, it -> {
            UI.runNow(()->{
                setIcon(_getFromCacheOrLoadFrom(it.get()));
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

    private static @Nullable ImageIcon _getFromCacheOrLoadFrom( IconDeclaration declaration ) {
        if ( !UI.thisIsUIThread() )
            log.warn(
                "Loading an icon off the UI thread. " +
                "This may lead to unexpected behavior and should be avoided.",
                new Throwable() // Log the stack trace for debugging purposes.
            );

        return UI.runAndGet(()-> {
            Map<IconDeclaration, ImageIcon> cache = SwingTree.get().getIconCache();
            ImageIcon icon = cache.get(declaration);
            if ( icon == null ) {
                icon = UI.findIcon(declaration).orElse(null);
                if ( icon != null )
                    cache.put(declaration, icon);
            }

            Size size = declaration.size();
            return (ImageIcon) _scale(size, icon);
        });
    }

    private static @Nullable Icon _scale(Size size, @Nullable Icon icon) {
        int width = size.width().map(Float::intValue).orElse(0);
        int height = size.height().map(Float::intValue).orElse(0);
        float scale = UI.scale();

        if ( width < 1 || height < 1 || scale <= 0 || icon == null ) {
            log.warn(
                    "Encountered an invalid icon size '" + size + "' or scale '" + scale + "' " +
                    "while scaling an icon for the JIcon component.", new Throwable()
                );
            return icon;
        }

        int scaleHint = Image.SCALE_SMOOTH;
        if ( scale != 1f && icon != null) {
            if (scale > 1.5f)
                scaleHint = Image.SCALE_FAST; // High DPI is smooth enough.

            width  = Math.round(width * scale);
            height = Math.round(height * scale);

        }
        if ( icon instanceof SvgIcon ) {
            SvgIcon svgIcon = (SvgIcon) icon;
            svgIcon = svgIcon.withIconSize(width, height);
            return svgIcon;
        } else if ( icon instanceof ImageIcon ) {
            Image scaled = ((ImageIcon)icon).getImage().getScaledInstance(width, height, scaleHint);
            return new ImageIcon(scaled);
        }
        return icon;
    }
}

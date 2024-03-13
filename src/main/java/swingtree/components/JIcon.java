package swingtree.components;

import org.jspecify.annotations.Nullable;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.IconDeclaration;
import swingtree.style.ComponentExtension;
import swingtree.style.StylableComponent;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

/**
 *  A {@link JLabel} subclass specifically designed to display icons only.
 *  Although a {@link JLabel} already provides the ability to display icons,
 *  this class is useful for styling purposes, as it is possible to specifically
 *  target icons inside of {@link swingtree.style.StyleSheet}s.
 */
public class JIcon extends JLabel implements StylableComponent
{
    public JIcon(String path) {
        super(_getFromCacheOrLoadFrom(IconDeclaration.of(path)));
        updateUI();
    }

    public JIcon(IconDeclaration declaration) {
        super(_getFromCacheOrLoadFrom(declaration));
    }

    public JIcon(Icon icon) {
        super(icon);
        updateUI();
    }

    public JIcon(Icon icon, String text, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        updateUI();
    }

    public JIcon(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        updateUI();
    }

    public JIcon(String path, String text) {
        super(text, _getFromCacheOrLoadFrom(IconDeclaration.of(path)), CENTER);
        updateUI();
    }

    public JIcon() {
        super();
        updateUI();
    }

    /** {@inheritDoc} */
    @Override public void paint(Graphics g){
        paintBackground(g, ()->super.paint(g));
    }

    /** {@inheritDoc} */
    @Override public void paintChildren(Graphics g) {
        paintForeground(g, ()->super.paintChildren(g));
    }

    @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }

    @Override
    public void updateUI() {
        ComponentExtension.from(this).installCustomUIIfPossible();
        /*
            The JIcon is a SwingTree native component type, so it also
            enjoys the perks of having a SwingTree based look and feel!
        */
    }

    private static @Nullable ImageIcon _getFromCacheOrLoadFrom(IconDeclaration declaration ) {
        Map<IconDeclaration, ImageIcon> cache = SwingTree.get().getIconCache();
        ImageIcon icon = cache.get(declaration);
        if ( icon == null ) {
            icon = UI.findIcon(declaration).orElse(null);
            if ( icon != null )
                cache.put(declaration, icon);
        }
        return icon;
    }
}

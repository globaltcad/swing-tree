package swingtree.components;

import swingtree.SwingTree;
import swingtree.UI;
import swingtree.style.ComponentExtension;

import javax.swing.*;
import javax.swing.plaf.LabelUI;
import java.util.Map;

/**
 *  A {@link JLabel} subclass specifically designed to display icons only.
 *  Although a {@link JLabel} already provides the ability to display icons,
 *  this class is useful for styling purposes, as it is possible to specifically
 *  target icons inside of {@link swingtree.style.StyleSheet}s.
 */
public class JIcon extends JLabel
{
    public JIcon(String path) {
        super(_getFromCacheOrLoadFrom(path));
        updateUI();
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
        super(text, _getFromCacheOrLoadFrom(path), CENTER);
        updateUI();
    }

    public JIcon() {
        super();
        updateUI();
    }

    @Override
    public void updateUI() {
        ComponentExtension.from(this).updateUI();
        /*
            The JIcon is a SwingTree native component type, so it also
            enjoys the perks of having a SwingTree based look and feel!
        */
    }

    private static ImageIcon _getFromCacheOrLoadFrom( String path ) {
        Map<String, ImageIcon> cache = SwingTree.get().getIconCache();
        ImageIcon icon = cache.get(path);
        if ( icon == null ) {
            icon = UI.loadIcon(path);
            cache.put(path, icon);
        }
        return icon;
    }
}

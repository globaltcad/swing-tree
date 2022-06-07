package com.globaltcad.swingtree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *  A swing tree builder for {@link JLabel} instances.
 */
public class UIForLabel<L extends JLabel> extends UIForAbstractSwing<UIForLabel<L>, L>
{
    protected UIForLabel(L component) { super(component); }

    /**
     *  Makes the wrapped {@link JLabel} font bold (!plain).
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> makeBold() {
        this.make( label -> {
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        });
        return this;
    }

    public UIForLabel<L> makeLinkTo(String href) {
        LazyRef<String> text = LazyRef.of(_component::getText);
        if ( !href.startsWith("http") ) href = "https://" + href;
        String finalHref = href;
        _component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(finalHref));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
            @Override  public void mouseExited(MouseEvent e) { _component.setText(text.get()); }
            @Override
            public void mouseEntered(MouseEvent e) {
                _component.setText("<html><a href=''>" + text.get() + "</a></html>");
            }
        });
        return this;
    }

    /**
     *  Makes the wrapped {@link JLabel} font plain (!bold).
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> makePlain() {
        this.make( label -> {
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
        });
        return this;
    }

    /**
     *  Makes the wrapped {@link JLabel} font bold if it is plain
     *  and plain if it is bold...
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> toggleBold() {
        this.make( label -> {
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        });
        return this;
    }

    public UIForLabel<L> withPosition(UI.HorizontalAlign horizontalAlign) {
        _component.setHorizontalAlignment(horizontalAlign.forSwing());
        return this;
    }

    public UIForLabel<L> withTextPosition(UI.HorizontalAlign horizontalAlign) {
        _component.setHorizontalTextPosition(horizontalAlign.forSwing());
        return this;
    }

    public UIForLabel<L> withPosition(UI.VerticalAlign horizontalAlign) {
        _component.setVerticalAlignment(horizontalAlign.forSwing());
        return this;
    }

    public UIForLabel<L> withTextPosition(UI.VerticalAlign horizontalAlign) {
        _component.setVerticalTextPosition(horizontalAlign.forSwing());
        return this;
    }

    public UIForLabel<L> withIcon(Icon icon) {
        LogUtil.nullArgCheck(icon,"icon",Icon.class);
        _component.setIcon(icon);
        return this;
    }
}


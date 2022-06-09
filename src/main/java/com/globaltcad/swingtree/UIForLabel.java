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
        this.peek(label -> {
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        });
        return this;
    }

    /**
     *  Use this to transform the underlying {@link JLabel} into a clickable link.
     *
     * @param href A string containing a valid URL used as link hyper reference.
     * @return This very builder to allow for method chaining.
     */
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
        this.peek(label -> {
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
        this.peek(label -> {
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        });
        return this;
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setHorizontalAlignment(...) );
     *  }</pre>
     *
     * @param horizontalAlign The horizontal alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withPosition(UI.HorizontalAlign horizontalAlign) {
        _component.setHorizontalAlignment(horizontalAlign.forSwing());
        return this;
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setHorizontalTextPosition(...) );
     *  }</pre>
     *
     * @param horizontalAlign The horizontal alignment which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withTextPosition(UI.HorizontalAlign horizontalAlign) {
        _component.setHorizontalTextPosition(horizontalAlign.forSwing());
        return this;
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setVerticalAlignment(...) );
     *  }</pre>
     *
     * @param verticalAlign The vertical alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withPosition(UI.VerticalAlign verticalAlign) {
        _component.setVerticalAlignment(verticalAlign.forSwing());
        return this;
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setVerticalTextPosition(...) );
     *  }</pre>
     *
     * @param verticalAlign The vertical alignment which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withTextPosition(UI.VerticalAlign verticalAlign) {
        _component.setVerticalTextPosition(verticalAlign.forSwing());
        return this;
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setIcon(...) );
     *  }</pre>
     *
     *
     * @param icon The {@link Icon} which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withIcon(Icon icon) {
        LogUtil.nullArgCheck(icon,"icon",Icon.class);
        _component.setIcon(icon);
        return this;
    }
}


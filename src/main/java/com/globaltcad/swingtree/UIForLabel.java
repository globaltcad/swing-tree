package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *  A swing tree builder node for {@link JLabel} instances.
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
        L list = getComponent();
        LazyRef<String> text = LazyRef.of(list::getText);
        if ( !href.startsWith("http") ) href = "https://" + href;
        String finalHref = href;
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(finalHref));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
            @Override  public void mouseExited(MouseEvent e) { list.setText(text.get()); }
            @Override
            public void mouseEntered(MouseEvent e) {
                list.setText("<html><a href=''>" + text.get() + "</a></html>");
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
     * Defines the single line of text this component will display.  If
     * the value of text is null or empty string, nothing is displayed.
     * <p>
     * The default value of this property is null.
     * <p>
     *
     * @param text The new text to be set for the wrapped label.
     * @return This very builder to allow for method chaining.
     */
    public final UIForLabel<L> withText(String text) { getComponent().setText(text); return this; }

    public final UIForLabel<L> withText( Val<String> val ) {
        L list = getComponent();
        val.onShow(v->doUI(()->list.setText(v)));
        return withText( val.get() );
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setHorizontalAlignment(...) );
     *  }</pre>
     * This sets the horizontal alignment of the label's content (icon and text).
     *
     * @param horizontalAlign The horizontal alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> with(UI.HorizontalAlignment horizontalAlign) {
        getComponent().setHorizontalAlignment(horizontalAlign.forSwing());
        return this;
    }

    /**
     *  Use this to set the vertical alignment of the label's content (icon and text).
     *  This is a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setVerticalAlignment(...) );
     *  }</pre>
     *
     * @param verticalAlign The vertical alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> with(UI.VerticalAlignment verticalAlign) {
        getComponent().setVerticalAlignment(verticalAlign.forSwing());
        return this;
    }

    /**
     *  Use this to set the horizontal position of the label's text, relative to its image.
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setHorizontalTextPosition(...) );
     *  }</pre>
     *
     * @param horizontalAlign The horizontal alignment which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withImageRelative(UI.HorizontalAlignment horizontalAlign) {
        getComponent().setHorizontalTextPosition(horizontalAlign.forSwing());
        return this;
    }

    /**
     *  Use this to set the horizontal position of the label's text, relative to its image. <br>
     *  This is a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setVerticalTextPosition(...) );
     *  }</pre>
     *
     * @param verticalAlign The vertical alignment which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withImageRelative(UI.VerticalAlignment verticalAlign) {
        getComponent().setVerticalTextPosition(verticalAlign.forSwing());
        return this;
    }

    /**
     *  Use this to set the icon for the wrapped {@link JLabel}. 
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setIcon(...) );
     *  }</pre>
     *
     *
     * @param icon The {@link Icon} which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> with(Icon icon) {
        LogUtil.nullArgCheck(icon,"icon",Icon.class);
        getComponent().setIcon(icon);
        return this;
    }

    /**
     *  Use this to set the size of the font of the wrapped {@link JLabel}.
     * @param size The size of the font which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withFontSize(int size) {
        L label = getComponent();
        Font old = label.getFont();
        label.setFont(new Font(old.getName(), old.getStyle(), size));
        return this;
    }

}


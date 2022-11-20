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
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
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
    public final UIForLabel<L> toggleBold() {
        this.peek(label -> {
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        });
        return this;
    }

    public final UIForLabel<L> isBoldIf( boolean isBold ) {
        if ( isBold ) makeBold();
        else makePlain();
        return this;
    }

    public final UIForLabel<L> isBoldIf( Val<Boolean> val ) {
        val.onShow(v -> _doUI(() -> isBoldIf(v)));
        return isBoldIf( val.get() );
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
    public final UIForLabel<L> withText( String text ) { getComponent().setText(text); return this; }

    public final UIForLabel<L> withText( Val<String> val ) {
        val.onShow(v-> _doUI(()->getComponent().setText(v)));
        return withText( val.orElseThrow() );
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
    public UIForLabel<L> with( UI.HorizontalAlignment horizontalAlign ) {
        getComponent().setHorizontalAlignment(horizontalAlign.forSwing());
        return this;
    }


    /**
     * This binds to a property defining the horizontal alignment of the label's content (icon and text).
     *
     * @param horizontalAlign The horizontal alignment property which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withHorizontalAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        horizontalAlign.onShow(v -> _doUI(() -> getComponent().setHorizontalAlignment(v.forSwing())));
        return with(horizontalAlign.orElseThrow());
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
    public UIForLabel<L> with( UI.VerticalAlignment verticalAlign ) {
        getComponent().setVerticalAlignment(verticalAlign.forSwing());
        return this;
    }

    /**
     * This binds to a property defining the vertical alignment of the label's content (icon and text).
     *
     * @param verticalAlign The vertical alignment property which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withVerticalAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        verticalAlign.onShow(v -> _doUI(() -> getComponent().setVerticalAlignment(v.forSwing())));
        return with(verticalAlign.orElseThrow());
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
    public UIForLabel<L> withImageRelative( UI.HorizontalAlignment horizontalAlign ) {
        getComponent().setHorizontalTextPosition(horizontalAlign.forSwing());
        return this;
    }

    /**
     *  Use this to bind to a property defining the horizontal position of the label's text, relative to its image.
     *
     * @param horizontalAlign The horizontal alignment property which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withImageRelativeHorizontalAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        horizontalAlign.onShow(v -> _doUI(() -> getComponent().setHorizontalTextPosition(v.forSwing())));
        return withImageRelative(horizontalAlign.orElseThrow());
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
    public UIForLabel<L> withImageRelative( UI.VerticalAlignment verticalAlign ) {
        getComponent().setVerticalTextPosition(verticalAlign.forSwing());
        return this;
    }

    /**
     *  Use this to bind to a property defining the vertical position of the label's text, relative to its image.
     *
     * @param verticalAlign The vertical alignment property which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withImageRelativeVerticalAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        verticalAlign.onShow(v -> _doUI(() -> getComponent().setVerticalTextPosition(v.forSwing())));
        return withImageRelative(verticalAlign.orElseThrow());
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
    public UIForLabel<L> with( Icon icon ) {
        LogUtil.nullArgCheck(icon,"icon",Icon.class);
        getComponent().setIcon(icon);
        return this;
    }

    /**
     *  Use this to dynamically set the icon property for the wrapped {@link JLabel}.
     *
     * @param icon The {@link Icon} property which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withIcon( Val<Icon> icon ) {
        icon.onShow(i-> _doUI(()->getComponent().setIcon(i)));
        return with(icon.orElseThrow());
    }

    /**
     *  Use this to set the size of the font of the wrapped {@link JLabel}.
     * @param size The size of the font which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withFontSize( int size ) {
        L label = getComponent();
        Font old = label.getFont();
        label.setFont(new Font(old.getName(), old.getStyle(), size));
        return this;
    }

    /**
     *  Use this to dynamically set the size of the font of the wrapped {@link JLabel}
     *  through the provided view model property.
     * @param size The size property of the font which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withFontSize( Val<Integer> size ) {
        size.onShow(s -> _doUI(() -> withFontSize(s)));
        return withFontSize(size.orElseThrow());
    }
}


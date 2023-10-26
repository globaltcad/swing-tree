package swingtree;

import sprouts.Val;
import swingtree.api.IconDeclaration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JLabel} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public class UIForLabel<L extends JLabel> extends UIForAnySwing<UIForLabel<L>, L>
{
    protected UIForLabel( L component ) { super(component); }

    private void _makeBold( L thisComponent ) {
        Font f = thisComponent.getFont();
        thisComponent.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
    }

    private void _makePlain( L thisComponent ) {
        Font f = thisComponent.getFont();
        thisComponent.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
    }

    /**
     *  Makes the wrapped {@link JLabel} font bold (!plain).
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> makeBold() {
        this.peek( label -> {
            _makeBold(label);
        });
        return this;
    }

    /**
     *  Use this to make the underlying {@link JLabel} into a clickable link.
     *
     * @param href A string containing a valid URL used as link hyper reference.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code href} is {@code null}.
     */
    public UIForLabel<L> makeLinkTo( String href ) {
        NullUtil.nullArgCheck( href, "href", String.class );
        return makeLinkTo( Val.of(href) );
    }

    /**
     *  Use this to make the underlying {@link JLabel} into a clickable link
     *  based on the string provided property defining the link address.
     *  When the link wrapped by the provided property changes,
     *  then a click on the label will lead to the wrapped link.
     *
     * @param href A string property containing a valid URL used as link hyper reference.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code href} is {@code null}.
     */
    public UIForLabel<L> makeLinkTo( Val<String> href ) {
        NullUtil.nullArgCheck( href, "href", Val.class );
        NullUtil.nullPropertyCheck( href, "href", "Use an empty String instead of null to model a link going nowhere." );
        return _with( thisComponent -> {
                    LazyRef<String> text = LazyRef.of(thisComponent::getText);
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            try {
                                String ref = href.orElseThrow().trim();
                                if ( ref.isEmpty() ) return;
                                if ( !ref.startsWith("http") ) ref = "https://" + ref;
                                Desktop.getDesktop().browse(new URI(ref));
                            } catch (IOException | URISyntaxException e1) {
                                e1.printStackTrace();
                            }
                        }
                        @Override  public void mouseExited(MouseEvent e) { thisComponent.setText(text.get()); }
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            thisComponent.setText("<html><a href=''>" + text.get() + "</a></html>");
                        }
                    });
                })
                ._this();
    }

    /**
     *  Makes the wrapped {@link JLabel} font plain (!bold).
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> makePlain() {
        this.peek( label -> {
            _makePlain(label);
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

    /**
     * @param isBold The flag determining if the font of this label should be bold or plain.
     * @return This very builder to allow for method chaining.
     */
    public final UIForLabel<L> isBoldIf( boolean isBold ) {
        if ( isBold )
            return makeBold();
        else
            return makePlain();
    }

    /**
     *  When the flag wrapped by the provided property changes,
     *  then the font of this label will switch between being bold and plain.
     *
     * @param isBold The property which should be bound to the boldness of this label.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code isBold} is {@code null}.
     */
    public final UIForLabel<L> isBoldIf( Val<Boolean> isBold ) {
        NullUtil.nullArgCheck( isBold, "isBold", Val.class );
        NullUtil.nullPropertyCheck( isBold, "isBold", "You can not use null to model if a label is bold or not." );
        return _withOnShow( isBold, (thisComponent,v) -> {
                    if ( v )
                        _makeBold(thisComponent);
                    else
                        _makePlain(thisComponent);
                })
                ._with( thisComponent -> {
                    if ( isBold.orElseThrow() )
                        _makeBold(thisComponent);
                    else
                        _makePlain(thisComponent);
                })
                ._this();
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
     * @throws IllegalArgumentException if {@code text} is {@code null}.
     */
    public final UIForLabel<L> withText( String text ) {
        NullUtil.nullArgCheck( text, "text", String.class );
        return _with( thisComponent -> {
                    thisComponent.setText(text);
                })
                ._this();
    }

    /**
     *  Dynamically defines a single line of text displayed on this label.
     *  If the value of text is null or an empty string, nothing is displayed.
     *  When the text wrapped by the provided property changes,
     *  then so does the text displayed on this label change.
     *
     * @param text The text property to be bound to the wrapped label.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code text} is {@code null}.
     */
    public final UIForLabel<L> withText( Val<String> text ) {
        NullUtil.nullArgCheck( text, "text", Val.class );
        NullUtil.nullPropertyCheck( text, "text", "Please use an empty String instead of null." );
        return _withOnShow( text, (thisComponent,v) -> {
                    thisComponent.setText(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setText( text.orElseThrow() );
                })
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *     .peek( label -> label.setHorizontalAlignment(...) );
     *  }</pre>
     * This sets the horizontal alignment of the label's content (icon and text).
     *
     * @param horizontalAlign The horizontal alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public UIForLabel<L> withHorizontalAlignment( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck( horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class );
        return _with( thisComponent -> {
                    thisComponent.setHorizontalAlignment(horizontalAlign.forSwing());
                })
                ._this();
    }


    /**
     *  This binds to a property defining the horizontal alignment of the label's content (icon and text).
     *  When the alignment enum wrapped by the provided property changes,
     *  then so does the alignment of this label.
     *
     * @param horizontalAlign The horizontal alignment property which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public UIForLabel<L> withHorizontalAlignment(Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck( horizontalAlign, "horizontalAlign", Val.class );
        NullUtil.nullPropertyCheck( horizontalAlign, "horizontalAlign", "Null is not a valid alignment." );
        return _withOnShow( horizontalAlign, (thisComponent,v) -> {
                    thisComponent.setHorizontalAlignment(v.forSwing());
                })
                ._with( thisComponent -> {
                    thisComponent.setHorizontalAlignment(horizontalAlign.orElseThrow().forSwing());
                })
                ._this();
    }

    /**
     *  Use this to set the vertical alignment of the label's content (icon and text).
     *  This is a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *     .peek( label -> label.setVerticalAlignment(...) );
     *  }</pre>
     *
     * @param verticalAlign The vertical alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public UIForLabel<L> withVerticalAlignment( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck( verticalAlign, "verticalAlign", UI.VerticalAlignment.class );
        return _with( thisComponent -> {
                    thisComponent.setVerticalAlignment(verticalAlign.forSwing());
                })
                ._this();
    }

    /**
     * This binds to a property defining the vertical alignment of the label's content (icon and text).
     *  When the alignment enum wrapped by the provided property changes,
     *  then so does the alignment of this label.
     *
     * @param verticalAlign The vertical alignment property which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public UIForLabel<L> withVerticalAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck( verticalAlign, "verticalAlign", Val.class );
        NullUtil.nullPropertyCheck( verticalAlign, "verticalAlign", "Null is not a valid alignment." );
        return _withOnShow( verticalAlign, (thisComponent,v) -> {
                    thisComponent.setVerticalAlignment(v.forSwing());
                })
                ._with( thisComponent -> {
                    thisComponent.setVerticalAlignment(verticalAlign.orElseThrow().forSwing());
                })
                ._this();
    }

    /**
     *  Use this to set the horizontal and vertical alignment of the label's content (icon and text).
     *  This is a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *     .peek( label -> label.setHorizontalAlignment(...); label.setVerticalAlignment(...) );
     *  }</pre>
     *
     * @param alignment The alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code alignment} is {@code null}.
     */
    public UIForLabel<L> withAlignment( UI.Alignment alignment ) {
        NullUtil.nullArgCheck( alignment, "alignment", UI.Alignment.class );
        return _with( thisComponent -> {
                    thisComponent.setHorizontalAlignment(alignment.getHorizontal().forSwing());
                    thisComponent.setVerticalAlignment(alignment.getVertical().forSwing());
                })
                ._this();
    }

    /**
     *  This binds to a property defining the horizontal and vertical alignment of the label's content (icon and text).
     *  When the alignment enum wrapped by the provided property changes,
     *  then so does the alignment of this label.
     *
     * @param alignment The alignment property which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code alignment} is {@code null}.
     */
    public UIForLabel<L> withAlignment( Val<UI.Alignment> alignment ) {
        NullUtil.nullArgCheck( alignment, "alignment", Val.class );
        NullUtil.nullPropertyCheck( alignment, "alignment", "Null is not a valid alignment." );
        return _withOnShow( alignment, (thisComponent,v) -> {
                    thisComponent.setHorizontalAlignment(v.getHorizontal().forSwing());
                    thisComponent.setVerticalAlignment(v.getVertical().forSwing());
                })
                ._with( thisComponent -> {
                    UI.Alignment a = alignment.orElseThrow();
                    thisComponent.setHorizontalAlignment(a.getHorizontal().forSwing());
                    thisComponent.setVerticalAlignment(a.getVertical().forSwing());
                })
                ._this();
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
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public UIForLabel<L> withHorizontalTextPosition( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck( horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class );
        return _with( thisComponent -> {
                    thisComponent.setHorizontalTextPosition(horizontalAlign.forSwing());
                })
                ._this();
    }

    /**
     *  Use this to bind to a property defining the horizontal position of the label's text, relative to its image.
     *  When the alignment enum wrapped by the provided property changes,
     *  then so does the alignment of this label.
     *
     * @param horizontalAlign The horizontal alignment property which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public UIForLabel<L> withHorizontalTextPosition( Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck( horizontalAlign, "horizontalAlign", Val.class );
        NullUtil.nullPropertyCheck( horizontalAlign, "horizontalAlign", "Null is not a valid alignment." );
        return _withOnShow( horizontalAlign, (thisComponent, v) -> {
                    thisComponent.setHorizontalTextPosition(v.forSwing());
                })
                ._with( thisComponent -> {
                    thisComponent.setHorizontalTextPosition(horizontalAlign.orElseThrow().forSwing());
                })
                ._this();
    }

    /**
     *  Use this to set the horizontal position of the label's text, relative to its image. <br>
     *  This is a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *     .peek( label -> label.setVerticalTextPosition(...) );
     *  }</pre>
     *
     * @param verticalAlign The vertical alignment which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public UIForLabel<L> withVerticalTextPosition( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck( verticalAlign, "verticalAlign", UI.VerticalAlignment.class );
        return _with( thisComponent -> {
                    thisComponent.setVerticalTextPosition(verticalAlign.forSwing());
                })
                ._this();
    }

    /**
     *  Use this to bind to a property defining the vertical position of the label's text, relative to its image.
     *  When the alignment enum wrapped by the provided property changes,
     *  then so does the alignment of this label.
     *
     * @param verticalAlign The vertical alignment property which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public UIForLabel<L> withVerticalTextPosition( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck( verticalAlign, "verticalAlign", Val.class );
        NullUtil.nullPropertyCheck( verticalAlign, "verticalAlign", "Null is not a valid alignment." );
        return _withOnShow( verticalAlign, (thisComponent,v) -> {
                    thisComponent.setVerticalTextPosition(v.forSwing());
                })
                ._with( thisComponent -> {
                    thisComponent.setVerticalTextPosition(verticalAlign.orElseThrow().forSwing());
                })
                ._this();
    }

    /**
     *  Use this to set the horizontal and vertical position of the label's text, relative to its image.
     *  This is a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.label("Something")
     *         .peek( label -> label.setHorizontalTextPosition(...); label.setVerticalTextPosition(...) );
     *  }</pre>
     *
     * @param alignment The alignment which should be applied to the text of the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code alignment} is {@code null}.
     */
    public UIForLabel<L> withTextPosition( UI.Alignment alignment ) {
        NullUtil.nullArgCheck( alignment, "alignment", UI.Alignment.class );
        return _with( thisComponent -> {
                    thisComponent.setHorizontalTextPosition(alignment.getHorizontal().forSwing());
                    thisComponent.setVerticalTextPosition(alignment.getVertical().forSwing());
                })
                ._this();
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
    public UIForLabel<L> withIcon( Icon icon ) {
        return _with( thisComponent -> {
                    thisComponent.setIcon(icon);
                })
                ._this();
    }

    /**
     *  Use this to set the icon for the wrapped {@link JLabel}
     *  based on the provided {@link IconDeclaration}.
     *  <p>
     *  An {@link IconDeclaration} should be preferred over the {@link Icon} class
     *  as part of a view model, because it is a lightweight value object that merely
     *  models the resource location of the icon even if it is not yet loaded or even
     *  does not exist at all.
     *
     * @param icon The {@link IconDeclaration} which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withIcon( IconDeclaration icon ) {
        Objects.requireNonNull(icon,"icon");
        return _with( thisComponent -> {
                    icon.find().ifPresent( i -> thisComponent.setIcon(i) );
                })
                ._this();
    }

    /**
     *  Use this to dynamically set the icon property for the wrapped {@link JLabel}.
     *  When the icon wrapped by the provided property changes,
     *  then so does the icon of this label.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code icon} is {@code null}.
     */
    public UIForLabel<L> withIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon,"icon",Val.class);
        return _withOnShow( icon, (thisComponent,v) -> {
                    v.find().ifPresent(thisComponent::setIcon);
                })
                ._with( thisComponent -> {
                    icon.orElseThrow().find().ifPresent(thisComponent::setIcon);
                })
                ._this();
    }

    /**
     *  Use this to set the size of the font of the wrapped {@link JLabel}.
     * @param size The size of the font which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     */
    public UIForLabel<L> withFontSize( int size ) {
        return _with( thisComponent -> {
                    Font f = thisComponent.getFont();
                    thisComponent.setFont(f.deriveFont((float)size));
                })
                ._this();
    }

    /**
     *  Use this to dynamically set the size of the font of the wrapped {@link JLabel}
     *  through the provided view model property.
     *  When the integer wrapped by the provided property changes,
     *  then so does the font size of this label.
     *
     * @param size The size property of the font which should be displayed on the label.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code size} is {@code null}.
     */
    public UIForLabel<L> withFontSize( Val<Integer> size ) {
        NullUtil.nullArgCheck( size, "size", Val.class );
        NullUtil.nullPropertyCheck( size, "size", "Use the default font size of this component instead of null!" );
        return _withOnShow( size, (thisComponent,v) -> {
                    Font f = thisComponent.getFont();
                    thisComponent.setFont(f.deriveFont((float)v));
                })
                ._with( thisComponent -> {
                    Font f = thisComponent.getFont();
                    thisComponent.setFont(f.deriveFont((float)size.orElseThrow()));
                })
                ._this();
    }

    /**
     *  Use this to set the font of the wrapped {@link JLabel}.
     * @param font The font of the text which should be displayed on the label.
     * @return This builder instance, to allow for method chaining.
     * @throws IllegalArgumentException if {@code font} is {@code null}.
     */
    public final UIForLabel<L> withFont( Font font ) {
        NullUtil.nullArgCheck(font, "font", Font.class);
        return _with( thisComponent -> {
                    thisComponent.setFont(font);
                })
                ._this();
    }

    /**
     *  Use this to dynamically set the font of the wrapped {@link JLabel}
     *  through the provided view model property.
     *  When the font wrapped by the provided property changes,
     *  then so does the font of this label.
     *
     * @param font The font property of the text which should be displayed on the label.
     * @return This builder instance, to allow for method chaining.
     * @throws IllegalArgumentException if {@code font} is {@code null}.
     * @throws IllegalArgumentException if {@code font} is a property which can wrap {@code null}.
     */
    public final UIForLabel<L> withFont( Val<Font> font ) {
        NullUtil.nullArgCheck(font, "font", Val.class);
        NullUtil.nullPropertyCheck(font, "font", "Use the default font of this component instead of null!");
        return _withOnShow( font, (thisComponent,v) -> {
                    thisComponent.setFont(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setFont(font.orElseThrow());
                })
                ._this();
    }

}


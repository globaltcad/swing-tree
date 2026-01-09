package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.api.IconDeclaration;
import swingtree.layout.Size;
import swingtree.style.ScalableImageIcon;
import swingtree.style.SvgIcon;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *  The following is a more specialized type of builder node based on the {@link UIForAnySwing} builder type,
 *  and provides additional features associated with the more specialized
 *  {@link AbstractButton}" Swing component type.
 *  One of such features is the {@link #onClick(Action)} method allowing for a more readable way of adding
 *  {@link ActionListener} instances to button types...
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *  <br><br>
 *
 * @param <B> The type parameter for the component wrapped by an instance of this class.
 * @param <I> The type parameter for the instance type of this concrete class itself.
 */
public abstract class UIForAnyButton<I, B extends AbstractButton> extends UIForAnySwing<I, B>
{
    private static final Logger log = LoggerFactory.getLogger(UIForAnyButton.class);

    /**
     * Defines the single line of text the wrapped button type will display.
     * If the value of text is null or empty string, nothing is displayed.
     *
     * @param text The new text to be set for the wrapped button type.
     * @return This very builder to allow for method chaining.
     */
    public final I withText( String text ) {
        return _with( thisComponent -> thisComponent.setText(text) )._this();
    }

    /**
     *  Binds the provided {@link Val} property to the wrapped button type
     *  and sets the text of the button to the value of the property.
     * <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to manually
     * send the property value to this view component.</i>
     * @param text The view model property which should be bound to this UI.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code text} is {@code null}.
     */
    public final I withText( Val<String> text ) {
        NullUtil.nullArgCheck(text, "val", Val.class);
        NullUtil.nullPropertyCheck(text, "text");
        return _withOnShow( text, AbstractButton::setText )
               ._with( c -> c.setText(text.orElseThrowUnchecked()) )
               ._this();
    }

    /**
     *  Use this to set the icon for the wrapped button type.
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setIcon(...) );
     *  }</pre>
     *  But in addition to simply setting the icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *  Please also see {@link #withIcon(IconDeclaration)}, which is
     *  <b>the recommended way of setting icons on buttons!</b>
     *
     *
     * @param icon The {@link Icon} which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code icon} is {@code null}.
     */
    public final I withIcon( Icon icon ) {
        NullUtil.nullArgCheck(icon,"icon",Icon.class);
        return _with( c -> c.setIcon(_ensureIconIsScalable(icon)) )._this();
    }

    /**
     *  Use this to specify the icon for the wrapped button type.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Click me!")
     *     .peek( button -> icon.find().ifPresent(button::setIcon) );
     *  }</pre>
     *  But in addition to simply setting the icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *
     * @param icon The desired icon to be displayed on top of the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withIcon( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon,"icon", IconDeclaration.class);
        return _with( c -> icon.find().map(UIForAnyButton::_ensureIconIsScalable).ifPresent(c::setIcon) )._this();
    }

    /**
     *  Use this to dynamically set the icon property for the wrapped button type.
     *  When the {@link IconDeclaration} wrapped by the provided property changes,
     *  then so does the icon displayed on this button.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.find().ifPresent(button::setIcon)
     *     ));
     *  }</pre>
     *  But in addition to simply setting the icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *  <p>
     *  But note that here you cannot use the {@link Icon} or {@link ImageIcon} classes directly,
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
     *  This is especially useful when writing unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code icon} is {@code null}.
     */
    public final I withIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow( icon, UIForAnyButton::_setIconFromDeclaration)
               ._with( c -> _setIconFromDeclaration(c, icon.orElseThrowUnchecked()) )
               ._this();
    }

    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type.<br>
     *  Also see {@link #withIcon(int, int, IconDeclaration)}, which is
     *  <b>the preferred way of setting icons on buttons!</b>
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link Icon} which should be scaled and displayed on the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withIcon( int width, int height, Icon icon ) {
        NullUtil.nullArgCheck(icon,"icon",Icon.class);
        icon = _fitTo( width, height, icon );
        return withIcon(icon);
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales it to the provided width and height
     *  before displaying it on the wrapped button type.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link IconDeclaration} which should be scaled and displayed on the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withIcon( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon,"icon",IconDeclaration.class);
        return icon.find()
                   .map( i -> withIcon(width, height, i) )
                   .orElseGet( this::_this );
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales the corresponding icon it
     *  to the provided width and height before displaying it on the wrapped button type.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param icon The {@link IconDeclaration}, whose referenced icon will be scaled and displayed on the button.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withIcon( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent ) {
        NullUtil.nullArgCheck(icon,"icon",IconDeclaration.class);
        return icon.find()
                .map( i -> withIcon(_fitTo(width, height, i), fitComponent) )
                .orElseGet( this::_this );
    }

    /**
     *  Sets the {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is also used as the "pressed" and "disabled" icon if
     *  there is no explicitly set pressed icon.<br>
     *  Please also see {@link #withIcon(IconDeclaration, UI.FitComponent)}, which is
     *  <b>the recommended way of setting icons on buttons!</b>
     *
     * @param icon The {@link Icon} which should be displayed on the button.
     *             Ideally, this should be an {@link SvgIcon} to ensure that {@link swingtree.UI.FitComponent}
     *             can be applied properly.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should
     *                     be scaled to fit the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withIcon( Icon icon, UI.FitComponent fitComponent ) {
        NullUtil.nullArgCheck(icon,"icon", Icon.class);
        NullUtil.nullArgCheck(fitComponent,"fitComponent", UI.FitComponent.class);
        return _with( thisComponent -> {
                          _installAutomaticIconApplier(thisComponent, icon, fitComponent, AbstractButton::setIcon);
                   })
                   ._this();
    }

    /**
     *  Sets the {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is also used as the "pressed" and "disabled" icon if
     *  no other icon type is explicitly set.
     *  This method will try to load and install the icon as a scalable {@link ImageIcon}, which means that
     *  in case of the referenced icon being an SVG file, the icon will be loaded as
     *  a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The {@link IconDeclaration} which should be displayed on the button.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     */
    public final I withIcon( IconDeclaration icon, UI.FitComponent fitComponent ) {
        NullUtil.nullArgCheck(icon,"icon", IconDeclaration.class);
        NullUtil.nullArgCheck(fitComponent,"fitComponent", UI.FitComponent.class);
        return icon.find()
                   .map( i -> withIcon(i, fitComponent) )
                   .orElseGet( this::_this );
    }

    // - On Press Icon:

    /**
     *  Use this to set the "pressed" icon for the wrapped button type.
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setPressedIcon(...) );
     *  }</pre>
     *  Please also see {@link #withIconOnPress(IconDeclaration)}, which is
     *  <b>the recommended way of setting icons on buttons!</b>
     *
     *
     * @param icon The {@link Icon} which should be displayed when the button is pressed.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnPress( Icon icon ) {
        Objects.requireNonNull(icon);
        return _with( c -> c.setPressedIcon(_ensureIconIsScalable(icon)) )._this();
    }

    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type when the user presses the button type.<br>
     *  Also see {@link #withIconOnPress(int, int, IconDeclaration)}, which is
     *  <b>the preferred way of setting icons on buttons!</b>
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is being pressed.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnPress( int width, int height, Icon icon ) {
        Objects.requireNonNull(icon);
        icon = _fitTo( width, height, icon );
        return withIconOnPress(icon);
    }

    /**
     *  Takes the supplied {@link IconDeclaration} and scales it to the desired width and height
     *  before displaying it on the wrapped button type when pressed.
     *  This method will try to load and install the icon as a scalable {@link ImageIcon}, which means that
     *  in case of the referenced icon being an SVG file, the icon will be loaded as
     *  a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link IconDeclaration} which should be scaled and
     *             then displayed when the button is being pressed.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnPress( int width, int height, IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return icon.find()
                .map( i -> withIconOnPress(width, height, i) )
                .orElseGet( this::_this );
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales the corresponding icon it
     *  to the provided width and height before displaying it on the wrapped button type
     *  when pressed by the user.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is being pressed.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnPress( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent ) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map( i -> withIconOnPress(_fitTo(width, height, i), fitComponent) )
                .orElseGet( this::_this );
    }

    /**
     *  Sets the pressed {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the user presses the button type.<br>
     *  Please also see {@link #withIconOnPress(IconDeclaration, UI.FitComponent)}, which is
     *  <b>the recommended way of setting pressed icons on buttons!</b>
     *
     * @param icon The {@link SvgIcon} which should be displayed when the button is being pressed.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnPress( Icon icon, UI.FitComponent fitComponent ) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return _with( thisComponent -> {
                        _installAutomaticIconApplier(thisComponent, icon, fitComponent, AbstractButton::setPressedIcon);
                    })
                    ._this();

    }

    /**
     *  Sets the pressed {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed temporarily when the user presses and holds the button.
     *
     * @param icon The {@link IconDeclaration} which should be displayed when the button is being pressed.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnPress( IconDeclaration icon, UI.FitComponent fitComponent ) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map( i -> withIconOnPress(i, fitComponent) )
                .orElseGet( this::_this );
    }

    /**
     *  Use this to specify the icon for the wrapped button type,
     *  which ought to be displayed temporarily when the user presses the button.
     *  The icon is resolved based on the supplied {@link IconDeclaration}
     *  instance which serves as a resource path to the icon actual.
     *
     * @param icon The desired icon to be displayed on top of the button for when it is being pressed.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnPress( IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return _with( c -> icon.find().map(UIForAnyButton::_ensureIconIsScalable).ifPresent(c::setPressedIcon) )._this();
    }

    /**
     *  Use this to dynamically set the "pressed icon" property for the wrapped button type,
     *  which is displayed when the user presses and holds the button.
     *  When the icon wrapped by the supplied {@link Val} property changes,
     *  then so does the pressed icon of this button.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.find().ifPresent(button::setPressedIcon)
     *     ));
     *  }</pre>
     *  But in addition to simply setting the pressed icon on the component, this method
     *  will also try to load and install the icon as a scalable {@link ImageIcon}, which means that
     *  in case of the referenced icon being an SVG file, the icon will be loaded as
     *  a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *  <p>
     *  But note that here you cannot use the {@link Icon} or {@link ImageIcon} classes directly,
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
     *  This is especially useful when writing unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed when the button is being pressed.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnPress( Val<IconDeclaration> icon ) {
        Objects.requireNonNull(icon);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow( icon, UIForAnyButton::_setIconOnPressFromDeclaration)
                ._with( c -> _setIconOnPressFromDeclaration(c, icon.orElseThrowUnchecked()) )
                ._this();
    }

    // - On Hover Icon:

    /**
     *  Use this to set the "hovered" icon for the wrapped button type,
     *  which translates to {@link AbstractButton#setRolloverIcon(Icon)} of the
     *  underlying button component, but with additional UI scalability guarantees.
     *  In most cases this is a convenience method to avoid peeking into this
     *  builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setRolloverIcon(...) );
     *  }</pre>
     *  But in addition to simply setting the hover/rollover icon on the component,
     *  it will also try to convert it to an icon variant which scales according to
     *  the {@link UI#scale()} factor.
     *  Please also see {@link #withIconOnHover(IconDeclaration)}, which is
     *  <b>the recommended way of setting icons on buttons!</b>
     *
     *
     * @param icon The {@link Icon} which should be displayed when the cursor hovers over the button.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnHover( Icon icon ) {
        Objects.requireNonNull(icon);
        return _with( c -> c.setRolloverIcon(_ensureIconIsScalable(icon)) )._this();
    }

    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type when the user hovers their cursor over
     *  the button type.<br>
     *  Also see {@link #withIconOnHover(int, int, IconDeclaration)}, which is
     *  <b>the preferred way of setting hover icons on buttons!</b>
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is being hovered over.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnHover( int width, int height, Icon icon ) {
        Objects.requireNonNull(icon);
        icon = _fitTo( width, height, icon );
        return withIconOnHover(icon);
    }

    /**
     *  Takes the supplied {@link IconDeclaration} and scales it to the desired width and height
     *  before displaying it on the wrapped button type whenever the user hovers their cursor over the button.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Click me!")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.withSize(width,height)
     *                 .find()
     *                 .ifPresent(button::setRolloverIcon)
     *     ));
     *  }</pre>
     *  In case of the referenced icon being an SVG file, the icon will be loaded as
     *  a fully scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link IconDeclaration} which whose icon should be scaled and
     *             then displayed when the button is being hovered over.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnHover( int width, int height, IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return icon.find()
                .map( i -> withIconOnHover(width, height, i) )
                .orElseGet( this::_this );
    }

    /**
     *  Takes the supplied {@link IconDeclaration} and scales the corresponding icon it
     *  to the desired width and height after which it uses the {@link swingtree.UI.FitComponent}
     *  layout policy enum constant to display it on the wrapped button type
     *  whenever the user hovers their cursor over the button...<br>
     *  Note that the {@code width} and {@code height} properties only serve as reference values
     *  as the {@link swingtree.UI.FitComponent} is used to resize the icon to fit the component.
     *  <p>
     *  In case of the referenced icon being an SVG file, the icon will be loaded as
     *  a fully scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is being hovered over.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnHover( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent ) {
        Objects.requireNonNull(icon); 
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map( i -> withIconOnHover(_fitTo(width, height, i), fitComponent) )
                .orElseGet( this::_this );
    }

    /**
     *  Sets the hover {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the user hovers their cursor over the button type.<br>
     *  Please also see {@link #withIconOnHover(IconDeclaration, UI.FitComponent)}, which is
     *  <b>the recommended way of setting hover icons on buttons!</b>
     *
     * @param icon The {@link SvgIcon} which should be displayed when the button is being hovered over.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnHover( Icon icon, UI.FitComponent fitComponent ) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return _with( thisComponent -> {
                    _installAutomaticIconApplier(thisComponent, icon, fitComponent, AbstractButton::setRolloverIcon);
                })
                ._this();

    }

    /**
     *  Sets the hover {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed temporarily when the user hovers their mouse cursor
     *  over the button area.
     *
     * @param icon The {@link IconDeclaration} which should be displayed when the button is being hovered over.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnHover( IconDeclaration icon, UI.FitComponent fitComponent ) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map( i -> withIconOnHover(i, fitComponent) )
                .orElseGet( this::_this );
    }

    /**
     *  Use this to specify the icon for the wrapped button type,
     *  which ought to be displayed temporarily whenever the user
     *  has their cursor hovering over the button area.
     *  The icon is resolved based on the supplied {@link IconDeclaration}
     *  instance which serves as a resource path to the icon actual.
     *
     * @param icon The desired icon to be displayed on top of the button for when it is being hovered over.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnHover( IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return _with( c -> icon.find().map(UIForAnyButton::_ensureIconIsScalable).ifPresent(c::setRolloverIcon) )._this();
    }

    /**
     *  Use this to dynamically set the "hovered icon" property for the wrapped button type,
     *  which is displayed when the user hovers their cursor over the button area.
     *  When the icon wrapped by the supplied {@link Val} property changes,
     *  then so does the "hover icon" of this button. Or more specifically,
     *  the {@link AbstractButton#getRolloverIcon()} property of the underlying component.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.find().ifPresent(button::setRolloverIcon)
     *     ));
     *  }</pre>
     *  But in addition to simply setting the rollover icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *  <p>
     *  But note that here you cannot use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should always use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful when writing unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed when the button is being hovered over.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnHover( Val<IconDeclaration> icon ) {
        Objects.requireNonNull(icon);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow( icon, UIForAnyButton::_setIconOnHoverFromDeclaration)
                ._with( c -> _setIconOnHoverFromDeclaration(c, icon.orElseThrowUnchecked()) )
                ._this();
    }

    // Icon on Hover and Selected
    
    /**
     *  Use this to set the "rollover and selected" icon for the wrapped button type.
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setRolloverSelectedIcon(...) );
     *  }</pre>
     *  But in addition to simply setting the rollover and selected icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *  Please also see {@link #withIconOnHoverAndSelected(IconDeclaration)}, which is
     *  <b>the recommended way of setting rollover and selected icons on buttons!</b>
     *
     * @param icon The {@link Icon} which should be displayed when the button is both rolled over and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnHoverAndSelected(Icon icon) {
        Objects.requireNonNull(icon);
        return _with(c -> c.setRolloverSelectedIcon(_ensureIconIsScalable(icon)))._this();
    }
    
    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type when the button is both rolled over and selected.<br>
     *  Also see {@link #withIconOnHoverAndSelected(int, int, IconDeclaration)}, which is
     *  <b>the preferred way of setting rollover and selected icons on buttons!</b>
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is both rolled over and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnHoverAndSelected(int width, int height, Icon icon) {
        Objects.requireNonNull(icon);
        icon = _fitTo(width, height, icon);
        return withIconOnHoverAndSelected(icon);
    }
    
    /**
     *  Takes the supplied {@link IconDeclaration} and scales it to the desired width and height
     *  before displaying it on the wrapped button type when both rolled over and selected.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link IconDeclaration} which should be scaled and
     *             then displayed when the button is both rolled over and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnHoverAndSelected(int width, int height, IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return icon.find()
                .map(i -> withIconOnHoverAndSelected(width, height, i))
                .orElseGet(this::_this);
    }
    
    /**
     *  Takes the provided {@link IconDeclaration} and scales the corresponding icon it
     *  to the provided width and height before displaying it on the wrapped button type
     *  when both rolled over and selected by the user.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is both rolled over and selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnHoverAndSelected(int width, int height, IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnHoverAndSelected(_fitTo(width, height, i), fitComponent))
                .orElseGet(this::_this);
    }
    
    /**
     *  Sets the rollover and selected {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is both rolled over and selected.<br>
     *  Please also see {@link #withIconOnHoverAndSelected(IconDeclaration, UI.FitComponent)}, which is
     *  <b>the recommended way of setting rollover and selected icons on buttons!</b>
     *
     * @param icon The {@link SvgIcon} which should be displayed when the button is both rolled over and selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnHoverAndSelected(Icon icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return _with(thisComponent -> {
            _installAutomaticIconApplier(thisComponent, icon, fitComponent, AbstractButton::setRolloverSelectedIcon);
        })
                ._this();
    }
    
    /**
     *  Sets the rollover and selected {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is both rolled over and selected.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The {@link IconDeclaration} which should be displayed when the button is both rolled over and selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnHoverAndSelected(IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnHoverAndSelected(i, fitComponent))
                .orElseGet(this::_this);
    }
    
    /**
     *  Use this to specify the icon for the wrapped button type,
     *  which ought to be displayed when the button is both rolled over and selected.
     *  The icon is resolved based on the supplied {@link IconDeclaration}
     *  instance which serves as a resource path to the icon actual.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The desired icon to be displayed on top of the button for when it is both rolled over and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnHoverAndSelected(IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return _with(c -> icon.find().map(UIForAnyButton::_ensureIconIsScalable).ifPresent(c::setRolloverSelectedIcon))._this();
    }
    
    /**
     *  Use this to dynamically set the "rollover and selected icon" property for the wrapped button type,
     *  which is displayed when the button is both rolled over and selected.
     *  When the icon wrapped by the supplied {@link Val} property changes,
     *  then so does the rollover and selected icon of this button.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.find().ifPresent(button::setRolloverSelectedIcon)
     *     ));
     *  }</pre>
     *  But in addition to simply setting the rollover and selected icon on the component, this method
     *  will also try to load and install the icon as a scalable {@link ImageIcon}, which means that
     *  in case of the referenced icon being an SVG file, the icon will be loaded as
     *  a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *  <p>
     *  But note that here you cannot use the {@link Icon} or {@link ImageIcon} classes directly,
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
     *  This is especially useful when writing unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed when the button is both rolled over and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnHoverAndSelected(Val<IconDeclaration> icon) {
        Objects.requireNonNull(icon);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow(icon, UIForAnyButton::_setIconOnHoverAndSelectedFromDeclaration)
                ._with(c -> _setIconOnHoverAndSelectedFromDeclaration(c, icon.orElseThrowUnchecked()))
                ._this();
    }

    // Icon on Selected:

    /**
     *  Use this to set the "selected" icon for the wrapped button type.
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setSelectedIcon(...) );
     *  }</pre>
     *  But in addition to simply setting the selected icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *  Please also see {@link #withIconOnSelected(IconDeclaration)}, which is
     *  <b>the recommended way of setting selected icons on buttons!</b>
     *
     * @param icon The {@link Icon} which should be displayed when the button is selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnSelected(Icon icon) {
        Objects.requireNonNull(icon);
        return _with(c -> c.setSelectedIcon(_ensureIconIsScalable(icon)))._this();
    }

    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type when the button is selected.<br>
     *  Also see {@link #withIconOnSelected(int, int, IconDeclaration)}, which is
     *  <b>the preferred way of setting selected icons on buttons!</b>
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnSelected(int width, int height, Icon icon) {
        Objects.requireNonNull(icon);
        icon = _fitTo(width, height, icon);
        return withIconOnSelected(icon);
    }

    /**
     *  Takes the supplied {@link IconDeclaration} and scales it to the desired width and height
     *  before displaying it on the wrapped button type when selected.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link IconDeclaration} which should be scaled and
     *             then displayed when the button is selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnSelected(int width, int height, IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return icon.find()
                .map(i -> withIconOnSelected(width, height, i))
                .orElseGet(this::_this);
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales the corresponding icon it
     *  to the provided width and height before displaying it on the wrapped button type
     *  when selected by the user.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnSelected(int width, int height, IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnSelected(_fitTo(width, height, i), fitComponent))
                .orElseGet(this::_this);
    }

    /**
     *  Sets the selected {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is selected.<br>
     *  Please also see {@link #withIconOnSelected(IconDeclaration, UI.FitComponent)}, which is
     *  <b>the recommended way of setting selected icons on buttons!</b>
     *
     * @param icon The {@link SvgIcon} which should be displayed when the button is selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnSelected(Icon icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return _with(thisComponent -> {
            _installAutomaticIconApplier(thisComponent, icon, fitComponent, AbstractButton::setSelectedIcon);
        })
                ._this();
    }

    /**
     *  Sets the selected {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is selected.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The {@link IconDeclaration} which should be displayed when the button is selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnSelected(IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnSelected(i, fitComponent))
                .orElseGet(this::_this);
    }

    /**
     *  Use this to specify the icon for the wrapped button type,
     *  which ought to be displayed when the button is selected.
     *  The icon is resolved based on the supplied {@link IconDeclaration}
     *  instance which serves as a resource path to the icon actual.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The desired icon to be displayed on top of the button for when it is selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnSelected(IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return _with(c -> icon.find().map(UIForAnyButton::_ensureIconIsScalable).ifPresent(c::setSelectedIcon))._this();
    }

    /**
     *  Use this to dynamically set the "selected icon" property for the wrapped button type,
     *  which is displayed when the button is selected.
     *  When the icon wrapped by the supplied {@link Val} property changes,
     *  then so does the selected icon of this button.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.find().ifPresent(button::setSelectedIcon)
     *     ));
     *  }</pre>
     *  But in addition to simply setting the selected icon on the component, this method
     *  will also try to load and install the icon as a scalable {@link ImageIcon}, which means that
     *  in case of the referenced icon being an SVG file, the icon will be loaded as
     *  a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *  <p>
     *  But note that here you cannot use the {@link Icon} or {@link ImageIcon} classes directly,
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
     *  This is especially useful when writing unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed when the button is selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnSelected(Val<IconDeclaration> icon) {
        Objects.requireNonNull(icon);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow(icon, UIForAnyButton::_setIconOnSelectedFromDeclaration)
                ._with(c -> _setIconOnSelectedFromDeclaration(c, icon.orElseThrowUnchecked()))
                ._this();
    }

    // Icon on Disabled:

        /**
     *  Use this to set the "disabled" icon for the wrapped button type.
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setDisabledIcon(...) );
     *  }</pre>
     *  But in addition to simply setting the disabled icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *  Please also see {@link #withIconOnDisabled(IconDeclaration)}, which is
     *  <b>the recommended way of setting disabled icons on buttons!</b>
     *
     * @param icon The {@link Icon} which should be displayed when the button is disabled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnDisabled(Icon icon) {
        Objects.requireNonNull(icon);
        return _with(c -> c.setDisabledIcon(_ensureIconIsScalable(icon)))._this();
    }

    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type when the button is disabled.<br>
     *  Also see {@link #withIconOnDisabled(int, int, IconDeclaration)}, which is
     *  <b>the preferred way of setting disabled icons on buttons!</b>
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is disabled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnDisabled(int width, int height, Icon icon) {
        Objects.requireNonNull(icon);
        icon = _fitTo(width, height, icon);
        return withIconOnDisabled(icon);
    }

    /**
     *  Takes the supplied {@link IconDeclaration} and scales it to the desired width and height
     *  before displaying it on the wrapped button type when disabled.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link IconDeclaration} which should be scaled and
     *             then displayed when the button is disabled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnDisabled(int width, int height, IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return icon.find()
                .map(i -> withIconOnDisabled(width, height, i))
                .orElseGet(this::_this);
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales the corresponding icon it
     *  to the provided width and height before displaying it on the wrapped button type
     *  when disabled by the user.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is disabled.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnDisabled(int width, int height, IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnDisabled(_fitTo(width, height, i), fitComponent))
                .orElseGet(this::_this);
    }

    /**
     *  Sets the disabled {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is disabled.<br>
     *  Please also see {@link #withIconOnDisabled(IconDeclaration, UI.FitComponent)}, which is
     *  <b>the recommended way of setting disabled icons on buttons!</b>
     *
     * @param icon The {@link SvgIcon} which should be displayed when the button is disabled.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnDisabled(Icon icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return _with(thisComponent -> {
                    _installAutomaticIconApplier(thisComponent, icon, fitComponent, AbstractButton::setDisabledIcon);
                })
                ._this();
    }

    /**
     *  Sets the disabled {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is disabled.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The {@link IconDeclaration} which should be displayed when the button is disabled.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnDisabled(IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnDisabled(i, fitComponent))
                .orElseGet(this::_this);
    }

    /**
     *  Use this to specify the icon for the wrapped button type,
     *  which ought to be displayed when the button is disabled.
     *  The icon is resolved based on the supplied {@link IconDeclaration}
     *  instance which serves as a resource path to the icon actual.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The desired icon to be displayed on top of the button for when it is disabled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnDisabled(IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return _with(c -> icon.find().map(UIForAnyButton::_ensureIconIsScalable).ifPresent(c::setDisabledIcon))._this();
    }

    /**
     *  Use this to dynamically set the "disabled icon" property for the wrapped button type,
     *  which is displayed when the button is disabled.
     *  When the icon wrapped by the supplied {@link Val} property changes,
     *  then so does the disabled icon of this button.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.find().ifPresent(button::setDisabledIcon)
     *     ));
     *  }</pre>
     *  But in addition to simply setting the disabled icon on the component, this method
     *  will also try to load and install the icon as a scalable {@link ImageIcon}, which means that
     *  in case of the referenced icon being an SVG file, the icon will be loaded as
     *  a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *  <p>
     *  But note that here you cannot use the {@link Icon} or {@link ImageIcon} classes directly,
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
     *  This is especially useful when writing unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed when the button is disabled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnDisabled(Val<IconDeclaration> icon) {
        Objects.requireNonNull(icon);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow(icon, UIForAnyButton::_setIconOnDisabledFromDeclaration)
                ._with(c -> _setIconOnDisabledFromDeclaration(c, icon.orElseThrowUnchecked()))
                ._this();
    }

    // Icon on disabled AND selected

    /**
     *  Use this to set the "disabled and selected" icon for the wrapped button type.
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setDisabledSelectedIcon(...) );
     *  }</pre>
     *  But in addition to simply setting the disabled and selected icon on the component, this method
     *  will also try to convert the icon to an icon variant which scales according to
     *  the current {@link UI#scale()} factor (see {@link ScalableImageIcon}) so
     *  that the icon is upscaled proportionally in high-dpi environments.
     *  Please also see {@link #withIconOnDisabledAndSelected(IconDeclaration)}, which is
     *  <b>the recommended way of setting disabled and selected icons on buttons!</b>
     *
     * @param icon The {@link Icon} which should be displayed when the button is both disabled and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnDisabledAndSelected(Icon icon) {
        Objects.requireNonNull(icon);
        return _with(c -> c.setDisabledSelectedIcon(_ensureIconIsScalable(icon)))._this();
    }

    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type when the button is both disabled and selected.<br>
     *  Also see {@link #withIconOnDisabledAndSelected(int, int, IconDeclaration)}, which is
     *  <b>the preferred way of setting disabled and selected icons on buttons!</b>
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is both disabled and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnDisabledAndSelected(int width, int height, Icon icon) {
        Objects.requireNonNull(icon);
        icon = _fitTo(width, height, icon);
        return withIconOnDisabledAndSelected(icon);
    }

    /**
     *  Takes the supplied {@link IconDeclaration} and scales it to the desired width and height
     *  before displaying it on the wrapped button type when both disabled and selected.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()}.
     * @param icon The {@link IconDeclaration} which should be scaled and
     *             then displayed when the button is both disabled and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnDisabledAndSelected(int width, int height, IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return icon.find()
                .map(i -> withIconOnDisabledAndSelected(width, height, i))
                .orElseGet(this::_this);
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales the corresponding icon it
     *  to the provided width and height before displaying it on the wrapped button type
     *  when both disabled and selected by the user.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param width The desired width of the icon in "developer pixels",
     *              the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param height The desired height of the icon in "developer pixels",
     *               the actual width may be influenced by {@link UI#scale()} and {@link UI.FitComponent}.
     * @param icon The {@link Icon} which should be scaled and then displayed when the button is both disabled and selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnDisabledAndSelected(int width, int height, IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnDisabledAndSelected(_fitTo(width, height, i), fitComponent))
                .orElseGet(this::_this);
    }

    /**
     *  Sets the disabled and selected {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is both disabled and selected.<br>
     *  Please also see {@link #withIconOnDisabledAndSelected(IconDeclaration, UI.FitComponent)}, which is
     *  <b>the recommended way of setting disabled and selected icons on buttons!</b>
     *
     * @param icon The {@link SvgIcon} which should be displayed when the button is both disabled and selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnDisabledAndSelected(Icon icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return _with(thisComponent -> {
                    _installAutomaticIconApplier(thisComponent, icon, fitComponent, AbstractButton::setDisabledSelectedIcon);
                })
                ._this();
    }

    /**
     *  Sets the disabled and selected {@link Icon} property of the wrapped button type and scales it
     *  according to the provided {@link UI.FitComponent} policy.
     *  This icon is only displayed when the button is both disabled and selected.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The {@link IconDeclaration} which should be displayed when the button is both disabled and selected.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if any of the supplied arguments are {@code null}!
     */
    public final I withIconOnDisabledAndSelected(IconDeclaration icon, UI.FitComponent fitComponent) {
        Objects.requireNonNull(icon);
        Objects.requireNonNull(fitComponent);
        return icon.find()
                .map(i -> withIconOnDisabledAndSelected(i, fitComponent))
                .orElseGet(this::_this);
    }

    /**
     *  Use this to specify the icon for the wrapped button type,
     *  which ought to be displayed when the button is both disabled and selected.
     *  The icon is resolved based on the supplied {@link IconDeclaration}
     *  instance which serves as a resource path to the icon actual.<br>
     *  This method will try to load and install the icon as a scalable {@link ImageIcon},
     *  which means that in case of the referenced icon being an SVG file, the icon will be
     *  loaded as a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *
     * @param icon The desired icon to be displayed on top of the button for when it is both disabled and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}!
     */
    public final I withIconOnDisabledAndSelected(IconDeclaration icon) {
        Objects.requireNonNull(icon);
        return _with(c -> icon.find().map(UIForAnyButton::_ensureIconIsScalable).ifPresent(c::setDisabledSelectedIcon))._this();
    }

    /**
     *  Use this to dynamically set the "disabled and selected icon" property for the wrapped button type,
     *  which is displayed when the button is both disabled and selected.
     *  When the icon wrapped by the supplied {@link Val} property changes,
     *  then so does the disabled and selected icon of this button.<br>
     *  <p>
     *  For most scenarios, this is a convenience method equivalent to
     *  peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> iconProperty.onChange(From.ALL,
     *          icon -> icon.find().ifPresent(button::setDisabledSelectedIcon)
     *     ));
     *  }</pre>
     *  But in addition to simply setting the disabled and selected icon on the component, this method
     *  will also try to load and install the icon as a scalable {@link ImageIcon}, which means that
     *  in case of the referenced icon being an SVG file, the icon will be loaded as
     *  a smoothly scalable {@link SvgIcon}, if it is a png or jpeg file however,
     *  then this method will represent it as a {@link ScalableImageIcon}, which
     *  dynamically scales the underlying image according to the {@link UI#scale()}
     *  so that it has a proportionally correct size in high-dpi environments.
     *  <p>
     *  But note that here you cannot use the {@link Icon} or {@link ImageIcon} classes directly,
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
     *  This is especially useful when writing unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed when the button is both disabled and selected.
     * @return This very builder to allow for method chaining.
     * @throws NullPointerException if {@code icon} is {@code null}.
     */
    public final I withIconOnDisabledAndSelected(Val<IconDeclaration> icon) {
        Objects.requireNonNull(icon);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow(icon, UIForAnyButton::_setIconOnDisabledAndSelectedFromDeclaration)
                ._with(c -> _setIconOnDisabledAndSelectedFromDeclaration(c, icon.orElseThrowUnchecked()))
                ._this();
    }

    /**
     *  Sets the gap between icon and text for the wrapped button type,
     *  if both the icon and text properties are set.
     *  The gap is specified in "developer pixels" where the look and feel is expected
     *  to automatically scale the gap according to the current {@link UI#scale()} factor.
     *  <p>
     *  The default value of this property is 4 pixels.
     *
     * @param gap The desired gap between icon and text in "developer pixels".
     * @return This very builder to allow for method chaining.
     * @see AbstractButton#setIconTextGap(int)
     */
    public final I withIconTextGap( int gap ) {
        return _with(c -> {
            c.setIconTextGap(gap);
        })._this();
    }

    /**
     *  Binds an integer property to the gap between icon and text for the wrapped button type,
     *  if both the icon and text properties are set.
     *  The gap is specified in "developer pixels" where the look and feel is expected
     *  to automatically scale the gap according to the current {@link UI#scale()} factor.
     *  <p>
     *  The default value of this property is 4 pixels.
     *
     * @param gap The integer property which determines the gap between icon and text in "developer pixels".
     * @return This very builder to allow for method chaining.
     * @see AbstractButton#setIconTextGap(int)
     * @throws NullPointerException if {@code gap} is {@code null}!
     * @throws IllegalArgumentException if {@code gap} is a {@code Val} property
     *                                  that permits {@code null} values.
     */
    public final I withIconTextGap( Val<Integer> gap ) {
        Objects.requireNonNull(gap);
        NullUtil.nullPropertyCheck(gap,"gap", "The gap between a button icon and text may not be modelled using null!");
        return _withOnShow( gap, AbstractButton::setIconTextGap)
                ._with( c -> c.setIconTextGap(gap.get()))
                ._this();
    }

    private static void _installAutomaticIconApplier(
        AbstractButton thisComponent,
        Icon icon,
        UI.FitComponent fitComponent,
        BiConsumer<AbstractButton, Icon> iconApplier
    ) {
        if ( icon instanceof SvgIcon) {
            SvgIcon svgIcon = (SvgIcon) icon;
            iconApplier.accept(thisComponent, svgIcon.withFitComponent(fitComponent));
            return; // An SvgIcon already scales dynamically!
        }
        Runnable dynamicSizer = _createDynamicIconApplier(thisComponent, icon, fitComponent, iconApplier);
        dynamicSizer.run(); // Initial sizing
        UI.runLater(dynamicSizer); // Run later to ensure correct sizing after layout
        // TODO: make ScalableImageIcon use FitComponent to automatically resize itself
    }

    private static Runnable _createDynamicIconApplier(
        AbstractButton thisComponent,
        Icon icon,
        UI.FitComponent fitComponent,
        BiConsumer<AbstractButton, Icon> iconApplier
    ) {
        AtomicReference<Size> lastSize = new AtomicReference<>(Size.of(-1,-1));
        return ()->{
            Size size = _determineIconSize(thisComponent, icon, fitComponent);
            if ( !Objects.equals(size,lastSize.get()) ) {
                lastSize.set(size);
                int width = size.width().map(Number::intValue).orElse(-1);
                int height = size.height().map(Number::intValue).orElse(-1);
                if ( width > 0 && height > 0 )
                    iconApplier.accept(thisComponent, _fitTo( size, icon ));
            }
        };
    }

    private static Size _determineIconSize(JComponent thisComponent, Icon icon, UI.FitComponent fitComponent) {
        float width ;
        float height;
        Insets insets = thisComponent.getInsets();
        float fittedWidth = Math.max(thisComponent.getWidth(),  thisComponent.getMinimumSize().width);
        float fittedHeight = Math.max(thisComponent.getHeight(), thisComponent.getMinimumSize().height);
        fittedWidth  = Math.max(0, UI.unscale((float) fittedWidth - insets.left - insets.right)); // We unscale because the icon will be scaled internally
        fittedHeight = Math.max(0, UI.unscale((float) fittedHeight - insets.top  - insets.bottom));
        int iconWidth  = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();
        // We need to determine and return a base size to be scaled later on...
        // But some icon types already do the scaling internally, so we need to check for that:
        if ( icon instanceof ScalableImageIcon ) {
            ScalableImageIcon scalableIcon = (ScalableImageIcon) icon;
            if ( scalableIcon.getBaseWidth() > 0 )
                iconWidth = scalableIcon.getBaseWidth();
            if ( scalableIcon.getBaseHeight() > 0 )
                iconHeight = scalableIcon.getBaseHeight();
        } else if ( icon instanceof SvgIcon ) {
            SvgIcon svgIcon = (SvgIcon) icon;
            if ( svgIcon.getBaseWidth() > 0 )
                iconWidth = svgIcon.getBaseWidth();
            if ( svgIcon.getBaseHeight() > 0 )
                iconHeight = svgIcon.getBaseHeight();
        }
        if ( fitComponent == UI.FitComponent.NO ) {
            width  = iconWidth;
            height = iconHeight;
        } else if ( fitComponent == UI.FitComponent.WIDTH_AND_HEIGHT) {
            width  = fittedWidth;
            height = fittedHeight;
        } else if ( fitComponent == UI.FitComponent.WIDTH ) {
            width  = fittedWidth;
            height = iconHeight;
        } else if ( fitComponent == UI.FitComponent.HEIGHT ) {
            width = iconWidth;
            height = fittedHeight;
        } else if ( fitComponent == UI.FitComponent.MAX_DIM ) {
            width  = Math.max(fittedWidth, fittedHeight);
            height = Math.max(fittedWidth, fittedHeight);
        } else if ( fitComponent == UI.FitComponent.MIN_DIM ) {
            width  = Math.min(fittedWidth, fittedHeight);
            height = Math.min(fittedWidth, fittedHeight);
        } else {
            log.error(SwingTree.get().logMarker(), "Unknown 'UI.FitComponent' value: '{}'. Using 'NO' instead.", fitComponent);
            width  = icon.getIconWidth();
            height = icon.getIconHeight();
        }
        return Size.of(width, height);
    }

    private static Icon _fitTo(int width, int height, Icon icon) {
        return _fitTo(Size.of(width, height), icon);
    }

    private static Icon _fitTo(Size size, Icon icon) {
        float width = size.width().orElse(0f);
        float height = size.height().orElse(0f);
        if ( icon instanceof SvgIcon ) {
            SvgIcon svgIcon = (SvgIcon) icon;
            svgIcon = svgIcon.withIconWidth((int) width);
            return svgIcon.withIconHeight((int) height);
        }
        else if ( icon instanceof ScalableImageIcon ) {
            return ((ScalableImageIcon)icon).withSize(Size.of(width, height));
        } else if ( icon instanceof ImageIcon ) {
            return ScalableImageIcon.of(Size.of(width, height), (ImageIcon) icon);
        }
        return icon;
    }

    private static Icon _ensureIconIsScalable(Icon icon) {
        if ( icon instanceof ScalableImageIcon ) {
            return icon; // Already scalable
        }
        if ( icon instanceof SvgIcon ) {
            return icon; // Already scalable
        }
        if ( icon instanceof ImageIcon ) {
            return ScalableImageIcon.of(Size.of(icon.getIconWidth(), icon.getIconHeight()), (ImageIcon) icon);
        }
        return icon; // Not yet supported
    }

    private static void _setIconFromDeclaration( AbstractButton button, IconDeclaration icon ) {
        _setAnyIconFromDeclaration(button, icon, AbstractButton::setIcon);
    }

    private static void _setIconOnPressFromDeclaration( AbstractButton button, IconDeclaration icon ) {
        _setAnyIconFromDeclaration(button, icon, AbstractButton::setPressedIcon);
    }

    private static void _setIconOnHoverFromDeclaration( AbstractButton button, IconDeclaration icon ) {
        _setAnyIconFromDeclaration(button, icon, AbstractButton::setRolloverIcon);
    }

    private static void _setIconOnHoverAndSelectedFromDeclaration(AbstractButton button, IconDeclaration icon) {
        _setAnyIconFromDeclaration(button, icon, AbstractButton::setRolloverSelectedIcon);
    }

    private static void _setIconOnSelectedFromDeclaration(AbstractButton button, IconDeclaration icon) {
        _setAnyIconFromDeclaration(button, icon, AbstractButton::setSelectedIcon);
    }

    private static void _setIconOnDisabledFromDeclaration(AbstractButton button, IconDeclaration icon) {
        _setAnyIconFromDeclaration(button, icon, AbstractButton::setDisabledIcon);
    }

    private static void _setIconOnDisabledAndSelectedFromDeclaration(AbstractButton button, IconDeclaration icon) {
        _setAnyIconFromDeclaration(button, icon, AbstractButton::setDisabledSelectedIcon);
    }


    private static void _setAnyIconFromDeclaration(
        AbstractButton button,
        IconDeclaration icon,
        BiConsumer<AbstractButton, @Nullable Icon> iconSetter
    ) {
        Optional<ImageIcon> optIcon = icon.find();
        if ( optIcon.isPresent() )
            iconSetter.accept(button, optIcon.get());
        else {
            log.warn(SwingTree.get().logMarker(),
                    "Failed to load from 'IconDeclaration' instance '{}', " +
                    "with source '{}', format {}, and size '{}', and set it as the icon of 'AbstractButton' '{}'.",
                    icon, icon.source(), icon.sourceFormat(), icon.size(), button,
                    new Throwable("Stack trace for debugging purposes.")
                );
            iconSetter.accept(button, null);
        }
    }

    /**
     *  Use this to set the selection state of the wrapped button type.
     *
     * @param isSelected The selection state of the wrapped button type, which translates to {@link AbstractButton#setSelected(boolean)}.
     * @return This builder instance, to allow for method chaining.
     */
    public final I isSelectedIf( boolean isSelected ) {
        return _with( button -> _setSelectedSilently(button, isSelected) )._this();
    }

    void _setSelectedSilently( B thisButton, boolean isSelected ) {
        /*
            This is used to change the selection state of the button without triggering
            any action listeners. We need this because we want to construct the
            GUI and the state of its properties without side effects.
         */
        ItemListener[] listeners = thisButton.getItemListeners();
        for ( ItemListener l : listeners )
            thisButton.removeItemListener(l);

        thisButton.setSelected(isSelected);

        for ( ItemListener l : listeners )
            thisButton.addItemListener(l);

    }

    /**
     *  Use this to bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to model the selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIf( Val<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Val.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow(selected, this::_setSelectedSilently)
               ._with( button -> _setSelectedSilently(button, selected.get()) )._this();
    }

    /**
     *  Use this to bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the inverse selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to
     *                 model the inverse selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIfNot( Val<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Val.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow( selected, (c, v) -> _setSelectedSilently(c, !v) )
               ._with( button -> _setSelectedSilently(button, !selected.is(true)) )._this();
    }

    /**
     *  Use this to bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to model the selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIf( Var<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow( selected, this::_setSelectedSilently )
                ._with( button -> {
                    _onClick(button,
                        e -> _runInApp(button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                    _onChange(button,
                        v -> _runInApp(button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                })
                ._with( button -> _setSelectedSilently(button, selected.get()) )
                ._this();
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the inverse selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to
     *                 model the inverse selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIfNot( Var<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow( selected, (c, v) -> {
                    _setSelectedSilently(c, !v);
                })
                ._with( button -> {
                    _onClick(button,
                        e -> _runInApp(!button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                    _onChange(button,
                        v -> _runInApp(!button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                })
                ._with( button -> _setSelectedSilently(button, !selected.is(true)) )
                ._this();
    }

    /**
     *  Use this to dynamically bind the selection flag of the button to a {@link Val}
     *  property which will determine the selection state of the button based on the
     *  equality of the property value and the provided reference value.
     *  So if the first {@code state} argument is equal to the value of the {@code selection} property,
     *  the button will be selected, otherwise it will be deselected.<br>
     *  A typical use case is to bind a button to an enum property, like so: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Step { ONE, TWO, THREE }
     *      Var<Step> step = Var.of(Step.ONE);
     *
     *      // In your view:
     *      UI.radioButton("Two").isSelectedIf(Step.TWO, vm.getStep());
     *  }</pre>
     *  As you can see, the radio button will be selected if the enum property is equal to the supplied enum value
     *  and deselected otherwise. <br>
     *  <br>
     * <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param state The reference value which the button type should represent when selected.
     * @param selection The {@link sprouts.Val} instance which will be used
     *                  to dynamically model the selection state of the wrapped button type
     *                  based on the equality of the {@code state} argument and the value of the property.
     * @param <T> The type of the property value.
     * @return The current builder type, to allow for further method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final <T> I isSelectedIf( T state, Val<T> selection ) {
        NullUtil.nullArgCheck(state, "state", Object.class);
        NullUtil.nullArgCheck(selection, "selection", Val.class);
        return _withOnShow( selection, (c,v) -> _setSelectedSilently(c, state.equals(v)) )
                ._with( button ->
                     _setSelectedSilently(button, state.equals(selection.orElseThrowUnchecked()))
                )
                ._this();
    }

    /**
     *  This is the inverse of {@link #isSelectedIf(Object, Val)}.
     *  Use this to make the wrapped UI component dynamically deselected or selected,
     *  based on the equality between the supplied value and the property value. <br>
     *  <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i><br>
     *  A typical use case is to bind to an enum property, like so: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Step { ONE, TWO, THREE }
     *      Var<Step> step = Var.of(Step.ONE);
     *
     *      // In your view:
     *      UI.radioButton("Not Two")
     *      .isSelectedIfNot(Step.TWO, vm.getStep());
     *  }</pre>
     *  As you can see, the radio button will be selected if the enum property is equal to the supplied enum value
     *  and deselected otherwise. <br>
     *
     * @param state The value which, if equal to the supplied property value, makes the UI component deselected.
     * @param selection The enum property which, if equal to the supplied value, makes the UI component deselected.
     * @param <T> The type of the value.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T> I isSelectedIfNot( T state, Val<T> selection ) {
        NullUtil.nullArgCheck(state, "state", Object.class);
        NullUtil.nullArgCheck(selection, "selection", Val.class);
        return _withOnShow( selection, (c, v) -> _setSelectedSilently(c, !state.equals(v)) )
                ._with( button ->
                     _setSelectedSilently(button, !state.equals(selection.orElseThrowUnchecked()))
                )
                ._this();
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the pressed state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param var The {@link sprouts.Var} instance which will be used to model the pressed state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code var} is {@code null}.
     */
    public final I isPressedIf( Var<Boolean> var ) {
        NullUtil.nullArgCheck(var, "var", Var.class);
        return _withOnShow( var, (c,v) -> {
                    if ( v != c.getModel().isPressed() )
                        c.getModel().setPressed(v);
                })
                ._with( button -> {
                    _onModelChange(button, e ->
                        _runInApp(button.getModel().isPressed(), pressed->{
                            var.set(From.VIEW, pressed);
                        })
                    );
                })
                // on change is not needed because the pressed state is only changed by the user.
                ._with( button ->
                    button.setSelected(var.orElseThrowUnchecked())
                )
                ._this();
    }

    protected final void _onModelChange( B button, Consumer<ChangeEvent> action ) {
        /*
            When an item event is fired Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addItemListener" method, we will be the last to be notified.
            This is problematic because the first listeners we register are usually
            the ones that are responsible for updating properties.
            This means that when the item listener events of the user
            are fired, the properties will not be updated yet.
            To solve this problem, we do the revers by making sure that our listener is added
            at the first position in the list of listeners inside the button.
        */
        ItemListener[] listeners = button.getItemListeners();
        for (ItemListener listener : listeners)
            button.removeItemListener(listener);

        button.addChangeListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            button.addItemListener(listeners[i]);
        /*
            The reasoning behind why Swing calls item listeners from last to first is
            the assumption that the last listener added is more interested in the event
            than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
         */
    }


    /**
     *  Sets the {@link AbstractButton#setBorderPainted(boolean)} flag of the wrapped button type.
     *  If the flag is set to true, the border of the button will be painted.
     *  The default value for the borderPainted property is true.
     *  Some look and feels might not support the borderPainted property,
     *  in which case they ignore this.
     *
     * @param borderPainted Whether the border of the button should be painted.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I isBorderPaintedIf( boolean borderPainted ) {
        return _with( button -> button.setBorderPainted(borderPainted) )._this();
    }

    /**
     *  Binds the provided {@link Val} property to the {@link AbstractButton#setBorderPainted(boolean)} method.,
     *  which means that whenever the value of the property changes, the border of the button will be painted or not.
     *  The default value for the borderPainted property is true.
     *  Some look and feels might not support the borderPainted property, in which case they ignore this.
     *
     * @param val Whether the border of the button should be painted.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I isBorderPaintedIf( Val<Boolean> val ) {
        NullUtil.nullArgCheck(val, "val", Val.class);
        NullUtil.nullPropertyCheck(val, "val", "Null is not a valid value for the border painted property.");
        return _withOnShow( val, AbstractButton::setBorderPainted )
               ._with( button -> button.setBorderPainted(val.orElseThrowUnchecked()) )
               ._this();
    }

    /**
     *  Effectively removes the native style of this button.
     *  Without an icon or text, one will not be able to recognize the button.
     *  Use this for buttons with a custom icon or clickable text!
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I makePlain() {
        return peek( it -> {
                    it.setBorderPainted(false);
                    it.setContentAreaFilled(false);
                    it.setOpaque(false);
                    it.setFocusPainted(false);
                    it.setMargin(new Insets(0,0,0,0));
                    _setBackground(it, UI.Color.TRANSPARENT);
                });
    }

    /**
     *  This method adds the provided
     *  {@link ItemListener} instance to the wrapped button component.
     *  <br><br>
     *
     * @param action The change action lambda which will be passed to the button component.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public final I onChange( Action<ComponentDelegate<B, ItemEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( button -> {
                    _onChange(button, e ->
                        _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(button, e));
                            } catch ( Exception ex ) {
                                log.error(SwingTree.get().logMarker(), "Failed to execute action on button change event.", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    protected final void _onChange( B button, Consumer<ItemEvent> action ) {
        /*
            When an item event is fired Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addItemListener" method, we will be the last to be notified.
            This is problematic because the first listeners we register are usually
            the ones that are responsible for updating properties.
            This means that when the item listener events of the user
            are fired, the properties will not be updated yet.
            To solve this problem, we do the revers by making sure that our listener is added
            at the first position in the list of listeners inside the button.
        */
        ItemListener[] listeners = button.getItemListeners();
        for (ItemListener listener : listeners)
            button.removeItemListener(listener);

        button.addItemListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            button.addItemListener(listeners[i]);
        /*
            The reasoning behind why Swing calls item listeners from last to first is
            the assumption that the last listener added is more interested in the event
            than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
         */
    }

    /**
     *  This method enables a more readable way of adding
     *  {@link ActionListener} instances to button types.
     *  Additionally, to the other "onClick" method this method enables the involvement of the
     *  {@link JComponent} itself into the action supplied to it.
     *  This is very useful for changing the state of the JComponent when the action is being triggered.
     *  <br><br>
     *
     * @param action an {@link Action} instance which will receive an {@link ComponentDelegate} containing important context information.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public I onClick( Action<ComponentDelegate<B, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( button -> {
                    _onClick(button, e ->
                        _runInApp(() -> {
                            try {
                                action.accept(new ComponentDelegate<>(button, e));
                            } catch ( Exception ex ) {
                                log.error(SwingTree.get().logMarker(), "Failed to execute action on button click event.", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    protected final void _onClick( B button, Consumer<ActionEvent> action ) {
        /*
            When an action event is fired, Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addActionListener" method, we will be the last to be notified.
            This is problematic because it is built on the assumption that the last listener
            added is more interested in the event than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
        */
        ActionListener[] listeners = button.getActionListeners();
        for (ActionListener listener : listeners)
            button.removeActionListener(listener);

        button.addActionListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            button.addActionListener(listeners[i]);
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setHorizontalAlignment(...) );
     *  }</pre>
     * This sets the horizontal alignment of the icon and text.
     *
     * @param horizontalAlign The horizontal alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withHorizontalAlignment( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        return _with( c ->
                    horizontalAlign.forSwing().ifPresent(c::setHorizontalAlignment)
                )
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setHorizontalAlignment(v.forSwing()));
     *          button.setHorizontalAlignment(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the horizontal alignment of the icon and text
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param horizontalAlign The horizontal alignment property which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withHorizontalAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", Val.class);
        NullUtil.nullPropertyCheck(horizontalAlign, "horizontalAlign");
        return _withOnShow( horizontalAlign, (c, align) -> {
                    align.forSwing().ifPresent(c::setHorizontalAlignment);
                })
                ._with( c ->
                    horizontalAlign.orElseThrowUnchecked().forSwing().ifPresent(c::setHorizontalAlignment)
                )
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setVerticalAlignment(...) );
     *  }</pre>
     * This sets the vertical alignment of the icon and text.
     *
     * @param verticalAlign The vertical alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withVerticalAlignment( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        return _with( c ->
                    verticalAlign.forSwing().ifPresent(c::setVerticalAlignment)
                )
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setVerticalAlignment(v.forSwing()));
     *          button.setVerticalAlignment(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the vertical alignment of the icon and text
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param verticalAlign The vertical alignment property which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withVerticalAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", Val.class);
        NullUtil.nullPropertyCheck(verticalAlign, "verticalAlign");
        return _withOnShow( verticalAlign, (c, align) -> {
                    align.forSwing().ifPresent(c::setVerticalAlignment);
                })
                ._with( c ->
                    verticalAlign.orElseThrowUnchecked().forSwing().ifPresent(c::setVerticalAlignment)
                )
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setHorizontalTextPosition(...) );
     *  }</pre>
     * This sets the horizontal position of the text relative to the icon.
     *
     * @param horizontalAlign The horizontal text alignment relative to the icon which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withHorizontalTextAlignment( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        return _with( c ->
                    horizontalAlign.forSwing().ifPresent(c::setHorizontalTextPosition)
                )
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setHorizontalTextPosition(v.forSwing()));
     *          button.setHorizontalTextPosition(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the horizontal position of the text relative to the icon
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param horizontalAlign The horizontal text alignment property relative to the icon which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withHorizontalTextAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", Val.class);
        return _withOnShow( horizontalAlign, (c, align) -> {
                    align.forSwing().ifPresent(c::setHorizontalTextPosition);
                })
                ._with( c ->
                    horizontalAlign.orElseThrowUnchecked().forSwing().ifPresent(c::setHorizontalTextPosition)
                )
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setVerticalTextPosition(...) );
     *  }</pre>
     * This sets the vertical position of the text relative to the icon.
     *
     * @param verticalAlign The vertical text alignment relative to the icon which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withVerticalTextAlignment( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        return _with( c ->
                    verticalAlign.forSwing().ifPresent(c::setVerticalTextPosition)
                )
                ._this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setVerticalTextPosition(v.forSwing()));
     *          button.setVerticalTextPosition(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the vertical position of the text relative to the icon
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param verticalAlign The vertical text alignment property relative to the icon which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withVerticalTextAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", Val.class);
        return _withOnShow( verticalAlign, (c, align) -> {
                    align.forSwing().ifPresent(c::setVerticalTextPosition);
                })
                ._with( c ->
                    verticalAlign.orElseThrowUnchecked().forSwing().ifPresent(c::setVerticalTextPosition)
                )
                ._this();
    }

    /**
     *  Use this to attach this button type to a button group.
     *
     * @param group The button group to which this button should be attached.
     * @return This very builder to allow for method chaining.
     */
    public final I withButtonGroup( ButtonGroup group ) {
        NullUtil.nullArgCheck(group, "group", ButtonGroup.class);
        return _with(group::add)._this();
    }

    /**
     *  Use this to set the margin of the wrapped button type.
     *
     * @param insets The margin of the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withMargin( Insets insets ) {
        NullUtil.nullArgCheck(insets, "insets", Insets.class);
        return _with( thisComponent ->
                    thisComponent.setMargin(insets)
                )
                ._this();
    }

    /**
     *  Use this to set the margin of the wrapped button type.
     *
     * @param top The top margin of the button.
     * @param left The left margin of the button.
     * @param bottom The bottom margin of the button.
     * @param right The right margin of the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withMargin( int top, int left, int bottom, int right ) {
        return withMargin( new Insets(top, left, bottom, right) );
    }
}
package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Val;
import swingtree.animation.Animation;
import swingtree.animation.AnimationDispatcher;
import swingtree.animation.AnimationStatus;
import swingtree.animation.LifeTime;
import swingtree.api.AnimatedStyler;
import swingtree.api.Painter;
import swingtree.api.Styler;
import swingtree.layout.Bounds;
import swingtree.layout.Position;
import swingtree.layout.Size;
import swingtree.style.ComponentExtension;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  Extensions of this class delegate a component
 *  as well as provide useful methods for trying the tree of the components
 *  in which the delegated component is contained. <br>
 *  Instances of this class are passed to various user event {@link Action} handlers.
 *  You can use this to change the state of the component, schedule animations
 *  for the component or query the tree of the components.
 *
 * @param <C> The type of the component that is delegated.
 */
public class AbstractDelegate<C extends JComponent>
{
    private static final Logger log = LoggerFactory.getLogger(AbstractDelegate.class);
    private final GuiTraverser _guiTraverser; // the traverser object that allows us to query the component tree
    private final C _component;

    /**
     * @param component The component that is delegated.
     * @param handle A component that is used as a starting point for traversing the component tree,
     *               usually the same component as the one that is delegated.
     */
    AbstractDelegate( boolean nullable, C component, JComponent handle ) {
        _guiTraverser = new GuiTraverser(Objects.requireNonNull(handle));
        _component    = nullable ? component : Objects.requireNonNull(component);
    }

    /**
     *  Checks if the given font is the undefined font constant
     *  with respect to regular object identity instead of value equality.
     *  This is a deliberate design choice to allow the user to use the
     *  {@link UI.Font#UNDEFINED} constant instead of {@code null}
     *  as a placeholder for the absence of a font.
     * @param font The font to check.
     * @return {@code true} if the font is the undefined font constant, {@code false} otherwise.
     */
    @SuppressWarnings("ReferenceEquality")
    protected final boolean _isUndefinedFont( Font font ) {
        return font == UI.Font.UNDEFINED;
    }

    /**
     *  Checks if the given color is the undefined color constant
     *  with respect to regular object identity instead of value equality.
     *  This is a deliberate design choice to allow the user to use the
     *  {@link UI.Color#UNDEFINED} constant instead of {@code null}
     *  as a placeholder for the absence of a color.
     * @param color The color to check.
     * @return {@code true} if the color is the undefined color constant, {@code false} otherwise.
     */
    @SuppressWarnings("ReferenceEquality")
    protected final boolean _isUndefinedColor( Color color ) {
        return color == UI.Color.UNDEFINED;
    }


    protected final C _component() {
        return _component;
    }

    /**
     *  A library internal utility method that exposes the sibling components
     *  of the delegated component.
     * @return A list of sibling components.
     */
    protected final List<JComponent> _siblingsSource() {
        return Optional.ofNullable(_component.getParent())
                .map(Container::getComponents)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(c -> c instanceof JComponent)
                .map(c -> (JComponent) c)
                .collect(Collectors.toList());
    }

    /**
     *  This is a delegate to the underlying component, but not every method of the component
     *  is delegated. This method allows you to access the underlying component directly.
     *  <p>
     *  Note that this method expects that the accessing thread is the event dispatch thread,
     *  not the application thread.
     *  If you want to access the component from the application thread, you should use <br>
     *  {@code UI.run(() -> delegate.component())}.
     *
     * @return The underlying component.
     * @throws IllegalStateException If the accessing thread is not the event dispatch thread.
     */
    public final C get() {
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "You can only access the component from the GUI thread. " +
                    "Use 'UI.run(() -> delegate.component())' to access the component from the application thread."
                );
        return _component();
    }

    /**
     *  Allows you to access the x-coordinate of the delegated component relative to its parent,
     *  and <b>scaled to "developer pixel space" instead of "component pixel space".</b>
     *  This means that even if your component and its placement was upscaled
     *  for a particular high DPI environment for example, then you will still
     *  receive a consistent coordinate.
     *
     * @return The x-coordinate of the component relative to its parent
     *         and in "developer pixel space" (without DPI aware scaling applied).
     */
    public final int getX() { return UI.unscale(_component().getX()); }

    /**
     *  Allows you to access the y-coordinate of the delegated component relative to its parent,
     *  and <b>scaled to "developer pixel space" instead of "component pixel space".</b>
     *  This means that even if your component and its placement was upscaled
     *  for a particular high DPI environment for example, then you will still
     *  receive a consistent coordinate.
     *
     * @return The y-coordinate of the component relative to its parent
     *         and in "developer pixel space" (without DPI aware scaling applied).
     */
    public final int getY() { return UI.unscale(_component().getY()); }

    /**
     *  This method allows you to access the location of the delegated component r
     *  elative to its parent in "developer pixel space" instead of "component pixel space".
     *  It returns an immutable {@link Position} object holding both x and y components
     *  which are equal to the values available through {@link #getX()} and {@link #getY()}.
     *
     * @return The location of the component relative to its parent
     *         in "developer pixel space" (without DPI scaling applied).
     */
    public final Position getLocation() {
        return Position.of(getX(), getY());
    }

    /**
     *  This is a component delegate API, which means that it represents
     *  the API of a wrapped component. So this method allows you to access
     *  the parent of the underlying component.
     *  In essence, this is a delegate to {@link Component#getParent()}. <br>
     *
     * @return The parent {@link Container} of the underlying component.
     * @throws IllegalStateException If the accessing thread is not the event dispatch thread.
     */
    public final Container getParent() {
        if ( UI.thisIsUIThread() )
            return _component().getParent();
        else
            throw new IllegalStateException(
                    "You can only access the parent component from the GUI thread. " +
                    "Use 'UI.run(() -> delegate.getParent())' to access the parent component from the application thread."
                );
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the background color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setBackground(Color)} for more information.
     *  </p>
     *
     * @param color The color that should be used to paint the background of the component.
     *              If this parameter is <code>null</code> then this component will inherit
     *              the background color of its parent.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setBackground( Color color ) {
        Objects.requireNonNull(color, "Use UI.Color.UNDEFINED instead of null to represent the absence of a color.");
        if ( _isUndefinedColor(color) )
            _component().setBackground(null);
        else
            _component().setBackground(color);

        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the background color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setBackground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as a double between 0 and 1.
     * @param g The green component of the color as a double between 0 and 1.
     * @param b The blue component of the color as a double between 0 and 1.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setBackgroundColor( double r, double g, double b ) {
        return setBackgroundColor(r, g, b, 1.0);
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the background color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setBackground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as a double between 0 and 1.
     * @param g The green component of the color as a double between 0 and 1.
     * @param b The blue component of the color as a double between 0 and 1.
     * @param a The alpha component of the color as a double between 0 and 1.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setBackgroundColor( double r, double g, double b, double a ) {
        return setBackground(new Color((float) r, (float) g, (float) b, (float) a));
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the background color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setBackground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as an integer between 0 and 255.
     * @param g The green component of the color as an integer between 0 and 255.
     * @param b The blue component of the color as an integer between 0 and 255.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setBackgroundColor( int r, int g, int b ) {
        return setBackgroundColor(r, g, b, 255);
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the background color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setBackground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as an integer between 0 and 255.
     * @param g The green component of the color as an integer between 0 and 255.
     * @param b The blue component of the color as an integer between 0 and 255.
     * @param a The alpha component of the color as an integer between 0 and 255.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setBackgroundColor( int r, int g, int b, int a ) {
        return setBackground(new Color(r, g, b, a));
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the background color of the component.
     *  <p>
     *  See {@link Component#getBackground()} for more information.
     *  </p>
     *
     * @return The background color of the component, or {@link UI.Color#UNDEFINED} if the component
     *         does not have a background color (i.e. {@link Component#getBackground()} returns <code>null</code>).
     *         The return value will never be <code>null</code>.
     */
    public final Color getBackground() {
        Color backgroundColor = _component().getBackground();
        if ( backgroundColor == null )
            return UI.Color.UNDEFINED;
        else
            return backgroundColor;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the foreground color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setForeground(Color)} for more information.
     *  </p>
     *
     * @param color The color that should be used to paint the foreground of the component.
     *              If this parameter is <code>null</code> then this component will inherit
     *              the foreground color of its parent.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setForeground( Color color ) {
        Objects.requireNonNull(color, "Use UI.Color.UNDEFINED instead of null to represent the absence of a color.");
        if ( _isUndefinedColor(color) )
            _component().setForeground( null );
        else
            _component().setForeground( color );
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the foreground color of the component.
     *  <p>
     *  See {@link Component#getForeground()} for more information.
     *  </p>
     *
     * @return The foreground color of the component, or {@link UI.Color#UNDEFINED} if the component
     *        does not have a foreground color (i.e. {@link Component#getForeground()} returns <code>null</code>).
     *        The return value will never be <code>null</code>.
     */
    public final Color getForeground() {
        Color foregroundColor = _component().getForeground();
        if ( foregroundColor == null )
            return UI.Color.UNDEFINED;
        else
            return foregroundColor;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the foreground color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setForeground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as a double between 0 and 1.
     * @param g The green component of the color as a double between 0 and 1.
     * @param b The blue component of the color as a double between 0 and 1.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setForegroundColor( double r, double g, double b ) {
        return setForegroundColor(r, g, b, 1.0);
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the foreground color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setForeground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as a double between 0 and 1.
     * @param g The green component of the color as a double between 0 and 1.
     * @param b The blue component of the color as a double between 0 and 1.
     * @param a The alpha component of the color as a double between 0 and 1.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setForegroundColor( double r, double g, double b, double a ) {
        return setForeground(new Color((float) r, (float) g, (float) b, (float) a));
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the foreground color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setForeground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as an integer between 0 and 255.
     * @param g The green component of the color as an integer between 0 and 255.
     * @param b The blue component of the color as an integer between 0 and 255.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setForegroundColor( int r, int g, int b ) {
        return setForegroundColor(r, g, b, 255);
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the foreground color of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setForeground(Color)} for more information.
     *  </p>
     *
     * @param r The red component of the color as an integer between 0 and 255.
     * @param g The green component of the color as an integer between 0 and 255.
     * @param b The blue component of the color as an integer between 0 and 255.
     * @param a The alpha component of the color as an integer between 0 and 255.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setForegroundColor( int r, int g, int b, int a ) {
        return setForeground(new Color(r, g, b, a));
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the font of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setFont(Font)} for more information.
     *  </p>
     *
     * @param font The font that should be used to paint the text of the component.
     *             If this parameter is {@link UI.Font#UNDEFINED} then this component will inherit
     *             the font of its parent. Null is not allowed.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setFont( Font font ) {
        Objects.requireNonNull(font, "Use UI.Font.UNDEFINED instead of null to represent the absence of a font.");
        if ( _isUndefinedFont(font) )
            _component().setFont(null);
        else
            _component().setFont(font);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the font of the component.
     *  <p>
     *  See {@link Component#getFont()} for more information.
     *  </p>
     *
     * @return The font of the component.
     */
    public final Font getFont() {
        Font font = _component().getFont();
        if ( font == null )
            return UI.Font.UNDEFINED;
        else
            return font;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the border of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  Note that this method is a delegate to {@link JComponent#setBorder(Border)}.
     *  </p>
     *
     * @param border The border that should be used to paint the border of the component.
     *               If this parameter is <code>null</code> then this component will inherit
     *               the border of its parent.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setBorder( Border border ) {
        _component().setBorder(border);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the border of the component.
     *  <p>
     *  Note that this method is a delegate to {@link JComponent#getBorder()}.
     *  </p>
     *
     * @return The border of the component.
     */
    public final Border getBorder() {
        return _component().getBorder();
    }

    /**
     *  Allows you to specify new bounds for the delegated component in "developer pixel space"
     *  and then have them scaled and set as "component pixel size", which may be scaled for hgigh
     *  DPI environments by the {@link UI#scale()} factor.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setBounds(int, int, int, int)} for more information.
     *  </p>
     *
     *  @param x The x coordinate of the new location of the component.
     *           This is relative to the component's parent.
     *  @param y The y coordinate of the new location of the component.
     *           This is relative to the component's parent.
     *  @param width The new width of the component.
     *  @param height The new height of the component.
     *  @return The delegate itself, so you can chain calls to this method.
     */
    public final AbstractDelegate<C> setBounds( int x, int y, int width, int height ) {
        _component().setBounds(UI.scale(x), UI.scale(y), UI.scale(width), UI.scale(height));
        return this;
    }

    /**
     *  Delegates to the {@link JComponent#setBounds(int, int, int, int)} method
     *  of the underlying component. The bounds consist of a location and a size
     *  which are relative to the component's parent.
     *  <b>Important:</b> The supplied bounds are interpreted as "being in developer pixel space"
     *  without any scaling factor applied to it. And so this method will scale the bounds
     *  to DPI aware "component pixel space" by dividing by {@link UI#scale()}.
     *
     * @param bounds The new bounds of the component.
     *                This is relative to the component's parent.
     * @return The delegate itself, so you can chain calls to this method.
     */
    public final AbstractDelegate<C> setBounds( Bounds bounds ) {
        return setBounds(
                UI.scale((int) bounds.location().x()),
                UI.scale((int) bounds.location().y()),
                UI.scale(bounds.size().width().map(Number::intValue).orElse(0)),
                UI.scale(bounds.size().height().map(Number::intValue).orElse(0))
            );
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the bounds of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link Component#setBounds(Rectangle)} for more information.
     *  </p>
     *
     *  @param bounds The new bounds of the component.
     *                  This is relative to the component's parent.
     * @return The delegate itself, so you can chain calls to this method.
     */
    public final AbstractDelegate<C> setBounds( Rectangle bounds ) {
        _component().setBounds(bounds);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the bounds of the component in the form of an immutable {@link Bounds} value object.
     *  The bounds consist of a location and a size which are relative to the component's parent.
     *  <p>
     *  See {@link Component#getBounds()} for more information.
     *  </p>
     *
     *  @return The bounds of the component.
     *          This is relative to the component's parent.
     */
    public final Bounds getBounds() {
        return Bounds.of(_component().getBounds());
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  access a specific {@link UI.ComponentArea} of the component.
     *  This method returns an {@link Optional} value, which means that the
     *  component area may not be present.
     *  The {@link swingtree.UI.ComponentArea#BORDER} for example, may not be present
     *  in case of there not being a border width defined through {@link UIForAnySwing#withStyle(Styler)}.
     *  <p>
     *  See {@link ComponentExtension#getComponentArea(UI.ComponentArea)} for more information.
     *  </p>
     *
     * @param area The component area to access.
     * @return An optional value that contains the component area if it is present.
     */
    public Optional<Shape> shapeOf( UI.ComponentArea area ) {
        return ComponentExtension.from(_component()).getComponentArea(area);
    }

    /**
     *  Allows you to set the preferred size of the component in DPI agnostic "developer pixels",
     *  which are automatically converted to DPI aware "component pixels" using {@link UI#scale()}.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  @param size The preferred size of the component.
     *  @return The delegate itself.
     *  @deprecated Use {@link #setPrefSize(Size)} instead of this method.
     */
    @Deprecated
    public final AbstractDelegate<C> setPrefSize( Dimension size ) {
        _scaleAndSetPrefSize(size);
        return this;
    }

    /**
     *  Allows you to set the preferred size of the component in DPI agnostic "developer pixels",
     *  which are automatically converted to DPI aware "component pixels" using {@link UI#scale()}.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  Also note that the supplied size is treated as being measured in "developer pixel size", 
     *  which are defined as pixels <b>without</b> the UI scaling factor applied to them.<br>
     *  This method will scale the supplied size automatically for you, and therefore you as a developer
     *  may define dimensions in your GUI code consistently, without sacrificing dynamic DPI scaling.
     *  
     *  @param size The preferred size of the component in developer pixel size.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefSize( Size size ) {
        _scaleAndSetPrefSize(size.toDimension());
        return this;
    }

    /**
     *  Allows you to set the preferred width and height of the component in DPI agnostic "developer pixels",
     *  which are automatically converted to DPI aware "component pixels" using {@link UI#scale()}.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  Also note that the supplied size is treated as being measured in "developer pixel size", 
     *  which are defined as pixels <b>without</b> the UI scaling factor applied to them.<br>
     *  This method will scale the supplied width and height automatically for you, and therefore you as a developer
     *  may define dimensions in your GUI code consistently, without sacrificing dynamic DPI scaling.
     *  
     *  @param width The preferred width of the component in developer pixels (not scaled for high DPI).
     *  @param height The preferred height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefSize( int width, int height ) {
        _scaleAndSetPrefSize(new Dimension(width, height));
        return this;
    }

    private void _scaleAndSetPrefSize( Dimension size ) {
        _component().setPreferredSize(UI.scale(size));
    }

    /**
     *  Allows you to set the preferred width of the component in DPI agnostic "developer pixels",
     *  which are automatically converted to DPI aware "component pixels" using {@link UI#scale()}.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  @param width The preferred width of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefWidth( int width ) {
        Dimension size = _component().getPreferredSize();
        _component().setPreferredSize(new Dimension(UI.scale(width), size.height));
        return this;
    }

    /**
     *  Allows you to set the preferred height of the component in DPI agnostic "developer pixels",
     *  which are automatically converted to DPI aware "component pixels" using {@link UI#scale()}.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  @param height The preferred height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefHeight( int height ) {
        Dimension size = _component().getPreferredSize();
        _component().setPreferredSize(new Dimension(size.width, UI.scale(height)));
        return this;
    }

    /**
     *  Exposes the preferred size of the component in DPI agnostic "developer pixels" which are
     *  computed by dividing the already scaled preferred component width and height 
     *  in "component pixels" by the {@link UI#scale()} factor.<br>
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getPreferredSize()} for more information.
     *  </p>
     *  @return The preferred size of the component scaled to "developer pixel size".
     */
    public final Size getPrefSize() {
        return Size.of(UI.unscale(_component().getPreferredSize()));
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the minimum size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMinimumSize(Dimension)} for more information.
     *  </p>
     *  @param size The minimum size of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     *  @deprecated Use {@link #setMinSize(Size)} instead!
     */
    @Deprecated
    public final AbstractDelegate<C> setMinSize( Dimension size ) {
        _component().setMinimumSize(size == null ? null : UI.scale(size));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the minimum size of the component <b>in developer pixel size</b>.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMinimumSize(Dimension)} for more information 
     *  about the meaning of this property.
     *  </p>
     *  Also note that the supplied size is treated as being measured in "developer pixel size", 
     *  which are defined as pixels <b>without</b> the UI scaling factor applied to them.<br>
     *  This method will scale the supplied size automatically for you, and therefore you as a developer
     *  may define dimensions in your GUI code consistently, without sacrificing dynamic DPI scaling.
     *  
     *  @param size The minimum size of the component in developer pixel size,
     *              or {@link Size#unknown()} to set the minimum size to being "undefined"/"null".
     *  @return The delegate itself.
     *  @throws NullPointerException if the supplied size is null.
     */
    public final AbstractDelegate<C> setMinSize( Size size ) {
        Objects.requireNonNull(size, "Use Size.unknown() instead of null to represent the absence of a size.");
        if ( size.equals(Size.unknown()) )
            _component().setMinimumSize(null);
        else
            _component().setMinimumSize(UI.scale(size.toDimension()));
        return this;
    }

    /**
     *  This method allows you to set the minimum size of the delegated component
     *  in "developer pixels" instead of "component pixels".
     *  The dimension you supply to this method will be scale to "component pixels" for you, so you
     *  don't have to rely on component pixel space in your code (which varies depending on the {@link UI#scale()}).
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMinimumSize(Dimension)} for more information.
     *  </p>
     *  @param width The minimum width of the component in developer pixels (not scaled for high DPI).
     *  @param height The minimum height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMinSize( int width, int height ) {
        _component().setMinimumSize(new Dimension(UI.scale(width), UI.scale(height)));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the minimum width of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMinimumSize(Dimension)} for more information.
     *  </p>
     *  @param width The minimum width of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMinWidth( int width ) {
        Dimension size = _component().getMinimumSize();
        _component().setMinimumSize(new Dimension(UI.scale(width), size.height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the minimum height of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMinimumSize(Dimension)} for more information.
     *  </p>
     *  @param height The minimum height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMinHeight( int height ) {
        Dimension size = _component().getMinimumSize();
        _component().setMinimumSize(new Dimension(size.width, UI.scale(height)));
        return this;
    }

    /**
     *  Exposes the minimum size of the component in DPI agnostic "developer pixels" which are
     *  computed by dividing the already scaled minimum component width and height 
     *  in "component pixels" by the {@link UI#scale()} factor.<br>
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getMinimumSize()} for more information.
     *  </p>
     *  @return The minimum size of the component.
     */
    public final Size getMinSize() {
        return Size.of(UI.unscale(_component().getMinimumSize()));
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the maximum size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMaximumSize(Dimension)} for more information.
     *  </p>
     *  @param size The maximum size of the component.
     *  @return The delegate itself.
     */
    @Deprecated
    public final AbstractDelegate<C> setMaxSize( Dimension size ) {
        _component().setMaximumSize(size == null ? null : UI.scale(size));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the maximum size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMaximumSize(Dimension)} for more information.
     *  </p>
     *  Also note that the supplied size is treated as being measured in "developer pixel size", 
     *  which are defined as pixels <b>without</b> the UI scaling factor applied to them.<br>
     *  This method will scale the supplied size automatically for you, and therefore you as a developer
     *  may define dimensions in your GUI code consistently, without sacrificing dynamic DPI scaling.
     *  
     *  @param size The maximum size of the component in developer pixel size,
     *              or {@link Size#unknown()} to set the maximum size to being "undefined"/"null".
     *  @return The delegate itself.
     *  @throws NullPointerException if the supplied size is {@code null}.
     */
    public final AbstractDelegate<C> setMaxSize( Size size ) {
        Objects.requireNonNull(size, "Use Size.unknown() instead of null to represent the absence of a size.");
        if ( size.equals(Size.unknown()) )
            _component().setMaximumSize(null);
        else
            _component().setMaximumSize(UI.scale(size.toDimension()));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the maximum size of the component in "developer pixel".
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMaximumSize(Dimension)} for more information.
     *  </p>
     *  Also note that the supplied size is treated as being measured in "developer pixel size", 
     *  which are defined as pixels <b>without</b> the UI scaling factor applied to them.<br>
     *  This method will scale the supplied size automatically for you, and therefore you as a developer
     *  may define dimensions in your GUI code consistently, without sacrificing dynamic DPI scaling.
     *  
     *  @param width The maximum width of the component in developer pixels (not scaled for high DPI).
     *  @param height The maximum height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMaxSize( int width, int height ) {
        _component().setMaximumSize(UI.scale(new Dimension(width, height)));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the maximum width of the component in "developer pixel".
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMaximumSize(Dimension)} for more information.
     *  </p>
     *  Also note that the supplied width is treated as being measured in "developer pixel size", 
     *  which are defined as pixels <b>without</b> the UI scaling factor applied to them.<br>
     *  This method will scale the supplied width automatically for you, and therefore you as a developer
     *  may define dimensions in your GUI code consistently, without sacrificing dynamic DPI scaling.
     *  
     *  @param width The maximum width of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMaxWidth( int width ) {
        Dimension size = _component().getMaximumSize();
        _component().setMaximumSize(new Dimension(UI.scale(width), size.height));
        return this;
    }

    /**
     *  Takes a new maximum height for the component in "developer pixel" and applies it as "component pixels".
     *  This means that the supplied height will be scaled to dynamic "component pixels"
     *  through multiplication with the {@link UI#scale()} factor.<br>
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMaximumSize(Dimension)} for more information.
     *  </p>
     *  Also note that the supplied height is treated as being measured in "developer pixel size",
     *  which are defined as pixels <b>without</b> the UI scaling factor applied to them.<br>
     *  This method will scale the supplied height automatically for you, and therefore you as a developer
     *  may define dimensions in your GUI code consistently, without sacrificing dynamic DPI scaling.
     *
     *  @param height The maximum height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMaxHeight( int height ) {
        Dimension size = _component().getMaximumSize();
        _component().setMaximumSize(new Dimension(size.width, UI.scale(height)));
        return this;
    }

    /**
     *  Exposes the maximum size of the component in DPI agnostic "developer pixels" which are
     *  computed by dividing the already scaled maximum width and height of the component
     *  in "component pixels" by the {@link UI#scale()} factor.<br>
     *  The maximum size of a component is used by the layout manager to determine 
     *  the size of the component.
     *  <p>
     *  See {@link Component#getMaximumSize()} for more information.
     *  </p>
     *  @return The maximum size of the component in "developer pixels".
     */
    public final Size getMaxSize() {
        return Size.of(UI.unscale(_component().getMaximumSize()));
    }

    /**
     *  You can use this method to set the size of the component in "developer pixel",
     *  meaning that the supplied dimensions will be scaled to dynamic "component pixels"
     *  using the {@link UI#scale()} factor.<br>
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param size The size of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     *  @deprecated Use {@link #setSize(Size)} instead of this method!
     */
    @Deprecated
    public final AbstractDelegate<C> setSize( Dimension size ) {
        _component().setSize(size == null ? null : UI.scale(size));
        return this;
    }

    /**
     *  Takes a new {@link Size} for the component in "developer pixel" and applies it as "component pixels".
     *  This means that the supplied dimensions will be scaled to dynamic "component pixels"
     *  through multiplication with the {@link UI#scale()} factor.<br>
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param size The size of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setSize( Size size ) {
        Objects.requireNonNull(size);
        _component().setSize(UI.scale(size.toDimension()));
        return this;
    }

    /**
     *  Takes two new width and height for the component in "developer pixel" and applies it as "component pixels".
     *  This means that the supplied dimensions will be scaled to dynamic "component pixels"
     *  through multiplication with the {@link UI#scale()} factor.<br>
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param width The width of the component in developer pixels (not scaled for high DPI).
     *  @param height The height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setSize( int width, int height ) {
        _component().setSize(UI.scale(new Dimension(width, height)));
        return this;
    }

    /**
     *  Takes a new width for the underlying component in "developer pixel" and applies it as "component pixels".
     *  This means that the supplied width will be scaled to dynamic "component pixels"
     *  through multiplication with the {@link UI#scale()} factor.<br>
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The regular size of a component is typically managed and set by the layout manager of a component.
     *  So you may not want to set this in most cases...
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param width The width of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setWidth( int width ) {
        Dimension size = _component().getSize();
        _component().setSize(new Dimension(UI.scale(width), size.height));
        return this;
    }

    /**
     *  You can use this method to set the height of the delegated component in "developer pixel".
     *  The value you pass to this method will be scaled by {@link UI#scale(int)} and then set.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param height The height of the component in developer pixels (not scaled for high DPI).
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setHeight( int height ) {
        Dimension size = _component().getSize();
        _component().setSize(new Dimension(size.width, UI.scale(height)));
        return this;
    }

    /**
     *  Exposes the size of the delegated component in "developer pixel".
     *  Theis property is typically managed and set by the layout manager.
     *  <p>
     *  See {@link Component#getSize()} for more information.
     *  </p>
     *  @return The size of the component.
     */
    public final Size getSize() {
        return Size.of(UI.unscale(_component().getSize()));
    }

    /**
     *  Exposes the width of the underlying component in "developer pixel".
     *  Theis property is typically managed and set by the layout manager.
     *  <p>
     *  See {@link Component#getSize()} for more information.
     *  </p>
     *  @return The width of the component in developer pixels (not scaled for high DPI).
     */
    public final int getWidth() {
        return UI.unscale(_component().getSize().width);
    }

    /**
     *  Exposes the height of the component in "developer pixel" pixel.
     *  Theis property is typically managed and set by the layout manager.
     *  <p>
     *  See {@link Component#getSize()} for more information.
     *  </p>
     *  @return The height of the component in developer pixels (not scaled for high DPI).
     */
    public final int getHeight() {
        return UI.unscale(_component().getSize().height);
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the {@link UI.Cursor} of the component.
     *
     * @param cursor The {@link UI.Cursor} which should be set.
     * @return The delegate itself.
     * @throws NullPointerException if the supplied enum instance is {@code null}!
     */
    public final AbstractDelegate<C> setCursor( UI.Cursor cursor ) {
        Objects.requireNonNull(cursor);
        _component().setCursor(Cursor.getPredefinedCursor(cursor.type));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the {@link Cursor} of the component.
     * @param cursor The {@link Cursor} which should be set.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setCursor( Cursor cursor ) {
        _component().setCursor(cursor);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the {@link Cursor} of the component.
     * @return The {@link Cursor} of the component.
     */
    public final Cursor getCursor() {
        return _component().getCursor();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the tooltip of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link JComponent#setToolTipText(String)} for more information.
     *  </p>
     * @param text  The tooltip text.
     * @return The delegate itself.
     * @throws NullPointerException If the text is null, use an empty string to model the absence of a tooltip.
     */
    public final AbstractDelegate<C> setTooltip( String text ) {
        Objects.requireNonNull(text, "Use an empty string instead of null to represent the absence of a tooltip.");
        _component().setToolTipText( text.isEmpty() ? null : text );
        return this;
    }

    /**
     *  Exposes the tooltip {@link String} of the component or
     *  an empty {@link String} if the component does not have a tooltip.
     *  Note that this method will never return {@code null}.
     *  <p>
     *  See {@link JComponent#getToolTipText()} for more information.
     *  </p>
     * @return The tooltip text or an empty {@link String} if this component
     *         does not have a tooltip, <b>but {@code null} is never returned</b>!
     */
    public final String getTooltip() {
        String nullableToolTip = _component().getToolTipText();
        return nullableToolTip == null ? "" : nullableToolTip;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently enable or disable the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link JComponent#setEnabled(boolean)} for more information.
     *  </p>
     *  @param enabled True if the component should be enabled, false otherwise.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setEnabled( boolean enabled ) {
        _component().setEnabled(enabled);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently check if the component is enabled.
     *  <p>
     *  See {@link JComponent#isEnabled()} for more information.
     *  </p>
     *  @return True if the component is enabled, false otherwise.
     */
    public final boolean isEnabled() {
        return _component().isEnabled();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently enable or disable the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link JComponent#setVisible(boolean)} for more information.
     *  </p>
     *  @param visible True if the component should be visible, false otherwise.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setVisible( boolean visible ) {
        _component().setVisible(visible);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently check if the component is visible.
     *  <p>
     *  See {@link JComponent#isVisible()} for more information.
     *  </p>
     *  @return True if the component is visible, false otherwise.
     */
    public final boolean isVisible() {
        return _component().isVisible();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently check if the component is opaque.
     *  <p>
     *  See {@link JComponent#isOpaque()} for more information.
     *  </p>
     * @return True if the component is opaque, false otherwise.
     */
    public final boolean isOpaque() {
        return _component().isOpaque();
    }

    /**
     *  Use this to query the UI's component tree and find any {@link JComponent}
     *  of a particular type and id (the name of the component).
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param id The ide of the {@link JComponent} which should be found in the swing tree.
     * @return An {@link Optional} instance which may or may not contain the requested component.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> OptionalUI<T> find( Class<T> type, String id ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(id);
        return this.find( type, c -> ComponentExtension.from(c).hasId(id) );
    }

    /**
     *  Use this to query the UI's component tree and find any {@link JComponent}
     *  of a particular type and id (the name of the component).
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param id The ide of the {@link JComponent} which should be found in the swing tree.
     * @return An {@link Optional} instance which may or may not contain the requested component.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> OptionalUI<T> find( Class<T> type, Enum<?> id ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(id);
        return this.find( type, c -> ComponentExtension.from(c).hasId(id) );
    }

    /**
     *  Use this to query the UI's component tree and find any {@link JComponent}
     *  based on a specific type and a predicate which is used to test
     *  if a particular component in the tree is the one you are looking for.
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param predicate The predicate which should be used to test the {@link JComponent}.
     * @return An {@link Optional} instance which may or may not contain the requested component.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> OptionalUI<T> find( Class<T> type, Predicate<T> predicate ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(predicate);
        return _guiTraverser.find(type, predicate)
                .findFirst()
                .map(OptionalUI::ofNullable)
                .orElse(OptionalUI.empty());
    }

    /**
     *  Use this to query the UI's component tree and find all {@link JComponent}s
     *  based on a specific type and a predicate which is used to test
     *  if a particular component in the tree is the one you are looking for.
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param predicate The predicate which should be used to test the {@link JComponent}.
     * @return A list of {@link JComponent} instances which match the given type and predicate.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> List<T> findAll( Class<T> type, Predicate<T> predicate ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(predicate);
        return _guiTraverser.find(type, predicate).collect(Collectors.toList());
    }

    /**
     *  Use this to query the UI's component tree and find all {@link JComponent}s
     *  of a particular type and also that belong to a particular style group.
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param group The style group which should be used to test the {@link JComponent}.
     * @return A list of {@link JComponent} instances which match the given type and group.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> List<T> findAllByGroup( Class<T> type, String group ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(group);
        return this.findAll( type, c -> ComponentExtension.from(c).belongsToGroup(group) );
    }

    /**
     *  Use this to query the UI's component tree and find all {@link JComponent}s
     *  that belong to a particular style group.
     *
     * @param group The style group which should be used to check if a particular {@link JComponent} belongs to it.
     * @return A list of {@link JComponent} instances which all have the given style group.
     * @throws NullPointerException If the group is null.
     */
    public final List<JComponent> findAllByGroup( String group ) {
        Objects.requireNonNull(group);
        return this.findAll( JComponent.class, c -> ComponentExtension.from(c).belongsToGroup(group) );
    }


    /**
     *  Use this to query the UI's component tree and find all {@link JComponent}s
     *  of a particular type and also that belong to a particular style group.
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param group The style group which should be used to test the {@link JComponent}.
     * @return A list of {@link JComponent} instances which match the given type and predicate.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> List<T> findAllByGroup( Class<T> type, Enum<?> group ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(group);
        return this.findAll( type, c -> ComponentExtension.from(c).belongsToGroup(group) );
    }

    /**
     *  Use this to query the UI's component tree and find all {@link JComponent}s
     *  that belong to a particular style group.
     *
     * @param group The style group which should be used to check if a particular {@link JComponent} belongs to it.
     * @return A list of {@link JComponent} instances which all have the given style group.
     */
    public final List<JComponent> findAllByGroup( Enum<?> group ) {
        Objects.requireNonNull(group);
        return this.findAll( JComponent.class, c -> ComponentExtension.from(c).belongsToGroup(group) );
    }

    /**
     *  A common use case is to render something on top of the {@link UI.ComponentArea#BODY}
     *  of the component using the {@link Graphics2D} instance of the component.
     *  This method allows you to attach a paint task to the component, which
     *  the EDT will process in the next repaint event cycle, and remove when the animation expires.
     *  This ensures that custom rendering
     *  is not erased by a potential repaint of the component after a user event.
     *  <p>
     *  Here is an example of how to use this method as part of a fancy button animation:
     *  <pre>{@code
     *      UI.button("Click me").withPrefSize(400, 400)
     *      .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
     *          double r = 300 * status.progress() * it.scale();
     *          double x = it.mouseX() - r / 2;
     *          double y = it.mouseY() - r / 2;
     *          it.paint(status, g -> {
     *              g.setColor(new Color(1f, 1f, 0f, (float) (1 - status.progress())));
     *              g.fillOval((int) x, (int) y, (int) r, (int) r);
     *          });
     *      }))
     *  }</pre>
     *  You may also be interested in doing style animations, if so, maybe consider taking a look at
     *  {@link UIForAnySwing#withTransitoryStyle(sprouts.Observable, LifeTime, AnimatedStyler)} to see how to do event based styling animations
     *  and {@link UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)} to see how to do 2 state switch based styling animations.
     *
     * @param status The current animation progress status, which is important so that the rendering can be synchronized with the animation.
     * @param painter The rendering task which should be executed on the EDT at the end of the current event cycle.
     */
    public final void paint( AnimationStatus status, Painter painter ) {
        Objects.requireNonNull(status);
        Objects.requireNonNull(painter);
        paint(UI.ComponentArea.BODY, status, painter);
    }

    /**
     *  A common use case is to render something on top of the component
     *  using the {@link Graphics2D} instance of the component.
     *  This method allows you to attach a paint task to the component, which
     *  the EDT will process in the next repaint event cycle, and remove when the animation expires.
     *  This ensures that custom rendering
     *  is not erased by a potential repaint of the component after a user event. <br>
     *  Additionally, you can specify the area of the component which should be painted.
     *  <p>
     *  Here is an example of how to use this method as part of a button animation:
     *  <pre>{@code
     *      UI.button("Click me").withPrefSize(400, 400)
     *      .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
     *          double r = 300 * status.progress();
     *          double x = it.mouseX() - r / 2;
     *          double y = it.mouseY() - r / 2;
     *          it.paint(UI.ComponentArea.BORDER, state, g -> {
     *              g.setColor(new Color(1f, 1f, 0f, (float) (1 - status.progress())));
     *              g.fillOval((int) x, (int) y, (int) r, (int) r);
     *          });
     *      }))
     *  }</pre>
     *  You may also be interested in doing style animations, if so, maybe consider taking a look at
     *  {@link UIForAnySwing#withTransitoryStyle(sprouts.Observable, LifeTime, AnimatedStyler)} to see how to do event based styling animations
     *  and {@link UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)} to see how to do 2 state switch based styling animations.
     *
     * @param area The area of the component which should be painted.
     * @param status The current animation progress status,
     *               which needs to be provided so that the rendering can be synchronized with the animation.
     * @param painter The rendering task which should be executed on the EDT at the end of the current event cycle.
     */
    public final void paint( UI.ComponentArea area, AnimationStatus status, Painter painter ) {
        Objects.requireNonNull(area);
        Objects.requireNonNull(status);
        Objects.requireNonNull(painter);
        this.paint(area, UI.Layer.BORDER, status, painter);
    }

    /**
     *  A common use case is to render something on top of the {@link UI.ComponentArea#BODY}
     *  of the component using the {@link Graphics2D} instance of the component.
     *  This method allows you to attach a paint task to the component, which
     *  the EDT will process in the next repaint event cycle, and remove when the animation expires.
     *  This ensures that custom rendering is not erased by a potential repaint
     *  of the component after a user event.
     *  <p>
     *  Here is an example of how to use this method as part of a fancy button animation:
     *  <pre>{@code
     *      UI.button("Click me").withPrefSize(400, 400)
     *      .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
     *          double r = 300 * status.progress() * it.scale();
     *          double x = it.mouseX() - r / 2;
     *          double y = it.mouseY() - r / 2;
     *          it.paint(UI.Layer.CONTENT, status, g -> {
     *              g.setColor(new Color(1f, 1f, 0f, (float) (1 - status.progress())));
     *              g.fillOval((int) x, (int) y, (int) r, (int) r);
     *          });
     *      }))
     *  }</pre>
     *  You may also be interested in doing style animations, if so, maybe consider taking a look at
     *  {@link UIForAnySwing#withTransitoryStyle(sprouts.Observable, LifeTime, AnimatedStyler)} to see how to do event based styling animations
     *  and {@link UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)} to see how to do 2 state switch based styling animations.
     *
     * @param layer The layer of the component which should be painted on.
     * @param status The current animation progress status, which is important so that the rendering can be synchronized with the animation.
     * @param painter The rendering task which should be executed on the EDT at the end of the current event cycle.
     */
    public final void paint( UI.Layer layer, AnimationStatus status, Painter painter ) {
        this.paint(UI.ComponentArea.BODY, layer, status, painter);
    }

    /**
     *  A common use case is to render something on top of the component
     *  using the {@link Graphics2D} instance used by the component to render itself.
     *  This method allows you to attach a paint task to the component, which
     *  the EDT will process in the next repaint event cycle, and remove when the animation expires.
     *  This ensures that custom rendering
     *  is not erased by a potential repaint of the component after a user event. <br>
     *  Additionally, you can specify the area of the component which should be painted
     *  as well as a layer onto which the painter should paint.
     *  <p>
     *  Here is an example of how to use this method as part of a button animation:
     *  <pre>{@code
     *      UI.button("Click me").withPrefSize(400, 400)
     *      .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
     *          double r = 300 * status.progress();
     *          double x = it.mouseX() - r / 2;
     *          double y = it.mouseY() - r / 2;
     *          it.paint(UI.ComponentArea.BODY, UI.Layer.CONTENT, state, g -> {
     *              g.setColor(new Color(1f, 1f, 0f, (float) (1 - status.progress())));
     *              g.fillOval((int) x, (int) y, (int) r, (int) r);
     *          });
     *      }))
     *  }</pre>
     *  You may also be interested in doing style animations, if so, maybe consider taking a look at
     *  {@link UIForAnySwing#withTransitoryStyle(sprouts.Observable, LifeTime, AnimatedStyler)} to see how to do event based styling animations
     *  and {@link UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)} to see how to do 2 state switch based styling animations.
     *
     * @param area The area of the component which should be painted.
     * @param layer The layer of the component which should be painted.
     * @param status The current animation progress status,
     *               which needs to be provided so that the rendering can be synchronized with the animation.
     * @param painter An implementation of a simple functional interface receiving a
     *                {@link Graphics2D} instance for doing paint operations...
     */
    public final void paint( UI.ComponentArea area, UI.Layer layer, AnimationStatus status, Painter painter ) {
        Objects.requireNonNull(status);
        Objects.requireNonNull(painter);
        UI.run(()->{ // This method might be called by the application thread, so we need to run on the EDT!
            // We do the rendering later in the paint method of a custom border implementation!
            ComponentExtension.from(_component).addAnimatedPainter(status, layer, area, painter);
        });
    }

    /**
     *  Use this method to delegate to the parent of the component.
     *  This is useful if you want to apply a style or animation
     *  to the parent of a component which just received an event,
     *  like a mouse click event.
     *  <p>
     *  Here is an example demonstrating how
     *  to apply a style to the parent of a component when it is clicked:
     *  <pre>{@code
     *      UI.button("Click me").withPrefSize(400, 400)
     *      .onMouseClick( it -> it
     *          .parentDelegate( parent -> parent
     *              .style( style -> style
     *                  .borderWidth(6)
     *                  .borderColor(Color.RED)
     *                  .borderRadius(60)
     *              )
     *          )
     *      )
     *  }</pre> <br>
     *  And here another more advanced example demonstrating how to do animated
     *  painting on the parent of a component that is being dragged away:
     *  <pre>{@code
     *  label("Drag me away")
     *  .withDragAway( conf -> conf
     *      .onDragStart( it -> {
     *          it.parentDelegate( parent -> parent
     *              .animateFor(1, TimeUnit.SECONDS, status -> {
     *                  double r = 320 * status.fadeOut();
     *                  double x = it.getEvent().getDragOrigin().getX() - r / 2;
     *                  double y = it.getEvent().getDragOrigin().getY() - r / 2;
     *                  parent.paint(status, g -> {
     *                      g.setColor(new Color(0f, 1f, 1f, (float) status.fadeIn()));
     *                      g.fillOval((int) x, (int) y, (int) r, (int) r);
     *                  });
     *              })
     *          );
     *      })
     *  )
     *  }</pre>
     *  The paint method draws a circle around the drag origin of the drag event
     *  which gets smaller and more opaque as the animation progresses
     *  for a duration of 1 second. <br>
     *  <p>
     *  You may also be interested in doing style animations, if so, maybe consider taking a look at
     *  {@link UIForAnySwing#withTransitoryStyle(sprouts.Observable, LifeTime, AnimatedStyler)} to see how to do event
     *  based styling animations and {@link UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)}
     *  to see how to do 2 state switch based styling animations.
     *
     * @param action The action which should be executed on the delegate of the parent component.
     */
    public final void parentDelegate( sprouts.Action<AbstractDelegate<JComponent>> action ) {
        Objects.requireNonNull(action);
        UI.run(()->{ // This method might be called by the application thread, so we need to run on the EDT!
            Container parent = _component().getParent();
            if ( parent instanceof JComponent ) {
                try {
                    AbstractDelegate<JComponent> delegate = new AbstractDelegate<>( false, (JComponent) parent, (JComponent) parent );
                    action.accept(delegate);
                } catch( Exception e ) {
                    log.error(SwingTree.get().logMarker(), "Error while processing parent delegate!", e);
                }
            } else {
                log.warn(SwingTree.get().logMarker(), "Parent of component is not a JComponent, cannot apply parent delegate!");
            }
        });
    }

    /**
     *  A common use case is to animate the style of a component when a user event occurs.
     *  This method allows you to dispatch a styling animation to the EDT
     *  which will cause the component style to updated repeatedly until the animation expires.
     *  Because animation styles are applied last, it is guaranteed not to be overwritten by
     *  other {@link Styler} lambdas.
     *  Note that the provided styling will be removed automatically when the animation expires,
     *  so no manual cleanup is required.
     *  <p>
     *  Here is an example of how to use this method as part of a fancy styling animation:
     *  <pre>{@code
     *    UI.button("Click me").withPrefSize(400, 400)
     *    .onMouseClick( it -> it.animateStyleFor(2, TimeUnit.SECONDS, (state, style) ->
     *        .borderWidthAt(UI.Edge.BOTTOM, (int)(6 * state.progress()) )
     *        .borderColor( new Color(0f, 1f, 1f, (float) (1 - state.progress())) )
     *        .borderRadius( (int)(60 * state.progress()) )
     *    ))
     *  }</pre>
     *  <b>You can achieve the same effect using {@link #animateFor(LifeTime, Animation)}
     *  and {@link #style(AnimationStatus, Styler)} as follows:</b>
     *  <pre>{@code
     *    UI.button("Click me").withPrefSize(400, 400)
     *    .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
     *        it.style(status, style -> style
     *            // This is the same as the animateStyleFor() method above!
     *        );
     *    }))
     *  }</pre>
     *  Also see {@link #animateStyleFor(LifeTime, AnimatedStyler)} for a version of this method which
     *  uses a {@link LifeTime} instead of a duration.
     *  If you are interested in doing more advanced style animations, consider taking a look at
     *  {@link UIForAnySwing#withTransitoryStyle(sprouts.Observable, LifeTime, AnimatedStyler)} to see how to do event
     *  based styling animations and {@link UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)} to
     *  see how to do 2 state switch based styling animations.
     *
     * @param duration The duration of the animation.
     * @param unit The time unit of the duration.
     * @param styler The styling animation task which should be executed on the EDT at the end of the current event cycle.
     *               It receives both the current animation state and the {@link swingtree.style.ComponentStyleDelegate}
     *               for which you can define the style properties.
     */
    public final void animateStyleFor( double duration, TimeUnit unit, AnimatedStyler<C> styler ) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(styler);
        UI.run(()->{ // This method might be called by the application thread, so we need to run on the EDT!
            // We do the styling later in the paint method of a custom border implementation!
            this.animateFor(duration, unit, status ->
                ComponentExtension.from(_component).addAnimatedStyler(status, conf -> styler.style(status, conf))
            );
        });
    }

    /**
     *  A common use case is to animate the style of a component when a user event occurs.
     *  This method allows you to dispatch a styling animation to the EDT
     *  which will cause the component style to updated repeatedly until the animation expires.
     *  Because animation styles are applied last, it is guaranteed not to be overwritten by
     *  other {@link Styler} lambdas.
     *  Note that the provided styling will be removed automatically when the animation expires,
     *  so no manual cleanup is required.
     *  <p>
     *  Here is an example of how to use this method as part of a fancy styling animation:
     *  <pre>{@code
     *    UI.button("Click me").withPrefSize(400, 400)
     *    .onMouseClick( it -> it.animateStyleFor(UI.lifetime(2, TimeUnit.SECONDS), (state, style) ->
     *        .borderWidthAt(UI.Edge.BOTTOM, (int)(6 * state.progress()) )
     *        .borderColor( new Color(0f, 1f, 1f, (float) (1 - state.progress())) )
     *        .borderRadius( (int)(60 * state.progress()) )
     *    ))
     *  }</pre>
     *  <b>Not that the effect of this method can also be modelled using {@link #animateFor(LifeTime, Animation)}
     *  and {@link #style(AnimationStatus, Styler)} as follows:</b>
     *  <pre>{@code
     *    UI.button("Click me").withPrefSize(400, 400)
     *    .onMouseClick( it -> it.animateFor(UI.lifetime(2, TimeUnit.SECONDS), status -> {
     *        it.style(status, style -> style
     *            // This is the same as the animateStyleFor() method above!
     *        );
     *    }))
     *  }</pre>
     *  Also see {@link #animateStyleFor(double, TimeUnit, AnimatedStyler)} for a version of this method which uses a {@link LifeTime} instead of a duration.
     *  If you are interested in doing more advanced style animations, consider taking a look at
     *  {@link UIForAnySwing#withTransitoryStyle(sprouts.Observable, LifeTime, AnimatedStyler)} to see how to do event based styling animations
     *  and {@link UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)} to see how to do 2 state switch based styling animations.
     *
     * @param lifetime The lifetime of the animation.
     *                 The animation will be removed automatically when the lifetime expires.
     * @param styler The styling animation task which should be executed on the EDT at the end of the current event cycle.
     *               It receives both the current animation state and the {@link swingtree.style.ComponentStyleDelegate}
     *               for which you can define the style properties.
     */
    public final void animateStyleFor( LifeTime lifetime, AnimatedStyler<C> styler ) {
        Objects.requireNonNull(lifetime);
        Objects.requireNonNull(styler);
        UI.run(()->{ // This method might be called by the application thread, so we need to run on the EDT!
            // We do the styling later in the paint method of a custom border implementation!
            this.animateFor(lifetime, status ->
                ComponentExtension.from(_component).addAnimatedStyler(status, conf -> styler.style(status, conf))
            );
        });
    }

    /**
     *  A common use case is to style the component based on the current animation state.
     *  This method allows you to dispatch a styling task to the EDT
     *  which will be executed before the next component repaint.
     *  Because animation styles are applied last, it is guaranteed not to be overwritten by
     *  other styles.
     *  The provided styling will be removed when the animation expires.
     *  <p>
     *  Here is an example of how to use this method as part of a fancy styling animation:
     *  <pre>{@code
     *      UI.button("Click me").withPrefSize(400, 400)
     *      .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
     *          it.style(status, style -> style
     *              .borderWidth((int)(10 * status.progress()))
     *              .borderColor(new Color(1f, 1f, 0f, (float) (1 - status.progress())))
     *              .borderRadius((int)(100 * status.progress()))
     *          );
     *      }))
     *  }</pre>
     *
     * @param state The current animation state, which is important so that the styling can be synchronized with the animation.
     * @param styler The styling task which should be executed on the EDT at the end of the current event cycle.
     */
    public final void style(AnimationStatus state, Styler<C> styler ) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(styler);
        UI.run(()->{ // This method might be called by the application thread, so we need to run on the EDT!
            // We do the styling later in the paint method of a custom border implementation!
            ComponentExtension.from(_component).addAnimatedStyler(state, styler);
        });
    }

    /**
     *  Exposes access the animation builder API, where you can define the conditions
     *  under which the animation should be executed and then dispatch the animation to the EDT
     *  through the {@link AnimationDispatcher#go(Animation)} method.
     *
     *  @param duration The duration of the animation.
     *  @param unit The time unit of the duration.
     *  @return An {@link AnimationDispatcher} instance which can be used to define how the animation should be executed.
     */
    public final AnimationDispatcher animateFor(double duration, TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return AnimationDispatcher.animateFor(LifeTime.of(duration, unit), _component());
    }

    /**
     *  Exposes access the animation builder API, where you can define the conditions
     *  under which the animation should be executed and then dispatch the animation to the EDT
     *  through the {@link AnimationDispatcher#go(Animation)} method.
     *
     *  @param lifeTime The lifetime of the animation.
     *  @return An {@link AnimationDispatcher} instance which can be used to define how the animation should be executed.
     */
    public final AnimationDispatcher animateFor(LifeTime lifeTime ) {
        Objects.requireNonNull(lifeTime);
        return AnimationDispatcher.animateFor(lifeTime, _component());
    }

    /**
     *  Use this to schedule and run the provided animation
     *  to be executed on the EDT.
     *  A single animation iteration may be executed multiple times
     *  for the given duration in order to achieve a smooth transition. <br>
     *  Here an example of how to use this method
     *  on a "Save" button:
     *  <pre>{@code
     *  UI.button("Save").withPrefSize(400, 400)
     *  .onMouseClick( it -> it
     *    .animateFor(
     *      UI.lifetime(1, TimeUnit.SECONDS)
     *      .startingIn( 0.5, TimeUnit.SECONDS )
     *    )
     *    .go( myAnimation )
     *  )
     *  }</pre>
     *
     *
     *  @param lifeTime The lifetime of the animation.
     *  @param animation The animation that should be executed.
     */
    public final void animateFor( LifeTime lifeTime, Animation animation ) {
        Objects.requireNonNull(lifeTime);
        Objects.requireNonNull(animation);
        AnimationDispatcher.animateFor(lifeTime, _component()).go(animation);
    }

    /**
     *  Use this to schedule and run the provided animation
     *  to be executed on the EDT.
     *  A single animation iteration may be executed multiple times
     *  for the given duration in order to achieve a smooth transition.
     *
     *  @param duration The duration of the animation.
     *  @param unit The time unit of the duration.
     *  @param animation The animation that should be executed.
     */
    public final void animateFor( double duration, TimeUnit unit, Animation animation ) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(animation);
        this.animateFor(duration, unit).go(animation);
    }

}

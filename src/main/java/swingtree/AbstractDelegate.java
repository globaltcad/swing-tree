package swingtree;

import swingtree.animation.Animate;
import swingtree.animation.Animation;
import swingtree.animation.Schedule;
import swingtree.style.Painter;
import swingtree.style.Style;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 *  Extensions of this class delegate a component event
 *  the component itself and the component tree of the component
 *  in which the component is contained.
 *  Instances of this class are passed to various user event {@link Action} handlers.
 *  You can use this to change the state of the component, schedule animations
 *  for the component or query the tree of the components.
 *
 * @param <C> The type of the component that is delegated.
 */
abstract class AbstractDelegate<C extends JComponent>
{
    private final Query _query; // the query object that allows us to query the component tree
    private final C _component;

    AbstractDelegate(C component, JComponent handle) {
        _query = new Query(handle);
        _component = component;
    }

    protected C _component() { return _component; }

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
     * @return The background color of the component.
     */
    public final Color getBackground() {
        return _component().getBackground();
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
        _component().setForeground(color);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the foreground color of the component.
     *  <p>
     *  See {@link Component#getForeground()} for more information.
     *  </p>
     *
     * @return The foreground color of the component.
     */
    public final Color getForeground() {
        return _component().getForeground();
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
     *             If this parameter is <code>null</code> then this component will inherit
     *             the font of its parent.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setFont( Font font ) {
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
        return _component().getFont();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the bounds of the component.
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
     */
    public final AbstractDelegate<C> setBounds( int x, int y, int width, int height ) {
        _component().setBounds(x, y, width, height);
        return this;
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
     */
    public final AbstractDelegate<C> setBounds( Rectangle bounds ) {
        _component().setBounds(bounds);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the bounds of the component.
     *  <p>
     *  See {@link Component#getBounds()} for more information.
     *  </p>
     *
     *  @return The bounds of the component.
     *          This is relative to the component's parent.
     */
    public final Rectangle getBounds() {
        return _component().getBounds();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the preferred size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  @param size The preferred size of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefSize( Dimension size ) {
        _component().setPreferredSize(size);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the preferred size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  @param width The preferred width of the component.
     *  @param height The preferred height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefSize( int width, int height ) {
        _component().setPreferredSize(new Dimension(width, height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the preferred width of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  @param width The preferred width of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefWidth( int width ) {
        Dimension size = _component().getPreferredSize();
        _component().setPreferredSize(new Dimension(width, size.height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the preferred height of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setPreferredSize(Dimension)} for more information.
     *  </p>
     *  @param height The preferred height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setPrefHeight( int height ) {
        Dimension size = _component().getPreferredSize();
        _component().setPreferredSize(new Dimension(size.width, height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the preferred size of the component.
     *  The preferred size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getPreferredSize()} for more information.
     *  </p>
     *  @return The preferred size of the component.
     */
    public final Dimension getPrefSize() {
        return _component().getPreferredSize();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the minimum size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMinimumSize(Dimension)} for more information.
     *  </p>
     *  @param size The minimum size of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMinSize( Dimension size ) {
        _component().setMinimumSize(size);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the minimum size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMinimumSize(Dimension)} for more information.
     *  </p>
     *  @param width The minimum width of the component.
     *  @param height The minimum height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMinSize( int width, int height ) {
        _component().setMinimumSize(new Dimension(width, height));
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
     *  @param width The minimum width of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMinWidth( int width ) {
        Dimension size = _component().getMinimumSize();
        _component().setMinimumSize(new Dimension(width, size.height));
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
     *  @param height The minimum height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMinHeight( int height ) {
        Dimension size = _component().getMinimumSize();
        _component().setMinimumSize(new Dimension(size.width, height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the minimum size of the component.
     *  The minimum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getMinimumSize()} for more information.
     *  </p>
     *  @return The minimum size of the component.
     */
    public final Dimension getMinSize() {
        return _component().getMinimumSize();
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
    public final AbstractDelegate<C> setMaxSize( Dimension size ) {
        _component().setMaximumSize(size);
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
     *  @param width The maximum width of the component.
     *  @param height The maximum height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMaxSize( int width, int height ) {
        _component().setMaximumSize(new Dimension(width, height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the maximum width of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMaximumSize(Dimension)} for more information.
     *  </p>
     *  @param width The maximum width of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMaxWidth( int width ) {
        Dimension size = _component().getMaximumSize();
        _component().setMaximumSize(new Dimension(width, size.height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the maximum height of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setMaximumSize(Dimension)} for more information.
     *  </p>
     *  @param height The maximum height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setMaxHeight( int height ) {
        Dimension size = _component().getMaximumSize();
        _component().setMaximumSize(new Dimension(size.width, height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the maximum size of the component.
     *  The maximum size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getMaximumSize()} for more information.
     *  </p>
     *  @return The maximum size of the component.
     */
    public final Dimension getMaxSize() {
        return _component().getMaximumSize();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param size The size of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setSize( Dimension size ) {
        _component().setSize(size);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the size of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param width The width of the component.
     *  @param height The height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setSize( int width, int height ) {
        _component().setSize(new Dimension(width, height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the width of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param width The width of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setWidth( int width ) {
        Dimension size = _component().getSize();
        _component().setSize(new Dimension(width, size.height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the height of the component.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#setSize(Dimension)} for more information.
     *  </p>
     *  @param height The height of the component.
     *  @return The delegate itself.
     */
    public final AbstractDelegate<C> setHeight( int height ) {
        Dimension size = _component().getSize();
        _component().setSize(new Dimension(size.width, height));
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the size of the component.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getSize()} for more information.
     *  </p>
     *  @return The size of the component.
     */
    public final Dimension getSize() {
        return _component().getSize();
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the width of the component.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getSize()} for more information.
     *  </p>
     *  @return The width of the component.
     */
    public final int getWidth() {
        return _component().getSize().width;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the height of the component.
     *  The size is used by the layout manager to determine the size of the component.
     *  <p>
     *  See {@link Component#getSize()} for more information.
     *  </p>
     *  @return The height of the component.
     */
    public final int getHeight() {
        return _component().getSize().height;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently set the {@link UI.Cursor} of the component.
     *
     * @param cursor The {@link UI.Cursor} which should be set.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setCursor( UI.Cursor cursor ) {
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
     */
    public final AbstractDelegate<C> setTooltip( String text ) {
        _component().setToolTipText(text);
        return this;
    }

    /**
     *  As a delegate to the underlying component, you can use this method to
     *  conveniently get the tooltip of the component.
     *  <p>
     *  See {@link JComponent#getToolTipText()} for more information.
     *  </p>
     * @return The tooltip text.
     */
    public final String getTooltip() {
        return _component().getToolTipText();
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
     *  conveniently make the component opaque or transparent.
     *  This method returns the delegate itself, so you can chain calls to this method.
     *  <p>
     *  See {@link JComponent#setOpaque(boolean)} for more information.
     *  </p>
     * @param opaque True if the component should be opaque, false otherwise.
     * @return The delegate itself.
     */
    public final AbstractDelegate<C> setOpaque( boolean opaque ) {
        _component().setOpaque(opaque);
        return this;
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
     *  Use this to query the UI tree and find any {@link JComponent}.
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param id The ide of the {@link JComponent} which should be found in the swing tree.
     * @return An {@link Optional} instance which may or may not contain the requested component.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> OptionalUI<T> find( Class<T> type, String id ) {
        return _query.find(type, id);
    }

    /**
     *  A common use case is to render something on top of the component
     *  using the {@link Graphics2D} instance of the component.
     *  This method allows you to dispatch a rendering task to the EDT
     *  at the end of the current event cycle, ensuring that custom rendering
     *  is not erased by a potential repaint of the component after a user event.
     *  <p>
     *  Here is an example of how to use this method as part of a fancy button animation:
     *  <pre>{@code
     *      UI.button("Click me").withPrefSize(400, 400)
     *      .onMouseClick( it -> it.animateOnce(2, TimeUnit.SECONDS, state -> {
     *          double r = 300 * state.progress();
     *          double x = it.getEvent().getX() - r / 2;
     *          double y = it.getEvent().getY() - r / 2;
     *          it.paint( g -> {
     *              g.setColor(new Color(1f, 1f, 0f, (float) (1 - state.progress())));
     *              g.fillOval((int) x, (int) y, (int) r, (int) r);
     *          });
     *      }))
     *  }</pre>
     *
     * @param painter The rendering task which should be executed on the EDT at the end of the current event cycle.
     */
    public final void paint( Painter painter ) {
        UI.run(()->{ // This method might be called by the application thread, so we need to run on the EDT!
            // We do the rendering later in the paint method of a custom border implementation!
            ComponentExtension.from(_component).addAnimationPainter(painter);
        });
    }

    /**
     *  Use this to access the animation API and schedule an animation.
     *  The animation will be executed on the EDT.
     *  @param duration The duration of the animation.
     *  @param unit The time unit of the duration.
     *  @return An {@link Animate} instance which can be used to define how the animation should be executed.
     */
    public final Animate animate( double duration, TimeUnit unit ) {
        return Animate.on( _component(), Schedule.of(duration, unit) );
    }

    /**
     *  Use this to schedule a single animation iteration
     *  that will be executed on the EDT multiple times for the given duration.
     *
     *  @param duration The duration of the animation.
     *  @param unit The time unit of the duration.
     *  @param animation The animation that should be executed.
     */
    public final void animateOnce( double duration, TimeUnit unit, Animation animation ) {
        this.animate(duration, unit).goOnce(animation);
    }

    /**
     *  Use this to schedule 2 animation iterations
     *  that will be executed on the EDT multiple times for the given duration.
     *
     *  @param duration The duration of the animation.
     *  @param unit The time unit of the duration.
     *  @param animation The animation that should be executed.
     */
    public final void animateTwice( double duration, TimeUnit unit, Animation animation ) {
        this.animate(duration, unit).goTwice(animation);
    }

}

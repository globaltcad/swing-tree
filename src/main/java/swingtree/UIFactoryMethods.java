package swingtree;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.*;
import sprouts.Event;
import swingtree.animation.*;
import swingtree.api.Configurator;
import swingtree.api.IconDeclaration;
import swingtree.api.MenuBuilder;
import swingtree.api.SwingBuilder;
import swingtree.api.model.BasicTableModel;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;
import swingtree.components.*;
import swingtree.dialogs.ConfirmAnswer;
import swingtree.dialogs.ConfirmDialog;
import swingtree.dialogs.MessageDialog;
import swingtree.dialogs.OptionsDialog;
import swingtree.layout.LayoutConstraint;
import swingtree.layout.Size;
import swingtree.style.ComponentExtension;
import swingtree.style.ScalableImageIcon;
import swingtree.style.StyleSheet;
import swingtree.style.SvgIcon;
import swingtree.threading.EventProcessor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  A namespace for useful factory methods like
 *  {@link #color(String)} and {@link #font(String)},
 *  and layout constants (see {@link UILayoutConstants}).
 *  <br>
 *  <b>
 *      This class is intended to be used
 *      by the {@link UI} namespace class ONLY!
 *      <br>
 *      Please do not inherit or import this class
 *      in your own code directly, as it is not intended
 *      to be used outside the {@link UI} namespace.
 *  </b>
 */
public abstract class UIFactoryMethods extends UILayoutConstants
{
    private static final Logger log = LoggerFactory.getLogger(UIFactoryMethods.class);

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green and blue values.
     *
     * @param r The red value (0-255).
     * @param g The green value (0-255).
     * @param b The blue value (0-255).
     * @return The new color.
     */
    public static UI.Color color( int r, int g, int b ) {
        return UI.Color.ofRgb(r, g, b);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green, blue and alpha values.
     *
     * @param r The red value (0-255).
     * @param g The green value (0-255).
     * @param b The blue value (0-255).
     * @param a The alpha value (0-255).
     * @return The new color.
     */
    public static UI.Color color( int r, int g, int b, int a ) {
        return UI.Color.ofRgba(r, g, b, a);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green and blue values.
     *
     * @param r The red value (0.0-1.0).
     * @param g The green value (0.0-1.0).
     * @param b The blue value (0.0-1.0).
     * @return The new color.
     */
    public static UI.Color color( double r, double g, double b ) {
        return UI.Color.of((float) r, (float) g, (float) b);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green, blue and alpha values.
     *
     * @param r The red value (0.0-1.0).
     * @param g The green value (0.0-1.0).
     * @param b The blue value (0.0-1.0).
     * @param a The alpha value (0.0-1.0).
     * @return The new color.
     */
    public static UI.Color color( double r, double g, double b, double a ) {
        return UI.Color.of((float) r, (float) g, (float) b, (float) a);
    }

    /**
     *  Tries to parse the supplied string as a color value
     *  based on various formats.
     *
     * @param colorAsString The string to parse.
     * @return The parsed color.
     * @throws IllegalArgumentException If the string could not be parsed.
     * @throws NullPointerException If the string is null.
     */
    public static UI.Color color( final String colorAsString )
    {
        Objects.requireNonNull(colorAsString);
        return UI.Color.of(colorAsString);
    }

    /**
     * Returns the {@code UI.Font} that the {@code fontString}
     * argument describes.
     * To ensure that this method returns the desired Font,
     * format the {@code fontString} parameter in
     * one of these ways:
     * <ul>
     * <li><em>fontname-style-pointsize</em>
     * <li><em>fontname-pointsize</em>
     * <li><em>fontname-style</em>
     * <li><em>fontname</em>
     * <li><em>fontname style pointsize</em>
     * <li><em>fontname pointsize</em>
     * <li><em>fontname style</em>
     * <li><em>fontname</em>
     * </ul>
     * in which <i>style</i> is one of the four
     * case-insensitive strings:
     * {@code "PLAIN"}, {@code "BOLD"}, {@code "BOLDITALIC"}, or
     * {@code "ITALIC"}, and {@code pointsize} is a positive decimal integer
     * representation of the point size.
     * For example, if you want a font that is Arial, bold, with
     * a point size of 18, you would call this method with:
     * "Arial-BOLD-18".
     * This is equivalent to calling the Font constructor :
     * {@code new Font("Arial", Font.BOLD, 18);}
     * and the values are interpreted as specified by that constructor.
     * <p>
     * A valid trailing decimal field is always interpreted as the pointsize.
     * Therefore, a {@code fontname} containing a trailing decimal value should not
     * be used in the {@code fontname} only form.
     * <p>
     * If a style name field is not one of the valid style strings, it is
     * interpreted as part of the font name, and the default style is used.
     * <p>
     * Only one of ' ' or '-' may be used to separate fields in the input.
     * The identified separator is the one closest to the end of the string,
     * which separates a valid pointsize, or a valid style name from
     * the rest of the string.
     * Null (empty) pointsize and style fields are treated
     * as valid fields with the default value for that field.
     *<p>
     * Some font names may include the separator characters ' ' or '-'.
     * If {@code fontString} is not formed with 3 components, e.g. such that
     * {@code style} or {@code pointsize} fields are not present in
     * {@code fontString}, and {@code fontname} also contains a
     * character determined to be the separator character
     * then these characters where they appear as intended to be part of
     * {@code fontname} may instead be interpreted as separators
     * so the font name may not be properly recognized.
     *
     * <p>
     * The default size is 12 and the default style is PLAIN.
     * If {@code str} does not specify a valid size, the returned
     * {@code Font} has a size of 12.  If {@code fontString} does not
     * specify a valid style, the returned Font has a style of PLAIN.
     * If you do not specify a valid font name in
     * the {@code fontString} argument, this method will return
     * a font with the family name "Dialog".
     * To determine what font family names are available on
     * your system, use the
     * {@link GraphicsEnvironment#getAvailableFontFamilyNames()} method.
     * If {@code fontString} is {@code null}, a new {@code Font}
     * is returned with the family name "Dialog", a size of 12 and a
     * PLAIN style.
     * @param fontString the name of the font, or {@code null}
     * @return the {@code Font} object that {@code fontString} describes.
     * @throws NullPointerException if {@code fontString} is {@code null}
     */
    public static UI.Font font( String fontString ) {
        Objects.requireNonNull(fontString);
        Exception potentialProblem1 = null;
        Exception potentialProblem2 = null;
        String mayBeProperty = System.getProperty(fontString);
        UI.Font font = null;
        try {
            if ( mayBeProperty == null )
                font = _decodeFont(fontString);
        } catch( Exception e ) {
            potentialProblem1 = e;
        }
        try {
            if ( mayBeProperty != null )
                font = _decodeFont(mayBeProperty);
        } catch( Exception e ) {
            potentialProblem2 = e;
        }
        if ( font == null ) {
            if ( potentialProblem1 != null )
                log.error("Could not parse font string '" + fontString + "' using 'Font.decode(String)'.", potentialProblem1);
            if ( potentialProblem2 != null )
                log.error("Could not parse font string '" + fontString + "' from 'System.getProperty(String)'.", potentialProblem2);

            log.error("Could not parse font string '" + fontString + "' using 'Font.decode(String)' or 'System.getProperty(String)'.", new Throwable());

            try {
                return UI.Font.of(fontString, UI.FontStyle.PLAIN, UI.scale(12));
            } catch (Exception e) {
                log.error("Could not create font with name '" + fontString + "' and size 12.", e);
                return UI.Font.of(Font.DIALOG, UI.FontStyle.PLAIN, UI.scale(12));
            }
        }
        return font;
    }

    private static UI.Font _decodeFont( String fontString ) {
        if ( !_endsWithFontSize(fontString) )
            fontString += ( "-" + UI.scale(12) );
        return UI.Font.of(Font.decode(fontString));
    }

    private static boolean _endsWithFontSize( String fontString ) {
        return fontString.matches(".*-+\\d+$");
    }

    /**
     *  This returns an instance of a generic swing tree builder
     *  for anything extending the {@link JComponent} class.
     *  <br><br>
     *
     * @param component The new component instance which ought to be part of the Swing UI.
     * @param <T> The concrete type of this new component.
     * @return A basic UI builder instance wrapping any {@link JComponent}.
     */
    public static <T extends JComponent> UIForSwing<T> of( T component )
    {
        NullUtil.nullArgCheck(component, "component", JComponent.class);
        return new UIForSwing<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for the provided {@link JPanel} type. <br>
     *  This method is typically used to enable declarative UI design for custom
     *  {@link JPanel} based components either within the constructor of a custom
     *  subclass, like so: <br>
     *  <pre>{@code
     *  class MyCustomPanel extends JPanel {
     *      public MyCustomPanel() {
     *          UI.of(this)
     *          .add(UI.label("Hello Swing!"))
     *          .add(UI.button("Click Me"))
     *          .add(UI.button("Or Me") );
     *      }
     *  }
     *  }</pre>
     *  <br>
     *  ... or as part of a UI declaration, where the custom {@link JPanel} type
     *  is added to the components tree, like so: <br>
     *  <pre>{@code
     *  UI.show(
     *      UI.panel()
     *      .add(
     *          new MyCustomPanel()
     *      )
     *      .add(..more stuff..)
     *  );
     *  }</pre>
     *  <br>
     *
     * @param component The {@link JPanel} instance to be wrapped by a swing tree UI builder for panel components.
     * @return A builder instance for the provided {@link JPanel}, which enables fluent method chaining.
     * @param <P> The type parameter of the concrete {@link JPanel} type to be wrapped.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JPanel> UIForPanel<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JPanel.class);
        return new UIForPanel<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager.
     *  This is in essence a convenience method for {@code UI.of(new JPanel(new MigLayout()))}.
     *
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel() {
        return _panel().withLayout(new MigLayout("hidemode 2"));
    }

    private static UIForPanel<JPanel> _panel() {
        return new UIForPanel<>(new BuilderState<>(UI.Panel.class, UI.Panel::new));
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( String attr ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        return _panel().withLayout(attr);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( String attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return _panel().withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints, rowConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( String attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return _panel().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LayoutConstraint attr ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        return panel(attr.toString());
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LayoutConstraint attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return _panel().withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints, rowConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LayoutConstraint attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return _panel().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}. <br>
     *  This method is typiclly used alongside the {@link UI#LC()} factory
     *  method to create a layout attributes/constraints builder, like so: <br>
     *  <pre>{@code
     *      UI.panel(
     *          UI.LC()
     *          .insets("10 10 10 10")
     *          .debug()
     *      )
     *      .add(..)
     *      .add(..)
     *  }</pre>
     *
     * @param attr The constraint attributes concerning the entire {@link MigLayout}
     *             in the form of a {@link LC} instance.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LC attr ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        return _panel().withLayout( attr );
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, ConstraintParser.parseColumnConstraints(colConstraints))))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LC} constants.
     * @param colConstraints The column constraints in the form of a {@link String} instance.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LC attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return _panel().withLayout( attr, colConstraints );
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(
     *          new MigLayout(
     *              attr,
     *              ConstraintParser.parseColumnConstraints(colConstraints),
     *              ConstraintParser.parseRowConstraints(rowConstraints)
     *          )
     *      ))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LC} instance.
     * @param colConstraints The column constraints in the form of a {@link String} instance.
     * @param rowConstraints The row constraints in the form of a {@link String} instance.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LC attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return _panel().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component with a
     *  dynamically updated set of {@link MigLayout} constraints/attributes.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}.
     *
     * @param attr The layout attributes property which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( Val<LayoutConstraint> attr ) {
        NullUtil.nullArgCheck(attr, "attr", Val.class);
        NullUtil.nullPropertyCheck(attr, "attr", "Null is not a valid layout attribute.");
        return panel(attr.get().toString()).withLayout(attr);
    }

    /**
     *  Use this to create a builder for the provided {@link JBox} instance,
     *  which is a general purpose component wrapper type with the following properties:
     *  <ul>
     *      <li>
     *          It is transparent, meaning that it does not paint its background.
     *      </li>
     *      <li>
     *          The default layout manager is a {@link MigLayout}.
     *      </li>
     *      <li>
     *          The insets (the space between the wrapped component and the box's border)
     *          are set to zero.
     *      </li>
     *      <li>
     *          There the gap size between the components added to the box is set to zero.
     *          So they will be tightly packed.
     *      </li>
     *  </ul>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     *  This method is typically used to enable declarative UI design for custom
     *  {@link JBox} based components either within the constructor of a custom
     *  subclass, like so: <br>
     *  <pre>{@code
     *  class MyCustomBox extends JBox {
     *      public MyCustomBox() {
     *          UI.of(this)
     *          .add(UI.label("Hello Swing!"))
     *          .add(UI.button("Click Me"))
     *          .add(UI.button("Or Me") );
     *      }
     *  }
     *  }</pre>
     *  <br>
     *  ... or as part of a UI declaration, where the custom {@link JBox} type
     *  is added to the component tree, like so: <br>
     *  <pre>{@code
     *  UI.show(
     *      UI.panel()
     *      .add(
     *          new MyCustomBox()
     *      )
     *      .add(..more stuff..)
     *  );
     *  }</pre>
     *  <br>
     *
     * @param component The box component type for which a builder should be created.
     * @param <B> THe type parameter defining the concrete {@link JBox} type.
     * @return A builder for the provided box component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <B extends JBox> UIForBox<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JPanel.class);
        return new UIForBox<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a {@link JBox} instance,
     *  which is a general purpose component wrapper type with the following properties:
     *  <ul>
     *      <li>
     *          It is transparent, meaning that it does not paint its background.
     *      </li>
     *      <li>
     *          The default layout manager is a {@link MigLayout}.
     *      </li>
     *      <li>
     *          The insets (the spaces between the wrapped component and the box's border)
     *          are all set to zero.
     *      </li>
     *      <li>
     *          The gap sizes between the components added to the box is set to zero.
     *          So the children of this component will be tightly packed.
     *      </li>
     *  </ul>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     *  This factory method is especially useful for when you simply want to nest components
     *  tightly without having to worry about the layout manager or the background
     *  color covering the background of the parent component.
     *  <br>
     *  Note that you can also emulate the {@link JBox} type with a {@link JPanel} using
     *  {@code UI.panel("ins 0, gap 0").makeNonOpaque()}.
     *
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     */
    public static UIForBox<JBox> box() {
        return new UIForBox<>(new BuilderState<>(UI.Box.class, UI.Box::new));
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( String attr ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        if ( attr.isEmpty() ) attr = "ins 0";
        else if ( !attr.contains("ins") ) attr += ", ins 0";
        return box().withLayout(attr);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( String attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        if (attr.isEmpty()) attr = "ins 0";
        else if (!attr.contains("ins")) attr += ", ins 0";
        return box().withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This factory method is especially useful for when you simply want to nest components
     *  tightly without having to worry about the layout manager or the background
     *  color covering the background of the parent component.
     *  <br>
     *  Note that you can also emulate the {@link JBox} type with a {@link JPanel} using
     *  <pre>{@code
     *      UI.panel(attr, colConstraints, rowConstraints).makeNonOpaque()
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *
     * @param attr The layout attributes.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( String attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        if (attr.isEmpty()) attr = "ins 0";
        else if (!attr.contains("ins")) attr += ", ins 0";
        return box().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints. <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other subcomponents seamlessly...</b>
     *  <p>
     *  <br>
     *  This method allows you to pass a {@link LayoutConstraint} constants as the layout attributes,
     *  which is an instance typically chosen from the {@link UI} class constants
     *  like for example {@link UI#FILL}, {@link UI#FILL_X}, {@link UI#FILL_Y}... <br>
     *  A typical usage example would be: <br>
     *  <pre>{@code
     *      UI.box(UI.FILL_X.and(UI.WRAP(2)))
     *      .add(..)
     *      .add(..)
     *  }</pre>
     *  In this code snippet the creates a {@link JBox} with a {@link MigLayout} as its layout manager
     *  where the box will fill the parent component horizontally and
     *  the components added to the box will be wrapped after every two components.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LayoutConstraint attr ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        return box(attr.toString());
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints. <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other subcomponents seamlessly...</b>
     *  <p>
     *  This method allows you to pass a {@link LayoutConstraint} constants as the layout attributes,
     *  which is an instance typically chosen from the {@link UI} class constants
     *  like for example {@link UI#FILL}, {@link UI#FILL_X}, {@link UI#FILL_Y}... <br>
     *  A typical usage example would be: <br>
     *  <pre>{@code
     *      UI.box(UI.FILL, "[shrink]6[grow]")
     *      .add(..)
     *      .add(..)
     *  }</pre>
     *  In this code snippet that creates a {@link JBox} with a {@link MigLayout} as its layout manager
     *  where the box will fill the parent component horizontally and vertically
     *  and the first column of components will be shrunk to their preferred size
     *  and the second column will grow to fill the available space.
     *  Both columns will have a gap of 6 pixels between them.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LayoutConstraint attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return box(attr.toString(), colConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JBox(new MigLayout(attr, colConstraints, rowConstraints)))
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other subcomponents seamlessly...</b>
     *
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LayoutConstraint attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return box(attr.toString(), colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LC attr ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        return box().withLayout(attr);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This is essentially a convenience method that can also be expressed as: <br>
     *  <pre>{@code
     *      UI.of(new JBox(new MigLayout(attr, colConstraints)))
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other subcomponents seamlessly...</b>
     *
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LC attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return box().withLayout( attr, colConstraints )           ;
    }

    /**
     *  Use this to create a builder for a {@link JBox}, conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This is essentially a convenience method which may also be expressed as: <br>
     *  <pre>{@code
     *      UI.of(new JBox())
     *      .peek( box -> {
     *          box.setLayout(
     *              new MigLayout(
     *                  attr,
     *                  ConstraintParser.parseColumnConstraints(colConstraints),
     *                  ConstraintParser.parseRowConstraints(rowConstraints)
     *              )
     *          )
     *      })
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LC attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return box().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This method allows you to dynamically determine the {@link LayoutConstraint} constants
     *  of the {@link MigLayout} instance, by passing a {@link Val} property which
     *  will be observed and its value passed to the {@link MigLayout} constructor
     *  whenever it changes.
     *  This is in essence a convenience method for:
     *  {@code UI.box().withLayout(attr.viewAsString( it -> it+", ins 0"))}.
     *
     * @param attr The layout attributes property which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( Val<LayoutConstraint> attr ) {
        NullUtil.nullArgCheck(attr, "attr", Val.class);
        NullUtil.nullPropertyCheck(attr, "attr", "Null is not a valid layout attribute.");
        return box().withLayout(attr.view( it -> it.and("ins 0")));
    }

    /**
     *  If you are using builders for your custom {@link JComponent},
     *  implement this to allow the {@link UI} API to call the {@link SwingBuilder#build()}
     *  method for you.
     *
     * @param builder A builder for custom {@link JComponent} types.
     * @param <T> The UI component type built by implementations of the provided builder.
     * @return A basic UI builder instance wrapping any {@link JComponent}.
     */
    public static <T extends JComponent> UIForSwing<T> of( SwingBuilder<T> builder )
    {
        NullUtil.nullArgCheck(builder, "builder", SwingBuilder.class);
        return new UIForSwing<>(new BuilderState<>((Class<T>) JComponent.class, ()->{
            try {
                return builder.build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    /**
     *  If you are using builders for custom {@link JMenuItem} components,
     *  implement this to allow the {@link UI} API to call the {@link SwingBuilder#build()}
     *  method for you.
     *
     * @param builder A builder for custom {@link JMenuItem} types.
     * @param <M> The {@link JMenuItem} type built by implementations of the provided builder.
     * @return A builder instance for a {@link JMenuItem}, which enables fluent method chaining.
     */
    public static <M extends JMenuItem> UIForMenuItem<M> of( MenuBuilder<M> builder )
    {
        NullUtil.nullArgCheck(builder, "builder", MenuBuilder.class);
        return new UIForMenuItem<>(new BuilderState<>(builder.build()));
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JPopupMenu} UI component.
     *
     * @param popup The new {@link JPopupMenu} instance which ought to be part of the Swing UI.
     * @param <P> The concrete type of this new component.
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static <P extends JPopupMenu> UIForPopup<P> of( P popup )
    {
        NullUtil.nullArgCheck(popup, "popup", JPopupMenu.class);
        return new UIForPopup<>(new BuilderState<>(popup));
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JPopupMenu} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPopupMenu())}.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static UIForPopup<JPopupMenu> popupMenu() {
        return new UIForPopup<>(new BuilderState<>(UI.PopupMenu.class, UI.PopupMenu::new));
    }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *
     * @param separator The new {@link JSeparator} instance which ought to be part of the Swing UI.
     * @param <S> The concrete type of this new component.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static <S extends JSeparator> UIForSeparator<S> of( S separator )
    {
        NullUtil.nullArgCheck(separator, "separator", JSeparator.class);
        return new UIForSeparator<>(new BuilderState<>(separator));
    }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *  This is in essence a convenience method for {@code UI.of(new JSeparator())}.
     *
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator<JSeparator> separator() {
        return new UIForSeparator<>(new BuilderState<>(JSeparator.class, UI.Separator::new));
    }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *  This is in essence a convenience method for {@code UI.of(new JSeparator(JSeparator.VERTICAL))}.
     *
     * @param align The alignment of the separator which may either be horizontal or vertical.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator<JSeparator> separator( UI.Align align ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return separator().withOrientation(align);
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JSeparator} whose
     *  alignment is dynamically determined based on a provided property.
     *
     * @param align The alignment property of the separator which may either be horizontal or vertical.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator<JSeparator> separator( Val<UI.Align> align ) {
        NullUtil.nullArgCheck(align, "align", Val.class);
        return separator().withOrientation(align);
    }

    /**
     *  This returns a {@link JButton} swing tree builder.
     *
     * @param component The button component which ought to be wrapped by the swing tree UI builder.
     * @param <T> The concrete type of this new component.
     * @return A basic UI {@link JButton} builder instance.
     */
    public static <T extends AbstractButton> UIForButton<T> of( T component )
    {
        NullUtil.nullArgCheck(component, "component", AbstractButton.class);
        return new UIForButton<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component without any text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton())}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button() {
        return new UIForButton<>(new BuilderState<>(UI.Button.class, UI.Button::new));
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component with the provided text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton(String text))}.
     *
     * @param text The text to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( String text ) { return button().withText(text); }

    /**
     *  Create a builder for the {@link JButton} UI component where the text of the provided
     *  property is dynamically displayed on top.
     *
     * @param text The text property to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Val<String> text ) {
        NullUtil.nullArgCheck( text, "text", Val.class );
        return button().withText(text);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).peek( it -> it.setIcon(icon) )}.
     *
     * @param icon The icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().withIcon(icon);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *
     * @param icon The icon to be displayed on top of the button.
     * @param fit The fit mode of the icon.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( ImageIcon icon, UI.FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(fit, "fit", UI.FitComponent.class);
        return button().withIcon(icon, fit);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *
     * @param icon The desired icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map(UI::button).orElseGet(UI::button);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *
     * @param icon The desired icon to be displayed on top of the button.
     * @param fit The fit mode of the icon.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon, UI.FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(fit, "fit", UI.FitComponent.class);
        return icon.find().map( it -> button(it, fit) ).orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top which should be scaled to the provided dimensions.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).peek( it -> it.setIcon(icon) )}.
     *
     * @param width The width the icon should be scaled to.
     * @param height The height the icon should be scaled to.
     * @param icon The icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, ImageIcon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().withIcon(width, height, icon);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top which should be scaled to the provided dimensions.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *
     * @param width The width the icon should be scaled to.
     * @param height The height the icon should be scaled to.
     * @param icon The desired icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map( it -> button(width, height, it) ).orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a dynamically displayed icon on top.
     *  <p>
     *  Note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed. Therefore, they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in the case of unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behavior of your view model.
     *
     * @param icon The icon property whose value ought to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> buttonWithIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon");
        return button().withIcon(icon);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top.
     *
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon, Icon onHover ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(onHover, "onHover", Icon.class);
        return button(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top.
     *  The icons are determined based on the provided {@link IconDeclaration}
     *  instances which is conceptually merely a resource path to the icons.
     *
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon, IconDeclaration onHover ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(onHover, "onHover", IconDeclaration.class);
        return icon.find()
                .flatMap( it -> onHover.find().map( it2 -> button(it, it2) ) )
                .orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top
     *  which should both be scaled to the provided dimensions.
     *
     * @param width The width the icons should be scaled to.
     * @param height The height the icons should be scaled to.
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, ImageIcon icon, ImageIcon onHover ) {
        NullUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        NullUtil.nullArgCheck(onHover, "onHover", ImageIcon.class);
        icon    = _fitTo(width, height, icon);
        onHover = _fitTo(width, height, onHover);
        return button(icon, onHover, onHover);
    }


    private static ImageIcon _fitTo( int width, int height, ImageIcon icon ) {
        if ( icon instanceof SvgIcon ) {
            SvgIcon svgIcon = (SvgIcon) icon;
            svgIcon = svgIcon.withIconWidth(width);
            return svgIcon.withIconHeight(height);
        }
        else if ( icon instanceof ScalableImageIcon ) {
            return ((ScalableImageIcon)icon).withSize(Size.of(width, height));
        }
        else if ( width != icon.getIconWidth() || height != icon.getIconHeight() ) {
            return ScalableImageIcon.of(Size.of(width, height), icon);
        }
        return icon;
    }


    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top
     *  which should both be scaled to the provided dimensions.
     *  The icons are determined based on the provided {@link IconDeclaration}
     *  instances which is conceptually merely a resource path to the icons.
     *
     * @param width The width the icons should be scaled to.
     * @param height The height the icons should be scaled to.
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, IconDeclaration icon, IconDeclaration onHover ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(onHover, "onHover", IconDeclaration.class);
        return icon.find()
                .flatMap( it -> onHover.find().map( it2 -> button(width, height, it, it2) ) )
                .orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default, an on-hover and an on-press icon displayed on top.
     *  This is in essence a convenience method for:
     *  <pre>{@code
     *      UI.of(new JButton()).peek( it -> {
     *          it.setIcon(icon);
     *          it.setRolloverIcon(onHover);
     *          it.setPressedIcon(onPress);
     *      })
     *  }</pre>
     *
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @param onPress The pressed icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon, Icon onHover, Icon onPress ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(onHover, "onHover", Icon.class);
        NullUtil.nullArgCheck(onPress, "onPress", Icon.class);
        return button().peek(it -> it.setIcon(icon) )
                .peek(it -> it.setRolloverIcon(onHover) )
                .peek(it -> it.setPressedIcon(onPress) );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default, an on-hover and an on-press icon displayed on top.
     *  The icons are determined based on the provided {@link IconDeclaration}
     *  instances which is conceptually merely a resource paths to the icons.
     *
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @param onPress The pressed icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon, IconDeclaration onHover, IconDeclaration onPress ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(onHover, "onHover", IconDeclaration.class);
        NullUtil.nullArgCheck(onPress, "onPress", IconDeclaration.class);
        return icon.find()
                .flatMap( it -> onHover.find().flatMap( it2 -> onPress.find().map( it3 -> button(it, it2, it3) ) ) )
                .orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JSplitButton} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSplitButton())}.
     *
     * @param splitButton The split button component which ought to be wrapped by the swing tree UI builder.
     * @param <B> The concrete type of this new component.
     * @return A builder instance for a {@link JSplitButton}, which enables fluent method chaining.
     */
    public static <B extends JSplitButton> UIForSplitButton<B> of( B splitButton ) {
        NullUtil.nullArgCheck(splitButton, "splitButton", JSplitButton.class);
        return new UIForSplitButton<>(new BuilderState<>(splitButton));
    }

    /**
     *  Use this to build {@link JSplitButton}s with custom text displayed ont top.
     *  The {@link JSplitButton} wrapped by the returned builder can be populated
     *  with {@link JMenuItem}s like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Displayed on button!")
     *      .add(UI.splitItem("first"))
     *      .add(UI.splitItem("second").onButtonClick( it -> ... ))
     *      .add(UI.splitItem("third"))
     *  }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JSplitButton}
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     */
    public static UIForSplitButton<JSplitButton> splitButton( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForSplitButton<>(new BuilderState<>(JSplitButton.class, UI.SplitButton::new))
                .withText(text);
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type, and the click event is
     *  handles by an {@link Event} instance. <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *      private Event clickEvent = Event.of(()->{ ... }
     *
     *      public Var<Size> selection() { return selection; }
     *      public Event clickEvent() { return clickEvent; }
     *
     *      // In your view:
     *      UI.splitButton(vm.selection(), vm.clickEvent())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      For the text displayed on the split button, the selected enum state
     *      will be converted to strings based on the {@link Object#toString()}
     *      method. If you want to customize how they are displayed (e.g.
     *      so that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      it is recommended to use {@link #splitButton(Var, Event, Function)} instead.
     *      But you can also override the {@link Object#toString()} method in your enum. </i><br>
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param clickEvent The {@link Event} which will be fired when the user clicks on the button.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     * @param <E> The type of the {@link Enum} representing the selectable options.
     */
    public static <E extends Enum<E>> UIForSplitButton<JSplitButton> splitButton( Var<E> selection, Event clickEvent ) {
        return splitButton("").withSelection(selection, clickEvent);
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type, and the click event is
     *  handles by an {@link Event} instance. <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *      private Event clickEvent = Event.of(()->{ ... }
     *
     *      public Var<Size> selection() { return selection; }
     *      public Event clickEvent() { return clickEvent; }
     *
     *      // In your view:
     *      UI.splitButton(vm.selection(), vm.clickEvent(), it -> it.toString().toLowerCase())
     * }</pre>
     * <p>
     * Note that the text displayed on the split button is based on the
     * supplied {@link Function} which converts the enum instances to strings.
     * In this function you may, for example, convert 'Size.LARGE' to 'Large' instead of 'LARGE'.
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param clickEvent The {@link Event} which will be fired when the user clicks on the button.
     * @param labelProvider A function which converts the enum instances to strings.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     * @param <E> The type of the {@link Enum} representing the selectable options.
     */
    public static <E extends Enum<E>> UIForSplitButton<JSplitButton> splitButton( Var<E> selection, Event clickEvent, Function<E, String> labelProvider ) {
        return splitButton("").withSelection(selection, clickEvent, labelProvider);
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type. <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.splitButton(vm.selection())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      The text displayed on the button is based on the {@link Object#toString()}
     *      method of the enum instances. If you want to customize how they are displayed
     *      (i.e. so that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      it is recommended to use {@link #splitButton(Var, Function)} instead. But you can
     *      also choose to override the {@link Object#toString()} method in your enum
     *      to achieve the same effect. </i><br>
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     * @param <E> The type of the {@link Enum} representing the selectable options.
     */
    public static <E extends Enum<E>> UIForSplitButton<JSplitButton> splitButton( Var<E> selection ) {
        return splitButton("").withSelection(selection);
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type. <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.splitButton(vm.selection(), it -> it.toString().toLowerCase())
     * }</pre>
     * <p>
     * Note that the text displayed on the split button is based on the
     * supplied {@link Function} which converts the enum instances to strings.
     * In this function you may for example convert 'Size.LARGE' to 'Large' instead of 'LARGE'.
     * In the above example we simply convert the enum instances to lower case strings,
     * but you can customize this function to your liking.
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param labelProvider A function which converts the enum instances to strings.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     * @param <E> The type of the {@link Enum} representing the selectable options.
     */
    public static <E extends Enum<E>> UIForSplitButton<JSplitButton> splitButton( Var<E> selection, Function<E, String> labelProvider ) {
        return splitButton("").withSelection(selection, labelProvider);
    }

    /**
     *  Use this to add entries to the {@link JSplitButton} by
     *  passing {@link SplitItem} instances to {@link UIForSplitButton} builder like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Button")
     *      .add(UI.splitItem("first"))
     *      .add(UI.splitItem("second"))
     *      .add(UI.splitItem("third"))
     *  }</pre>
     *  You can also use the {@link SplitItem} wrapper class to wrap
     *  useful action lambdas for the split item.
     *
     * @param text The text displayed on the {@link JMenuItem} exposed by the {@link JSplitButton}s {@link JPopupMenu}.
     * @return A new {@link SplitItem} wrapping a simple {@link JMenuItem}.
     */
    public static SplitItem<JMenuItem> splitItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return SplitItem.of(text);
    }

    /**
     *  Use this to add property-bound entries to the {@link JSplitButton} by
     *  passing {@link SplitItem} instances to {@link UIForSplitButton} builder like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Button")
     *      .add(UI.splitItem(viewModel.getFirstButtonName()))
     *      .add(UI.splitItem(viewModel.getSecondButtonName()))
     *      .add(UI.splitItem(viewModel.getThirdButtonName()))
     *  }</pre>
     *  You can also use the {@link SplitItem} wrapper class to wrap
     *  useful action lambdas for the split item.
     *
     * @param text The text property to dynamically display text on the {@link JMenuItem} exposed by the {@link swingtree.components.JSplitButton}s {@link JPopupMenu}.
     * @return A new {@link SplitItem} wrapping a simple {@link JMenuItem}.
     */
    public static SplitItem<JMenuItem> splitItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return SplitItem.of(text);
    }

    /**
     *  Use this to add radio item entries to the {@link swingtree.components.JSplitButton} by
     *  passing {@link SplitItem} instances to {@link UIForSplitButton} builder like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Button")
     *      .add(UI.splitRadioItem("first"))
     *      .add(UI.splitRadioItem("second"))
     *      .add(UI.splitRadioItem("third"))
     *  }</pre>
     *  You can also use the {@link SplitItem} wrapper class to wrap
     *  useful action lambdas for the split item.
     *
     * @param text The text displayed on the {@link JRadioButtonMenuItem} exposed by the {@link swingtree.components.JSplitButton}s {@link JPopupMenu}.
     * @return A new {@link SplitItem} wrapping a simple {@link JRadioButtonMenuItem}.
     */
    public static SplitItem<JRadioButtonMenuItem> splitRadioItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        JRadioButtonMenuItem item = new UI.RadioButtonMenuItem();
        item.setText(text);
        return SplitItem.of(item);
    }

    /**
     *  Creates a UI builder for a custom {@link JTabbedPane} type.
     *
     * @param pane The {@link JTabbedPane} type which should be used wrapped.
     * @return This instance, to allow for method chaining.
     * @param <P> The pane type parameter.
     */
    public static <P extends JTabbedPane> UIForTabbedPane<P> of( P pane ) {
        NullUtil.nullArgCheck(pane, "pane", JTabbedPane.class);
        return new UIForTabbedPane<>(new BuilderState<>(pane));
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTabbedPane())}.
     *  To add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab("one").add(UI.panel().add(..)))
     *      .add(UI.tab("two").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("three").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     *
     * @return A builder instance for a new {@link JTabbedPane}, which enables fluent method chaining.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane() {
        return new UIForTabbedPane<>(new BuilderState<>(JTabbedPane.class, UI.TabbedPane::new));
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link UI.Side} applied to the tab buttons
     *  (see {@link JTabbedPane#setTabLayoutPolicy(int)}).
     *  In order to add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *
     *  <pre>{@code
     *      UI.tabbedPane(UI.Side.RIGHT)
     *      .add(UI.tab("first").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param tabsSide The position of the tab buttons which may be {@link UI.Side#TOP}, {@link UI.Side#RIGHT}, {@link UI.Side#BOTTOM}, {@link UI.Side#LEFT}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPosition} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( UI.Side tabsSide ) {
        NullUtil.nullArgCheck(tabsSide, "tabsPosition", UI.Side.class);
        return tabbedPane().withTabPlacementAt(tabsSide);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link UI.OverflowPolicy} and {@link UI.Side} applied to the tab buttons
     *  (see {@link JTabbedPane#setTabLayoutPolicy(int)} and {@link JTabbedPane#setTabPlacement(int)}).
     *  To add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(UI.Side.LEFT, UI.OverflowPolicy.WRAP)
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param tabsSide The position of the tab buttons which may be {@link UI.Side#TOP}, {@link UI.Side#RIGHT}, {@link UI.Side#BOTTOM}, {@link UI.Side#LEFT}.
     * @param tabsPolicy The overflow policy of the tab buttons which can either be {@link UI.OverflowPolicy#SCROLL} or {@link UI.OverflowPolicy#WRAP}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPosition} or {@code tabsPolicy} are {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( UI.Side tabsSide, UI.OverflowPolicy tabsPolicy ) {
        NullUtil.nullArgCheck(tabsSide, "tabsPosition", UI.Side.class);
        NullUtil.nullArgCheck(tabsPolicy, "tabsPolicy", UI.OverflowPolicy.class);
        return tabbedPane().withTabPlacementAt(tabsSide).withOverflowPolicy(tabsPolicy);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link UI.OverflowPolicy} applied to the tab buttons (see {@link JTabbedPane#setTabLayoutPolicy(int)}).
     *  To add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(UI.OverflowPolicy.SCROLL)
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param tabsPolicy The overflow policy of the tab button which can either be {@link UI.OverflowPolicy#SCROLL} or {@link UI.OverflowPolicy#WRAP}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPolicy} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( UI.OverflowPolicy tabsPolicy ) {
        NullUtil.nullArgCheck(tabsPolicy, "tabsPolicy", UI.OverflowPolicy.class);
        return tabbedPane().withTabPlacementAt(UI.Side.TOP).withOverflowPolicy(tabsPolicy);
    }


    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@code selectedIndex} property which should be determined the
     *  tab selection of the {@link JTabbedPane} dynamically.
     *  To add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(vm.getSelectionIndex())
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *  Note that contrary to method {@link #tabbedPane(Var)}, this method receives a {@link Val}
     *  property which may not be changed by the GUI user. If you want to allow the user to change
     *  the selection index property state, use {@link #tabbedPane(Var)} instead.
     *
     * @param selectedIndex The index of the tab to select.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code selectedIndex} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( Val<Integer> selectedIndex ) {
        return tabbedPane().withSelectedIndex(selectedIndex);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@code selectedIndex} property which should determine the
     *  tab selection of the {@link JTabbedPane} dynamically.
     *  To add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(vm.getSelectionIndex())
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param selectedIndex The index of the tab to select.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code selectedIndex} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( Var<Integer> selectedIndex ) {
        return tabbedPane().withSelectedIndex(selectedIndex);
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param title The text displayed on the tab button.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code title} is {@code null}.
     */
    public static Tab tab( String title ) {
        NullUtil.nullArgCheck(title, "title", String.class);
        return new Tab(null, null, Val.of(title), null, null, null, null, null, null);
    }

    /**
     *  A factory method producing a {@link Tab} instance with the provided {@code title} property
     *  which can dynamically change the title of the tab button.
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab(property1).add(UI.panel().add(..)))
     *      .add(UI.tab(property2).withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab(property3).withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param title The text property dynamically changing the title of the tab button when the property changes.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code title} is {@code null}.
     */
    public static Tab tab( Val<String> title ) {
        NullUtil.nullArgCheck(title, "title", Val.class);
        return new Tab(null, null, title, null, null, null, null, null, null);
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab(new JButton("X")).add(UI.panel().add(..)))
     *      .add(UI.tab(new JLabel("Hi!")).withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab(new JPanel()).add(UI.button("click me")))
     *  }</pre>
     *
     * @param component The component displayed on the tab button.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static Tab tab( JComponent component ) {
        NullUtil.nullArgCheck(component, "component", JComponent.class);
        return new Tab(null, component, null, null, null, null, null, null, null);
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab(UI.button("X")).add(UI.panel().add(..)))
     *      .add(UI.tab(UI.label("Hi!")).withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab(UI.of(...)).withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param builder The builder wrapping the component displayed on the tab button.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code builder} is {@code null}.
     */
    public static Tab tab( UIForAnySwing<?, ?> builder ) {
        NullUtil.nullArgCheck(builder, "builder", UIForAnySwing.class);
        return new Tab(null, builder.get((Class) builder.getType()), null, null, null, null, null, null, null);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenu} instance.
     *
     * @param component The {@link JMenu} component which should be wrapped by the swing tree UI builder designed for menus.
     * @return A builder instance for the provided {@link JMenu}, which enables fluent method chaining.
     * @param <M> The concrete type of the menu.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenu> UIForMenu<M> of( M component ) {
        NullUtil.nullArgCheck(component, "component", JMenu.class);
        return new UIForMenu<>(new BuilderState<>(component));
    }

    /**
     *  A convenient factory method for creating a declarative
     *  builder object for a plain {@link JMenu} component.
     *
     * @return A SwingTree builder node for the {@link JMenu} type.
     */
    public static UIForMenu<JMenu> menu() {
        return new UIForMenu<>(new BuilderState<>(JMenu.class, UI.Menu::new));
    }

    /**
     *  A convenient factory method for creating a declarative
     *  builder object for a {@link JMenu} component
     *  initialized with the provided text.
     *
     * @param text The text displayed on the menu.
     * @return A SwingTree builder node for the {@link JMenu} type.
     */
    public static UIForMenu<JMenu> menu( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return menu().withText(text);
    }

    /**
     *  A convenient factory method for creating a declarative
     *  builder object for a {@link JMenu} component
     *  bound to the supplied text property.
     *  Note that whenever the text property changes, the text
     *  displayed on the menu will be updated accordingly.
     *
     * @param text The text property dynamically changing the text displayed on the menu.
     * @return A SwingTree builder node for the {@link JMenu} type.
     */
    public static UIForMenu<JMenu> menu( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return menu().withText(text);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenuItem} instance.
     *
     * @param component The {@link JMenuItem} component which should be wrapped by the swing tree UI builder designed for menu items.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     * @param <M> The type parameter of the concrete menu item component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenuItem> UIForMenuItem<M> of( M component ) {
        NullUtil.nullArgCheck(component, "component", JMenuItem.class);
        return new UIForMenuItem<>(new BuilderState<>(component));
    }

    /**
     *  A convenient factory method for creating a declarative
     *  builder object for a the {@link JMenuItem} component type.<br>
     *  Menu items are usually passed to {@link JMenu}s or {@link JPopupMenu}s
     *  like so: <br>
     *  <pre>{@code
     *  UI.menu()
     *  .add(UI.menuItem("Delete").onClick( it->{..} ))
     *  .add(UI.menuItem("Save").onClick( it->{..} ))
     *  .add(UI.menuItem("Exit").onClick( it->{..} ))
     *  }</pre>
     *  See also {@link #menu()} and {@link #menu(String)}
     *  or {@link #popupMenu()} for related factory methods.
     *
     * @return A SwingTree builder node for the {@link JMenuItem} type.
     */
    public static UIForMenuItem<JMenuItem> menuItem() {
        return new UIForMenuItem<>(new BuilderState<>(JMenuItem.class, UI.MenuItem::new));
    }

    /**
     *  This factory method creates a {@link JMenu} with the provided text
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.menuItem("Delete").onClick( it -> {..} ))
     *    .add(UI.menuItem("Edit").onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return menuItem().withText(text);
    }

    /**
     *  This factory method creates a {@link JMenuItem} with the provided text property
     *  bound to the menu item. So when the property state changes to a different text,
     *  then so does the text displayed on the menu item. <br>
     *  A {@link JMenuItem} is typically used as part of {@link JMenu}s or {@link JPopupMenu}s.
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *  UI.popupMenu()
     *  .add(UI.menuItem(viewModel.actionName1()).onClick( it -> {..} ))
     *  .add(UI.menuItem(viewModel.actionName2()).onClick( it -> {..} ))
     *  }</pre>
     *
     *
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return menuItem().withText(text);
    }

    /**
     *  Use this factory method to create a {@link JMenuItem} with the
     *  provided text and default icon. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.menuItem("Hello", UI.icon("path/to/icon.png"))
     *    .withTip("I give info!")
     *    .onClick( it -> {...} )
     *  }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return menuItem()
                .withText(text)
                .withIcon(icon);
    }

    /**
     *  Use this factory method to create a {@link JMenuItem} with the
     *  provided text and default icon based on the provided {@link IconDeclaration}. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.menuItem("Hello", Icons.MY_ICON)
     *    .withTip("I give info!")
     *    .onClick( it -> {...} )
     *  }</pre>
     *  Note that a {@link JMenuItem} is typically used as part of {@link JMenu}s or {@link JPopupMenu}s.
     *  See also {@link #menu()}, {@link #menu(String)} or {@link #popupMenu()} for related factory methods.
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text, IconDeclaration icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return menuItem()
                .withText(text)
                .withIcon(icon);
    }

    /**
     *  A factory method for creating a {@link JMenuItem} with an icon and the supplied text
     *  property uni-directionally bound to the menu item. <br>
     *  When the text property changes, the text displayed on the menu item will be updated accordingly.
     *
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return menuItem()
                .withText(text)
                .withIcon(icon);
    }

    /**
     *  A factory method for creating a {@link JMenuItem} with an icon and the supplied text
     *  property uni-directionally bound to the menu item. <br>
     *  When the text property changes in the view model,
     *  the text displayed on the menu item will be updated accordingly.
     *
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text, IconDeclaration icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return menuItem()
                .withText(text)
                .withIcon(icon);
    }

    /**
     *  Allows you to create a menu item with an icon property bound to it.
     *  So when the property state changes to a different icon, then so does the
     *  icon displayed on top of the menu item.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed. Therefore, they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight and error tolerant value-based object that merely
     *  models the resource location of the icon. It can exist even if the target
     *  icon is not yet loaded or does not exist at all.
     *  <p>
     *  This is especially useful when writing unit tests for your view models,
     *  where the icon resources may not be available, but you still want to test
     *  the behavior of your view model.
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text, Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        return menuItem().withText(text).withIcon(icon);
    }

    /**
     *  Allows you to create a menu item with a text property and
     *  an icon property bound to it.
     *  So when the text or con property state changes to a different text or icon, then so does the
     *  text and/or icon displayed on top of the menu item.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed. Therefore, they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight and error tolerant value based object that merely
     *  models the resource location of the icon. It can exist even if the target
     *  icon is not yet loaded or does not exist at all.
     *  <p>
     *  This is especially useful when writing unit tests for your view models,
     *  where the icon resources may not be available, but you still want to test
     *  the behavior of your view model.
     *
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text, Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        return menuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to wrap the provided {@link JRadioButtonMenuItem} instance in a SwingTree UI builder.
     *
     * @param radioMenuItem The {@link JRadioButtonMenuItem} instance to be wrapped.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     * @param <M> The type of the {@link JRadioButtonMenuItem} instance to be wrapped.
     */
    public static <M extends JRadioButtonMenuItem> UIForRadioButtonMenuItem<M> of( M radioMenuItem ) {
        NullUtil.nullArgCheck(radioMenuItem, "component", JRadioButtonMenuItem.class);
        return new UIForRadioButtonMenuItem<>(new BuilderState<>(radioMenuItem));
    }

    /**
     *  A factory method to create a plain {@link JRadioButtonMenuItem} instance. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem().onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem().onClick( it -> {..} ))
     *  }</pre>
     *
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem() {
        return new UIForRadioButtonMenuItem<>(new BuilderState<>(JRadioButtonMenuItem.class, UI.RadioButtonMenuItem::new));
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with the provided text
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem("Delete").onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem("Edit").onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return radioButtonMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Val.of("Delete")).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem(Val.of("Edit")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JRadioButtonMenuItem} dynamically.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return radioButtonMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with the provided text
     *  displayed on the menu button and the provided icon displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem("Delete", UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem("Edit", UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( String text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return radioButtonMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically and the provided icon
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Val.of("Delete"), UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem(Val.of("Edit"), UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JRadioButtonMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */

    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( Val<String> text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return radioButtonMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} bound to a fixed enum value
     *  and a variable enum property which will dynamically select the menu item based on the
     *  equality of the fixed enum value and the variable enum property value. <br>
     *  Consider the following example code:
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Unit.SECONDS, myViewModel.unitProperty()))
     *    .add(UI.radioButtonMenuItem(Unit.MINUTES, myViewModel.unitProperty()))
     *    .add(UI.radioButtonMenuItem(Unit.HOURS,   myViewModel.unitProperty()))
     *  }</pre>
     *  In this example the {@code myViewModel.unitProperty()} is a {@link Var} property of
     *  example type {@code Unit}.
     *  A given menu item will be selected if the value of the {@code myViewModel.unitProperty()}
     *  is equal to the first enum value passed to the factory method.
     *  This first enum will also be used as the text of the menu item through the {@code toString()}.
     *
     * @param state The fixed enum value which will be used as the text of the menu item and
     * @param property The variable enum property which will be used to select the menu item.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     * @param <E> The type of the enum.
     * @throws IllegalArgumentException if {@code state} or {@code property} are {@code null}.
     */
    public static <E extends Enum<E>> UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( E state, Var<E> property ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(property, "property", Var.class);
        return radioButtonMenuItem(state.toString()).isSelectedIf(state, property);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with some custom text and a boolean property,
     *  dynamically determining whether the radio-button-based menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected1 = Var.of(false);
     *    Var<Boolean> isSelected2 = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem("Make Coffee", isSelected1).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem("Make Tea", isSelected2).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the boolean property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getIsSelectedProperty()}).
     *
     * @param text The text which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determines whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( String text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return radioButtonMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with some custom text and a boolean property,
     *  dynamically determining whether the radio-button-based menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected1 = Var.of(false);
     *    Var<Boolean> isSelected2 = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Val.of("Make Coffee"), isSelected1).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem(Val.of("Make Tea"), isSelected2).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the {@code String} and {@code boolean}
     * properties from a view model object through
     * plain old getter methods
     * (e.g. {@code myViewModel.getTextProperty()} and {@code myViewModel.getIsSelectedProperty()}).
     *
     * @param text The text property whose text should dynamically be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determines whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( Val<String> text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return radioButtonMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  A factory method to wrap the provided {@link JCheckBoxMenuItem} instance in a SwingTree UI builder.
     *
     * @param checkBoxMenuItem The {@link JCheckBoxMenuItem} instance to be wrapped.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     * @param <M> The type of the {@link JCheckBoxMenuItem} instance to be wrapped.
     */
    public static <M extends JCheckBoxMenuItem> UIForCheckBoxMenuItem<M> of( M checkBoxMenuItem ) {
        NullUtil.nullArgCheck(checkBoxMenuItem, "component", JCheckBoxMenuItem.class);
        return new UIForCheckBoxMenuItem<>(new BuilderState<>(checkBoxMenuItem));
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} without text
     *  displayed on top of the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem().onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem().onClick( it -> {..} ))
     * }</pre>
     *
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem() {
        return new UIForCheckBoxMenuItem<>(new BuilderState<>(JCheckBoxMenuItem.class, UI.CheckBoxMenuItem::new));
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} with the provided text
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem("Delete").onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem("Edit").onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return checkBoxMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem(Val.of("Delete")).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem(Val.of("Edit")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JCheckBoxMenuItem} dynamically.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return checkBoxMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} with the provided text
     *  displayed on the menu button and the provided icon displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem("Delete", UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem("Edit", UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( String text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return checkBoxMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} with some custom text and a boolean property,
     *  dynamically determining whether the menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem("Delete", isSelected).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem("Edit", isSelected).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the boolean property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getIsSelectedProperty()}).
     *
     * @param text The text which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determine whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( String text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return checkBoxMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically and the provided icon
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem(Val.of("Delete"), UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem(Val.of("Edit"), UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JCheckBoxMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( Val<String> text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return checkBoxMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically and the provided boolean property,
     *  dynamically determining whether the menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem(Val.of("Delete"), isSelected).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem(Val.of("Edit"), isSelected).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JCheckBoxMenuItem} dynamically.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determines whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( Val<String> text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return checkBoxMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  Use this to create a builder for the provided {@link JToolBar} instance.
     *  Using method chaining, you can populate the {@link JToolBar} by like so: <br>
     *  <pre>{@code
     *    UI.of(myToolBar)
     *    .add(UI.button("X"))
     *    .add(UI.button("Y"))
     *    .add(UI.button("Z"))
     *    .addSeparator()
     *    .add(UI.button("A"))
     *  }</pre>
     *  <br>
     * @param component The {@link JToolBar} instance to be wrapped.
     * @param <T> The type of the {@link JToolBar} instance to be wrapped.
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <T extends JToolBar> UIForToolBar<T> of( T component ) {
        NullUtil.nullArgCheck(component, "component", JToolBar.class);
        return new UIForToolBar<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JToolBar} instance.
     *  Use method chaining to add buttons or other components to a {@link JToolBar} by
     *  passing them to {@link UIForToolBar} builder like so: <br>
     *  <pre>{@code
     *    UI.toolBar()
     *    .add(UI.button("X"))
     *    .add(UI.button("Y"))
     *    .add(UI.button("Z"))
     *    .addSeparator()
     *    .add(UI.button("A"))
     *  }</pre>
     *  <br>
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static UIForToolBar<JToolBar> toolBar() {
        return new UIForToolBar<>(new BuilderState<>(JToolBar.class, UI.ToolBar::new));
    }

    /**
     *  A factory method for creating a {@link JToolBar} instance where
     *  the provided {@link UI.Align} enum defines the orientation of the {@link JToolBar}.
     *
     * @param align The {@link UI.Align} enum which defines the orientation of the {@link JToolBar}.
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForToolBar<JToolBar> toolBar( UI.Align align ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return toolBar().withOrientation(align);
    }

    /**
     *  A factory method for creating a {@link JToolBar} instance where
     *  the provided {@link Val} property dynamically defines
     *  the orientation of the {@link JToolBar}
     *
     * @param align The {@link Val} property which dynamically defines the orientation of the {@link JToolBar}.
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForToolBar<JToolBar> toolBar( Val<UI.Align> align ) {
        NullUtil.nullArgCheck(align, "align", Val.class);
        return toolBar().withOrientation(align);
    }

    /**
     *  Use this to create a builder for the provided {@link JScrollPane} component.
     *
     * @param component The {@link JScrollPane} component which should be represented by the returned builder.
     * @param <P> The type parameter defining the concrete scroll pane type.
     * @return A {@link UIForScrollPane} builder representing the provided component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JScrollPane> UIForScrollPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPane<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPane())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPane()
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *  }</pre>
     *
     * @return A builder instance for a new {@link JScrollPane}, which enables fluent method chaining.
     * @see #scrollPane(Configurator) for a more advanced version of this method
     *       where you can configure how the contents of the scroll pane should behave
     *       in the viewport.
     */
    public static UIForScrollPane<JScrollPane> scrollPane() {
        return new UIForScrollPane<>(new BuilderState(UI.ScrollPane.class, UI.ScrollPane::new));
    }

    /**
     * Allows you to create a declarative builder for the {@link JScrollPane} component type,
     * where you can also configure how the contained component should behave in the scroll pane viewport
     * through a {@link Configurator} lambda. <br>
     * The configurator receives a {@link ScrollableComponentDelegate} on which you can define
     * properties like the preferred viewport size, unit increment, block increment, and whether the component should
     * fit the width or height of the viewport. <br>
     * <p>
     * Here a short code snippet demonstrating how this factory method
     * is typically used:
     * <pre>{@code
     * UI.panel()
     * .withBorderTitled("Scrollable Panel")
     * .add(
     *     UI.scrollPane(conf -> conf
     *         .prefSize(400, 300)
     *         .unitIncrement(20)
     *         .blockIncrement(50)
     *         .fitWidth(true)
     *         .fitHeight(false)
     *     )
     * )
     * }</pre>
     * Note that these properties are directly translated to an underlying implementation
     * of the {@link Scrollable} interface which will wrap your content component
     * seamlessly in the scroll pane when added using one of the various {@code add} methods. <br>
     * <b>So you do not have to implement the {@link Scrollable} interface yourself!</b><br>
     * <p>
     * The {@link ScrollableComponentDelegate} object passed to the configurator exposes some context
     * information you may find useful when defining its properties. Like for example the current
     * {@link ScrollableComponentDelegate#view()}, which implements the {@link javax.swing.Scrollable}
     * and wraps your {@link ScrollableComponentDelegate#content} component.
     * You can also access the {@link ScrollableComponentDelegate#viewport()} as
     * well as the current {@link #scrollPane()} component itself!<br>
     * <p>
     * Note that the provided {@link Configurator} will be called
     * for every call to a method of the underlying {@link javax.swing.Scrollable}
     * component implementation, so the settings you define in the configurator
     * are updated dynamically based on the context captured by the lambda
     * as well as the involved components, like viewport, view, content and the
     * scroll pane itself.
     *
     * @param configurator A configurator for configuring the scrollable content of the scroll pane.
     * @return A builder instance for a new {@link JScrollPane}, which enables fluent method chaining.
     */
    public static UIForScrollPane<JScrollPane> scrollPane(
        Configurator<ScrollableComponentDelegate> configurator
    ) {
        return new UIForScrollPane<>(new BuilderState(UI.ScrollPane.class, UI.ScrollPane::new), configurator);
    }

    /**
     *  Use this to create a builder for the provided {@link JScrollPanels} component.
     *
     * @param component The {@link JScrollPanels} component which should be represented by the returned builder.
     * @param <P> The type parameter defining the concrete scroll panels type.
     * @return A {@link UIForScrollPanels} builder representing the provided component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JScrollPanels> UIForScrollPanels<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPanels<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPanels} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPanels())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPanels()
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *      .add(UI.label("I am a label!"))
     *      .add(UI.button("I am a button! Click me!"))
     *  }</pre>
     *
     * @return A builder instance for a new {@link JScrollPanels}, which enables fluent method chaining.
     */
    public static UIForScrollPanels<JScrollPanels> scrollPanels() {
        return new UIForScrollPanels<>(new BuilderState<>(JScrollPanels.class, ()->JScrollPanels.of(UI.Align.VERTICAL, new Dimension(100,100))));
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPanels} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPanels())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPanels(UI.Align.HORIZONTAL)
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *      .add(UI.label("I am a label!"))
     *      .add(UI.button("I am a button! Click me!"))
     *  }</pre>
     *
     * @param align The alignment of the scroll panels.
     * @return A builder instance for a new {@link JScrollPanels}, which enables fluent method chaining.
     */
    public static UIForScrollPanels<JScrollPanels> scrollPanels( UI.Align align ) {
        return new UIForScrollPanels<>(new BuilderState<>(JScrollPanels.class, ()->JScrollPanels.of(align, null)));
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPanels} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPanels())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPanels(UI.Align.HORIZONTAL, new Dimension(100,100))
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *      .add(UI.label("I am a label!"))
     *      .add(UI.button("I am a button! Click me!"))
     *  }</pre>
     *
     * @param align The alignment of the scroll panels.
     * @param size The size of the scroll panels.
     * @return A builder instance for a new {@link JScrollPanels}, which enables fluent method chaining.
     */
    public static UIForScrollPanels<JScrollPanels> scrollPanels(UI.Align align, Dimension size) {
        return new UIForScrollPanels<>(new BuilderState<>(JScrollPanels.class, ()->JScrollPanels.of(align, size)));
    }

    /**
     *  Use this to create a builder for the provided {@link JSplitPane} instance.
     *
     * @param component The {@link JSplitPane} instance to create a builder for.
     * @param <P> The type of the {@link JSplitPane} instance.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JSplitPane> UIForSplitPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JSplitPane.class);
        return new UIForSplitPane<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JSplitPane} instance
     *  based on the provided alignment enum determining how
     *  the split itself should be aligned. <br>
     *  You can create a simple split pane based UI like so: <br>
     *  <pre>{@code
     *      UI.splitPane(UI.Align.HORIZONTAL) // The split bar will be horizontal
     *      .withDividerAt(50)
     *      .add(UI.panel().add(...)) // top
     *      .add(UI.scrollPane().add(...)) // bottom
     *  }</pre>
     *
     * @param align The alignment determining if the {@link JSplitPane} split bar is aligned vertically or horizontally.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForSplitPane<JSplitPane> splitPane( UI.Align align ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return new UIForSplitPane<>(new BuilderState<>(JSplitPane.class, ()->new UI.SplitPane(align)))
                .withOrientation(align);
    }

    /**
     *  Use this to create a builder for a new {@link JSplitPane} instance
     *  based on the provided alignment property determining how
     *  the split itself should be aligned. <br>
     *  You can create a simple split pane based UI like so: <br>
     *  <pre>{@code
     *    UI.splitPane(viewModel.getAlignment())
     *    .withDividerAt(50)
     *    .add(UI.panel().add(...)) // top
     *    .add(UI.scrollPane().add(...)) // bottom
     *  }</pre>
     *  <br>
     *  The split pane will be updated whenever the provided property changes.
     *  <br>
     *  <b>Note:</b> The provided property must not be {@code null}!
     *  Otherwise, an {@link IllegalArgumentException} will be thrown.
     *  <br>
     * @param align The alignment determining if the {@link JSplitPane} split bar is aligned vertically or horizontally.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForSplitPane<JSplitPane> splitPane( Val<UI.Align> align ) {
        NullUtil.nullArgCheck(align, "align", Val.class);
        NullUtil.nullPropertyCheck(align, "align", "Null is not a valid alignment.");
        return new UIForSplitPane<>(new BuilderState<>(JSplitPane.class, ()->new UI.SplitPane(align.get())))
                .withOrientation(align);
    }

    /**
     *  Use this to create a builder for the provided {@link JEditorPane} instance.
     *
     * @param component The {@link JEditorPane} instance to create a builder for.
     * @param <P> The type of the {@link JEditorPane} instance.
     * @return A builder instance for the provided {@link JEditorPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JEditorPane> UIForEditorPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JEditorPane.class);
        return new UIForEditorPane<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JEditorPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JEditorPane())}.
     *
     * @return A builder instance for a new {@link JEditorPane}, which enables fluent method chaining.
     */
    public static UIForEditorPane<JEditorPane> editorPane() {
        return new UIForEditorPane<>(new BuilderState<>(JEditorPane.class, UI.EditorPane::new));
    }

    /**
     *  Use this to create a builder for the provided {@link JTextPane} instance.
     *
     * @param component The {@link JTextPane} instance to create a builder for.
     * @param <P> The type of the {@link JTextPane} instance.
     * @return A builder instance for the provided {@link JTextPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JTextPane> UIForTextPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JTextPane.class);
        return new UIForTextPane<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JTextPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextPane())}.
     *
     * @return A builder instance for a new {@link JTextPane}, which enables fluent method chaining.
     */
    public static UIForTextPane<JTextPane> textPane() {
        return new UIForTextPane<>(new BuilderState<>(UI.TextPane.class, UI.TextPane::new));
    }

    /**
     *  Use this to create a builder for the provided {@link JSlider} instance.
     *
     * @param component The {@link JSlider} instance to create a builder for.
     * @param <S> The type of the {@link JSlider} instance.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <S extends JSlider> UIForSlider<S> of( S component ) {
        NullUtil.nullArgCheck(component, "component", JSlider.class);
        return new UIForSlider<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JSlider} instance
     *  based on tbe provided alignment type determining if
     *  the slider will be aligned vertically or horizontally.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     */
    public static UIForSlider<JSlider> slider( UI.Align align ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align);
    }

    /**
     *  Use this to create a builder for a new {@link JSlider} instance
     *  based on the provided alignment property which dynamically
     *  determines if the property is aligned vertically or horizontally.
     *
     * @param align The alignment property determining if the {@link JSlider} aligns vertically or horizontally.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if the {@code align} property is {@code null}.
     *
     * @see JSlider#setOrientation
     */
    public static UIForSlider<JSlider> slider( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align);
    }

    /**
     *  Use this to create a builder for a new {@link JSlider} instance
     *  based on tbe provided alignment type, min slider value and max slider value.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     */
    public static UIForSlider<JSlider> slider( UI.Align align, int min, int max ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align)
                .withMin(min)
                .withMax(max)
                .withValue((min + max) / 2);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and initial values.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @param value  the initial value of the slider
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static UIForSlider<JSlider> slider( UI.Align align, int min, int max, int value ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align)
                .withMin(min)
                .withMax(max)
                .withValue(value);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and dynamic value. <br>
     * The slider will be updated whenever the provided property changes.
     * But note that the property is of the read only {@link Val} type,
     * which means that when the user moves the slider, the property will not be updated.
     * <br>
     * If you want bidirectional binding, use {@link #slider(UI.Align, Number, Number, Var)}
     * instead of this method.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @param value The property holding the value of the slider
     * @param <N> The type of the number used for the slider values.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static <N extends Number> UIForSlider<JSlider> slider( UI.Align align, N min, N max, Val<N> value ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        NullUtil.nullPropertyCheck(value, "value", "The state of the slider should not be null!");
        Objects.requireNonNull(min, "The minimum value of the slider should not be null!");
        Objects.requireNonNull(max, "The maximum value of the slider should not be null!");
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align)
                ._withBinding(Val.of(min), Val.of(max), value, false);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and dynamic value property.
     * The property will be updated whenever the user
     * moves the slider and the slider will be updated whenever
     * the property changes in your code (see {@link Var#set(Object)}).
     *
     * @param <N> The type of the number used for the slider values.
     *            This may be {@link Integer}, {@link Double}, {@link Float}, {@link Long} or any other number type.
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @param value The property holding the value of the slider
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static <N extends Number> UIForSlider<JSlider> slider( UI.Align align, N min, N max, Var<N> value ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        NullUtil.nullPropertyCheck(value, "value", "The state of the slider should not be null!");
        Objects.requireNonNull(min, "The minimum value of the slider should not be null!");
        Objects.requireNonNull(max, "The maximum value of the slider should not be null!");
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align)
                ._withBinding(Val.of(min), Val.of(max), value, true);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and value property views.
     * The min, max and value may be updated dynamically
     * when the properties change their values.
     * The current value property item will however not be updated
     * whenever the user moves the slider due to the
     * usage of the read only {@link Val} type.
     *
     * @param <N> The type of the number used for the slider values.
     *            This may be {@link Integer}, {@link Double}, {@link Float}, {@link Long} or any other number type.
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider, which
     *            may be updated dynamically when the property changes.
     * @param max The maximum possible value of the slider, which
     *            may be updated dynamically when the property changes.
     * @param value The property holding the value of the slider which
     *              may be updated dynamically when the property changes
     *              in your code (see {@link Var#set(Object)}).
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     *
     * @throws IllegalArgumentException if {@code align}, {@code min}, {@code max} or {@code value} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static <N extends Number> UIForSlider<JSlider> slider( UI.Align align, Val<N> min, Val<N> max, Val<N> value ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        NullUtil.nullPropertyCheck(min, "min", "The minimum value of the slider should not be null!");
        NullUtil.nullPropertyCheck(max, "max", "The maximum value of the slider should not be null!");
        NullUtil.nullPropertyCheck(value, "value", "The state of the slider should not be null!");
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align)
                ._withBinding(min, max, value, false);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and value property views.
     * The min, max and value may be updated dynamically
     * when the properties change their values in your code.
     * The current value property may also be updated
     * by the user moving the slider due to the
     * usage of the read-write {@link Var} type
     * here in contrast to {@link #slider(UI.Align, Val, Val, Val)}.
     *
     * @param <N> The type of the number used for the slider values.
     *            This may be {@link Integer}, {@link Double}, {@link Float}, {@link Long} or any other number type.
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider, which
     *            may be updated dynamically when the property changes.
     * @param max The maximum possible value of the slider, which
     *            may be updated dynamically when the property changes.
     * @param value The property holding the value of the slider which
     *              may be updated dynamically when the property changes
     *              in your code (see {@link Var#set(Object)}) or when the user moves the slider.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     *
     * @throws IllegalArgumentException if {@code align}, {@code min}, {@code max} or {@code value} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static <N extends Number> UIForSlider<JSlider> slider( UI.Align align, Val<N> min, Val<N> max, Var<N> value ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        NullUtil.nullPropertyCheck(min, "min", "The minimum value of the slider should not be null!");
        NullUtil.nullPropertyCheck(max, "max", "The maximum value of the slider should not be null!");
        NullUtil.nullPropertyCheck(value, "value", "The state of the slider should not be null!");
        return new UIForSlider<>(new BuilderState<>(JSlider.class, UI.Slider::new))
                .withOrientation(align)
                ._withBinding(min, max, value, true);
    }

    /**
     *  Use this to create a builder for the provided {@link JComboBox} instance.<br>
     *  This is useful when you want to write declarative UI with a custom {@link JComboBox} type.
     *  Also see {@link #comboBox()} for a more convenient way to create a new {@link JComboBox} instance.
     *
     * @param component The {@link JComboBox} instance to create a builder for.
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @param <C> The type of the {@link JComboBox} instance.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     */
    public static <E, C extends JComboBox<E>> UIForCombo<E,C> of( C component ) {
        NullUtil.nullArgCheck(component, "component", JComboBox.class);
        return new UIForCombo<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a UI builder for a the {@link JComboBox} component type.
     *  This is similar to {@code UI.of(new JComboBox())}.
     *
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @return A builder instance for a new {@link JComboBox}, which enables fluent method chaining.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox() {
        return new UIForCombo<>(new BuilderState<>(JComboBox.class, UI.ComboBox::new));
    }

    /**
     *  Use this to declare a UI builder for the {@link JComboBox} component type
     *  with the provided array of elements as selectable items. <br>
     *  Note that the user may modify the items in the provided array
     *  (if the combo box is editable), if you do not want that,
     *  consider using {@link UI#comboBoxWithUnmodifiable(Object[])}
     *  or {@link UI#comboBoxWithUnmodifiable(java.util.List)}.
     *
     * @param items The array of elements to be selectable in the {@link JComboBox}.
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    @SafeVarargs
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( E... items ) {
        NullUtil.nullArgCheck(items, "items", Object[].class);
        return ((UIForCombo)comboBox()).withModel(new ArrayBasedComboModel<>(items));
    }

    /**
     *  Use this create a UI declaration for the {@link JComboBox} component type
     *  with the provided array of elements as selectable items
     *  and a lambda function converting each item into a
     *  user-friendly {@link String} representation. <br>
     *  Note that the user may modify the items in the provided array
     *  (if the combo box is editable), if you do not want that,
     *  consider using {@link UI#comboBoxWithUnmodifiable(Object[])}
     *  or {@link UI#comboBoxWithUnmodifiable(java.util.List)}.
     *
     * @param items The array of elements to be selectable in the {@link JComboBox}.
     * @param renderer A lambda function which is used for mapping each entry to a
     *                 user-friendly {@link String} representation.
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( E[] items, Function<E,String> renderer ) {
        NullUtil.nullArgCheck(items, "items", Object[].class);
        return comboBox(items)
                .withTextRenderer(cell -> cell.entry().map(renderer).orElse(""));
    }

    /**
     *  Use this to declare a UI builder for the {@link JComboBox} type
     *  with the provided array of elements as selectable items which
     *  may not be modified by the user.
     *
     * @param items The unmodifiable array of elements to be selectable in the {@link JList}.
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    @SafeVarargs
    public static <E> UIForCombo<E,JComboBox<E>> comboBoxWithUnmodifiable( E... items ) {
        NullUtil.nullArgCheck(items, "items", Object[].class); // Unmodifiable
        java.util.List<E> unmodifiableList = Collections.unmodifiableList(java.util.Arrays.asList(items));
        return comboBox(unmodifiableList);
    }

    /**
     *  Use this to declare a UI builder for the {@link JComboBox} type
     *  with the provided array of elements as selectable items which
     *  may not be modified by the user.
     *  Use this create a UI declaration for the {@link JComboBox} component type
     *  with the provided array of elements as selectable <b>but unmodifiable</b> items
     *  and a lambda function converting each item into a
     *  user-friendly {@link String} representation. <br>
     *
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @param items The unmodifiable array of elements to be selectable in the drop down list of the combo box.
     * @param renderer A lambda function which is used for mapping each entry to a
     *                 user-friendly {@link String} representation shown in the combo box drop down list.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBoxWithUnmodifiable( E[] items, Function<E,String> renderer ) {
        NullUtil.nullArgCheck(items, "items", Object[].class); // Unmodifiable
        java.util.List<E> unmodifiableList = Collections.unmodifiableList(java.util.Arrays.asList(items));
        return comboBox(unmodifiableList)
                .withTextRenderer(cell -> cell.entry().map(renderer).orElse(""));
    }

    /**
     *  Use this to create a builder for a new {@link JComboBox} instance
     *  where the provided enum based property dynamically models the selected item
     *  as well as all possible options (all the enum states).
     *  The property will be updated whenever the user
     *  selects a new item in the {@link JComboBox} and the {@link JComboBox}
     *  will be updated whenever the property changes in your code (see {@link Var#set(Object)}).
     *  <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.comboBox(vm.selection())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      The text displayed on the combo box is based on the {@link Object#toString()}
     *      method of the enum instances. If you want to customize how they are displayed
     *      (So that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      simply override the {@link Object#toString()} method in your enum. </i>
     *
     * @param selectedItem A property modelling the selected item in the combo box.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E extends Enum<E>> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selectedItem ) {
        NullUtil.nullArgCheck(selectedItem, "selectedItem", Var.class);
        // We get an array of possible enum states from the enum class
        return comboBox(selectedItem.type().getEnumConstants()).withSelectedItem(selectedItem);
    }

    /**
     *  Use this to create a builder for a new {@link JComboBox} instance
     *  where the provided enum based property dynamically models the selected item
     *  as well as all possible options (all the enum states).
     *  The property will be updated whenever the user
     *  selects a new item in the {@link JComboBox} and the {@link JComboBox}
     *  will be updated whenever the property changes in your code (see {@link Var#set(Object)}).
     *  <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.comboBox(vm.selection(), e -> switch (e) {
     *          case SMALL -> "Small";
     *          case MEDIUM -> "Medium";
     *          case LARGE -> "Large";
     *      })
     * }</pre>
     * <p>
     * Note that the second argument is a function that maps each enum state to the text
     * which is actually displayed in the combo box to the user.
     *
     * @param selectedItem A property modelling the selected item in the combo box.
     * @param renderer A lambda function which is used for mapping each entry to a
     *                 user friendly {@link String} representation.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E extends Enum<E>> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selectedItem, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(selectedItem, "selectedItem", Var.class);
        // We get an array of possible enum states from the enum class
        return comboBox(selectedItem.type().getEnumConstants())
                .withSelectedItem(selectedItem)
                .withTextRenderer(cell -> cell.entry().map(renderer).orElse(""));
    }

    /**
     *  Use this to declare a builder for a new {@link JComboBox} instance
     *  with the provided list of elements as selectable items.
     *
     * @param items The list of elements to be selectable in the {@link JComboBox}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( java.util.List<E> items ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        return ((UIForCombo)comboBox()).withItems(items);
    }

    /**
     *   Use this to declare a builder for a new {@link JComboBox} instance
     *  with the provided list of elements as selectable items and a
     *  custom renderer function to display the items in the combo box
     *  as text.
     *  <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      List<String> items = List.of("Apple", "Banana", "Cherry");
     *      // In your view:
     *      UI.comboBox(items, fruit -> fruit.toUpperCase())
     *  }</pre>
     *  In this example, the combo box will display the items as "APPLE", "BANANA", "CHERRY".
     *  The provided function is called for each item in the list to determine the text
     *  that should be displayed in the combo box.
     *
     * @param items The list of elements to be selectable in the {@link JComboBox}.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A builder instance for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
     public static <E> UIForCombo<E,JComboBox<E>> comboBox( java.util.List<E> items, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        Objects.requireNonNull(renderer, "renderer");
        return comboBox(items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided {@link UI.ListView} of elements as selectable items which
     *  may not be modified by the user. <br>
     *  So even if the combo box is editable, the user will not be able to
     *  modify the items in the list (the selected item inside the
     *  text field can still be modified though).
     *
     * @param items The list of elements to be selectable in the {@link JComboBox}.
     * @return A UI builder for the provided {@link JComboBox}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBoxWithUnmodifiable( java.util.List<E> items ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        java.util.List<E> unmodifiableList = Collections.unmodifiableList(items);
        return comboBox(unmodifiableList);
    }

    /**
     *  Creates a declarative combo box UI based on the provided list of items,
     *  which may not be modified by the user. <br>
     *  An additional renderer function is provided to customize how the items
     *  are displayed as texts in the combo box.<br>
     *  Here's an example of how the method may be used: <br>
     *  <pre>{@code
     *      // In your view model:
     *      List<String> items = List.of("John", "Jane", "Jack");
     *      // In your view:
     *      UI.comboBoxWithUnmodifiable(items, name -> name.toLowerCase())
     *  }</pre>
     *  In this example, the combo box will display the items as "john", "jane", "jack".
     *  The provided function is called for each item in the list to determine the text
     *  that should be displayed in the combo box.
     *
     * @param items The list of elements to be selectable in the {@link JComboBox}.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A builder instance for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBoxWithUnmodifiable( java.util.List<E> items, Function<E, String> renderer ) {
        Objects.requireNonNull(renderer, "renderer");
        return comboBoxWithUnmodifiable(items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a list of items as a dynamically sized model for the
     *  selectable items.
     *  <p>
     *  Note that the provided list may be mutated by the combo box UI component
     *
     * @param selection The property holding the current selection.
     * @param items The list of selectable items.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selection, java.util.List<E> items ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return ((UIForCombo)comboBox()).withItems(selection, items);
    }

    /**
     *  Creates a declarative combo box UI based on the provided selection property
     *  and the list of items as well as a custom renderer function to display the items
     *  as text in the combo box. <br>
     *  Here's an example of how the method can be used: <br>
     *  <pre>{@code
     *      // In your view model:
     *      List<String> days = List.of("Monday", "Tuesday", "Wednesday");
     *      Var<String> selectedDay = Var.of("Monday");
     *      // In your view:
     *      UI.comboBox(selectedDay, days, day -> "Day: " + day)
     *      // The combo box will display the items as "Day: Monday", "Day: Tuesday", "Day: Wednesday"
     *  }</pre>
     *  In this example, the provided function is called for each item in the list to determine the text
     *  that should be displayed in the combo box.
     *
     * @param selection The property holding the current selection, which will be updated whenever the user selects a new item.
     * @param items The list of selectable items.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A builder instance for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox(
        Var<E> selection, java.util.List<E> items, Function<E, String> renderer
    ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(renderer, "renderer", Function.class);
        return comboBox(selection, items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }


    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided properties list object as selectable (and mutable) items.
     *
     * @param items The {@link Vars} properties of elements to be selectable in the {@link JComboBox}.
     * @param <E> The type of the elements in the list.
     * @return A declarative builder for the provided {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Vars<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vars.class);
        return ((UIForCombo)comboBox()).withItems(items);
    }

    /**
     *  Creates a declarative UI builder for the {@link JComboBox} component type
     *  where the provided property list dynamically models the selectable items
     *  in the combo box and a renderer function determines how the items are displayed
     *  as text in the combo box dropdown list. <br>
     *  The following example demonstrates how this method may be used: <br>
     *  <pre>{@code
     *    // In your view model:
     *    enum Coffee { ESPRESSO, LATTE, CAPPUCCINO }
     *    Vars<Coffee> items = Vars.of(Coffee.ESPRESSO,
     *                                 Coffee.LATTE,
     *                                 Coffee.CAPPUCCINO);
     *    // In your view:
     *    UI.comboBox(items, coffee -> switch (coffee) {
     *      case ESPRESSO -> "Espresso";
     *      case LATTE -> "Latte";
     *      case CAPPUCCINO -> "Cappuccino";
     *    })
     *    .onSelection( it -> ... )
     *  }</pre>
     *
     * @param items The property holding the list of selectable items.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A declarative builder for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Vars<E> items, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(items, "items", Vars.class);
        NullUtil.nullArgCheck(renderer, "renderer", Function.class);
        return comboBox(items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided properties list object as selectable (and immutable) items which
     *  may not be modified by the user.
     *
     * @param items The {@link sprouts.Vals} properties of elements to be selectable in the {@link JComboBox}.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Vals<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vals.class);
        return ((UIForCombo)comboBox()).withItems(items);
    }

    /**
     *  Creates a declarative UI builder for the {@link JComboBox} component type
     *  where the provided property list dynamically models the selectable items
     *  in the combo box and a renderer function determines how the items are displayed
     *  as text in the combo box dropdown list. <br>
     *  The following example demonstrates how this method may be used: <br>
     *  <pre>{@code
     *    // In your view model:
     *    enum Coffee { ESPRESSO, LATTE, CAPPUCCINO }
     *    Vals<Coffee> items = Vals.of(Coffee.ESPRESSO,
     *                                 Coffee.LATTE,
     *                                 Coffee.CAPPUCCINO);
     *    // In your view:
     *    UI.comboBox(items, coffee -> switch (coffee) {
     *      case ESPRESSO -> "Espresso";
     *      case LATTE -> "Latte";
     *      case CAPPUCCINO -> "Cappuccino";
     *    })
     *    .onSelection( it -> ... )
     *  }</pre>
     *  Note that the provided list may not be modified by the user
     *  due to the use of the {@link Vals} property type, which is an immutable
     *  view of the list of items.
     *
     * @param items The property holding the list of selectable items.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A declarative builder for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Vals<E> items, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(items, "items", Vals.class);
        NullUtil.nullArgCheck(renderer, "renderer", Function.class);
        return comboBox(items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a list of items as a dynamically sized model for the
     *  selectable items.
     *  <p>
     *  Note that the provided list may be mutated by the combo box UI component
     *
     * @param selection The property holding the current selection.
     * @param items The list of selectable items.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selection, Vars<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vars.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return ((UIForCombo)comboBox()).withItems(selection, items);
    }

    /**
     *  Creates a declarative combo box UI based on the provided selection property,
     *  a property list of selectable items as well as a custom renderer
     *  function to display the items as the desired text in the combo box. <br>
     *  Here's an example of how the method can be used: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Sentiment { POSITIVE, NEUTRAL, NEGATIVE }
     *      Var<Sentiment> selected = Var.of(Sentiment.NEUTRAL);
     *      Vars<Sentiment> sentiments = Vars.of(Sentiment.POSITIVE,
     *                                           Sentiment.NEUTRAL,
     *                                           Sentiment.NEGATIVE);
     *      // In your view:
     *      UI.comboBox(selected, sentiments, s -> switch (s) {
     *          case POSITIVE -> "Positive";
     *          case NEUTRAL -> "Neutral";
     *          case NEGATIVE -> "Negative";
     *      })
     *  }</pre>
     *  In the example above, the provided function is called for each item in the list
     *  to determine the text that should be displayed in the combo box dropdown list.
     *
     * @param selection The property holding the current selection, which will be updated whenever the user selects a new item.
     * @param items A property list of selectable items.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A declarative builder for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox(
        Var<E> selection, Vars<E> items, Function<E, String> renderer
    ) {
        NullUtil.nullArgCheck(items, "items", Vars.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(renderer, "renderer", Function.class);
        return comboBox(selection, items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a property list of items as a dynamically sized model for the
     *  selectable items which may not be modified by the user.
     *  Use {@link #comboBox(Var, Vars)} if you want the user to be able to modify the items.
     *
     * @param selection The property holding the current selection.
     * @param items The list of selectable items which may not be modified by the user.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selection, Vals<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vals.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return ((UIForCombo)comboBox()).withItems(selection, items);
    }

    /**
     *  Creates a declarative combo box UI based on the provided selection property,
     *  a property list of selectable items as well as a custom renderer
     *  function to display the items as the desired text in the combo box. <br>
     *  Here's an example of how the method can be used: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum BloodType { A, B, AB, O }
     *      Var<Sentiment> selected = Var.of(BloodType.A);
     *      Vals<Sentiment> types = Vals.of(BloodType.A,
     *                                      BloodType.B,
     *                                      BloodType.AB,
     *                                      BloodType.O);
     *      // In your view:
     *      UI.comboBox(selected, types, t -> switch (t) {
     *          case A -> "Type A";
     *          case B -> "Type B";
     *          case AB -> "Type AB";
     *          case O -> "Type O";
     *      })
     *  }</pre>
     *  In the example above, the provided function is called for each item in the list
     *  to determine the text that should be displayed in the combo box dropdown list.
     *  Note that we are using the {@link Vals} property type to ensure the list of items
     *  is immutable and cannot be modified by the user.
     *  If you want the user to be able to modify the items, use {@link #comboBox(Var, Vars, Function)}.
     *
     * @param selection The property holding the current selection, which will be updated whenever the user selects a new item.
     * @param items A property list of selectable items which may not be modified by the user.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A declarative builder for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selection, Vals<E> items, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(items, "items", Vals.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(renderer, "renderer", Function.class);
        return comboBox(selection, items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and an array of items as a fixed-size model for the
     *  selectable items.
     *  <p>
     *  Note that the provided array may be mutated by the combo box UI component
     *
     * @param var The property holding the current selection.
     * @param items The array of selectable items.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> var, E... items ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        return ((UIForCombo)comboBox()).withItems(var, items);
    }

    /**
     *  Creates a combo box UI declaration with a {@link Var} property as the model
     *  for the current selection and an array of items as a fixed-size model for the
     *  selectable items, as well as a lambda function which maps each combo box item
     *  to a user-friendly {@link String} representation.
     *  <p>
     *  Note that the provided array may be mutated by the combo box UI component.
     *
     * @param var The property holding the current selection.
     * @param items The array of selectable items.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     *                 It is intended to make the type of entry more human readable and thereby user-friendly.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox(
        Var<E> var, E[] items, Function<E, String> renderer
    ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        return comboBox(var, items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a tuple property of items as a selectable items model
     *  which may be modified by the user or change dynamically in your code.
     *  So whenever the tuple property changes, the combo box will be updated
     *  with the new selectable items. <br>
     *
     * @param var The property holding the current selection.
     * @param items The property holding a tuple of selectable items which can be mutated by the combo box.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> var, Var<Tuple<E>> items ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        return ((UIForCombo)comboBox()).withItems(var, items);
    }

    /**
     *  Creates a declarative combo box UI based on the provided selection property,
     *  a property of an array of selectable items and a custom renderer
     *  function to display the items as the desired text in the combo box. <br>
     *  Here's an example of how the method can be used: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Cost { CHEAP, MODERATE, EXPENSIVE }
     *      Var<Cost> selected = Var.of(Cost.CHEAP);
     *      Var<Cost[]> costs = Var.of(Cost.values());
     *      // In your view:
     *      UI.comboBox(selected, costs, c -> switch (c) {
     *          case CHEAP -> "Cheap";
     *          case MODERATE -> "Moderate";
     *          case EXPENSIVE -> "Expensive";
     *      })
     *  }</pre>
     *  In the example above, the provided function is called for each item in the list
     *  to determine the text that should be displayed in the combo box dropdown list.
     *  Note that changing the contents of the array in the property may not properly
     *  update the selectable options in the combo box. Instead, ensure that the
     *  {@link Var#set(Object)}, or {@link Var#fireChange(Channel)} method is used
     *  to update the available options in the combo box.
     *
     * @param var The property holding the current selection, which will be updated whenever the user selects a new item.
     * @param items A property array of selectable items which can be mutated by the user.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A declarative builder for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> var, Var<Tuple<E>> items, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        NullUtil.nullArgCheck(var, "var", Var.class);
        NullUtil.nullArgCheck(renderer, "renderer", Function.class);
        return comboBox(var, items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a tuple property of items as a selectable items model
     *  that may change dynamically. So when the tuple in the property changes, the selectable
     *  items in the combo box will be updated accordingly.
     *  <p>
     *  Note that the supplied tuple property may not be modified by the user.
     *  If you want the user to be able to modify the items, use {@link #comboBox(Var, Var)}.
     *
     * @param selectedItem The property holding the current selection.
     * @param items The property holding a tuple of selectable items which may not be modified by the user.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selectedItem, Val<Tuple<E>> items ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        NullUtil.nullArgCheck(selectedItem, "selectedItem", Var.class);
        return ((UIForCombo)comboBox()).withItems(selectedItem, items);
    }

    /**
     *  Creates a declarative combo box UI based on the provided selection property,
     *  a property of an array of selectable items and a custom renderer
     *  function to display the items as the desired text in the combo box. <br>
     *  Here's an example of how the method can be used: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Vehicle { TRAIN, BIKE, BUS }
     *      Var<Vehicle> selected = Var.of(Vehicle.BIKE);
     *      Val<Vehicle[]> vehicles = Val.of(Vehicle.values());
     *      // In your view:
     *      UI.comboBox(selected, vehicles, v -> switch (v) {
     *          case TRAIN -> "Train";
     *          case BIKE -> "Bike";
     *          case BUS -> "Bus";
     *      })
     *  }</pre>
     *  In this example the provided function is called for each item in the list
     *  to determine the text that should be displayed in the combo box dropdown list.
     *  Note that changing the contents of the array in the property may not properly
     *  update the selectable options in the combo box. Instead, ensure that the
     *  {@link Var#set(Object)}, or {@link Var#fireChange(Channel)} method is used
     *  to update the available options in the combo box.
     *
     * @param selectedItem The property holding the current selection, which will be updated whenever the user selects a new item.
     * @param items A property array of selectable items which may not be modified by the user.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     * @return A declarative builder for the {@link JComboBox} type, to allow for fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selectedItem, Val<Tuple<E>> items, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(items, "items", UI.ListView.class);
        NullUtil.nullArgCheck(selectedItem, "selectedItem", Var.class);
        NullUtil.nullArgCheck(renderer, "renderer", Function.class);
        return comboBox(selectedItem, items)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Created a combo box UI builder node with the provided {@link ComboBoxModel}.
     *
     * @param model The model to be used by the combo box.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( ComboBoxModel<E> model ) {
        NullUtil.nullArgCheck(model, "model", ComboBoxModel.class);
        return (UIForCombo)comboBox().peek( c -> ((JComboBox)c).setModel(model) );
    }

    /**
     *  Created a combo box UI builder node with the provided {@link ComboBoxModel}
     *  and a lambda function mapping each model entry to a user-friendly
     *  human-readable {@link String} representation.
     *
     * @param model The model to be used by the combo box.
     * @param renderer A function that maps each item to the text that should be displayed in the combo box.
     *                 It is intended to make the type of entry more human-readable and thereby user-friendly.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( ComboBoxModel<E> model, Function<E, String> renderer ) {
        NullUtil.nullArgCheck(model, "model", ComboBoxModel.class);
        return comboBox(model)
                .withTextRenderer( cell -> cell.entry().map(renderer).orElse("") );
    }

    /**
     *  Use this to create a builder for the provided {@link JSpinner} instance.
     *
     * @param spinner The {@link JSpinner} instance to create a builder for.
     *                The provided {@link JSpinner} instance must not be {@code null}.
     * @param <S> The type parameter of the concrete {@link JSpinner} subclass to be used by the builder.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code spinner} is {@code null}.
     */
    public static <S extends JSpinner> UIForSpinner<S> of( S spinner ) {
        NullUtil.nullArgCheck(spinner, "spinner", JSpinner.class);
        return new UIForSpinner<>(new BuilderState<>(spinner));
    }

    /**
     *  Use this to create a builder for a new {@link JSpinner} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSpinner())}.
     *
     * @return A builder instance for a new {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<JSpinner> spinner() {
        return new UIForSpinner<>(new BuilderState<>(UI.Spinner.class, UI.Spinner::new));
    }

    /**
     * Use this to create a builder for the provided {@link JSpinner} instance
     * with the provided {@link SpinnerModel} as the model.
     *
     * @param model The {@link SpinnerModel} to be used by the {@link JSpinner}.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( SpinnerModel model ) {
        NullUtil.nullArgCheck(model, "model", SpinnerModel.class);
        return new UIForSpinner<>(new BuilderState<JSpinner>(UI.Spinner.class, UI.Spinner::new))
                .peek( s -> s.setModel(model) );
    }

    /**
     *  Use this factory method to create a {@link JSpinner} bound to a property of any type.
     *  The property will be updated when the user modifies its value.
     *
     * @param value A property of any type which should be bound to this spinner.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( Var<?> value ) {
        NullUtil.nullArgCheck(value, "value", Var.class);
        NullUtil.nullPropertyCheck(value, "value", "The state of the spinner should not be null!");
        return spinner().withValue(value);
    }

    /**
     * Use this to create a builder for the provided {@link JSpinner} instance
     * with the provided {@code min}, {@code max}, default {@code value} and {@code step} as the model.
     *
     * @param value The default value of the {@link JSpinner}.
     * @param min The minimum possible value of the {@link JSpinner}.
     * @param max The maximum possible value of the {@link JSpinner}.
     * @param step The step size of the {@link JSpinner}.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( int value, int min, int max, int step ) {
        return new UIForSpinner<>(new BuilderState<JSpinner>(UI.Spinner.class, UI.Spinner::new))
                .peek( s -> s.setModel(new SpinnerNumberModel(value, min, max, step)) );
    }

    /**
     * Use this to create a builder for the provided {@link JSpinner} instance
     * with the provided {@code min}, {@code max} and default {@code value} as the model.
     *
     * @param value The default value of the {@link JSpinner}.
     * @param min The minimum possible value of the {@link JSpinner}.
     * @param max The maximum possible value of the {@link JSpinner}.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( int value, int min, int max ) {
        return new UIForSpinner<>(new BuilderState<JSpinner>(UI.Spinner.class, UI.Spinner::new))
                .peek( s -> s.setModel(new SpinnerNumberModel(value, min, max, 1)) );
    }

    /**
     *  Use this to create a builder for the provided {@link JLabel} instance.
     *
     * @param label The {@link JLabel} instance to be used by the builder.
     * @param <L> The type parameter of the concrete {@link JLabel} subclass to be used by the builder.
     * @return A builder instance for the provided {@link JLabel}, which enables fluent method chaining.
     */
    public static <L extends JLabel> UIForLabel<L> of( L label ) {
        NullUtil.nullArgCheck(label, "component", JLabel.class);
        return new UIForLabel<>(new BuilderState<>(label));
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(text)}.
     *
     * @param text The text which should be displayed on the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return _label().withText(text);
    }

    private static UIForLabel<JLabel> _label() {
        return new UIForLabel<>(new BuilderState<>(UI.Label.class, UI.Label::new));
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(text, alignment))}.
     *
     * @param text The text which should be displayed on the label.
     * @param alignment The horizontal alignment of the text.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( String text, UI.HorizontalAlignment alignment ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(alignment, "alignment", UI.HorizontalAlignment.class);
        return _label().withText(text).withHorizontalAlignment( alignment );
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *
     * @param text The text which should be displayed on the label.
     * @param alignment The vertical and horizontal alignment of the text.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( String text, UI.Alignment alignment ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(alignment, "alignment", UI.Alignment.class);
        return _label().withText(text).withAlignment( alignment );
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(Val<String> text)}.
     *
     * @param text The text property which should be bound to the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return _label()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(Val<String> text, alignment)}.
     *
     * @param text The text property which should be bound to the label.
     * @param alignment The horizontal alignment of the text.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( Val<String> text, UI.HorizontalAlignment alignment ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        NullUtil.nullArgCheck(alignment, "alignment", UI.HorizontalAlignment.class);
        return _label()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text)
                .withHorizontalAlignment( alignment );
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return _label().withIcon(icon);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *  The icon is specified by a {@link IconDeclaration} which
     *  is essentially just a path to an icon resource.
     *  If the icon cannot be found, the label will be empty.
     *  Note that loaded icons are cached, so if you load the same icon multiple times,
     *  the same icon instance will be used (see {@link SwingTree#getIconCache()}).
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map( UI::label ).orElseGet( () -> label("") );
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon dynamically.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed. Therefore, they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in the case of unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behavior of your view model.
     *
     * @param icon The icon property which should dynamically provide a desired icon for the {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> labelWithIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "Null icons are not allowed!");
        return _label().withIcon(icon);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param width The width of the icon when displayed on the label.
     * @param height The height of the icon when displayed on the label.
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( int width, int height, ImageIcon icon ) {
        NullUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        float scale = UI.scale();

        int scaleHint = Image.SCALE_SMOOTH;
        if ( scale > 1.5f )
            scaleHint = Image.SCALE_FAST;

        width  = (int) (width * scale);
        height = (int) (height * scale);

        Image scaled = icon.getImage().getScaledInstance(width, height, scaleHint);
        return _label()
                .withIcon(new ImageIcon(scaled));
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *  The icon is specified by a {@link IconDeclaration} which
     *  is essentially just a path to an icon resource.
     *  If the icon cannot be found, the label will be empty.
     *  Note that loaded icons are cached, so if you load the same icon multiple times,
     *  the same icon instance will be used (see {@link SwingTree#getIconCache()}).
     *
     * @param width The width of the icon when displayed on the label.
     * @param height The height of the icon when displayed on the label.
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map( i -> label(width, height, i) ).orElseGet( () -> label("") );
    }

    /**
     *  Use this to create a UI builder for a {@link JLabel} with bold font.
     *  This is in essence a convenience method for {@code UI.label(String text).makeBold()}.
     *  @param text The text which should be displayed on the label.
     *  @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> boldLabel( String text ) {
        return _label().withText(text).makeBold();
    }

    /**
     *  Use this to create a UI builder for a bound {@link JLabel} with bold font.
     *  This is in essence a convenience method for {@code UI.label(Val<String> text).makeBold()}.
     *  @param text The text property which should be displayed on the label dynamically.
     *  @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> boldLabel( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return _label().withText(text).makeBold();
    }

    /**
     *  Use this to create a builder for a {@link JLabel} displaying HTML.
     *  This is in essence a convenience method for {@code UI.of(new JLabel("<html>" + text + "</html>"))}.
     *
     * @param text The html text which should be displayed on the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> html( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return _label().withText("<html>" + text + "</html>");
    }

    /**
     *  Use this to create a builder for a {@link JLabel} displaying HTML.
     *  This is in essence a convenience method for {@code UI.of(new JLabel("<html>" + text + "</html>"))}.
     *
     * @param text The html text property which should be bound to the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> html( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return _label()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text.view( it -> "<html>" + it + "</html>"));
    }

    /**
     *  Use this to create a builder for the provided {@link swingtree.components.JIcon} instance.
     *
     * @param icon The {@link swingtree.components.JIcon} instance to be used by the builder.
     * @param <I> The type of the {@link swingtree.components.JIcon} instance.
     * @return A builder instance for the provided {@link swingtree.components.JIcon}, which enables fluent method chaining.
     */
    public static <I extends JIcon> UIForIcon<I> of( I icon ) {
        NullUtil.nullArgCheck(icon, "icon", JIcon.class);
        return new UIForIcon<>(new BuilderState<>(icon));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the provided
     *  icon displayed on it.
     *
     * @param icon The icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(icon)));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the
     *  path provided by the supplied {@link IconDeclaration} displayed on it.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param icon The icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(icon)));
    }

    /**
     *  Creates a UI declaration for a {@link JIcon} which is dynamically bound to the provided
     *  {@link Val} property containing an {@link IconDeclaration}. When the {@link IconDeclaration}
     *  of the property changes, the icon displayed on the {@link JIcon} will be updated accordingly.
     *
     * @param icon The property containing the {@link IconDeclaration} whose
     *             {@link Icon} should be displayed on the {@link JIcon}.
     * @return A declarative builder for the {@link JIcon} type, to allow for fluent method chaining.
     */
    public static UIForIcon<JIcon> icon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Var.class);
        NullUtil.nullPropertyCheck(icon, "icon", "Please use an empty string instead of null!");
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(icon)));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the
     *  provided icon scaled to the provided width and height.
     *
     * @param width The width of the icon when displayed on the {@link JIcon}.
     * @param height The height of the icon when displayed on the {@link JIcon}.
     * @param icon The icon which should be placed into a {@link JIcon} for display.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( int width, int height, Icon icon ) {
        return icon(Size.of(width, height), icon);
    }

    /**
     *  Creates a declarative builder for the {@link JIcon} component with the
     *  supplied icon scaled to fit the specified {@link Size},
     *  which consists of a width and height.
     *
     * @param size The size of the icon when displayed on the {@link JIcon}.
     * @param icon The icon which should be placed into a {@link JIcon} for display.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( Size size, Icon icon ) {
        Objects.requireNonNull(size, "size");
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(size, icon)));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the
     *  path defined by the supplied {@link IconDeclaration} displayed on it and scaled to the
     *  provided width and height.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param width The width of the icon when displayed on the {@link JIcon}.
     * @param height The height of the icon when displayed on the {@link JIcon}.
     * @param icon The icon which should be placed into a {@link JIcon} for display.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( int width, int height, IconDeclaration icon ) {
        return icon(Size.of(width, height), icon);
    }

    /**
     *  Creates a declarative builder for the {@link JIcon} component with the
     *  icon found at the path defined by the supplied {@link IconDeclaration} displayed on it
     *  and scaled to fit the provided {@link Size}, consisting of a width and height.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param size The size of the icon when displayed on the {@link JIcon}.
     * @param icon The icon which should be placed into a {@link JIcon} for display.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( Size size, IconDeclaration icon ) {
        Objects.requireNonNull(size, "size");
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        int width = size.width().map(Float::intValue).orElse(0);
        int height = size.height().map(Float::intValue).orElse(0);
        return icon.find().map( i -> icon(width, height, i) ).orElseGet( () -> icon("") );
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the provided
     *  path displayed on it and scaled to the provided width and height.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param width The width of the icon when displayed on the {@link JIcon}.
     * @param height The height of the icon when displayed on the {@link JIcon}.
     * @param iconPath The path to the icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon path is null.
     */
    public static UIForIcon<JIcon> icon( int width, int height, String iconPath ) {
        NullUtil.nullArgCheck(iconPath, "iconPath", String.class);
        return icon(Size.of(width, height), UI.findIcon(iconPath).orElse(new ImageIcon()));
    }

    /**
     *  Creates a declarative builder for the {@link JIcon} component with the
     *  icon found at the path defined by the supplied {@link IconDeclaration} displayed on it
     *  and scaled to fit the provided {@link Size}, consisting of a width and height.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param size The size of the icon when displayed on the {@link JIcon}.
     * @param iconPath The path to the icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon path is null.
     */
    public static UIForIcon<JIcon> icon( Size size, String iconPath ) {
        NullUtil.nullArgCheck(iconPath, "iconPath", String.class);
        return icon(size, UI.findIcon(iconPath).orElse(new ImageIcon()));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the provided
     *  path displayed on it.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param iconPath The path to the icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon path is null.
     */
    public static UIForIcon<JIcon> icon( String iconPath ) {
        NullUtil.nullArgCheck(iconPath, "iconPath", String.class);
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(iconPath)));
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance with the provided
     *  text displayed on it.
     *
     * @param text The text which should be displayed on the checkbox.
     * @return A builder instance for the checkbox, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(UI.CheckBox.class, UI.CheckBox::new))
                .withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance where the provided
     *  text property dynamically displays its value on the checkbox.
     *
     * @param text The text property which should be bound to the checkbox.
     * @return A builder instance for the checkbox, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text property is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(UI.CheckBox.class, UI.CheckBox::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance
     *  where the provided text property dynamically displays its value on the checkbox
     *  and the provided selection property dynamically determines whether the checkbox
     *  is selected or not.
     *
     * @param text The text property which should be bound to the checkbox.
     *             This is the text which is displayed on the checkbox.
     * @param isChecked The selection property which should be bound to the checkbox and determines whether it is selected or not.
     * @return A builder instance for the checkbox, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text property is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( Val<String> text, Var<Boolean> isChecked ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isChecked, "isChecked", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        NullUtil.nullPropertyCheck(isChecked, "isChecked", "The selection state of a check box may not be modelled using null!");
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(UI.CheckBox.class, UI.CheckBox::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .applyIf(!isChecked.hasNoID(), it -> it.id(isChecked.id()))
                .withText(text)
                .isSelectedIf(isChecked);
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance
     *  with the provided text displayed on it and the provided selection property
     *  dynamically determining whether the checkbox is selected or not.
     *  @param text The text which should be displayed on the checkbox.
     *  @param isChecked The selection property which should be bound to the checkbox and determines whether it is selected or not.
     *  @return A builder instance for the checkbox, which enables fluent method chaining.
     *  @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( String text, Var<Boolean> isChecked ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(isChecked, "isChecked", Var.class);
        NullUtil.nullPropertyCheck(isChecked, "isChecked", "The selection state of a check box may not be modelled using null!");
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(UI.CheckBox.class, UI.CheckBox::new))
                .applyIf(!isChecked.hasNoID(), it -> it.id(isChecked.id()))
                .withText(text)
                .isSelectedIf(isChecked);
    }

    /**
     *  Use this to create a builder for the provided {@link JCheckBox} instance.
     *
     * @param component The {@link JCheckBox} instance to be used by the builder.
     * @param <B> The type parameter of the concrete {@link JCheckBox} subclass to be used by the builder.
     * @return A builder instance for the provided {@link JCheckBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided checkbox is null.
     */
    public static <B extends JCheckBox> UIForCheckBox<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JCheckBox.class);
        return new UIForCheckBox<>(new BuilderState<>(component));
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance with the provided
     *  text displayed on it.
     *
     * @param text The text which should be displayed on the radio button.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForRadioButton<JRadioButton> radioButton( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(UI.RadioButton.class, UI.RadioButton::new))
                .withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance where the provided
     *  text property dynamically displays its value on the radio button.
     *
     * @param text The text property which should be bound to the radio button.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     */
    public static UIForRadioButton<JRadioButton> radioButton( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(UI.RadioButton.class, UI.RadioButton::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance
     *  where the provided text property dynamically displays its value on the radio button
     *  and the provided selection property dynamically determines whether the radio button
     *  is selected or not.
     *
     * @param text The text property which should be bound to the radio button.
     *             This is the text which is displayed on the radio button.
     * @param selected The selection property which should be bound to the radio button and determines whether it is selected or not.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text property is null.
     */
    public static UIForRadioButton<JRadioButton> radioButton( Val<String> text, Var<Boolean> selected ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(text, "selected", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        NullUtil.nullPropertyCheck(selected, "selected", "The selection state of a radio button may not be modelled using null!");
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(UI.RadioButton.class, UI.RadioButton::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .applyIf(!selected.hasNoID(), it -> it.id(selected.id()))
                .withText(text)
                .isSelectedIf(selected);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance
     *  with the provided text displayed on it and the provided selection property
     *  dynamically determining whether the radio button is selected or not.
     *  @param text The text which should be displayed on the radio button.
     *  @param selected The selection property which should be bound to the radio button and determines whether it is selected or not.
     *  @return A builder instance for the radio button, which enables fluent method chaining.
     *  @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForRadioButton<JRadioButton> radioButton( String text, Var<Boolean> selected ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(text, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "The selection state of a radio button may not be modelled using null!");
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(UI.RadioButton.class, UI.RadioButton::new))
                .withText(text)
                .isSelectedIf(selected);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance
     *  dynamically bound to an enum based {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link JToggleButton} type by checking
     *  weather the property matches the provided enum or not.
     *  <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.panel()
     *      .add(UI.radioButton(Size.SMALL,  vm.selection())
     *      .add(UI.radioButton(Size.MEDIUM, vm.selection())
     *      .add(UI.radioButton(Size.LARGE,  vm.selection())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      For the text displayed on the radio buttons, the enums will be converted
     *      to strings using {@link Object#toString()} method.
     *      If you want to customize how they are displayed
     *      (So that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      simply override the {@link Object#toString()} method in your enum. </i>
     *
     *
     * @param state The reference {@link Enum} which this {@link JToggleButton} should represent.
     * @param selection The {@link sprouts.Var} instance which will be used
     *                  to dynamically model the selection state of the wrapped {@link JToggleButton} type.
     * @param <E> The type of the enum which this {@link JToggleButton} should represent.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public static <E extends Enum<E>> UIForRadioButton<JRadioButton> radioButton( E state, Var<E> selection ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullPropertyCheck(selection, "selection", "The selection state of a radio button may not be modelled using null!");
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(UI.RadioButton.class, UI.RadioButton::new))
                .applyIf(!selection.hasNoID(), it -> it.id(selection.id()))
                .withText( state.toString() )
                .isSelectedIf( state, selection );
    }

    /**
     *  Creates a declarative UI builder for the {@link JRadioButton} component type which
     *  is dynamically bound to the equality of the provided state and the provided selection property.
     *  This means that the radio button will be selected if the provided state is
     *  equal to the value of the provided selection property and deselected otherwise.
     *  <br>
     *  A typical use case for this is to use an enum based property to model the selection state of the radio button
     *  like so:
     *  <pre>{@code
     *    // In your view model:
     *    enum Size { SMALL, MEDIUM, LARGE }
     *    private Var<Size> selection = Var.of(Size.SMALL);
     *    public Var<Size> selection() { return selection; }
     *    // In your view:
     *    UI.panel()
     *    .add(UI.radioButton("Small", Size.SMALL, vm.selection())
     *    .add(UI.radioButton("Medium", Size.MEDIUM, vm.selection())
     *    .add(UI.radioButton("Large", Size.LARGE, vm.selection())
     *  }</pre>
     *
     * @param label The text which should be displayed on the radio button.
     * @param state The reference object which this radio button should represent.
     * @param selection The property which will be used to model the selection state of the radio button.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @param <T> The type of the state object which this radio button should represent.
     */
    public static <T> UIForRadioButton<JRadioButton> radioButton( String label, T state, Var<T> selection ) {
        NullUtil.nullArgCheck(state, "state", Object.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullPropertyCheck(selection, "selection", "The selection state of a radio button may not be modelled using null!");
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(UI.RadioButton.class, UI.RadioButton::new))
                .applyIf(!selection.hasNoID(), it -> it.id(selection.id()))
                .withText( label )
                .isSelectedIf( state, selection );
    }


    /**
     *  Use this to create a builder for the provided {@link JRadioButton} instance.
     *
     * @param component The {@link JRadioButton} instance which should be wrapped by the builder.
     * @param <R> The type of the {@link JRadioButton} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JRadioButton}, which enables fluent method chaining.
     */
    public static <R extends JRadioButton> UIForRadioButton<R> of( R component ) {
        NullUtil.nullArgCheck(component, "component", JRadioButton.class);
        return new UIForRadioButton<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a {@link JToggleButton} instance.
     *
     * @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton() {
        return new UIForToggleButton<>(new BuilderState<>(UI.ToggleButton.class, UI.ToggleButton::new));
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  with the provided text displayed on it.
     *
     * @param text The text which should be displayed on the toggle button.
     * @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return toggleButton().withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  where the provided text property dynamically displays its value on the toggle button.
     *  <p>
     *  Note that the provided text property may not be null,
     *  and it is also not permitted to contain null values,
     *  instead use an empty string instead of null.
     *
     * @param text The text property which should be bound to the toggle button.
     * @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return toggleButton()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  where the provided boolean property dynamically determines whether the toggle button is selected or not.
     *  @param  isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     *  @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Var<Boolean> isToggled ) {
        NullUtil.nullPropertyCheck(isToggled, "isToggled");
        return toggleButton()
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  with the provided text displayed on it and the provided boolean property
     *  dynamically determining whether the toggle button is selected or not.
     *  @param text The text which should be displayed on the toggle button.
     *  @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     *  @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( String text, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullPropertyCheck(isToggled, "isToggled");
        return toggleButton()
                .withText(text)
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  where the provided text property dynamically displays its value on the toggle button
     *  and the provided boolean property dynamically determines whether the toggle button is selected or not.
     *  @param text The text property which should be bound to the toggle button.
     *             This is the text which is displayed on the toggle button.
     *  @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     *  @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Val<String> text, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isToggled, "isToggled", Var.class);
        NullUtil.nullPropertyCheck(isToggled, "isToggled", "The selection state of a toggle button may not be modelled using null!");
        return toggleButton()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .withText(text)
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance with
     *  the provided {@link Icon} displayed on it.
     *
     * @param icon The icon which should be displayed on the toggle button.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return toggleButton().withIcon(icon);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance with
     *  an {@link ImageIcon} displayed on it and the supplied {@link UI.FitComponent}
     *  determining how the icon should be fit the content bounds of the button.
     *
     * @param icon The icon which should be displayed on the toggle button.
     * @param fit The {@link UI.FitComponent} which determines how the icon should be fitted into the button.
     * @return A builder instance for a {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( ImageIcon icon, UI.FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(fit, "fit", UI.FitComponent.class);
        return toggleButton().withIcon(icon, fit);
    }

    /**
     *  Use this to create a builder for the {@link JToggleButton} UI component
     *  with an icon displayed on it scaled according to the provided width and height.
     *
     * @param width The width to which the icon should be scaled to.
     * @param height The height to which the icon should be scaled to.
     * @param icon The icon to be displayed on top of the button.
     * @return A builder instance for a {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( int width, int height, ImageIcon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return toggleButton().withIcon(width, height, icon);
    }

    /**
     *  Use this to create a builder for the {@link JToggleButton} UI component
     *  with an icon displayed on it scaled according to the provided width and height.
     *
     * @param width The width to which the icon should be scaled to.
     * @param height The height to which the icon should be scaled to.
     * @param icon The {@link IconDeclaration} whose icon ought to be displayed on top of the button.
     * @return A builder instance for a {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return toggleButton().withIcon(width, height, icon);
    }

    /**
     *  Creates a declarative toggle button builder for a {@link JToggleButton}
     *  displaying the provided icon
     *  scaled to fit the desired size and {@link UI.FitComponent} policy.
     *
     * @param width The width to which the icon should be scaled to.
     * @param height The height to which the icon should be scaled to.
     * @param icon The {@link IconDeclaration} whose icon ought to be displayed on top of the button.
     * @param fit The {@link UI.FitComponent} which determines how the icon should be fitted into the button.
     * @return A builder instance for a {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( int width, int height, IconDeclaration icon, UI.FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(fit, "fit", UI.FitComponent.class);
        return toggleButton().withIcon(width, height, icon, fit);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance with
     *  the icon found at the path provided by the supplied {@link IconDeclaration} displayed on top of it.
     *  Note that the icon will be cached by the {@link JToggleButton} instance, so that it will not be reloaded.
     *
     * @param icon The icon which should be displayed on the toggle button.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return toggleButton().withIcon(icon);
    }

    public static UIForToggleButton<JToggleButton> toggleButton( IconDeclaration icon, UI.FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(fit, "fit", UI.FitComponent.class);
        return toggleButton().withIcon(icon, fit);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance with
     *  the provided {@link Icon} displayed on it and the provided boolean property
     *  dynamically determining whether the toggle button is selected or not.
     *
     * @param icon The icon which should be displayed on the toggle button.
     * @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Icon icon, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullPropertyCheck(isToggled, "isToggled", "The selection state of a toggle button may not be modelled using null!");
        return toggleButton(icon)
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance where
     *  the provided {@link IconDeclaration} based property dynamically
     *  displays the targeted image on the toggle button.
     *  <p>
     *  Note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed. Therefore, they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in the case of unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behavior of your view model.
     *
     *
     * @param icon The icon property which should be bound to the toggle button.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButtonWithIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "The icon of a toggle button may not be modelled using null!");
        return new UIForToggleButton<>(new BuilderState<>(JToggleButton.class, UI.ToggleButton::new))
                .applyIf(!icon.hasNoID(), it -> it.id(icon.id()))
                .withIcon(icon);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance where
     *  the provided {@link IconDeclaration} property dynamically displays its targeted icon on the toggle button
     *  and the provided boolean property dynamically determines whether the toggle button is selected or not.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed. Therefore, they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in the case of unit tests for your view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behavior of your view model.
     *
     * @param icon The icon property which should be bound to the toggle button.
     * @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButtonWithIcon( Val<IconDeclaration> icon, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "The icon of a toggle button may not be modelled using null!");
        NullUtil.nullPropertyCheck(isToggled, "isToggled", "The selection state of a toggle button may not be modelled using null!");
        return new UIForToggleButton<>(new BuilderState<>(JToggleButton.class, UI.ToggleButton::new))
                .applyIf(icon.hasID(), it -> it.id(icon.id()))
                .applyIf(isToggled.hasID(), it -> it.id(isToggled.id()))
                .withIcon(icon)
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for the provided {@link JToggleButton} instance.
     *
     * @param component The {@link JToggleButton} instance which should be wrapped by the builder.
     * @param <B> The type of the {@link JToggleButton} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static <B extends JToggleButton> UIForToggleButton<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JToggleButton.class);
        return new UIForToggleButton<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for the provided {@link JTextField} instance.
     *
     * @param component The {@link JTextField} instance which should be wrapped by the builder.
     * @param <F> The type of the {@link JTextField} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text field is null.
     */
    public static <F extends JTextField> UIForTextField<F> of( F component ) {
        NullUtil.nullArgCheck(component, "component", JTextComponent.class);
        return new UIForTextField<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided text displayed on it.
     *
     * @param text The text which should be displayed on the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return textField().withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided text property dynamically displaying its value on the text field.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the text field.
     *
     * @param text The text property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return textField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided text property dynamically displaying its value on the text field.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return textField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextField())}.
     *
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField() {
        return new UIForTextField<>(new BuilderState<>(UI.TextField.class, UI.TextField::new));
    }

    /**
     *  A convenience method for creating a builder for a {@link JTextField} with
     *  the specified {@link UI.HorizontalAlignment} constant as the text orientation.
     *  You may also use {@link UIForTextField#withTextOrientation(UI.HorizontalAlignment)}
     *  to define the text orientation:
     *  <pre>{@code
     *    UI.textField("may text")
     *    .withTextOrientation(
     *        UI.HorizontalAlignment.RIGHT
     *    );
     *  }</pre>
     *
     * @param direction The text orientation type which should be used.
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( UI.HorizontalAlignment direction ) {
        NullUtil.nullArgCheck(direction, "direction", UI.HorizontalAlignment.class);
        return textField().withTextOrientation(direction);
    }

    /**
     *  A convenience method for creating a builder for a {@link JTextField}
     *  with the specified text and text orientation.
     *  You may also use {@link UIForTextField#withText(String)}
     *  and {@link UIForTextField#withTextOrientation(UI.HorizontalAlignment)}
     *  to define the text and text orientation:
     *  <pre>{@code
     *    UI.textField()
     *    .withTextOrientation(
     *        UI.HorizontalAlignment.LEFT
     *    )
     *    .withText(text);
     *  }</pre>
     *
     * @param orientation Defines the orientation of the text inside the text field.<br>
     *                    This may be one of the following constants:
     *                    <ul>
     *                      <li>{@link UI.HorizontalAlignment#LEFT}</li>
     *                      <li>{@link UI.HorizontalAlignment#CENTER}</li>
     *                      <li>{@link UI.HorizontalAlignment#RIGHT}</li>
     *                      <li>{@link UI.HorizontalAlignment#LEADING}</li>
     *                      <li>{@link UI.HorizontalAlignment#TRAILING}</li>
     *                      <li>{@link UI.HorizontalAlignment#UNDEFINED} (No-Op)</li>
     *                    </ul>
     * @param text The new text to be set for the wrapped text component type.
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( UI.HorizontalAlignment orientation, String text ) {
        NullUtil.nullArgCheck(orientation, "orientation", UI.HorizontalAlignment.class);
        return textField().withTextOrientation(orientation).withText(text);
    }

    /**
     *  Creates a UI builder for a text field where the text is aligned according
     *  to the provided {@link UI.HorizontalAlignment} constant, and the text
     *  of the text field is bound to a string property.
     *  Whenever the user modifies the text inside the text field, the value of the
     *  property will be updated accordingly. Conversely, when the state of the property
     *  is modified inside your view model through the {@link Var#set(Object)} method,
     *  the text field will be updated accordingly.
     *  <p>
     *  You may also use {@link UIForTextField#withTextOrientation(UI.HorizontalAlignment)}
     *  and {@link UIForTextField#withText(Var)} to define the text orientation and text property
     *  of the text field:
     *  <pre>{@code
     *  UI.textField()
     *  .withTextOrientation(
     *    UI.HorizontalAlignment.RIGHT
     *  )
     *  .withText(textProperty);
     *  }</pre>
     *
     * @param textOrientation The orientation of the text inside the text field.
     * @param text A string property which is used to model the text of this text field.
     * @return A text field UI builder for declarative UI
     *        design based on method chaining and nesting of SwingTree builder types.
     */
    public static UIForTextField<JTextField> textField(UI.HorizontalAlignment textOrientation, Var<String> text ) {
        NullUtil.nullArgCheck(textOrientation, "textOrientation", UI.HorizontalAlignment.class);
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return textField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withTextOrientation(textOrientation)
                .withText(text);
    }

    /**
     *  Creates a UI builder for a text field where the text is aligned according
     *  to the provided {@link UI.HorizontalAlignment} constant, and the text
     *  of the text field is uni-directionally bound to a string property.
     *  Whenever the state of the property is modified inside your view model through the
     *  {@link Var#set(Object)} method, the text field will be updated accordingly. <br>
     *  But note that <b>when the user modifies the text inside the text field, the value of the
     *  property will not be updated</b>.
     *  <p>
     *  You may also use {@link UIForTextField#withTextOrientation(UI.HorizontalAlignment)}
     *  and {@link UIForTextField#withText(Val)} to define the text orientation and text property
     *  of the text field:
     *  <pre>{@code
     *  UI.textField()
     *  .withTextOrientation(
     *    UI.HorizontalAlignment.RIGHT
     *  )
     *  .withText(readOnlyTextProperty);
     *  }</pre>
     *
     *  @param orientation The orientation of the text inside the text field.
     *                     This is the direction in which the text is aligned.<br>
     *                     It may be one of the following constants:
     *                     <ul>
     *                       <li>{@link UI.HorizontalAlignment#LEFT}</li>
     *                       <li>{@link UI.HorizontalAlignment#CENTER}</li>
     *                       <li>{@link UI.HorizontalAlignment#RIGHT}</li>
     *                       <li>{@link UI.HorizontalAlignment#LEADING}</li>
     *                       <li>{@link UI.HorizontalAlignment#TRAILING}</li>
     *                       <li>{@link UI.HorizontalAlignment#UNDEFINED} (No-Op)</li>
     *                     </ul>
     *  @param text A string property which is used to model the text of this text field
     *              uni-directionally (read-only).
     *  @return A text field UI builder for declarative UI design based on method chaining
     *        and nesting of SwingTree builder types.
     */
    public static UIForTextField<JTextField> textField(UI.HorizontalAlignment orientation, Val<String> text ) {
        NullUtil.nullArgCheck(orientation, "orientation", UI.HorizontalAlignment.class);
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return textField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withTextOrientation(orientation)
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field.
     *  The property is a {@link Var}, meaning that it can be modified by the user.
     *  <p>
     *  The number property will only receive values if the text field contains a valid number.
     *  <p>
     *  Also note that the provided property is not allowed to contain {@code null} values,
     *  as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field
     *  and a function which will be used to format the number as a string.
     *  <p>
     *  The number property will only receive values if the text in the text field can be parsed as a number,
     *  in which case the provided formatter function will be used to convert the number to a string.
     *  <p>
     *  Note that the provided property is not allowed to contain {@code null} values,
     *  as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param formatter The function which will be used to format the number as a string.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number, Function<N,String> formatter ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullArgCheck(formatter, "formatter", Function.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number, formatter);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field
     *  and a boolean property which will be set to {@code true} if the text field contains a valid number,
     *  and {@code false} otherwise.
     *  <p>
     *  The number property will only receive values if the text in the text field can be parsed as a number,
     *  in which case the provided {@link Var} will be set to {@code true}, otherwise it will be set to {@code false}.
     *  <p>
     *  Note that the two provided properties are not permitted to
     *  contain {@code null} values, as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param isValid A {@link Var} which will be set to {@code true} if the text field contains a valid number,
     *                and {@code false} otherwise.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code number} is {@code null}.
     * @throws IllegalArgumentException if {@code isValid} is {@code null}.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number, Var<Boolean> isValid ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        NullUtil.nullArgCheck(isValid, "isValid", Var.class);
        NullUtil.nullPropertyCheck(isValid, "isValid", "Please use false instead of null!");
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number, isValid);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field
     *  and a boolean property which will be set to {@code true} if the text field contains a valid number,
     *  and {@code false} otherwise.
     *  <p>
     *  The number property will only receive values if the text in the text field can be parsed as a number,
     *  in which case the provided {@link Var} will be set to {@code true}, otherwise it will be set to {@code false}.
     *  <p>
     *  Note that the two provided properties are not permitted to
     *  contain {@code null} values, as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param isValid A {@link Var} which will be set to {@code true} if the text field contains a valid number,
     *                and {@code false} otherwise.
     * @param formatter The function which will be used to format the number as a string.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code number} is {@code null}.
     * @throws IllegalArgumentException if {@code isValid} is {@code null}.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number, Var<Boolean> isValid, Function<N,String> formatter ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        NullUtil.nullArgCheck(isValid, "isValid", Var.class);
        NullUtil.nullPropertyCheck(isValid, "isValid", "Please use false instead of null!");
        NullUtil.nullArgCheck(formatter, "formatter", Function.class);
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number, isValid, formatter);
    }


    /**
     *  Use this to create a builder for the provided {@link JFormattedTextField} instance.
     *
     * @param component The {@link JFormattedTextField} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static UIForFormattedTextField of( JFormattedTextField component ) {
        NullUtil.nullArgCheck(component, "component", JFormattedTextField.class);
        return new UIForFormattedTextField(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} instance with
     *  the provided text displayed on it.
     *
     * @param text The text which should be displayed on the text field.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->{
            JFormattedTextField tf = new UI.FormattedTextField();
            tf.setText(text);
            return tf;
        }));
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} instance with
     *  the provided text property dynamically displaying its value in the text field.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the text field.
     *
     * @param text The text property which should be bound to the text field.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->new UI.FormattedTextField()))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} instance with
     *  the provided text property dynamically displaying its value in the formatted text field.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the formatted text field.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->new UI.FormattedTextField()))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JFormattedTextField())}.
     *
     * @return A builder instance for a new {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField() {
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->new UI.FormattedTextField()));
    }

    /**
     *  Use this to create a builder for the provided {@link JPasswordField} instance.
     *
     * @param passwordField The {@link JPasswordField} instance which should be wrapped by the builder.
     * @param <F> The type of the {@link JPasswordField} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <F extends JPasswordField> UIForPasswordField<F> of( F passwordField ) {
        NullUtil.nullArgCheck(passwordField, "passwordField", JPasswordField.class);
        return new UIForPasswordField<>(new BuilderState<>(passwordField));
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} instance with
     *  the provided text as the initial password.
     *
     * @param text The initial password which should be displayed on the password field.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForPasswordField<>(new BuilderState<JPasswordField>(UI.PasswordField.class, UI.PasswordField::new))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} instance with
     *  the provided text property dynamically displaying its value in the password field.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the password field.
     *
     * @param text The text property which should be bound to the password field.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return new UIForPasswordField<>(new BuilderState<>(JPasswordField.class, UI.PasswordField::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} instance with
     *  the provided text property dynamically displaying its value in the password field.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the password field.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return passwordField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPasswordField())}.
     *
     * @return A builder instance for a new {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField() {
        return new UIForPasswordField<>(new BuilderState<>(JPasswordField.class, UI.PasswordField::new));
    }

    /**
     *  Use this to create a builder for the provided {@link JProgressBar} instance.
     *
     * @param progressBar The {@link JProgressBar} instance which should be wrapped by the builder.
     * @param <P> The type of the {@link JProgressBar} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JProgressBar> UIForProgressBar<P> of( P progressBar ) {
        NullUtil.nullArgCheck(progressBar, "progressBar", JProgressBar.class);
        return new UIForProgressBar<>(new BuilderState<>(progressBar));
    }

    /**
     *  A factory method for creating a progress bar builder with a default {@link JProgressBar} implementation.
     *
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar() {
        return new UIForProgressBar<>(new BuilderState<>(UI.ProgressBar.class, UI.ProgressBar::new));
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided minimum and maximum values.
     *
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( int min, int max ) {
        return progressBar().withMin(min).withMax(max);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided minimum, maximum and current value.
     *
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( int min, int max, int value ) {
        return progressBar().withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided minimum, maximum and current value property dynamically bound to the progress bar.
     *
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value property of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( int min, int max, Val<Integer> value ) {
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid value for the value property of a progress bar.");
        return progressBar().withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided alignment, minimum and maximum values.
     *  The alignment is a {@link UI.Align} value, which may be either {@link UI.Align#HORIZONTAL}
     *  or {@link UI.Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar(UI.Align align, int min, int max ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return progressBar().withOrientation(align).withMin(min).withMax(max);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided alignment, minimum, maximum and current value.
     *  The alignment is a {@link UI.Align} value, which may be either {@link UI.Align#HORIZONTAL}
     *  or {@link UI.Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar(UI.Align align, int min, int max, int value ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return progressBar().withOrientation(align).withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided alignment, minimum, maximum and current value property dynamically bound to the progress bar.
     *  The alignment is a {@link UI.Align} value, which may be either {@link UI.Align#HORIZONTAL}
     *  or {@link UI.Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value property of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar(UI.Align align, int min, int max, Val<Integer> value ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        NullUtil.nullArgCheck(value, "value", Val.class);
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid value for the value property of a progress bar.");
        return progressBar().withOrientation(align).withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with a default minimum and maximum value
     *  of 0 and 100 and the provided alignment and double based progress property (a property wrapping a double value between 0 and 1)
     *  dynamically bound to the progress bar.
     *  The alignment is a {@link UI.Align} value, which may be either {@link UI.Align#HORIZONTAL}
     *  or {@link UI.Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param progress The current progress property of the progress bar, a property wrapping a double value between 0 and 1.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar(UI.Align align, Val<Double> progress ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        NullUtil.nullArgCheck(progress, "progress", Val.class);
        NullUtil.nullPropertyCheck(progress, "progress", "Null is not a valid value for the progress property of a progress bar.");
        return progressBar().withOrientation(align).withMin(0).withMax(100).withProgress(progress);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with a default minimum and maximum value
     *  of 0 and 100 and the provided alignment and double based progress property (a property wrapping a double value between 0 and 1)
     *  dynamically bound to the progress bar.
     *  The alignment is a {@link UI.Align} value, which may be either {@link UI.Align#HORIZONTAL}
     *  or {@link UI.Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param progress The current progress property of the progress bar, a property wrapping a double value between 0 and 1.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar(UI.Align align, double progress ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        return progressBar().withOrientation(align).withMin(0).withMax(100).withProgress(progress);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with a default minimum and maximum value
     *  of 0 and 100 and the provided alignment property and double based progress
     *  property (a property wrapping a double value between 0 and 1)
     *  dynamically bound to the progress bar.
     *  The alignment property wraps a {@link UI.Align} value, which may be either {@link UI.Align#HORIZONTAL}
     *  or {@link UI.Align#VERTICAL}.
     *  When any of the two properties change in your view model, the progress bar will be updated accordingly.
     *
     * @param align The alignment of the progress bar.
     * @param progress The current progress property of the progress bar, a property wrapping a double value between 0 and 1.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar(Val<UI.Align> align, Val<Double> progress ) {
        NullUtil.nullArgCheck(align, "align", UI.Align.class);
        NullUtil.nullArgCheck(progress, "progress", Val.class);
        NullUtil.nullPropertyCheck(progress, "progress", "Null is not a valid value for the progress property of a progress bar.");
        return progressBar().withOrientation(align).withMin(0).withMax(100).withProgress(progress);
    }

    /**
     *  Use this to create a builder for the provided {@link JTextArea} instance.
     *
     * @param area The {@link JTextArea} which should be wrapped by the builder.
     * @param <A> The type of the {@link JTextArea} for which the builder should be created.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static <A extends JTextArea> UIForTextArea<A> of( A area ) {
        NullUtil.nullArgCheck(area, "area", JTextArea.class);
        return new UIForTextArea<>(new BuilderState<>(area));
    }

    /**
     *  Use this to create a builder for a new {@link JTextArea} instance with
     *  the provided text as the initial text.
     *
     * @param text The initial text which should be displayed on the text area.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForTextArea<>(new BuilderState<JTextArea>(UI.TextArea.class, UI.TextArea::new))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextArea} instance with
     *  the provided text property dynamically displaying its value in the text area.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the text area.
     *
     * @param text The text property which should be bound to the text area.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return new UIForTextArea<>(new BuilderState<JTextArea>(UI.TextArea.class, UI.TextArea::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextArea} instance with
     *  the provided text property dynamically displaying its value in the text area.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the text area.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return new UIForTextArea<>(new BuilderState<JTextArea>(UI.TextArea.class, UI.TextArea::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a concrete {@link JList} component
     *  type instance. This method allows you to easily integrate custom
     *  {@link JList} implementations into the SwingTree framework.
     *
     * @param list The {@link JList} which should be wrapped by the builder.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for the provided {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> of( JList<E> list ) {
        NullUtil.nullArgCheck(list, "list", JList.class);
        return new UIForList<>(new BuilderState<>(list));
    }

    /**
     *  Allows for the creation of a declarative UI for the {@link JList} component type.
     *
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> list() {
        return new UIForList<>(new BuilderState<>(UI.ListView.class, UI.ListView::new));
    }

    /**
     *  Allows for the creation of a declarative UI for a new {@link JList} instance
     *  with a custom list model.
     *
     * @param model The model which should be used for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> list( ListModel<E> model ) {
        NullUtil.nullArgCheck(model, "model", ListModel.class);
        return new UIForList<>(new BuilderState<>(UI.ListView.class, ()->{
            JList<E> list = new UI.ListView<>();
            list.setModel(model);
            return list;
        }));
    }

    /**
     *  Creates a new {@link JList} instance builder
     *  with the provided array as a data model.
     *  This is functionally equivalent to {@link #listOf(Object...)}.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList} with the provided array as data model.
     */
    @SafeVarargs
    public static <E> UIForList<E, JList<E>> list( E... elements ) {
        NullUtil.nullArgCheck(elements, "elements", Object[].class);
        return new UIForList<>(new BuilderState<JList<E>>(UI.ListView.class, UI.ListView::new))
                .withEntries( elements );
    }

    /**
     *  Allows for the creation of a new {@link JList} instance with the provided
     *  observable property list (a {@link Vals} object) as data model.
     *  When the property list changes, the {@link JList} will be updated accordingly.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @return A builder instance for a new {@link JList} with the provided {@link Vals} as data model.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForList<E, JList<E>> list( Vals<E> elements ) {
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return new UIForList<>(new BuilderState<JList<E>>(UI.ListView.class, UI.ListView::new))
                .withEntries( elements );
    }

    /**
     *  A functionally identical alias method for {@link #list(Vals)}, which allows for
     *  the creation of a new {@link JList} instance with the provided
     *  observable property list (a {@link Vals} object) as data model.
     *  When the property list changes, the {@link JList} will be updated accordingly.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @return A builder instance for a new {@link JList} with the provided {@link Vals} as data model.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForList<E, JList<E>> listOf( Vals<E> elements ) {
        return list( elements );
    }

    /**
     *  Allows for the creation of a new {@link JList} instance with 2 observable
     *  collections as data model, a {@link Var} property for the selection and a {@link Vals}
     *  property list for the elements.
     *  When any of the properties change, the {@link JList} will be updated accordingly,
     *  and conversely, when the {@link JList} selection changes, the properties will be updated accordingly.
     *
     * @param selection The {@link Var} property which should be bound to the selection of the {@link JList}.
     * @param elements The {@link Vals} property which should be bound to the displayed elements of the {@link JList}.
     * @return A builder instance for a new {@link JList} with the provided arguments as data model.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForList<E, JList<E>> list( Var<E> selection, Vals<E> elements ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return list( elements ).withSelection( selection );
    }

    /**
     *  Allows for the creation of a new {@link JList} instance with 2 observable
     *  collections as data model, a {@link Val} property for the selection and a {@link Vals}
     *  property list for the elements.
     *  When any of the properties change, the {@link JList} will be updated accordingly,
     *  however, due to the usage of a read only {@link Val} property for the selection,
     *  the {@link JList} selection will not be updated when the property changes.
     *  If you want a bidirectional binding, use {@link #list(Var, Vals)} instead.
     *
     * @param selection The {@link Val} property which should be bound to the selection of the {@link JList}.
     * @param elements The {@link Vals} property which should be bound to the displayed elements of the {@link JList}.
     * @return A builder instance for a new {@link JList} with the provided {@link Val} and {@link Vals} as data models.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForList<E, JList<E>> list( Val<E> selection, Vals<E> elements ) {
        NullUtil.nullArgCheck(selection, "selection", Val.class);
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return new UIForList<>(new BuilderState<JList<E>>(UI.ListView.class, UI.ListView::new))
                .withEntries( elements ).withSelection( selection );
    }

    /**
     *  Creates a new {@link JList} instance with the provided array
     *  as data model.
     *  This is functionally equivalent to {@link #list(Object...)}.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList} with the provided array as data model.
     */
    @SafeVarargs
    public static <E> UIForList<E, JList<E>> listOf( E... elements ) { return list( elements ); }

    /**
     *  Creates a new {@link JList} instance with the provided {@link UI.ListView}
     *  as data model.
     *  This is functionally equivalent to {@link #listOf(java.util.List)}.
     *
     * @param entries The list of entries used for populating a new {@link JList} component.
     * @param <E> The type parameter defining the concrete type of the list entries.
     * @return A builder instance for a new {@link JList} with the provided {@link UI.ListView} as data model.
     */
    public static <E> UIForList<E, JList<E>> list( java.util.List<E> entries ) {
        return new UIForList<>(new BuilderState<JList<E>>(UI.ListView.class, UI.ListView::new))
                .withEntries( entries );
    }

    /**
     *  Creates a new {@link JList} instance with the provided {@link UI.ListView}
     *  as data model.
     *  This is functionally equivalent to {@link #list(java.util.List)}.
     *
     * @param entries The elements which should be used as model data for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList} with the provided {@link UI.ListView} as data model.
     */
    public static <E> UIForList<E, JList<E>> listOf( java.util.List<E> entries ) { return list( entries ); }

    /**
     *  Allows you to wrap the provided {@link JTable} type in a declarative UI builder.
     *  This is useful when you want to use a custom {@link JTable} implementation
     *  in the SwingTree framework.
     *
     * @param table The table which should be wrapped by the builder.
     * @param <T> The {@link JTable} type.
     * @return A builder instance for a new {@link JTable}.
     */
    public static <T extends JTable> UIForTable<T> of( T table ) {
        NullUtil.nullArgCheck(table, "table", JTable.class);
        return new UIForTable<>(new BuilderState<>(table));
    }

    /**
     *  Creates a declarative UI builder for the {@link JTable} component type.
     *
     * @return A fluent builder instance for a new {@link JTable}.
     */
    public static UIForTable<JTable> table() {
        return new UIForTable<>(new BuilderState<>(UI.Table.class, UI.Table::new));
    }

    /**
     *  Use this to create a new {@link JTable} with a table model whose data can be represented based
     *  on a {@link java.util.List} of {@link java.util.List}s of entries.  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *  <p>
     *      <b>Please note that when the data of the provided data source changes (i.e. when the data source
     *      is a {@link java.util.List} which gets modified), the table model will not be updated automatically!
     *      Use {@link UIForTable#updateTableOn(sprouts.Event)} to bind an update {@link Event} to the table model.</b>
     *
     * @param dataFormat An enum which configures the modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableMapDataSource} returning a column major map based matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public static <E> UIForTable<JTable> table( UI.ListData dataFormat, TableListDataSource<E> dataSource ) {
        NullUtil.nullArgCheck(dataFormat, "dataFormat", UI.ListData.class);
        NullUtil.nullArgCheck(dataSource, "dataSource", TableListDataSource.class);
        return table().withModel(dataFormat, dataSource);
    }

    /**
     *  Use this to create a new {@link JTable} with a table model whose data can be represented based
     *  on a map of column names to lists of table entries (basically a column major matrix).  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *  <p>
     *  <b>Please note that when the data of the provided data source changes (i.e. when the data source
     *  is a {@link Map} which gets modified), the table model will not be updated automatically!
     *  Use {@link UIForTable#updateTableOn(sprouts.Event)} to bind an update {@link Event} to the table model.</b>
     *
     * @param dataFormat An enum which configures the modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableMapDataSource} returning a column major map-based matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public static <E> UIForTable<JTable> table( UI.MapData dataFormat, TableMapDataSource<E> dataSource ) {
        NullUtil.nullArgCheck(dataFormat, "dataFormat", UI.ListData.class);
        NullUtil.nullArgCheck(dataSource, "dataSource", TableMapDataSource.class);
        return table().withModel(dataFormat, dataSource);
    }

    /**
     *  Creates a new {@link JTable} instance builder with the provided table model
     *  configuration as a basis for creating the table model in a declarative fashion. <br>
     *  It is expected to be used like so:
     *  <pre>{@code
     *  UI.table( m -> m
     *    .colCount( () -> data[0].size() )
     *    .rowCount( () -> data.size() )
     *    .getsEntryAt((col, row) -> data[col][row] )
     * )
     * }</pre>
     * The purpose of this pattern is to remove the necessity of implementing the {@link javax.swing.table.TableModel}
     * interface manually, which is a rather tedious task.
     * Instead, you can use ths fluent API provided by the {@link BasicTableModel.Builder} to create
     * a general purpose table model for your table.
     *
     * @param tableModelBuildable A lambda function which takes in a model builder
     *                            and then returns a fully configured model builder
     *                            used as a basis for the table model.
     * @return This builder instance, to allow for further method chaining.
     */
    public static UIForTable<JTable> table(
        Configurator<BasicTableModel.Builder<Object>> tableModelBuildable
    ) {
        Objects.requireNonNull(tableModelBuildable);
        BasicTableModel.Builder<Object> builder = new BasicTableModel.Builder<>(Object.class);
        BasicTableModel.Builder<Object> modifiedBuilder;
        try {
            modifiedBuilder = tableModelBuildable.configure(builder);
        } catch (Exception e) {
            log.error("Failed to configure table model!", e);
            return table();
        }
        return table().withModel(modifiedBuilder);
    }

    /**
     *  Creates a new {@link JTable} instance builder with the provided table model
     *  configuration as a basis for creating the table model in a declarative fashion. <br>
     *  It is expected to be used like so:
     *  <pre>{@code
     *  UI.table(Double.class, m -> m
     *    .colCount( () -> data[0].size() )
     *    .rowCount( () -> data.size() )
     *    .getsEntryAt((col, row) -> data[col][row] )
     * )
     * }</pre>
     * This API removes the necessity to implement the {@link javax.swing.table.TableModel}
     * interface manually, which is a rather tedious task.
     * Instead, you can configure a model step by step through a {@link Configurator} function
     * receiving the fluent builder API provided by the {@link BasicTableModel.Builder}.
     *
     * @param itemType The type of the items in the entries of the table model.
     * @param tableModelBuildable A lambda function which takes in a model builder
     *                            and then returns a fully configured model builder
     *                            used as a basis for the table model.
     * @return This builder instance, to allow for further method chaining.
     * @param <T> The type of the items in the table model.
     */
    public static <T> UIForTable<JTable> table(
        Class<T> itemType,
        Configurator<BasicTableModel.Builder<T>> tableModelBuildable
    ) {
        Objects.requireNonNull(tableModelBuildable);
        BasicTableModel.Builder<T> builder = new BasicTableModel.Builder<>(itemType);
        BasicTableModel.Builder<T> modifiedBuilder;
        try {
            modifiedBuilder = tableModelBuildable.configure(builder);
        } catch (Exception e) {
            log.error("Failed to configure table model!", e);
            return table();
        }
        return table().withModel(modifiedBuilder.build());
    }

    /**
     *  Allows you to wrap a custom {@link JTableHeader} type in a
     *  declarative SwingTree UI builder.
     *
     * @param header The table header which should be wrapped by the builder.
     * @return A builder instance for a new {@link JTableHeader}.
     * @param <H> The type of the {@link JTableHeader} for which the builder should be created.
     */
    public static <H extends UI.TableHeader> UIForTableHeader<H> of( H header ) {
        NullUtil.nullArgCheck(header, "header", UI.TableHeader.class);
        return new UIForTableHeader<>(new BuilderState<>(header));
    }

    /**
     *  Allows you to create a declarative builder for the {@link JTableHeader} UI component.
     * @return A builder instance for a new {@link JTableHeader}.
     */
    public static UIForTableHeader<UI.TableHeader> tableHeader() {
        return new UIForTableHeader<>(new BuilderState<>(UI.TableHeader.class, ()->new UI.TableHeader()));
    }

    /**
     *  This returns an instance of a SwingTree builder for a {@link JFrame} type.
     * @param frame The new frame instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     * @param <F> The concrete type of this new frame.
     */
    public static <F extends JFrame> UIForJFrame<F> of( F frame ) {
        Objects.requireNonNull(frame);
        if ( !(frame.getGlassPane() instanceof JGlassPane) )
            log.debug(
                    "Encountered a JFrame instance which does not have a JGlassPane as its glass pane. " +
                    "SwingTree based drag and drop functionality will not work as expected for this frame.",
                    new Throwable()
                );
        return new UIForJFrame<>(new BuilderState<>(frame));
    }

    private static JFrame _withGlassPane( JFrame frame ) {
        JGlassPane glassPane = new JGlassPane();
        frame.setGlassPane(new JGlassPane());
        glassPane.toRootPane(frame.getRootPane());
        return frame;
    }

    /**
     *  Use this to create a builder for the supplied {@link JFrame}. <br>
     *  This is in essence a convenience method for {@code UI.of(new JFrame()) )}.
     *
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     */
    public static UIForJFrame<JFrame> frame() {
        return new UIForJFrame<>(new BuilderState<>(JFrame.class, ()->_withGlassPane(new JFrame())));
    }

    /**
     *  Use this to create a builder for the supplied {@link JFrame} with the supplied title. <br>
     * @param title The title for the new frame.
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     */
    public static UIForJFrame<JFrame> frame( String title ) {
        return new UIForJFrame<>(new BuilderState<>(JFrame.class, ()->_withGlassPane(new JFrame())))
                .withTitle(title);
    }

    /**
     *  This returns an instance of a SwingTree builder for a {@link JDialog} type.
     * @param dialog The new dialog instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     * @param <D> The concrete type of this new dialog.
     */
    public static <D extends JDialog> UIForJDialog<D> of( D dialog ) {
        if ( !(dialog.getGlassPane() instanceof JGlassPane) )
            log.debug(
                    "Encountered a JDialog instance which does not have a JGlassPane as its glass pane. " +
                    "SwingTree based drag and drop functionality will not work as expected for this dialog.",
                    new Throwable()
                );
        return new UIForJDialog<>(new BuilderState<>(dialog));
    }

    private static JDialog _withGlassPane( JDialog dialog ) {
        JGlassPane glassPane = new JGlassPane();
        dialog.setGlassPane(new JGlassPane());
        glassPane.toRootPane(dialog.getRootPane());
        return dialog;
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog}. <br>
     *  This is in essence a convenience method for {@code UI.of(new JDialog()) )}.
     *
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog() {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()->_withGlassPane(new JDialog())));
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied owner. <br>
     * @param owner The owner for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( Window owner ) {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()->_withGlassPane(new JDialog(owner)) ));
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied title. <br>
     * @param title The title for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( String title ) {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()-> _withGlassPane(new JDialog())))
                .withTitle(title);
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied owner and title. <br>
     * @param owner The owner for the new dialog.
     * @param title The title for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( Window owner, String title ) {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()-> _withGlassPane(new JDialog(owner))))
                .withTitle(title);
    }

    /**
     *  Use this to animate the contents of a property through using an {@link Animatable}
     *  instance holding a transformational function for the intended {@link AnimationStatus}
     *  based changes and a {@link LifeTime} defining the duration of the animation. <br>
     *  Here how this method may be used as part of a UI declaration: <br>
     *  <pre>{@code
     *    UI.button("Login").onClick( it -> {
     *      UI.animate(vm, vm.get().withLoginAnimation());
     *    })
     *  }</pre>
     *
     * @param state A mutable property or property lens holding an immutable item which should
     *              be updated repeatedly by the {@link AnimationTransformation} inside the {@link Animatable}.
     *              The item may also be an immutable view model in case of MVI/MVL design patterns.
     * @param animatable A wrapper for the transformational {@link AnimationTransformation} and the {@link LifeTime}
     *                   defining the duration of the animation.
     * @param <T> The type of the property or property lens.
     */
    public static <T> void animate( Var<T> state, Animatable<T> animatable ) {
        Optional<T>     initialState = animatable.initialState();
        LifeTime        lifeTime     = animatable.lifeTime();
        AnimationTransformation<T> animator     = animatable.animator();

        initialState.ifPresent(state::set);

        if ( !lifeTime.equals(LifeTime.none()) ) {
            Animation animation = Animation.of(state, animator);
            UI.animateFor(lifeTime).go(animation);
        }
    }

    /**
     *  Use this to animate the contents of a property through using an {@link Animatable}
     *  instance holding a transformational function for the intended {@link AnimationStatus}
     *  based changes and a {@link LifeTime} defining the duration of the animation. <br>
     *  Here how this method can be used as part of a UI declaration: <br>
     *  <pre>{@code
     *    UI.button("Login").onClick( it -> {
     *      UI.animate(vm, LoginViewModel::withLoginAnimation);
     *    })
     *  }</pre>
     *
     * @param state A mutable property or property lens holding an immutable item which should
     *              be updated repeatedly by the {@link AnimationTransformation} inside the {@link Animatable}.
     *              The item may also be an immutable view model in case of MVI/MVL design patterns.
     * @param animatable A function taking in the current property item and returning
     *                   a wrapper for the transformational {@link AnimationTransformation} and the {@link LifeTime}
     *                   defining the duration of the animation.
     * @param <T> The type of the property or property lens.
     */
    public static <T> void animate( Var<T> state, Function<T, Animatable<T>> animatable ) {
        animate(state, animatable.apply(state.get()));
    }

    /**
     *  Exposes an API for scheduling periodic animation updates.
     *  This is a convenience method for {@link AnimationDispatcher#animateFor(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( 100, TimeUnit.MILLISECONDS )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setValue( it.progress() );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  Also see {@link UI#animate(Var, Animatable)} for a more straight
     *  forward approach to animating the state of your view models and
     *  consequently also the GUI components bound to them.
     *
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *  @param unit The time unit of the duration.
     *  @return An {@link AnimationDispatcher} instance which allows you to configure the animation.
     */
    public static AnimationDispatcher animateFor( long duration, TimeUnit unit ) {
        Objects.requireNonNull(unit, "unit");
        return AnimationDispatcher.animateFor( LifeTime.of(duration, unit) );
    }

    /**
     *  Exposes a builder API for creating and scheduling periodic animation updates.
     *  This is a convenience method for {@link AnimationDispatcher#animateFor(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( 0.1, TimeUnit.MINUTES )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  Also see {@link UI#animate(Var, Animatable)} for a more straight
     *  forward approach to animating the state of your view models and
     *  consequently also the GUI components bound to them.
     *
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *  @param unit The time unit of the duration.
     *  @return An {@link AnimationDispatcher} instance which allows you to configure the animation.
     */
    public static AnimationDispatcher animateFor( double duration, TimeUnit unit ) {
        return AnimationDispatcher.animateFor( LifeTime.of(duration, unit) );
    }

    /**
     *  Exposes a builder API for creating and scheduling periodic animation updates.
     *  This is a convenience method for {@link AnimationDispatcher#animateFor(LifeTime, Stride)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( 0.1, TimeUnit.MINUTES, Stride.REGRESSIVE )
     *       .until( it -> it.progress() < 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  Also see {@link UI#animate(Var, Animatable)} for a more straight
     *  forward approach to animating the state of your view models and
     *  consequently also the GUI components bound to them.
     *
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *  @param unit The time unit of the duration.
     *  @param stride The stride of the animation, which determines whether the animation
     *                progresses going forward or backwards.
     *  @return An {@link AnimationDispatcher} instance which allows you to configure the animation.
     */
    public static AnimationDispatcher animateFor( double duration, TimeUnit unit, Stride stride ) {
        return AnimationDispatcher.animateFor( LifeTime.of(duration, unit), stride );
    }

    /**
     *  Exposes an API for scheduling periodic animation updates.
     *  This is a convenience method for {@link AnimationDispatcher#animateFor(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( LifeTime.of(0.1, TimeUnit.MINUTES) )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  Also see {@link UI#animate(Var, Animatable)} for a more straight
     *  forward approach to animating the state of your view models and
     *  consequently also the GUI components bound to them.
     *
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *
     *  @return An {@link AnimationDispatcher} instance which allows you to configure and run the animation.
     */
    public static AnimationDispatcher animateFor( LifeTime duration ) {
        return AnimationDispatcher.animateFor( duration );
    }

    /**
     * Exposes an API for scheduling periodic animation updates
     * for a specific component whose {@link java.awt.Component#repaint()}
     * method should be called after every animation update.
     * This is a convenience method for {@link AnimationDispatcher#animateFor(LifeTime)}. <br>
     * A typical usage would be:
     * <pre>{@code
     *    UI.animateFor( UI.lifeTime(0.1, TimeUnit.MINUTES), someComponent )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *       });
     *  }</pre>
     *  Also see {@link UI#animate(Var, Animatable)} for a more straight
     *  forward approach to animating the state of your view models and
     *  consequently also the GUI components bound to them.
     *
     * @param duration  The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     * @param component The component which should be repainted after every animation update.
     * @return An {@link AnimationDispatcher} instance which allows you to configure and then run the animation.
     */
    public static AnimationDispatcher animateFor( LifeTime duration, java.awt.Component component ) {
        return AnimationDispatcher.animateFor( duration, component );
    }

    /**
     *  A factory method for creating a {@link LifeTime} instance
     *  with the given duration and time unit.
     *  This is a convenience method for {@link LifeTime#of(long, TimeUnit)}.
     *  The {@link LifeTime} instance is an immutable value type
     *  which is used for scheduling animations, usually through
     *  {@link AnimationDispatcher#animateFor(LifeTime)} or the convenience methods
     *  {@link UI#animateFor(long, TimeUnit)}, {@link UI#animateFor(double, TimeUnit)},
     *  {@link UI#animateFor(LifeTime)} or {@link UI#animateFor(LifeTime, java.awt.Component)}.
     *  A typical usage would be:
     *  <pre>{@code
     *      UI.animateFor( UI.lifeTime(0.1, TimeUnit.MINUTES) )
     *      .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *      .go( it -> {
     *          // do something
     *      });
     *  }</pre>
     *  You may also want to use a lifetime through an {@link Animatable} passed
     *  to {@link UI#animate(Var, Animatable)} for a more straight
     *  forward approach to animating the state of your view models and
     *  consequently also the GUI components bound to them.
     *
     * @param duration The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A {@link LifeTime} instance.
     */
    public static LifeTime lifeTime( long duration, TimeUnit unit ) { return LifeTime.of(duration, unit); }

    /**
     *  Shows an info dialog with the given message.
     * @param message The message to show in the dialog.
     */
    public static void info( String message ) { info("Info", message); }

    /**
     * Shows an info dialog with the given message and dialog title.
     *
     * @param title   The title of the dialog.
     * @param message The message to show in the dialog.
     */
    public static void info( String title, String message ) {
        message(message)
                .titled(title)
                .showAsInfo();
    }

    /**
     *  Shows a warning dialog with the given message.
     * @param message The warning message to show in the dialog.
     */
    public static void warn( String message ) { warn("Warning", message); }

    /**
     * Shows a warning dialog with the given message and dialog title.
     *
     * @param title   The title of the dialog.
     * @param message The warning message to show in the dialog.
     */
    public static void warn( String title, String message ) {
        message(message)
                .titled(title)
                .showAsWarning();
    }

    /**
     *  Shows an error dialog with the given message.
     * @param message The error message to show in the dialog.
     */
    public static void error( String message ) { error("Error", message); }

    /**
     * Shows an error dialog with the given message and dialog title.
     *
     * @param title   The title of the dialog.
     * @param message The error message to show in the dialog.
     */
    public static void error( String title, String message ) {
        message(message)
                .titled(title)
                .showAsError();
    }

    /**
     *  Exposes the {@link MessageDialog} API, an immutable builder config
     *  for creating a message dialog with a given message text.
     *  Call methods like {@link MessageDialog#showAsInfo()}, {@link MessageDialog#showAsWarning()}
     *  or {@link MessageDialog#showAsError()} to show the dialog in the desired style.
     *
     * @param text The text to show in the dialog.
     * @return A builder for creating an error dialog.
     */
    public static MessageDialog message( String text ) { return MessageDialog.saying(text); }

    /**
     *  Shows a conformation dialog with the given message and
     *  returns the user's answer in the form of a {@link ConfirmAnswer}
     *  enum constant.
     *
     * @param message the message to show
     * @return {@code Answer.YES} if the user clicked "Yes", {@code Answer.NO} if the user clicked "No", {@code Answer.CANCEL} otherwise.
     */
    public static ConfirmAnswer confirm( String message ) { return confirm("Confirm", message); }

    /**
     * Shows a conformation dialog with the given title and message and
     * returns the user's answer in the form of a {@link ConfirmAnswer}
     * enum constant.
     *
     * @param title   the title of the dialog
     * @param message the message to show
     * @return {@code Answer.YES} if the user clicked "Yes", {@code Answer.NO} if the user clicked "No", {@code Answer.CANCEL} otherwise.
     */
    public static ConfirmAnswer confirm( String title, String message ) {
        return ConfirmDialog.asking(message)
                .titled(title)
                .showAsQuestion();
    }

    /**
     *  Exposes the {@link ConfirmDialog} API, an immutable builder config type
     *  for creating a confirmation dialog designed to ask a question.
     *  The supplied string will be used as the question to ask the user
     *  when the dialog is shown using the {@link ConfirmDialog#showAsQuestion()} method.
     *
     * @param toBeConfirmed The question to ask the user.
     * @return A builder for creating a confirmation dialog designed to ask a question.
     */
    public static ConfirmDialog confirmation( String toBeConfirmed ) {
        return ConfirmDialog.asking(toBeConfirmed);
    }

    /**
     *  Shows a dialog where the user can select a value from a list of options
     *  based on the enum type implicitly defined by the given enum based property.
     *  The selected value will be stored in said property after the user has
     *  selected a value and also returned as an {@link Optional}.
     *  If no value is selected, the returned {@link Optional} will be empty
     *  and the property will not be changed.
     *
     * @param question The message to show in the dialog.
     * @param selected The enum-based property to store the selected value in.
     * @param <E> The enum type.
     * @return The selected enum value wrapped in an {@link Optional} or an empty optional if the user cancelled the dialog.
     */
    public static <E extends Enum<E>> Optional<E> ask( String question, Var<E> selected ) {
        return ask("Select", question, selected );
    }

    /**
     * Shows a dialog where the user can select a value from a list of options
     * based on the enum type implicitly defined by the given enum based property.
     * The selected value will be stored in said property after the user has
     * selected a value.
     *
     * @param title    The title of the dialog.
     * @param message  The message to show in the dialog.
     * @param selected The enum based property to store the selected value in.
     * @param <E> The enum type.
     * @return The selected enum value wrapped in an {@link Optional} or an empty optional if the user cancelled the dialog.
     */
    public static <E extends Enum<E>> Optional<E> ask( String title, String message, Var<E> selected ) {
        Objects.requireNonNull( message  );
        Objects.requireNonNull( title    );
        Objects.requireNonNull( selected );
        return OptionsDialog.offering(message, selected)
                .titled(title)
                .showAsQuestion();
    }

    /**
     * Shows a dialog where the user can select a value from a list of options
     * based on the enum type implicitly defined by the given enum based property.
     * The selected value will be stored in said property after the user has
     * selected a value.
     *
     * @param title    The title of the dialog.
     * @param message  The message to show in the dialog.
     * @param icon     The icon to show in the dialog.
     * @param selected The enum-based property to store the selected value in.
     * @param <E> The type parameter defining the concrete enum type.
     */
    public static <E extends Enum<E>> void ask( String title, String message, Icon icon, Var<E> selected ) {
        Objects.requireNonNull( message  );
        Objects.requireNonNull( title    );
        Objects.requireNonNull( selected );
        OptionsDialog.offering(message, selected)
                .titled(title)
                .icon(icon)
                .showAsQuestion();
    }

    /**
     *  Exposes the {@link OptionsDialog} API for creating a question dialog
     *  that allows the user to select a value from an array of provided enum values.
     *
     * @param offer The message to show in the dialog.
     * @param options The array of enum values to show in the dialog.
     * @param <E> The enum type.
     * @return A builder for creating a question dialog with a set of selectable enum values
     *         based on the provided array of enum values.
     */
    @SafeVarargs
    public static <E extends Enum<E>> OptionsDialog<E> choice( String offer, E... options ) {
        return OptionsDialog.offering(offer, options);
    }

    /**
     *  Exposes the {@link OptionsDialog} API for creating a question dialog
     *  that allows the user to select and set a value from the provided enum based property.
     *
     * @param offer The message to show in the dialog.
     * @param selectable The enum-based property to store the selected value in.
     * @param <E> The enum type.
     * @return A builder for creating a question dialog with a set of selectable enum values
     *         based on the provided array of enum values.
     */
    public static <E extends Enum<E>> OptionsDialog<E> choice( String offer, Var<E> selectable ) {
        return OptionsDialog.offering(offer, selectable);
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen.<br>
     *  <b>Warning: This method should only be invoked from the Event Dispatch Thread (EDT).
     *  You may encounter unexpected behavior if you call this method from another thread.<br>
     *  Use {@link #show(Function)} instead to ensure that the UI is created on the EDT.</b>
     *
     * @param component The component to show in the window.
     */
    public static void show( java.awt.Component component ) {
        Objects.requireNonNull( component );
        new TestWindow( "", f -> component );
    }

    /**
     *  Use this to quickly launch a UI component in a titled {@link JFrame} window
     *  at the center of the screen.<br>
     *  <b>Warning: This method should only be invoked from the Event Dispatch Thread (EDT).
     *  You may encounter unexpected behavior if you call this method from another thread.<br>
     *  Use {@link #show(String, Function)} instead to ensure that the UI is created on the EDT.</b>
     *
     * @param title The title of the window.
     * @param component The component to show in the window.
     */
    public static void show( String title, java.awt.Component component ) {
        Objects.requireNonNull( component );
        new TestWindow( title, f -> component );
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen. <br>
     *  <b>Warning: This method should only be invoked from the Event Dispatch Thread (EDT).
     *  You may encounter unexpected behavior if you call this method from another thread.<br>
     *  Use {@link #show(Function)} instead to ensure that the UI is created on the EDT.</b>
     *
     * @param ui The SwingTree UI to show in the window.
     * @param <C> The type of the component to show in the window.
     */
    public static <C extends JComponent> void show( UIForAnySwing<?, C> ui ) {
        new TestWindow( "", f -> ui.get(ui.getType()) );
    }

    /**
     *  Use this to quickly launch a UI component in a titled {@link JFrame} window
     *  at the center of the screen. <br>
     *  <b>Warning: This method should only be invoked from the Event Dispatch Thread (EDT).
     *  You may encounter unexpected behavior if you call this method from another thread.<br>
     *  Use {@link #show(String, Function)} instead to ensure that the UI is created on the EDT.</b>
     *  
     * @param title The title of the window.
     * @param ui The SwingTree UI to show in the window.
     * @param <C> The type of the component to show in the window.
     */
    public static <C extends JComponent> void show( String title, UIForAnySwing<?, C> ui ) {
        new TestWindow( title, f -> ui.get(ui.getType()) );
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen using a function receiving the {@link JFrame}
     *  and returning the component to be shown.
     *
     * @param uiSupplier The component supplier which receives the current {@link JFrame}
     *                   and returns the component to be shown.
     */
    public static void show( Function<JFrame, java.awt.Component> uiSupplier ) {
        Objects.requireNonNull( uiSupplier );
        new TestWindow( "", uiSupplier);
    }

    /**
     *  Use this to quickly launch a UI component in a titled {@link JFrame} window
     *  at the center of the screen using a function receiving the {@link JFrame}
     *  and returning the component to be shown.
     *
     * @param title The title of the window.
     * @param uiSupplier The component supplier which receives the current {@link JFrame}
     *                   and returns the component to be shown.
     */
    public static void show( String title, Function<JFrame, java.awt.Component> uiSupplier ) {
        Objects.requireNonNull( uiSupplier );
        new TestWindow( title, uiSupplier);
    }

    /**
     *  Use this to quickly launch a UI component with a custom event processor
     *  in {@link JFrame} window at the center of the screen.
     *
     * @param eventProcessor the event processor to use for the UI built inside the {@link Supplier} lambda.
     * @param uiSupplier The component supplier which builds the UI and supplies the component to be shown.
     */
    public static void showUsing(EventProcessor eventProcessor, Function<JFrame, java.awt.Component> uiSupplier ) {
        Objects.requireNonNull( eventProcessor );
        Objects.requireNonNull( uiSupplier );
        show(frame -> use(eventProcessor, () -> {
            try {
                return uiSupplier.apply(frame);
            } catch (Exception e) {
                log.error("Error trying to create a UI component for a new JFrame.", e);
                return panel("fill")
                        .add("grow", label("Error: " + e.getMessage()) )
                        .get(JPanel.class);
            }
        }));
    }

    /**
     *  Use this to quickly launch a UI component with a custom event processor
     *  in a titled {@link JFrame} window at the center of the screen.
     *
     * @param eventProcessor the event processor to use for the UI built inside the {@link Supplier} lambda.
     * @param title The title of the window.
     * @param uiSupplier The component supplier which builds the UI and supplies the component to be shown.
     */
    public static void showUsing(
            EventProcessor eventProcessor,
            String title,
            Function<JFrame, java.awt.Component> uiSupplier
    ) {
        Objects.requireNonNull( eventProcessor );
        Objects.requireNonNull( uiSupplier );
        show(title, frame -> use(eventProcessor, () -> uiSupplier.apply(frame)));
    }


    /**
     *  Sets a {@link StyleSheet} which will be applied to all SwingTree UIs defined in the subsequent lambda scope.
     *  This method allows to switch between different style sheets.
     *  <p>
     * 	You can switch to a style sheet like so: <br>
     * 	<pre>{@code
     * 	use(new MyCustomStyeSheet(), ()->
     *      UI.panel("fill")
     *      .add( "shrink", UI.label( "Username:" ) )
     *      .add( "grow, pushx", UI.textField("User1234..42") )
     *      .add( label( "Password:" ) )
     *      .add( "grow, pushx", UI.passwordField("child-birthday") )
     *      .add( "span",
     *          UI.button("Login!").onClick( it -> {...} )
     *      )
     *  );
     *  }</pre>
     *
     * @param styleSheet The style sheet to be used for all subsequent UI building operations.
     * @param scope A lambda scope in which the style sheet is active for all subsequent UI building operations.
     * @param <T> The type of the result of the given scope.
     * @return the result of the given scope, usually a {@link JComponent} or SwingTree UI.
     */
    public static <T> T use( StyleSheet styleSheet, Supplier<T> scope ) {
        if ( !UI.thisIsUIThread() )
            return UI.runAndGet( ()-> use(styleSheet, scope) );

        SwingTree swingTreeContext = SwingTree.get();
        StyleSheet oldStyleSheet = swingTreeContext.getStyleSheet();
        swingTreeContext.setStyleSheet(styleSheet);
        try {
            T result = scope.get();
            if ( result instanceof JComponent )
                ComponentExtension.from((JComponent) result).gatherApplyAndInstallStyle(true);
            if ( result instanceof UIForAnySwing ) {
                UIForAnySwing<?,JComponent> resultSwing = (UIForAnySwing) result;
                ComponentExtension.from(resultSwing.get(resultSwing.getType()))
                        .gatherApplyAndInstallStyle(true);
            }
            return result;
        } finally {
            swingTreeContext.setStyleSheet(oldStyleSheet);
        }
    }

    /**
     *  Sets the {@link EventProcessor} to be used for all subsequent UI building operations.
     *  This method allows to switch between different event processing strategies.
     *  In particular, the {@link EventProcessor#DECOUPLED} is recommended to be used for
     *  proper decoupling of the UI thread from the application logic.
     *  <p>
     * 	You can switch to the decoupled event processor like so: <br>
     * 	<pre>{@code
     * 	use(EventProcessor.DECOUPLED, ()->
     *      UI.panel("fill")
     *      .add( "shrink", UI.label( "Username:" ) )
     *      .add( "grow, pushx", UI.textField("User1234..42") )
     *      .add( label( "Password:" ) )
     *      .add( "grow, pushx", UI.passwordField("child-birthday") )
     *      .add( "span",
     *          UI.button("Login!").onClick( it -> {...} )
     *      )
     *  );
     *  }</pre>
     *
     * @param processor The event processor to be used for all subsequent UI building operations
     * @param scope The scope of the event processor to be used for all subsequent UI building operations.
     *              The value returned by the given scope is returned by this method.
     * @return The value returned by the given scope.
     * @param <T> The type of the value returned by the given scope.
     */
    public static <T> T use( EventProcessor processor, Supplier<T> scope )
    {
        if ( !UI.thisIsUIThread() )
            return UI.runAndGet(()-> use(processor, scope));

        SwingTree swingTreeContext = SwingTree.get();
        EventProcessor oldProcessor = swingTreeContext.getEventProcessor();
        swingTreeContext.setEventProcessor(processor);
        try {
            return scope.get();
        } finally {
            swingTreeContext.setEventProcessor(oldProcessor);
        }
    }

    /**
     *  Use this to quickly create and inspect a test window for a UI component.
     */
    private static class TestWindow
    {
        private final JFrame frame;
        private final java.awt.@Nullable Component component;

        private TestWindow( String title, Function<JFrame, java.awt.Component> uiSupplier ) {
            Objects.requireNonNull( title );
            Objects.requireNonNull( uiSupplier );
            this.frame = new JFrame();
            if ( !title.isEmpty() ) this.frame.setTitle(title);
            frame.setLocationRelativeTo(null); // Initial centering!
            java.awt.Component c = null;
            if ( !UI.thisIsUIThread() ) {
                try {
                    c = UI.runAndGet(() -> uiSupplier.apply(frame));
                } catch (Exception e) {
                    log.error("Error trying to create UI component for display in new JFrame, using supplier function '{}'.", uiSupplier, e);
                }
            }
            else
                c = uiSupplier.apply(frame);

            if ( c == null ) {
                log.error(
                        "Failed to create a UI component for a new JFrame using supplier function '{}'.", uiSupplier,
                        new Throwable()
                    );
            }
            this.component = c;
            UI.runNow(()->{
                frame.add(component);
                frame.pack(); // Otherwise some components resize strangely or are not shown at all...
                // Make sure that the window is centered on the screen again but with the component:
                frame.setLocationRelativeTo(null);
                // We set the size to fit the component:
                _determineSize();
                JGlassPane glassPane = new JGlassPane();
                frame.setGlassPane(new JGlassPane());
                glassPane.toRootPane(frame.getRootPane());
                frame.setVisible(true);
            });
        }
        private void _determineSize() {
            Dimension size = frame.getSize();
            if ( component != null ) {
                if ( size == null ) // The frame has no size! It is best to set the size to the preferred size of the component:
                    size = component.getPreferredSize();

                if ( size == null ) // The component has no preferred size! It is best to set the size to the minimum size of the component:
                    size = component.getMinimumSize();

                if ( size == null ) // The component has no minimum size! Let's just look up the size of the component:
                    size = component.getSize();
            }
            frame.setSize(size);
        }
    }


    /**
     * Loads an {@link ImageIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Note that this method will also return {@link SvgIcon} instances, if the icon is an SVG image.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param path The path to the icon. It can be a classpath resource or a file path.
     * @return An optional containing the icon if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public static Optional<ImageIcon> findIcon(String path ) {
        return findIcon(IconDeclaration.of(path));
    }

    /**
     * Loads an {@link ImageIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Note that this method will also return {@link SvgIcon} instances, if the icon is an SVG image.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param declaration The icon declaration, a value object defining the path to the icon.
     * @return An optional containing the icon if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code declaration} is {@code null}.
     */
    public static Optional<ImageIcon> findIcon( IconDeclaration declaration ) {
        Objects.requireNonNull(declaration, "declaration");
        Map<IconDeclaration, ImageIcon> cache = SwingTree.get().getIconCache();
        ImageIcon icon = cache.get(declaration);
        if ( icon == null ) {
            icon = _tryLoadIcon(declaration);
            if ( icon != null )
                cache.put(declaration, icon);
            else {
                IconDeclaration unscaled = IconDeclaration.of(declaration.path());
                icon = _tryLoadIcon(unscaled);
                if ( icon != null )
                    cache.put(unscaled, icon);
                icon = (ImageIcon) scaleIconTo(declaration.size(), icon);
                if ( icon != null )
                    cache.put(declaration, icon);
            }
        }
        return Optional.ofNullable(icon);
    }

    public static @Nullable Icon scaleIconTo( Size size, @Nullable Icon icon ) {
        if ( icon == null )
            return null;

        if ( icon instanceof SvgIcon ) {
            SvgIcon svg = (SvgIcon) icon;
            icon = svg.withIconSize(size);
        } else if ( icon instanceof ScalableImageIcon ) {
            ScalableImageIcon imageIcon = (ScalableImageIcon) icon;
            icon = imageIcon.withSize(size);
        }

        final double actualWidth  = _getBaseWidth(icon);
        final double actualHeight = _getBaseHeight(icon);
        if ( actualWidth <= 0 || actualHeight <= 0 ){
            log.debug(
                    "Encountered an invalid icon width '" + actualWidth + "' or height '" + actualHeight + "'.",
                    new Throwable()
                );
            return icon;
        }

        if ( size.equals(Size.of(actualWidth, actualHeight)) )
            return icon;

        if ( !size.hasPositiveWidth() && !size.hasPositiveHeight() )
            size = Size.of(actualWidth, actualHeight);
        if  ( !size.hasPositiveWidth() )
            size = size.withWidth((float) (size.height().orElse(1f) * actualWidth / actualHeight));
        if ( !size.hasPositiveHeight() )
            size = size.withHeight((float) (size.width().orElse(1f) * actualHeight / actualWidth));

        int width  = size.width().map(Float::intValue).orElse(0);
        int height = size.height().map(Float::intValue).orElse(0);

        if ( width < 1 || height < 1 ) {
            log.warn(
                    "Encountered an invalid icon size '" + size + "' " +
                    "while scaling an icon for the '"+JIcon.class.getSimpleName()+"' component.",
                    new Throwable()
                );
            return icon;
        }

        if ( icon instanceof ScalableImageIcon ) {
            ScalableImageIcon scalable = (ScalableImageIcon) icon;
            scalable = scalable.withSize(size);
            return scalable;
        }

        if ( icon instanceof SvgIcon ) {
            SvgIcon svgIcon = (SvgIcon) icon;
            svgIcon = svgIcon.withIconSize(width, height);
            return svgIcon;
        } else if ( icon instanceof ImageIcon ) {
            Image scaled = ((ImageIcon)icon).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return icon;
    }

    private static int _getBaseWidth(Icon icon) {
        if ( icon instanceof ScalableImageIcon )
            return ((ScalableImageIcon) icon).getBaseWidth();
        if ( icon instanceof SvgIcon )
            return ((SvgIcon) icon).getBaseWidth();
        return icon.getIconWidth();
    }

    private static int _getBaseHeight(Icon icon) {
        if ( icon instanceof ScalableImageIcon )
            return ((ScalableImageIcon) icon).getBaseHeight();
        if ( icon instanceof SvgIcon )
            return ((SvgIcon) icon).getBaseHeight();
        return icon.getIconHeight();
    }

    /**
     * Loads an {@link SvgIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param path The path to the icon. It can be a classpath resource or a file path.
     * @return An optional containing the {@link SvgIcon} if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public static Optional<SvgIcon> findSvgIcon( String path ) {
        Objects.requireNonNull(path, "path");
        return findSvgIcon(IconDeclaration.of(path));
    }

    /**
     * Loads an {@link SvgIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param declaration The icon declaration, a value object defining the path to the icon.
     * @return An optional containing the {@link SvgIcon} if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code declaration} is {@code null}.
     */
    public static Optional<SvgIcon> findSvgIcon( IconDeclaration declaration ) {
        Objects.requireNonNull(declaration, "declaration");
        if ( !declaration.path().endsWith(".svg") )
            return Optional.empty();

        Map<IconDeclaration, ImageIcon> cache = SwingTree.get().getIconCache();
        ImageIcon icon = cache.get(declaration);
        if ( icon == null ) {
            icon = _tryLoadIcon(declaration);
            if ( icon != null )
                cache.put(declaration, icon);
        }
        if ( !(icon instanceof SvgIcon) )
            return Optional.empty();
        else
            return Optional.of(icon).map(SvgIcon.class::cast);
    }

    /**
     * Loads an icon from the classpath or from a file.
     * @param declaration The icon declaration, a value object defining the path to the icon.
     *          The path can be a classpath resource or a file path.
     * @return The icon.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    private static @Nullable ImageIcon _tryLoadIcon(IconDeclaration declaration )
    {
        ImageIcon icon = null;
        try {
            icon = _loadIcon(declaration);
        } catch (Exception e) {
            log.error("Failed to load icon from declaration: " + declaration, e);
        }
        return icon;
    }

    /**
     * Loads an icon from the classpath or from a file.
     * @param declaration The icon declaration, a value object defining the path to the icon.
     *          The path can be a classpath resource or a file path.
     * @return The icon.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    private static @Nullable ImageIcon _loadIcon( IconDeclaration declaration )
    {
        Objects.requireNonNull(declaration, "declaration");
        String path = declaration.path();
        Objects.requireNonNull(path, "path");
        path = path.trim();
        if ( path.isEmpty() )
            return null;
        // First we make the path platform independent:
        path = path.replace('\\', '/');
        // Then we try to load the icon url from the classpath:
        URL url = UI.class.getResource(path);
        // We check if the url is null:
        if ( url == null ) {
            // It is, let's do some troubleshooting:
            if ( !path.startsWith("/") )
                url = UI.class.getResource("/" + path);

            if ( url == null ) // Still null? Let's try to load it as a file:
                try {
                    url = new File(path).toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
        }
        if ( path.endsWith(".svg") ) {
            SVGDocument tempSVGDocument;
            try {
                SVGLoader loader = new SVGLoader();
                tempSVGDocument = Objects.requireNonNull(loader.load(url));
            } catch (Exception e) {
                log.error("Failed to load SVG document from URL: " + url, e);
                return null;
            }
            return new SvgIcon(tempSVGDocument).withIconSize(declaration.size());
        } else {
            /*
                Not that we explicitly use the "createImage" method of the toolkit here.
                This is because otherwise the image might get cached inside the toolkit,
                which is in the way of our own caching mechanism.
                (The internal caching of the toolkit is somewhat limited and we have no control over it,
                which is why we use our own cache.)
            */
            ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(url), url.toExternalForm());
            return ScalableImageIcon.of(declaration.size(), icon);
        }
    }


}

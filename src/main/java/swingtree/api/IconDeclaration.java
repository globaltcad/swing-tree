package swingtree.api;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;
import swingtree.layout.Size;
import swingtree.style.SvgIcon;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

/**
 *  Primarily designed to be implemented by an {@link Enum} type
 *  that declares a set of icon paths so that the enum instances
 *  can be used to identify and load
 *  (cached) icons across your application.
 *  <p>
 *  Here an example of how to use this interface:
 *  <pre>{@code
 * public enum Icons implements IconDeclaration
 * {
 *     ADD("icons/add.png"),
 *     REMOVE("icons/remove.png"),
 *     EDIT("icons/edit.png"),
 *     SAVE("icons/save.png"),
 *     CANCEL("icons/cancel.png"),
 *     REFRESH("icons/refresh.png");
 *     // ...
 *
 *     private final String path;
 *
 *     Icons(String path) { this.path = path; }
 *
 *     {@literal @}Override public String source() {
 *         return path;
 *     }
 * }
 * }</pre>
 *
 *  You may then use the enum instances
 *  in the SwingTree API just like you would use the {@link ImageIcon} class:
 *  <pre>{@code
 *  UI.button(Icons.ADD)
 *  .onClick( it -> vm.add() )
 *  }</pre>
 *
 *  The reason why enums should be used instead of Strings is
 *  so that you have some more compile time safety in your application!
 *  When it comes to resource loading Strings are brittle because they
 *  are susceptible to typos and refactoring mistakes.
 *  <p>
 *  Instances of this class are intended to be used as part of a view model
 *  instead of using the {@link Icon} or {@link ImageIcon} classes directly.
 *  <p>
 *  The reason for this is the fact that traditional Swing icons
 *  are often heavyweight objects whose loading may or may not succeed, and so they are
 *  not suitable for direct use in a property as part of your view-model.
 *  Instead, you should use this {@link IconDeclaration} interface, which is a
 *  lightweight value object that merely models the resource location of the icon
 *  even if it is not yet loaded or even does not exist at all.
 *  <p>
 *  Not only does this make your view model more robust, but it also allows you
 *  to write unit tests much more easily. You can create icon declarations
 *  even if the targeted icon may not be available at all, yet you can still test the
 *  behavior of your view-model.
 *  <p>
 *  <strong>SVG Support:</strong>
 *  In addition to traditional image files, {@code IconDeclaration} supports
 *  programmatically defined SVG icons through the {@link #ofAutoScaledSvg(String)} factory method.
 *  This enables creation of resolution-independent vector icons directly in code:
 *  <pre>{@code
 *  // Create a custom SVG icon declaration
 *  IconDeclaration playButton = IconDeclaration.ofSvg(
 *      "<svg width='24' height='24' viewBox='0 0 24 24'>" +
 *      "  <path d='M8 5v14l11-7z' fill='currentColor'/>" +
 *      "</svg>"
 *  );
 *
 *  // Use it in UI declarations
 *  UI.button(playButton)
 *    .onClick(it -> mediaPlayer.play())
 *  }</pre>
 *
 *  SVG icons automatically scale to match DPI settings and maintain crisp edges
 *  at any resolution. They can be sized dynamically or set to a fixed size
 *  using {@link #withSize(Size)}.
 */
@Immutable
public interface IconDeclaration
{
    /**
     * Defines the format of the source string returned by {@link #source()}.
     * This enum determines how the source string should be interpreted
     * when loading or creating an icon.
     * <p>
     * For example, a source string could be a file path pointing to a
     * PNG or JPEG image file, or it could be a complete SVG document
     * in XML text form.
     */
    enum SourceFormat {
        /**
         * The source string is a path to an icon file.
         * This path can be either:
         * <ul>
         *     <li>A relative path resolved against the classpath</li>
         *     <li>An absolute file system path</li>
         * </ul>
         * The file format can be any image format supported by Java's
         * {@link ImageIcon} class (PNG, JPEG, etc.).
         */
        PATH_TO_ICON,

        /**
         * The source string is a complete SVG document in XML text form.
         * This allows for programmatic creation of vector icons without
         * needing external files.
         * <p>
         * Example:
         * <pre>{@code
         * String svg = "<svg width='16' height='16'><circle cx='8' cy='8' r='6' fill='red'/></svg>";
         * IconDeclaration.ofSvg(svg);
         * }</pre>
         */
        SVG_STRING
    }

    /**
     *  This method supplies a String which is a "source" for producing {@link ImageIcon},
     *  <b>this is typically a path to a file</b>, but may also be a full-blown SVG document in text form.<br>
     *  The exact meaning of the source String is defined by the {@link SourceFormat}
     *  returned by {@link #sourceFormat()} method. Together, these two properties form
     *  the most important part an icon declaration, since they constitute the minimum
     *  amount of information needed to resolve an actual icon...<br>
     *  Note that in case of the source String being a {@link SourceFormat#PATH_TO_ICON},
     *  it may either be a path relative to the classpath or may be an absolute path.
     *
     * @return The "source String" which is used together with the {@link #sourceFormat()}
     *          to resolve an icon. It is typically a path to the icon resource,
     *          which is relative to the classpath or may be an absolute path.
     */
    String source();

    /**
     * Returns the format of the source string returned by {@link #source()}.
     * This determines how the source string should be interpreted when
     * loading or creating the icon.
     * <p>
     * By default, this method returns {@link SourceFormat#PATH_TO_ICON},
     * meaning the source string is treated as a file path. Implementations
     * can override this method to return {@link SourceFormat#SVG_STRING}
     * if they provide SVG content directly.
     *
     * @return The format of the source string, never {@code null}.
     */
    default SourceFormat sourceFormat() {
        return SourceFormat.PATH_TO_ICON;
    }

    /**
     *  The preferred size of the icon, which is not necessarily the actual size
     *  of the icon that is being loaded but rather the size that the icon should
     *  be scaled to when it is being loaded.<br>
     *  If this method returns {@link Optional#empty()}, then the loaded icon will have
     *  the exact same size found at the image file found at {@link #source()}.
     *  A non-empty optional with a {@link Size#unknown()}, specifically indicates to
     *  {@link swingtree.style.SvgIcon}s that their size should be context dependent,
     *  meaning that their {@link SvgIcon#getIconWidth()} and {@link SvgIcon#getIconHeight()}
     *  are both returning {@code -1} to indicate flexible scaling.<br>
     *  In case of a regular png or jpeg on the other hand, a {@link Size#unknown()} will
     *  also cause the resulting {@link ImageIcon} to have the same dimensions found in the file.
     *
     * @return The preferred size of the icon or {@link Optional#empty()} if the size is unspecified,
     *          which means that the icon should be loaded in its original size.
     *          A non-empty optional with a {@link Size#unknown()}, specifically indicates to
     *          {@link swingtree.style.SvgIcon}s that their size should be context dependent.
     */
    default Optional<Size> size() {
        return Optional.of(Size.unknown());
    }

    /**
     *  This method is used to find the icon resource
     *  and load it as an {@link ImageIcon} instance
     *  wrapped in an {@link Optional},
     *  or return an empty {@link Optional} if the icon resource
     *  could not be found.
     *
     * @return An {@link Optional} that contains the {@link ImageIcon}
     *         if the icon resource was found, otherwise an empty {@link Optional}.
     */
    default Optional<ImageIcon> find() {
        return UI.findIcon(this);
    }

    /**
     *  Creates and returns an updated {@link IconDeclaration} instance
     *  with a new preferred size for the icon.
     *
     * @param size The preferred size of the icon in the form of a {@link Size} instance.
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the given size.
     */
    default IconDeclaration withSize( Size size ) {
        return IconDeclaration.of(size, sourceFormat(), source());
    }

    /**
     *  Creates and returns an updated {@link IconDeclaration} instance
     *  with a new preferred width and height for the icon.
     *
     * @param width The preferred width of the icon.
     * @param height The preferred height of the icon.
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the specified width and height as preferred size.
     */
    default IconDeclaration withSize( int width, int height ) {
        return IconDeclaration.of(Size.of(width, height), sourceFormat(), source());
    }

    /**
     *  Creates and returns an updated {@link IconDeclaration} instance
     *  with a new preferred width for the icon.
     *
     * @param width The preferred width of the icon.
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the specified width as preferred width.
     */
    default IconDeclaration withWidth( int width ) {
        return IconDeclaration.of(
                size().map(it->it.withWidth(width)).orElse(Size.of(width, -1)),
                sourceFormat(),
                source()
            );
    }

    /**
     *  Allows you to create an updated {@link IconDeclaration} instance
     *  with a new preferred height for the icon.
     *
     * @param height The preferred height of the icon.
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the specified height as preferred height.
     */
    default IconDeclaration withHeight( int height ) {
        return IconDeclaration.of(
                size().map(it->it.withHeight(height)).orElse(Size.of(-1, height)),
                sourceFormat(),
                source()
            );
    }

    /**
     *  This method is used to find an {@link ImageIcon} of a specific type
     *  and load and return it wrapped in an {@link Optional},
     *  or return an empty {@link Optional} if the icon resource could not be found.
     *
     * @param type The type of icon to find.
     * @return An {@link Optional} that contains the {@link ImageIcon} of the given type.
     * @param <T> The type of icon to find.
     */
    default <T extends ImageIcon> Optional<T> find( Class<T> type ) {
        return UI.findIcon(this).map(type::cast);
    }

    /**
     *  A factory method for creating an {@link IconDeclaration} instance
     *  from the provided path to the icon resource.
     *
     * @param path The path to the icon resource, which may be relative
     *             to the classpath or may be an absolute path.
     * @return A new {@link IconDeclaration} instance
     *        that represents the given icon resource as a constant.
     */
    static IconDeclaration of( String path ) {
        Objects.requireNonNull(path);
        return IconDeclaration.of(Size.unknown(), SourceFormat.PATH_TO_ICON, path);
    }

    /**
     *  A factory method for creating an {@link IconDeclaration} instance
     *  from the provided path to the icon resource and the preferred size.
     *
     * @param size The preferred size of the icon.
     * @param path The path to the icon resource, which may be relative
     *             to the classpath or may be an absolute path.
     * @return A new {@link IconDeclaration} instance
     *        that represents the given icon resource as a constant.
     */
    static IconDeclaration of( Size size, String path ) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(path);
        return IconDeclaration.of(size, SourceFormat.PATH_TO_ICON, path);
    }

    /**
     * Creates an {@link IconDeclaration} instance from an SVG document string,
     * <b>which will be resolved to an {@link SvgIcon} with an unknown size,
     * effectively making it scale depending on its usage context.</b>
     * This factory method is specifically designed for creating vector-based
     * icons programmatically without requiring external files.
     * <p>
     * The provided SVG string must be a complete, well-formed SVG XML document.
     * The resulting icon declaration will have its {@link #sourceFormat()}
     * set to {@link SourceFormat#SVG_STRING}.
     * <p>
     * Icon declarations produced by this method will resolve to
     * {@link swingtree.style.SvgIcon} instances, which support dynamic
     * scaling while maintaining crisp edges at any scale.
     * <p>
     * Example:
     * <pre>{@code
     * String checkmarkSvg = "<svg width='16' height='16'>" +
     *                       "<path d='M2 8l4 4l8-8' stroke='green' stroke-width='2' fill='none'/>" +
     *                       "</svg>";
     * IconDeclaration checkmark = IconDeclaration.ofAutoScaledSvg(checkmarkSvg)
     *                                            .withSize(24, 24);
     * }</pre>
     * In the above example, an {@link SvgIcon} loaded using this declaration will report {@code -1}
     * for both {@link SvgIcon#getIconWidth()} and {@link SvgIcon#getIconHeight()} instead of 16!
     * This is to ensure that the svg is scalable when used as component icons...
     *
     * @param svg The complete SVG document as a string.
     *           Must not be {@code null}.
     * @return A new {@link IconDeclaration} instance representing the SVG icon.
     * @throws NullPointerException if {@code svg} is {@code null}.
     */
    static IconDeclaration ofAutoScaledSvg(String svg) {
        Objects.requireNonNull(svg);
        return IconDeclaration.of(Size.unknown(), SourceFormat.SVG_STRING, svg);
    }

    /**
     * Creates an {@link IconDeclaration} instance from an SVG document string,
     * <b>which will be resolved to an {@link SvgIcon} with an unknown size,
     * effectively making it scale depending on its usage context.</b>
     * This factory method is specifically designed for creating vector-based
     * icons programmatically without requiring external files.
     * <p>
     * The provided SVG string must be a complete, well-formed SVG XML document.
     * The resulting icon declaration will have its {@link #sourceFormat()}
     * set to {@link SourceFormat#SVG_STRING}.
     * <p>
     * Icon declarations produced by this method will resolve to
     * {@link swingtree.style.SvgIcon} instances, which support dynamic
     * scaling while maintaining crisp edges at any scale.
     * <p>
     * Example:
     * <pre>{@code
     * String checkmarkSvg = "<svg width='16' height='16'>" +
     *                       "<path d='M2 8l4 4l8-8' stroke='green' stroke-width='2' fill='none'/>" +
     *                       "</svg>";
     * IconDeclaration checkmark = IconDeclaration.ofSvg(checkmarkSvg)
     *                                            .withSize(24, 24);
     * }</pre>
     * In the above example, an {@link SvgIcon} loaded using this declaration will report {@code 16}
     * for both {@link SvgIcon#getIconWidth()} and {@link SvgIcon#getIconHeight()}.
     *
     * @param svg The complete SVG document as a string.
     *           Must not be {@code null}.
     * @return A new {@link IconDeclaration} instance representing the SVG icon.
     * @throws NullPointerException if {@code svg} is {@code null}.
     */
    static IconDeclaration ofSvg(String svg) {
        Objects.requireNonNull(svg);
        return BasicIconDeclaration.of(null, SourceFormat.SVG_STRING, svg);
    }

    /**
     *  Creates an {@link IconDeclaration} instance with full control over all
     *  declaration properties: size, source format, and source content.
     *  <p>
     *  This is the most comprehensive factory method, allowing precise specification
     *  of how the icon should be interpreted and displayed. It's particularly useful
     *  when creating icon declarations programmatically or when you need explicit
     *  control over the source format.
     *  <p>
     *  <b>Size Handling:</b>
     *  If the provided {@code size} parameter is {@link Size#unknown()}, it will be
     *  treated as quasi-{@code null} internally, indicating that the icon should use its
     *  natural dimensions (for raster images) or be context-dependent (for SVG icons).
     *  <p>
     *  <b>Example Usage:</b>
     *  <pre>{@code
     *  // Create a PNG icon declaration with specific size
     *  IconDeclaration icon1 = IconDeclaration.of(
     *      Size.of(32, 32),
     *      SourceFormat.PATH_TO_ICON,
     *      "icons/user.png"
     *  );
     *
     *  // Create an SVG icon declaration with unknown (flexible) size
     *  IconDeclaration icon2 = IconDeclaration.of(
     *      Size.unknown(),
     *      SourceFormat.SVG_STRING,
     *      "<svg ...>...</svg>"
     *  );
     *  }</pre>
     *
     *  @param size The preferred display size for the icon. Use {@link Size#unknown()}
     *              to indicate natural/context-dependent sizing. Must not be {@code null}.
     *  @param sourceFormat The format interpretation of the source string.
     *              Must not be {@code null}.
     *  @param source The icon source content (file path or SVG XML).
     *              Must not be {@code null}.
     *  @return A new {@link IconDeclaration} instance configured with the specified
     *          properties.
     *  @throws NullPointerException if any parameter is {@code null}.
     *  @see Size#unknown()
     *  @see SourceFormat
     */
    static IconDeclaration of( Size size, SourceFormat sourceFormat, String source ) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(sourceFormat);
        Objects.requireNonNull(source);
        return BasicIconDeclaration.of( size, sourceFormat, source );
    }
}

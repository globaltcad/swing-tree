package swingtree.api;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;
import swingtree.layout.Size;

import javax.swing.Icon;
import javax.swing.ImageIcon;
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
 *     {@literal @}Override public String path() {
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
 *  not suitable for direct use in a property as part of your view model.
 *  Instead, you should use this {@link IconDeclaration} interface, which is a
 *  lightweight value object that merely models the resource location of the icon
 *  even if it is not yet loaded or even does not exist at all.
 *  <p>
 *  Not only does this make your view model more robust, but it also allows you
 *  to write unit tests much more easily, because you can now create instances
 *  where the icon may not be available at all, yet you can still test the
 *  behavior of your view model.
 */
@Immutable
public interface IconDeclaration
{
    /**
     *  The most important part of the identity of an
     *  icon declaration is the path to the icon resource.
     *  This path may be relative to the classpath or may be an absolute path.
     *
     * @return The path to the icon resource, which is relative
     *         to the classpath or may be an absolute path.
     */
    String path();

    /**
     *  The preferred size of the icon, which is not necessarily the actual size
     *  of the icon that is being loaded but rather the size that the icon should
     *  be scaled to when it is being loaded.
     *
     * @return The preferred size of the icon or {@link Size#unknown()} if the size is unspecified,
     *          which means that the icon should be loaded in its original size.
     */
    default Size size() {
        return Size.unknown();
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
        return IconDeclaration.of(size, path());
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
        return IconDeclaration.of(Size.of(width, height), path());
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
        return IconDeclaration.of(size().withWidth(width), path());
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
        return IconDeclaration.of(size().withHeight(height), path());
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
        return of(Size.unknown(), path);
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
        return new BasicIconDeclaration(size, path);
    }
}

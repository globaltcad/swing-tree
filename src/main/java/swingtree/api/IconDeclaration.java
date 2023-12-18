package swingtree.api;

import swingtree.UI;
import swingtree.style.Size;

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
public interface IconDeclaration
{
    /**
     * @return The path to the icon resource, which is relative
     *         to the classpath or may be an absolute path.
     */
    String path();

    /**
     * @return The preferred size of the icon or {@link Size#unknown()} if the size is unspecified,
     *          which means that the icon should be loaded in its original size.
     */
    default Size size() {
        return Size.unknown();
    }

    /**
     * @return An {@link Optional} that contains the {@link ImageIcon}
     *         if the icon resource was found, otherwise an empty {@link Optional}.
     */
    default Optional<ImageIcon> find() {
        return UI.findIcon(this);
    }

    /**
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the given size.
     */
    default IconDeclaration withSize( Size size ) {
        return IconDeclaration.of(size, path());
    }

    /**
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the specified width and height as preferred size.
     */
    default IconDeclaration withSize( int width, int height ) {
        return IconDeclaration.of(Size.of(width, height), path());
    }

    /**
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the specified width as preferred width.
     */
    default IconDeclaration withWidth( int width ) {
        return IconDeclaration.of(size().width(width), path());
    }

    /**
     * @return A new {@link IconDeclaration} instance with the same path
     *        but with the specified height as preferred height.
     */
    default IconDeclaration withHeight( int height ) {
        return IconDeclaration.of(size().height(height), path());
    }

    /**
     * @param type The type of icon to find.
     * @return An {@link Optional} that contains the {@link ImageIcon} of the given type.
     * @param <T> The type of icon to find.
     */
    default <T extends ImageIcon> Optional<T> find( Class<T> type ) {
        return UI.findIcon(this).map(type::cast);
    }

    /**
     * @param path The path to the icon resource, which may be relative
     *             to the classpath or may be an absolute path.
     * @return A new {@link IconDeclaration} instance
     *        that represents the given icon resource as a constant.
     */
    static IconDeclaration of( String path ) {
        return of(Size.unknown(), path);
    }

    /**
     * @param size The preferred size of the icon.
     * @param path The path to the icon resource, which may be relative
     *             to the classpath or may be an absolute path.
     * @return A new {@link IconDeclaration} instance
     *        that represents the given icon resource as a constant.
     */
    static IconDeclaration of( Size size, String path ) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(path);
        return new IconDeclaration() {
            @Override
            public Size size() {
                return size;
            }
            @Override
            public String path() {
                return path;
            }
            @Override
            public String toString() {
                return this.getClass().getSimpleName()+"["+
                            "size=" + ( size().equals(Size.unknown()) ? "?" : size() ) + ", " +
                            "path='" + path() + "'" +
                        "]";
            }
            @Override public int hashCode() {
                return Objects.hash(path(), size());
            }
            @Override public boolean equals( Object other ) {
                if ( other == this ) return true;
                if ( other == null ) return false;
                if ( other.getClass() != this.getClass() ) return false;
                IconDeclaration that = (IconDeclaration) other;
                return Objects.equals(this.path(), that.path())
                    && Objects.equals(this.size(), that.size());
            }
        };
    }
}

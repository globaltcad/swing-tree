package swingtree.api;

import swingtree.UI;

import javax.swing.Icon;
import javax.swing.ImageIcon;
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
 *  in the SwingTree API like this
 *  just like you would use the {@link ImageIcon} class:
 *  <pre>{@code
 *  UI.button(Icons.ADD)
 *  .onClick( it -> vm.add() )
 *  }</pre>
 *
 *  The reason why enums should be used instead of Strings is
 *  so that you have some more compile time safety in your application!
 *  When it comes to resource loading Strings are brittle because they
 *  are susceptible to typos and refactoring.
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
     * @return An {@link Optional} that contains the {@link ImageIcon}
     *         if the icon resource was found, otherwise an empty {@link Optional}.
     */
    default Optional<ImageIcon> find() { return UI.findIcon(path()); }
}

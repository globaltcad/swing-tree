package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.api.Configurator;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class models the state of an individual table/tree/list/drop down cell alongside
 * various properties that a cell should have, like for example
 * the value of the cell, its position within the component
 * as well as a {@link CellDelegate#view()} (renderer/editor) in the form of an AWT {@link Component}
 * which may or may not be replaced or modified.
 * <br>
 * The {@link CellDelegate} is exposed to the {@link RenderAs#as(Configurator)}
 * method after a {@link CellBuilder#when(Class)} call as part of various
 * cell builder APIs like: <br>
 * <ul>
 *     <li>{@link UIForTable#withCell(Configurator)}</li>
 *     <li>{@link UIForTable#withCells(Configurator)}</li>
 *     <li>{@link UIForTable#withCellForColumn(String, Configurator)} </li>
 *     <li>{@link UIForTable#withCellsForColumn(String, Configurator)} </li>
 *     <li>{@link UIForTable#withCellForColumn(int, Configurator)} </li>
 *     <li>{@link UIForTable#withCellsForColumn(int, Configurator)} </li>
 *     <li>{@link UIForList#withCell(Configurator)} </li>
 *     <li>{@link UIForList#withCells(Configurator)} </li>
 * </ul>
 * When configuring your cell, you may use methods like
 * {@link CellDelegate#view(Component)} or {@link CellDelegate#withRenderer(Consumer)}
 * to define how the cell should be rendered.
 * <p>
 * Note that the {@link CellDelegate#isEditing()} flag determines
 * two important modes in which this class is exposed to {@link RenderAs#as(Configurator)}.
 * If the {@code isEditing()} is true, then you are expected to configure a
 * cell editor component for the {@link CellDelegate#view()} property.
 * If the {@code isEditing()} is false, then you are expected to configure a simple
 * cell renderer component as the {@link CellDelegate#view()} property.<br>
 * Note that for each state of the {@code isEditing()} flag, the view component
 * is persisted across multiple calls to the {@link RenderAs#as(Configurator)} method.
 * <p>
 * This design allows you to easily define and continuously update both a
 * renderer and an editor for a cell on a single call to the {@link RenderAs#as(Configurator)} method, and then
 * to update the renderer or editor in every subsequent call to the same method.
 *
 * @param <V> The value type of the entry of this {@link CellDelegate}.
 */
public final class CellDelegate<C extends JComponent, V>
{
    private static final Logger log = LoggerFactory.getLogger(CellDelegate.class);

    public static <C extends JComponent, V> CellDelegate<C, V> of(
        @Nullable Component lastRenderer,
        C                   owner,
        @Nullable V         value,
        boolean             isSelected,
        boolean             hasFocus,
        boolean             isEditing,
        boolean             isExpanded,
        boolean             isLeaf,
        int                 row,
        int                 column,
        Supplier<Component> defaultRenderSource
    ) {
        List<String> toolTips = new ArrayList<>();
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            isEditing,
            isExpanded,
            isLeaf,
            row,
            column,
            lastRenderer,
            toolTips,
            null,
            defaultRenderSource
        );
    }

    private final C                   owner;
    private final @Nullable V         value;
    private final boolean             isSelected;
    private final boolean             hasFocus;
    private final boolean             isEditing;
    private final boolean             isExpanded;
    private final boolean             isLeaf;
    private final int                 row;
    private final int                 column;
    private final @Nullable Component view;
    private final List<String>        toolTips;
    private final @Nullable Object    presentationValue;
    private final Supplier<Component> defaultRenderSource;


    private CellDelegate(
        C                   owner,
        @Nullable V         value,
        boolean             isSelected,
        boolean             hasFocus,
        boolean             isEditing,
        boolean             isExpanded,
        boolean             isLeaf,
        int                 row,
        int                 column,
        @Nullable Component renderer,
        List<String>        toolTips,
        @Nullable Object    presentationValue,
        Supplier<Component> defaultRenderSource
    ) {
        this.owner               = Objects.requireNonNull(owner);
        this.value               = value;
        this.isSelected          = isSelected;
        this.hasFocus            = hasFocus;
        this.isEditing           = isEditing;
        this.isExpanded          = isExpanded;
        this.isLeaf              = isLeaf;
        this.row                 = row;
        this.column              = column;
        this.view = renderer;
        this.toolTips            = Objects.requireNonNull(toolTips);
        this.presentationValue   = presentationValue;
        this.defaultRenderSource = Objects.requireNonNull(defaultRenderSource);
    }

    /**
     *  Returns the owner of this cell, i.e. the component
     *  which contains this cell, like a table, list or drop down.
     *
     * @return The owner of this cell, typically a table, list or drop down.
     */
    public C getOwner() {
        return owner;
    }

    /**
     *  Returns the value of this cell, which is the data
     *  that this cell represents. The value is wrapped in an
     *  {@link Optional} to indicate that the value may be null.
     *  A cell value is typically a string, number or custom user object.
     *
     * @return An optional of the value of this cell, or an empty optional if the value is null.
     */
    public Optional<V> value() {
        return Optional.ofNullable(value);
    }

    /**
     *  Returns the value of this cell as a string, if the value
     *  is not null. If the value is null, then an empty optional
     *  is returned.
     *
     * @return An optional of the value of this cell as a string,
     *         or an empty optional if the value is null.
     */
    public Optional<String> valueAsString() {
        return value().map(Object::toString);
    }

    /**
     *  The flag returned by this method indicates whether this cell
     *  is selected or not. A cell is selected when the user interacts
     *  with it, like clicking on it or navigating to it using the keyboard.
     *  You may want to use this flag to change the appearance of the cell
     *  when it is selected. For example, you may want to highlight the cell
     *  by changing its background color.
     *
     * @return True if the cell is selected, false otherwise.
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     *  Just like any other component, a cell may have focus or not.
     *  The focus is typically indicated by a border around the cell.
     *  It is an important property to consider when designing your cell
     *  renderer, as you may want to change the appearance of the cell
     *  when it has focus.
     *
     *  @return True if the cell has focus, false otherwise.
     */
    public boolean hasFocus() {
        return hasFocus;
    }

    /**
     *  This method returns true if the cell is currently being edited.
     *  A cell is typically edited when the user double-clicks on it
     *  or presses the F2 key. When a cell is being edited, then the cell
     *  renderer wrapped by this cell will be used as an editor.
     *  You may want to use this flag to change the appearance of the cell
     *  when it is being edited. For example, you may want to show a text
     *  field instead of a label when the cell is being edited.
     *
     * @return True if the cell is being edited, false otherwise.
     *         Note that you can reliably say that when this flag
     *         is true, then the cell builder is being used to construct
     *         or maintain an editor.
     */
    public boolean isEditing() {
        return isEditing;
    }

    /**
     *  This method returns true if the cell is expanded, i.e. if it
     *  is a parent cell in a {@link javax.swing.JTree}.
     *  You may want to use this flag to change the appearance of the cell
     *  when it is expanded.You may, for example, want to show a different
     *  icon when the cell is expanded.
     *
     * @return True if the cell is expanded, false otherwise.
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    /**
     *  This method returns true if the cell is a leaf, i.e. if it
     *  is a child cell in a {@link javax.swing.JTree}.
     *  You may want to use this flag to change the appearance of the cell
     *  when it is a leaf. You may, for example, want to show a different
     *  icon when the cell is a leaf.
     *
     * @return True if the cell is a leaf, false otherwise.
     */
    public boolean isLeaf() {
        return isLeaf;
    }

    /**
     *  Exposes a list of tool tips that should be shown when the user
     *  hovers over the cell. The tool tips are strings that provide
     *  additional information about the cell to the user.
     *
     * @return An unmodifiable list of tool tips that should be shown when the user hovers over the cell.
     */
    public List<String> toolTips() {
        return Collections.unmodifiableList(toolTips);
    }

    /**
     *  Gives you the row index of the cell in the table, list or drop down.
     *  It tells you the location of the cell in the vertical direction.
     *
     * @return The row index of the cell in the table, list or drop down.
     */
    public int row() {
        return row;
    }

    /**
     *  Gives you the column index of the cell in the table, list or drop down.
     *  It tells you the location of the cell in the horizontal direction.
     *
     * @return The column index of the cell in the table, list or drop down.
     */
    public int column() {
        return column;
    }

    /**
     *  Returns the renderer/editor of this cell, which is the component
     *  that is used to display the cell to the user. The view
     *  is typically a label, text field or some other custom component.
     *  It is wrapped in an {@link Optional} to clearly indicate
     *  that it may be null.<br>
     *  Note that in case of the {@link CellDelegate#isEditing()} method
     *  returning true, the component stored in this optional is used as an editor.
     *  If the cell is not being edited, then the component is used as a renderer.<br>
     *  Two components are persisted across multiple calls to the
     *  {@link CellBuilder}s {@link RenderAs#as(Configurator)} method, one
     *  for the renderer and one for the editor. (So technically there are two views)<br>
     *  Also note that not all types of components are suitable to
     *  be used as editors. For example, a label is not suitable to be used as an editor.
     *  Instead, you should use a text field or a combo box as an editor.<br>
     *  If a component is not suitable to be used as an editor, then it
     *  will simply be ignored in exchange for a default editor.
     *
     * @return An optional of the view of this cell, or an empty optional if the view is null.
     *         In case of the {@link CellDelegate#isEditing()} method returning true,
     *         the component stored in this optional is used as an editor.
     *         The cell will remember the renderer and editor components across multiple calls
     *         to the {@link CellBuilder}s {@link RenderAs#as(Configurator)} method.
     */
    public OptionalUI<Component> view() {
        return OptionalUI.ofNullable(view);
    }

    /**
     *  Allows you to configure the view of this cell by providing
     *  a configurator lambda, which takes an {@link OptionalUI} of the
     *  current renderer and returns a (potentially updated) {@link OptionalUI}
     *  of the new renderer. <br>
     *  The benefit of using this method is that you can easily initialize
     *  the renderer with a new component through the {@link OptionalUI#orGetUi(Supplier)}
     *  method, and then update it in every refresh coll inside the
     *  {@link OptionalUI#update(java.util.function.Function)} method. <br>
     *  This may look like the following:
     *  <pre>{@code
     *      UI.table()
     *      .withRenderer( it -> it
     *          .when(Object.class).as( cell -> cell
     *              .view( comp -> comp
     *                  .update( r -> { r.setText(cell.valueAsString().orElse("")); return r; } )
     *                  .orGetUi( () -> UI.textField().withBackground(Color.CYAN) )
     *              )
     *          )
     *      )
     *      // ...
     *  }</pre>
     *  In this example, the view is initialized with a text field
     *  if it is not present, and then the text field is continuously updated
     *  with the value of the cell. <br>
     *
     * @param configurator The {@link Configurator} lambda which takes an {@link OptionalUI}
     *                     of the current view and returns a (potentially updated or initialized)
     *                     {@link OptionalUI} of the new view.
     * @return An updated cell delegate object with the new view.
     *        If the configurator returns an empty optional, then the view
     *        of the cell will be reset to null.
     */
    public CellDelegate<C,V> view( Configurator<OptionalUI<Component>> configurator ) {
        OptionalUI<Component> newRenderer = configurator.configure(view());
        return _withRenderer(newRenderer.orElseNullable(null));
    }

    /**
     *  Creates an updated cell delegate object with the given component
     *  as the view (renderer/editor) of the cell. view is the
     *  component that is used to render the cell to the user. It is
     *  typically a label, text field or some other custom component.
     *  <br>
     *  Note that in case of the {@link CellDelegate#isEditing()} method
     *  returning true, this {@link CellDelegate} is used for constructing
     *  or maintaining an editor. If the cell is not being edited, then
     *  this {@link CellDelegate} is used for rendering.<br>
     *  Either way, the component is memorized across multiple calls to the
     *  {@link CellBuilder}s {@link RenderAs#as(Configurator)} method.
     *
     * @param component The component to be used as the view of the cell.
     * @return An updated cell delegate object with the new view to
     *          serve as the renderer/editor of the cell.
     */
    public CellDelegate<C, V> view( Component component ) {
        return _withRenderer(component);
    }

    public CellDelegate<C, V> withRenderer( Consumer<Graphics2D> painter ) {
        return view(new Component() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                painter.accept((Graphics2D) g);
            }
            /*
                 The following methods are overridden as a performance measure to
                 to prune code-paths are often called in the case of renders
                 but which we know are unnecessary.  Great care should be taken
                 when writing your own renderer to weigh the benefits and
                 drawbacks of overriding methods like these.
             */
            @Override
            public boolean isOpaque() {
                Color back = getBackground();
                Component p = getParent();
                if (p != null) {
                    p = p.getParent();
                }
                // p should now be the JTable.
                boolean colorMatch = (back != null) && (p != null) &&
                        back.equals(p.getBackground()) &&
                        p.isOpaque();
                return !colorMatch && super.isOpaque();
            }
            @Override
            public void invalidate() {}
            @Override
            public void validate() {}
            @Override
            public void revalidate() {}
            @Override
            public void repaint(long tm, int x, int y, int width, int height) {}
            @Override
            public void repaint() {}
            @Override
            public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) { }
        });
    }

    /**
     *   Creates an updated cell delegate object with the default cell
     *   renderer component based on the {@link javax.swing.DefaultListCellRenderer},
     *   {@link javax.swing.table.DefaultTableCellRenderer} and {@link javax.swing.tree.DefaultTreeCellRenderer}
     *   classes.
     *
     * @return An updated cell delegate object with the default renderer component.
     *         This will override any custom renderer that was previously specified.
     */
    public CellDelegate<C, V> withDefaultView() {
        try {
            return this.view(this.defaultRenderSource.get());
        } catch (Exception e) {
            log.error("Failed to create default renderer!", e);
        }
        return this;
    }

    public CellDelegate<C, V> _withRenderer(@Nullable Component component) {
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            isEditing,
            isExpanded,
            isLeaf,
            row,
            column,
            component,
            toolTips,
            presentationValue,
            defaultRenderSource
        );
    }

    public CellDelegate<C, V> withToolTip( String toolTip ) {
        ArrayList<String> newToolTips = new ArrayList<>(toolTips);
        newToolTips.add(toolTip);
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            isEditing,
            isExpanded,
            isLeaf,
            row,
            column,
            view,
            newToolTips,
            presentationValue,
            defaultRenderSource
        );
    }

    /**
     *  The presentation value is the first choice of the
     *  cell renderer to be used for rendering and presentation
     *  to the user. If it does not exist then the regular
     *  cell value is used for rendering.
     *
     * @return An optional of the presentation value.
     *         It may be an empty optional if no presentation value was specified.
     */
    public Optional<Object> presentationValue() {
        return Optional.ofNullable(presentationValue);
    }

    /**
     *  Represents the value how it should be displayed
     *  to the user by the cell renderer. By default, this
     *  value is null, in which case the regular cell value is
     *  presented to the user.
     *
     * @param toBeShown The object which should be used by the renderer
     *                  to present to the user, typically a String.
     * @return An updated cell delegate object with the new presentation value.
     */
    public CellDelegate<C, V> withPresentationValue( @Nullable Object toBeShown ) {
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            isEditing,
            isExpanded,
            isLeaf,
            row,
            column,
            view,
            toolTips,
            toBeShown,
            defaultRenderSource
        );
    }

}

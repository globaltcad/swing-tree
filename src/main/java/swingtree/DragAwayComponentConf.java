package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Action;
import swingtree.api.Configurator;
import swingtree.api.IconDeclaration;
import swingtree.layout.Position;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Image;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.util.Objects;
import java.util.Optional;

/**
 *  A value based builder object for configuring drag away events of a component
 *  using the {@link UIForAnySwing#withDragAway(Configurator)} method, where an instance of this
 *  class is passed to the {@link Configurator} lambda defining how
 *  the component should behave and look when dragged away. <br>
 *  <br>
 *  <b>So you may for example use the following methods to define the visual appearance of the
 *  drag away operation:</b>
 *  <ul>
 *      <li>{@link #enabled(boolean)} -
 *          Enables or disables the drag away operation entirely.
 *      </li>
 *      <li>{@link #opacity(double)} -
 *          Sets the opacity of the drag image. The value should be between 0.0 and 1.0.
 *          A value of 0.0 means the drag image is completely transparent and a value of 1.0
 *          means the drag image is completely opaque.
 *      </li>
 *      <li>{@link #cursor(UI.Cursor)} -
 *          Sets the cursor to be used during the drag operation.
 *          The default cursor is {@link UI.Cursor#DEFAULT}.
 *      </li>
 *      <li>{@link #customDragImage(Image)} -
 *          Sets a custom drag image to be used during the drag operation.
 *          If no custom drag image is set, the component will be rendered int
 *          a drag image during the drag operation.
 *      </li>
 *  </ul>
 *  <br>
 *  <b>For listening to drag away events, the following methods are available:</b>
 *  <ul>
 *      <li>{@link #onDragEnter(Action)} - 
 *           Called as the cursor's hotspot enters a platform-dependent drop site.
 *           The provided {@link Action} is invoked when all the following conditions are true:
 *           <ul>
 *           <li>The cursor's hotspot enters the operable part of a platform-
 *           dependent drop site.
 *           <li>The drop site is active.
 *           <li>The drop site accepts the drag.
 *           </ul>
 *      </li>
 *      <li>{@link #onDragMove(Action)} -
 *          Called whenever the mouse is moved during a drag operation.
 *      </li>
 *      <li>{@link #onDragOver(Action)} -
 *          Called as the cursor's hotspot moves over a platform-dependent drop site.
 *          This method is invoked when all the following conditions are true:
 *          <ul>
 *          <li>The cursor's hotspot has moved, but still intersects the
 *          operable part of the drop site associated with the previous
 *          dragEnter() invocation.
 *          <li>The drop site is still active.
 *          <li>The drop site accepts the drag.
 *          </ul>
 *      </li>
 *      <li>{@link #onDropActionChanged(Action)} -
 *          Called when the user has modified the drop gesture.
 *          This method is invoked when the state of the input
 *          device(s) that the user is interacting with changes.
 *          Such devices are typically the mouse buttons or keyboard
 *          modifiers that the user is interacting with.
 *      </li>
 *      <li>{@link #onDragExit(Action)} -
 *          Called as the cursor's hotspot exits a platform-dependent drop site.
 *          This {@link Action} is invoked when any of the following conditions are true:
 *          <ul>
 *          <li>The cursor's hotspot no longer intersects the operable part
 *          of the drop site associated with the previous dragEnter() invocation.
 *          </ul>
 *          OR
 *          <ul>
 *          <li>The drop site associated with the previous dragEnter() invocation
 *          is no longer active.
 *          </ul>
 *          OR
 *          <ul>
 *          <li> The drop site associated with the previous dragEnter() invocation
 *          has rejected the drag.
 *          </ul>
 *      </li>
 *      <li>{@link #onDragDropEnd(Action)} -
 *          This {@link Action} is invoked to signify that the Drag and Drop
 *          operation is complete. The getDropSuccess() method of
 *          the <code>DragSourceDropEvent</code> can be used to
 *          determine the termination state. The getDropAction() method
 *      </li>
 *  </ul>
 *          
 * @param <C> The type of the component to be dragged away.
 */
public final class DragAwayComponentConf<C extends JComponent>
{
    private static final Action NO_ACTION = e -> {};

    public static <C extends JComponent> DragAwayComponentConf<C> of(
        C        component,
        Position mousePosition
    ) {
        return new DragAwayComponentConf<>(
                component, mousePosition, true, 1, UI.Cursor.DEFAULT, null, null,
                UI.DragAction.COPY_OR_MOVE, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION
        );
    }

    private final C                      _component;
    private final Position               _mousePosition;
    private final boolean                _enabled;
    private final double                 _opacity;
    private final UI.Cursor              _cursor;
    private final @Nullable Image        _customDragImage;
    private final @Nullable Transferable _payload;
    private final UI.DragAction          _dragAction;
    private final Action<ComponentDelegate<C, DragSourceDragEvent>> _onDragEnter;
    private final Action<ComponentDelegate<C, DragSourceDragEvent>> _onDragMove;
    private final Action<ComponentDelegate<C, DragSourceDragEvent>> _onDragOver;
    private final Action<ComponentDelegate<C, DragSourceDragEvent>> _onDropActionChanged;
    private final Action<ComponentDelegate<C, DragSourceEvent    >> _onDragExit;
    private final Action<ComponentDelegate<C, DragSourceDropEvent>> _onDragDropEnd;


    private DragAwayComponentConf(
        C                      component,
        Position               mousePosition,
        boolean                enabled,
        double                 opacity,
        UI.Cursor              cursor,
        @Nullable Image        customDragImage,
        @Nullable Transferable payload,
        UI.DragAction          dragAction,
        Action<ComponentDelegate<C, DragSourceDragEvent>> onDragEnter,
        Action<ComponentDelegate<C, DragSourceDragEvent>> onDragMove,
        Action<ComponentDelegate<C, DragSourceDragEvent>> onDragOver,
        Action<ComponentDelegate<C, DragSourceDragEvent>> onDropActionChanged,
        Action<ComponentDelegate<C, DragSourceEvent>>     onDragExit,
        Action<ComponentDelegate<C, DragSourceDropEvent>> onDragDropEnd
    ) {
        _component           = Objects.requireNonNull(component);
        _mousePosition       = Objects.requireNonNull(mousePosition);
        _enabled             = enabled;
        _opacity             = opacity;
        _cursor              = Objects.requireNonNull(cursor);
        _customDragImage     = customDragImage;
        _payload             = payload;
        _dragAction          = Objects.requireNonNull(dragAction);
        _onDragEnter         = Objects.requireNonNull(onDragEnter);
        _onDragMove          = Objects.requireNonNull(onDragMove);
        _onDragOver          = Objects.requireNonNull(onDragOver);
        _onDropActionChanged = Objects.requireNonNull(onDropActionChanged);
        _onDragExit          = Objects.requireNonNull(onDragExit);
        _onDragDropEnd       = Objects.requireNonNull(onDragDropEnd);
    }

    /**
     *  Exposes the component that can be dragged away by the user.
     *  You may use this method configure the drag away operation based
     *  on the component's properties.
     *
     * @return The component to be dragged away.
     */
    public C component() {
        return _component;
    }

    /**
     *  Exposes the current mouse position of the drag operation.
     *  The mouse position is the location of the mouse cursor when the drag operation
     *  was initiated.
     *
     * @return The mouse position of the drag operation.
     */
    public Position mousePosition() {
        return _mousePosition;
    }

    /**
     *  Exposes the current x-coordinate of the mouse position of the drag operation.
     *  The x-coordinate is the horizontal position of the mouse cursor when the drag operation
     *  was initiated.
     *
     * @return The x-coordinate of the mouse position of the drag operation.
     */
    public double mouseX() {
        return _mousePosition.x();
    }

    /**
     *  Exposes the current y-coordinate of the mouse position of the drag operation.
     *  The y-coordinate is the vertical position of the mouse cursor when the drag operation
     *  was initiated.
     *
     * @return The y-coordinate of the mouse position of the drag operation.
     */
    public double mouseY() {
        return _mousePosition.y();
    }

    /**
     *  Exposes the current enabled state of the drag away operation.
     *  If the drag away operation is disabled, the user will not be able to drag
     *  the component away.
     *
     * @return <code>true</code> if the drag away operation is enabled, <code>false</code> otherwise.
     */
    public boolean enabled() {
        return _enabled;
    }

    /**
     *  Exposes the current opacity of the drag image.
     *  The value should be between 0.0 and 1.0.
     *  A value of 0.0 means the drag image is completely transparent and a value of 1.0
     *  means the drag image is completely opaque.
     *
     * @return The opacity of the drag image in the form of a double value
     *         ranging from 0.0 to 1.0.
     */
    public double opacity() {
        return _opacity;
    }

    /**
     *  Gives you the current cursor to be used during the drag operation.
     *  The default cursor is {@link UI.Cursor#DEFAULT}.
     *  You may want to use {@link UI.Cursor#MOVE} for example to indicate that the user
     *  is moving the component.
     *
     * @return The cursor to be used during the drag operation.
     */
    public UI.Cursor cursor() {
        return _cursor;
    }

    /**
     *  Returns an {@link Optional} of the current custom drag image to be used
     *  as a visual representation of the component during the drag operation.
     *  If no custom drag image is set, the component will be rendered into
     *  a drag image during the drag operation. <br>
     *  Note that a visualization of the component outside the application
     *  window depends on the platform and the implementation of the drag and drop
     *  operation, see {@link DragSource#isDragImageSupported()} for more information.
     *
     * @return The custom drag image to be used during the drag operation
     *         or <code>Optional.empty()</code> if no custom drag image is set.
     */
    public Optional<Image> customDragImage() {
        return Optional.ofNullable(_customDragImage);
    }

    /**
     *  Returns an {@link Optional} of the current {@link Transferable} object
     *  that is used to transfer the data during the drag operation.
     *  If no custom {@link Transferable} object is set, the default
     *  {@link Transferable} object will be used to transfer the data.
     *
     * @return The {@link Transferable} object that is used to transfer the data
     *         during the drag operation or <code>Optional.empty()</code> if no
     *         custom {@link Transferable} object is set.
     */
    public Optional<Transferable> payload() {
        return Optional.ofNullable(_payload);
    }

    /**
     *  Returns the current drag action of this drag away operation.
     *  The drag action constant is used to specify the action(s) supported by the drag source.
     *  If no drag action is set, the default drag action is {@link UI.DragAction#NONE}.
     *  The {@link UI.DragAction} enum type used here, directly maps to the
     *  int based constants defined in {@link java.awt.dnd.DnDConstants}.
     *
     * @return The drag action of this drag away operation.
     */
    public UI.DragAction dragAction() {
        return _dragAction;
    }

    /**
     *  Returns the {@link Action} that is invoked when the cursor's hotspot enters
     *  a platform-dependent drop site. This method is invoked when all the following
     *  conditions are true:
     *  <ul>
     *  <li>The cursor's hotspot enters the operable part of a platform-dependent drop site.
     *  <li>The drop site is active.
     *  <li>The drop site accepts the drag.
     *  </ul>
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragEnter(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @return The {@link Action} that is invoked when the cursor's hotspot enters
     *         a platform-dependent drop site.
     */
    public Action<ComponentDelegate<C, DragSourceDragEvent>> onDragEnter() {
        return _onDragEnter;
    }

    /**
     *  Returns the {@link Action} that is invoked whenever the mouse is moved during a drag operation.
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceMotionListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceMotionListener#dragMouseMoved(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceMotionListener} implementation registered to listen to
     *      AWT native drag operations, through {@link java.awt.dnd.DragSource#addDragSourceMotionListener}.
     *  </p>
     *
     * @return The {@link Action} that is invoked whenever the mouse is moved during a drag operation.
     */
    public Action<ComponentDelegate<C, DragSourceDragEvent>> onDragMove() {
        return _onDragMove;
    }

    /**
     *  Returns the {@link Action} that is invoked as the cursor's hotspot moves over a platform-dependent drop site.
     *  This method is invoked when all the following conditions are true:
     *  <ul>
     *  <li>The cursor's hotspot has moved, but still intersects the operable part of the drop site
     *  associated with the previous {@link #onDragEnter(Action)} invocation.
     *  <li>The drop site is still active.
     *  <li>The drop site accepts the drag.
     *  </ul>
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragOver(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @return The {@link Action} that is invoked as the cursor's hotspot moves over a platform-dependent drop site.
     */
    public Action<ComponentDelegate<C, DragSourceDragEvent>> onDragOver() {
        return _onDragOver;
    }

    /**
     *  Returns the {@link Action} that is invoked when the user has modified the drop gesture.
     *  This method is invoked when the state of the input device(s) that the user is interacting with changes.
     *  Such devices are typically the mouse buttons or keyboard modifiers that the user is interacting with.
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dropActionChanged(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @return The {@link Action} that is invoked when the user has modified the drop gesture.
     */
    public Action<ComponentDelegate<C, DragSourceDragEvent>> onDropActionChanged() {
        return _onDropActionChanged;
    }

    /**
     *  Returns the {@link Action} that is invoked as the cursor's hotspot exits a platform-dependent drop site.
     *  This {@link Action} is invoked when any of the following conditions are true:
     *  <ul>
     *  <li>
     *      The cursor's hotspot no longer intersects the operable part of the
     *      drop site associated with the previous {@link #onDragEnter(Action)} invocation.
     *  </ul>
     *  OR
     *  <ul>
     *  <li>
     *      The drop site associated with the previous {@link #onDragEnter(Action)} invocation
     *      is no longer active.
     *  </ul>
     *  OR
     *  <ul>
     *  <li>
     *      The drop site associated with the previous {@link #onDragEnter(Action)} invocation
     *      has rejected the drag.
     *  </ul>
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragExit(DragSourceEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @return The {@link Action} that is invoked as the cursor's hotspot exits a platform-dependent drop site.
     */
    public Action<ComponentDelegate<C, DragSourceEvent>> onDragExit() {
        return _onDragExit;
    }

    /**
     *  Returns the {@link Action} that is invoked to signify that the Drag and Drop
     *  operation is complete.
     *  The getDropSuccess() method of the <code>DragSourceDropEvent</code> can be used to
     *  determine the termination state. The getDropAction() method
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragDropEnd(DragSourceDropEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @return The {@link Action} that is invoked to signify that the Drag and Drop
     *         operation is complete.
     */
    public Action<ComponentDelegate<C, DragSourceDropEvent>> onDragDropEnd() {
        return _onDragDropEnd;
    }

    /**
     *  Enables or disables the drag away operation entirely.
     *
     * @param enabled <code>true</code> to enable the drag away operation, <code>false</code> to disable it.
     * @return A new {@link DragAwayComponentConf} instance with the updated enabled state.
     */
    public DragAwayComponentConf<C> enabled( boolean enabled ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, enabled, _opacity, _cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
            );
    }

    /**
     *  Allows you to define the opacity of the drag image.
     *  The value should be between {@code 0.0} and {@code 1.0}, where
     *  the number {@code 0.0} means the drag image is completely
     *  transparent and a value of {@code 1.0}
     *  means the drag image is completely opaque.
     *  <br><p>
     *      Note that the opacity will automatically applied to the {@link #customDragImage(Image)}
     *      if a custom drag image is set. <br>
     *      If no custom drag image is set, the opacity will be applied to the
     *      drag image that is created from the component during the drag operation.
     *  </p>
     *
     * @param opacity The opacity of the drag image in the form of a double value
     *                ranging from {@code 0.0} to {@code 1.0}.
     * @return A new {@link DragAwayComponentConf} instance with the updated opacity.
     */
    public DragAwayComponentConf<C> opacity( double opacity ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, opacity, _cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Configures the mouse cursor to be displayed during the drag operation.
     *  The default cursor is {@link UI.Cursor#DEFAULT}.
     *  You may want to use {@link UI.Cursor#MOVE} for example to indicate that the user
     *  is moving the component.
     *
     * @param cursor The cursor to be used during the drag operation.
     * @return A new {@link DragAwayComponentConf} instance with the updated cursor.
     */
    public DragAwayComponentConf<C> cursor( UI.Cursor cursor ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Allows you to define a custom drag image to be used during the drag operation.
     *  If no custom drag image is set, the component appearance will be used
     *  as the drag image during the drag operation. <br>
     *  <br>
     *  Note that a visualization of the component outside the application
     *  window depends on the platform and the implementation of the drag and drop
     *  operation, see {@link DragSource#isDragImageSupported()} for more information.
     *
     * @param customDragImage The custom drag image to be used during the drag operation.
     * @return A new {@link DragAwayComponentConf} instance with the updated custom drag image.
     */
    public DragAwayComponentConf<C> customDragImage( Image customDragImage ) {
        Objects.requireNonNull(customDragImage);
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Allows you to define a custom drag image to be used during the drag operation.
     *  If no custom drag image is set, the component appearance will be used
     *  as the drag image during the drag operation. <br>
     *  <br>
     *  Note that a visualization of the component outside the application
     *  window depends on the platform and the implementation of the drag and drop
     *  operation, see {@link DragSource#isDragImageSupported()} for more information.
     *
     * @param icon The icon declaration to be used as the custom drag image.
     *             An {@link IconDeclaration} is a constant which defines the path to the icon image.
     * @return A new {@link DragAwayComponentConf} instance with the updated custom drag image.
     */
    public DragAwayComponentConf<C> customDragImage( IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return icon.find().map(ImageIcon::getImage).map(this::customDragImage).orElse(this);
    }

    /**
     *  Allows you to define a custom drag image to be used during the drag operation.
     *  If no custom drag image is set, the component appearance will be used
     *  as the drag image during the drag operation. <br>
     *  <br>
     *  Note that a visualization of the component outside the application
     *  window depends on the platform and the implementation of the drag and drop
     *  operation, see {@link DragSource#isDragImageSupported()} for more information.
     *
     * @param path The path to the custom drag image to be used during the drag operation.
     * @return A new {@link DragAwayComponentConf} instance with the updated custom drag image.
     */
    public DragAwayComponentConf<C> customDragImage( String path ) {
        return customDragImage(IconDeclaration.of(path));
    }

    /**
     *  Use this to specify the payload of this drag away operation
     *  in the form of a {@link Transferable} object.
     *  The {@link Transferable} object is used to transfer data in various
     *  data formats during the drag operation so that it may eventually be
     *  dropped at a drop site, like for example {@link UIForAnySwing#withDropSite(Configurator)}. <br>
     *  If no custom {@link Transferable} object is set,
     *  {@link java.awt.datatransfer.StringSelection} with an
     *  empty string is used as the default payload.
     *
     * @param payload The {@link Transferable} object that is used to transfer the data
     *                     during the drag operation.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Transferable} object.
     */
    public DragAwayComponentConf<C> payload( Transferable payload ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Allows you to specify the payload of this drag away operation
     *  in the form of a {@link String} object to be transferred during the
     *  drag away operation. <br>
     *  If no payload object is specified an empty string will be used as the default payload.
     *
     * @param payload The {@link String} object that is used to transfer the data
     *                during the drag operation.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Transferable}
     *         of the {@link StringSelection} subtype.
     */
    public DragAwayComponentConf<C> payload( String payload ) {
        return payload(new StringSelection(payload));
    }

    /**
     *  Use this to specify the type drag action of this drag away operation by
     *  passing one of the following {@link UI.DragAction} enum values:
     *  <ul>
     *      <li>{@link UI.DragAction#NONE}
     *      <li>{@link UI.DragAction#COPY}
     *      <li>{@link UI.DragAction#MOVE}
     *      <li>{@link UI.DragAction#COPY_OR_MOVE}
     *      <li>{@link UI.DragAction#LINK}
     *  </ul>
     *  This drag action constant is used to specify the action(s) supported by the drag source.<br>
     *  If no drag action is set, the default drag action is {@link UI.DragAction#NONE}. <br>
     *  The {@link UI.DragAction} enum type used here, directly maps to the
     *  int based constants defined in {@link java.awt.dnd.DnDConstants}.
     *
     * @param dragAction The drag action of this drag away operation.
     * @return A new {@link DragAwayComponentConf} instance with the updated drag action.
     */
    public DragAwayComponentConf<C> dragAction( UI.DragAction dragAction ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, _payload, dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Use this to specify an {@link Action} that is invoked when the cursor's hotspot enters
     *  a platform-dependent drop site.
     *  This method is invoked when all the following conditions are true:
     *  <ul>
     *    <li>The cursor's hotspot enters the operable part of a platform-dependent drop site.
     *    <li>The drop site is active.
     *    <li>The drop site accepts the drag.
     *  </ul>
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragEnter(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @param onDragEnter The {@link Action} to be invoked when the cursor's hotspot enters
     *                    a platform-dependent drop site.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Action} for onDragEnter.
     */
    public DragAwayComponentConf<C> onDragEnter( Action<ComponentDelegate<C, DragSourceDragEvent>> onDragEnter ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, _payload, _dragAction,
                onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Use this to specify an {@link Action} that is invoked whenever the mouse is moved during a drag operation.
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceMotionListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceMotionListener#dragMouseMoved(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceMotionListener} implementation registered to listen to
     *      AWT native drag operations, through {@link java.awt.dnd.DragSource#addDragSourceMotionListener}.
     *  </p>
     *
     * @param onDragMove The {@link Action} to be invoked whenever the mouse is moved during a drag operation.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Action} for onDragMove.
     */
    public DragAwayComponentConf<C> onDragMove( Action<ComponentDelegate<C, DragSourceDragEvent>> onDragMove ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Use this to specify an {@link Action} that is invoked as the cursor's hotspot moves over a platform-dependent drop site.
     *  This method is invoked when all the following conditions are true:
     *  <ul>
     *  <li>The cursor's hotspot has moved, but still intersects the operable part of the drop site
     *  associated with the previous {@link #onDragEnter(Action)} invocation.
     *  <li>The drop site is still active.
     *  <li>The drop site accepts the drag.
     *  </ul>
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragOver(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @param onDragOver The {@link Action} to be invoked as the cursor's hotspot moves over a platform-dependent drop site.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Action} for onDragOver.
     */
    public DragAwayComponentConf<C> onDragOver( Action<ComponentDelegate<C, DragSourceDragEvent>> onDragOver ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Use this to specify an {@link Action} that is invoked when the user has modified the drop gesture.
     *  This method is invoked when the state of the input device(s) that the user is interacting with changes.
     *  Such devices are typically the mouse buttons or keyboard modifiers that the user is interacting with.
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dropActionChanged(DragSourceDragEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @param onDropActionChanged The {@link Action} to be invoked when the user has modified the drop gesture.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Action} for onDropActionChanged.
     */
    public DragAwayComponentConf<C> onDropActionChanged( Action<ComponentDelegate<C, DragSourceDragEvent>> onDropActionChanged ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Use this to specify an {@link Action} that is invoked as the cursor's hotspot exits a platform-dependent drop site.
     *  This {@link Action} is invoked when any of the following conditions are true:
     *  <ul>
     *  <li>
     *      The cursor's hotspot no longer intersects the operable part of the
     *      drop site associated with the previous {@link #onDragEnter(Action)} invocation.
     *  </ul>
     *  OR
     *  <ul>
     *  <li>
     *      The drop site associated with the previous {@link #onDragEnter(Action)} invocation
     *      is no longer active.
     *  </ul>
     *  OR
     *  <ul>
     *  <li>
     *      The drop site associated with the previous {@link #onDragEnter(Action)} invocation
     *      has rejected the drag.
     *  </ul>
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragExit(DragSourceEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @param onDragExit The {@link Action} to be invoked as the cursor's hotspot exits a platform-dependent drop site.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Action} for onDragExit.
     */
    public DragAwayComponentConf<C> onDragExit( Action<ComponentDelegate<C, DragSourceEvent>> onDragExit ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, onDragExit, _onDragDropEnd
        );
    }

    /**
     *  Use this to specify an {@link Action} that is invoked to signify that the Drag and Drop
     *  operation is complete.
     *  The getDropSuccess() method of the <code>DragSourceDropEvent</code> can be used to
     *  determine the termination state. The getDropAction() method
     *  <br><p>
     *      For more information about how this is implemented, check out the
     *      documentation of {@link java.awt.dnd.DragSourceListener}. <br>
     *      As the supplied {@link Action} will be invoked inside the
     *      {@link java.awt.dnd.DragSourceListener#dragDropEnd(DragSourceDropEvent)} method as part of
     *      a custom {@link java.awt.dnd.DragSourceListener} implementation registered to listen to
     *      AWT native drag and drop operations, through {@link java.awt.dnd.DragGestureEvent#startDrag}
     *      and ultimately {@link DragSource#startDrag}.
     *  </p>
     *
     * @param onDragDropEnd The {@link Action} to be invoked to signify that the Drag and Drop
     *                      operation is complete.
     * @return A new {@link DragAwayComponentConf} instance with the updated {@link Action} for onDragDropEnd.
     */
    public DragAwayComponentConf<C> onDragDropEnd( Action<ComponentDelegate<C, DragSourceDropEvent>> onDragDropEnd ) {
        return new DragAwayComponentConf<>(
                _component, _mousePosition, _enabled, _opacity, _cursor, _customDragImage, _payload, _dragAction,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, onDragDropEnd
        );
    }

}

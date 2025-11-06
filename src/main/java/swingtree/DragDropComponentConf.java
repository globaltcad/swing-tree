package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import swingtree.api.Configurator;

import javax.swing.JComponent;
import java.awt.dnd.*;
import java.util.Objects;

/**
 *  A value based builder object for configuring drop target events for a component
 *  using the {@link UIForAnySwing#withDropSite(Configurator)} method, where an instance of this
 *  class is passed to the {@link Configurator} lambda defining how
 *  the component should behave and look when something is dragged and dropped
 *  on top of it. <br>
 *  <br>
 *  <b>For listening to drop events, the following methods are available:</b>
 *  <ul>
 *      <li>{@link #onDragEnter(Action)} - 
 *          Called while a drag operation is ongoing, when the mouse pointer enters
 *          the operable part of the drop site for the <code>DropTarget</code>
 *          registered with this listener.
 *      </li>
 *      <li>{@link #onDragOver(Action)} -
 *          Called when a drag operation is ongoing, while the mouse pointer is still
 *          over the operable part of the drop site for the <code>DropTarget</code>
 *          registered with this listener.
 *      </li>
 *      <li>{@link #onDropActionChanged(Action)} -
 *          Called if the user has modified
 *          the current drop gesture.
 *      </li>
 *      <li>{@link #onDragExit(Action)} -
 *          Called while a drag operation is ongoing, when the mouse pointer has
 *          exited the operable part of the drop site for the
 *          <code>DropTarget</code> registered with this listener.
 *      </li>
 *      <li>{@link #onDrop(Action)} -
 *          Called when the drag operation has terminated with a drop on
 *          the operable part of the drop site for the <code>DropTarget</code>
 *          registered with this listener.
 *          This is where the transfer of the data should take place.
 *      </li>
 *  </ul>
 *
 * @param <C> The type of the component to be dragged away.
 */
public final class DragDropComponentConf<C extends JComponent>
{
    private static final Action NO_ACTION = e -> {};
    private static final Logger log = LoggerFactory.getLogger(DragDropComponentConf.class);

    public static <C extends JComponent> DragDropComponentConf<C> of(
        C component
    ) {
        return new DragDropComponentConf<>(
                component,
                NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION
        );
    }

    private final C                           _component;
    private final Action<ComponentDelegate<C,DropTargetDragEvent>> _onDragEnter;
    private final Action<ComponentDelegate<C,DropTargetDragEvent>> _onDragOver;
    private final Action<ComponentDelegate<C,DropTargetDragEvent>> _onDropActionChanged;
    private final Action<ComponentDelegate<C,DropTargetEvent>>     _onDragExit;
    private final Action<ComponentDelegate<C,DropTargetDropEvent>> _onDragDropEnd;

    private DragDropComponentConf(
        C component,
        Action<ComponentDelegate<C,DropTargetDragEvent>> onDragEnter,
        Action<ComponentDelegate<C,DropTargetDragEvent>> onDragOver,
        Action<ComponentDelegate<C,DropTargetDragEvent>> onDropActionChanged,
        Action<ComponentDelegate<C,DropTargetEvent>>     onDragExit,
        Action<ComponentDelegate<C,DropTargetDropEvent>> onDragDropEnd
    ) {
        _component           = Objects.requireNonNull(component);
        _onDragEnter         = Objects.requireNonNull(onDragEnter);
        _onDragOver          = Objects.requireNonNull(onDragOver);
        _onDropActionChanged = Objects.requireNonNull(onDropActionChanged);
        _onDragExit          = Objects.requireNonNull(onDragExit);
        _onDragDropEnd       = Objects.requireNonNull(onDragDropEnd);
    }

    /**
     * Returns the component that this configuration is for
     * so that it can be used in the {@link UIForAnySwing#withDropSite(Configurator)}
     * method to configure the drop site based on the component state.
     *
     * @return The component that this configuration is for.
     */
    public C component() {
        return _component;
    }

    /**
     * The supplied action is called while a drag operation is ongoing, 
     * when the mouse pointer enters
     * the operable part of the drop site for the <code>DropTarget</code>
     * registered with this listener.
     *
     * @param action An {@link Action} with a <code>DropTargetDragEvent</code>
     *               wrapped in a {@link ComponentDelegate} passed to it.
     * @return A new {@link DragDropComponentConf} updated with the supplied action.
     */
    public DragDropComponentConf<C> onDragEnter(Action<ComponentDelegate<C,DropTargetDragEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            action,
            _onDragOver,
            _onDropActionChanged,
            _onDragExit,
            _onDragDropEnd
        );
    }

    /**
     * The supplied action is called when a drag operation is ongoing, while the mouse pointer is still
     * over the operable part of the drop site for the <code>DropTarget</code>
     * registered with this listener.
     *
     * @param action An {@link Action} with a <code>DropTargetDragEvent</code>
     *               wrapped in a {@link ComponentDelegate} passed to it.
     * @return A new {@link DragDropComponentConf} updated with the supplied action.
     */
    public DragDropComponentConf<C> onDragOver(Action<ComponentDelegate<C,DropTargetDragEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            action,
            _onDropActionChanged,
            _onDragExit,
            _onDragDropEnd
        );
    }

    /**
     * The supplied action is called if the user has modified
     * the current drop gesture.
     *
     * @param action An {@link Action} with a <code>DropTargetDragEvent</code>
     *               wrapped in a {@link ComponentDelegate} passed to it.
     * @return A new {@link DragDropComponentConf} updated with the supplied action.
     */
    public DragDropComponentConf<C> onDropActionChanged(Action<ComponentDelegate<C,DropTargetDragEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            _onDragOver,
            action,
            _onDragExit,
            _onDragDropEnd
        );
    }

    /**
     * The supplied action is called while a drag operation is ongoing, when the mouse pointer has
     * exited the operable part of the drop site for the
     * {@link DropTarget} registered with this listener.
     *
     * @param action An {@link Action} with a <code>DropTargetEvent</code>
     *               wrapped in a {@link ComponentDelegate} passed to it.
     * @return A new {@link DragDropComponentConf} updated with the supplied action.
     */
    public DragDropComponentConf<C> onDragExit(Action<ComponentDelegate<C,DropTargetEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            _onDragOver,
            _onDropActionChanged,
            action,
            _onDragDropEnd
        );
    }

    /**
     * The supplied {@link Action} is called when the drag operation 
     * has terminated with a drop on
     * the operable part of the drop site for the <code>DropTarget</code>
     * registered with this listener.
     * <p>
     * This method is responsible for undertaking
     * the transfer of the data associated with the
     * gesture. The {@link DropTargetDropEvent}
     * provides a means to obtain a {@link java.awt.datatransfer.Transferable}
     * object that represents the data object(s) to
     * be transfered.<P>
     * From this method, the <code>DropTargetListener</code>
     * shall accept or reject the drop via the
     * acceptDrop(int dropAction) or rejectDrop() methods of the
     * {@link DropTargetDropEvent} parameter.
     * <P>
     * After acceptDrop(), but not before,
     * {@link DropTargetDropEvent}'s getTransferable()
     * method may be invoked, and data transfer may be
     * performed via the returned {@link java.awt.datatransfer.Transferable}'s
     * getTransferData() method.
     * <P>
     * At the completion of a drop, an implementation
     * of this method is required to signal the success/failure
     * of the drop by passing an appropriate
     * <code>boolean</code> to the {@link DropTargetDropEvent}'s
     * dropComplete(boolean success) method.
     * <P>
     * Note: The data transfer should be completed before the call  to the
     * {@link DropTargetDropEvent}'s dropComplete(boolean success) method.
     * After that, a call to the getTransferData() method of the
     * {@link java.awt.datatransfer.Transferable} returned by
     * <code>DropTargetDropEvent.getTransferable()</code> is guaranteed to
     * succeed only if the data transfer is local; that is, only if
     * {@link DropTargetDropEvent#isLocalTransfer} returns
     * <code>true</code>. Otherwise, the behavior of the call is
     * implementation-dependent.
     *
     * @param action An {@link Action} with a {@link DropTargetDropEvent}
     *               wrapped in a {@link ComponentDelegate} passed to it.
     * @return A new {@link DragDropComponentConf} updated with the supplied action.
     */
    public DragDropComponentConf<C> onDrop( Action<ComponentDelegate<C,DropTargetDropEvent>> action ) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            _onDragOver,
            _onDropActionChanged,
            _onDragExit,
            action
        );
    }


    DropTarget toNewDropTarget() {
        return new DropTarget(
            _component,
            new DropTargetListener() {
                @Override
                public void dragEnter(DropTargetDragEvent event) {
                    try {
                        _onDragEnter.accept(new ComponentDelegate<>(_component, event));
                    } catch (Exception e) {
                        log.error(SwingTree.get().logMarker(), "Error occurred while processing drag enter event.", e);
                    }
                }

                @Override
                public void dragOver(DropTargetDragEvent event) {
                    try {
                        _onDragOver.accept(new ComponentDelegate<>(_component, event));
                    } catch (Exception e) {
                        log.error(SwingTree.get().logMarker(), "Error occurred while processing drag over event.", e);
                    }
                }

                @Override
                public void dropActionChanged(DropTargetDragEvent event) {
                    try {
                        _onDropActionChanged.accept(new ComponentDelegate<>(_component, event));
                    } catch (Exception e) {
                        log.error(SwingTree.get().logMarker(), "Error occurred while processing drop action changed event.", e);
                    }
                }

                @Override
                public void dragExit(DropTargetEvent event) {
                    try {
                        _onDragExit.accept(new ComponentDelegate<>(_component, event));
                    } catch (Exception e) {
                        log.error(SwingTree.get().logMarker(), "Error occurred while processing drag exit event.", e);
                    }
                }

                @Override
                public void drop(DropTargetDropEvent event) {
                    try {
                        _onDragDropEnd.accept(new ComponentDelegate<>(_component, event));
                    } catch (Exception e) {
                        log.error(SwingTree.get().logMarker(), "Error occurred while processing drop event.", e);
                    }
                }
            }
        );
    }
}

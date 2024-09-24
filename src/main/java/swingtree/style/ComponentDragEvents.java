package swingtree.style;

import sprouts.Action;
import swingtree.DragAwayComponentConf;
import swingtree.DragDropEventComponentDelegate;
import swingtree.DragOverEventComponentDelegate;
import swingtree.api.Configurator;

import javax.swing.JComponent;
import java.awt.event.MouseEvent;

final class ComponentDragEvents<C extends JComponent>
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ComponentDragEvents.class);
    private static final Action NO_ACTION = it -> {};

    static final ComponentDragEvents<?> BLANK = new ComponentDragEvents<>(Configurator.none(), NO_ACTION, NO_ACTION);

    private final Configurator<DragAwayComponentConf<C>>    _dragAwayConfigurator;
    private final Action<DragOverEventComponentDelegate<C>> _dragOverEventAction;
    private final Action<DragDropEventComponentDelegate<C>> _dragDropEventAction;

    ComponentDragEvents(
        Configurator<DragAwayComponentConf<C>>    dragAwayConfigurator,
        Action<DragOverEventComponentDelegate<C>> dragOverConfigurator,
        Action<DragDropEventComponentDelegate<C>> dragDropConfigurator
    ) {
        _dragAwayConfigurator = dragAwayConfigurator;
        _dragOverEventAction  = dragOverConfigurator;
        _dragDropEventAction  = dragDropConfigurator;
    }


    public ComponentDragEvents<C> withDragAwayConf(Configurator<DragAwayComponentConf<C>> configurator ) {
        if ( Configurator.none().equals(_dragAwayConfigurator) )
            return new ComponentDragEvents<>(configurator, _dragOverEventAction, _dragDropEventAction);
        else
            return new ComponentDragEvents<>(_dragAwayConfigurator.andThen(configurator), _dragOverEventAction, _dragDropEventAction);
    }

    ComponentDragEvents<C> withDragOverConf(Action<DragOverEventComponentDelegate<C>> configurator ) {
        if ( Configurator.none().equals(_dragOverEventAction) )
            return new ComponentDragEvents<>(_dragAwayConfigurator, configurator, _dragDropEventAction);
        else
            return new ComponentDragEvents<>(_dragAwayConfigurator, _dragOverEventAction.andThen(configurator), _dragDropEventAction);
    }

    ComponentDragEvents<C> withDragDropConf(Action<DragDropEventComponentDelegate<C>> configurator ) {
        if ( Configurator.none().equals(_dragDropEventAction) )
            return new ComponentDragEvents<>(_dragAwayConfigurator, _dragOverEventAction, configurator);
        else
            return new ComponentDragEvents<>(_dragAwayConfigurator, _dragOverEventAction, _dragDropEventAction.andThen(configurator));
    }

    DragAwayComponentConf<C> getDragAwayConf( C owner, MouseEvent event ) {
        if ( Configurator.none().equals(_dragAwayConfigurator) )
            return DragAwayComponentConf.of(owner, event);

        try {
            return _dragAwayConfigurator.configure(DragAwayComponentConf.of(owner, event));
        } catch ( Exception e ) {
            log.error("Error while configuring drag away component!", e);
            return DragAwayComponentConf.of(owner, event);
        }
    }

    boolean dispatchDragOverEvent(C owner, MouseEvent event, JComponent hoveringComponent ) {
        if ( NO_ACTION == _dragOverEventAction )
            return false;

        try {
            _dragOverEventAction.accept(DragOverEventComponentDelegate.of(owner, event, hoveringComponent));
        } catch ( Exception e ) {
            log.error("Error while configuring drag over component!", e);
        }
        return true;
    }

    boolean dispatchDragDropEvent( C owner, MouseEvent event, JComponent hoveringComponent ) {
        if ( NO_ACTION == _dragDropEventAction )
            return false;

        DragDropEventComponentDelegate<C> dragDropEventComponentDelegate = DragDropEventComponentDelegate.of(owner, event, hoveringComponent);
        try {
            _dragDropEventAction.accept(dragDropEventComponentDelegate);
        } catch ( Exception e ) {
            log.error("Error while configuring drag drop component!", e);
        }
        return dragDropEventComponentDelegate.isConsuming();
    }

}

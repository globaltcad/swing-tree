package swingtree.style;

import swingtree.DragAwayComponentConf;
import swingtree.DragDropComponentConf;
import swingtree.DragOverComponentConf;
import swingtree.api.Configurator;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Optional;

class ComponentDragEvents<C extends JComponent>
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ComponentDragEvents.class);

    static final ComponentDragEvents<?> BLANK = new ComponentDragEvents<>(Configurator.none(), Configurator.none(), Configurator.none());

    private final Configurator<DragAwayComponentConf<C, MouseEvent>> _dragAwayConfigurator;
    private final Configurator<DragOverComponentConf<C, MouseEvent>> _dragOverConfigurator;
    private final Configurator<DragDropComponentConf<C, MouseEvent>> _dragDropConfigurator;

    ComponentDragEvents(
        Configurator<DragAwayComponentConf<C, MouseEvent>> dragAwayConfigurator,
        Configurator<DragOverComponentConf<C, MouseEvent>> dragOverConfigurator,
        Configurator<DragDropComponentConf<C, MouseEvent>> dragDropConfigurator
    ) {
        _dragAwayConfigurator = dragAwayConfigurator;
        _dragOverConfigurator = dragOverConfigurator;
        _dragDropConfigurator = dragDropConfigurator;
    }


    public ComponentDragEvents<C> withDragAwayConf(Configurator<DragAwayComponentConf<C, MouseEvent>> configurator ) {
        if ( Configurator.none().equals(_dragAwayConfigurator) )
            return new ComponentDragEvents<>(configurator, _dragOverConfigurator, _dragDropConfigurator);
        else
            return new ComponentDragEvents<>(_dragAwayConfigurator.andThen(configurator), _dragOverConfigurator, _dragDropConfigurator);
    }


    ComponentDragEvents<C> withDragOverConf(Configurator<DragOverComponentConf<C, MouseEvent>> configurator ) {
        if ( Configurator.none().equals(_dragOverConfigurator) )
            return new ComponentDragEvents<>(_dragAwayConfigurator, configurator, _dragDropConfigurator);
        else
            return new ComponentDragEvents<>(_dragAwayConfigurator, _dragOverConfigurator.andThen(configurator), _dragDropConfigurator);
    }

    ComponentDragEvents<C> withDragDropConf(Configurator<DragDropComponentConf<C, MouseEvent>> configurator ) {
        if ( Configurator.none().equals(_dragDropConfigurator) )
            return new ComponentDragEvents<>(_dragAwayConfigurator, _dragOverConfigurator, configurator);
        else
            return new ComponentDragEvents<>(_dragAwayConfigurator, _dragOverConfigurator, _dragDropConfigurator.andThen(configurator));
    }


    DragAwayComponentConf<C, MouseEvent> getDragAwayConf( C owner, MouseEvent event ) {
        if ( Configurator.none().equals(_dragAwayConfigurator) )
            return DragAwayComponentConf.of(owner, event);

        try {
            return _dragAwayConfigurator.configure(DragAwayComponentConf.of(owner, event));
        } catch ( Exception e ) {
            log.error("Error while configuring drag away component!", e);
            return DragAwayComponentConf.of(owner, event);
        }
    }

    Optional<DragOverComponentConf<C, MouseEvent>> getDragOverConf(C owner, MouseEvent event, JComponent hoveringComponent ) {
        if ( Configurator.none().equals(_dragOverConfigurator) )
            return Optional.empty();

        try {
            return Optional.of(_dragOverConfigurator.configure(DragOverComponentConf.of(owner, event, hoveringComponent)));
        } catch ( Exception e ) {
            log.error("Error while configuring drag over component!", e);
            return Optional.of(DragOverComponentConf.of(owner, event, hoveringComponent));
        }
    }

    DragDropComponentConf<C, MouseEvent> getDragDropConf( C owner, MouseEvent event, JComponent hoveringComponent ) {
        if ( Configurator.none().equals(_dragDropConfigurator) )
            return DragDropComponentConf.of(owner, event, hoveringComponent);

        try {
            return _dragDropConfigurator.configure(DragDropComponentConf.of(owner, event, hoveringComponent));
        } catch ( Exception e ) {
            log.error("Error while configuring drag drop component!", e);
            return DragDropComponentConf.of(owner, event, hoveringComponent);
        }
    }

}

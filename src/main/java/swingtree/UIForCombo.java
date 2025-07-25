package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.*;
import sprouts.Action;
import swingtree.api.Configurator;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  A SwingTree builder node designed for configuring {@link JComboBox} instances.
 *  <p>
 *  <b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 *  where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <E> The type of the items of the {@link JComboBox} that this {@link UIForCombo} is configuring.
 * @param <C> The type of the {@link JComboBox} that this {@link UIForCombo} is configuring.
 */
public final class UIForCombo<E,C extends JComboBox<E>> extends UIForAnySwing<UIForCombo<E,C>, JComboBox<E>>
{
    private static final Logger log = LoggerFactory.getLogger(UIForCombo.class);
    private final BuilderState<JComboBox<E>> _state;

    UIForCombo( BuilderState<JComboBox<E>> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    /**
     *  Builds and returns the configured {@link JComboBox} instance.
     *
     * @return The configured {@link JComboBox} instance.
     */
    public JComboBox<E> getComboBox() {
        return this.get(getType());
    }

    @Override
    protected BuilderState<JComboBox<E>> _state() {
        return _state;
    }
    
    @Override
    protected UIForCombo<E,C> _newBuilderWithState(BuilderState<JComboBox<E>> newState ) {
        return new UIForCombo<>(newState);
    }

    /**
     *  Registers a listener to be notified when the combo box is opened,
     *  meaning its popup menu is shown after the user clicks on the combo box.
     *
     * @param action the action to be executed when the combo box is opened.
     * @return this
     */
    public UIForCombo<E,C> onOpen( Action<ComponentDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupOpen(thisComponent, e -> _runInApp(()->{
                        try {
                            action.accept(new ComponentDelegate<>((C) thisComponent, e));
                        } catch (Exception ex) {
                            log.error("Error while executing action on popup open!", ex);
                        }
                    }));
                })
                ._this();
    }

    private void _onPopupOpen( JComboBox<E> thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // This method is called before the popup menu becomes visible.
                consumer.accept(e);
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the combo box is closed,
     *  meaning its popup menu is hidden after the user clicks on the combo box.
     *
     * @param action the action to be executed when the combo box is closed.
     * @return this
     */
    public UIForCombo<E,C> onClose( Action<ComponentDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupClose(thisComponent,
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>((C) thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error while executing action on popup close!", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    private void _onPopupClose( JComboBox<E> thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                consumer.accept(e); // This method is called before the popup menu becomes invisible
            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the combo box is canceled,
     *  meaning its popup menu is hidden which
     *  typically happens when the user clicks outside the combo box.
     *
     * @param action the action to be executed when the combo box is canceled.
     * @return this
     */
    public UIForCombo<E,C> onCancel( Action<ComponentDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupCancel(thisComponent,
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>((C) thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error while executing action on popup cancel!", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    private void _onPopupCancel( JComboBox<E> thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                consumer.accept(e); // This method is called when the popup menu is canceled
            }
        });
    }

    /**
     * Adds an {@link Action} to the underlying {@link JComboBox}
     * through an {@link java.awt.event.ActionListener},
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     *
     * @param action The {@link Action} that will be notified.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public UIForCombo<E,C> onSelection( Action<ComponentDelegate<JComboBox<E>, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                   _onSelection(thisComponent,
                       e -> _runInApp(()->{
                           try {
                               action.accept(new ComponentDelegate<>(thisComponent, e));
                           } catch (Exception ex) {
                               log.error("Error while executing action on selection!", ex);
                           }
                       })
                   );
               })
               ._this();
    }

    private void _onSelection( JComboBox<E> thisComponent, Consumer<ActionEvent> consumer ) {
        /*
            When an action event is fired, Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addActionListener" method, we will be the last to be notified.
            This is problematic because it is built on the assumption that the last listener
            added is more interested in the event than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
        */
        ActionListener[] listeners = thisComponent.getActionListeners();
        for (ActionListener listener : listeners)
            thisComponent.removeActionListener(listener);

        thisComponent.addActionListener(e -> {
            /*
                Unfortunately, this event is fired in all kinds of
                annoying situations, so we need to filter things that
                are only relevant to us...

                We know that when the action command is "comboBoxEdited", then
                the user wrote into the text field.
                This is also fired when the user presses enter!
                So we filter the event:
            */
            if ( "comboBoxEdited".equals(e.getActionCommand()) )
                return;

            /*
                Now another big problem is that when a user types
                something, the editor will inform our combo box model
                of the change and then the model will trigger item change listeners.
                Unfortunately, this will then cause a domino effect, because
                the combo box will consequently trigger an
                action event with the action command "comboBoxChanged".
                We can filter the event by checking if it comes from
                our model:
            */
            if ( e.getSource() instanceof JComboBox ) {
                ComboBoxModel<?> model = ((JComboBox<?>) e.getSource()).getModel();
                if ( model instanceof AbstractComboModel ) {
                    AbstractComboModel<?> swingTreeModel = (AbstractComboModel<?>) model;
                    if ( !swingTreeModel.acceptsEditorChanges() )
                        return;
                }
            }
            consumer.accept(e);
        });

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            thisComponent.addActionListener(listeners[i]);
    }

    /**
     * Adds an {@link ActionListener} to the editor component of the underlying {@link JComboBox}
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     * <p>
     * @param action The {@link Action} that will be notified.
     * @return This very builder instance, which allows for method chaining.
     **/
    public UIForCombo<E,C> onEnter( Action<ComponentDelegate<C, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                   _onEnter(thisComponent, e -> _runInApp(()->{
                       try {
                           action.accept(new ComponentDelegate<>((C) thisComponent, e));
                       } catch (Exception ex) {
                           log.error("Error while executing action on enter!", ex);
                       }
                   }));
               })
               ._this();
    }

    private void _onEnter( JComboBox<E> thisComponent, Consumer<ActionEvent> consumer ) {
        Component editor = thisComponent.getEditor().getEditorComponent();
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            UI.of(field).onEnter( it -> consumer.accept(it.getEvent()) );
        }
    }

    /**
     *  Use this to enable or disable editing for the wrapped UI component.
     *
     * @param isEditable The truth value determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E,C> isEditableIf( boolean isEditable ) {
        return _with( thisComponent -> {
                   thisComponent.setEditable(isEditable);
               })
               ._this();
    }

    /**
     *  Use this to enable or disable editing of the wrapped UI component
     *  through property binding dynamically.
     *
     * @param isEditable The boolean property determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code isEditable} is {@code null}.
     */
    public UIForCombo<E,C> isEditableIf( Var<Boolean> isEditable ) {
        NullUtil.nullPropertyCheck(isEditable, "isEditable", "Null is not a valid state for modelling 'isEditable''.");
        return _withOnShow( isEditable, (thisComponent, v) -> {
                    thisComponent.setEditable(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setEditable(isEditable.get());
                })
                ._this();
    }

    public final <V extends E> UIForCombo<E,C> _withRendererAndEditor(CellBuilder<C,V> cellBuilder) {
        NullUtil.nullArgCheck(cellBuilder, "cellBuilder", CellBuilder.class);
        return _with( thisComponent -> {
                    cellBuilder.buildForCombo((C)thisComponent, true);
                })
                ._this();
    }

    /**
     *  Use this to set a helpful tool tip text for each menu item
     *  in the drop-down menu of the {@link JComboBox} UI declaration.
     *  The tool tip text will be displayed when the mouse hovers on a
     *  menu item for some time. <br>
     *  This is in essence a convenience method and abstraction over
     *  the {@link #withCell(Configurator)} method, which avoid dealing
     *  with the complexity of dealing with a more customized approach
     *  to configuring you combo-box cell renderer and cell editors...<br>
     *  <p>
     *  Here an example demonstrating how to configure a
     *  tooltip for each available option of a combo-box individually: <br>
     *  <pre>{@code
     *  UI.comboBox(DayOfWeek.values())
     *  .withTooltips( day -> {
     *    return switch (day) {
     *      case MONDAY -> "Monday";
     *      case TUESDAY -> "Tuesday";
     *      case WEDNESDAY -> "Wednesday";
     *      case THURSDAY -> "Thursday";
     *      case FRIDAY -> "Friday";
     *      case SATURDAY -> "Saturday";
     *      case SUNDAY -> "Sunday";
     *    };
     *  })
     *  }</pre>
     *
     * @param tooltips A function which returns a tooltip for a specific item.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withTooltips(
        Function<E, String> tooltips
    ) {
        return withCell( it -> it
                .updateView(view -> view
                    .update( comp -> {
                        if ( comp instanceof JComponent && it.entry().isPresent() ) {
                            JComponent jcomp =  (JComponent) comp;
                            String toolTip = null;
                            try {
                                Object result = tooltips.apply(it.entry().get());
                                toolTip = result == null ? null : result.toString();
                                toolTip = toolTip == null || "".equals(toolTip) ? null : toolTip;
                            } catch (Exception ex) {
                                log.error("Error while fetching tooltip for cell '{}'.", it, ex);
                            }
                            if ( !Objects.equals(toolTip, jcomp.getToolTipText()) ) {
                                jcomp.setToolTipText(toolTip);
                            }
                            return jcomp;
                        }
                        return comp;
                    })
                 )
            );
    }

    /**
     *  Use this to define a generic combo box view/renderer for any type of item.
     *  You would typically want to use this method to render generic types where the only
     *  common type is {@link Object}, yet you still want to render the items
     *  in a specific way depending on their actual type. <br>
     *  This is done like so:
     *  <pre>{@code
     *  UI.comboBox(new Object[]{":-)", 42L, '§'})
     *  .withCells( it -> it
     *      .when(String.class).asText( cell -> "String: "+cell.getValue() )
     *      .when(Character.class).asText( cell -> "Char: "+cell.getValue() )
     *      .when(Number.class).asText( cell -> "Number: "+cell.getValue() )
     *  );
     *  }</pre>
     *  Note that inside the lambda function, you can use the {@link CellBuilder} to define
     *  for what type of item you want to render the item in a specific way and the {@link RenderAs}
     *  to define how the item should be rendered.
     *  <p>
     *  You may want to know that a similar API is also available for the {@link javax.swing.JList}
     *  and {@link javax.swing.JTable} components, see {@link UIForList#withCells(Configurator)},
     *  {@link UIForTable#withCells(Configurator)} and {@link UI#table(Configurator)}
     *  for more information.
     * <p>
     * <b>Note that the preferred way of building a list cell view
     * is through the {@link #withCell(Configurator)} method, which is
     * way simpler as it does not assume the combobox has an item type ambiguity.</b>
     *
     * @param renderBuilder A lambda function that configures the renderer for this combo box.
     * @return This combo box instance for further configuration.
     * @param <V> The type of the value that is being rendered in this combo box.
     */
    public final <V extends E> UIForCombo<E,C> withCells(
        Configurator<CellBuilder<C,V>> renderBuilder
    ) {
        Class<Object> commonType = Object.class;
        Objects.requireNonNull(commonType);
        CellBuilder render = CellBuilder.forCombo(commonType);
        try {
            render = renderBuilder.configure(render);
        } catch (Exception e) {
            log.error("Error while building renderer.", e);
            return this;
        }
        Objects.requireNonNull(render);
        return _withRendererAndEditor(render);
    }

    /**
     *  Use this method to configure how the combo box views should
     *  be rendered for a specific type of item. This is useful when you have a
     *  combo box with a single type of item, and you want to render the items
     *  according to your specific needs. <br>
     *  An example of how to use this method is as follows:
     *  <pre>{@code
     *      UI.comboBox(TimeUnit.values())
     *      .withCell( cell -> cell
     *          .updateView( comp -> comp
     *              .orGet(JLabel::new)
     *              .updateIf(JLabel.class, label -> {
     *                  label.setText(cell.valueAsString().orElse(""));
     *                  return label;
     *              })
     *          )
     *      );
     *  }</pre>
     *
     * @param cellConfigurator The configurator for the cell, receiving a {@link CellConf} as input
     *                         and returning an updated CellDelegate.
     * @return This very instance, which enables builder-style method chaining.
     * @param <V> The type of the value that is being rendered in this combo box.
     */
    public final <V extends E> UIForCombo<E,C> withCell(
        Configurator<CellConf<C,V>> cellConfigurator
    ) {
        return withCells(it -> it.when((Class) Object.class).as(cellConfigurator));
    }

    /**
     * Sets the {@link ListCellRenderer} for the {@link JComboBox}, which renders the combo box items
     * by supplying a custom component for each item through the
     * {@link ListCellRenderer#getListCellRendererComponent(JList, Object, int, boolean, boolean)} method.
     * <p>
     * @param renderer The {@link ListCellRenderer} that will be used to paint each cell in the combo box.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withCellRenderer( ListCellRenderer<E> renderer ) {
        return _with( thisComponent -> {
                    thisComponent.setRenderer(renderer);
                })
                ._this();
    }

    /**
     *  Use this to specify a custom text based cell renderer for each item in the combo box.
     *  The renderer is a function that takes a {@link CellConf} as input
     *  and returns a {@link String} which will be used as the text for the combo box item.
     *
     * @param renderer The function that will be used to render the combo box items.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withTextRenderer( Function<CellConf<C,E>, String> renderer ) {
        Objects.requireNonNull(renderer, "renderer");
        return withCells(it -> it.when((Class<E>) Object.class).asText( renderer ) );
    }

    /**
     *  Use this convenience method to specify the model for the combo box,
     *  which is used by the combo box component to determine the available options
     *  and the currently selected item.
     *
     * @param model The {@link ComboBoxModel} to be used for modelling the content data of the combo box.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withModel( ComboBoxModel<E> model ) {
        return _with( thisComponent -> {
                    _setModel(model, thisComponent);
                })
                ._this();
    }

    private void _setModel( ComboBoxModel<E> model, JComboBox<E> thisComponent ) {
        if ( model instanceof AbstractComboModel )
            _bindComboModelToEditor(thisComponent, (AbstractComboModel<E>) model );
        thisComponent.setModel(model);
    }

    /**
     *  Uses the given list of elements as a basis for a new combo box model
     *  and sets it as the model for the combo box.
     *  This means that whenever the list of elements changes,
     *  and the combo box is rendered, the combo box will be updated accordingly.
     *
     * @param options The list of elements to be used as the basis for the combo box model.
     * @return This builder node, which enables builder-style method chaining.
     * @throws NullPointerException if {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( java.util.List<E> options ) {
        Objects.requireNonNull(options, "options");
        return this.withModel(new ListBasedComboModel<>(options));
    }

    /**
     *  Uses the provided selection property as well as a list of elements as a basis for a new combo box model.
     *  Whenever the selection or the list of elements changes,
     *  and the combo box is rendered, the combo box will be updated accordingly.
     *  Note that the use of the {@link Var} type for the selection property
     *  allows the combo box to listen for changes to the selection property,
     *  which ensures that the combo box is updated whenever the selection property changes.
     *
     * @param selection The selection property to be used as the basis for modelling the currently selected item in a new combo box model.
     * @param options The list of elements to be used as the basis for modelling the available options in a new combo box model.
     * @return This builder node, which enables builder-style method chaining.
     * @throws NullPointerException if {@code selection} or {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( Var<E> selection, java.util.List<E> options ) {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(options, "options");
        return this.withModel(new ListBasedComboModel<>(selection, options));
    }

    /**
     *  Uses the given property list of elements as a basis for a new combo box model
     *  and sets it as the model for the combo box.
     *  The combo box will register a change listener and update itself whenever the list of elements changes.
     *
     * @param options The property list of elements to be used as the basis for a new combo box model.
     * @return This builder node, which enables builder-style method chaining.
     * @throws NullPointerException if {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( Vars<E> options ) {
        Objects.requireNonNull(options, "options");
        return this.withModel(new VarsBasedComboModel<>(options));
    }

    /**
     *  Uses a read only property list of elements as a basis for a new combo box model
     *  and sets it as the model for the combo box.
     *  The combo box will register a change listener and update itself whenever the list of elements changes.
     *  Due to the fact that the list of elements is read only,
     *  changes to the list of elements can only come from the view model.
     *
     * @param options The read only property list of elements to be used as the basis for a new combo box model.
     * @return This builder node, which enables builder-style method chaining.
     * @throws NullPointerException if {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( Vals<E> options ) {
        Objects.requireNonNull(options, "options");
        return this.withModel(new ValsBasedComboModel<>(options));
    }

    /**
     *  Uses the given selection property as well as a property list of elements as a basis
     *  for a new combo box model and sets it as the new model for the combo box state.
     *  This means that whenever the state of the selection property or the property list of elements changes,
     *  then combo box will be updated and rendered accordingly.<br>
     *  <b>
     *      Note that we recommend using the {@link Tuple} property based {@link #withItems(Var, Var)} method instead
     *      of this method, as it requires less state to be managed in your application and allows for a more
     *      data-oriented programming style.
     *  </b>
     *
     * @param selection The selection property to be used as the basis for modelling the currently selected item in a new combo box model.
     * @param options The property list of elements to be used as the basis for modelling the available options in a new combo box model.
     * @return This builder node instance, which allows for builder-style method chaining.
     * @throws NullPointerException if either one of {@code selection} or {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( Var<E> selection, Vars<E> options ) {
        return this.withModel(new VarsBasedComboModel<>(selection, options));
    }

    /**
     *  Uses the given selection property as well as a read only property list of elements as a basis
     *  for a new combo box model and sets it as the new model for the combo box state.
     *  This means that whenever the state of the selection property or the read only property list of elements changes,
     *  then combo box will be updated and rendered according to said changes.
     *  Due to the list of options being read only, changes to it can only come from the view model.<br>
     *  <b>
     *      Note that we recommend using the {@link Tuple} property based {@link #withItems(Var, Val)} method instead
     *      of this method, as it requires less state to be managed in your application and allows for a more
     *      data-oriented programming style.
     *  </b>
     *
     * @param selection The selection property to be used as the basis for modelling the currently selected item in a new combo box model.
     * @param options The read only property list of elements to be used as the basis for modelling the available options in a new combo box model.
     * @return This builder node, which allows for builder-style method chaining.
     * @throws NullPointerException if either one of {@code selection} or {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( Var<E> selection, Vals<E> options ) {
        return this.withModel(new ValsBasedComboModel<>(selection, options));
    }

    /**
     *  Uses the given selection property as well as an array of elements as a basis
     *  for a new combo box model and sets it as the new model for the combo box state.
     *  This means that whenever the state of the selection property or the array of elements changes,
     *  then combo box will be updated and rendered according to said changes.
     *  Note that the combo box can not register change listeners on the array of elements,
     *  which means that for the combo box to be updated whenever the array of elements changes,
     *  you must trigger the update manually.
     *
     * @param selection The selection property to be used as the basis for modelling the currently selected item in a new combo box model.
     * @param options The array of elements to be used as the basis for modelling the available options in a new combo box model.
     * @return This builder node, which allows for builder-style method chaining.
     * @throws NullPointerException if either one of {@code selection} or {@code options} is {@code null}.
     */
    @SafeVarargs
    public final UIForCombo<E,C> withItems( Var<E> selection, E... options ) {
        return this.withModel(new ArrayBasedComboModel<>(selection, options));
    }

    /**
     *  Uses the given selection property as well as a property of a {@link Tuple} of elements as a basis
     *  for a new combo box model and sets it as the new model for the combo box state.
     *  This means that whenever the state of the selection property or the property of tuple of elements changes,
     *  then combo box will be updated and rendered according to said changes.
     *
     * @param selection The selection property to be used as the basis for modelling the currently selected item in a new combo box model.
     * @param options The property of a tuple of elements to be used as the basis for modelling the available options in a new combo box model.
     * @return This builder node, which allows for builder-style method chaining.
     * @throws NullPointerException if either one of {@code selection} or {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( Var<E> selection, Var<Tuple<E>> options ) {
        return this.withModel(new TuplePropertyComboModel<>(selection, options));
    }

    /**
     *  Uses the given selection property as well as a read only property of a {@link Tuple} of elements as a basis
     *  for a new combo box model and sets it as the new model for the combo box state.
     *  This means that whenever the state of the selection property or the read only property of a tuple of elements changes,
     *  then combo box will be updated and rendered according to said changes.
     *  Due to the list of options being read only, changes to it can only come from the view model.
     *
     * @param selection The selection property to be used as the basis for modelling the currently selected item in a new combo box model.
     * @param options The read only property of a tuple of elements to be used as the basis for modelling the available options in a new combo box model.
     * @return This builder node, which allows for builder-style method chaining.
     * @throws NullPointerException if either one of {@code selection} or {@code options} is {@code null}.
     */
    public final UIForCombo<E,C> withItems( Var<E> selection, Val<Tuple<E>> options ) {
        return this.withModel(new TuplePropertyComboModel<>(selection, options));
    }

    /**
     *  This method allows you to specify an initial selection for the combo box.
     *
     * @param item The item which should be set as the currently selected combo box item.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withSelectedItem( E item ) {
        return _with( thisComponent -> {
                    _setSelectedItem(thisComponent, item);
               })
               ._this();
    }

    /**
     *  Use this to dynamically set the selected item of the combo box.
     *
     * @param item The item to be selected.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withSelectedItem( Var<E> item ) {
        NullUtil.nullArgCheck(item, "item", Var.class);
        return _with( thisComponent -> {
                    ComboBoxModel<E> model = thisComponent.getModel();
                    if ( model instanceof AbstractComboModel )
                        _setModel(((AbstractComboModel<E>)model).withVar(item), thisComponent);
                    else {
                        // The user has a custom model AND wants to bind to a property:
                        _onShow( item, thisComponent, (c,v) -> _setSelectedItem(c, v) );
                        _onSelection(thisComponent,
                            e -> _runInApp( (E)thisComponent.getSelectedItem(), newItem -> item.set(From.VIEW, newItem)  )
                        );
                    }
                    _setSelectedItem(thisComponent, item.get());
                })
                ._this();
    }

    private void _setSelectedItem( JComboBox<E> thisComponent, E item ) {
        // Ok, so a combo box fires an event when the selection is changed programmatically.
        // This is a problem, because we don't want to trigger the action listener.
        // So we temporarily remove the action listener(s), and then add them back.
        // 1. Get the action listener(s)
        Component editor = thisComponent.getEditor().getEditorComponent();
        AbstractDocument abstractDocument = null;
        ActionListener[]   listeners    = thisComponent.getActionListeners();
        DocumentListener[] docListeners = {};
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            Document doc = field.getDocument();
            if ( doc instanceof AbstractDocument ) {
                abstractDocument = (AbstractDocument) doc;
                docListeners = ((AbstractDocument)doc).getDocumentListeners();
            }
        }

        // 2. Remove them
        for ( ActionListener listener : listeners )
            thisComponent.removeActionListener(listener);
        if ( abstractDocument != null ) {
            for (DocumentListener listener : docListeners) {
                abstractDocument.removeDocumentListener(listener);
            }
        }

        try {
            // 3. Set the selected item
            thisComponent.setSelectedItem(item);
            // 3.1 We make sure the editor also gets an update!
            thisComponent.getEditor().setItem(item);

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }

        // 4. Add them back
        for ( ActionListener listener : listeners )
            thisComponent.addActionListener(listener);
        if ( abstractDocument != null ) {
            for (DocumentListener listener : docListeners) {
                abstractDocument.addDocumentListener(listener);
            }
        }
    }

    private void _bindComboModelToEditor( JComboBox<E> thisComponent, AbstractComboModel<E> model ) {
        Component editor = thisComponent.getEditor().getEditorComponent();
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            MemSafeBinding<E> memSafeBinding = new MemSafeBinding<>(field, thisComponent, model);

            UI.of(field).onTextChange( it -> {
                memSafeBinding.textChanged();
            });
            _onShow( model._getSelectedItemVar(), thisComponent, (c, v) -> {
                memSafeBinding.itemChanged(c, v);
            });
            // Adds a PopupMenu listener which will listen to notification
            // messages from the popup portion of the combo box.
            thisComponent.addPopupMenuListener(memSafeBinding);
        }
    }

    /**
     *  A popup menu listener for the combo box which synchronizes text field, model
     *  and combo box while at the same time keeping weak references on
     *  all involved components to ensure that no memory leaks occur.
     * @param <E> The type of the items in the combo box.
     */
    private static class MemSafeBinding<E> implements PopupMenuListener {

        private final WeakReference<JTextField> weakField;
        private final WeakReference<JComboBox<E>> weakCombo;
        private final WeakReference<AbstractComboModel<E>> weakModel;

        private boolean comboIsOpen = false;

        private MemSafeBinding(
                JTextField field,
                JComboBox<E> combo,
                AbstractComboModel<E> model
        ) {
            this.weakField = new WeakReference<>(field);
            this.weakCombo = new WeakReference<>(combo);
            this.weakModel = new WeakReference<>(model);
        }

        public void textChanged() {
            JTextField field = weakField.get();
            AbstractComboModel<E> strongModel = weakModel.get();
            if ( strongModel != null && field != null ) {
                JComboBox<E> strongCombo = weakCombo.get();
                if (!comboIsOpen && strongCombo != null && strongCombo.isEditable())
                    strongModel.setFromEditor(field.getText());
            }
        }

        public void itemChanged(JComboBox<E> combo, E newItem) {
            AbstractComboModel<E> strongModel = weakModel.get();
            if ( strongModel != null ) {
                strongModel.doQuietly(() -> {
                    combo.getEditor().setItem(newItem);
                    strongModel.fireListeners();
                });
            }
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            // This method is called before the popup menu becomes visible.
            comboIsOpen = true;
        }
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            // This method is called before the popup menu becomes invisible
            comboIsOpen = false;
        }
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            // This method is called when the popup menu is canceled
            comboIsOpen = false;
        }
    }

}

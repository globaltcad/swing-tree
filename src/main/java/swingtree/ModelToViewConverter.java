package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.HasId;
import sprouts.Var;
import swingtree.api.mvvm.ViewSupplier;
import swingtree.components.JScrollPanels;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

final class ModelToViewConverter<M> implements ViewSupplier<M> {

    static <M, C extends JComponent> ModelToViewConverter<M> of(
        C parent,
        ViewSupplier<M> viewSupplier,
        BiFunction<M, Exception, JComponent> errorViewCreator
    ) {
        return new ModelToViewConverter<>(parent, viewSupplier, errorViewCreator);
    }

    private static final Object UNIQUE_VIEW_CACHE_KEY = UUID.randomUUID();

    private final WeakReference<JComponent>            parentRef;
    private final ViewSupplier<M>                      viewCreator;
    private final BiFunction<M, Exception, JComponent> errorViewCreator;

    private final List<JComponent> childComponents = new ArrayList<>();


    ModelToViewConverter(
        JComponent parent,
        ViewSupplier<M> viewCreator,
        BiFunction<M, Exception, JComponent> errorViewCreator
    ) {
        this.parentRef        = new WeakReference<>(Objects.requireNonNull(parent));
        this.viewCreator      = Objects.requireNonNull(viewCreator);
        this.errorViewCreator = Objects.requireNonNull(errorViewCreator);
    }

    void rememberCurrentViewsForReuse() {
        JComponent parent = _subViewParent();
        if ( parent != null ) {
            for ( int vi = 0; vi < parent.getComponentCount(); vi++ ) {
                Component child = parent.getComponent(vi);
                if ( child instanceof JComponent ) {
                    childComponents.add((JComponent) child);
                }
            }
        }
    }

    void clearCurrentViews() {
        childComponents.clear();
    }

    @Override
    public UIForAnySwing<?, ?> createViewFor(M viewModel) throws Exception {
        return UI.of(_createViewFor(viewModel));
    }

    private JComponent _createViewFor(M model) {
        try {
            JComponent existingView = _findCachedViewIn(model);
            if ( existingView != null )
                return existingView;
            else {
                UIForAnySwing<?,?> newView = viewCreator.createViewFor(model);
                Objects.requireNonNull(newView);
                JComponent viewComponent = newView.get((Class) newView.getType());
                if ( model instanceof UIForAnySwing.ViewHandle ) {
                    UIForAnySwing.ViewHandle handle = (UIForAnySwing.ViewHandle) model;
                    handle.setChild(viewComponent);
                }
                viewComponent.putClientProperty(UNIQUE_VIEW_CACHE_KEY, _idFrom(model));
                return viewComponent;
            }
        } catch (Exception e) {
            return errorViewCreator.apply(model, e);
        }
    }

    private @Nullable JComponent _subViewParent() {
        JComponent parent = this.parentRef.get();
        if ( parent instanceof JScrollPanels ) {
            JScrollPanels panels = (JScrollPanels) parent;
            parent = panels.getContentPanel();
        }
        return parent;
    }

    private @Nullable JComponent _findCachedViewIn(M model) throws Exception {
        JComponent parent = _subViewParent();
        if ( parent != null ) {
            Object id = _idFrom(model);
            for ( JComponent existingSubView : this.childComponents ) {
                if ( existingSubView instanceof JScrollPanels.EntryPanel ) {
                    JScrollPanels.EntryPanel entryPanel = (JScrollPanels.EntryPanel) existingSubView;
                    JComponent actualView = (JComponent) entryPanel.getComponent(0);
                    Object foundId = actualView.getClientProperty(UNIQUE_VIEW_CACHE_KEY);
                    if ( Objects.equals(id, foundId) ) {
                        actualView.putClientProperty(UNIQUE_VIEW_CACHE_KEY, id);
                        return (JComponent) existingSubView.getComponent(0);
                    }
                } else {
                    Object foundId = existingSubView.getClientProperty(UNIQUE_VIEW_CACHE_KEY);
                    if ( Objects.equals(id, foundId) ) {
                        existingSubView.putClientProperty(UNIQUE_VIEW_CACHE_KEY, id);
                        return existingSubView;
                    }
                }
            }
        }
        return null;
    }

    private @Nullable Object _idFrom( Object model ) {
        if (model instanceof Var)
            model = ((Var<?>) model).orElseNull();
        if ( model instanceof UIForAnySwing.ViewHandle )
            model = ((UIForAnySwing.ViewHandle<?>) model).property().orElseNull();

        Object id = model;
        if ( model instanceof HasId ) {
            HasId<?> idModel = (HasId<?>) model;
            id = idModel.id();
        }
        return id;
    }

}

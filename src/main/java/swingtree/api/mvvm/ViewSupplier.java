package swingtree.api.mvvm;

import sprouts.Val;
import sprouts.Vals;
import swingtree.UIForAnySwing;

/**
 *  A provider of a view, usually a view model or a simple data model with the purpose of
 *  making it possible to dynamically creating sub-views for inside a view for a given
 *  sub-view model.
 *  <p>
 *  See: <br>
 *  {@link swingtree.UIForAnySwing#add(Val, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#add(Vals, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#add(String, Val, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#add(String, Vals, ViewSupplier)}, <br>
 *  {@link swingtree.UIForAnySwing#add(swingtree.layout.CompAttr, Val, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#add(swingtree.layout.CompAttr, Vals, ViewSupplier)}, <br>
 */
@FunctionalInterface
public interface ViewSupplier<M>
{
    /**
     *  Creates a view representing an instance of the type {@code M},
     *  which is a view model or a simple data model.
     *
     * @param viewModel The thing to create a view for, usually a view model or a simple data model.
     * @return A view for the given view model in the form of a {@link UIForAnySwing}, a builder node
     *         which wraps the actual main component of the view.
     */
    UIForAnySwing<?,?> createViewFor( M viewModel );
}

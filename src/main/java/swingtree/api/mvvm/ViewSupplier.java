package swingtree.api.mvvm;

import sprouts.Val;
import sprouts.Vals;
import swingtree.UIForAnySwing;
import swingtree.layout.AddConstraint;

/**
 *  A provider of a view, usually a view model or a simple data model with the purpose of
 *  making it possible to dynamically creating sub-views for inside a view for a given
 *  sub-view model.
 *  <p>
 *  See: <br>
 *  {@link swingtree.UIForAnySwing#add(Val, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#addAll(Vals, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#add(String, Val, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#addAll(String, Vals, ViewSupplier)}, <br>
 *  {@link swingtree.UIForAnySwing#add(AddConstraint, Val, ViewSupplier)},  <br>
 *  {@link swingtree.UIForAnySwing#addAll(AddConstraint, Vals, ViewSupplier)}, <br>
 *
 * @param <M> The type of model for which a view declaration is created
 *           upon an invocation to {{@link #createViewFor(Object)}}.
 */
@FunctionalInterface
public interface ViewSupplier<M>
{
    /**
     *  Creates a view representing an instance of the type {@code M},
     *  which is a view model or a simple data model. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param viewModel The thing to create a view for, usually a view model or a simple data model.
     * @return A view for the given view model in the form of a {@link UIForAnySwing}, a builder node
     *         which wraps the actual main component of the view.
     * @throws Exception if the view could not be created by the client code.
     */
    UIForAnySwing<?,?> createViewFor( M viewModel ) throws Exception;
}

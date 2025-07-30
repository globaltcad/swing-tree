package swingtree.api.mvvm;

import sprouts.HasId;
import sprouts.Var;
import swingtree.UIForAnySwing;
import swingtree.layout.AddConstraint;

/**
 *  A provider of a view bound to a {@link Var} property, usually containing a value object
 *  based view model or a simple data model in the form of a record from
 *  which the calcite dynamically creates and manages sub-views.
 *  <p>
 *  See: <br>
 *  {@link UIForAnySwing#addAll(Var, BoundViewSupplier)},  <br>
 *  {@link UIForAnySwing#addAll(String, Var, BoundViewSupplier)}, <br>
 *  {@link UIForAnySwing#addAll(AddConstraint, Var, BoundViewSupplier)}, <br>
 *
 * @param <M> The type of model for which a view declaration is created
 *           upon an invocation to {{@link #createViewFor(Var)}}.
 *           This is expected to be an immutable object with value semantics,
 *           like a record for example...
 */
@FunctionalInterface
public interface BoundViewSupplier<M extends HasId<?>>
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
    UIForAnySwing<?,?> createViewFor( Var<M> viewModel ) throws Exception;
}

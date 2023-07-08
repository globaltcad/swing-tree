package swingtree.api.mvvm;

import sprouts.Val;
import sprouts.Vals;
import swingtree.UIForAnySwing;

import javax.swing.*;

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
    UIForAnySwing<?,?> createViewFor( M viewModel );
}

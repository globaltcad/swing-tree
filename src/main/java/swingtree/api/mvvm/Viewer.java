package swingtree.api.mvvm;

import sprouts.Val;
import sprouts.Vals;

import javax.swing.*;

/**
 *  A provider of a view, usually a view model or a simple data model with the purpose of
 *  making it possible to dynamically creating sub-views for inside a view for a given
 *  sub-view model.
 *  <p>
 *  See: <br>
 *  {@link swingtree.UIForAnySwing#add(Val, Viewer)},  <br>
 *  {@link swingtree.UIForAnySwing#add(Vals, Viewer)},  <br>
 *  {@link swingtree.UIForAnySwing#add(String, Val, Viewer)},  <br>
 *  {@link swingtree.UIForAnySwing#add(String, Vals, Viewer)}, <br>
 *  {@link swingtree.UIForAnySwing#add(swingtree.layout.CompAttr, Val, Viewer)},  <br>
 *  {@link swingtree.UIForAnySwing#add(swingtree.layout.CompAttr, Vals, Viewer)}, <br>
 */
@FunctionalInterface
public interface Viewer<M>
{
    JComponent getView(M viewModel);
}

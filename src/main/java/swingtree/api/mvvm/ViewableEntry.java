package swingtree.api.mvvm;

import swingtree.components.JScrollPanels;

/**
 *  A {@link ViewableEntry} is used to model an entry inside of the {@link JScrollPanels} component.
 *  It is expected to be a view model with 2 default properties: <br>
 *  - {@link #isSelected()} <br>
 *  - {@link #position()} <br>
 *  The {@link #isSelected()} method defines a boolean property determining whether
 *  the entry is currently selected or not. <br>
 *  The {@link #position()} method defines an integer property determining the position
 *  of the entry in the list of all entries.
 *  <p>
 *  <b>Note that implementations of this are not supposed to be part of your UI code.
 *  They are supposed to be part of your business logic code in the form of view models.
 *  </b>
 */
@Deprecated
public interface ViewableEntry extends Viewable, EntryViewModel
{
}

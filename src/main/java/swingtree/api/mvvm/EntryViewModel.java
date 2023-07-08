package swingtree.api.mvvm;

import sprouts.Var;

public interface EntryViewModel
{
    /**
     *  This method implies the existence of a boolean property in this view model
     *  determining whether the entry is currently selected or not.
     *
     * @return A {@link Var} instance representing the selected state of the entry.
     */
    Var<Boolean> isSelected();

    /**
     *  This method implies the existence of an integer property in this view model
     *  determining the position of the entry in the list of all entries.
     *
     * @return A {@link Var} instance representing the position of the entry.
     */
    Var<Integer> position();
}

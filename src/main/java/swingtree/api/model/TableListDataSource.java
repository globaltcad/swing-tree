package swingtree.api.model;

import swingtree.UI;

import java.util.List;

/**
 *  A simple functional interface whose implementations are used to
 *  form simple {@link javax.swing.table.TableModel} implementations
 *  based on lists of lists.
 *
 * @param <E> The type of the table entry {@link Object}s.
 */
@FunctionalInterface
public interface TableListDataSource<E> {

    /**
     *  When passed to {@link swingtree.UIForTable#withModel(UI.ListData, TableListDataSource)},
     *  this method is called continuously by you table to fetch the current {@link List}
     *  based table data. It is not cached or stored insider the table, so if you
     *  do not want to rebuild the list based model over and over again make sure this
     *  list data source always returns the same object instead of rebuilding it eagerly...
     *
     * @return A list matrix which will be used to populate the table.
     */
    List<List<E>> get();

}

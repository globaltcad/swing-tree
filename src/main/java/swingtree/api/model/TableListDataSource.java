package swingtree.api.model;

import swingtree.UI;

import java.util.List;

/**
 *  A simple functional interface whose implementations are used to
 *  form simple {@link javax.swing.table.TableModel} implementations
 *  based on lists of lists.
 *  <p>
 *  <b>Note that {@link TableData} is the recommended way of modelling a table in
 *  SwingTree.</b> It is a single immutable value describing the whole table (cells,
 *  column names, column classes and layout), which you hold in a
 *  {@link sprouts.Var} property and bind through {@link swingtree.UI#table(sprouts.Var)}.
 *  A table bound like that updates itself, is thread safe by construction, and syncs
 *  row changes to the {@link javax.swing.JTable} incrementally rather than rebuilding
 *  it. A pull based data source like this one, by contrast, has to be told when to
 *  refresh (see {@code updateTableOn(..)}), and can only ever refresh <i>everything</i>.
 *
 * @param <E> The type of the table entry {@link Object}s.
 */
@FunctionalInterface
public interface TableListDataSource<E> {

    /**
     *  When passed to {@link swingtree.UIForTable#withModel(UI.CellOrder, UI.Editability, TableListDataSource)},
     *  this method is called continuously by you table to fetch the current {@link List}
     *  based table data. It is not cached or stored insider the table, so if you
     *  do not want to rebuild the list based model over and over again make sure this
     *  list data source always returns the same object instead of rebuilding it eagerly...
     *
     * @return A list matrix which will be used to populate the table.
     */
    List<List<E>> get();

}

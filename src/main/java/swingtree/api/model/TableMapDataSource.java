package swingtree.api.model;

import java.util.List;
import java.util.Map;

/**
 *  A simple functional interface whose implementations are used to
 *  form simple {@link javax.swing.table.TableModel} implementations
 *  based on a mapping of column names to columns.
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
public interface TableMapDataSource<E> {

    /**
     *  Supplies a map of columns forming a matrix which will be used to render a table.
     *  It is called continuously by the table model to populate the table,
     *  so the implementation should avoid doing heavy computations, I/O operations
     *  or large allocations.
     *
     * @return A map of columns forming a matrix which will be used to populate the table.
     */
    Map<String, List<E>> get();

}

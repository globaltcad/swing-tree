package swingtree.api.model;

import java.util.List;
import java.util.Map;

/**
 *  A simple functional interface whose implementations are used to
 *  form simple {@link javax.swing.table.TableModel} implementations
 *  based on a mapping of column names to columns.
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

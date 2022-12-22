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
     * @return A map of columns forming a matrix which will be used to populate the table.
     */
    Map<String, List<E>> get();

}

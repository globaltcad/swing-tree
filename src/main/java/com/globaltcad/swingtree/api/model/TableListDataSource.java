package com.globaltcad.swingtree.api.model;

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
     * @return A list matrix which will be used to populate the table.
     */
    List<List<E>> get();

}

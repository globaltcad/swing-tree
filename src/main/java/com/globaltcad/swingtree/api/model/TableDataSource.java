package com.globaltcad.swingtree.api.model;

public interface TableDataSource<E> {

    int rowCount();
    int colCount();
    E    getAt(int rowIndex, int columnIndex);
    void setAt(E aValue, int rowIndex, int columnIndex);
    default boolean isEditableAt(int rowIndex, int columnIndex) { return false; }

}

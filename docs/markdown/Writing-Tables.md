
# Tables #

Tables are a common way to display data.
In SwingTree, tables are created using the `UI.table()` factory method.
But in order to display data, a table needs a model.
The model builder is created internally and then exposed to the
user in the configuration lambda expression passed to the
`withModel` method. The model builder has a fluent API
for defining the structure of the table and source of its data.
Take a look at the following example:

```java
var header = new String[]{"X", "Y", "Z"};
var data = new int[][]{...}; // or any other type of data
var dataChanged = Event.create();

// ...

UI.table().withModel( m -> m
    .colName( i -> header[i] )
    .colCount( () -> header.length )
    .rowCount( () -> data.length )
    .getter( ( rowIndex, colIndex ) ->data[rowIndex][colIndex] )
    .updateOn(dataChanged)
)
//...

// update the data displayed in the table:
dataChanged.trigger();
```

SwingTree does not require you to implement
the `TableModel` interface, instead it implements
the `TableModel` interface for you based on the
lambda expressions you provide.

These lambda expressions are supposed to
fetch all the data needed to display the table.
The `getter` lambda expression is used to fetch
the data for a specific cell.

If your table is editable, you also need to
provide a `setter` lambda expression,
which is used to update the data in the table.

Note that the table does not know when the data
has changed, so you need to fire the `dataChanged`
event yourself after you have updated the data.

The fact that all the table configuration is
done using lambda expressions, makes it very
easy to completely change how your table looks.

Additionally, this design also decouples the table from
the data it displays, which
makes it very easy to implement the MVVM
design pattern.
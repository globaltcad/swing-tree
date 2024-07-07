
# Tables #

Tables are a common way to display data.
In SwingTree, tables are created using the `UI.table()` factory method.
You may then use method chaining to define the properties of the table.
This then may look something like this:

```java
UI.table()
.withPrefSize( 200, 200 )
.withStyle( it -> it.border(3, "black") )
//...
```

For configuring the outer appearance of the table,
it is recommended to use the `withStyle` method.

## Data ##

But in order to display data, a table needs a model
which you can easily define in SwingTree through a model builder API.
The model builder is created internally and then exposed to the
user in the configuration lambda expression passed to the
`withModel` method. The model builder has a fluent API
for defining the structure of the table and source of its data.

Take a look at the following example:

```java
//...
var header = new String[]{"X", "Y", "Z"};
var data = new int[][]{...}; // or any other type of data
var dataChanged = Event.create();

// ...

UI.table().withModel( m -> m
    .colName( i -> header[i] )
    .colCount( () -> header.length )
    .rowCount( () -> data.length )
    .getsEntryAt( ( rowIndex, colIndex ) ->data[rowIndex][colIndex] )
    .setsEntryAt( ( rowIndex, colIndex, item ) -> data[rowIndex][colIndex] = (int)item )
    .isEditableIf(()->true)
    .updateOn(dataChanged)
)
//...

// update the data displayed in the table:
dataChanged.fire();
```

SwingTree does not require you to implement
the `TableModel` interface, instead it implements
the `TableModel` interface for you based on the
table model declaration you provide using
various lambda expressions.

These lambda expressions are supposed to
fetch all the data needed to display the table.
The `getsEntryAt` lambda expression is used to fetch
the data for a specific cell.

If a particular cell is defined to be editable through
the `isEditableIf` lambda expression,
then you also need to provide a `setsEntryAt` lambda expression,
which is used to update the data in the table model.

Note that the table does not know when the data
has changed, so you need to fire the `dataChanged`
event yourself after you have updated the data.

The fact that all the table configuration is
done using lambda expressions, makes it very
easy to completely change how your table looks.

Additionally, this design also decouples the table from
the data it displays, which
makes it very easy to implement the MVVM, MVI, or MVP
design patterns.

## Viewing Cells ##

The `withCell` method is used to configure the appearance
of the cells in the table. The `withCell` method takes
a lambda expression that is called for each cell in the table
for each time the cell is rendered.

It is a clean abstraction over the `TableCellRenderer` interface
which bundles all the cell state in an immutable configuration
object that is updated and then returned by the lambda expression.
This object may also hold a view component that is used to render
the cell and can be updated by the lambda expression.

The following example demonstrates how to change the background
color of the cells in the table:

```java
UI.table()
.withCell( cell -> cell
    .view( comp -> comp
        .orGetUi(()->UI.textField().withBackground(Color.MAGENTA))
        .updateIf(JTextField.class, tf -> {
            tf.setText(cell.entryAsString().orElse(""));
            tf.setForeground(cell.isSelected() ? Color.RED : Color.WHITE)
            return tf;
        })
    )
)
```

Here we use the `CellConf.view(Configurator)` method to process an `OptionalUI`
monad wrapper around the view component of the cell.
If the view is not present, we create an initial view component
using the `orGetUi` method.
And then we update the view component using the `updateIf` method,
which is only called if the view component is of the specified type.



# Tables #

A table is the most data-shaped widget there is. Which is awkward, because
`javax.swing.table.TableModel` is anything but data-shaped: it is an interface you
implement, with listeners you fire, read live by a thread you did not pick, at a
moment you cannot predict. Getting that right is a chore, and getting it right
*under two threads* is a chore with teeth.

SwingTree's answer is to stop modelling tables as behaviour and start modelling them
as **data**. Meet `TableData`:

```java
var data =
    TableData.of(UI.ListData.ROW_MAJOR, "Name", "Age", "City")
        .addRow("Alice", 30, "Rome")
        .addRow("Bob",   42, "Oslo");
```

That is a table. Not a model of a table, not a builder for a table, not a strategy
for producing a table on request. Just a value which happens to describe one, and
which you can print, compare, store, pass around and hand to another thread without
a second thought.

Put it in a property and show it:

```java
Var<TableData> data = Var.of(
        TableData.of(UI.ListData.ROW_MAJOR, "Name", "Age", "City")
            .addRow("Alice", 30, "Rome")
            .addRow("Bob",   42, "Oslo")
    );

UI.table(data);
```

That's the whole thing. There is no model to implement, no `updateTableOn(..)` to
remember, no event to fire. **Change the value, and the table changes.**

```java
data.update( it -> it.addRow("Carol", 55, "Lima") );
```

---

## Changing your table ##

Every method on `TableData` which sounds like a change gives you back a *new*
`TableData` and leaves the old one alone, exactly like `String` or `Tuple` do.
So your business logic reads like a sentence about your data, and never like
a conversation with a widget.

**Rows** come and go:

```java
data.update( it -> it
    .addRow("Dave", 27, "Kyoto")            // append one
    .addRowAt(0, "Zoe", 33, "Cairo")        // insert at the top
    .setCellAt(1, 1, 31)                    // change one cell
    .setRowAt(2, "Bobby", 43, "Oslo")       // replace a whole row
    .removeRowAt(4)                         // drop one
);
```

**Columns** come and go too, and they bring their names and classes along with
them, so there is no second list of headers to keep in sync:

```java
data.update( it -> it
    .addColumn("Active", Boolean.class, TableData.row(true, false, true))
    .setColumnNameAt(0, "Full name")
    .setColumnClassAt(1, Integer.class)     // now it renders as a number
    .removeColumnAt(2)
);
```

And when the shape of your data changes *completely* — a different report, a
different file, a different day — that is not a special case either. It is just
another value:

```java
data.set(
    TableData.of(UI.ListData.ROW_MAJOR, "Id", "Score")
        .setColumnClassAt(0, Integer.class)
        .setColumnClassAt(1, Double.class)
        .addRow(1, 9.5)
        .addRow(2, 7.25)
);
```

Rows, columns, names, classes, the number of each: all of it may change at any time.
The table simply follows.

### Reading it back ###

```java
data.getValueAt(0, 1);              // one cell
data.getRow(0);                     // a whole row
data.getColumn(1);                  // a whole column
data.getRowCount();
data.getColumnCount();
data.indexOfColumn("Age");          // address columns by meaning, not position
```

That last one deserves a word. Column indices are the kind of thing that rots the
moment somebody inserts a column, so say what you mean:

```java
data.update( it -> it.setCellAt(0, it.indexOfColumn("Age"), 31) );
```

---

## "But surely all that copying is slow" ##

It isn't, and it is worth explaining exactly why, because the worry is a reasonable
one.

**Adding a row to a thousand-row table does not copy a thousand rows.** The cells
live in `Tuple`s, which are *persistent* data structures: a new version shares its
guts with the old one and only pays for what actually differs.

**And the table does not repaint a thousand rows either.** This is the part that
usually surprises people. A `Tuple` remembers *how* it was derived from its
predecessor, and SwingTree reads that history. So when you say:

```java
data.update( it -> it.addRow("Carol", 55, "Lima") );
```

...the `JTable` is not told "everything changed, start over". It is told
*"one row was inserted at index 2"*, and repaints that one row. The same goes for
removals and updates. Your ten-thousand-row table stays fluid while you poke at it.

This is also why **range operations are worth reaching for**:

```java
data.update( it -> it.addRows(incomingBatch) );      // ONE insert event
data.update( it -> it.removeRowsAt(0, 500) );        // ONE delete event
data.update( it -> it.setRowsAt(10, updatedRows) );  // ONE update event
```

Adding a hundred rows one at a time gives you a hundred events. Adding them with
`addRows(..)` gives you one. Same data, one hundredth of the repainting.

There is one honest caveat. All of the above is true for a **row major** layout
(`UI.ListData.ROW_MAJOR`), where the cells are stored as rows, so a change to a row
is a change the table can act on directly. A `COLUMN_MAJOR` table stores columns, so
a change to it never lines up with a row range and the table has to rebuild. Column
major is there because some data genuinely is column shaped — but if your table is
big and lively, store it row major.

---

## Editable cells ##

Two things have to be true before a user may edit a cell, and both of them are the
kind of thing you would want to be explicit about anyway:

1. The layout has to say so — use one of the `*_EDITABLE` constants.
2. The data has to live in a mutable `Var`, so that the edit has somewhere to go.

```java
Var<TableData> data = Var.of(
        TableData.of(UI.ListData.ROW_MAJOR_EDITABLE, "Name", "Age")
            .addRow("Alice", 30)
    );

UI.table(data);
```

Now an edit made in the UI flows straight back into your property, as a new value.
Bind a `Val` instead of a `Var`, or use a non-editable layout, and the table is
simply read only. Flipping between the two is itself just a change of the value:

```java
data.update( it -> it.withLayout(UI.ListData.ROW_MAJOR_EDITABLE) );
```

---

## About that other thread ##

Here is the part that makes all of this more than a matter of taste.

Under `EventProcessor.DECOUPLED`, your application logic runs on one thread and Swing
paints on another. A traditional `TableModel` reads your data *live*, from the paint
thread, whenever it feels like it — which means that if your thread is halfway
through updating that data, the UI thread can see it halfway updated. Torn rows,
phantom nulls, and the occasional `IndexOutOfBoundsException` from inside a paint
call. These bugs are miserable to reproduce and worse to fix.

A `TableData` cannot be halfway anything. It is immutable, so the value the UI thread
is reading is a value nobody can change underneath it. Your thread builds the next
version at its leisure, hands it over, and the UI thread swaps it in whole. There is
no lock, no copy, and no window during which the table is inconsistent — not because
anybody was careful, but because there is nothing to be careful about.

You get this by binding a property. There is nothing to configure.

---

## The older ways ##

`TableData` is the recommended way to build a table. The rest of this page is here
for tables you already have, or for the occasional case where your data really does
live somewhere else and you would rather not copy it across.

### Lambda based models ###

You can describe a table as a set of lambdas which fetch the data on demand:

```java
var header = new String[]{"X", "Y", "Z"};
var data = new int[][]{...};
var dataChanged = Event.create();

UI.table().withModel( m -> m
    .colName( i -> header[i] )
    .colCount( () -> header.length )
    .rowCount( () -> data.length )
    .getsEntryAt( ( row, col ) -> data[row][col] )
    .setsEntryAt( ( row, col, item ) -> data[row][col] = (int) item )
    .isEditableIf( () -> true )
    .updateOn( dataChanged )
);

// ...and then, after you change the data:
dataChanged.fire();
```

SwingTree implements `TableModel` for you here, so you still do not have to. But note
what you have taken back on: the table does not know when your data changed, so *you*
have to tell it, and if you forget, the table quietly lies to your users. And because
these lambdas read your data live, SwingTree has to snapshot them for you under the
decoupled protocol — which costs a copy of the whole table on every refresh, the very
thing `TableData` never needs.

### Collection based models ###

The same applies to the collection based factories:

```java
UI.table(UI.ListData.ROW_MAJOR_EDITABLE, () -> listOfRows)
.updateTableOn(dataChanged);

UI.table(UI.MapData.EDITABLE, () -> mapOfColumns)
.updateTableOn(dataChanged);
```

Convenient for a quick table over data you already hold, and fine for small or static
ones. But they are pull based, so they need `updateTableOn(..)`; they cannot tell the
table *what* changed, so every refresh is a full rebuild; and they read live data from
the paint thread unless SwingTree copies it for them.

### Tuple based models ###

If your data is already a `Tuple` of `Tuple`s, you can bind it directly and keep the
incremental updates:

```java
Var<Tuple<Tuple<String>>> rows = Var.of(Tuple.of(
        Tuple.of("Alice", "30"),
        Tuple.of("Bob",   "42")
    ));

UI.table(UI.ListData.ROW_MAJOR, rows);
```

This is really just `TableData` with the column names and classes left out — in fact
that is exactly what SwingTree turns it into. Reach for it when your cells genuinely
are a bare matrix and you have no headers to speak of.

---

## Viewing Cells ##

However your data gets there, `withCell` is how you decide what it looks like. It
takes a lambda called for each cell as it is rendered, and it is a clean abstraction
over `TableCellRenderer` which bundles the cell state into an immutable configuration
object that you update and return. That object may also hold the view component used
to render the cell.

The following example changes the colours of the cells:

```java
UI.table(data)
.withCell( cell -> cell
    .view( comp -> comp
        .orGetUi(()->UI.textField().withBackground(Color.MAGENTA))
        .updateIf(JTextField.class, tf -> {
            tf.setText(cell.entryAsString());
            tf.setForeground(cell.isSelected() ? Color.RED : Color.WHITE);
            return tf;
        })
    )
)
```

Here we use `CellConf.view(Configurator)` to process an `OptionalUI` monad wrapper
around the view component of the cell. If the view is not present, we create an
initial one with `orGetUi`, and then update it with `updateIf`, which only runs if
the view component is of the given type.

Note that `TableData` carries the *class* of every column, and a `JTable` picks its
renderer and editor from that. So a good deal of "how does this look" is answered
simply by saying what your data is:

```java
data.update( it -> it
    .setColumnClassAt(2, Boolean.class)   // check boxes, for free
);
```

For configuring the outer appearance of the table itself, use `withStyle`:

```java
UI.table(data)
.withPrefSize( 200, 200 )
.withStyle( it -> it.border(3, "black") )
```

---

## Where to next? ##

- For the full catalogue of what a `TableData` can do, see the living documentation
  of the `Table_Data_Spec`, or just follow your IDE's autocompletion — the class is
  small on purpose.
- For why values beat objects for this sort of thing, see
  [Data Oriented Programming Benefits](./Data-Oriented-Programming-Benefits.md) and
  [Data Oriented SwingTree](./Data-Oriented-SwingTree.md).
- For dynamic *list* views (rather than tables) backed by reactive collections,
  see the `addAll(..)` pattern explained in
  [Functional MVVM → Dealing with Lists](./Functional-MVVM.md#dealing-with-lists).
- For wiring a table into a view model, see [Basic MVVM](./Basic-MVVM.md) or
  [Functional MVVM](./Functional-MVVM.md). A `Var<TableData>` exposed by your view
  model is all a table needs.
- For styling the table (gradients, rounded corners, custom borders), any of the
  patterns from [Style Sheets and Groups](./Style-Sheets-And-Groups.md) applies to a
  `JTable` exactly as it does to a `JButton`.

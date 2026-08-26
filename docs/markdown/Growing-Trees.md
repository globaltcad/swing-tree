# Trees #

Somewhere in your application there is already a tree. A folder with folders in it,
a scene graph, a parsed document, a menu, an org chart, a comment thread. You did
not have to be told it was a tree; it just is one.

And then you go to put it on screen, and `javax.swing.JTree` asks you to build a
*second* tree — a graph of `DefaultMutableTreeNode`s, each one a little mutable wrapper
around a value you already had — and to keep the two in step by hand, forever.
Change something in the real tree and the one on screen has no idea. Change something
on screen and now you have two versions of the truth and a bug waiting for a demo.

SwingTree's answer is to skip the second tree entirely. **Bind the one you already
have, and say which parts of it are the branches.**

---

## Everything is in a box ##

Let's move house, because nobody has to be taught what packing looks like. The kitchen
box has a smaller box in it marked *small appliances*, and the kettle is in there
somewhere. A box holds boxes; a kettle holds nothing. That is a tree, and you did not
design it — it is just how packing works.

In Java 21 it is a sealed interface and two records:

```java
public sealed interface Packed extends HasId<String> { String label(); }

@With public record Box ( String id, String label, Tuple<Packed> contents ) implements Packed {}
@With public record Item( String id, String label, boolean fragile )        implements Packed {}
```

(The `@With` is Lombok, and it is what gives us `withLabel(..)` and `withContents(..)`
further down. Records do not generate withers by themselves, and hand-writing them
gets old fast.)

That is your data. No Swing anywhere in it, no base class, nothing to extend. Put the
move in a property:

```java
Var<Packed> move = Var.of(Packed.class, new Box("move", "The move", Tuple.of(
        new Box("kitchen", "Kitchen", Tuple.of(
                new Box("appliances", "Small appliances", Tuple.of(
                        new Item("kettle",  "Kettle",  false),
                        new Item("blender", "Blender", true)
                )),
                new Item("plates", "Plates", true)
        )),
        new Box("books", "Books", Tuple.of(
                new Item("cookbooks", "Cookbooks", false)
        ))
)));
```

...and show it:

```java
UI.tree(move, conf -> conf
    .nodesOf(Box.class,  it -> it.children(Box::contents).text(Box::label))
    .nodesOf(Item.class, it -> it.text(Item::label))
);
```

That's the whole thing. One `nodesOf(..)` block per kind of node, which — read out
loud — is one `case` of the `switch` you would otherwise have written by hand:

> *A box's children are its contents, and it is labelled by its label.
> An item is labelled too, and has nothing inside it.*

Nothing said an item is a leaf. Nothing had to: **declaring a `children(..)` rule is
what makes a node type a branch**, and a type without one is a leaf. Your data already
knew which was which; the rules just read it out.

---

## Getting the shape right ##

Three small things, and then we can move on to the interesting part.

**An empty box is still a box.** A plain `JTree` calls any node with zero children a
leaf, which draws an empty box as though it were a kettle. SwingTree goes the other
way: a children rule makes you a branch whether or not you currently have anything in
you, so the box you have not filled yet still gets a handle the user can open. If you
want the old behaviour for the whole tree:

```java
conf.leafWhenEmpty(true)
```

**A box you have not opened yet is a case of its own.** Sum types are good at this. The
boxes still at the storage unit are definitely boxes — you just do not know what is in
them until somebody drives over and opens one, and clicking the handle is what sends
them:

```java
@With public record InStorage( String id, String label ) implements Packed {}

conf.nodesOf(InStorage.class, it -> it
        .text( b -> b.label() + " (still in storage)" )
        .isLeaf(false)              // a branch with nothing in it — yet
)
```

**Labels, icons and tool tips live with the node type they belong to**, which is how a
box and a wine glass end up looking different without a single `if` in a renderer:

```java
conf.nodesOf(Item.class, it -> it
        .text( i -> i.fragile() ? i.label() + "  ·  fragile" : i.label() )
        .icon( i -> i.fragile() ? Icons.FRAGILE : Icons.ITEM )
        .toolTip( i -> "Packed on the 14th" )
)
```

Forget a `nodesOf(..)` block and the node still shows up — as a leaf labelled by its
`toString()`, with a warning in the log naming the type you have no rule for. Java 8,
which SwingTree compiles against, cannot check a sealed hierarchy for exhaustiveness,
so this is the next best thing: not a compile error, but never a silent one either.

---

## One wither, and the whole thing runs backwards ##

Here is the rule the rest of this page is really about:

> **A getter alone is read only. A getter *together with a wither* is a lens, and a
> lens is two-way.**

It is the same rule `Var.zoomTo(getter, wither)` follows, applied per node type. Add a
wither to `children(..)` and the contents become the user's to move around. Add one to
`text(..)` and they can relabel a box in place, right in the tree:

```java
UI.tree(move, conf -> conf
    .nodesOf(Box.class, it -> it
        .children(Box::contents, Box::withContents)   // ← contents are writable
        .text(Box::label, Box::withLabel)             // ← relabel in place
    )
    .nodesOf(Item.class, it -> it
        .text(Item::label, Item::withLabel)
    )
);
```

Now think about what has to happen when somebody relabels the kettle.

`Item` is immutable, so relabelling it makes a new `Item`. Which means the *small
appliances* box now holds different contents, so that is a new `Box`. Which means the
*Kitchen* box holds a different box, so that is a new `Box` too — and so is the root.
The change walks all the way up and comes out the top as **one new value in your one
property**.

You do not write any of that. The chain of `children(getter, wither)` rules from the
root down to the changed node is exactly the information needed to do it, and declaring
those rules is all the API ever asks of you.

Leave a wither off and you have said something useful too. A box declared
`children(Box::contents)` with no wither is one nobody may repack: a change from inside
it is dropped, and SwingTree logs it once, naming the box that refused. Which is a fair
description of a box you have already taped shut.

---

## Selection is a property — and a selection is a *path* ##

```java
Var<Tuple<String>> opened = vm.zoomTo(Move::opened, Move::withOpened);

UI.tree(move, conf -> ..)
.withSelection(opened);
```

Both directions, no listener. Clicking a box writes into the property; assigning to the
property opens whatever boxes stood closed above the one it names, selects its row and
scrolls it into view.

But look at what the property holds. Not a box — **a path of ids**, leading from the root
down to whatever is selected:

```
nothing selected   Tuple.of(String.class)
the root           [ "move" ]
the kettle         [ "move", "kitchen", "appliances", "kettle" ]
```

This is worth dwelling on, because the obvious design is wrong. A selection is a
**position** in the tree, and a node value cannot name a position. Every box you own has a
cable in it; ids only have to be unique among siblings, so several of those cables may
legitimately be `"cable"`. Hand a tree a *box* and ask it to select that box, and it can
only go looking — and it may well find the wrong one, or move the user's selection off the
row they just clicked.

A path has none of that. There is nothing to search for, nothing to be ambiguous about, and
— the part that seals it — no node data riding along which could *contradict* the tree. A
`Tuple<Box>` whose ids match a real path but whose contents differ would have no defensible
meaning; a selection binding has no business writing box data back, but silently ignoring it
would leave the property lying about what it holds. A `Tuple<String>` has nothing to lie
about.

The empty path means nothing is selected, so there is no null to handle either. And because
the root's own id is the first element, "the root is selected" is a path of exactly one id
rather than being indistinguishable from "nothing".

### Getting from a path back to a box ###

A path says *where*, not *what*. When a detail pane needs the box itself, ask the shape of
the tree — which is an ordinary value you can keep:

```java
TreeConf<String, Packed> shape = TreeConf.of(Packed.class)
        .nodesOf(Box.class,  it -> it.children(Box::contents).text(Box::label))
        .nodesOf(Item.class, it -> it.text(Item::label));

UI.tree(move, shape);                       // bind it...

Val<Packed> openedBox = Viewable.of(move, opened,     // ...and ask it questions
        (root, path) -> shape.nodeAt(root, path).orElse(null));

.add(openedBox, this::detailsFor)
```

`nodesAlong(..)` gives you the whole chain instead of its last step, which is what a
breadcrumb is:

```java
shape.nodesAlong(move.get(), opened.get())
     .toList().stream().map(Packed::label).collect(joining("  →  "));
// The move  →  Kitchen  →  Small appliances  →  Kettle
```

Both answer emptily for a path which no longer leads anywhere, so a selection left pointing
into a box you have since unpacked never produces a half-resolved trail.

### Several at once ###

Bind a tuple *of* paths, and the tree switches to multiple selection on its own — there is
no mode to set, because which overload you called already said it:

```java
.withSelectionPaths(unpackFirst)   // Var<Tuple<Tuple<String>>>
```

### When a property is not enough ###

`onSelection(..)` hands you a delegate that answers both questions a selection raises —
*what* is selected, in your own types, and *which position*:

```java
.onSelection( it -> {
    whereItIs.set( it.pathToLead().toList().stream()
                     .map(Packed::label).collect(joining("  →  ")) );
    // The move  →  Kitchen  →  Small appliances  →  Kettle
    remember( it.leadPath() );          // [ "move", "kitchen", "appliances", "kettle" ]
} )
```

It will also hand you a `Var` focused on the selected node, so an action can go straight on
to change what was just picked:

```java
.onSelection( it -> it.property().ifPresent( node ->
        node.update( n -> n instanceof Item i ? i.withFragile(true) : n )
) )
```

---

## "Surely relabelling one box collapses the whole inventory" ##

This is the objection to have, and it is a good one. Let's take it seriously, because
the answer is the most interesting thing in this whole API.

A `JTree` remembers which branches you opened, and what you had selected, as a set of
`TreePath`s. A `TreePath` is a list of nodes, and two of them are equal when their
nodes are equal.

Now: your nodes are records. Records compare by **content**. And we just established
that relabelling the kettle produces a new `Item`, a new *small appliances*, a new
*Kitchen* and a new root.

So every single `TreePath` in the tree is now stale. Simultaneously. The tree has no
idea any of these are the same nodes it was showing a moment ago, and everything the
user had open snaps shut. This is the reason there is no obvious way to put immutable
data in a `JTree`, and it is why people go on building that second tree of mutable
wrappers.

The fix is to stop asking the tree to identify a node by what it *contains*, and to
identify it by **where it sits** — specifically, by the path of ids leading down to it:

```
[ "move", "kitchen", "appliances", "kettle" ]
```

Relabelling the kettle does not change any of those ids. So the path is the same path,
the tree recognises it, and your open boxes stay open. That is what
`Packed extends HasId<String>` is for, and it is the only thing SwingTree asks of your
node type. If you cannot change the type — somebody else's class, an enum, a value
straight out of a parser — say what its identity is instead. Here is what that looks
like for the company tree further down, whose three node types are records nobody
thought to give an id to:

```java
conf.idOf( node -> node instanceof Department d ? d.code() : "company" )
```

One nice detail: an id only has to be unique **among siblings**. Every box may hold
something called `"cable"` and nothing gets confused, because the box above each of them is
part of the address — which is the same reason a selection is a path rather than a node.

---

## "And surely all that walking is slow" ##

Also a fair question, also worth answering properly, because the answer is what makes
this usable on a tree with two hundred thousand nodes in it.

When your property changes, SwingTree walks the tree to work out what to tell the
`JTree` about. That walk is bounded by two lines, and they are the whole performance
story:

**It stops at anything reference-identical to what was there before.** Your `Tuple`s are
persistent, so relabelling the kettle leaves the *Books* box not merely equal to what it
was, but *the very same object*. One `==` and the walk turns around. Everything the
change did not touch costs a pointer comparison.

**It never looks below a collapsed branch.** Nothing under it is on screen, so nothing
under it needs an event. A change buried four boxes deep inside one nobody has opened
costs nothing at all — and shows up correctly the moment they open it, because the tree
reads the contents from your value at that point, not from a copy it made earlier.

Put those together and the cost of a change is proportional to **what is on screen**,
not to what exists. Which is the property you actually want: the tree stays fluid
whether you are moving out of a studio flat or a warehouse.

The events it sends are targeted too. A relabel becomes "these rows read differently"
and repaints them, rather than "everything changed, start over" — which the `JTree`
would answer by throwing away everything it knows, including the expansion you just
went to such trouble to preserve.

When a change *is* structural — a box packed or unpacked — that rebuild does happen, and
expansion and selection are captured by id and put back. One detail of that is worth
knowing about, because it would otherwise be a slow-burning bug in your application: a
`JTree` drops its selection on the way through a rebuild, and a binding which passed that
on would tell you "nothing is selected" every time anybody added anything, anywhere. It
does not. The write-back is muted for the length of the rebuild and the settled selection
announced once at the end, so what your property sees is what is true after the tree has
finished moving — including, when the selected box really was unpacked, that nothing is
selected any more.

---

## Trees that are not sum types ##

Not every tree is one type. A company holds departments, a department holds employees,
and those three have nothing in common but `Object`. That works exactly the same way —
name the common supertype and carry on:

```java
UI.tree(Object.class, String.class, company, conf -> conf
    .idOf( node -> node instanceof Department d ? d.code()
                 : node instanceof Employee e   ? e.code()
                 : "company" )
    .nodesOf(Company.class,    it -> it.children(Company::departments).text(Company::name))
    .nodesOf(Department.class, it -> it.children(Department::staff).text(Department::name))
    .nodesOf(Employee.class,   it -> it.text(Employee::name))
);
```

Two types are named here rather than one: the common supertype, and the type of the ids —
because none of these three can implement `HasId`, so `idOf(..)` supplies their identity and
the tree needs to know what a path through them is made of.

The children of a node do not have to be the same type as the node, which is what makes
this work at all. And because `nodesOf(..)` is typed on the class you passed it, the
rules inside a `Department` block still receive a `Department` and not an `Object`.

When several types share behaviour, declare it once and let the types that differ say so —
the most specific rule for a node's type wins:

```java
conf.nodesOf( it -> it.text(Packed::label) )               // items are labelled by this...
    .nodesOf(Box.class, it -> it.children(Box::contents)   // ...and boxes by their own block,
                                .text(Box::label))         //    which has to say so itself
```

Note the repeated `text(..)`. **The winning rule is chosen, not merged**: a block for one
type is the whole answer for that type, so it does not inherit anything from the catch-all
above it. A `nodesOf(..)` block reads like one `case` of a `switch`, and a `case` does not
quietly continue into another one. Leave the `text(..)` off the `Box` block and boxes lose
their labels to `toString()`.

---

## Making cells look like something ##

`text(..)` and `icon(..)` cover the great majority of trees, and they have the virtue
of living next to everything else that is true about a node type. When a node needs a
view a label cannot give it, `withCells(..)` opens the same cell builder the list,
combo box and table components use:

```java
UI.tree(move, conf -> ..)
.withCells( it -> it
    .when(Box.class).asText( cell -> cell.entry().map(Box::label).orElse("") )
    .when(Item.class).asComponent( cell -> itemRow(cell) )
);
```

Your renderer is handed **your** node value. The handles the tree keeps inside its
paths are an internal matter and never surface — which is also why
`JTree.getLastSelectedPathComponent()` is not a useful thing to call on a bound tree.
Use `onSelection(..)`, or the bound property, and you will never want it.

The two ways of saying how a node looks mix freely. A type no `when(..)` clause mentions
keeps whatever its own `text(..)`, `icon(..)` and `toolTip(..)` say, so reaching for
`withCells(..)` to give *one* case a real component does not cost you the labels of all the
others. (`withCellRenderer(..)` is the blunt version and does: a renderer installed that way
answers for every node, and the rules step aside.)

Styling the tree itself is styling like anywhere else:

```java
UI.tree(move, conf -> ..)
.withRootVisible(false)              // "The move" is a container, not a thing to look at
.withInitialExpansionDepth(2)        // open the first two levels on arrival
.withStyle( it -> it.borderRadius(8).padding(6) );
```

`withRootVisible(false)` deserves a word. A tree grown from `tree(..)` has exactly one
root, and that root is sometimes a container which is simply not a thing to look at — "The
move" itself. Hiding it drops that row and turns the top-level handles on, so what is now
the first visible level can still be opened. `withInitialExpansionDepth(n)` then counts from
that first visible level, so `n` means the same thing whether the root is drawn or not: the
number of levels a user would count looking at the screen.

Do not invent a root in order to hide it, though. Where your data genuinely has several
things at the top, say so — which is the next section.

A property may also hold *nothing*, which a `Var.ofNullable(Packed.class, null)` does while
the move is still being loaded. That is an empty tree: no root, no rows, and no placeholder
row reading `null`. It fills in the moment the property does.

---

## Several boxes on the van floor ##

Look at the move again. Everything in it was inside *one* box marked "The move", which is
convenient for an example and is not how a van gets loaded. Boxes stand side by side on the
floor, and **the floor is not a box**.

That shape — several top-level things with nothing above them — is at least as common as
the single-rooted one. A workspace holds several open projects, a document holds several
top-level blocks, a scene holds several graphs. In a view model it is a tuple:

```java
Var<Tuple<Packed>> onTheVan;
```

You *can* squeeze that into `UI.tree(..)` by inventing a box to put it all in, and it is
worth seeing what that costs before reaching for it:

```java
record Van( Tuple<Packed> boxes ) implements Packed {   // ← a case your domain does not have
    public String id() { return "van"; }                // ← an id nobody chose
}
```

`Van` is now a permitted case of `Packed`, so every `switch` over `Packed` anywhere in the
application has to handle it or stop compiling — which is precisely the property you chose a
sealed interface for, spent to satisfy a view. And since a selection is a path of ids,
`"van"` is now the first element of every path you persist:

```
[ "van", "kitchen", "appliances", "kettle" ]
```

The fiction did not stay at the binding site. It got into the data.

So say what is true instead — there are several, and nothing contains them:

```java
UI.trees(onTheVan, conf -> conf
    .nodesOf(Box.class,  it -> it.children(Box::contents, Box::withContents).text(Box::label))
    .nodesOf(Item.class, it -> it.text(Item::label))
)
.withSelection(opened);   // [ "kitchen", "appliances", "kettle" ]
```

**`trees(..)` is `tree(..)` with the plural spelled out**, and that is the whole of the
difference: the same `TreeConf`, the same rules, the same wither turning a rule two-way, the
same sync walk, the same everything below the top level. The very same configuration value
binds either form, in fact, because a `TreeConf` describes node types and says nothing at all
about how many things are at the top.

Three consequences are worth writing down:

- **Nothing is drawn above the top level, and nothing can be.** There is no root row, no
  `withRootVisible(true)` which will produce one — asking for it is ignored and logged — and
  no path which names the forest. `[]` still means "nothing is selected", and it is now the
  only special case left.
- **Selection paths are one element shorter.** `[ "kitchen", "appliances", "kettle" ]`, not
  `[ "van", ... ]`. Migrating off the wrapper above therefore means dropping the first element
  of every path you have stored.
- **An empty tuple is an empty tree.** No rows, no placeholder, filling in when the property
  does — the same thing a `tree(..)` bound to a property holding nothing already does.

You do not have to name the node type. A `Tuple` carries the type of its elements, and carries
it while empty, so `UI.trees(onTheVan, ..)` reads it off the value — which matters, because a
workspace with nothing open yet is the ordinary state on first launch. Name it —
`UI.trees(Packed.class, onTheVan, ..)` — only when the tuple was built with a *narrower*
element type than the tree's nodes, which `Tuple.of(Box.class, ..)` assigned to a
`Var<Tuple<Packed>>` does.

Underneath, this is still exactly one `JTree`. `javax.swing.tree.TreeModel` declares

```java
public abstract java.lang.Object getRoot();
```

— one `Object`, and Swing has never had room for more. So the container still exists. It is
simply SwingTree's, held inside the model, drawn nowhere and named by nothing you can see,
which is where a container of that kind belongs. A panel of several `JTree`s would have been
worse: selection, keyboard navigation and drag are all per-component, so arrow-keying off the
bottom of one would not enter the next.

---

## Under the hood, briefly ##

Three pieces, if you ever go looking:

- **`TreeNodeRef`** is what the `JTree` actually holds in its paths. Its identity is
  the path of ids; the node's current value rides along in a field that takes no part
  in equality. This is the trick from the identity section, made concrete — and it is
  also why a bound selection is exact in both directions: the path the user clicked *is*
  the value written into your property, with nothing reconstructed by searching.
- **`TreePathLens`** turns a change into a new bound value: walk down by ids from the top
  level, then rebuild upwards through each level's children wither. It never touches the UI,
  so it is safe to evaluate on any thread — which matters, because under
  `EventProcessor.DECOUPLED` it runs on your application thread. The last step up is the only
  thing a forest does differently: it writes into the tuple rather than replacing the root.
- **The sync walk** is the two-line performance story above, plus a fallback. When a
  change is structural — something packed, something unpacked, a node that became a
  different case of the sum type — it captures which branches are open by their ids,
  tells the tree to rebuild, and opens them again. Correct, and invisible, precisely
  because ids survive what content does not.

And the threading, which you get without asking: your node type is deeply immutable, so
the value the paint thread is reading is a value nobody can change underneath it. There
is no lock, no defensive copy, and no window during which the tree is inconsistent —
not because anyone was careful, but because there is nothing to be careful about.

---

## What is not there yet ##

Two things are designed but unbuilt, and it is better to say so than to let you go
looking:

- **Expansion as model state.** An `expanded(getter, wither)` rule, so that "which
  boxes are open" becomes an ordinary field of your value — serialisable, testable
  without a GUI, restorable by assignment. Today expansion is Swing's, and survives
  your edits, but is not yours to set from the model.
- **A bound view per node.** A `view(..)` rule receiving a `Var` focused on that one
  node, so a cell can hold a real check box — *unpacked, yes or no* — wired straight
  into your data. The lens it needs already exists (`onSelection(..)` hands you the
  very same thing), but wiring live components into a `JTree`'s renderer/editor split
  is its own piece of work.

---

## Where to next? ##

- The executable catalogue of everything on this page lives in the living
  documentation: **Growing a Tree from a Property**, **Keeping a Bound Tree in Sync**,
  **Selecting and Editing in a Bound Tree** and **Growing a Forest from a Property**.
- A tree in a real application: the materials tree in
  [**AtelierView**](../../src/test/java/examples/laf/app/AtelierView.java), derived from
  the shelves of its view model, so a shelf that runs down while cloth ships relabels
  itself without anybody telling the tree about it.
- For the sibling idea one dimension down, see [Writing Tables](./Writing-Tables.md) —
  a `JTable` bound to one immutable `TableData`.
- For *why* immutable values make all of this work, see
  [Data Oriented SwingTree](./Data-Oriented-SwingTree.md) and
  [Data Oriented Programming — Benefits](./Data-Oriented-Programming-Benefits.md).
- For putting a `Var<Packed>` into a view model in the first place, see
  [Functional MVVM](./Functional-MVVM.md).

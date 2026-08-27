# Growing Forests #

> **Status: implemented.** This started life as a design sketch and is kept as the record of
> why the API looks the way it does. What shipped follows it closely; the places it departs
> from the sketch are listed under *Decisions taken* at the end, and the
> `withInitialExpansionDepth(..)` measurements there are the ones the fix was written against.
>
> The user-facing prose lives in
> [Growing Trees § *Several boxes on the van floor*](../markdown/Growing-Trees.md), and the
> executable catalogue in `Tree_Forest_Spec` ("Growing a Forest from a Property").

---

## The problem ##

`UI.tree(..)` binds a property holding **one** node:

```java
UI.tree( Var<FsNode> root, conf -> .. )
```

A great many real trees are not shaped like that. A workspace holds several open
projects, a document holds several top-level blocks, a store room holds several
shelves, a scene holds several graphs. Their natural shape is a property holding a
**tuple** of top-level nodes:

```java
Var<Tuple<FsNode>> projects;
```

Before this work, that had to be squeezed into the single-root form by inventing a node
type which is not part of the domain:

```java
public sealed interface FsNode extends HasId<String> { .. }

record Workspace( Tuple<FsNode> roots ) implements FsNode {   // <- a case the domain does not have
    public String id() { return "workspace"; }                // <- an id nobody chose
}

Var<FsNode> wrapped = vm.zoomTo(
    m -> new Workspace(m.projects()),
    (m, w) -> m.withProjects(((Workspace) w).roots())          // <- a cast, because the lens is typed on the sum
);

UI.tree(wrapped, conf -> conf
    .nodesOf(Workspace.class, it -> it.children(Workspace::roots, Workspace::withRoots))
    .nodesOf(Dir.class,  ..)
    .nodesOf(Doc.class,  ..)
).withRootVisible(false);
```

Two things are wrong with that, and the second is the serious one.

**The sum type gains a case.** `Workspace` is now a permitted subtype of `FsNode`, so
every `switch` over `FsNode` anywhere in the application has to handle it or fail
exhaustiveness checking. That is exactly the property a sealed interface was chosen for,
spent to satisfy a view.

**The fiction leaks into stored state.** A selection is a path of ids, so
`withSelection(Var<Tuple<String>>)` writes

```
[ "workspace", "myapp", "src", "App.java" ]
```

into the view model. That tuple is what gets persisted, asserted on in tests, and
restored on the next launch. The workaround is not confined to the binding site — it
contaminates the data.

There is also an inconsistency worth naming: `addAll(Var<Tuple<M>> models, BoundViewSupplier<M>)`
binds a tuple property directly. One dimension up, the tree refuses to.

---

## Why this is one `JTree`, not several ##

`javax.swing.tree.TreeModel` declares:

```java
public abstract java.lang.Object getRoot();
```

One `Object`. Swing has no multi-root tree model and never has had one. The universal
idiom for a forest is a single container root that is never drawn, which is what
`setRootVisible(false)` plus `setShowsRootHandles(true)` produces.

So the proposal below still builds **one** `JTree` and still returns
`UIForTree<I, N, UI.Tree>`. Nothing about the component changes. The only difference is
that the container root becomes SwingTree's fiction, held inside the model, instead of
one the application had to add to its own sum type.

A panel of several `JTree`s would be actively worse: selection, keyboard navigation and
drag are all per-component, so arrow-keying off the bottom of one would not enter the
next, and a single bound selection property could not describe "which node, in which
tree" without reintroducing exactly the container it was trying to avoid.

---

## The API ##

A parallel family of factory methods named `trees(..)`, plural, because what is bound is
several top-level nodes and because the name then says at a glance which of the two
forms a call site is using.

```java
// identity from the HasId bound, node type from the tuple
static <I, N extends HasId<I>> UIForTree<I,N,UI.Tree> trees( Var<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );
static <I, N extends HasId<I>> UIForTree<I,N,UI.Tree> trees( Val<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );

// the tuple's element type is narrower than the tree's nodes: name the node type
static <I, N extends HasId<I>> UIForTree<I,N,UI.Tree> trees( Class<N> nodeType, Var<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );
static <I, N extends HasId<I>> UIForTree<I,N,UI.Tree> trees( Class<N> nodeType, Val<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );

// node types which cannot implement HasId: name both types, declare conf.idOf(..)
static <I, N> UIForTree<I,N,UI.Tree> trees( Class<N> nodeType, Class<I> idType, Var<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );
static <I, N> UIForTree<I,N,UI.Tree> trees( Class<N> nodeType, Class<I> idType, Val<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );

// a TreeConf you already hold
static <I, N> UIForTree<I,N,UI.Tree> trees( Var<Tuple<N>> roots, TreeConf<I,N> conf );
static <I, N> UIForTree<I,N,UI.Tree> trees( Val<Tuple<N>> roots, TreeConf<I,N> conf );
```

Eight overloads, positionally identical to the eight `tree(..)` has. (The sketch originally
proposed a three-argument form naming the *identity* type instead; see *Decisions taken*.)

Note what is missing compared with `tree(..)`: there is no `Class<N> nodeType`
parameter in the common case. A `Tuple` carries its element type at runtime
(`Tuple.type()`), and it carries it even when empty, so the tree can ask the bound value
what its nodes are instead of being told.

That inference has one failure mode which needs an escape hatch: a tuple built as
`Tuple.of(Dir.class, ..)` and assigned to a `Var<Tuple<FsNode>>` reports `Dir` as its
element type, which would make the configuration reject `Doc` nodes below it. That is what
the three-argument overload above is for.

**A `TreeConf` describes node types, not root shape**, so the very same configuration
value binds through `tree(..)` and through `trees(..)`. That is worth preserving: it is
what lets an application hold one `TreeConf` and use it both to drive a tree and to
answer `nodeAt(..)` questions.

Usage, for comparison with the workaround above:

```java
UI.trees(projects, conf -> conf
    .nodesOf(Dir.class, it -> it.children(Dir::entries, Dir::withEntries).text(Dir::name))
    .nodesOf(Doc.class, it -> it.text(Doc::name))
)
.withSelection(selectedPath);   // [ "myapp", "src", "App.java" ]  -- no fiction in it
```

No wrapper type, no cast, no `withRootVisible(false)` to remember, and the sealed
interface still has exactly the cases the domain has.

---

## The mechanism: the forest handle ##

A bound tree hands the `JTree` `TreeNodeRef` handles rather than the application's own
node values, and a handle's identity is the **path of ids** leading down to it. A
forest is a synthetic handle sitting above the top-level nodes, carrying:

- an **empty id path** (`Object[0]`),
- a `null` value, because it wraps no node of the application's,
- `null` as its parent.

Almost everything falls out of that empty id path. `TreeNodeRef.child(..)` builds a
child's id path by appending to its parent's:

```java
Object[] childPath = Arrays.copyOf(_idPath, _idPath.length + 1);
```

So a forest handle's children get `[ childId ]` — a path of length one — and their
children `[ childId, grandchildId ]`, and so on. Selection paths therefore come out
relative to the forest with no special casing anywhere in the path arithmetic, and
`withSelection(..)` writes exactly what the application would have written by hand.

`_isExpanded(..)` already does the right thing without modification, because it reads:

```java
if ( ref.parent() == null && !tree.isRootVisible() )
    return true; // An invisible root always shows its children.
```

which is precisely the forest handle's situation.

---

## What changes, site by site ##

The risk in this work is **not** the path arithmetic. It is that a forest handle wraps
no node value, and several places assumed that a handle always does. Each of these needed
a decision; the middle column is what the code did before this landed.

### `PropertyTreeModel`

| Site | Today | For a forest |
|---|---|---|
| `_newRootRef()` | wraps the root value, id path `[rootId]` | build the forest handle: `null` value, id path `[]` |
| `_childrenOf(node)` | looks up the node's rule and calls its `children(..)` getter | for the forest handle, return the bound tuple directly — there is no rule to consult |
| `isLeaf(node)` | no rule ⇒ `true` | **must return `false`** for the forest handle, or nothing draws |
| `valueOf(node)` | the handle's node value | `null` for the forest handle; every caller must tolerate it |
| `valueForPathChanged(..)` | guards non-handles | must also ignore the forest handle — it has no text rule to write through |
| `propertyFor(idPath, value)` | builds a `TreePathLens` | an **empty** id path focuses nothing; return `null` |
| `applyNewRoot(..)` | rebuilds when the root's *id* changed | compare the tuples instead; the forest handle's identity never changes |
| `_sync(..)` | bails out when `oldValue.getClass() != newValue.getClass()` | at the top level compare the two tuples; below the top level unchanged |
| `pathForIds(idPath)` | checks `idPath[0]` against the root id, then loops from level `1` | loop from `rootRef.idPath().length` — `1` for a tree, `0` for a forest — which unifies both |
| `idType()` | derives from the root node's id | derive from the first element of the tuple |

Everything else in that class — `_canonicalChild`, `_captureExpandedIdPaths`,
`_restoreExpanded`, `_restoreSelection`, `idTupleOf`, `_fireStructureChanged` — is
already expressed in terms of handles and id paths and should need no change.

### `TreePathLens` — the one structural change

`TreePathLens` is declared `Lens<N, N>`: it reads a node out of a **root node** and
writes a new root node back. A forest is bound to `Var<Tuple<N>>`, so the lens there is
`Lens<Tuple<N>, N>` — a different type, not a different implementation.

Two ways to take this, and the choice should be made deliberately:

1. **A second lens class.** Duplicates the descent and the bottom-up rebuild. Simple,
   dumb, and the two copies will drift.
2. **Parameterise the lens on the bound value type**, with a small internal notion of
   "how to get the top-level nodes out of the bound value, and how to put them back".
   For a forest that is the tuple itself; for a single-rooted tree it is the one root.
   Both `_sync` and `pathForIds` then collapse to one code path with the single-root
   case as the degenerate one.

Option 2 is the better shape, with one hazard to respect: the single-root case must not
be expressed as `Tuple.of(theRoot)` computed on demand. `_sync`'s whole performance
story rests on reference identity (`oldChildren == newChildren`), and a tuple allocated
fresh on every read is never reference-identical to the last one, which would silently
turn "stop at anything unchanged" into "walk everything on screen, every time". Keep the
single-root path free of allocation.

### `TreeConf`

`nodeAt(root, path)` and `nodesAlong(root, path)` resolve a selection path back to
nodes, and they take the root **node**. They need tuple-taking overloads:

```java
public Optional<N> nodeAt   ( Tuple<N> roots, Tuple<I> path );
public Tuple<N>    nodesAlong( Tuple<N> roots, Tuple<I> path );
```

with the same contract as the existing pair: empty for the empty path, empty for a path
that no longer leads anywhere, and never a partial trail.

### `UIForTree`

- `_bind(..)` sets `rootVisible = false` and `showsRootHandles = true` for a forest, and
  nothing later should be able to turn the first of those back on.
- `withRootVisible(true)` on a forest-bound tree has no meaning — there is no root node
  to show. Log a warning naming the method and ignore it, in the same spirit as
  `_requireBoundModel(..)`.
- `withInitialExpansionDepth(..)` — see the open decision below.

---

## Semantic differences to write down ##

These are not implementation details; they are things an application author has to know.

**No path names the forest.** With `tree(..)`, a path of one id means "the root is
selected". With `trees(..)`, a path of one id means "a top-level node is selected", and
there is *no* path meaning "the forest is selected". That is correct — the forest is not
a thing in the domain — but it means the two forms are not interchangeable for code that
inspects paths, and `[]` (nothing selected) is the only special case left.

**Selection paths get one element shorter.** An application migrating off the wrapper
workaround will find its persisted selection paths no longer match, because the
synthetic root's id used to be element 0. This belongs in the release notes with a
one-line migration: drop the first element.

**An empty tuple is an empty tree.** No rows, no placeholder, and it fills in when the
property does — the same behaviour a `tree(..)` bound to a property holding nothing
already has.

---

## Decisions taken ##

**1. `withInitialExpansionDepth(..)` counted the invisible root.** It compared
`TreePath.getPathCount()`, which includes the root whether or not the root is drawn.
Measured against the implementation as it then stood:

```
visible root, depth 1:  root / src / README.md          <- the root opened
hidden  root, depth 1:  src / README.md                 <- nothing opened
hidden  root, depth 2:  src / deep / README.md          <- the top level opened
```

So the argument meant one thing for a visible root and one less for a hidden one. Under
`trees(..)` a hidden root is the *only* configuration, which would have made that the default
experience.

**Taken: depth counts from the first visible level in both forms**, so `depth(1)` opens the
top level whether the root is drawn, hidden, or absent. This is a behaviour change for
`tree(..).withRootVisible(false).withInitialExpansionDepth(n)`; the alternative, fixing it
only for `trees(..)`, would have left two rules for one method. Three scenarios pin it, and
they do fail if the offset is removed. It has one ordering consequence, now documented on the
method: it reads `isRootVisible()`, so it has to be called *after* `withRootVisible(false)`.

**2. Node type inference from `Tuple.type()`.** Confirmed by measurement, which was the open
question: an empty tuple reports its declared element type through every construction path
sprouts offers — `Tuple.of(Class)`, `Tuple.of(Class, Iterable)`, and a tuple emptied by
`removeAll` — so a forest which starts empty, the ordinary state on first launch, still knows
what it is a forest of. `Var.of(tuple).type()` is *not* usable for this: it reports the
concrete implementation class rather than `Tuple`, and carries no element type at all. The
inference therefore reads `roots.orElseNull().type()`, and only a property holding no tuple
at all leaves the question unanswered, which is logged naming the overload to reach for.

**3. `Vals<N>` was rejected.** The tuple form is the one the rest of the immutable-value API
is built on, and a second binding shape would have doubled the surface for nothing.

**4. The overload family mirrors `tree(..)` positionally.** The sketch proposed
`trees(Class<I> idType, Var<Tuple<N>>, conf)` as the three-argument form, on the grounds that
the node type is inferred and the id type is therefore the only one left to name. Rejected:
`tree(SomeClass.class, root, conf)` names the *node* type, and a `trees(..)` next to it in the
same file naming the *id* type instead is a trap no javadoc undoes. Argument one now means the
same thing in both, and the escape hatch for inference is that three-argument form rather than
a signature of its own.

**5. `TreeRoots` — option 2 of the two the sketch offered.** A package-private pair of
operations, "read the top level nodes out of the bound value" and "write them back", with two
implementations: `single` wraps and unwraps one root, `forest` is the identity on the tuple.
`TreePathLens<I,N,R> implements Lens<R,N>` and `PropertyTreeModel<I,N,R>` are then written once
for both forms, and the descent is uniform from the first id downwards, because the top level
is a tuple of siblings either way. `UIForTree<I,N,T>` is untouched: it holds the model as
`PropertyTreeModel<I,N,?>`, and every method it calls on it is independent of `R`.

The hazard the sketch named is respected — the update walk never goes through `TreeRoots`. A
single rooted tree's `applyNewValue` compares the two `R` values directly and hands them to
`_syncNode`, so nothing is allocated and reference identity still short-circuits everything the
change did not touch. Only the forest branch calls `TreeRoots.of(..)`, where it is the identity
on a tuple the property already holds.

**6. One change the sketch did not foresee: `TreeNodeRef.path()`.** It sized the `TreePath`
from the length of the id path, which is exact for a single rooted tree — a node's id path and
its chain of handles are the same length — and one short for every node under a forest handle,
which contributes a component to the path without contributing an id. A top level node would
have produced a `TreePath` of `[node]` rather than `[forest, node]`, which is not a path a
`JTree` can resolve. The handle now carries its depth explicitly and walks its parent chain.
The empty case matters too: `new TreePath(new Object[0])` throws, so the forest handle's own
path is `[forest]` — one component, exactly as a root handle's is.

## What this does not change ##

`UI.tree(..)` keeps its current signature, semantics and behaviour. Applications whose
data really does have one root — and many do — are unaffected, and their selection paths
still begin with the root's own id.

---

## Documentation, as it landed ##

- [`Growing-Trees.md`](../markdown/Growing-Trees.md) — a new section, *Several boxes on the
  van floor*, and a rewrite of the paragraph beginning *"`withRootVisible(false)` deserves a
  word"*, which used to present the wrapper idiom as the design.
- [`agent-skills/SKILL.md`](../agent-skills/SKILL.md) — the component table, the tree
  section and the cheat sheet.
- [`markdown/README.md`](../markdown/README.md) — one line, on the *Growing Trees* row.
- `Tree_Forest_Spec` — a living document of its own, *Growing a Forest from a Property*,
  rather than an appendix on each of the three single-root ones. The binding, the update walk,
  selection, editing and the cost story all read as one story for a forest, and splitting them
  across three documents would have left each with a lopsided tail. `Tree_Binding_Spec` keeps
  one scenario of its own for the expansion-depth change, since that is `tree(..)` behaviour.

# Growing Forests #

> **Status: design sketch.** Nothing described here is built. This is the plan for a
> follow-up iteration on top of the `UI.tree(..)` binding that landed in
> [Growing Trees](../markdown/Growing-Trees.md), written so that someone who did not
> take part in the discussion can execute it.

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

Today that has to be squeezed into the single-root form by inventing a node type which
is not part of the domain:

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

## The proposed API ##

A parallel family of factory methods named `trees(..)`, plural, because what is bound is
several top-level nodes and because the name then says at a glance which of the two
forms a call site is using.

```java
// identity from the HasId bound, node type from the tuple
static <I, N extends HasId<I>> UIForTree<I,N,UI.Tree> trees( Var<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );
static <I, N extends HasId<I>> UIForTree<I,N,UI.Tree> trees( Val<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );

// node types which cannot implement HasId: name the identity type, declare conf.idOf(..)
static <I, N> UIForTree<I,N,UI.Tree> trees( Class<I> idType, Var<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );
static <I, N> UIForTree<I,N,UI.Tree> trees( Class<I> idType, Val<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );

// a TreeConf you already hold
static <I, N> UIForTree<I,N,UI.Tree> trees( Var<Tuple<N>> roots, TreeConf<I,N> conf );
static <I, N> UIForTree<I,N,UI.Tree> trees( Val<Tuple<N>> roots, TreeConf<I,N> conf );
```

Note what is missing compared with `tree(..)`: there is no `Class<N> nodeType`
parameter in the common case. A `Tuple` carries its element type at runtime
(`Tuple.type()`), and it carries it even when empty, so the tree can ask the bound value
what its nodes are instead of being told.

That inference has one failure mode which needs an escape hatch: a tuple built as
`Tuple.of(Dir.class, ..)` and assigned to a `Var<Tuple<FsNode>>` reports `Dir` as its
element type, which would make the configuration reject `Doc` nodes below it. So keep
one explicit overload for the case where the tuple's element type is narrower than the
tree's node type:

```java
static <I, N> UIForTree<I,N,UI.Tree> trees( Class<N> nodeType, Class<I> idType, Var<Tuple<N>> roots, Configurator<TreeConf<I,N>> conf );
```

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
no node value, and several places currently assume that a handle always does. Each of
these needs a decision, and the middle column is what the code does today.

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

## Open decisions ##

**1. `withInitialExpansionDepth(..)` counts the invisible root.** It compares
`TreePath.getPathCount()`, which includes the root whether or not the root is drawn.
Measured against the current implementation:

```
visible root, depth 1:  root / src / README.md          <- the root opened
hidden  root, depth 1:  src / README.md                 <- nothing opened
hidden  root, depth 2:  src / deep / README.md          <- the top level opened
```

So the argument means one thing for a visible root and one less for a hidden one. Today
that is a wart on an uncommon configuration. Under `trees(..)` a hidden root is the only
configuration, so it becomes the default experience.

Recommendation: count depth from the first **visible** level, so `depth(1)` opens the
top-level nodes in both forms. This changes the existing hidden-root behaviour of
`tree(..)`, which is a small break, but the current behaviour is hard to defend as
intentional. The alternative — fixing it only for `trees(..)` — leaves two rules for one
method and should be rejected.

**2. Node type inference from `Tuple.type()`.** Recommended as the default, with the
explicit four-argument overload as the escape hatch. Worth confirming that
`Tuple.type()` on an *empty* tuple reports the declared element type in every
construction path sprouts offers, since a forest that starts empty is the ordinary case.

**3. Whether `trees(..)` should also accept `Vals<N>`** (a sprouts property *list*)
rather than only `Var<Tuple<N>>`. Probably not — the tuple form is the one the rest of
the immutable-value API is built on — but it should be a decision rather than an
omission.

---

## What this does not change ##

`UI.tree(..)` keeps its current signature, semantics and behaviour. Applications whose
data really does have one root — and many do — are unaffected, and their selection paths
still begin with the root's own id.

---

## Documentation to update when this lands ##

- [`Growing-Trees.md`](../markdown/Growing-Trees.md) — the section beginning
  *"`withRootVisible(false)` deserves a word"* currently presents the wrapper idiom as
  the design ("a bound tree always has exactly one root — and that root is very often a
  container nobody needs to see"). Once forests are first-class, that paragraph is
  describing a workaround as though it were intent.
- [`agent-skills/SKILL.md`](../agent-skills/SKILL.md) — the component table, the tree
  section's method list, and the cheat sheet all name `tree(..)` only.
- [`markdown/README.md`](../markdown/README.md) — no change needed unless this sketch
  grows into a wiki page of its own.
- New scenarios in `Tree_Binding_Spec`, `Tree_Update_Spec` and
  `Tree_Selection_And_Editing_Spec`, mirroring the single-root ones: a forest is bound,
  a top-level node is added and removed, a selection path has no synthetic first
  element, and an edit nine levels down still produces one new tuple value.

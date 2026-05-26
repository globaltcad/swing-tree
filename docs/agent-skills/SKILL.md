---
name: swingtree
description: >
  Write Swing desktop UIs with the SwingTree library and its companion property
  library Sprouts. Use this skill whenever you are building, editing, reviewing,
  or debugging Java Swing GUI code that imports `swingtree.UI` or `sprouts.*` —
  declarative component trees, MigLayout/responsive/reactive layouts, the
  functional style API, animations, and MVI/MVL or MVVM view models.
---

# Writing SwingTree Applications

SwingTree is a Java library for building **Swing** desktop GUIs **declaratively**,
the way Flutter / SwiftUI / Jetpack Compose / JetBrains' Kotlin UI DSL build
theirs. You describe the component tree with **method chaining + nesting**, bind
it to state with the **Sprouts** property library (`Var`/`Val`/`Vars`/`Tuple`),
and paint it with a **functional, immutable style API**. There is no XML, no FXML,
no separate template language — it is all plain Java, fully type-safe and
debuggable.

This document gives you the intuition to write *any* SwingTree app: the builder,
layout, properties & lenses, the two architecture patterns (MVI/MVL and classic
MVVM), events, styling, animation, tables, dialogs, and the non-obvious gotchas
that bite people. Read it top to bottom once; thereafter use the cheat sheet at
the end.

---

## 0. The one import and the mental model

```java
import swingtree.UI;
import static swingtree.UI.*;   // brings panel(), button(), FILL, WRAP, GROW, ...
```

A UI is a **tree of components**. Every node is built by a `UI.xxx(..)` **factory**
that returns a **builder** (`UIForPanel`, `UIForButton`, `UIForLabel`, … all
subtypes of `UIForAnySwing`). On a builder you:

- **configure** it with chained `withXyz(..)` / `isXyzIf(..)` calls,
- **nest children** with `.add(..)`,
- **bind** it to `Var`/`Val` properties for reactivity,
- **style** it with `.withStyle(it -> ...)`,
- **wire events** with `.onXyz(..)`,
- and finally **unwrap** it with `.get(JPanel.class)` or hand it to `UI.show(..)`.

Crucial idea: **a builder is a recipe, not the component.** It produces a real
`JComponent` underneath. You can always escape to the raw component with
`.peek(c -> ...)` or unwrap with `.get(Type.class)`.

The smallest complete program:

```java
import static swingtree.UI.*;

public static void main(String[] args) {
    UI.show(
        panel("wrap 1")
        .add(label("Welcome to SwingTree!"))
        .add(button("Click me").onClick(it -> System.out.println("clicked")))
    );
}
```

---

## 1. Growing the tree — factories, nesting, `add`

`UI.show(component | builder | title, builder | Function<JFrame,Component>)`
opens a window. Inside it you compose nodes:

```java
UI.show(
    panel("wrap 2")                              // a JPanel, MigLayout "wrap 2"
    .add(label("Name:"))
    .add("grow", textField("John"))              // first String arg = per-child layout constraint
    .add(label("Age:"))
    .add("grow", textField("42"))
    .add("span", separator())                    // span all columns, then wrap
    .add(button("Save"))
);
```

Rules of `.add(..)`:

- `.add(childBuilder)` — add with no constraint.
- `.add("growx, span 2", childBuilder)` — first arg is a **MigLayout add-constraint string**.
- `.add(GROW.and(SPAN), childBuilder)` — or a **type-safe constraint** (see §2).
- `.add(a, b, c)` — add several children at once (same constraint applies to each).

### Common factories (each returns a builder)

| Factory | Component |
|---|---|
| `panel(...)`, `box(...)` | `JPanel` / `JBox` (a transparent, insets-free panel — perfect for grouping) |
| `label(text)`, `html("<h1>..</h1>")` | `JLabel` (html(..) renders HTML) |
| `button(text)`, `toggleButton(text)`, `checkBox(text)`, `radioButton(text)` | buttons |
| `textField(text)`, `textArea(text)`, `passwordField()`, `numericTextField(var)` | text inputs |
| `comboBox(...)`, `slider(Align, min, max)`, `spinner(...)`, `progressBar(...)` | value pickers |
| `separator()`, `scrollPane()`, `scrollPanels()`, `splitPane(Align)`, `tabbedPane()` | structure |
| `table()`, `list(...)`, `menu(...)`, `menuItem(...)`, `splitButton(text)` | data / menus |
| `icon(path)`, `icon(w,h,path)` | `JIcon` (supports SVG, see §10) |

`box(...)` vs `panel(...)`: a `JBox` is non-opaque with zero default insets — use it
for invisible structural grouping; use `panel` when you want a real surface to
style. **Never call `setOpaque(..)` yourself on a styled component — the style
engine owns opacity and will fight you.**

### Wrapping a custom / third-party component

```java
.add( UI.of(new MyCustomJComponent()).onMouseClick(it -> ...) )
```

`UI.of(jcomponent)` wraps any `JComponent` so the declaration keeps flowing.
`UI.of(this)` is the standard way to start a `View extends JPanel` (see §5).

---

## 2. Layout — MigLayout, type-safe constants, responsive, reactive

SwingTree's default layout manager is **MigLayout**. You drive it three ways.

### 2a. String constraints (most common, terse)

The **container** constraint goes in the factory; **per-child** constraints go as
the first `add(..)` arg:

```java
panel("fill, wrap 3, insets 12, gap 8")   // container: fill space, 3 cols, 12px insets
.add("growx", a)
.add("span 2, growx", b)                   // this child spans 2 columns
.add("wrap", c)                            // force a new row after c
```

Memorize these MigLayout keywords:
- Container: `fill`, `fillx`, `filly`, `wrap N` (N columns), `insets T L B R` / `ins N`, `gap`, `debug` (draws guide borders — great for diagnosing layout).
- Per-child: `grow`, `growx`, `growy`, `push`, `pushx`, `pushy`, `span` / `span N`, `wrap`, `align center/left/right`, `top/bottom`, `width 60px::`, `w 180!`, `h 90!`.
- `60px::` means "min 60, no max"; `180!` means "exactly 180".

`withLayout("fill, wrap 2")` sets the container constraint after the fact, and
`withLayout(layout, colConstraints, rowConstraints)` gives full control, e.g.
`.withLayout("fill, wrap 2", "[grow 60][grow 40]")`.

Full keyword reference: http://www.miglayout.com/

### 2b. Type-safe constants (refactor-safe, composable)

`import static swingtree.UI.*` exposes constants that compose with `.and(..)`:

```java
of(this).withLayout(FILL.and(WRAP(1)).and(INS(16)))
.add(GROW.and(PUSH), child)
.add(CENTER.and(SPAN), html("<h2>Title</h2>"))
.add(RIGHT, button("OK"));
```

Container constants: `FILL`, `FILL_X`, `FILL_Y`, `WRAP(n)`, `INS(n)` / `INS(t,l,b,r)`, `GAP_REL(n)`, `FLOW_X`, `DEBUG`.
Per-child constants: `GROW`, `GROW_X`, `GROW_Y`, `PUSH`, `PUSH_X`, `PUSH_Y`, `SPAN`, `SPAN(n)`, `WRAP`, `SHRINK`, `CENTER`, `LEFT`, `RIGHT`, `TOP`, `BOTTOM`, `ALIGN_CENTER`, `ALIGN_LEFT`, `ALIGN_X_CENTER`, `ALIGN_Y_TOP`, `GAP_LEFT(n)`, …

String constraints and constants are interchangeable — pick whichever reads
clearer locally. (Examples in this codebase mix both freely.)

### 2c. Responsive flow layout (`ResponsiveGridFlowLayout`, Bootstrap-style 12 columns)

For layouts that adapt **as the container resizes**, use a flow layout plus
`AUTO_SPAN(..)` per child. Each child declares how many of 12 virtual columns it
occupies at each width category (`small`, `medium`, `large`, `veryLarge`,
`oversize`). The category is the container's current width vs. its preferred width.

```java
panel().withFlowLayout()
.add(AUTO_SPAN(it -> it.small(12).medium(6).large(3)), boxA)
.add(AUTO_SPAN(it -> it.small(12).medium(6).large(3)), boxB)
```

The `AUTO_SPAN` lambda re-runs on every resize, so the spans are genuinely dynamic.

### 2d. Reactive layout — bind the layout itself to a `Var<Layout>`

To swap the *entire layout manager* at runtime (compact ↔ tablet ↔ wide, edit ↔
read mode) **without destroying or rebuilding any child**, bind a panel to a
`Var<Layout>`:

```java
import swingtree.api.Layout;
import swingtree.layout.MigAddConstraint;

Var<Layout> layout = Var.of(Layout.class, Layout.mig("fill, wrap 1"));

panel(layout)                                  // == panel().withLayout(layout)
.add("growx", a).add("growx", b);

// later, anywhere — atomic reflow, no rebuild:
layout.set(Layout.mig("fill, wrap 2").withChildConstraints(
    MigAddConstraint.of("growx"),
    MigAddConstraint.of("growx, span 2")       // positional: index 0, 1, ...
));
```

`Layout` factories: `Layout.mig(constraints)`, `Layout.flow(FlowCell...)`,
`Layout.border()`, `Layout.grid(rows,cols)`, `Layout.box(UI.Axis.X)`,
`Layout.none()` (absolute positioning — `setLayout(null)`), `Layout.unspecific()`
(no-op, leaves current manager alone). `withChildConstraints(...)` maps
positionally to children. This is how `SalesDashboard` and `CelestialScribe`
work — see §5.4 for deriving a layout from data.

---

## 3. State — Sprouts properties (`Var`, `Val`) and binding

Reactivity comes from the **Sprouts** library. The whole point: **the view never
holds Swing state; it binds to properties, and the property system keeps the two
in sync bidirectionally.** Your business logic never imports a Swing class.

- `Var<T>` — a **mutable** property. `get()`, `set(value)`, `update(fn)`, `onChange(..)`.
- `Val<T>` — a **read-only** view of a property. `Var extends Val`, so you can
  expose `Val` from a view model to prevent the view from writing.
- `Vars<T>` / `Vals<T>` — observable **lists** of properties (classic MVVM).
- `Tuple<T>` — an **immutable** ordered collection (functional MVI/MVL).

```java
Var<String>  name  = Var.of("Joseph");
Var<Boolean> ok    = Var.of(true);
Var<Integer> count = Var.of(0);
Var<Layout>  lay   = Var.of(Layout.class, Layout.mig("fill"));  // explicit type when value could be null/ambiguous
```

### Binding properties to components

Pass the property to the factory and the binding is automatic and bidirectional:

```java
textField(name)                 // user typing -> name.set(..); name.set(..) -> field text
checkBox("Agree", ok)           // toggling <-> ok
slider(Align.HORIZONTAL, 0.0, 1.0, ratio)   // generic over Number: int OR double
comboBox(selectedEnum, e -> prettyLabel(e)) // selection <-> Var<MyEnum>
label(name)                     // one-way: label text follows name
progressBar(Align.HORIZONTAL, ratioVal)     // one-way Val<Double> 0..1
```

Flags bind through `isXyzIf(Val<Boolean>)`:

```java
textField(name).isEnabledIf(ok).isVisibleIf(showAdvanced)
button("Go").isEnabledIf(canSubmit)
checkBox("edit").isSelectedIf(...)  // and isEditableIf on text components
```

### Derived (computed) read-only views

`view*` methods produce a `Val` that recomputes when the source changes — perfect
for labels and computed flags:

```java
Val<String> caption = count.viewAsString(n -> "Items: " + n);
Val<Boolean> isEmpty = name.viewAs(Boolean.class, s -> s.isBlank());
Val<Double>  asD     = count.viewAsDouble(n -> n / 100.0);
label(caption);
```

`viewAsString/Int/Double()` with **no mapper** just stringify/convert the value;
the `nullObject`-first overloads (`viewAsString("", fn)`) define what to show when
the source is null — null-safe by construction. To derive from **two** sources at
once, combine them — the result recomputes when *either* input changes:

```java
Viewable<Double> total = Viewable.of(price, taxRate, (p, tr) -> p * (1 + tr));   // Val<Double>, updates live
```

> All `view*`/`viewAs*` results are `Viewable` (a `Val` you may listen on). They
> are held **weakly** by their source — see the GC gotcha in §9c: if you only
> register an `onChange` on one, keep it in a field or it is collected.

### The two change channels (`From.VIEW` vs `From.VIEW_MODEL`)

Every `Var` distinguishes who caused a change:

- `set(From.VIEW, v)` — the **user/view** changed it (SwingTree calls this for you when the user types/clicks).
- `set(From.VIEW_MODEL, v)` / plain `set(v)` — your **application logic** changed it.

Register listeners per channel via `Viewable.cast(prop).onChange(From.VIEW_MODEL, it -> ...)`
(or `From.VIEW`, or `From.ALL`). This split prevents infinite feedback loops and
lets you react only to user input or only to logic. Inside a listener,
`it.currentValue()` is the new value.

> **`prop.view()` vs `Viewable.cast(prop)`.** You cannot listen on a raw
> `Var`/`Val` directly — you need a `Viewable`. Two ways to get one, and the
> difference is lifecycle: `prop.view()` returns a **new, weakly-held** view
> (the sprouts-preferred default) — store it in a field so it isn't GC'd.
> `Viewable.cast(prop)` reinterprets the property *itself* as `Viewable`, so the
> listener lives exactly as long as that property object. Both are safe **only
> when the thing you listen on is reachable**: for a lens (which its parent holds
> *weakly*) you must keep the lens — or its `view()` — in a field either way (§9c).

```java
Viewable.cast(firstName).onChange(From.ALL, it ->
    fullName.set(it.currentValue().orElseThrowUnchecked() + " " + lastName.get())
);
```
**Warning:** The approach above can lead to memory leaks due to change listeners
never being garbage collected and still holding strong references to captured variables.

→ So the prefer custom change listener registration on views instead of directly!

---

## 4. Lenses — `zoomTo` and immutable view models

This is the heart of the **recommended** SwingTree architecture (MVI/MVL). A
**lens** focuses a root `Var<BigImmutableRecord>` down onto one field, giving you
a `Var<Field>` that reads via a getter and writes via a **wither** (a method that
returns a *new* record with that field changed).

```java
record Person(String forename, String surname, Address address) {
    Person withForename(String f){ return new Person(f, surname, address); }
    Person withSurname(String s){ return new Person(forename, s, address); }
    Person withAddress(Address a){ return new Person(forename, surname, a); }
}

Var<Person>  person   = Var.of(new Person("Tom","Schultz", addr));
Var<String>  forename = person.zoomTo(Person::forename, Person::withForename);
Var<Address> address  = person.zoomTo(Person::address,  Person::withAddress);
Var<String>  street   = address.zoomTo(Address::street, Address::withStreet);  // lenses nest!
```

Now `textField(forename)` edits the forename, and a keystroke produces a brand-new
`Person` (and `Team`, etc., all the way up) inside `person`. Lenses are **smart**:
they fire change events only when *their own slice* actually changes, even if the
whole root record was replaced.

Other lens flavors:
- `viewAs(Type.class, getter)` / `viewAsString/Double/Int(getter)` — **read-only** derived `Val`.
- `zoomToNullable(Type.class, getter, wither)` — when the focused value may be null.
- `zoomTo(defaultValue, getter, wither)` — supply a fallback for null parents.
- `zoomTo(Lens<S,T>)` — a hand-written lens (implement `Lens.getter`/`wither`, or
  `Lens.of(getter, wither)`) when the focus needs **logic** — clamping, derived
  fields, or zooming into a collection entry (see below).

**Tip:** Generate withers with Lombok `@With` on records to avoid boilerplate
(this is also how you stay on **Java 8** — records need 16+, but `@With @Getter`
on a `final class` gives the same value semantics):
```java
@With record Person(String forename, String surname, Address address) {}
// person.zoomTo(Person::forename, Person::withForename)  // withForename generated by @With
```

### Sprouts immutable collections — `Tuple`, `Association`, `ValueSet`, `Pair`

Records model *fixed* shape; for *variable-size* state inside a view model, use
Sprouts' **persistent** (structural-sharing) collections instead of
`java.util` — they are immutable value objects, so they fit record fields and
withers, and SwingTree binds to several of them directly. Every "mutation"
returns a **new** instance.

| Type | `java.util` analogue | Make it | Key ops (all return a new instance) |
|---|---|---|---|
| `Tuple<T>` | `List<T>` | `Tuple.of(a,b,c)`, `Tuple.of(T.class)` (empty), `Tuple.of(T.class, iterable)` | `add`, `remove`, `removeAt`, `setAt(i,x)`, `map`, `retainIf`/`removeIf`, `slice`, `sort`, `first`/`last` |
| `Association<K,V>` | `Map<K,V>` | `Association.between(K.class, V.class)` (empty!), `.ofLinked(..)` (insertion-ordered) | `put`, `putAll(Pair...)`, `get(k) → Optional`, `remove`, `removeIf(pair->..)` |
| `ValueSet<E>` | `Set<E>` | `ValueSet.of(E.class)`, `ValueSet.of(a,b,..)`, `.ofLinked(..)` | `add`, `addAll`, `remove`, `retainAll`, `retainIf`, `any(pred)` |
| `Pair<A,B>` | `Map.Entry` | `Pair.of(a, b)` | `.first()`, `.second()` |

> ⚠️ The empty-map factory is **`Association.between(K.class, V.class)`**, *not*
> `Association.of(..)` — `of(key, value)` builds a one-entry map (and
> `of(String.class, Integer.class)` would silently make an `Association<Class,Class>`).

A field of one of these *is* part of the immutable value, so it composes with
lenses and withers like any other field:

```java
@With record PartyPlan(
    Tuple<Guest>                 guests,       // ordered, may repeat
    Association<String,Integer>  drinkStock,   // name -> quantity
    ValueSet<String>             decorations   // unique, unordered
) {}

Var<PartyPlan>                    plan  = Var.of(initialPlan);
Var<Tuple<Guest>>                 guests = plan.zoomTo(PartyPlan::guests, PartyPlan::withGuests);
Var<Association<String,Integer>>  stock  = plan.zoomTo(PartyPlan::drinkStock, PartyPlan::withDrinkStock);

guests.update(g -> g.add(new Guest("Gimli")));      // immutable add, fires change
stock.update(s -> s.put("Ale", 12));                // immutable put
```

You can even **lens into a single entry** of a collection with logic lenses —
the write rebuilds the whole collection immutably, but the property behaves like
a plain `Var<V>` (great for binding one map value to one field):

```java
Var<Integer> aleStock = stock.zoomTo(
    s -> s.get("Ale").orElse(0),                    // getter: read the entry
    (s, qty) -> s.put("Ale", qty)                   // wither: return a new map
);
aleStock.set(20);   // updates the entire association inside `plan`
```

`Tuple` is the one most wired into SwingTree: `addAll(..)` renders one sub-view
per element (§5.2), and `Var<Tuple<Item>>` is the canonical MVI list.

---

## 5. Architecture — how to structure a real app

A SwingTree **view** is conventionally a `class extends JPanel` whose constructor
takes the view model (or a `Var` of it) and builds itself with `UI.of(this)`:

```java
public final class MyView extends JPanel {
    public MyView(Var<MyViewModel> vm) {
        UI.of(this).withLayout("fill, wrap 1")
        .add(...)
        .add(...);
    }
    public static void main(String[] args) {
        Var<MyViewModel> vm = Var.of(new MyViewModel());
        UI.show(f -> new MyView(vm));
        EventProcessor.DECOUPLED.join();   // keep the app thread alive (see §11)
    }
}
```

Pull repeated fragments into `private static UIForAnySwing<?,?> someSection(...)`
methods that return builders — this is the standard way large views (TeamView,
BreathingView, CelestialScribe) stay readable.

### 5.1 MVI / MVL — the recommended pattern (immutable records + lenses)

The whole UI state lives in **one immutable record** (the view model). The view is
a pure function of it; every change produces a new record via withers; the view
reaches fields through `zoomTo`. There are no Swing references and no mutable
fields in the view model — it is unit-testable in isolation.

**View model** (note: `static empty()` / no-arg constructor for the initial state,
withers for every field, and *business methods* that return new instances):

```java
public record CalculatorViewModel(CalculatorInputs inputs, CalculatorOutput output) {
    public static CalculatorViewModel empty(){ return new CalculatorViewModel(CalculatorInputs.empty(), CalculatorOutput.empty()); }
    public CalculatorViewModel withInputs(CalculatorInputs i){ return new CalculatorViewModel(i, output); }
    public CalculatorViewModel withOutput(CalculatorOutput o){ return new CalculatorViewModel(inputs, o); }
    public CalculatorViewModel runCalculation(){            // business logic = pure function returning new VM
        try {
            double l = Double.parseDouble(inputs.left()), r = Double.parseDouble(inputs.right());
            double res = switch (inputs.operator()) {
                case ADD -> l+r; case SUBTRACT -> l-r; case MULTIPLY -> l*r; case DIVIDE -> l/r;
            };
            return withOutput(output.withResult(res).withValid(true));
        } catch (NumberFormatException e) { return withOutput(output.withError("Invalid number").withValid(false)); }
    }
}
```

For business logic that can **fail** (parsing, validation, IO), Sprouts'
`Result<T>` is a cleaner alternative to ad-hoc error fields: it is a `Maybe<T>`
(present-or-empty, like `Optional`) that *also* carries a `Tuple<Problem>`
describing what went wrong. `Result.ofTry(T.class, () -> risky())` runs a
throwing supplier and captures any exception as a `Problem` instead of
propagating it — ideal inside a pure view-model method. The view then renders
`result.problems()` (e.g. an error label) and `result.orElse(fallback)` for the
value. (SwingTree itself returns `Result` from table-cell conversions.)

**View** zooms in and triggers business methods with `vm.set(vm.get().runCalculation())`
or, more idiomatically, `vm.update(CalculatorViewModel::runCalculation)`:

```java
public final class CalculatorView extends JPanel {
    public CalculatorView(Var<CalculatorViewModel> vm) {
        Var<CalculatorInputs> inputs = vm.zoomTo(CalculatorViewModel::inputs, CalculatorViewModel::withInputs);
        Var<CalculatorOutput> output = vm.zoomTo(CalculatorViewModel::output, CalculatorViewModel::withOutput);
        UI.of(this).withLayout("fill")
        .add("growx", textField(inputs.zoomTo(CalculatorInputs::left, CalculatorInputs::withLeft)))
        .add(comboBox(inputs.zoomTo(CalculatorInputs::operator, CalculatorInputs::withOperator), Operator::symbol))
        .add("growx", textField(inputs.zoomTo(CalculatorInputs::right, CalculatorInputs::withRight)))
        .add("wrap", button("Run!").onClick(e -> vm.update(CalculatorViewModel::runCalculation)))
        .add("span", label(output.viewAsString(o -> o.valid() ? "= " + o.result() : o.error())));
    }
}
```

`vm.update(fn)` is shorthand for `vm.set(fn.apply(vm.get()))` — prefer it for
applying a business method.

### 5.2 Lists in MVI/MVL — `Tuple` + `addAll` + `HasId`

Model a collection as a `Tuple<T>` field; zoom to it; render with `addAll`:

```java
record ChatVM(Tuple<Message> allMessages, String draft) {
    record Message(UUID id, String text, LocalDateTime sentAt, boolean editing) implements HasId<UUID> {
        Message(){ this(UUID.randomUUID(), "", LocalDateTime.now(), false); }
    }
}

Var<Tuple<Message>> messages = vm.zoomTo(ChatVM::allMessages, ChatVM::withAllMessages);

scrollPanels()
.addAll(messages, (Var<Message> entry) -> {              // one sub-view per item; entry is a per-item lens
    Var<String> text = entry.zoomTo(Message::text, Message::withText);
    return panel(FILL)
        .add(GROW_X.and(WRAP), textArea(text))
        .add(RIGHT, button("✕").onClick(it -> messages.update(t -> t.remove(entry))));
});

// add an item:
messages.update(t -> t.add(new Message().withText(draft.get())));
```

> **CRITICAL: when you bind a *mutable* `Var<Tuple<M>>` and want a per-item lens,
> the item type MUST implement `sprouts.HasId<IdType>`** (carry a `UUID`/stable
> id). That overload — `addAll(Var<Tuple<M>>, entry -> ...)`, where `entry` is a
> `Var<M>` lens — is the one above, and it is `<M extends HasId<?>>`. Value
> records define identity by *content*, so two equal records would confuse the
> component binding; `HasId.id()` gives each item a stable identity so SwingTree
> knows which sub-view maps to which item, which item-lens to hand it, and which
> rows to reuse vs. rebuild on change. Add a `UUID id` field and `implements
> HasId<UUID>`.
>
> The **read-only** overloads do *not* require `HasId`: `addAll(Val<Tuple<M>>,
> m -> view)` and `addAll(Tuple<M>, m -> view)` (and the `Vals<M>` MVVM overload)
> hand the supplier the **value** `M`, not a lens — use these when items aren't
> individually editable. `HasId` is the price of admission for per-item editing.

`Tuple` is functional: `.add(x)`, `.remove(x)`, `.map(fn)`, `.setAt(i, x)`,
`.get(i)`, `.size()`, `.isEmpty()` — all return new tuples (or values).
`Tuple.of(Message.class)` makes an empty typed tuple; `Tuple.of(a, b, c)` a
populated one.

### 5.3 Classic MVVM — mutable view models (the alternative)

If you prefer mutable view models: the view model holds `Var<X>` *fields* directly
(no root record, no lenses), exposes them through getters, and uses `Vars<T>` for
observable lists. The view binds straight to those fields.

```java
public class PersonVM {
    private final Var<String> firstName = Var.of("Joseph");
    private final Var<String> lastName  = Var.of("Armstrong");
    private final Var<String> fullName  = Var.of("");
    public PersonVM() {
        Viewable.cast(firstName).onChange(From.ALL, it -> recompute());
        Viewable.cast(lastName ).onChange(From.ALL, it -> recompute());
        recompute();
    }
    private void recompute(){ fullName.set(firstName.get() + " " + lastName.get()); }
    public Var<String> firstName(){ return firstName; }   // mutable out
    public Var<String> lastName(){ return lastName; }
    public Val<String> fullName(){ return fullName; }     // read-only out
}
```

**Polymorphic / dynamic sub-views** work in both patterns via the property-bound
`add` overload — when the property changes, SwingTree swaps the sub-view:

```java
// MVVM: Var<Object> subVM, view supplier dispatches on type
.add(vm.subViewModel(), subVM ->
    subVM instanceof SubVM1 s ? new SubView1(s) : new SubView2((SubVM2) subVM))

// MVI: Val<Boolean> + supplier picks which fragment to (re)build
.add("grow, push", hasSelection, has -> has ? editorBody(vm) : emptyState())
```

A `Vars<T>` (MVVM) and a `Var<Tuple<T>>` (MVI) are both rendered with
`addAll(list, viewSupplier)`. **TeamView exists in the SwingTree repo in both flavors**
(`examples.team.mvi` and `examples.team.mvvm`) — the clearest side-by-side
contrast. Choose **MVI/MVL for new code**; reach for MVVM only when integrating
with existing mutable models.

### 5.4 Deriving a layout from data (advanced reactive)

`CelestialScribe` derives the entire child layout from a tuple of model objects —
positions are a pure function of state, so dragging a star just updates the model:

```java
Val<Layout> layout = stars.viewAs(Layout.class, tuple -> {
    Layout.None none = Layout.none();
    for (int i = 0; i < tuple.size(); i++)
        none = none.withChildBound(i, tuple.get(i).bounds());
    return none;
});
box().withLayout(layout).withRepaintOn(stars).addAll(stars, this::starPanel);
```

---

## 6. Events

Every component supports the same base events; the handler receives a delegate
(conventionally `it`) that wraps **both the component and the event state** and
offers query/animation helpers.

```java
button("Go")
.onClick(it -> doThing())                      // also: onClick(Runnable) for no-arg
.onMouseClick(it -> ...).onMousePress(it -> ...).onMouseRelease(it -> ...)
.onMouseEnter(it -> ...).onMouseExit(it -> ...).onMouseMove(it -> ...).onMouseDrag(it -> ...)
.onFocusGain(it -> ...).onFocusLoss(it -> ...)
.onKeyPress(it -> ...).onKeyRelease(it -> ...).onKeyTyped(it -> ...)
.onResize(it -> ...).onShown(it -> ...).onHidden(it -> ...);
```

Useful delegate methods: `it.get()` / `it.getComponent()` (the component),
`it.getParent()`, `it.mouseX()` / `it.mouseY()`, `it.animateFor(..)` (§9),
`it.paint(status, g -> ...)` (custom rendering), drag deltas
(`it.deltaXSinceStart()`, `it.initialComponentPosition()`). **All geometry these
return is in DPI-agnostic "developer pixels"** (except `mouse*OnScreen()`, which
is raw screen pixels) — see §13.

### Custom / model-driven events: `on(..)` vs `onView(..)`

Both attach an `Action` to any `sprouts.Observable` (e.g. an `Event` from
`Event.create()`, or a property). The difference is **which thread runs the
handler**:

| Method | Handler runs on | Use for |
|---|---|---|
| `onView(observable, it -> ...)` | **EDT** (Swing thread) | reacting to model changes that **touch the view** — resize a label, animate a colour |
| `on(observable, it -> ...)` | **application thread** | reacting to external/business events that **update your model** — network, custom input |

Rule: if your handler sets Swing properties → `onView`; if it mutates the view
model or does non-UI work → `on`.

---

## 7. Styling — the functional `withStyle` API

`.withStyle(it -> it. ... )` receives a `ComponentStyleDelegate` (`it`) and returns
a configured one. It is **immutable and re-run on every paint**, so styles can
depend on live state (selection, animation progress, model fields). This is how
SwingTree paints shadows, gradients, rounded borders, etc. *on top of* the current
Look-and-Feel — things plain Swing cannot do.

```java
panel("fill")
.withStyle(it -> it
    .margin(8).padding(24)
    .backgroundColor(new Color(57,221,255))
    .foregroundColor(Color.WHITE)
    .borderRadius(32)
    .border(2, Color.DARK_GRAY)                 // width + color
    .borderAt(Edge.LEFT, 5, accent)             // one edge only (great for accent bars)
    .shadowColor(new Color(0,0,0,128)).shadowBlurRadius(5).shadowSpreadRadius(1).shadowOffset(0,2)
    .shadowIsInset(false)
);
```

Frequently used delegate methods (all chainable, all DPI/HiDPI aware):

- Box: `margin`, `padding`, `borderRadius`, `borderRadiusAt(Corner, w, h)`, `border`, `borderAt(Edge, w, color)`, `prefSize`, `size`.
- Fill: `backgroundColor` / `foundationColor`, `foregroundColor`, `gradient(...)`, `noise(...)`, `image(img -> ...)`.
- Shadow: `shadowColor`, `shadowBlurRadius`, `shadowSpreadRadius`, `shadowOffset`, `shadowIsInset`. Named shadows: `.shadow("name", s -> s.color(..).offset(..))`.
- Layered painting: `.painter(Layer.CONTENT, g -> ...)` for raw `Graphics2D`.
- `component()` returns the live component, so you can branch on its state (e.g. `it.component().isSelected()`). **Deprecated for reading geometry** — its sizes are in *component pixels* and double-scale if fed back in; use `componentWidth/Height()` / `componentPrefWidth/Height()` instead (§13).

Gradients and named layers:

```java
.gradient(Layer.BACKGROUND, "glow", g -> g
    .type(GradientType.RADIAL)                  // or LINEAR
    .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
    .span(Span.TOP_LEFT_TO_BOTTOM_RIGHT)
    .offset(cx, cy).size(radius)
    .colors(color(0.75,1,0.5,0.5), color(0.5,1,1,0))   // UI.color(r,g,b[,a]) -> UI.Color
    .clipTo(ComponentArea.BODY)
)
```

`UI.Color` (via `color(...)`, `Color.ofRgb(...)`, `Color.ofHsb(...)`) adds
`.blend(other, t)`, `.shade(amount)`, `.brighter()`, alpha helpers — handy for
deriving palettes.

### Font styling (`componentFont`)

```java
.withStyle(it -> it.componentFont(f -> f
    .size(32).family("Arial").weight(2f).color(Color.WHITE).posture(0.1f).spacing(0.12f)
    .gradient(grad -> grad.colors(Color.GREEN, Color.BLUE).span(UI.Span.LEFT_TO_RIGHT))
    .noise(n -> n.colors(Color.DARK_GRAY, Color.CYAN).function(UI.NoiseType.CELLS).scale(1.25))
))
```

There are also `.withFontSize(n)`, `.withForeground(color)`, `.withBackground(color)`
shortcuts directly on the builder for simple cases.

### Background filtering (frosted glass)

A non-opaque child can blur/scale the parent's pixels behind it:

```java
.withStyle(it -> it
    .backgroundColor(Color.TRANSPARENT)         // must be non-opaque for the filter to show
    .parentFilter(f -> f.area(ComponentArea.BODY).blur(16).scale(1.25, 1.25))
)
```

### Central style sheets + semantic groups (CSS-like, hot-swappable themes)

For app-wide styling, pull rules into a `StyleSheet` and tag components with
`.group(EnumTag)` / `.id("name")`. This is how the **Theme Garden** swaps five
complete themes at runtime with zero changes to the view skeleton.

```java
enum Skin { PRIMARY, SECONDARY }

final class MySheet extends StyleSheet {
    @Override protected void configure() {
        add(type(JButton.class),                it -> it.borderRadius(8).padding(6,14,6,14));
        add(type(JButton.class).group(Skin.PRIMARY), it -> it.backgroundColor(BLUE).foregroundColor(WHITE));
        add(id("ok-button"),                    it -> it.shadowBlurRadius(8));
    }
}
```

Traits: `id("x")` (most specific), `group(tag)` (prefer **enum** tags over
strings — type-safe), `type(Class)`. They compose:
`type(JButton.class).group(Skin.PRIMARY)`. Specificity: `id` > `type+group` >
`group` > `type`; later `add(..)` wins ties.

Install a sheet either globally —
`SwingTree.initializeUsing(cfg -> cfg.styleSheet(new MySheet()))` — or for a scope:

```java
UI.use(new MySheet(), () -> UI.show(f -> new MyView()));   // only components built INSIDE the lambda bind
```

**Hot-swap themes**: keep mutable state in the sheet and call `reconfigure()` to
re-run `configure()` and instantly repaint every component in the `UI.use` scope:

```java
final class ThemedSheet extends StyleSheet {
    private Theme theme = Theme.LIGHT;
    public void setTheme(Theme t){ if (t != theme){ theme = t; reconfigure(); } }
    @Override protected void configure(){ switch (theme){ case LIGHT -> light(); case DARK -> dark(); } }
}
// in the view: bind a Var<Theme> to the sheet
Viewable.cast(theme).onChange(From.ALL, it -> sheet.setTheme(theme.get()));
UI.use(sheet, () -> of(this).group(Skin.FRAME). ... .add(comboBox(theme)));
```

---

## 8. Repainting on state — `withRepaintOn`

Because `withStyle` reads live state, you must tell SwingTree **when** to repaint a
styled component whose appearance depends on a property that the component isn't
otherwise bound to:

```java
box().withRepaintOn(orbScale, phaseProgress)   // repaint whenever either Val changes
.withStyle(it -> it.shadowBlurRadius((int)(16 + 78 * orbScale.get())). ...)
```

Without `withRepaintOn`, a style that reads `someVal.get()` won't refresh when that
val changes (unless the value also drives a binding like `label(..)`). This is the
standard pattern for model-driven styling and animation.

---

## 9. Animation

Animations are timer-driven lambdas invoked ~60×/s on the EDT. Two levels:

### 9a. View-side, fire-and-forget (`it.animateFor` / `UI.animateFor`)

```java
button("hover me")
.onMouseEnter(it -> it.animateFor(0.5, TimeUnit.SECONDS, status -> {
    double h = 1 - status.progress() * 0.5;
    it.setBackgroundColor(h, 1, h);
}));
```

The `AnimationStatus status` gives you `progress()` (0→1), `fadeIn()`, `fadeOut()`,
`pulse()`, `cycle()`. Drive *anything* from it: colors, bounds (`setBounds`),
text, or custom rendering via `it.paint(status, g -> ...)`:

```java
.onMouseClick(it -> it.animateFor(1.2, TimeUnit.SECONDS, s -> it.paint(s, g -> {
    g.setColor(new Color(120,176,238,(int)(200*s.fadeOut())));
    for (int i=0;i<5;i++){ double r=280*s.fadeIn()*(1-i*0.18);
        g.drawOval((int)(it.mouseX()-r/2),(int)(it.mouseY()-r/2),(int)r,(int)r); }
})));
```

`UI.animateFor(dur, unit).go(s -> someVar.set(s.progress()))` runs an animation not
tied to an event; `.asLongAs(s -> true).go(...)` loops forever (ambient effects).
A common idiom: animate a `Var<Double>` and let `withRepaintOn` + `withStyle`
render the frames.

### 9b. View-side transition between two states (`withTransitionalStyle`)

Given a `Var<Boolean>` and a duration, SwingTree interpolates `progress` 0↔1 every
time the flag flips. Multiply style props by `state.progress()`:

```java
label("toggle me")
.withTransitionalStyle(isOn, LifeTime.of(2, TimeUnit.SECONDS), (state, it) -> it
    .borderRadius(38 * state.progress())
    .backgroundColor(200/255d, 210/255d, 220/255d, state.progress())
    .shadowBlurRadius(10 * state.progress())
);
// elsewhere: toggleButton("toggle").onClick(it -> isOn.set(it.get().isSelected()));
```

### 9c. Modelled animation (MVI-friendly — state lives in the view model)

For testable, multi-phase animation, the view model exposes an `Animatable` (a pure
function of `AnimationStatus` → new model). The view hands it to `UI.animate(vm, vm::xxx)`
and **re-arms** the next phase by listening for the model's phase change.

```java
// view model
public Animatable<BreathingViewModel> breathAnimation() {
    BreathPhase ph = this.phase; double secs = settings.secondsFor(ph);
    return Animatable.of(LifeTime.of(secs, TimeUnit.SECONDS), this,
        new AnimationTransformation<>() {
            public BreathingViewModel run(AnimationStatus s, BreathingViewModel m){   // pure, every frame
                return m.withPhase(ph).withPhaseProgress(s.progress()).withOrbScale(ph.scaleAt(s));
            }
            public BreathingViewModel finish(AnimationStatus s, BreathingViewModel m){ // once, at end
                return m.advancePhase();
            }
        });
}

// view: chain phases by re-arming on phase change
Viewable.cast(phase).onChange(From.VIEW_MODEL, it -> {
    if (vm.get().running()) UI.animate(vm, BreathingViewModel::breathAnimation);
});
// start it:
button.onClick(it -> { vm.update(BreathingViewModel::begin); UI.animate(vm, BreathingViewModel::breathAnimation); });
```

> **GC GOTCHA (this WILL bite you):** Sprouts lenses/views observe their parent
> **weakly**. SwingTree's own bindings (`label`, `slider`, `withRepaintOn`, …)
> hold a strong ref internally, so lenses you pass *to them* are safe as locals.
> But a lens consumed **only** by a raw `Viewable.cast(lens).onChange(..)`
> subscription (like the `phase` re-arming lens above) is **not** retained — it
> gets garbage-collected and the animation silently freezes after one phase.
> **Fix: keep that lens as a `private final` field of the view.** (See the
> `BreathingView.phase` field and its Javadoc.)

---

## 10. Tables, lists, icons, dialogs

### Tables — lambda-defined model, no `TableModel` subclass

```java
UI.table().withModel(m -> m
    .colName(i -> headers[i]).colCount(() -> headers.length).rowCount(() -> data.length)
    .getsEntryAt((r,c) -> data[r][c])
    .setsEntryAt((r,c,val) -> data[r][c] = (int) val)
    .isEditableIf(() -> true)
    .updateOn(dataChangedEvent)        // an Event.create(); call dataChangedEvent.fire() to refresh
);
```
Custom cell rendering: `.withCell(cell -> cell.view(c -> c.orGetUi(() -> textField()).updateIf(JTextField.class, tf -> { tf.setText(cell.entryAsString()); return tf; })))`.

### SVG / icons

`icon("path.svg")`, `findIcon("path")` (returns `Optional<Icon>`, classpath + cache,
SVG-aware), `SvgIcon.of(svgString).withFitComponent(..).withPreferredPlacement(..)`,
and in the style API: `.image(img -> img.svg(svgText).fitMode(..).placement(..))`.

### Dialogs (`JOptionPane` wrappers)

```java
ConfirmAnswer a = UI.confirmation("Continue?").titled("Confirm").show();   // YES/NO/CANCEL/CLOSE
UI.confirmation("Heads up!").showAsWarning();   // .showAsError() .showAsInfo()
UI.message("Saved.").showAsInfo();              // no return value
// customize buttons: .yesOption("OK").noOption("").cancelOption("")  (empty hides a button)
```

---

## 11. Threading & lifecycle

- SwingTree binding/animation callbacks run on the **EDT**. Business logic that you
  trigger via `on(..)` runs on the **application thread**.
- `UI.run(r)` runs on EDT now; `UI.runLater(r)` / `runLater(delay, r)` defer to EDT.
- In a `main`, after `UI.show(...)`, call `EventProcessor.DECOUPLED.join()` to keep
  the (decoupled) application thread alive so the program doesn't exit.
- Set a Look-and-Feel before showing if desired (examples use FlatLaf:
  `FlatDarkLaf.setup();` / `FlatLightLaf.setup();`).

---

## 12. Escape hatches & error containment

SwingTree wraps **every lambda it invokes for you** in try/catch + SLF4J logging,
so a thrown exception in one fragment doesn't tear down the whole UI ("the show
must go on"). Caught: `peek`, `apply`, `applyIf`, `applyIfPresent`, `withStyle`,
all `onXyz` handlers, and `zoomTo` map/wither lambdas. **NOT** caught: code at the
top level of your declaration (your own `for`/`if`/arithmetic *outside* a captured
lambda) — push risky top-level code into `apply(ui -> ...)` or `peek(c -> ...)`.

| Hatch | Use |
|---|---|
| `.peek(c -> ...)` | grab the raw Swing component for an imperative tweak |
| `.apply(ui -> ...)` | imperative loop that `add(..)`s many children (the lambda gets the builder) |
| `.applyIf(boolean, ui -> ...)` | inline conditional sub-tree (static shape decisions) |
| `.applyIfPresent(Optional<Consumer<I>>)` | inline `Optional`-driven sub-tree |
| `.get(JPanel.class)` | unwrap the builder to the real component |
| `UI.of(jcomponent)` | wrap a hand-rolled/3rd-party component into the tree |

> **Prefer reactivity over hatches.** If a condition depends on app state, bind it
> (`isVisibleIf`, `isEnabledIf`, property-bound `add`) instead of `applyIf`, so the
> UI updates automatically. The hatches are for *construction-time* decisions.

---

## 13. HiDPI scaling — "developer pixels" vs "component pixels"

SwingTree maintains one **UI scale factor** (`UI.scale()`, a `float`, derived
from the system font) and applies it everywhere, because vanilla Swing + the
JDK's bundled Look-and-Feels do **not** scale for HiDPI. This creates two
coordinate spaces:

- **Developer pixels** — the DPI-agnostic numbers *you* write (`withPrefSize(100,50)`).
- **Component pixels** — the real scaled numbers Swing lays out/paints (at scale `2.0` → `200×100`).

**The symmetry you can rely on:** everything you pass *into* the SwingTree API is
in developer pixels and gets scaled **up** for you; everything SwingTree reads
*back* for you is scaled **down** into developer pixels. So values round-trip
cleanly — you almost never call `UI.scale(..)` yourself.

- **Inputs scaled up:** all builder dims (`withPrefSize/withMinSize/withWidth/withSizeExactly/...`)
  and all style dims (`prefSize`, `minHeight`, `margin`, `padding`, `borderWidth`,
  `borderRadius`, gradient/shadow offsets & sizes, …).
- **Outputs scaled down (already in developer px):**
  - Style delegate: `it.componentWidth()`, `it.componentHeight()`,
    `it.componentPrefWidth()`, `it.componentPrefHeight()`.
  - Event delegates (`onClick`, `onResize`, `onMouseMove`, `onDrag`, …):
    `it.getX/getY/getPosition`, `it.getWidth/getHeight/getSize`, `it.getPrefSize`,
    `it.getBounds`; setters like `it.setBounds/setPrefSize/setMinSize` take
    developer px. Mouse: `it.mouseX()/mouseY()/mousePosition()`. Drag:
    `it.initialComponentPosition()`, `it.dragPositions()`, `it.deltaXSinceStart()`.

> **THE DOUBLE-SCALING TRAP (this is why `component()` is deprecated):** the raw
> Swing component returns **component pixels**. If you read
> `it.component().getPreferredSize().height` (already scaled) and pass it back
> into a scaling method like `minHeight(..)`, it is scaled **twice** — min height
> becomes `200` when you meant `100`, and the error grows with the scale factor.
> **Fix:** use the developer-pixel accessor instead:
> ```java
> .withStyle( it -> it.minHeight(it.componentPrefHeight()) )   // ✅ round-trips; NOT it.component().getPreferredSize().height ❌
> ```

> **THE ONE EXCEPTION:** absolute on-screen coords are **raw**, not unscaled —
> `it.mouseXOnScreen()`, `it.mouseYOnScreen()`, `it.mousePositionOnScreen()` are
> in real screen pixels (they're desktop-absolute, possibly multi-monitor).

Only call the raw helpers when working **against raw Swing** (custom `Graphics2D`
painting, a peeked component, a third-party widget): `UI.scale(int|float|double)`
(developer→component), `UI.unscale(int|float|Dimension)`
(component→developer), `UI.scale(Graphics2D)` (scales a context in place),
`UI.scale()` (the raw factor). Override the factor with
`SwingTree.get().setUiScaleFactor(2.0f)` or
`SwingTree.initializeUsing(cfg -> cfg.uiScaleFactor(2.0f))`. Full prose:
`docs/markdown/HiDPI-Scaling.md`.

## 14. Hard-won gotchas (check these in any review)

1. **Never `setOpaque(..)`** on a styled component — the style engine controls
   opacity; manual calls fight it. Use `backgroundColor(Color.TRANSPARENT)` / a real
   color in `withStyle` instead.
2. **`Tuple` items bound for *per-item editing* (`addAll(Var<Tuple<M>>, entry -> ..)`,
   where `entry` is a `Var<M>` lens) must `implement HasId<UUID>`** with a stable id —
   otherwise equal value-records collide and bindings target the wrong sub-view. The
   read-only `addAll(Val<Tuple<M>>/Tuple<M>, m -> ..)` overloads pass the value and
   need no `HasId` (§5.2).
3. **Hold a strong reference (a view field) to any lens used only by a raw
   `onChange` subscription** — weak observation will GC it and silently break (§9c).
4. **Add `withRepaintOn(props...)`** to styled components whose `withStyle` reads
   property values that aren't otherwise bound on that component.
5. **Pick the right thread:** `onView` for view-touching handlers, `on` for
   model/business handlers; respect `From.VIEW` vs `From.VIEW_MODEL` to avoid
   feedback loops.
6. **View models import zero Swing classes.** If you find a `JComponent` in a view
   model, the architecture is wrong.
7. Expose **`Val`** (not `Var`) from a view model for fields the view must not write.
8. Use **enum** group tags and the type-safe layout constants for refactor safety.
9. Withers must be **pure** and return **new** instances (Lombok `@With` on records
   is the cleanest path); never mutate `this`.
10. **Never feed a raw Swing size/position back into the SwingTree API** — values
    from `it.component().getPreferredSize()`/`getBounds()`/`getWidth()` are in
    *component pixels* (already scaled); passing them to `minHeight(..)`/`size(..)`/etc.
    double-scales them. Read geometry through the delegate accessors
    (`componentPrefHeight()`, `getWidth()`, `mouseX()`, …) which give developer pixels. (§13)

---

## 15. Cheat sheet

```java
import static swingtree.UI.*;
import sprouts.*;                        // Var, Val, Vars, Vals, Tuple, From, Viewable, HasId, Event

// build + show
UI.show(panel("fill, wrap 2").add("growx", textField(name)).add(button("Go").onClick(it -> ...)));
UI.show(f -> new MyView(vm)); EventProcessor.DECOUPLED.join();

// view skeleton
UI.of(this).withLayout(FILL.and(WRAP(1)).and(INS(16))).add(GROW, child);

// state
Var<T> v = Var.of(value);  v.get(); v.set(x); v.update(fn);  Val<U> d = v.viewAsString(fn);
v.isEnabledIf / isVisibleIf / isSelectedIf / isEditableIf (Val<Boolean>)
Viewable<R> c = Viewable.of(a, b, (x,y) -> combine);     // derived from 2 sources (recomputes on either)
Viewable<T> w = v.view();                                // weakly-held listenable view (store in a field!)

// sprouts immutable collections (persistent; every op returns a new instance)
Tuple<T> t = Tuple.of(a,b,c) / Tuple.of(T.class);        // List-like: add/remove/setAt/map/retainIf/sort
Association<K,V> m = Association.between(K.class,V.class);// Map-like: put / get(k)->Optional / remove   (NOT .of!)
ValueSet<E> s = ValueSet.of(E.class);                    // Set-like: add/addAll/retainAll/any
Result<T> res = Result.ofTry(T.class, () -> risky());    // Maybe<T> + Tuple<Problem>; res.problems()/orElse(x)

// lenses (MVI/MVL)
Var<F> f = root.zoomTo(Root::f, Root::withF);             // mutable lens
Val<F> r = root.viewAs(F.class, Root::f);                 // read-only view
Var<V> e = root.zoomTo(c -> c.get(k).orElse(d), (c,x) -> c.put(k,x));  // lens into a collection entry
Var<Tuple<Item>> items = root.zoomTo(Root::items, Root::withItems);
panel.addAll(items, (Var<Item> it) -> itemView(it));      // per-item lens ⇒ Item implements HasId<UUID>!
panel.addAll(roTuple, (Item it) -> itemView(it));         // read-only value ⇒ no HasId needed

// events
.onClick / .onMouseEnter / .onMouseClick / .onKeyPress / .onResize (it -> ...)
.on(observable, it -> appWork)        .onView(observable, it -> viewWork)

// style
.withStyle(it -> it.padding(8).borderRadius(12).backgroundColor(c).shadowBlurRadius(6)
                   .gradient(Layer.BACKGROUND,"g",g->g.type(GradientType.RADIAL).colors(a,b))
                   .componentFont(fc -> fc.size(14).family("Serif")))
.withRepaintOn(propA, propB)
.withTransitionalStyle(boolVar, LifeTime.of(0.4, SECONDS), (state, it) -> it. ...progress()...)

// animation
it.animateFor(0.5, TimeUnit.SECONDS, s -> ... s.progress() / s.fadeIn() / it.paint(s, g->...));
UI.animateFor(2, SECONDS).go(s -> p.set(s.progress()));
UI.animate(vm, ViewModel::someAnimatable);

// reactive layout
Var<Layout> L = Var.of(Layout.class, Layout.mig("fill, wrap 1"));
panel(L)...;  L.set(Layout.mig("fill, wrap 2").withChildConstraints(MigAddConstraint.of("growx, span 2")));

// style sheet + theme
UI.use(sheet, () -> UI.show(f -> new View()));   // sheet.reconfigure() hot-swaps

// escape hatches
.peek(c -> c.setX(..)).apply(ui -> {for(..) ui.add(..);}).applyIf(cond, ui -> ui.add(..)).get(JPanel.class)

// HiDPI scaling — you write developer px (scaled up), delegates return developer px (scaled down)
.withStyle(it -> it.minHeight(it.componentPrefHeight()))   // ✅ round-trips; NOT it.component().getPreferredSize().height ❌
it.getWidth()/getHeight()/getBounds()/mouseX()/mouseY()    // all developer px;  mouse*OnScreen() = raw screen px
UI.scale(int|float|double) / UI.unscale(..) / UI.scale(g2d)  // only when working against RAW Swing
```

### Runnable examples in the SwingTree repo (read these for full context)

- `examples/calculator/mvi/CalculatorView.java` — canonical MVI/MVL.
- `examples/team/mvi/TeamView.java` **vs** `examples/team/mvvm/TeamView.java` — same UI, both architectures.
- `examples/chat/mvi/ChatView.java` — `Tuple` + `addAll` + `HasId`.
- `examples/trains/mvi/TrainsView.java` (+ `TrainsViewModel`, `TransitClient`) — real-world MVI: `Tuple`-valued state, a Swing-free data layer doing blocking IO off the EDT, and Lombok `@With`/`@Getter` value objects (records-free, **Java 8**-clean).
- `examples/breathing/mvi/BreathingView.java` (+ `BreathingViewModel`) — modelled animation, re-arming, the GC gotcha.
- `examples/animated/AnimatedView.java` / `TransitionalAnimation.java` — the full animation primitive tour.
- `examples/zen/ThemeGardenView.java` (+ `ThemedStyleSheet`) — style sheets, groups, runtime theme swap.
- `examples/scribe/CelestialScribe.java` — `Layout.none()` derived from data, styled text flowing around children.
- `examples/dashboard/SalesDashboard.java` — reactive `Var<Layout>` reflow.
- `examples/stylish/SoftUIView.java`, `SvgViewer.java` — style sheets, SVG, custom paint.
- `examples/simple/ResponsiveLayout*.java` — `AUTO_SPAN` responsive flow.

The wiki (`docs/markdown/`) is the prose companion; start at `README.md` →
`Climbing-Swing-Tree.md` → `Functional-MVVM.md`.
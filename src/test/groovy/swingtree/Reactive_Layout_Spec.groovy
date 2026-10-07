package swingtree

import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.Layout
import swingtree.components.JBox
import swingtree.layout.*
import swingtree.threading.EventProcessor

import javax.swing.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.List
import java.util.concurrent.atomic.AtomicInteger

@Title("Reactive Layouts")
@Narrative("""

    Layouts in SwingTree are not just static configurations attached to a component
    at construction time — they can be made fully reactive. This means that a layout
    can be driven by a mutable property (`Var<Layout>`), so that whenever the property
    changes, the component's layout manager is updated automatically, without recreating
    the component from scratch.

    This is especially powerful for responsive UIs, where the layout of a panel
    may need to adapt to user interaction, viewport size, application state, or
    data changes at runtime.

    The entry point for this feature is `UIForAnySwing::withLayout(Val<Layout>)`.
    Internally, it is the property driven style `withStyle(layout, (l, it) -> it.layout(l))`,
    which means:

      - Whenever the `layout` property fires a change event, the new `Layout` object
        travels to the UI thread as part of that event, and the style is re-evaluated.
      - The re-evaluation hands that `Layout` object to the style, so the UI thread never
        reads the property itself.
      - That object's `installFor(component)` method is called, which installs or
        updates the layout manager on the panel in-place.

    The `Layout` implementations are deliberately immutable and designed with efficient
    in-place update semantics: if the same type of layout manager is already installed
    on the component, `installFor` updates its constraints directly rather than replacing
    the manager instance, avoiding unnecessary component tree invalidation.

    The tests below cover:
      - Initial property value being applied at build time
      - Swapping the layout manager type via a property change
      - `Layout.unspecific()` acting as a no-op
      - `Layout.none()` removing the layout manager
      - `Layout.none()` with child bounds enabling declarative absolute positioning
      - Sparse per-child bound targeting via `withChildBound(int, Bounds)` without placeholder entries
      - `Layout.none()` child bounds honouring the SwingTree UI scale factor for HiDPI-aware absolute positioning
      - In-place constraint updates for `ForMigLayout`
      - Positional per-child MigLayout add-constraints applied reactively
      - Sparse per-child MigLayout constraint via `withChildConstraint(int, MigAddConstraint)`
      - Per-child `FlowCell` constraints pushed as client properties for responsive layouts
      - End-to-end reactive responsive layout: changing span policies changes actual bounds
      - `UI.panel(Val<Layout>)` factory giving full layout control from the start
      - `UI.box(Val<Layout>)` factory giving full layout control from the start
      - `UI.panel(Val<Layout>)` MigLayout in-place update via the factory entry-point
      - `UI.box(Val<Layout>)` verbatim layout installation without implicit inset injection
      - `Layout.grid(rows, cols)` and `Layout.box(axis)` installed and updated reactively
      - A `FlowCell` with no span policies always spans all 12 columns at every parent size category

""")
@Subject([Layout, UIForAnySwing])
class Reactive_Layout_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'The initial value of a `Var<Layout>` property is applied at component build time.'()
    {
        reportInfo """
            When `withLayout(Val<Layout>)` is called with a property that already holds a
            layout value, that layout is installed on the component immediately during
            construction — just as if `withLayout(Layout)` had been called with the same value.

            This means the initial state of the component is always predictable:
            whatever layout the property holds when `.get(JPanel)` is called is the
            layout the panel starts with. There is no "pending" or "deferred" installation.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property initially configured as a MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A panel whose layout is bound to the property:'
            def panel =
                UI.panel()
                .withLayout(layout)
                .get(JPanel)

        expect: 'The panel immediately has a MigLayout installed, matching the initial property value:'
            (panel.getLayout() instanceof MigLayout)
        and: 'The layout constraints on the MigLayout are exactly what was specified:'
            ((MigLayout) panel.getLayout()).getLayoutConstraints() == "fill"
    }

    def 'Updating a `Var<Layout>` property replaces the panel`s layout manager type on the fly.'()
    {
        reportInfo """
            The primary power of `withLayout(Val<Layout>)` is that the entire layout manager
            can be swapped at runtime by simply changing the property value — no UI rebuild needed.

            This enables features like toggling between a grid and a flow layout, or switching
            to a compact layout in a narrow viewport. From the application code's perspective,
            only the `Var.set(...)` call is needed; SwingTree handles all the plumbing.

            Internally, the property change event carries the new `Layout` object to the UI thread,
            which re-evaluates the style function `(l, it) -> it.layout(l)` with it, producing a
            new `StyleConf`. The `StyleInstaller` then calls `installFor(panel)` on the new
            `Layout` object, which checks the currently installed layout manager type and
            replaces it if it no longer matches.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property starting with a MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A panel bound to the property:'
            def panel =
                UI.panel()
                .withLayout(layout)
                .get(JPanel)

        expect: 'The panel starts with a MigLayout, as specified by the initial property value:'
            (panel.getLayout() instanceof MigLayout)

        when: 'We change the property to a responsive flow layout:'
            layout.set(Layout.flow())
        then: 'The panel now has a ResponsiveGridFlowLayout — the MigLayout was replaced:'
            (panel.getLayout() instanceof ResponsiveGridFlowLayout)

        when: 'We switch back to a MigLayout with different constraints:'
            layout.set(Layout.mig("flowy, wrap 2"))
        then: 'The MigLayout is reinstalled with the updated constraints:'
            (panel.getLayout() instanceof MigLayout)
            ((MigLayout) panel.getLayout()).getLayoutConstraints() == "flowy, wrap 2"
    }

    def '`Layout.unspecific()` is a deliberate no-op that leaves any existing layout manager untouched.'()
    {
        reportInfo """
            Not every property state needs to result in a layout manager update.
            Sometimes a reactive layout property may transition through a "no preference"
            phase while other model state is being resolved. For such cases,
            `Layout.unspecific()` acts as a deliberate no-op: when `installFor` is called
            on it, it does nothing at all, leaving whatever layout manager is already present
            on the component completely unchanged.

            This is particularly useful in reactive scenarios where the layout property
            is temporarily `unspecific()` as a neutral starting value, without disturbing
            any layout manager that was applied before the binding was established.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A panel that starts with a MigLayout via the reactive layout property:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
            def panel =
                UI.panel()
                .withLayout(layout)
                .get(JPanel)

        expect: 'The panel has a MigLayout installed as expected:'
            (panel.getLayout() instanceof MigLayout)

        when: 'We switch the layout property to `Layout.unspecific()`, the no-op sentinel:'
            layout.set(Layout.unspecific())
        then: 'The MigLayout is still present — `unspecific()` did not touch it:'
            (panel.getLayout() instanceof MigLayout)
        and: 'The layout constraints are still the same as before the property change:'
            ((MigLayout) panel.getLayout()).getLayoutConstraints() == "fill"
    }

    def '`Layout.none()` removes any existing layout manager by setting it to null.'()
    {
        reportInfo """
            For components that should be laid out manually (i.e. with absolute positioning),
            `Layout.none()` removes the layout manager entirely by calling `setLayout(null)`.

            This is useful in reactive scenarios where a panel might start with a layout
            manager for its normal operating state, then switch to `Layout.none()` to enter
            a "canvas mode" where child components are positioned programmatically.

            Switching back from `Layout.none()` to a concrete layout type is equally easy:
            just set the property to the desired layout, and the new manager will be installed.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A panel that starts with a MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
            def panel =
                UI.panel()
                .withLayout(layout)
                .get(JPanel)

        expect: 'The panel starts with a MigLayout:'
            (panel.getLayout() instanceof MigLayout)

        when: 'We switch to `Layout.none()`, which removes the layout manager:'
            layout.set(Layout.none())
        then: 'The layout manager is now null, enabling absolute positioning:'
            panel.getLayout() == null

        when: 'We install a new layout by switching the property back to a concrete layout:'
            layout.set(Layout.flow())
        then: 'The new layout manager is installed as expected:'
            panel.getLayout() instanceof ResponsiveGridFlowLayout
    }

    def 'Changing MigLayout constraints via a property updates them in-place on the existing manager instance.'()
    {
        reportInfo """
            When the `ForMigLayout` configuration changes (e.g. different layout, column, or
            row constraint strings), SwingTree does not create a brand-new `MigLayout` instance.
            Instead, it calls the setters on the existing manager to update its constraints
            in-place. The manager instance itself stays the same.

            This in-place update strategy matters for two reasons:

              1. **Performance**: replacing the layout manager would invalidate the entire
                 component subtree unnecessarily. In-place updates avoid that churn.

              2. **State preservation**: any per-component constraint state cached inside the
                 layout manager (such as `CC` objects for individual children) is retained
                 across minor constraint changes, instead of being silently wiped.

            The in-place update only triggers a `revalidate()` call when the constraints
            actually changed, so unchanged re-applications are also cheap.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A panel with an initial "fill" MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
            def panel =
                UI.panel()
                .withLayout(layout)
                .get(JPanel)
        and: 'We capture the MigLayout instance identity for comparison:'
            def originalMigLayout = panel.getLayout()

        expect: 'The layout constraints match the initial property value:'
            ((MigLayout) originalMigLayout).getLayoutConstraints() == "fill"

        when: 'We update the property with a new set of layout constraints:'
            layout.set(Layout.mig("flowy, wrap 3"))
        then: 'The constraints are updated — but on the SAME manager instance, not a new one:'
            panel.getLayout().is(originalMigLayout)
            ((MigLayout) panel.getLayout()).getLayoutConstraints() == "flowy, wrap 3"

        when: 'We update all three constraint types at once (layout, column, row):'
            layout.set(Layout.mig("fill", "[grow][]", "[shrink]"))
        then: 'All three constraint types are updated in-place on the same MigLayout instance:'
            panel.getLayout().is(originalMigLayout)
            ((MigLayout) panel.getLayout()).getLayoutConstraints() == "fill"
            ((MigLayout) panel.getLayout()).getColumnConstraints() == "[grow][]"
            ((MigLayout) panel.getLayout()).getRowConstraints() == "[shrink]"
    }

    def 'A `ForMigLayout` with child constraints applies them reactively to child components.'()
    {
        reportInfo """
            Beyond controlling the MigLayout's own constraints (layout, column, row), a
            `ForMigLayout` configuration can also specify per-child *add-constraints* — the
            constraint strings that determine how each individual child component is placed
            within the MigLayout grid (e.g. "grow", "span 2", "wrap").

            These are stored in a sparse `Association<Integer, MigAddConstraint>` inside the
            `ForMigLayout` object, keyed by child index. When `ForMigLayout.installFor(panel)`
            runs, it iterates over the association entries and pushes each constraint to the
            `MigLayout` via `setComponentConstraints(child, constraint)` for the child at
            the corresponding index.

            Since `ForMigLayout` is immutable, changing the per-child constraints requires
            creating a new instance (typically via `withChildConstraints(...)` or the
            `Layout.mig(constr, childConstraints...)` factory). Storing that new instance in
            the `Var<Layout>` triggers the reactive update and applies the constraints.

            This makes the entire MigLayout configuration — parent constraints AND per-child
            add-constraints — fully reactive and bindable to a single `Var<Layout>` property.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout with no initial per-child constraints:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A panel with three buttons bound to the reactive layout:'
            def panel =
                UI.panel()
                .withLayout(layout)
                .add(UI.button("A"))
                .add(UI.button("B"))
                .add(UI.button("C"))
                .get(JPanel)
        and: 'A direct reference to the underlying MigLayout for constraint inspection:'
            def migLayout = (MigLayout) panel.getLayout()

        expect: 'Before any child constraints are set, none of the children have custom add-constraints:'
            migLayout.getComponentConstraints(panel.getComponent(0)) in ([null, ""] as List<Object>)
            migLayout.getComponentConstraints(panel.getComponent(1)) in ([null, ""] as List<Object>)
            migLayout.getComponentConstraints(panel.getComponent(2)) in ([null, ""] as List<Object>)

        when: 'We update the layout property to carry per-child constraints for all three buttons:'
            layout.set(
                Layout.mig("fill").withChildConstraints(
                    MigAddConstraint.of("grow"),
                    MigAddConstraint.of("shrink"),
                    MigAddConstraint.of("wrap")
                )
            )
        then: 'Each child component now has its designated constraint applied inside the MigLayout:'
            migLayout.getComponentConstraints(panel.getComponent(0)) == "grow"
            migLayout.getComponentConstraints(panel.getComponent(1)) == "shrink"
            migLayout.getComponentConstraints(panel.getComponent(2)) == "wrap"

        when: 'We change the constraints again — for example, to reverse the role of the first two buttons:'
            layout.set(
                Layout.mig("fill").withChildConstraints(
                    MigAddConstraint.of("shrink"),
                    MigAddConstraint.of("grow"),
                    MigAddConstraint.of("wrap")
                )
            )
        then: 'The updated constraints are reflected immediately on the same children:'
            migLayout.getComponentConstraints(panel.getComponent(0)) == "shrink"
            migLayout.getComponentConstraints(panel.getComponent(1)) == "grow"
            migLayout.getComponentConstraints(panel.getComponent(2)) == "wrap"
    }

    def 'A `ForFlowLayout` with child `FlowCell` constraints pushes them as client properties onto children.'()
    {
        reportInfo """
            The `ForFlowLayout` layout configuration (backed by `ResponsiveGridFlowLayout`)
            supports per-child `FlowCell` constraints that define how many 12-column grid
            cells each child component should span at different parent container size categories.

            When `ForFlowLayout.installFor(panel)` runs, it iterates over the panel's children
            and writes each child's `FlowCell` to that child's client property keyed by
            `AddConstraint.class`. The `ResponsiveGridFlowLayout` reads these client properties
            during its next layout pass to determine the actual width of each child.

            Because `installFor` is called whenever the `Var<Layout>` property changes,
            the responsive span policies for ALL children can be updated atomically in a
            single property assignment. The `ResponsiveGridFlowLayout` picks up the new
            policies on the very next `doLayout()` call.

            This is the recommended approach for highly dynamic responsive layouts:
            use `Var<Layout>` with a `ForFlowLayout` carrying explicit `FlowCell` child
            constraints, rather than managing `AUTO_SPAN` constraints through individual
            component add-calls.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'Two FlowCell instances representing distinct responsive span policies:'
            def cellA = UI.AUTO_SPAN({ it.verySmall(12).small(6).medium(4).large(3) })
            def cellB = UI.AUTO_SPAN({ it.verySmall(12).small(6).medium(8).large(9) })
        and: 'A reactive layout property with no initial per-child constraints:'
            def layout = Var.of(Layout.class, Layout.flow())
        and: 'A panel with two child components bound to the reactive flow layout:'
            def panel =
                UI.panel().withPrefSize(120, 100)
                .withLayout(layout)
                .add(UI.box().withPrefHeight(20))
                .add(UI.box().withPrefHeight(20))
                .get(JPanel)

        expect: 'Initially the children have no FlowCell client properties (no span policy set):'
            panel.getComponent(0).getClientProperty(AddConstraint.class) == null
            panel.getComponent(1).getClientProperty(AddConstraint.class) == null

        when: 'We update the layout property with explicit FlowCell constraints for each child:'
            layout.set(Layout.flow().withChildConstraints(cellA, cellB))
        then: 'Each child component now carries its designated FlowCell as a client property:'
            panel.getComponent(0).getClientProperty(AddConstraint.class) == cellA
            panel.getComponent(1).getClientProperty(AddConstraint.class) == cellB

        when: 'We swap the FlowCell assignments between the two children:'
            layout.set(Layout.flow().withChildConstraints(cellB, cellA))
        then: 'The client properties are updated to reflect the new assignment:'
            panel.getComponent(0).getClientProperty(AddConstraint.class) == cellB
            panel.getComponent(1).getClientProperty(AddConstraint.class) == cellA

        when: 'We revert to a layout with no child constraints:'
            layout.set(Layout.flow())
        then: """
            The client properties from the previous layout are no longer being pushed.
            Note that unlike removing them, they remain on the children from the last
            `installFor` call — the client property API does not support a "remove" operation.
            However, a freshly added child that was not present when the constrained layout
            was installed will have no client property set.
        """
            panel.getComponent(0).getClientProperty(AddConstraint.class) == cellB
            panel.getComponent(1).getClientProperty(AddConstraint.class) == cellA
    }

    def 'Changing child `FlowCell` constraints reactively changes how children are laid out.'()
    {
        reportInfo """
            This test demonstrates the complete end-to-end reactive responsive layout
            scenario: a panel contains children whose responsive span policies are
            controlled entirely by a `Var<Layout>` property. When the property changes,
            the new span policies are pushed to the children as client properties, and
            the very next `doLayout()` call produces a different visual arrangement.

            We use two children and test at a "medium" panel width. At this width:

              - If each child spans 12/12 cells (full width), both children each occupy a
                full row — so the second child is positioned BELOW the first.
              - If each child spans 6/12 cells (half width), both children fit on the same
                row — so the second child is positioned BESIDE the first (same Y coordinate).

            One important subtlety worth knowing: when `withLayout(Val<Layout>)` is called,
            the style engine applies the layout immediately at that point in the builder chain.
            Since children are added AFTER `withLayout(...)` in a builder expression, Phase 2
            of `ForFlowLayout.installFor` (which pushes `FlowCell` constraints to children)
            runs before any children exist. This means the initial `FlowCell` constraints
            from the `Var`'s starting value have no effect — the children aren't there yet
            to receive them.

            The correct pattern is therefore to call `Var.set(...)` explicitly after the
            component is fully built. This triggers a re-evaluation when the children ARE
            present, and the `FlowCell` constraints are pushed correctly.

            Changing the `Var<Layout>` property atomically updates both children's span
            policies and takes effect on the next layout pass. No other imperative calls
            are needed beyond `doLayout()`.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property, initially with no child constraints:'
            def layout = Var.of(Layout.class, Layout.flow())
        and: 'A panel with two fixed-height child boxes and a known preferred size:'
            def panel =
                UI.panel().withPrefSize(120, 100)
                .withLayout(layout)
                .add(UI.box().withPrefHeight(20))
                .add(UI.box().withPrefHeight(20))
                .get(JPanel)
        and: """
            Now that the panel is fully built (children are present), we set the layout
            property to use full-width (12/12) spans for the medium size category.
            At this point, `installFor` runs with children in the panel, so Phase 2
            successfully pushes the `FlowCell` constraints to both child components.
        """
            layout.set(
                Layout.flow(
                    UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(12).large(12) }),
                    UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(12).large(12) })
                )
            )

        when: """
            We trigger a layout at a "medium" panel width — 60px, which is between
            2/5 (48px) and 3/5 (72px) of the panel's preferred width of 120px.
            At this size, the `medium` span policy is active.
        """
            panel.setSize(60, 200)
            panel.doLayout()
        then: 'With full-width (12/12) spans, the second child is placed on a separate row BELOW the first:'
            panel.getComponent(1).y > panel.getComponent(0).y

        when: 'We change the layout so each child spans only half the width (6/12 cells at medium size):'
            layout.set(
                Layout.flow(
                    UI.AUTO_SPAN({ it.verySmall(6).small(6).medium(6).large(6) }),
                    UI.AUTO_SPAN({ it.verySmall(6).small(6).medium(6).large(6) })
                )
            )
        and: 'We trigger another layout pass:'
            panel.doLayout()
        then: 'With half-width (6/12) spans, both children now share the same row (identical Y coordinate):'
            panel.getComponent(0).y == panel.getComponent(1).y

        when: 'We revert to full-width spans:'
            layout.set(
                Layout.flow(
                    UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(12).large(12) }),
                    UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(12).large(12) })
                )
            )
        and: 'We trigger another layout pass:'
            panel.doLayout()
        then: 'The second child is back on its own row below the first:'
            panel.getComponent(1).y > panel.getComponent(0).y
    }

    def '`UI.panel(Val<Layout>)` applies any Layout type reactively, not just MigLayout.'()
    {
        reportInfo """
            The `UI.panel(Val<Layout>)` factory is the most flexible reactive panel factory:
            it accepts any `Layout` implementation — MigLayout, responsive flow, border,
            grid, box axis — and installs it reactively whenever the property changes.

            The property value is used verbatim — the caller can supply any `Layout`
            implementation, enabling switches between entirely different layout families
            by just calling `Var.set(...)` with the desired `Layout` instance.

            This test verifies:
              - The initial `Layout` held by the property is installed at build time.
              - A switch to a completely different layout type (flow) replaces the manager.
              - A switch back to a MigLayout with different constraints updates it in-place.
              - A switch to `Layout.border()` installs a `BorderLayout`.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property starting with a MigLayout with column and row constraints:'
            def layout = Var.of(Layout.class, Layout.mig("fill", "[grow][]", "[shrink]"))
        and: 'A panel created via the Val<Layout> factory overload:'
            def panel = UI.panel(layout).get(JPanel)

        expect: 'The panel immediately has a MigLayout with the specified constraints:'
            (panel.getLayout() instanceof MigLayout)
            ((MigLayout) panel.getLayout()).getLayoutConstraints()  == "fill"
            ((MigLayout) panel.getLayout()).getColumnConstraints()  == "[grow][]"
            ((MigLayout) panel.getLayout()).getRowConstraints()     == "[shrink]"

        when: 'We switch to a responsive flow layout:'
            layout.set(Layout.flow())
        then: 'The MigLayout is replaced by a ResponsiveGridFlowLayout:'
            (panel.getLayout() instanceof ResponsiveGridFlowLayout)

        when: 'We switch back to MigLayout with different constraints:'
            layout.set(Layout.mig("flowy, wrap 2"))
        then: 'A MigLayout is (re-)installed with the new constraints:'
            (panel.getLayout() instanceof MigLayout)
            ((MigLayout) panel.getLayout()).getLayoutConstraints() == "flowy, wrap 2"

        when: 'We switch to a border layout:'
            layout.set(Layout.border())
        then: 'A BorderLayout is installed:'
            panel.getLayout() instanceof BorderLayout
    }

    def '`UI.box(Val<Layout>)` applies any Layout type reactively without appending "ins 0".'()
    {
        reportInfo """
            `UI.box(Val<Layout>)` is the full-control reactive variant of the box factory.
            It installs whatever `Layout` the property holds at build time and updates it
            whenever the property changes — identical to calling `UI.box().withLayout(layout)`.

            The caller owns the layout object entirely and can choose a non-MigLayout type,
            supply explicit inset constraints, or leave insets at their default values.
            No implicit `"ins 0"` suffix is ever appended.

            This test verifies:
              - The initial Layout is installed verbatim (no "ins 0" appended).
              - Switching to a flow layout replaces the manager.
              - Switching to `Layout.none()` removes the layout manager.
              - Switching back to a MigLayout reinstalls it correctly.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property holding an explicit MigLayout with custom insets:'
            def layout = Var.of(Layout.class, Layout.mig(LayoutConstraint.of("fill", "ins 4")))
        and: 'A JBox created via the Val<Layout> factory overload:'
            def box = UI.box(layout).get(JBox)

        expect: 'The box has a MigLayout installed, containing both provided constraint tokens:'
            (box.getLayout() instanceof MigLayout)
            ((MigLayout) box.getLayout()).getLayoutConstraints().contains("fill")
            ((MigLayout) box.getLayout()).getLayoutConstraints().contains("ins 4")

        when: 'We switch to a responsive flow layout:'
            layout.set(Layout.flow())
        then: 'The MigLayout is replaced by a ResponsiveGridFlowLayout:'
            (box.getLayout() instanceof ResponsiveGridFlowLayout)

        when: 'We switch to Layout.none() to remove the layout manager entirely:'
            layout.set(Layout.none())
        then: 'The layout manager is null, enabling absolute positioning:'
            box.getLayout() == null

        when: 'We restore a MigLayout:'
            layout.set(Layout.mig("wrap 1"))
        then: 'The MigLayout is reinstalled:'
            (box.getLayout() instanceof MigLayout)
            ((MigLayout) box.getLayout()).getLayoutConstraints() == "wrap 1"
    }

    def '`UI.panel(Val<Layout>)` updates MigLayout constraints in-place across successive property changes.'()
    {
        reportInfo """
            When the `Var<Layout>` property held by `UI.panel(Val<Layout>)` is updated
            with successive `ForMigLayout` values, SwingTree updates the existing MigLayout
            instance's constraints in-place rather than replacing the manager object.

            This means the MigLayout object identity is preserved across constraint-only
            changes, avoiding unnecessary component-tree invalidation while still reflecting
            the new constraint strings immediately.

            The test pins all three constraint axes (layout, column, row) across multiple
            successive updates to ensure no regression in the in-place update path when
            the panel is built via the factory overload rather than via
            `UI.panel().withLayout(...)` directly.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property starting with a full three-axes MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill", "[grow][]", "[shrink]"))
        and: 'A panel created via the Val<Layout> factory overload:'
            def panel = UI.panel(layout).get(JPanel)
        and: 'We capture the MigLayout instance for identity checks later:'
            def originalMig = panel.getLayout()

        expect: 'All three constraint axes match the initial property value:'
            originalMig instanceof MigLayout
            ((MigLayout) originalMig).getLayoutConstraints()  == "fill"
            ((MigLayout) originalMig).getColumnConstraints()  == "[grow][]"
            ((MigLayout) originalMig).getRowConstraints()     == "[shrink]"

        when: 'We update to a new MigLayout value with different constraints:'
            layout.set(Layout.mig("flowy, wrap 3", "[fill]", "[grow]"))
        then: 'The same MigLayout instance is used and all three axes are updated:'
            panel.getLayout().is(originalMig)
            ((MigLayout) panel.getLayout()).getLayoutConstraints()  == "flowy, wrap 3"
            ((MigLayout) panel.getLayout()).getColumnConstraints()  == "[fill]"
            ((MigLayout) panel.getLayout()).getRowConstraints()     == "[grow]"

        when: 'We update again, changing only the layout constraint axis:'
            layout.set(Layout.mig("fill, wrap 2"))
        then: 'The in-place update applies and the panel still uses the original manager instance:'
            panel.getLayout().is(originalMig)
            ((MigLayout) panel.getLayout()).getLayoutConstraints()  == "fill, wrap 2"
    }

    def '`UI.box(Val<Layout>)` uses the full layout verbatim and does not append implicit insets.'()
    {
        reportInfo """
            `UI.box(Val<Layout>)` installs the `Layout` held by the property exactly as
            provided — no implicit constraint suffix (such as `"ins 0"`) is ever appended.

            With the full-control `Val<Layout>` API the caller is responsible for choosing
            the desired inset behaviour by constructing the `Layout` appropriately:
              - `Layout.mig(LayoutConstraint.of("fill", "ins 0"))` for explicit zero insets.
              - `Layout.mig("fill")` when default insets are acceptable.
              - Any other `Layout` family (flow, border, …) for non-MigLayout managers.

            This test verifies that no implicit `"ins 0"` is injected and that the constraint
            string roundtrips exactly through the MigLayout manager across multiple updates.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property with a plain MigLayout constraint (no "ins 0"):'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A JBox created via the Val<Layout> factory overload:'
            def box = UI.box(layout).get(JBox)

        expect: 'The MigLayout has exactly the constraint provided — no "ins 0" is appended:'
            box.getLayout() instanceof MigLayout
            ((MigLayout) box.getLayout()).getLayoutConstraints() == "fill"

        when: 'We switch to a constraint that explicitly sets custom insets:'
            layout.set(Layout.mig(LayoutConstraint.of("fill", "ins 8")))
        then: 'Both constraint tokens are present in the installed layout constraints:'
            box.getLayout() instanceof MigLayout
            ((MigLayout) box.getLayout()).getLayoutConstraints().contains("fill")
            ((MigLayout) box.getLayout()).getLayoutConstraints().contains("ins 8")

        when: 'We switch to a plain constraint with no insets at all:'
            layout.set(Layout.mig("wrap 2"))
        then: 'The constraint is installed as-is — still no implicit "ins 0" suffix:'
            box.getLayout() instanceof MigLayout
            ((MigLayout) box.getLayout()).getLayoutConstraints() == "wrap 2"
    }

    def '`Layout.none()` with child bounds removes the layout manager and positions children at the specified coordinates.'()
    {
        reportInfo """
            When a `None` layout carries per-child `Bounds`, its `installFor` performs
            two actions in sequence:

              1. Any existing layout manager is removed (`setLayout(null)`), leaving the
                 component in "canvas" mode where children are positioned manually.
              2. For each entry in the sparse `Association<Integer, Bounds>`, the child at
                 that index has its absolute position and size applied via
                 `Component.setBounds(...)`.

            Because the reactive layout system calls `installFor` on every `Var.set(...)`
            call, the absolute positions of child components can be changed at runtime by
            supplying a new `Layout.none(Bounds...)` value to the property —
            exactly like any other layout family in SwingTree.

            Switching back to a concrete layout (e.g. `Layout.mig(...)`) reinstalls the
            layout manager and restores managed positioning.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property starting with a MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A panel with three child components:'
            def panel =
                UI.panel()
                .withLayout(layout)
                .add(UI.box())
                .add(UI.box())
                .add(UI.box())
                .get(JPanel)

        expect: 'The panel starts with a MigLayout:'
            (panel.getLayout() instanceof MigLayout)

        when: 'We switch to `Layout.none()` with explicit bounds declared for each child:'
            layout.set(
                Layout.none(
                    Bounds.of(  0,   0, 120, 40),
                    Bounds.of(  0,  50, 120, 40),
                    Bounds.of(  0, 100, 120, 40)
                )
            )
        then: 'The layout manager is removed — the panel is now in canvas/absolute mode:'
            panel.getLayout() == null
        and: 'Each child now occupies exactly the bounds that were declared in the layout object:'
            panel.getComponent(0).getBounds() == Bounds.of(  0,   0, 120, 40).toRectangle()
            panel.getComponent(1).getBounds() == Bounds.of(  0,  50, 120, 40).toRectangle()
            panel.getComponent(2).getBounds() == Bounds.of(  0, 100, 120, 40).toRectangle()

        when: 'We update the bounds reactively — repositioning all three children:'
            layout.set(
                Layout.none(
                    Bounds.of( 10,  10,  80, 30),
                    Bounds.of( 10,  50,  80, 30),
                    Bounds.of( 10,  90, 160, 60)
                )
            )
        then: 'The layout manager remains null and the new bounds are applied to each child:'
            panel.getLayout() == null
            panel.getComponent(0).getBounds() == Bounds.of( 10,  10,  80, 30).toRectangle()
            panel.getComponent(1).getBounds() == Bounds.of( 10,  50,  80, 30).toRectangle()
            panel.getComponent(2).getBounds() == Bounds.of( 10,  90, 160, 60).toRectangle()

        when: 'We switch back to a MigLayout to restore managed positioning:'
            layout.set(Layout.mig("fill"))
        then: 'The MigLayout is reinstalled — the panel is managed again:'
            panel.getLayout() instanceof MigLayout
    }

    def '`Layout.none()` child bounds are multiplied by the SwingTree UI scale factor so absolute positioning stays HiDPI aware.'()
    {
        reportInfo """
            SwingTree supports a global UI scale factor which is applied to virtually every
            pixel value that flows through the library. This is how SwingTree achieves crisp
            rendering on HiDPI displays from the very same UI code that also runs on regular
            displays.

            Absolute positioning via `Layout.none(Bounds...)` is no exception: the `Bounds`
            you declare in your layout code are specified in logical (unscaled) pixels — the
            same units you would write in any other SwingTree API. When the layout is
            installed on a component, each bound is passed through `UI.scale(Rectangle)`
            before it is forwarded to `Component.setBounds(...)`, so the component ends up
            at the correct physical location for the current scale factor.

            This means you can author a screen with hand-placed children at, say,
            `Bounds.of(10, 10, 100, 50)` and have it automatically produce
            `(20, 20, 200, 100)` on a display running at a UI scale of 2.
            No special case handling is required in your UI code.

            Because this scaling happens inside `installFor`, it is re-evaluated on every
            reactive layout update — so even after toggling between different `Layout.none(...)`
            values at runtime, the final child bounds always reflect the current UI scale.
        """
        given: 'We set the UI scale factor to 2 — emulating a HiDPI display:'
            SwingTree.get().setUiScaleFactor(2f)
        and: 'A reactive layout property starting with a MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A panel with three child components and a bound layout:'
            def panel =
                UI.panel()
                .withLayout(layout)
                .add(UI.box())
                .add(UI.box())
                .add(UI.box())
                .get(JPanel)

        when: 'We switch to `Layout.none()` with logical (unscaled) bounds declared for each child:'
            layout.set(
                Layout.none(
                    Bounds.of(  0,   0, 120, 40),
                    Bounds.of(  0,  50, 120, 40),
                    Bounds.of(  0, 100, 120, 40)
                )
            )
        then: 'The layout manager is removed and the panel enters absolute positioning mode:'
            panel.getLayout() == null
        and: 'Each child ends up at its logical bounds multiplied by the UI scale factor of 2:'
            panel.getComponent(0).getBounds() == new java.awt.Rectangle(  0,   0, 240, 80)
            panel.getComponent(1).getBounds() == new java.awt.Rectangle(  0, 100, 240, 80)
            panel.getComponent(2).getBounds() == new java.awt.Rectangle(  0, 200, 240, 80)

        when: 'We reactively update the layout with a new set of logical bounds:'
            layout.set(
                Layout.none(
                    Bounds.of( 10,  10,  80, 30),
                    Bounds.of( 10,  50,  80, 30),
                    Bounds.of( 10,  90, 160, 60)
                )
            )
        then: 'The new bounds are, again, scaled by the current UI scale factor before being applied:'
            panel.getComponent(0).getBounds() == new java.awt.Rectangle( 20,  20, 160, 60)
            panel.getComponent(1).getBounds() == new java.awt.Rectangle( 20, 100, 160, 60)
            panel.getComponent(2).getBounds() == new java.awt.Rectangle( 20, 180, 320, 120)

        when: 'We also verify sparse per-child bounds are scaled identically:'
            layout.set(Layout.none().withChildBound(1, Bounds.of(5, 15, 70, 25)))
        then: 'Only the targeted child is updated, and its bounds are scaled by the UI scale factor:'
            panel.getComponent(1).getBounds() == new java.awt.Rectangle(10, 30, 140, 50)

        cleanup: 'We restore the default UI scale factor so later tests are not affected:'
            SwingTree.get().setUiScaleFactor(1f)
    }

    def '`withChildBound(int, Bounds)` targets only the specified child, leaving all others untouched.'()
    {
        reportInfo """
            The per-child bounds inside a `None` layout are stored in a sparse
            `Association<Integer, Bounds>` keyed by child index.  This means a bound
            can be applied to a specific child by index without supplying any entries
            for the other children.  When `installFor` runs, only the children whose
            index appears in the association are repositioned; all others keep whatever
            bounds they already have.

            This is the key advantage over a positional collection: with a sparse
            association there is no need to supply placeholder entries for every
            preceding child just to reach the one you actually want to move.

            Because `Component.setBounds(...)` is a persistent operation on the component,
            a bound set by one `installFor` call survives subsequent calls that target
            different children — the association controls which children are updated,
            not which children retain their current state.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout starting with `Layout.none()` so there is no layout manager:'
            def layout = Var.of(Layout.class, Layout.none())
        and: 'A panel with three children, all starting at default bounds (0, 0, 0, 0):'
            def panel =
                UI.panel()
                .withLayout(layout)
                .add(UI.box())
                .add(UI.box())
                .add(UI.box())
                .get(JPanel)

        expect: 'The panel has no layout manager and all children are at their default bounds:'
            panel.getLayout() == null
            panel.getComponent(0).getBounds() == new java.awt.Rectangle(0, 0, 0, 0)
            panel.getComponent(1).getBounds() == new java.awt.Rectangle(0, 0, 0, 0)
            panel.getComponent(2).getBounds() == new java.awt.Rectangle(0, 0, 0, 0)

        when: 'We set bounds only for child at index 1 — without specifying indices 0 or 2:'
            layout.set(Layout.none().withChildBound(1, Bounds.of(10, 30, 100, 50)))
        then: 'Only child 1 is repositioned; children 0 and 2 keep their original bounds:'
            panel.getComponent(0).getBounds() == new java.awt.Rectangle(0, 0, 0, 0)
            panel.getComponent(1).getBounds() == Bounds.of(10, 30, 100, 50).toRectangle()
            panel.getComponent(2).getBounds() == new java.awt.Rectangle(0, 0, 0, 0)

        when: 'We now target only child at index 2 — the sparse association does not revisit index 1:'
            layout.set(Layout.none().withChildBound(2, Bounds.of(0, 90, 200, 60)))
        then: 'Child 2 receives its new bounds; child 1 still has the bounds set by the previous call; child 0 remains at origin:'
            panel.getComponent(0).getBounds() == new java.awt.Rectangle(0, 0, 0, 0)
            panel.getComponent(1).getBounds() == Bounds.of(10, 30, 100, 50).toRectangle()
            panel.getComponent(2).getBounds() == Bounds.of(0, 90, 200, 60).toRectangle()
    }

    def '`withChildConstraint(int, MigAddConstraint)` targets a single child without needing to fill in preceding entries.'()
    {
        reportInfo """
            The per-child add-constraints inside a `ForMigLayout` are backed by a sparse
            `Association<Integer, MigAddConstraint>` keyed by child index.  A constraint can
            be applied to any child by index without supplying entries for all preceding
            children.

            When `installFor` iterates the association, it only calls
            `MigLayout.setComponentConstraints(child, constraint)` for the children that
            have an explicit entry.  Children at other indices are left with whatever
            constraint the `MigLayout` already stores for them.

            Because the `MigLayout` instance is updated in-place across reactive property
            changes, a constraint set in one `installFor` call persists until a subsequent
            call explicitly overwrites it for that same index.  This means sparse updates
            accumulate naturally: applying a constraint for index 0, then separately for
            index 2, leaves both constraints in effect simultaneously.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout with no initial per-child constraints:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A panel with three buttons bound to the reactive layout:'
            def panel =
                UI.panel()
                .withLayout(layout)
                .add(UI.button("A"))
                .add(UI.button("B"))
                .add(UI.button("C"))
                .get(JPanel)
        and: 'A direct reference to the MigLayout instance for constraint inspection:'
            def migLayout = (MigLayout) panel.getLayout()

        expect: 'No child has a custom add-constraint initially:'
            migLayout.getComponentConstraints(panel.getComponent(0)) in ([null, ""] as List<Object>)
            migLayout.getComponentConstraints(panel.getComponent(1)) in ([null, ""] as List<Object>)
            migLayout.getComponentConstraints(panel.getComponent(2)) in ([null, ""] as List<Object>)

        when: 'We set a constraint only for child 2 — the sparse API requires no placeholder entries for 0 or 1:'
            layout.set(Layout.mig("fill").withChildConstraint(2, MigAddConstraint.of("span 2")))
        then: 'Only child 2 receives the constraint; children 0 and 1 are untouched:'
            migLayout.getComponentConstraints(panel.getComponent(0)) in ([null, ""] as List<Object>)
            migLayout.getComponentConstraints(panel.getComponent(1)) in ([null, ""] as List<Object>)
            migLayout.getComponentConstraints(panel.getComponent(2)) == "span 2"

        when: 'We separately configure only child 0 — without re-specifying child 2:'
            layout.set(Layout.mig("fill").withChildConstraint(0, MigAddConstraint.of("growx")))
        then: """
            Child 0 now has its constraint applied. Because the MigLayout is updated in-place
            and the new layout only touched index 0, child 2 still retains the constraint
            that was applied by the earlier `installFor` call.
        """
            migLayout.getComponentConstraints(panel.getComponent(0)) == "growx"
            migLayout.getComponentConstraints(panel.getComponent(1)) in ([null, ""] as List<Object>)
            migLayout.getComponentConstraints(panel.getComponent(2)) == "span 2"
    }

    def '`Layout.grid()` and `Layout.box()` can be installed and updated reactively.'()
    {
        reportInfo """
            SwingTree's reactive layout system is not limited to MigLayout and
            `ResponsiveGridFlowLayout`. `Layout.grid(rows, cols)` installs a
            `UniformGridLayout`, and `Layout.box(UI.Axis)` installs a
            `javax.swing.BoxLayout`. Both are installed or replaced exactly like
            any other `Layout` implementation — just call `Var.set(...)` with the
            desired value.

            `UniformGridLayout` exposes setters for all of its properties (rows, columns,
            both gap sizes, its mode and which empty rows and columns it leaves out), so
            SwingTree can update an existing `UniformGridLayout` instance
            in-place when the layout type hasn't changed. The identity of the manager
            object is preserved across such constraint-only updates.

            `BoxLayout` does not expose a setter for its axis after construction, so
            switching the axis requires a fresh manager instance. This is handled
            automatically by `ForBoxLayout.installFor`.
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive layout property starting with a plain MigLayout:'
            def layout = Var.of(Layout.class, Layout.mig("fill"))
        and: 'A panel bound to the property:'
            def panel = UI.panel().withLayout(layout).get(JPanel)

        expect: 'The panel starts with a MigLayout:'
            (panel.getLayout() instanceof MigLayout)

        when: 'We switch to a 2-row, 3-column UniformGridLayout:'
            layout.set(Layout.grid(2, 3))
        then: 'A UniformGridLayout is installed with exactly those dimensions:'
            panel.getLayout() instanceof UniformGridLayout
            ((UniformGridLayout) panel.getLayout()).getRows() == 2
            ((UniformGridLayout) panel.getLayout()).getColumns() == 3

        when: 'We update the UniformGridLayout to different dimensions — the type stays the same:'
            def firstGridLayout = panel.getLayout()
            layout.set(Layout.grid(3, 2))
        then: 'The row/column counts are updated in-place on the existing UniformGridLayout instance:'
            panel.getLayout().is(firstGridLayout)
            ((UniformGridLayout) panel.getLayout()).getRows() == 3
            ((UniformGridLayout) panel.getLayout()).getColumns() == 2

        when: 'We switch to a horizontal BoxLayout:'
            layout.set(Layout.box(UI.Axis.HORIZONTAL))
        then: 'A BoxLayout is installed along the X axis:'
            panel.getLayout() instanceof BoxLayout
            ((BoxLayout) panel.getLayout()).getAxis() == BoxLayout.X_AXIS

        when: 'We switch to a vertical BoxLayout — a new instance is created since the axis changed:'
            layout.set(Layout.box(UI.Axis.VERTICAL))
        then: 'A BoxLayout is installed along the Y axis:'
            panel.getLayout() instanceof BoxLayout
            ((BoxLayout) panel.getLayout()).getAxis() == BoxLayout.Y_AXIS

        when: 'We switch back to MigLayout:'
            layout.set(Layout.mig("flowy"))
        then: 'The MigLayout is reinstalled correctly:'
            panel.getLayout() instanceof MigLayout
            ((MigLayout) panel.getLayout()).getLayoutConstraints() == "flowy"
    }

    def 'A `FlowCell` with no span policies always spans all 12 columns at every parent size category.'()
    {
        reportInfo """
            When a `FlowCell` is constructed with an empty configurator — one that defines
            no span policies at all — the layout falls back to a default of 12 columns for
            every parent size category. This is the documented safety net specified in both
            `FlowCell` and `Layout.ForFlowLayout`: a cell without any explicit policies must
            always occupy a full row regardless of how wide or narrow the container is.

            Internally, `FlowCell.fetchConfig(...)` detects the empty `autoSpans` array after
            invoking the configurator and injects a `medium(12)` policy as the sole entry.
            The `ResponsiveGridFlowLayout` then resolves any other size category (very small,
            small, large, very large, oversize) via `_findNextBestAutoSpan`, which searches
            outward from the requested category and lands on that `MEDIUM` entry — yielding
            12 cells in every case.

            The observable consequence is straightforward: if two children both carry a
            no-policy `FlowCell`, they always stack vertically no matter what width the
            container is given, because a 12/12 span fills the entire row.

            This test exercises six distinct size categories to confirm the invariant holds
            across the full spectrum of parent widths:
              - very small  : < 1/5 of preferred width (< 24 px for a 120 px pref)
              - small       : 1/5 – 2/5                (24 – 48 px)
              - medium      : 2/5 – 3/5                (48 – 72 px)
              - large       : 3/5 – 4/5                (72 – 96 px)
              - very large  : 4/5 – 5/5                (96 – 120 px)
              - oversize    : > preferred width         (> 120 px)
        """
        given: 'We set the UI scale factor to 1 for consistent test behavior:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A no-policy FlowCell — the configurator returns the conf unchanged, defining no spans:'
            def noPolicyCell = UI.AUTO_SPAN({ it })
        and: 'A panel with two fixed-height children, each carrying the no-policy FlowCell:'
            def panel =
                UI.panel().withPrefSize(120, 100)
                .withFlowLayout()
                .add(noPolicyCell, UI.box().withPrefHeight(20))
                .add(noPolicyCell, UI.box().withPrefHeight(20))
                .get(JPanel)

        when: 'We lay out at a VERY SMALL width (12 px — less than 1/5 of the 120 px preferred width):'
            panel.setSize(12, 200)
            panel.doLayout()
        then: 'The second child is placed below the first — 12/12 span forces a new row:'
            panel.getComponent(1).y > panel.getComponent(0).y

        when: 'We lay out at a SMALL width (36 px — between 1/5 and 2/5 of preferred width):'
            panel.setSize(36, 200)
            panel.doLayout()
        then: 'Still stacked vertically — 12/12 default span is applied:'
            panel.getComponent(1).y > panel.getComponent(0).y

        when: 'We lay out at a MEDIUM width (60 px — between 2/5 and 3/5 of preferred width):'
            panel.setSize(60, 200)
            panel.doLayout()
        then: 'Still stacked vertically — this is the size category of the injected default policy:'
            panel.getComponent(1).y > panel.getComponent(0).y

        when: 'We lay out at a LARGE width (84 px — between 3/5 and 4/5 of preferred width):'
            panel.setSize(84, 200)
            panel.doLayout()
        then: 'Still stacked vertically — the MEDIUM fallback is found and returns 12 columns:'
            panel.getComponent(1).y > panel.getComponent(0).y

        when: 'We lay out at a VERY LARGE width (108 px — between 4/5 and 5/5 of preferred width):'
            panel.setSize(108, 200)
            panel.doLayout()
        then: 'Still stacked vertically — 12/12 default span holds:'
            panel.getComponent(1).y > panel.getComponent(0).y

        when: 'We lay out at an OVERSIZE width (150 px — greater than the 120 px preferred width):'
            panel.setSize(150, 200)
            panel.doLayout()
        then: 'Still stacked vertically — the invariant holds even when the panel is wider than preferred:'
            panel.getComponent(1).y > panel.getComponent(0).y
    }

    def 'Reactively resizing a "validate root" child (like a JTextField) re-lays-out its parent.'()
    {
        reportInfo """
            When you reactively change the preferred size of a child component, the surrounding
            layout must update so the new size actually takes effect on screen. SwingTree triggers
            this with a `revalidate()` call.

            There is a subtlety here that this test guards against: Swing's `revalidate()` only
            re-runs the layout up to the nearest *validate root* — a component whose
            `isValidateRoot()` returns `true`, such as a `JScrollPane`, a `JTextField` or a
            `JRootPane`. If the resized component is *itself* a validate root, then revalidating it
            alone lays out only its internals; its parent is never told to re-position it, so the
            new preferred size would silently have no effect. SwingTree therefore also revalidates
            the parent of a validate root.

            We verify this with a `JTextField` (which is a validate root, yet not a `JScrollPane`):
            after reactively changing its preferred height, the parent must lay it out at the new
            height. To test this faithfully we briefly show a real window and then process exactly
            the work that `revalidate()` scheduled — deliberately *without* forcing a layout pass
            ourselves, which would mask the very behaviour under test.
        """
        given: 'A UI scale of 1, so that preferred heights map one-to-one to pixels:'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A reactive preferred height and a parent panel holding a JTextField bound to it:'
            var prefHeight = Var.of(60)
            var textField = null
            var parent =
                    UI.panel("wrap 1")
                    .add("growx", UI.textField("text").withPrefHeight(prefHeight).peek(c -> textField = c))
                    .get(JPanel)
        and: 'We place it in a briefly shown window and lay it out once, establishing a clean baseline:'
            var frame = new javax.swing.JFrame()
            int initialHeight = 0
            int updatedHeight = 0
            UI.runNow {
                frame.setContentPane(parent)
                frame.setSize(360, 640)
                frame.setVisible(true)
                frame.validate()
                initialHeight = textField.getHeight()
            }
        expect: 'The JTextField starts laid out at its initial preferred height:'
            textField instanceof javax.swing.JTextField
            initialHeight == 60

        when: 'We reactively change the preferred height, then process only what `revalidate()` scheduled:'
            UI.runNow {
                prefHeight.set(180)
                // The real event loop would flush the scheduled (in)validations here. We do NOT call
                // parent.validate() ourselves, since that would re-lay-out the parent unconditionally
                // and hide whether the parent was actually scheduled for revalidation.
                javax.swing.RepaintManager.currentManager(parent).validateInvalidComponents()
                updatedHeight = textField.getHeight()
            }
        then: 'The parent re-laid-out the validate-root JTextField to its new preferred height:'
            updatedHeight == 180

        cleanup:
            UI.runNow { frame.dispose() }
    }

    def 'Hiding a #kind through `isVisibleIf` gives its room to the component below it.'( String kind, Closure<UIForAnySwing> component )
    {
        reportInfo """
            This scenario makes sure that hiding a ${kind} through `isVisibleIf` looks the same as
            if the ${kind} had never been added: the components after it move up and close the gap.

            You might expect that to happen all by itself, and for a label or a plain panel it
            does. Hiding a Swing component through `setVisible(false)` calls `revalidate()` on that
            component, and `revalidate()` schedules a new layout for the parent of the component,
            and for every container around that, up to the nearest *validate root*. A validate root
            is a component whose `isValidateRoot()` method returns `true`, which tells Swing that
            nothing inside it can change its size. A `JTextField`, a `JScrollPane` and a `JSplitPane`
            all return `true`. So when a ${kind} is hidden, the nearest validate root is the
            ${kind} itself. Swing also skips a validate root which is invisible, so if SwingTree
            left this to Swing alone, hiding the ${kind} would schedule no layout at all. The parent
            of the ${kind} would never be laid out again, and your users would look at an empty hole
            where the ${kind} used to be, until some unrelated change elsewhere in the window
            happened to lay the parent out.

            SwingTree prevents this: whenever `isVisibleIf` changes the visibility of a validate
            root, it also calls `revalidate()` on the parent of that validate root, so the parent
            is laid out again. (For any other component Swing already does this by itself.)
            SwingTree does the same when you change a size of a validate root through a property,
            for example through `withPrefHeight(Val)`.

            Here is a column holding the ${kind} before it is hidden, what you see afterwards, and
            what your users would see if the column were not laid out again. In this diagram,
            `[validate root]` stands for a `JTextField`, a `JScrollPane` or a `JSplitPane`, because
            the scenario is run once for each of them:

            ```
              before hiding          after hiding             after hiding, if the column
                                     (what you see)           were not laid out again
              ┌─────────────────┐    ┌─────────────────┐      ┌─────────────────┐
              │ Above           │    │ Above           │      │ Above           │
              │ [validate root] │    │ Below           │      │                 │ ← empty hole
              │ Below           │    │                 │      │ Below           │
              └─────────────────┘    └─────────────────┘      └─────────────────┘
            ```

            The scenario walks through exactly this picture. We build the column, show it in a real
            window, remember where the ${kind} starts, hide the ${kind} through its property, let
            Swing carry out whatever layouts this scheduled, and then check that the label "Below"
            now sits where the ${kind} used to start.
        """
        given : 'A UI scale of 1, so that the sizes in this scenario map one-to-one to pixels.'
            SwingTree.get().setUiScaleFactor(1f)
        and : 'A property which decides whether the middle component is visible. It starts out as `true`.'
            var isShown = Var.of(true)
        and : """
            A column of three rows: a label, the middle component bound to the property, and
            another label. The column is laid out by MigLayout with `hidemode 3`. By default,
            MigLayout keeps the room of an invisible component reserved, so that nothing moves
            when something disappears. `hidemode 3` tells MigLayout to give an invisible component
            no room and no gaps at all, so a hidden middle component should leave no trace.
        """
            var middle = null
            var below = null
            var column =
                    UI.panel("wrap 1, hidemode 3")
                    .add(UI.label("Above"))
                    .add(component().isVisibleIf(isShown).peek(c -> middle = c))
                    .add(UI.label("Below").peek(c -> below = c))
                    .get(JPanel)
        and : """
            We show the column in a real window, because Swing only lays out components which
            sit inside a window that is on screen. A window which was just shown receives a few
            more resize and move events from the window system of the operating system over the
            next moments, and Swing lays out the whole window for each of them. If one of those
            events arrived while we check what hiding the middle component has scheduled, it would
            lay out the column no matter what, and the scenario would pass or fail depending on
            timing. So `showAndWaitUntilTheWindowHasSettled` waits until no such event has arrived
            for 200 milliseconds, and then lays the window out one final time.
        """
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(column)
                frame.pack()
            }
            showAndWaitUntilTheWindowHasSettled(frame)
            int middleY = UI.runAndGet({ middle.getY() })
            int belowY = UI.runAndGet({ below.getY() })
        expect : 'The middle component is a validate root, and the label "Below" starts out underneath it.'
            middle.isValidateRoot()
            belowY > middleY

        when : """
            Now we hide the middle component through the property. Then we let Swing work through
            everything this has queued up, the way the event loop of a real application would:
            `waitUntilSwingHasWorkedThroughItsQueue` calls `UI.runNow` three times in a row, which
            waits until every event posted before it has been handled, and inside it calls
            `RepaintManager.validateInvalidComponents()`, which carries out the layouts that
            `revalidate()` has queued up. It takes three rounds, because a layout can post a
            resize event, and handling that event can queue up another layout. We never call
            `validate()` on the window here, because that would lay out the whole window no matter
            what was queued, and so it would hide whether the column was ever scheduled for a new
            layout.
        """
            UI.runNow { isShown.set(false) }
            waitUntilSwingHasWorkedThroughItsQueue(column)
            int belowYAfterHiding = UI.runAndGet({ below.getY() })
        then : 'The label "Below" has moved up to where the middle component used to start.'
            belowYAfterHiding == middleY

        cleanup :
            UI.runNow { frame.dispose() }

        where : 'The middle component is one of these components, each of which is a validate root:'
            kind          | component
            "JTextField"  | { UI.textField("Some text") }
            "JScrollPane" | { UI.scrollPane().add(UI.label("Inside a scroll pane")) }
            "JSplitPane"  | { UI.splitPane(UI.Axis.HORIZONTAL).add(UI.label("Left")).add(UI.label("Right")) }
    }

    def 'Showing a #kind through `isVisibleIf` makes room for it in its parent.'( String kind, Closure<UIForAnySwing> component )
    {
        reportInfo """
            This scenario makes sure that showing a ${kind} through `isVisibleIf` looks the same as
            if the ${kind} had been visible from the start: the ${kind} gets at least its preferred
            height, and the components after it move down to make room for it.

            You might expect that to happen all by itself, and for a label or a plain panel it
            does. Showing a Swing component through `setVisible(true)` calls `revalidate()` on that
            component, and `revalidate()` schedules a new layout for the parent of the component,
            and for every container around that, up to the nearest *validate root*. A validate root
            is a component whose `isValidateRoot()` method returns `true`, which tells Swing that
            nothing inside it can change its size. A `JTextField`, a `JScrollPane` and a `JSplitPane`
            all return `true`. So when a ${kind} is shown, the nearest validate root is the ${kind}
            itself, and if SwingTree left this to Swing alone, Swing would lay out only what is
            inside the ${kind}. Its parent would never be laid out again, the ${kind} would keep the
            height of zero it had while it was hidden, and your users would not see it at all, until
            some unrelated change elsewhere in the window happened to lay the parent out.

            SwingTree prevents this: whenever `isVisibleIf` changes the visibility of a validate
            root, it also calls `revalidate()` on the parent of that validate root, so the parent
            is laid out again. (For any other component Swing already does this by itself.)
            SwingTree does the same when you change a size of a validate root through a property,
            for example through `withPrefHeight(Val)`.

            Here is a column holding the ${kind} before it is shown, what you see afterwards, and
            what your users would see if the column were not laid out again. In this diagram,
            `[validate root]` stands for a `JTextField`, a `JScrollPane` or a `JSplitPane`, because
            the scenario is run once for each of them:

            ```
              before showing         after showing            after showing, if the column
                                     (what you see)           were not laid out again
              ┌─────────────────┐    ┌─────────────────┐      ┌─────────────────┐
              │ Above           │    │ Above           │      │ Above           │
              │ Below           │    │ [validate root] │      │ Below           │ ← the validate root is
              │                 │    │ Below           │      │                 │   0 pixels high
              └─────────────────┘    └─────────────────┘      └─────────────────┘
            ```

            The scenario walks through exactly this picture. We build the column with the ${kind}
            hidden, show the column in a real window, show the ${kind} through its property, let
            Swing carry out whatever layouts this scheduled, and then check the height of the
            ${kind} and the position of the label "Below".
        """
        given : 'A UI scale of 1, so that the sizes in this scenario map one-to-one to pixels.'
            SwingTree.get().setUiScaleFactor(1f)
        and : 'A property which decides whether the middle component is visible. It starts out as `false`.'
            var isShown = Var.of(false)
        and : """
            A column of three rows: a label, the middle component bound to the property, and
            another label. The column is laid out by MigLayout with `hidemode 3`, which tells
            MigLayout to give an invisible component no room and no gaps at all. So while the
            middle component is hidden, it has a height of zero, and the label "Below" sits right
            where the middle component would start.
        """
            var middle = null
            var below = null
            var column =
                    UI.panel("wrap 1, hidemode 3")
                    .add(UI.label("Above"))
                    .add(component().isVisibleIf(isShown).peek(c -> middle = c))
                    .add(UI.label("Below").peek(c -> below = c))
                    .get(JPanel)
        and : """
            We show the column in a real window while the middle component is still hidden,
            because Swing only lays out components which sit inside a window that is on screen.
            A window which was just shown receives a few more resize and move events from the
            window system of the operating system over the next moments, and Swing lays out the
            whole window for each of them. If one of those events arrived while we check what
            showing the middle component has scheduled, it would lay out the column no matter
            what, and the scenario would pass or fail depending on timing. So
            `showAndWaitUntilTheWindowHasSettled` waits until no such event has arrived for
            200 milliseconds, and then lays the window out one final time.
        """
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(column)
                frame.setSize(300, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
            int belowY = UI.runAndGet({ below.getY() })
        expect : 'The middle component is a validate root, and it has no room in the column yet.'
            middle.isValidateRoot()
            UI.runAndGet({ middle.getHeight() }) == 0

        when : """
            Now we show the middle component through the property. Then we let Swing work through
            everything this has queued up, the way the event loop of a real application would:
            `waitUntilSwingHasWorkedThroughItsQueue` calls `UI.runNow` three times in a row, which
            waits until every event posted before it has been handled, and inside it calls
            `RepaintManager.validateInvalidComponents()`, which carries out the layouts that
            `revalidate()` has queued up. It takes three rounds, because a layout can post a
            resize event, and handling that event can queue up another layout. We never call
            `validate()` on the window here, because that would lay out the whole window no matter
            what was queued, and so it would hide whether the column was ever scheduled for a new
            layout.
        """
            UI.runNow { isShown.set(true) }
            waitUntilSwingHasWorkedThroughItsQueue(column)
            int middleHeight = UI.runAndGet({ middle.getHeight() })
            int preferredHeight = UI.runAndGet({ middle.getPreferredSize().height })
            int belowYAfterShowing = UI.runAndGet({ below.getY() })
        then : """
            The middle component has been given at least its preferred height. We say "at least",
            because MigLayout never makes a component smaller than its minimum size, and the
            minimum height of a `JScrollPane` can be a little larger than its preferred height.
        """
            preferredHeight > 0
            middleHeight >= preferredHeight
        and : 'The label "Below" has moved down by at least that height, to make room for the middle component.'
            belowYAfterShowing >= belowY + middleHeight

        cleanup :
            UI.runNow { frame.dispose() }

        where : 'The middle component is one of these components, each of which is a validate root:'
            kind          | component
            "JTextField"  | { UI.textField("Some text") }
            "JScrollPane" | { UI.scrollPane().add(UI.label("Inside a scroll pane")) }
            "JSplitPane"  | { UI.splitPane(UI.Axis.HORIZONTAL).add(UI.label("Left")).add(UI.label("Right")) }
    }

    def 'A scroll pane which is laid out at its preferred height grows as soon as its content grows.'()
    {
        reportInfo """
            This scenario makes sure that a scroll pane grows together with its content, in a
            layout which gives every row its preferred height. A scroll pane prefers to be as large
            as the content inside it plus its own border, so in such a layout the scroll pane is
            just as tall as its content. When the content grows, for example because a row of a
            `JTree` inside it was expanded or a list inside it got more items, the scroll pane
            grows with it, and the components below the scroll pane move down.

            You might expect that to happen all by itself, but plain Swing does not do it. A
            `JScrollPane` is a *validate root*: its `isValidateRoot()` method returns `true`, which
            tells Swing that nothing inside the scroll pane can change the size of the scroll pane.
            When the content calls `revalidate()`, Swing marks the content, the scroll pane and
            every container around it, all the way up to the window, as invalid, which means "needs
            a new layout". But it schedules a layout only for the scroll pane. If SwingTree left
            this to Swing alone, the scroll pane would keep its old height and show a vertical
            scroll bar, and the containers around it would stay marked as invalid with no layout
            scheduled for them. That is worse than it sounds: the new height would still arrive
            later, the next time anything else in the window calls `revalidate()`, and the scroll
            pane would jump at a moment which has nothing to do with its content. To your users the
            layout would look flaky.

            SwingTree prevents this: a scroll pane made by SwingTree keeps track of the size of its
            content, and whenever that content is given a new size, the scroll pane calls
            `revalidate()` on its own parent as well. Swing delivers the news of a new size as a
            resize event after the layout which caused it has finished, so the parent is laid out
            right after the scroll pane, and not in the middle of it.

            Here is a column holding a scroll pane before the content of the scroll pane grows,
            what you see afterwards, and what your users would see if the column were not laid out
            again:

            ```
              before growing             after growing              after growing, if the column
                                         (what you see)             were not laid out again
              ┌────────────────────────┐ ┌────────────────────────┐ ┌────────────────────────┐
              │┌──────────────────────┐│ │┌──────────────────────┐│ │┌────────────────────┬─┐│
              ││ Row 1                ││ ││ Row 1                ││ ││ Row 1              │▲││ ← old height,
              │└──────────────────────┘│ ││ Row 2                ││ │└────────────────────┴─┘│   rows 2 to 4 are
              │ Below the scroll pane  │ ││ Row 3                ││ │ Below the scroll pane  │   only reachable by
              │                        │ ││ Row 4                ││ │                        │   scrolling
              │                        │ │└──────────────────────┘│ │                        │
              │                        │ │ Below the scroll pane  │ │                        │
              └────────────────────────┘ └────────────────────────┘ └────────────────────────┘
            ```

            The scenario walks through exactly this picture. We build the column, show it in a real
            window, add three rows to the content of the scroll pane, let Swing carry out whatever
            events and layouts this scheduled, and then check the height of the scroll pane and the
            position of the label below it.
        """
        given : 'A UI scale of 1, so that the sizes in this scenario map one-to-one to pixels.'
            SwingTree.get().setUiScaleFactor(1f)
        and : """
            A column which gives every row its preferred height: a scroll pane holding a panel
            with one label, and a label below the scroll pane. The column is laid out by MigLayout
            without any row constraint, so no row grows beyond its preferred height.
        """
            var scrollPane = null
            var content = null
            var below = null
            var column =
                    UI.panel("wrap 1, ins 0", "[grow]")
                    .add("growx",
                        UI.scrollPane().peek(c -> scrollPane = c)
                        .add(
                            UI.panel("wrap 1, ins 0").peek(c -> content = c)
                            .add(UI.label("Row 1"))
                        )
                    )
                    .add(UI.label("Below the scroll pane").peek(c -> below = c))
                    .get(JPanel)
        and : """
            We show the column in a real window, because Swing only lays out components which
            sit inside a window that is on screen. The window is much taller than the column
            needs, so that there is room for the scroll pane to grow into.

            A window which was just shown receives a few more resize and move events from the
            window system of the operating system over the next moments, and Swing lays out the
            whole window for each of them. If one of those events arrived while we check what
            growing the content has scheduled, it would lay out the column no matter what, and
            the scenario would pass or fail depending on timing. So
            `showAndWaitUntilTheWindowHasSettled` waits until no such event has arrived for
            200 milliseconds, and then lays the window out one final time.

            That final layout matters for a second reason as well. When the window is shown for
            the first time, the content of the scroll pane gets its first size, and Swing posts a
            resize event for it. The `JViewport` of the scroll pane handles that event by calling
            `revalidate()` on itself, which marks the column as invalid but schedules a layout
            only for the scroll pane. The final layout makes sure that the column starts out fully
            laid out.
        """
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(column)
                frame.setSize(300, 500)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
            int startHeight = UI.runAndGet({ scrollPane.getHeight() })
            int belowY = UI.runAndGet({ below.getY() })
        expect : 'Swing has laid out everything in the column, so the column starts out valid.'
            UI.runAndGet({ column.isValid() })
            startHeight > 0

        when : """
            Now the content grows: we add three more labels to it and call `revalidate()` on it,
            which is what Swing components do themselves whenever their content changes. Then we
            let Swing work through everything this has queued up, the way the event loop of a real
            application would: `waitUntilSwingHasWorkedThroughItsQueue` calls `UI.runNow` three times in a row,
            which waits until every event posted before it has been handled, and inside it calls
            `RepaintManager.validateInvalidComponents()`, which carries out the layouts that
            `revalidate()` has queued up. It takes three rounds, because a layout can post a
            resize event, and handling that event can queue up another layout. We never call
            `validate()` on the window here, because that would lay out the whole window no matter
            what was queued, and so it would hide whether the column was ever scheduled for a new
            layout.
        """
            UI.runNow {
                content.add(new JLabel("Row 2"))
                content.add(new JLabel("Row 3"))
                content.add(new JLabel("Row 4"))
                content.revalidate()
            }
            waitUntilSwingHasWorkedThroughItsQueue(column)
            int grownHeight = UI.runAndGet({ scrollPane.getHeight() })
            int preferredHeight = UI.runAndGet({ scrollPane.getPreferredSize().height })
            int belowYAfterGrowing = UI.runAndGet({ below.getY() })
        then : 'The scroll pane now prefers to be taller than it was at the start, about four times as tall.'
            preferredHeight > startHeight
        and : 'It has been laid out at that new preferred height.'
            grownHeight == preferredHeight
        and : 'The label below the scroll pane has moved down by as much as the scroll pane grew.'
            belowYAfterGrowing == belowY + (grownHeight - startHeight)

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'An unrelated change in the window does not resize a scroll pane whose content grew earlier.'()
    {
        reportInfo """
            This scenario makes sure that a scroll pane whose content grew keeps its height when
            something unrelated changes elsewhere in the window. The size of a component on screen
            should change because something about that component changed, and never later because
            something else changed. Otherwise your users see a layout which seems to jump at random.

            You might expect that to hold all by itself, but plain Swing does not guarantee it. A
            `JScrollPane` is a *validate root*: its `isValidateRoot()` method returns `true`, which
            tells Swing that nothing inside the scroll pane can change the size of the scroll pane.
            When the content calls `revalidate()`, Swing marks the content, the scroll pane and
            every container around it, all the way up to the window, as invalid, which means "needs
            a new layout". But it schedules a layout only for the scroll pane. If SwingTree left
            this to Swing alone, the scroll pane would keep its old height, and the containers
            around it would stay marked as invalid with no layout scheduled for them. Then any
            later `revalidate()` elsewhere in the window, for example from a label which gets a new
            text, would make Swing lay out the whole window, including those invalid containers,
            and the scroll pane would suddenly jump to the height its content asked for earlier.

            SwingTree prevents this: a scroll pane made by SwingTree keeps track of the size of its
            content, and whenever that content is given a new size, the scroll pane calls
            `revalidate()` on its own parent as well. So the scroll pane reaches its new height
            right after its content grew, and a later, unrelated layout finds nothing left to
            change.

            Here is a column with a header label and a scroll pane, after the content of the
            scroll pane grew and after the text of the header label changed. First as you see it,
            and then as your users would see it if the column were not laid out when the content
            grew:

            ```
              what you see:
              after the content grew            after the header text changed
              ┌──────────────────────────────┐  ┌──────────────────────────────┐
              │ Header                       │  │ A header with a longer text  │
              │┌────────────────────────────┐│  │┌────────────────────────────┐│
              ││ Row 1                      ││  ││ Row 1                      ││
              ││ Row 2                      ││  ││ Row 2                      ││ ← same height
              ││ Row 3                      ││  ││ Row 3                      ││
              ││ Row 4                      ││  ││ Row 4                      ││
              │└────────────────────────────┘│  │└────────────────────────────┘│
              └──────────────────────────────┘  └──────────────────────────────┘

              if the column were not laid out when the content grew:
              after the content grew            after the header text changed
              ┌──────────────────────────────┐  ┌──────────────────────────────┐
              │ Header                       │  │ A header with a longer text  │
              │┌──────────────────────────┬─┐│  │┌────────────────────────────┐│
              ││ Row 1                    │▲││  ││ Row 1                      ││
              │└──────────────────────────┴─┘│  ││ Row 2                      ││ ← jumps to the
              │                              │  ││ Row 3                      ││   new height
              │                              │  ││ Row 4                      ││
              │                              │  │└────────────────────────────┘│
              └──────────────────────────────┘  └──────────────────────────────┘
            ```

            The scenario walks through exactly this picture. We build the column, show it in a real
            window, add three rows to the content of the scroll pane and note the height of the
            scroll pane, then change only the text of the header label, and check that the height
            of the scroll pane stayed the same.
        """
        given : 'A UI scale of 1, so that the sizes in this scenario map one-to-one to pixels.'
            SwingTree.get().setUiScaleFactor(1f)
        and : """
            A column which gives every row its preferred height: a header label, and a scroll
            pane holding a panel with one label. The column is laid out by MigLayout without any
            row constraint, so no row grows beyond its preferred height.
        """
            var header = null
            var scrollPane = null
            var content = null
            var column =
                    UI.panel("wrap 1, ins 0", "[grow]")
                    .add(UI.label("Header").peek(c -> header = c))
                    .add("growx",
                        UI.scrollPane().peek(c -> scrollPane = c)
                        .add(
                            UI.panel("wrap 1, ins 0").peek(c -> content = c)
                            .add(UI.label("Row 1"))
                        )
                    )
                    .get(JPanel)
        and : """
            We show the column in a real window, because Swing only lays out components which
            sit inside a window that is on screen. The window is much taller than the column
            needs, so that there is room for the scroll pane to grow into.

            A window which was just shown receives a few more resize and move events from the
            window system of the operating system over the next moments, and Swing lays out the
            whole window for each of them. If one of those events arrived while we check what
            our changes have scheduled, it would lay out the column no matter what, and the
            scenario would pass or fail depending on timing. So
            `showAndWaitUntilTheWindowHasSettled` waits until no such event has arrived for
            200 milliseconds, and then lays the window out one final time.

            That final layout matters for a second reason as well. When the window is shown for
            the first time, the content of the scroll pane gets its first size, and Swing posts a
            resize event for it. The `JViewport` of the scroll pane handles that event by calling
            `revalidate()` on itself, which marks the column as invalid but schedules a layout
            only for the scroll pane. The final layout makes sure that the column starts out fully
            laid out.
        """
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(column)
                frame.setSize(300, 500)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
        expect : 'Swing has laid out everything in the column, so the column starts out valid.'
            UI.runAndGet({ column.isValid() })

        when : """
            Now the content grows: we add three more labels to it and call `revalidate()` on it,
            which is what Swing components do themselves whenever their content changes. Then we
            let Swing work through everything this has queued up, the way the event loop of a real
            application would: `waitUntilSwingHasWorkedThroughItsQueue` calls `UI.runNow` three times in a row,
            which waits until every event posted before it has been handled, and inside it calls
            `RepaintManager.validateInvalidComponents()`, which carries out the layouts that
            `revalidate()` has queued up. It takes three rounds, because a layout can post a
            resize event, and handling that event can queue up another layout. We never call
            `validate()` on the window here, because that would lay out the whole window no matter
            what was queued, and so it would hide which layouts were actually scheduled.
        """
            UI.runNow {
                content.add(new JLabel("Row 2"))
                content.add(new JLabel("Row 3"))
                content.add(new JLabel("Row 4"))
                content.revalidate()
            }
            waitUntilSwingHasWorkedThroughItsQueue(column)
            int heightAfterGrowing = UI.runAndGet({ scrollPane.getHeight() })
        and : """
            We change only the text of the header label, and again let Swing work through
            everything this has queued up. Setting the text of a label calls `revalidate()` on
            the label, and the nearest validate root of a label in a window is the root pane of
            the window, so this makes Swing lay out the whole window.
        """
            UI.runNow { header.setText("A header with a longer text") }
            waitUntilSwingHasWorkedThroughItsQueue(column)
            int heightAfterHeaderChange = UI.runAndGet({ scrollPane.getHeight() })
        then : 'The scroll pane is exactly as tall as it was before the text of the header label changed.'
            heightAfterHeaderChange == heightAfterGrowing

        cleanup :
            UI.runNow { frame.dispose() }
    }

    private static void showAndWaitUntilTheWindowHasSettled( JFrame frame ) {
        var windowEvents = new AtomicInteger(0)
        var countWindowEvents = new ComponentAdapter() {
            @Override void componentResized( ComponentEvent e ) { windowEvents.incrementAndGet() }
            @Override void componentMoved( ComponentEvent e ) { windowEvents.incrementAndGet() }
        }
        UI.runNow {
            frame.addComponentListener(countWindowEvents)
            frame.setVisible(true)
            frame.validate()
        }
        int quietRounds = 0
        int eventsSeen = -1
        for ( int round = 0; round < 60 && quietRounds < 4; round++ ) {
            Thread.sleep(50)
            int eventsNow = UI.runAndGet({ windowEvents.get() })
            quietRounds = eventsNow == eventsSeen ? quietRounds + 1 : 0
            eventsSeen = eventsNow
        }
        UI.runNow {
            frame.removeComponentListener(countWindowEvents)
            frame.validate()
        }
        waitUntilSwingHasWorkedThroughItsQueue(frame.getRootPane())
        UI.runNow { frame.validate() }
    }

    private static void waitUntilSwingHasWorkedThroughItsQueue( JComponent anyComponentOfTheWindow ) {
        3.times {
            UI.runNow { RepaintManager.currentManager(anyComponentOfTheWindow).validateInvalidComponents() }
        }
    }
}

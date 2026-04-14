package swingtree

import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.Layout
import swingtree.layout.AddConstraint
import swingtree.layout.MigAddConstraint
import swingtree.layout.ResponsiveGridFlowLayout
import swingtree.components.JBox
import swingtree.layout.LayoutConstraint
import swingtree.threading.EventProcessor

import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import swingtree.layout.Bounds

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
    Internally, it combines `withRepaintOn(layout)` with `withStyle(it -> it.layout(layout.get()))`,
    which means:

      - Whenever the `layout` property fires a change event, the style is re-evaluated.
      - The re-evaluation calls `layout.get()` to pick up the latest `Layout` object.
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

            Internally, the style engine's repaint subscription fires when the property changes.
            This re-evaluates the style function `it -> it.layout(layout.get())`, producing a
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
            `java.awt.GridLayout`, and `Layout.box(UI.Axis)` installs a
            `javax.swing.BoxLayout`. Both are installed or replaced exactly like
            any other `Layout` implementation — just call `Var.set(...)` with the
            desired value.

            `GridLayout` exposes setters for all of its properties (rows, columns, and
            both gap sizes), so SwingTree can update an existing `GridLayout` instance
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

        when: 'We switch to a 2-row, 3-column GridLayout:'
            layout.set(Layout.grid(2, 3))
        then: 'A GridLayout is installed with exactly those dimensions:'
            panel.getLayout() instanceof GridLayout
            ((GridLayout) panel.getLayout()).getRows() == 2
            ((GridLayout) panel.getLayout()).getColumns() == 3

        when: 'We update the GridLayout to different dimensions — the type stays the same:'
            def firstGridLayout = panel.getLayout()
            layout.set(Layout.grid(3, 2))
        then: 'The row/column counts are updated in-place on the existing GridLayout instance:'
            panel.getLayout().is(firstGridLayout)
            ((GridLayout) panel.getLayout()).getRows() == 3
            ((GridLayout) panel.getLayout()).getColumns() == 2

        when: 'We switch to a horizontal BoxLayout:'
            layout.set(Layout.box(UI.Axis.X))
        then: 'A BoxLayout is installed along the X axis:'
            panel.getLayout() instanceof BoxLayout
            ((BoxLayout) panel.getLayout()).getAxis() == BoxLayout.X_AXIS

        when: 'We switch to a vertical BoxLayout — a new instance is created since the axis changed:'
            layout.set(Layout.box(UI.Axis.Y))
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
}
package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.layout.Bounds
import swingtree.layout.Position
import swingtree.layout.Size
import swingtree.threading.EventProcessor

import javax.swing.JPanel

@Title("Binding Bounds and Locations")
@Narrative('''

    SwingTree's declarative builder API exposes a family of methods for setting
    the *imperative* `setBounds(...)` / `setLocation(...)` properties of a
    `JComponent` directly from the builder, both as static one-shot values
    and as reactive `Val` properties that follow a view model.

    These methods are the natural complement of `Layout.none()` (and other
    layout managers that do not reposition their children): when a parent
    layout will not lay out the children for you, you typically still want
    to dictate **where** a child sits and **how big** it is. The methods
    in this specification are exactly that escape hatch, but lifted into
    SwingTree's declarative, reactive, and DPI-aware idiom.

    A central design decision here is that the supplied `Bounds`/`Position`
    values live in **developer pixel space**, not in raw component pixel
    space. Internally, SwingTree multiplies them by the active UI scale
    factor (`UI.scale()`) before forwarding them to the underlying
    `JComponent`. This keeps your view-model coordinates DPI-independent:
    the same model renders pixel-correctly at 1x, 1.5x, 2x, ... and is also
    re-applied automatically when the UI scale factor is changed at runtime.

    Each scenario in this specification is parameterised over a range of
    UI scale factors (1.0, 1.5, 2.0) so that the developer-pixel-space
    contract is verified end-to-end – not just on a single happy-path scale.

''')
@Subject([UIForAnySwing])
class Bounds_And_Location_Binding_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'A static `Bounds` value can be installed onto a component via `withBounds(Bounds)`.'(
        float uiScaleFactor
    ) {
        reportInfo """
            The `withBounds(Bounds)` method takes a single `Bounds` value, which is
            the algebraic union of a `Position` (top-left corner) and a `Size`
            (width, height). It is the imperative twin of `setBounds(...)`
            on the underlying `JComponent`, but expressed declaratively as
            part of the builder chain.

            Use it when you place a component into a parent that uses
            `Layout.none()` or any other layout that does not reposition
            its children, and you want the initial position-and-size to come
            from a single, atomic value instead of two separate calls to
            `withLocation(...)` and `withSize(...)`.

            Because the `Bounds` value lives in *developer pixel space*,
            we parameterise this scenario over a range of UI scale factors
            and verify that the rendered component dimensions scale linearly.
        """
        given : 'We pin the UI scale factor *before* we build the component:'
            SwingTree.get().setUiScaleFactor(uiScaleFactor)
        and   : 'A `Bounds` value describing where and how big the component should be.'
            var bounds = Bounds.of(40, 60, 120, 80)
        when : 'We build a panel and seed its bounds via `withBounds(Bounds)`:'
            var panel = UI.panel().withBounds(bounds).get(JPanel.class)
        then : 'The component\'s bounds match the supplied developer-pixel value, scaled by the UI factor.'
            panel.getX()      == Math.round(40  * uiScaleFactor)
            panel.getY()      == Math.round(60  * uiScaleFactor)
            panel.getWidth()  == Math.round(120 * uiScaleFactor)
            panel.getHeight() == Math.round(80  * uiScaleFactor)

        where :
            uiScaleFactor << [1.0f, 1.5f, 2.0f]
    }

    def 'A reactive `Val<Bounds>` keeps the component\'s bounds in sync with a view model.'(
        float uiScaleFactor
    ) {
        reportInfo """
            The `withBounds(Val<Bounds>)` overload is the reactive counterpart of
            `withBounds(Bounds)`. It is bound to a sprouts `Val` property and
            re-applies the bounds whenever that property changes.

            This is the canonical way to seed and update a component's location
            and size from a view model: the model owns the truth, the view
            simply mirrors it. No `peek(c -> c.setBounds(...))` boilerplate
            necessary – and importantly, the developer-pixel-space contract
            is preserved on every property change, not just on initial display.

            We parameterise over multiple UI scale factors to demonstrate that
            *both* the initial seeding *and* every subsequent reactive update
            honour the same DPI-aware contract.
        """
        given : 'A pinned UI scale factor:'
            SwingTree.get().setUiScaleFactor(uiScaleFactor)
        and   : 'A `Var<Bounds>` modeled as part of our (mock) view model:'
            Var<Bounds> bounds = Var.of(Bounds.of(10, 10, 50, 50))
        when : 'We bind the property to a panel via `withBounds(Val<Bounds>)`:'
            var panel = UI.panel().withBounds(bounds).get(JPanel.class)
        then : 'The component is initially seeded with the model\'s bounds, scaled by the UI factor.'
            panel.getBounds().x      == Math.round(10 * uiScaleFactor)
            panel.getBounds().y      == Math.round(10 * uiScaleFactor)
            panel.getBounds().width  == Math.round(50 * uiScaleFactor)
            panel.getBounds().height == Math.round(50 * uiScaleFactor)

        when : 'The view model produces an updated `Bounds` value (e.g. via a wither):'
            bounds.set(Bounds.of(100, 200, 300, 400))
            UI.sync()
        then : 'The component\'s bounds reactively follow the property update, still in developer pixel space.'
            panel.getBounds().x      == Math.round(100 * uiScaleFactor)
            panel.getBounds().y      == Math.round(200 * uiScaleFactor)
            panel.getBounds().width  == Math.round(300 * uiScaleFactor)
            panel.getBounds().height == Math.round(400 * uiScaleFactor)

        where :
            uiScaleFactor << [1.0f, 1.5f, 2.0f]
    }

    def 'A static `Position` value can be installed via `withLocation(Position)` without affecting the size.'(
        float uiScaleFactor
    ) {
        reportInfo """
            `withLocation(Position)` is the location-only counterpart of
            `withSize(Size)`: it moves the component's top-left corner to
            the supplied position but leaves its width and height untouched.
            Use it when you want to dictate *where* a component sits but
            its size should keep coming from elsewhere – e.g. from
            `withSize(...)`, `withPrefSize(...)` or an automatic computation.

            Because both `withSize(...)` and `withLocation(...)` interpret
            their arguments in developer pixel space, they compose cleanly
            under any UI scale factor.
        """
        given : 'A pinned UI scale factor:'
            SwingTree.get().setUiScaleFactor(uiScaleFactor)
        and   : 'A panel with an initial size and a separate location property:'
            var panel = UI.panel()
                            .withSize(80, 40)
                            .withLocation(Position.of(120, 30))
                            .get(JPanel.class)
        expect : 'The size came from `withSize(...)`, scaled by the UI factor:'
            panel.getWidth()  == Math.round(80 * uiScaleFactor)
            panel.getHeight() == Math.round(40 * uiScaleFactor)
        and    : 'and the location came from `withLocation(Position)`, scaled by the same UI factor.'
            panel.getX() == Math.round(120 * uiScaleFactor)
            panel.getY() == Math.round(30  * uiScaleFactor)

        where :
            uiScaleFactor << [1.0f, 1.5f, 2.0f]
    }

    def 'A reactive `Val<Position>` keeps the component\'s location in sync with a view model.'(
        float uiScaleFactor
    ) {
        reportInfo """
            `withLocation(Val<Position>)` reactively mirrors a `Val<Position>`
            into the component's `setLocation(...)`. It is the perfect
            companion for drag-and-drop interactions in a `Layout.none()`
            container: drag updates the property, the property updates the
            component – no imperative bookkeeping in the view.

            The DPI-aware contract again applies *both* on initial seeding
            *and* on every reactive update, which is exactly what we
            demonstrate by sweeping the UI scale factor.
        """
        given : 'A pinned UI scale factor:'
            SwingTree.get().setUiScaleFactor(uiScaleFactor)
        and   : 'A reactive position property modelled as part of our view model:'
            Var<Position> position = Var.of(Position.of(0, 0))
        and   : 'A panel of fixed size whose location is bound to that property:'
            var panel = UI.panel()
                            .withSize(60, 60)
                            .withLocation(position)
                            .get(JPanel.class)
        expect : 'The panel is initially located at the position carried by the property,'
            panel.getX() == 0
            panel.getY() == 0
        and    : 'and its size came from `withSize(...)`, scaled by the UI factor.'
            panel.getWidth()  == Math.round(60 * uiScaleFactor)
            panel.getHeight() == Math.round(60 * uiScaleFactor)

        when : 'We update the position property (e.g. as part of a drag):'
            position.set(Position.of(75, 125))
            UI.sync()
        then : 'The panel reactively moves to the new location, in developer pixel space:'
            panel.getX() == Math.round(75  * uiScaleFactor)
            panel.getY() == Math.round(125 * uiScaleFactor)
        and  : 'its size is unaffected.'
            panel.getWidth()  == Math.round(60 * uiScaleFactor)
            panel.getHeight() == Math.round(60 * uiScaleFactor)

        where :
            uiScaleFactor << [1.0f, 1.5f, 2.0f]
    }

    def 'A `Val<Position>` update only moves the component – width and height stay put.'(
        float uiScaleFactor
    ) {
        reportInfo """
            This is a regression-style guard for the documented intent:
            `withLocation(...)` is *strictly* a location concern. Even when
            the bound `Val<Position>` changes, the component's size is
            never touched. This makes the method composable with
            independent size sources like `withPrefSize(...)` or
            `withSize(...)`.

            We sweep the UI scale factor to make sure this invariant holds
            on every supported DPI tier, not just on a 1.0 pass-through.
        """
        given : 'A pinned UI scale factor:'
            SwingTree.get().setUiScaleFactor(uiScaleFactor)
        and   : 'A panel sized via `withSize(...)` and located via a reactive property:'
            Var<Position> position = Var.of(Position.of(10, 10))
            var panel = UI.panel()
                            .withSize(Size.of(100, 50))
                            .withLocation(position)
                            .get(JPanel.class)

        when : 'We move the panel several times via the property:'
            position.set(Position.of(50, 60));   UI.sync()
            position.set(Position.of(200, 300)); UI.sync()
        then : 'The size never changes (still the originally scaled `withSize(...)`):'
            panel.getWidth()  == Math.round(100 * uiScaleFactor)
            panel.getHeight() == Math.round(50  * uiScaleFactor)
        and  : 'but the location follows the latest property value, in developer pixel space.'
            panel.getX() == Math.round(200 * uiScaleFactor)
            panel.getY() == Math.round(300 * uiScaleFactor)

        where :
            uiScaleFactor << [1.0f, 1.5f, 2.0f]
    }
}
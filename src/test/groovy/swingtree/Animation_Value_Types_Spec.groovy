package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.animation.Animatable
import swingtree.animation.AnimationStatus
import swingtree.animation.LifeSpan
import swingtree.animation.LifeTime
import swingtree.animation.Progress
import swingtree.animation.Stride

import java.awt.event.ActionEvent
import java.util.concurrent.TimeUnit

@Title("Animation Value Types")
@Narrative('''

    The animation API of SwingTree is centered around a small family of
    immutable, value based types that you typically combine to describe
    *what* should be animated and *how* it should progress over time.

    The most fundamental one is the `Progress` interface, which models
    a single animation snapshot as a number between `0` and `1` and
    exposes a number of pure-function methods to interpret that number
    in different ways (linearly, sinusoidally, oscillating, etc.).
    On top of that we have `LifeTime`, which describes the *duration*,
    *start delay* and *refresh interval* of an animation, and `Stride`
    which decides whether the progress runs forward (`PROGRESSIVE`) or
    backward (`REGRESSIVE`).

    Finally there is `Animatable`, an immutable wrapper that combines
    a `LifeTime` with a transformation function and an optional initial
    state. It is the recommended way of describing animations on top of
    immutable view models.

    This specification describes the contracts of these value types in
    isolation, without involving any actual `javax.swing.Timer` or UI
    component. The goal is to act as a living reference for what these
    objects guarantee about their state and behaviour.

''')
@Subject([Progress, LifeTime, LifeSpan, Stride, AnimationStatus, Animatable])
class Animation_Value_Types_Spec extends Specification
{
    /**
     *  Approximate-equality helper for the floating point math
     *  that backs the various `Progress` curves. We use a tight
     *  tolerance because all the formulas under test are exact.
     */
    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0e-9d
    }

    /**
     *  Lifts a fixed `double` into a `Progress` instance by constructing
     *  a real `AnimationStatus` whose start/duration/interval are chosen
     *  so that `progress()` deterministically returns the requested value.
     *  We deliberately go through the public `AnimationStatus` API instead
     *  of implementing `Progress` ad-hoc, because `Progress` is intended
     *  to remain a closed interface implemented only by `AnimationStatus`
     *  and its `slice(...)` view.
     */
    private static Progress progressOf(double value) {
        long duration = 1000L
        long interval = 1L  // 1000 steps -> any value in [0, 1] is exactly representable
        var lifeTime  = LifeTime.of(0, TimeUnit.MILLISECONDS, duration, TimeUnit.MILLISECONDS)
                                .withInterval(interval, TimeUnit.MILLISECONDS)
        var lifeSpan  = LifeSpan.startingNowWith(lifeTime)
        var event     = new ActionEvent("test", 0, "tick")
        if ( value >= 1.0d )
            return AnimationStatus.endOf(lifeSpan, Stride.PROGRESSIVE, event, 1L)
        long offset = Math.round(value * duration)
        long now    = lifeSpan.getStartTimeIn(TimeUnit.MILLISECONDS) + offset
        return AnimationStatus.of(lifeSpan, Stride.PROGRESSIVE, event, now)
    }

    def 'A `Progress` of `0.5` produces the documented values for all curve methods.'()
    {
        reportInfo """
            The `Progress` interface exposes a family of pure-function methods
            that interpret a single `progress()` value (a `double` between
            `0` and `1`) as different animation curves:

            - `regress()` is the linear inverse, going from `1` to `0`.
            - `pulse()` is a sine wave going from `0` to `1` and back to `0`.
            - `jumpIn()` and `jumpOut()` are quarter-sine curves.
            - `fadeIn()` and `fadeOut()` are smooth half-cosine curves.
            - `cycle()` oscillates between `0`, `1` and `0` over the lifetime.

            At the exact midpoint of the animation, all of these have
            well known, easy to verify values. We pin them down here so
            that any change to these formulas is an explicit, conscious one.
        """
        given : 'A progress snapshot positioned exactly at the halfway point.'
            var p = progressOf(0.5d)

        expect : 'The linear progress and its inverse are exactly `0.5`.'
            p.progress() == 0.5d
            p.regress()  == 0.5d
        and : 'The pulse, which is `sin(PI * progress)`, peaks at `1`.'
            closeTo(p.pulse(), 1.0d)
        and : 'Both `jumpIn` and `jumpOut` evaluate to `sin(PI/4)` at the midpoint.'
            closeTo(p.jumpIn(),  Math.sin(Math.PI / 4d))
            closeTo(p.jumpOut(), Math.sin(Math.PI / 4d))
        and : 'The smooth `fadeIn`/`fadeOut` curve crosses `0.5` exactly at the middle.'
            closeTo(p.fadeIn(),  0.5d)
            closeTo(p.fadeOut(), 0.5d)
        and : 'The `cycle` reaches its peak of `1.0` at the halfway point.'
            closeTo(p.cycle(), 1.0d)
    }

    def 'The `Progress` curve functions agree on their boundary values.'(
            double progress, double regress, double pulse, double jumpIn, double jumpOut, double fadeIn, double fadeOut, double cycle
    ) {
        reportInfo """
            All curves share the same start and end *anchors*: at `progress() == 0`
            the animation has not yet started, at `progress() == 1` the animation
            is finished. The various curves only differ in *how* they
            travel between these anchors.

            This data driven test pins down what the value of every curve
            method must be at the start, end and midpoint of an animation.
            These guarantees are important because they make animation curves
            composable: regardless of which curve you pick, the start and
            end values of your animation will line up cleanly.
        """
        given : 'A `Progress` snapshot positioned at the given progress value.'
            var p = progressOf(progress)
        expect :
            closeTo(p.progress(), progress)
            closeTo(p.regress(),  regress)
            closeTo(p.pulse(),    pulse)
            closeTo(p.jumpIn(),   jumpIn)
            closeTo(p.jumpOut(),  jumpOut)
            closeTo(p.fadeIn(),   fadeIn)
            closeTo(p.fadeOut(),  fadeOut)
            closeTo(p.cycle(),    cycle)
        where :
            progress || regress | pulse | jumpIn                  | jumpOut                 | fadeIn | fadeOut | cycle
            0.0d     || 1.0d    | 0.0d  | 0.0d                    | 1.0d                    | 0.0d   | 1.0d    | 0.0d
            0.5d     || 0.5d    | 1.0d  | Math.sin(Math.PI / 4d)  | Math.sin(Math.PI / 4d)  | 0.5d   | 0.5d    | 1.0d
            1.0d     || 0.0d    | 0.0d  | 1.0d                    | 0.0d                    | 1.0d   | 0.0d    | 0.0d
    }

    def 'The ranged variants of the `Progress` curves linearly map onto the supplied `start..end` interval.'()
    {
        reportInfo """
            Most of the curve methods on `Progress` come in two flavours:
            a no-arg version that returns a value in `[0, 1]`, and a
            ranged version that linearly maps that value onto a custom
            `[start, end]` interval. This lets you write things like
            `status.fadeIn(10, 200)` to fade from `10` pixels to `200`
            pixels along a smooth curve.

            This test pins down the contract that the ranged variants
            are *exactly* `start + (end - start) * curve()`, even when
            `start > end` (i.e. when the range is reversed).
        """
        given : 'A progress snapshot positioned a quarter into the animation.'
            var p = progressOf(0.25d)

        expect : '`progress(start, end)` linearly interpolates between the two values.'
            closeTo(p.progress(10d, 110d), 35d)
        and : '`regress(a, b)` is equivalent to `progress(a, b)` (it inverts the *curve*, not the argument order).'
            closeTo(p.regress(10d, 110d), p.progress(10d, 110d))
        and : 'The pulse, jumpIn, jumpOut and fade variants all use the same `start + (end-start)*curve` formula.'
            closeTo(p.pulse(0d, 100d),   100d * Math.sin(Math.PI * 0.25d))
            closeTo(p.jumpIn(0d, 100d),  100d * Math.sin(Math.PI * 0.25d / 2d))
            closeTo(p.jumpOut(0d, 100d), 100d * Math.sin(Math.PI * 0.75d / 2d))
            closeTo(p.fadeIn(0d, 100d),  50d * (1d + Math.sin(Math.PI * (0.25d - 0.5d))))
        and : 'A reversed range `start > end` simply reverses the direction of the curve.'
            closeTo(p.progress(100d, 0d), 75d)
            closeTo(p.fadeIn(100d, 0d),   100d - p.fadeIn(0d, 100d))
    }

    def 'The `cyclePlus` and `cycleMinus` methods offset the `cycle` wave but stay in `[0, 1]`.'()
    {
        reportInfo """
            The plain `cycle()` method oscillates between `0`, `1` and `0`
            over a single animation iteration. The `cyclePlus(offset)` and
            `cycleMinus(offset)` variants do the same, but shift the wave
            forwards or backwards by the given `offset` (the offset is
            taken modulo `1`, so any real number is valid).

            This is useful for staggering several animations so they
            reach their peak at different moments.
        """
        given : 'A progress snapshot at `0.25`, where `cycle()` is exactly `0.5`.'
            var p = progressOf(0.25d)

        expect : 'The plain `cycle` is half way up the wave.'
            closeTo(p.cycle(), 0.5d)
        and : 'Adding an offset of `0.25` shifts the wave forward to its peak.'
            closeTo(p.cyclePlus(0.25d), 1.0d)
        and : 'Subtracting an offset of `0.25` shifts the wave backward to the start.'
            closeTo(p.cycleMinus(0.25d), 0.0d)
        and : 'Any offset, no matter how large or negative, still produces values in `[0, 1]`.'
            var offsets = (-30..30).collect({ it * 0.1d })
            offsets.every({ double off ->
                double up   = p.cyclePlus(off)
                double down = p.cycleMinus(off)
                up >= 0d && up <= 1d && down >= 0d && down <= 1d
            })
    }

    def 'The two `Stride` constants are the only valid directions, and `inverse()` swaps them.'()
    {
        reportInfo """
            A `Stride` describes the *direction* of an animation:
            `PROGRESSIVE` runs the progress from `0` to `1`, `REGRESSIVE`
            runs it from `1` to `0`. There are no other directions, and
            calling `inverse()` simply swaps the two.

            The `applyTo(progress)` method translates an externally
            supplied progress value through the stride: it returns the
            value unchanged for `PROGRESSIVE` and inverts it for `REGRESSIVE`.
        """
        expect : 'There are exactly two stride values.'
            Stride.values().toList() == [Stride.PROGRESSIVE, Stride.REGRESSIVE]
        and : '`inverse()` is a self-inverse operation that swaps the two values.'
            Stride.PROGRESSIVE.inverse() == Stride.REGRESSIVE
            Stride.REGRESSIVE.inverse()  == Stride.PROGRESSIVE
            Stride.PROGRESSIVE.inverse().inverse() == Stride.PROGRESSIVE
        and : '`PROGRESSIVE.applyTo(p)` returns `p` unchanged, `REGRESSIVE.applyTo(p)` returns `1 - p`.'
            Stride.PROGRESSIVE.applyTo(0.0d) == 0.0d
            Stride.PROGRESSIVE.applyTo(0.4d) == 0.4d
            Stride.PROGRESSIVE.applyTo(1.0d) == 1.0d
            Stride.REGRESSIVE.applyTo(0.0d)  == 1.0d
            closeTo(Stride.REGRESSIVE.applyTo(0.4d), 0.6d)
            Stride.REGRESSIVE.applyTo(1.0d)  == 0.0d
    }

    def 'A `LifeTime` is an immutable value object whose getters honour the requested `TimeUnit`.'()
    {
        reportInfo """
            `LifeTime` is one of the central building blocks of the SwingTree
            animation API. It is fully immutable and value based, meaning
            two life times constructed with the same delay, duration and
            interval are considered equal regardless of which factory
            method created them.

            The various `getXxxIn(TimeUnit)` accessors do *not* mutate the
            life time, they merely convert the internally stored milliseconds
            into the requested unit.
        """
        given : 'A life time describing a `2` second animation that starts after a `500` ms delay.'
            var lifeTime = LifeTime.of(500, TimeUnit.MILLISECONDS, 2, TimeUnit.SECONDS)

        expect : 'The duration is reported correctly across different time units.'
            lifeTime.getDurationIn(TimeUnit.MILLISECONDS) == 2_000L
            lifeTime.getDurationIn(TimeUnit.SECONDS)      == 2L
        and : 'The same is true for the start delay.'
            lifeTime.getDelayIn(TimeUnit.MILLISECONDS) == 500L
            lifeTime.getDelayIn(TimeUnit.SECONDS)      == 0L // 500 ms truncates to 0 seconds
        and : 'Two life times built from the same numbers in different units are equal.'
            lifeTime == LifeTime.of(500, TimeUnit.MILLISECONDS, 2_000, TimeUnit.MILLISECONDS)
            lifeTime.hashCode() == LifeTime.of(500, TimeUnit.MILLISECONDS, 2_000, TimeUnit.MILLISECONDS).hashCode()
        and : 'A life time with a different duration is *not* equal, even if the delay matches.'
            lifeTime != LifeTime.of(500, TimeUnit.MILLISECONDS, 3, TimeUnit.SECONDS)
        and : 'The `toString()` representation contains the canonical fields.'
            lifeTime.toString().contains("delay=500")
            lifeTime.toString().contains("duration=2000")
    }

    def 'The `withInterval` and `startingIn` methods of `LifeTime` are non-mutating "withers".'()
    {
        reportInfo """
            Like all SwingTree value types, `LifeTime` follows the wither
            pattern: methods like `withInterval(...)` and `startingIn(...)`
            return a *new* `LifeTime` rather than modifying the existing one.

            This guarantees that a `LifeTime` instance can be safely shared
            across threads and reused as a configuration template without
            risk of accidental mutation.
        """
        given : 'A baseline life time of `1` second with the configured default refresh interval.'
            var original = LifeTime.of(1, TimeUnit.SECONDS)
            var originalInterval = original.getIntervalIn(TimeUnit.MILLISECONDS)

        when : 'We derive a new life time with a custom `4` ms interval.'
            var faster = original.withInterval(4, TimeUnit.MILLISECONDS)
        then : 'The derived life time has the new interval.'
            faster.getIntervalIn(TimeUnit.MILLISECONDS) == 4L
        and : 'The original life time is *unchanged*.'
            original.getIntervalIn(TimeUnit.MILLISECONDS) == originalInterval

        when : 'We derive a new life time that starts after a `200` ms delay.'
            var delayed = original.startingIn(200, TimeUnit.MILLISECONDS)
        then : 'The derived life time carries the new delay but the same duration.'
            delayed.getDelayIn(TimeUnit.MILLISECONDS)    == 200L
            delayed.getDurationIn(TimeUnit.MILLISECONDS) == 1_000L
        and : 'The original life time still has no delay.'
            original.getDelayIn(TimeUnit.MILLISECONDS) == 0L
    }

    def 'The `LifeTime.none()` constant represents a no-op animation with zero duration, delay and interval.'()
    {
        reportInfo """
            `LifeTime.none()` is a shared singleton meant to be used wherever
            a `null` `LifeTime` would otherwise be tempting. It explicitly
            communicates "no animation" and avoids null-checks throughout
            the API surface.
        """
        given : 'The shared "no-op" lifetime constant.'
            var none = LifeTime.none()

        expect : 'All three of its time aspects are zero.'
            none.getDelayIn(TimeUnit.MILLISECONDS)    == 0L
            none.getDurationIn(TimeUnit.MILLISECONDS) == 0L
            none.getIntervalIn(TimeUnit.MILLISECONDS) == 0L
        and : 'It is equal to itself and any other zero-valued life time built explicitly.'
            none == LifeTime.none()
            none == LifeTime.of(0, TimeUnit.MILLISECONDS, 0, TimeUnit.MILLISECONDS).withInterval(0, TimeUnit.MILLISECONDS)
    }

    def 'The `Animatable.none()` value is a shared, type-agnostic, no-op animatable.'()
    {
        reportInfo """
            `Animatable.none()` is the recommended replacement for `null`
            whenever an `Animatable` is expected but no animation should
            actually run. Because it does not carry any state of its own,
            the same instance is reused for every type parameter.
        """
        given : 'Two type parametrized references to the no-op animatable.'
            Animatable<String>  noneA = Animatable.none()
            Animatable<Integer> noneB = Animatable.none()

        expect : 'They are the *same* shared instance, regardless of the type parameter.'
            noneA.is(noneB)
        and : 'Their lifetime is `LifeTime.none()`, signalling that nothing will be scheduled.'
            noneA.lifeTime() == LifeTime.none()
        and : 'They carry no initial state.'
            !noneA.initialState().isPresent()
    }

    def 'An `Animatable` built from a single value behaves like an instantaneous "set this state" command.'()
    {
        reportInfo """
            `Animatable.of(value)` is a convenience for the common case where
            you simply want to *jump* to a new model state without any actual
            animation in between. The lifetime is `LifeTime.none()` and the
            transformation is an identity function that always returns the
            supplied initial value.

            This is useful when a view model returns an `Animatable` from a
            method but, depending on the situation, may not actually want
            anything to be animated, just the state changed.
        """
        given : 'A target model value that we want to "animate" to.'
            var target = "Hello, World!"

        when : 'We wrap it in an `Animatable`.'
            var animatable = Animatable.of(target)
        then : 'The lifetime is the no-op lifetime, so no scheduler will ever tick.'
            animatable.lifeTime() == LifeTime.none()
        and : 'The supplied value becomes the (single) initial state.'
            animatable.initialState().isPresent()
            animatable.initialState().get() == target

        when : 'We try to wrap a `null` value, which is meaningless for this factory.'
            Animatable.of(null)
        then : 'A `NullPointerException` is raised, signalling the misuse early.'
            thrown(NullPointerException)
    }

    def 'An `AnimationStatus` reports its progress, repetition count, life span and event.'()
    {
        reportInfo """
            `AnimationStatus` is the concrete `Progress` implementation that
            the animation scheduler hands to your `Animation` callbacks
            on every tick. Beyond just `progress()`, it also exposes:

            - `repeats()` — the number of *completed* loops so far.
            - `lifeSpan()` — the `LifeSpan` driving this status.
            - `event()` — the underlying timer `ActionEvent`.

            Two statuses built from the same numbers must be considered
            equal; their `toString()` representation should reflect every
            constituent field so that logs and debug traces remain useful.
        """
        given : 'A 200 ms life span and a deterministic timer event.'
            var lifeTime = LifeTime.of(0, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS)
                                   .withInterval(1, TimeUnit.MILLISECONDS)
            var span     = LifeSpan.startingNowWith(lifeTime)
            var event    = new ActionEvent("source", 0, "tick")

        when : 'We ask for the status one quarter into the very first iteration.'
            var status = AnimationStatus.of(span, Stride.PROGRESSIVE, event, span.getStartTimeIn(TimeUnit.MILLISECONDS) + 50)
        then : 'It reports `progress=0.25`, no completed repeats, and exposes its life span and event.'
            closeTo(status.progress(), 0.25d)
            status.repeats()  == 0L
            status.lifeSpan() == span
            status.event()    .is(event)
        and : 'Its `toString` mentions every field so it is useful in debug traces.'
            status.toString().contains("progress=")
            status.toString().contains("repeats=")
            status.toString().contains("lifeSpan=")
            status.toString().contains("event=")
        and : 'A second status built from the same arguments is equal and shares its hash code.'
            var twin = AnimationStatus.of(span, Stride.PROGRESSIVE, event, span.getStartTimeIn(TimeUnit.MILLISECONDS) + 50)
            status == twin
            status.hashCode() == twin.hashCode()
    }

    def 'A `Stride.REGRESSIVE` `AnimationStatus` runs progress backwards from `1` to `0`.'()
    {
        reportInfo """
            The `Stride` of an `AnimationStatus` controls the *direction*
            of the progress value. A `REGRESSIVE` status produces the same
            magnitudes as a `PROGRESSIVE` one, but with `1 - p` instead of `p`.

            This is the building block behind effects like fade-outs, where
            the same animation logic should run "in reverse".
        """
        given : 'A 200 ms life span and a deterministic event.'
            var lifeTime = LifeTime.of(0, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS)
                                   .withInterval(1, TimeUnit.MILLISECONDS)
            var span     = LifeSpan.startingNowWith(lifeTime)
            var event    = new ActionEvent("source", 0, "tick")
            long start   = span.getStartTimeIn(TimeUnit.MILLISECONDS)

        expect : 'At the very start, the regressive progress is `1`.'
            closeTo(AnimationStatus.startOf(span, Stride.REGRESSIVE, event).progress(), 1.0d)
        and : 'A quarter through the iteration, the regressive progress is `0.75`.'
            closeTo(AnimationStatus.of(span, Stride.REGRESSIVE, event, start + 50).progress(), 0.75d)
        and : 'And at the very end, the regressive progress drops to `0`.'
            closeTo(AnimationStatus.endOf(span, Stride.REGRESSIVE, event, 1L).progress(), 0.0d)
    }

    def 'The `startOf` and `endOf` factories of `AnimationStatus` describe the boundaries of an iteration.'()
    {
        reportInfo """
            `AnimationStatus.startOf(...)` constructs the snapshot at the
            *very beginning* of an iteration (`progress == 0`, `repeats == 0`),
            while `endOf(..., iteration)` constructs the snapshot at the
            *very end* of the requested iteration (`progress == 1`, `repeats == iteration`).

            These two factories are how the `AnimationRunner` guarantees
            that an animation always observes both its `0` and its `1`
            anchor, regardless of how the timer ticks land in real time.
        """
        given :
            var lifeTime = LifeTime.of(0, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS)
                                   .withInterval(1, TimeUnit.MILLISECONDS)
            var span     = LifeSpan.startingNowWith(lifeTime)
            var event    = new ActionEvent("source", 0, "tick")

        when : 'We snapshot the start of the animation.'
            var atStart = AnimationStatus.startOf(span, Stride.PROGRESSIVE, event)
        then : 'It is positioned exactly at the beginning, with no completed repeats.'
            atStart.progress() == 0.0d
            atStart.repeats()  == 0L

        when : 'We snapshot the end of the third iteration (zero indexed).'
            var atEnd = AnimationStatus.endOf(span, Stride.PROGRESSIVE, event, 3L)
        then : 'The progress is exactly `1` and the iteration count matches what we asked for.'
            atEnd.progress() == 1.0d
            atEnd.repeats()  == 3L
    }

    def 'The `slice(from, to)` method of `AnimationStatus` clamps progress to the requested sub-range.'()
    {
        reportInfo """
            The `slice(from, to)` method on `AnimationStatus` returns a sub
            `Progress` view whose `progress()` value is the original progress
            *clamped* to the `[from, to]` interval. This means that:

            - Outside of the `[from, to]` range, the sub-progress is pinned
              to whichever boundary the animation has not yet crossed.
            - Inside the range, the sub-progress equals the original progress.
            - Slicing the full `[0, 1]` range is a no-op and the receiver
              is returned unchanged, avoiding pointless allocations.

            This is useful for chaining several distinct effects onto a
            single underlying animation timeline.
        """
        given : 'A 200 ms life span we will sample at different points in time.'
            var lifeTime = LifeTime.of(0, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS)
                                   .withInterval(1, TimeUnit.MILLISECONDS)
            var span     = LifeSpan.startingNowWith(lifeTime)
            var event    = new ActionEvent("source", 0, "tick")
            long start   = span.getStartTimeIn(TimeUnit.MILLISECONDS)

        when : 'We position the status before the slice (`progress = 0.1`) and slice `[0.5, 1.0]`.'
            var beforeSlice = AnimationStatus.of(span, Stride.PROGRESSIVE, event, start + 20).slice(0.5d, 1.0d)
        then : 'The sub-progress is clamped *up* to the lower bound of the slice.'
            closeTo(beforeSlice.progress(), 0.5d)

        when : 'We position the status inside the slice (`progress = 0.75`) and slice `[0.5, 1.0]`.'
            var insideSlice = AnimationStatus.of(span, Stride.PROGRESSIVE, event, start + 150).slice(0.5d, 1.0d)
        then : 'The sub-progress matches the underlying progress, no remapping occurs.'
            closeTo(insideSlice.progress(), 0.75d)

        when : 'We slice the full `[0, 1]` range, which is identified as a no-op.'
            var full = AnimationStatus.startOf(span, Stride.PROGRESSIVE, event)
            var sameAsFull = full.slice(0.0d, 1.0d)
        then : 'The slice returns the *exact* underlying status instance.'
            sameAsFull.is(full)
    }
}
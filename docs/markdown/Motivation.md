
# Motivation #

Here at [Global TCAD Solutions](https://www.globaltcad.com/) we 
make extensive use of the Java's Swing library for our desktop based solutions.

Although raw Swing is a stable, tried and tested cross-platform
desktop GUI framework, we have made the observation that it is still 
a tedious task to write GUI code with it
as it is unfortunately lacking a modern API and support for
important design patterns that are crucial for fast as well as maintainable development.
There certainly is a reason why development with Swing is dreaded among many developers,
especially those hip youngsters who got spoiled with the newer stuff. :)

To compensate for these various deficits we developed **[SwingTree](https://github.com/globaltcad/swing-tree)**, which allows us to write Swing GUIs in a declarative manner.
The goal is to give us the ability to develop our Swing based application
with the same ease as it is possible in other modern GUI frameworks, like [Jetpack Compose](https://developer.android.com/jetpack/compose), [SwiftUI](https://developer.apple.com/xcode/swiftui/) or [Flutter](https://flutter.dev).

We are actually not the first company with that idea, **JetBrains, has their own
Kotlin based wrapper framework for their various IDEs**!
Checkout their [JetBrain's UI DSL](https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics) library.
(Yes, believe it or not, all the JetBrain IDEs are actually Swing applications)

---

## The Problem with Building UIs ##

Graphical user interfaces, are like many
concepts in the field of computer science
just tree structures based on simple UI
components as tree nodes.
This is a very well proven concept, and it shows:
**HTML dominates the web and consequently most of the GUIs we interact with on a daily basis.**

This success story was so impactful that it even lead to the **creation of 
custom xml like file formats** introduced by desktop application frameworks 
emerging after the success of HTML, think of **`FXML` in JavaFX, `XAML` in WPF**.
However, this was a bad move, as it **missed the point as to why HTML succeeded**.

HTML was so successful merely because it's syntax made it so easy to construct the UIs tree like structure.
But it may have failed in gathering traction, if it was not for combining it with a turing complete scripting 
language, **JavaScript embedded into HTML**. <br>
Without JavaScript, HTML would have been just another static file format,
and it would have never gained the popularity it has today.

## Simply Using Declarative Code #

Based on the observation that UIs are just tree like data structures,
we can conclude that declarative syntax is the way to go for building UIs.
So you might think that XML like formats are superior to alternatives, but they are not!

Modern programming languages have evolved to the point where they allow
for the design of declarative APIs, which are just as concise as XML like formats.
They support nesting and the ability to assemble things
in a declarative manner when the right design patterns are applied.

The logical question that follows:
Why should we use two different file formats, when we can just use one, the programming language itself?
All it takes is the builder pattern (method chaining)
and some statement nesting. That's it!

This is not some wild theory proposed by a fringe minority
of bold programmers, no! This is a very popular approach
which is embraced by modern UI frameworks like  
[Flutter](https://flutter.dev/), 
[React native](https://reactnative.dev/), 
[SwiftUI](https://developer.apple.com/tutorials/swiftui/), 
[Jetpack compose](https://developer.android.com/jetpack/compose)
and of course [JetBrain's UI DSL](https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics),
which is also Swing based.

The benefits this approach of UI design brings
are overwhelming:

- Irrespective of the language used, the result is not more verbose than XML like formats.
- Binding can be done through simple lambda expressions.
- The result UI is extremely debuggable.
- It is easier to do fast prototyping.
- Lambda based event registration (instead binding magic) makes MVVM, MVC, MVP and other design patterns extremely simple and transparent!
- And finally: It is possible to use functional design patterns where UI is regenerated on the fly based on immutable data! 
This opens the door to designing UIs which are both extremely dynamic and still easily maintainable.

So here you have it, **this is why swing-tree exists**!

---

If you are interested in learning more about how to use swing-tree,
checkout the [Getting Started Tutorial](./Climbing-Swing-Tree.md).
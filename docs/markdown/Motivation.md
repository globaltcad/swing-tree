# The Problem: Building UIs #

Graphical user interfaces, are like many
concepts in the field of computer science
just tree structures based on simple UI
components as tree nodes.
This is a very well proven concept, and it shows:
**HTML dominates the UI world nowadays**. 

This success story was so impactful that it even lead to the **creation of 
custom xml like file formats** introduced by desktop application frameworks 
emerging after the success of HTML, think of **`FXML` in JavaFX, `XAML` in WPF**.
However, this was a bad move, as it **missed the point as to why HTML succeeded**.

I believe it is safe to say that 
HTML may have failed in gathering traction, if it was not for combining a turing complete scripting 
language in a nesting based markup format, **JavaScript embedded into HTML**.

This is where desktop app frameworks miss the point. 
They are built around the idea to define the UI structure **in one static xml like file**, 
and then use some fancy binding mechanism (a xml parser and some tree traversal +maybe reflection magic) 
to attach the UI to a view model.<br>
Don't get me wrong, that's great! <br>
It is best practice to decouple UI from the UIs logic...
However, this approach makes the UI inherently static and hard to interface with.
In the world of web development the UIs are dynamically assembled HTML texts which gets sent to 
the users' browser where then JS takes over, meaning there is an inherent divide.
Desktop application frameworks on the other hand, don't need to send anything 
over the internet, so why use xml formats?


## Making Imperative UIs Declarative #

You might say that XML-like formats are superior to building UI
imperatively 
because of their declarative nature and inherent support for nesting.
However, your favourite programming language also supports nesting 
and the ability to assemble things
in a declarative manner when the right design patterns are applied.

Why not program the UIs state using a nested builder pattern? 
All it takes is some method chaining.

This is not some wild theory proposed by a fringe minority
of bold programmers, no! This is a very popular approach
which is embraced by modern UI frameworks like  
[Flutter](https://flutter.dev/), 
[React native](https://reactnative.dev/), 
[SwiftUI](https://developer.apple.com/tutorials/swiftui/) 
and [Jetpack compose](https://developer.android.com/jetpack/compose).

The benefits this approach of UI design brings
are overwhelming:

- Irrespective of the language used, the result is not more verbose than XML like formats.
- Binding can be done through simple lambda expressions.
- The result UI is extremely debuggable.
- It is easier to do fast prototyping.
- Lambda based event registration (instead binding magic) makes MVVM, MVC, MVP and other design patterns extremely simple and transparent!
- And finally: It is possible to use functional design patterns where UI is regenerated on the fly based on immutable data! 
This would open the door to designing UIs which are both extremely dynamic and still easily maintainable.
(Although this is only performant when rebuilding smaller chunks of the UI at a time...)

So here you have it, **this is why swing-tree exists**!
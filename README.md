
# Swing Tree - A UI builder [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) ![Java Version](https://img.shields.io/static/v1.svg?label=Java&message=8%2B&color=blue) #

Fluent and boilerplate free Swing UI building!

- [Motivation](docs/markdown/Motivation.md)

<table>
<tr>
<th></th>
<th></th>
</tr>
<tr>
<td> 

- intuitive nested UI building in an HTML like fashion
- extremely light weight and simple
- powered by `MigLayout` by default
- lambda friendly API for peeking into the underlying UI tree and manipulating swing components directly
- boilerplate free event registration through `onClick`, `onChange` methods...
- compatible with custom swing components
	
</td>
<td>
	
<img href="https://www.flaticon.com/free-icons/swing" title="swing icons" src="docs/img/swing.png" style="width:200px;"/>
</td>
</tr>
</table>

---

<table>
<tr>
<th></th>
<th></th>
</tr>
<tr>
<td> 

```java
FlatLightLaf.setup();
UI.of(this/*JPanel subtype*/).withLayout("fill, insets 10")
.add("grow, span, wrap",
   UI.panel("fill, ins 0")
   .add("shrink", UI.label("Result:"))
   .add("grow, wrap",
      UI.label("42.0").with(UI.HorizontalAlignment.RIGHT
      .withProperty("FlatLaf.styleClass", "large")
   )
   .add("grow, span, wrap",
      UI.textArea(UI.HorizontalDirection.RIGHT_TO_LEFT, "13 - 73")
      .id("input-text-area")
   )
)
.add("growx", UI.radioButton("DEG"), UI.radioButton("RAD"))
.add("shrinkx", UI.splitButton("sin"))
.add("growx, wrap",
   UI.button("Help").withProperty("JButton.buttonType", "help")
)
.add("growx, span, wrap",
   UI.panel("fill")
   .add("span, grow, wrap",
       UI.panel("fill, ins 0")
       .add("grow",
           UI.button("(")
	   .withProperty("JButton.buttonType", "roundRect"),
           UI.button(")")
	   .withProperty("JButton.buttonType", "roundRect")
       )
   )
   .add("grow",
      UI.panel("fill, ins 0")
      .apply( it -> {
         String[] labels = {
            "7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "C"
         };
         for ( int i = 0; i < labels.length; i++ )
            it.add("grow" + ( i % 3 == 2 ? ", wrap" : "" ), 
               UI.button(labels[i])
            );
      }),
      UI.panel("fill, ins 0")
      .add("grow", 
         UI.button("-").withProperty("JButton.buttonType", "roundRect")
      )
      .add("grow, wrap", 
         UI.button("/").withProperty("JButton.buttonType", "roundRect")
      )
      .add("span, grow, wrap",
         UI.panel("fill, ins 0")
            .add("grow", 
            UI.button("+")
	    .withProperty("JButton.buttonType", "roundRect"),
            UI.panel("fill, ins 0")
            .add("grow, wrap",
               UI.button("*")
	       .withProperty("JButton.buttonType", "roundRect"),
               UI.button("%")
	       .withProperty("JButton.buttonType", "roundRect")
            )
         ),
         UI.button("=")
         .withBackground(new Color(103, 255, 190))
         .withProperty("JButton.buttonType", "roundRect")
      )
   )
);
```

</td>
<td style="vertical-align:top">
	
Here an example of some swing tree code which defines a simple calculator UI based on the `FlatLaF` look-and-feel. <br>
This code will produce the following UI when added to a `JFrame`.
	
---
	
<img href="" title="example" src="docs/img/simple-example.png" style="width:100%"/>

---
	
As you can see, swing tree has a very simple API, which only requires a
single class to be imported, the `UI` class which can even be imported 
statically to remove any `UI.` prefixes.

---
	
Also, note that there are usually 2 arguments 
added to a tree node: a `String` and then UI nodes.
This first argument simply translates 
to the layout constraints which should be applied
to the UI element(s) added. <br>
	
In this example, strings will be passed to a `MigLayout`
simply because it is a general purpose layout and no other
layout was specified.

---
	
For more examples take a look at the [examples folder](src/test/groovy/com/globaltcad/swingtree/examples) inside the test suite. 
	
</td>
</tr>
</table>

## Getting started with [![](https://jitpack.io/v/globaltcad/swing-tree.svg)](https://jitpack.io/#globaltcad/swing-tree) ##
**1. Add the JitPack url in your root `build.gradle` at the end of `repositories`**
```
allprojects {
	repositories {
		//...
		maven { url 'https://jitpack.io' }
	}
}
```
**2. Add swing-tree as dependency**

...either by specifiying the version tag:
```
dependencies {
	implementation 'com.github.globaltcad:swing-tree:v0.0.1'
}
```
...or by using a custom commit hash instead:
```
dependencies {
	implementation 'com.github.globaltcad:swing-tree:7c74811'//Any commit hash...
}
```
---


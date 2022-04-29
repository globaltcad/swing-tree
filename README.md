
# Swing Tree - A UI builder #

Fluent and boilerplate free Swing UI building!

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


```java
	
UI.panelWithLayout("fill, insets 10","[grow][shrink]")
.withBackground(Color.WHITE)
.add("cell 0 0",
    UI.label("Hello and welcome to this UI! (Click me I'm a link)")
    .makeBold()
    .makeLinkTo("https://github.com/globaltcad")
)
.add("cell 0 1, grow, shrinky",
    UI.panelWithLayout("fill, insets 0","[grow][shrink]")
    .withBackground(Color.WHITE)
    .add("cell 0 0, aligny top, grow x, grow y",
        UI.panelWithLayout("fill, insets 7","grow")
        .withBackground(Color.LIGHT_GRAY)
        .add("span", UI.label("...some text..."))
        .add("shrink", UI.label("First Name"))
        .add("grow", UI.of(new JTextField("John")))
        .add("gap unrelated, shrink", UI.label("Last Name"))
        .add("wrap, grow", UI.of(new JTextField("Smith")))
        .add("shrink", UI.label("Address"))
        .add("span, grow", UI.of(new JTextField("Somewhere")))
    )
    .add("cell 1 0, grow y",
        UI.panelWithLayout("fill", "[grow]")
        .add("cell 0 0, aligny top", UI.button("I am a normal button"))
        .add("cell 0 1, aligny top",
            UI.button("<html><i>I am a naked button</i><html>")
            .withCursor(UI.Cursor.HAND)
            .makePlain()
            .onClick( e -> {/* does something */} )
        )
        .withBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY))
        .add( "cell 0 2, aligny bottom, span, shrink", UI.label("Here is a text area:"))
        .add("cell 0 3, aligny bottom, span, grow", UI.of(new JTextArea("Anything...")))
    )
)
.add("cell 0 2, grow",
    UI.panelWithLayout("fill, insets 0 0 0 0","[grow][grow][grow]")
    .withBackground(Color.WHITE)
    .add("cell 1 0", UI.label("Built with swingtree"))
    .add("cell 2 0", UI.label("by GTS"))
    .add("cell 3 0", UI.label("29-March-2022"))
)
.add("cell 0 3, span 2, grow",
    UI.label("...here the UI comes to an end...").withForeground(Color.LIGHT_GRAY)
)
.withBackground(Color.WHITE);
	
```

Which produces the following UI when added to a JFrame:

<img href="" title="example" src="docs/img/simple-example.png" style="width:60%"/>


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


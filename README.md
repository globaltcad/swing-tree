
# STML - Swing Tree Markup Language #

Fluent and boilerplate free Swing UI building!

<table style=".markdown-body .highlight pre, .markdown-body pre { font-size: 50%; }">
<tr>
<th></th>
<th></th>
</tr>
<tr>
<td style="font-size: 50% !important;"> 

```java
	
UI.of(this)
.withLayout(debug+"fill", "[][grow]")
.withBackground(Color.WHITE)
.add(UI.labelWithIcon(200, 200, cover).withTooltip("I am a picture of a swing on a tree!"))
.add("grow",
    UI.panelWithLayout(debug+"fill, insets 0","[grow][shrink]")
    .onMouseClick( e -> {/* does something */} )
    .add("cell 0 0",
    	UI.label("Hello and welcome to this UI! (Click me I'm a link)")
    	.makeBold()
    	.makeLinkTo("https://github.com/gts-oss")
    )
    .add("cell 0 1, grow, pushy",
    	UI.panelWithLayout(debug+"fill, insets 0","[grow][shrink]")
    	.withBackground(Color.WHITE)
    	.add("cell 0 0, aligny top, grow x",
    		UI.panelWithLayout(debug+"fill, insets 7","grow")
    		.withBackground(new Color(255, 138, 99))
    		.add( "span",
    			UI.label("<html><div style=\"width:275px;\">"+ description +"</div></html>")
    		)
    		.add("shrink x", UI.label("First Name"))
    		.add("grow", UI.of(new JTextField("")))
    		.add("gap unrelated", UI.label("Last Name"))
    		.add("wrap, grow", UI.of(new JTextField("")))
    		.add(UI.label("Address"))
    		.add("span, grow", UI.of(new JTextField("")))
    		.add( "span, grow", UI.label("Here is a text area:"))
    		.add("span, grow", UI.of(new JTextArea("")))
    	)
    	.add("cell 1 0",
    		UI.buttonWithIcon(50, 50, pdfIcon, pdfHover)
    		.withCursor(UI.Cursor.HAND)
    		.makePlain()
    		.onClick( e -> {/* does something */} )
    	)
    	.withBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY))
    )
    .add("cell 0 2, grow",
    	UI.panelWithLayout(debug+"fill, insets 0 0 0 0","[grow][grow][grow]")
    	.withBackground(Color.WHITE)
    	.add("cell 1 0", UI.label("Built with swingtree"))
    	.add("cell 2 0", UI.label("GTS-OSS"))
    	.add("cell 3 0", UI.label("29-March-2022"))
    )
    .add("cell 0 3, span 2, grow",
    	UI.label("...here the UI comes to an end...").withColor(Color.LIGHT_GRAY)
    )
    .withBackground(Color.WHITE)
);
	
 ```

</td>
<td>
	
![<a href="https://www.flaticon.com/free-icons/swing" title="swing icons">Swing icons created by prettycons - Flaticon</a>](docs/img/swing.png)

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


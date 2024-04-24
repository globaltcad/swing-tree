
# Confirm Dialogs #

SwingTree provides a concise and intuitive API for creating simple confirm dialogs
based on the `JOptionPane` class and the immutable builder pattern.
The following example shows how to create a simple message dialog with a title, message, 
and three `Yes`, `No`, and `Cancel` buttons.

```java
var answer = UI.confirmation("Do you want to continue reading?")
             .titled("Please confirm!")
             .show();
```

The `answer` is a `ConfirmAnswer` enum which can 
be one of the following values:

- `YES`, the user clicked the `Yes` button
- `NO`, the user clicked the `No` button
- `CANCEL`, the user clicked the `Cancel` button
- `CLOSE`, the user closed the dialog without clicking a button!

If you want to configure which buttons are shown and what their text is,
you can achieve this by explicitly setting the texts like this:

```java
var answer = UI.confirmation("Only one button!")
             .titled("Confirm OK!")
             .yesOption("I am ok with this")
             .noOption("")
             .cancelOption("")
             .show();
```

In this example, the `No` and `Cancel` buttons are not shown.
Only the `Yes` button is shown with the text `I am ok with this`.

---

The `UI.confirm()` factory method is designed for showing question based 
dialogs, however there are other factory methods for showing
information, warning, and error dialogs.

```java
UI.confirmation("Bananas are yellow.").show();
UI.confirmation("You forgot to turn off the stove!").showAsWarning();
UI.confirmation("The world is ending!").showAsError();
```

They differ from one another in the look and feel of the dialog.
The look however is mostly determined by the icon that is shown in the dialog,
which you can also configure yourself.

```java
  UI.confirmation("Do you like my icon?")
  .titled("Confirm with custom icon"!)
  .icon(UI.findIcon("my-icon.png").orElse(null))
  .show();
```

The `UI.findIcon()` method is a utility method that searches for an icon in the
classpath or in the icon cache of the `SwingTree` context object. 
It returns an `Optional<Icon>` which is empty if the icon could not be found.
The `UI.findIcon()` method also supports SVG icons.

---

# Message Dialogs #

A message dialog differs from a confirm dialog in that it
does not return a value. 
So it is intended to only show a message to the user without asking a question.

Just like there are different factory methods for confirm dialogs,
there are also different factory methods for message dialogs.

```java
UI.message("Bananas are still yellow!").showAsInfo();
UI.message("You will burn the house down!").showWarning();
UI.message("The world is still ending!").showAsError();
```

---







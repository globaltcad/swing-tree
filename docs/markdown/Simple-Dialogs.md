
# Dialogs in SwingTree #

> **Prerequisites:** This guide assumes you are already comfortable
> writing simple SwingTree declarations — see
> [Climbing the Swing Tree](./Climbing-Swing-Tree.md) for a primer.

SwingTree wraps the most common uses of `JOptionPane` behind a small, fluent
builder API. The factories on `UI` give you back an **immutable builder** that
you configure step by step and finalize with one of the `show...()` methods.

Two complementary entry points cover almost every interactive dialog you will
ever need:

| Factory | Returns | Use when… |
|---|---|---|
| [`UI.confirmation(message)`](#confirm-dialogs) | a `ConfirmAnswer` enum | You need to **ask the user a question** and react to their choice. |
| [`UI.message(message)`](#message-dialogs)     | nothing                 | You only need to **tell the user something** — no decision required. |

# Confirm Dialogs #

The following example shows how to create a simple confirmation dialog
with a title, a question, and three `Yes`, `No`, and `Cancel` buttons.

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

The `UI.confirmation(..)` factory method is designed for showing question-based
dialogs, but the same builder also offers `showAsInfo()`, `showAsWarning()` and
`showAsError()` variants that vary the icon and tone of the dialog:

```java
UI.confirmation("Bananas are yellow.").show();             // plain question
UI.confirmation("You forgot to turn off the stove!").showAsWarning();
UI.confirmation("The world is ending!").showAsError();
```

The look is mostly determined by the icon shown in the dialog,
which you can also configure yourself:

```java
  UI.confirmation("Do you like my icon?")
  .titled("Confirm with custom icon!")
  .icon(UI.findIcon("my-icon.png").orElse(null))
  .show();
```

The `UI.findIcon(..)` method is a utility that searches for an icon in the
classpath or in the icon cache of the `SwingTree` context object.
It returns an `Optional<Icon>` which is empty if the icon could not be found,
and it also supports SVG icons.

---

# Message Dialogs #

A message dialog differs from a confirm dialog in that it
does not return a value.
It is intended to only show a message to the user without asking a question.

Just like with confirm dialogs, there are several `show...()` variants on the
message builder that pick the appropriate icon:

```java
UI.message("Bananas are still yellow!").showAsInfo();
UI.message("You will burn the house down!").showAsWarning();
UI.message("The world is still ending!").showAsError();
```

---

## Where to next? ##

- Need to build the dialog content itself (forms, tables, custom layouts)
  rather than just show a message? Then anything you have learned about
  building views — [Climbing the Swing Tree](./Climbing-Swing-Tree.md),
  [Functional MVVM](./Functional-MVVM.md),
  [Style Sheets and Groups](./Style-Sheets-And-Groups.md) — composes naturally
  inside the dialog window you embed your panel into via `UI.show(..)`.
- For an overview of every guide, see the [wiki index](./README.md).







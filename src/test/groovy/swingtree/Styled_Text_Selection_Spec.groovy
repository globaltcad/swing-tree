package swingtree.style

import spock.lang.IgnoreIf
import spock.lang.Narrative
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Var
import swingtree.SwingTree
import swingtree.UI
import swingtree.UIForAnySwing
import swingtree.api.Configurator
import swingtree.components.JBox
import swingtree.layout.Position
import swingtree.threading.EventProcessor
import utility.ApplicationThread
import utility.LogSpy
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.*
import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.util.concurrent.CopyOnWriteArrayList

@Title("Selecting And Copying Styled Text")
@Narrative('''

    With `withStyle(it -> it.text("body", t -> t.content("...")))` you can paint text
    onto any component. This text is not painted by a Swing text component like `JTextField`,
    so by default your users can only read it. With `withTextSelection("body", selection)` your users can also
    select parts of it with the mouse and copy them with the keyboard, just like the
    text of a web page.

    A selection is described by a `TextSelection`, which is an immutable value holding
    two positions in the text. A position is a gap between two characters: position 0
    is the gap in front of the first character, position 1 is the gap behind the first
    character, and so on. One of the two positions is the *anchor*: it is where your
    user pressed the mouse button, and it stays where it is while the mouse moves.
    The other position is the *caret*: it follows the mouse. The selected characters
    are the characters between the two positions, no matter which of the two comes
    first in the text.

    The `selection` property passed to `withTextSelection` always holds the current
    selection. When your user selects something, SwingTree writes the new selection
    into the property. When your code writes a selection into the property, SwingTree
    paints it.

    You decide how a selection looks with `selection(s -> ...)` inside the text style:
    `color(..)` is the colour painted behind the selected characters, `borderRadius(..)`
    rounds the corners of that coloured area, and `showCaret(true)` paints the caret as a
    thin vertical bar in the colour `caretColor(..)` whenever nothing is selected.

    Many scenarios check rendered pixels, but they never compare against a reference image.
    A reference image breaks whenever a JDK version or an operating system draws a glyph
    slightly differently. Instead, the scenarios paint the words of a text in different
    colours, find the smallest rectangle around the pixels of one colour, and compare such
    rectangles with each other. Glyph edges are smoothed with partly transparent pixels,
    so only pixels which are at least 160 out of 255 opaque, and whose red, green and blue
    values each differ by at most 40 from the colour, count as painted in that colour.

    Every text in these scenarios uses the Ubuntu font, which the tests load from their own
    resources. A logical font like `Font.DIALOG` is a different font on every operating system,
    so its letters have different widths, and a line which breaks behind the second word on one
    computer breaks behind the third word on another. In the Ubuntu font at a size of 20, an "H"
    and an "N" are about 14 pixels wide, an "E" about 11 pixels, a space about 5 pixels, and a
    line is about 22.4 pixels tall. The scenarios use these numbers when they explain where a
    line breaks.

''')
@Subject([TextSelection, TextSelectionConf, TextConf, UIForAnySwing])
class Styled_Text_Selection_Spec extends Specification
{
    static final Color RED    = new Color(220,   0,   0)
    static final Color GREEN  = new Color(  0, 150,   0)
    static final Color BLUE   = new Color(  0,   0, 220)
    static final Color YELLOW = new Color(255, 220,   0)
    static final Color WHITE  = Color.WHITE

    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def setup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def 'A `TextSelection` made with `TextSelection.between(anchor, caret)` keeps both positions, and `start()` and `end()` are the smaller and the larger of the two.'()
    {
        reportInfo """
            A `TextSelection` holds two positions in a text. A position is a gap between two
            characters: position 0 is the gap in front of the first character, position 1
            is the gap behind the first character, and so on.

            The *anchor* is the position where the selection began, for example where your
            user pressed the mouse button. The *caret* is the position which moves, for
            example where your user drags the mouse to. When your user drags the mouse
            backwards, the caret is in front of the anchor, so the caret is the smaller of
            the two positions.

            `start()` is always the smaller position and `end()` is always the larger one.
            If `start()` simply returned the anchor, then every piece of code which cuts the
            selected characters out of a text would fail for a selection which was made
            backwards.

            Two selections with the same anchor and the same caret are equal. A selection
            from 2 to 9 is not equal to a selection from 9 to 2, even though both select the
            same characters. The difference shows when your user clicks with the shift key held
            down, which keeps the anchor and moves the caret. A shift-click at position 11 turns
            the selection from 2 to 9 into a selection from 2 to 11, but it turns the selection
            from 9 to 2 into a selection from 9 to 11.
        """
        given : 'A selection made forwards, from position 2 to position 9, and the same characters selected backwards:'
            var forwards  = TextSelection.between(2, 9)
            var backwards = TextSelection.between(9, 2)

        expect : 'Both keep their anchor and their caret:'
            forwards.anchor()  == 2
            forwards.caret()   == 9
            backwards.anchor() == 9
            backwards.caret()  == 2
        and : 'Both start at position 2 and end at position 9:'
            forwards.start()  == 2
            forwards.end()    == 9
            backwards.start() == 2
            backwards.end()   == 9
        and : 'A selection equals another selection only when anchor and caret are both the same:'
            forwards == TextSelection.between(2, 9)
            forwards.hashCode() == TextSelection.between(2, 9).hashCode()
            forwards != backwards
    }

    def 'A `TextSelection` made with `TextSelection.caretAt(index)` selects no character, and `isEmpty()` is true exactly when the anchor and the caret are the same position.'()
    {
        reportInfo """
            A `TextSelection` holds two positions in a text: the *anchor*, where the selection
            began, and the *caret*, which is the position that moves. A position is a gap
            between two characters.

            When the anchor and the caret are the same gap, no character lies between them,
            so nothing is selected. Such a selection still says where the caret is, which
            is where a shift-click would continue from. `TextSelection.caretAt(4)` makes
            this kind of selection, with both positions at the gap behind the fourth character.
        """
        given :
            var caretOnly = TextSelection.caretAt(4)

        expect : 'Both positions are at 4, and nothing is selected:'
            caretOnly.anchor() == 4
            caretOnly.caret()  == 4
            caretOnly.isEmpty()
            caretOnly == TextSelection.between(4, 4)
        and : 'A selection whose positions differ by one character is not empty:'
            !TextSelection.between(4, 5).isEmpty()
            !TextSelection.between(5, 4).isEmpty()
    }

    def 'The factory methods of `TextSelection` refuse a negative position.'()
    {
        reportInfo """
            A position in a text is a gap between two characters, counted from 0 for the gap
            in front of the first character. There is no gap in front of that gap, so a
            negative position is always a mistake in the code which computed it.
            `TextSelection.between(..)` and `TextSelection.caretAt(..)` throw an
            `IllegalArgumentException` for a negative position, so that the mistake shows
            up where it was made, instead of later as a selection colour painted in a wrong place.
        """
        when :
            TextSelection.between(-1, 4)
        then :
            thrown(IllegalArgumentException)

        when :
            TextSelection.between(4, -1)
        then :
            thrown(IllegalArgumentException)

        when :
            TextSelection.caretAt(-3)
        then :
            thrown(IllegalArgumentException)
    }

    def 'With `withTextSelection("body", selection)` and `placement(#placement)`, pressing the mouse on the first letter of a word and releasing it on the last letter selects that word.'(
        UI.Placement placement
    ) {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 0 is the gap in front of the "H", position 6 is the gap in front of the "w",
            and position 11 is the gap behind the "d".

            When your user presses the mouse button on a letter, SwingTree picks the gap
            nearest to the mouse: the gap in front of the letter when the mouse is on its left
            half, and the gap behind the letter when the mouse is on its right half.
            That position becomes the *anchor*, which stays put while the mouse moves.
            The position nearest to where the mouse is dragged and released becomes the
            *caret*. Pressing on the "w" and releasing on the "d" therefore selects from
            position 6 to position 11, which is the word "world".

            The component is 400 pixels wide and 100 pixels tall and has a padding of 10 pixels,
            so its content area runs from 10 to 390 pixels horizontally and from 10 to 90 pixels
            vertically. The placement decides where in the content area the line is painted:
            with `TOP_LEFT` the line starts at x = 10 and its top is at y = 10, with `TOP_RIGHT`
            the line ends at x = 390, and with `BOTTOM_RIGHT` it also ends at x = 390 and its
            bottom is at y = 90.

            To find the letter under the mouse, SwingTree uses the positions at which it paints
            the letters. If it worked those positions out a second time for the mouse, and for
            example forgot the placement, then with `TOP_RIGHT` it would look for the letters
            near x = 10 while they are painted near x = 390, and the selected letters would not
            be the letters under the mouse. So this scenario repeats the same gesture for all
            nine placements.

            In this scenario the placement is `${placement}`.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(placement)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangle around the blue pixels, which are
            the pixels of "world". The leftmost blue pixel column belongs to the left half of
            the "w", and the rightmost blue pixel column belongs to the right half of the "d".
            So we can aim the mouse at these two columns without computing the widths of the
            letters:
        """
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : """
            We send the component the mouse events of a drag: the left mouse button is pressed
            on the left edge of the "w", the mouse is dragged to the right edge of the "d",
            and the button is released there:
        """
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }

        then : 'The anchor is the gap in front of the "w", and the caret is the gap behind the "d":'
            selection.get().anchor() == 6
            selection.get().caret()  == 11

        where :
            placement << [
                UI.Placement.TOP_LEFT, UI.Placement.TOP, UI.Placement.TOP_RIGHT,
                UI.Placement.LEFT,     UI.Placement.CENTER,   UI.Placement.RIGHT,
                UI.Placement.BOTTOM_LEFT, UI.Placement.BOTTOM, UI.Placement.BOTTOM_RIGHT
            ]
    }

    def 'With `withTextSelection("body", selection)`, dragging the mouse backwards puts the anchor behind the caret.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 6 is the gap in front of the "w", and position 11 is the gap behind the "d".

            The *anchor* is the position where your user pressed the mouse button, and the
            *caret* is the position where your user released it. When your user presses the
            mouse on the "d" and drags it back to the "w", the anchor is 11 and the caret is 6.
            The same five letters are selected as when dragging forwards, but the two positions
            are swapped. The difference shows when your user clicks with the shift key held down,
            which keeps the anchor and moves the caret. A shift-click at position 0, in front of
            the "H", turns the selection from 11 to 6 into a selection from 11 to 0, which is the
            whole text. After a forwards drag from 6 to 11, the same shift-click would make a
            selection from 6 to 0, which is "Hello ".
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangle around the blue pixels of "world".
            Its leftmost pixel column belongs to the left half of the "w" and its rightmost
            pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'We press the mouse button on the right edge of the "d", drag to the left edge of the "w", and release the button there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }

        then : 'The anchor is the gap behind the "d", and the caret is the gap in front of the "w":'
            selection.get().anchor() == 11
            selection.get().caret()  == 6
    }

    def 'With `withTextSelection("body", selection)`, dragging the mouse beyond either end of a line selects up to the first or the last character of the text, and never beyond it.'()
    {
        reportInfo """
            The component is 400 pixels wide and paints the text "Hello world", which has
            11 characters and is far narrower than the component. A selection holds two
            positions, and a position is a gap between two characters: position 0 is the gap
            in front of the "H", position 6 is the gap in front of the "w", and position 11
            is the gap behind the "d", which is the end of the text.

            When your user drags the mouse right of the last letter, the nearest gap is the
            end of the text, so the caret goes to position 11. A text of 11 characters has no
            position 12 or 30, so SwingTree never writes such a position into the property.
            In the same way, dragging the mouse left of the first letter, into the padding of
            the component, puts the caret at position 0. Every position which comes from the
            mouse lies inside the text, so your code can use it directly, for example with
            `String.substring`.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which is 400 pixels wide, has a padding of 10 pixels, and paints "Hello " in red and "world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the rectangle around the blue pixels of "world". Its leftmost pixel column belongs to the left half of the "w":'
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW = world.x
            int lineMiddle  = world.centerY

        when : 'We press the mouse button on the left edge of the "w" and drag the mouse to x = 395, which is far right of the end of the text:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED, 1, InputEvent.BUTTON1_DOWN_MASK, 395, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        then : 'The caret is at the end of the text, which is position 11:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11

        when : 'We keep the button down and drag the mouse to x = 3, which lies in the padding left of the first letter:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  2, InputEvent.BUTTON1_DOWN_MASK, 3, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, 3, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        then : 'The caret is at the start of the text, which is position 0, and the anchor is still in front of the "w":'
            selection.get().anchor() == 6
            selection.get().caret()  == 0
    }

    def 'With `withTextSelection("body", selection)`, a double click on a word selects the whole word.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 6 is the gap in front of the "w", and position 11 is the gap behind the "d".

            When your user double-clicks a letter, SwingTree selects the word which contains
            that letter. SwingTree finds the start and the end of the word with
            `java.text.BreakIterator.getWordInstance()`, which contains the rules for word
            boundaries of many languages. The *anchor*, which is the position
            that stays put during a later shift-click, is the start of the word, and the
            *caret* is the end of the word.

            Here your user double-clicks in the middle of "world", so the selection goes
            from position 6 to position 11.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the middle of the blue pixels of "world":'
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int x = world.centerX
            int y = world.centerY

        when : """
            We send the events of a double click to the middle of "world". Swing reports the
            second press of a double click as a mouse press event whose click count is 2:
        """
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, x, y, 2, false, MouseEvent.BUTTON1))
            }

        then : 'The word "world" is selected, from its start at position 6 to its end at position 11:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
    }

    def 'With `withTextSelection("body", selection)`, a double click on the right half of the last letter of a word selects that word.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 0 is the gap in front of the "H", position 5 is the gap behind the "o"
            of "Hello", which is in front of the space, and position 6 is the gap behind the space.

            Your user double-clicks on the right half of the "o" of "Hello". The gap nearest
            to the mouse is the gap behind the "o", which is position 5. But a double click is
            about the letter under the mouse, not about the nearest gap: the letter under the
            mouse is the "o", so SwingTree selects the word "Hello", from position 0 to position 5.
            The *anchor*, which is the position that stays put during a later shift-click, is the
            start of the word, and the *caret*, which is the position that moves, is its end.

            If SwingTree looked for the word around the nearest gap instead, it would find the
            space behind position 5, and your user would see the space selected instead of the
            word they double-clicked.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello" in red and " world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello"  ),
                                StyledString.of( f -> f.color(BLUE), " world" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangle around the red pixels of "Hello".
            Its rightmost pixel column belongs to the right half of the "o". Glyph edges are smoothed,
            and on some computers that outermost pixel is painted in the first pixel column of the space
            behind the "o", where a double click would select the space. So we aim 3 pixels further left,
            which is still in the right half of an "o" that is about 11 pixels wide, so the gap nearest
            to it is still the gap behind the "o":
        """
            var image = Utility.renderSingleComponent(box)
            var hello = boundsOfPixelsPaintedIn(image, RED)
            int rightHalfOfO = hello.x + hello.width - 1 - 3
            int lineMiddle   = hello.centerY

        when : """
            We send the events of a double click to the right half of the "o". Swing reports the
            second press of a double click as a mouse press event whose click count is 2:
        """
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, rightHalfOfO, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, rightHalfOfO, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, rightHalfOfO, lineMiddle, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, rightHalfOfO, lineMiddle, 2, false, MouseEvent.BUTTON1))
            }

        then : 'The word "Hello" is selected, from its start at position 0 to its end at position 5:'
            selection.get().anchor() == 0
            selection.get().caret()  == 5
    }

    def 'With `withTextSelection("body", selection)`, a click with the shift key held down moves the caret and keeps the anchor where it was.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 0 is the gap in front of the "H", position 5 is the gap behind the "o"
            of "Hello", and position 11 is the gap behind the "d".

            The *anchor* is the position where the selection began, and the *caret* is the
            position which moves. A plain click puts both positions at the gap nearest to the
            mouse. A click with the shift key held down only moves the caret, so the selection
            grows or shrinks while it keeps the place where it began. This is how your users
            select a long text without dragging the mouse all the way.

            Here your user clicks in front of the "H", then shift-clicks behind the "d", which
            selects everything, and then shift-clicks behind the "o" of "Hello", which shrinks
            the selection to "Hello".
        """
        given : 'A property which holds the selection, starting with a selection somewhere in the middle of the text:'
            var selection = Var.of(TextSelection.between(2, 8))
        and : 'A component which paints "Hello " in red and "world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangles around the red pixels of "Hello"
            and the blue pixels of "world". The leftmost red pixel column belongs to the left
            half of the "H", the rightmost red pixel column belongs to the right half of the
            "o", and the rightmost blue pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            var hello = boundsOfPixelsPaintedIn(image, RED)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfH  = hello.x
            int rightEdgeOfO = hello.x + hello.width - 1
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'We click on the left edge of the "H" without the shift key:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfH, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, leftEdgeOfH, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        then : 'Both positions are in front of the "H", so nothing is selected:'
            selection.get().anchor() == 0
            selection.get().caret()  == 0

        when : 'We click on the right edge of the "d" with the shift key held down:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, InputEvent.SHIFT_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        then : 'The anchor stayed in front of the "H", and the caret moved behind the "d":'
            selection.get().anchor() == 0
            selection.get().caret()  == 11

        when : 'We click on the right edge of the "o" of "Hello" with the shift key held down:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  4, InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, rightEdgeOfO, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 5, InputEvent.SHIFT_DOWN_MASK, rightEdgeOfO, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        then : 'The anchor is still in front of the "H", and the caret moved back to the gap behind the "o":'
            selection.get().anchor() == 0
            selection.get().caret()  == 5
    }

    def 'With `withTextSelection("body", selection)`, the line break between two paragraphs is one character of the selection.'()
    {
        reportInfo """
            The component paints the text "Hello\\nworld", where "\\n" is a line break.
            The line break starts a new paragraph, so "Hello" is painted on the first line
            and "world" on the second line.

            A selection holds two positions, and a position is a gap between two characters.
            The line break counts as one character, just like in the `String` of the text:
            position 5 is the gap in front of the line break, position 6 is the gap behind it,
            which is in front of the "w", and position 11 is the gap behind the "d".

            So when your user drags from the "H" in the first line to the "d" in the second
            line, the selection goes from position 0 to position 11, and copying it gives
            "Hello\\nworld" with the line break in it. If SwingTree did not count the line
            break, every position in the second paragraph would be off by one, and the
            last letter of the selection would be missing.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello" in red in a first paragraph and "world" in blue in a second paragraph:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello\n" ),
                                StyledString.of( f -> f.color(BLUE), "world"   )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangles around the red and the blue pixels.
            The leftmost red pixel column belongs to the left half of the "H", and the rightmost
            blue pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            var hello = boundsOfPixelsPaintedIn(image, RED)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfH        = hello.x
            int middleOfFirstLine  = hello.centerY
            int rightEdgeOfD       = world.x + world.width - 1
            int middleOfSecondLine = world.centerY

        when : 'We press the mouse button on the left edge of the "H", drag down to the right edge of the "d", and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfH, middleOfFirstLine, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, middleOfSecondLine, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, middleOfSecondLine, 1, false, MouseEvent.BUTTON1))
            }
        and : """
            We copy the selection. Swing copies through the `TransferHandler` of a component: the
            keyboard shortcut for copying calls that handler, and the handler copies into the
            clipboard of the computer. Here we call `exportToClipboard` on the same handler with a
            clipboard that only this scenario uses, so the scenario neither needs nor overwrites
            the clipboard of the computer:
        """
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The selection goes from the gap in front of the "H" to the gap behind the "d":'
            selection.get().anchor() == 0
            selection.get().caret()  == 11
        and : 'The copied text contains the line break between the two paragraphs:'
            clipboard.getData(DataFlavor.stringFlavor) == "Hello\nworld"
    }

    def 'With `withTextSelection("body")`, a text which wraps onto a second line is copied without a line break where it was wrapped.'()
    {
        reportInfo """
            The content area of the component is only 100 pixels wide, and the text is
            "HHHH HHHH". At a font size of 20, one word of four "H" letters is about 56 pixels
            wide, so one word fits into a line, but two words do not. With `wrapLines(true)`,
            SwingTree therefore paints the first word on the first line and the second word
            on the second line.

            Wrapping changes only where the letters are painted. It does not change the text,
            so it adds no character to it. Your user selects all characters with the keyboard
            shortcut for selecting everything, and copying them gives "HHHH HHHH", with the
            space between the two words and without a line break. A line break would only be
            in the copied text if the text itself contained one.
        """
        given : 'A component whose content area is 100 pixels wide, with a first word in red and a second word in green:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                        )
                    )
                    .withTextSelection("body")
                    .get(JBox)
        and : 'We render the component and find the rectangles around the red and the green pixels:'
            var image = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)

        when : """
            We send the keyboard shortcut for selecting everything, which is Ctrl+A on Windows
            and Linux and Command+A on macOS. A key event sent with `dispatchEvent` only reaches
            the component which has the keyboard focus, and a component which is not inside a
            window never has the keyboard focus. So we hand the key event to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers it directly:
        """
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED)
                )
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The second word is painted below the first word, so the text was really wrapped:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The copied text is the text as it was written, without a line break:'
            clipboard.getData(DataFlavor.stringFlavor) == "HHHH HHHH"
    }

    def 'With `withTextSelection("body", selection)` and `obstacles(..)`, dragging from the letters left of an obstacle to the letters right of it selects the letters of both sides in reading order.'()
    {
        reportInfo """
            An obstacle is a shape which the text flows around. Here a rectangle from
            x = 150 to x = 250 is in the way of the first line. The content area of the
            component reaches from x = 10 to x = 390, so the first line has room for letters
            on two sides: from 10 to 150 on the left, and from 250 to 390 on the right.

            The text is "HHHHH HHHHHHH HHHHHHH". At a font size of 20 an "H" is about 14
            pixels wide. The first word, "HHHHH" plus a space, is about 75 pixels wide
            and fits on the left side. The second word, "HHHHHHH", is about 98 pixels wide
            and does not fit into the remaining 65 pixels on the left side, so SwingTree
            paints it on the right side of the obstacle. The third word goes to the second line.

            A selection holds two positions, and a position is a gap between two characters.
            The text is read from left to right across the obstacle, so position 6 is the
            gap in front of the first letter right of the obstacle, and position 13 is the
            gap behind the last "H" of the second word.

            When your user presses the mouse on the first letter left of the obstacle and
            releases it on the last letter right of the obstacle, the selection goes from
            position 0 to position 13. The positions right of the obstacle continue the
            positions left of it. If SwingTree counted the letters on the right side from 0
            again, the selection would end at position 7, in the middle of the first word.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A rectangle from x = 150 to x = 250 across the first line:'
            var obstacle = new Rectangle(150, 0, 100, 40)
        and : 'A component which paints the first word in red, the second word in green and the third word in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacle)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHHH "   ),
                                StyledString.of( f -> f.color(GREEN), "HHHHHHH " ),
                                StyledString.of( f -> f.color(BLUE),  "HHHHHHH"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangles around the red and the green pixels.
            The leftmost red pixel column belongs to the left half of the first "H", and the
            rightmost green pixel column belongs to the right half of the last green "H":
        """
            var image = Utility.renderSingleComponent(box)
            var leftOfObstacle  = boundsOfPixelsPaintedIn(image, RED)
            var rightOfObstacle = boundsOfPixelsPaintedIn(image, GREEN)
            int leftEdgeOfRedWord    = leftOfObstacle.x
            int rightEdgeOfGreenWord = rightOfObstacle.x + rightOfObstacle.width - 1
            int lineMiddle           = leftOfObstacle.centerY

        when : 'We press the mouse button on the left edge of the red word, drag to the right edge of the green word, and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfRedWord, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfGreenWord, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfGreenWord, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The red word is left of the obstacle and the green word is right of it, on the same line:'
            leftOfObstacle.x + leftOfObstacle.width <= 150
            rightOfObstacle.x >= 250
            Math.abs(rightOfObstacle.y - leftOfObstacle.y) <= 1
        and : 'The selection goes from the gap in front of the first letter to the gap behind the last green "H":'
            selection.get().anchor() == 0
            selection.get().caret()  == 13
        and : 'The copied text holds both words in reading order:'
            clipboard.getData(DataFlavor.stringFlavor) == "HHHHH HHHHHHH"
    }

    def 'With `withTextSelection("body", selection)`, the keyboard shortcut for selecting everything selects every character of the text.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 0 is the gap in front of the "H", and position 11 is the gap behind the "d".

            The keyboard shortcut for selecting everything is Ctrl+A on Windows and Linux and
            Command+A on macOS. It puts the *anchor*, which is the position that stays put during
            a later shift-click, at the start of the text, and the *caret*, which is the position
            that moves, at the end of the text.
        """
        given : 'A property which holds the selection, starting with two letters in the middle selected:'
            var selection = Var.of(TextSelection.between(3, 5))
        and : 'A component which paints "Hello world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We send the keyboard shortcut for selecting everything. A key event sent with
            `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand
            the key event to the component with `KeyboardFocusManager.redispatchEvent`, which
            delivers it directly:
        """
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED)
                )
            }

        then : 'The selection goes from the start to the end of the text:'
            selection.get().anchor() == 0
            selection.get().caret()  == 11
    }

    @IgnoreIf({ GraphicsEnvironment.isHeadless() })
    def 'With `withTextSelection("body", selection)`, the keyboard shortcut for copying puts exactly the selected characters onto the clipboard of the computer.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 6 is the gap in front of the "w", and position 11 is the gap behind the "d".
            Your code put the selection from 6 to 11 into the property, so "world" is selected.

            The keyboard shortcut for copying is Ctrl+C on Windows and Linux and Command+C on
            macOS. It puts the selected characters, "world", onto the clipboard of the computer,
            from where your user can paste them into any other program.

            When Java runs in headless mode, as it does on many build servers,
            `Toolkit.getSystemClipboard()` throws a `HeadlessException`. So this scenario
            only runs when Java is not headless.
        """
        given : 'A property which holds a selection from position 6 to position 11:'
            var selection = Var.of(TextSelection.between(6, 11))
        and : 'A component which paints "Hello world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            This scenario uses the real clipboard of the computer which runs it. We remember the
            text on it, so that we can put that text back when the scenario ends:
        """
            var systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            var formerContents  = systemClipboard.getContents(null)
            var formerText      = formerContents != null && formerContents.isDataFlavorSupported(DataFlavor.stringFlavor)
                                    ? formerContents.getTransferData(DataFlavor.stringFlavor)
                                    : null
        and : 'Something else on the clipboard, so that we can tell whether the clipboard changed:'
            systemClipboard.setContents(new StringSelection("something else"), null)

        when : """
            We send the keyboard shortcut for copying. A key event sent with `dispatchEvent` only
            reaches the component which has the keyboard focus, and a component which is not inside
            a window never has the keyboard focus. So we hand the key event to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers it directly:
        """
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_C, KeyEvent.CHAR_UNDEFINED)
                )
            }

        then : 'The clipboard holds the selected word and nothing else:'
            systemClipboard.getData(DataFlavor.stringFlavor) == "world"

        cleanup : 'We put the text back which was on the clipboard before the scenario began:'
            if ( formerText != null )
                systemClipboard.setContents(new StringSelection(formerText as String), null)
    }

    def 'With `withTextSelection("body", selection)`, a selection which reaches beyond the end of the text copies the characters up to the end of the text.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters.
            A selection holds two positions, and a position is a gap between two characters:
            position 6 is the gap in front of the "w", and position 11 is the gap behind the "d",
            which is the end of the text.

            Your code may put any selection into the property, for example a selection from
            position 6 to position 50, which it kept from a longer text. Copying takes the
            characters from position 6 up to the end of the text, which is "world". Copying
            neither throws an exception nor copies an empty text.
        """
        given : 'A property which holds a selection from position 6 to position 50:'
            var selection = Var.of(TextSelection.between(6, 50))
        and : 'A component which paints "Hello world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The copied text goes from the "w" to the end of the text:'
            clipboard.getData(DataFlavor.stringFlavor) == "world"
    }

    def 'The positions of a selection made with `withTextSelection("body", selection)` count only the characters which are painted, so a `StyledString` with a font size of 0 takes up no position.'()
    {
        reportInfo """
            The content of the text consists of three parts: "Hello", then "HIDDEN" with a
            font size of 0, and then " world". A font size of 0 makes a part invisible, and
            SwingTree leaves such a part out entirely before it paints the text. The painted
            text is therefore "Hello world", which has 11 characters.

            A selection holds two positions, and a position is a gap between two characters
            of the painted text. Position 11 is the gap behind the "d", which is the end of
            the painted text. The six letters of "HIDDEN" are not painted, so they have no
            positions, and copying never includes them. Otherwise your users would paste
            characters which they never saw.

            This applies to every part which SwingTree does not paint: a `StyledString` with
            an empty string, and a `StyledString` with a font size of 0.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component whose text has an invisible part between two visible ones:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( "Hello" ),
                                StyledString.of( f -> f.size(0), "HIDDEN" ),
                                StyledString.of( " world" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We send the keyboard shortcut for selecting everything, which is Ctrl+A on Windows
            and Linux and Command+A on macOS. We hand the key event to the component with
            `KeyboardFocusManager.redispatchEvent`, because a component outside of a window
            never has the keyboard focus, and only the component with the keyboard focus
            receives a key event sent with `dispatchEvent`:
        """
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED)
                )
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The selection ends at position 11, the end of the painted text:'
            selection.get().anchor() == 0
            selection.get().caret()  == 11
        and : 'The copied text is the painted text, without the invisible part:'
            clipboard.getData(DataFlavor.stringFlavor) == "Hello world"
    }

    def 'With `withTextSelection("body")` and no property, your users can still select and copy the text.'()
    {
        reportInfo """
            Often you only want your users to be able to copy a text, and your code does not
            need to know what they selected. Then you call `withTextSelection("body")` without
            a property, and SwingTree keeps the selection to itself.

            The component paints the text "Hello world". Your user drags the mouse from the
            first letter of "world" to its last letter, and copying gives "world".
        """
        given : 'A component which paints "Hello " in red and "world" in blue, with text selection but without a property:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body")
                    .get(JBox)
        and : """
            We render the component and find the rectangle around the blue pixels of "world".
            Its leftmost pixel column belongs to the left half of the "w" and its rightmost
            pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'We press the mouse button on the left edge of the "w", drag to the right edge of the "d", and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The copied text is the word which we dragged across:'
            clipboard.getData(DataFlavor.stringFlavor) == "world"
    }

    def 'With `EventProcessor.DECOUPLED`, a selection which your user makes with the mouse is written into the property of `withTextSelection` on the application thread, not on the UI thread.'()
    {
        reportInfo """
            A Swing program receives every mouse event and paints every component on one thread,
            the UI thread, which Swing calls the event dispatch thread. With
            `EventProcessor.DECOUPLED`, SwingTree writes the selections of your users into the
            property on a second thread, the application thread, and a listener of a property runs
            on the thread which wrote the property. A slow listener then delays the application
            thread, but the window keeps reacting to the mouse, because the UI thread is free.

            The component paints the text "Hello world". A selection holds two positions, and
            a position is a gap between two characters: position 6 is the gap in front of the "w",
            and position 11 is the gap behind the "d". The *anchor* is the position where your
            user pressed the mouse button, and the *caret* is the position which follows the mouse.

            Your user presses the mouse button on the "w" and drags it to the "d". The mouse
            events arrive on the UI thread. SwingTree hands every new selection over to the
            application thread, and the application thread writes it into the property.
            Pressing the button puts both positions at 6, and dragging moves the caret to 11.
            SwingTree also writes into the property after it has painted the text, to fill in the
            selected characters and the pixel rectangles of the caret and the anchor. The application
            thread writes these selections too. So every run of the listener of the property is a
            run on the application thread.
        """
        given : 'An application thread, which runs every task that SwingTree hands over to it:'
            var app = ApplicationThread.startDraining()
        and : 'A property with a listener which records the name of the thread on which it runs:'
            var threadsOfTheListener = new CopyOnWriteArrayList<String>()
            var selection = Var.of(TextSelection.caretAt(0))
            selection.onChange(From.VIEW, change -> threadsOfTheListener.add(Thread.currentThread().getName()) )
        and : """
            A component which paints "Hello " in red and "world" in blue. We declare it inside
            `UI.use(EventProcessor.DECOUPLED, ..)`, which makes the component hand the selections
            of your user over to the application thread:
        """
            var box = UI.runAndGet({ ->
                UI.use(EventProcessor.DECOUPLED, () ->
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                ).get(JBox)
            })
        and : """
            We render the component and find the rectangle around the blue pixels of "world".
            Its leftmost pixel column belongs to the left half of the "w" and its rightmost
            pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'On the UI thread, we press the mouse button on the left edge of the "w", drag to the right edge of the "d", and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We wait until the application thread has run every task which was handed over to it:'
            app.awaitAllProcessed()

        then : 'The property holds the selection of "world":'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
        and : 'The listener ran at least twice, once for pressing and once for dragging, and every time on the application thread:'
            threadsOfTheListener.size() >= 2
            threadsOfTheListener.every { it == app.name() }
        and : 'No task failed on the application thread:'
            app.failures().isEmpty()

        cleanup : 'We stop the application thread:'
            app?.stop()
    }

    def 'With `withTextSelection("body", selection)`, the mouse cursor is the text cursor while it is over the text, and the default cursor while it is over the rest of the component.'()
    {
        reportInfo """
            Your users need to see that they can select a text before they try it.
            Text components show this with the text cursor, which is the thin vertical
            "I" shaped mouse cursor. With `withTextSelection`, the mouse cursor turns into
            the text cursor while the mouse is over a line of the text, and turns back into
            the default arrow cursor while the mouse is over a part of the component where
            no text is painted.

            The component is 400 pixels wide and 100 pixels tall. It paints one line of text
            at its top, so its lower half has no text.
        """
        given : 'A component which paints "Hello world" in red in its top left corner:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(0)))
                    .get(JBox)
        and : 'We render the component and find the middle of the red pixels:'
            var image = Utility.renderSingleComponent(box)
            var text = boundsOfPixelsPaintedIn(image, RED)
            int middleX = text.centerX
            int middleY = text.centerY

        when : 'We move the mouse onto the middle of the text:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_MOVED, 0, 0, middleX, middleY, 0, false, MouseEvent.NOBUTTON))
            }
        then : 'The mouse cursor is the text cursor:'
            box.getCursor().getType() == Cursor.TEXT_CURSOR

        when : 'We move the mouse to x = 200 and y = 90, which is near the bottom of the component, far below the text:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_MOVED, 1, 0, 200, 90, 0, false, MouseEvent.NOBUTTON))
            }
        then : 'The mouse cursor is the default cursor again:'
            box.getCursor().getType() == Cursor.DEFAULT_CURSOR
    }

    def 'Without `withTextSelection`, dragging the mouse across a styled text selects nothing, paints no selection colour and keeps the default mouse cursor.'()
    {
        reportInfo """
            Text selection is something you switch on for a component with `withTextSelection`.
            On a component without it, dragging the mouse across the text does nothing:
            no selection colour is painted, the mouse cursor stays the default arrow cursor,
            and there is nothing to copy. This holds even when the
            style of the text describes how a selection would look with `selection(s -> ...)`.

            If SwingTree reacted to the mouse on every component with styled text, it would
            interfere with components which use the mouse for something else, like buttons
            and draggable panels.
        """
        given : 'A component which paints "Hello world" in red on white, with a yellow selection colour, but without `withTextSelection`:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .get(JBox)
        and : 'We render the component and find the rectangle around the red pixels:'
            var text = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), RED)
            int leftEdgeOfText  = text.x
            int rightEdgeOfText = text.x + text.width - 1
            int lineMiddle      = text.centerY

        when : 'We move the mouse onto the text, press the button on its left edge, drag to its right edge and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_MOVED,    0, 0, leftEdgeOfText, lineMiddle, 0, false, MouseEvent.NOBUTTON))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  1, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfText, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  2, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfText, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, rightEdgeOfText, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We render the component again:'
            var image = Utility.renderSingleComponent(box)

        then : 'No pixel is painted in the selection colour:'
            boundsOfPixelsPaintedIn(image, YELLOW) == null
        and : 'The mouse cursor is still the default cursor:'
            box.getCursor().getType() == Cursor.DEFAULT_CURSOR
        and : 'The component has no `TransferHandler`, so there is nothing to copy:'
            box.getTransferHandler() == null
    }

    def 'The selection colour set with `selection(s -> s.color(..))` is painted behind the selected letters and does not reach the letters which are not selected.'()
    {
        reportInfo """
            The component paints the text "Hello world". A selection holds two positions,
            and a position is a gap between two characters: position 6 is the gap in front
            of the "w", and position 11 is the gap behind the "d". Your code puts the
            selection from 6 to 11 into the property, so "world" is selected.

            SwingTree paints a rectangle in the selection colour behind the selected letters.
            It reaches from the gap in front of the first selected letter to the gap behind the
            last selected letter, and from the top to the bottom of the line. The letters are
            painted on top of it, so they stay readable.

            The rectangle covers "world" and nothing of "Hello". It is not taller than one line.
        """
        given : """
            A component which paints "Hello " in red and "world" in blue on a white background.
            The selection colour is an opaque yellow. A selection colour is normally partly
            transparent, but then its pixels would be a mixture of yellow and white, which we
            could not tell apart from the smoothed edges of the letters:
        """
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)
        and : 'The height of one line of the Ubuntu font at a size of 20, read from its `FontMetrics`:'
            var lineHeight = new JBox().getFontMetrics(new Font("Ubuntu", Font.PLAIN, 20)).getHeight()

        when : 'We render the component and find the rectangles around the red, the blue and the yellow pixels:'
            var image = Utility.renderSingleComponent(box)
            var hello     = boundsOfPixelsPaintedIn(image, RED)
            var world     = boundsOfPixelsPaintedIn(image, BLUE)
            var highlight = boundsOfPixelsPaintedIn(image, YELLOW)

        then : 'The yellow rectangle encloses all pixels of "world":'
            highlight.x <= world.x
            highlight.x + highlight.width  >= world.x + world.width
            highlight.y <= world.y
            highlight.y + highlight.height >= world.y + world.height
        and : 'The yellow rectangle begins right of the last pixel of "Hello", because neither "Hello" nor the space behind it is selected:'
            highlight.x > hello.x + hello.width
        and : 'The yellow rectangle is not taller than one line, give or take one pixel at the top and one at the bottom for rounding:'
            highlight.height <= lineHeight + 2
    }

    def 'When your code writes a new selection into the property of `withTextSelection`, the selection colour moves to the newly selected letters and leaves the formerly selected letters.'()
    {
        reportInfo """
            The component paints the text "Hello world". A selection holds two positions,
            and a position is a gap between two characters: position 0 is the gap in front
            of the "H", position 5 is the gap behind the "o" of "Hello", position 6 is the gap
            in front of the "w", and position 11 is the gap behind the "d".

            First the property holds the selection from 0 to 5, which is "Hello", and SwingTree
            paints the selection colour behind "Hello". Then your code writes the selection from
            6 to 11, which is "world", into the property. SwingTree now paints the selection colour
            behind "world", and no longer behind "Hello".

            SwingTree keeps the image which it painted for the style of a component, and paints
            that image again as long as the style has not changed. If the selection were not part
            of the style, SwingTree would paint the old image with "Hello" selected, and the new
            selection would not show.
        """
        given : 'A property which holds the selection from 0 to 5, so "Hello" is selected:'
            var selection = Var.of(TextSelection.between(0, 5))
        and : 'A component which paints "Hello " in red and "world" in blue on white, with an opaque yellow selection colour:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red, the blue and the yellow pixels:'
            var image = Utility.renderSingleComponent(box)
            var hello     = boundsOfPixelsPaintedIn(image, RED)
            var world     = boundsOfPixelsPaintedIn(image, BLUE)
            var highlight = boundsOfPixelsPaintedIn(image, YELLOW)
        then : 'The yellow rectangle encloses "Hello" and ends left of "world":'
            highlight.x <= hello.x
            highlight.x + highlight.width >= hello.x + hello.width
            highlight.x + highlight.width < world.x

        when : 'Your code selects "world" by writing the selection from 6 to 11 into the property, and we render the component again:'
            UI.runNow { selection.set(TextSelection.between(6, 11)) }
            image     = Utility.renderSingleComponent(box)
            highlight = boundsOfPixelsPaintedIn(image, YELLOW)
        then : 'The yellow rectangle encloses "world" and begins right of "Hello":'
            highlight.x <= world.x
            highlight.x + highlight.width >= world.x + world.width
            highlight.x > hello.x + hello.width
    }

    def 'With `placement(#placement)`, the selection colour is painted behind the selected word wherever the placement puts the line.'(
        UI.Placement placement
    ) {
        reportInfo """
            The component is 400 pixels wide and paints the text "Hello world", which is much
            narrower than the component. The placement moves the line to the left side, to the
            middle or to the right side of the component, and to its top, its middle or its bottom.

            A selection holds two positions, and a position is a gap between two characters:
            position 6 is the gap in front of the "w", and position 11 is the gap behind the "d".
            The selection from 6 to 11 selects "world".

            The component has a padding of 10 pixels, so its content area runs from x = 10 to
            x = 390. SwingTree paints the selection colour at the positions at which it paints
            the selected letters, so the selection colour moves along with the line. If SwingTree
            worked out the place of the selection colour separately from the place of the letters,
            and forgot the placement, then with `TOP_RIGHT` the colour would be painted near
            x = 10 while "world" is painted near x = 390.

            In this scenario the placement is `${placement}`.
        """
        given : 'A component which paints "Hello " in red and "world" in blue on white, with an opaque yellow selection colour, and "world" selected:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(placement)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red, the blue and the yellow pixels:'
            var image = Utility.renderSingleComponent(box)
            var hello     = boundsOfPixelsPaintedIn(image, RED)
            var world     = boundsOfPixelsPaintedIn(image, BLUE)
            var highlight = boundsOfPixelsPaintedIn(image, YELLOW)

        then : 'The yellow rectangle encloses all pixels of "world":'
            highlight.x <= world.x
            highlight.x + highlight.width  >= world.x + world.width
            highlight.y <= world.y
            highlight.y + highlight.height >= world.y + world.height
        and : 'The yellow rectangle begins right of the last pixel of "Hello":'
            highlight.x > hello.x + hello.width

        where :
            placement << [
                UI.Placement.TOP_LEFT, UI.Placement.TOP, UI.Placement.TOP_RIGHT,
                UI.Placement.LEFT,     UI.Placement.CENTER,   UI.Placement.RIGHT,
                UI.Placement.BOTTOM_LEFT, UI.Placement.BOTTOM, UI.Placement.BOTTOM_RIGHT
            ]
    }

    def 'A selection across the place where a long line was wrapped paints the selection colour behind the end of the first line and the start of the second line, and nowhere else.'()
    {
        reportInfo """
            The content area of the component is only 100 pixels wide, and the text is
            "HHHH HHHH". At a font size of 20, one word of four "H" letters is about 56 pixels
            wide, so one word fits into a line, but two words do not. With `wrapLines(true)`,
            SwingTree paints the first word on the first line and the second word on the
            second line.

            A selection holds two positions, and a position is a gap between two characters.
            The selection from 2 to 7 selects five characters: the last two letters of the
            first word, the space behind the first word, and the first two letters of the
            second word.

            So in the first line the selection colour starts behind the second "H", roughly in
            the middle of the word, and reaches to the end of the line. In the second line it
            starts at the beginning of the line and ends behind the second "H", roughly in the
            middle of the word. A mistake in counting the characters of the first line would
            shift the selection colour in the second line.
        """
        given : 'A component whose content area is 100 pixels wide, with a first word in red, a second word in green, and an opaque yellow selection colour:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(2, 7)))
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red and the green pixels:'
            var image = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)
        and : """
            We look for yellow pixels separately in the pixel rows of each word. Inside the rows
            of one word, only the selection colour of that line can appear, because the selection
            colour of a line is never taller than the line:
        """
            var firstRows  = new Rectangle(0, firstWord.y as int,  image.width, firstWord.height as int)
            var secondRows = new Rectangle(0, secondWord.y as int, image.width, secondWord.height as int)
            var highlightInFirstLine  = boundsOfPixelsPaintedIn(image, YELLOW, firstRows)
            var highlightInSecondLine = boundsOfPixelsPaintedIn(image, YELLOW, secondRows)

        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : """
            In the first line, the yellow pixels begin between 30% and 70% of the width of the
            first word, because two of its four letters are not selected, and they reach to its end:
        """
            highlightInFirstLine.x > firstWord.x + firstWord.width * 0.3
            highlightInFirstLine.x < firstWord.x + firstWord.width * 0.7
            highlightInFirstLine.x + highlightInFirstLine.width >= firstWord.x + firstWord.width
        and : """
            In the second line, the yellow pixels begin at the start of the second word, and
            end between 30% and 70% of its width, because only two of its four letters are selected:
        """
            highlightInSecondLine.x <= secondWord.x
            highlightInSecondLine.x + highlightInSecondLine.width > secondWord.x + secondWord.width * 0.3
            highlightInSecondLine.x + highlightInSecondLine.width < secondWord.x + secondWord.width * 0.7
    }

    def 'With `obstacles(..)`, a selection of the letters on both sides of an obstacle paints the selection colour on both sides, and none on the obstacle.'()
    {
        reportInfo """
            An obstacle is a shape which the text flows around. Here a rectangle from
            x = 150 to x = 250 is in the way of the first line. The content area of the
            component reaches from x = 10 to x = 390, so the first line has room for letters
            on two sides: from 10 to 150 on the left, and from 250 to 390 on the right.

            The text is "HHHHH HHHHHHH HHHHHHH". At a font size of 20 an "H" is about 14
            pixels wide. The first word and the space behind it fit on the left side, the
            second word is painted on the right side, and the third word goes to the second line.

            A selection holds two positions, and a position is a gap between two characters.
            The selection from 0 to 13 selects the first word, the space behind it, and the
            second word. So SwingTree paints the selection colour behind the first word on the
            left side and behind the second word on the right side. It paints none on the
            obstacle, because no letter is painted there, and none on the second line, because
            the third word is not selected.
        """
        given : 'A rectangle from x = 150 to x = 250 across the first line:'
            var obstacle = new Rectangle(150, 0, 100, 40)
        and : 'A component which paints three words in red, green and blue on white, with an opaque yellow selection colour and the first two words selected:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacle)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHHH "   ),
                                StyledString.of( f -> f.color(GREEN), "HHHHHHH " ),
                                StyledString.of( f -> f.color(BLUE),  "HHHHHHH"  )
                            )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 13)))
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red, the green and the blue pixels:'
            var image = Utility.renderSingleComponent(box)
            var leftOfObstacle  = boundsOfPixelsPaintedIn(image, RED)
            var rightOfObstacle = boundsOfPixelsPaintedIn(image, GREEN)
            var secondLine      = boundsOfPixelsPaintedIn(image, BLUE)
        and : 'We look for yellow pixels left of the obstacle, right of it, on it, and in the rows of the second line:'
            var leftSide  = boundsOfPixelsPaintedIn(image, YELLOW, new Rectangle(0,   0, 150, 40))
            var rightSide = boundsOfPixelsPaintedIn(image, YELLOW, new Rectangle(250, 0, 150, 40))
            var onObstacle            = boundsOfPixelsPaintedIn(image, YELLOW, obstacle)
            var inRowsOfTheSecondLine = boundsOfPixelsPaintedIn(image, YELLOW, new Rectangle(0, secondLine.y as int, image.width, secondLine.height as int))

        then : 'The yellow pixels left of the obstacle enclose the red word horizontally:'
            leftSide.x <= leftOfObstacle.x
            leftSide.x + leftSide.width >= leftOfObstacle.x + leftOfObstacle.width
        and : 'The yellow pixels right of the obstacle enclose the green word horizontally:'
            rightSide.x <= rightOfObstacle.x
            rightSide.x + rightSide.width >= rightOfObstacle.x + rightOfObstacle.width
        and : 'There are no yellow pixels on the obstacle and none in the second line:'
            onObstacle == null
            inRowsOfTheSecondLine == null
    }

    def 'An empty selection paints nothing in the selection colour.'()
    {
        reportInfo """
            A selection holds two positions, and a position is a gap between two characters.
            When both positions are the same gap, no character lies between them, so nothing
            is selected. Here both positions are the gap behind the "o" of "Hello".

            Nothing is selected, so SwingTree paints nothing in the selection colour.
        """
        given : 'A component which paints "Hello world" in red on white, with an opaque yellow selection colour and an empty selection behind the "o":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(5)))
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)

        then : 'The text is painted:'
            boundsOfPixelsPaintedIn(image, RED) != null
        and : 'No pixel is painted in the selection colour:'
            boundsOfPixelsPaintedIn(image, YELLOW) == null
    }

    def 'Unless you call `selection(s -> s.showCaret(true))`, SwingTree does not paint the caret.'()
    {
        reportInfo """
            The caret is a thin vertical bar which marks the position where the selection
            continues when your user shift-clicks. A selection holds two positions, and a
            position is a gap between two characters. Here both positions are the gap behind
            the "o" of "Hello", so nothing is selected and only the caret could be painted.

            Most texts which your users can select are labels and descriptions, which nobody
            types into, and a caret would make them look like a text field which your users can type into. So SwingTree paints
            the caret only when you ask for it with `showCaret(true)`. This scenario gives the
            caret a green colour with `caretColor(..)`, but does not call `showCaret(true)`,
            so no green pixel is painted.
        """
        given : 'A component which paints "Hello world" in red on white, with a green caret colour and an empty selection behind the "o":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                            .selection( s -> s.color(YELLOW).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(5)))
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)

        then : 'The text is painted:'
            boundsOfPixelsPaintedIn(image, RED) != null
        and : 'No pixel is painted in the caret colour:'
            boundsOfPixelsPaintedIn(image, GREEN) == null
    }

    def 'With `selection(s -> s.showCaret(true))`, an empty selection paints the caret as a thin bar in the caret colour, in the gap between the two letters where the caret is.'()
    {
        reportInfo """
            The caret is a thin vertical bar which marks the position where the selection
            continues when your user shift-clicks. A selection holds two positions, and a
            position is a gap between two characters. The text is "HHHH HHHH", and both
            positions are 4, which is the gap behind the fourth "H" of the first word.
            Nothing is selected, so SwingTree paints the caret into that gap.

            The caret is at most 3 pixels wide: 1 pixel, plus up to one partly covered pixel
            on each side, because the gap may lie between two pixel columns. It is at least as
            tall as the letters. It is right of the last pixel of the first word, give or take
            one pixel for smoothing, and at most 4 pixels further right, because the letter "H"
            in the Ubuntu font has a few empty pixel columns right of its last stroke. It is left of
            the second word, because the space between the words lies between them.

            SwingTree does not let the caret blink. A blinking caret would need the component
            to be painted again twice a second for as long as it is shown.
        """
        given : 'A component which paints "HHHH " in red and "HHHH" in blue on white, with a green caret behind the fourth "H":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "HHHH " ),
                                StyledString.of( f -> f.color(BLUE), "HHHH"  )
                            )
                            .selection( s -> s.color(YELLOW).showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(4)))
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red, the blue and the green pixels:'
            var image = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, BLUE)
            var caret      = boundsOfPixelsPaintedIn(image, GREEN)

        then : 'The caret is a thin bar, at least as tall as the letters:'
            caret.width  <= 3
            caret.height >= firstWord.height
        and : 'The caret is in the gap behind the first word:'
            caret.x >= firstWord.x + firstWord.width - 1
            caret.x <= firstWord.x + firstWord.width + 4
            caret.x + caret.width <= secondWord.x
        and : 'Nothing is selected, so no pixel is painted in the selection colour:'
            boundsOfPixelsPaintedIn(image, YELLOW) == null
    }

    def 'With `selection(s -> s.borderRadius(#radius))`, the corners of the area painted in the selection colour are #shape.'(
        int radius, String shape, boolean cornerIsPaintedInSelectionColour
    ) {
        reportInfo """
            `borderRadius(..)` rounds the corners of the area which SwingTree paints in the
            selection colour behind the selected letters. With a radius of 0 the corners are
            square. With a radius of 8 every corner is a quarter of a circle with a radius
            of 8 pixels.

            We look at the pixel which lies one pixel to the right of and one pixel below the
            top left corner of the rectangle around the pixels in the selection colour. That pixel
            covers the square from 1 to 2 pixels right of the corner and from 1 to 2 pixels below it,
            so its centre is 1.5 pixels right of and 1.5 pixels below the corner. A square corner
            covers that pixel. The quarter circle of a corner with a radius of 8 pixels has its
            centre 8 pixels right of and 8 pixels below the corner. The centre of the pixel is
            6.5 pixels away from it in both directions, so its distance from the centre of the
            circle is √(6.5² + 6.5²) ≈ 9.2 pixels. That is more than 8, so the pixel lies outside
            the rounded corner.

            In this scenario the radius is ${radius}.
        """
        given : 'A component which paints "Hello world" in blue on white, with an opaque yellow selection colour, and "world" selected:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> s.color(YELLOW).borderRadius(radius) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)

        when : 'We render the component and find the rectangle around the yellow pixels:'
            var image = Utility.renderSingleComponent(box)
            var highlight = boundsOfPixelsPaintedIn(image, YELLOW)
        and : 'We look at the pixel one pixel right of and one pixel below its top left corner:'
            var nearCorner = new Rectangle((highlight.x + 1) as int, (highlight.y + 1) as int, 1, 1)

        then :
            (boundsOfPixelsPaintedIn(image, YELLOW, nearCorner) != null) == cornerIsPaintedInSelectionColour

        where :
            radius | shape     || cornerIsPaintedInSelectionColour
            0      | "square"  || true
            8      | "rounded" || false
    }

    def 'With `withTextSelection("body", selection)` and an obstacle across the full width of the content area in the middle of a paragraph, the positions below the obstacle continue the positions above it.'()
    {
        reportInfo """
            An obstacle is a shape which you pass to `obstacles(..)`. SwingTree paints no text on
            an obstacle. When an obstacle covers the content area from its left edge to its right
            edge, no letter of a line fits next to it, so SwingTree paints no text at the height of
            the obstacle and continues the text below it. The content area is the component minus
            its margin, its border and its padding.

            A selection holds two positions, and a position is a gap between two characters.
            The obstacle only moves letters down. It adds no character to the text, so the
            positions of the letters below the obstacle continue the positions of the letters
            above it.

            The text has two paragraphs. The first paragraph is "HHHHHHHHHH NNNNNNNNNN" and the
            second paragraph is "EEEEEEEEEE". The first paragraph needs two lines, and the obstacle
            lies between these two lines. Position 11 is the gap in front of the first "N", and
            position 21 is the gap behind the last "N". Position 22 is the gap behind the line
            break between the two paragraphs, in front of the first "E".

            Your user drags the mouse across the word "NNNNNNNNNN" below the obstacle, so the
            selection goes from position 11 to position 21, and copying it gives "NNNNNNNNNN".
            If SwingTree counted the height which it skips at the obstacle as the start of a new
            paragraph, it would add the 11 characters of the first line and one line break which
            the text does not contain to every position below the obstacle. The selection would
            then begin behind position 21, and copying would give letters of the second paragraph
            instead of the word which your user dragged across.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : """
            A rectangle which covers the full width of the component from y = 40 to y = 80.
            The component is 240 pixels wide and has a padding of 10 pixels, so its content area
            reaches from x = 10 to x = 230 and from y = 10 to y = 210. The rectangle covers the
            content area from its left edge to its right edge between y = 40 and y = 80:
        """
            var obstacle = new Rectangle(0, 40, 240, 40)
        and : """
            A component which paints "HHHHHHHHHH " in red, "NNNNNNNNNN" in green, and a second
            paragraph "EEEEEEEEEE" in blue. At a font size of 20, an "H" and an "N" are about 14
            pixels wide, so the red word and the green word are each about 140 pixels wide, and
            one of them fits into the 220 pixels of a line. The red word, a space and the green
            word need about 140 + 5 + 140 = 285 pixels, so the green word does not fit behind the
            red word and goes to the next line.
            The first line starts at the top of the content area at y = 10 and is about 22.4
            pixels tall, so it ends above the obstacle at y = 40. The next line would overlap
            the obstacle, so SwingTree paints the green word below y = 80:
        """
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(240, 220)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacle)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHHHHHHHH "  ),
                                StyledString.of( f -> f.color(GREEN), "NNNNNNNNNN\n" ),
                                StyledString.of( f -> f.color(BLUE),  "EEEEEEEEEE"   )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangles around the red, the green and the blue
            pixels. The leftmost green pixel column belongs to the left half of the first "N", and
            the rightmost green pixel column belongs to the right half of the last "N":
        """
            var image = Utility.renderSingleComponent(box)
            var redWord   = boundsOfPixelsPaintedIn(image, RED)
            var greenWord = boundsOfPixelsPaintedIn(image, GREEN)
            var blueWord  = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfGreenWord  = greenWord.x
            int rightEdgeOfGreenWord = greenWord.x + greenWord.width - 1
            int middleOfGreenWord    = greenWord.centerY

        when : 'We press the mouse button on the left edge of the green word, drag to its right edge, and release the button there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfGreenWord,  middleOfGreenWord, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfGreenWord, middleOfGreenWord, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfGreenWord, middleOfGreenWord, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The red word lies above the obstacle, and the green word and the blue word lie below it:'
            redWord.y + redWord.height <= 40
            greenWord.y >= 80
            blueWord.y  >= greenWord.y + greenWord.height
        and : 'The selection goes from the gap in front of the first "N" to the gap behind the last "N":'
            selection.get().anchor() == 11
            selection.get().caret()  == 21
        and : 'The copied text is the green word:'
            clipboard.getData(DataFlavor.stringFlavor) == "NNNNNNNNNN"
    }

    def 'With `withTextSelection("body", selection)` and an obstacle across the full width of the content area #obstaclePlace, the positions of the second paragraph count every character of the first paragraph.'(
        String obstaclePlace, Rectangle obstacle
    ) {
        reportInfo """
            An obstacle is a shape which you pass to `obstacles(..)`. SwingTree paints no text on
            an obstacle. When an obstacle covers the content area from its left edge to its right
            edge, no letter of a line fits next to it, so SwingTree paints no text at the height of
            the obstacle and continues the text below it. The content area is the component minus
            its margin, its border and its padding.

            A selection holds two positions, and a position is a gap between two characters.
            The text is "HHHHHHHHHH", a line break, and "NNNNNNNNNN", so it has two paragraphs.
            Position 10 is the gap in front of the line break, position 11 is the gap behind it,
            in front of the first "N", and position 21 is the gap behind the last "N". An obstacle
            only moves letters down. It adds no character to the text, so these positions are the
            same wherever the obstacle is.

            Your user drags the mouse across the word "NNNNNNNNNN", so the selection goes from
            position 11 to position 21, and copying it gives "NNNNNNNNNN". SwingTree finds the
            first position of a paragraph by adding the characters of the line above it, plus one
            for the line break. If SwingTree took the height which it skips at the obstacle for
            that line above, it would add only the one line break and none of the 10 characters
            of "HHHHHHHHHH". The positions would then be too small, and copying would give letters
            of the first paragraph or too few letters.

            In this scenario the obstacle lies ${obstaclePlace}.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : """
            A component which is 300 pixels wide and 200 pixels tall and has a padding of 10 pixels,
            so its content area reaches from x = 10 to x = 290 and from y = 10 to y = 190. It paints
            "HHHHHHHHHH" in red, a line break, and "NNNNNNNNNN" in green. At a font size of 20, a word
            of 10 of these letters is about 140 pixels wide, so each paragraph fits into one line.
            One line is less than 30 pixels tall, so the two lines and the 40 pixels of the obstacle
            need less than 30 + 30 + 40 = 100 pixels and fit into the 180 pixels of the content area:
        """
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(300, 200)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacle)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHHHHHHHH\n" ),
                                StyledString.of( f -> f.color(GREEN), "NNNNNNNNNN"   )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangles around the red and the green pixels.
            The leftmost green pixel column belongs to the left half of the first "N", and the
            rightmost green pixel column belongs to the right half of the last "N":
        """
            var image = Utility.renderSingleComponent(box)
            var redWord   = boundsOfPixelsPaintedIn(image, RED)
            var greenWord = boundsOfPixelsPaintedIn(image, GREEN)
            int leftEdgeOfGreenWord  = greenWord.x
            int rightEdgeOfGreenWord = greenWord.x + greenWord.width - 1
            int middleOfGreenWord    = greenWord.centerY

        when : 'We press the mouse button on the left edge of the green word, drag to its right edge, and release the button there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfGreenWord,  middleOfGreenWord, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfGreenWord, middleOfGreenWord, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfGreenWord, middleOfGreenWord, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'No letter is painted on the obstacle, and the green word is painted below the red word:'
            boundsOfPixelsPaintedIn(image, RED,   obstacle) == null
            boundsOfPixelsPaintedIn(image, GREEN, obstacle) == null
            greenWord.y >= redWord.y + redWord.height
        and : 'The selection goes from the gap in front of the first "N" to the gap behind the last "N":'
            selection.get().anchor() == 11
            selection.get().caret()  == 21
        and : 'The copied text is the green word:'
            clipboard.getData(DataFlavor.stringFlavor) == "NNNNNNNNNN"

        where : """
            The obstacle is a rectangle across the full width of the component. Above the first
            paragraph, it covers the content area from y = 10 to y = 40, so the text starts below
            y = 40. Between the two paragraphs, it covers the content area from y = 40 to y = 80:
            the first line starts at y = 10 and ends above y = 40, and the second line would overlap
            the obstacle, so SwingTree paints it below y = 80:
        """
            obstaclePlace                   | obstacle
            "above the first paragraph"     | new Rectangle(0,  0, 300, 40)
            "between the two paragraphs"    | new Rectangle(0, 40, 300, 40)
    }

    def 'After SwingTree has painted a text made selectable with `withTextSelection("body", selection)`, it writes the selected characters into the property, and it writes after the paint, not during it.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters. A selection
            holds two positions, and a position is a gap between two characters: position 6 is
            the gap in front of the "w", and position 11 is the gap behind the "d", which is the
            end of the text.

            Your code puts a selection from position 6 to position 50 into the property, for example
            because it kept the selection from a longer text. A `TextSelection` which your code creates
            holds only its two positions, so its `selectedText()` is an empty string. When SwingTree
            paints the text, it works out which characters the positions select. Then it writes a new
            selection into the property: position 50 lies behind the end of the text, so the new
            selection goes from position 6 to position 11, and its `selectedText()` is "world".

            SwingTree writes the new selection after the paint has finished. A listener of the property
            may change a component, for example the text of a label. Such a change makes Swing lay out
            and paint parts of the window again. If SwingTree wrote into the property in the middle of a
            paint, the window could show a mix of the old and the new state, and the layout of the window
            could change while its pixels are being drawn.
        """
        given : 'A property which holds a selection from position 6 to position 50:'
            var selection = Var.of(TextSelection.between(6, 50))
        and : """
            A `JBox` with a field `isBeingPainted`, which its `paint` method sets to true before it
            paints and back to false after it has painted:
        """
            var box = new JBox() {
                boolean isBeingPainted = false
                @Override void paint( Graphics g ) {
                    isBeingPainted = true
                    try { super.paint(g) } finally { isBeingPainted = false }
                }
            }
        and : 'A listener which records, for every new selection in the property, whether the component was being painted at that moment:'
            var paintingWhileWritten = new CopyOnWriteArrayList<Boolean>()
            selection.onChange(From.VIEW, change -> paintingWhileWritten.add(box.isBeingPainted) )
        and : 'We let the box paint "Hello world" and make the text selectable:'
            UI.of(box)
            .withStyle( it -> it
                .size(400, 100)
                .padding(10)
                .text("body", t -> t
                    .font( f -> f.family("Ubuntu").size(20) )
                    .content("Hello world")
                )
            )
            .withTextSelection("body", selection)

        when : """
            We paint the component on the UI thread. When SwingTree paints the component, it queues
            a task on the UI thread which writes the new selection into the property. `UI.sync()`
            waits until the UI thread has run every task queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()

        then : 'The property holds a selection from position 6 to position 11, the end of the text:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
        and : 'Its selected characters are "world":'
            selection.get().selectedText() == "world"
        and : 'SwingTree wrote into the property at least once, and never while the component was being painted:'
            !paintingWhileWritten.isEmpty()
            !paintingWhileWritten.contains(true)
    }

    def 'With `EventProcessor.DECOUPLED`, when your code writes a selection into the property of `withTextSelection` while a task of SwingTree, which fills in the selected characters of the painted selection, still waits on the application thread, the property ends with the selection of your code.'()
    {
        reportInfo """
            A Swing program paints every component on one thread, the UI thread. With
            `EventProcessor.DECOUPLED`, your code writes into properties on a second thread,
            the application thread, and SwingTree writes into the property of `withTextSelection`
            by handing a task to the application thread. The two threads run at the same time,
            so your code can write a new selection while a task of SwingTree waits on the
            application thread.

            The component paints the text "Hello world". A selection holds two positions, and a
            position is a gap between two characters: position 0 is the gap in front of the "H",
            position 5 is the gap behind the "o" of "Hello", position 6 is the gap in front of the
            "w", and position 11 is the gap behind the "d".

            At first the property holds the selection from 0 to 5, which selects "Hello". SwingTree
            paints it and hands a task to the application thread which fills in the selected characters.
            Before the application thread runs that task, your code writes the selection from 6 to 11,
            which selects "world". Then the application thread runs the task of SwingTree.

            The task of SwingTree reads the selection which the property holds when the task runs, and
            fills in the characters which that selection selects. So the property ends with the
            selection from 6 to 11 and the selected characters "world". If the task wrote the selection
            which SwingTree had painted before, it would put the selection from 0 to 5 back into the
            property, and the selection which your code wrote would be lost.
        """
        given : 'A property which holds the selection from 0 to 5, so "Hello" is selected:'
            var selection = Var.of(TextSelection.between(0, 5))
        and : """
            A component which paints "Hello world". We declare it inside
            `UI.use(EventProcessor.DECOUPLED, ..)`, so that it hands its writes into the property
            over to the application thread. We declare it on the UI thread with `UI.runAndGet`,
            because with `EventProcessor.DECOUPLED` the method `get(..)`, which returns the component,
            throws an exception on every other thread:
        """
            var box = UI.runAndGet({ ->
                UI.use(EventProcessor.DECOUPLED, () ->
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                ).get(JBox)
            })

        when : """
            We paint the component and wait with `UI.sync()` until the UI thread has run every task
            which it received so far. In this scenario no thread runs the tasks of the application thread
            on its own. The test thread plays the application thread, so every task which SwingTree hands
            to the application thread waits until the test thread runs it:
        """
            Utility.renderSingleComponent(box)
            UI.sync()
        then : 'SwingTree has not written into the property yet, so the property holds the selection from 0 to 5 without selected characters:'
            selection.get().anchor() == 0
            selection.get().caret()  == 5
            selection.get().selectedText() == ""

        when : 'Your code writes the selection from 6 to 11 into the property, on the test thread, which plays the application thread:'
            selection.set(TextSelection.between(6, 11))
        and : """
            We let the application thread and the UI thread run every waiting task, until neither of
            them has a task left. `EventProcessor.DECOUPLED.joinUntilDoneOrException()` runs the tasks
            of the application thread on the test thread. `UI.sync()` waits for the UI thread.
            Between the two, we paint the component again, as Swing would after the property changed:
        """
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()
            Utility.renderSingleComponent(box)
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()

        then : 'The property holds the selection from 6 to 11 which your code wrote:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
        and : 'Its selected characters are "world":'
            selection.get().selectedText() == "world"
    }

    def 'With `withTextSelection(selection)`, your users can select the text which `text(t -> ..)` declares without a name.'()
    {
        reportInfo """
            A style can declare a text without a name, with `text(t -> ..)`. Most components
            only need one text, so this is the shortest way to declare it. `withTextSelection(selection)`,
            which takes only the property, makes exactly this text selectable.

            The component paints the text "Hello world", which has 11 characters. A selection holds
            two positions, and a position is a gap between two characters: position 6 is the gap in
            front of the "w", and position 11 is the gap behind the "d". Your user drags the mouse
            across "world", so the selection goes from position 6 to position 11, and copying it gives
            "world". If `withTextSelection(selection)` looked for a text with some other name, it would
            find no text, and dragging the mouse would select nothing.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue, with a text that has no name:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text( t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection(selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangle around the blue pixels of "world".
            Its leftmost pixel column belongs to the left half of the "w" and its rightmost
            pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'We press the mouse button on the left edge of the "w", drag to the right edge of the "d", and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The selection goes from the gap in front of the "w" to the gap behind the "d":'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
        and : 'The copied text is "world":'
            clipboard.getData(DataFlavor.stringFlavor) == "world"
    }

    def 'With `withTextSelection(UI.Layer.FOREGROUND, "caption", selection)`, your users can select a text which the style declares in its foreground with `text(UI.Layer.FOREGROUND, "caption", ..)`.'()
    {
        reportInfo """
            SwingTree paints the style of a component in four parts, one after the other: the
            background, the content, the border and the foreground. `text("caption", ..)` declares
            a text in the content part, and `text(UI.Layer.FOREGROUND, "caption", ..)` declares a text
            in the foreground part, which SwingTree paints after the other three parts. Two texts in
            different parts may have the same name, so `withTextSelection` takes the part and the name.

            The component paints the text "Hello world" in its foreground. A selection holds two
            positions, and a position is a gap between two characters: position 6 is the gap in
            front of the "w", and position 11 is the gap behind the "d". Your user drags the mouse
            across "world", so the selection goes from position 6 to position 11, copying it gives
            "world", and SwingTree paints the selection colour behind "world". If SwingTree looked
            for the text named "caption" in the content part of the style, it would find no text
            there: dragging the mouse would select nothing, or the selection colour would not be painted.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue on white, with an opaque yellow selection colour, in a text named "caption" in the foreground of its style:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text(UI.Layer.FOREGROUND, "caption", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection(UI.Layer.FOREGROUND, "caption", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangle around the blue pixels of "world".
            Its leftmost pixel column belongs to the left half of the "w" and its rightmost
            pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'We press the mouse button on the left edge of the "w", drag to the right edge of the "d", and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }
        and : 'We render the component again and find the rectangle around the yellow pixels:'
            var highlight = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), YELLOW)

        then : 'The selection goes from the gap in front of the "w" to the gap behind the "d":'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
        and : 'The copied text is "world":'
            clipboard.getData(DataFlavor.stringFlavor) == "world"
        and : 'The yellow pixels enclose "world":'
            highlight.x <= world.x
            highlight.x + highlight.width >= world.x + world.width
    }

    def 'When the selection in the property of `withTextSelection` is disabled with `withEnabled(false)`, SwingTree paints no selection, the mouse and the keyboard select and copy nothing, and the mouse cursor stays the default cursor.'()
    {
        reportInfo """
            `isEnabled()` of a `TextSelection` tells whether your users can select characters.
            Your code turns selecting off with `withEnabled(false)` and on again with `withEnabled(true)`,
            for example while the text is being edited somewhere else.

            The component paints the text "Hello world". A selection holds two positions, and a
            position is a gap between two characters: position 6 is the gap in front of the "w",
            and position 11 is the gap behind the "d". The property holds the selection from
            6 to 11, which selects "world", but the selection is disabled.

            While the selection is disabled, SwingTree paints no selection colour. Dragging the
            mouse across "Hello" and the keyboard shortcut for selecting everything leave the
            positions in the property as they are, so the selection still goes from 6 to 11.
            Copying puts nothing onto the clipboard, and the mouse cursor stays the default arrow
            cursor over the text, so that nothing suggests to your users that they can select the text.

            When your code enables the selection again, SwingTree paints the selection colour behind
            "world" again, because the property still holds the selection from 6 to 11.
        """
        given : 'A property which holds the disabled selection from 6 to 11:'
            var selection = Var.of(TextSelection.between(6, 11).withEnabled(false))
        and : 'A component which paints "Hello " in red and "world" in blue on white, with an opaque yellow selection colour:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red, the blue and the yellow pixels:'
            var image = Utility.renderSingleComponent(box)
            var hello = boundsOfPixelsPaintedIn(image, RED)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
        then : 'No pixel is painted in the selection colour:'
            boundsOfPixelsPaintedIn(image, YELLOW) == null

        when : """
            We move the mouse onto the left edge of "Hello", press the button there, drag to the right
            edge of "Hello" and release it. The leftmost red pixel column belongs to the left half of
            the "H", and the rightmost red pixel column belongs to the right half of the "o":
        """
            int leftEdgeOfH  = hello.x
            int rightEdgeOfO = hello.x + hello.width - 1
            int lineMiddle   = hello.centerY
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_MOVED,    0, 0, leftEdgeOfH, lineMiddle, 0, false, MouseEvent.NOBUTTON))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  1, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfH, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  2, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfO, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, rightEdgeOfO, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : """
            We send the keyboard shortcut for selecting everything, which is Ctrl+A on Windows and Linux
            and Command+A on macOS. We hand the key event to the component with
            `KeyboardFocusManager.redispatchEvent`, because a component outside of a window never has
            the keyboard focus, and only the component with the keyboard focus receives a key event sent
            with `dispatchEvent`:
        """
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED)
                )
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }
        and : '`UI.sync()` waits until the UI thread has run every task queued so far:'
            UI.sync()

        then : 'The property still holds the disabled selection from 6 to 11:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
            !selection.get().isEnabled()
        and : 'The clipboard is empty:'
            clipboard.getContents(null) == null
        and : 'The mouse cursor is the default cursor:'
            box.getCursor().getType() == Cursor.DEFAULT_CURSOR

        when : 'Your code enables the selection, and we render the component again:'
            UI.runNow { selection.set(selection.get().withEnabled(true)) }
            image = Utility.renderSingleComponent(box)
            var highlight = boundsOfPixelsPaintedIn(image, YELLOW)
        then : 'The yellow pixels enclose "world" and begin right of "Hello":'
            highlight.x <= world.x
            highlight.x + highlight.width >= world.x + world.width
            highlight.x > hello.x + hello.width
    }

    def 'Without `withTextSelection`, a text whose style calls `selection(s -> s.showCaret(true))` paints no caret.'()
    {
        reportInfo """
            `selection(s -> ..)` in the style of a text only describes how a selection looks.
            Text selection is something you switch on for a component with `withTextSelection`.
            On a component without it, SwingTree paints no selection colour and no caret, even
            when the style of the text asks for a caret with `showCaret(true)`.

            If SwingTree painted the caret here, every component whose style shares a selection look
            with selectable texts, for example through a style sheet, would show a thin bar in front
            of its first letter, and your users would take it for a text field.
        """
        given : 'A component which paints "Hello world" in red on white, with a green caret, but without `withTextSelection`:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                            .selection( s -> s.showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)

        then : 'The text is painted:'
            boundsOfPixelsPaintedIn(image, RED) != null
        and : 'No pixel is painted in the caret colour:'
            boundsOfPixelsPaintedIn(image, GREEN) == null
    }

    def 'The text style printed by `toString()` mentions a selection only when the style describes one with `selection(..)`.'()
    {
        reportInfo """
            The `toString()` method of the style of a component prints every part of the style,
            and many programs write it into their log files. A text whose style does not call
            `selection(..)` prints exactly what it printed before text selection existed, so a log
            file of a program which does not use text selection does not change. A text whose
            style calls `selection(..)` prints the selection look as `selection=TextSelectionConf[..]`.
        """
        given : 'A component with a text whose style does not call `selection(..)`, and a component with a text whose style does:'
            var plain =
                    UI.box()
                    .withStyle( it -> it.text("body", t -> t.content("Hello world")) )
                    .get(JBox)
            var withLook =
                    UI.box()
                    .withStyle( it -> it.text("body", t -> t.content("Hello world").selection( s -> s.color(YELLOW) )) )
                    .get(JBox)

        when : 'We print the styles of both components:'
            var plainStyle    = ComponentBackend.powering(plain).getStyle().toString()
            var withLookStyle = ComponentBackend.powering(withLook).getStyle().toString()

        then : 'The style of the first text ends with its last setting from before text selection existed, and mentions no selection:'
            plainStyle.contains("obstaclesFromChildrenEnabled=true]")
            !plainStyle.contains("selection=")
        and : 'The style of the second text prints its selection look:'
            withLookStyle.contains("selection=TextSelectionConf[")
    }

    def 'When the text gets shorter than the selection in the property of `withTextSelection`, SwingTree moves the positions which lie behind the end of the text to the end of the text and updates `selectedText()`.'()
    {
        reportInfo """
            A selection holds two positions, and a position is a gap between two characters.
            The text is "Hello world" at first. Position 6 is the gap in front of the "w", and
            position 11 is the gap behind the "d". The property holds the selection from 6 to 11,
            and its `selectedText()` is "world".

            Then your code changes the text to "Hello wo", which has only 8 characters. Position 11
            no longer exists. After SwingTree has painted the new text, the property holds the
            selection from 6 to 8, the end of the new text, and its `selectedText()` is "wo".
            If SwingTree kept position 11, a selection of the new text would reach behind its end,
            and your code would have to check every position before it uses it.
        """
        given : 'A property which holds the selection from 6 to 11:'
            var selection = Var.of(TextSelection.between(6, 11))
        and : 'The text which the component paints, held in a variable which the style reads every time SwingTree computes the style:'
            var content = "Hello world"
        and : 'A component which paints that text:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(content)
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We paint the component. When SwingTree paints the component, it queues a task on the UI
            thread which writes the selected characters into the property. `UI.sync()` waits until
            the UI thread has run every task queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()
        then : 'The property selects "world":'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
            selection.get().selectedText() == "world"

        when : 'Your code shortens the text to "Hello wo", and we paint the component again:'
            content = "Hello wo"
            Utility.renderSingleComponent(box)
            UI.sync()
        then : 'The selection goes from 6 to 8, the end of the new text, and selects "wo":'
            selection.get().anchor() == 6
            selection.get().caret()  == 8
            selection.get().selectedText() == "wo"
    }

    def 'The `selectedText()` of the selection in the property of `withTextSelection` holds the painted characters, with one line break between two paragraphs, and leaves out a `StyledString` which is not painted.'()
    {
        reportInfo """
            The content of the text consists of five parts: "Hello", an empty string, "HIDDEN" with
            a font size of 0, a line break, and "world". SwingTree paints neither the empty string
            nor a part with a font size of 0, so the painted text is "Hello", a line break, and "world".
            These are 11 characters: 5 letters, the line break, and 5 letters.

            A selection holds two positions, and a position is a gap between two characters of the
            painted text. Position 3 is the gap behind "Hel", position 5 is the gap in front of the
            line break, position 6 is the gap behind it, and position 8 is the gap behind "wo".
            The selection from 3 to 8 therefore selects "lo", the line break, and "wo".
            If SwingTree counted the letters of "HIDDEN", the positions would point six characters
            too far to the left, and `selectedText()` would hold letters which your users never saw.
        """
        given : 'A property which holds the selection from 3 to 8:'
            var selection = Var.of(TextSelection.between(3, 8))
        and : 'A component whose text has an empty part, an invisible part and two paragraphs:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( "Hello" ),
                                StyledString.of( "" ),
                                StyledString.of( f -> f.size(0), "HIDDEN" ),
                                StyledString.of( "\nworld" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We paint the component. When SwingTree paints the component, it queues a task on the UI
            thread which writes the selected characters into the property. `UI.sync()` waits until
            the UI thread has run every task queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()

        then : 'The selected characters are "lo", a line break, and "wo":'
            selection.get().selectedText() == "lo\nwo"
    }

    def 'With `EventProcessor.DECOUPLED`, when your code writes two selections into the property of `withTextSelection` before the UI thread has painted the first one, the property ends with the second selection, and SwingTree stops writing into it.'()
    {
        reportInfo """
            A Swing program paints every component on one thread, the UI thread. With
            `EventProcessor.DECOUPLED`, your code writes into properties on a second thread,
            the application thread. Each selection which your code writes reaches the UI thread
            as a task, and after each paint SwingTree hands a task back to the application thread,
            which fills in the selected characters.

            The component paints the text "Hello world". A selection holds two positions, and a
            position is a gap between two characters: position 0 is the gap in front of the "H",
            position 5 is the gap behind the "o" of "Hello", position 6 is the gap in front of the
            "w", and position 11 is the gap behind the "d".

            While the UI thread is busy, your code writes the selection from 0 to 5 and then the
            selection from 6 to 11. Both reach the UI thread one after the other, and SwingTree paints
            the component after each of them. The task which SwingTree hands to the application thread
            fills in the characters of the selection which the property holds when the task runs. So the
            property ends with the selection from 6 to 11 and the characters "world". A further paint
            finds that the property already holds the selected characters, so SwingTree writes nothing
            more, and a listener of the property is not called again.

            If each task wrote the selection which the UI thread had just painted, the task for the
            selection from 0 to 5 would put that selection back after your code had written the
            selection from 6 to 11, and the property would end with the wrong selection.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character, and a listener which counts every new selection:'
            var selection = Var.of(TextSelection.caretAt(0))
            var changes = new java.util.concurrent.atomic.AtomicInteger(0)
            selection.onChange(From.ALL, change -> changes.incrementAndGet() )
        and : """
            A component which paints "Hello world". We declare it inside
            `UI.use(EventProcessor.DECOUPLED, ..)`, so that it hands its writes into the property
            over to the application thread. We declare it on the UI thread with `UI.runAndGet`,
            because with `EventProcessor.DECOUPLED` the method `get(..)`, which returns the component,
            throws an exception on every other thread:
        """
            var box = UI.runAndGet({ ->
                UI.use(EventProcessor.DECOUPLED, () ->
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                ).get(JBox)
            })
        and : """
            In this scenario no thread runs the tasks of the application thread on its own. The test
            thread plays the application thread: `EventProcessor.DECOUPLED.joinUntilDoneOrException()`
            runs every task which waits for the application thread. `UI.sync()` waits until the UI thread
            has run every task queued so far. We paint the component, and then let both threads run their
            tasks until neither has a task left:
        """
            var paint = { ->
                var image = new BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB)
                Utility.paintWithoutWindow(box, image.createGraphics())
            }
            UI.runNow(paint)
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()

        when : """
            We keep the UI thread busy with a task which waits for a latch. While it waits, your code
            writes the selection from 0 to 5, we queue a paint on the UI thread, your code writes the
            selection from 6 to 11, and we queue a second paint. Then we open the latch, so the UI
            thread runs these tasks in the order in which they were queued:
        """
            var latch = new java.util.concurrent.CountDownLatch(1)
            try {
                SwingUtilities.invokeLater { latch.await() }
                selection.set(TextSelection.between(0, 5))
                SwingUtilities.invokeLater(paint)
                selection.set(TextSelection.between(6, 11))
                SwingUtilities.invokeLater(paint)
            } finally {
                latch.countDown()
            }
        and : 'We let both threads run their tasks until neither has a task left, paint once more, and do the same again:'
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()
            UI.runNow(paint)
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()
        and : 'We count the new selections so far, then paint once more and let both threads run their tasks again:'
            int changesBeforeTheLastPaint = changes.get()
            UI.runNow(paint)
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()

        then : 'The property holds the selection from 6 to 11 which your code wrote last, with the selected characters "world":'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
            selection.get().selectedText() == "world"
        and : 'The last paint wrote no new selection into the property:'
            changes.get() == changesBeforeTheLastPaint
    }

    def 'With `withTextSelection("body", selection)`, your users can select and copy the right-to-left Hebrew word "שלום", and the selection colour is painted behind it.'()
    {
        reportInfo """
            Hebrew is written from right to left. The first letter of the word "שלום" is painted
            at its right end, and the last letter at its left end. A selection holds two positions,
            and a position is a gap between two characters, counted in the order in which the
            letters are written: position 0 is the gap in front of the first letter, at the right
            end of the word, and position 4 is the gap behind the last letter, at its left end.

            Your user drags the mouse from the right end of the word to its left end, so the selection
            goes from position 0 to position 4, and copying it gives "שלום". SwingTree paints the
            selection colour from the left end to the right end of the word. If SwingTree computed the
            width of the selection colour as the place of the end minus the place of the start, the width
            of a right-to-left word would be negative, and no selection colour would be painted.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first letter:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "שלום" in blue on white, with an opaque yellow selection colour:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "שלום" ) )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangle around the blue pixels of the word.
            Its rightmost pixel column belongs to the first letter, and its leftmost pixel column
            belongs to the last letter:
        """
            var image = Utility.renderSingleComponent(box)
            var word = boundsOfPixelsPaintedIn(image, BLUE)
            int rightEnd   = word.x + word.width - 1
            int leftEnd    = word.x
            int lineMiddle = word.centerY

        when : 'We press the mouse button on the right end of the word, drag to its left end, and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, rightEnd, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, leftEnd, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, leftEnd, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }
        and : 'We render the component again and find the rectangle around the yellow pixels:'
            var highlight = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), YELLOW)

        then : 'The selection goes from position 0 to position 4:'
            selection.get().anchor() == 0
            selection.get().caret()  == 4
        and : 'The copied text is the Hebrew word:'
            clipboard.getData(DataFlavor.stringFlavor) == "שלום"
        and : 'The yellow pixels enclose the word:'
            highlight.x <= word.x
            highlight.x + highlight.width >= word.x + word.width
    }

    def 'At a UI scale of #uiScale and with `placement(#placement)`, the rectangle which `caretBounds()` returns is the rectangle where SwingTree paints the caret.'(
        float uiScale, UI.Placement placement
    ) {
        reportInfo """
            After SwingTree has painted a text made selectable with `withTextSelection`, the property
            holds a selection whose `caretBounds()` is the pixel rectangle of the caret on the component.
            Your code can use this rectangle to paint something of its own next to the caret, for example
            a round handle which your users drag on a touch screen. Such a handle only lines up with the
            text if the rectangle is exactly where SwingTree paints the caret.

            This scenario paints the caret with `showCaret(true)` in green and compares the rectangle
            around the green pixels with `caretBounds()`. The rectangle uses the same coordinates as the
            mouse events of the component: pixels, counted from the top left corner of the component, with
            the UI scale already applied. So a component which is 400 pixels wide in its style is
            400 × ${uiScale} pixels wide on the screen, and every coordinate of the caret rectangle is
            multiplied by ${uiScale} in the same way.
            The component has a margin, a border and a padding, which all move the text, and the placement
            moves the line to a side or to the middle of the component. The caret is the gap behind the
            "o" of "Hello", which is position 5.

            The edges of a painted rectangle which do not fall on a pixel boundary cover their pixel
            only partly, and a partly covered pixel is a mixture of green and white, which the method
            `boundsOfPixelsPaintedIn` of this specification does not count as green, because it only
            counts pixels close to pure green. So the green rectangle may differ from `caretBounds()` by one pixel at each
            edge. If SwingTree worked out the caret rectangle with a different font size, a different
            placement or without the margin, the border or the padding, the rectangles would lie many
            pixels apart.

            In this scenario the UI scale is ${uiScale} and the placement is `${placement}`.
        """
        given : """
            We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds an empty selection with the caret at position 5:'
            var selection = Var.of(TextSelection.caretAt(5))
        and : 'A component with a margin, a border and a padding, which paints "Hello world" in red on white, with a green caret:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .margin(5)
                        .border(3, Color.GRAY)
                        .padding(7)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(placement)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                            .selection( s -> s.showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangle around the green pixels. When SwingTree paints
            the component, it queues a task on the UI thread which writes the caret rectangle into the
            property. `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            var caret = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), GREEN)
            UI.sync()
        and : 'We read the caret rectangle from the property:'
            var bounds = selection.get().caretBounds().get()

        then : 'The left edge, the top and the bottom of the green pixels are at most one pixel away from the rectangle of the property:'
            Math.abs(caret.x - bounds.location().x()) <= 1
            Math.abs(caret.y - bounds.location().y()) <= 1
            Math.abs((caret.y + caret.height) - (bounds.location().y() + bounds.size().height().get())) <= 1
        and : 'The green pixels are at most one pixel wider than the rectangle of the property:'
            caret.width <= bounds.size().width().get() + 1

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            [uiScale, placement] << [
                [1f, 1.5f, 2f],
                [UI.Placement.TOP_LEFT, UI.Placement.CENTER, UI.Placement.BOTTOM_RIGHT]
            ].combinations()
    }

    def 'At a UI scale of #uiScale, the rectangle which `caretBounds()` returns is the rectangle where SwingTree paints the caret, also #where.'(
        float uiScale, String where, int caretPosition, boolean narrow, int obstacleLeft
    ) {
        reportInfo """
            After SwingTree has painted a text made selectable with `withTextSelection`, the property
            holds a selection whose `caretBounds()` is the pixel rectangle of the caret on the component.
            Your code can use this rectangle to paint something of its own next to the caret, for example
            a round handle which your users drag on a touch screen.

            This scenario paints the caret with `showCaret(true)` in green and compares the rectangle
            around the green pixels with `caretBounds()`. The text is "HHHHH HHHHHHH HHHHHHH". At a font
            size of 20, an "H" is about 14 pixels wide, so a word of 7 letters is about 98 pixels wide.

            The text is declared with `wrapLines(true)`. When the content area is 160 pixels wide, each
            word goes onto a line of its own, because two words need at least 70 + 5 + 98 = 173 pixels. The caret is at
            position 9, which lies in the second word, on the second line.

            With an obstacle from x = 150 to x = 250 across the first line of a content area which reaches
            from x = 10 to x = 390, the first word fits left of the obstacle and the second word goes
            right of it. The caret is at position 9, which lies in the second word, right of the obstacle.
            The obstacle is given in pixels on the component, so its numbers are multiplied by the UI scale
            in this scenario.

            The edges of a painted rectangle which do not fall on a pixel boundary cover their pixel only
            partly, and such a pixel is a mixture of green and white, which the method
            `boundsOfPixelsPaintedIn` of this specification does not count as green, because it only
            counts pixels close to pure green. So the green rectangle may differ from
            `caretBounds()` by one pixel at each edge. If SwingTree worked out the caret rectangle without
            the line breaks or without the obstacle, the rectangles would lie a whole line or many letters apart.

            In this scenario the UI scale is ${uiScale}, and the text is laid out ${where}.
        """
        given : """
            We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds an empty selection with the caret at position 9:'
            var selection = Var.of(TextSelection.caretAt(caretPosition))
        and : 'The obstacle, if there is one, in pixels on the component:'
            var obstacles = obstacleLeft < 0 ? new Shape[0] : [ new Rectangle((int)(obstacleLeft * uiScale), 0, (int)(100 * uiScale), (int)(40 * uiScale)) ] as Shape[]
        and : 'A component which paints the three words in red on white, with a green caret:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(narrow ? 180 : 400, 200)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacles)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHHH HHHHHHH HHHHHHH" ) )
                            .selection( s -> s.showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangle around the green pixels. When SwingTree paints
            the component, it queues a task on the UI thread which writes the caret rectangle into the
            property. `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            var caret = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), GREEN)
            UI.sync()
        and : 'We read the caret rectangle from the property:'
            var bounds = selection.get().caretBounds().get()

        then : 'The left edge, the top and the bottom of the green pixels are at most one pixel away from the rectangle of the property:'
            Math.abs(caret.x - bounds.location().x()) <= 1
            Math.abs(caret.y - bounds.location().y()) <= 1
            Math.abs((caret.y + caret.height) - (bounds.location().y() + bounds.size().height().get())) <= 1
        and : 'With line breaks, the caret lies below the first line, whose top is at the padding of 10 pixels and which is at least 20 pixels tall; with the obstacle, it lies right of the obstacle:'
            narrow ? bounds.location().y() >= (10 + 20) * uiScale : bounds.location().x() >= (obstacleLeft + 100) * uiScale

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            uiScale | where                        | caretPosition | narrow    | obstacleLeft
            1f      | "on three lines"             | 9             | true      | -1
            1.5f    | "on three lines"             | 9             | true      | -1
            2f      | "on three lines"             | 9             | true      | -1
            1f      | "around an obstacle"         | 9             | false     | 150
            1.5f    | "around an obstacle"         | 9             | false     | 150
            2f      | "around an obstacle"         | 9             | false     | 150
    }

    def 'At a UI scale of #uiScale, the selection colour of a selection from #anchor to #caret within one line reaches from `anchorBounds()` to `caretBounds()`.'(
        float uiScale, int anchor, int caret
    ) {
        reportInfo """
            After SwingTree has painted a text made selectable with `withTextSelection`, the property
            holds a selection whose `anchorBounds()` and `caretBounds()` are the pixel rectangles of the
            anchor and the caret on the component. The anchor is the position where the selection began,
            and the caret is the position which moves. On one line, the selection colour fills the space
            between these two positions, from the top to the bottom of the line.

            The text is "Hello world". A position is a gap between two characters: position 6 is the gap
            in front of the "w", and position 11 is the gap behind the "d". The selection selects "world",
            forwards from 6 to 11 or backwards from 11 to 6. Either way, the left edge of the opaque yellow
            selection colour lies at the left edge of the rectangle of position 6, and its right edge at the
            left edge of the rectangle of position 11. Its top and its bottom are the top and the bottom of
            both rectangles.

            The edges of a painted rectangle which do not fall on a pixel boundary cover their pixel only
            partly, and such a pixel is a mixture of yellow and white, which the method
            `boundsOfPixelsPaintedIn` of this specification does not count as yellow, because it only
            counts pixels close to pure yellow. So the yellow rectangle may differ by one
            pixel at each edge. If SwingTree worked out the rectangles of the anchor and the caret in another
            way than the selection colour, your code could not paint anything which lines up with the selection.

            In this scenario the UI scale is ${uiScale}, the anchor is ${anchor} and the caret is ${caret}.
        """
        given : """
            We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds the selection:'
            var selection = Var.of(TextSelection.between(anchor, caret))
        and : 'A component which paints "Hello world" in blue on white, with an opaque yellow selection colour:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.CENTER)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangle around the yellow pixels. When SwingTree paints
            the component, it queues a task on the UI thread which writes the rectangles of the anchor and
            the caret into the property. `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            var highlight = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), YELLOW)
            UI.sync()
        and : 'We read the rectangles of position 6 and of position 11 from the property:'
            var start = (anchor < caret ? selection.get().anchorBounds() : selection.get().caretBounds()).get()
            var end   = (anchor < caret ? selection.get().caretBounds()  : selection.get().anchorBounds()).get()

        then : 'The yellow pixels begin at most one pixel away from the left edge of the rectangle of position 6:'
            Math.abs(highlight.x - start.location().x()) <= 1
        and : 'The yellow pixels end at most one pixel away from the left edge of the rectangle of position 11:'
            Math.abs((highlight.x + highlight.width) - end.location().x()) <= 1
        and : 'The yellow pixels reach from the top to the bottom of both rectangles, give or take one pixel:'
            Math.abs(highlight.y - start.location().y()) <= 1
            Math.abs(highlight.y - end.location().y())   <= 1
            Math.abs((highlight.y + highlight.height) - (start.location().y() + start.size().height().get())) <= 1

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            uiScale | anchor | caret
            1f      | 6      | 11
            1.5f    | 6      | 11
            2f      | 6      | 11
            1f      | 11     | 6
            1.5f    | 11     | 6
            2f      | 11     | 6
    }

    def 'At a UI scale of #uiScale and with the text laid out #where, `positionAt(..)` turns the middle of the caret rectangle of every position back into that position.'(
        float uiScale, String where, boolean narrow, int obstacleLeft
    ) {
        reportInfo """
            After SwingTree has painted a text made selectable with `withTextSelection`, the property holds
            a selection which converts between positions and pixel coordinates in both directions.
            `withCaret(position).caretBounds()` returns the pixel rectangle of a position, and
            `positionAt(point)` returns the position nearest to a pixel coordinate. A position is a gap
            between two characters.

            Both directions must agree: the middle of the rectangle of a position lies in the gap of that
            position, so `positionAt(..)` must return the same position for it. This scenario checks it for
            every position of the text "HHHHH HHHHHHH HHHHHHH", from 0 to 21, including the positions where
            a line ends and the next one begins. The gap at the end of one line and the gap at the start of
            the next line are the same position. The selections in this scenario reach from position 0 to
            the position, so the position is their end, and `caretBounds()` returns the rectangle at the end
            of the upper line for it, next to the last selected character. It also checks that `withCaretAt(..)`, given the middle of the caret
            rectangle, returns a selection which equals the selection it was called on, because the caret
            does not move, and that `withAnchorAt(..)`, given the same coordinate, puts the anchor at the
            same position.

            If the conversion from a coordinate to a position used another rule than the conversion from a
            position to a coordinate, a round handle which your code paints at the caret, and which your
            users drag, would jump to a neighbouring gap as soon as they touch it.

            In this scenario the UI scale is ${uiScale}, and the text is laid out ${where}.
        """
        given : """
            We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds an empty selection with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'The obstacle, if there is one, in pixels on the component, which is why its numbers are multiplied by the UI scale:'
            var obstacles = obstacleLeft < 0 ? new Shape[0] : [ new Rectangle((int)(obstacleLeft * uiScale), 0, (int)(100 * uiScale), (int)(40 * uiScale)) ] as Shape[]
        and : 'A component which paints "HHHHH HHHHHHH HHHHHHH":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(narrow ? 180 : 400, 200)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacles)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("HHHHH HHHHHHH HHHHHHH")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component. When SwingTree paints the component, it queues a task on the UI
            thread which writes the selection with its pixel rectangles into the property. `UI.sync()`
            waits until the UI thread has run every task queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()
            var painted = selection.get()

        when : 'For every position from 0 to 21, we take the middle of its caret rectangle and ask for the position there:'
            var middles = (0..21).collect { position ->
                var bounds = painted.withCaret(position).caretBounds().get()
                Position.of(bounds.location().x() + bounds.size().width().get() / 2, bounds.location().y() + bounds.size().height().get() / 2)
            }
            var positionsFound = middles.collect { middle -> painted.positionAt(middle).get() }

        then : 'Every middle of a caret rectangle gives back its own position:'
            positionsFound == (0..21).toList()
        and : 'Moving the caret of each of these selections to the middle of its own caret rectangle changes nothing:'
            (0..21).every { position ->
                var atPosition = painted.withCaret(position)
                atPosition.withCaretAt(middles[position]) == atPosition
            }
        and : 'Moving the anchor to the middle of the caret rectangle of a position puts the anchor at that position:'
            (0..21).every { position -> painted.withAnchorAt(middles[position]).anchor() == position }

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            uiScale | where                  | narrow    | obstacleLeft
            1f      | "on one line"          | false     | -1
            1.5f    | "on one line"          | false     | -1
            1f      | "on three lines"       | true      | -1
            1.5f    | "on three lines"       | true      | -1
            2f      | "on three lines"       | true      | -1
            1f      | "around an obstacle"   | false     | 150
            1.5f    | "around an obstacle"   | false     | 150
    }

    def 'Moving the caret with `withCaretAt(..)` to a coordinate gives the same position as dragging the mouse to that coordinate, also left of, right of, above and below the text.'()
    {
        reportInfo """
            `withCaretAt(point)` of a selection which SwingTree has written into the property of
            `withTextSelection` moves the caret to the position nearest to a pixel coordinate. It uses the
            rule which SwingTree uses when your users drag the mouse: first the line nearest to the
            coordinate, then the gap nearest to the coordinate in that line. So your code can move the
            caret with its own gestures, for example with a handle which your users drag on a touch screen,
            and the caret behaves exactly as if your users dragged the mouse.

            The component is 400 pixels wide and 100 pixels tall, has a padding of 10 pixels, and paints
            "Hello world" in its top left corner. A position is a gap between two characters: position 0
            is the gap in front of the "H", and position 11 is the gap behind the "d", which is the end of
            the text. This scenario drags the mouse to four coordinates and moves the caret with
            `withCaretAt(..)` to the same four coordinates:
            x = 3, in the padding left of the text, which gives position 0;
            x = 395, far right of the text, which gives position 11;
            and the middle of "world" at y = 2 above the line and at y = 95 below it, which both give a
            position inside "world", because the only line is the nearest line.

            If `withCaretAt(..)` used another rule than the mouse, a handle dragged by your users and the
            mouse would put the caret into different gaps for the same coordinate.
        """
        given : 'A property which holds the selection, starting with the caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We render the component and find the rectangles around the red pixels of "Hello" and the blue
            pixels of "world". When SwingTree paints the component, it queues a task on the UI thread which
            writes the selection with its pixel rectangles into the property. `UI.sync()` waits until the UI
            thread has run every task queued so far:
        """
            var image = Utility.renderSingleComponent(box)
            UI.sync()
            var hello = boundsOfPixelsPaintedIn(image, RED)
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            var painted = selection.get()
        and : 'The four coordinates, left of, right of, above and below the text:'
            var points = [
                Position.of(3,   hello.centerY),
                Position.of(395, hello.centerY),
                Position.of(world.centerX, 2),
                Position.of(world.centerX, 95)
            ]

        when : """
            For each coordinate, we press the mouse button on the left edge of the "H", which puts the caret
            at position 0, drag the mouse to the coordinate, release the button there, and read the caret
            from the property:
        """
            int leftEdgeOfH = hello.x
            int lineMiddle  = hello.centerY
            var caretsFromTheMouse = points.collect { point ->
                int x = point.x()
                int y = point.y()
                UI.runNow {
                    box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfH, lineMiddle, 1, false, MouseEvent.BUTTON1))
                    box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                    box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, x, y, 1, false, MouseEvent.BUTTON1))
                }
                selection.get().caret()
            }
        and : 'For each coordinate, we move the caret of the painted selection there with `withCaretAt(..)`:'
            var caretsFromWithCaretAt = points.collect { point -> painted.withCaretAt(point).caret() }

        then : 'Both ways give the same positions:'
            caretsFromTheMouse == caretsFromWithCaretAt
        and : 'Left of the text the caret is at position 0, and right of the text at position 11:'
            caretsFromWithCaretAt[0] == 0
            caretsFromWithCaretAt[1] == 11
        and : 'Above and below the line, the caret is inside "world", between position 6 and position 11:'
            caretsFromWithCaretAt[2] > 6 && caretsFromWithCaretAt[2] < 11
            caretsFromWithCaretAt[3] > 6 && caretsFromWithCaretAt[3] < 11
    }

    def 'When the component gets wider, the caret rectangle in the property of `withTextSelection` moves with the text, and SwingTree writes the new selection into the property without any input from your users.'()
    {
        reportInfo """
            The pixel rectangles in a selection which SwingTree writes into the property of
            `withTextSelection` depend on where the text is painted. With `placement(TOP_RIGHT)`,
            the text is painted at the right edge of the component, so when the component gets
            200 pixels wider, the text and its caret move 200 pixels to the right.

            Positions are the only part of a selection which your code and your users choose. The
            rectangles are worked out from the positions every time SwingTree paints the text. When the
            rectangles change, SwingTree writes a new selection with the same positions and the new
            rectangles into the property, without any input from your users, so that something which
            your code paints at the caret follows the text. If SwingTree kept the old rectangles, such a
            handle would stay 200 pixels left of the caret.
        """
        given : 'A property which holds an empty selection with the caret at position 5, and a listener which counts every new selection:'
            var selection = Var.of(TextSelection.caretAt(5))
            var changes = new java.util.concurrent.atomic.AtomicInteger(0)
            selection.onChange(From.ALL, change -> changes.incrementAndGet() )
        and : """
            A component which paints "Hello world" in the top right corner of its content area. Its style
            sets no size, so that `setSize(..)` decides how large it is:
        """
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_RIGHT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We make the component 400 pixels wide and paint it. When SwingTree paints the component, it
            queues a task on the UI thread which writes the selection with its rectangles into the property.
            `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            UI.runNow { box.setSize(400, 100) }
            Utility.renderSingleComponent(box)
            UI.sync()
            var narrowCaret = selection.get().caretBounds().get()
            int changesBeforeTheResize = changes.get()

        when : 'We make the component 600 pixels wide, which is 200 pixels wider, and paint it again:'
            UI.runNow { box.setSize(600, 100) }
            Utility.renderSingleComponent(box)
            UI.sync()
            var wideCaret = selection.get().caretBounds().get()

        then : 'The component really is 600 pixels wide:'
            box.width == 600
        and : 'The caret rectangle moved 200 pixels to the right, give or take one pixel for rounding:'
            Math.abs(wideCaret.location().x() - narrowCaret.location().x() - 200) <= 1
        and : 'SwingTree wrote a new selection into the property, and its caret is still at position 5:'
            changes.get() > changesBeforeTheResize
            selection.get().caret() == 5
    }

    def 'When the UI scale changes from 1 to 2, the caret rectangle in the property of `withTextSelection` moves to where SwingTree paints the caret at the new scale.'()
    {
        reportInfo """
            The pixel rectangles in a selection which SwingTree writes into the property of
            `withTextSelection` use the same coordinates as the mouse events of the component: pixels,
            counted from the top left corner of the component, with the UI scale already applied. When your users move a window to a screen with twice the pixel
            density, the UI scale changes from 1 to 2, and every size in the style doubles: the padding
            of 10 pixels becomes 20 pixels, and the font size of 20 becomes 40.

            The caret is at position 5, behind "Hello". At a UI scale of 1 its left edge is the padding of
            10 pixels plus the width of "Hello". At a UI scale of 2 both double, so the left edge moves to
            about twice its former x coordinate. A glyph of a font at size 40 is not always exactly twice
            as wide as at size 20, because fonts round their widths to pixels at each size, so this scenario
            allows 4 pixels of difference. If SwingTree kept the rectangle of the old scale, something which
            your code paints at the caret would stay at half the distance from the left edge.
        """
        given : 'A property which holds an empty selection with the caret at position 5:'
            var selection = Var.of(TextSelection.caretAt(5))
        and : 'A component which paints "Hello world" in its top left corner:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(600, 150)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We paint the component at a UI scale of 1. When SwingTree paints the component, it queues a task
            on the UI thread which writes the selection with its rectangles into the property. `UI.sync()`
            waits until the UI thread has run every task queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()
            var caretAtScale1 = selection.get().caretBounds().get()

        when : 'We set the UI scale factor to 2 and paint the component again:'
            SwingTree.get().setUiScaleFactor(2f)
            Utility.renderSingleComponent(box)
            UI.sync()
            var caretAtScale2 = selection.get().caretBounds().get()

        then : 'The left edge of the caret is about twice as far from the left edge of the component as before:'
            Math.abs(caretAtScale2.location().x() - 2 * caretAtScale1.location().x()) <= 4
        and : 'The caret is about twice as tall as before:'
            Math.abs(caretAtScale2.size().height().get() - 2 * caretAtScale1.size().height().get()) <= 4

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)
    }

    def 'For a position on a line which SwingTree does not paint, because the component is too short for it, `caretBounds()` is empty.'()
    {
        reportInfo """
            The component is 50 pixels tall and has a padding of 10 pixels, so its content area is
            30 pixels tall. The text has two paragraphs, "Hello" and "world". A line of a font of size 20
            is about 22.4 pixels tall, so the first line fits into the content area and the second
            line does not. SwingTree paints only the lines which fit.

            A position is a gap between two characters: position 2 is the gap behind "He" on the first
            line, and position 8 is the gap behind "wo" on the second line. A position on a line which is
            not painted has no place on the component, so `caretBounds()` and `anchorBounds()` are empty for
            it, while a position on the painted line has a rectangle. If SwingTree returned a rectangle for
            a line which it does not paint, your code would paint a handle below the bottom edge of the
            content area, where your users see no text.
        """
        given : 'A property which holds an empty selection with the caret at position 8, on the second line:'
            var selection = Var.of(TextSelection.caretAt(8))
        and : 'A component whose content area is 30 pixels tall, with the text "Hello", a line break, and "world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 50)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello\nworld")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We paint the component. When SwingTree paints the component, it queues a task on the UI thread
            which writes the selection with its rectangles into the property. `UI.sync()` waits until the
            UI thread has run every task queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()

        then : 'Position 8, on the second line, has no rectangle:'
            !selection.get().caretBounds().isPresent()
            !selection.get().anchorBounds().isPresent()
        and : 'Position 2, on the first line, has a rectangle inside the content area:'
            selection.get().withCaret(2).caretBounds().isPresent()
            selection.get().withCaret(2).caretBounds().get().location().y() >= 10
    }

    def 'A `TextSelection` which your code creates has no caret rectangle and converts no coordinate into a position, until SwingTree has written it into the property of `withTextSelection`.'()
    {
        reportInfo """
            A `TextSelection` made with `TextSelection.between(..)` or `TextSelection.caretAt(..)` holds
            only its two positions. It does not belong to any text yet, so it does not know where any
            character is painted. Only the selections which SwingTree writes into the property after it has
            painted the text carry the pixel rectangles and the selected characters.

            So for a selection which your code creates, `caretBounds()` and `anchorBounds()` are empty,
            `positionAt(..)` returns an empty `Optional`, `withCaretAt(..)` and `withAnchorAt(..)` return
            the very same selection, and `selectedText()` is an empty string. If such a selection guessed
            positions or rectangles, your code would act on a place where no text is painted.
        """
        given : 'A selection from position 0 to position 5, which no component has painted:'
            var created = TextSelection.between(0, 5)

        expect : 'It has neither a caret rectangle nor an anchor rectangle:'
            !created.caretBounds().isPresent()
            !created.anchorBounds().isPresent()
        and : 'It converts no coordinate into a position, and moving its caret or its anchor to a coordinate changes nothing:'
            !created.positionAt(Position.of(20, 20)).isPresent()
            created.withCaretAt(Position.of(20, 20)).is(created)
            created.withAnchorAt(Position.of(20, 20)).is(created)
        and : 'It holds no selected characters:'
            created.selectedText() == ""
    }

    def 'At a UI scale of #uiScale, the border from `selection(s -> s.border(3, ..))` is painted inside the area which the selection colour covers without a border, so the selection does not get larger.'(
        float uiScale
    ) {
        reportInfo """
            `border(width, colour)` in the style of a selection paints a border along the edge of the area
            which SwingTree paints behind the selected characters of a line. The border is painted on the
            inside of that area: its outer edge is where the edge of the selection colour is without a border,
            and the selection colour fills only what lies inside the border. So a selection with a border is
            exactly as large as a selection without one, and the selected letters do not move. If the border
            were painted around the outside of the area, it would cover parts of the neighbouring letters and
            of the lines above and below.

            The width of the border is given in pixels before the UI scale is applied, like every size in a
            style. This scenario paints two components which differ only in the border: the first has an
            opaque yellow selection colour, and the second has the same colour and a green border of 3 pixels,
            which is 3 × ${uiScale} pixels at a UI scale of ${uiScale}. The text is "Hello world", and the
            selection from position 6 to position 11 selects "world".

            The edges of a painted area which do not fall on a pixel boundary cover their pixel only partly,
            and such a pixel is a mixture of two colours, which the method `boundsOfPixelsPaintedIn` of this
            specification does not count as either colour, because it only counts pixels close to a pure colour.
            So this scenario allows one pixel of difference at each edge.

            In this scenario the UI scale is ${uiScale}.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A component which paints "Hello world" in blue on white, with an opaque yellow selection colour and "world" selected, but without a border:'
            var withoutBorder =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)
        and : 'The same component with a green border of 3 pixels around the selection:'
            var withBorder =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> s.color(YELLOW).border(3, GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)

        when : 'We render both components and find the rectangles around their yellow and their green pixels:'
            var plainImage    = Utility.renderSingleComponent(withoutBorder)
            var borderedImage = Utility.renderSingleComponent(withBorder)
            var selectionWithoutBorder = boundsOfPixelsPaintedIn(plainImage, YELLOW)
            var border                 = boundsOfPixelsPaintedIn(borderedImage, GREEN)
            var selectionInsideBorder  = boundsOfPixelsPaintedIn(borderedImage, YELLOW)
        and : 'The width of the border on the screen:'
            float borderWidth = 3 * uiScale

        then : 'The green pixels lie where the yellow pixels lie without a border, give or take one pixel at each edge:'
            Math.abs(border.x - selectionWithoutBorder.x) <= 1
            Math.abs(border.y - selectionWithoutBorder.y) <= 1
            Math.abs((border.x + border.width)  - (selectionWithoutBorder.x + selectionWithoutBorder.width))  <= 1
            Math.abs((border.y + border.height) - (selectionWithoutBorder.y + selectionWithoutBorder.height)) <= 1
        and : 'The yellow pixels lie inside the border, at least the width of the border minus one pixel away from each of its edges:'
            selectionInsideBorder.x >= border.x + borderWidth - 1
            selectionInsideBorder.y >= border.y + borderWidth - 1
            selectionInsideBorder.x + selectionInsideBorder.width  <= border.x + border.width  - borderWidth + 1
            selectionInsideBorder.y + selectionInsideBorder.height <= border.y + border.height - borderWidth + 1

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            uiScale << [1f, 1.5f, 2f]
    }

    def 'SwingTree paints no selection colour under the border from `selection(s -> s.border(..))`, so a partly transparent border shows the background behind it.'()
    {
        reportInfo """
            `border(width, colour)` in the style of a selection paints a border along the inner edge of the
            area behind the selected characters, and the selection colour fills only what lies inside the
            border. The two colours never lie on top of each other. This matters for partly transparent
            colours: a pixel of the border shows the border colour mixed with the background of the component,
            and a pixel inside the border shows the selection colour mixed with the background.

            This scenario uses a half transparent blue border and a half transparent green selection colour
            on a white background, so four mixtures are possible:
            the border on white is (127, 127, 237),
            the selection colour on white is (127, 202, 127),
            the border on the selection colour on white is (63, 101, 174),
            and the selection colour on the border on white is (63, 139, 118).
            Only the first two may appear. If SwingTree first filled the whole area with the selection colour
            and then painted the border over it, the third mixture would appear on the border. If it first
            filled the whole area with the border colour and then painted the selection colour over the
            inside, the fourth mixture would appear inside the border.

            The top and the bottom of a line do not always fall on a pixel boundary. There, the inner edge of the
            border runs through a row of pixels, and each pixel of that row is covered partly by the border and
            partly by the selection colour. Such a pixel mixes both colours with the background, each of them only
            partly, so it is lighter than the two mixtures which must not appear, and differs from each of them by
            more than 60 in at least one of its red, green and blue values.

            The text is "Hello world", and the selection from position 5 to position 11 selects " world". The
            letters are white, so that their smoothed edges, which mix white into the colours below them, never
            come close to one of the two mixtures which must not appear.
        """
        given : 'A half transparent blue for the border and a half transparent green for the selection colour:'
            var borderColour    = new Color(0, 0, 220, 128)
            var selectionColour = new Color(0, 150, 0, 128)
        and : 'A component which paints "Hello world" in white on white, with " world" selected and a border of 3 pixels:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(WHITE), "Hello world" ) )
                            .selection( s -> s.color(selectionColour).border(3, borderColour) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(5, 11)))
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)

        then : 'The border on white and the selection colour on white are both painted:'
            boundsOfPixelsPaintedIn(image, new Color(127, 127, 237)) != null
            boundsOfPixelsPaintedIn(image, new Color(127, 202, 127)) != null
        and : 'Neither colour is painted on top of the other:'
            boundsOfPixelsPaintedIn(image, new Color(63, 101, 174)) == null
            boundsOfPixelsPaintedIn(image, new Color(63, 139, 118)) == null
    }

    def 'With `selection(s -> s.mergeLines(#merge).borderRadius(8).border(6, ..))`, the outer edge of the border is rounded with a radius of 8 pixels at a convex corner, and its inner edge with a radius of 8 - 6 = 2 pixels.'(
        boolean merge
    ) {
        reportInfo """
            `borderRadius(..)` rounds the corners of the area behind the selected characters. A selection on one
            line is a rectangle, and every corner of a rectangle is a convex corner, which points out of the area.
            With a border, the outer edge of the border has this radius at a convex corner. The border is 6 pixels
            wide, so its inner edge lies 6 pixels further inside, and at a convex corner the inner edge has a radius
            of 8 - 6 = 2 pixels. Then the border is 6 pixels wide along the curve of each corner too. If the inner
            edge kept the radius of 8 pixels, the border would be thicker in the corners than along its sides.
            With `mergeLines(true)`, SwingTree joins the selected parts of neighbouring lines into one shape. On one
            line there is nothing to join, so the shape is the same rectangle and its corners are rounded the same way.

            The text is "_____", and the selection from position 0 to position 5 selects all of it. The
            underscores are painted at the bottom of the line, so the top left corner of the selection shows no
            letter. The component has a padding of 10 pixels and the text starts in its top left corner, so the
            selection starts at x = 10 and y = 10, and the inner edge of the border at x = 16 and y = 16.

            The quarter circle of the outer edge at the top left corner has its centre at x = 18 and y = 18. The pixel from x = 11 to
            x = 12 and from y = 11 to y = 12 lies completely outside it, because even its corner nearest to the
            centre is √(6² + 6²) ≈ 8.5 pixels away from the centre. So that pixel is neither green nor yellow.
            The quarter circle of the inner edge, with a radius of 2 pixels, has its centre at x = 18 and y = 18
            as well. The pixel from x = 17 to x = 18 and from y = 17 to y = 18 lies completely inside it, because
            its corner farthest from the centre is √(1² + 1²) ≈ 1.4 pixels away. So that pixel is yellow. If the
            inner edge had a radius of 8 pixels at that corner, its centre would be at x = 24 and y = 24, the pixel
            would lie about 9 pixels away from it, outside the curve of the inner edge, and it would be green.

            In this scenario `mergeLines` is ${merge}.
        """
        given : 'A component which paints "_____" in blue on white, with an opaque yellow selection colour, a radius of 8 pixels and a green border of 6 pixels:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "_____" ) )
                            .selection( s -> s.color(YELLOW).mergeLines(merge).borderRadius(8).border(6, GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 5)))
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)
            var border = boundsOfPixelsPaintedIn(image, GREEN)

        then : 'The border starts at the top left corner of the content area, at x = 10 and y = 10, give or take one pixel:'
            Math.abs(border.x - 10) <= 1
            Math.abs(border.y - 10) <= 1
        and : 'The pixel at x = 11 and y = 11 lies outside the curve of the outer edge, so it is neither green nor yellow:'
            boundsOfPixelsPaintedIn(image, GREEN,  new Rectangle(11, 11, 1, 1)) == null
            boundsOfPixelsPaintedIn(image, YELLOW, new Rectangle(11, 11, 1, 1)) == null
        and : 'The pixel at x = 17 and y = 17 lies inside the curve of the inner edge with a radius of 2 pixels, so it is yellow:'
            boundsOfPixelsPaintedIn(image, YELLOW, new Rectangle(17, 17, 1, 1)) != null

        where :
            merge << [false, true]
    }

    def 'Without a border width or without a border colour, `selection(s -> s.#look)` paints the selection exactly as large as without a border, and paints no border.'(
        String look, Configurator<TextSelectionConf> styler
    ) {
        reportInfo """
            A border in the style of a selection needs a width larger than 0 and a colour. `borderWidth(..)`
            sets only the width and `borderColor(..)` sets only the colour, so that a style sheet can set one
            and a more specific style the other. As long as one of the two is missing, SwingTree paints no
            border, and the selection colour covers the same area as without any border setting.

            This scenario compares a component whose selection style is `${look}` with a component without
            any border setting. The text is "Hello world", and the selection from position 6 to position 11
            selects "world".
        """
        given : 'A component which paints "Hello world" in blue on white, with an opaque yellow selection colour and "world" selected:'
            var plain =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)
        and : 'The same component with the incomplete border setting of this scenario:'
            var incomplete =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> styler.configure(s.color(YELLOW)) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)

        when : 'We render both components and find the rectangles around their yellow pixels:'
            var plainSelection      = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(plain), YELLOW)
            var incompleteImage     = Utility.renderSingleComponent(incomplete)
            var incompleteSelection = boundsOfPixelsPaintedIn(incompleteImage, YELLOW)

        then : 'The yellow pixels cover the same rectangle in both components, give or take one pixel at each edge:'
            Math.abs(incompleteSelection.x - plainSelection.x) <= 1
            Math.abs(incompleteSelection.y - plainSelection.y) <= 1
            Math.abs(incompleteSelection.width  - plainSelection.width)  <= 1
            Math.abs(incompleteSelection.height - plainSelection.height) <= 1
        and : 'No pixel is green:'
            boundsOfPixelsPaintedIn(incompleteImage, GREEN) == null

        where :
            look                    | styler
            "borderWidth(3)"        | { TextSelectionConf s -> s.borderWidth(3) } as Configurator<TextSelectionConf>
            "borderColor(GREEN)"    | { TextSelectionConf s -> s.borderColor(GREEN) } as Configurator<TextSelectionConf>
            "border(0, GREEN)"      | { TextSelectionConf s -> s.border(0, GREEN) } as Configurator<TextSelectionConf>
    }

    def 'When the same property is passed to `withTextSelection` of two components, SwingTree logs a warning which names both components, says what goes wrong and what to do instead.'()
    {
        reportInfo """
            After every paint, a component made selectable with `withTextSelection` writes a selection into
            its property which holds the pixel rectangles and the selected characters of its own text. When
            two components share one property, each of them writes the values of its own text into it, so the
            property switches back and forth between the values of the two components, and its listeners run
            every time. Nothing throws an exception in that case, so SwingTree logs a warning instead. The
            warning names both components with their class, their identity hash and their name, says that the
            property switches back and forth, and says that every component needs its own `Var<TextSelection>`.

            Each of the two components logs the warning at most once, so a log file does not fill up with it.
            A component with a property of its own logs no warning, however often SwingTree paints it.
        """
        given : 'A spy which records the messages that SwingTree logs:'
            var log = LogSpy.attach()
        and : 'One property, which we pass to two components:'
            var shared = Var.of(TextSelection.caretAt(0))
        and : 'Two components named "left" and "right", which paint different texts and both use the same property:'
            var left =
                    UI.box()
                    .id("left")
                    .withStyle( it -> it
                        .size(400, 100)
                        .text("body", t -> t.font( f -> f.family("Ubuntu").size(20) ).content("Hello world"))
                    )
                    .withTextSelection("body", shared)
                    .get(JBox)
            var right =
                    UI.box()
                    .id("right")
                    .withStyle( it -> it
                        .size(400, 100)
                        .text("body", t -> t.font( f -> f.family("Ubuntu").size(30) ).content("Goodbye"))
                    )
                    .withTextSelection("body", shared)
                    .get(JBox)
        and : 'A third component named "alone", with a property of its own:'
            var alone =
                    UI.box()
                    .id("alone")
                    .withStyle( it -> it
                        .size(400, 100)
                        .text("body", t -> t.font( f -> f.family("Ubuntu").size(20) ).content("Hello world"))
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(0)))
                    .get(JBox)

        when : """
            We paint the three components three times, one after the other. When SwingTree paints a component,
            it queues a task on the UI thread which writes the selection of that component into the property.
            `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            3.times {
                Utility.renderSingleComponent(left)
                UI.sync()
                Utility.renderSingleComponent(right)
                UI.sync()
                Utility.renderSingleComponent(alone)
                UI.sync()
            }
            var warnings = log.warnings().findAll { it.contains("Var<TextSelection>") }

        then : 'Each of the two components logged the warning once:'
            warnings.size() == 2
        and : 'Every warning names both components:'
            warnings.every { it.contains("named 'left'") && it.contains("named 'right'") }
        and : 'Every warning says what goes wrong and what to do instead:'
            warnings.every { it.contains("switches back and forth") && it.contains("Give every component its own 'Var<TextSelection>'") }
        and : 'No warning mentions the component with a property of its own:'
            !log.warnings().any { it.contains("named 'alone'") }

        cleanup : 'We detach the spy:'
            log?.detach()
    }

    def 'At a UI scale of #uiScale and with `selection(s -> s.mergeLines(#merge).border(2, ..))`, a selection over three lines paints #what between the first and the second line.'(
        float uiScale, boolean merge, String what, boolean borderBetweenLines
    ) {
        reportInfo """
            SwingTree paints the selection colour behind the selected characters of every line as a
            rectangle. With `border(width, colour)`, it also paints a border along the inner edge of
            that rectangle. Without `mergeLines(true)`, every line gets a rectangle and a border of its
            own. Between two selected lines, the bottom border of the upper line and the top border of the
            lower line lie directly below each other, so your users see a green bar of 2 + 2 = 4 pixels
            between the two lines, multiplied by the UI scale.

            With `mergeLines(true)`, SwingTree joins the rectangles of neighbouring lines into one shape
            and paints the border only along the outline of that shape. Between two selected lines lies
            the inside of the shape, so SwingTree paints the selection colour there and no border.

            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH HHHH". At a
            font size of 20, one word of four "H" letters is about 56 pixels wide and a space about 5 pixels,
            so two words need about 56 + 5 + 56 = 117 pixels, more than the 100 pixels of the content area.
            With `wrapLines(true)` every word therefore gets a line of its own. The selection from position 0
            to position 14 selects all three words. The border is 2 pixels wide, and green.

            The first word is red and the second word is blue. Between the bottom of the red letters and the
            top of the blue letters lie a few rows of pixels without any letter, and the line boundary lies
            in these rows. We look for border colour in these rows, but not in the 4 × ${uiScale} pixel
            columns at the left and at the right end of the rows, where the border along the left and the
            right side of the selection runs. Every size in a style, the 2 pixels of the border included, is
            multiplied by the UI scale.

            In this scenario the UI scale is ${uiScale} and `mergeLines` is ${merge}.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A component whose content area is 100 pixels wide, with the three words in red, blue and black, an opaque yellow selection colour and a green border of 2 pixels:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),         "HHHH " ),
                                StyledString.of( f -> f.color(BLUE),        "HHHH " ),
                                StyledString.of( f -> f.color(Color.BLACK), "HHHH"  )
                            )
                            .selection( s -> s.color(YELLOW).border(2, GREEN).mergeLines(merge) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 14)))
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red letters, the blue letters and the green pixels:'
            var image = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, BLUE)
            var border     = boundsOfPixelsPaintedIn(image, GREEN)
        and : """
            The rows between the bottom of the red letters and the top of the blue letters, without the
            columns at each end of the selection, 4 pixels multiplied by the UI scale:
        """
            int sideColumns = 4 * uiScale
            int rowsTop     = firstWord.y + firstWord.height
            int rowsBottom  = secondWord.y
            int borderLeft  = border.x
            int borderWidth = border.width
            var betweenTheLines = new Rectangle(borderLeft + sideColumns, rowsTop, borderWidth - 2 * sideColumns, rowsBottom - rowsTop)
        and : 'The same rows within the columns at the left end of the selection, 4 pixels multiplied by the UI scale:'
            var leftSide = new Rectangle(borderLeft, rowsTop, sideColumns, rowsBottom - rowsTop)

        then : 'The second word is painted below the first word, so the text was really wrapped:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The border runs along the left side of the selection in these rows:'
            boundsOfPixelsPaintedIn(image, GREEN, leftSide) != null
        and : 'Between the lines, the selection colour is painted, and the border colour is painted only without `mergeLines(true)`:'
            boundsOfPixelsPaintedIn(image, YELLOW, betweenTheLines) != null
            (boundsOfPixelsPaintedIn(image, GREEN, betweenTheLines) != null) == borderBetweenLines

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            uiScale | merge | what                                             || borderBetweenLines
            1f      | true  | "the selection colour and no border colour"      || false
            2f      | true  | "the selection colour and no border colour"      || false
            1f      | false | "the border colour"                              || true
            2f      | false | "the border colour"                              || true
    }

    def 'With `selection(s -> s.mergeLines(true).concaveRadius(12))`, the concave corner where a selection which starts in the middle of a line meets the next line is filled with a curve, which adds selection colour that a square concave corner does not have.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape. Such a shape has two kinds of corners. A convex corner points
            out of the shape, like each of the four corners of a rectangle. A concave corner points into the
            shape, like the corner where the end of a short line meets the longer line below it.
            `convexRadius(..)` rounds the convex corners, and `concaveRadius(..)` rounds the concave corners.

            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH HHHH". At a font
            size of 20, one word of four "H" letters is about 56 pixels wide and a space about 5 pixels, so two
            words need about 56 + 5 + 56 = 117 pixels, more than the 100 pixels of the content area, and with
            `wrapLines(true)` every word gets a line of its own. The selection from position 2 to position 14 starts in the middle of the
            first word, behind its second "H". So the selected part of the first line is shorter than the second
            line: it starts about 30 pixels further right. Where the left edge of the selected part of the first
            line meets the top edge of the second line, the shape has a concave corner.

            A square concave corner is just the corner of the two edges. A concave corner with a radius of 12
            pixels is a quarter circle which curves from the left edge of the first line down to the top edge of
            the second line. The quarter circle has its centre 12 pixels left of and 12 pixels above the corner,
            in the empty space next to the selection. Between the quarter circle and the corner lies an area of
            12 × 12 − π × 12² ÷ 4 ≈ 31 square pixels, which the rounded corner fills with the selection colour
            and the square corner leaves empty. SwingTree uses at most half of an edge for the curve of one corner.
            The left edge of the selected part of the first line is as tall as the line, which is about
            22.4 pixels for a font of size 20, so the radius is about 22.4 ÷ 2 = 11.2 pixels, and the area is
            about 11.2 × 11.2 − π × 11.2² ÷ 4 ≈ 27 square pixels.

            The shape has a second concave corner at its right side, where the end of the third line, which has
            no space behind it, meets the second line, which ends with a space. That corner's horizontal edge is
            only as wide as a space, about 5 pixels, so its curve is at most 2.5 pixels and adds at most one or two
            yellow pixels. This only makes the difference larger, so it cannot make the check pass by mistake: if
            SwingTree ignored `concaveRadius(..)`, both counts would still be equal.

            Pixels at the edge of the curve are only partly covered, and the method `boundsOfPixelsPaintedIn`
            of this specification does not count a partly covered pixel as yellow. We therefore count the yellow
            pixels of the whole component with both radii and expect at least 12 more yellow pixels with the
            rounded concave corner. The second "H" of the first line, which is not selected, stands right next to
            the concave corner, and its red pixels cover some of the added yellow pixels. With the Ubuntu font,
            the rounded concave corner adds 28 yellow pixels, which leaves room for that.
        """
        given : 'A function which builds a component whose content area is 100 pixels wide, with the three words in red, an opaque yellow selection colour, square convex corners and the given concave radius:'
            var componentWithConcaveRadius = { double concaveRadius ->
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH HHHH" ) )
                            .selection( s -> s.color(YELLOW).mergeLines(true).convexRadius(0).concaveRadius(concaveRadius) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(2, 14)))
                    .get(JBox)
            }

        when : 'We render one component with a square concave corner and one with a concave radius of 12 pixels, and count their yellow pixels:'
            int squareCorner  = numberOfPixelsPaintedIn(Utility.renderSingleComponent(componentWithConcaveRadius(0)),  YELLOW)
            int roundedCorner = numberOfPixelsPaintedIn(Utility.renderSingleComponent(componentWithConcaveRadius(12)), YELLOW)

        then : 'The rounded concave corner adds at least 12 yellow pixels:'
            roundedCorner >= squareCorner + 12
    }

    def 'With `mergeLines(true)`, `convexRadius(..)` rounds only the convex corners and `concaveRadius(..)` only the concave corners of a selection.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape. Such a shape has two kinds of corners. A convex corner points
            out of the shape, like each of the four corners of a rectangle. A concave corner points into the
            shape, like the corner where the end of a short line meets the longer line below it.
            `convexRadius(..)` rounds the convex corners, and `concaveRadius(..)` rounds the concave corners,
            each without changing the other kind.

            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH HHHH". At a font
            size of 20, one word of four "H" letters is about 56 pixels wide and a space about 5 pixels, so two
            words need about 56 + 5 + 56 = 117 pixels, more than the 100 pixels of the content area, and with
            `wrapLines(true)` every word gets a line of its own. The selection from position 2 to position 14
            starts behind the second "H" of the first line. The selected part of the first line therefore starts
            about 30 pixels right of the start of the second line. Its top left corner is a convex corner. Where
            its left edge meets the top edge of the second line, the shape has a concave corner.

            We paint the selection twice: once with `convexRadius(12).concaveRadius(0)`, and once with
            `convexRadius(0).concaveRadius(12)`. SwingTree uses at most half of an edge for the curve of one corner.
            The left edge of the selected part of the first line is as tall as the line, which is about 22.4
            pixels for a font of size 20, so a radius of 12 becomes about 22.4 ÷ 2 = 11.2 pixels.

            At the convex corner we look at the pixel which covers the square from 1 to 2 pixels right of and below
            the corner. The quarter circle of a convex radius of 11.2 pixels has its centre 11.2 pixels right
            of and below the corner, and the corner of that pixel nearest to the centre is
            √(9.2² + 9.2²) ≈ 13.0 pixels away from it, which is more than the radius. So with the rounded convex corner
            the pixel is not yellow, and with the square convex corner it is.

            At the concave corner we look at the square of 7 × 7 pixels left of and above the corner, which lies in the
            empty space next to the selection. The square concave corner leaves it empty. The rounded concave corner
            fills the part of it between the corner and its quarter circle, so it paints some yellow pixels there.
        """
        given : 'A function which builds a component with the three words in red, blue and black, an opaque yellow selection colour and the given radii:'
            var componentWith = { double convex, double concave ->
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),         "HHHH " ),
                                StyledString.of( f -> f.color(BLUE),        "HHHH " ),
                                StyledString.of( f -> f.color(Color.BLACK), "HHHH"  )
                            )
                            .selection( s -> s.color(YELLOW).mergeLines(true).convexRadius(convex).concaveRadius(concave) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(2, 14)))
                    .get(JBox)
            }
        and : 'We render both components:'
            var roundedConvex  = Utility.renderSingleComponent(componentWith(12, 0))
            var roundedConcave = Utility.renderSingleComponent(componentWith(0, 12))
        and : """
            We find the corners in the image with square concave corners, where no curve bends into the empty space
            next to the selection. The red letters mark the rows of the first line. In these rows, the leftmost yellow
            pixel column is the left edge of the selected part of the first line, because its rounded convex corner
            only takes the upper half of that edge. The topmost yellow pixel row is the top edge of the first line.
            The column 15 pixels left of the left edge lies above the second line, and it lies more than 12 pixels
            right of the start of the second line, beyond the curve of its rounded convex corner. In that column, the
            topmost yellow pixel row is the top edge of the second line:
        """
            var firstLine   = boundsOfPixelsPaintedIn(roundedConvex, RED)
            int left        = boundsOfPixelsPaintedIn(roundedConvex, YELLOW, new Rectangle(0, firstLine.y as int, roundedConvex.width, firstLine.height as int)).x
            int top         = boundsOfPixelsPaintedIn(roundedConvex, YELLOW).y
            int secondLine  = boundsOfPixelsPaintedIn(roundedConvex, YELLOW, new Rectangle(left - 15, 0, 1, roundedConvex.height)).y
        and : 'The pixel near the convex corner, and the square of 7 × 7 pixels left of and above the concave corner:'
            var nearTheConvexCorner = new Rectangle(left + 1, top + 1, 1, 1)
            var besideTheConcaveCorner = new Rectangle(left - 7, secondLine - 7, 7, 7)

        expect : 'The pixel near the convex corner is yellow only with the square convex corner:'
            boundsOfPixelsPaintedIn(roundedConvex,  YELLOW, nearTheConvexCorner) == null
            boundsOfPixelsPaintedIn(roundedConcave, YELLOW, nearTheConvexCorner) != null
        and : 'The square beside the concave corner has yellow pixels only with the rounded concave corner:'
            boundsOfPixelsPaintedIn(roundedConvex,  YELLOW, besideTheConcaveCorner) == null
            boundsOfPixelsPaintedIn(roundedConcave, YELLOW, besideTheConcaveCorner) != null
    }

    def 'The selection style made by `borderRadius(5)` equals the selection style made by `convexRadius(5).concaveRadius(5)`.'()
    {
        reportInfo """
            `borderRadius(..)` rounds every corner of the area behind the selected characters with one radius.
            With `mergeLines(true)`, that area has convex corners, which point out of it like the corners of a
            rectangle, and concave corners, which point into it like the corner where the end of a short line meets
            the longer line below it. `borderRadius(r)` gives both kinds the radius r, so it makes exactly the
            selection style which `convexRadius(r).concaveRadius(r)` makes. Two equal styles are also painted the
            same, and SwingTree paints a style which is equal to the one it painted before from the image it
            painted then.

            We let the style of a text hand the selection style which it builds to a variable of this scenario.
            `selection(..)` passes the current selection style to the function you give it, and uses the style
            which that function returns.
        """
        given : 'Two variables which receive the selection styles:'
            var withBorderRadius = null
            var withBothRadii    = null
        and : 'Two components whose selection styles are built with `borderRadius(5)` and with `convexRadius(5).concaveRadius(5)`:'
            UI.box()
            .withStyle( it -> it.text("body", t -> t.content("Hello world").selection( s -> withBorderRadius = s.mergeLines(true).borderRadius(5) )) )
            .withTextSelection("body")
            .get(JBox)
            UI.box()
            .withStyle( it -> it.text("body", t -> t.content("Hello world").selection( s -> withBothRadii = s.mergeLines(true).convexRadius(5).concaveRadius(5) )) )
            .withTextSelection("body")
            .get(JBox)

        expect : 'Both styles are equal, have the same hash code and print the same text:'
            withBorderRadius != null
            withBorderRadius == withBothRadii
            withBorderRadius.hashCode() == withBothRadii.hashCode()
            withBorderRadius.toString() == withBothRadii.toString()
        and : 'A style with different radii is not equal to them:'
            withBorderRadius != withBothRadii.concaveRadius(6)
    }

    def 'With `mergeLines(true)` and `concaveRadius(2)`, a border of 8 pixels is about 8 pixels wide at a concave corner too.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape and paints the border along its outline, growing inwards.
            A concave corner points into the shape, like the corner where the end of a short line meets the
            longer line below it. A convex corner points out of the shape, like the corners of a rectangle.

            The outer edge of the border is the outline. At a concave corner with a radius of 2 pixels, the outer
            edge is a quarter circle of radius 2 around a centre which lies in the empty space next to the corner,
            2 pixels left of and 2 pixels above it. The inner edge of the border lies 8 pixels further inside the
            shape, so at the concave corner it is a quarter circle of radius 2 + 8 = 10 around the same centre.
            Then the border is 10 − 2 = 8 pixels wide along the whole curve, just like along a straight edge. If the
            inner edge had the radius 2 of the outer edge, its centre would lie 8 pixels further right and 8 pixels
            further down, and on the line through the corner at 45 degrees the border would be 8 × √2 ≈ 11.3 pixels
            wide instead of 8.

            SwingTree uses at most half of an edge for the curve of one corner. The shortest edge next to the inner
            curve is the left edge of the inner side of the border of the first line, which is as tall as the line,
            about 22.4 pixels for a font of size 20. Half of it is about 11.2 pixels, so the radius of 10 is
            not cut.

            The text is "HHHH HHHH HHHH" in a content area which is 100 pixels wide, so every word gets a line of its
            own: one word is about 56 pixels wide and a space about 5 pixels, and two words need about 56 + 5 + 56 = 117
            pixels. The selection from position 2 to position 14 starts behind the second "H" of the first line, which
            makes a concave corner where the left edge of the selected part of the first line meets the top edge of
            the second line.

            We count the green pixels on the line at 45 degrees which starts at the centre of the quarter circles and
            runs through the corner, one pixel to the right and one pixel down per step. Each step is √2 ≈ 1.41 pixels
            long. The border of 8 pixels covers 8 ÷ 1.41 ≈ 5.7 steps of that line, and a border of 11.3 pixels would
            cover 8 steps. Pixels at the edges of the border are only partly covered, and the method
            `boundsOfPixelsPaintedIn` of this specification only counts pixels which are close to pure green, so we
            expect between 4 and 6 green pixels.
        """
        given : 'A component with the three words in red, blue and black, an opaque yellow selection colour, a concave radius of 2 and a green border of 8 pixels:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),         "HHHH " ),
                                StyledString.of( f -> f.color(BLUE),        "HHHH " ),
                                StyledString.of( f -> f.color(Color.BLACK), "HHHH"  )
                            )
                            .selection( s -> s.color(YELLOW).mergeLines(true).convexRadius(0).concaveRadius(2).border(8, GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(2, 14)))
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)
        and : """
            We find the concave corner. The red letters mark the rows of the first line. In these rows, the leftmost
            green pixel column is the left edge of the selected part of the first line. In the column 20 pixels left
            of that edge, which lies above the second line but not above the selected part of the first line, the
            topmost green pixel row is the top edge of the second line:
        """
            var firstLine  = boundsOfPixelsPaintedIn(image, RED)
            int left       = boundsOfPixelsPaintedIn(image, GREEN, new Rectangle(0, firstLine.y as int, image.width, firstLine.height as int)).x
            int secondLine = boundsOfPixelsPaintedIn(image, GREEN, new Rectangle(left - 20, 0, 1, image.height)).y
        and : 'We count the green pixels on the line at 45 degrees from the centre of the quarter circles, 2 pixels left of and above the corner, through the corner and 14 steps beyond it:'
            int greenOnTheDiagonal = (0..<16).count { int step ->
                boundsOfPixelsPaintedIn(image, GREEN, new Rectangle(left - 2 + step, secondLine - 2 + step, 1, 1)) != null
            }

        then : 'The border covers between 4 and 6 steps of the line at 45 degrees, so it is about 8 pixels wide there:'
            greenOnTheDiagonal >= 4
            greenOnTheDiagonal <= 6
    }

    def 'With `mergeLines(true)`, the shape of a selection on both sides of an obstacle paints neither the selection colour nor the border on the obstacle.'()
    {
        reportInfo """
            An obstacle is a shape which you pass to `obstacles(..)`, and SwingTree paints no text on it. Here a
            rectangle from x = 150 to x = 250 and from y = 0 to y = 40 is in the way of the first line. The content
            area reaches from x = 10 to x = 390, so the first line has room for letters on two sides of the obstacle.

            The text is "HHHHH HHHHHHH HHHHHHH". At a font size of 20 an "H" is about 14 pixels wide. The first word
            and the space behind it, about 75 pixels, fit left of the obstacle, the second word, about 98 pixels,
            goes right of it, and the third word goes to the second line. The selection from position 0 to position 21
            selects all three words.

            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of neighbouring
            lines where they overlap horizontally. The two rectangles of the first line lie on the same line, left
            and right of the obstacle, and they do not touch, so SwingTree does not join them to each other. The left
            rectangle of the first line and the rectangle of the second line overlap horizontally and become one shape.
            With a concave radius, which rounds the corners pointing into the shape, the curve at the end of the first
            word bends into the space between the first word and the obstacle, but never onto the obstacle. If
            SwingTree joined the two rectangles of the first line, the shape would cover the obstacle, for example a
            child component which sits there.
        """
        given : 'A rectangle from x = 150 to x = 250 across the first line:'
            var obstacle = new Rectangle(150, 0, 100, 40)
        and : 'A component which paints the three words in red, with an opaque yellow selection colour, a green border of 2 pixels and rounded corners:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacle)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHHH HHHHHHH HHHHHHH" ) )
                            .selection( s -> s.color(YELLOW).border(2, GREEN).mergeLines(true).borderRadius(6) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 21)))
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)

        then : 'The selection colour and the border are painted on both sides of the obstacle:'
            boundsOfPixelsPaintedIn(image, YELLOW, new Rectangle(0,   0, 150, 40)) != null
            boundsOfPixelsPaintedIn(image, YELLOW, new Rectangle(250, 0, 150, 40)) != null
            boundsOfPixelsPaintedIn(image, GREEN,  new Rectangle(0,   0, 150, 40)) != null
            boundsOfPixelsPaintedIn(image, GREEN,  new Rectangle(250, 0, 150, 40)) != null
        and : 'Neither colour is painted on the obstacle:'
            boundsOfPixelsPaintedIn(image, YELLOW, obstacle) == null
            boundsOfPixelsPaintedIn(image, GREEN,  obstacle) == null
    }

    def 'With `mergeLines(true)`, SwingTree paints no selection colour under the border, so partly transparent colours do not mix where the lines are joined.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of neighbouring
            lines into one shape, paints the border along the inner edge of its outline, and fills only what lies
            inside the border with the selection colour. The two colours never lie on top of each other, so a pixel
            of the border shows the border colour mixed with the background, and a pixel inside the border shows the
            selection colour mixed with the background.

            This scenario uses a half transparent blue border and a half transparent green selection colour on a
            white background, so four mixtures are possible:
            the border on white is (127, 127, 237),
            the selection colour on white is (127, 202, 127),
            the border on the selection colour on white is (63, 101, 174),
            and the selection colour on the border on white is (63, 139, 118).
            Only the first two may appear. Where the inner edge of the border runs through a row of pixels, each
            pixel of that row is covered partly by the border and partly by the selection colour, and it is lighter
            than the two mixtures which must not appear, and differs from each of them by more than 60 in at least
            one of its red, green and blue values.

            The text is "HHHH HHHH HHHH" in a content area which is 100 pixels wide, so every word gets a line of its
            own: one word is about 56 pixels wide and a space about 5 pixels, and two words need about 56 + 5 + 56 = 117
            pixels. The selection from position 0 to position 14 selects all three lines. The letters are white, so
            that their smoothed edges, which mix white into the colours below them, never come close to one of the
            two mixtures which must not appear.
        """
        given : 'A half transparent blue for the border and a half transparent green for the selection colour:'
            var borderColour    = new Color(0, 0, 220, 128)
            var selectionColour = new Color(0, 150, 0, 128)
        and : 'A component which paints three lines of white letters on white, all selected, joined into one shape with a border of 3 pixels:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(WHITE), "HHHH HHHH HHHH" ) )
                            .selection( s -> s.color(selectionColour).border(3, borderColour).mergeLines(true).borderRadius(6) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 14)))
                    .get(JBox)

        when : 'We render the component:'
            var image = Utility.renderSingleComponent(box)

        then : 'The border on white and the selection colour on white are both painted:'
            boundsOfPixelsPaintedIn(image, new Color(127, 127, 237)) != null
            boundsOfPixelsPaintedIn(image, new Color(127, 202, 127)) != null
        and : 'Neither colour is painted on top of the other:'
            boundsOfPixelsPaintedIn(image, new Color(63, 101, 174)) == null
            boundsOfPixelsPaintedIn(image, new Color(63, 139, 118)) == null
    }

    def 'With `mergeLines(true)` and a radius larger than half of a line, SwingTree uses half of each edge for the curve of a corner and paints nothing outside the area of the selection.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape and rounds its corners. A convex corner points out of the shape,
            like each of the four corners of a rectangle, and `convexRadius(..)` rounds it. On one line the shape
            is a single rectangle, so all of its corners are convex.

            The curve of a corner starts on one edge and ends on the other edge. SwingTree uses at most half of an
            edge for the curve of one corner, so the curves of the two corners at the ends of an edge never overlap.
            The text is "Hello world", and the selection from position 6 to position 11 selects "world". A line of
            a font of size 20 is about 22.4 pixels tall, so half of the left and of the right edge of the
            selection is about 11.2 pixels. With `borderRadius(40)`, the curves at the four corners therefore have a
            radius of about 11.2 pixels, which turns the left and the right end of the selection into half circles.

            If SwingTree used the full radius of 40 pixels, the curves would start 40 pixels away from each corner,
            beyond the end of the left and the right edge, and the outline would leave the selected area. We compare
            the yellow pixels with the yellow pixels of the same selection without rounded corners: they must not
            reach beyond them, and the middle of the selection must still be yellow.
        """
        given : 'A function which builds a component that paints "Hello world" in blue on white, with "world" selected, an opaque yellow selection colour and the given radius:'
            var componentWithRadius = { double radius ->
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> s.color(YELLOW).mergeLines(true).borderRadius(radius) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)
            }

        when : 'We render the selection with square corners and with a radius of 40 pixels, and find the rectangles around their yellow pixels:'
            var square  = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(componentWithRadius(0)), YELLOW)
            var rounded = Utility.renderSingleComponent(componentWithRadius(40))
            var roundedSelection = boundsOfPixelsPaintedIn(rounded, YELLOW)

        then : 'The yellow pixels with the radius of 40 lie inside the yellow pixels with square corners, give or take one pixel:'
            roundedSelection.x >= square.x - 1
            roundedSelection.y >= square.y - 1
            roundedSelection.x + roundedSelection.width  <= square.x + square.width  + 1
            roundedSelection.y + roundedSelection.height <= square.y + square.height + 1
        and : 'The pixel in the middle of the selection, 2 pixels below its top edge and above the letters, is yellow:'
            boundsOfPixelsPaintedIn(rounded, YELLOW, new Rectangle(square.centerX as int, (square.y + 2) as int, 1, 1)) != null
    }

    def 'For a selection which ends where a line wraps, the rectangle of its end lies at the right end of the upper line, next to the last selected character, while a caret without selected characters at that place lies at the start of the next line.'()
    {
        reportInfo """
            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". At a font size
            of 20, one word of four "H" letters is about 56 pixels wide and a space about 5 pixels, so the two
            words need about 56 + 5 + 56 = 117 pixels, more than the 100 pixels of the content area. With
            `wrapLines(true)` the first word and the space behind it go onto the first line, and the second word
            onto the second line.

            A selection holds two positions, and a position is a gap between two characters. Position 5 is the
            gap behind the space, which is where the first line ends. It is also the gap in front of the first "H"
            of the second line, where the second line starts. So position 5 has two places on the component.

            A selection from position 2 to position 5 selects the last two "H" letters of the first word and the
            space, which all lie on the first line. Its end, position 5, belongs at the right end of the first line,
            next to the last selected character. So `caretBounds()` returns the rectangle at the right end of the
            first line. If it returned the rectangle at the start of the second line, a handle which your code
            paints at the caret would hang in front of the second word, away from the selected characters.
            For the same selection made backwards, from position 5 to position 2, the end is the anchor, so
            `anchorBounds()` returns the rectangle at the right end of the first line.

            A caret without selected characters at position 5 belongs to no selected character, and its rectangle
            lies at the start of the second line, in front of the character behind it.
        """
        given : 'A property which holds a caret at position 5, without selected characters:'
            var selection = Var.of(TextSelection.caretAt(5))
        and : 'A component whose content area is 100 pixels wide, with the first word in red and the second word in green:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red and the green pixels. When SwingTree
            paints the component, it queues a task on the UI thread which writes the selection with its pixel
            rectangles into the property. `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            var image = Utility.renderSingleComponent(box)
            UI.sync()
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)
        and : """
            We read the caret at position 5 from the property, and move its caret to position 2 with `withCaret(2)`.
            That makes the selection from 5 to 2, made backwards, whose anchor is at position 5:
        """
            var caretOnly = selection.get()
            var backwards = caretOnly.withCaret(2)
        and : 'Your code writes the selection from 2 to 5 into the property, and we render the component again so that SwingTree fills in its rectangles:'
            UI.runNow { selection.set(TextSelection.between(2, 5)) }
            Utility.renderSingleComponent(box)
            UI.sync()
            var forwards = selection.get()

        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The caret of the selection from 2 to 5 lies right of the red letters, and reaches from above to below them:'
            forwards.caretBounds().get().location().x() >= firstWord.x + firstWord.width - 1
            forwards.caretBounds().get().location().y() <= firstWord.y
            forwards.caretBounds().get().location().y() + forwards.caretBounds().get().size().height().get() >= firstWord.y + firstWord.height
        and : 'The anchor of the selection from 5 to 2 has the same rectangle:'
            backwards.anchorBounds() == forwards.caretBounds()
        and : 'The caret at position 5 without selected characters lies in front of the green letters, and reaches from above to below them:'
            caretOnly.caretBounds().get().location().x() <= secondWord.x
            caretOnly.caretBounds().get().location().y() <= secondWord.y
            caretOnly.caretBounds().get().location().y() + caretOnly.caretBounds().get().size().height().get() >= secondWord.y + secondWord.height
    }

    def 'With `mergeLines(true)` and a font size of #fontSize at a UI scale of #uiScale, a selection over six lines has no border between its lines, even where the bottom of one line and the top of the next differ by a tiny fraction of a pixel.'(
        float uiScale, int fontSize
    ) {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape and paints the border only along the outline of that shape.
            SwingTree computes the top and the bottom of every line with floating point numbers, by adding
            the heights of the lines above it. Such sums are rounded, so the bottom of one line and the top of
            the next can differ by a few millionths of a pixel. For the Ubuntu font at a size of 23 they do at
            the UI scales 1, 1.5 and 2, and at a size of 20 they do at the UI scales 1 and 2. At a size of 20
            and a UI scale of 1.5 they do not, and there this scenario only checks that no border is painted
            between the lines.

            Your users cannot see a gap of a few millionths of a pixel. But if SwingTree treated the two lines
            as two shapes because of it, it would paint the bottom border of the upper line and the top border
            of the lower line across the selection, a bar of 2 + 2 = 4 pixels, multiplied by the UI scale.
            So SwingTree treats a gap of less than half a pixel between two lines as no gap at all.

            The content area of the component is 90 pixels wide, multiplied by the UI scale like the font size,
            and the text is six words of four "H" letters. At a font size of 20 an "H" is about 14 pixels wide
            and a space about 5 pixels, and at a font size of 23 an "H" is about 16 pixels wide and a space also
            about 5 pixels. One word and the space behind it need at most 4 × 16 + 5 = 69 pixels and fit into a
            line, and two words need at least 4 × 14 + 5 + 4 × 14 = 117 pixels and do not. So with `wrapLines(true)`
            every word gets a line of its own. The selection selects all six lines, and the border is 2 pixels wide.

            We look at the pixel column in the middle of the selection. The border along the top of the first
            line and along the bottom of the last line lies in that column, so we leave out the 3 pixel rows at
            the top and at the bottom of the green pixels, multiplied by the UI scale, and expect no green pixel
            in the rest of the column.

            In this scenario the UI scale is ${uiScale} and the font size is ${fontSize}.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A component whose content area is 90 pixels wide, with six words in blue, an opaque yellow selection colour, all six lines joined into one shape, and a green border of 2 pixels:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(110, 250)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(fontSize) )
                            .content( StyledString.of( f -> f.color(BLUE), "HHHH HHHH HHHH HHHH HHHH HHHH" ) )
                            .selection( s -> s.color(YELLOW).mergeLines(true).border(2, GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 29)))
                    .get(JBox)

        when : 'We render the component and find the rectangle around the green pixels:'
            var image  = Utility.renderSingleComponent(box)
            var border = boundsOfPixelsPaintedIn(image, GREEN)
        and : 'The pixel column in the middle of the selection, without 3 pixel rows at its top and at its bottom, multiplied by the UI scale:'
            int edgeRows = 3 * uiScale
            var middleColumn = new Rectangle(border.centerX as int, (border.y + edgeRows) as int, 1, (border.height - 2 * edgeRows) as int)

        then : 'The selection is painted in the middle column, and no border colour:'
            boundsOfPixelsPaintedIn(image, YELLOW, middleColumn) != null
            boundsOfPixelsPaintedIn(image, GREEN,  middleColumn) == null

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            [uiScale, fontSize] << [[1f, 1.5f, 2f], [20, 23]].combinations()
    }

    def 'With `mergeLines(true)`, when your code writes a new selection into the property of `withTextSelection`, the selection colour moves to the newly selected letters.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape. Building that shape takes some work, so SwingTree keeps the
            shape of the last painted selection and paints it again as long as the rectangles behind the
            selected characters and the look of the selection stay the same. When your code selects other
            characters, the rectangles change, and SwingTree builds a new shape.

            The component paints the text "Hello world". A selection holds two positions, and a position is
            a gap between two characters: position 0 is the gap in front of the "H", position 5 is the gap
            behind the "o" of "Hello", position 6 is the gap in front of the "w", and position 11 is the gap
            behind the "d". First the property holds the selection from 0 to 5, then your code writes the
            selection from 6 to 11. If SwingTree painted the kept shape of the first selection again, the
            selection colour would stay behind "Hello".
        """
        given : 'A property which holds the selection from 0 to 5, so "Hello" is selected:'
            var selection = Var.of(TextSelection.between(0, 5))
        and : 'A component which paints "Hello " in red and "world" in blue on white, with an opaque yellow selection colour and `mergeLines(true)`:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.color(YELLOW).mergeLines(true) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : 'We render the component and find the rectangles around the red, the blue and the yellow pixels:'
            var image = Utility.renderSingleComponent(box)
            var hello     = boundsOfPixelsPaintedIn(image, RED)
            var world     = boundsOfPixelsPaintedIn(image, BLUE)
            var highlight = boundsOfPixelsPaintedIn(image, YELLOW)
        then : 'The yellow rectangle encloses "Hello" and ends left of "world":'
            highlight.x <= hello.x
            highlight.x + highlight.width >= hello.x + hello.width
            highlight.x + highlight.width < world.x

        when : 'Your code selects "world" by writing the selection from 6 to 11 into the property, and we render the component again:'
            UI.runNow { selection.set(TextSelection.between(6, 11)) }
            image     = Utility.renderSingleComponent(box)
            highlight = boundsOfPixelsPaintedIn(image, YELLOW)
        then : 'The yellow rectangle encloses "world" and begins right of "Hello":'
            highlight.x <= world.x
            highlight.x + highlight.width >= world.x + world.width
            highlight.x > hello.x + hello.width
    }

    def 'With `mergeLines(true)` and a border wider than half of the selected area, the border fills the whole area and SwingTree paints no selection colour, inside or outside of it.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape, and `border(width, colour)` paints a border along the inner edge
            of its outline. The inner edge of the border lies the width of the border inside the outline, and
            the selection colour fills what lies inside the inner edge.

            The text is "Hello world", and the selection from position 2 to position 3 selects the first "l",
            which is about 5 pixels wide at a font size of 20. The border is 8 pixels wide. Moving both
            sides of the selected area 8 pixels inwards leaves nothing: the left side would end up right of the
            right side. So the border covers the whole selected area, and no selection colour is painted. If
            SwingTree moved the sides anyway, the two sides would swap places, and the selection colour would
            be painted in a strip which reaches up to 8 − 5 = 3 pixels beyond each side of the selected area.
        """
        given : 'A function which builds a component that paints "Hello world" in blue on white, with the first "l" selected, an opaque yellow selection colour and `mergeLines(true)`, with or without a green border of 8 pixels:'
            var componentWithBorder = { boolean withBorder ->
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "Hello world" ) )
                            .selection( s -> withBorder ? s.color(YELLOW).mergeLines(true).border(8, GREEN) : s.color(YELLOW).mergeLines(true) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(2, 3)))
                    .get(JBox)
            }

        when : 'We render the selection without and with the border:'
            var withoutBorder = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(componentWithBorder(false)), YELLOW)
            var image         = Utility.renderSingleComponent(componentWithBorder(true))
            var border        = boundsOfPixelsPaintedIn(image, GREEN)

        then : 'No pixel is painted in the selection colour:'
            boundsOfPixelsPaintedIn(image, YELLOW) == null
        and : 'The green pixels lie where the yellow pixels lie without a border, give or take one pixel at each edge:'
            Math.abs(border.x - withoutBorder.x) <= 1
            Math.abs((border.x + border.width) - (withoutBorder.x + withoutBorder.width)) <= 1
    }

    def 'SwingTree paints a caret without selected characters at the place where a line wraps in front of the first character of the next line, where `caretBounds()` says it is.'()
    {
        reportInfo """
            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". At a font size
            of 20, one word of four "H" letters is about 56 pixels wide and a space about 5 pixels, so the two
            words need about 56 + 5 + 56 = 117 pixels, more than the 100 pixels of the content area. With
            `wrapLines(true)` the first word and the space behind it go onto the first line, and the second word
            onto the second line.

            A position is a gap between two characters. Position 5 is the gap behind the space, where the first
            line ends, and also the gap in front of the first "H" of the second line. A caret without selected
            characters at position 5 belongs in front of the character behind it, so SwingTree paints it at the
            start of the second line, and `caretBounds()` returns that place too. If SwingTree painted the caret at
            the end of the first line, your users would see it in another place than the one where your code
            paints something at `caretBounds()`.
        """
        given : 'A property which holds a caret at position 5, without selected characters:'
            var selection = Var.of(TextSelection.caretAt(5))
        and : 'A component whose content area is 100 pixels wide, with the first word in red, the second word in blue, and a green caret:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "HHHH " ),
                                StyledString.of( f -> f.color(BLUE), "HHHH"  )
                            )
                            .selection( s -> s.showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red, the blue and the green pixels. When
            SwingTree paints the component, it queues a task on the UI thread which writes the caret rectangle into
            the property. `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            var image = Utility.renderSingleComponent(box)
            UI.sync()
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, BLUE)
            var caret      = boundsOfPixelsPaintedIn(image, GREEN)
            var bounds     = selection.get().caretBounds().get()

        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The painted caret lies in front of the blue letters, below the red letters:'
            caret.x <= secondWord.x
            caret.y >= firstWord.y + firstWord.height - 1
        and : 'The painted caret lies where `caretBounds()` says, give or take one pixel:'
            Math.abs(caret.x - bounds.location().x()) <= 1
            Math.abs(caret.y - bounds.location().y()) <= 1
    }

    def 'When a view is built again with the same property of `withTextSelection`, while the first component is thrown away, SwingTree logs no warning about two components.'()
    {
        reportInfo """
            In a SwingTree application a view is often built again while its view model lives on, for example
            when a property swaps the view, or when a tab or an entry of a list is shown again. The new component
            then gets the same `Var<TextSelection>` which the thrown away component had.

            After every paint, a component made selectable with `withTextSelection` writes a selection into its
            property which holds the pixel rectangles and the selected characters of its own text. The new
            component is therefore shown one selection which the thrown away component wrote. A component which
            is thrown away never paints again, so this happens once. Two components which both stay on the screen
            instead write into the property again and again, and only then does SwingTree log the warning that
            one property is bound to two components. If SwingTree warned about the one selection of the thrown
            away component, every view which is built again would fill the log with a warning about a mistake
            which your code did not make.
        """
        given : 'A spy which records the messages that SwingTree logs:'
            var log = LogSpy.attach()
        and : 'A property which outlives the views, and a function which builds a view of it:'
            var selection = Var.of(TextSelection.caretAt(0))
            var buildView = { String name ->
                    UI.box()
                    .id(name)
                    .withStyle( it -> it
                        .size(400, 100)
                        .text("body", t -> t.font( f -> f.family("Ubuntu").size(20) ).content("Hello world"))
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
            }

        when : """
            We build the first view, paint it, and throw it away. When SwingTree paints a component, it queues a
            task on the UI thread which writes the selection of that component into the property. `UI.sync()`
            waits until the UI thread has run every task queued so far:
        """
            var first = buildView("first")
            Utility.renderSingleComponent(first)
            UI.sync()
            first = null
        and : 'We build the second view with the same property, paint it twice, let your code select "world", and paint it again:'
            var second = buildView("second")
            Utility.renderSingleComponent(second)
            UI.sync()
            Utility.renderSingleComponent(second)
            UI.sync()
            UI.runNow { selection.set(TextSelection.between(6, 11)) }
            Utility.renderSingleComponent(second)
            UI.sync()

        then : 'SwingTree logged no warning about a property which is bound to two components:'
            !log.warnings().any { it.contains("Var<TextSelection>") }
        and : 'The property holds the selection of "world", which the second view filled in:'
            selection.get().selectedText() == "world"

        cleanup : 'We detach the spy:'
            log?.detach()
    }

    def 'With `EventProcessor.DECOUPLED`, a selection which your user makes keeps `isEnabled()` as your code sets it, even when your code disables the selection while the selection of your user is still on its way to the application thread.'()
    {
        reportInfo """
            `isEnabled()` of a `TextSelection` belongs to your code: it decides whether your users may select.
            The positions of a selection which your user makes belong to your user.

            With `EventProcessor.DECOUPLED`, the mouse events arrive on the UI thread, and SwingTree hands the
            new selection to the application thread as a task, which writes it into the property. Here your user
            drags the mouse across "world", and before the application thread runs that task, your code disables
            the selection with `withEnabled(false)`. Then the application thread runs the task of SwingTree.

            The task keeps the flag which the property holds when the task runs, and takes the positions of your
            user. So the property ends with the positions of "world" and disabled. If the task wrote the selection
            of your user as it was made, with the flag it had on the UI thread, it would enable the selection
            again, and your users could select although your code had switched selecting off.
        """
        given : 'A property which holds an enabled caret in front of the first character:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : """
            A component which paints "Hello " in red and "world" in blue. We declare it inside
            `UI.use(EventProcessor.DECOUPLED, ..)`, so that it hands its writes into the property over to the
            application thread. We declare it on the UI thread with `UI.runAndGet`, because with
            `EventProcessor.DECOUPLED` the method `get(..)`, which returns the component, throws an exception on
            every other thread:
        """
            var box = UI.runAndGet({ ->
                UI.use(EventProcessor.DECOUPLED, () ->
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                ).get(JBox)
            })
        and : """
            In this scenario no thread runs the tasks of the application thread on its own. The test thread plays
            the application thread: `EventProcessor.DECOUPLED.joinUntilDoneOrException()` runs every task which
            waits for the application thread, and `UI.sync()` waits until the UI thread has run every task queued
            so far. We paint the component, let both threads run their tasks, and find the rectangle around the
            blue pixels of "world". Its leftmost pixel column belongs to the left half of the "w" and its rightmost
            pixel column belongs to the right half of the "d":
        """
            var image = Utility.renderSingleComponent(box)
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()
            var world = boundsOfPixelsPaintedIn(image, BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'Your user drags the mouse from the left edge of the "w" to the right edge of the "d", on the UI thread:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'Before the application thread runs the tasks of SwingTree, your code disables the selection, on the test thread, which plays the application thread:'
            selection.set(selection.get().withEnabled(false))
        and : 'We let both threads run their tasks until neither has a task left:'
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()

        then : 'The property holds the positions of "world" which your user selected:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
        and : 'The selection is still disabled, as your code set it:'
            !selection.get().isEnabled()
    }

    def 'With `mergeLines(true)`, a component which paints the same selection again, after a change which does not move the selected letters, paints the selection colour and the border where it painted them before.'()
    {
        reportInfo """
            With `mergeLines(true)`, SwingTree joins the rectangles behind the selected characters of
            neighbouring lines into one shape. Building that shape takes some work, so SwingTree keeps the shape
            of the last painted selection, and paints it again while the rectangles behind the selected characters
            and the look of the selection stay the same.

            The text is "HHHH HHHH HHHH" in a content area which is 100 pixels wide, so every word gets a line of its
            own: one word is about 56 pixels wide and a space about 5 pixels, and two words need about 56 + 5 + 56 =
            117 pixels. The selection from position 2 to position 14 covers the last two letters of the first word
            and the two other words. Your code then makes the component 1 pixel wider. The content area becomes
            101 pixels wide, which is still less than 117, so every word stays on its line, and the text starts at
            the top left corner of the content area as before. So the rectangles stay the same, and SwingTree paints
            the kept shape. The selection colour and the border must lie exactly where they lay before.
        """
        given : 'The width of the component, held in a variable which the style reads every time SwingTree computes the style:'
            var width = 120
        and : 'A component whose content area is 100 pixels wide, with an opaque yellow selection colour, the lines joined into one shape and a green border of 2 pixels:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(width, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(BLUE), "HHHH HHHH HHHH" ) )
                            .selection( s -> s.color(YELLOW).mergeLines(true).border(2, GREEN).borderRadius(4) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(2, 14)))
                    .get(JBox)

        when : 'We render the component and find the rectangles around the yellow and the green pixels:'
            var firstImage = Utility.renderSingleComponent(box)
            var firstSelection = boundsOfPixelsPaintedIn(firstImage, YELLOW)
            var firstBorder    = boundsOfPixelsPaintedIn(firstImage, GREEN)
        and : """
            Your code makes the component 1 pixel wider, and we render it again. `setSize(..)` gives the component
            its new size right away, so that the rendered image has it too:
        """
            width = 121
            UI.runNow { box.setSize(121, 150) }
            var secondImage = Utility.renderSingleComponent(box)

        then : 'The component really is 1 pixel wider:'
            secondImage.width == firstImage.width + 1
        and : 'The yellow and the green pixels lie exactly where they lay before:'
            boundsOfPixelsPaintedIn(secondImage, YELLOW) == firstSelection
            boundsOfPixelsPaintedIn(secondImage, GREEN)  == firstBorder
    }

    def 'With `withTextSelection("body", selection)` on a component whose own cursor your code has set, the mouse cursor is the text cursor over the text and that cursor again over the rest of the component.'()
    {
        reportInfo """
            With `withTextSelection`, the mouse cursor turns into the text cursor while the mouse is over the text,
            so that your users see that they can select it. Your code may have given the component a cursor of its
            own, for example the hand cursor with `withCursor(UI.Cursor.HAND)`. When the mouse leaves the text,
            SwingTree gives the component that cursor back, and not the default arrow cursor. If SwingTree set the
            default cursor instead, the hand cursor of your code would be gone after the first time the mouse passed
            over the text.

            The component is 400 pixels wide and 100 pixels tall. It paints one line of text at its top, so its
            lower half has no text.
        """
        given : 'A component with the hand cursor, which paints "Hello world" in red in its top left corner:'
            var box =
                    UI.box()
                    .withCursor(UI.Cursor.HAND)
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(0)))
                    .get(JBox)
        and : 'We render the component and find the middle of the red pixels:'
            var text = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), RED)
            int middleX = text.centerX
            int middleY = text.centerY

        when : 'We move the mouse onto the middle of the text:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_MOVED, 0, 0, middleX, middleY, 0, false, MouseEvent.NOBUTTON))
            }
        then : 'The mouse cursor is the text cursor:'
            box.getCursor().getType() == Cursor.TEXT_CURSOR

        when : 'We move the mouse to x = 200 and y = 90, which is near the bottom of the component, far below the text:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_MOVED, 1, 0, 200, 90, 0, false, MouseEvent.NOBUTTON))
            }
        then : 'The mouse cursor is the hand cursor again:'
            box.getCursor().getType() == Cursor.HAND_CURSOR
    }

    def 'With `withTextSelection("caption", selection)` on a component whose style declares no text named "caption", nothing is selected, painted or copied, and no error is logged.'()
    {
        reportInfo """
            `withTextSelection("caption", selection)` makes the text which the style of the component declares
            with `text("caption", ..)` selectable. Here the style declares a text named "body", and no text named
            "caption". A style may add such a text later, for example when a style sheet changes, so SwingTree
            does not treat the missing text as a mistake. As long as the text is missing, there is nothing to
            select: the mouse and the keyboard select nothing, nothing is painted in the selection colour, copying
            puts nothing onto the clipboard, the mouse cursor stays the default cursor, and nothing is logged as
            an error. After SwingTree has painted the component, it treats the missing text as a text without
            characters, so it moves both positions of the selection in the property to position 0, the end of a
            text without characters.
        """
        given : 'A spy which records the messages that SwingTree logs:'
            var log = LogSpy.attach()
        and : 'A property which holds the selection from 0 to 5:'
            var selection = Var.of(TextSelection.between(0, 5))
        and : 'A component which paints "Hello world" in red on white in a text named "body", with an opaque yellow selection colour, and which makes the text named "caption" selectable:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "Hello world" ) )
                            .selection( s -> s.color(YELLOW) )
                        )
                    )
                    .withTextSelection("caption", selection)
                    .get(JBox)
        and : 'We render the component and find the rectangle around the red pixels:'
            var image = Utility.renderSingleComponent(box)
            var text  = boundsOfPixelsPaintedIn(image, RED)
            int leftEdgeOfText  = text.x
            int rightEdgeOfText = text.x + text.width - 1
            int lineMiddle      = text.centerY

        when : 'We move the mouse onto the text, press the button on its left edge, drag to its right edge and release it there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_MOVED,    0, 0, leftEdgeOfText, lineMiddle, 0, false, MouseEvent.NOBUTTON))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  1, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfText, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  2, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfText, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, rightEdgeOfText, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : """
            We send the keyboard shortcut for selecting everything, which is Ctrl+A on Windows and Linux and
            Command+A on macOS. We hand the key event to the component with `KeyboardFocusManager.redispatchEvent`,
            because a component outside of a window never has the keyboard focus, and only the component with the
            keyboard focus receives a key event sent with `dispatchEvent`:
        """
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED)
                )
            }
        and : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }
        and : '`UI.sync()` waits until the UI thread has run every task queued so far:'
            UI.sync()

        then : 'No pixel is painted in the selection colour:'
            boundsOfPixelsPaintedIn(image, YELLOW) == null
        and : 'The property holds no selected characters, with both positions at 0:'
            selection.get().anchor() == 0
            selection.get().caret()  == 0
            selection.get().selectedText() == ""
        and : 'The clipboard is empty, and the mouse cursor is the default cursor:'
            clipboard.getContents(null) == null
            box.getCursor().getType() == Cursor.DEFAULT_CURSOR
        and : 'SwingTree logged no error:'
            log.errors().isEmpty()

        cleanup : 'We detach the spy:'
            log?.detach()
    }

    def 'With `withTextSelection("body", selection)` and no selected characters, copying leaves the clipboard as it was.'()
    {
        reportInfo """
            Copying puts the selected characters onto the clipboard. When the selection holds no characters,
            because its anchor and its caret are the same position, there is nothing to copy, so SwingTree leaves
            the clipboard as it was. If SwingTree put an empty text onto the clipboard, copying with nothing selected
            would delete what your users had copied before, for example from another program.

            The component paints the text "Hello world", and the selection is a caret at position 3, behind "Hel".
        """
        given : 'A component which paints "Hello world", with a caret at position 3 and no selected characters:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(3)))
                    .get(JBox)
        and : 'A clipboard that only this scenario uses, which holds the text "copied before":'
            var clipboard = new Clipboard("clipboard of this scenario")
            clipboard.setContents(new StringSelection("copied before"), null)

        when : 'We call `exportToClipboard` on the `TransferHandler` of the component, which copies the selection into that clipboard:'
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The clipboard still holds "copied before":'
            clipboard.getData(DataFlavor.stringFlavor) == "copied before"
    }

    def 'With `selection(s -> s.showCaret(true).caretShape(UI.CaretShape.#shape).caretWidth(3))` at a UI scale of #uiScale, every pixel of the caret lies inside the rectangle which `caretBounds()` returns.'(
        UI.CaretShape shape, float uiScale
    ) {
        reportInfo """
            After SwingTree has painted a text made selectable with `withTextSelection`, the property holds a
            selection whose `caretBounds()` is the pixel rectangle of the caret on the component. Your code can
            paint something of its own next to the caret, for example a round handle below it, and such a handle
            only lines up with the caret if no part of the caret sticks out of that rectangle.

            The rectangle reaches from the top to the bottom of the line, and its left edge is the gap where the
            caret is. A `BAR` is a vertical bar as wide as `caretWidth(3)`, so it fills the rectangle. A `BLOCK`
            covers the character behind the gap, and an `UNDERLINE` is a line, 3 pixels thick, along the bottom
            of that character. For a `BLOCK` and an `UNDERLINE` the rectangle is as wide as the character behind
            the gap, so both shapes stay inside it too.

            The text is "HHHH HHHH", and the caret is at position 2, which is the gap in front of the third "H".
            The caret is painted in green behind the red letters. The edges of a painted rectangle which do not
            fall on a pixel boundary cover their pixel only partly, so the green pixels may reach one pixel beyond
            `caretBounds()` at each edge. If SwingTree painted a shape somewhere else than where `caretBounds()`
            says, for example an underline at the top of the line or a block in front of the gap, the green pixels
            would lie several pixels outside of the rectangle.

            In this scenario the shape is `${shape}` and the UI scale is ${uiScale}.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds an empty selection with the caret at position 2:'
            var selection = Var.of(TextSelection.caretAt(2))
        and : 'A component which paints "HHHH HHHH" in red on white, with a green caret of the given shape, 3 pixels thick:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.showCaret(true).caretShape(shape).caretWidth(3).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red and the green pixels. When SwingTree
            paints the component, it queues a task on the UI thread which writes the caret rectangle into the
            property. `UI.sync()` waits until the UI thread has run every task queued so far:
        """
            var image  = Utility.renderSingleComponent(box)
            UI.sync()
            var text   = boundsOfPixelsPaintedIn(image, RED)
            var caret  = boundsOfPixelsPaintedIn(image, GREEN)
            var bounds = selection.get().caretBounds().get()
            int left   = Math.round(bounds.location().x())
            int top    = Math.round(bounds.location().y())
            int right  = Math.round(bounds.location().x() + bounds.size().width().get())
            int bottom = Math.round(bounds.location().y() + bounds.size().height().get())

        then : 'The text and the caret are painted:'
            text  != null
            caret != null
        and : 'The green pixels lie inside the rectangle of `caretBounds()`, give or take one pixel at each edge:'
            caret.x >= left - 1
            caret.y >= top  - 1
            caret.x + caret.width  <= right  + 1
            caret.y + caret.height <= bottom + 1

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            [shape, uiScale] << [
                [UI.CaretShape.BAR, UI.CaretShape.BLOCK, UI.CaretShape.UNDERLINE],
                [1f, 2f]
            ].combinations()
    }

    def 'With `selection(s -> s.showCaret(true).caretWidth(#caretWidth))` at a UI scale of #uiScale, the caret is a bar #expectedWidth pixels wide on the screen, and `caretBounds()` is just as wide.'(
        double caretWidth, float uiScale, int expectedWidth
    ) {
        reportInfo """
            The default shape of the caret is a vertical bar, and `caretWidth(..)` decides how wide it is.
            Like every size in a style, the width is multiplied by the UI scale: a bar with `caretWidth(4)`
            is 4 pixels wide on the screen at a UI scale of 1 and 8 pixels wide at a UI scale of 2.

            However thin you make it, SwingTree paints the bar at least one pixel wide on the screen. A bar of
            0.2 pixels would cover a fifth of a pixel column, which shows as a faint grey line that your users
            barely see. So `caretWidth(0.2)` at a UI scale of 1 and `caretWidth(0.3)` at a UI scale of 2,
            which is 0.6 pixels on the screen, both paint a bar of one pixel.

            `caretBounds()` is as wide as the bar, so that your code which paints next to the caret knows its
            right edge too. The text is "HHHH HHHH", and the caret is at position 2, which is the gap in front of
            the third "H". The left edge of the bar lies on a whole pixel, so its width shows exactly in the
            green pixels; this scenario still allows one pixel of difference for smoothing.

            In this scenario `caretWidth(${caretWidth})` is used at a UI scale of ${uiScale}.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds an empty selection with the caret at position 2:'
            var selection = Var.of(TextSelection.caretAt(2))
        and : 'A component which paints "HHHH HHHH" in red on white, with a green caret of the given width:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.showCaret(true).caretWidth(caretWidth).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red and the green pixels. `UI.sync()`
            waits until SwingTree has written the caret rectangle into the property:
        """
            var image  = Utility.renderSingleComponent(box)
            UI.sync()
            var text   = boundsOfPixelsPaintedIn(image, RED)
            var caret  = boundsOfPixelsPaintedIn(image, GREEN)
            var bounds = selection.get().caretBounds().get()

        then : 'The caret is painted, at least as tall as the letters:'
            caret.height >= text.height
        and : 'The rectangle of `caretBounds()` is exactly as wide as expected:'
            bounds.size().width().get() == expectedWidth
        and : 'The green pixels are as wide as that, give or take one pixel:'
            Math.abs(caret.width - expectedWidth) <= 1

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            caretWidth | uiScale || expectedWidth
            4          | 1f      || 4
            4          | 2f      || 8
            0.2        | 1f      || 1
            0.3        | 2f      || 1
    }

    def 'Without `caretWidth(..)`, the caret of `selection(s -> s.showCaret(true))` at a UI scale of #uiScale is a bar #expectedWidth pixels wide on the screen, and `caretBounds()` is just as wide.'(
        float uiScale, int expectedWidth
    ) {
        reportInfo """
            The default shape of the caret is a vertical bar, and without `caretWidth(..)` it is 1 pixel wide
            before the UI scale is applied, like the caret of most text fields. Like every size in a style, this
            width is multiplied by the UI scale, so the bar is 1 pixel wide on the screen at a UI scale of 1 and
            2 pixels wide at a UI scale of 2. If SwingTree did not scale the default width, the caret would look
            half as thick as the letters around it on a screen with twice the pixel density.

            The text is "HHHH HHHH", and the caret is at position 2, which is the gap in front of the third "H".
            The left edge of the bar lies on a whole pixel, so its width shows exactly in the green pixels;
            this scenario still allows one pixel of difference for smoothing.

            In this scenario the UI scale is ${uiScale}.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds an empty selection with the caret at position 2:'
            var selection = Var.of(TextSelection.caretAt(2))
        and : 'A component which paints "HHHH HHHH" in red on white, with a green caret and no `caretWidth(..)`:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red and the green pixels. `UI.sync()`
            waits until SwingTree has written the caret rectangle into the property:
        """
            var image  = Utility.renderSingleComponent(box)
            UI.sync()
            var text   = boundsOfPixelsPaintedIn(image, RED)
            var caret  = boundsOfPixelsPaintedIn(image, GREEN)
            var bounds = selection.get().caretBounds().get()

        then : 'The caret is painted, at least as tall as the letters:'
            caret.height >= text.height
        and : 'The rectangle of `caretBounds()` is exactly as wide as expected:'
            bounds.size().width().get() == expectedWidth
        and : 'The green pixels are as wide as that, give or take one pixel:'
            Math.abs(caret.width - expectedWidth) <= 1

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            uiScale || expectedWidth
            1f      || 1
            2f      || 2
    }

    def 'With `selection(s -> s.showCaret(true).caretShape(UI.CaretShape.UNDERLINE).caretWidth(#caretWidth))` at a UI scale of #uiScale, the caret is a line about #expectedThickness pixels thick along the bottom of the line of text.'(
        double caretWidth, float uiScale, int expectedThickness
    ) {
        reportInfo """
            An `UNDERLINE` caret is a line under the character behind the gap, like the caret of an old
            terminal. `caretWidth(..)` decides how thick it is, and the line grows upwards from the bottom
            of the line of text. Like every size in a style, the thickness is multiplied by the UI scale:
            `caretWidth(3)` is 3 pixels thick at a UI scale of 1 and 6 pixels at a UI scale of 2, and
            `caretWidth(2)` is 2 pixels thick at a UI scale of 1.

            The text is "HHHH HHHH", and the caret is at position 2, which is the gap in front of the third "H".
            The bottom of the line of text is the bottom of `caretBounds()`, which lies a few pixels below the
            bottom of the "H", because a line has room for letters like "g" and "y" which reach below the others.
            So the underline is painted below the letters, where your users see it. If SwingTree grew the line
            downwards from the bottom of the line, it would be painted outside of the line, and if it ignored
            `caretWidth(..)`, it would be as thick as a whole line.

            The top and the bottom of a line of text rarely fall on a pixel boundary, so the edges of the
            underline cover their pixel row only partly, and this scenario allows one pixel of difference.

            In this scenario `caretWidth(${caretWidth})` is used at a UI scale of ${uiScale}.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds an empty selection with the caret at position 2:'
            var selection = Var.of(TextSelection.caretAt(2))
        and : 'A component which paints "HHHH HHHH" in red on white, with a green underline caret of the given thickness:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.showCaret(true).caretShape(UI.CaretShape.UNDERLINE).caretWidth(caretWidth).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red and the green pixels. `UI.sync()`
            waits until SwingTree has written the caret rectangle into the property:
        """
            var image  = Utility.renderSingleComponent(box)
            UI.sync()
            var text   = boundsOfPixelsPaintedIn(image, RED)
            var caret  = boundsOfPixelsPaintedIn(image, GREEN)
            var bounds = selection.get().caretBounds().get()
            var bottom = bounds.location().y() + bounds.size().height().get()

        then : 'The text and the caret are painted:'
            text  != null
            caret != null
        and : 'The green line is as thick as expected, give or take one pixel:'
            Math.abs(caret.height - expectedThickness) <= 1
        and : 'It ends at the bottom of `caretBounds()`, give or take one pixel:'
            Math.abs((caret.y + caret.height) - bottom) <= 1
        and : 'It lies below the red letters, apart from at most one pixel row of smoothing:'
            caret.y >= text.y + text.height - 1

        cleanup : 'We set the UI scale factor back to 1:'
            SwingTree.get().setUiScaleFactor(1f)

        where :
            caretWidth | uiScale || expectedThickness
            2          | 1f      || 2
            3          | 1f      || 3
            3          | 2f      || 6
    }

    def 'With `selection(s -> s.showCaret(true).caretShape(UI.CaretShape.#shape).caretWidth(6))`, the caret is as wide as the character behind the gap, so it is wider in front of an "H" than in front of an "i".'(
        UI.CaretShape shape
    ) {
        reportInfo """
            A `BLOCK` caret covers the character behind the gap, like the caret of a terminal, and an `UNDERLINE`
            caret is a line under that character. Both take their width from that character, so they are as wide
            as an "H" in front of an "H" and as narrow as an "i" in front of an "i". In the Ubuntu font at a size
            of 20, an "H" takes about 14 pixels, and an "i" about 5 pixels. `caretWidth(6)` is the thickness
            of an underline, and it does not change the width of either shape.

            The text is "iiHii", with the "H" in blue and every "i" in red. The caret is painted in green behind
            the letters. First, the caret is at position 2, the gap in front of the "H": the green pixels reach
            from the left edge of the "H" to its right edge, and `caretBounds()` is as wide as the green pixels.
            Then your code moves the caret to position 3, the gap in front of the third "i": the green pixels
            get less than half as wide. If SwingTree took the width from `caretWidth(6)` instead, both carets
            would be 6 pixels wide, which is too narrow for the "H", and in front of the "i" the caret would be
            more than half as wide as in front of the "H".

            In this scenario the shape is `${shape}`.
        """
        given : 'A property which holds an empty selection with the caret at position 2, in front of the "H":'
            var selection = Var.of(TextSelection.caretAt(2))
        and : 'A component which paints "ii" in red, "H" in blue and "ii" in red on white, with a green caret of the given shape:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "ii" ),
                                StyledString.of( f -> f.color(BLUE), "H"  ),
                                StyledString.of( f -> f.color(RED),  "ii" )
                            )
                            .selection( s -> s.showCaret(true).caretShape(shape).caretWidth(6).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the blue and the green pixels. `UI.sync()`
            waits until SwingTree has written the caret rectangle into the property:
        """
            var image          = Utility.renderSingleComponent(box)
            UI.sync()
            var letterH        = boundsOfPixelsPaintedIn(image, BLUE)
            var caretInFrontOfH = boundsOfPixelsPaintedIn(image, GREEN)
            var boundsInFrontOfH = selection.get().caretBounds().get()
        and : 'Your code moves the caret to position 3, in front of the third "i", and we render the component again:'
            UI.runNow { selection.set(TextSelection.caretAt(3)) }
            var caretInFrontOfI = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), GREEN)

        then : """
            In front of the "H", the green pixels reach from the left edge to the right edge of the blue "H".
            The edges of the letter and of the caret are smoothed, and the green rectangle starts at the gap rounded
            to a whole pixel, so the two edges may lie one pixel apart on another computer:
        """
            caretInFrontOfH.x <= letterH.x + 1
            caretInFrontOfH.x + caretInFrontOfH.width >= letterH.x + letterH.width - 1
        and : 'They are as wide as `caretBounds()`, give or take one pixel:'
            Math.abs(caretInFrontOfH.width - boundsInFrontOfH.size().width().get()) <= 1
        and : 'In front of the "i", the green pixels are less than half as wide as in front of the "H":'
            caretInFrontOfI.width < caretInFrontOfH.width / 2
        and : 'In front of the "H", the caret is wider than the 6 pixels of `caretWidth(6)`, by more than a pixel:'
            caretInFrontOfH.width > 6 + 1

        where :
            shape << [UI.CaretShape.BLOCK, UI.CaretShape.UNDERLINE]
    }

    def 'With `selection(s -> s.showCaret(true).caretShape(UI.CaretShape.#shape).caretWidth(3))`, a caret behind the last character of the text, where no character follows, is half as wide as the line is tall.'(
        UI.CaretShape shape
    ) {
        reportInfo """
            A `BLOCK` caret covers the character behind the gap, and an `UNDERLINE` caret is a line under that
            character. Behind the last character of the text, no character follows the gap, so there is no
            character to take the width from. There, SwingTree paints both shapes half as wide as the line is
            tall. In the Ubuntu font at a size of 20, a line is about 22.4 pixels tall, so the caret is about
            11.2 pixels wide, which is close to the width of a letter. If SwingTree took the width of a missing
            character as 0, your users would see no caret at all at the end of the text.

            The text is "HHHH HHHH", which has 9 characters, and the caret is at position 9, the gap behind the
            last "H". The caret is painted in green, and `caretWidth(3)` makes an underline 3 pixels thick, which
            does not change the width of either shape.

            In this scenario the shape is `${shape}`.
        """
        given : 'A property which holds an empty selection with the caret at position 9, the end of the text:'
            var selection = Var.of(TextSelection.caretAt(9))
        and : 'A component which paints "HHHH HHHH" in red on white, with a green caret of the given shape:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.showCaret(true).caretShape(shape).caretWidth(3).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red and the green pixels. `UI.sync()`
            waits until SwingTree has written the caret rectangle into the property:
        """
            var image  = Utility.renderSingleComponent(box)
            UI.sync()
            var text   = boundsOfPixelsPaintedIn(image, RED)
            var caret  = boundsOfPixelsPaintedIn(image, GREEN)
            var bounds = selection.get().caretBounds().get()

        then : 'The rectangle of `caretBounds()` is half as wide as it is tall:'
            bounds.size().width().get() == bounds.size().height().get() / 2
        and : 'The green pixels are as wide as that, give or take one pixel:'
            Math.abs(caret.width - bounds.size().width().get()) <= 1
        and : 'The caret lies right of the last red letter, apart from at most one pixel column of smoothing:'
            caret.x >= text.x + text.width - 1

        where :
            shape << [UI.CaretShape.BLOCK, UI.CaretShape.UNDERLINE]
    }

    def 'When the `caretWidth(..)` and the `caretShape(..)` of a style change after the first paint, `caretBounds()` follows them at the next paint.'()
    {
        reportInfo """
            The style of a component is a function which SwingTree runs again whenever it paints the component.
            So a style may read the width and the shape of the caret from properties of your view model, for
            example from a setting which your users change. `caretBounds()` must follow such a change at the next
            paint, because your code places things next to the caret with it.

            SwingTree measures the positions of the gaps between the characters once and reuses the measurement
            until something changes which moves them or changes the caret, so that moving the mouse over a long
            text stays fast. The width and the shape of the caret are part of what it checks. If they were not,
            `caretBounds()` would keep the width of the first paint for as long as the text stays the same.

            The text is "HHHH HHHH", and the caret is at position 2, which is the gap in front of the third "H".
            First the caret is a bar 1 pixel wide. Then your code sets its width to 6, so `caretBounds()` gets
            6 pixels wide. Then your code turns it into a `BLOCK`, which covers the "H" behind the gap: in the Ubuntu
            font at a size of 20, an "H" takes about 14 pixels, so `caretBounds()` gets more than 10 pixels wide.
        """
        given : 'A property which holds an empty selection with the caret at position 2:'
            var selection = Var.of(TextSelection.caretAt(2))
        and : 'Two properties of your view model, holding the width and the shape of the caret:'
            var width = Var.of(1d)
            var shape = Var.of(UI.CaretShape.BAR)
        and : 'A component whose style reads the width and the shape of the caret from these properties:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("HHHH HHHH")
                            .selection( s -> s.showCaret(true).caretWidth(width.get()).caretShape(shape.get()) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : """
            We paint the component. When SwingTree paints the component, it queues a task on the UI thread which
            writes the caret rectangle into the property. `UI.sync()` waits until the UI thread has run every task
            queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()

        expect : 'The caret rectangle is 1 pixel wide:'
            selection.get().caretBounds().get().size().width().get() == 1

        when : 'Your code sets the width of the caret to 6, and we paint the component again:'
            UI.runNow { width.set(6d) }
            Utility.renderSingleComponent(box)
            UI.sync()
        then : 'The caret rectangle is 6 pixels wide:'
            selection.get().caretBounds().get().size().width().get() == 6

        when : 'Your code turns the caret into a block, and we paint the component again:'
            UI.runNow { shape.set(UI.CaretShape.BLOCK) }
            Utility.renderSingleComponent(box)
            UI.sync()
        then : 'The caret rectangle covers the "H" behind the gap, so it is more than 10 pixels wide:'
            selection.get().caretBounds().get().size().width().get() > 10
    }

    def 'A text which never had the keyboard focus paints its selection and its caret in the focused look.'()
    {
        reportInfo """
            From the moment a component loses the keyboard focus until it gets it back, SwingTree greys its
            selection and paints no caret. A component which never had the keyboard focus has not lost it, so it
            looks focused: SwingTree paints the selection in the colour of `color(..)` and the caret of
            `showCaret(true)`. A text which your code shows in a window before your users ever click into it
            therefore shows the colours of its style. If SwingTree greyed every text which does not have the
            keyboard focus, every selectable text of a window would be grey but the one with the focus, and a
            text in a component which can never get the keyboard focus would always be grey.

            The component paints the text "HHHH HHHH" in red. First the first word is selected in yellow. Then
            your code writes a caret at position 4, behind the first word, into the property, and the caret is
            painted in green. The component never gets a focus event.
        """
        given : 'A property which holds a selection of the first word:'
            var selection = Var.of(TextSelection.between(0, 4))
        and : 'A component which paints "HHHH HHHH" in red on white, with a yellow selection and a green caret:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.color(YELLOW).showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : 'We render the component:'
            var withSelection = Utility.renderSingleComponent(box)
        then : 'The text is painted, and the selected word lies on yellow:'
            boundsOfPixelsPaintedIn(withSelection, RED)    != null
            boundsOfPixelsPaintedIn(withSelection, YELLOW) != null

        when : 'Your code writes a caret at position 4 into the property, and we render the component again:'
            UI.runNow { selection.set(TextSelection.caretAt(4)) }
            var withCaret = Utility.renderSingleComponent(box)
        then : 'The green caret is painted, at least as tall as the letters:'
            boundsOfPixelsPaintedIn(withCaret, GREEN).height >= boundsOfPixelsPaintedIn(withCaret, RED).height
    }

    def 'With `withTextSelection("body", selection)`, a selection #fill turns grey from the moment the component loses the keyboard focus until it gets it back, and keeps its brightness.'(
        String fill, Color color, Color seenOnWhite
    ) {
        reportInfo """
            Many programs, like the text fields of web browsers and of macOS, grey a selection when the keyboard
            focus goes elsewhere, so that your users can tell at a glance where the keys they type go to.
            SwingTree does the same for a styled text made
            selectable with `withTextSelection`: from the moment the component loses the keyboard focus until
            it gets it back, the selection colour and the border colour lose most of their saturation, which
            is how far a colour is from the grey of the same brightness.

            The brightness stays the same, and so does the transparency, so the grey selection is as easy to
            see as the coloured one. This scenario measures brightness the way the television standard
            ITU-R BT.601 does, as 0.299 × red + 0.587 × green + 0.114 × blue, and saturation as the largest
            minus the smallest of red, green and blue. An opaque yellow (255, 220, 0) has a saturation of 255,
            and the grey selection may keep at most a third of it. A blue of (0, 0, 220) which is half transparent
            looks like (127, 127, 237) on the white background of the component. If SwingTree made the grey
            colour opaque, the dark grey of that blue would hide the background and be far darker; if it faded
            the colour instead of greying it, the selection would be far brighter and hard to see.

            The selection has a green border 4 pixels wide, which turns grey in the same way. A component which
            never had the keyboard focus looks focused, so the first picture shows the colours of the style.

            A focus event sent with `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it
            when the component is not inside a focused window. A real window only gets the keyboard focus when the
            window manager of the operating system gives it, which takes an unknown amount of time and may not
            happen at all while other windows are in front, so a scenario which waited for it would fail at random. So this scenario hands the focus events to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers them directly, as if Swing had moved the
            keyboard focus away from the component and back, whatever the window manager does.

            In this scenario the selection is ${fill}.
        """
        given : 'A component which paints "HHHH HHHH" in red on white, with the first word selected in the given colour and with a green border:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.color(color).border(4, GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 4)))
                    .get(JBox)
        and : 'Four small measuring tools: one finds a pixel painted in a colour, one reads the colour of a pixel, one measures brightness, one measures saturation:'
            var aPixelPaintedIn = { BufferedImage image, Color wanted ->
                for ( int y = 0; y < image.height; y++ )
                    for ( int x = 0; x < image.width; x++ )
                        if ( boundsOfPixelsPaintedIn(image, wanted, new Rectangle(x, y, 1, 1)) != null )
                            return new Rectangle(x, y, 1, 1)
                return null
            }
            var colourOf = { BufferedImage image, Rectangle pixel -> new Color(image.getRGB((int) pixel.x, (int) pixel.y)) }
            var brightnessOf = { Color c -> 0.299 * c.red + 0.587 * c.green + 0.114 * c.blue }
            var saturationOf = { Color c -> [c.red, c.green, c.blue].max() - [c.red, c.green, c.blue].min() }

        when : 'We render the component and pick one pixel of the selection colour and one pixel of the border:'
            var focusedImage = Utility.renderSingleComponent(box)
            var inside   = aPixelPaintedIn(focusedImage, seenOnWhite)
            var onBorder = aPixelPaintedIn(focusedImage, GREEN)
        then : 'Both colours of the style are painted, so the component looks focused without ever having had the focus:'
            inside   != null
            onBorder != null

        when : 'The component loses the keyboard focus, and we render it again:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
            var unfocusedImage = Utility.renderSingleComponent(box)
        and : 'We read the two pixels in both pictures:'
            var focusedInside     = colourOf(focusedImage,   inside)
            var focusedOnBorder   = colourOf(focusedImage,   onBorder)
            var unfocusedInside   = colourOf(unfocusedImage, inside)
            var unfocusedOnBorder = colourOf(unfocusedImage, onBorder)

        then : 'The two pixels are no longer painted in the colours of the style:'
            boundsOfPixelsPaintedIn(unfocusedImage, seenOnWhite, inside) == null
            boundsOfPixelsPaintedIn(unfocusedImage, GREEN, onBorder) == null
        and : 'Without the focus, the two pixels keep their brightness, give or take 8 out of 255:'
            Math.abs(brightnessOf(unfocusedInside)   - brightnessOf(focusedInside))   <= 8
            Math.abs(brightnessOf(unfocusedOnBorder) - brightnessOf(focusedOnBorder)) <= 8
        and : 'They keep at most a third of their saturation:'
            saturationOf(unfocusedInside)   <= saturationOf(focusedInside)   / 3
            saturationOf(unfocusedOnBorder) <= saturationOf(focusedOnBorder) / 3

        when : 'The component gets the keyboard focus back, and we render it again:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_GAINED))
            }
            var refocusedImage = Utility.renderSingleComponent(box)

        then : 'The two pixels are painted in the colours of the style again:'
            boundsOfPixelsPaintedIn(refocusedImage, seenOnWhite, inside) != null
            boundsOfPixelsPaintedIn(refocusedImage, GREEN, onBorder) != null

        where :
            fill                                       | color                       | seenOnWhite
            "in an opaque yellow"                      | YELLOW                      | YELLOW
            "in a blue which is half transparent"      | new Color(0, 0, 220, 128)   | new Color(127, 127, 237)
    }

    def 'With `selection(s -> s.unfocusedColor(..).unfocusedBorderColor(..))`, the selection is painted in these two colours from the moment the component loses the keyboard focus until it gets it back.'()
    {
        reportInfo """
            By default, SwingTree greys the selection from the moment the component loses the keyboard focus
            until it gets it back.
            With `unfocusedColor(..)` and `unfocusedBorderColor(..)` you choose the two colours yourself,
            for example the colours which the text fields of your application use for an unfocused selection.

            The component paints the text "HHHH HHHH" in red with the first word selected. While the component
            has the keyboard focus, the selection is yellow with a green border 4 pixels wide. After it lost the
            keyboard focus, the selection is blue with a purple border. The colours of one state never show in the other
            state: if SwingTree painted the unfocused fill but kept the focused border, or the other way around,
            green or yellow pixels would remain next to the blue or purple ones.

            A focus event sent with `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it
            when the component is not inside a focused window. A real window only gets the keyboard focus when the
            window manager of the operating system gives it, which takes an unknown amount of time and may not
            happen at all while other windows are in front, so a scenario which waited for it would fail at random. So this scenario hands the focus events to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A purple for the border of the unfocused selection:'
            var purple = new Color(150, 0, 150)
        and : 'A component which paints "HHHH HHHH" in red on white, with the first word selected:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s
                                .color(YELLOW).border(4, GREEN)
                                .unfocusedColor(BLUE).unfocusedBorderColor(purple)
                            )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 4)))
                    .get(JBox)

        when : 'The component loses the keyboard focus, and we render it:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
            var unfocused = Utility.renderSingleComponent(box)

        then : 'The text is painted:'
            boundsOfPixelsPaintedIn(unfocused, RED) != null
        and : 'The selection is blue inside a purple border:'
            var fillWithoutFocus   = boundsOfPixelsPaintedIn(unfocused, BLUE)
            var borderWithoutFocus = boundsOfPixelsPaintedIn(unfocused, purple)
            fillWithoutFocus.x > borderWithoutFocus.x
            fillWithoutFocus.y > borderWithoutFocus.y
            fillWithoutFocus.x + fillWithoutFocus.width  < borderWithoutFocus.x + borderWithoutFocus.width
            fillWithoutFocus.y + fillWithoutFocus.height < borderWithoutFocus.y + borderWithoutFocus.height
        and : 'No pixel is yellow or green:'
            boundsOfPixelsPaintedIn(unfocused, YELLOW) == null
            boundsOfPixelsPaintedIn(unfocused, GREEN)  == null

        when : 'The component gets the keyboard focus back, and we render it again:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_GAINED))
            }
            var focused = Utility.renderSingleComponent(box)

        then : 'The selection is yellow inside a green border, where the blue and the purple pixels were:'
            boundsOfPixelsPaintedIn(focused, YELLOW) == fillWithoutFocus
            boundsOfPixelsPaintedIn(focused, GREEN)  == borderWithoutFocus
        and : 'No pixel is blue or purple:'
            boundsOfPixelsPaintedIn(focused, BLUE)   == null
            boundsOfPixelsPaintedIn(focused, purple) == null
    }

    def 'With `selection(s -> s.borderWidth(4).unfocusedBorderColor(..))` and no `borderColor(..)`, the selection has no border, neither with nor without the keyboard focus.'()
    {
        reportInfo """
            SwingTree paints the border of a selection only when the border has a width and a colour. Here the
            style gives the border a width of 4 pixels with `borderWidth(4)`, but no colour, so the selection has
            no border while the component has the keyboard focus. `unfocusedBorderColor(..)` only replaces the
            colour of a border which is painted while the component has the focus, so it does not add a border
            after the component lost the focus either. If it did, the selection of your users would
            grow a border the moment they click into another component.

            The component paints the text "HHHH HHHH" in red with the first word selected, in yellow while it
            has the keyboard focus and in blue while it does not. The border colour for an unfocused selection
            is purple, and no purple pixel may appear.

            A focus event sent with `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it
            when the component is not inside a focused window. A real window only gets the keyboard focus when the
            window manager of the operating system gives it, which takes an unknown amount of time and may not
            happen at all while other windows are in front, so a scenario which waited for it would fail at random. So this scenario hands the focus event to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers it directly.
        """
        given : 'A purple for the border of the unfocused selection:'
            var purple = new Color(150, 0, 150)
        and : 'A component which paints "HHHH HHHH" in red on white, with the first word selected, and a border width but no border colour:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s
                                .color(YELLOW).borderWidth(4)
                                .unfocusedColor(BLUE).unfocusedBorderColor(purple)
                            )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 4)))
                    .get(JBox)
        and : 'We render the component, which never had the keyboard focus and so looks focused, and find the yellow pixels:'
            var focused = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), YELLOW)

        when : 'The component loses the keyboard focus, and we render it again:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
            var unfocused = Utility.renderSingleComponent(box)

        then : 'The selection was painted in yellow while the component looked focused:'
            focused != null
        and : 'Without the focus, the blue pixels cover the same rectangle as the yellow pixels did, because no border takes room from them:'
            boundsOfPixelsPaintedIn(unfocused, BLUE) == focused
        and : 'No pixel is purple:'
            boundsOfPixelsPaintedIn(unfocused, purple) == null
    }

    def 'With nothing but `selection(s -> s.useUnfocusedLook(false))`, the default selection colour stays the same when the component loses the keyboard focus.'()
    {
        reportInfo """
            Without `color(..)`, SwingTree paints a partly transparent light blue behind the selected characters,
            and by default it greys that blue from the moment the component loses the keyboard focus until it gets it back.
            `useUnfocusedLook(false)` switches the grey off, and it does so on its own too, in a style which says
            nothing else about the selection. If SwingTree treated such a style like a style without any
            `selection(..)`, the selection would still turn grey.

            The component paints the text "HHHH HHHH" in red on white, with the first word selected. This scenario
            renders it once with the keyboard focus and once without, and compares the two pictures pixel by pixel.
            They must be the same. To be sure that the pictures show a selection at all, it also renders the
            component with no selected character, and that picture must differ.

            A focus event sent with `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it
            when the component is not inside a focused window. A real window only gets the keyboard focus when the
            window manager of the operating system gives it, which takes an unknown amount of time and may not
            happen at all while other windows are in front, so a scenario which waited for it would fail at random. So this scenario hands the focus events to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a selection of the first word:'
            var selection = Var.of(TextSelection.between(0, 4))
        and : 'A component which paints "HHHH HHHH" in red on white, whose selection style only switches the unfocused look off:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.useUnfocusedLook(false) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which tells whether two pictures have the same colour in every pixel:'
            var samePixels = { BufferedImage a, BufferedImage b ->
                for ( int y = 0; y < a.height; y++ )
                    for ( int x = 0; x < a.width; x++ )
                        if ( a.getRGB(x, y) != b.getRGB(x, y) )
                            return false
                return true
            }

        when : 'We render the component with the keyboard focus, which it gets with a focus event:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_GAINED))
            }
            var focused = Utility.renderSingleComponent(box)
        and : 'We render it again after it lost the keyboard focus:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
            var unfocused = Utility.renderSingleComponent(box)
        and : 'Your code writes a caret without selected characters into the property, and we render the component once more:'
            UI.runNow { selection.set(TextSelection.caretAt(0)) }
            var nothingSelected = Utility.renderSingleComponent(box)

        then : 'The text is painted:'
            boundsOfPixelsPaintedIn(focused, RED) != null
        and : 'The selection is painted, because the picture without a selection differs:'
            !samePixels(focused, nothingSelected)
        and : 'The pictures with and without the keyboard focus are the same in every pixel:'
            samePixels(focused, unfocused)
    }

    def 'With `selection(s -> s.showCaret(true))`, SwingTree paints no caret from the moment the component loses the keyboard focus until it gets it back.'()
    {
        reportInfo """
            The caret marks the gap where the keys which your users type would act. While the component does not
            have the keyboard focus, these keys go to another component, and a caret would point your users to the
            wrong place. So SwingTree paints no caret from the moment the component loses the keyboard focus until
            it gets it back. A component which never had the keyboard focus looks focused, so its caret is painted.

            The text is "HHHH HHHH", and the caret is at position 4, behind the first word. It is painted in green.

            A focus event sent with `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it
            when the component is not inside a focused window. A real window only gets the keyboard focus when the
            window manager of the operating system gives it, which takes an unknown amount of time and may not
            happen at all while other windows are in front, so a scenario which waited for it would fail at random. So this scenario hands the focus events to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A component which paints "HHHH HHHH" in red on white, with a green caret at position 4:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s.showCaret(true).caretColor(GREEN) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.caretAt(4)))
                    .get(JBox)

        when : 'We render the component, which never had the keyboard focus:'
            var neverFocused = Utility.renderSingleComponent(box)
        then : 'The caret is painted:'
            var caret = boundsOfPixelsPaintedIn(neverFocused, GREEN)
            caret != null

        when : 'The component loses the keyboard focus, and we render it again:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
            var unfocused = Utility.renderSingleComponent(box)
        then : 'The text is painted, and the caret is not:'
            boundsOfPixelsPaintedIn(unfocused, RED)   != null
            boundsOfPixelsPaintedIn(unfocused, GREEN) == null

        when : 'The component gets the keyboard focus back, and we render it again:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_GAINED))
            }
            var refocused = Utility.renderSingleComponent(box)
        then : 'The caret is painted where it was before:'
            boundsOfPixelsPaintedIn(refocused, GREEN) == caret
    }

    def 'With `withTextSelection("body", selection)`, SwingTree asks Swing to paint the component again when it loses the keyboard focus and when it gets the focus back.'()
    {
        reportInfo """
            From the moment the component loses the keyboard focus until it gets it back, SwingTree paints its selection in grey and
            paints no caret. Swing does not paint a component again by itself when the keyboard focus moves, so
            SwingTree asks for it: it calls `repaint()` on the component when it loses the focus and when it gets
            the focus back. If it forgot that, your users would see the old colours until something else
            happened to make Swing paint the component again, for example until the mouse moved over it.

            Swing collects the components which ask to be painted again in its `RepaintManager`. This scenario
            replaces the `RepaintManager` with one which writes down every component which asks, and checks
            that the component is written down after each of the two focus events.

            A focus event sent with `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it
            when the component is not inside a focused window. A real window only gets the keyboard focus when the
            window manager of the operating system gives it, which takes an unknown amount of time and may not
            happen at all while other windows are in front, so a scenario which waited for it would fail at random. So this scenario hands the focus events to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A component which paints "HHHH HHHH" with its first word selected:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("HHHH HHHH")
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(0, 4)))
                    .get(JBox)
        and : 'A `RepaintManager` which writes down every component which asks to be painted again:'
            var askedToBePainted = new CopyOnWriteArrayList<JComponent>()
            var formerManager = RepaintManager.currentManager(box)
            RepaintManager.setCurrentManager(new RepaintManager() {
                @Override
                void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
                    askedToBePainted.add(component)
                    super.addDirtyRegion(component, x, y, w, h)
                }
            })

        when : 'The component loses the keyboard focus:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
        then : 'It asked to be painted again:'
            askedToBePainted.contains(box)

        when : 'We forget what was written down, and the component gets the keyboard focus back:'
            askedToBePainted.clear()
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_GAINED))
            }
        then : 'It asked to be painted again:'
            askedToBePainted.contains(box)

        cleanup : 'We put the former `RepaintManager` back:'
            RepaintManager.setCurrentManager(formerManager)
    }

    def 'With `selection(s -> s.useUnfocusedLook(false))`, the selection colour, its border and the caret look the same whether the component has the keyboard focus or not.'()
    {
        reportInfo """
            By default, SwingTree greys the selection and hides the caret while the component does not have the
            keyboard focus. With `useUnfocusedLook(false)`, the keyboard focus changes nothing that your users
            can see. This suits an application whose look should not change with the focus, and a test which
            compares pictures of the component taken while the keyboard focus is somewhere else.

            The component paints the text "HHHH HHHH" in red. First, the first word is selected, in yellow with
            a green border 4 pixels wide. Then your code writes a caret at position 4, behind the first word, into
            the property, and the caret is painted in blue. Both are painted after the component lost the keyboard
            focus exactly where they are painted while it has the focus. If SwingTree ignored `useUnfocusedLook(false)`,
            the yellow and the green would turn grey and the blue caret would disappear.

            A focus event sent with `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it
            when the component is not inside a focused window. A real window only gets the keyboard focus when the
            window manager of the operating system gives it, which takes an unknown amount of time and may not
            happen at all while other windows are in front, so a scenario which waited for it would fail at random. So this scenario hands the focus events to the component with
            `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a selection of the first word:'
            var selection = Var.of(TextSelection.between(0, 4))
        and : 'A component which paints "HHHH HHHH" in red on white, with a yellow selection, a green border and a blue caret:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH HHHH" ) )
                            .selection( s -> s
                                .color(YELLOW).border(4, GREEN)
                                .showCaret(true).caretColor(BLUE)
                                .useUnfocusedLook(false)
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component while it has the keyboard focus, which it gets with a focus event, and find the yellow and the green pixels:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_GAINED))
            }
            var focused = Utility.renderSingleComponent(box)
            var fill    = boundsOfPixelsPaintedIn(focused, YELLOW)
            var border  = boundsOfPixelsPaintedIn(focused, GREEN)

        when : 'The component loses the keyboard focus, and we render it again:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
            var unfocused = Utility.renderSingleComponent(box)

        then : 'The selection was painted while the component had the focus:'
            fill   != null
            border != null
        and : 'Without the focus, the yellow and the green pixels are where they were:'
            boundsOfPixelsPaintedIn(unfocused, YELLOW) == fill
            boundsOfPixelsPaintedIn(unfocused, GREEN)  == border
            numberOfPixelsPaintedIn(unfocused, YELLOW) == numberOfPixelsPaintedIn(focused, YELLOW)

        when : 'Your code writes a caret at position 4 into the property, and we render the component again, still without the focus:'
            UI.runNow { selection.set(TextSelection.caretAt(4)) }
            var caretWithoutFocus = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)

        then : 'The caret is painted, at least as tall as the yellow selection was without its border:'
            caretWithoutFocus != null
            caretWithoutFocus.height >= fill.height
    }

    def 'A `TextSelection` tells with `isCaretAtLineEnd()` on which of two lines its caret lies; `caretAt(..)`, `between(..)` and `withCaret(..)` choose it by whether the caret ends the selected characters, and `withCaretAtLineEnd(..)` sets it.'()
    {
        reportInfo """
            Where SwingTree wraps a long line, one position is both the end of the upper line and the start of
            the next line. A caret at such a position could be painted in two places. `isCaretAtLineEnd()` says
            which one: `true` means at the end of the upper line, next to the character in front of the caret,
            and `false` means at the start of the next line, in front of the character behind the caret.

            When your code does not choose, SwingTree decides by the selected characters. `caretAt(5)` selects
            nothing, so its caret belongs in front of the character behind it: the flag is `false`.
            `between(2, 5)` selects up to position 5, so its caret is the end of the selected characters and
            belongs next to the last of them: the flag is `true`. `between(5, 2)` has its caret at the start of
            the selected characters, so the flag is `false`. `withCaret(..)` uses the same rule for the selection
            which it returns. `withCaretAtLineEnd(..)` sets the flag and keeps everything else.

            The flag is part of the value: two selections with the same positions but different flags are not
            equal, because your users see their carets in different places. `toString()` shows it.
        """
        expect : 'A caret made by `caretAt(..)` lies at the start of the next line:'
            !TextSelection.caretAt(5).isCaretAtLineEnd()
        and : 'A caret which ends the selected characters lies at the end of the upper line, every other caret at the start of the next line:'
            TextSelection.between(2, 5).isCaretAtLineEnd()
            !TextSelection.between(5, 2).isCaretAtLineEnd()
            !TextSelection.between(5, 5).isCaretAtLineEnd()
        and : '`withCaret(..)` applies the same rule to the selection which it returns:'
            TextSelection.caretAt(2).withCaret(5).isCaretAtLineEnd()
            !TextSelection.caretAt(5).withCaret(2).isCaretAtLineEnd()
            !TextSelection.between(2, 5).withCaret(2).isCaretAtLineEnd()

        when : 'We set the flag of a caret at position 5 with `withCaretAtLineEnd(true)`:'
            var atLineEnd = TextSelection.caretAt(5).withCaretAtLineEnd(true)
        then : 'Only the flag changed:'
            atLineEnd.isCaretAtLineEnd()
            atLineEnd.anchor() == 5
            atLineEnd.caret()  == 5
            atLineEnd.isEnabled()
        and : 'The flag survives `withEnabled(..)`:'
            atLineEnd.withEnabled(false).isCaretAtLineEnd()
        and : 'A selection with the flag is not equal to the same selection without it, and equal to another one with it:'
            atLineEnd != TextSelection.caretAt(5)
            atLineEnd == TextSelection.caretAt(5).withCaretAtLineEnd(true)
            atLineEnd.hashCode() == TextSelection.caretAt(5).withCaretAtLineEnd(true).hashCode()
            atLineEnd.withCaretAtLineEnd(false) == TextSelection.caretAt(5)
        and : '`toString()` shows the flag:'
            atLineEnd.toString().contains("caretAtLineEnd=true")
            TextSelection.caretAt(5).toString().contains("caretAtLineEnd=false")
    }

    def 'With `withCaretAtLineEnd(#atLineEnd)`, a caret without selected characters at the place where a line wraps is painted #where, where `caretBounds()` says it is.'(
        boolean atLineEnd, String where
    ) {
        reportInfo """
            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". At a font size
            of 20, one word of four "H" letters is about 56 pixels wide and a space about 5 pixels, so the two
            words need about 117 pixels. With `wrapLines(true)` the first word and the space behind it go onto
            the first line, and the second word onto the second line. Position 5 is the gap behind the space,
            where the first line ends, and also the gap in front of the first "H" of the second line.

            `isCaretAtLineEnd()` chooses between these two places. With `true`, SwingTree paints the caret at
            the end of the first line, right of the red letters. With `false`, it paints the caret at the start
            of the second line, in front of the green letters. `caretBounds()` returns the same place, so that
            your code which paints next to the caret and the caret which your users see agree.

            In this scenario the flag is `${atLineEnd}`, so the caret is painted ${where}.
        """
        given : 'A property which holds a caret at position 5 with the given flag:'
            var selection = Var.of(TextSelection.caretAt(5).withCaretAtLineEnd(atLineEnd))
        and : 'A component whose content area is 100 pixels wide, with the first word in red, the second in green, and a blue caret:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                            .selection( s -> s.showCaret(true).caretColor(BLUE) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the rectangles around the red, the green and the blue pixels.
            `UI.sync()` waits until SwingTree has written the caret rectangle into the property:
        """
            var image      = Utility.renderSingleComponent(box)
            UI.sync()
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)
            var caret      = boundsOfPixelsPaintedIn(image, BLUE)
            var bounds     = selection.get().caretBounds().get()

        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The caret is painted next to the red letters on the first line, or in front of the green letters on the second line:'
            atLineEnd ? caret.x >= firstWord.x + firstWord.width - 1 && caret.y < firstWord.y + firstWord.height
                      : caret.x <= secondWord.x && caret.y >= firstWord.y + firstWord.height - 1
        and : 'The painted caret lies where `caretBounds()` says, give or take one pixel:'
            Math.abs(caret.x - bounds.location().x()) <= 1
            Math.abs(caret.y - bounds.location().y()) <= 1
        and : 'The property kept the flag:'
            selection.get().isCaretAtLineEnd() == atLineEnd

        where :
            atLineEnd | where
            true      | "at the end of the first line"
            false     | "at the start of the second line"
    }

    def 'With `withCaretAtLineEnd(#atLineEnd)` and `obstacles(..)`, a caret at the position where the text jumps over an obstacle lies #where.'(
        boolean atLineEnd, String where
    ) {
        reportInfo """
            The content area of the component reaches from x = 10 to x = 390, and an obstacle covers the first
            line from x = 150 to x = 250. The text is "HHHHH HHHHHHH HHHHHHH". At a font size of 20, an "H" is
            about 14 pixels wide, so the first word, 70 pixels, fits left of the obstacle, and the second word,
            98 pixels, goes right of it. Position 6 is the gap behind the space after the first word. It ends the
            part of the line left of the obstacle and starts the part right of it, so it has two places, like the
            place where a line wraps.

            `isCaretAtLineEnd()` chooses between them: `true` puts the caret right of the red first word, left of
            the obstacle, and `false` puts it in front of the blue second word, right of the obstacle.

            In this scenario the flag is `${atLineEnd}`, so the caret lies ${where}.
        """
        given : 'A property which holds a caret at position 6 with the given flag:'
            var selection = Var.of(TextSelection.caretAt(6).withCaretAtLineEnd(atLineEnd))
        and : 'A component which paints the first word in red and the rest in blue, around an obstacle on the first line:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(new Rectangle(150, 0, 100, 30))
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "HHHHH " ),
                                StyledString.of( f -> f.color(BLUE), "HHHHHHH HHHHHHH" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : 'We render the component, find the red pixels, and let SwingTree write the caret rectangle into the property:'
            var image     = Utility.renderSingleComponent(box)
            UI.sync()
            var firstWord = boundsOfPixelsPaintedIn(image, RED)
            var bounds    = selection.get().caretBounds().get()

        then : 'The caret lies on the first line, from its top to its bottom:'
            bounds.location().y() <= firstWord.y
            bounds.location().y() + bounds.size().height().get() >= firstWord.y + firstWord.height
        and : 'It lies right of the red word and left of the obstacle, or right of the obstacle:'
            atLineEnd ? bounds.location().x() >= firstWord.x + firstWord.width - 1 && bounds.location().x() < 150
                      : bounds.location().x() >= 250

        where :
            atLineEnd | where
            true      | "right of the first word, left of the obstacle"
            false     | "in front of the second word, right of the obstacle"
    }

    def 'With `withTextSelection("body", selection)`, a click right of the end of a wrapped line puts the caret at the end of that line, and a click in front of the first letter of the next line puts it at the start of that line.'()
    {
        reportInfo """
            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". With `wrapLines(true)`
            the first word and the space behind it go onto the first line, and the second word onto the second line.
            Position 5 is the gap behind the space, where the first line ends, and also the gap in front of the first
            "H" of the second line.

            When your user clicks right of the end of the first line, the nearest gap of the first line is position 5.
            Your user clicked on the first line, so SwingTree puts the caret at the end of the first line and sets
            `isCaretAtLineEnd()` to `true`. If it put the caret at the start of the second line, your user would
            see it jump to a line which they did not click on. When your user clicks on the left edge of the first
            "H" of the second line, the nearest gap is position 5 as well, and SwingTree puts the caret at the start
            of the second line, with the flag `false`.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component whose content area is 100 pixels wide, with the first word in red, the second in green, and a blue caret:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                            .selection( s -> s.showCaret(true).caretColor(BLUE) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the rectangles around the red and the green letters:'
            var image      = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)

        when : 'We click at x = 105, right of the end of the first line, in the middle of the first line:'
            int firstLineMiddle = firstWord.centerY
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, 105, firstLineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, 105, firstLineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We render the component again:'
            var caretAtLineEnd = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The caret is at position 5, at the end of the first line:'
            selection.get().caret() == 5
            selection.get().isCaretAtLineEnd()
        and : 'It is painted right of the red letters, on the first line:'
            caretAtLineEnd.x >= firstWord.x + firstWord.width - 1
            caretAtLineEnd.y < firstWord.y + firstWord.height

        when : 'We click on the left edge of the first green letter:'
            int leftEdgeOfSecondWord = secondWord.x
            int secondLineMiddle     = secondWord.centerY
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfSecondWord, secondLineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, leftEdgeOfSecondWord, secondLineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We render the component again:'
            var caretAtLineStart = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
        then : 'The caret is at position 5 again, now at the start of the second line:'
            selection.get().caret() == 5
            !selection.get().isCaretAtLineEnd()
        and : 'It is painted in front of the green letters, on the second line:'
            caretAtLineStart.x <= secondWord.x
            caretAtLineStart.y >= firstWord.y + firstWord.height - 1
    }

    def 'With `withTextSelection("body", selection)`, the left and the right arrow key move the caret by one character, and put it at the start or the end of selected characters instead of moving it.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters. A selection holds two positions,
            and a position is a gap between two characters: position 0 is the gap in front of the "H", and
            position 11 is the gap behind the "d".

            The right arrow key moves the caret one position forwards and the left arrow key one position
            backwards, and neither selects anything. At the start and at the end of the text the caret stays
            where it is, so a position never leaves the text. When characters are selected, a press without the
            shift key ends the selection, like in a text editor: the left arrow key puts the caret at the start
            of the selected characters, and the right arrow key at their end. Your users expect this, because it
            lets them continue typing on the side of the selection where they look.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a selection from position 3 to position 8, which is "lo wo":'
            var selection = Var.of(TextSelection.between(3, 8))
        and : 'A component which paints "Hello world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press the right arrow key:'
            press(KeyEvent.VK_RIGHT, 0)
        then : 'The selection ends, with the caret at the end of the selected characters:'
            selection.get().anchor() == 8
            selection.get().caret()  == 8
        when : 'We press the left arrow key:'
            press(KeyEvent.VK_LEFT, 0)
        then : 'The caret moved one position backwards:'
            selection.get().anchor() == 7
            selection.get().caret()  == 7

        when : 'Your code selects "lo wo" again, and we press the left arrow key:'
            UI.runNow { selection.set(TextSelection.between(3, 8)) }
            press(KeyEvent.VK_LEFT, 0)
        then : 'The selection ends, with the caret at the start of the selected characters:'
            selection.get().anchor() == 3
            selection.get().caret()  == 3
        when : 'We press the right arrow key:'
            press(KeyEvent.VK_RIGHT, 0)
        then : 'The caret moved one position forwards:'
            selection.get().anchor() == 4
            selection.get().caret()  == 4

        when : 'Your code puts the caret at the start of the text, and we press the left arrow key:'
            UI.runNow { selection.set(TextSelection.caretAt(0)) }
            press(KeyEvent.VK_LEFT, 0)
        then : 'The caret stays at the start:'
            selection.get().caret() == 0
        when : 'Your code puts the caret at the end of the text, and we press the right arrow key:'
            UI.runNow { selection.set(TextSelection.caretAt(11)) }
            press(KeyEvent.VK_RIGHT, 0)
        then : 'The caret stays at the end:'
            selection.get().caret() == 11
    }

    def 'With `withTextSelection("body", selection)`, the arrow keys, Home and End with the shift key held down keep the anchor and move only the caret, so they select the characters which the caret passes over.'()
    {
        reportInfo """
            The component paints the text "Hello world", which has 11 characters. A selection holds two positions:
            the *anchor*, which stays where it is, and the *caret*, which moves. Position 6 is the gap in front of
            the "w".

            With the shift key held down, every key which moves the caret keeps the anchor where it is, so the
            characters between the anchor and the caret are selected, like in a text editor. Starting at position 6,
            two presses of Shift+Right select "wo", three presses of Shift+Left then move the caret back over the
            "o", the "w" and the space, so the selection reaches from 6 back to 5 and holds only the space.
            Shift+End and Shift+Home then move the caret to the end and to the start of the line, and the anchor
            stays at position 6 throughout.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a caret at position 6, in front of the "w":'
            var selection = Var.of(TextSelection.caretAt(6))
        and : 'A component which paints "Hello world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press Shift+Right twice:'
            2.times { press(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK) }
        then : '"wo" is selected:'
            selection.get().anchor() == 6
            selection.get().caret()  == 8
            selection.get().selectedText() == "wo"

        when : 'We press Shift+Left three times:'
            3.times { press(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK) }
        then : 'The caret moved back behind the anchor, so the space is selected:'
            selection.get().anchor() == 6
            selection.get().caret()  == 5
            selection.get().selectedText() == " "

        when : 'We press Shift+End:'
            press(KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK)
        then : '"world" is selected:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11
            selection.get().selectedText() == "world"

        when : 'We press Shift+Home:'
            press(KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK)
        then : '"Hello " is selected:'
            selection.get().anchor() == 6
            selection.get().caret()  == 0
            selection.get().selectedText() == "Hello "
    }

    def 'With `withTextSelection("body", selection)`, the left and the right arrow key with Ctrl held down, or Alt on macOS, move the caret to the start of the word in front of it or to the end of the word behind it.'()
    {
        reportInfo """
            The component paints the text "Hello brave world", which has 17 characters. Position 6 is the gap in
            front of "brave", position 11 the gap behind it, position 12 the gap in front of "world", and
            position 17 the end of the text.

            With Ctrl held down, the right arrow key moves the caret over the next word to its end, skipping the
            spaces in front of it, and the left arrow key moves it to the start of the word in front of it. A word
            is a run of letters and digits. On macOS, Ctrl together with the arrow keys switches between desktops,
            and text fields jump by words with the Alt key, which macOS calls Option, instead. SwingTree uses
            Alt wherever the keyboard shortcuts of the operating system use the Command key instead of Ctrl, which
            `Toolkit.getMenuShortcutKeyMask()` tells. This scenario asks it in the same way.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a caret at position 6, in front of "brave":'
            var selection = Var.of(TextSelection.caretAt(6))
        and : 'A component which paints "Hello brave world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello brave world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'The modifier key which jumps by words: Alt where the shortcut key is the Command key, and Ctrl everywhere else:'
            int wordKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                            ? InputEvent.ALT_DOWN_MASK
                            : InputEvent.CTRL_DOWN_MASK
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press the right arrow key with the word key held down:'
            press(KeyEvent.VK_RIGHT, wordKey)
        then : 'The caret is at the end of "brave":'
            selection.get().caret() == 11
            selection.get().isEmpty()
        when : 'We press it again:'
            press(KeyEvent.VK_RIGHT, wordKey)
        then : 'The caret skipped the space and is at the end of "world", which is the end of the text:'
            selection.get().caret() == 17
        when : 'We press the left arrow key with the word key held down:'
            press(KeyEvent.VK_LEFT, wordKey)
        then : 'The caret is at the start of "world":'
            selection.get().caret() == 12
        when : 'We press it twice more:'
            2.times { press(KeyEvent.VK_LEFT, wordKey) }
        then : 'The caret skipped "brave" and is at the start of "Hello", which is the start of the text:'
            selection.get().caret() == 0
        when : 'We press the right arrow key with the word key and the shift key held down:'
            press(KeyEvent.VK_RIGHT, wordKey | InputEvent.SHIFT_DOWN_MASK)
        then : 'The word "Hello" is selected:'
            selection.get().anchor() == 0
            selection.get().caret()  == 5
    }

    def 'With `withTextSelection("body", selection)`, Home and End move the caret to the start and the end of the painted line, also where a line wraps, and with Ctrl held down, or Alt on macOS, to the start and the end of the text.'()
    {
        reportInfo """
            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". At a font size
            of 20, one word of four "H" letters is about 56 pixels wide and a space about 5 pixels, so with
            `wrapLines(true)` the first word and the space behind it go onto the first line, and the second word
            onto the second line. Position 5 is the gap behind the space, where the first line ends, and also the
            gap in front of the first "H" of the second line. Position 9 is the end of the text.

            Home and End move the caret to the start and to the end of the line which your users see it on.
            End on the first line moves the caret to position 5, and SwingTree paints it at the end of the first
            line, because that is the line your users pressed End on. If it painted the caret at the start of the
            second line instead, End would look as if it jumped to the next line, and a second press of End would
            then go to the end of the second line. Home on the second line moves the caret to position 5 as well,
            and there SwingTree paints it at the start of the second line. The flag `isCaretAtLineEnd()` of the
            selection tells which of the two places the caret has.

            With Ctrl held down, Home and End move the caret to the start and the end of the whole text. SwingTree
            uses Alt instead of Ctrl wherever the keyboard shortcuts of the operating system use the Command key,
            which `Toolkit.getMenuShortcutKeyMask()` tells, and this scenario asks it in the same way.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a caret at position 2, on the first line:'
            var selection = Var.of(TextSelection.caretAt(2))
        and : 'A component whose content area is 100 pixels wide, with the first word in red and the second in green:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'The modifier key which jumps to the start or the end of the text: Alt where the shortcut key is the Command key, and Ctrl everywhere else:'
            int farKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                            ? InputEvent.ALT_DOWN_MASK
                            : InputEvent.CTRL_DOWN_MASK
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component and find the red and the green letters:'
            var image      = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)

        when : 'We press End:'
            press(KeyEvent.VK_END, 0)
        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The caret is at position 5, at the end of the first line, right of the red letters:'
            selection.get().caret() == 5
            selection.get().isCaretAtLineEnd()
            selection.get().caretBounds().get().location().x() >= firstWord.x + firstWord.width - 1
            selection.get().caretBounds().get().location().y() < firstWord.y + firstWord.height
        when : 'We press End again:'
            press(KeyEvent.VK_END, 0)
        then : 'The caret stays at the end of the first line:'
            selection.get().caret() == 5
            selection.get().isCaretAtLineEnd()
        when : 'We press Home:'
            press(KeyEvent.VK_HOME, 0)
        then : 'The caret is at the start of the first line:'
            selection.get().caret() == 0

        when : 'Your code puts the caret at position 7, on the second line, and we press Home:'
            UI.runNow { selection.set(TextSelection.caretAt(7)) }
            press(KeyEvent.VK_HOME, 0)
        then : 'The caret is at position 5, at the start of the second line, in front of the green letters:'
            selection.get().caret() == 5
            !selection.get().isCaretAtLineEnd()
            selection.get().caretBounds().get().location().x() <= secondWord.x
            selection.get().caretBounds().get().location().y() >= firstWord.y + firstWord.height - 1
        when : 'We press End:'
            press(KeyEvent.VK_END, 0)
        then : 'The caret is at the end of the second line, which is the end of the text:'
            selection.get().caret() == 9

        when : 'We press Home with the far key held down:'
            press(KeyEvent.VK_HOME, farKey)
        then : 'The caret is at the start of the text:'
            selection.get().caret() == 0
        when : 'We press End with the far key held down:'
            press(KeyEvent.VK_END, farKey)
        then : 'The caret is at the end of the text:'
            selection.get().caret() == 9
    }

    def 'With `withTextSelection("body", selection)`, the up and the down arrow key move the caret to the gap nearest to it on the line above or below, also across the place where a line wraps.'()
    {
        reportInfo """
            The content area of the component is 100 pixels wide, and the text is "HHHH HHHHHH". At a font size
            of 20, an "H" is about 14 pixels wide and a space about 5 pixels, so with `wrapLines(true)` the first
            word and the space go onto the first line, and the six letters of the second word, about 84 pixels,
            onto the second line. Position 5 ends the first line and starts the second, and position 11 is the end
            of the text.

            The up and the down arrow key move the caret to the line above or below, to the gap which lies nearest
            to the caret horizontally. Position 7 lies two letters into the second line, at the same distance
            from the left edge as position 2 on the first line, so the up arrow key moves the caret from 7 to 2,
            and the down arrow key back to 7. On the last line there is no line below, so the down arrow key moves
            the caret to the end of the text, and on the first line the up arrow key moves it to the start of the
            text, like in a text editor.

            The end of the text, position 11, lies about 94 pixels right of the left edge, and the first line
            ends at about 75 pixels. So from there the up arrow key moves the caret to position 5, at the end of
            the first line, and SwingTree paints the caret at the end of the first line, not at the start of the
            second line, which would look as if the caret had not moved up at all.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a caret at position 7, two letters into the second line:'
            var selection = Var.of(TextSelection.caretAt(7))
        and : 'A component whose content area is 100 pixels wide, with the first word in red and the second in green:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHHHH" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component and find the red and the green letters:'
            var image      = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)

        when : 'We press the up arrow key:'
            press(KeyEvent.VK_UP, 0)
        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The caret moved to position 2 on the first line:'
            selection.get().caret() == 2
        when : 'We press the down arrow key:'
            press(KeyEvent.VK_DOWN, 0)
        then : 'The caret is back at position 7:'
            selection.get().caret() == 7
        when : 'We press the down arrow key on the last line:'
            press(KeyEvent.VK_DOWN, 0)
        then : 'The caret is at the end of the text:'
            selection.get().caret() == 11

        when : 'Your code puts the caret at the end of the text again, and we press the up arrow key:'
            UI.runNow { selection.set(TextSelection.caretAt(11)) }
            press(KeyEvent.VK_UP, 0)
        then : 'The caret is at position 5, at the end of the first line, right of the red letters:'
            selection.get().caret() == 5
            selection.get().isCaretAtLineEnd()
            selection.get().caretBounds().get().location().x() >= firstWord.x + firstWord.width - 1
            selection.get().caretBounds().get().location().y() < firstWord.y + firstWord.height
        when : 'We press the up arrow key on the first line:'
            press(KeyEvent.VK_UP, 0)
        then : 'The caret is at the start of the text:'
            selection.get().caret() == 0
    }

    def 'With `withTextSelection("body", selection)` and `obstacles(..)`, the up and the down arrow key move the caret between the line below an obstacle and the two parts of the line beside it.'()
    {
        reportInfo """
            The content area of the component reaches from x = 10 to x = 390, and an obstacle covers the first
            line from x = 150 to x = 250. The text is "HHHHH HHHHHHH " followed by a word of 22 letters "H". At a
            font size of 20, an "H" is about 14 pixels wide. The first word and the space behind it, positions 0 to
            6, fit left of the obstacle, and the second word and the space behind it, positions 6 to 14, go right
            of it. The long third word, positions 14 to 36, is about 308 pixels wide, so it goes onto the second
            line, below the obstacle, starting at the left edge and reaching to about x = 318, under the second word.

            The up and the down arrow key move the caret to the gap nearest to it horizontally on the line above
            or below. On the line above, both parts beside the obstacle count, and the caret goes to the part which
            lies nearest to it horizontally. Position 16 lies two letters into the third word, at about x = 38,
            below the first word, so the up arrow key moves the caret to position 2, two letters into the first
            word. Position 9 lies three letters into the second word, at about x = 292, right of the obstacle, and
            the down arrow key moves the caret from there to the gap below it, 20 letters into the third word,
            which is position 34. The up arrow key brings it back to position 9, right of the obstacle, and not to
            the end of the first word, which is the gap of the left part nearest to x = 292.
        """
        given : 'A property which holds a caret at position 16, two letters into the third word:'
            var selection = Var.of(TextSelection.caretAt(16))
        and : 'A component which paints the three words around an obstacle on the first line:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(new Rectangle(150, 0, 100, 30))
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("HHHHH HHHHHHH " + "H" * 22)
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, and let SwingTree write the caret rectangles into the property:'
            Utility.renderSingleComponent(box)
            UI.sync()

        expect : 'The text flows around the obstacle: position 9 lies right of the obstacle, and position 16 below it:'
            selection.get().withCaret(9).caretBounds().get().location().x() >= 250
            selection.get().caretBounds().get().location().y() >= 30

        when : 'We press the up arrow key:'
            press(KeyEvent.VK_UP, 0)
        then : 'The caret moved to position 2, left of the obstacle:'
            selection.get().caret() == 2

        when : 'Your code puts the caret at position 9, right of the obstacle, and we press the down arrow key:'
            UI.runNow { selection.set(TextSelection.caretAt(9)) }
            press(KeyEvent.VK_DOWN, 0)
        then : 'The caret is 20 letters into the third word, below the place where it was:'
            selection.get().caret() == 34
        when : 'We press the up arrow key:'
            press(KeyEvent.VK_UP, 0)
        then : 'The caret is back at position 9, right of the obstacle:'
            selection.get().caret() == 9
    }

    def 'With `withTextSelection("body", selection)`, several presses of the up or the down arrow key in a row keep the caret near the place where the first press started, even across a shorter line.'()
    {
        reportInfo """
            The component paints three paragraphs: "HHHHHH", "HH" and "HHHHHH". A line break between two
            paragraphs is one character, so the second paragraph holds the positions 7 to 9 and the third the
            positions 10 to 16. At a font size of 20, an "H" is about 14 pixels wide.

            The caret is at position 5, five letters into the first line, about 70 pixels right of the start of the
            line. The down arrow key moves it to the second line, which is only two letters long, so the nearest gap
            is the end of that line, position 9, about 28 pixels right of the start. A second press of the down
            arrow key moves the caret to the third line. A text editor remembers where the first press started, so
            the caret goes back to about 70 pixels, which is position 15, and not to about 28 pixels, which would
            be position 12. The caret keeps that place for as long as your users press only the up and the down
            arrow key: two presses of the up arrow key move it back to position 9 and then to position 5.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a caret at position 5:'
            var selection = Var.of(TextSelection.caretAt(5))
        and : 'A component which paints three paragraphs, the middle one shorter than the others:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 150)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("HHHHHH\nHH\nHHHHHH")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press the down arrow key:'
            press(KeyEvent.VK_DOWN, 0)
        then : 'The caret is at the end of the short line:'
            selection.get().caret() == 9
        when : 'We press the down arrow key again:'
            press(KeyEvent.VK_DOWN, 0)
        then : 'The caret is on the third line, five letters in, below the place where it started:'
            selection.get().caret() == 15
        when : 'We press the up arrow key twice:'
            2.times { press(KeyEvent.VK_UP, 0) }
        then : 'The caret is back where it started:'
            selection.get().caret() == 5

        when : 'We press the right arrow key, which ends the row of up and down presses, and then the down arrow key twice:'
            press(KeyEvent.VK_RIGHT, 0)
            2.times { press(KeyEvent.VK_DOWN, 0) }
        then : 'The caret is six letters into the third line, because the new row of presses started at position 6:'
            selection.get().caret() == 16
    }

    def 'With `withTextSelection("body", selection)`, Shift+Down from the start of a wrapped line selects up to the start of the next line and shows the caret there, not at the end of the line above.'()
    {
        reportInfo """
            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". With `wrapLines(true)`
            the first word and the space behind it go onto the first line, and the second word onto the second line.
            Position 5 ends the first line and starts the second.

            The caret is at position 0. Shift+Down keeps the anchor at 0 and moves the caret to the gap on the second
            line nearest to it, which is the start of the second line, position 5. The selection now reaches from 0
            to 5, and its caret is the end of the selected characters. Your users moved the caret down, so SwingTree
            shows it at the start of the second line, where it went, and `caretBounds()` returns that place. If
            SwingTree showed it at the end of the selected characters on the first line instead, the caret would
            look as if Shift+Down had not moved it to another line at all.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key event to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers it directly.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component whose content area is 100 pixels wide, with the first word in red and the second in green:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the red and the green letters:'
            var image      = Utility.renderSingleComponent(box)
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)

        when : 'We press Shift+Down, handing the key event over with `redispatchEvent`:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)
                )
            }

        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The first line is selected, from position 0 to position 5:'
            selection.get().anchor() == 0
            selection.get().caret()  == 5
        and : 'The caret lies at the start of the second line, in front of the green letters:'
            !selection.get().isCaretAtLineEnd()
            selection.get().caretBounds().get().location().x() <= secondWord.x
            selection.get().caretBounds().get().location().y() >= firstWord.y + firstWord.height - 1
    }

    def 'With `selection(s -> s.navigateWithKeyboard(false))`, the arrow keys, Home and End leave the selection as it is, while the keyboard shortcut for selecting everything still selects every character.'()
    {
        reportInfo """
            By default, the arrow keys, Home and End move the caret of a text made selectable with
            `withTextSelection`. With `navigateWithKeyboard(false)`, SwingTree leaves these keys alone, for example
            because your code moves the caret with keys of its own, and two handlers of the same key would move the
            caret twice. The other keyboard shortcuts of the selection are not about moving the caret, so they keep
            working: Ctrl+A, or Command+A on macOS, still selects everything.

            The component paints "Hello world", and "lo" is selected, from position 3 to position 5. Every key
            press of this scenario, with and without the shift key, leaves that selection exactly as it is.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a selection from position 3 to position 5:'
            var selection = Var.of(TextSelection.between(3, 5))
        and : 'A component which paints "Hello world", whose selection style switches keyboard navigation off:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                            .selection( s -> s.navigateWithKeyboard(false) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press each of the six keys, once without and once with the shift key:'
            [KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_HOME, KeyEvent.VK_END].each {
                press(it, 0)
                press(it, InputEvent.SHIFT_DOWN_MASK)
            }
        then : 'The selection is still "lo":'
            selection.get().anchor() == 3
            selection.get().caret()  == 5

        when : 'We press the keyboard shortcut for selecting everything:'
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            press(KeyEvent.VK_A, shortcutKey)
        then : 'Everything is selected:'
            selection.get().anchor() == 0
            selection.get().caret()  == 11
    }

    @IgnoreIf({ GraphicsEnvironment.isHeadless() })
    def 'With `selection(s -> s.navigateWithKeyboard(false))`, the keyboard shortcut for copying still puts the selected characters onto the clipboard of the computer.'()
    {
        reportInfo """
            `navigateWithKeyboard(false)` switches off the keys which move the caret: the arrow keys, Home and End.
            Copying does not move the caret, so Ctrl+C, or Command+C on macOS, still puts the selected characters
            onto the clipboard of the computer. The component paints "Hello world", and "world" is selected.

            When Java runs in headless mode, `Toolkit.getSystemClipboard()` throws a `HeadlessException`, so this
            scenario only runs when Java is not headless. A key event sent with `dispatchEvent` only reaches the
            component which has the keyboard focus, and a component which is not inside a window never has the
            keyboard focus. So we hand the key event to the component with `KeyboardFocusManager.redispatchEvent`,
            which delivers it directly.
        """
        given : 'A component which paints "Hello world" with "world" selected, whose selection style switches keyboard navigation off:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                            .selection( s -> s.navigateWithKeyboard(false) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)
        and : 'The real clipboard of the computer, whose text we remember so that we can put it back, with something else on it:'
            var systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            var formerContents  = systemClipboard.getContents(null)
            var formerText      = formerContents != null && formerContents.isDataFlavorSupported(DataFlavor.stringFlavor)
                                    ? formerContents.getTransferData(DataFlavor.stringFlavor)
                                    : null
            systemClipboard.setContents(new StringSelection("something else"), null)

        when : 'We send the keyboard shortcut for copying:'
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_C, KeyEvent.CHAR_UNDEFINED)
                )
            }

        then : 'The clipboard holds the selected word:'
            systemClipboard.getData(DataFlavor.stringFlavor) == "world"

        cleanup : 'We put the text back which was on the clipboard before the scenario began:'
            if ( formerText != null )
                systemClipboard.setContents(new StringSelection(formerText as String), null)
    }

    def 'When the selection in the property of `withTextSelection` is disabled with `withEnabled(false)`, the arrow keys, Home and End move nothing.'()
    {
        reportInfo """
            A selection whose `isEnabled()` is `false` cannot be changed by your users: SwingTree ignores the mouse and
            the keyboard. This includes the keys which move the caret. The component paints "Hello world", and the
            disabled selection reaches from position 3 to position 5. After presses of every arrow key, Home and End,
            with and without the shift key, the property still holds that selection.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a disabled selection from position 3 to position 5:'
            var selection = Var.of(TextSelection.between(3, 5).withEnabled(false))
        and : 'A component which paints "Hello world":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press each of the six keys, once without and once with the shift key:'
            [KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_HOME, KeyEvent.VK_END].each {
                press(it, 0)
                press(it, InputEvent.SHIFT_DOWN_MASK)
            }
        then : 'The selection is still the disabled selection from 3 to 5:'
            selection.get().anchor() == 3
            selection.get().caret()  == 5
            !selection.get().isEnabled()
    }

    def 'With `withTextSelection("body", selection)`, a caret in the middle of a line which your users reach with the right arrow key equals the same caret reached with the left arrow key, and neither says `isCaretAtLineEnd()`.'()
    {
        reportInfo """
            `isCaretAtLineEnd()` tells on which of two lines a caret lies where a position is both the end of one
            painted line and the start of the next. In the middle of a line there is only one place, so the flag
            has nothing to choose. When SwingTree writes a selection into the property, it keeps the flag only
            where the caret really is the end of a painted line, and sets it to `false` everywhere else.

            The component paints "Hello world" on one line. Your user moves the caret to position 5, behind
            "Hello", once with the right arrow key from position 4 and once with the left arrow key from position 6.
            Both carets lie in the same place, so the property holds two equal selections, and neither says that
            its caret is at the end of a line. If the right arrow key left the flag `true` there, the flag would
            claim a line end in the middle of a line, and your code which compares the selection with
            `TextSelection.caretAt(5)` would get a different answer depending on the key your user pressed.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a caret at position 4:'
            var selection = Var.of(TextSelection.caretAt(4))
        and : 'A component which paints "Hello world" on one line:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press the right arrow key, handing the key event over with `redispatchEvent`:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED)
                )
            }
            var reachedFromTheLeft = selection.get()
        and : 'Your code puts the caret at position 6, and we press the left arrow key:'
            UI.runNow { selection.set(TextSelection.caretAt(6)) }
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED)
                )
            }
            var reachedFromTheRight = selection.get()

        then : 'Both carets are at position 5:'
            reachedFromTheLeft.caret()  == 5
            reachedFromTheRight.caret() == 5
        and : 'Neither says that it is at the end of a line:'
            !reachedFromTheLeft.isCaretAtLineEnd()
            !reachedFromTheRight.isCaretAtLineEnd()
        and : 'The two selections are equal:'
            reachedFromTheLeft == reachedFromTheRight
            reachedFromTheLeft.hashCode() == reachedFromTheRight.hashCode()
    }

    def 'After SwingTree has written a selection into the property of `withTextSelection`, `isCaretAtLineEnd()` is `true` only for a caret at the end of a painted line.'()
    {
        reportInfo """
            `TextSelection.between(anchor, caret)` sets `isCaretAtLineEnd()` to `true` when the caret ends the
            selected characters, because where a line wraps, such a caret belongs at the end of the upper line,
            next to the last selected character. A selection which your code creates holds that value until
            SwingTree has written it into the property. Then SwingTree keeps the flag only where the caret really
            is the end of a painted line, so the flag never claims a line end where there is none.

            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". With
            `wrapLines(true)` the first word and the space behind it go onto the first line, and the second word
            onto the second line, so position 5 ends the first line. Position 7 lies two letters into the second
            line. `between(2, 7)` therefore says `true` when your code creates it, and `false` once SwingTree has
            painted it. `between(2, 5)` ends at the end of the first line, so it still says `true` after the paint.
        """
        given : 'A property which holds a selection from position 2 to position 7:'
            var selection = Var.of(TextSelection.between(2, 7))
        and : 'A component whose content area is 100 pixels wide:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("HHHH HHHH")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        expect : 'The selection which your code created says that its caret is at the end of a line:'
            selection.get().isCaretAtLineEnd()

        when : """
            We render the component. When SwingTree paints the component, it queues a task on the UI thread which
            writes the selection with its pixel rectangles into the property. `UI.sync()` waits until the UI thread
            has run every task queued so far:
        """
            Utility.renderSingleComponent(box)
            UI.sync()
        then : 'The caret at position 7, in the middle of the second line, is not at the end of a line:'
            selection.get().caret() == 7
            !selection.get().isCaretAtLineEnd()

        when : 'Your code writes the selection from position 2 to position 5 into the property, and we render the component again:'
            UI.runNow { selection.set(TextSelection.between(2, 5)) }
            Utility.renderSingleComponent(box)
            UI.sync()
        then : 'The caret at position 5, at the end of the first line, still says so:'
            selection.get().caret() == 5
            selection.get().isCaretAtLineEnd()
    }

    def 'With `withTextSelection("body", selection)`, the arrow keys with Ctrl held down, or Alt on macOS, and a double click agree on what a word is, so "don\'t" is one word for both.'()
    {
        reportInfo """
            A double click selects the word under the mouse, and SwingTree finds the start and the end of that
            word with `java.text.BreakIterator.getWordInstance()`, which knows the word rules of many languages.
            By these rules, the apostrophe in "don't" belongs to the word. The right arrow key with Ctrl held down,
            or Alt on macOS, jumps over the next word by the same rules, skipping the spaces and punctuation in
            front of it. So your users see one rule for words in the whole text: if the keys stopped behind
            "don", a double click on "don't" would still select all five characters.

            The component paints "don't stop", which has 10 characters. Position 5 is the gap behind "don't".
            SwingTree uses Alt instead of Ctrl wherever the keyboard shortcuts of the operating system use the
            Command key, which `Toolkit.getMenuShortcutKeyMask()` tells, and this scenario asks it in the same way.
            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key event to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers it directly.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "don\'t stop" in red on white:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "don't" ),
                                StyledString.of( f -> f.color(BLUE), " stop"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'The modifier key which jumps by words: Alt where the shortcut key is the Command key, and Ctrl everywhere else:'
            int wordKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                            ? InputEvent.ALT_DOWN_MASK
                            : InputEvent.CTRL_DOWN_MASK
        and : 'We render the component and find the middle of the red letters of "don\'t":'
            var dont = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), RED)
            int x = dont.centerX
            int y = dont.centerY

        when : 'We press the right arrow key with the word key held down:'
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, wordKey, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED)
                )
            }
        then : 'The caret jumped over the whole of "don\'t":'
            selection.get().caret() == 5

        when : 'We double-click the middle of "don\'t":'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, x, y, 2, false, MouseEvent.BUTTON1))
            }
        then : 'The double click selected the same word, from position 0 to position 5:'
            selection.get().anchor() == 0
            selection.get().caret()  == 5
    }

    def 'With `selection(s -> s.selectWordOnDoubleClick(false))`, a double click puts the caret into the gap nearest to the mouse, like a single click, and a triple click still selects the paragraph.'()
    {
        reportInfo """
            By default, a double click selects the word under the mouse. With `selectWordOnDoubleClick(false)`,
            the second click of a double click does what a single click does: it puts the caret into the gap
            nearest to the mouse, and selects nothing. The triple click is a switch of its own, so a third click
            still selects the whole paragraph.

            The component paints "Hello world", which has 11 characters. Your user double-clicks on the left
            edge of the "w", where the nearest gap is position 6. Then your user clicks a third time, which
            selects the paragraph from position 0 to position 11. Swing reports the second and the third press
            of such a series of clicks as mouse press events whose click count is 2 and 3.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue, whose selection style switches the double click off:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.selectWordOnDoubleClick(false) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the left edge of the blue "w":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int x = world.x
            int y = world.centerY

        when : 'We double-click on the left edge of the "w":'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, x, y, 2, false, MouseEvent.BUTTON1))
            }
        then : 'Nothing is selected, and the caret is in the gap in front of the "w":'
            selection.get().isEmpty()
            selection.get().caret() == 6

        when : 'We click a third time:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  4, InputEvent.BUTTON1_DOWN_MASK, x, y, 3, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 5, 0, x, y, 3, false, MouseEvent.BUTTON1))
            }
        then : 'The whole paragraph is selected:'
            selection.get().anchor() == 0
            selection.get().caret()  == 11
    }

    def 'With `withTextSelection("body", selection)`, a triple click selects the #which paragraph, which is everything between two line breaks however many lines it wraps over, without the line breaks.'(
        String which, Color colour, boolean aimAtItsLastLine, int start, int end, String text
    ) {
        reportInfo """
            A paragraph is everything between two line breaks of the text, the "\\n" characters. A triple click
            selects the paragraph under the mouse, also when the paragraph is wrapped over several painted lines.
            The line breaks themselves are not selected, so a copied paragraph does not end with an empty line.
            The first paragraph starts at the start of the text, and the last one ends at the end of the text.

            The content area of the component is 100 pixels wide. The text has three paragraphs: "HH HH" in red,
            "HHHH HHHH HHHH" in green and "HH" in blue. At a font size of 20, an "H" is about 14 pixels wide, so the
            green paragraph, about 178 pixels, wraps over three lines with one word on each, while the red and the
            blue one fit on a line. A line break counts as one character, so the red paragraph holds the positions
            0 to 5, the green one 6 to 20 and the blue one 21 to 23. For the green paragraph your user clicks on its
            last line, so the selection reaches back over two lines which your user did not click on.

            Swing reports the second and the third press of a series of clicks as mouse press events whose click
            count is 2 and 3. In this scenario your user triple-clicks the ${which} paragraph.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component whose content area is 100 pixels wide, which paints three paragraphs in red, green and blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 200)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HH HH\n" ),
                                StyledString.of( f -> f.color(GREEN), "HHHH HHHH HHHH\n" ),
                                StyledString.of( f -> f.color(BLUE),  "HH" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the letters of the paragraph, and the point to click on: the first letter of its last line, or its middle:'
            var image     = Utility.renderSingleComponent(box)
            var paragraph = boundsOfPixelsPaintedIn(image, colour)
            int x = aimAtItsLastLine ? paragraph.x + 3 : paragraph.centerX
            int y = aimAtItsLastLine ? paragraph.y + paragraph.height - 5 : paragraph.centerY

        when : 'We triple-click there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  4, InputEvent.BUTTON1_DOWN_MASK, x, y, 3, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 5, 0, x, y, 3, false, MouseEvent.BUTTON1))
            }

        then : 'The green paragraph was really wrapped, so it is taller than one line of about 22 pixels:'
            boundsOfPixelsPaintedIn(image, GREEN).height > 30
        and : 'The whole paragraph is selected, without the line breaks:'
            selection.get().anchor() == start
            selection.get().caret()  == end
            selection.get().selectedText() == text

        where :
            which    | colour | aimAtItsLastLine || start | end | text
            "first"  | RED    | false            || 0     | 5   | "HH HH"
            "middle" | GREEN  | true             || 6     | 20  | "HHHH HHHH HHHH"
            "last"   | BLUE   | false            || 21    | 23  | "HH"
    }

    def 'With `selection(s -> s.selectParagraphOnTripleClick(false))`, the third click of a triple click puts the caret into the gap nearest to the mouse, like a single click.'()
    {
        reportInfo """
            By default, a triple click selects the whole paragraph under the mouse. With
            `selectParagraphOnTripleClick(false)`, the third click does what a single click does: it puts the
            caret into the gap nearest to the mouse and selects nothing. The second click still selects the word,
            because the double click is a switch of its own.

            The component paints "Hello world". Your user triple-clicks on the left half of the "w", where the
            nearest gap is position 6. After the second click "world" is selected, from position 6 to position 11,
            and after the third click only the caret is left, at position 6. Swing reports the second and the
            third press of such a series of clicks as mouse press events whose click count is 2 and 3.

            A double click selects the word of the character under the mouse, so the mouse must be on the "w"
            itself and not on its outermost pixel. Glyph edges are smoothed, and on some computers the leftmost
            pixel of the "w" is painted in the last pixel column of the space in front of it. A double click on
            that pixel would select the space. So the scenario aims 4 pixels right of the leftmost blue pixel,
            which is still in the left half of a "w" that is about 14 pixels wide.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue, whose selection style switches the triple click off:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.selectParagraphOnTripleClick(false) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and aim 4 pixels right of the leftmost blue pixel, into the left half of the "w":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int x = world.x + 4
            int y = world.centerY

        when : 'We double-click on the left half of the "w":'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, x, y, 2, false, MouseEvent.BUTTON1))
            }
        then : 'The word "world" is selected:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11

        when : 'We click a third time:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  4, InputEvent.BUTTON1_DOWN_MASK, x, y, 3, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 5, 0, x, y, 3, false, MouseEvent.BUTTON1))
            }
        then : 'Nothing is selected, and the caret is in the gap in front of the "w":'
            selection.get().isEmpty()
            selection.get().caret() == 6
    }

    def 'Without `selection(s -> s.selectWordOnLongPress(true))`, holding the mouse button down on a word selects nothing.'()
    {
        reportInfo """
            On a phone, your users select a word by holding a finger on it. On a desktop they double-click it, so
            by default SwingTree does not select a word when the mouse button is held down: the press only puts the
            caret into the gap nearest to the mouse. The time a long press needs is about half a second. This
            scenario holds the button down for three times as long, which a long press would certainly have used.

            The component paints "Hello world". Your user presses the button on the middle of "world" and keeps it
            down.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the middle of the blue "world":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int x = world.centerX
            int y = world.centerY

        when : 'We press the mouse button on the middle of "world" and keep it down for 1.5 seconds, three times the time of a long press:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
            }
            Thread.sleep(1500)
            UI.sync()

        then : 'The press put the caret into "world", and nothing is selected:'
            selection.get().isEmpty()
            selection.get().caret() > 6
            selection.get().caret() < 11
    }

    def 'With `selection(s -> s.selectWordOnLongPress(true))` at a UI scale of #uiScale, holding the mouse button down on a word and moving it by #dx pixels to the right and #dy pixels down on the screen selects that word after about half a second.'(
        float uiScale, int dx, int dy
    ) {
        reportInfo """
            On a phone, your users select a word by holding a finger on it. With `selectWordOnLongPress(true)`
            SwingTree does the same for the mouse: about half a second after your user pressed the left button, the
            word under the mouse is selected, as if your user had double-clicked it. A finger never holds perfectly
            still, so a movement of up to 5 pixels in each direction before then does not cancel the long press.

            SwingTree multiplies these 5 pixels by the UI scale, just as it multiplies every size in a style:
            at a UI scale of 2 the letters are twice as large, a finger covers twice as many pixels of the screen,
            and the long press allows 10 pixels on the screen. A movement of 8 pixels then keeps the long press
            alive. If SwingTree compared the movement with 5 pixels of the screen, it would cancel it.

            The component paints "Hello world". Your user presses the button on the middle of "world" and moves the
            mouse while holding it. The time of a long press depends on the timer of Swing and on how busy the
            computer is, so this scenario does not wait a fixed time: it checks the property every 10 milliseconds
            and gives up only after 5 seconds.

            In this scenario the UI scale is ${uiScale}, and the mouse moves ${dx} pixels to the right and ${dy}
            pixels down.
        """
        given : 'We set the UI scale factor of SwingTree. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue, whose selection style switches the long press on:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.selectWordOnLongPress(true) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the middle of the blue "world":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int x = world.centerX
            int y = world.centerY

        when : 'We press the mouse button on the middle of "world" and move the mouse while holding it:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED, 1, InputEvent.BUTTON1_DOWN_MASK, x + dx, y + dy, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We check the property every 10 milliseconds until "world" is selected, for at most 5 seconds:'
            var deadline = System.currentTimeMillis() + 5000
            while ( selection.get().selectedText() != "world" && System.currentTimeMillis() < deadline )
                Thread.sleep(10)

        then : 'The word "world" is selected, from its start at position 6 to its end at position 11:'
            selection.get().anchor() == 6
            selection.get().caret()  == 11

        cleanup : 'We release the mouse button and set the UI scale factor back to 1:'
            UI.runNow {
                box?.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, x + dx, y + dy, 1, false, MouseEvent.BUTTON1))
            }
            SwingTree.get().setUiScaleFactor(1f)

        where :
            uiScale | dx | dy
            1f      | 3  | 2
            2f      | 8  | 0
    }

    def 'At a UI scale of 2, with `selection(s -> s.selectWordOnLongPress(true))`, moving the mouse by 14 pixels on the screen, more than the 10 pixels which the allowed 5 pixels become, cancels the long press.'()
    {
        reportInfo """
            With `selectWordOnLongPress(true)`, holding the left mouse button down on a word for about half a second
            selects that word, and a movement of up to 5 pixels before then is allowed. Just as with every size in a style,
            these 5 pixels are multiplied by the UI scale, so at a UI scale of 2 the long press allows 10 pixels on
            the screen, and a movement of 14 pixels cancels it. The movement is then a drag, which selects from the
            press to the mouse, and not the word.

            The component paints "Hello world". Your user presses the button on the middle of "world" and moves the
            mouse 14 pixels to the right. To prove that the long press does not happen later, this scenario waits
            1.5 seconds, three times the time of a long press.
        """
        given : 'We set the UI scale factor of SwingTree to 2. Every size in a style is multiplied by it:'
            SwingTree.get().setUiScaleFactor(2f)
        and : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue, whose selection style switches the long press on:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.selectWordOnLongPress(true) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the middle of the blue "world":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int x = world.centerX
            int y = world.centerY

        when : 'We press the mouse button on the middle of "world", move the mouse 14 pixels to the right while holding it, and wait 1.5 seconds:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED, 1, InputEvent.BUTTON1_DOWN_MASK, x + 14, y, 1, false, MouseEvent.BUTTON1))
            }
            Thread.sleep(1500)
            UI.sync()

        then : 'The drag selected from the gap nearest to the press to the gap nearest to the mouse, and not the word:'
            selection.get().anchor() == selection.get().positionAt(Position.of(x, y)).get()
            selection.get().caret()  == selection.get().positionAt(Position.of(x + 14, y)).get()
            selection.get().selectedText() != "world"

        cleanup : 'We release the mouse button and set the UI scale factor back to 1:'
            UI.runNow {
                box?.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, x + 14, y, 1, false, MouseEvent.BUTTON1))
            }
            SwingTree.get().setUiScaleFactor(1f)
    }

    def 'With `selection(s -> s.selectWordOnLongPress(true))`, moving the mouse by more than 5 pixels, releasing the button early, or losing the keyboard focus cancels the long press.'()
    {
        reportInfo """
            With `selectWordOnLongPress(true)`, holding the left mouse button down on a word for about half a second
            selects that word. Three things cancel the long press before then, and after each of them the press is
            only what it is without a long press. If the long press fired anyway, the selection of your user would
            jump to a word half a second after your user started to do something else.

            Moving the mouse by more than 5 pixels while holding the button is a drag, so it selects from the
            press to the mouse and not the word. Releasing the button early is a click, so it leaves only the caret.
            Losing the keyboard focus, for example because a dialog opened, also leaves only the caret.

            The component paints "Hello world". Each time, your user presses the button on the left edge of the
            "w", where the nearest gap is position 6. To prove that nothing happens later, this scenario waits
            1.5 seconds after each cancellation, three times the time of a long press. A focus event sent with
            `dispatchEvent` first passes the `KeyboardFocusManager` of Swing, which drops it when the component is
            not inside a focused window. A real window only gets the keyboard focus when the window manager of the
            operating system gives it, which takes an unknown amount of time and may not happen at all while other
            windows are in front, so this scenario hands the focus event over with
            `KeyboardFocusManager.redispatchEvent`, which delivers it directly.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "Hello " in red and "world" in blue, whose selection style switches the long press on:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                            .selection( s -> s.selectWordOnLongPress(true) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the left edge of the blue "w":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int x = world.x
            int y = world.centerY

        when : 'We press the button on the left edge of the "w", move the mouse 20 pixels to the right while holding it, and wait 1.5 seconds:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED, 1, InputEvent.BUTTON1_DOWN_MASK, x + 20, y, 1, false, MouseEvent.BUTTON1))
            }
            Thread.sleep(1500)
            UI.sync()
        then : 'The drag selected from the "w" to the gap nearest to the mouse, and not the word:'
            selection.get().anchor() == 6
            selection.get().caret()  == selection.get().positionAt(Position.of(x + 20, y)).get()
            selection.get().caret()  < 11

        when : 'We release the button, press it again on the left edge of the "w", release it at once, and wait 1.5 seconds:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, x + 20, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  3, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 4, 0, x, y, 1, false, MouseEvent.BUTTON1))
            }
            Thread.sleep(1500)
            UI.sync()
        then : 'Only the caret is left, in front of the "w":'
            selection.get().isEmpty()
            selection.get().caret() == 6

        when : 'We press the button again on the left edge of the "w", the component loses the keyboard focus, and we wait 1.5 seconds:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, 5, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box, new FocusEvent(box, FocusEvent.FOCUS_LOST))
            }
            Thread.sleep(1500)
            UI.sync()
        then : 'Only the caret is left, in front of the "w":'
            selection.get().isEmpty()
            selection.get().caret() == 6

        cleanup : 'We release the mouse button:'
            UI.runNow {
                box?.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 6, 0, x, y, 1, false, MouseEvent.BUTTON1))
            }
    }

    def 'With `selection(s -> s.copyToClipboard(false))`, copying puts nothing onto a clipboard and leaves on it what was there, while the keyboard shortcut for selecting everything still selects every character.'()
    {
        reportInfo """
            By default, your users can copy the selected characters. With `copyToClipboard(false)`, they can still
            select them, but nothing reaches any clipboard: the `TransferHandler` of the component, which the keyboard
            shortcut for copying calls, hands out nothing. Whatever was on the clipboard before stays there, so your
            users do not lose what they copied from another program. Selecting everything with Ctrl+A, or Command+A
            on macOS, is not copying, so it still works.

            The component paints "Hello world", and "world" is selected. This scenario calls `exportToClipboard` on
            the `TransferHandler` of the component with a clipboard that only this scenario uses, which holds the
            text "copied before", so it neither needs nor overwrites the clipboard of the computer. A key event sent
            with `dispatchEvent` only reaches the component which has the keyboard focus, and a component which is
            not inside a window never has the keyboard focus. So we hand the key event for selecting everything to
            the component with `KeyboardFocusManager.redispatchEvent`, which delivers it directly.
        """
        given : 'A property which holds a selection of "world", from position 6 to position 11:'
            var selection = Var.of(TextSelection.between(6, 11))
        and : 'A component which paints "Hello world", whose selection style switches copying off:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                            .selection( s -> s.copyToClipboard(false) )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'A clipboard that only this scenario uses, which holds the text "copied before":'
            var clipboard = new Clipboard("clipboard of this scenario")
            clipboard.setContents(new StringSelection("copied before"), null)
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We call `exportToClipboard` on the `TransferHandler` of the component:'
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }
        then : '"world" is still selected, and the clipboard still holds "copied before":'
            selection.get().selectedText() == "world"
            clipboard.getData(DataFlavor.stringFlavor) == "copied before"

        when : 'We press the keyboard shortcut for selecting everything:'
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED)
                )
            }
        then : 'Everything is selected:'
            selection.get().anchor() == 0
            selection.get().caret()  == 11
    }

    @IgnoreIf({ GraphicsEnvironment.isHeadless() })
    def 'With `selection(s -> s.copyToClipboard(false))`, the keyboard shortcut for copying leaves the clipboard of the computer as it was.'()
    {
        reportInfo """
            With `copyToClipboard(false)`, nothing reaches any clipboard, also not through the keyboard shortcut for
            copying, which is Ctrl+C on Windows and Linux and Command+C on macOS. The component paints "Hello world"
            and "world" is selected, but after the shortcut the clipboard of the computer still holds what was on it.

            When Java runs in headless mode, `Toolkit.getSystemClipboard()` throws a `HeadlessException`, so this
            scenario only runs when Java is not headless. A key event sent with `dispatchEvent` only reaches the
            component which has the keyboard focus, and a component which is not inside a window never has the
            keyboard focus. So we hand the key event to the component with `KeyboardFocusManager.redispatchEvent`,
            which delivers it directly.
        """
        given : 'A component which paints "Hello world" with "world" selected, whose selection style switches copying off:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .text("body", t -> t
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("Hello world")
                            .selection( s -> s.copyToClipboard(false) )
                        )
                    )
                    .withTextSelection("body", Var.of(TextSelection.between(6, 11)))
                    .get(JBox)
        and : 'The real clipboard of the computer, whose text we remember so that we can put it back, with "something else" on it:'
            var systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            var formerContents  = systemClipboard.getContents(null)
            var formerText      = formerContents != null && formerContents.isDataFlavorSupported(DataFlavor.stringFlavor)
                                    ? formerContents.getTransferData(DataFlavor.stringFlavor)
                                    : null
            systemClipboard.setContents(new StringSelection("something else"), null)
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We send the keyboard shortcut for copying:'
            int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                                ? InputEvent.META_DOWN_MASK
                                : InputEvent.CTRL_DOWN_MASK
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, shortcutKey, KeyEvent.VK_C, KeyEvent.CHAR_UNDEFINED)
                )
            }

        then : 'The clipboard still holds "something else":'
            systemClipboard.getData(DataFlavor.stringFlavor) == "something else"

        cleanup : 'We put the text back which was on the clipboard before the scenario began:'
            if ( formerText != null )
                systemClipboard.setContents(new StringSelection(formerText as String), null)
    }

    def 'Setting each of the switches `navigateWithKeyboard`, `selectWordOnDoubleClick`, `selectParagraphOnTripleClick`, `selectWordOnLongPress` and `copyToClipboard` to its default makes a selection style equal to the default one.'()
    {
        reportInfo """
            The selection style has five switches for what the mouse and the keyboard do: `navigateWithKeyboard`,
            `selectWordOnDoubleClick`, `selectParagraphOnTripleClick` and `copyToClipboard` are `true` by default,
            and `selectWordOnLongPress` is `false`. Setting a switch to the value which it already has changes
            nothing, so a style which sets all five to their defaults equals a style which sets none of them, has
            the same hash code and prints the same text. Your code can therefore set a switch from a setting of
            your users without checking whether the setting differs from the default. If setting a switch to its
            current value flipped it instead, `selectWordOnLongPress(false)` would switch the long press on.

            We let the style of a text hand the selection style which it builds to a variable of this scenario.
            `selection(..)` passes the current selection style to the function you give it, and uses the style
            which that function returns.
        """
        given : 'Two variables which receive the selection styles:'
            var withoutSwitches  = null
            var withDefaultsSet  = null
        and : 'Two components, one whose selection style sets no switch, and one which sets every switch to its default:'
            UI.box()
            .withStyle( it -> it.text("body", t -> t.content("Hello world").selection( s -> withoutSwitches = s.showCaret(true) )) )
            .withTextSelection("body")
            .get(JBox)
            UI.box()
            .withStyle( it -> it.text("body", t -> t.content("Hello world").selection( s -> withDefaultsSet = s
                .showCaret(true)
                .navigateWithKeyboard(true)
                .selectWordOnDoubleClick(true)
                .selectParagraphOnTripleClick(true)
                .selectWordOnLongPress(false)
                .copyToClipboard(true)
            )) )
            .withTextSelection("body")
            .get(JBox)

        expect : 'Both styles are equal, have the same hash code and print the same text:'
            withoutSwitches != null
            withoutSwitches == withDefaultsSet
            withoutSwitches.hashCode() == withDefaultsSet.hashCode()
            withoutSwitches.toString() == withDefaultsSet.toString()
        and : 'The text names every switch with its value:'
            withoutSwitches.toString().contains("navigateWithKeyboard=true")
            withoutSwitches.toString().contains("selectWordOnDoubleClick=true")
            withoutSwitches.toString().contains("selectParagraphOnTripleClick=true")
            withoutSwitches.toString().contains("selectWordOnLongPress=false")
            withoutSwitches.toString().contains("copyToClipboard=true")
        and : 'Flipping any one switch makes a style which is not equal to them:'
            withoutSwitches != withDefaultsSet.navigateWithKeyboard(false)
            withoutSwitches != withDefaultsSet.selectWordOnDoubleClick(false)
            withoutSwitches != withDefaultsSet.selectParagraphOnTripleClick(false)
            withoutSwitches != withDefaultsSet.selectWordOnLongPress(true)
            withoutSwitches != withDefaultsSet.copyToClipboard(false)
    }

    def 'With `withTextSelection("body", selection)`, Home and End leave a caret in an empty paragraph where it is, while Ctrl+End, or Alt+End on macOS, still moves it to the end of the text.'()
    {
        reportInfo """
            Home and End move the caret to the start and the end of its line. In an empty paragraph, which lies
            between two line breaks in a row, the start and the end of the line are the same gap, so Home and End
            leave the caret where it is. If they jumped to the start or the end of the whole text instead, your
            users would lose their place in a long text by pressing a key which should keep them on their line.

            The text is "First", two line breaks and "Third". A line break counts as one character, so position 5
            is the gap in front of the first line break, position 6 is the gap between the two line breaks, which
            is the empty paragraph, and position 12 is the end of the text. With Ctrl held down, or Alt where the
            keyboard shortcuts of the operating system use the Command key, End still moves the caret to the end of
            the text. This scenario asks `Toolkit.getMenuShortcutKeyMask()` which of the two keys applies.

            A key event sent with `dispatchEvent` only reaches the component which has the keyboard focus, and a
            component which is not inside a window never has the keyboard focus. So we hand the key events to the
            component with `KeyboardFocusManager.redispatchEvent`, which delivers them directly.
        """
        given : 'A property which holds a caret at position 5, behind "First":'
            var selection = Var.of(TextSelection.caretAt(5))
        and : 'A component which paints "First", an empty paragraph and "Third":'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 150)
                        .padding(10)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content("First\n\nThird")
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'The modifier key which jumps to the end of the text: Alt where the shortcut key is the Command key, and Ctrl everywhere else:'
            int farKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.META_MASK
                            ? InputEvent.ALT_DOWN_MASK
                            : InputEvent.CTRL_DOWN_MASK
        and : 'A small tool which presses a key with the given modifier keys held down:'
            var press = { int keyCode, int modifiers ->
                UI.runNow {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                        new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)
                    )
                }
            }
        and : 'We render the component once, so that SwingTree knows where every character is:'
            Utility.renderSingleComponent(box)

        when : 'We press the right arrow key, which moves the caret over the first line break into the empty paragraph:'
            press(KeyEvent.VK_RIGHT, 0)
        then : 'The caret is at position 6:'
            selection.get().caret() == 6

        when : 'We press End:'
            press(KeyEvent.VK_END, 0)
        then : 'The caret is still at position 6:'
            selection.get().caret() == 6
        when : 'We press Home:'
            press(KeyEvent.VK_HOME, 0)
        then : 'The caret is still at position 6:'
            selection.get().caret() == 6

        when : 'We press End with the far key held down:'
            press(KeyEvent.VK_END, farKey)
        then : 'The caret is at the end of the text:'
            selection.get().caret() == 12
    }

    @PendingFeature(reason = "an empty paragraph has no painted line, so no caret rectangle and no mouse position (TextLayoutEngine)")
    def 'With `withTextSelection("body", selection)`, a triple click on an empty paragraph selects nothing and puts the caret between its two line breaks.'()
    {
        reportInfo """
            A paragraph is everything between two line breaks of the text, the "\\n" characters, and a triple click
            selects the paragraph under the mouse. Two line breaks in a row make an empty paragraph, which SwingTree
            paints as an empty line. A triple click on that line selects everything between its two line breaks,
            which is nothing, so only the caret is left, in the gap between them.

            The text is "First", two line breaks and "Third". A line break counts as one character, so position 6 is
            the gap between the two line breaks. Your user triple-clicks in the middle of the empty line, halfway
            between the letters of "First" and the letters of "Third". If SwingTree took the paragraph of the nearest
            letters instead, it would select "First" or "Third", which your user did not click on.

            Swing reports the second and the third press of a series of clicks as mouse press events whose click
            count is 2 and 3.
        """
        given : 'A property which holds a caret at position 0:'
            var selection = Var.of(TextSelection.caretAt(0))
        and : 'A component which paints "First" in red, an empty paragraph, and "Third" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "First\n\n" ),
                                StyledString.of( f -> f.color(BLUE), "Third" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component and find the point halfway between the red and the blue letters:'
            var image = Utility.renderSingleComponent(box)
            var above = boundsOfPixelsPaintedIn(image, RED)
            var below = boundsOfPixelsPaintedIn(image, BLUE)
            int x = above.x
            int y = ((above.y + above.height) + below.y) / 2

        when : 'We triple-click there:'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  2, InputEvent.BUTTON1_DOWN_MASK, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 3, 0, x, y, 2, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  4, InputEvent.BUTTON1_DOWN_MASK, x, y, 3, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 5, 0, x, y, 3, false, MouseEvent.BUTTON1))
            }

        then : '"Third" is painted two lines below "First", with the empty line between them:'
            below.y >= above.y + above.height + 15
        and : 'Nothing is selected, and the caret is in the gap between the two line breaks:'
            selection.get().isEmpty()
            selection.get().caret() == 6
    }

    @PendingFeature(reason = "an empty paragraph has no painted line, so no caret rectangle and no mouse position (TextLayoutEngine)")
    def 'With `withTextSelection("body", selection)`, a caret in an empty paragraph has a rectangle on the empty line, and the down arrow key reaches it from the line above.'()
    {
        reportInfo """
            Two line breaks in a row make an empty paragraph, which SwingTree paints as an empty line. A caret in that
            paragraph lies on the empty line, so `caretBounds()` returns a rectangle below the line above it and above
            the line below it, and SwingTree paints the caret there. The down arrow key moves the caret from the line
            above to the line below it, and the empty line is the line below the first one, so the caret goes into the
            empty paragraph and not over it.

            The text is "First", two line breaks and "Third". A line break counts as one character, so position 6 is
            the gap between the two line breaks. A key event sent with `dispatchEvent` only reaches the component which
            has the keyboard focus, and a component which is not inside a window never has the keyboard focus. So we
            hand the key event to the component with `KeyboardFocusManager.redispatchEvent`, which delivers it directly.
        """
        given : 'A property which holds a caret at position 6, in the empty paragraph:'
            var selection = Var.of(TextSelection.caretAt(6))
        and : 'A component which paints "First" in red, an empty paragraph, and "Third" in blue:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "First\n\n" ),
                                StyledString.of( f -> f.color(BLUE), "Third" )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)

        when : """
            We render the component and find the red and the blue letters. `UI.sync()` waits until SwingTree has
            written the caret rectangle into the property:
        """
            var image = Utility.renderSingleComponent(box)
            UI.sync()
            var above = boundsOfPixelsPaintedIn(image, RED)
            var below = boundsOfPixelsPaintedIn(image, BLUE)

        then : 'The caret rectangle lies between the red and the blue letters:'
            selection.get().caretBounds().isPresent()
            selection.get().caretBounds().get().location().y() >= above.y + above.height - 1
            selection.get().caretBounds().get().location().y() + selection.get().caretBounds().get().size().height().get() <= below.y + 1

        when : 'Your code puts the caret at position 2, in "First", and we press the down arrow key:'
            UI.runNow { selection.set(TextSelection.caretAt(2)) }
            UI.runNow {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(box,
                    new KeyEvent(box, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)
                )
            }
        then : 'The caret is in the empty paragraph:'
            selection.get().caret() == 6
    }

    def 'With `withTextSelection()` and no property, your users can select and copy the text which `text(t -> ..)` declares without a name.'()
    {
        reportInfo """
            `withTextSelection()` takes no property, for a text which your users should be able to select and copy
            while your code never needs to know what they selected. SwingTree then keeps the selection inside the
            component. It makes the text selectable which the style declares with `text(t -> ..)`, without a name,
            in the content layer.

            The component paints "Hello " in red and "world" in blue. Your user presses the mouse button on the left
            edge of the "w", drags it to the right edge of the "d" and releases it there. Then the scenario copies
            the selection by calling `exportToClipboard` on the `TransferHandler` of the component, which is what the
            keyboard shortcut for copying calls, with a clipboard that only this scenario uses. If SwingTree made the
            wrong text selectable, or none, the clipboard would stay empty.
        """
        given : 'A component which paints "Hello " in red and "world" in blue as the text without a name, made selectable with `withTextSelection()`:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                            .text( t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection()
                    .get(JBox)
        and : 'We render the component and find the left edge of the "w" and the right edge of the "d":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'We drag from the left edge of the "w" to the right edge of the "d":'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We copy the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The clipboard holds "world":'
            clipboard.getData(DataFlavor.stringFlavor) == "world"
    }

    def 'With `withTextSelection(UI.Layer.FOREGROUND, "caption")` and no property, your users can select and copy the text which the style declares in its foreground with `text(UI.Layer.FOREGROUND, "caption", ..)`.'()
    {
        reportInfo """
            `withTextSelection(UI.Layer.FOREGROUND, "caption")` takes no property, for a text which your users should
            be able to select and copy while your code never needs to know what they selected. SwingTree then keeps
            the selection inside the component. It makes the text named "caption" selectable which the style
            declares in its foreground layer with `text(UI.Layer.FOREGROUND, "caption", ..)`.

            The component paints "Hello " in red and "world" in blue. Your user presses the mouse button on the left
            edge of the "w", drags it to the right edge of the "d" and releases it there. Then the scenario copies
            the selection by calling `exportToClipboard` on the `TransferHandler` of the component, which is what the
            keyboard shortcut for copying calls, with a clipboard that only this scenario uses. If SwingTree made the
            wrong text selectable, or none, the clipboard would stay empty.
        """
        given : 'A component which paints "Hello " in red and "world" in blue as the foreground text "caption", made selectable with `withTextSelection(UI.Layer.FOREGROUND, "caption")`:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(400, 100)
                        .padding(10)
                        .backgroundColor(WHITE)
                            .text(UI.Layer.FOREGROUND, "caption", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "Hello " ),
                                StyledString.of( f -> f.color(BLUE), "world"  )
                            )
                        )
                    )
                    .withTextSelection(UI.Layer.FOREGROUND, "caption")
                    .get(JBox)
        and : 'We render the component and find the left edge of the "w" and the right edge of the "d":'
            var world = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(box), BLUE)
            int leftEdgeOfW  = world.x
            int rightEdgeOfD = world.x + world.width - 1
            int lineMiddle   = world.centerY

        when : 'We drag from the left edge of the "w" to the right edge of the "d":'
            UI.runNow {
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED,  0, InputEvent.BUTTON1_DOWN_MASK, leftEdgeOfW, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_DRAGGED,  1, InputEvent.BUTTON1_DOWN_MASK, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
                box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, 2, 0, rightEdgeOfD, lineMiddle, 1, false, MouseEvent.BUTTON1))
            }
        and : 'We copy the selection into a clipboard that only this scenario uses:'
            var clipboard = new Clipboard("clipboard of this scenario")
            UI.runNow { box.getTransferHandler().exportToClipboard(box, clipboard, TransferHandler.COPY) }

        then : 'The clipboard holds "world":'
            clipboard.getData(DataFlavor.stringFlavor) == "world"
    }

    def 'Moving the anchor with `withAnchorAt(..)` keeps the caret and the line on which it lies, also where a line wraps.'()
    {
        reportInfo """
            `withAnchorAt(..)` moves the anchor of a selection to the position nearest to a pixel coordinate, for
            example where your code paints a handle which your users drag. The caret is the other end of the
            selection, and it stays exactly where it was: at the same position, and on the same one of the two
            lines where its position is both the end of one line and the start of the next.

            The content area of the component is 100 pixels wide, and the text is "HHHH HHHH". With
            `wrapLines(true)` the first word and the space behind it go onto the first line, and the second word
            onto the second line, so position 5 ends the first line and starts the second. Your code selects from
            position 2 to position 5, so the caret lies at the end of the first line. Then it moves the anchor to
            the middle of the third green letter, on the second line, which is position 7. The selection now reaches
            from 7 back to 5, but its caret still lies at the end of the first line. If `withAnchorAt(..)` worked out
            the place of the caret again, the caret would jump to the start of the second line, although your users
            moved only the anchor.
        """
        given : 'A property which holds a selection from position 2 to position 5:'
            var selection = Var.of(TextSelection.between(2, 5))
        and : 'A component whose content area is 100 pixels wide, with the first word in red and the second in green:'
            var box =
                    UI.box()
                    .withStyle( it -> it
                        .size(120, 150)
                        .padding(10)
                        .backgroundColor(WHITE)
                        .text("body", t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .font( f -> f.family("Ubuntu").size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HHHH " ),
                                StyledString.of( f -> f.color(GREEN), "HHHH"  )
                            )
                        )
                    )
                    .withTextSelection("body", selection)
                    .get(JBox)
        and : 'We render the component, find the letters, and let SwingTree write the selection with its rectangles into the property:'
            var image      = Utility.renderSingleComponent(box)
            UI.sync()
            var firstWord  = boundsOfPixelsPaintedIn(image, RED)
            var secondWord = boundsOfPixelsPaintedIn(image, GREEN)
            var before     = selection.get()

        when : 'Your code moves the anchor to the gap between the second and the third green letter, two letters into the second line:'
            var moved = before.withAnchorAt(Position.of(secondWord.x + 2 * secondWord.width / 4, secondWord.centerY))

        then : 'The text was really wrapped, so the second word is below the first word:'
            secondWord.y >= firstWord.y + firstWord.height
        and : 'The anchor moved to position 7, and the caret stayed at position 5, at the end of the first line:'
            moved.anchor() == 7
            moved.caret()  == 5
            moved.isCaretAtLineEnd()
            moved.caretBounds() == before.caretBounds()
            moved.caretBounds().get().location().y() < firstWord.y + firstWord.height
    }

    static int numberOfPixelsPaintedIn( BufferedImage image, Color color ) {
        int count = 0
        for ( int y = 0; y < image.height; y++ )
            for ( int x = 0; x < image.width; x++ )
                if ( boundsOfPixelsPaintedIn(image, color, new Rectangle(x, y, 1, 1)) != null )
                    count++
        return count
    }

    static Rectangle boundsOfPixelsPaintedIn( BufferedImage image, Color color ) {
        return boundsOfPixelsPaintedIn(image, color, new Rectangle(0, 0, image.width, image.height))
    }

    static Rectangle boundsOfPixelsPaintedIn( BufferedImage image, Color color, Rectangle region ) {
        var area = region.intersection(new Rectangle(0, 0, image.width, image.height))
        Rectangle bounds = null
        for ( int y = area.y; y < area.y + area.height; y++ )
            for ( int x = area.x; x < area.x + area.width; x++ ) {
                var pixel = new Color(image.getRGB(x, y), true)
                var isPaintedInColor = pixel.alpha >= 160 &&
                                       Math.abs(pixel.red   - color.red  ) <= 40 &&
                                       Math.abs(pixel.green - color.green) <= 40 &&
                                       Math.abs(pixel.blue  - color.blue ) <= 40
                if ( isPaintedInColor ) {
                    if ( bounds == null ) bounds = new Rectangle(x, y, 1, 1)
                    else bounds.add(new Rectangle(x, y, 1, 1))
                }
            }
        return bounds
    }
}

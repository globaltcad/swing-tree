package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.Configurator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.text.DefaultEditorKit;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class SelectableText extends MouseAdapter implements FocusListener
{
    private static final Logger log = LoggerFactory.getLogger(SelectableText.class);
    private static final Cursor TEXT_CURSOR = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    private static final int    LONG_PRESS_MILLIS    = 500;
    private static final int    LONG_PRESS_TOLERANCE = 5;

    private final JComponent                             _owner;
    private final UI.Layer                               _layer;
    private final String                                 _textName;
    private final Consumer<Configurator<TextSelection>>  _propertyUpdateDispatcher; // The passed configurator is executed on the app thread!
    private final int                                    _farJumpModifier;
    private TextSelection                                _shown;
    private boolean                                      _showsTextCursor;
    private boolean                                      _focusLost;
    private float                                        _verticalMoveX = Float.NaN;
    private @Nullable Timer                              _longPress;
    private int                                          _pressX;
    private int                                          _pressY;
    private @Nullable Cursor                             _cursorOutsideText;
    private boolean                                      _resolutionQueued;
    private boolean                                      _warnedAboutSharedProperty;
    private String                                       _lastForeignComponent = "";
    private @Nullable StyleConf                          _styleOfCarets;
    private @Nullable BoxModelConf                       _boxModelOfCarets;
    private @Nullable Font                               _fontOfCarets;
    private List<?>                                      _layoutOfCarets = Collections.emptyList();
    private CaretTable                                   _carets = CaretTable.EMPTY;

    SelectableText(
        final JComponent                             owner,
        final UI.Layer                               layer,
        final String                                 textName,
        final TextSelection                          shown,
        final Consumer<Configurator<TextSelection>>  updateProperty
    ) {
        _owner                    = owner;
        _layer                    = layer;
        _textName                 = textName;
        _shown                    = shown;
        _propertyUpdateDispatcher = updateProperty;
        owner.addMouseListener(this);
        owner.addMouseMotionListener(this);
        owner.addFocusListener(this);
        owner.addKeyListener(new KeyboardNavigation());
        owner.setTransferHandler(new Copying());
        final int shortcutKey = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK : _menuShortcutKey();
        _farJumpModifier = ( shortcutKey & ( InputEvent.META_MASK | InputEvent.META_DOWN_MASK ) ) != 0 ? InputEvent.ALT_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
        final Action copy = TransferHandler.getCopyAction();
        owner.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey), copy.getValue(Action.NAME));
        owner.getActionMap().put(copy.getValue(Action.NAME), copy);
        owner.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcutKey), DefaultEditorKit.selectAllAction);
        owner.getActionMap().put(DefaultEditorKit.selectAllAction, new AbstractAction() {
            @Override public void actionPerformed( ActionEvent e ) {
                if ( _shown.isEnabled() )
                    _select(TextSelection.between(0, _carets().text().length()));
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static int _menuShortcutKey() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    }

    private boolean _jumpsFar( KeyEvent e ) {
        return ( e.getModifiersEx() & _farJumpModifier ) != 0;
    }

    TextSelectionConf styled( TextSelectionConf look ) {
        if ( !_resolutionQueued && UI.thisIsUIThread() ) {
            _resolutionQueued = true;
            UI.runLater(this::_resolve);
        }
        final TextSelectionConf withRange = look.range(_shown);
        return _focusLost ? withRange.unfocused() : withRange;
    }

    @Override
    public void focusGained( FocusEvent e ) {
        _setFocusLost(false);
    }

    @Override
    public void focusLost( FocusEvent e ) {
        _cancelLongPress();
        _setFocusLost(true);
    }

    private void _setFocusLost( boolean focusLost ) {
        if ( focusLost == _focusLost )
            return;
        _focusLost = focusLost;
        _owner.repaint();
    }

    void show( TextSelection selection ) {
        _warnIfPaintedByAnotherComponent(selection.carets().component());
        if ( selection.equals(_shown) )
            return;
        _shown = selection;
        _verticalMoveX = Float.NaN;
        _owner.repaint();
    }

    private void _warnIfPaintedByAnotherComponent( String otherComponent ) {
        final String thisComponent = _description();
        if ( _warnedAboutSharedProperty || otherComponent.isEmpty() || otherComponent.equals(thisComponent) )
            return;
        final boolean replacesOwnSelection = _shown.carets().component().equals(thisComponent);
        if ( !replacesOwnSelection || !otherComponent.equals(_lastForeignComponent) ) {
            _lastForeignComponent = otherComponent;
            return;
        }
        _warnedAboutSharedProperty = true;
        log.warn(SwingTree.get().logMarker(),
            "The same 'Var<TextSelection>' is bound with 'withTextSelection' to two components: {} and {}. " +
            "After every paint, each of the two components writes the pixel coordinates and the selected text " +
            "of its own text into this property. So the property switches back and forth between the values " +
            "of the two components, and it fires a change event every time. " +
            "Give every component its own 'Var<TextSelection>'.",
            thisComponent, otherComponent
        );
    }

    private String _description() {
        final @Nullable String name = _owner.getName();
        return _owner.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(_owner)) + ( name == null ? "" : " named '" + name + "'" );
    }

    @SuppressWarnings("ReferenceEquality")
    private void _resolve() {
        _resolutionQueued = false;
        final CaretTable    carets   = _carets();
        final TextSelection resolved = _shown.resolvedWith(carets);
        if ( resolved == _shown )
            return;
        if ( !resolved.equals(_shown) )
            _propertyUpdateDispatcher.accept(current -> current.resolvedWith(carets) );
        _shown = resolved;
    }

    @Override
    public void mousePressed( MouseEvent e ) {
        if ( !SwingUtilities.isLeftMouseButton(e) || !_shown.isEnabled() )
            return;
        final CaretTable carets   = _carets();
        final int        position = carets.positionAt(e.getX(), e.getY());
        if ( position < 0 )
            return;
        _owner.requestFocusInWindow();
        _verticalMoveX = Float.NaN;
        _cancelLongPress();
        final TextSelectionConf look   = _look();
        final int               clicks = e.getClickCount();
        if ( clicks >= 3 && look.selectParagraphOnTripleClick() )
            _select(_paragraphAround(carets.text(), carets.characterAt(e.getX(), e.getY())));
        else if ( clicks == 2 && look.selectWordOnDoubleClick() )
            _select(_wordAround(carets.text(), carets.characterAt(e.getX(), e.getY())));
        else if ( e.isShiftDown() )
            _select(_shown.withCaret(position).withCaretAtLineEnd(carets.endsLineAt(e.getX(), e.getY())));
        else {
            _select(TextSelection.caretAt(position).withCaretAtLineEnd(carets.endsLineAt(e.getX(), e.getY())));
            if ( clicks == 1 && look.selectWordOnLongPress() )
                _startLongPress(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased( MouseEvent e ) {
        if ( SwingUtilities.isLeftMouseButton(e) )
            _cancelLongPress();
    }

    private void _startLongPress( int x, int y ) {
        _pressX = x;
        _pressY = y;
        if ( _longPress == null ) {
            _longPress = new Timer(LONG_PRESS_MILLIS, event -> _selectWordAtPress());
            _longPress.setRepeats(false);
        }
        _longPress.restart();
    }

    private void _cancelLongPress() {
        if ( _longPress != null )
            _longPress.stop();
    }

    private void _selectWordAtPress() {
        if ( !_shown.isEnabled() )
            return;
        final CaretTable carets = _carets();
        _select(_wordAround(carets.text(), carets.characterAt(_pressX, _pressY)));
    }

    @Override
    public void mouseDragged( MouseEvent e ) {
        if ( !SwingUtilities.isLeftMouseButton(e) || !_shown.isEnabled() )
            return;
        if ( Math.abs(e.getX() - _pressX) > UI.scale(LONG_PRESS_TOLERANCE) || Math.abs(e.getY() - _pressY) > UI.scale(LONG_PRESS_TOLERANCE) )
            _cancelLongPress();
        final CaretTable carets   = _carets();
        final int        position = carets.positionAt(e.getX(), e.getY());
        if ( position >= 0 )
            _select(_shown.withCaret(position).withCaretAtLineEnd(carets.endsLineAt(e.getX(), e.getY())));
    }

    @Override
    public void mouseMoved( MouseEvent e ) {
        final boolean overText = _shown.isEnabled() && _carets().isOverText(e.getX(), e.getY());
        if ( overText == _showsTextCursor )
            return;
        _showsTextCursor = overText;
        if ( overText ) {
            _cursorOutsideText = _owner.isCursorSet() ? _owner.getCursor() : null;
            _owner.setCursor(TEXT_CURSOR);
        } else {
            _owner.setCursor(_cursorOutsideText);
            _cursorOutsideText = null;
        }
    }

    private void _select( TextSelection selection ) {
        final TextSelection resolved = selection.resolvedWith(_carets());
        if ( resolved.equals(_shown) )
            return;
        _shown = resolved;
        _owner.repaint();
        // We send the modification to the application thread:
        _propertyUpdateDispatcher.accept(current -> resolved.withEnabled(current.isEnabled()) );
    }

    private void _navigate( KeyEvent e ) {
        if ( !_shown.isEnabled() || !_look().navigateWithKeyboard() )
            return;
        final CaretTable    carets  = _carets();
        final TextSelection current = _shown.resolvedWith(carets);
        final String        text    = carets.text();
        final int           caret   = current.caret();
        final boolean       extend  = e.isShiftDown();
        final boolean       far     = _jumpsFar(e);
        final int           line    = carets.lineOf(caret, current.isCaretAtLineEnd());
        final int           key     = e.getKeyCode();
        if ( key != KeyEvent.VK_UP && key != KeyEvent.VK_DOWN )
            _verticalMoveX = Float.NaN;
        final TextSelection target;
        switch ( key ) {
            case KeyEvent.VK_LEFT:
                if ( !extend && !far && !current.isEmpty() )
                    target = TextSelection.caretAt(current.start());
                else
                    target = _moved(current, far ? _wordStartBefore(text, caret) : Math.max(0, caret - 1), false, extend);
                break;
            case KeyEvent.VK_RIGHT:
                if ( !extend && !far && !current.isEmpty() )
                    target = TextSelection.caretAt(current.end()).withCaretAtLineEnd(true);
                else
                    target = _moved(current, far ? _wordEndAfter(text, caret) : Math.min(text.length(), caret + 1), true, extend);
                break;
            case KeyEvent.VK_HOME:
                target = _moved(current, far ? 0 : line < 0 ? caret : carets.startOfLine(line), false, extend);
                break;
            case KeyEvent.VK_END:
                target = _moved(current, far ? text.length() : line < 0 ? caret : carets.endOfLine(line), true, extend);
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
                if ( line < 0 )
                    return;
                target = _movedToNeighbourLine(current, carets, line, key == KeyEvent.VK_UP ? -1 : 1, extend);
                break;
            default:
                return;
        }
        e.consume();
        _select(target);
    }

    private TextSelection _movedToNeighbourLine( TextSelection current, CaretTable carets, int line, int direction, boolean extend ) {
        if ( Float.isNaN(_verticalMoveX) )
            _verticalMoveX = carets.xOf(line, current.caret());
        final int neighbour = carets.lineNextTo(line, direction, _verticalMoveX);
        if ( neighbour < 0 )
            return _moved(current, direction < 0 ? 0 : carets.text().length(), direction > 0, extend);
        final int position = carets.positionOnLine(neighbour, _verticalMoveX);
        return _moved(current, position, carets.endsLine(neighbour, position), extend);
    }

    private static TextSelection _moved( TextSelection current, int caret, boolean atLineEnd, boolean extend ) {
        return ( extend ? current.withCaret(caret) : TextSelection.caretAt(caret) ).withCaretAtLineEnd(atLineEnd);
    }

    private static int _wordStartBefore( String text, int position ) {
        final BreakIterator words = BreakIterator.getWordInstance();
        words.setText(text);
        for ( int end = position, start = words.preceding(position); start != BreakIterator.DONE; end = start, start = words.previous() )
            if ( _holdsLetterOrDigit(text, start, end) )
                return start;
        return 0;
    }

    private static int _wordEndAfter( String text, int position ) {
        final BreakIterator words = BreakIterator.getWordInstance();
        words.setText(text);
        for ( int start = position, end = words.following(position); end != BreakIterator.DONE; start = end, end = words.next() )
            if ( _holdsLetterOrDigit(text, start, end) )
                return end;
        return text.length();
    }

    private static boolean _holdsLetterOrDigit( String text, int start, int end ) {
        for ( int i = start; i < end; i++ )
            if ( Character.isLetterOrDigit(text.charAt(i)) )
                return true;
        return false;
    }

    private TextSelectionConf _look() {
        final @Nullable TextConf text = ComponentBackend.powering(_owner).getStyle().layer(_layer).texts().get(_textName);
        return text == null ? TextSelectionConf.none() : text.selection();
    }

    private static TextSelection _paragraphAround( String text, int character ) {
        if ( character < 0 )
            return TextSelection.caretAt(0);
        final int start = text.lastIndexOf('\n', character - 1) + 1;
        final int end   = text.indexOf('\n', character);
        return TextSelection.between(start, end < 0 ? text.length() : end);
    }

    private static TextSelection _wordAround( String text, int character ) {
        if ( text.isEmpty() )
            return TextSelection.caretAt(0);
        final BreakIterator words = BreakIterator.getWordInstance();
        words.setText(text);
        final int end   = words.following(Math.max(0, Math.min(character, text.length() - 1)));
        final int start = words.previous();
        return TextSelection.between(start, end);
    }

    @SuppressWarnings("ReferenceEquality")
    private CaretTable _carets() {
        final ComponentBackend<?> backend  = ComponentBackend.powering(_owner);
        final StyleConf           style    = backend.getStyle();
        final BoxModelConf        boxModel = backend.getBoxModelConf();
        final Font                font     = _owner.getFont();
        if ( style == _styleOfCarets && boxModel == _boxModelOfCarets && font == _fontOfCarets )
            return _carets;
        _styleOfCarets    = style;
        _boxModelOfCarets = boxModel;
        _fontOfCarets     = font;
        final @Nullable TextConf text = style.layer(_layer).texts().get(_textName);
        final List<?> layout = Arrays.asList(text == null ? null : text.selection(text.selection().caretGeometry()), boxModel, font, UI.scale());
        if ( !layout.equals(_layoutOfCarets) ) {
            _layoutOfCarets = layout;
            _carets = text == null || text.content().isEmpty() ? CaretTable.EMPTY : _caretsOf(text, boxModel, font);
        }
        return _carets;
    }

    private CaretTable _caretsOf( TextConf text, BoxModelConf boxModel, @Nullable Font componentFont ) {
        final Font font = TextLayoutEngine.fontOf(text, boxModel, componentFont);
        return CaretTable.of(_description(), text.paintedText(), TextLayoutEngine.placeText(text, TextLayoutEngine.computeTextBounds(text, boxModel), boxModel, font, _owner.getFontMetrics(font).getFontRenderContext()), text.selection());
    }

    private final class KeyboardNavigation extends KeyAdapter
    {
        @Override
        public void keyPressed( KeyEvent e ) {
            _navigate(e);
        }
    }

    private final class Copying extends TransferHandler
    {
        @Override
        public int getSourceActions( JComponent component ) {
            return COPY;
        }

        @Override
        protected @Nullable Transferable createTransferable( JComponent component ) {
            if ( !_shown.isEnabled() || !_look().copyToClipboard() )
                return null;
            final String selected = _shown.resolvedWith(_carets()).selectedText();
            return selected.isEmpty() ? null : new StringSelection(selected);
        }
    }
}

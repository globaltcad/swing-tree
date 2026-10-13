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
import javax.swing.TransferHandler;
import javax.swing.text.DefaultEditorKit;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class SelectableText extends MouseAdapter
{
    private static final Logger log = LoggerFactory.getLogger(SelectableText.class);
    private static final Cursor TEXT_CURSOR = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

    private final JComponent                             _owner;
    private final UI.Layer                               _layer;
    private final String                                 _textName;
    private final Consumer<Configurator<TextSelection>>  _propertyUpdateDispatcher; // The passed configurator is executed on the app thread!
    private TextSelection                                _shown;
    private boolean                                      _showsTextCursor;
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
        owner.setTransferHandler(new Copying());
        final int shortcutKey = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK : _menuShortcutKey();
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

    TextSelectionConf styled( TextSelectionConf look ) {
        if ( !_resolutionQueued && UI.thisIsUIThread() ) {
            _resolutionQueued = true;
            UI.runLater(this::_resolve);
        }
        return look.range(_shown);
    }

    void show( TextSelection selection ) {
        _warnIfPaintedByAnotherComponent(selection.carets().component());
        if ( selection.equals(_shown) )
            return;
        _shown = selection;
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
        if ( e.getClickCount() == 2 )
            _select(_wordAround(carets.text(), carets.characterAt(e.getX(), e.getY())));
        else if ( e.isShiftDown() )
            _select(_shown.withCaret(position));
        else
            _select(TextSelection.caretAt(position));
    }

    @Override
    public void mouseDragged( MouseEvent e ) {
        if ( !SwingUtilities.isLeftMouseButton(e) || !_shown.isEnabled() )
            return;
        final int position = _carets().positionAt(e.getX(), e.getY());
        if ( position >= 0 )
            _select(_shown.withCaret(position));
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
        final List<?> layout = Arrays.asList(text == null ? null : text.selection(TextSelectionConf.none()), boxModel, font, UI.scale());
        if ( !layout.equals(_layoutOfCarets) ) {
            _layoutOfCarets = layout;
            _carets = text == null || text.content().isEmpty() ? CaretTable.EMPTY : _caretsOf(text, boxModel, font);
        }
        return _carets;
    }

    private CaretTable _caretsOf( TextConf text, BoxModelConf boxModel, @Nullable Font componentFont ) {
        final Font font = TextLayoutEngine.fontOf(text, boxModel, componentFont);
        return CaretTable.of(_description(), text.paintedText(), TextLayoutEngine.placeText(text, TextLayoutEngine.computeTextBounds(text, boxModel), boxModel, font, _owner.getFontMetrics(font).getFontRenderContext()));
    }

    private final class Copying extends TransferHandler
    {
        @Override
        public int getSourceActions( JComponent component ) {
            return COPY;
        }

        @Override
        protected @Nullable Transferable createTransferable( JComponent component ) {
            if ( !_shown.isEnabled() )
                return null;
            final String selected = _shown.resolvedWith(_carets()).selectedText();
            return selected.isEmpty() ? null : new StringSelection(selected);
        }
    }
}

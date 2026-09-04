package examples.laf;

import examples.laf.SwingTreeLookAndFeel.PopupWindowMode;
import swingtree.UI;
import swingtree.style.ComponentExtension;

import javax.swing.JComponent;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.util.Objects;

/**
 *  The {@link PopupFactory} which {@link SwingTreeLookAndFeel} installs, so that a popup Swing puts
 *  in a window of its own looks like one that fits inside the application window.
 *  <p>
 *  A popup that fits is added to the application window's {@link javax.swing.JLayeredPane}, and
 *  everything a style rule paints outside the sheet - the {@code margin} ring, the rounded corners,
 *  the drop shadow - falls on the application underneath. A popup that does not fit is packed into
 *  an undecorated {@code javax.swing.Popup$HeavyWeightWindow} whose opaque background covers that
 *  whole ring, which turns a rounded shadowed sheet into a square one on a light slab. A style rule
 *  cannot correct it, because the window is one level above the component a rule is handed.
 *  <p>
 *  So this factory dresses the window instead, in whichever way
 *  {@link SwingTreeLookAndFeel#popupWindowMode()} says the platform supports. The style rules are
 *  untouched under every mode: the sheet keeps the margin, the radius and the shadow it has
 *  in-frame, and only the window behind it changes.
 *  <ul>
 *      <li>{@link PopupWindowMode#TRANSLUCENT} - the window background is a fully transparent
 *          colour, so the ring shows the desktop and the shadow falls on it.</li>
 *      <li>{@link PopupWindowMode#SHAPED} - the window is clipped to the sheet's own
 *          {@link UI.ComponentArea#BODY}, so the corners are cut out of the window. The shadow is
 *          painted outside that shape and is clipped away with it.</li>
 *      <li>{@link PopupWindowMode#OPAQUE} - the window keeps its rectangle, filled with
 *          {@link SwingTreeLookAndFeel.Palette#background()} so the ring is the colour the sheet
 *          would have stood on in-frame.</li>
 *  </ul>
 *  The last two both fill the window with the palette's ground colour for a second reason: a preset
 *  that paints the sheet in a translucent colour, as {@link Styles.Glassmorphic} does, composites
 *  it against the window background, and against the default white every such sheet washes out.
 */
final class SwingTreePopupFactory extends PopupFactory
{
    /** Records on the popup which mode {@link #getPopup} resolved for it, so that a style rule can
     *  ask {@link SwingTreeLookAndFeel#popupWindowModeOf(JComponent)} what it is painting into. */
    static final String MODE_KEY = "SwingTree.popupWindowMode";

    private static final Color FULLY_TRANSPARENT = new Color(0, 0, 0, 0);

    private final PopupFactory _replaced;

    SwingTreePopupFactory( PopupFactory replaced ) {
        _replaced = Objects.requireNonNull(replaced);
    }

    /** @return the factory this one was installed in front of, which uninstalling restores */
    PopupFactory replaced() { return _replaced; }

    @Override
    public Popup getPopup( Component owner, Component contents, int x, int y ) {
        Popup popup = _replaced.getPopup(owner, contents, x, y);
        if ( !(contents instanceof JComponent) )
            return popup;
        JComponent sheet = (JComponent) contents;
        Window host = SwingUtilities.getWindowAncestor(sheet);
        Window ownerWindow = ( owner == null ? null : SwingUtilities.getWindowAncestor(owner) );
        if ( host == null || host == ownerWindow ) {
            sheet.putClientProperty(MODE_KEY, PopupWindowMode.IN_FRAME);
            return popup;
        }
        PopupWindowMode mode = _dress(host);
        sheet.putClientProperty(MODE_KEY, mode);
        return mode == PopupWindowMode.SHAPED ? new ShapedPopup(popup, sheet) : popup;
    }

    /**
     *  Gives the popup's window the background its mode calls for and returns the mode that was
     *  achieved: a platform which advertises per-pixel translucency and then refuses it makes
     *  {@link Window#setBackground(Color)} throw, and the popup falls back to
     *  {@link PopupWindowMode#SHAPED}.
     *  <p>
     *  The shape cannot be set here, because it has to match the sheet and the sheet has no size
     *  until the {@code pack()} inside {@link Popup#show()} has run.
     */
    private static PopupWindowMode _dress( Window host ) {
        PopupWindowMode mode = SwingTreeLookAndFeel.popupWindowMode();
        if ( mode == PopupWindowMode.TRANSLUCENT ) {
            try {
                Color current = host.getBackground();
                if ( current == null || current.getAlpha() != 0 )
                    host.setBackground(FULLY_TRANSPARENT);
                return PopupWindowMode.TRANSLUCENT;
            } catch ( RuntimeException e ) {
                mode = PopupWindowMode.SHAPED;
            }
        }
        Color ground = SwingTreeLookAndFeel.palette().background();
        if ( !ground.equals(host.getBackground()) )
            host.setBackground(ground);
        if ( mode == PopupWindowMode.SHAPED )
            return PopupWindowMode.SHAPED;
        _reshape(host, null);
        return PopupWindowMode.OPAQUE;
    }

    /**
     *  Clips {@code host} to {@code shape}, or removes an earlier clip when {@code shape} is
     *  {@code null}. Removing it matters because {@code PopupFactory} keeps a small cache of
     *  heavyweight windows per owner and hands the same one to the next popup, which is a
     *  different size and may be resolved to a different mode.
     *
     * @return {@code true} if the platform accepted the call
     */
    private static boolean _reshape( Window host, Shape shape ) {
        try {
            host.setShape(shape);
            return true;
        } catch ( RuntimeException e ) {
            return false;
        }
    }

    /**
     *  Cuts the corners out of the popup's window once that window has its final size, which is
     *  after {@link Popup#show()} rather than when the popup was handed out.
     *  <p>
     *  The shape is the sheet's own {@link UI.ComponentArea#BODY}, so that the window edge and the
     *  painted edge are one curve at whatever corner radius the installed preset chose.
     */
    private static final class ShapedPopup extends Popup
    {
        private final Popup      _delegate;
        private final JComponent _sheet;

        ShapedPopup( Popup delegate, JComponent sheet ) {
            _delegate = delegate;
            _sheet    = sheet;
        }

        @Override
        public void show() {
            _delegate.show();
            Window host = SwingUtilities.getWindowAncestor(_sheet);
            if ( host != null )
                _reshape(host, _sheetShapeIn(host));
        }

        @Override
        public void hide() {
            _delegate.hide();
        }

        private Shape _sheetShapeIn( Window host ) {
            ComponentExtension<?> extension = ComponentExtension.from(_sheet);
            extension.gatherApplyAndInstallStyle(true);
            Shape body = extension.getComponentArea(UI.ComponentArea.BODY).orElse(null);
            if ( body == null )
                return null;
            Point origin = SwingUtilities.convertPoint(_sheet, 0, 0, host);
            return AffineTransform.getTranslateInstance(origin.x, origin.y).createTransformedShape(body);
        }
    }
}

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
 *  The {@link PopupFactory} which {@link SwingTreeLookAndFeel} installs, so that a popup Swing has
 *  to put in a window of its own looks like the popups which fit inside the application window.
 *
 *  <h2>What goes wrong without it</h2>
 *  A popup that fits inside its owner's window is added to that window's
 *  {@link javax.swing.JLayeredPane}, and everything a style rule paints - the {@code margin} ring,
 *  the rounded corners, the drop shadow - falls on the application underneath. A popup that does
 *  not fit is given a {@code javax.swing.Popup$HeavyWeightWindow} instead: an undecorated
 *  {@link javax.swing.JWindow} packed around the popup, whose background is opaque and covers the
 *  whole rectangle. The margin ring is then painted over in that background colour, which turns
 *  the rounded shadowed sheet into a square one sitting on a light slab. No style rule can correct
 *  this, because the window is one level above the component a rule is handed.
 *
 *  <h2>What this factory does instead</h2>
 *  It dresses that window, in whichever of the three ways
 *  {@link SwingTreeLookAndFeel#popupWindowMode()} reports the platform supports. Every mode leaves
 *  the style rules alone: the popup keeps the margin, the radius and the shadow it is painted with
 *  in-frame, and only the window behind it changes.
 *  <ul>
 *      <li>{@link PopupWindowMode#TRANSLUCENT} - the window background is set to a fully
 *          transparent colour, so the margin ring shows the desktop and the shadow falls on it.</li>
 *      <li>{@link PopupWindowMode#SHAPED} - the window is clipped to the popup's own
 *          {@link UI.ComponentArea#BODY}, so the corners are cut out of the window itself. The
 *          shadow is painted into the ring outside that shape and is therefore clipped away.</li>
 *      <li>{@link PopupWindowMode#OPAQUE} - the window keeps its rectangle, and is filled with
 *          {@link SwingTreeLookAndFeel.Palette#background()} so that the ring is the colour the
 *          popup would have been standing on in-frame rather than an unrelated white.</li>
 *  </ul>
 *  The window is filled with the palette's ground colour in the two opaque modes for a second
 *  reason: a preset which paints the popup in a translucent colour - {@link GlassmorphicPreset}
 *  does - composites it against whatever the window background is, and against the default white
 *  every such sheet washes out.
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
     *  Gives the popup's own window the background the resolved mode calls for, and reports the
     *  mode that was actually achieved. A platform which advertises per-pixel translucency and
     *  then refuses it leaves {@link Window#setBackground(Color)} throwing, and the popup is
     *  dressed as a {@link PopupWindowMode#SHAPED} one instead.
     *  <p>
     *  Only the background is set here. The shape cannot be, because a shape has to match the
     *  popup and the popup has no size until {@link Popup#show()} has packed the window around it.
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
     *  Cuts the corners out of a heavyweight popup's window once the window exists at its final
     *  size.
     *  <p>
     *  The shape has to be the popup's own {@link UI.ComponentArea#BODY}, so that the window edge
     *  and the painted edge are the same curve; reading it off the style engine is also what keeps
     *  the corner radius correct across presets which each choose their own. That area is only
     *  calculated once the popup has a size, and the popup is sized by the {@code pack()} inside
     *  {@link Popup#show()} - hence the work happens after the delegate has been shown rather than
     *  when the popup was handed out.
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

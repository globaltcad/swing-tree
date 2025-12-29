package swingtree.style;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.layout.Size;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.util.Objects;

/**
 *  A wrapper for {@link ImageIcon} that automatically scales the image to the
 *  current {@link UI#scale()} value defined in the current {@link swingtree.SwingTree}
 *  library context singleton.<br>
 *
 */
public final class ScalableImageIcon extends ImageIcon
{
    private static final Logger log = LoggerFactory.getLogger(ScalableImageIcon.class);

    /**
     *  A factory method that creates a new {@link ScalableImageIcon} that will render
     *  the supplied {@link ImageIcon} using the given base size scaled according to the current DPI settings.
     *  <p>
     *      If the given {@link ImageIcon} is already a {@link ScalableImageIcon},
     *      a new instance will be created from the existing one
     *      using {@link ScalableImageIcon#withSize(Size)}.
     *  </p>
     * @param size The size to render the icon at.
     * @param icon The icon to render.
     * @return A new {@link ScalableImageIcon} that will render the image
     *          scaled according to the current DPI settings.
     */
    public static ScalableImageIcon of( Size size, ImageIcon icon ) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(icon);
        if ( icon instanceof ScalableImageIcon )
            return ((ScalableImageIcon) icon).withSize(size);
        return new ScalableImageIcon(size, icon);
    }

    private final ImageIcon _sourceIcon;
    private final Size      _relativeScale;
    private final Size      _baseSize;

    private ImageIcon _scaled;
    private float     _currentScale;

    private ScalableImageIcon( Size size, ImageIcon original ) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(original);
        Size relativeScale = Size.unknown();
        double targetWidth = -1;
        double targetHeight = -1;
        try {
            double originalIconWidth = original.getIconWidth();
            double originalIconHeight = original.getIconHeight();
            double ratio = originalIconWidth / originalIconHeight;
            if (size.hasPositiveWidth() && size.hasPositiveHeight()) {
                targetWidth = size.width().orElse(0.0f);
                targetHeight = size.height().orElse(0.0f);
            } else if (size.hasPositiveWidth()) {
                targetWidth = size.width().orElse(0.0f);
                targetHeight = targetWidth / ratio;
            } else if (size.hasPositiveHeight()) {
                targetHeight = size.height().orElse(0.0f);
                targetWidth = targetHeight * ratio;
            } else {
                targetWidth = originalIconWidth;
                targetHeight = originalIconHeight;
            }
            relativeScale = Size.of(targetWidth / originalIconWidth, targetHeight / originalIconHeight);
        } catch ( Exception e ) {
            log.error(SwingTree.get().logMarker(), "An error occurred while calculating the size of a ScalableImageIcon.", e);
        }
        _baseSize      = Size.of(targetWidth, targetHeight);
        _sourceIcon = original;
        _relativeScale = relativeScale;
        _currentScale  = UI.scale();
        _scaled        = _scaleTo(_currentScale, _relativeScale, original);
    }

    private ImageIcon _scaleTo( float scale, Size relativeScale, ImageIcon original ) {
        if ( !_relativeScale.hasPositiveWidth() || !_relativeScale.hasPositiveHeight() )
            return original;
        try {
            int width  = Math.round(original.getIconWidth()  * scale * relativeScale.width().orElse(0.0f));
            int height = Math.round(original.getIconHeight() * scale * relativeScale.height().orElse(0.0f));
            Image originalImage = original.getImage();
            if ( width == original.getIconWidth() && height == original.getIconHeight() )
                return original;
            if ( width <= 0 || height <= 0 ) {
                // We create the smallest possible image to avoid exceptions.
                return new ImageIcon(new ImageIcon(new byte[0]).getImage());
            }
            return new ImageIcon(originalImage.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
        } catch ( Exception e ) {
            log.error(SwingTree.get().logMarker(), "An error occurred while scaling an image icon.", e);
            return original;
        }
    }

    private void _updateScale() {
        float newScale = UI.scale();
        if ( newScale != _currentScale ) {
            _scaled = _scaleTo(newScale, _relativeScale, _sourceIcon);
            _currentScale = newScale;
        }
    }

    /**
     *  Returns a new {@link ScalableImageIcon} that will render the image
     *  at the given size.<br>
     *  <p>
     *      Note that the returned icon will be a new instance and will not
     *      affect the current icon.
     *  </p>
     * @param size The size to render the icon at.
     * @return A new {@link ScalableImageIcon} that will render the image
     *  at the given size.
     */
    public ScalableImageIcon withSize( Size size ) {
        return new ScalableImageIcon(size, _sourceIcon);
    }

    /**
     *  Exposes the width of the icon, or -1 if the icon does not have a fixed width.<br>
     *  <b>
     *      Note that the returned width is dynamically scaled according to
     *      the current {@link swingtree.UI#scale()} value.
     *      This is to ensure that the icon is rendered at the correct size
     *      according to the current DPI settings.
     *      If you want the unscaled width, use {@link #getBaseWidth()}.
     *  </b>
     * @return The width of the icon, or -1 if the icon does not have a width.
     */
    @Override
    public int getIconWidth() {
        _updateScale();
        return _scaled.getIconWidth();
    }

    /**
     *  Exposes the height of the icon, or -1 if the icon does not have a fixed height.<br>
     *  <b>
     *      Note that the returned height is dynamically scaled according to
     *      the current {@link swingtree.UI#scale()} value.
     *      This is to ensure that the icon is rendered at the correct size
     *      according to the current DPI settings.
     *      If you want the unscaled height, use {@link #getBaseHeight()}.
     *  </b>
     * @return The height of the icon, or -1 if the icon does not have a height.
     */
    @Override
    public int getIconHeight() {
        _updateScale();
        return _scaled.getIconHeight();
    }

    /**
     *  Returns the unscaled width of the icon.
     *  This is the width of the icon as it was originally loaded
     *  and is not affected by the current {@link swingtree.UI#scale()} value.<br>
     *  <p>
     *      If you want a width that is more suited for rendering
     *      according to the current DPI settings, use {@link #getIconWidth()}.
     *  </p>
     *
     * @return The unscaled width of the icon.
     */
    public int getBaseWidth() {
        return _baseSize.width().map(Math::round).orElse(0);
    }

    /**
     *  Returns the unscaled height of the icon.
     *  This is the height of the icon as it was originally loaded
     *  and is not affected by the current {@link swingtree.UI#scale()} value.<br>
     *  <p>
     *      If you want a height that is more suited for rendering
     *      according to the current DPI settings, use {@link #getIconHeight()}.
     *  </p>
     *
     * @return The unscaled height of the icon.
     */
    public int getBaseHeight() {
        return _baseSize.height().map(Math::round).orElse(0);
    }

    @Override
    public synchronized void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        _updateScale();
        _scaled.paintIcon(c, g, x, y);
    }

    @Override
    public Image getImage() {
        _updateScale();
        return _scaled.getImage();
    }

    @Override
    public void setImage( Image image ) {
        log.warn(SwingTree.get().logMarker(),
                "Setting the image of a {} is not supported.",
                this.getClass().getSimpleName(), new Throwable("Stack trace for debugging purposes.")
            );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "baseSize=" + _baseSize + ", " +
                    "displaySize=" + Size.of(getIconWidth(), getIconHeight()) + ", " +
                    "scale=" + _currentScale + ", " +
                    "sourceSize=" + Size.of(_sourceIcon.getIconWidth(), _sourceIcon.getIconHeight()) +
                "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this )
            return true;
        if ( obj == null || obj.getClass() != this.getClass() )
            return false;
        ScalableImageIcon other = (ScalableImageIcon) obj;
        return _sourceIcon.equals(other._sourceIcon) &&
               _relativeScale.equals(other._relativeScale) &&
               _baseSize.equals(other._baseSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_sourceIcon, _relativeScale, _baseSize);
    }

}

package swingtree.style;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ImageIcon _original;
    private final Size      _relativeScale;

    private ImageIcon _scaled;
    private float     _currentScale;

    public ScalableImageIcon( Size size, ImageIcon original ) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(original);
        Size relativeScale = Size.unknown();
        try {
            double originalIconWidth = original.getIconWidth();
            double originalIconHeight = original.getIconHeight();
            double ratio = originalIconWidth / originalIconHeight;
            double targetWidth;
            double targetHeight;
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
            log.error("An error occurred while calculating the size of a ScalableImageIcon.", e);
        }
        _original      = original;
        _relativeScale = relativeScale;
        _currentScale  = UI.scale();
        _scaled        = _scaleTo(_currentScale, _relativeScale, original);
    }

    private ImageIcon _scaleTo( float scale, Size relativeScale, ImageIcon original ) {
        if ( !_relativeScale.hasPositiveWidth() || !_relativeScale.hasPositiveHeight() )
            return original;
        try {
            int width  = (int) (original.getIconWidth()  * scale * relativeScale.width().orElse(0.0f));
            int height = (int) (original.getIconHeight() * scale * relativeScale.height().orElse(0.0f));
            Image originalImage = original.getImage();
            if ( width == original.getIconWidth() && height == original.getIconHeight() )
                return original;
            if ( width <= 0 || height <= 0 ) {
                // We create the smallest possible image to avoid exceptions.
                return new ImageIcon(new ImageIcon(new byte[0]).getImage());
            }
            return new ImageIcon(originalImage.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
        } catch ( Exception e ) {
            log.error("An error occurred while scaling an image icon.", e);
            return original;
        }
    }

    private void _updateScale() {
        float newScale = UI.scale();
        if ( newScale != _currentScale ) {
            _scaled = _scaleTo(newScale, _relativeScale, _original);
            _currentScale = newScale;
        }
    }

    public ScalableImageIcon withSize( Size size ) {
        return new ScalableImageIcon(size, _original);
    }

    @Override
    public int getIconWidth() {
        _updateScale();
        return _scaled.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        _updateScale();
        return _scaled.getIconHeight();
    }

    @Override
    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
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
        log.warn("Setting the image of a ScalableImageIcon is not supported.");
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "width=" + getIconWidth() + ", " +
                    "height=" + getIconHeight() + ", " +
                    "scale=" + _currentScale + ", " +
                    "original=" + _original + "" +
                "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this )
            return true;
        if ( obj == null || obj.getClass() != this.getClass() )
            return false;
        ScalableImageIcon other = (ScalableImageIcon) obj;
        return _original.equals(other._original) && _relativeScale.equals(other._relativeScale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_original, _relativeScale);
    }

}

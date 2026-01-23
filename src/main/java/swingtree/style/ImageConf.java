package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.api.IconDeclaration;
import swingtree.layout.Size;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;

/**
 *  This class represents the style of an image which can be drawn onto a component area,
 *  typically the inner area of a component, which is the area enclosed by the component border.
 *  <p>
 *  The following properties with their respective purpose are available:
 *  <br>
 *  <ol>
 *      <li><b>Layer:</b>
 *          The layer onto which the image will be drawn.
 *          Layers exist to determine the order in which something is drawn onto the component.
 *          Here a list of available layers:
 *          <ul>
 *              <li>{@link swingtree.UI.Layer#BACKGROUND}</li>
 *              <li>{@link swingtree.UI.Layer#CONTENT}   </li>
 *              <li>{@link swingtree.UI.Layer#BORDER}    </li>
 *              <li>{@link swingtree.UI.Layer#FOREGROUND}</li>
 *          </ul>
 *      </li>
 *      <li><b>Primer:</b>
 *          The primer color of the image style which will
 *          be used as a filler color for the image background.
 *          It will fill whatever area was specified using
 *          {@link #clipTo(UI.ComponentArea)}. <br>
 *          The default area used for rendering the image
 *          style is {@link swingtree.UI.ComponentArea#BODY}
 *      </li>
 *      <li><b>Image:</b>
 *          The image which will be drawn onto the component,
 *          which may be specified as an instance of {@link Image}, {@link ImageIcon}
 *          or path to an image file (see {@link swingtree.UI#findIcon(String)}).
 *      </li>
 *      <li><b>Placement:</b>
 *          The placement type determines where the image will be drawn onto the component.
 *          The following placement options are available:
 *          <ul>
 *              <li> {@link swingtree.UI.Placement#CENTER} </li>
 *              <li> {@link swingtree.UI.Placement#TOP_LEFT} </li>
 *              <li> {@link swingtree.UI.Placement#TOP_RIGHT} </li>
 *              <li> {@link swingtree.UI.Placement#BOTTOM_LEFT} </li>
 *              <li> {@link swingtree.UI.Placement#BOTTOM_RIGHT} </li>
 *              <li> {@link swingtree.UI.Placement#TOP} </li>
 *              <li> {@link swingtree.UI.Placement#BOTTOM} </li>
 *              <li> {@link swingtree.UI.Placement#LEFT} </li>
 *              <li> {@link swingtree.UI.Placement#RIGHT} </li>
 *          </ul>
 *      </li>
 *      <li><b>Placement Boundary:</b>
 *          The placement boundary refers to one of many rectangular bounding boxes that capture
 *          <b>the transitional bounding lines between different {@link UI.ComponentArea}s in the
 *          box model (margin|border|padding) of a styled component.</b><br>
 *          You can configure it through {@link ImageConf#placementBoundary(UI.ComponentBoundary)}.<br>
 *          The default placement boundary is {@link UI.ComponentBoundary#BORDER_TO_INTERIOR},
 *          <b>which does not honor the padding of the component.</b>
 *          If you want to include the padding int the placement policy of the image,
 *          then you may want to use {@link UI.ComponentBoundary#INTERIOR_TO_CONTENT} to achieve that.
 *      </li>
 *      <li><b>Repeat:</b>
 *          If this flag is set to {@code true}, then the image may be painted
 *          multiple times so that it fills up the entire inner component area.
 *          There will not be a noticeable effect of this flag if the
 *          image already fills out the inner component area (see {@link #autoFit(boolean)}, {@link #size(int, int)}).
 *      </li>
 *      <li><b>Fit-Mode:</b>
 *          If this enum determines how the image will be stretched or shrunk
 *          to fill the inner component area dependent on the specified width and height,
 *          meaning that if the width was not specified explicitly through {@link #width(int)}
 *          then the image will be scaled to fit the inner component width,
 *          and if a height was not specified through {@link #height(int)} then
 *          the image will be scaled to fit the inner component height. <br>
 *          <b>Note that the inner component area is the area enclosed by the border, which
 *          is itself not part of said area!</b>
 *      </li>
 *      <li><b>Width and Height:</b>
 *          These properties allow you to specify the width and height of the image.
 *          If the width or height is not specified, then the image will be drawn
 *          with its original width or height or it will be scaled to fit the inner component area
 *          if {@link #autoFit(boolean)} is set to {@code true}.
 *      </li>
 *      <li><b>Opacity:</b>
 *          This property allows you to specify the opacity of the image.
 *          The opacity must be between 0.0f and 1.0f, where 0.0f means that the image is completely transparent
 *          and 1.0f means that the image is completely opaque.
 *      </li>
 *      <li><b>Padding:</b>
 *          This property allows you to specify the padding of the image.
 *          The padding is the space between the image and the inner component area.
 *          The padding can be specified for each side of the image individually
 *          or for all sides at once.
 *      </li>
 *      <li><b>Offset:</b>
 *          The offset consists of two integers, one for the horizontal offset
 *          and one for the vertical offset. <br>
 *          It allows you to specify the offset of the image from the placement position.
 *          This means that after the image has been placed onto the component,
 *          it will be moved by the specified offset in the horizontal and vertical direction.
 *      </li>
 *      <li><b>Clip Area:</b>
 *          The clip area determines onto which part of the component the image will be drawn.
 *          The following clip areas are available:
 *          <ul>
 *              <li>{@link swingtree.UI.ComponentArea#ALL} -
 *              The entire component, which is the union of all other clip
 *              areas ({@code INTERIOR + EXTERIOR + BORDER + CONTENT}).
 *              </li>
 *              <li>{@link swingtree.UI.ComponentArea#INTERIOR} -
 *              The inner component area, which is defined as {@code ALL - EXTERIOR - BORDER}.
 *              </li>
 *              <li>{@link swingtree.UI.ComponentArea#EXTERIOR} -
 *              The outer component area, which can be expressed as {@code ALL - INTERIOR - BORDER},
 *              or {@code ALL - CONTENT}.
 *              </li>
 *              <li>{@link swingtree.UI.ComponentArea#BORDER} -
 *              The border of the component, which is the area between the inner and outer component area
 *              and which can be expressed as {@code ALL - INTERIOR - EXTERIOR}.
 *              </li>
 *              <li>{@link swingtree.UI.ComponentArea#BODY} -
 *              The body of the component is the inner component area including the border area.
 *              It can be expressed as {@code ALL - EXTERIOR}, or {@code INTERIOR + BORDER}.
 *              </li>
 *          </ul>
 *          <b>Note that the inner/interior component area is the area enclosed by (and excluding) the border,
 *          whereas the exterior component area is the area surrounding the border.
 *          The component body area is the interior/inner component area plus the border.</b>
 *          <p>
 *          The default clip area is {@link swingtree.UI.ComponentArea#BODY}
 *          as this is the area which is most likely the desired bounding area for the image.
 *      </li>
 *  </ol>
 *  <p>
 *  <b>Take a look at the following example:</b>
 *  <pre>{@code
 *      of(component).withStyle( it -> it
 *          .image( image -> image
 *              .layer(Layer.BACKGROUND)
 *              .image(image)
 *              .placement(Placement.CENTER)
 *              .autoFit(false)
 *              .repeat(true)
 *              .primer(Color.CYAN)
 *              .padding(12)
 *          )
 *      );
 *  }</pre>
 *  <p>
 *      This will draw the specified image onto the background layer of the component.
 *      The image will be drawn at the center of the inner component area with a padding of 12,
 *      without being scaled to fit the inner component area, instead the size of the image
 *      will be used. <br>
 *      If it does not fill the entire inner component area based on its size, then
 *      it will be repeated across said area. The primer color serves as a consistent
 *      background color which will leak through the transparent parts of the image.
 *  </p>
 **/
@Immutable
@SuppressWarnings("Immutable")
public final class ImageConf implements Simplifiable<ImageConf>
{
    static final UI.Layer DEFAULT_LAYER = UI.Layer.BACKGROUND;
    private static final ImageConf _NONE = new ImageConf(
                                                null,
                                                null,
                                                UI.Placement.UNDEFINED,
                                                UI.ComponentBoundary.BORDER_TO_INTERIOR,
                                                false,
                                                UI.FitComponent.UNDEFINED,
                                                Size.unknown(),
                                                1.0f,
                                                Outline.none(),
                                                Offset.none(),
                                                UI.ComponentArea.BODY
                                            );

    static ImageConf none() { return _NONE; }

    static ImageConf of(
        @Nullable Color      primer,
        @Nullable ImageIcon  image,
        UI.Placement         placement,
        UI.ComponentBoundary placementBoundary,
        boolean              repeat,
        UI.FitComponent      fitMode,
        Size                 size,
        float                opacity,
        Outline              padding,
        Offset               offset,
        UI.ComponentArea     clipArea
    ) {
        if (
            Objects.equals( primer, _NONE._primer )   &&
            Objects.equals( image , _NONE._image  )   &&
            placement.equals( _NONE._placement ) &&
            repeat   == _NONE._repeat            &&
            fitMode  .equals( _NONE._fitMode   ) &&
            size     .equals( _NONE._size      ) &&
            opacity  == _NONE._opacity           &&
            padding  .equals( _NONE._padding   ) &&
            offset   .equals( _NONE._offset    ) &&
            clipArea .equals( _NONE._clipArea  )
        )
            return _NONE;
        else
            return new ImageConf(primer, image, placement, placementBoundary, repeat, fitMode, size, opacity, padding, offset, clipArea);
    }


    private final @Nullable Color      _primer;
    private final @Nullable ImageIcon  _image;
    private final UI.Placement         _placement;
    private final UI.ComponentBoundary _placementBoundary;
    private final boolean              _repeat;
    private final UI.FitComponent      _fitMode;
    private final Size                 _size;
    private final float                _opacity;
    private final Outline              _padding;
    private final Offset               _offset;
    private final UI.ComponentArea     _clipArea;


    private ImageConf(
        @Nullable Color      primer,
        @Nullable ImageIcon  image,
        UI.Placement         placement,
        UI.ComponentBoundary placementBoundary,
        boolean              repeat,
        UI.FitComponent      fitMode,
        Size                 size,
        float                opacity,
        Outline              padding,
        Offset               offset,
        UI.ComponentArea     clipArea
    ) {
        _primer    = primer;
        _image     = image;
        _placement = Objects.requireNonNull(placement);
        _placementBoundary = Objects.requireNonNull(placementBoundary);
        _repeat    = repeat;
        _fitMode   = Objects.requireNonNull(fitMode);
        _size      = Objects.requireNonNull(size);
        _opacity   = opacity;
        _padding   = Objects.requireNonNull(padding);
        _offset    = Objects.requireNonNull(offset);
        _clipArea  = Objects.requireNonNull(clipArea);
        if ( _opacity < 0.0f || _opacity > 1.0f )
            throw new IllegalArgumentException("transparency must be between 0.0f and 1.0f");
    }

    Optional<Color> primer() { return Optional.ofNullable(_primer); }

    Optional<ImageIcon> image() { return Optional.ofNullable(_image); }

    UI.Placement placement() {
        if ( _placement == UI.Placement.UNDEFINED && _image instanceof SvgIcon )
            return ((SvgIcon) _image).getPreferredPlacement();

        return _placement;
    }

    UI.ComponentBoundary placementBoundary() {
        return _placementBoundary;
    }

    boolean repeat() { return _repeat; }

    UI.FitComponent fitMode() { return _fitMode; }

    Optional<Integer> width() { return _size.width().map(Number::intValue); }

    Optional<Integer> height() { return _size.height().map(Number::intValue); }

    float opacity() { return _opacity; }

    Outline padding() { return _padding; }
    
    int horizontalOffset() { return (int) _offset.x(); }
    
    int verticalOffset() { return (int) _offset.y(); }

    Offset offset() { return _offset; }

    UI.ComponentArea clipArea() { return _clipArea; }

    /**
     *  Here you can specify the <b>primer color of the image style</b> which will be used
     *  as background color rendered before the image is rendered. <br>
     *  Note that the primer color may not necessarily visible if the specified image is
     *  fully opaque, and fills the entire component.
     *
     * @param color The primer color of the image style.
     * @return A new {@link ImageConf} instance with the specified primer color.
     */
    public ImageConf primer( Color color ) {
        Objects.requireNonNull(color, "Use UI.Color.UNDEFINED instead of null to represent the absence of a color.");
        if ( StyleUtil.isUndefinedColor(color) )
            color = null;
        if ( Objects.equals(color, _primer) )
            return this;
        return ImageConf.of(color, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  Here you can specify the <b>image</b> which will be drawn onto the component.
     *  The supplied object must be aa subtype of {@link Image}, like
     *  {@link java.awt.image.BufferedImage} for example.<br>
     *  <p><b>
     *     Please note that using this method will override whatever information
     *     was previously passed to {@link #image(ImageIcon)}, {@link #image(IconDeclaration)},
     *     {@link #image(String)} or {@link #svg(String)}!
     *  </b></p>
     *
     * @param image The image which will be drawn onto the component.
     * @return A new {@link ImageConf} instance with the specified image.
     */
    public ImageConf image( Image image ) {
        return ImageConf.of(_primer, image == null ? null : new ImageIcon(image), _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  Here you can specify the <b>image icon</b> which will be drawn onto the component.
     *  The supplied object must be an instance of {@link ImageIcon} implementation.
     *  So this may also be an {@link SvgIcon} for example, which is a {@link ImageIcon} subtype...<br>
     * <p><b>
     *     Please note that using this method will override whatever information
     *     was previously passed to {@link #svg(String)}, {@link #image(IconDeclaration)},
     *     {@link #image(String)} and {@link #image(Image)}!
     * </b></p>
     *
     * @param image The image icon which will be drawn onto the component.
     * @return A new {@link ImageConf} instance with the specified image.
     */
    public ImageConf image( ImageIcon image ) {
        return ImageConf.of(_primer, image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  Here you can specify an image in the form of an {@link IconDeclaration},
     *  which typically has a <b>{@link IconDeclaration#source()} that holds a path to an image.</b>
     *  If the supplied {@link IconDeclaration} is path based, then it will be resolved
     *  relative to the classpath or as an absolute path. However, the declaration
     *  may also be based on an SVG String as source (see {@link IconDeclaration#ofSvg(String)}).<br>
     *  If the icon could not be found or parsed, then the image will not be drawn.<br>
     * <p><b>
     *     Please note that using this method will override whatever information
     *     was previously passed to {@link #image(ImageIcon)}, {@link #svg(String)},
     *     {@link #image(String)} and {@link #image(Image)}!
     * </b></p>
     *
     * @param image The path to the (icon) image or SVG String in the form of an {@link IconDeclaration}.
     * @return A new {@link ImageConf} instance with the specified icon declaration for resolving an image.
     * @throws NullPointerException If the specified {@code image} is null.
     */
    public ImageConf image( IconDeclaration image ) {
        Objects.requireNonNull(image);
        return image.find().map(this::image).orElse(this);
    }

    /**
     * Use this to specify an image directly in the form of
     * an SVG string in your style. <br>
     * The supplied {@link String} will be parsed and converted
     * into a {@link SvgIcon}, which can render itself onto a component
     * according to your desired configuration...<br>
     * This is equivalent to calling:
     * <pre>{@code
     *     .image(IconDeclaration.ofSvg("..."))
     * }</pre>
     * If the supplied SVG is invalid, then this will
     * result in an empty {@link SvgIcon}, which will simply not render anything.<br>
     * <p><b>
     *     Please note that using this method will override whatever information
     *     was previously passed to {@link #image(ImageIcon)}, {@link #image(IconDeclaration)},
     *     {@link #image(String)} and {@link #image(Image)}!
     * </b></p>
     *
     * @param svg A {@link String} holding an XML based SVG document.
     * @return A new {@link ImageConf} instance with the specified SVG String as source to be rendered as image.
     * @throws NullPointerException If the specified {@code svg} is null.
     */
    public ImageConf svg( String svg ) {
        Objects.requireNonNull(svg);
        return this.image(IconDeclaration.ofSvg(svg));
    }

    /**
     *  Here you can specify the <b>path to the image</b> you want to draw onto the component.
     *  The underlying icon will be loaded, cached and rendered for you automatically.
     *  If the icon could not be found, then the image will not be drawn.
     *  The path is relative to the classpath or may be an absolute path.
     *  (see {@link swingtree.UI#findIcon(String)}).<br>
     * <p><b>
     *     Please note that using this method will override whatever information
     *     was previously passed to {@link #image(ImageIcon)}, {@link #image(IconDeclaration)},
     *     {@link #svg(String)} and {@link #image(Image)}!
     * </b></p>
     *
     * @param path The path to the (icon) image.
     * @return A new {@link ImageConf} instance with the specified image.
     * @throws NullPointerException If the specified {@code path} is null.
     */
    public ImageConf image( String path ) {
        Objects.requireNonNull(path);
        return image(IconDeclaration.of(path));
    }

    /**
     *  Here you can specify the <b>placement</b> of the image onto the component.
     *  The default placement is {@link swingtree.UI.Placement#CENTER}. <br>
     *  Here a list of available options and their effect:
     *  <ul>
     *      <li>{@link swingtree.UI.Placement#CENTER} -
     *          The image will be drawn at the center of the component.
     *          So the center of the image will be at the center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#TOP_LEFT} -
     *          The image will be drawn beginning at the top left corner of the inner component area.
     *          So the top left corner of the image will be in the top left corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#TOP_RIGHT} -
     *          The image will be placed in the top right corner of the inner component area.
     *          So the top right corner of the image will be in the top right corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#BOTTOM_LEFT} -
     *          The image will be drawn in the bottom left corner of the inner component area.
     *          So the bottom left corner of the image will be in the bottom left corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#BOTTOM_RIGHT} -
     *          The image will be drawn in the bottom right corner of the inner component area.
     *          So the bottom right corner of the image will be in the bottom right corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#TOP} -
     *          The image will be drawn in the top center of the inner component area.
     *          So the top center of the image will be in the top center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#BOTTOM} -
     *          The image will be drawn in the bottom center of the inner component area.
     *          So the bottom center of the image will be in the bottom center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#LEFT} -
     *          The image will be drawn in the left center of the inner component area.
     *          So the left center of the image will be in the left center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#RIGHT} -
     *          The image will be drawn in the right center of the inner component area.
     *          So the right center of the image will be in the right center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#UNDEFINED} -
     *          The image will be drawn at a position which is determined by other
     *          factors such as the image size and the component size.
     *  </ul>
     *
     * @param placement The placement of the image onto the component.
     * @return A new {@link ImageConf} instance with the specified placement.
     * @throws NullPointerException If the supplied enum constant is {@code null}.
     */
    public ImageConf placement( UI.Placement placement ) {
        Objects.requireNonNull(placement);
        return ImageConf.of(_primer, _image, placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     * Allows you to narrow down the bounding box area of the image in the box
     * model of the underlying component using a {@link UI.ComponentBoundary} enum constant.
     * The component boundaries can be thought of as rectangular bounding boxes that capture
     * the transitional edges between different {@link UI.ComponentArea}s.<br>
     * This property ensures that the image is always placed inside and relative
     * to one of the well-defined {@link UI.ComponentBoundary} (box model bounds).
     * <p>
     * The following placement boundaries are available:
     * <ul>
     *     <li>{@link UI.ComponentBoundary#OUTER_TO_EXTERIOR} -
     *     The outermost boundary of the entire component, including any margin that might be applied.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#EXTERIOR_TO_BORDER} -
     *     The boundary located after the margin but before the border.
     *     This tightly wraps the entire {@link UI.ComponentArea#BODY}.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#BORDER_TO_INTERIOR} -
     *     The boundary located after the border but before the padding.
     *     It represents the edge of the component's interior.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#INTERIOR_TO_CONTENT} -
     *     The boundary located after the padding.
     *     It represents the innermost boundary of the component, where the actual content of the component begins,
     *     typically the sub-components of the component.
     *     </li>
     * </ul>
     * <b>The default value of this property is {@link UI.ComponentBoundary#BORDER_TO_INTERIOR}</b>.
     * So the {@link ComponentStyleDelegate#padding(double, double, double, double)} of the component
     * will not be taken into account by default when placing an image through this API!
     *
     * @param placementBoundary The placement boundary of the component defining the rectangular bounds in which to place the image.
     * @return A new {@link ImageConf} object with the given placement boundary.
     */
    public ImageConf placementBoundary( UI.ComponentBoundary placementBoundary ) {
        Objects.requireNonNull(placementBoundary);
        return ImageConf.of(_primer, _image, _placement, placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  If this flag is set to {@code true}, then the image may be painted
     *  multiple times so that it fills out the entire inner component area.
     *  There will not be a noticeable effect of this flag if the
     *  image already fills out the inner component area (see {@link #autoFit(boolean)}, {@link #size(int, int)}).
     *
     * @param repeat Whether the image should be painted repeatedly across the inner component area.
     * @return A new {@link ImageConf} instance with the specified {@code repeat} flag value.
     */
    public ImageConf repeat( boolean repeat ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, repeat, _fitMode, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  If this flag is set to {@code true}, then the image will be stretched or shrunk
     *  to fill the inner component area dependent on the specified width and height.
     *  This means that if no custom width was specified through {@link #width(int)}
     *  then the image will be scaled to fit the inner component width.
     *  Also, if no custom height was passed to {@link #height(int)} then
     *  the image will be scaled to fit the inner component height. <br>
     *  <b>Note that what we mean by the "inner component area" is the area enclosed
     *  by the border. It is represented by the {@link UI.ComponentArea#INTERIOR} constant.</b>
     *
     * @param autoFit If true the image will be scaled to fit the inner component area for every
     *                dimension which was not specified,
     *                otherwise the image will not be scaled to fit the inner component area.
     * @return A new {@link ImageConf} instance with the specified {@code autoFit} flag value.
     */
    public ImageConf autoFit( boolean autoFit ) {
        UI.FitComponent fit = autoFit ? UI.FitComponent.WIDTH_AND_HEIGHT : UI.FitComponent.UNDEFINED;
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, fit, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  There are different kinds of strategies to fit the image onto the component.
     *  These strategies are identified using the {@link UI.FitComponent} enum
     *  which defines the following fit modes:
     *  <ul>
     *      <li>{@link UI.FitComponent#NO} -
     *          The image will not be scaled to fit the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#WIDTH} -
     *          The image will be scaled to fit the inner component width.
     *          <b>Note that this will only scale the width of the image, but not its height,
     *          and so the inherent aspect ratio of the image may not be preserved!</b>
     *      </li>
     *      <li>{@link UI.FitComponent#HEIGHT} -
     *          The image will be scaled to fit the inner component height.
     *          <b>Note that this will only scale the height of the image, but not its width,
     *          and so the inherent aspect ratio of the image may not be preserved!</b>
     *      </li>
     *      <li>{@link UI.FitComponent#WIDTH_AND_HEIGHT} -
     *          The image will be scaled to fit both the component width and height.
     *          <b>Note that this may override the inherent aspect ratio of the image in favor
     *          of the aspect ratio of the component dimensions!</b>
     *      </li>
     *      <li>{@link UI.FitComponent#MAX_DIM} -
     *          The image will be scaled to fit the smaller
     *          of the two dimension of the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#MIN_DIM} -
     *          The image will be scaled to fit the larger
     *          of the two dimension of the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#UNDEFINED} -
     *          How the image will be scaled to fit the component is unclear.
     *          Another property may override this, but typically the behavior
     *          is similar to {@link UI.FitComponent#NO}.<br>
     *          So for example, if you pass an {@link SvgIcon} to {@link #image(ImageIcon)}
     *          which whose {@link SvgIcon#getFitComponent()} <b>is not {@code UNDEFINED}</b>,
     *          then this will override the "fit component" policy of this config object.
     *      </li>
     *  </ul>
     * @param fit The fit mode of the image.
     * @return A new {@link ImageConf} instance with the specified {@code fit} mode.
     * @throws NullPointerException If the supplied enum constant is {@code null}.
     */
    public ImageConf fitMode( UI.FitComponent fit ) {
        Objects.requireNonNull(fit);
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, fit, _size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  Ensures that the image has the specified width, irrespective of other configurations.<br>
     *  So, if a {@link swingtree.UI.FitComponent} policy was supplied to
     *  {@link #fitMode(UI.FitComponent)} which, for example, stretches the image to fill out the
     *  entire component, then supplying a custom {@code width} to this method will force this stretched
     *  image to have that specific {@code width} <b>without preserving its aspect ratio!</b><br>
     *  <b>So you may end up rendering a distorted image if not careful.</b><br>
     *  Also note that the provided width is considered to include any padding supplied to methods like
     *  {@link #padding(int)}, {@link #padding(int, int)}, or {@link #padding(int, int, int, int)}.
     *
     * @param width The desired width of the rendered image.
     * @return A new {@link ImageConf} instance with the specified {@code width}.
     */
    public ImageConf width( int width ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size.withWidth(width), _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  Ensures that the image has the specified height, irrespective of other configurations.<br>
     *  So, if a {@link swingtree.UI.FitComponent} policy was supplied to
     *  {@link #fitMode(UI.FitComponent)} which, for example, stretches the image to fill out
     *  the entire component, then supplying a custom {@code height} to this method will force this
     *  stretched image to have that specific {@code height} <b>without preserving its aspect ratio!</b><br>
     *  <b>So you may end up rendering a distorted image if not careful.</b><br>
     *  Also note that the provided height is considered to include any padding supplied to methods like
     *  {@link #padding(int)}, {@link #padding(int, int)}, or {@link #padding(int, int, int, int)}.
     *
     * @param height The desired height of the rendered image, including top and bottom padding.
     * @return A new {@link ImageConf} instance with the specified {@code height}.
     */
    public ImageConf height( int height ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size.withHeight(height), _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  Ensures that the image has the specified width and height (including padding).<br>
     *  <b>Note that the image may be rendered smaller than the specified
     *  dimensions if a padding was specified through methods like {@link #padding(int)},
     *  {@link #padding(int, int)}, or {@link #padding(int, int, int, int)}.</b>
     *
     * @param width The width of the image, including left and right padding if specified.
     * @param height The height of the image, including top and bottom padding if specified.
     * @return A new {@link ImageConf} instance with the specified {@code width} and {@code height}.
     */
    public ImageConf size( int width, int height ) {
        return size(Size.of(width, height));
    }

    /**
     *  Ensures that the image has the specified width and height.
     *  Note that if the aspect ratio of these two dimensions does not
     *  match the innate aspect ratio of the image, then the final render
     *  of the image will end up looking distorted.
     *
     * @param size The desired size of the rendered image.
     * @return A new {@link ImageConf} instance with the specified {@code size}.
     */
    public ImageConf size( Size size ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, size, _opacity, _padding, _offset, _clipArea);
    }

    /**
     *  This method allows you to specify the opacity of the image.
     *  The opacity must be between 0.0f and 1.0f, where 0.0f means that the image is completely transparent
     *  and 1.0f means that the image is completely opaque.
     *
     * @param opacity The opacity of the image.
     * @return A new {@link ImageConf} instance with the specified opacity.
     */
    public ImageConf opacity( float opacity ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, opacity, _padding, _offset, _clipArea);
    }

    /**
     *  This method allows you to specify extra padding to the rectangle where the image is drawn.
     *  <b>This will eat into the native size of the image, or the custom image dimensions
     *  supplied to methods like {@link #width(int)}, {@link #height(int)} and {@link #size(int, int)}.</b>
     *
     * @param padding The padding of the image.
     * @return A new {@link ImageConf} instance with the specified padding.
     */
    ImageConf padding( Outline padding ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, padding, _offset, _clipArea);
    }

    /**
     *  This method allows you to specify extra padding to the rectangle where the image is drawn.
     *  <b>This will eat into the native size of the image, or the custom image dimensions
     *  supplied to methods like {@link #width(int)}, {@link #height(int)} and {@link #size(int, int)}.</b>
     *
     * @param top The top padding of the image.
     * @param right The right padding of the image.
     * @param bottom The bottom padding of the image.
     * @param left The left padding of the image.
     * @return A new {@link ImageConf} instance with the specified padding.
     */
    public ImageConf padding( int top, int right, int bottom, int left ) {
        return padding(Outline.of(top, right, bottom, left));
    }

    /**
     *  This method allows you to specify extra padding of the image's bounding rectangle.
     *  <b>This will eat into the native size of the image, or the custom image dimensions
     *  supplied to methods like {@link #width(int)}, {@link #height(int)} and {@link #size(int, int)}.</b>
     *
     * @param topBottom The top and bottom padding of the image.
     * @param leftRight The left and right padding of the image.
     * @return A new {@link ImageConf} instance with the specified padding.
     */
    public ImageConf padding( int topBottom, int leftRight ) {
        return padding(Outline.of(topBottom, leftRight, topBottom, leftRight));
    }

    /**
     *  This method allows you to specify the padding for all sides of the image.
     *  <b>This will eat into the native size of the image, or the custom image dimensions
     *  supplied to methods like {@link #width(int)}, {@link #height(int)} and {@link #size(int, int)}.</b>
     *
     * @param padding The padding of the image.
     * @return A new {@link ImageConf} instance with the specified padding.
     */
    public ImageConf padding( int padding ) {
        return padding(Outline.of(padding, padding, padding, padding));
    }

    /**
     *  Use this to specify the vertical and horizontal offset by which the image will be moved
     *  and drawn onto the component. Note that this is relative to the {@link swingtree.UI.Placement} policy
     *  of the image specified through {@link #placement(UI.Placement)}.<br>
     *  The default placement of an image is {@link UI.Placement#CENTER}, and so the offset will
     *  start from there...
     *
     *  @param x The horizontal offset relative to the {@link #placement(UI.Placement)} of the image.
     *  @param y The vertical offset relative to the {@link #placement(UI.Placement)} of the image.
     *  @return A new {@link ImageConf} instance with the specified offset.
     */
    public ImageConf offset( int x, int y ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, Offset.of(x, y), _clipArea);
    }

    /**
     *  Use this to specify the horizontal offset by which the image will be moved
     *  and drawn onto the component. Note that this is relative to the {@link swingtree.UI.Placement} policy
     *  of the image specified through {@link #placement(UI.Placement)}.<br>
     *  The default placement of an image is {@link UI.Placement#CENTER}, and so the offset will
     *  start from there...
     *
     *  @param x The horizontal offset relative to the {@link #placement(UI.Placement)} of the image.
     *  @return A new {@link ImageConf} instance with the specified offset.
     */
    public ImageConf horizontalOffset( int x ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset.withX(x), _clipArea);
    }

    /**
     *  Use this to specify the vertical offset by which the image will be moved
     *  and drawn onto the component. Note that this is relative to the {@link swingtree.UI.Placement} policy
     *  of the image specified through {@link #placement(UI.Placement)}.<br>
     *  The default placement of an image is {@link UI.Placement#CENTER}, and so the offset will
     *  start from there...
     *
     *  @param y The vertical offset relative to the {@link #placement(UI.Placement)} of the image.
     *  @return A new {@link ImageConf} instance with the specified offset.
     */
    public ImageConf verticalOffset( int y ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset.withY(y), _clipArea);
    }

    /**
     *  Use this to specify the clip area of the image,
     *  which determines on which part of the component the image will be drawn.
     *  The {@link swingtree.UI.ComponentArea} enum defines the following clip areas:
     *  <ul>
     *      <li>{@link swingtree.UI.ComponentArea#ALL} -
     *      The image will be drawn onto the entire component, which
     *      is the union of all other clip areas ({@code INTERIOR + EXTERIOR + BORDER + CONTENT}).
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#INTERIOR} -
     *      The image will be drawn onto the inner component area,
     *      which is defined as {@code ALL - EXTERIOR - BORDER}.
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#EXTERIOR} -
     *      The image will be drawn onto the outer component area,
     *      which can be expressed as {@code ALL - INTERIOR - BORDER},
     *      or {@code ALL - CONTENT}.
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#BORDER} -
     *      The image will be drawn onto the border of the component,
     *      which is the area between the inner and outer component area
     *      and which can be expressed as {@code ALL - INTERIOR - EXTERIOR}.
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#BODY} -
     *      The image will be drawn onto the component body,
     *      which is the inner component area including the border area.
     *      It can be expressed as {@code ALL - EXTERIOR}, or {@code INTERIOR + BORDER}.
     *      </li>
     *  </ul>
     *  The default clip area is {@link swingtree.UI.ComponentArea#INTERIOR},
     *  which means that the image will be drawn onto the inner component area.
     *  <p>
     *  Use {@link UI.ComponentArea#ALL} to draw the image without any additional clipping
     *  onto the entire component, which may also cover the border and margin area of the component.
     *
     *  @param clipArea The clip area of the image.
     *  @return A new {@link ImageConf} instance with the specified clip area.
     */
    public ImageConf clipTo(UI.ComponentArea clipArea ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset, clipArea);
    }

    ImageConf _scale(double scaleFactor ) {
        return ImageConf.of(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size.scale(scaleFactor), _opacity, _padding.scale(scaleFactor), _offset.scale(scaleFactor), _clipArea);
    }

    @Override
    public ImageConf simplified() {
        if ( this.equals(_NONE) )
            return _NONE;

        ImageIcon simplifiedImage = _opacity == 0.0f ? null : _image;
        Color simplifiedPrimer = _primer == null || _primer.getAlpha() == 0 ? null : _primer;

        if ( StyleUtil.isUndefinedColor(simplifiedPrimer) )
            simplifiedPrimer = null;

        if ( simplifiedImage == null && simplifiedPrimer == null )
            return none();

        return ImageConf.of(
                    simplifiedPrimer,
                    simplifiedImage,
                    _placement,
                    _placementBoundary,
                    _repeat,
                    _fitMode,
                    _size,
                    _opacity,
                    _padding.simplified(),
                    _offset,
                    _clipArea
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(_primer, _image, _placement, _placementBoundary, _repeat, _fitMode, _size, _opacity, _padding, _offset, _clipArea);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ImageConf rhs = (ImageConf) obj;
        return Objects.equals(_primer,            rhs._primer)    &&
               Objects.equals(_image,             rhs._image)     &&
               Objects.equals(_placement,         rhs._placement) &&
               Objects.equals(_placementBoundary, rhs._placementBoundary) &&
               _repeat == rhs._repeat    &&
               Objects.equals(_fitMode, rhs._fitMode)   &&
               Objects.equals(_size,    rhs._size)      &&
               _opacity == rhs._opacity   &&
               Objects.equals(_padding,   rhs._padding)   &&
               Objects.equals(_offset,    rhs._offset)    &&
               Objects.equals(_clipArea,  rhs._clipArea);
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) ) return this.getClass().getSimpleName()+"[NONE]";
        return this.getClass().getSimpleName() + "[" +
                    "primer="            + StyleUtil.toString(_primer)                             + ", " +
                    "image="             + ( _image == null ? "?" : _image.toString() )            + ", " +
                    "placement="         + _placement                                              + ", " +
                    "placementBoundary=" + _placementBoundary                                      + ", " +
                    "repeat="            + _repeat                                                 + ", " +
                    "fitComponent="      + _fitMode                                                + ", " +
                    "width="             + _size.width().map(Objects::toString).orElse("?")  + ", " +
                    "height="            + _size.height().map(Objects::toString).orElse("?") + ", " +
                    "opacity="           + _opacity                                                + ", " +
                    "padding="           + _padding                                                + ", " +
                    "offset="            + _offset                                                 + ", " +
                    "clipArea="          + _clipArea                                               +
                "]";
    }

}

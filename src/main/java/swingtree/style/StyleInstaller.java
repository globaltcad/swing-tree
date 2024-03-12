package swingtree.style;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.DimConstraint;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.components.JIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *  This contains all the logic needed for installing the SwingTree style
 *  configurations on a particular component reasonably gracefully.
 *  SwingTree adds a lot of features on top of
 *  regular AWT/Swing.
 *  But it turns out, this is actually fairly difficult,
 *  because when it comes to rendering the UI, Swing has an all or nothing approach,
 *  where you either completely reinstall the look and feel of a component ({@link javax.swing.plaf.ComponentUI}),
 *  or you leave it be. Anything in between is a finicky situation which
 *  is exactly where SwingTree is situated.
 *  The core problem here is that the transition between a SwingTree style and the look and feel
 *  and border of a raw Swing component should be as smooth as possible.
 *  If the user defines a border radius for a component, then this should
 *  not automatically lead to its look and feel being lost.
 *  Instead, only parts of the original look and feel should applied.
 *  This is also true for other component properties like the background color,
 *  the foreground color, the font and the opacity flag.<br>
 *  This last part is especially tricky, because the opacity flag is a very
 *  important property for the performance of Swing components.
 *  <p>
 *  So this class orchestrates under which precise condition, and how, the component should be mutated to
 *  enable the SwingTree style to be effective.
 *
 * @param <C> The type of the component.
 */
final class StyleInstaller<C extends JComponent>
{
    private DynamicLaF _dynamicLaF = DynamicLaF.none(); // Not null, but can be DynamicLaF.none().
    private Color      _initialBackgroundColor   = null;
    private Color      _currentBackgroundColor   = null;
    private Boolean    _initialIsOpaque          = null;
    private Boolean    _initialContentAreaFilled = null;


    void installCustomBorderBasedStyleAndAnimationRenderer( C owner, StyleConf styleConf) {
        Border currentBorder = owner.getBorder();
        if ( !(currentBorder instanceof StyleAndAnimationBorder) )
            owner.setBorder(new StyleAndAnimationBorder<>(ComponentExtension.from(owner), currentBorder, styleConf));
    }

    void installCustomUIFor( C owner ) {
        _dynamicLaF.installCustomUIFor(owner);
    }

    boolean customLookAndFeelIsInstalled() {
        return _dynamicLaF.customLookAndFeelIsInstalled();
    }

    StyleConf applyStyleToComponentState(
        C              owner, // <- The component we want to style.
        StyleConf      newStyle,
        StyleSource<C> styleSource
    ) {
        final boolean noLayoutStyle           = StyleConf.none().hasEqualLayoutAs(newStyle);
        final boolean noPaddingAndMarginStyle = StyleConf.none().hasEqualMarginAndPaddingAs(newStyle);
        final boolean noBorderStyle           = StyleConf.none().hasEqualBorderAs(newStyle);
        final boolean noBaseStyle             = StyleConf.none().hasEqualBaseAs(newStyle);
        final boolean noFontStyle             = StyleConf.none().hasEqualFontAs(newStyle);
        final boolean noDimensionalityStyle   = StyleConf.none().hasEqualDimensionalityAs(newStyle);
        final boolean noShadowStyle           = StyleConf.none().hasEqualShadowsAs(newStyle);
        final boolean noPainters              = StyleConf.none().hasEqualPaintersAs(newStyle);
        final boolean noGradients             = StyleConf.none().hasEqualGradientsAs(newStyle);
        final boolean noNoises                = StyleConf.none().hasEqualNoisesAs(newStyle);
        final boolean noImages                = StyleConf.none().hasEqualImagesAs(newStyle);
        final boolean noProperties            = StyleConf.none().hasEqualPropertiesAs(newStyle);

        final boolean baseStyleIsBasic = newStyle.base().isBasic();

        final boolean allShadowsAreBorderShadows     = newStyle.layers().everyNamedStyle( (layer, styleLayer) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) || styleLayer.shadows().everyNamedStyle(ns -> !ns.style().color().isPresent() ) );
        final boolean allGradientsAreBorderGradients = newStyle.layers().everyNamedStyle( (layer, styleLayer) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) || styleLayer.gradients().everyNamedStyle(ns -> ns.style().colors().length == 0 ) );
        final boolean allNoisesAreBorderNoises       = newStyle.layers().everyNamedStyle( (layer, styleLayer) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) || styleLayer.noises().everyNamedStyle(ns -> ns.style().colors().length == 0 ) );
        final boolean allPaintersAreBorderPainters   = newStyle.layers().everyNamedStyle( (layer, styleLayer) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) || styleLayer.painters().everyNamedStyle(ns -> Painter.none().equals(ns.style().painter()) ) );
        final boolean allImagesAreBorderImages       = newStyle.layers().everyNamedStyle( (layer, styleLayer) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) || styleLayer.images().everyNamedStyle(ns -> !ns.style().image().isPresent() && !ns.style().primer().isPresent() ) );

        final boolean isNotStyled = noLayoutStyle           &&
                                    noPaddingAndMarginStyle &&
                                    noBorderStyle           &&
                                    noBaseStyle             &&
                                    noFontStyle             &&
                                    noDimensionalityStyle   &&
                                    noShadowStyle           &&
                                    noPainters              &&
                                    noGradients             &&
                                    noNoises                &&
                                    noImages                &&
                                    noProperties;

        final boolean pluginsNeeded = !(// A plugin is either a Border or ComponentUI!
                                   noLayoutStyle                              &&
                                   noPaddingAndMarginStyle                    &&
                                   noBorderStyle                              &&
                                   (noBaseStyle || baseStyleIsBasic)          &&
                                   noFontStyle                                &&
                                   noShadowStyle                              &&
                                   noPainters                                 &&
                                   noGradients                                &&
                                   noNoises                                   &&
                                   noImages                                   &&
                                   noProperties );

        final boolean styleCanBeRenderedThroughBorder = (
                                                       (noBaseStyle   || !newStyle.base().hasAnyColors())&&
                                                       (noShadowStyle || allShadowsAreBorderShadows)     &&
                                                       (noPainters    || allPaintersAreBorderPainters)   &&
                                                       (noGradients   || allGradientsAreBorderGradients) &&
                                                       (noNoises      || allNoisesAreBorderNoises)       &&
                                                       (noImages      || allImagesAreBorderImages)
                                                   );

        Runnable backgroundSetter = ()->{};

        if ( pluginsNeeded )
            installCustomBorderBasedStyleAndAnimationRenderer(owner, newStyle);
        else if ( styleSource.hasNoAnimationStylers() )
            _uninstallCustomBorderBasedStyleAndAnimationRenderer(owner);

        if ( pluginsNeeded ) {
            if ( !styleCanBeRenderedThroughBorder )
                _dynamicLaF = _dynamicLaF.establishLookAndFeelFor(newStyle, owner);
        }

        if ( isNotStyled || !pluginsNeeded ) {
            _dynamicLaF = _dynamicLaF._uninstallCustomLaF(owner);
            if ( _initialBackgroundColor != null ) {
                if ( !Objects.equals( owner.getBackground(), _initialBackgroundColor ) )
                    backgroundSetter = () -> owner.setBackground(_initialBackgroundColor);
                _initialBackgroundColor = null;
            }
            if ( _initialIsOpaque != null ) {
                if ( owner.isOpaque() != _initialIsOpaque )
                    owner.setOpaque(_initialIsOpaque);
                _initialIsOpaque = null;
            }
            if ( isNotStyled )
                return newStyle;
        }

        // The component is styled, so we can now apply the style to the component:

        if ( _initialIsOpaque == null )
            _initialIsOpaque = owner.isOpaque();

        if ( owner instanceof AbstractButton && _initialContentAreaFilled == null )
            _initialContentAreaFilled = ((AbstractButton) owner).isContentAreaFilled();

        final List<UI.ComponentArea> opaqueGradAreas     = newStyle.noiseAndGradientCoveredAreas();
        final boolean hasBackgroundGradients             = newStyle.hasVisibleGradientsOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundNoise                 = newStyle.hasVisibleNoisesOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundPainters              = newStyle.hasPaintersOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundImages                = newStyle.hasImagesOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundShadows               = newStyle.hasVisibleShadows(UI.Layer.BACKGROUND);
        final boolean hasBorderRadius                    = newStyle.border().hasAnyNonZeroArcs();
        final boolean hasBackground                      = newStyle.base().backgroundColor().isPresent();
        final boolean hasMargin                          = newStyle.margin().isPositive();
        final boolean hasOpaqueBorder                    = !(255 > newStyle.border().color().map(Color::getAlpha).orElse(0));
        final boolean isSwingTreeComponent               = owner instanceof StylableComponent;
        final boolean backgroundIsActuallyBackground =
                                    !( owner instanceof JTabbedPane  ) && // The LaFs interpret ths tab buttons as background
                                    !( owner instanceof JSlider      ) && // The track color is usually considered the background
                                    !( owner instanceof JProgressBar );   // also the track color is usually considered the background
                                    // TODO: Find and add more cases!

        if ( !hasBackground && _initialIsOpaque ) {
            // If the style has a border radius set we need to make sure that we have a background color:
            if ( hasBorderRadius || newStyle.border().margin().isPositive() ) {
                _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor : owner.getBackground();
                newStyle = newStyle.backgroundColor(_initialBackgroundColor);
            }
        }

        boolean backgroundWasSetSomewhereElse = backgroundWasChangedSomewhereElse( owner );

        if ( hasBackground ) {
            boolean backgroundIsAlreadySet = Objects.equals( owner.getBackground(), newStyle.base().backgroundColor().get() );
            if ( !backgroundIsAlreadySet || newStyle.base().backgroundColor().get() == UI.COLOR_UNDEFINED )
            {
                _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  owner.getBackground();
                Color newColor = newStyle.base().backgroundColor()
                                                .filter( c -> c != UI.COLOR_UNDEFINED )
                                                .orElse(null);

                backgroundSetter = () -> {
                    if ( !Objects.equals( owner.getBackground(), newColor ) )
                        owner.setBackground(newColor);
                };
                /*
                    This component is not a SwingTree component, which means that
                    the paint method is not overridden, and the style engine
                    cannot render the background of the component itself.
                    So we delegate this task to the look and feel.
                */
                if ( owner instanceof JScrollPane ) {
                    JScrollPane scrollPane = (JScrollPane) owner;
                    if ( scrollPane.getViewport() != null ) {
                        JViewport viewport = scrollPane.getViewport();
                        if ( !Objects.equals( viewport.getBackground(), newColor ) )
                            viewport.setBackground( newColor );
                    }
                }
            }
        }

        boolean canBeOpaque = true;

        if ( !opaqueGradAreas.contains(UI.ComponentArea.ALL) ) {
            boolean hasOpaqueFoundation = 255 == newStyle.base().foundationColor().map(Color::getAlpha).orElse(0);
            boolean hasOpaqueBackground = 255 == newStyle.base().backgroundColor().map( c -> c != UI.COLOR_UNDEFINED ? c : _initialBackgroundColor ).map(Color::getAlpha).orElse(255);
            boolean hasBorder           = newStyle.border().widths().isPositive();

            if ( !hasOpaqueFoundation && !opaqueGradAreas.contains(UI.ComponentArea.EXTERIOR) ) {
                if ( hasBorderRadius )
                    canBeOpaque = false;
                else if ( hasMargin )
                    canBeOpaque = false;
            }

            if ( hasBorder && (!hasOpaqueBorder && !opaqueGradAreas.contains(UI.ComponentArea.BORDER)) )
                canBeOpaque = false;

            if (
                !hasOpaqueBackground &&
                !opaqueGradAreas.contains(UI.ComponentArea.INTERIOR) &&
                !opaqueGradAreas.contains(UI.ComponentArea.BODY)
            )
                canBeOpaque = false;
        }


        final Color   backgroundColor = owner.getBackground();
        final boolean backgroundIsFullyTransparent = backgroundColor == null || backgroundColor.getAlpha() == 0;
        final boolean customLookAndFeelInstalled = _dynamicLaF.customLookAndFeelIsInstalled();
        final boolean requiresBackgroundPainting =
                                             hasBackgroundGradients ||
                                             hasBackgroundNoise     ||
                                             hasBackgroundShadows   ||
                                             hasBackgroundPainters  ||
                                             hasBackgroundImages    ||
                                             hasBorderRadius        ||
                                             hasMargin;

        if ( _dynamicLaF.overrideWasNeeded() ) {
            if ( owner instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) owner;

                boolean shouldButtonBeFilled =  !hasBackgroundImages &&
                        !hasBackgroundShadows &&
                        !hasBackground &&
                        !hasBackgroundGradients &&
                        !hasBackgroundNoise &&
                        !hasBackgroundPainters;

                if ( _initialContentAreaFilled != null && !_initialContentAreaFilled )
                    shouldButtonBeFilled = false;

                if ( shouldButtonBeFilled != b.isContentAreaFilled() )
                    b.setContentAreaFilled( shouldButtonBeFilled );
            }
        }

        if ( !canBeOpaque )
        {
            if ( owner.isOpaque() )
                owner.setOpaque(false);
        }
        else if ( !isSwingTreeComponent && !backgroundIsFullyTransparent )
        {
            if ( owner.isOpaque() != _initialIsOpaque )
                owner.setOpaque(_initialIsOpaque);
        }
        else if ( !isSwingTreeComponent && !backgroundWasSetSomewhereElse )
        {
            if ( owner.isOpaque() )
                owner.setOpaque(false);
        }
        else if (
            requiresBackgroundPainting &&
            ( !hasBackground || !customLookAndFeelInstalled ) &&
            ( backgroundWasSetSomewhereElse || !backgroundIsActuallyBackground )
        )
        {
            if ( owner.isOpaque() )
                owner.setOpaque(false);
        }
        else
        {
            if ( !owner.isOpaque() )
                owner.setOpaque(true);

            boolean bypassLaFBackgroundPainting = requiresBackgroundPainting || (hasBackground && isSwingTreeComponent);

            if ( bypassLaFBackgroundPainting )
                if ( backgroundIsActuallyBackground )
                    backgroundSetter = () -> {
                        if ( !Objects.equals( owner.getBackground(), UI.COLOR_UNDEFINED ) )
                            owner.setBackground(UI.COLOR_UNDEFINED);
                    };
            /*
                The above line looks very strange, but it is very important!

                To understand what is going on here, you have to know that when a component is
                flagged as opaque, then every Swing look and feel will, before painting
                anything else, first fill out the entire background of the component with
                the background color of the component.
                It does this to ensure that rendering artifacts from the parent
                are overridden.

                Now this is a problem when you have the background layer of your SwingTree component
                styled using various things like gradients, shadows, images, etc.
                Because SwingTree, unfortunately, cannot hijack the internals of the ComponentUI,
                it can however do some painting before the ComponentUI
                through an overridden `paint(Graphics2D)` method!

                Now, we could simply set the opaque flag to false in order to prevent the ComponentUI
                from filling the component bounds, but then we would lose the
                performance benefits of having the opaque flag set to true (avoiding the
                traversal repaint of parent components, and their parent components, etc).

                In this branch we have already determined that the style configuration
                leads to an opaque component, and we also have the ability to render
                the background of the component ourselves due to the
                component being a SwingTree component (it has the paint method overridden).

                So what we do here is we set the background color of the component to
                UI.COLOR_UNDEFINED, which is a special color that is actually fully transparent.

                This way, when the Swing look and feel tries to paint the background of the
                component, it will actually paint nothing, and we can do the background
                painting ourselves in the paint method of the component.
            */
        }

        _applyGenericBaseStyleTo(owner, newStyle);
        _applyIconStyleTo(owner, newStyle);
        _applyLayoutStyleTo(owner, newStyle);
        _applyDimensionalityStyleTo(owner, newStyle);
        _applyFontStyleTo(owner, newStyle);
        _applyPropertiesTo(owner, newStyle);
        _doComboBoxMarginAdjustment(owner, newStyle);

        if ( newStyle.hasPaintersOnLayer(UI.Layer.FOREGROUND) )
            _makeAllChildrenTransparent(owner);

        backgroundSetter.run();

        if ( !backgroundWasSetSomewhereElse )
            _currentBackgroundColor = owner.getBackground();

        return newStyle;
    }

    boolean backgroundWasChangedSomewhereElse( C owner ) {
        if ( _currentBackgroundColor != null ) {
            if ( _currentBackgroundColor != owner.getBackground() ) {
                _initialBackgroundColor = _currentBackgroundColor;
                return true;
            }
        }
        return false;
    }

    private void _applyGenericBaseStyleTo( final C owner, final StyleConf styleConf )
    {
        if ( styleConf.base().foregroundColor().isPresent() && !Objects.equals( owner.getForeground(), styleConf.base().foregroundColor().get() ) ) {
            Color newColor = styleConf.base().foregroundColor().get();
            if ( newColor == UI.COLOR_UNDEFINED)
                newColor = null;

            if ( !Objects.equals( owner.getForeground(), newColor ) )
                owner.setForeground( newColor );
        }

        styleConf.base().cursor().ifPresent( cursor -> {
            if ( !Objects.equals( owner.getCursor(), cursor ) )
                owner.setCursor( cursor );
        });

        if ( styleConf.base().orientation() != UI.ComponentOrientation.UNKNOWN ) {
            ComponentOrientation currentOrientation = owner.getComponentOrientation();
            UI.ComponentOrientation newOrientation = styleConf.base().orientation();
            switch ( newOrientation ) {
                case LEFT_TO_RIGHT:
                    if ( !Objects.equals( currentOrientation, ComponentOrientation.LEFT_TO_RIGHT ) )
                        owner.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
                    break;
                case RIGHT_TO_LEFT:
                    if ( !Objects.equals( currentOrientation, ComponentOrientation.RIGHT_TO_LEFT ) )
                        owner.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                    break;
                default:
                    if ( !Objects.equals( currentOrientation, ComponentOrientation.UNKNOWN ) )
                        owner.applyComponentOrientation(ComponentOrientation.UNKNOWN);
                    break;
            }
        }
    }

    private void _applyIconStyleTo( final C owner, StyleConf styleConf )
    {
        UI.FitComponent fit = styleConf.base().fit();
        styleConf.base().icon().ifPresent( icon -> {
            if ( icon instanceof SvgIcon) {
                SvgIcon svgIcon = (SvgIcon) icon;
                icon = svgIcon.withFitComponent(fit);
            }
            if ( owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) owner;
                if ( !Objects.equals( button.getIcon(), icon ) )
                    button.setIcon( icon );
            }
            if ( owner instanceof JLabel ) {
                JLabel label = (JLabel) owner;
                if ( !Objects.equals( label.getIcon(), icon ) )
                    label.setIcon( icon );
            }
            if ( owner instanceof JIcon ) {
                JIcon jIcon = (JIcon) owner;
                if ( !Objects.equals( jIcon.getIcon(), icon ) )
                    jIcon.setIcon( icon );
            }
        });
    }

    private void _applyLayoutStyleTo( final C owner, final StyleConf styleConf )
    {
        final LayoutConf style = styleConf.layout();

        // Generic Layout stuff:

        styleConf.layout().alignmentX().ifPresent( alignmentX -> {
            if ( !Objects.equals( owner.getAlignmentX(), alignmentX ) )
                owner.setAlignmentX( alignmentX );
        });

        styleConf.layout().alignmentY().ifPresent( alignmentY -> {
            if ( !Objects.equals( owner.getAlignmentY(), alignmentY ) )
                owner.setAlignmentY( alignmentY );
        });

        // Install Generic Layout:
        styleConf.layout().layout().installFor(owner);

        // Now on to MigLayout stuff:

        Optional<Float> alignmentX = style.alignmentX();
        Optional<Float> alignmentY = style.alignmentY();

        if ( !alignmentX.isPresent() && !alignmentY.isPresent() )
            return;

        LayoutManager layout = ( owner.getParent() == null ? null : owner.getParent().getLayout() );
        if ( layout instanceof MigLayout ) {
            MigLayout migLayout = (MigLayout) layout;
            Object rawComponentConstraints = migLayout.getComponentConstraints(owner);
            if ( rawComponentConstraints instanceof String )
                rawComponentConstraints = ConstraintParser.parseComponentConstraint(rawComponentConstraints.toString());

            CC componentConstraints = (rawComponentConstraints instanceof CC ? (CC) rawComponentConstraints : null);

            final CC finalComponentConstraints = ( componentConstraints == null ? new CC() : componentConstraints );

            String x = alignmentX.map( a -> (int) ( a * 100f ) )
                                  .map( a -> a + "%" )
                                  .orElse("");

            String y = alignmentY.map( a -> (int) ( a * 100f ) )
                                  .map( a -> a + "%" )
                                  .orElse("");

            DimConstraint horizontalDimConstraint = finalComponentConstraints.getHorizontal();
            DimConstraint verticalDimConstraint   = finalComponentConstraints.getVertical();

            UnitValue xAlign = horizontalDimConstraint.getAlign();
            UnitValue yAlign = verticalDimConstraint.getAlign();

            boolean xChange = !x.equals( xAlign == null ? "" : xAlign.getConstraintString() );
            boolean yChange = !y.equals( yAlign == null ? "" : yAlign.getConstraintString() );

            if ( !x.isEmpty() && xChange )
                finalComponentConstraints.alignX(x);

            if ( !y.isEmpty() && yChange )
                finalComponentConstraints.alignY(y);

            if ( xChange || yChange ) {
                migLayout.setComponentConstraints(owner, finalComponentConstraints);
                owner.getParent().revalidate();
            }
        }
    }

    private void _applyDimensionalityStyleTo( final C owner, final StyleConf styleConf )
    {
        final DimensionalityConf dimensionalityConf = styleConf.dimensionality();

        if ( dimensionalityConf.minWidth().isPresent() || dimensionalityConf.minHeight().isPresent() ) {
            Dimension minSize = owner.getMinimumSize();

            int minWidth  = dimensionalityConf.minWidth().orElse(minSize == null ? 0 : minSize.width);
            int minHeight = dimensionalityConf.minHeight().orElse(minSize == null ? 0 : minSize.height);

            Dimension newMinSize = new Dimension(minWidth, minHeight);

            if ( ! newMinSize.equals(minSize) )
                owner.setMinimumSize(newMinSize);
        }

        if ( dimensionalityConf.maxWidth().isPresent() || dimensionalityConf.maxHeight().isPresent() ) {
            Dimension maxSize = owner.getMaximumSize();

            int maxWidth  = dimensionalityConf.maxWidth().orElse(maxSize == null  ? Integer.MAX_VALUE : maxSize.width);
            int maxHeight = dimensionalityConf.maxHeight().orElse(maxSize == null ? Integer.MAX_VALUE : maxSize.height);

            Dimension newMaxSize = new Dimension(maxWidth, maxHeight);

            if ( ! newMaxSize.equals(maxSize) )
                owner.setMaximumSize(newMaxSize);
        }

        if ( dimensionalityConf.preferredWidth().isPresent() || dimensionalityConf.preferredHeight().isPresent() ) {
            Dimension prefSize = owner.getPreferredSize();

            int prefWidth  = dimensionalityConf.preferredWidth().orElse(prefSize == null ? 0 : prefSize.width);
            int prefHeight = dimensionalityConf.preferredHeight().orElse(prefSize == null ? 0 : prefSize.height);

            Dimension newPrefSize = new Dimension(prefWidth, prefHeight);

            if ( !newPrefSize.equals(prefSize) )
                owner.setPreferredSize(newPrefSize);
        }

        if ( dimensionalityConf.width().isPresent() || dimensionalityConf.height().isPresent() ) {
            Dimension size = owner.getSize();

            int width  = dimensionalityConf.width().orElse(size == null ? 0 : size.width);
            int height = dimensionalityConf.height().orElse(size == null ? 0 : size.height);

            Dimension newSize = new Dimension(width, height);

            if ( ! newSize.equals(size) )
                owner.setSize(newSize);
        }
    }

    private void _applyFontStyleTo( final C owner, final StyleConf styleConf )
    {
        final FontConf fontConf = styleConf.font();

        if ( owner instanceof JTextComponent ) {
            JTextComponent tc = (JTextComponent) owner;
            if ( fontConf.selectionColor().isPresent() && ! Objects.equals( tc.getSelectionColor(), fontConf.selectionColor().get() ) )
                tc.setSelectionColor(fontConf.selectionColor().get());
        }

        fontConf
             .createDerivedFrom(owner.getFont())
             .ifPresent( newFont -> {
                    if ( !newFont.equals(owner.getFont()) )
                        owner.setFont( newFont );
                });

        fontConf.horizontalAlignment().ifPresent(alignment -> {
            if ( owner instanceof JLabel ) {
                JLabel label = (JLabel) owner;
                if ( !Objects.equals( label.getHorizontalAlignment(), alignment.forSwing() ) )
                    label.setHorizontalAlignment( alignment.forSwing() );
            }
            if ( owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) owner;
                if ( !Objects.equals( button.getHorizontalAlignment(), alignment.forSwing() ) )
                    button.setHorizontalAlignment( alignment.forSwing() );
            }
            if ( owner instanceof JTextField ) {
                JTextField textField = (JTextField) owner;
                if ( !Objects.equals( textField.getHorizontalAlignment(), alignment.forSwing() ) )
                    textField.setHorizontalAlignment( alignment.forSwing() );
            }
        });
        fontConf.verticalAlignment().ifPresent(alignment -> {
            if ( owner instanceof JLabel ) {
                JLabel label = (JLabel) owner;
                if ( !Objects.equals( label.getVerticalAlignment(), alignment.forSwing() ) )
                    label.setVerticalAlignment( alignment.forSwing() );
            }
            if ( owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) owner;
                if ( !Objects.equals( button.getVerticalAlignment(), alignment.forSwing() ) )
                    button.setVerticalAlignment( alignment.forSwing() );
            }
        });
    }

    private void _applyPropertiesTo( final C owner, final StyleConf styleConf ) {
        styleConf.properties().forEach( property -> {
            Object oldValue = owner.getClientProperty(property.name());
            if ( property.style().equals(oldValue) )
                return;

            if ( property.style().isEmpty() )
                owner.putClientProperty(property.name(), null); // remove property
            else
                owner.putClientProperty(property.name(), property.style());
        });
    }

    private void _doComboBoxMarginAdjustment( final C owner, final StyleConf styleConf ) {
        if ( owner instanceof JComboBox ) {
            int bottom = styleConf.margin().bottom().map(Number::intValue).orElse(0);
            // We adjust the position of the popup menu:
            try {
                Point location = owner.getLocationOnScreen();
                int x = location.x;
                int y = location.y + owner.getHeight() - bottom;
                JComboBox<?> comboBox = (JComboBox<?>) owner;
                JPopupMenu popup = (JPopupMenu) comboBox.getAccessibleContext().getAccessibleChild(0);
                Point oldLocation = popup.getLocation();
                if ( popup.isShowing() && (oldLocation.x != x || oldLocation.y != y) )
                    popup.setLocation(x, y);
            } catch ( Exception e ) {
                // ignore
            }
        }
    }

    private void _uninstallCustomBorderBasedStyleAndAnimationRenderer( C owner ) {
        Border currentBorder = owner.getBorder();
        if ( currentBorder == null )
            return;

        if ( currentBorder instanceof StyleAndAnimationBorder) {
            StyleAndAnimationBorder<?> border = (StyleAndAnimationBorder<?>) currentBorder;
            owner.setBorder(border.getFormerBorder());
        }

        if ( _initialBackgroundColor != null ) {
            if ( !Objects.equals( owner.getBackground(), _initialBackgroundColor ) )
                owner.setBackground(_initialBackgroundColor);
            _initialBackgroundColor = null;
        }
    }

    /**
     *  Note that the foreground painter is intended to paint over all children of the component, <br>
     *  which is why it will be called at the end of {@code JComponent::paintChildren(Graphics)}.
     *  <br>
     *  However, there is a problem with this approach! <br>
     *  If not all children are transparent, the result of the foreground painter can be overwritten
     *  by {@link JComponent#paintImmediately(int, int, int, int)} when certain events occur
     *  (like a child component is a text field with a blinking cursor, or a button with hover effect).
     *  This type of repaint does unfortunately not call {@code JComponent::paintChildren(Graphics)},
     *  in fact it completely bypasses the rendering of this current component!
     *  In order to ensure that the stuff painted by the foreground painter is not overwritten
     *  in these types of cases,
     *  we make all children transparent (non-opaque) so that the foreground painter is always visible.
     *
     * @param c The component to make all children transparent.
     */
    private void _makeAllChildrenTransparent( JComponent c ) {
        if ( !c.isVisible() )
            return;

        if ( c.isOpaque() )
            c.setOpaque(false);

        for ( Component child : c.getComponents() ) {
            if ( child instanceof JComponent ) {
                JComponent jChild = (JComponent) child;
                _makeAllChildrenTransparent(jChild);
            }
        }
    }

}

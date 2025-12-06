package swingtree.style;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.DimConstraint;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.Configurator;
import swingtree.api.Painter;
import swingtree.components.JBox;
import swingtree.components.JIcon;
import swingtree.layout.Bounds;

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
 *  Instead, only parts of the original look and feel should be applied.
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
    private static final Logger log = LoggerFactory.getLogger(StyleInstaller.class);

    private DynamicLaF        _dynamicLaF = DynamicLaF.none(); // Not null, but can be DynamicLaF.none().
    private @Nullable Color   _outSideBackgroundColor    = null;
    private @Nullable Color   _lastInsideBackgroundColor = null;
    private @Nullable Boolean _initialIsOpaque           = null;
    private @Nullable Boolean _initialContentAreaFilled  = null;
    private @Nullable Font    _initialFont               = null;

    void updateDynamicLookAndFeel(Configurator<DynamicLaF> updater) {
        try {
            _dynamicLaF = updater.configure(_dynamicLaF);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Failed to update dynamic look and feel!", e);
        }
    }

    void installCustomBorderBasedStyleAndAnimationRenderer( C owner, StyleConf styleConf) {
        Border currentBorder = owner.getBorder();
        if ( !(currentBorder instanceof StyleAndAnimationBorder) )
            owner.setBorder(new StyleAndAnimationBorder<>(ComponentExtension.from(owner), currentBorder, styleConf));
    }

    StyleConf recalculateInsets( C owner, StyleConf styleConf ) {
        if ( owner.getBorder() instanceof StyleAndAnimationBorder ) {
            final Outline paddingCorrection = _formerBorderPaddingCorrection(owner, styleConf);
            final Outline adjustedPadding   = styleConf.border().padding().or(paddingCorrection);
            styleConf = styleConf._withBorder(styleConf.border().withPadding(adjustedPadding));
            StyleAndAnimationBorder<?> border = (StyleAndAnimationBorder<?>) owner.getBorder();
            border.recalculateInsets(styleConf);
        }
        return styleConf;
    }

    void installCustomUIFor( C owner ) {
        _dynamicLaF.installCustomUIFor(owner);
    }

    boolean customLookAndFeelIsInstalled(C owner) {
        return _dynamicLaF.customLookAndFeelIsInstalled(owner);
    }

    Outline _formerBorderMarginCorrection( C owner ) {
        Border border = owner.getBorder();
        if ( border instanceof StyleAndAnimationBorder ) {
            return ((StyleAndAnimationBorder<?>) border).getDelegatedInsetsComponentAreaCorrection();
        }
        return Outline.none();
    }

    Outline _formerBorderPaddingCorrection( C owner, StyleConf conf ) {
        Border border = owner.getBorder();
        Outline result = Outline.none();
        if ( border instanceof StyleAndAnimationBorder ) {
            result = ((StyleAndAnimationBorder<?>) border).getDelegatedInsets(conf);
        }
        return result.map( v -> v <= 0 ? null : v );
    }

    StyleEngine _updateEngine(
        final C           owner,
        final StyleEngine engine,
        final StyleConf   newStyle,
        final Outline     marginCorrection
    ) {
        _lastInsideBackgroundColor = owner.getBackground();

        final ComponentConf currentConf = engine.getComponentConf();
        final boolean sameStyle      = currentConf.style().equals(newStyle);
        final boolean sameBounds     = currentConf.currentBounds().equals(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
        final boolean sameCorrection = currentConf.areaMarginCorrection().equals(marginCorrection);

        ComponentConf newConf;
        if ( sameStyle && sameBounds && sameCorrection )
            newConf = currentConf;
        else
            newConf = new ComponentConf(
                            newStyle,
                            Bounds.of(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight()),
                            marginCorrection
                        );

        LayerCache[] layerCaches = engine.getLayerCaches();
        for ( LayerCache layerCache : layerCaches )
            layerCache.validate(currentConf, newConf);

        BoxModelConf newBoxModelConf = BoxModelConf.of(newConf.style().border(), newConf.areaMarginCorrection(), newConf.currentBounds().size());
        return engine.with(newBoxModelConf, newConf);
    }

    StyleEngine applyStyleToComponentState(
        final C              owner, // <- The component we want to style.
        final StyleEngine    engine,
        final StyleSource<C> styleSource,
        StyleConf            newStyle,
        final boolean        force
    ) {
        boolean initialIsOpaqueFlagState = owner.isOpaque();
        boolean initialIsContentAreaFilled = ( owner instanceof AbstractButton && ((AbstractButton) owner).isContentAreaFilled() );
        Runnable backgroundSetter = ()->{
            if ( StyleUtil.isUndefinedColor(owner.getBackground()) )
                _establishDefaultBackgroundColorFor(owner);
            /*
                The default background setter ensures that the background
                cannot be in an undefined state, we use the identity of the
                UI.Color.UNDEFINED constant to check this.

                Note that the undefined background is a state
                unique to a certain style state where we need to override
                the native look and feel...
            */
        };

        boolean doInstallation = true;
        boolean backgroundWasSetSomewhereElse = this.backgroundWasChangedSomewhereElse(owner);
        if ( backgroundWasSetSomewhereElse ) {
            _outSideBackgroundColor = owner.getBackground();
            Color outSideBackgroundColor = _outSideBackgroundColor;
            backgroundSetter = () -> {
                if ( !Objects.equals( owner.getBackground(), outSideBackgroundColor ) )
                    owner.setBackground(outSideBackgroundColor);
            };
        }

        StyleConf oldStyle = engine.getComponentConf().style();
        if ( !force ) {
            // We check if it makes sense to apply the new style:
            if ( !backgroundWasSetSomewhereElse && oldStyle.equals(newStyle) )
                doInstallation = false;
        }

        final Outline marginCorrection = _formerBorderMarginCorrection(owner);
        if ( !doInstallation ) {
            final Outline paddingCorrection = _formerBorderPaddingCorrection(owner, newStyle);
            final Outline adjustedPadding   = newStyle.border().padding().or(paddingCorrection);
            newStyle = newStyle._withBorder(newStyle.border().withPadding(adjustedPadding));

            if ( owner.getBorder() instanceof StyleAndAnimationBorder<?> ) {
                StyleAndAnimationBorder<C> border = (StyleAndAnimationBorder<C>) owner.getBorder();
                border.recalculateInsets(newStyle);
            }

            return _updateEngine(owner, engine, newStyle, marginCorrection);
        }

        final boolean isSwingTreeComponent = owner instanceof StylableComponent;

        final boolean isStyled            = !newStyle.equals(StyleConf.none());
        final boolean hasPaddingAndMargin = isStyled && !StyleConf.none().hasEqualMarginAndPaddingAs(newStyle);
        final boolean hasBorderStyle      = isStyled && !StyleConf.none().hasEqualBorderAs(newStyle);
        final boolean hasBaseStyle        = isStyled && !StyleConf.none().hasEqualBaseAs(newStyle);
        final boolean hasBaseColors       = isStyled && (hasBaseStyle && newStyle.base().hasAnyColors());
        final boolean hasBackFilter       = isStyled && !FilterConf.none().equals(newStyle.layers().filter());

        final boolean weNeedToInstallTheCustomBorder = isStyled && (
               hasPaddingAndMargin || hasBorderStyle
               || newStyle.layers().any( (layer, it) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) && it.shadows().any(named -> named.style().color().isPresent() ) )
               || newStyle.layers().any( (layer, it) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) && it.gradients().any(named -> named.style().colors().length > 0 ) )
               || newStyle.layers().any( (layer, it) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) && it.images().any(named -> named.style().image().isPresent() || named.style().primer().isPresent() ) )
               || newStyle.layers().any( (layer, it) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) && it.texts().any(named -> !TextConf.none().equals(named.style()) ) )
               || newStyle.layers().any( (layer, it) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) && it.painters().any(named -> !Painter.none().equals(named.style().painter()) ) )
               || newStyle.layers().any( (layer, it) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) && it.noises().any(named -> named.style().colors().length > 0 ) )
            );

        final boolean weNeedToInstallTheCustomUI = isStyled && (
               (hasBackFilter && !isSwingTreeComponent) ||
               (hasBaseColors && newStyle.base().requiresCustomUI())
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.shadows().any(named -> named.style().color().isPresent() ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.gradients().any(named -> named.style().colors().length > 0 ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.images().any(named -> named.style().image().isPresent() || named.style().primer().isPresent() ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.painters().any(named -> !Painter.none().equals(named.style().painter()) ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.texts().any(named -> !TextConf.none().equals(named.style()) ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.noises().any(named -> named.style().colors().length > 0 ) )
            );

        if ( weNeedToInstallTheCustomBorder ) {
            installCustomBorderBasedStyleAndAnimationRenderer(owner, newStyle);
            newStyle = recalculateInsets(owner, newStyle);
        } else if ( styleSource.hasNoAnimationStylers() ) {
            _uninstallCustomBorderBasedStyleAndAnimationRenderer(owner);
        }

        if ( weNeedToInstallTheCustomUI ) {
            _dynamicLaF = _dynamicLaF.establishLookAndFeelFor(newStyle, owner);
        } else {
            if ( _outSideBackgroundColor != null ) {
                if ( !Objects.equals( owner.getBackground(), _outSideBackgroundColor) )
                    owner.setBackground(_outSideBackgroundColor);
                _outSideBackgroundColor = null;
            }
        }

        if ( !isStyled || !weNeedToInstallTheCustomUI ) {
            _dynamicLaF = _dynamicLaF._uninstallCustomLaF(owner);
            if ( owner instanceof AbstractButton && _initialContentAreaFilled != null ) {
                AbstractButton button = (AbstractButton) owner;
                if ( button.isContentAreaFilled() != _initialContentAreaFilled)
                    button.setContentAreaFilled(_initialContentAreaFilled);
                _initialContentAreaFilled = null;
            }
            if ( _initialIsOpaque != null ) {
                if ( owner.isOpaque() != _initialIsOpaque )
                    owner.setOpaque(_initialIsOpaque);
            }
            if ( !isStyled ) {
                backgroundSetter.run();
                if ( _initialFont != null && !Objects.equals(_initialFont, owner.getFont()) ) {
                    owner.setFont(_initialFont);
                    _initialFont = null;
                }
                return _updateEngine(owner, engine, newStyle, marginCorrection);
            }
        }

        if ( _initialIsOpaque == null )
            _initialIsOpaque = initialIsOpaqueFlagState; // Important: Use the state of the flag before SwingTree did anything!

        if ( owner instanceof AbstractButton && _initialContentAreaFilled == null )
            _initialContentAreaFilled = initialIsContentAreaFilled;

        final List<UI.ComponentArea> opaqueGradAreas = newStyle.noiseAndGradientCoveredAreas();
        final boolean hasBackgroundGradients         = newStyle.hasVisibleGradientsOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundNoise             = newStyle.hasVisibleNoisesOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundPainters          = newStyle.hasPaintersOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundImages            = newStyle.hasImagesOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundShadows           = newStyle.hasVisibleShadows(UI.Layer.BACKGROUND);
        final boolean hasBorderRadius                = newStyle.border().hasAnyNonZeroArcs();
        final boolean hasBackground                  = newStyle.base().backgroundColor().isPresent();
        final boolean hasMargin                      = newStyle.margin().isPositive();
        final boolean hasOpaqueBorder                = newStyle.border().colors().isFullyOpaue();
        final boolean isNaturallyTransparent         = ( _initialIsOpaque == false ); // We categorize based on the initial state of the flag.
        final boolean backgroundIsActuallyBackground =
                                    !( owner instanceof JTabbedPane  ) && // The LaFs interpret the tab buttons as background
                                    !( owner instanceof JSlider      ) && // The track color is usually considered the background
                                    !( owner instanceof JProgressBar );   // also the progress track color is usually considered the background
                                    // TODO: Find and add more cases!

        if ( !hasBackground && _initialIsOpaque ) {
            // If the style has a border radius set we need to make sure that we have a background color:
            if ( hasBorderRadius || newStyle.border().margin().isPositive() ) {
                _outSideBackgroundColor = _outSideBackgroundColor != null ? _outSideBackgroundColor : owner.getBackground();
                newStyle = newStyle.backgroundColor(_outSideBackgroundColor);
            }
        }

        boolean hasUndefinedNullBackground = false;
        if ( hasBackground ) {
            boolean backgroundIsAlreadySet = Objects.equals( owner.getBackground(), newStyle.base().backgroundColor().get() );
            if ( !backgroundIsAlreadySet || StyleUtil.isUndefinedColor(newStyle.base().backgroundColor().get()) )
            {
                _outSideBackgroundColor = _outSideBackgroundColor != null ? _outSideBackgroundColor :  owner.getBackground();
                Color newColor = newStyle.base().backgroundColor()
                                                .filter( c -> !StyleUtil.isUndefinedColor(c) )
                                                .orElse(null);

                if ( newColor == null )
                    hasUndefinedNullBackground = true;

                backgroundSetter = () -> {
                    if ( newColor == null ) {
                        if ( owner.isBackgroundSet() )
                            owner.setBackground(null);
                    } else if ( !Objects.equals( owner.getBackground(), newColor ) )
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

        if ( _isTransparentConstant(owner.getBackground()) )
            canBeOpaque = false;

        if ( !opaqueGradAreas.contains(UI.ComponentArea.ALL) ) {
            boolean hasOpaqueFoundation = 255 == newStyle.base().foundationColor().map(java.awt.Color::getAlpha).orElse(0);
            boolean hasOpaqueBackground = 255 == newStyle.base().backgroundColor().map( c -> !StyleUtil.isUndefinedColor(c) ? c : _outSideBackgroundColor).map(java.awt.Color::getAlpha).orElse(255);
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
        final boolean customLookAndFeelInstalled = _dynamicLaF.customLookAndFeelIsInstalled(owner);
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
        else if ( !isSwingTreeComponent && !backgroundIsFullyTransparent && _initialIsOpaque != null )
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
            boolean shouldBeOpaque = !_isTransparentConstant(owner.getBackground());
            boolean bypassLaFBackgroundPainting = requiresBackgroundPainting || (hasBackground && isSwingTreeComponent);

            if ( bypassLaFBackgroundPainting && backgroundIsActuallyBackground && !hasUndefinedNullBackground ) {
                backgroundSetter = () -> {
                    if ( !Objects.equals( owner.getBackground(), UI.Color.UNDEFINED ) )
                        owner.setBackground(UI.Color.UNDEFINED);
                };
            }
            if ( !hasBackground && isNaturallyTransparent )
                shouldBeOpaque = false;
            if ( owner.isOpaque() != shouldBeOpaque )
                owner.setOpaque(shouldBeOpaque);
            /*
                The above line 'owner.setBackground(UI.Color.UNDEFINED);'
                may look very strange to you, but it is very important!

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
                UI.Color.UNDEFINED, which is a special color that is actually fully transparent.

                This way, when the Swing look and feel tries to paint the background of the
                component, it will actually paint nothing, and we can do the background
                painting ourselves in the paint method of the component.
            */
        }

        _applyGenericBaseStyleTo(owner, newStyle);
        _applyIconStyleTo(owner, newStyle);
        _applyLayoutStyleTo(owner, newStyle);
        _applyDimensionalityStyleTo(owner, newStyle);
        _applyPropertiesTo(owner, newStyle);
        _doComboBoxMarginAdjustment(owner, newStyle);

        if ( newStyle.hasPaintersOnLayer(UI.Layer.FOREGROUND) )
            _makeAllChildrenTransparent(owner);

        backgroundSetter.run();

        StyleEngine newEngine = _updateEngine(owner, engine, newStyle, marginCorrection);

        _applyFontStyleTo(owner, newStyle);

        return newEngine;
    }

    @SuppressWarnings("ReferenceEquality")
    private final boolean _isTransparentConstant( final Color color ) {
        return color == UI.Color.TRANSPARENT;
    }

    @SuppressWarnings("ReferenceEquality")
    boolean backgroundWasChangedSomewhereElse( C owner ) {
        if ( _lastInsideBackgroundColor != null ) {
            if ( _lastInsideBackgroundColor != owner.getBackground() ) {
                return true;
            }
        }
        return false;
    }

    private void _applyGenericBaseStyleTo( final C owner, final StyleConf styleConf )
    {
        final BaseConf base = styleConf.base();

        if ( base.foregroundColor().isPresent() && !Objects.equals( owner.getForeground(), base.foregroundColor().get() ) ) {
            Color newColor = base.foregroundColor().get();
            if ( StyleUtil.isUndefinedColor(newColor) )
                newColor = null;

            if ( !Objects.equals( owner.getForeground(), newColor ) )
                owner.setForeground( newColor );
        }

        base.cursor().ifPresent( cursor -> {
            if ( !Objects.equals( owner.getCursor(), cursor ) )
                owner.setCursor( cursor );
        });

        if ( base.orientation() != UI.ComponentOrientation.UNKNOWN ) {
            ComponentOrientation currentOrientation = owner.getComponentOrientation();
            UI.ComponentOrientation newOrientation = base.orientation();
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
        final BaseConf base = styleConf.base();

        UI.FitComponent fit = base.fit();
        base.icon().ifPresent( icon -> {
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

    private void _applyLayoutStyleTo( final C owner, final StyleConf style )
    {
        final LayoutConf layoutConf = style.layout();
        // Generic Layout stuff:

        layoutConf.alignmentX().ifPresent( alignmentX -> {
            if ( !Objects.equals( owner.getAlignmentX(), alignmentX ) )
                owner.setAlignmentX( alignmentX );
        });

        layoutConf.alignmentY().ifPresent( alignmentY -> {
            if ( !Objects.equals( owner.getAlignmentY(), alignmentY ) )
                owner.setAlignmentY( alignmentY );
        });

        // Install Generic Layout:
        layoutConf.layout().installFor(owner);

        // Now on to MigLayout installation details:

        Optional<Float> alignmentX = layoutConf.alignmentX();
        Optional<Float> alignmentY = layoutConf.alignmentY();

        if ( !alignmentX.isPresent() && !alignmentY.isPresent() )
            return;

        LayoutManager layoutManager = ( owner.getParent() == null ? null : owner.getParent().getLayout() );
        if ( layoutManager instanceof MigLayout ) {
            MigLayout migLayout = (MigLayout) layoutManager;
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

            if ( !newMaxSize.equals(maxSize) )
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

            if ( !newSize.equals(size) )
                owner.setSize(newSize);
        }
    }

    private void _applyFontStyleTo( final C owner, final StyleConf styleConf )
    {
        final FontConf fontConf = styleConf.font();
        if ( FontConf.none().equals(fontConf) ) {
            if ( _initialFont != null && !Objects.equals(_initialFont, owner.getFont()) ) {
                owner.setFont(_initialFont);
                _initialFont = null;
            }
            return;
        } else if ( _initialFont == null ) {
            _initialFont = owner.getFont();
        }

        if ( owner instanceof JTextComponent ) {
            JTextComponent tc = (JTextComponent) owner;
            if ( fontConf.selectionColor().isPresent() && ! Objects.equals( tc.getSelectionColor(), fontConf.selectionColor().get() ) )
                tc.setSelectionColor(fontConf.selectionColor().get());
        }

        fontConf
             .createDerivedFrom(owner.getFont(), owner)
             .ifPresent( newFont -> {
                    if ( !newFont.equals(owner.getFont()) )
                        owner.setFont( newFont );
                });

        _installLayoutInfoFromFontConf(fontConf, owner);
    }

    @SuppressWarnings("DoNotCall")
    private static void _installLayoutInfoFromFontConf(FontConf fontConf, JComponent owner) {
        LibraryInternalCrossPackageStyleUtil.applyFontConfAlignmentsToComponent(fontConf, owner);
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

    private void _establishDefaultBackgroundColorFor(JComponent owner) {
        Class<?> type = owner.getClass();
        JComponent other = null;
        try {
            other = (JComponent) type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.debug(
                    "Failed to instantiate component '"+type.getName()+"' as part " +
                    "part of an attempt to get the default color of said type!",
                    e
                );
        }
        Color defaultBackgroundColor = null;
        if ( other != null ) {
            defaultBackgroundColor = other.getBackground();
        }
        if ( defaultBackgroundColor == null ) {
            if ( owner.isBackgroundSet() ) // is this is false then the component already has it set to null!
                owner.setBackground(defaultBackgroundColor);
        } else {
            if ( !Objects.equals(owner.getBackground(), defaultBackgroundColor) )
                owner.setBackground(defaultBackgroundColor);
        }
    }

}

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
import swingtree.components.JIcon;
import swingtree.layout.Bounds;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 *  This contains all the logic needed for installing the SwingTree style
 *  configurations on a particular component reasonably gracefully.
 *  SwingTree adds a lot of features on top of
 *  regular AWT/Swing. <br>
 *  But it turns out, this is actually fairly difficult,
 *  because when it comes to rendering the UI, Swing has an all or nothing approach:
 *  Either completely reinstall the look and feel of a component ({@link javax.swing.plaf.ComponentUI}),
 *  or you leave it be. Anything in between is a finicky situation which
 *  is exactly where SwingTree is situated. <br>
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
 *  enable the SwingTree style to be effective. <br>
 *  Naturally, there are various workarounds and hacks to make this work.
 *  The most noteworthy is that when a component is styled to have things rendered in the background layer,
 *  such as a gradient or a shadow. In that case SwingTree needs to paint this stuff
 *  before the Look and Feel renders the contents of the component.
 * <p>
 *  This is not a problem by itself, especially for naturally non-opaque components like {@code JLabel} or
 *  {@link swingtree.components.JBox}, but for opaque components like {@code JPanel} most Look and Feel's
 *  {@link javax.swing.plaf.ComponentUI}s will always fill out the entire background with the AWT background
 *  color taken from {@code Component.getBackground()}.<br>
 *  This is a problem because it will cover up any custom painting done in the background layer of the style!<br>
 * <p>
 *  Now, to overcome this problem, this class implements a special "background color bypass" mechanism that it
 *  applies to opaque components that have background layer styles.
 *  It takes the {@code Component.getBackground()} and sets it in the SwingTree style,
 *  and then sets the AWT background color to {@code UI.Color.UNDEFINED}, which is transparent.
 *  That way SwingTree will effectively take over the responsibility of painting the component background!
 *
 * @param <C> The type of the component.
 */
final class StyleInstaller<C extends JComponent>
{
    private static final Logger log = LoggerFactory.getLogger(StyleInstaller.class);

    private DynamicLaF        _dynamicLaF = DynamicLaF.none(); // Not null, but can be DynamicLaF.none().
    private @Nullable Color   _overriddenBackgroundColor = null;
    private @Nullable Color   _outSideBackgroundColor    = null;
    private @Nullable Color   _lastInsideBackgroundColor = null;
    private @Nullable Boolean _initialIsOpaque           = null;
    private @Nullable Boolean _initialContentAreaFilled  = null;
    private @Nullable Font    _initialFont               = null;
    private @Nullable Color   _initialForeground         = null; // set when a solid font color is routed through the foreground channel (see _applyFontStyleTo)
    private @Nullable Color   _initialViewportBackground = null; // set when the styled background is handed down to a scroll pane viewport (see _restoreViewportBackgroundOf)
    // Remember the component's minimum/maximum size from before the style engine overrode them, so
    // that they can be restored once a (possibly animated/transitional) style stops specifying them.
    // A 'true' ownership flag means the style engine currently holds an explicit override;
    // the matching '_initial*' field holds the value to restore (null == "was not explicitly set").
    // Note: we deliberately do NOT do this for the *preferred* size. The preferred size is only a
    // hint (it cannot pin a component the way a minimum can), and it is also driven by the auto
    // preferred height feature (see TextConf#autoPreferredHeight), which expects the last computed
    // value to stick when it is switched off, rather than snap back to a natural size.
    private boolean            _styleOwnsMinSize = false;
    private boolean            _styleOwnsMaxSize = false;
    private @Nullable Dimension _initialMinSize  = null;
    private @Nullable Dimension _initialMaxSize  = null;

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
        return _dynamicLaF.customLookAndFeelIsInstalled(owner) || _dynamicLaF.currentLookAndFeelSupportsSwingTree(owner);
    }

    static Outline _formerBorderMarginCorrection(JComponent owner) {
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
        final StyleConf   newStyle
    ) {
        /*
            HTML scaling / FontConf injection must run every paint cycle.
            JLabel uses the standard settle-check (works because BasicHTML
            does NOT re-normalise the document model on round-trip).
            JEditorPane uses a settle-guard that extracts our injected CSS
            block rather than comparing whole strings — HTMLEditorKit
            normalises whitespace/comments/structure so string equality
            perpetually fails and would cause an infinite setText loop.
        */
        if ( owner instanceof JLabel ) {
            LabelStyleInstallerUtility._applyHtmlScalingAndStyle(
                (JLabel) owner, newStyle.font()
            );
        } else if ( owner instanceof JEditorPane ) {
            LabelStyleInstallerUtility.htmlSettles(
                (JEditorPane) owner, newStyle.font()
            );
        }

        StyleConf adjustedStyle = newStyle;
        if ( StyleUtil.isUndefinedColor(owner.getBackground()) ) {
            if (owner.isOpaque() && _overriddenBackgroundColor != null && !adjustedStyle.base().backgroundColor().isPresent())
                adjustedStyle = adjustedStyle.backgroundColor(_overriddenBackgroundColor);
        }
        _lastInsideBackgroundColor = owner.getBackground();
        return engine.update(
                Bounds.of(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight()),
                adjustedStyle,
                _formerBorderMarginCorrection(owner)
            );
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

        if ( !doInstallation ) {
            final Outline paddingCorrection = _formerBorderPaddingCorrection(owner, newStyle);
            final Outline adjustedPadding   = newStyle.border().padding().or(paddingCorrection);
            newStyle = newStyle._withBorder(newStyle.border().withPadding(adjustedPadding));

            if ( owner.getBorder() instanceof StyleAndAnimationBorder<?> ) {
                StyleAndAnimationBorder<C> border = (StyleAndAnimationBorder<C>) owner.getBorder();
                border.recalculateInsets(newStyle);
            }

            return _updateEngine(owner, engine, newStyle);
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
               || newStyle.layers().any( (layer, it) -> layer.isOneOf(UI.Layer.BORDER, UI.Layer.CONTENT) && it.noises().any(named -> named.style().get().colors().length > 0 ) )
            );

        final boolean weNeedToInstallTheCustomUI = isStyled && (
               (hasBackFilter && !isSwingTreeComponent) ||
               (hasBaseColors && newStyle.base().requiresCustomUI())
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.shadows().any(named -> named.style().color().isPresent() ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.gradients().any(named -> named.style().colors().length > 0 ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.images().any(named -> named.style().image().isPresent() || named.style().primer().isPresent() ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.painters().any(named -> !Painter.none().equals(named.style().painter()) ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.texts().any(named -> !TextConf.none().equals(named.style()) ) )
               || newStyle.layers().any( (layer, it) -> layer == UI.Layer.BACKGROUND && it.noises().any(named -> named.style().get().colors().length > 0 ) )
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
                _restoreForegroundIfFontColorWasInstalled(owner, newStyle);
                _restoreStyleOwnedSizesOf(owner);
                _restoreViewportBackgroundOf(owner);
                _updateViewportOpaquenessOf(owner, newStyle);

                return _updateEngine(owner, engine, newStyle);
            }
        }

        if ( _initialIsOpaque == null )
            _initialIsOpaque = initialIsOpaqueFlagState; // Important: Use the state of the flag before SwingTree did anything!

        if ( owner instanceof AbstractButton && _initialContentAreaFilled == null )
            _initialContentAreaFilled = initialIsContentAreaFilled;

        final Predicate<UI.ComponentArea> hasGradOrNoise = newStyle::hasOpaqueGradientsOrNoisesOn;
        final boolean hasBackgroundGradients             = newStyle.hasVisibleGradientsOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundNoise                 = newStyle.hasVisibleNoisesOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundPainters              = newStyle.hasPaintersOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundImages                = newStyle.hasImagesOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundShadows               = newStyle.hasVisibleShadows(UI.Layer.BACKGROUND);
        final boolean hasBorderRadius                    = newStyle.border().hasAnyNonZeroArcs();
        final boolean hasBackground                      = newStyle.base().backgroundColor().isPresent();
        final boolean hasMargin                          = newStyle.margin().isPositive();
        final boolean hasOpaqueBorder                    = newStyle.border().colors().isFullyOpaque();
        final boolean isNaturallyTransparent             = !_initialIsOpaque; // We categorize based on the initial state of the flag.
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
                        if ( !Objects.equals( viewport.getBackground(), newColor ) ) {
                            if ( _initialViewportBackground == null )
                                _initialViewportBackground = viewport.getBackground();
                            viewport.setBackground( newColor );
                        }
                    }
                }
            }
        }

        boolean canBeOpaque = true;

        if ( _isTransparentConstant(owner.getBackground()) )
            canBeOpaque = false;

        if ( !hasGradOrNoise.test(UI.ComponentArea.ALL) ) {
            boolean hasOpaqueFoundation = 255 == newStyle.base().foundationColor().map(java.awt.Color::getAlpha).orElse(0);
            boolean hasOpaqueBackground = 255 == newStyle.base().backgroundColor().map( c -> !StyleUtil.isUndefinedColor(c) ? c : _outSideBackgroundColor).map(java.awt.Color::getAlpha).orElse(255);
            boolean hasBorder           = newStyle.border().widths().isPositive();

            if ( !hasOpaqueFoundation && !hasGradOrNoise.test(UI.ComponentArea.EXTERIOR) ) {
                if ( hasBorderRadius )
                    canBeOpaque = false;
                else if ( hasMargin )
                    canBeOpaque = false;
            }

            if ( hasBorder && (!hasOpaqueBorder && !hasGradOrNoise.test(UI.ComponentArea.BORDER)) )
                canBeOpaque = false;

            if (
                !hasOpaqueBackground &&
                !hasGradOrNoise.test(UI.ComponentArea.INTERIOR) &&
                !hasGradOrNoise.test(UI.ComponentArea.BODY)
            )
                canBeOpaque = false;
        }

        final Color   backgroundColor = owner.getBackground();
        final boolean backgroundIsFullyTransparent = backgroundColor == null || backgroundColor.getAlpha() == 0;
        final boolean customLookAndFeelInstalled = customLookAndFeelIsInstalled(owner);
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
                Color currentBackground = owner.getBackground();
                if ( !StyleUtil.isUndefinedColor(currentBackground) )
                    _overriddenBackgroundColor = currentBackground;
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

        _updateViewportOpaquenessOf(owner, newStyle);
        /*
            Note that the above call has to happen after every code path which
            may still change the opaqueness or background color of the owner,
            because the viewport has to mirror the final state of the scroll pane.
        */

        StyleEngine newEngine = _updateEngine(owner, engine, newStyle);

        _applyFontStyleTo(owner, newStyle);

        return newEngine;
    }

    /**
     *  The viewport of a scroll pane is opaque by default, which is a promise to Swing
     *  that it will fill every single pixel of its bounds. A scroll pane, on the other hand,
     *  may very well be styled to be (partially) transparent, in which case the style engine
     *  hands the styled background color over to the viewport (see the {@code JScrollPane}
     *  case further above), because the viewport lies on top of the scroll pane background.
     *  <p>
     *  A viewport which fills its bounds with a transparent color paints little or nothing,
     *  and yet, as long as it is flagged as opaque, Swing will not repaint the ancestors
     *  behind it. Whatever was painted there before consequently survives every repaint,
     *  which makes the renderings of successive paints pile up into ever growing artifacts.
     *  This is especially noticeable when there are animations inside the scroll pane.
     *  <p>
     *  So a viewport may never claim to be opaque when the scroll pane behind it does not,
     *  or when the color it would fill its bounds with is not fully opaque.
     *  The reverse is just as important though: as soon as the style of the scroll pane
     *  turns opaque again, the viewport has to reclaim its opaqueness, or else Swing's
     *  repaint manager keeps climbing past it to find an opaque ancestor, and every
     *  little repaint inside the scroll pane costs more than it should, forever.
     *  <p>
     *  There is a third party in this though, namely the style engine itself:
     *  Everything it renders for a scroll pane, be it an inset shadow, a gradient
     *  or a rounded background, is painted before the children of the scroll pane,
     *  which means the viewport lies on top of all of it.
     *  An opaque viewport would simply wipe these renderings away with its flat
     *  background color, so it may only make its promise when the style engine
     *  has nothing to show underneath it.
     *
     * @param owner The component whose viewport should be synchronized, only scroll panes have one.
     * @param style The style of the owner, which tells us if the style engine paints below the viewport.
     */
    private void _updateViewportOpaquenessOf( final C owner, final StyleConf style ) {
        if ( !(owner instanceof JScrollPane) )
            return;

        JViewport viewport = ((JScrollPane) owner).getViewport();
        if ( viewport == null )
            return;

        Color   viewportBackground   = viewport.getBackground();
        boolean viewportFillsItsArea = viewportBackground != null && viewportBackground.getAlpha() == 255;
        boolean shouldBeOpaque       = owner.isOpaque() && viewportFillsItsArea && !_stylePaintsBelowViewportOf(style);
        /*
            Note that a viewport without its own background color inherits the one of
            the scroll pane, which the style engine may well have set to the fully
            transparent 'UI.Color.UNDEFINED' so that it can paint the background itself.
            The viewport cannot do that painting, so it also cannot claim to be opaque.
        */
        if ( viewport.isOpaque() != shouldBeOpaque )
            viewport.setOpaque(shouldBeOpaque);
    }

    /**
     *  Determines if the supplied style makes the style engine render anything
     *  in the area which the viewport of a scroll pane covers.
     *  This is the case for all layers except the foreground layer, because they
     *  are all painted before the children of a component, and it is also the case
     *  for a border radius, whose rounded corners reach into the rectangular
     *  bounds of the viewport.
     *
     * @param style The style of a scroll pane.
     * @return {@code true} if the style engine paints something below the viewport.
     */
    private static boolean _stylePaintsBelowViewportOf( final StyleConf style ) {
        return style.border().hasAnyNonZeroArcs()
            || !style.layer(UI.Layer.BACKGROUND).isNone()
            || !style.layer(UI.Layer.CONTENT).isNone()
            || !style.layer(UI.Layer.BORDER).isNone();
    }

    /**
     *  Gives the viewport of a scroll pane its original background color back,
     *  the one the style engine took away from it when it handed the styled
     *  background color down to the viewport.
     *  Without this, a scroll pane whose styling was removed again would keep
     *  a translucent viewport, and with it the loss of opaqueness
     *  which {@link #_updateViewportOpaquenessOf(JComponent, StyleConf)} derives from it.
     *
     * @param owner The component whose viewport should be restored, only scroll panes have one.
     */
    private void _restoreViewportBackgroundOf( final C owner ) {
        if ( !(owner instanceof JScrollPane) || _initialViewportBackground == null )
            return;

        JViewport viewport = ((JScrollPane) owner).getViewport();
        if ( viewport != null && !Objects.equals( viewport.getBackground(), _initialViewportBackground ) )
            viewport.setBackground(_initialViewportBackground);

        _initialViewportBackground = null;
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

        // A solid *font* color is the more specific property and wins over the base foreground
        // color — it is applied through the same channel by _applyFontStyleTo (which runs after
        // this), so applying the base color here would only cause a set/override ping-pong.
        boolean fontColorTakesPrecedence = styleConf.font().solidColor() != null;

        if ( !fontColorTakesPrecedence && base.foregroundColor().isPresent() && !Objects.equals( owner.getForeground(), base.foregroundColor().get() ) ) {
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

    /**
     *  Restores the component's minimum/maximum sizes to what they were before the style engine first
     *  overrode them. This is needed because the style engine sets these sizes additively; without an
     *  explicit restore, a stale override (e.g. one left behind by a transitional/animated style such
     *  as a fold animation) would keep the component from shrinking to fit its content.
     */
    private void _restoreStyleOwnedSizesOf( final C owner ) {
        boolean changed = false;
        if ( _styleOwnsMinSize ) {
            owner.setMinimumSize(_initialMinSize);
            _styleOwnsMinSize = false;
            _initialMinSize = null;
            changed = true;
        }
        if ( _styleOwnsMaxSize ) {
            owner.setMaximumSize(_initialMaxSize);
            _styleOwnsMaxSize = false;
            _initialMaxSize = null;
            changed = true;
        }
        if ( changed && owner.getParent() != null )
            owner.getParent().revalidate();
    }

    private void _applyDimensionalityStyleTo( final C owner, final StyleConf styleConf )
    {
        final DimensionalityConf dimensionalityConf = styleConf.dimensionality();

        if ( dimensionalityConf.minWidth().isPresent() || dimensionalityConf.minHeight().isPresent() ) {
            if ( !_styleOwnsMinSize ) {
                _initialMinSize = owner.isMinimumSizeSet() ? owner.getMinimumSize() : null;
                _styleOwnsMinSize = true;
            }
            Dimension minSize = owner.getMinimumSize();

            int minWidth  = dimensionalityConf.minWidth().orElse(minSize == null ? 0 : minSize.width);
            int minHeight = dimensionalityConf.minHeight().orElse(minSize == null ? 0 : minSize.height);

            Dimension newMinSize = new Dimension(minWidth, minHeight);

            if ( ! newMinSize.equals(minSize) )
                owner.setMinimumSize(newMinSize);
        }
        else if ( _styleOwnsMinSize ) {
            // The style no longer specifies a minimum size, so we restore the original one.
            // Without this, a stale override (e.g. left by a transitional/animated style)
            // would keep the component from shrinking to fit its (possibly reduced) content.
            owner.setMinimumSize(_initialMinSize);
            _styleOwnsMinSize = false;
            _initialMinSize = null;
            if ( owner.getParent() != null )
                owner.getParent().revalidate();
        }

        if ( dimensionalityConf.maxWidth().isPresent() || dimensionalityConf.maxHeight().isPresent() ) {
            if ( !_styleOwnsMaxSize ) {
                _initialMaxSize = owner.isMaximumSizeSet() ? owner.getMaximumSize() : null;
                _styleOwnsMaxSize = true;
            }
            Dimension maxSize = owner.getMaximumSize();

            int maxWidth  = dimensionalityConf.maxWidth().orElse(maxSize == null  ? Integer.MAX_VALUE : maxSize.width);
            int maxHeight = dimensionalityConf.maxHeight().orElse(maxSize == null ? Integer.MAX_VALUE : maxSize.height);

            Dimension newMaxSize = new Dimension(maxWidth, maxHeight);

            if ( !newMaxSize.equals(maxSize) )
                owner.setMaximumSize(newMaxSize);
        }
        else if ( _styleOwnsMaxSize ) {
            owner.setMaximumSize(_initialMaxSize);
            _styleOwnsMaxSize = false;
            _initialMaxSize = null;
            if ( owner.getParent() != null )
                owner.getParent().revalidate();
        }

        if ( dimensionalityConf.preferredWidth().isPresent() || dimensionalityConf.preferredHeight().isPresent() ) {
            Dimension prefSize = owner.getPreferredSize();

            int prefWidth  = dimensionalityConf.preferredWidth().orElse(prefSize == null ? 0 : prefSize.width);
            int prefHeight = dimensionalityConf.preferredHeight().orElse(prefSize == null ? 0 : prefSize.height);

            Dimension newPrefSize = new Dimension(prefWidth, prefHeight);

            if ( !newPrefSize.equals(prefSize) ) {
                owner.setPreferredSize(newPrefSize);
                // Trigger a re-layout of the parent container, because preferred size changes can affect the layout:
                if ( owner.getParent() != null )
                    owner.getParent().revalidate();
            }
            // Note: unlike the minimum/maximum size, we intentionally do NOT remember and restore the
            // preferred size when the style stops specifying it. The preferred size is only a hint and
            // is also driven by the auto preferred height feature (TextConf#autoPreferredHeight), which
            // expects the last computed value to remain in place when it is switched off.
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
            _restoreForegroundIfFontColorWasInstalled(owner, styleConf);
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
             .createDerivedWithoutSolidColorFrom(owner.getFont(), owner)
             .ifPresent( newFont -> {
                    if ( !newFont.equals(owner.getFont()) )
                        owner.setFont( newFont );
                });

        /*
            A SOLID font color deliberately does not travel inside the font (it would flip
            'Font.hasLayoutAttributes()' and put every measure/draw of this component on the
            expensive TextLayout path, and it would override the LaF's state colors) — it is
            applied through Swing's native channel for text color instead: the foreground
            property, which is exactly what a LaF consults when painting enabled text.
            It wins over the base style's 'foregroundColor' (the more specific property
            takes precedence, see _applyGenericBaseStyleTo), and the pre-style foreground
            is remembered and restored just like '_initialFont' above.
        */
        Color solidFontColor = fontConf.solidColor();
        if ( solidFontColor != null ) {
            if ( _initialForeground == null )
                _initialForeground = owner.getForeground();
            if ( !Objects.equals(owner.getForeground(), solidFontColor) )
                owner.setForeground(solidFontColor);
        }
        else
            _restoreForegroundIfFontColorWasInstalled(owner, styleConf);

        _installLayoutInfoFromFontConf(fontConf, owner);
    }

    /** Ends the solid font color's ownership of the foreground property. If the current style
     *  still defines a base 'foregroundColor', that base color — already applied earlier in this
     *  very installation cycle by _applyGenericBaseStyleTo — takes over the channel, so restoring
     *  the remembered pre-style value here would wrongly override it; the memory is just dropped
     *  (matching the base color's own set-only semantics). Only when nothing else claims the
     *  channel is the pre-style foreground actually restored. */
    private void _restoreForegroundIfFontColorWasInstalled( C owner, StyleConf styleConf ) {
        if ( _initialForeground != null ) {
            if ( styleConf.base().foregroundColor().isPresent() )
                _initialForeground = null; // the base style owns the foreground now
            else {
                if ( !Objects.equals(owner.getForeground(), _initialForeground) )
                    owner.setForeground(_initialForeground);
                _initialForeground = null;
            }
        }
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
            log.debug(SwingTree.get().logMarker(),
                    "Failed to instantiate component '{}' as part of an " +
                    "attempt to get the default color of said type!",
                    type.getName(), e
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

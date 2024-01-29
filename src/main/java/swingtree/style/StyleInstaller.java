package swingtree.style;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.DimConstraint;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;
import swingtree.UI;
import swingtree.components.JIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *  Contains all the logic for installing a style on a component.
 * @param <C> The type of the component.
 */
final class StyleInstaller<C extends JComponent>
{
    private DynamicLaF _dynamicLaF = DynamicLaF.none(); // Not null, but can be DynamicLaF.none().
    private Color      _initialBackgroundColor = null;
    private Boolean    _initialIsOpaque = null;
    private Boolean    _initialContentAreaFilled = null;


    void installCustomBorderBasedStyleAndAnimationRenderer(C owner ) {
        Border currentBorder = owner.getBorder();
        if ( !(currentBorder instanceof StyleAndAnimationBorder) )
            owner.setBorder(new StyleAndAnimationBorder<>(ComponentExtension.from(owner), currentBorder));
    }

    void installCustomUIFor( C owner ) {
        _dynamicLaF.installCustomUIFor(owner);
    }

    boolean customLookAndFeelIsInstalled() {
        return _dynamicLaF.customLookAndFeelIsInstalled();
    }

    Style applyStyleToComponentState( C owner, Style newStyle, StyleSource<C> styleSource, StyleEngine styleEngine )
    {
        final Style.Report styleReport = newStyle.getReport();

        boolean isNotStyled                     = styleReport.isNotStyled();
        boolean onlyDimensionalityIsStyled      = styleReport.onlyDimensionalityIsStyled();
        boolean styleCanBeRenderedThroughBorder = (
                                                       styleReport.noBaseStyle    &&
                                                       (styleReport.noShadowStyle || styleReport.allShadowsAreBorderShadows)     &&
                                                       (styleReport.noPainters    || styleReport.allPaintersAreBorderPainters)   &&
                                                       (styleReport.noGradients   || styleReport.allGradientsAreBorderGradients) &&
                                                       (styleReport.noImages      || styleReport.allImagesAreBorderImages)
                                                   );

        if ( isNotStyled || onlyDimensionalityIsStyled ) {
            _dynamicLaF = _dynamicLaF._uninstallCustomLaF(owner);
            if ( styleSource.hasNoAnimationStylers() && styleEngine.hasNoPainters() )
                _uninstallCustomBorderBasedStyleAndAnimationRenderer(owner);
            if ( _initialBackgroundColor != null ) {
                if ( !Objects.equals( owner.getBackground(), _initialBackgroundColor ) )
                    owner.setBackground(_initialBackgroundColor);
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

        if ( _initialIsOpaque == null )
            _initialIsOpaque = owner.isOpaque();

        if ( owner instanceof AbstractButton && _initialContentAreaFilled == null )
            _initialContentAreaFilled = ((AbstractButton) owner).isContentAreaFilled();

        final List<UI.ComponentArea> opaqueGradientAreas = newStyle.gradientCoveredAreas();
        final boolean hasBackgroundGradients             = newStyle.hasVisibleGradientsOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundPainters              = newStyle.hasPaintersOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundImages                = newStyle.hasImagesOnLayer(UI.Layer.BACKGROUND);
        final boolean hasBackgroundShadows               = newStyle.hasVisibleShadows(UI.Layer.BACKGROUND);
        final boolean hasBorderRadius                    = newStyle.border().hasAnyNonZeroArcs();
        final boolean hasBackground                      = newStyle.base().backgroundColor().isPresent();
        final boolean hasMargin                          = newStyle.margin().isPositive();
        final boolean hasOpaqueBorder                    = !(255 > newStyle.border().color().map(Color::getAlpha).orElse(0));


        if ( !hasBackground && _initialIsOpaque ) {
            // If the style has a border radius set we need to make sure that we have a background color:
            if ( hasBorderRadius || newStyle.border().margin().isPositive() ) {
                _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor : owner.getBackground();
                newStyle = newStyle.backgroundColor(_initialBackgroundColor);
            }
        }

        if ( hasBackground ) {
            boolean backgroundIsAlreadySet = Objects.equals( owner.getBackground(), newStyle.base().backgroundColor().get() );
            if ( !backgroundIsAlreadySet || newStyle.base().backgroundColor().get() == UI.COLOR_UNDEFINED )
            {
                _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  owner.getBackground();
                Color newColor =  newStyle.base().backgroundColor().get();
                if ( newColor == UI.COLOR_UNDEFINED)
                    newColor = null;

                if ( !Objects.equals( owner.getBackground(), newColor ) ) {
                    owner.setBackground( newColor );
                    if ( owner instanceof JScrollPane ) {
                        JScrollPane scrollPane = (JScrollPane) owner;
                        if ( scrollPane.getViewport() != null ) {
                            newColor = newStyle.base().backgroundColor().get();
                            if ( newColor == UI.COLOR_UNDEFINED)
                                newColor = null;

                            JViewport viewport = scrollPane.getViewport();
                            if ( !Objects.equals( viewport.getBackground(), newColor ) )
                                viewport.setBackground( newColor );
                        }
                    }
                }
            }
        }

        if ( !onlyDimensionalityIsStyled ) {
            installCustomBorderBasedStyleAndAnimationRenderer(owner);
            if ( !styleCanBeRenderedThroughBorder )
                _dynamicLaF = _dynamicLaF.establishLookAndFeelFor(newStyle, owner);
        }

        boolean canBeOpaque = true;

        if ( !opaqueGradientAreas.contains(UI.ComponentArea.ALL) ) {
            boolean hasOpaqueFoundation = 255 == newStyle.base().foundationColor().map(Color::getAlpha).orElse(0);
            boolean hasOpaqueBackground = 255 == newStyle.base().backgroundColor().map( c -> c != UI.COLOR_UNDEFINED ? c : _initialBackgroundColor ).map(Color::getAlpha).orElse(255);
            boolean hasBorder           = newStyle.border().widths().isPositive();

            if ( !hasOpaqueFoundation && !opaqueGradientAreas.contains(UI.ComponentArea.EXTERIOR) ) {
                if ( hasBorderRadius )
                    canBeOpaque = false;
                else if ( hasMargin )
                    canBeOpaque = false;
            }

            if ( hasBorder && (!hasOpaqueBorder && !opaqueGradientAreas.contains(UI.ComponentArea.BORDER)) )
                canBeOpaque = false;

            if (
                !hasOpaqueBackground &&
                !opaqueGradientAreas.contains(UI.ComponentArea.INTERIOR) &&
                !opaqueGradientAreas.contains(UI.ComponentArea.BODY)
            )
                canBeOpaque = false;
        }

        if ( !canBeOpaque ) {
            if ( owner.isOpaque() )
                owner.setOpaque(false);
        }
        else
        {
            boolean isSwingTreeComponent = _isNestedClassInUINamespace(owner);

            if ( !isSwingTreeComponent ) {
                if ( owner.isOpaque() )
                    owner.setOpaque(false);
            } else {
                if ( !owner.isOpaque() )
                    owner.setOpaque(true);

                boolean customBackgroundFucksUpComponent = ( owner instanceof JTabbedPane );

                if ( !customBackgroundFucksUpComponent ) {
                    boolean requiresBackgroundPainting = hasBackgroundGradients;
                    requiresBackgroundPainting = requiresBackgroundPainting || hasBackgroundShadows;
                    requiresBackgroundPainting = requiresBackgroundPainting || hasBackgroundPainters;
                    requiresBackgroundPainting = requiresBackgroundPainting || hasBackgroundImages;
                    requiresBackgroundPainting = requiresBackgroundPainting || hasBorderRadius;
                    requiresBackgroundPainting = requiresBackgroundPainting || hasMargin;

                    if ( requiresBackgroundPainting && !Objects.equals( owner.getBackground(), UI.COLOR_UNDEFINED ) )
                        owner.setBackground(UI.COLOR_UNDEFINED);
                    /*
                        The above line looks very strange, but it is very important!
                        To understand what is going on here, you have to know that when a component is
                        flagged to be opaque, then every Swing look and feel will, before painting
                        anything else, first fill out the entire background of the component with
                        the background color of the component.

                        Now this is a problem when you have the background layer of your SwingTree component
                        styled using various things like gradients, shadows, images, etc.

                        We could simply set the opaque flag to false, but then we would lose the
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
            }
        }

        if ( _dynamicLaF.overrideWasNeeded() ) {
            if ( owner instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) owner;

                boolean shouldButtonBeFilled = !hasBackgroundGradients && !hasBackgroundPainters;

                if ( shouldButtonBeFilled != b.isContentAreaFilled() )
                    b.setContentAreaFilled( shouldButtonBeFilled );
            }
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

        return newStyle;
    }

    private void _applyGenericBaseStyleTo( final C owner, final Style styleConf )
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

    private void _applyIconStyleTo( final C owner, Style styleConf )
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

    private void _applyLayoutStyleTo( final C owner, final Style styleConf )
    {
        final LayoutStyle style = styleConf.layout();

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

    private void _applyDimensionalityStyleTo( final C owner, final Style styleConf )
    {
        final DimensionalityStyle dimensionalityStyle = styleConf.dimensionality();

        if ( dimensionalityStyle.minWidth().isPresent() || dimensionalityStyle.minHeight().isPresent() ) {
            Dimension minSize = owner.getMinimumSize();

            int minWidth  = dimensionalityStyle.minWidth().orElse(minSize == null ? 0 : minSize.width);
            int minHeight = dimensionalityStyle.minHeight().orElse(minSize == null ? 0 : minSize.height);

            Dimension newMinSize = new Dimension(minWidth, minHeight);

            if ( ! newMinSize.equals(minSize) )
                owner.setMinimumSize(newMinSize);
        }

        if ( dimensionalityStyle.maxWidth().isPresent() || dimensionalityStyle.maxHeight().isPresent() ) {
            Dimension maxSize = owner.getMaximumSize();

            int maxWidth  = dimensionalityStyle.maxWidth().orElse(maxSize == null  ? Integer.MAX_VALUE : maxSize.width);
            int maxHeight = dimensionalityStyle.maxHeight().orElse(maxSize == null ? Integer.MAX_VALUE : maxSize.height);

            Dimension newMaxSize = new Dimension(maxWidth, maxHeight);

            if ( ! newMaxSize.equals(maxSize) )
                owner.setMaximumSize(newMaxSize);
        }

        if ( dimensionalityStyle.preferredWidth().isPresent() || dimensionalityStyle.preferredHeight().isPresent() ) {
            Dimension prefSize = owner.getPreferredSize();

            int prefWidth  = dimensionalityStyle.preferredWidth().orElse(prefSize == null ? 0 : prefSize.width);
            int prefHeight = dimensionalityStyle.preferredHeight().orElse(prefSize == null ? 0 : prefSize.height);

            Dimension newPrefSize = new Dimension(prefWidth, prefHeight);

            if ( !newPrefSize.equals(prefSize) )
                owner.setPreferredSize(newPrefSize);
        }

        if ( dimensionalityStyle.width().isPresent() || dimensionalityStyle.height().isPresent() ) {
            Dimension size = owner.getSize();

            int width  = dimensionalityStyle.width().orElse(size == null ? 0 : size.width);
            int height = dimensionalityStyle.height().orElse(size == null ? 0 : size.height);

            Dimension newSize = new Dimension(width, height);

            if ( ! newSize.equals(size) )
                owner.setSize(newSize);
        }
    }

    private void _applyFontStyleTo( final C owner, final Style styleConf )
    {
        final FontStyle fontStyle = styleConf.font();

        if ( owner instanceof JTextComponent ) {
            JTextComponent tc = (JTextComponent) owner;
            if ( fontStyle.selectionColor().isPresent() && ! Objects.equals( tc.getSelectionColor(), fontStyle.selectionColor().get() ) )
                tc.setSelectionColor(fontStyle.selectionColor().get());
        }

        fontStyle
             .createDerivedFrom(owner.getFont())
             .ifPresent( newFont -> {
                    if ( !newFont.equals(owner.getFont()) )
                        owner.setFont( newFont );
                });

        fontStyle.horizontalAlignment().ifPresent( alignment -> {
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
        fontStyle.verticalAlignment().ifPresent( alignment -> {
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

    private void _applyPropertiesTo( final C owner, final Style styleConf ) {
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

    private void _doComboBoxMarginAdjustment( final C owner, final Style styleConf ) {
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

    private boolean _isNestedClassInUINamespace( C owner ) {
        Class<UI> uiClass = UI.class;
        Class<?>  componentClass = owner.getClass();
        /*
            Inside the UI namespace are nested classes extending various Swing components.
            Here we check if the component is one of those nested classes.
        */
        while ( componentClass != null ) {
            if ( componentClass.getEnclosingClass() == uiClass )
                return true;
            componentClass = componentClass.getSuperclass();
        }
        return false;
    }

    private void _uninstallCustomBorderBasedStyleAndAnimationRenderer( C owner ) {
        Border currentBorder = owner.getBorder();
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

}

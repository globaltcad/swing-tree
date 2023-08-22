package swingtree.style;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.DimConstraint;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;
import swingtree.UI;
import swingtree.animation.AnimationState;
import swingtree.api.Painter;
import swingtree.api.Styler;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 *  Is attached to UI components in the form of a client property.
 *  It exists to give Swing-Tree components some custom style and animation capabilities.
 */
public final class ComponentExtension<C extends JComponent>
{
    /**
     * Returns the {@link ComponentExtension} associated with the given component.
     * If the component does not have an extension, a new one is created and associated with the component.
     */
    public static <C extends JComponent> ComponentExtension<C> from( C comp ) {
        ComponentExtension<C> ext = (ComponentExtension<C>) comp.getClientProperty( ComponentExtension.class );
        if ( ext == null ) {
            ext = new ComponentExtension<>(comp);
            comp.putClientProperty( ComponentExtension.class, ext );
        }
        return ext;
    }

    public static void makeSureComponentHasExtension( JComponent comp ) { from(comp); }

    private final C _owner;

    private final List<Expirable<Painter>> _animationPainters = new ArrayList<>(0);

    private final List<String> _styleGroups = new ArrayList<>(0);

    private StylePainter<C> _stylePainter = StylePainter.none();

    private DynamicLaF _laf = DynamicLaF.none();

    private StyleSource<C> _styleSource = StyleSource.create();

    private Color _initialBackgroundColor = null;


    private ComponentExtension( C owner ) { _owner = Objects.requireNonNull(owner); }

    C getOwner() { return _owner; }

    Shape getMainClip() { return _stylePainter.getMainClip(); }

    public void addStyling( Styler<C> styler ) {
        Objects.requireNonNull(styler);
        _styleSource = _styleSource.withLocalStyler(styler, _owner);
        calculateApplyAndInstallStyle(false);
    }

    public Style calculateStyle() {
        return _styleSource.calculateStyleFor(_owner);
    }

    private Style _calculateAndApplyStyle(boolean force) {
        return applyStyleToComponentState(calculateStyle(), force);
    }

    public void calculateApplyAndInstallStyle( boolean force ) {
        installStyle(_calculateAndApplyStyle(force));
    }

    public void applyAndInstallStyle( Style style, boolean force ) {
        installStyle(applyStyleToComponentState(style, force));
    }

    public void installStyle( Style style ) {
        _stylePainter = StylePainter.none(); // We reset the style painter so that the style is applied again!
        _stylePainter = _stylePainter.update(style);
    }

    public void setStyleGroups( String... styleName ) {
        Objects.requireNonNull(styleName);
        if ( !_styleGroups.isEmpty() )
            throw new IllegalStateException("Style groups already specified!");

        _styleGroups.addAll( java.util.Arrays.asList(styleName) );
    }

    public List<String> getStyleGroups() { return Collections.unmodifiableList(_styleGroups); }

    public Style getStyle() { return _stylePainter.getStyle(); }

    public void clearAnimations() {
        _animationPainters.clear();
        _styleSource = _styleSource.withoutAnimationStylers();
    }

    public void addAnimationPainter( AnimationState state, swingtree.api.Painter painter ) {
        _animationPainters.add(new Expirable<>(Objects.requireNonNull(state.lifetime()), Objects.requireNonNull(painter)));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    public void addAnimationStyler( AnimationState state, Styler<C> styler ) {
        _styleSource = _styleSource.withAnimationStyler(state.lifetime(), styler);
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    public void updateUI() { _laf.updateUIFor(_owner); }

    void establishStyleAndBeginPainting( Graphics g ) {
        Style style = _calculateAndApplyStyle(false);
        _stylePainter = _stylePainter.beginPaintingWith( style, g );
    }

    public void paintBackgroundStyle( Graphics g )
    {
        if ( _laf.customLookAndFeelIsInstalled() )
            return; // We render through the custom installed UI!

        if ( _componentIsDeclaredInUI(_owner) )
            _paintBackground(g);
        else
            _stylePainter = _stylePainter.endPainting(); // custom style rendering unfortunately not possible for this component :/
    }

    void _paintBackground(Graphics g)
    {
        // If end the painting of the last painting cycle if it was not already ended:
        _stylePainter = _stylePainter.endPainting();

        establishStyleAndBeginPainting(g);

        _stylePainter.renderBackgroundStyle( (Graphics2D) g, _owner );
    }

    void paintBorderStyle( Graphics2D g2d, JComponent component ) {
        _stylePainter.paintBorderStyle(g2d, component);
    }

    public void _renderAnimations( Graphics2D g2d )
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        if ( StylePainter.DO_ANTIALIASING() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // Animations are last: they are rendered on top of everything else:
        for ( Expirable<Painter> expirablePainter : new ArrayList<>(_animationPainters) )
            if ( expirablePainter.isExpired() )
                _animationPainters.remove(expirablePainter);
            else {
                try {
                    expirablePainter.get().paint(g2d);
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a painter should not prevent everything else from being painted!
                }
            }

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    public void paintForegroundStyle( Graphics2D g2d )
    {
        establishStyleAndBeginPainting(g2d);

        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Reset antialiasing to its previous state:
        if ( StylePainter.DO_ANTIALIASING() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // We remember the clip:
        Shape formerClip = g2d.getClip();

        _stylePainter.paintForegroundStyle(g2d, _owner);

        // We restore the clip:
        if ( g2d.getClip() != formerClip )
            g2d.setClip(formerClip);

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    public Style applyStyleToComponentState( Style style, boolean force )
    {
        _styleSource = _styleSource.withoutExpiredAnimationStylers(); // Clean up expired animation stylers!

        Objects.requireNonNull(style);

        if ( _owner.getBorder() instanceof StyleAndAnimationBorder<?> ) {
            StyleAndAnimationBorder<C> border = (StyleAndAnimationBorder<C>) _owner.getBorder();
            border.recalculateInsets(style);
        }

        if ( _stylePainter.getStyle().equals(style) && !force )
            return style;

        final Style.Report styleReport = style.getReport();

        boolean isNotStyled                     = styleReport.isNotStyled();
        boolean onlyDimensionalityIsStyled      = styleReport.onlyDimensionalityIsStyled();
        boolean styleCanBeRenderedThroughBorder = (
                                                       styleReport.noBaseStyle    &&
                                                       (styleReport.noShadowStyle || styleReport.allShadowsAreBorderShadows)     &&
                                                       (styleReport.noPainters    || styleReport.allPaintersAreBorderPainters)   &&
                                                       (styleReport.noGradients   || styleReport.allGradientsAreBorderGradients) &&
                                                       (styleReport.noImages      || styleReport.allImagesAreBorderImages)
                                                   );

        if ( _owner instanceof JTextField && style.margin().isPositive() )
            styleCanBeRenderedThroughBorder = false;

        if ( isNotStyled || onlyDimensionalityIsStyled ) {
            _laf = _laf._uninstallCustomLaF(_owner);
            if ( _styleSource.hasNoAnimationStylers() && _animationPainters.isEmpty() )
                _uninstallCustomBorderBasedStyleAndAnimationRenderer();
            if ( _initialBackgroundColor != null ) {
                _owner.setBackground(_initialBackgroundColor);
                _initialBackgroundColor = null;
            }
            if ( isNotStyled )
                return style;
        }

        boolean hasBorderRadius = style.border().hasAnyNonZeroArcs();
        boolean hasBackground   = style.base().backgroundColor().isPresent();

        if ( hasBackground && !Objects.equals( _owner.getBackground(), style.base().backgroundColor().get() ) ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            _owner.setBackground( style.base().backgroundColor().get() );
        }

        // If the style has a border radius set we need to make sure that we have a background color:
        if ( hasBorderRadius && !hasBackground ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            style = style.backgroundColor(_initialBackgroundColor);
        }

        if ( style.base().foregroundColo().isPresent() && !Objects.equals( _owner.getForeground(), style.base().foregroundColo().get() ) )
            _owner.setForeground( style.base().foregroundColo().get() );

        style.base().cursor().ifPresent( cursor -> {
            if ( !Objects.equals( _owner.getCursor(), cursor ) )
                _owner.setCursor( cursor );
        });

        style.layout().alignmentX().ifPresent( alignmentX -> {
            if ( !Objects.equals( _owner.getAlignmentX(), alignmentX ) )
                _owner.setAlignmentX( alignmentX );
        });

        style.layout().alignmentY().ifPresent( alignmentY -> {
            if ( !Objects.equals( _owner.getAlignmentY(), alignmentY ) )
                _owner.setAlignmentY( alignmentY );
        });

        style.layout().layout().installFor( _owner );

        _applyAlignmentToMigLayoutIfItExists(style.layout());

        if ( style.dimensionality().minWidth().isPresent() || style.dimensionality().minHeight().isPresent() ) {
            Dimension minSize = _owner.getMinimumSize();

            int minWidth  = style.dimensionality().minWidth().orElse(minSize == null ? 0 : minSize.width);
            int minHeight = style.dimensionality().minHeight().orElse(minSize == null ? 0 : minSize.height);

            Dimension newMinSize = new Dimension(minWidth, minHeight);

            if ( ! newMinSize.equals(minSize) )
                _owner.setMinimumSize(newMinSize);
        }

        if ( style.dimensionality().maxWidth().isPresent() || style.dimensionality().maxHeight().isPresent() ) {
            Dimension maxSize = _owner.getMaximumSize();

            int maxWidth  = style.dimensionality().maxWidth().orElse(maxSize == null  ? Integer.MAX_VALUE : maxSize.width);
            int maxHeight = style.dimensionality().maxHeight().orElse(maxSize == null ? Integer.MAX_VALUE : maxSize.height);

            Dimension newMaxSize = new Dimension(maxWidth, maxHeight);

            if ( ! newMaxSize.equals(maxSize) )
                _owner.setMaximumSize(newMaxSize);
        }

        if ( style.dimensionality().preferredWidth().isPresent() || style.dimensionality().preferredHeight().isPresent() ) {
            Dimension prefSize = _owner.getPreferredSize();

            int prefWidth  = style.dimensionality().preferredWidth().orElse(prefSize == null ? 0 : prefSize.width);
            int prefHeight = style.dimensionality().preferredHeight().orElse(prefSize == null ? 0 : prefSize.height);

            Dimension newPrefSize = new Dimension(prefWidth, prefHeight);

            if ( !newPrefSize.equals(prefSize) )
                _owner.setPreferredSize(newPrefSize);
        }

        if ( style.dimensionality().width().isPresent() || style.dimensionality().height().isPresent() ) {
            Dimension size = _owner.getSize();

            int width  = style.dimensionality().width().orElse(size == null ? 0 : size.width);
            int height = style.dimensionality().height().orElse(size == null ? 0 : size.height);

            Dimension newSize = new Dimension(width, height);

            if ( ! newSize.equals(size) )
                _owner.setSize(newSize);
        }

        if ( _owner instanceof JTextComponent ) {
            JTextComponent tc = (JTextComponent) _owner;
            if ( style.font().selectionColor().isPresent() && ! Objects.equals( tc.getSelectionColor(), style.font().selectionColor().get() ) )
                tc.setSelectionColor(style.font().selectionColor().get());
        }

        if ( _owner instanceof JComboBox ) {
            int bottom = style.margin().bottom().orElse(0);
            // We adjust the position of the popup menu:
            try {
                Point location = _owner.getLocationOnScreen();
                int x = location.x;
                int y = location.y + _owner.getHeight() - bottom;
                JComboBox<?> comboBox = (JComboBox<?>) _owner;
                JPopupMenu popup = (JPopupMenu) comboBox.getAccessibleContext().getAccessibleChild(0);
                Point oldLocation = popup.getLocation();
                if ( popup.isShowing() && (oldLocation.x != x || oldLocation.y != y) )
                    popup.setLocation(x, y);
            } catch ( Exception e ) {
                // ignore
            }
        }

        style.font()
             .createDerivedFrom(_owner.getFont())
             .ifPresent( newFont -> {
                    if ( !newFont.equals(_owner.getFont()) )
                        _owner.setFont( newFont );
                });

        style.font().horizontalAlignment().ifPresent( alignment -> {
            if ( _owner instanceof JLabel ) {
                JLabel label = (JLabel) _owner;
                if ( !Objects.equals( label.getHorizontalAlignment(), alignment.forSwing() ) )
                    label.setHorizontalAlignment( alignment.forSwing() );
            }
            if ( _owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) _owner;
                if ( !Objects.equals( button.getHorizontalAlignment(), alignment.forSwing() ) )
                    button.setHorizontalAlignment( alignment.forSwing() );
            }
            if ( _owner instanceof JTextField ) {
                JTextField textField = (JTextField) _owner;
                if ( !Objects.equals( textField.getHorizontalAlignment(), alignment.forSwing() ) )
                    textField.setHorizontalAlignment( alignment.forSwing() );
            }
        });
        style.font().verticalAlignment().ifPresent( alignment -> {
            if ( _owner instanceof JLabel ) {
                JLabel label = (JLabel) _owner;
                if ( !Objects.equals( label.getVerticalAlignment(), alignment.forSwing() ) )
                    label.setVerticalAlignment( alignment.forSwing() );
            }
            if ( _owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) _owner;
                if ( !Objects.equals( button.getVerticalAlignment(), alignment.forSwing() ) )
                    button.setVerticalAlignment( alignment.forSwing() );
            }
        });

        if ( !onlyDimensionalityIsStyled ) {
            _installCustomBorderBasedStyleAndAnimationRenderer();
            if ( !styleCanBeRenderedThroughBorder )
                _laf = _laf.establishLookAndFeelFor(style, _owner);
        }

        if ( style.hasCustomForegroundPainters() )
            _makeAllChildrenTransparent(_owner);

        if ( style.hasActiveBackgroundGradients() && _owner.isOpaque() )
            _owner.setOpaque(false);

        style.properties().forEach( property -> {

            Object oldValue = _owner.getClientProperty(property.name());
            if ( property.style().equals(oldValue) )
                return;

            if ( property.style().isEmpty() )
                _owner.putClientProperty(property.name(), null); // remove property
            else
                _owner.putClientProperty(property.name(), property.style());
        });

        return style;
    }

    private void _applyAlignmentToMigLayoutIfItExists(LayoutStyle style)
    {
        Optional<Float> alignmentX = style.alignmentX();
        Optional<Float> alignmentY = style.alignmentY();

        if ( !alignmentX.isPresent() && !alignmentY.isPresent() )
            return;

        LayoutManager layout = ( _owner.getParent() == null ? null : _owner.getParent().getLayout() );
        if ( layout instanceof MigLayout ) {
            MigLayout migLayout = (MigLayout) layout;
            Object rawComponentConstraints = migLayout.getComponentConstraints(_owner);
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
                migLayout.setComponentConstraints(_owner, finalComponentConstraints);
                _owner.getParent().revalidate();
            }
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
        if ( c.isOpaque() )
            c.setOpaque(false);

        for ( Component child : c.getComponents() ) {
            if ( child instanceof JComponent ) {
                JComponent jChild = (JComponent) child;
                _makeAllChildrenTransparent(jChild);
            }
        }
    }

    private void _installCustomBorderBasedStyleAndAnimationRenderer() {
        Border currentBorder = _owner.getBorder();
        if ( !(currentBorder instanceof StyleAndAnimationBorder) )
            _owner.setBorder(new StyleAndAnimationBorder<>(this, currentBorder));
    }

    private void _uninstallCustomBorderBasedStyleAndAnimationRenderer() {
        Border currentBorder = _owner.getBorder();
        if ( currentBorder instanceof StyleAndAnimationBorder) {
            StyleAndAnimationBorder<?> border = (StyleAndAnimationBorder<?>) currentBorder;
            _owner.setBorder(border.getFormerBorder());
        }
    }

    static boolean _componentIsDeclaredInUI(JComponent comp ) {
        // The component must be a subtype of one of the classes enclosed in this UI class!
        // Let's get all the classes declared in UI:
        Class<?>[] declaredInUI = UI.class.getDeclaredClasses();
        // We want to ensure that the component is a sub-type of any of the classes declared in UI.
        Class<?> clazz = comp.getClass();
        boolean isSwingTreeComponent = false;
        while ( clazz != null ) {
            for ( Class<?> c : declaredInUI )
                if ( c.isAssignableFrom(clazz) ) {
                    isSwingTreeComponent = true;
                    break;
                }

            clazz = clazz.getSuperclass();
        }
        return isSwingTreeComponent;
    }

}

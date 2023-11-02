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
import swingtree.components.JIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/**
 *  Is attached to UI components in the form of a client property.
 *  It exists to give Swing-Tree components some custom style and animation capabilities.
 */
public final class ComponentExtension<C extends JComponent>
{
    /**
     * Returns the {@link ComponentExtension} associated with the given component.
     * If the component does not have an extension, a new one is created and associated with the component.
     *
     * @param comp The component for which to get the extension.
     * @return The extension associated with the component.
     * @param <C> The type of the component.
     */
    public static <C extends JComponent> ComponentExtension<C> from( C comp ) {
        ComponentExtension<C> ext = (ComponentExtension<C>) comp.getClientProperty( ComponentExtension.class );
        if ( ext == null ) {
            ext = new ComponentExtension<>(comp);
            comp.putClientProperty( ComponentExtension.class, ext );
        }
        return ext;
    }

    /**
     *  Initializes the given component with a new {@link ComponentExtension}.
     *  This method is called by a SwingTree builder node when it
     *  receives and builds a new component.
     *  The former extension of the component is replaced by a new one.
     *
     * @param comp The component to initialize.
     */
    public static void initializeFor( JComponent comp ) {
        comp.putClientProperty( ComponentExtension.class, new ComponentExtension<>(comp) );
    }

    private final C _owner;

    private final List<Object> _extraState = new ArrayList<>(0);

    private final List<String> _styleGroups = new ArrayList<>(0);


    private StylePainter<C> _stylePainter = StylePainter.none();
    private DynamicLaF      _dynamicLaF   = DynamicLaF.none();
    private StyleSource<C>  _styleSource  = StyleSource.create();

    private Color _initialBackgroundColor = null;

    private Shape _mainClip = null;


    private ComponentExtension( C owner ) { _owner = Objects.requireNonNull(owner); }


    C getOwner() { return _owner; }

    /**
     *  Allows for extra state to be attached to the component extension.
     *  (Conceptually similar to how Swing components can have client properties.)<br>
     *  If the component already has an object of the given type attached,
     *  that object is returned. Otherwise, the given fetcher is used to create
     *  a new object of the given type, which is then attached to the component
     *  and returned.
     *
     * @param type The type of the extra state to attach.
     * @param fetcher A supplier which is used to create a new object of the given type.
     * @return The extra state object of the given type which is attached to the component.
     * @param <P> The type of the extra state.
     */
    public <P> P getOrSet( Class<P> type, Supplier<P> fetcher ) {
        for ( Object plugin : _extraState)
            if ( type.isInstance(plugin) )
                return (P) plugin;

        P plugin = fetcher.get();
        _extraState.add(plugin);
        return plugin;
    }

    /**
     *   This method is used by {@link swingtree.UIForAnySwing#group(String...)} to attach
     *   so called <i>group tags</i> to a component. <br>
     *   They are used by the SwingTree style engine to apply
     *   styles with the same tags, which
     *   is conceptually similar to CSS classes. <br>
     *   <b>It is advised to use the {@link #setStyleGroups(Enum[])} method
     *   instead of this method, as the usage of enums for modelling
     *   group tags offers much better compile time type safety!</b>
     *
     * @param groupTags An array of group tags.
     */
    public void setStyleGroups( String... groupTags ) {
        Objects.requireNonNull(groupTags);
        boolean alreadyHasGroupTags = !_styleGroups.isEmpty();
        if ( alreadyHasGroupTags )
            _styleGroups.clear();

        _styleGroups.addAll( java.util.Arrays.asList(groupTags) );

        if ( alreadyHasGroupTags )
            calculateApplyAndInstallStyle(false);
    }

    /**
     *   This method is used by {@link swingtree.UIForAnySwing#group(String...)}
     *   to attach so called <i>group tags</i> to a component. <br>
     *   They are used by the SwingTree style engine to apply
     *   styles with the same tags, which
     *   is conceptually similar to CSS classes. <br>
     *   It is advised to use this method over the {@link #setStyleGroups(String[])}
     *   method, as the usage of enums for modelling
     *   group tags offers much better compile time type safety!
     *
     * @param groupTags An array of group tags.
     * @param <E> The type of the enum.
     */
    @SafeVarargs
    public final <E extends Enum<E>> void setStyleGroups( E... groupTags ) {
        String[] stringTags = new String[groupTags.length];
        for ( int i = 0; i < groupTags.length; i++ ) {
            E group = groupTags[i];
            Objects.requireNonNull(group);
            stringTags[i] = group.getClass().getSimpleName() + "." + group.name();
        }
        setStyleGroups(stringTags);
    }

    /**
     * @return The group tags associated with the component
     *         in the form of an unmodifiable list of {@link String}s.
     */
    public List<String> getStyleGroups() { return Collections.unmodifiableList(_styleGroups); }

    /**
     * @return {@code true} if the component belongs to the given group.
     */
    public boolean belongsToGroup( String group ) { return _styleGroups.contains(group); }

    /**
     * @return {@code true} if the component belongs to the given group.
     */
    public boolean belongsToGroup( Enum<?> group ) {
        return belongsToGroup(group.getClass().getSimpleName() + "." + group.name());
    }

    Shape getMainClip() { return _mainClip; }

    /**
     * @return The current {@link Style} configuration of the component
     *         which is calculated based on the {@link Styler} lambdas
     *         associated with the component.
     */
    public Style getStyle() { return _stylePainter.getStyle(); }

    /**
     *  Removes all animations from the component.
     *  This includes both {@link Painter} based animations
     *  as well as {@link Styler} based animations.
     */
    public void clearAnimations() {
        _stylePainter = _stylePainter.withoutAnimationPainters();
        _styleSource  = _styleSource.withoutAnimationStylers();
    }

    /**
     *  Use this to add a {@link Painter} based animation to the component.
     *
     * @param state The {@link AnimationState} which defines when the animation is active.
     * @param painter The {@link Painter} which defines how the animation is rendered.
     */
    public void addAnimationPainter( AnimationState state, swingtree.api.Painter painter ) {
        _stylePainter = _stylePainter.withAnimationPainter(state.lifetime(), Objects.requireNonNull(painter));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    /**
     *  Use this to add a {@link Styler} based animation to the component.
     *
     * @param state The {@link AnimationState} which defines when the animation is active.
     * @param styler The {@link Styler} which defines how the style of the component is changed during the animation.
     */
    public void addAnimationStyler( AnimationState state, Styler<C> styler ) {
        _styleSource = _styleSource.withAnimationStyler(state.lifetime(), styler);
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    /**
     *  SwingTree overrides the default Swing look and feel
     *  to enable custom styling and animation capabilities.
     *  This method is used to install the custom look and feel
     *  for the component, if possible.
     */
    public void installCustomUIIfPossible() { _dynamicLaF.installCustomUIFor(_owner); }

    /**
     *  This method is used to paint the background style of the component
     *  using the provided {@link Graphics} object.
     *  The method is designed for components for which SwingTree could not install a custom UI,
     *  and it is intended to be used by custom {@link JComponent#paint(Graphics)}
     *  overrides, before calling the super implementation.
     *
     * @param g The {@link Graphics} object to use for rendering.
     */
    public void paintBackgroundStyle( Graphics g, Runnable lookAndFeelPaint )
    {
        if ( _dynamicLaF.customLookAndFeelIsInstalled() ) {
            if ( lookAndFeelPaint != null )
                lookAndFeelPaint.run();
            return; // We render ĥere through the custom installed UI!
        }

        if ( _componentIsDeclaredInUI(_owner) )
            _paintBackground(g, lookAndFeelPaint);
        else {
            _stylePainter = _stylePainter.endPainting(); // custom style rendering unfortunately not possible for this component :/
            if ( lookAndFeelPaint != null )
                lookAndFeelPaint.run();
        }
    }

    /**
     *  This method is used to paint the foreground style of the component
     *  using the provided {@link Graphics2D} object.
     *
     * @param g2d The {@link Graphics2D} object to use for rendering.
     */
    public void paintForegroundStyle( Graphics2D g2d )
    {
        establishStyleAndBeginPainting();

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

    Shape getInnerComponentArea() { return _stylePainter._getBaseArea(_owner); }

    /**
     *  Adds a {@link Styler} to the component.
     *  The styler will be used to calculate the style of the component.
     *
     * @param styler The styler to add.
     */
    public void addStyler( Styler<C> styler ) {
        Objects.requireNonNull(styler);
        _styleSource = _styleSource.withLocalStyler(styler, _owner);
        calculateApplyAndInstallStyle(false);
    }

    /**
     *  Calculates a new {@link Style} object based on the {@link Styler} lambdas associated
     *  with the component...
     *
     * @return A new immutable {@link Style} configuration.
     */
    public Style calculateStyle() {
        return _styleSource.calculateStyleFor(_owner);
    }

    /**
     *  Calculates a new {@link Style} object based on the {@link Styler} lambdas associated
     *  with the component and then applies it to the component after which
     *  a new {@link StylePainter} is installed for the component.
     *  If the calculated style is the same as the current style, nothing happens
     *  except in case the <code>force</code> parameter is set to <code>true</code>.
     *
     * @param force If set to <code>true</code>, the style will be applied even if it is the same as the current style.
     */
    public void calculateApplyAndInstallStyle( boolean force ) {
        _installStylePainterFor( _calculateAndApplyStyle(force) );
    }

    /**
     *  Applies the given {@link Style} to the component after which
     *  a new {@link StylePainter} is installed for the component.
     *  If the given style is the same as the current style, nothing happens
     *  except in case the <code>force</code> parameter is set to <code>true</code>.
     *
     * @param style The style to apply.
     * @param force If set to <code>true</code>, the style will be applied even if it is the same as the current style.
     */
    public void applyAndInstallStyle( Style style, boolean force ) {
        _installStylePainterFor( _applyStyleToComponentState(style, force) );
    }

    void establishStyleAndBeginPainting() {
        _stylePainter = _stylePainter.update( _calculateAndApplyStyle(false) );
    }

    private Style _calculateAndApplyStyle( boolean force ) {
        return _applyStyleToComponentState(calculateStyle(), force);
    }

    private void _installStylePainterFor( Style style ) {
        _stylePainter = _stylePainter.update(style);
    }

    void _paintBackground( Graphics g, Runnable lookAndFeelPainting )
    {
        // If end the painting of the last painting cycle if it was not already ended:
        _stylePainter = _stylePainter.endPainting();

        establishStyleAndBeginPainting();

        _stylePainter.renderBackgroundStyle( (Graphics2D) g, _owner );

        if ( lookAndFeelPainting != null ) {
            _mainClip = g.getClip();
            Shape clip = _mainClip;
            if (_stylePainter._getBaseArea() != null)
                clip = _stylePainter._getBaseArea(_owner);
            else if (_stylePainter.getStyle().margin().isPositive())
                clip = _stylePainter._getBaseArea(_owner);

            _stylePainter._withClip((Graphics2D) g, clip, () -> {
                try {
                    lookAndFeelPainting.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            _mainClip = null;
        }
    }

    void _paintBorderStyle( Graphics2D g2d, JComponent component ) {
        _stylePainter.paintBorderStyle(g2d, component);
    }

    void _renderAnimations( Graphics2D g2d )
    {
        _stylePainter.renderAnimations(g2d);
        _stylePainter = _stylePainter.withoutExpiredAnimationPainters();
    }

    private Style _applyStyleToComponentState( Style style, boolean force )
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

        if ( isNotStyled || onlyDimensionalityIsStyled ) {
            _dynamicLaF = _dynamicLaF._uninstallCustomLaF(_owner);
            if ( _styleSource.hasNoAnimationStylers() && _stylePainter.hasNoPainters() )
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

        UI.FitComponent fit = style.base().fit();
        style.base().icon().ifPresent( icon -> {
            if ( icon instanceof SvgIcon) {
                SvgIcon svgIcon = (SvgIcon) icon;
                icon = svgIcon.withFitComponent(fit);
            }
            if ( _owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) _owner;
                if ( !Objects.equals( button.getIcon(), icon ) )
                    button.setIcon( icon );
            }
            if ( _owner instanceof JLabel ) {
                JLabel label = (JLabel) _owner;
                if ( !Objects.equals( label.getIcon(), icon ) )
                    label.setIcon( icon );
            }
            if ( _owner instanceof JIcon ) {
                JIcon jIcon = (JIcon) _owner;
                if ( !Objects.equals( jIcon.getIcon(), icon ) )
                    jIcon.setIcon( icon );
            }
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
                _dynamicLaF = _dynamicLaF.establishLookAndFeelFor(style, _owner);
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

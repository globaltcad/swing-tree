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
        from(comp);
    }

    private final C _owner;

    private final List<Object> _extraState = new ArrayList<>(0);

    private final List<String> _styleGroups = new ArrayList<>(0);


    private StyleEngine     _styleEngine = StyleEngine.create();
    private DynamicLaF      _dynamicLaF   = DynamicLaF.none();
    private StyleSource<C>  _styleSource  = StyleSource.create();

    private Color _initialBackgroundColor = null;

    private Shape _outerBaseClip = null;


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
            gatherApplyAndInstallStyle(false);
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
            stringTags[i] = StyleUtility.toString(group);
        }
        setStyleGroups(stringTags);
    }

    public final void setId( String id ) {
        _owner.setName(id);
    }

    public final <E extends Enum<E>> void setId( E id ) {
        this.setId(StyleUtility.toString(id));
    }

    public final boolean hasId( String id ) {
        return Objects.equals(_owner.getName(), id);
    }

    public final boolean hasId( Enum<?> id ) {
        return hasId(StyleUtility.toString(id));
    }

    final UI.Placement preferredIconPlacement() {
        UI.Placement preferredPlacement = UI.Placement.UNDEFINED;
        if ( _hasText(_owner) )
            preferredPlacement = UI.Placement.LEFT;
        if ( !Objects.equals(ComponentOrientation.UNKNOWN, _owner.getComponentOrientation()) ) {
            if (  Objects.equals(ComponentOrientation.LEFT_TO_RIGHT, _owner.getComponentOrientation()) )
                preferredPlacement = UI.Placement.LEFT;
            if (  Objects.equals(ComponentOrientation.RIGHT_TO_LEFT, _owner.getComponentOrientation()) )
                preferredPlacement = UI.Placement.RIGHT;
        }
        return preferredPlacement;
    }

    private boolean _hasText( Component component ) {
        return !Optional.ofNullable( _findTextOf(component) ).map( String::isEmpty ).orElse(true);
    }

    private String _findTextOf( Component component ) {
        // We go through all the components which can display text and return the first one we find:
        if ( component instanceof javax.swing.AbstractButton ) // Covers JButton, JToggleButton, JCheckBox, JRadioButton...
            return ((javax.swing.AbstractButton) component).getText();
        if ( component instanceof javax.swing.JLabel )
            return ((javax.swing.JLabel) component).getText();
        if ( component instanceof JTextComponent )
            return ((JTextComponent) component).getText();

        return "";
    }

    /**
     * @return The group tags associated with the component
     *         in the form of an unmodifiable list of {@link String}s.
     */
    public List<String> getStyleGroups() { return Collections.unmodifiableList(_styleGroups); }

    /**
     * @param group The group to check.
     * @return {@code true} if the component belongs to the given group.
     */
    public boolean belongsToGroup( String group ) { return _styleGroups.contains(group); }

    /**
     * @param group The group to check.
     * @return {@code true} if the component belongs to the given group.
     */
    public boolean belongsToGroup( Enum<?> group ) {
        return belongsToGroup(StyleUtility.toString(group));
    }

    Shape getCurrentOuterBaseClip() { return _outerBaseClip; }

    /**
     * @return The current {@link Style} configuration of the component
     *         which is calculated based on the {@link Styler} lambdas
     *         associated with the component.
     */
    public Style getStyle() { return _styleEngine.getComponentConf().style(); }

    ComponentConf getConf() {
        return _styleEngine.getComponentConf();
    }

    /**
     *  Removes all animations from the component.
     *  This includes both {@link Painter} based animations
     *  as well as {@link Styler} based animations.
     */
    public void clearAnimations() {
        _styleEngine = _styleEngine.withoutAnimationPainters();
        _styleSource = _styleSource.withoutAnimationStylers();
    }

    /**
     *  Use this to add a {@link Painter} based animation to the component.
     *
     * @param state The {@link AnimationState} which defines when the animation is active.
     * @param painter The {@link Painter} which defines how the animation is rendered.
     */
    public void addAnimationPainter( AnimationState state, swingtree.api.Painter painter ) {
        _styleEngine = _styleEngine.withAnimationPainter(state.lifeSpan(), Objects.requireNonNull(painter));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    /**
     *  Use this to add a {@link Styler} based animation to the component.
     *
     * @param state The {@link AnimationState} which defines when the animation is active.
     * @param styler The {@link Styler} which defines how the style of the component is changed during the animation.
     */
    public void addAnimationStyler( AnimationState state, Styler<C> styler ) {
        _styleSource = _styleSource.withAnimationStyler(state.lifeSpan(), styler);
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    /**
     *  SwingTree overrides the default Swing look and feel
     *  to enable custom styling and animation capabilities.
     *  This method is used to install the custom look and feel
     *  for the component, if possible.
     */
    public void installCustomUIIfPossible() { _dynamicLaF.installCustomUIFor(_owner); }

    Insets getMarginInsets() {
        if ( _owner.getBorder() instanceof StyleAndAnimationBorder ) {
            StyleAndAnimationBorder<?> styleBorder = (StyleAndAnimationBorder<?>) _owner.getBorder();
            return styleBorder.getMarginInsets();
        }
        else
            return new Insets(0,0,0,0);
    }

    /**
     *  This method is used to paint the background style of the component
     *  using the provided {@link Graphics} object.
     *  The method is designed for components for which SwingTree could not install a custom UI,
     *  and it is intended to be used by custom {@link JComponent#paint(Graphics)}
     *  overrides, before calling the super implementation.
     *
     * @param g The {@link Graphics} object to use for rendering.
     * @param lookAndFeelPaint A {@link Runnable} which is used to paint the look and feel of the component.
     */
    public void paintBackgroundStyle( Graphics g, Runnable lookAndFeelPaint )
    {
        if ( _dynamicLaF.customLookAndFeelIsInstalled() ) {
            if ( lookAndFeelPaint != null )
                lookAndFeelPaint.run();
            return; // We render ĥere through the custom installed UI!
        }
        paintBackground(g, lookAndFeelPaint);
    }

    /**
     *  This method is used to paint the foreground style of the component
     *  using the provided {@link Graphics2D} object.
     *
     * @param g2d The {@link Graphics2D} object to use for rendering.
     * @param superPaint A {@link Runnable} which is used to paint the look and feel of the component.
     */
    public void paintForeground( Graphics2D g2d, Runnable superPaint )
    {
        gatherApplyAndInstallStyleConfig();

        Shape clip = _outerBaseClip != null ? _outerBaseClip : g2d.getClip();
        if ( _owner instanceof JScrollPane ) {
            /*
                Scroll panes are not like other components, they have a viewport
                which clips the children.
                Now if we have a round border for the scroll pane, we want the
                children to be clipped by the round border (and the viewport).
                So we use the inner component area as the clip for the children.
            */
            clip = StyleUtility.intersect( _styleEngine.getComponentConf().componentArea().orElse(clip), clip );
        }
        paintWithClip(g2d, clip, ()->{
            superPaint.run();
        });

        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Reset antialiasing to its previous state:
        if ( StyleEngine.IS_ANTIALIASING_ENABLED() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // We remember the clip:
        Shape formerClip = g2d.getClip();

        Font componentFont = _owner.getFont();
        if ( componentFont != null && !componentFont.equals(g2d.getFont()) )
            g2d.setFont( componentFont );

        _styleEngine.paintForeground(g2d);

        // We restore the clip:
        if ( g2d.getClip() != formerClip )
            g2d.setClip(formerClip);

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    void paintWithContentAreaClip( Graphics g, Runnable painter ) {
        gatherApplyAndInstallStyleConfig();
        _styleEngine.getComponentConf().paintWithContentAreaClip(g, painter);
    }

    /**
     *  Adds a {@link Styler} to the component.
     *  The styler will be used to calculate the style of the component.
     *
     * @param styler The styler to add.
     */
    public void addStyler( Styler<C> styler ) {
        Objects.requireNonNull(styler);
        _styleSource = _styleSource.withLocalStyler(styler);
        gatherApplyAndInstallStyle(false);
    }

    /**
     *  Calculates a new {@link Style} object based on the {@link Styler} lambdas associated
     *  with the component...
     *
     * @return A new immutable {@link Style} configuration.
     */
    public Style gatherStyle() {
        return _styleSource.gatherStyleFor(_owner);
    }

    /**
     *  Calculates a new {@link Style} object based on the {@link Styler} lambdas associated
     *  with the component and then applies it to the component after which
     *  a new {@link StyleEngine} is installed for the component.
     *  If the calculated style is the same as the current style, nothing happens
     *  except in case the <code>force</code> parameter is set to <code>true</code>.
     *
     * @param force If set to <code>true</code>, the style will be applied even if it is the same as the current style.
     */
    public void gatherApplyAndInstallStyle( boolean force ) {
        _installStyle( _applyStyleToComponentState(gatherStyle(), force) );
    }

    /**
     *  Applies the given {@link Style} to the component after which
     *  a new {@link StyleEngine} is installed for the component.
     *  If the given style is the same as the current style, nothing happens
     *  except in case the <code>force</code> parameter is set to <code>true</code>.
     *
     * @param style The style to apply.
     * @param force If set to <code>true</code>, the style will be applied even if it is the same as the current style.
     */
    public void applyAndInstallStyle( Style style, boolean force ) {
        _installStyle( _applyStyleToComponentState(style, force) );
    }

    void gatherApplyAndInstallStyleConfig() {
        _installStyle( _applyStyleToComponentState(gatherStyle(), false) );
    }

    private void _installStyle( Style style ) {
        _styleEngine = _styleEngine.withNewStyleAndComponent(style, _owner);
    }

    void paintBackground( Graphics g, Runnable lookAndFeelPainting )
    {
        gatherApplyAndInstallStyleConfig();

        _outerBaseClip = g.getClip();

        if ( _outerBaseClip == null && _owner.getParent() == null ) {
            // Happens when rendering individual components (usually unit tests)!
            int x = (int) ((Graphics2D) g).getTransform().getTranslateX();
            int y = (int) ((Graphics2D) g).getTransform().getTranslateY();
            int w = _owner.getWidth();
            int h = _owner.getHeight();
            _outerBaseClip = new Rectangle(x,y,w,h);
        }

        Font componentFont = _owner.getFont();
        if ( componentFont != null && !componentFont.equals(g.getFont()) )
            g.setFont( componentFont );

        _styleEngine.renderBackgroundStyle( (Graphics2D) g);

        if ( lookAndFeelPainting != null ) {
            Shape contentClip = _styleEngine.getComponentConf().componentArea().orElse(null);

            contentClip = StyleUtility.intersect( contentClip, _outerBaseClip );

            paintWithClip((Graphics2D) g, contentClip, () -> {
                try {
                    lookAndFeelPainting.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    void paintBorderAndAnimations( Graphics2D g2d, Runnable formerBorderPainter )
    {
        gatherApplyAndInstallStyleConfig();

        Shape former = g2d.getClip();

        if ( getCurrentOuterBaseClip() != null )
            g2d.setClip( getCurrentOuterBaseClip() );

        _styleEngine.paintBorder(g2d, formerBorderPainter);
        _styleEngine.paintAnimations(g2d);
        _styleEngine = _styleEngine.withoutExpiredAnimationPainters();

        g2d.setClip(former);
    }

    private Style _applyStyleToComponentState( Style newStyle, boolean force )
    {
        _styleSource = _styleSource.withoutExpiredAnimationStylers(); // Clean up expired animation stylers!

        Objects.requireNonNull(newStyle);

        if ( _owner.getBorder() instanceof StyleAndAnimationBorder<?> ) {
            StyleAndAnimationBorder<C> border = (StyleAndAnimationBorder<C>) _owner.getBorder();
            border.recalculateInsets(newStyle);
        }

        if ( _styleEngine.getComponentConf().style().equals(newStyle) && !force )
            return newStyle;

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
            _dynamicLaF = _dynamicLaF._uninstallCustomLaF(_owner);
            if ( _styleSource.hasNoAnimationStylers() && _styleEngine.hasNoPainters() )
                _uninstallCustomBorderBasedStyleAndAnimationRenderer();
            if ( _initialBackgroundColor != null ) {
                _owner.setBackground(_initialBackgroundColor);
                _initialBackgroundColor = null;
            }
            if ( isNotStyled )
                return newStyle;
        }

        boolean hasBorderRadius = newStyle.border().hasAnyNonZeroArcs();
        boolean hasBackground   = newStyle.base().backgroundColor().isPresent();

        if ( hasBackground && !Objects.equals( _owner.getBackground(), newStyle.base().backgroundColor().get() ) ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            Color newColor =  newStyle.base().backgroundColor().get();
            if ( newColor == UI.NO_COLOR )
                newColor = null;
            _owner.setBackground( newColor );
            if ( _owner instanceof JScrollPane ) {
                JScrollPane scrollPane = (JScrollPane) _owner;
                if ( scrollPane.getViewport() != null ) {
                    newColor = newStyle.base().backgroundColor().get();
                    if ( newColor == UI.NO_COLOR )
                        newColor = null;
                    scrollPane.getViewport().setBackground( newColor );
                }
            }
        }

        // If the style has a border radius set we need to make sure that we have a background color:
        if ( hasBorderRadius && !hasBackground ) {
            _initialBackgroundColor = _initialBackgroundColor != null ? _initialBackgroundColor :  _owner.getBackground();
            newStyle = newStyle.backgroundColor(_initialBackgroundColor);
        }

        if ( newStyle.base().foregroundColor().isPresent() && !Objects.equals( _owner.getForeground(), newStyle.base().foregroundColor().get() ) ) {
            Color newColor = newStyle.base().foregroundColor().get();
            if ( newColor == UI.NO_COLOR )
                newColor = null;
            _owner.setForeground( newColor );
        }


        newStyle.base().cursor().ifPresent( cursor -> {
            if ( !Objects.equals( _owner.getCursor(), cursor ) )
                _owner.setCursor( cursor );
        });

        if ( newStyle.base().orientation() != UI.ComponentOrientation.UNKNOWN ) {
            ComponentOrientation currentOrientation = _owner.getComponentOrientation();
            UI.ComponentOrientation newOrientation = newStyle.base().orientation();
            switch ( newOrientation ) {
                case LEFT_TO_RIGHT:
                    if ( !Objects.equals( currentOrientation, ComponentOrientation.LEFT_TO_RIGHT ) )
                        _owner.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
                    break;
                case RIGHT_TO_LEFT:
                    if ( !Objects.equals( currentOrientation, ComponentOrientation.RIGHT_TO_LEFT ) )
                        _owner.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                    break;
                default:
                    if ( !Objects.equals( currentOrientation, ComponentOrientation.UNKNOWN ) )
                        _owner.applyComponentOrientation(ComponentOrientation.UNKNOWN);
                    break;
            }
        }

        UI.FitComponent fit = newStyle.base().fit();
        newStyle.base().icon().ifPresent( icon -> {
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

        newStyle.layout().alignmentX().ifPresent( alignmentX -> {
            if ( !Objects.equals( _owner.getAlignmentX(), alignmentX ) )
                _owner.setAlignmentX( alignmentX );
        });

        newStyle.layout().alignmentY().ifPresent( alignmentY -> {
            if ( !Objects.equals( _owner.getAlignmentY(), alignmentY ) )
                _owner.setAlignmentY( alignmentY );
        });

        newStyle.layout().layout().installFor( _owner );

        _applyAlignmentToMigLayoutIfItExists(newStyle.layout());

        if ( newStyle.dimensionality().minWidth().isPresent() || newStyle.dimensionality().minHeight().isPresent() ) {
            Dimension minSize = _owner.getMinimumSize();

            int minWidth  = newStyle.dimensionality().minWidth().orElse(minSize == null ? 0 : minSize.width);
            int minHeight = newStyle.dimensionality().minHeight().orElse(minSize == null ? 0 : minSize.height);

            Dimension newMinSize = new Dimension(minWidth, minHeight);

            if ( ! newMinSize.equals(minSize) )
                _owner.setMinimumSize(newMinSize);
        }

        if ( newStyle.dimensionality().maxWidth().isPresent() || newStyle.dimensionality().maxHeight().isPresent() ) {
            Dimension maxSize = _owner.getMaximumSize();

            int maxWidth  = newStyle.dimensionality().maxWidth().orElse(maxSize == null  ? Integer.MAX_VALUE : maxSize.width);
            int maxHeight = newStyle.dimensionality().maxHeight().orElse(maxSize == null ? Integer.MAX_VALUE : maxSize.height);

            Dimension newMaxSize = new Dimension(maxWidth, maxHeight);

            if ( ! newMaxSize.equals(maxSize) )
                _owner.setMaximumSize(newMaxSize);
        }

        if ( newStyle.dimensionality().preferredWidth().isPresent() || newStyle.dimensionality().preferredHeight().isPresent() ) {
            Dimension prefSize = _owner.getPreferredSize();

            int prefWidth  = newStyle.dimensionality().preferredWidth().orElse(prefSize == null ? 0 : prefSize.width);
            int prefHeight = newStyle.dimensionality().preferredHeight().orElse(prefSize == null ? 0 : prefSize.height);

            Dimension newPrefSize = new Dimension(prefWidth, prefHeight);

            if ( !newPrefSize.equals(prefSize) )
                _owner.setPreferredSize(newPrefSize);
        }

        if ( newStyle.dimensionality().width().isPresent() || newStyle.dimensionality().height().isPresent() ) {
            Dimension size = _owner.getSize();

            int width  = newStyle.dimensionality().width().orElse(size == null ? 0 : size.width);
            int height = newStyle.dimensionality().height().orElse(size == null ? 0 : size.height);

            Dimension newSize = new Dimension(width, height);

            if ( ! newSize.equals(size) )
                _owner.setSize(newSize);
        }

        if ( _owner instanceof JTextComponent ) {
            JTextComponent tc = (JTextComponent) _owner;
            if ( newStyle.font().selectionColor().isPresent() && ! Objects.equals( tc.getSelectionColor(), newStyle.font().selectionColor().get() ) )
                tc.setSelectionColor(newStyle.font().selectionColor().get());
        }

        if ( _owner instanceof JComboBox ) {
            int bottom = newStyle.margin().bottom().map(Number::intValue).orElse(0);
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

        newStyle.font()
             .createDerivedFrom(_owner.getFont())
             .ifPresent( newFont -> {
                    if ( !newFont.equals(_owner.getFont()) )
                        _owner.setFont( newFont );
                });

        newStyle.font().horizontalAlignment().ifPresent( alignment -> {
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
        newStyle.font().verticalAlignment().ifPresent( alignment -> {
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
                _dynamicLaF = _dynamicLaF.establishLookAndFeelFor(newStyle, _owner);
        }

        if ( newStyle.hasCustomForegroundPainters() )
            _makeAllChildrenTransparent(_owner);

        if ( newStyle.hasActiveBackgroundGradients() && _owner.isOpaque() )
            _owner.setOpaque(false);

        newStyle.properties().forEach( property -> {

            Object oldValue = _owner.getClientProperty(property.name());
            if ( property.style().equals(oldValue) )
                return;

            if ( property.style().isEmpty() )
                _owner.putClientProperty(property.name(), null); // remove property
            else
                _owner.putClientProperty(property.name(), property.style());
        });

        return newStyle;
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


    static void paintWithClip( Graphics2D g2d, Shape clip, Runnable paintTask ) {
        Shape formerClip = g2d.getClip();
        g2d.setClip(clip);
        try {
            paintTask.run();
        } finally {
            g2d.setClip(formerClip);
        }
    }

}

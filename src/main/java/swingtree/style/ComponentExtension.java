package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.UI;
import swingtree.animation.AnimationStatus;
import swingtree.api.Painter;
import swingtree.api.Styler;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  Is attached to UI components in the form of a client property.
 *  It exists to give Swing-Tree components some custom style and animation capabilities.
 */
public final class ComponentExtension<C extends JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ComponentExtension.class);

    private static long _anonymousPainterCounter = 0;

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

    private final StyleInstaller<C> _styleInstaller = new StyleInstaller<>();


    private StyleEngine     _styleEngine = StyleEngine.create();
    private StyleSource<C>  _styleSource  = StyleSource.create();

    private @Nullable Shape _outerBaseClip = null;

    private PaintStep _lastPaintStep = PaintStep.UNDEFINED;
    private @Nullable BufferedImage _bufferedImage = null;


    private ComponentExtension( C owner ) { _owner = Objects.requireNonNull(owner); }


    C getOwner() { return _owner; }

    BoxModelConf getBoxModelConf() {
        return _styleEngine.getBoxModelConf();
    }

    Optional<BufferedImage> getBufferedImage() {
        return Optional.ofNullable(_bufferedImage);
    }

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
            stringTags[i] = StyleUtil.toString(group);
        }
        setStyleGroups(stringTags);
    }

    public final void setId( String id ) {
        _owner.setName(id);
    }

    public final <E extends Enum<E>> void setId( E id ) {
        this.setId(StyleUtil.toString(id));
    }

    public final boolean hasId( String id ) {
        return Objects.equals(_owner.getName(), id);
    }

    public final boolean hasId( Enum<?> id ) {
        return hasId(StyleUtil.toString(id));
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
     *  A component can have multiple group tags, which are used by the SwingTree style engine
     *  to apply styles with the same tags, which is conceptually similar to CSS classes.
     *  This method returns the group tags associated with the component.
     *
     * @return The group tags associated with the component
     *         in the form of an unmodifiable list of {@link String}s.
     */
    public List<String> getStyleGroups() { return Collections.unmodifiableList(_styleGroups); }

    /**
     *  A style group is a tag which is used by the SwingTree style engine
     *  to apply styles to things with the same tags making it conceptually similar to CSS classes.
     *  This method lets you check if the component belongs to a given String based group.
     *
     * @param group The group to check.
     * @return {@code true} if the component belongs to the given group.
     */
    public boolean belongsToGroup( String group ) { return _styleGroups.contains(group); }

    /**
     *  A style group is a tag which is used by the SwingTree style engine
     *  to apply styles to things with the same tags making it conceptually similar to CSS classes.
     *  This method lets you check if the component belongs to a given enum based group.
     *
     * @param group The group to check.
     * @return {@code true} if the component belongs to the given group.
     */
    public boolean belongsToGroup( Enum<?> group ) {
        return belongsToGroup(StyleUtil.toString(group));
    }

    /**
     *  Exposes the current {@link StyleConf} configuration of the component,
     *  which holds all the SwingTree style information needed to render the component.
     *
     * @return The current {@link StyleConf} configuration of the component
     *         which is calculated based on the {@link Styler} lambdas
     *         associated with the component.
     */
    public StyleConf getStyle() { return _styleEngine.getComponentConf().style(); }

    ComponentConf getConf() {
        return _styleEngine.getComponentConf();
    }

    public int getStateHash() {
        int hashCode = _styleEngine.getComponentConf().hashCode();
        if ( _owner instanceof JSlider ) {
            JSlider slider = (JSlider) _owner;
            hashCode = hashCode * 31 + slider.getValue();
            hashCode = hashCode * 31 + slider.getMinimum();
            hashCode = hashCode * 31 + slider.getMaximum();
        }
        if ( _owner instanceof JProgressBar ) {
            JProgressBar bar = (JProgressBar) _owner;
            hashCode = hashCode * 31 + bar.getValue();
            hashCode = hashCode * 31 + bar.getMinimum();
            hashCode = hashCode * 31 + bar.getMaximum();
        }
        if ( _owner instanceof JTextComponent ) {
            JTextComponent textComp = (JTextComponent) _owner;
            String text = textComp.getText();
            hashCode = hashCode * 31 + (text == null ? -1 : text.hashCode());
        }
        if ( _owner instanceof AbstractButton ) {
            AbstractButton button = (AbstractButton) _owner;
            hashCode = hashCode * 31 + (button.isSelected() ? 1 : 0);
        }
        return hashCode + ( _owner.isEnabled() ? 1 : 0 );
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
     * @param status The {@link AnimationStatus} which defines when the animation is active.
     * @param layer The {@link UI.Layer} which defines the layer on which the animation is rendered.
     * @param clipArea The {@link UI.ComponentArea} which defines the area of the component which is animated.
     * @param painter The {@link Painter} which defines how the animation is rendered.
     */
    public void addAnimatedPainter(
        AnimationStatus       status,
        UI.Layer              layer,
        UI.ComponentArea      clipArea,
        swingtree.api.Painter painter
    ) {
        _anonymousPainterCounter++;
        String painterName = "anonymous-painter-"+_anonymousPainterCounter;
        _styleSource = _styleSource.withAnimationStyler(status.lifeSpan(), it -> it.painter(layer, clipArea, painterName, painter));
        _styleInstaller.installCustomBorderBasedStyleAndAnimationRenderer(_owner, _styleEngine.getComponentConf().style());
        _styleInstaller.recalculateInsets(_owner, _styleEngine.getComponentConf().style());
        /*
            We need to install the custom SwingTree border which is used to render the animations!
        */
    }

    /**
     *  Use this to add a {@link Styler} based animation to the component.
     *
     * @param state The {@link AnimationStatus} which defines when the animation is active.
     * @param styler The {@link Styler} which defines how the style of the component is changed during the animation.
     */
    public void addAnimatedStyler( AnimationStatus state, Styler<C> styler ) {
        _styleSource = _styleSource.withAnimationStyler(state.lifeSpan(), styler);
    }

    /**
     *  SwingTree overrides the default Swing look and feel
     *  to enable custom styling and animation capabilities.
     *  This method is used to install the custom look and feel
     *  for the component, if possible.
     */
    public void installCustomUIIfPossible() {
        _styleInstaller.installCustomUIFor(_owner);
    }

    Insets getMarginInsets() {
        if ( _owner.getBorder() instanceof StyleAndAnimationBorder ) {
            StyleAndAnimationBorder<?> styleBorder = (StyleAndAnimationBorder<?>) _owner.getBorder();
            return styleBorder.getMarginInsets();
        }
        else
            return new Insets(0,0,0,0);
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
     *  Calculates a new {@link StyleConf} object based on the {@link Styler} lambdas associated
     *  with the component...
     *
     * @return A new immutable {@link StyleConf} configuration.
     */
    public StyleConf gatherStyle() {
        return _styleSource.gatherStyleFor(_owner);
    }

    /**
     *  Calculates a new {@link StyleConf} object based on the {@link Styler} lambdas associated
     *  with the component and then applies it to the component after which
     *  a new {@link StyleEngine} is installed for the component.
     *  If the calculated style is the same as the current style, nothing happens
     *  except in case the <code>force</code> parameter is set to <code>true</code>.
     *
     * @param force If set to <code>true</code>, the style will be applied even if it is the same as the current style.
     */
    public void gatherApplyAndInstallStyle( boolean force ) {
        _applyStyleToComponentState(gatherStyle(), force);
    }

    /**
     *  Applies the given {@link StyleConf} to the component after which
     *  a new {@link StyleEngine} is installed for the component.
     *  If the given style is the same as the current style, nothing happens
     *  except in case the <code>force</code> parameter is set to <code>true</code>.
     *
     * @param styleConf The style to apply.
     * @param force If set to <code>true</code>, the style will be applied even if it is the same as the current style.
     */
    public void applyAndInstallStyle(StyleConf styleConf, boolean force ) {
        _applyStyleToComponentState(styleConf, force);
    }

    void gatherApplyAndInstallStyleConfig() {
        _applyStyleToComponentState(gatherStyle(), false);
    }

    private void _applyStyleToComponentState( StyleConf newStyle, boolean force )
    {
        Objects.requireNonNull(newStyle);
        _styleEngine = _styleInstaller.applyStyleToComponentState(
                                    _owner,
                                    _styleEngine,
                                    _styleSource,
                                    newStyle,
                                    force
                                );
    }

    private void _doPaintStep(
        final PaintStep            step,
        final Graphics             graphics,
        final Consumer<Graphics2D> superPaint
    ) {
        final int newStep  = step.getStepOrder();
        final int lastStep = _lastPaintStep.getStepOrder();
        final boolean isNewPaintCycle = newStep <= lastStep;
        if ( isNewPaintCycle )
            gatherApplyAndInstallStyleConfig();
            /*
                If the new step is less than or equal to the last step,
                we consider it a new paint cycle and apply the style
            */

        _lastPaintStep = step;

        try {
            if ( isNewPaintCycle && step == PaintStep.BACKGROUND && _hasChildWithParentFilter() ) {
                int w = _owner.getWidth();
                int h = _owner.getHeight();
                _bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                _renderInto(_bufferedImage, step, graphics, superPaint);
            } else if ( _bufferedImage != null && step == PaintStep.BORDER ) {
                _renderInto(_bufferedImage, step, graphics, superPaint);
            } else {
                superPaint.accept((Graphics2D) graphics);
            }
        } catch ( Exception e ) {
            log.error("Error while painting step '"+step+"'!", e);
        }
    }

    private void _renderInto(BufferedImage buffer, PaintStep step, Graphics graphics, Consumer<Graphics2D> superPaint ) {
        Graphics2D bufferGraphics = buffer.createGraphics();
        StyleUtil.transferConfigurations((Graphics2D) graphics, bufferGraphics);
        bufferGraphics.setClip(graphics.getClip());
        try {
            superPaint.accept(bufferGraphics);
        } catch ( Exception e ) {
            log.error("Error while painting step '"+step+"' into component buffer!", e);
        }
        graphics.drawImage(buffer, 0, 0, null);
    }

    private boolean _hasChildWithParentFilter() {
        for ( Component child : _owner.getComponents() ) {
            if ( !child.isOpaque() && child instanceof JComponent ) {
                if ( _hasParentFilter((JComponent) child) )
                    return true;
            }
        }
        _bufferedImage = null;
        return false;
    }

    private boolean _hasParentFilter( JComponent aComponent ) {
        ComponentExtension<?> extension = from(aComponent);
        ComponentConf conf = extension.getConf();
        if ( conf.equals(ComponentConf.none()) )
            return false;
        StyleConf style = conf.style();
        if ( style.equals(StyleConf.none()) )
            return false;
        return !style.layers().filter().equals(FilterConf.none());
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
    void paintBackgroundIfNeeded( Graphics g, Consumer<Graphics> lookAndFeelPaint )
    {
        if ( _styleInstaller.customLookAndFeelIsInstalled() ) {
            if ( lookAndFeelPaint != null )
                lookAndFeelPaint.accept(g);
            return; // We render ĥere through the custom installed UI!
            // So the method call below will be called within lookAndFeelPaint.run();
        }
        paintBackground(g, false, lookAndFeelPaint);
    }

    /**
     *  Does a SwingTree based background painting call with a call bck for look and feel based
     *  drawing...
     *  If The {@code needsCustomWipe} flag is true, then it is assumed that SwingTree is the
     *  sole owner of this components look and feel, in which case it will fill the component area
     *  to cover up what was previously rendered (which is necessary in case of the component being opaque).
     *
     * @param graphics The {@link Graphics2D} API used for doing the 2D draw calls...
     * @param needsCustomWipe If this is true, then this means it is up to this SwingTree method to overwrite
     *                          what is in the graphics buffer by filling the rectangle in case of the
     *                          component being opaque. If this is false, we trust the look and feel to do it.
     * @param lookAndFeelPainting The look and feel background painting (usually just filling the component rectangle).
     *                            If this is null, then the {@code needsCustomWipe} flag should be true as well.
     */
    void paintBackground( Graphics graphics, boolean needsCustomWipe, @Nullable Consumer<Graphics> lookAndFeelPainting )
    {
        _doPaintStep(PaintStep.BACKGROUND, graphics, internalGraphics -> {
            if ( needsCustomWipe ) {
                if ( _owner.isOpaque() ) {
                    internalGraphics.setColor(_owner.getBackground());
                    int width = _owner.getWidth();
                    int height = _owner.getHeight();
                    internalGraphics.fillRect(0, 0, width, height);
                    /*
                        If "lookAndFeelPainting" is null then this means there is no
                        native ComponentUI, instead it is upd to SwingTree to override what was
                        rendered previously in the buffer.
                    */
                }
            }

            Shape baseClip = internalGraphics.getClip();
            _outerBaseClip = baseClip;

            if ( _outerBaseClip == null && _owner.getParent() == null ) {
                // Happens when rendering individual components (usually unit tests)!
                int x = (int) internalGraphics.getTransform().getTranslateX();
                int y = (int) internalGraphics.getTransform().getTranslateY();
                int w = _owner.getWidth();
                int h = _owner.getHeight();
                _outerBaseClip = new Rectangle(x,y,w,h);
            }

            Font componentFont = _owner.getFont();
            if ( componentFont != null && !componentFont.equals(internalGraphics.getFont()) )
                internalGraphics.setFont( componentFont );

            // Sometimes needed to render filtered backgrounds:
            BufferedImage parentRendering = Optional.ofNullable(_owner.getParent())
                                            .map( c -> c instanceof JComponent ? (JComponent) c : null )
                                            .map(ComponentExtension::from)
                                            .map(e -> e._bufferedImage)
                                            .orElse(null);

            // Location relative to the parent:
            _styleEngine.renderBackgroundStyle(internalGraphics, parentRendering, _owner.getX(), _owner.getY());

            if ( lookAndFeelPainting != null ) {
                Shape contentClip = _styleEngine.componentArea().orElse(null);

                contentClip = StyleUtil.intersect( contentClip, _outerBaseClip );

                paintWithClip(internalGraphics, contentClip, () -> {
                    try {
                        lookAndFeelPainting.accept(internalGraphics);
                    } catch (Exception e) {
                        String componentAsString = "?";
                        try {
                            // Anything can happen in client code...
                            componentAsString = _owner.toString();
                        } catch (Exception e2) {
                            log.error("Error while converting component to string!", e2);
                        }
                        log.error("Error while painting look and feel of component '"+componentAsString+"'!", e);
                    }
                });
            }

            internalGraphics.setClip(baseClip);
        });
    }

    void paintBorder( Graphics2D graphics, Consumer<Graphics> formerBorderPainter )
    {
        _doPaintStep(PaintStep.BORDER, graphics, internalGraphics -> {
            Shape former = internalGraphics.getClip();
                try {
                    if ( _outerBaseClip != null )
                        internalGraphics.setClip(_outerBaseClip);

                    _styleEngine.paintBorder(internalGraphics, formerBorderPainter);
                } catch (Exception e) {
                    log.error("Error while painting border!", e);
                }
                finally {
                    internalGraphics.setClip(former);
                }
        });
    }

    /**
     *  This method is used to paint the foreground style of the component
     *  using the provided {@link Graphics2D} object.
     *
     * @param graphics The {@link Graphics2D} object to use for rendering.
     * @param superPaint A {@link Runnable} which is used to paint the look and feel of the component.
     */
    void paintForeground( Graphics2D graphics, Consumer<Graphics> superPaint )
    {
        _doPaintStep(PaintStep.FOREGROUND, graphics, internalGraphics -> {
            // We remember the clip:
            Shape formerClip = internalGraphics.getClip();
            if ( _owner instanceof JScrollPane) {
                /*
                    Scroll panes are not like other components, they have a viewport
                    which clips the children.
                    Now if we have a round border for the scroll pane, we want the
                    children to be clipped by the round border (and the viewport).
                    So we use the inner component area as the clip for the children.
                */
                Shape localClip = StyleUtil.intersect( _styleEngine.componentArea().orElse(formerClip), formerClip );
                paintWithClip(internalGraphics, localClip, ()->{
                    superPaint.accept(internalGraphics);
                });
            }
            else
                superPaint.accept(internalGraphics);

            // We remember if antialiasing was enabled before we render:
            boolean antialiasingWasEnabled = internalGraphics.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
            // Reset antialiasing to its previous state:
            if ( StyleEngine.IS_ANTIALIASING_ENABLED() )
                internalGraphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

            Font componentFont = _owner.getFont();
            if ( componentFont != null && !componentFont.equals(internalGraphics.getFont()) )
                internalGraphics.setFont( componentFont );

            _styleEngine.paintForeground(internalGraphics);

            // Reset antialiasing to its previous state:
            internalGraphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

            // We restore the clip:
            if ( internalGraphics.getClip() != formerClip )
                internalGraphics.setClip(formerClip);
        });
    }

    void gatherStyleAndPaintInScope( Graphics g, Runnable painter ) {
        gatherApplyAndInstallStyleConfig();
        Shape oldClip = g.getClip();
        try {
            painter.run();
        } catch (Exception e) {
            log.warn("Error while rendering component of type '"+_owner.getClass().getName()+"'.", e);
        }
        g.setClip(oldClip);
    }

    static void paintWithClip( Graphics2D g2d, @Nullable Shape clip, Runnable paintTask ) {
        Shape formerClip = g2d.getClip();
        g2d.setClip(clip);
        try {
            paintTask.run();
        } catch (Exception e) {
            log.error("Error during clipped painting task.", e);
        } finally {
            g2d.setClip(formerClip);
        }
    }

    private enum PaintStep
    {
        BACKGROUND, BORDER, FOREGROUND, UNDEFINED;


        private int getStepOrder() {
            switch ( this ) {
                case BACKGROUND: return 0;
                case BORDER:     return 1;
                case FOREGROUND: return 2;
                case UNDEFINED:  return 3;
                default: return -1;
            }
        }

    }

    /**
     *  This method tries to hash everything relevant in the visual appearance
     *  of the component and it subcomponents into a single integer value.
     *  It is based on the current SwingTree style information as
     *  well as more general component information like the current value of a slider,
     *  text of a text component, etc. <br>
     *  <p>
     *  You may use this for rough cache invalidation purposes.
     *  So when you want to render the component into a {@link BufferedImage}
     *  and then only rerender it if the state hash changes, you can use this method.<br>
     *  <p>
     *  But keep in mind however, it cannot capture look and feel related changes
     *  which are not controlled by SwingTree. <br>
     *  So this hash code is not a perfect solution, but it can be useful in some cases.
     *  Like visualizing a drag and drop of a component...
     *
     * @return The current state hash of the component and all of it subcomponents, which includes
     *          SwingTree style information, as well as component specific information.
     */
    public int viewStateHashCode() {
        if ( _owner.getWidth() <= 0 )
            return 0;
        if ( _owner.getHeight() <= 0 )
            return 0;

        int hashCode = _styleEngine.getComponentConf().hashCode();
        // Common stuff:
        hashCode = hashCode + ( _owner.isEnabled() ? 1 : 0 );
        java.awt.Color background = _owner.getBackground();
        java.awt.Color foreground = _owner.getForeground();
        hashCode = hashCode * 31 + (background == null ? -1 : background.hashCode());
        hashCode = hashCode * 31 + (foreground == null ? -1 : foreground.hashCode());

        // Subtype component specific stuff:
        if ( _owner instanceof JSlider ) {
            JSlider slider = (JSlider) _owner;
            hashCode = hashCode * 31 + slider.getValue();
            hashCode = hashCode * 31 + slider.getMinimum();
            hashCode = hashCode * 31 + slider.getMaximum();
            // alignment:
            hashCode = hashCode * 31 + (slider.getOrientation() == JSlider.HORIZONTAL ? 0 : 1);
        }
        if ( _owner instanceof JProgressBar ) {
            JProgressBar bar = (JProgressBar) _owner;
            hashCode = hashCode * 31 + bar.getValue();
            hashCode = hashCode * 31 + bar.getMinimum();
            hashCode = hashCode * 31 + bar.getMaximum();
        }
        if ( _owner instanceof JTextComponent ) {
            JTextComponent textComp = (JTextComponent) _owner;
            String text = textComp.getText();
            hashCode = hashCode * 31 + (text == null ? -1 : text.hashCode());
            hashCode = hashCode * 31 + textComp.getCaretPosition();
        }
        if ( _owner instanceof AbstractButton ) {
            AbstractButton button = (AbstractButton) _owner;
            String text = button.getText();
            hashCode = hashCode * 31 + (button.isSelected() ? 1 : 0);
            hashCode = hashCode * 31 + (text == null ? -1 : text.hashCode());
        }
        if ( _owner instanceof JList ) {
            JList<?> list = (JList<?>) _owner;
            hashCode = hashCode * 31 + list.getSelectedIndex();
        }
        if ( _owner instanceof JTabbedPane ) {
            JTabbedPane tabbedPane = (JTabbedPane) _owner;
            hashCode = hashCode * 31 + tabbedPane.getSelectedIndex();
        }
        if ( _owner instanceof JTree ) {
            JTree tree = (JTree) _owner;
            hashCode = hashCode * 31 + tree.getSelectionCount();
        }
        if ( _owner instanceof JTable ) {
            JTable table = (JTable) _owner;
            hashCode = hashCode * 31 + table.getSelectedRow();
            hashCode = hashCode * 31 + table.getSelectedColumn();
        }
        if ( _owner instanceof JSpinner ) {
            JSpinner spinner = (JSpinner) _owner;
            hashCode = hashCode * 31 + spinner.getValue().hashCode();
        }
        if ( _owner instanceof JComboBox ) {
            JComboBox<?> comboBox = (JComboBox<?>) _owner;
            hashCode = hashCode * 31 + comboBox.getSelectedIndex();
        }
        if ( _owner instanceof JCheckBox ) {
            JCheckBox checkBox = (JCheckBox) _owner;
            hashCode = hashCode * 31 + (checkBox.isSelected() ? 1 : 0);
        }

        // Hashing the children recursively:
        try {
            for ( Component child : _owner.getComponents() ) {
                if ( child instanceof JComponent ) {
                    ComponentExtension<?> childExtension = from((JComponent) child);
                    hashCode = hashCode * 31 + childExtension.viewStateHashCode();
                }
            }
        } catch ( Exception e ) {
            log.error("Error while hashing children of component '"+_owner+"'.", e);
        }
        return hashCode;
    }

}

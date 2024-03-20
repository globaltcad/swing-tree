package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.animation.AnimationState;
import swingtree.api.Painter;
import swingtree.api.Styler;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
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
        return belongsToGroup(StyleUtil.toString(group));
    }

    /**
     * @return The current {@link StyleConf} configuration of the component
     *         which is calculated based on the {@link Styler} lambdas
     *         associated with the component.
     */
    public StyleConf getStyle() { return _styleEngine.getComponentConf().style(); }

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
     * @param layer The {@link UI.Layer} which defines the layer on which the animation is rendered.
     * @param clipArea The {@link UI.ComponentArea} which defines the area of the component which is animated.
     * @param painter The {@link Painter} which defines how the animation is rendered.
     */
    public void addAnimatedPainter(
        AnimationState        state,
        UI.Layer              layer,
        UI.ComponentArea      clipArea,
        swingtree.api.Painter painter
    ) {
        _anonymousPainterCounter++;
        String painterName = "anonymous-painter-"+_anonymousPainterCounter;
        _styleSource = _styleSource.withAnimationStyler(state.lifeSpan(), it -> it.painter(layer, clipArea, painterName, painter));
        _styleInstaller.installCustomBorderBasedStyleAndAnimationRenderer(_owner, _styleEngine.getComponentConf().style());
        /*
            We need to install the custom SwingTree border which is used to render the animations!
        */
    }

    /**
     *  Use this to add a {@link Styler} based animation to the component.
     *
     * @param state The {@link AnimationState} which defines when the animation is active.
     * @param styler The {@link Styler} which defines how the style of the component is changed during the animation.
     */
    public void addAnimatedStyler( AnimationState state, Styler<C> styler ) {
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
        _installStyle( _applyStyleToComponentState(gatherStyle(), force) );
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
        _installStyle( _applyStyleToComponentState(styleConf, force) );
    }

    void gatherApplyAndInstallStyleConfig() {
        _installStyle( _applyStyleToComponentState(gatherStyle(), false) );
    }

    private StyleConf _applyStyleToComponentState(StyleConf newStyle, boolean force )
    {
        _styleSource = _styleSource.withoutExpiredAnimationStylers(); // Clean up expired animation stylers!

        Objects.requireNonNull(newStyle);

        if ( _owner.getBorder() instanceof StyleAndAnimationBorder<?> ) {
            StyleAndAnimationBorder<C> border = (StyleAndAnimationBorder<C>) _owner.getBorder();
            border.recalculateInsets(newStyle);
        }

        if ( !force ) {
            // We check if it makes sense to apply the new style:
            boolean componentBackgroundWasMutated = _styleInstaller.backgroundWasChangedSomewhereElse(_owner);

            if ( !componentBackgroundWasMutated && _styleEngine.getComponentConf().style().equals(newStyle) )
                return newStyle;
        }

        return _styleInstaller.applyStyleToComponentState(_owner, newStyle, _styleSource);
    }

    private void _installStyle( StyleConf styleConf) {
        _styleEngine = _styleEngine.withNewStyleAndComponent(styleConf, _owner);
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
    void paintBackgroundStyle( Graphics g, Runnable lookAndFeelPaint )
    {
        if ( _styleInstaller.customLookAndFeelIsInstalled() ) {
            if ( lookAndFeelPaint != null )
                lookAndFeelPaint.run();
            return; // We render ĥere through the custom installed UI!
        }
        paintBackground(g, lookAndFeelPaint);
    }

    void paintBackground( Graphics g, @Nullable Runnable lookAndFeelPainting )
    {
        gatherApplyAndInstallStyleConfig();

        Shape baseClip = g.getClip();
        _outerBaseClip = baseClip;

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
            Shape contentClip = _styleEngine.componentArea().orElse(null);

            contentClip = StyleUtil.intersect( contentClip, _outerBaseClip );

            paintWithClip((Graphics2D) g, contentClip, () -> {
                try {
                    lookAndFeelPainting.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        g.setClip(baseClip);
    }

    /**
     *  This method is used to paint the foreground style of the component
     *  using the provided {@link Graphics2D} object.
     *
     * @param g2d The {@link Graphics2D} object to use for rendering.
     * @param superPaint A {@link Runnable} which is used to paint the look and feel of the component.
     */
    void paintForeground( Graphics2D g2d, Runnable superPaint )
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
            clip = StyleUtil.intersect( _styleEngine.componentArea().orElse(clip), clip );
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

    void paintBorder( Graphics2D g2d, Runnable formerBorderPainter )
    {
        gatherApplyAndInstallStyleConfig();

        Shape former = g2d.getClip();
        try {
            if ( _outerBaseClip != null )
                g2d.setClip(_outerBaseClip);

            _styleEngine.paintBorder(g2d, formerBorderPainter);
        } finally {
            g2d.setClip(former);
        }
    }

    void paintWithContentAreaClip( Graphics g, Runnable painter ) {
        gatherApplyAndInstallStyleConfig();
        _styleEngine.paintClippedTo(UI.ComponentArea.BODY, g, painter);
    }

    static void paintWithClip( Graphics2D g2d, @Nullable Shape clip, Runnable paintTask ) {
        Shape formerClip = g2d.getClip();
        g2d.setClip(clip);
        try {
            paintTask.run();
        } finally {
            g2d.setClip(formerClip);
        }
    }

}

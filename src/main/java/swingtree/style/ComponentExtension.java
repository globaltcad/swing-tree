package swingtree.style;

import swingtree.SwingTree;
import swingtree.UI;
import swingtree.animation.AnimationState;
import swingtree.animation.LifeTime;
import swingtree.api.Painter;
import swingtree.api.Styler;
import swingtree.components.JBox;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;
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

    private final Map<LifeTime, swingtree.api.Painter>   _animationPainters = new LinkedHashMap<>(0);
    private final Map<LifeTime, Styler<C>> _animationStylers  = new LinkedHashMap<>(0);

    private final List<String> _styleGroups = new ArrayList<>(0);

    StylePainter<C> _currentStylePainter = null;

    private final DynamicLaF _laf = DynamicLaF.none();

    private Styler<C> _styling = Styler.none();
    private StyleSheet _styleSheet = null;

    private Color _initialBackgroundColor = null;

    Shape _mainClip = null;


    private ComponentExtension( C owner ) {
        _owner = Objects.requireNonNull(owner);
    }

    C getOwner() { return _owner; }

    public void addStyling( Styler<C> styler ) {
        Objects.requireNonNull(styler);

        _styling = _styling.andThen( s -> styler.style(new ComponentStyleDelegate<>(_owner, s.style())) );

        establishStyle();
    }

    public void establishStyle() {
        _applyStyleToComponentState(_calculateStyle());
    }

    void _establishCurrentMainPaintClip(Graphics g) {
        if ( _mainClip == null )
            _mainClip = g.getClip();
    }

    public void setStyleGroups( String... styleName ) {
        Objects.requireNonNull(styleName);
        if ( !_styleGroups.isEmpty() )
            throw new IllegalStateException("Style groups already specified!");

        _styleGroups.addAll( java.util.Arrays.asList(styleName) );
    }

    public List<String> getStyleGroups() { return Collections.unmodifiableList(_styleGroups); }

    public void clearAnimationRenderer() {
        _animationPainters.clear();
        _animationStylers.clear();
    }

    public void addAnimationPainter( AnimationState state, swingtree.api.Painter painter ) {
        _animationPainters.put(Objects.requireNonNull(state.lifetime()), Objects.requireNonNull(painter));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    public void addAnimationStyler( AnimationState state, Styler<C> styler ) {
        _animationStylers.put(Objects.requireNonNull(state.lifetime()), Objects.requireNonNull(styler));
        _installCustomBorderBasedStyleAndAnimationRenderer();
    }

    public PanelUI createJBoxUI() {
        return new DynamicLaF.PanelStyler() {
            @Override
            public void installUI(JComponent c) {
                JBox b = (JBox)c;
                installDefaults(b);
            }
            @Override
            public void uninstallUI(JComponent c) {
                JBox b = (JBox)c;
                uninstallDefaults(b);
            }

            private void installDefaults(JBox b) {
                LookAndFeel.installColorsAndFont(b,
                        "Box.background",
                        "Box.foreground",
                        "Box.font");
                LookAndFeel.installBorder(b,"Box.border");
                LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
            }

            private void uninstallDefaults(JBox b) {
                LookAndFeel.uninstallBorder(b);
            }
        };
    }

    private StylePainter<C> _createStylePainter() {
        Style style = _applyStyleToComponentState(_calculateStyle());
        return style.equals(Style.none()) ? null : new StylePainter<>(_owner, style);
    }

    Optional<StylePainter<C>> _getOrCreateStylePainter() {
        if ( _currentStylePainter == null )
            _currentStylePainter = _createStylePainter();

        return Optional.ofNullable(_currentStylePainter);
    }

    public void paintBackgroundStyle( Graphics g )
    {
        if ( _laf.customLookAndFeelIsInstalled() )
            return; // We render through the custom installed UI!

        if ( _componentIsDeclaredInUI(_owner) )
            _paintBackground(g);
        else
            _currentStylePainter = null; // custom style rendering unfortunately not possible for this component :/
    }

    void _paintBackground(Graphics g)
    {
        _mainClip = null;
        _establishCurrentMainPaintClip(g);

        _currentStylePainter = _createStylePainter();
        if ( _currentStylePainter != null )
            _currentStylePainter.renderBackgroundStyle( (Graphics2D) g );
    }


    public void _renderAnimations( Graphics2D g2d )
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        if ( StylePainter.DO_ANTIALIASING() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // Animations are last: they are rendered on top of everything else:
        for ( Map.Entry<LifeTime, Painter> entry : new ArrayList<>(_animationPainters.entrySet()) )
            if ( entry.getKey().isExpired() )
                _animationPainters.remove(entry.getKey());
            else {
                try {
                    entry.getValue().paint(g2d);
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a painter should not prevent everything else from being painted!
                }
            }

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

    }

    public void paintForegroundStyle( Graphics2D g2d ) {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );

        // We remember the clip:
        Shape formerClip = g2d.getClip();

        if ( _currentStylePainter != null )
            _currentStylePainter.paintForegroundStyle(g2d);

        // We restore the clip:
        if ( g2d.getClip() != formerClip )
            g2d.setClip(formerClip);

        // Enable antialiasing again:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
    }

    private Style _calculateStyle() {
        _styleSheet = _styleSheet != null ? _styleSheet : SwingTree.get().getStyleSheet().orElse(null);
        Style style = _styleSheet == null ? Style.none() : _styleSheet.applyTo( _owner );
        style = _styling.style(new ComponentStyleDelegate<>(_owner, style)).style();

        // Animations styles are last: they override everything else:
        for ( Map.Entry<LifeTime, Styler<C>> entry : new ArrayList<>(_animationStylers.entrySet()) )
            if ( entry.getKey().isExpired() )
                _animationStylers.remove(entry.getKey());
            else {
                try {
                    style = entry.getValue().style(new ComponentStyleDelegate<>(_owner, style)).style();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a styler should not prevent other stylers from being applied!
                }
            }

        return _applyDPIScaling(style);
    }

    private static Style _applyDPIScaling( Style style ) {
        if ( UI.scale() == 1f )
            return style;

        return style.scale( UI.scale() );
    }

    private Style _applyStyleToComponentState( Style style )
    {
        Objects.requireNonNull(style);

        final Style.Report styleReport = style.getReport();

        boolean isNotStyled                     = styleReport.isNotStyled();
        boolean onlyDimensionalityIsStyled      = styleReport.onlyDimensionalityIsStyled();
        boolean styleCanBeRenderedThroughBorder = StyleAndAnimationBorder.canFullyPaint(styleReport);

        if ( _owner instanceof JTextField && style.margin().isPositive() )
            styleCanBeRenderedThroughBorder = false;

        if ( isNotStyled || onlyDimensionalityIsStyled ) {
            _laf._uninstallCustomLaF(_owner);
            if ( _animationStylers.isEmpty() && _animationPainters.isEmpty() )
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

        style.base().cursor().ifPresent( cursor -> {
            if ( !Objects.equals( _owner.getCursor(), cursor ) )
                _owner.setCursor( cursor );
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

        if ( !onlyDimensionalityIsStyled ) {
            _installCustomBorderBasedStyleAndAnimationRenderer();
            if ( !styleCanBeRenderedThroughBorder )
                _laf.establishLookAndFeelFor(style, _owner);
        }

        if ( style.hasCustomForegroundPainters() )
            _makeAllChildrenTransparent(_owner);

        if ( style.hasActiveBackgroundGradients() && _owner.isOpaque() )
            _owner.setOpaque(false);

        return style;
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
     *  in fact it completely ignores bypasses the rendering of this current component!
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

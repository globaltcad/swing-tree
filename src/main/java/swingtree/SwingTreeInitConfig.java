package swingtree;

import swingtree.style.StyleSheet;
import swingtree.threading.EventProcessor;

import java.awt.Font;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 *  An immutable configuration object for {@link SwingTree},
 *  which can be configured using a functional {@link SwingTreeConfigurator} lambda
 *  passed to {@link SwingTree#initialiseUsing(SwingTreeConfigurator)}.
 *  <p>
 *  It allows for the configuration of the default
 *  font, font installation, scaling, event processing and stylesheet.
 */
public final class SwingTreeInitConfig
{
    /**
     *  Defines how the {@link Font}, specified through {@link SwingTreeInitConfig#defaultFont(Font)},
     *  is installed in the {@link javax.swing.UIManager}.
     */
    public enum FontInstallation
    {
        /**
         *  A soft installation will only install the font as the "defaultFont" property
         *  of the {@link javax.swing.UIManager}.
         */
        SOFT,
        /**
         *  A hard installation will install the font as the "defaultFont" property
         *  of the {@link javax.swing.UIManager} and as the default font of all
         *  {@link javax.swing.UIManager} components.
         */
        HARD,
        /**
         *  No installation will be performed.
         */
        NONE
    }

    /**
     *  Defines how the scaling factor for the UI should be determined.
     */
    public enum Scaling
    {
        /**
         *  The scaling will be derived from the default font size.
         */
        FROM_FONT,
        /**
         *  The scaling will be derived from the system font size.
         */
        FROM_SYSTEM_FONT,
        /**
         *  No scaling will be performed.
         */
        NONE
    }

    private final static SwingTreeInitConfig DEFAULT = new SwingTreeInitConfig(
                                                           null,
                                                            FontInstallation.SOFT,
                                                            Scaling.FROM_FONT,
                                                            EventProcessor.COUPLED_STRICT,
                                                            null
                                                        );

    public static SwingTreeInitConfig none() { return DEFAULT; }


    private final Font             _defaultFont; // may be null
    private final FontInstallation _fontInstallation;
    private final Scaling          _scaling;
    private final EventProcessor   _eventProcessor;
    private final StyleSheet       _styleSheet; // may be null


    private SwingTreeInitConfig(
        Font             defaultFont,
        FontInstallation fontInstallation,
        Scaling          scaling,
        EventProcessor   eventProcessor,
        StyleSheet       styleSheet
    ) {
        _defaultFont      = defaultFont;
        _fontInstallation = Objects.requireNonNull(fontInstallation);
        _scaling          = Objects.requireNonNull(scaling);
        _eventProcessor   = Objects.requireNonNull(eventProcessor);
        _styleSheet       = styleSheet;
    }

    /**
     *  Returns an {@link Optional} containing the default font
     *  or an empty {@link Optional} if no default font is set.
     */
    Optional<Font> defaultFont() {
        return Optional.ofNullable(_defaultFont);
    }

    /**
     *  Returns the {@link FontInstallation} mode.
     */
    FontInstallation fontInstallation() {
        return _fontInstallation;
    }

    /**
     *  Returns the {@link Scaling} mode.
     */
    Scaling scaling() {
        return _scaling;
    }

    /**
     *  Returns the {@link EventProcessor}.
     */
    EventProcessor eventProcessor() {
        return _eventProcessor;
    }

    /**
     *  Returns an {@link Optional} containing the {@link StyleSheet}
     *  or an empty {@link Optional} if no {@link StyleSheet} is set.
     */
    Optional<StyleSheet> styleSheet() {
        return Optional.ofNullable(_styleSheet);
    }

    /**
     *  Used to configure the default font, which may be used by the {@link SwingTree}
     *  to derive the UI scaling factor and or to install the font in the {@link javax.swing.UIManager}
     *  depending on the {@link FontInstallation} mode (see {@link #defaultFont(Font, FontInstallation)}).
     * @param newDefaultFont The new default font, or {@code null} to unset the default font.
     * @return A new {@link SwingTreeInitConfig} instance with the new default font.
     */
    public SwingTreeInitConfig defaultFont( Font newDefaultFont ) {
        return new SwingTreeInitConfig(newDefaultFont, _fontInstallation, _scaling, _eventProcessor, _styleSheet);
    }

    /**
     *  Used to configure both the default {@link Font} and the {@link FontInstallation} mode.
     *  <p>
     *  The {@link FontInstallation} mode determines how the {@link Font} is installed
     *  in the {@link javax.swing.UIManager} (see {@link FontInstallation}).
     *  <p>
     *  If the {@link FontInstallation} mode is {@link FontInstallation#SOFT},
     *  then the {@link Font} will only be installed as the "defaultFont" property
     *  in the {@link javax.swing.UIManager}. If on the other hand the {@link FontInstallation}
     *  mode is {@link FontInstallation#HARD}, then the {@link Font} will be installed
     *  by replacing all {@link Font}s in the {@link javax.swing.UIManager}.
     *
     * @param newDefaultFont The new default font, or {@code null} to unset the default font.
     * @param newFontInstallation The new {@link FontInstallation} mode.
     * @return A new {@link SwingTreeInitConfig} instance with the new default font and {@link FontInstallation} mode.
     */
    public SwingTreeInitConfig defaultFont( Font newDefaultFont, FontInstallation newFontInstallation ) {
        return new SwingTreeInitConfig(newDefaultFont, newFontInstallation, _scaling, _eventProcessor, _styleSheet);
    }

    /**
     *  Used to configure the {@link Scaling} mode, which determines how the UI scaling factor
     *  is determined (see {@link Scaling}).
     *  If the {@link Scaling} mode is {@link Scaling#FROM_FONT}, then the UI scaling factor
     *  will be derived from the default font size. If on the other hand the {@link Scaling}
     *  mode is {@link Scaling#FROM_SYSTEM_FONT}, then the UI scaling factor will be derived
     *  from the system font size.
     *
     * @param newScaling The new {@link Scaling} mode.
     * @return A new {@link SwingTreeInitConfig} instance with the new {@link Scaling} mode.
     */
    public SwingTreeInitConfig scaling( Scaling newScaling ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, newScaling, _eventProcessor, _styleSheet);
    }

    /**
     *  Used to configure the {@link EventProcessor}, which is used to process both
     *  UI and application events.
     *  You may create your own {@link EventProcessor} implementation or use one of the
     *  predefined ones like {@link EventProcessor#COUPLED_STRICT}, {@link EventProcessor#COUPLED}
     *  or {@link EventProcessor#DECOUPLED}.
     *
     * @param newEventProcessor The new {@link EventProcessor}.
     * @return A new {@link SwingTreeInitConfig} instance with the new {@link EventProcessor}.
     */
    public SwingTreeInitConfig eventProcessor( EventProcessor newEventProcessor ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, _scaling, newEventProcessor, _styleSheet);
    }

    /**
     *  Used to configure a global {@link StyleSheet} serving as a base for all
     *  {@link StyleSheet}s used inside your application (see {@link UI#use(swingtree.style.StyleSheet, Supplier)}).
     *
     * @param newStyleSheet The new {@link StyleSheet}, or {@code null} to unset the {@link StyleSheet}.
     * @return A new {@link SwingTreeInitConfig} instance with the new {@link StyleSheet}.
     */
    public SwingTreeInitConfig styleSheet( StyleSheet newStyleSheet ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, _scaling, _eventProcessor, newStyleSheet);
    }
}

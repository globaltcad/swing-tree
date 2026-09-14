package examples.mixer;

import java.util.Locale;

/**
 *  How the number labels along the sliders are written. The engineer of the session picks it in
 *  the View menu, and the frequency and limiter scales follow through
 *  {@code SliderTicks.withLabelLocale(Locale)}: "1400" for the plain style, "1,400" in English
 *  and "1.400" in German, and "-2.5" against "-2,5" for the limiter.
 */
public enum LabelLanguage
{
    PLAIN  ("Plain numbers (1400 · -2.5)",   Locale.ROOT),
    ENGLISH("English (1,400 · -2.5)",        Locale.US),
    DEUTSCH("Deutsch (1.400 · -2,5)",        Locale.GERMANY);

    private final String title;
    private final Locale locale;

    LabelLanguage( String title, Locale locale ) {
        this.title  = title;
        this.locale = locale;
    }

    public Locale locale() { return locale; }

    @Override
    public String toString() { return title; }
}

package utility;

import swingtree.SwingTreeConfigurator;
import swingtree.SwingTreeInitConfig;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URL;
import java.util.Objects;

public class SwingTreeTestConfigurator implements SwingTreeConfigurator
{
    private static final SwingTreeTestConfigurator _INSTANCE = new SwingTreeTestConfigurator();

    public static SwingTreeTestConfigurator get() {
        return _INSTANCE;
    }

    private SwingTreeTestConfigurator() {
        _loadFonts();
    }

    private void _loadFonts() {

        String[] testFonts = {
                "/fonts/Ubuntu-Regular.ttf",
                "/fonts/Buggie-L3y03.ttf",
                "/fonts/DancingScript-3j68.ttf",
                "/fonts/AlloyInk-nRLyO.ttf",
                "/fonts/roboto-black.ttf"
        };

        // We simply load the fonts here, so that they are available
        // for the tests.
        for (String testFont : testFonts) {
            _loadFont(testFont);
        }
    }

    private void _loadFont(String fontPath) {
        try {
            URL fontUrl = getClass().getResource(fontPath);
            Objects.requireNonNull(fontUrl, "Font not found: " + fontPath);
            File fontFile = new File(fontUrl.toURI());
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SwingTreeInitConfig configure(SwingTreeInitConfig config) {
        return config.defaultFont(
                        new Font("Ubuntu", Font.PLAIN, 13),
                        SwingTreeInitConfig.FontInstallation.HARD
                    );
    }
}

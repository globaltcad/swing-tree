package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.border.MatteBorder
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

@Title("Delegating to the Border a Component already had")
@Narrative('''

    A Swing component usually arrives with a border its look and feel installed, and
    SwingTree replaces that border with one of its own, so that it can paint styles,
    animations and borders of your own. It does not throw the original away though:
    as long as your style does not define a border itself, the original is still
    painted, so a component you did not style in that respect keeps looking exactly
    the way the look and feel intended.

    "Exactly" is the word this specification is about. `getBorder()` really does hand
    back a different object afterwards, so "it still looks the same" is a claim about
    SwingTree rather than something Swing guarantees for us, and claims like that are
    worth holding to pixels.

    Every test below therefore renders a component SwingTree built next to a plain Swing
    component wearing the very same border, and insists the two images are identical -
    at a modest size and at a maximized one, on an ordinary screen and on a high
    resolution one, for every border we could get our hands on.

''')
@Subject([UI])
class Border_Delegation_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

    def setup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def 'A component keeps the border it came with, pixel for pixel.'()
    {
        reportInfo """
            The same border is worn by two panels: one built through SwingTree, and one a plain
            `JPanel` which Swing paints all by itself. Rendering both has to produce the very
            same image, whatever the border is and whatever size the panel was stretched to.
        """
        given : 'A panel handed to SwingTree with that border already on it, styled only with a padding.'
            var swingTreePanel = swingTreePanelWearing(inheritedBorder)
        and : 'And a plain Swing panel wearing the very same border.'
            var plainPanel = plainPanelWearing(inheritedBorder)

        when : 'We render both at the same size,'
            var throughSwingTree = render(swingTreePanel, width, height)
            var plainSwing       = render(plainPanel, width, height)

        then : 'SwingTree really did take the border over, so there was something to compare,'
            swingTreePanel.getBorder() !== inheritedBorder
            plainPanel.getBorder() === inheritedBorder
        and : 'the border really did leave paint behind, so the comparison is not vacuous,'
            paintedPixels(plainSwing) > 0
        and : 'not a single pixel differs.'
            differingPixels(throughSwingTree, plainSwing) == 0

        where : 'We try every kind of border we can think of, on a modest panel and on a maximized one.'
            inheritedBorder                                         | width | height
            new LineBorder(Color.RED, 1, false)                     | 300   | 200
            new LineBorder(Color.RED, 1, false)                     | 1400  | 1000
            new LineBorder(Color.BLUE, 5, false)                    | 1400  | 1000
            new LineBorder(new Color(0, 128, 0, 128), 3, true)      | 1400  | 1000
            new MatteBorder(4, 8, 12, 16, Color.MAGENTA)            | 1400  | 1000
            BorderFactory.createEtchedBorder()                      | 1400  | 1000
            BorderFactory.createRaisedSoftBevelBorder()             | 1400  | 1000
            BorderFactory.createLoweredBevelBorder()                | 1400  | 1000
            BorderFactory.createTitledBorder("A long enough title") | 1400  | 1000
            BorderFactory.createDashedBorder(Color.BLUE)            | 1400  | 1000
            compound()                                              | 1400  | 1000
            new RoundedOutlineBorder(1, 8)                          | 1400  | 1000
            new RoundedOutlineBorder(6, 24)                         | 1400  | 1000
            new RoundedOutlineBorder(3, 64)                         | 1400  | 1000
    }

    def 'A component keeps the border it came with on a high resolution screen too.'()
    {
        reportInfo """
            On a high resolution display the graphics a component paints into carries a scale
            factor, and at 125% or 150% - the two most common desktop scalings - a whole number
            of component pixels is not a whole number of screen pixels. That is where a border
            is most easily rendered a hair differently than Swing would render it, so it is
            worth checking rather than assuming.
        """
        given : 'A SwingTree panel and a plain Swing panel, both wearing a thin antialiased outline.'
            var inheritedBorder = new RoundedOutlineBorder(1, 12)
            var swingTreePanel = swingTreePanelWearing(inheritedBorder)
            var plainPanel = plainPanelWearing(inheritedBorder)

        when : 'We render both onto a screen scaled by the given factor,'
            var throughSwingTree = render(swingTreePanel, 1400, 1000, scale)
            var plainSwing       = render(plainPanel, 1400, 1000, scale)

        then : 'the border really did leave paint behind, so the comparison is not vacuous,'
            paintedPixels(plainSwing) > 0
        and : 'the two are still identical.'
            differingPixels(throughSwingTree, plainSwing) == 0

        where : 'We try the scalings a real desktop actually uses.'
            scale << [1.0d, 1.25d, 1.5d, 2.0d]
    }

    def 'The borders a real look and feel installs survive the delegation.'()
    {
        reportInfo """
            Hand written borders can only stand in for the real thing, so here we take the actual
            borders out of the look and feels a SwingTree application is likely to run under and
            hold them to the same standard.

            Note that a border is asked to paint onto a plain `JPanel` here rather than onto the
            component it was made for, and not every look and feel border draws anything in that
            situation - Metal's scroll pane border, for one, draws nothing at all. That is what
            the check on painted pixels below is for: a border that paints nothing would compare
            two blank images and pass without testing anything.
        """
        given : 'We switch to the given look and feel and take the border it gives a component.'
            Utility.setLaF(lookAndFeel)
            var inheritedBorder = borderSource.call()

        and : 'A SwingTree panel and a plain Swing panel, both wearing it.'
            var swingTreePanel = swingTreePanelWearing(inheritedBorder)
            var plainPanel = plainPanelWearing(inheritedBorder)

        when : 'We render both, on an ordinary screen and on a fractionally scaled one,'
            var plainOnAnOrdinaryScreen = render(plainPanel, 1400, 1000)
            var plainOnAHiDpiScreen     = render(plainPanel, 1400, 1000, 1.25d)
            var onAnOrdinaryScreen = differingPixels(render(swingTreePanel, 1400, 1000), plainOnAnOrdinaryScreen)
            var onAHiDpiScreen     = differingPixels(render(swingTreePanel, 1400, 1000, 1.25d), plainOnAHiDpiScreen)

        then : 'this look and feel border really does paint onto a panel, so the comparison means something,'
            paintedPixels(plainOnAnOrdinaryScreen) > 0
            paintedPixels(plainOnAHiDpiScreen) > 0
        and : 'both renderings are identical.'
            onAnOrdinaryScreen == 0
            onAHiDpiScreen == 0

        where : 'We try the look and feels a SwingTree application is likely to run under.'
            lookAndFeel             | borderSource
            Utility.LaF.NIMBUS      | { new JScrollPane().getBorder() }
            Utility.LaF.NIMBUS      | { new JTextField().getBorder() }
            Utility.LaF.NIMBUS      | { new JTextArea().getBorder() }
            Utility.LaF.FLAT_BRIGHT | { new JScrollPane().getBorder() }
            Utility.LaF.FLAT_BRIGHT | { new JTextField().getBorder() }
            Utility.LaF.FLAT_BRIGHT | { new JButton("x").getBorder() }
            Utility.LaF.DEFAULT     | { new JTextField().getBorder() }
    }

    def 'A component styled with a border of its own does not paint the one it came with.'()
    {
        reportInfo """
            The inherited border is a fallback, not an addition: as soon as your style defines a
            border, that is the border, and the one the look and feel installed is not painted at
            all. This is the condition under which the delegation above happens in the first
            place, so it belongs next to it.
        """
        given : 'A panel with a look and feel border, styled with a SwingTree border on top.'
            var counting = new CountingBorder(new LineBorder(Color.RED, 1, false))
            var raw = new JPanel()
            raw.setBorder(counting)
            var panel = UI.of(raw).withStyle(it -> it.border(3, Color.BLUE) ).get(JPanel)

        when : 'We render the component,'
            render(panel, 1400, 1000)

        then : 'the border it came with was never asked to paint anything.'
            counting.timesPainted == 0
    }

    // ────────────────────────── helpers ──────────────────────────

    /**
     *  A panel handed to SwingTree with the border already on it, which is the situation a real
     *  application is in: the look and feel installed the border long before any styling happened.
     */
    private static JPanel swingTreePanelWearing( Border border ) {
        var panel = new JPanel()
        panel.setBorder(border)
        return UI.of(panel).withStyle(it -> it.padding(1) ).get(JPanel)
    }

    private static JPanel plainPanelWearing( Border border ) {
        var panel = new JPanel()
        panel.setBorder(border)
        return panel
    }

    private static Border compound() {
        return BorderFactory.createCompoundBorder(
                    new LineBorder(Color.RED, 2, true), new EmptyBorder(4, 4, 4, 4)
                )
    }

    /**
     *  Renders a component the way Swing renders it - sized, laid out and painted into an image -
     *  optionally onto a screen whose pixels are {@code scale} times as dense.
     *  <p>
     *  Note that this resizes the component and makes it transparent, so that only its border
     *  ends up in the image. It is therefore not free of side effects on what it is handed, which
     *  is why every test builds its panels fresh rather than sharing them.
     */
    private static BufferedImage render( JComponent component, int width, int height, double scale = 1.0d ) {
        int deviceWidth  = (int) Math.ceil(width  * scale)
        int deviceHeight = (int) Math.ceil(height * scale)
        BufferedImage image = Utility.createDeterministicImage(deviceWidth, deviceHeight)
        UI.runNow(() -> {
            component.setOpaque(false)
            component.setSize(width, height)
            component.doLayout()
            Graphics2D g = Utility.createDeterministicGraphics(image)
            try {
                g.scale(scale, scale)
                g.clipRect(0, 0, width, height)
                component.paint(g)
            } finally {
                g.dispose()
            }
        })
        return image
    }

    /** How many pixels the border actually put paint on, so that a comparison of two blank images cannot pass. */
    private static int paintedPixels( BufferedImage image ) {
        int painted = 0
        for ( int y = 0; y < image.getHeight(); y++ )
            for ( int x = 0; x < image.getWidth(); x++ )
                if ( (image.getRGB(x, y) >>> 24) != 0 )
                    painted++
        return painted
    }

    private static int differingPixels( BufferedImage a, BufferedImage b ) {
        int differing = 0
        for ( int y = 0; y < a.getHeight(); y++ )
            for ( int x = 0; x < a.getWidth(); x++ )
                if ( a.getRGB(x, y) != b.getRGB(x, y) )
                    differing++
        return differing
    }

    /** A border which counts how often it was asked to paint, and then paints like any other. */
    static class CountingBorder implements Border {
        int timesPainted = 0
        private final Border _delegate
        CountingBorder( Border delegate ) { _delegate = delegate }
        @Override void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
            timesPainted += 1
            _delegate.paintBorder(c, g, x, y, width, height)
        }
        @Override Insets getBorderInsets( Component c ) { return _delegate.getBorderInsets(c) }
        @Override boolean isBorderOpaque() { return _delegate.isBorderOpaque() }
    }

    /** An antialiased rounded outline, which is what a modern look and feel draws. */
    static class RoundedOutlineBorder implements Border {
        private final int _thickness
        private final int _arcRadius
        RoundedOutlineBorder( int thickness, int arcRadius ) { _thickness = thickness; _arcRadius = arcRadius }
        @Override void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
            Graphics2D g2 = (Graphics2D) g.create()
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setColor(new Color(20, 90, 200))
                // A hollow shape, exactly as a look and feel builds it, so that the
                // antialiased coverage really does span the whole component:
                Area ring = new Area(new RoundRectangle2D.Float(
                                    x as float, y as float, width as float, height as float,
                                    (_arcRadius * 2) as float, (_arcRadius * 2) as float))
                ring.subtract(new Area(new RoundRectangle2D.Float(
                                    (x + _thickness) as float, (y + _thickness) as float,
                                    (width - 2 * _thickness) as float, (height - 2 * _thickness) as float,
                                    (_arcRadius * 2) as float, (_arcRadius * 2) as float)))
                g2.fill(ring)
            } finally {
                g2.dispose()
            }
        }
        @Override Insets getBorderInsets( Component c ) {
            return new Insets(_thickness, _thickness, _thickness, _thickness)
        }
        @Override boolean isBorderOpaque() { return false }
    }
}

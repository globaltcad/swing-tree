package stml;

import javax.swing.*;
import java.awt.*;

public class Example extends JPanel
{
	private static final Color HIGHLIGHT   = new Color(122, 200,  98);
	private static final Color RELEVANT    = new Color(255, 255, 255); // WHITE!
	private static final Color UNIMPORTANT = new Color(192, 192, 192); // Light gray!

	// Use this to test the UI!
	public static void main(String... args) {
		new UI.TestWindow(JFrame::new,new Example()).getFrame().setSize(new Dimension(700, 300));
	}

	public Example()
	{
		Color backColor = RELEVANT;
		Color pathColor = UNIMPORTANT;

		ImageIcon pdfIcon  = new ImageIcon(getClass().getResource("/img/seed.png"));
		ImageIcon pdfHover = new ImageIcon(getClass().getResource("/img/trees.png"));
		ImageIcon cover    = new ImageIcon(getClass().getResource("/img/swing.png"));
		String description = "All of this is less than a hundred lines of code!";
		String type        = "Built with STML";
		String typeLength  = "GTS-OSS";
		String dimension   = "29-March-2022";
		String debug = "";
		boolean detailsVisible    = true;

		UI.of(this)
			.withLayout(debug+"fill", "[][grow]")
			.doIf( backColor != null, it -> it.withBackground(backColor) )
			.onMouseClick( e -> {/* does something */} )
			.add(
				UI.labelWithIcon(200, 200, cover).id("I am the name of this component! (Not visible in UI)")
					.onMouseClick( e -> {/* does something */} )
			)
			.add("grow",
				UI.panelWithLayout(debug+"fill, insets 0","[grow][shrink]")
					.onMouseClick( e -> {/* does something */} )
					.add("cell 0 0",
						UI.label("Hello and welcome to this UI!")
							.makeBold()
							.makeLinkTo("https://github.com/gts-oss")
					)
					.add("cell 0 1, grow, pushy",
						UI.panelWithLayout(debug+"fill, insets 0","[grow][shrink]")
							.doIf( backColor != null, it -> it.withBackground(backColor) )
							.onMouseClick( e -> {/* does something */} )
							.add("cell 0 0, aligny top",
								UI.label("<html><div style=\"width:275px;\">"+ description +"</div></html>")
									.withColor(Color.GRAY)
									.onMouseClick( e -> {/* does something */} )
							)
							.add("cell 1 0",
								UI.buttonWithIcon(50, 50, pdfIcon, pdfHover)
									.withCursor(UI.Cursor.HAND)
									.make( it -> {
										it.setBorderPainted(false);
										it.setContentAreaFilled(false);
										it.setOpaque(false);
										it.setFocusPainted(false);
										it.setMargin(new Insets(0,0,0,0));
									})
									.onClick( e -> {/* does something */} )
							)
							.withBorder(BorderFactory.createMatteBorder(0,0,1,0,pathColor))
					)
					.add("cell 0 2, grow",
						UI.panelWithLayout(debug+"fill, insets 0 0 0 0","[grow][grow][grow]")
							.doIf( backColor != null, it -> it.withBackground(backColor) )
							.add("cell 1 0", UI.label(type      ).doIf(backColor != null, it -> it.withBackground(backColor) ).isVisibleIf(detailsVisible))
							.add("cell 2 0", UI.label(typeLength).doIf(backColor != null, it -> it.withBackground(backColor) ).isVisibleIf(detailsVisible))
							.add("cell 3 0", UI.label(dimension ).doIf(backColor != null, it -> it.withBackground(backColor) ).isVisibleIf(detailsVisible))
					)
					.add("cell 0 3, span 2, grow", //  filepath
						UI.label("...here the UI comes to an end...").withColor(pathColor)
					)
					.doIf( backColor != null, it -> it.withBackground(backColor) )
			);
	}

}

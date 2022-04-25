package com.globaltcad.swingtree;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class Example extends JPanel
{
	public Example()
	{
		FlatLightLaf.setup();
		ImageIcon pdfIcon  = new ImageIcon(getClass().getResource("/img/seed.png"));
		ImageIcon pdfHover = new ImageIcon(getClass().getResource("/img/trees.png"));
		ImageIcon cover    = new ImageIcon(getClass().getResource("/img/swing.png"));

		String description = "All of this is less than a hundred lines of code!<br>The layout of this is powered by MigLayout.<br><br>";
		String debug = "";

		UI.of(this)
		.withLayout(debug+"fill", "[][grow]")
		.withBackground(Color.WHITE)
		.add(UI.labelWithIcon(200, 200, cover).withTooltip("I am a picture of a swing on a tree!"))
		.add("grow",
			UI.panelWithLayout(debug+"fill, insets 0","[grow][shrink]")
			.onMouseClick( e -> {/* does something */} )
			.add("cell 0 0",
				UI.label("Hello and welcome to this UI! (Click me I'm a link)")
				.makeBold()
				.makeLinkTo("https://github.com/gts-oss")
			)
			.add("cell 0 1, grow, pushy",
				UI.panelWithLayout(debug+"fill, insets 0","[grow][shrink]")
				.withBackground(Color.WHITE)
				.add("cell 0 0, aligny top, grow x",
					UI.panelWithLayout(debug+"fill, insets 7","grow")
					.withBackground(new Color(255, 138, 99))
					.add( "span",
						UI.label("<html><div style=\"width:275px;\">"+ description +"</div></html>")
					)
					.add("shrink x", UI.label("First Name"))
					.add("grow", UI.of(new JTextField("")))
					.add("gap unrelated", UI.label("Last Name"))
					.add("wrap, grow", UI.of(new JTextField("")))
					.add(UI.label("Address"))
					.add("span, grow", UI.of(new JTextField("")))
					.add( "span, grow", UI.label("Here is a text area:"))
					.add("span, grow", UI.of(new JTextArea("")))
				)
				.add("cell 1 0",
					UI.buttonWithIcon(50, 50, pdfIcon, pdfHover)
					.withCursor(UI.Cursor.HAND)
					.makePlain()
					.onClick( e -> {/* does something */} )
				)
				.withBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY))
			)
			.add("cell 0 2, grow",
				UI.panelWithLayout(debug+"fill, insets 0 0 0 0","[grow][grow][grow]")
				.withBackground(Color.WHITE)
				.add("cell 1 0", UI.label("Built with swingtree"))
				.add("cell 2 0", UI.label("GTS-OSS"))
				.add("cell 3 0", UI.label("29-March-2022"))
			)
			.add("cell 0 3, span 2, grow",
				UI.label("...here the UI comes to an end...").withColor(Color.LIGHT_GRAY)
			)
			.withBackground(Color.WHITE)
		);
	}

	// Use this to test the UI!
	public static void main(String... args) {
		new UI.TestWindow(JFrame::new,new Example()).getFrame().setSize(new Dimension(700, 300));
	}
}

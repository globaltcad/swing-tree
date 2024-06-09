package swingtree;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;
import swingtree.api.CellInterpreter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.*;

/**
 * 	An API for building extensions of the {@link DefaultTableCellRenderer} in a functional style.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a collection of examples demonstrating how to use the API of this class.</b>
 */
@Immutable
public final class Render<C extends JComponent,E>
{
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(Render.class);

}

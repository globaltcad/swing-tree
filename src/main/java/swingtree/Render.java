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

	/**
	 * 	This interface models the API of the {@link Render} builder which allows you to
	 * 	specify how a cell should be rendered.
	 * 	Most likely you will want to call {@link #asText(Function)}
	 * 	on this as most cells are rendered as simple texts.
	 * 	An example would be a combo box containing enum values, which
	 * 	you don't want to render as the enum name (all capital letters), but rather as a
	 * 	more human-readable string.
	 *
	 * @param <C> The type of the component which is used to render the cell.
	 * @param <E> The type of the value of the cell.
	 * @param <T> The type of the value of the cell.
	 */
	public interface As<C extends JComponent, E, T extends E>
	{
		/**
		 * 	Specify a lambda which receives a {@link CellDelegate} instance
		 * 	for you to customize its renderer.
		 * 	This is the most generic way to customize the rendering of a cell,
		 * 	as you can choose between vastly different ways of rendering:
		 * 	<pre>{@code
		 * 		.when( MyEnum.class )
		 * 		.as( cell -> {
		 * 			// do component based rendering:
		 * 			cell.setRenderer( new JLabel( "Hello World" ) );
		 * 			// or do graphics rendering directly:
		 * 			cell.setRenderer( g -> {
		 * 				// draw something
		 * 				g.drawString( "Hello World", 0, 0 );
		 * 			});
		 * 		})
		 * 	}</pre>
		 *
		 * @param valueInterpreter A lambda which customizes the provided cell.
		 * @return The builder API allowing method chaining.
		 */
		RenderBuilder<C, E> as(CellInterpreter<C,T> valueInterpreter );

		/**
		 * 	Specify a lambda which receives a {@link CellDelegate} instance
		 * 	and return a {@link Component} which is then used to render the cell.
		 * 	<pre>{@code
		 * 		.when( MyEnum.class )
		 * 		.asComponent( cell -> new JLabel( "Hello World" ) )
		 * 	}</pre>
		 * @param renderer A function which returns a {@link Component} which is then used to render the cell.
		 * @return The builder API allowing method chaining.
		 */
		default RenderBuilder<C, E> asComponent(Function<CellDelegate<C,T>, Component> renderer ) {
			return this.as( cell -> cell.setRenderer(renderer.apply(cell)) );
		}

		/**
		 * 	Specify a lambda which receives a {@link CellDelegate} instance
		 * 	and return a {@link String} which is then used to render the cell.
		 * 		<pre>{@code
		 * 		.when( MyEnum.class )
		 * 		.asText( cell -> "Hello World" )
		 * 	}</pre>
		 *
		 * @param renderer A function which returns a {@link String} which is then used to render the cell.
		 * @return The builder API allowing method chaining.
		 */
		default RenderBuilder<C, E> asText(Function<CellDelegate<C,T>, String> renderer ) {
			return this.as(RenderBuilder._createDefaultTextRenderer(renderer));
		}

		/**
		 *  Specify a lambda which receives a {@link CellDelegate} instance as well as a {@link Graphics} instance
		 *  and then renders the cell.
		 *  <pre>{@code
		 *  	.when( MyEnum.class )
		 *  	.render( (cell, g) -> {
		 *  		// draw something
		 *  		g.drawString( "Hello World", 0, 0 );
		 *  	})
		 * }</pre>
		 * @param renderer A function which receives a {@link CellDelegate} instance as well as a {@link Graphics} instance and then renders the cell.
		 * @return The builder API allowing method chaining.
		 */
		default RenderBuilder<C, E> render(BiConsumer<CellDelegate<C,T>, Graphics2D> renderer ) {
			return this.as( cell -> cell.setRenderer(new JComponent(){
				@Override public void paintComponent(Graphics g) {
					try {
						renderer.accept(cell, (Graphics2D) g);
					} catch (Exception e) {
						log.warn("An exception occurred while rendering a cell!", e);
					    /*
					        We log as warning because exceptions during rendering are not considered
					        as harmful as elsewhere!
					    */
					}
				}
			}) );
		}
	}
}

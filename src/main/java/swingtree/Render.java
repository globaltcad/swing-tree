package swingtree;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.*;
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

	private final Class<C> _componentType;
	private final Class<E> _elementType;


    static <E> Render<JList<E>,E> forList(Class<E> elementType) {
		Render r = new Render<>(JList.class, elementType);
		return (Render<JList<E>,E>) r;
	}
	static <C extends JComboBox<E>, E> Render<C,E> forCombo(Class<E> elementType) {
		Render r = new Render<>(JComboBox.class, elementType);
		return (Render<C,E>) r;
	}
	static <E> Render<JTable,E> forTable(Class<E> elementType) {
		Render r = new Render<>(JTable.class, elementType);
		return (Render<JTable,E>) r;
	}

	private Render(Class<C> componentType, Class<E> elementType) {
		_componentType = componentType;
		_elementType   = elementType;
    }

	/**
	 * 	Use this to specify which type of values should have custom rendering.
	 * 	The object returned by this method allows you to specify how to render the values.
	 *
	 * @param valueType The type of cell value, for which you want custom rendering.
	 * @param <T> The type parameter of the cell value, for which you want custom rendering.
	 * @return The {@link As} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
	 */
	public <T extends E> As<C,E,T> when( Class<T> valueType ) {
		NullUtil.nullArgCheck(valueType, "valueType", Class.class);
		return when( valueType, cell -> true );
	}

	/**
	 * 	Use this to specify which type of values should have custom rendering.
	 * 	The object returned by this method allows you to specify how to render the values.
	 *
	 * @param valueType The type of cell value, for which you want custom rendering.
	 * @param valueValidator A condition which ought to be met for the custom rendering to be applied to the value.
	 * @param <T> The type parameter of the cell value, for which you want custom rendering.
	 * @return The {@link As} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
	 */
	public <T extends E> As<C,E,T> when(
			Class<T> valueType,
			Predicate<Cell<C,T>> valueValidator
	) {
		NullUtil.nullArgCheck(valueType, "valueType", Class.class);
		NullUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
		// We check if T is a subclass of E, if not we throw an exception:
		if ( !valueType.isAssignableFrom(_elementType) )
			throw new IllegalArgumentException("The value type "+valueType+" is not a subclass of "+_elementType);
		return new As<C,E,T>() {
			@Override
			public Builder<C,E> as(Cell.Interpreter<C,T> valueInterpreter) {
				NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
				return new Builder(_componentType, valueType, valueValidator, valueInterpreter);
			}
		};
	}

	/**
	 * 	This interface models an individual table cell alongside
	 * 	various properties that a cell should have, like for example
	 * 	the value of the cell, its position within the table
	 * 	as well as a renderer in the form of a AWT {@link Component}
	 * 	which may or not be replaced or modified.
	 *
	 * @param <V> The value type of the entry of this {@link Cell}.
	 */
	public interface Cell<C extends JComponent, V>
	{
		C getComponent();
		Optional<V> value();
		default Optional<String> valueAsString() { return value().map(Object::toString); }
		boolean isSelected();
		boolean hasFocus();
		int getRow();
		int getColumn();
		Component getRenderer();
		void setRenderer(Component component);
		void setToolTip(String toolTip);
		void setDefaultRenderValue(Object newValue);
		default void setRenderer(Consumer<Graphics2D> painter) {
			setRenderer(new Component() {
				@Override
				public void paint(Graphics g) {
					super.paint(g);
					painter.accept((Graphics2D) g);
				}
			});
		}

		/**
		 * 	An interface for interpreting the value of a {@link Cell} and
		 * 	setting a {@link Component} or custom renderer which is then used to render the cell.
		 * 	Use {@link Cell#setRenderer(Component)} or {@link Cell#setRenderer(Consumer)}
		 * 	to define how the cell should be rendered exactly.
		 *
		 * @param <C> The type of the component which is used to render the cell.
		 * @param <V> The type of the value of the cell.
		 */
		@FunctionalInterface
		interface Interpreter<C extends JComponent, V> {

			/**
			 * 	Interprets the value of a {@link Cell} and produces a {@link Component}
			 * 	which is then used to render the cell.
			 *
			 * @param cell The cell which is to be rendered.
			 */
			void interpret( Cell<C, V> cell );

		}
	}

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
		 * 	Specify a lambda which receives a {@link Cell} instance
		 * 	for you to customize its renderer.
		 * 	This is the most generic way to customize the rendering of a cell,
		 * 	as you can choose between vastly different ways of rendering:
		 * 	<pre>{@code
		 * 	    UI.renderTable()
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
		Builder<C, E> as( Cell.Interpreter<C,T> valueInterpreter );

		/**
		 * 	Specify a lambda which receives a {@link Cell} instance
		 * 	and return a {@link Component} which is then used to render the cell.
		 * 	<pre>{@code
		 * 		.when( MyEnum.class )
		 * 		.asComponent( cell -> new JLabel( "Hello World" ) )
		 * 	}</pre>
		 * @param renderer A function which returns a {@link Component} which is then used to render the cell.
		 * @return The builder API allowing method chaining.
		 */
		default Builder<C, E> asComponent( Function<Cell<C,T>, Component> renderer ) {
			return this.as( cell -> cell.setRenderer(renderer.apply(cell)) );
		}

		/**
		 * 	Specify a lambda which receives a {@link Cell} instance
		 * 	and return a {@link String} which is then used to render the cell.
		 * 		<pre>{@code
		 * 		.when( MyEnum.class )
		 * 		.asText( cell -> "Hello World" )
		 * 	}</pre>
		 *
		 * @param renderer A function which returns a {@link String} which is then used to render the cell.
		 * @return The builder API allowing method chaining.
		 */
		default Builder<C, E> asText( Function<Cell<C,T>, String> renderer ) {
			return this.as( cell -> {
				JLabel l = new JLabel(renderer.apply(cell));
				l.setOpaque(true);

				Color bg = null;
				Color fg = null;

				if ( cell.getComponent() instanceof JList ) {
					JList<?> jList = (JList<?>) cell.getComponent();
					bg = jList.getSelectionBackground();
					fg = jList.getSelectionForeground();
					if ( bg == null )
						bg = UIManager.getColor("List.selectionBackground");
					if ( fg == null )
						fg = UIManager.getColor("List.selectionForeground");
				}

				if ( cell.getComponent() instanceof JTable ) {
					JTable jTable = (JTable) cell.getComponent();
					bg = jTable.getSelectionBackground();
					fg = jTable.getSelectionForeground();
					if ( bg == null )
						bg = UIManager.getColor("Table.selectionBackground");
					if ( fg == null )
						fg = UIManager.getColor("Table.selectionForeground");
				}

				if ( bg == null && cell.getComponent() != null )
					bg = cell.getComponent().getBackground();
				if ( fg == null && cell.getComponent() != null )
					fg = cell.getComponent().getForeground();

				if ( bg == null )
					bg = UIManager.getColor( "ComboBox.selectionBackground" );
				if ( fg == null )
					fg = UIManager.getColor( "ComboBox.selectionForeground" );

				if ( bg == null )
					bg = UIManager.getColor( "List.dropCellBackground" );
				if ( fg == null )
					fg = UIManager.getColor( "List.dropCellForeground" );

				if ( bg == null )
					bg = UIManager.getColor( "ComboBox.background" );
				if ( fg == null )
					fg = UIManager.getColor( "ComboBox.foreground" );

				// Lastly we make sure the color is a user color, not a LaF color:
				if ( bg != null ) // This is because of a weired JDK bug it seems!
					bg = new Color( bg.getRGB() );
				if ( fg != null )
					fg = new Color( fg.getRGB() );

				if (cell.isSelected()) {
					if ( bg != null ) l.setBackground(bg);
					if ( fg != null ) l.setForeground(fg);
				}
				else {
					Color normalBg = Color.WHITE;
					if (  cell.getComponent() != null )
						normalBg = cell.getComponent().getBackground();

					// We need to make sure the color is a user color, not a LaF color:
					if ( normalBg != null )
						normalBg = new Color( normalBg.getRGB() ); // This is because of a weired JDK bug it seems!

					if ( cell.getRow() % 2 == 1 ) {
						// We determine if the base color is more bright or dark,
						// and then we set the foreground color accordingly
						double brightness = (0.299 * normalBg.getRed() + 0.587 * normalBg.getGreen() + 0.114 * normalBg.getBlue()) / 255;
						if ( brightness < 0.5 )
							normalBg = brighter(normalBg);
						else
							normalBg = darker(normalBg);
					}
					if ( bg != null ) l.setBackground( normalBg );
					if ( fg != null && cell.getComponent() != null )
						l.setForeground( cell.getComponent().getForeground() );
				}

				// TODO:
				//l.setEnabled(cell.getComponent().isEnabled());
				//l.setFont(cell.getComponent().getFont());

				Border border = null;
				if ( cell.hasFocus() ) {
					if ( cell.isSelected() )
						border = UIManager.getBorder( "List.focusSelectedCellHighlightBorder" );

					if ( border == null )
						border = UIManager.getBorder( "List.focusCellHighlightBorder" );
				}
				else
					border = UIManager.getBorder( "List.cellNoFocusBorder" );

				if ( border != null ) l.setBorder(border);

				cell.setRenderer(l);
			});
		}

		/**
		 *  Specify a lambda which receives a {@link Cell} instance as well as a {@link Graphics} instance
		 *  and then renders the cell.
		 *  <pre>{@code
		 *  	.when( MyEnum.class )
		 *  	.render( (cell, g) -> {
		 *  		// draw something
		 *  		g.drawString( "Hello World", 0, 0 );
		 *  	})
		 * }</pre>
		 * @param renderer A function which receives a {@link Cell} instance as well as a {@link Graphics} instance and then renders the cell.
		 * @return The builder API allowing method chaining.
		 */
		default Builder<C, E> render( BiConsumer<Cell<C,T>, Graphics2D> renderer ) {
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

	/**
	 * 	A builder for building simple customized {@link javax.swing.table.TableCellRenderer}!
	 *
	 * @param <C> The type of the component which is used to render the cell.
	 * @param <E> The type of the value of the cell.
	 */
	public static class Builder<C extends JComponent, E> {

		private final Class<C> _componentType;
        private final Map<Class<?>, java.util.List<Consumer<Cell<C,?>>>> _rendererLookup = new LinkedHashMap<>(16);

		public Builder(
				Class<C>                   componentType,
				Class<E>                   valueType,
				Predicate<Cell<C,E>>       valueValidator,
				Cell.Interpreter<C, E>     valueInterpreter
		) {
			NullUtil.nullArgCheck(valueType, "valueType", Class.class);
			NullUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
			NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
			_componentType = componentType;
            when(valueType, valueValidator).as(valueInterpreter);
		}

		/**
		 * 	Use this to specify which type of values should have custom rendering.
		 * 	The object returned by this method allows you to specify how to render the values.
		 *
		 * @param valueType The type of cell value, for which you want custom rendering.
		 * @param <T> The type parameter of the cell value, for which you want custom rendering.
		 * @return The {@link As} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
		 */
		public <T extends E> As<C,E,T> when( Class<T> valueType ) {
			NullUtil.nullArgCheck(valueType, "valueType", Class.class);
			return when( valueType, cell -> true );
		}

		public <T extends E> As<C,E,T> when(
				Class<T> valueType,
				Predicate<Cell<C,T>> valueValidator
		) {
			NullUtil.nullArgCheck(valueType, "valueType", Class.class);
			NullUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
			return new As<C,E,T>() {
				@Override
				public Builder<C,E> as( Cell.Interpreter<C,T> valueInterpreter ) {
					NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
					_store(valueType, valueValidator, valueInterpreter);
					return Builder.this;
				}
			};
		}

		private void _store(
			Class valueType,
			Predicate predicate,
			Cell.Interpreter valueInterpreter
		) {
			NullUtil.nullArgCheck(valueType, "valueType", Class.class);
			NullUtil.nullArgCheck(predicate, "predicate", Predicate.class);
			NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
			List<Consumer<Cell<C,?>>> found = _rendererLookup.computeIfAbsent(valueType, k -> new ArrayList<>());
			found.add( cell -> {
				if ( predicate.test(cell) )
					valueInterpreter.interpret(cell);
			});
		}

		private class SimpleTableCellRenderer extends DefaultTableCellRenderer
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table,
					Object value,
					boolean isSelected,
					boolean hasFocus,
					final int row,
					int column
			) {
				List<Consumer<Cell<C,?>>> interpreter = _find(value, _rendererLookup);
				if ( interpreter.isEmpty() )
					return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				else {
					Component[] componentRef = new Component[1];
					Object[] defaultValueRef = new Object[1];
					List<String> toolTips = new ArrayList<>();
					Cell<JTable,Object> cell = new Cell<JTable,Object>() {
						@Override public JTable getComponent() {return table;}
						@Override public Optional<Object> value() { return Optional.ofNullable(value); }
						@Override public boolean isSelected() {return isSelected;}
						@Override public boolean hasFocus() {return hasFocus;}
						@Override public int getRow() {return row;}
						@Override public int getColumn() {return column;}
						@Override public Component getRenderer() {return SimpleTableCellRenderer.super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);}
						@Override public void setRenderer(Component component) {componentRef[0] = component;}
						@Override public void setToolTip(String toolTip) { toolTips.add(toolTip);}

						@Override public void setDefaultRenderValue(Object newValue) {defaultValueRef[0] = newValue;}
					};
					interpreter.forEach(consumer -> consumer.accept((Cell<C,?>)cell) );
					Component choice;
					if ( componentRef[0] != null )
						choice = componentRef[0];
					else if ( defaultValueRef[0] != null )
						choice = super.getTableCellRendererComponent(table, defaultValueRef[0], isSelected, hasFocus, row, column);
					else
						choice = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					if ( !toolTips.isEmpty() && choice instanceof JComponent )
						((JComponent)choice).setToolTipText(String.join("; ", toolTips));

					return choice;
				}
			}
		}

		private class SimpleListCellRenderer<O extends C> extends DefaultListCellRenderer
		{
			private final O _component;

			private SimpleListCellRenderer( O component ) {
				_component = Objects.requireNonNull(component);
			}

			@Override
			public Component getListCellRendererComponent(
			    JList   list,
			    Object  value,
			    final   int row,
			    boolean isSelected,
			    boolean hasFocus
			) {
				List<Consumer<Cell<C,?>>> interpreter = _find(value, _rendererLookup);
				if ( interpreter.isEmpty() )
					return super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
				else {
					Component[] componentRef = new Component[1];
					Object[] defaultValueRef = new Object[1];
					List<String> toolTips = new ArrayList<>();
					Cell<O,Object> cell = new Cell<O, Object>() {
						@Override public O getComponent() { return _component; }
						@Override public Optional<Object> value() { return Optional.ofNullable(value); }
						@Override public boolean isSelected() {return isSelected;}
						@Override public boolean hasFocus() {return hasFocus;}
						@Override public int getRow() {return row;}
						@Override public int getColumn() {return 0;}
						@Override public Component getRenderer() {return SimpleListCellRenderer.super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);}
						@Override public void setRenderer(Component component) {componentRef[0] = component;}
						@Override public void setToolTip(String toolTip) { toolTips.add(toolTip);}
						@Override public void setDefaultRenderValue(Object newValue) {defaultValueRef[0] = newValue;}
					};
					interpreter.forEach(consumer -> consumer.accept((Cell<C,?>)cell) );
					Component choice;
					if ( componentRef[0] != null )
						choice = componentRef[0];
					else if ( defaultValueRef[0] != null )
						choice = super.getListCellRendererComponent(list, defaultValueRef[0], row, isSelected, hasFocus);
					else
						choice = super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);

					if ( !toolTips.isEmpty() && choice instanceof JComponent )
						((JComponent)choice).setToolTipText(String.join("; ", toolTips));

					return choice;
				}
			}
		}

		private static <C extends JComponent> List<Consumer<Cell<C,?>>> _find(
		    Object value,
			Map<Class<?>, java.util.List<Consumer<Cell<C,?>>>> rendererLookup
		) {
			Class<?> type = ( value == null ? Object.class : value.getClass() );
			List<Consumer<Cell<C,?>>> cellRenderer = new ArrayList<>();
			for (Map.Entry<Class<?>, List<Consumer<Cell<C,?>>>> e : rendererLookup.entrySet()) {
				if ( e.getKey().isAssignableFrom(type) )
					cellRenderer.addAll(e.getValue());
			}
			return cellRenderer;
		}

		DefaultTableCellRenderer getForTable() {
			if ( JTable.class.isAssignableFrom(_componentType) )
				return new SimpleTableCellRenderer();
			else
				throw new IllegalArgumentException("Renderer was set up to be used for a JTable!");
		}

		/**
		 *  Like many things in the SwingTree library, this class is
		 *  essentially a convenient builder for a {@link ListCellRenderer}.
		 *  This internal method actually builds the {@link ListCellRenderer} instance,
		 *  see {@link UIForList#withRenderer(Function)} for more details
		 *  about how to use this class as pat of the main API.
		 *
		 * @param list The list for which the renderer is to be built.
		 * @return The new {@link ListCellRenderer} instance specific to the given list.
		 */
		ListCellRenderer<E> buildForList( C list ) {
			if ( JList.class.isAssignableFrom(_componentType) )
				return (ListCellRenderer<E>) new SimpleListCellRenderer<>(list);
			else if ( JComboBox.class.isAssignableFrom(_componentType) )
				throw new IllegalArgumentException(
						"Renderer was set up to be used for a JList! (not "+ _componentType.getSimpleName()+")"
					);
			else
				throw new IllegalArgumentException(
						"Renderer was set up to be used for an unknown component type! (cannot handle '"+ _componentType.getSimpleName()+"')"
					);
		}

		/**
		 *  Like many things in the SwingTree library, this class is
		 *  essentially a convenient builder for a {@link ListCellRenderer}.
		 *  This internal method actually builds the {@link ListCellRenderer} instance,
		 *  see {@link UIForList#withRenderer(Function)} for more details
		 *  about how to use this class as pat of the main API.
		 *
		 * @param comboBox The combo box for which the renderer is to be built.
		 * @return The new {@link ListCellRenderer} instance specific to the given combo box.
		 */
		ListCellRenderer<E> buildForCombo( C comboBox ) {
			if ( JComboBox.class.isAssignableFrom(_componentType) )
				return (ListCellRenderer<E>) new SimpleListCellRenderer<>(comboBox);
			else
				throw new IllegalArgumentException(
						"Renderer was set up to be used for a JComboBox! (not "+ _componentType.getSimpleName()+")"
					);
		}
	}


	private static Color darker( Color c ) {
		final double PERCENTAGE = (242*3.0)/(255*3.0);
		return new Color(
				(int)(c.getRed()*PERCENTAGE),
				(int)(c.getGreen()*PERCENTAGE),
				(int)(c.getBlue()*PERCENTAGE)
		);
	}

	private static Color brighter( Color c ) {
		final double FACTOR = (242*3.0)/(255*3.0);
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		int alpha = c.getAlpha();

		int i = (int)(1.0/(1.0-FACTOR));
		if ( r == 0 && g == 0 && b == 0) {
			return new Color(i, i, i, alpha);
		}
		if ( r > 0 && r < i ) r = i;
		if ( g > 0 && g < i ) g = i;
		if ( b > 0 && b < i ) b = i;

		return new Color(Math.min((int)(r/FACTOR), 255),
				Math.min((int)(g/FACTOR), 255),
				Math.min((int)(b/FACTOR), 255),
				alpha);
	}

}

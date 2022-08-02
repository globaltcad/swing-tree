package com.globaltcad.swingtree;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.*;

/**
 * 	An API for building extensions of the {@link DefaultTableCellRenderer} in a functional style.
 */
public final class Render<C extends JComponent,E> {

	private final Class<C> componentType;
	private final Class<E> elementType;
	private final Supplier<Border> borderSupplier;

	static <E> Render<JList<E>,E> forList(Class<E> elementType, Supplier<Border> borderSupplier) {
		Render r = new Render<>(JComboBox.class, elementType, borderSupplier);
		return (Render<JList<E>,E>) r;
	}
	static <E> Render<JComboBox<E>,E> forCombo(Class<E> elementType, Supplier<Border> borderSupplier) {
		Render r = new Render<>(JComboBox.class, elementType, borderSupplier);
		return (Render<JComboBox<E>,E>) r;
	}
	static <E> Render<JTable,E> forTable(Class<E> elementType, Supplier<Border> borderSupplier) {
		Render r = new Render<>(JTable.class, elementType, borderSupplier);
		return (Render<JTable,E>) r;
	}

	private Render(Class<C> componentType, Class<E> elementType, Supplier<Border> borderSupplier) {
		this.componentType = componentType;
		this.elementType = elementType;
		this.borderSupplier = borderSupplier;
	}

	/**
	 * 	Use this to specify which type of values should have custom rendering.
	 *
	 * @param valueType The type of cell value, for which you want custom rendering.
	 * @param <T> The type parameter of the cell value, for which you want custom rendering.
	 * @return The {@link As} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
	 */
	public <T extends E> As<C,E,T> when(Class<T> valueType) {
		LogUtil.nullArgCheck(valueType, "valueType", Class.class);
		return when( valueType, cell -> true );
	}

	/**
	 * 	Use this to specify which type of values should have custom rendering.
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
		LogUtil.nullArgCheck(valueType, "valueType", Class.class);
		LogUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
		return new As<C,E,T>() {
			@Override
			public Builder<C,E> as(Cell.Interpreter<C,T> valueInterpreter) {
				LogUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
				return new Builder(componentType,valueType, valueValidator, valueInterpreter, borderSupplier);
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
		C           getComponent();
		V	    	getValue();
		boolean 	isSelected();
		boolean 	hasFocus();
		int     	getRow();
		int     	getColumn();
		Component   getRenderer();
		void        setRenderer(Component component);
		void        setDefaultRenderValue(Object newValue);
		default void setRenderer(Consumer<Graphics2D> painter) {
			setRenderer(new Component() {
				@Override
				public void paint(Graphics g) {
					super.paint(g);
					painter.accept((Graphics2D) g);
				}
			});
		}

		@FunctionalInterface
		interface Interpreter<C extends JComponent, V> {

			void interpret(Cell<C, V> cell);

		}
	}


	public interface As<C extends JComponent, E, T extends E> {
		/**
		 * 	Specify a lambda which receives a {@link Cell} instance
		 * 	for you to customize its renderer.
		 *
		 * @param valueInterpreter A lambda which customizes the provided cell.
		 * @return The builder API allowing method chaining.
		 */
		Builder<C, E> as( Cell.Interpreter<C,T> valueInterpreter );

		default Builder<C, E> asComponent(Function<Cell<C,T>, Component> renderer) {
			return this.as( cell -> cell.setRenderer(renderer.apply(cell)) );
		}

		default Builder<C, E> asText(Function<Cell<C,T>, String> renderer) {
			return this.as( cell -> cell.setRenderer(new JLabel(renderer.apply(cell))) );
		}
	}

	/**
	 * 	A builder for building simple customized {@link javax.swing.table.TableCellRenderer}!
	 */
	public static class Builder<C extends JComponent, E> {

		private final Class<C> componentType;
		private final Supplier<Border> border;
		private final Map<Class<?>, java.util.List<BiConsumer<Class<?>, Cell<C,?>>>> interpreters = new HashMap<>(16);

		public Builder(
				Class<C> componentType,
				Class<E> valueType,
				Predicate<Cell<C,E>> valueValidator,
				Cell.Interpreter<C, E> valueInterpreter,
				Supplier<Border> border
		) {
			LogUtil.nullArgCheck(valueType, "valueType", Class.class);
			LogUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
			LogUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
			this.componentType = componentType;
			this.border = border;
			when(valueType, valueValidator).as(valueInterpreter);
		}

		public <T extends E> As<C,E,T> when(Class<T> valueType) {
			LogUtil.nullArgCheck(valueType, "valueType", Class.class);
			return when( valueType, cell -> true );
		}

		public <T extends E> As<C,E,T> when(
				Class<T> valueType,
				Predicate<Cell<C,T>> valueValidator
		) {
			LogUtil.nullArgCheck(valueType, "valueType", Class.class);
			LogUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
			return new As<C,E,T>() {
				@Override
				public Builder<C,E> as(Cell.Interpreter<C,T> valueInterpreter) {
					LogUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
					store(valueType, valueValidator, valueInterpreter);
					return (Builder<C, E>) Builder.this;
				}
			};
		}

		private void store(
			Class valueType,
			Predicate predicate,
			Cell.Interpreter valueInterpreter
		) {
			LogUtil.nullArgCheck(valueType, "valueType", Class.class);
			LogUtil.nullArgCheck(predicate, "predicate", Predicate.class);
			LogUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
			List<BiConsumer<Class<?>, Cell<C,?>>> found = interpreters.computeIfAbsent(valueType, k -> new ArrayList<>());
			found.add( (type, cell) -> {
				if ( predicate.test(cell) )
					valueInterpreter.interpret(cell);
			} );
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
				Class<?> type = ( value == null ? Object.class : value.getClass() );

				List<BiConsumer<Class<?>, Cell<C,?>>> interpreter = interpreters.get(type);
				if ( interpreter == null ) {
					Class<?> finalType1 = type;
					type = interpreters.keySet().stream().filter(i -> i.isAssignableFrom(finalType1) ).findFirst().orElse(Object.class);
					interpreter = interpreters.get(type);
				}
				if ( interpreter == null )
					return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				else {
					Component[] componentRef = new Component[1];
					Object[] defaultValueRef = new Object[1];
					Cell<JTable,Object> cell = new Cell<JTable,Object>() {
						@Override public JTable getComponent() {return table;}
						@Override public Object getValue() {return value;}
						@Override public boolean isSelected() {return isSelected;}
						@Override public boolean hasFocus() {return hasFocus;}
						@Override public int getRow() {return row;}
						@Override public int getColumn() {return column;}
						@Override public Component getRenderer() {return SimpleTableCellRenderer.super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);}
						@Override public void setRenderer(Component component) {componentRef[0] = component;}
						@Override public void setDefaultRenderValue(Object newValue) {defaultValueRef[0] = newValue;}
					};
					Class<?> finalType = type;
					interpreter.forEach(consumer -> consumer.accept(finalType, (Cell<C,?>)cell) );
					if ( componentRef[0] != null )
						return componentRef[0];
					else if ( defaultValueRef[0] != null )
						return super.getTableCellRendererComponent(table, defaultValueRef[0], isSelected, hasFocus, row, column);
					else
						return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				}
			}

			@Override
			public Border getBorder() {
				if ( border != null ) return border.get();
				else
					return super.getBorder();
			}
		}

		private class SimpleListCellRenderer<T> extends DefaultListCellRenderer {
			@Override
			public Component getListCellRendererComponent(
					JList list,
					Object value,
					final int row,
					boolean isSelected,
					boolean hasFocus
			) {
				Class<?> type = ( value == null ? Object.class : value.getClass() );

				List<BiConsumer<Class<?>, Cell<C,?>>> interpreter = interpreters.get(type);
				if ( interpreter == null ) {
					Class<?> finalType1 = type;
					type = interpreters.keySet().stream().filter(i -> i.isAssignableFrom(finalType1) ).findFirst().orElse(Object.class);
					interpreter = interpreters.get(type);
				}
				if ( interpreter == null )
					return super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
				else {
					Component[] componentRef = new Component[1];
					Object[] defaultValueRef = new Object[1];
					Cell<JList<T>,Object> cell = new Cell<JList<T>, Object>() {
						@Override public JList<T> getComponent() {return list;}
						@Override public Object getValue() {return value;}
						@Override public boolean isSelected() {return isSelected;}
						@Override public boolean hasFocus() {return hasFocus;}
						@Override public int getRow() {return row;}
						@Override public int getColumn() {return 0;}
						@Override public Component getRenderer() {return SimpleListCellRenderer.super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);}
						@Override public void setRenderer(Component component) {componentRef[0] = component;}
						@Override public void setDefaultRenderValue(Object newValue) {defaultValueRef[0] = newValue;}
					};
					Class<?> finalType = type;
					interpreter.forEach(consumer -> consumer.accept(finalType, (Cell<C,?>)cell) );
					if ( componentRef[0] != null )
						return componentRef[0];
					else if ( defaultValueRef[0] != null )
						return super.getListCellRendererComponent(list, defaultValueRef[0], row, isSelected, hasFocus);
					else
						return super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
				}
			}
			@Override
			public Border getBorder() {
				if ( border != null ) return border.get();
				else
					return super.getBorder();
			}
		}

		DefaultTableCellRenderer getForTable() {
			if ( JTable.class.isAssignableFrom(componentType) )
				return new SimpleTableCellRenderer();
			else
				throw new IllegalArgumentException("Renderer was set up to be used for a JTable!");
		}

		ListCellRenderer<E> getForList() {
			if ( JList.class.isAssignableFrom(componentType) || JComboBox.class.isAssignableFrom(componentType) )
				return (ListCellRenderer<E>) new SimpleListCellRenderer<Object>();
			else
				throw new IllegalArgumentException("Renderer was set up to be used for a JList or JComboBox! (not "+componentType.getSimpleName()+")");
		}

	}

}

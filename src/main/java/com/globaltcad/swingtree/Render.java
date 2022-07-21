package com.globaltcad.swingtree;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 	An API for building extensions of the {@link DefaultTableCellRenderer} in a functional style.
 */
public final class Render extends DefaultTableCellRenderer {

	/**
	 * @param borderSupplier A lambda which provides a border for rendered cells.
	 * @return The builder API allowing method chaining.
	 */
	public static Builder withBorder(Supplier<Border> borderSupplier) {
		return new Builder(borderSupplier);
	}

	/**
	 * @param border A border for rendered cells.
	 * @return The builder API allowing method chaining.
	 */
	public static Builder withBorder(Border border) {
		return new Builder(()->border);
	}

	/**
	 * 	Use this to specify which type of values should have custom rendering.
	 *
	 * @param valueType The type of cell value, for which you want custom rendering.
	 * @param <T> The type parameter of the cell value, for which you want custom rendering.
	 * @return The {@link As} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
	 */
	public static <T> As<T> when(Class<T> valueType) {
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
	public static <T> As<T> when(
			Class<T> valueType,
			Predicate<Cell<T>> valueValidator
	) {
		LogUtil.nullArgCheck(valueType, "valueType", Class.class);
		LogUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
		return new As<T>() {
			@Override
			public Builder as(Cell.Interpreter<T> valueInterpreter) {
				LogUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
				return new Builder(valueType, valueValidator, valueInterpreter);
			}
		};
	}

	private final Supplier<Border> border;
	private final Map<Class<?>, List<BiConsumer<Class<?>, Cell<?>>>> interpreterLambdas;

	private Render(Supplier<Border> border, Map<Class<?>, List<BiConsumer<Class<?>, Cell<?>>>> interpreter) {
		this.border = border;
		this.interpreterLambdas = interpreter;
	}

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

		List<BiConsumer<Class<?>, Cell<?>>> interpreter = interpreterLambdas.get(type);
		if ( interpreter == null ) {
			Class<?> finalType1 = type;
			type = interpreterLambdas.keySet().stream().filter(i -> i.isAssignableFrom(finalType1) ).findFirst().orElse(Object.class);
			interpreter = interpreterLambdas.get(type);
		}
		if ( interpreter == null )
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		else {
			Component[] componentRef = new Component[1];
			Object[] defaultValueRef = new Object[1];
			Cell<Object> cell = new Cell<Object>() {
				@Override public JTable getTable() {return table;}
				@Override public Object getValue() {return value;}
				@Override public boolean isSelected() {return isSelected;}
				@Override public boolean hasFocus() {return hasFocus;}
				@Override public int getRow() {return row;}
				@Override public int getColumn() {return column;}
				@Override public Component getRenderer() {return Render.super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);}
				@Override public void setRenderer(Component component) {componentRef[0] = component;}
				@Override public void setDefaultRenderValue(Object newValue) {defaultValueRef[0] = newValue;}
			};
			Class<?> finalType = type;
			interpreter.forEach(consumer -> consumer.accept(finalType, cell) );
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
		if ( this.border != null ) return border.get();
		else
			return super.getBorder();
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
	public interface Cell<V>
	{
		JTable  	getTable();
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
		interface Interpreter<V> {

			void interpret(Cell<V> cell);

		}
	}


	public interface As<T> {
		/**
		 * 	Specify a lambda which receives a {@link Cell} instance
		 * 	for you to customize its renderer.
		 *
		 * @param valueInterpreter A lambda which customizes the provided cell.
		 * @return The builder API allowing method chaining.
		 */
		Builder as( Cell.Interpreter<T> valueInterpreter );
	}

	/**
	 * 	A builder for building simple customized {@link javax.swing.table.TableCellRenderer}!
	 */
	public static class Builder {

		private final Supplier<Border> border;
		private final Map<Class<?>, java.util.List<BiConsumer<Class<?>, Cell<?>>>> interpreters = new HashMap<>(16);

		public Builder(
				Supplier<Border> border
		) {
			this.border = border;
		}

		public Builder(
				Class valueType,
				Predicate valueValidator,
				Cell.Interpreter valueInterpreter
		) {
			LogUtil.nullArgCheck(valueType, "valueType", Class.class);
			LogUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
			LogUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
			this.border = null;
			when(valueType, valueValidator).as(valueInterpreter);
		}

		public <T> As<T> when(Class<T> valueType) {
			LogUtil.nullArgCheck(valueType, "valueType", Class.class);
			return when( valueType, cell -> true );
		}

		public <T> As<T> when(
				Class<T> valueType,
				Predicate<Cell<T>> valueValidator
		) {
			LogUtil.nullArgCheck(valueType, "valueType", Class.class);
			LogUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
			return new As<T>() {
				@Override
				public Builder as(Cell.Interpreter<T> valueInterpreter) {
					LogUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Cell.Interpreter.class);
					store(valueType, valueValidator, valueInterpreter);
					return Builder.this;
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
			List<BiConsumer<Class<?>, Cell<?>>> found = interpreters.computeIfAbsent(valueType, k -> new ArrayList<>());
			found.add( (type, cell) -> {
				if ( predicate.test(cell) )
					valueInterpreter.interpret((Cell) cell);
			} );
		}

		public DefaultTableCellRenderer get() {
			return new Render(border, interpreters);
		}

	}

}

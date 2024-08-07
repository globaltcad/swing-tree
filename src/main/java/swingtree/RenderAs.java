package swingtree;

import org.slf4j.LoggerFactory;
import swingtree.api.Configurator;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This class models the API of the {@link CellBuilder} which allows you to
 * specify how a cell should be rendered.
 * Most likely you will want to call {@link #asText(Function)}
 * on this as most cells are rendered as simple texts.
 * An example would be a combo box containing enum values, which
 * you don't want to render as the enum name (all capital letters), but rather as a
 * more human-readable string.
 *
 * @param <C> The type of the component which is used to render the cell.
 * @param <E> The type of the value of the cell.
 * @param <T> The type of the value of the cell.
 */
public final class RenderAs<C extends JComponent, E, T extends E>
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RenderAs.class);

    private final CellBuilder<C, E> _builder;
    private final Class<T>                      _valueType;
    private final Predicate<CellConf<C, T>> _valueValidator;


    RenderAs(CellBuilder<C, E> builder, Class<T> valueType, Predicate<CellConf<C, T>> valueValidator) {
        _builder        = builder;
        _valueType      = valueType;
        _valueValidator = valueValidator;
    }

    /**
     * Specify a lambda which receives a {@link CellConf} instance
     * for you to customize its renderer.
     * This is the most generic way to customize the rendering of a cell,
     * as you can choose between vastly different ways of rendering:
     * <pre>{@code
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
     *    }</pre>
     *
     * @param valueInterpreter A lambda which customizes the provided cell.
     * @return The builder API allowing method chaining.
     */
    public CellBuilder<C, E> as(Configurator<CellConf<C, T>> valueInterpreter ) {
        NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Configurator.class);
        _builder._store(_valueType, _valueValidator, valueInterpreter);
        return _builder;
    }

    /**
     * Specify a lambda which receives a {@link CellConf} instance
     * and return a {@link Component} which is then used to render the cell.
     * <pre>{@code
     * 		.when( MyEnum.class )
     * 		.asComponent( cell -> new JLabel( "Hello World" ) )
     *    }</pre>
     *
     * @param renderer A function which returns a {@link Component} which is then used to render the cell.
     * @return The builder API allowing method chaining.
     */
    public CellBuilder<C, E> asComponent(Function<CellConf<C ,T>, Component> renderer ) {
        return this.as( cell -> cell.view(renderer.apply(cell)) );
    }

    /**
     * Specify a lambda which receives a {@link CellConf} instance
     * and return a {@link String} which is then used to render the cell.
     * <pre>{@code
     * 		.when( MyEnum.class )
     * 		.asText( cell -> "Hello World" )
     *    }</pre>
     *
     * @param renderer A function which returns a {@link String} which is then used to render the cell.
     * @return The builder API allowing method chaining.
     */
    public CellBuilder<C, E> asText(Function<CellConf<C ,T>, String> renderer ) {
        return this.as(CellBuilder._createDefaultTextRenderer(renderer));
    }

    /**
     * Specify a lambda which receives a {@link CellConf} instance as well as a {@link Graphics} instance
     * and then renders the cell.
     * <pre>{@code
     *  	.when( MyEnum.class )
     *  	.render( (cell, g) -> {
     *  		// draw something
     *  		g.drawString( "Hello World", 0, 0 );
     *    })
     * }</pre>
     *
     * @param renderer A function which receives a {@link CellConf} instance as well as a {@link Graphics} instance and then renders the cell.
     * @return The builder API allowing method chaining.
     */
    public CellBuilder<C, E> render(BiConsumer<CellConf<C ,T>, Graphics2D> renderer ) {
        return this.as( cell -> cell.view(new JComponent( ){
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

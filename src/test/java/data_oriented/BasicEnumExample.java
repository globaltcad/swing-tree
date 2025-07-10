package data_oriented;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;

import javax.swing.*;
import java.time.LocalDateTime;

import static swingtree.UI.*;

/**
 *  This class is a basic SwingTree example demonstrating how to bind a
 *  SwingTree UI-declaration to a data oriented view model. More specifically,
 *  one of the members of the {@link Train} view model, has a {@link Model}
 *  enum, which you can bind to multiple radio boxes dynamically.<br>
 * <p>
 *  The view produces by this class starts off with a label "Speed:" in the
 *  top left corner of the window, followed by a text field right next to it
 *  spanning all the way to the right end of the window. The text field is editable
 *  and has the number "350.0" already inside of it.
 * <p>
 *  Below the "Speed:" label is a "Built:" label followed by yet another label
 *  telling us the date of when the train was built.
 *<p>
 *  Below this 4 by 4 grid of components, we see three left aligned radio boxes
 *  listed vertically and spanning an entire layout grid row each.
 */
public class BasicEnumExample {

    @With @Getter @Accessors(fluent = true)  @AllArgsConstructor @EqualsAndHashCode
    static class Train {final Double speed; LocalDateTime builtDate; final Model model;}
    enum Model {
        SHINKANSEN, BRIGHTLINE, ICE
    }


    @Getter static class PersonView extends Panel {
        public PersonView( Var<Train> person ) {
            Var<Double>        speed = person.zoomTo(Train::speed, Train::withSpeed);
            Var<LocalDateTime> date  = person.zoomTo(Train::builtDate,  Train::withBuiltDate);
            Var<Model>         model = person.zoomTo(Train::model,  Train::withModel);
            ButtonGroup group = new ButtonGroup();
            of(this).withLayout(FILL.and(WRAP(2)))
            .add(SHRINK, label("Speed:"))
            .add(GROW, numericTextField(speed))
            .add(SHRINK, label("Built: "), label(date.viewAsString()))
            .add(WRAP.and(SPAN),
                radioButton("Shinkansen", Model.SHINKANSEN, model).withButtonGroup(group),
                radioButton("Brightline", Model.BRIGHTLINE, model).withButtonGroup(group),
                radioButton("ICE", Model.ICE, model).withButtonGroup(group)
            );
        }
    }

    public static void main(String... args) {
        Var<Train> person = Var.of(new Train(350d, LocalDateTime.now(), Model.SHINKANSEN));
        UI.show(f->new PersonView(person));
        Viewable.cast(person).onChange(From.ALL, it -> {
            System.out.println(it.currentValue());
        });
    }

}

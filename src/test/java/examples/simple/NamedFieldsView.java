package examples.simple;

import examples.FancyTextField;
import sprouts.Event;
import sprouts.Var;
import swingtree.UI;
import swingtree.animation.LifeTime;
import swingtree.api.IconDeclaration;
import swingtree.style.SvgIcon;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NamedFieldsView extends JPanel {

    private final Var<Boolean> toggle = Var.of(false);
    private final Event highlight = Event.create();

    public NamedFieldsView()
    {
        DesignConstants design = new DesignConstants();

        javax.swing.JComboBox<String> comboboxDefinition = new javax.swing.JComboBox<>();
        comboboxDefinition.setEditable(true);
        comboboxDefinition.setInheritsPopupMenu(true);

        IconDeclaration funnel  = ()->"img/funnel.svg";
        IconDeclaration notes   = ()->"img/two-16th-notes.svg";
        IconDeclaration tree    = ()->"img/curvey-bubble-tree.svg";
        IconDeclaration svgT    = ()->"img/dandelion.svg";
        IconDeclaration pngT    = ()->"img/seed.png";

        Map<String, List<String>> data = new LinkedHashMap<>();
        data.put("A", Stream.of("A1", "A2", "A3").collect(Collectors.toList()));
        data.put("B", Stream.of("B1", "B2", "B3").collect(Collectors.toList()));
        data.put("C", Stream.of("C1", "C2", "C3").collect(Collectors.toList()));

        UI.of(this).withLayout("fill, wrap 2", "[grow][grow]")
        .add(
            UI.panel("fill, wrap 1", "[grow]")
            .add("center",
                UI.box("fill, wrap 5")
                .add("shrinkx", UI.label("Max dim sizing:"))
                .add("growx, pushx",
                    UI.toggleButton(funnel, UI.FitComponent.MAX_DIM).withMinSize(42, 36)
                )
                .add("growx, pushx",
                    UI.toggleButton(notes, UI.FitComponent.MAX_DIM).withMinSize(42, 36)
                )
                .add("growx, pushx",
                    UI.toggleButton(tree, UI.FitComponent.MAX_DIM).withMinSize(42, 36)
                )
                .add("growx, pushx",
                    UI.icon(funnel).withMinSize(19, 19)
                )
                .add("span, growx, pushx",
                    UI.separator(UI.Align.HORIZONTAL)
                )
                .add("shrinkx", UI.label("Min dim sizing:"))
                .add("growx, pushx",
                    UI.toggleButton(funnel, UI.FitComponent.MIN_DIM).withMinSize(42, 36)
                )
                .add("growx, pushx",
                    UI.toggleButton(notes, UI.FitComponent.MIN_DIM).withMinSize(42, 36)
                )
                .add("growx, pushx",
                    UI.toggleButton(tree, UI.FitComponent.MIN_DIM).withMinSize(42, 36)
                )
                .add("growx, pushx",
                    UI.icon(funnel).withMinSize(19, 19)
                )
            )
            .add("span, growx, pushx",
                UI.separator(UI.Align.HORIZONTAL).withMinHeight(18)
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Enclosing Width:").peek(design::fitLeft))
                .add("growx, pushx",          design.fitRight(new FancyTextField()))
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Definition:").peek(design::fitLeft))
                .add("growx, pushx",          design.fitRight(comboboxDefinition))
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Enclosing Direction:").peek(design::fitLeft))
                .add("growx, pushx",          design.fitRight(new JComboBox<>()))
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Menu Item:").peek(design::fitLeft))
                .add("growx, pushx",
                    UI.menuItem("<- The T is displayed here!", svgT.find(SvgIcon.class).map(i->i.withIconSize(16,16)).get())
                )
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Menu Item:").peek(design::fitLeft))
                .add("growx, pushx",
                    UI.menuItem("<- The T is displayed here!", pngT.withSize(16,16).find().get())
                )
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Menu Item:").peek(design::fitLeft))
                .add("growx, pushx",
                    UI.menuItem("<- The icon is displayed here!", funnel.find(SvgIcon.class).map(i->i.withIconSize(18,18)).get())
                )
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Menu Item:").peek(design::fitLeft))
                .add("growx, pushx",
                    UI.menuItem("The icon is displayed here! ->", funnel.find(SvgIcon.class).map(i->i.withIconSize(18,18)).get())
                    .withStyle( it -> it.orientation(UI.ComponentOrientation.RIGHT_TO_LEFT) )
                )
            )
            .add(
                UI.box("fill")
                .add("alignx right, shrinkx", UI.label("Menu Item:").peek(design::fitLeft))
                .add("growx, pushx",
                    UI.menuItem("<- The icon is displayed here!", funnel.find(SvgIcon.class).map(i->i.withIconSize(180,180)).get())
                    .withStyle( it -> it.orientation(UI.ComponentOrientation.LEFT_TO_RIGHT) )
                )
            )
        )
        .add(
            UI.scrollPane().withMaxWidth(300)
            .add(
                UI.table(UI.MapData.EDITABLE,()->{
                    return data;
                })
                .withCells( it -> it
                    .when(Object.class).as( cell -> cell
                        .updateView( comp -> comp
                        .updateIf(JLabel.class, r -> null )
                        .updateIf(JTextField.class, tf -> {
                            tf.setText(cell.entryAsString() );
                            return tf;
                        })
                        .update( r -> {
                            r.setBackground(cell.hasFocus() ? Color.YELLOW : Color.WHITE);
                            return r;
                        })
                        .orGetUi(()->
                           UI.textField()
                           .withMinHeight(30)
                        ))
                    )
                )
            )
        )
        .add("growx, pushx, span",
            UI.panel("fill")
            .add(
                UI.toggleButton("Animate").onClick( e -> {
                    toggle.set(!toggle.get());
                })
            )
            .add("growx, pushx",
                UI.label("Hello!")
                .withTransitionalStyle(toggle, LifeTime.of(1, TimeUnit.SECONDS), (state, conf)->conf
                    .border((3*state.progress()), Color.BLACK)
                    .marginLeft( (int)(conf.component().getWidth()*state.progress()/2) )
                    .marginRight( (int)(conf.component().getWidth()*state.regress()/2) )
                )
            )
        )
        .add(
            UI.comboBox(DayOfWeek.values()).isEditableIf(true)
            .withCells( it -> it.when(DayOfWeek.class).as(cell -> cell
                    .updateView( comp -> comp
                        .orGet(JTextField::new)
                        .updateIf(cell.isEditing(), v -> {
                            JTextField tf = new JTextField();
                            tf.setBackground(Color.YELLOW);
                            tf.setText(cell.entryAsString());
                            return tf;
                        })
                    )
                )
            )
        )
        .add("growx, pushx, span",
            UI.panel("fill")
            .add("center",
                UI.label("Hello!")
                .onMouseEnter( it -> highlight.fire() )
                .withTransitoryStyle(highlight, LifeTime.of(1, TimeUnit.SECONDS), (state, conf)->conf
                    .shadowSpreadRadius(-10 + 20 * state.cycle())
                    .shadowColor(Color.RED)
                    .shadowBlurRadius( 6 * state.cycle() )
                    .padding( 10 * state.cycle() )
                    .margin( 20 * state.cycle() )
                    .borderRadius(16)
                )
            )
        );

    }

    public static void main(String... args) {
        // First we set nimbus as the look and feel.
        // This is not necessary, but it looks better.
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ( "Nimbus".equals(info.getName()) ) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        UI.show(f->new NamedFieldsView());
    }


}

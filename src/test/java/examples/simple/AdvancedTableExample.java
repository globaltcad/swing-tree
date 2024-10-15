package examples.simple;

import swingtree.UI;
import swingtree.api.IconDeclaration;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdvancedTableExample extends JPanel {

    public AdvancedTableExample()
    {
        IconDeclaration pngT = ()->"img/seed.png";

        Map<String, List<Object>> data = new LinkedHashMap<>();
        data.put("A", Stream.of("A1", "A2", "A3").collect(Collectors.toList()));
        data.put("B", Stream.of('X', 'Y', 'Z').collect(Collectors.toList()));
        data.put("C", Stream.of(3_000, 42.73, 0.000987124).collect(Collectors.toList()));

        UI.of(this).withLayout("fill, wrap 2", "[grow][grow]")
        .add("grow, push",
            UI.scrollPane()
            .add(
                UI.table(UI.MapData.EDITABLE,()->{
                    return data;
                })
                .withCells( it -> it
                    .when(String.class).as( cell -> cell
                        .updateView( comp -> comp
                            .updateIf(JLabel.class, r -> null )
                            .updateIf(JTextField.class, tf -> {
                                tf.setText(cell.entryAsString());
                                return tf;
                            })
                            .update( r -> {
                                r.setBackground(cell.hasFocus() ? Color.YELLOW : Color.WHITE);
                                return r;
                            })
                            .orGetUi(()->
                               UI.textField()
                               .withMinHeight(30)
                            )
                        )
                    )
                    .when(Number.class).as( cell -> {
                        // We format the number to scientific notation.
                        String formatted = String.format("%.2e", cell.entry().map(Number::doubleValue).orElse(0.0));
                        return cell.presentationEntry(formatted);
                    })
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
        UI.show(f->new AdvancedTableExample());
    }


}

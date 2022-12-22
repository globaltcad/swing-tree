package example;

import javax.swing.*;

import static swingtree.UI.*;

public class SomeSettingsView extends JPanel
{
    public SomeSettingsView(SomeSettingsViewModel vm) {
        /*
            A simple form for setting OpenGL debug options
            like depth testing, culling, etc.
        */
        of(this).withLayout(FILL.and(WRAP(1)))
        .add(GROW_X,
            panel(FILL.and(WRAP(1)))
            .add(GROW,
                panel(FILL.and(INS(0)).and(WRAP(1)))
                .add(GROW, checkBox("Type:", vm.hasType()))
                .add(GROW,
                    panel(FILL.and(INS(0,24,0,0))).add(GROW,
                        comboBox(vm.type(), SomeSettingsViewModel.Type.values())
                        .isEnabledIf(vm.somethingEnabled())
                    )
                )
            )
            .add(GROW,
                panel(FILL.and(INS(0)).and(WRAP(1)))
                .add(GROW, label("Orientation:"))
                .add(GROW,
                    panel(FILL.and(INS(0,24,0,0))).add(GROW,
                        comboBox(vm.orientation(), SomeSettingsViewModel.Orientation.values())
                    )
                )
            )
            .add(GROW, checkBox("Visible", vm.somethingVisible()))
            .add(GROW, checkBox("Flipped", vm.flipped()))
        )
        .add(GROW_X, button("Apply").onClick(it->vm.apply()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new SomeSettingsView(new SomeSettingsViewModel()));
            frame.pack();
            frame.setVisible(true);
        });
    }

}

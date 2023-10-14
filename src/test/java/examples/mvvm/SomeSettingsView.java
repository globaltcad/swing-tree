package examples.mvvm;

import swingtree.UI;


import javax.swing.*;
import java.awt.Color;
import java.awt.Insets;

import static swingtree.UI.*;

/**
 *  This little example UI demonstrates how UI components can easily be placed vertically
 *  and with some slight indentation for some components, to indicate a certain grouping
 *  which is especially useful for settings dialogs.
 */
public class SomeSettingsView extends Panel
{
    public SomeSettingsView(SomeSettingsViewModel vm) {
        // We use the nimbus look and feel for this example, but you can use any look and feel you want.
        // We set the "ToggleButton.contentMargins":
        UIManager.put("ToggleButton.contentMargins", new Insets(5,5,5,5));
        setupNimbusLookAndFeel();
        of(this).withLayout(FILL.and(WRAP(1)))
        .add(label("Use this to configure something..."))
        .add(GROW_X,
            panel(FILL.and(WRAP(1))).withStyle( it -> it.borderRadius(12) )
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
        .add(GROW_X,
            panel(FILL.and(WRAP(4))).withStyle( it -> it.borderRadius(24) )
            .add(SHRINK, label("Speed:"))
            .add(GROW.and(PUSH),
                numericTextField(vm.speed(), vm.speedIsValid()).id("speed-text-field")
                .withBackground(
                    vm.speedIsValid().viewAs(Color.class, it -> it ? Color.WHITE : Color.RED )
                )
            )
            .add(SHRINK.and(ALIGN_LEFT),label("m/s"))
            .add(GROW.and(ALIGN_RIGHT),
                toggleButton(20,20, UI.findIcon("img/funnel.svg").get())
            )
        )
        .add(GROW_X, button("Apply").onClick(it->vm.apply()));
    }

    public static void main(String[] args) { UI.show(new SomeSettingsView(new SomeSettingsViewModel())); }

    private void setupNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
    }
}

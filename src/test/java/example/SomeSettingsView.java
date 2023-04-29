package example;

import swingtree.UI;


import java.awt.Color;

import static swingtree.UI.*;

public class SomeSettingsView extends Panel
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
        .add(GROW_X,
            panel(FILL.and(WRAP(3)))
            .add(SHRINK, label("Speed:"))
            .add(GROW.and(PUSH),
                numericTextField(vm.speed(), vm.speedIsValid()).id("speed-text-field")
                .withBackground(
                    vm.speedIsValid().viewAs(Color.class, it -> it ? Color.WHITE : Color.RED )
                )
            )
            .add(SHRINK.and(ALIGN_LEFT),label("m/s"))
        )
        .add(GROW_X, button("Apply").onClick(it->vm.apply()));
    }

    public static void main(String[] args) { UI.show(new SomeSettingsView(new SomeSettingsViewModel())); }

}

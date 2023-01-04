package example;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.EventProcessor;
import swingtree.UI;

import javax.swing.*;

import java.util.Objects;

import static swingtree.UI.*;

public class UserRegistrationView extends JPanel
{
    public UserRegistrationView( UserRegistrationViewModel vm ) {
        FlatLightLaf.setup();
        UI.use(EventProcessor.DECOUPLED, ()->
            of(this).withLayout(FILL.and(WRAP(2)))
            .withPreferredSize(500, 300)
            .add(GROW,
                panel(FILL_X.and(WRAP(2)), "[shrink][grow]")
                .add(label("Username"))
                .add(GROW_X,
                    textField(vm.username()).isEnabledIfNot(vm.allInputsDisabled())
                    .onKeyTyped( it -> vm.validateAll() )
                )
                .add(label("Password"))
                .add(GROW_X,
                    passwordField(vm.password()).isEnabledIfNot(vm.allInputsDisabled())
                    .onKeyTyped( it -> vm.validateAll() )
                )
            )
            .add(GROW,
                panel(FILL_X.and(WRAP(2)), "[shrink][grow]")
                .add(label("Email"))
                .add(GROW_X,
                    textField(vm.email()).isEnabledIfNot(vm.allInputsDisabled())
                    .onKeyTyped( it -> vm.validateAll() )
                )
                .add(label("Gender"))
                .add(GROW_X,
                    comboBox(vm.gender()).isEnabledIfNot(vm.allInputsDisabled())
                    .withRenderer(
                        renderComboItem(UserRegistrationViewModel.Gender.class)
                        .asText( it ->
                            it.value()
                              .map(Objects::toString)
                              .map( s -> s.replace("_", " ") )
                              .orElse("")
                              .toLowerCase()
                        )
                    )
                    .onSelection( it -> vm.validateAll() )
                )
            )
            .add(GROW_X,
                 panel(FILL_X.and(WRAP(1)))
                 .add(GROW_X,
                     checkBox("I accept the terms of service!", vm.termsAccepted())
                     .isEnabledIfNot(vm.allInputsDisabled())
                     .onClick( it -> vm.validateAll() )
                 )
                 .add(GROW_X,
                     button("Register").isEnabledIfNot(vm.allInputsDisabled())
                     .onClick( it -> vm.register() )
                 )
            )
            .add(GROW_X,
                panel(FILL_X.and(WRAP(1))).withBorderTitled("Feedback")
                .add(GROW_X,
                    boldLabel(
                        vm.feedback()
                        .view( f -> "<html>" + f.replace("\n", "<br>") + "</html>" )
                    )
                    .withForeground(vm.feedbackColor())
                )
            )
            .add(GROW_X.and(SPAN), button("RESET").onClick( it -> vm.reset() ))
        );
        vm.validateAll();
    }

    public static void main( String[] args ) {
        UI.show(new UserRegistrationView(new UserRegistrationViewModel()));
    }

}

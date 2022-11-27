package example;

import com.globaltcad.swingtree.EventProcessor;
import com.globaltcad.swingtree.UI;

import javax.swing.*;

import static com.globaltcad.swingtree.UI.*;

/**
 *  An example login view with property based binding to a view model (see {@link LoginViewModel}).
 */
public class LoginView extends JPanel
{
    public LoginView( LoginViewModel vm ) {
        use(EventProcessor.DECOUPLED, ()->
            of(this).withPreferredSize(350,220)
            .withLayout(FILL.and(WRAP(2)))
            .add( SHRINK, label( "Username:" ) )
            .add( GROW.and(PUSH_X), textField(vm.username()).withBackground(vm.validityColor()) )
            .add( label( "Password:" ) )
            .add( GROW.and(PUSH_X), passwordField(vm.password()).withBackground(vm.validityColor()) )
            .add( SPAN, label(vm.feedback()).withForeground(vm.feedbackColor()) )
            .add( "span",
                button( "Login" )
                .isEnabledIf(vm.loginEnabled())
                .onClick( it -> vm.login() )
            )
        );
    }

    public static void main( String[] args ) {
        UI.show(new LoginView( new LoginViewModel() ));
        while (true) { UI.processEvents(); }
    }
}

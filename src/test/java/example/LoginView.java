package example;

import com.globaltcad.swingtree.EventProcessor;
import com.globaltcad.swingtree.UI;

import javax.swing.*;

import static com.globaltcad.swingtree.UI.*;

public class LoginView extends JPanel
{
    public LoginView(LoginViewModel vm ) {
        use(EventProcessor.DECOUPLED, ()->
            of(this).withPreferredSize(350,150)
            .withLayout(FILL.and(WRAP(2)))
            .add( SHRINK, label( "Username:" ) )
            .add( GROW.and(PUSH_X), textField(vm.username()))
            .add( label( "Password:" ) )
            .add( GROW.and(PUSH_X), passwordField(vm.password()) )
            .add( SPAN, label(vm.validity()))
            .add( "span",
                button( "Login" )
                .isEnabledIf(vm.loginEnabled())
                .onClick( it -> vm.login() )
            )
        );
    }

    public static void main( String[] args ) {
        UI.showInNewFrame(new LoginView( new LoginViewModel() ));
        while (true) { UI.processEvents(); }
    }
}

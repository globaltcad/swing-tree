package example;

import com.globaltcad.swingtree.ThreadMode;
import com.globaltcad.swingtree.UI;

import javax.swing.*;

import static com.globaltcad.swingtree.UI.*;

public class FormView extends JPanel
{
    public FormView( FormViewModel vm ) {
        use(ThreadMode.COUPLED, ()->
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
        UI.showInNewFrame(new FormView( new FormViewModel() ));
        while (true) { UI.processEvents(); }
    }
}

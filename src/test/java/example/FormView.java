package example;

import javax.swing.*;
import static com.globaltcad.swingtree.UI.*;

public class FormView extends JPanel
{
    public FormView( FormViewModel vm ) {
        of(this)
        .withLayout("fill, wrap 2")
        .add( label( "Username:" ) )
        .add( "grow", textField(vm.username()))
        .add( label( "Password:" ) )
        .add( "grow", passwordField(vm.password()))
        .add( "span",
           label(vm.validity())
        )
        .add( "span",
            button( "Login" )
            .isEnabledIf(vm.loginEnabled())
            .onClick( it -> vm.login() )
        );
    }

    public static void main( String[] args ) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.add( new FormView( new FormViewModel() ) );
        frame.pack();
        frame.setVisible( true );
    }

}

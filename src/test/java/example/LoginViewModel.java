package example;

import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import java.awt.*;

/**
 *  An example view model for a simple login form view (see {@link LoginView}).
 */
public class LoginViewModel
{
    private final Var<String> username = Var.of( "" ).withAction( v -> validate() );
    private final Var<String> password = Var.of( "" ).withAction( v -> validate() );
    private final Var<String> feedback = Var.of( "" );
    private final Var<Boolean> loginEnabled = Var.of( false );
    private final Var<Color> feedbackColor = Var.of( Color.BLACK );
    private final Var<Color> validityColor = Var.of( Color.WHITE );

    private Form finalForm = null;

    public Var<String> username() { return username; }

    public Var<String> password() { return password; }

    public Val<String> feedback() { return feedback; }

    public Var<Boolean> loginEnabled() { return loginEnabled; }

    public Val<Color> feedbackColor() { return feedbackColor; }

    public Val<Color> validityColor() { return validityColor; }

    public void validate() {
        if ( username.orElseThrow().isEmpty() || password.orElseThrow().isEmpty() ) {
            feedbackColor.set( Color.RED );
            // A slight red tint:
            validityColor.set( new Color( 255, 200, 200 ) );
            feedback.set( "Username and password must not be empty!" );
            loginEnabled.set( false );
            finalForm = null;
        } else {
            feedbackColor.set( Color.BLACK );
            validityColor.set( Color.WHITE );
            feedback.set( "" );
            loginEnabled.set( true );
            finalForm = new Form( username.orElseThrow(), password.orElseThrow() );
        }
    }

    public void login() {
        loginEnabled.set( false );
        feedbackColor.set( Color.BLACK );
        feedback.set("Please wait...");
        try {
            Thread.sleep(500);
            feedback.set("Please wait.");
            Thread.sleep(500);
            feedback.set("Please wait..");
            Thread.sleep(500);
            feedback.set("Please wait...");
            Thread.sleep(500);
            feedback.set("Please wait.");
            Thread.sleep(500);
            feedback.set("Please wait..");
            Thread.sleep(500);
            feedback.set("Please wait...");
            Thread.sleep(500);
            feedback.set("Please wait..");
            Thread.sleep(500);
            // A dark yellow tint:
            feedbackColor.set( new Color(0, 71, 105) );
            feedback.set("Wait! Almost done!");
            Thread.sleep(1500);
        }
        catch(Exception e) {}
        if ( finalForm != null ) {
            System.out.println( "Logging in with " + finalForm.username() + " and " + finalForm.password() );
            feedbackColor.set( new Color(42, 101, 0) );
            feedback.set("Login successful!");
        }
        else
            feedback.set("Login failed!");

        loginEnabled.set( true );
    }

}

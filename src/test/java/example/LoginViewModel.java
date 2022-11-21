package example;

import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

public class LoginViewModel
{
    private final Var<String> username = Var.of( "" ).withAction( v -> validate() );
    private final Var<String> password = Var.of( "" ).withAction( v -> validate() );
    private final Var<String> validity = Var.of( "" );
    private final Var<Boolean> loginEnabled = Var.of( false );

    private Form finalForm = null;

    public Var<String> username() { return username; }

    public Var<String> password() { return password; }

    public Val<String> validity() { return validity; }


    public Var<Boolean> loginEnabled() { return loginEnabled; }

    public void validate() {
        if ( username.orElseThrow().isEmpty() || password.orElseThrow().isEmpty() ) {
            validity.set( "Username and password must not be empty!" ).show();
            loginEnabled.set( false ).show();
            this.finalForm = null;
        } else {
            validity.set( "" ).show();
            loginEnabled.set( true ).show();
            this.finalForm = new Form( username.orElseThrow(), password.orElseThrow() );
        }
    }

    public void login() {
        validity.set("Please wait...").show();
        try {Thread.sleep(6000);}catch(Exception e) {}
        if ( finalForm != null ) {
            System.out.println( "Logging in with " + finalForm.username() + " and " + finalForm.password() );
            validity.set("Login failed!").show();
        }
        else validity.set("Login successful").show();
    }

}

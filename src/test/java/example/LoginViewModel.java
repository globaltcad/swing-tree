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
            validity.set( "Username and password must not be empty!" );
            loginEnabled.set( false );
            this.finalForm = null;
        } else {
            validity.set( "" );
            loginEnabled.set( true );
            this.finalForm = new Form( username.orElseThrow(), password.orElseThrow() );
        }
    }

    public void login() {
        loginEnabled.set( false );
        validity.set("Please wait...");
        try {
            Thread.sleep(500);
            validity.set("Please wait.");
            Thread.sleep(500);
            validity.set("Please wait..");
            Thread.sleep(500);
            validity.set("Please wait...");
            Thread.sleep(500);
            validity.set("Please wait.");
            Thread.sleep(500);
            validity.set("Please wait..");
            Thread.sleep(500);
            validity.set("Please wait...");
            Thread.sleep(500);
            validity.set("Please wait..");
            Thread.sleep(500);
            validity.set("Please wait! Almost done!");
            Thread.sleep(1000);
        }
        catch(Exception e) {}
        if ( finalForm != null ) {
            System.out.println( "Logging in with " + finalForm.username() + " and " + finalForm.password() );
            validity.set("Login failed!");
        }
        else validity.set("Login successful");
        loginEnabled.set( true );
    }

}

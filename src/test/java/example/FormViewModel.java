package example;

import com.globaltcad.swingtree.api.mvvm.Var;

public class FormViewModel
{
    private final Var<String> username = Var.of( "" ).withAct( v -> validate() );
    private final Var<String> password = Var.of( "" ).withAct( v -> validate() );
    private final Var<String> validity = Var.of( "" );
    private final Var<Boolean> loginEnabled = Var.of( false );

    private Form finalForm = null;

    public Var<String> username() { return username; }

    public Var<String> password() { return password; }

    public Var<String> validity() { return validity; }

    public Var<Boolean> loginEnabled() { return loginEnabled; }

    public void validate() {
        if ( username.get().isEmpty() || password.get().isEmpty() ) {
            validity.set( "Username and password must not be empty!" ).view();
            loginEnabled.set( false ).view();
            this.finalForm = null;
        } else {
            validity.set( "" ).view();
            loginEnabled.set( true ).view();
            this.finalForm = new Form( username.get(), password.get() );
        }
    }

    public void login() {
        if ( finalForm != null ) {
            System.out.println( "Logging in with " + finalForm.username() + " and " + finalForm.password() );
        }
    }

}

package example;

public class Form
{
    private final String username;
    private final String password;

    public Form( String username, String password ) {
        this.username = username;
        this.password = password;
    }

    public String username() { return username; }

    public String password() { return password; }

}

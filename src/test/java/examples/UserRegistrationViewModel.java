package examples;

import sprouts.Val;
import sprouts.Var;

import java.awt.*;

public class UserRegistrationViewModel
{
    public enum Gender {
        NOT_SELECTED, MALE, FEMALE, OTHER
    }

    private final Var<String> username           = Var.of("").onAct(it -> validateAll() );
    private final Var<String> password           = Var.of("").onAct(it -> validateAll() );
    private final Var<String> email              = Var.of("").onAct(it -> validateAll() );
    private final Var<Gender> gender             = Var.of(Gender.NOT_SELECTED).onAct(it -> validateAll() );
    private final Var<Boolean> termsAccepted     = Var.of(false).onAct(it -> validateAll() );
    private final Var<String> feedback           = Var.of("");
    private final Var<Color> feedbackColor       = Var.of(Color.BLACK);
    private final Var<Boolean> allInputsDisabled = Var.of(false);


    public UserRegistrationViewModel() { validateAll(); }


    public Var<String> username() { return username; }
    
    public Var<String> password() { return password; }
    
    public Var<String> email() { return email; }
    
    public Var<Gender> gender() { return gender; }
    
    public Var<Boolean> termsAccepted() { return termsAccepted; }

    public Val<String> feedback() { return feedback; }

    public Val<Color> feedbackColor() { return feedbackColor; }

    public Val<Boolean> allInputsDisabled() { return allInputsDisabled; }


    private String validatePassword() {
        if ( password.get().length() < 8 )
            return "Password must be at least 8 characters long";
        if ( !password.get().matches(".*[A-Z].*") )
            return "Password must contain at least one uppercase letter";
        return "";
    }
    
    private String validateEmail() {
        if ( !email.get().matches(".*@.*") )
            return "Email must contain an @ character";
        return "";
    }
    
    private String validateUsername() {
        if ( username.get().length() < 3 )
            return "Username must be at least 3 characters long";
        return "";
    }
    
    private String validateTerms() {
        if ( !termsAccepted.get() )
            return "You must accept the terms and conditions";
        return "";
    }
    
    private String validateGender() {
        if ( gender.is(Gender.NOT_SELECTED) )
            return "You must select a valid gender";
        return "";
    }
    
    private String generateValidationMessage() {
        String username = validateUsername();
        String password = validatePassword();
        String email    = validateEmail();
        String terms    = validateTerms();
        String gender   = validateGender();
        
        if ( username.isEmpty() && password.isEmpty() && email.isEmpty() && terms.isEmpty() && gender.isEmpty() )
            return "";
        
        return "Please fix the following errors:\n" +
                username + "\n" +
                password + "\n" +
                email + "\n" +
                terms + "\n" +
                gender;
    }
    
    public boolean validateAll() {
        String validationMessage = generateValidationMessage();
        if ( validationMessage.isEmpty() ) {
            feedback.set("All inputs are valid, feel fre to press the submit button!");
            feedbackColor.set(Color.GREEN);
            return true;
        } else {
            feedback.set(validationMessage);
            feedbackColor.set(Color.RED);
            return false;
        }
    }
    
    public void register() {
        if ( validateAll() ) {
            allInputsDisabled.set(true);
            feedbackColor.set(Color.BLACK);
            doRegistration();
            feedback.set("Registration successful!");
            feedbackColor.set(Color.GREEN);
        } else {
            allInputsDisabled.set(false);
            feedback.set("Registration failed!");
            feedbackColor.set(Color.RED);
        }
    }
    
    private void doRegistration() {
        try {
            feedback.set("...connecting to server...");
            Thread.sleep(1000);
            feedback.set("...sending data...");
            Thread.sleep(1000);
            feedback.set("...waiting for response...");
            Thread.sleep(1000);
            feedback.set("...processing response...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean successfullyRegistered() {
        return feedback.get().equals("Registration successful!");
    }
    
    public void reset() {
        username.set("");
        password.set("");
        email.set("");
        termsAccepted.set(false);
        gender.set(Gender.NOT_SELECTED);
        feedback.set("");
        validateAll();
    }
}

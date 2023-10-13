package swingtree.testing

import examples.mvvm.LoginViewModel
import examples.mvvm.UserRegistrationViewModel
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import swingtree.SwingTree
import swingtree.threading.EventProcessor

@Title("Writing Unit Tests for View Models")
@Narrative('''

    Not only is Swing-Tree a framework for building UIs, it is also a framework for
    building view models based on its built in property types. 
    This specification demonstrates how to write unit tests
    for the example view models in the test suite.

''')
@Subject([UserRegistrationViewModel, LoginViewModel])
class MVVM_Unit_Test_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }


    def 'The registration view model will display feedback about invalid inputs.'()
    {
        reportInfo """
            Note that when changing the value of a property we
            use the "act" method instead of the "set" method.
            This is because the "set" method is used to set the
            value of the property without triggering any
            validation logic. The "act" method represents
            a user action that triggers validation logic.
        """
        given : 'We create the view model.'
            var vm = new UserRegistrationViewModel()
        expect : 'Initially all the user inputs are empty.'
            vm.username().is("")
            vm.password().is("")
            vm.email().is("")
            vm.gender().is(UserRegistrationViewModel.Gender.NOT_SELECTED)
            vm.termsAccepted().is(false)
        and : 'The initial feedback on the other hand is not empty, tells the user what is missing.'
            vm.feedback().isNot("")
        and : 'It contains the expected messages!'
            vm.feedback().get().contains("Username must be at least 3 characters long")
            vm.feedback().get().contains("Password must be at least 8 characters long")
            vm.feedback().get().contains("Email must contain an @ character")
            vm.feedback().get().contains("You must select a valid gender")
            vm.feedback().get().contains("You must accept the terms and conditions")
        when : 'We set the username to a valid value.'
            vm.username().set(From.VIEW, "bob")
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("Username must be at least 3 characters long")

        when : 'We set the password to a valid value.'
            vm.password().set(From.VIEW, "Password")
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("Password must be at least 8 characters long")

        when : 'We set the email to a valid value.'
            vm.email().set(From.VIEW, "something@something.com")
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("Email must contain an @ character")

        when : 'We set the gender to a valid value.'
            vm.gender().set(From.VIEW, UserRegistrationViewModel.Gender.FEMALE)
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("You must select a valid gender")

        when : 'We set the terms accepted to a valid value.'
            vm.termsAccepted().set(From.VIEW, true)
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("You must accept the terms and conditions")

        and : 'The feedback is now empty.'
            vm.feedback().is("All inputs are valid, feel fre to press the submit button!")
    }

    def 'The register button does nothing if the inputs are not all valid.'()
    {
        given : 'We create the view model.'
            var vm = new UserRegistrationViewModel()
        when : 'We press the register button.'
            vm.register()
        then : 'The register button is disabled.'
            !vm.successfullyRegistered()
    }

}

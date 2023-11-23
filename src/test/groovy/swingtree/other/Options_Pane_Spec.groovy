package swingtree.other


import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.UI
import swingtree.dialogs.ConfirmAnswer
import swingtree.dialogs.ConfirmDialog
import swingtree.dialogs.MessageDialog
import swingtree.dialogs.OptionsDialog

import javax.swing.Icon

@Title("Options Pane")
@Narrative('''

    In raw Swing a common way to present a set of options to the user is to
    the `JOptionPane` API to display a dialog with a set of options.
    SwingTree makes this API more intuitive and declarative.
    
    This specification demonstrates the use of the `OptionsPane` API.

''')
@Subject([ConfirmAnswer, ConfirmDialog, MessageDialog, OptionsDialog])
class Options_Pane_Spec extends Specification
{
    def 'Use the `UI.ask(String,Var)` factory method to get answers from the user through a dialog.'()
    {
        reportInfo """
            This `UI.ask(String,Var)` factory method is among the ones with the most ease of use
            as it simply takes a `String` describing the question to ask the user
            and an enum based `Var` to store the answer in.
            
            The options will automatically be generated from the enum values.
        """
        given : 'We create a simple property to store the answer in.'
            var enumProperty = Var.of(ConfirmAnswer.YES)
        and : """
            We mock the `JOptionPane` API through the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a delegate to the `JOptionPane` factory methods
            which we need for mocking.
            This stuff is package private so you can ignore this little implementation detail.
        """
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We call the `ask` factory method on the `UI` API.'
            UI.ask("Do you want to continue?", enumProperty)

        then : 'The dialog summoner API should have been called with the correct arguments.'
            1 * summoner.showOptionDialog(
                    null,
                    'Do you want to continue?',
                    'Select',
                    -1,
                    3,
                    null,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'YES'
            ) >> 1
        and : 'Because the mock returns `1` the answer should be the enum value with index `1`, which is `NO`.'
            enumProperty.get() == ConfirmAnswer.NO
    }

    def 'Use the `UI.ask(String,String,Var)` factory method to get answers from the user through a dialog.'() {
        reportInfo """
            This `UI.ask(String,String,Var)` factory method is among the ones with the most ease of use
            as it simply takes a title `String`, a `String` describing the question to ask the user
            and an enum based `Var` to store the answer in.
            
            You do not need to specify the options as they will automatically be generated from 
            the enum values for you.
        """
        given : 'We create a simple property to store the answer in (and get the options from).'
            var enumProperty = Var.of(ConfirmAnswer.YES)
        and : """
            We mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `ask` method on the `UI` API.'
            UI.ask("Please select!", "Do you want to continue?", enumProperty)

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Do you want to continue?',
                    'Please select!',
                    -1,
                    3,
                    null,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'YES'
            ) >> 2
        and : 'Due to the mock returning `2` the answer should be the enum value with index `2`, namely `CANCEL`.'
            enumProperty.get() == ConfirmAnswer.CANCEL
    }

    def 'Use the `UI.ask(String,String,Icon,Var)` factory method to get answers from the user through a dialog.'() {
        reportInfo """
            This `UI.ask(String,String,Icon,Var)` factory method is among the ones with the most ease of use
            as it simply takes a title `String`, a `String` describing the question to ask the user,
            an `Icon` to display in the dialog and an enum based `Var` to store the answer in.
            
            You do not need to specify the options as they will automatically be generated from 
            the enum values for you.
        """
        given : 'We create a simple property to store the answer in (and get the options from).'
            var enumProperty = Var.of(ConfirmAnswer.YES)
        and : 'A mocked `Icon` to display in the dialog.'
            var icon = Mock(Icon)
        and : """
            For this kind of test we need to mock the `JOptionPane` API.
            And this is achieved by mocking a custom delegate. This delegate
            is the package private `swingtree.dialogs.OptionsDialogSummoner` interface,
            which is a simple delegate for some of the static `JOptionPane` methods used
            to creating dialogs in raw Swing.
            
            We this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `askOptions` method on the `UI` API.'
            UI.ask("Please select!", "Do you want to continue?", icon, enumProperty)

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Do you want to continue?',
                    'Please select!',
                    -1,
                    3,
                    icon,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'YES'
            ) >> 3
        and : 'Due to the mock returning `3` the answer should be the enum value with index `3`, namely `CLOSE`.'
            enumProperty.get() == ConfirmAnswer.CLOSE
    }

    def 'Use `UI.choice(String, Var)` for accessing the options dialog builder API.'()
    {
        reportInfo """
            The `UI.choice(String, Var)` method returns a builder object
            for configuring the options dialog.
            The builder API is fluent and allows you to configure the dialog
            in a declarative way.
            
            You do not need to specify the options as they will automatically be generated from 
            the enum values for you.
        """
        given : 'We create a simple property to store the answer in (and get the options from).'
            var enumProperty = Var.of(ConfirmAnswer.YES)
        and : """
            We mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `choice` method on the `UI` API.'
            UI.choice("Is the answer to life, the universe and everything really 42?", enumProperty)
                .titled("A Question for You!")
                .icon(null)
                .defaultOption(ConfirmAnswer.CLOSE)
                .show()


        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Is the answer to life, the universe and everything really 42?',
                    'A Question for You!',
                    -1,
                    3,
                    null,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'CLOSE'
            ) >> 0
        and : 'Due to the mock returning `0` the answer should be the enum value with index `0`, namely `YES`.'
            enumProperty.get() == ConfirmAnswer.YES
    }

    def 'Use `confirmation(String)` to build a simple conformation dialog returning a simple answer enum.'()
    {
        reportInfo """
            The `UI.confirmation()` method returns a builder object
            for configuring the confirmation dialog.
            The builder API is fluent and allows you to configure the dialog
            in a declarative way.
            
            You do not need to specify the options as they will automatically be generated from 
            the enum values for you.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `confirmation` method on the `UI` API.'
            var answer =
                        UI.confirmation("Have you read the terms and conditions?")
                        .titled("Confirm or Deny")
                        .icon(null)
                        .yesOption("Sure, who hasn't?")
                        .noOption("No, Sorry!")
                        .defaultOption(ConfirmAnswer.YES)
                        .show()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Have you read the terms and conditions?',
                    'Confirm or Deny',
                    1,
                    3,
                    null,
                    ['Sure, who hasn\'t?', 'No, Sorry!', 'Cancel'],
                    'Sure, who hasn\'t?'
            ) >> 1
        and : 'Due to the mock returning `1` the answer should be the enum value with index `1`, namely `NO`.'
            answer == ConfirmAnswer.NO
    }

    def 'The convenience method `UI.confirm(String,String) summons a confirm dialog right away!'()
    {
        reportInfo """
            The `UI.confirm(String,String)` method is a convenience method
            for summoning a confirmation dialog right away.
            
            You do not need to specify the options as they will automatically be generated from 
            the enum values for you.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `confirm` method on the `UI` API.'
            var answer = UI.confirm("Confirm or Deny", "Have you read the terms and conditions?")

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Have you read the terms and conditions?',
                    'Confirm or Deny',
                    1,
                    3,
                    null,
                    ['Yes', 'No', 'Cancel'],
                    'Yes'
            ) >> 1
        and : 'Due to the mock returning `1` the answer should be the enum value with index `1`, namely `NO`.'
            answer == ConfirmAnswer.NO
    }

}

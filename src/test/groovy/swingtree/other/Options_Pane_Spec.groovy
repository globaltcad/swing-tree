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
import javax.swing.JOptionPane

@Title("Summoning Simple Dialogs")
@Narrative('''

    In raw Swing a common way to present a simple message or a set of options to the user is to
    the static methods of the `JOptionPane` API to summon and display a dialog.
    SwingTree makes this API more intuitive and declarative through the `UI` API.
    
    This specification demonstrates the use of this API.

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
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We call the `ask` factory method on the `UI` API.'
            UI.ask("Do you want to continue?", enumProperty)

        then : 'The dialog summoner API should have been called with the correct arguments.'
            1 * summoner.showOptionDialog(
                    null,
                    'Do you want to continue?', // message
                    'Select', // title
                    -1,
                    3,
                    null,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'YES'
            ) >> 1
        and : 'Because the mock returns `1` the answer should be the enum value with index `1`, which is `NO`.'
            enumProperty.get() == ConfirmAnswer.NO

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
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
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `ask` method on the `UI` API.'
            UI.ask("Please select!", "Do you want to continue?", enumProperty)

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Do you want to continue?', // message
                    'Please select!', // title
                    -1,
                    3,
                    null,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'YES'
            ) >> 2
        and : 'Due to the mock returning `2` the answer should be the enum value with index `2`, namely `CANCEL`.'
            enumProperty.get() == ConfirmAnswer.CANCEL
        
        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
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
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `askOptions` method on the `UI` API.'
            UI.ask("Please select!", "Do you want to continue?", icon, enumProperty)

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Do you want to continue?', // message
                    'Please select!', // title
                    -1,
                    3,
                    icon,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'YES'
            ) >> 3
        and : 'Due to the mock returning `3` the answer should be the enum value with index `3`, namely `CLOSE`.'
            enumProperty.get() == ConfirmAnswer.CLOSE
        
        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'Use `UI.choice(String, Var)` for accessing the options dialog builder API.'()
    {
        reportInfo """
            The `UI.choice(String, Var)` method returns a builder object
            for configuring the options dialog, where the options are 
            based on all of the enum values of the provided enum type
            defined by the `Var` property.
            So you do not need to specify the options specifically,
            as they will automatically be generated from 
            the enum values for you.
            The builder API is fluent and allows you to configure the dialog
            in a declarative way through method chaining.
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
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `choice` method on the `UI` API.'
            UI.choice("Is the answer to life, the universe and everything really 42?", enumProperty)
                .titled("A Question for You!")
                .icon((Icon)null)
                .defaultOption(ConfirmAnswer.CLOSE)
                .show()


        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Is the answer to life, the universe and everything really 42?',
                    'A Question for You!', // title
                    -1,
                    3,
                    null,
                    ['YES', 'NO', 'CANCEL', 'CLOSE'],
                    'CLOSE'
            ) >> 0
        and : 'Due to the mock returning `0` the answer should be the enum value with index `0`, namely `YES`.'
            enumProperty.get() == ConfirmAnswer.YES
        
        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'Use `UI.choice(String, Enum[])` for building an options dialog for a set of enum based options.'()
    {
        reportInfo """
            The `UI.choice(String, Enum[])` method returns a builder object
            for configuring the options dialog with the provided enum based options.
            The builder API is fluent and allows you to configure the dialog
            in a declarative way.
            
            You do not need to specify the options as they will automatically be generated from 
            the enum values for you.
        """
        given : """
            We mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `choice` method on the `UI` API.'
            var answer =
                    UI.choice("What is your favorite color?", ConfirmAnswer.YES, ConfirmAnswer.NO, ConfirmAnswer.CANCEL)
                    .titled("A Question for You!")
                    .icon((Icon)null)
                    .defaultOption(ConfirmAnswer.NO)
                    .show()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'What is your favorite color?', // message
                    'A Question for You!', // title
                    -1,
                    3,
                    null,
                    ['YES', 'NO', 'CANCEL'],
                    'NO'
            ) >> 1
        and : 'Due to the mock returning `1` the answer should be the enum value with index `1`, namely `NO`.'
            answer.get() == ConfirmAnswer.NO

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'Pass a lambda to the show method of an options dialog to create unique option presentations.'()
    {
        reportInfo """
            The `UI.choice(String, Enum[])` method returns a builder object
            for configuring the options dialog.
            It exposes a fluent builder API which allows you to configure the dialog
            in a declarative way.
            When calling the show method you can pass a lambda which
            takes each option and returns a string representation of it
            which will be displayed in the dialog to the user.
        """
        given : """
            We mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `choice` method on the `UI` API.'
            var answer =
                    UI.choice("What is your favorite color?", ConfirmAnswer.YES, ConfirmAnswer.NO, ConfirmAnswer.CANCEL)
                    .titled("A Question for You!")
                    .icon((Icon)null)
                    .defaultOption(ConfirmAnswer.NO)
                    .show( o -> o.toString().toLowerCase() )

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'What is your favorite color?', // message
                    'A Question for You!', // title
                    -1,
                    3,
                    null,
                    ['yes', 'no', 'cancel'],
                    'no'
            ) >> 1
        and : 'Due to the mock returning `1` the answer should be the enum value with index `1`, namely `no`.'
            answer.get() == ConfirmAnswer.NO

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
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
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `confirmation` method on the `UI` API.'
            var answer =
                        UI.confirmation("Have you read the terms and conditions?")
                        .titled("Confirm or Deny")
                        .icon((Icon)null)
                        .yesOption("Sure, who hasn't?")
                        .noOption("No, Sorry!")
                        .defaultOption(ConfirmAnswer.YES)
                        .show()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Have you read the terms and conditions?', // message
                    'Confirm or Deny', // title
                    1,
                    3,
                    null,
                    ['Sure, who hasn\'t?', 'No, Sorry!', 'Cancel'],
                    'Sure, who hasn\'t?'
            ) >> 1
        and : 'Due to the mock returning `1` the answer should be the enum value with index `1`, namely `NO`.'
            answer == ConfirmAnswer.NO
        
        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
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
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `confirm` method on the `UI` API.'
            var answer = UI.confirm("Confirm or Deny", "Have you read the terms and conditions?")

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showOptionDialog(
                    null,
                    'Have you read the terms and conditions?', // message
                    'Confirm or Deny', // title
                    1,
                    3,
                    null,
                    ['Yes', 'No', 'Cancel'],
                    'Yes'
            ) >> 1
        and : 'Due to the mock returning `1` the answer should be the enum value with index `1`, namely `NO`.'
            answer == ConfirmAnswer.NO
        
        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'The factory method `UI.message(String)` allows for the configuration and summoning of a message dialog.'()
    {
        reportInfo """
            The `UI.message(String)` method is a factory method
            for summoning a message dialog right away.
            
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
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `message` method on the `UI` API.'
            UI.message("I hope you have a nice day!")
                .titled("Some Information")
                .showAsInfo()
        and : 'Because this is running on the EDT, we need to wait for it...'
            UI.sync()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showMessageDialog(
                    null,
                    'I hope you have a nice day!',  // message
                    'Some Information', // title
                    JOptionPane.INFORMATION_MESSAGE,
                    null
                )
        
        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'Call `UI.info(String)` to easily summon a message dialog with an information icon.'()
    {
        reportInfo """
            The `UI.info(String)` method is a factory method
            for summoning a simple message dialog right away,
            with the provided message displayed on it.
            
            This method is a void method and does not return anything.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `info` method on the `UI` API.'
            UI.info("I hope you have a nice day!")

        and : 'Because this is running on the EDT, we need to wait for it...'
            UI.sync()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showMessageDialog(
                    null,
                    'I hope you have a nice day!', // message
                    'Info', // title
                    JOptionPane.INFORMATION_MESSAGE,
                    null
                )

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'Call `UI.info(String, String)` to easily summon a titled info message dialog.'()
    {
        reportInfo """
            The `UI.info(String, String)` method is a factory method
            for summoning a simple message dialog right away,
            with the provided message displayed on it.
            
            This method is a void method and does not return anything.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `info` method on the `UI` API.'
            UI.info("Some Information", "I hope you have a nice day!")

        and : 'Because this is running on the EDT, we need to wait for it to process the request...'
            UI.sync()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showMessageDialog(
                    null,
                    'I hope you have a nice day!', // message
                    'Some Information', // title
                    JOptionPane.INFORMATION_MESSAGE,
                    null
                )

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'Invoking `UI.warn(String)` summons a warning message dialog.'()
    {
        reportInfo """
            The `UI.warn(String)` method is a factory method
            for summoning a simple message dialog
            with the provided message displayed on it.
            
            This method is a void method and does not return anything.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `warn` method on the `UI` API.'
            UI.warn("I hope you have a nice day!")

        and : 'Because this is running on the EDT, we need to wait for it to process the request...'
            UI.sync()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showMessageDialog(
                    null,
                    'I hope you have a nice day!', // message
                    'Warning', // title
                    JOptionPane.WARNING_MESSAGE,
                    null
                )

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'Invoking `UI.warn(String, String)` summons a titled warning message dialog.'()
    {
        reportInfo """
            The `UI.warn(String, String)` method is a factory method
            for summoning a titled message dialog
            with the provided title and message displayed on it.
            
            This method is a void method and does not return anything.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code. 
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)
            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `warn` method on the `UI` API.'
            UI.warn("Some Warning", "I hope you have a nice day!")

        and : 'Because this is running on the EDT, we need to wait for it to process the request...'
            UI.sync()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showMessageDialog(
                    null,
                    'I hope you have a nice day!', // message
                    'Some Warning',  // title
                    JOptionPane.WARNING_MESSAGE,
                    null
                )

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'If you want to summon an error message dialog, use `UI.error(String)`.'()
    {
        reportInfo """
            The `UI.error(String)` method is a factory method
            for summoning a simple message dialog
            with the provided message displayed on it.
            
            This method is a void method and does not return anything.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            This stuff is package private so please ignore this little implementation detail
            in your own code.
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)

            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `error` method on the `UI` API.'
            UI.error("Oh no! Something went wrong!")

        and : 'Because this is running on the EDT, we need to wait for it to process the request...'
            UI.sync()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showMessageDialog(
                    null,
                    'Oh no! Something went wrong!', // message
                    'Error', // title
                    JOptionPane.ERROR_MESSAGE,
                    null
                )

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }

    def 'If you want to present a titled error message dialog, use `UI.error(String, String)`.'()
    {
        reportInfo """
            The `UI.error(String, String)` method is a factory method
            for summoning a titled message dialog
            with the provided title and message displayed on it.
            
            This method is a void method and does not return anything.
        """
        given : """
            We first mock the `JOptionPane` API through a package private delegate, 
            the `swingtree.dialogs.OptionsDialogSummoner` API.
            
            Note that this is a super simple delegate for the `JOptionPane` factory methods.
            We do this this for mocking the `JOptionPane` API in this specification.
            
            This stuff is package private so please ignore this little implementation detail
            in your own code.
        """
            var realSummoner = swingtree.dialogs.Context.summoner
            var summoner = Mock(swingtree.dialogs.OptionsDialogSummoner)

            swingtree.dialogs.Context.summoner = summoner

        when : 'We invoke the `error` method on the `UI` API.'
            UI.error("Some Error", "Oh no! Something went wrong!")

        and : 'Because this is running on the EDT, we need to wait for it to process the request...'
            UI.sync()

        then : 'The dialog summoner API is called with the correct arguments exactly once.'
            1 * summoner.showMessageDialog(
                    null,
                    'Oh no! Something went wrong!', // message
                    'Some Error', // title
                    JOptionPane.ERROR_MESSAGE,
                    null
                )

        cleanup : 'We put back the original summoner.'
            swingtree.dialogs.Context.summoner = realSummoner
    }
}

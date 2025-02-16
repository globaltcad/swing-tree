package examples.chat.mvi;

import sprouts.Tuple;
import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.threading.EventProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static swingtree.UI.*;

/**
 * The {@code ChatView} class represents a simple proof of concept
 * chat application user interface with an intuitive and minimalistic layout,
 * designed to facilitate text-based messaging with some very
 * basic message management features.<br>
 * <b>Here a visual description of the layout:</b>
 *
 * <h2>Layout Overview</h2>
 * The interface is structured vertically, with distinct sections for displaying messages,
 * composing new messages, and interacting with individual messages.
 *
 * <h3>1. Header</h3>
 * - At the top of the application window, below the window title bar, a centered "Chat" heading is displayed
 *   in a larger font to indicate the application's purpose.
 * - The window does not have a title in the title bar.
 *
 * <h3>2. Message Display Area</h3>
 * - Directly below the header is a scrollable panel displaying sent messages.
 *   At the beginning, it contains two example messages,
 *   one saying "Hey, how are you?" and the other "Good! :)".
 * - Messages are arranged vertically in chronological order, with the most recent message at the bottom.
 * - Each message entry is a separate panel which starts with a
 *   bordered text area which spans the full width of the scrollable panel.
 * - Each message entry panel includes:
 *   - The message text (editable if the "edit" checkbox is selected) above.
 *   - Below and to the left of the entry is a timestamp of the message in the format "MM-dd | HH:mm:ss".
 *   - In the same row but to the left is a checkbox with a pencil icon (✎) to toggle editing mode.
 *   - After that is a delete button (✕) for removing the message, which is fully aligned to the right.
 *
 * <h3>3. Message Input Area</h3>
 * - Below the message display panel, there is a text area for writing new messages.
 * - Below the input text area is a "Send ➤" button, it is fully aligned to the right.
 * - Pressing the "Send" button adds the current message to the display panel with the current timestamp,
 *   then clears the input field for the next message.
 *
 * <h3>4. Layout Details</h3>
 * - The scrollable chat area, input field and title are always centered in a
 *   common scroll pane that fills the whole window.
 * - The entire chat area has a responsive width that adjusts to the window size.
 * - The layout uses vertical stacking with spacing and padding for clarity.
 * - The message display area takes up most of the window's height.
 * - The input field and send button are grouped closely together near the bottom.
 *
 * <h3>5. Look and Feel</h3>
 * - The interface is instantiated with the default
 *   metal look and feel, which provides a clean and simple appearance.
 *
 * <h3>6. Architecture</h3>
 * - The chat view is bound to a value based {@link ChatViewModel} property
 *   using the MVI architectural pattern.
 * - The view model is completely immutable and stateless,
 *   and state changes in the view are managed through a single
 *   stateful property that updates atomically.
 * - The state of individual GUI widget is bound to
 *   individual lens properties derived rom the single stateful property.
 *   When the view model changes in the main property,
 *   the GUI is automatically updated through the lens properties.
 *   And when the user interacts with the GUI, the state changes
 *   are propagated back to the main property through the lens properties.
 */
public final class ChatView extends Panel
{
    ChatView(Var<ChatViewModel> vm) {
        Var<Tuple<ChatViewModel.Message>> sentMessages = vm.zoomTo(ChatViewModel::allMessages, ChatViewModel::withAllMessages);
        Var<String> currentMessage = vm.zoomTo(ChatViewModel::currentMessage, ChatViewModel::withCurrentMessage);
        of(this).withLayout(FILL.and(INS(16)).and(WRAP(1)))
        .withPrefSize(425, 550)
        .add(GROW.and(PUSH),
            scrollPane(it->it.fitWidth(true))
            .add(
                panel().withFlowLayout().withPrefSize(750,200)
                .add(AUTO_SPAN(it->it.oversize(6).veryLarge(8).large(10).medium(12)),
                    chatListPanel(sentMessages, currentMessage)
                )
            )
        );
    }

    private static UIForAnySwing<?,?> chatListPanel(
        Var<Tuple<ChatViewModel.Message>> sentMessages,
        Var<String> messageText
    ) {
        return
            panel(FILL.and(WRAP(1)))
            .add(CENTER.and(SPAN), html("<h2>Chat</h2>"))
            .add(GROW.and(PUSH),
                 scrollPanels().withPrefHeight(350)
                 .addAll(sentMessages, (Var<ChatViewModel.Message> entry) -> {
                     Var<Boolean> isEditing = entry.zoomTo(ChatViewModel.Message::isEditing, ChatViewModel.Message::withEditing);
                     String dateMark = entry.get().sentAt().format(DateTimeFormatter.ofPattern("MM-dd | HH:mm:ss"));
                     return
                         panel(FILL)
                         .add(GROW_X.and(PUSH_X).and(WRAP).and(SPAN),
                              textArea(entry.zoomTo(ChatViewModel.Message::text, ChatViewModel.Message::withText))
                              .isEditableIf(isEditing)
                         )
                         .add(label(dateMark))
                         .add(RIGHT, checkBox("✎", isEditing))
                         .add(RIGHT,
                             button("✕").makePlain()
                             .onClick(it -> {
                                 sentMessages.update(tuple->tuple.remove(entry));
                             })
                         );
                 })
            )
            .add(GROW_X.and(PUSH_X).and(SPAN),
                panel(FILL.and(WRAP(1)))
                .add(GROW.and(PUSH),
                    textArea(messageText)
                )
                .add(RIGHT,
                    button("Send ➤").onClick(it -> {
                        sentMessages.update(tuple-> tuple.add(
                            new ChatViewModel.Message()
                                .withText(messageText.get())
                                .withSentAt(LocalDateTime.now())
                        ));
                        messageText.set("");
                    })
                )
            );
    }

    public static void main(String[] args)
    {
        Var<ChatViewModel> vm = Var.of(new ChatViewModel().withAllMessages(Tuple.of(
            new ChatViewModel.Message().withText("Hey, how are you?").withSentAt(LocalDateTime.now().minusDays(1)),
            new ChatViewModel.Message().withText("Good! :)").withSentAt(LocalDateTime.now())
        )));
        UI.show(frame -> new ChatView(vm));
        EventProcessor.DECOUPLED.join();
    }
}

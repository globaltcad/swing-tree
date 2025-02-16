package examples.chat.mvvm;

import lombok.Getter;
import lombok.experimental.Accessors;
import sprouts.Var;
import sprouts.Vars;

import java.time.LocalDateTime;

@Getter @Accessors(fluent = true)
public final class ChatViewModel
{
    private final Vars<Message> allMessages = Vars.of(Message.class);
    private final Var<String> currentMessage = Var.of("");


    @Getter @Accessors(fluent = true)
    public final static class Message
    {
        private final Var<String> text = Var.of("");
        private final LocalDateTime sentAt;
        private final Var<Boolean> isEditing = Var.of(false);

        public Message(String messageText, LocalDateTime now) {
            this.text.set(messageText);
            this.sentAt = now;
        }
    }
}

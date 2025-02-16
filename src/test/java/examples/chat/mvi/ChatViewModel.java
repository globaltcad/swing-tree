package examples.chat.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;
import sprouts.Tuple;

import java.time.LocalDateTime;
import java.util.UUID;

@With @Getter @Accessors(fluent = true)
public final class ChatViewModel
{
    private final Tuple<Message> allMessages;
    private final String currentMessage;


    public ChatViewModel() { this(Tuple.of(Message.class), ""); }

    public ChatViewModel(Tuple<Message> allMessages, String currentMessage) {
        this.allMessages = allMessages;
        this.currentMessage = currentMessage;
    }

    @With @Getter @Accessors(fluent = true) @AllArgsConstructor
    public final static class Message implements HasId<UUID>
    {
        private final UUID id;
        private final String text;
        private final LocalDateTime sentAt;
        private final boolean isEditing;

        public Message() {this(UUID.randomUUID(), "", LocalDateTime.now(), false);}
    }
}

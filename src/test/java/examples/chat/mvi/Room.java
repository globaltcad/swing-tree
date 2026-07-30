package examples.chat.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;
import sprouts.Tuple;

import java.util.Locale;
import java.util.UUID;

/**
 *  One chat room: a name, a topic, a hue, the people in it and everything that
 *  was ever said. Rooms own their messages, which is what makes switching rooms
 *  a <em>pure</em> operation in {@link ChatViewModel} — no cache to invalidate,
 *  no "current messages" field to keep in sync, just a different index.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class Room implements HasId<UUID> {

    private final UUID            id;
    private final String          name;
    private final String          topic;
    private final int             hue;
    private final Tuple<Member>   members;
    private final Tuple<Message>  messages;
    private final int             unread;

    public static Room of( String name, String topic, int hue, Tuple<Member> members ) {
        return new Room(UUID.randomUUID(), name, topic, hue, members, Tuple.of(Message.class), 0);
    }

    /** The channel name as it is shown everywhere in the UI. */
    public String tag() { return "#" + name; }

    /** Resolves an author id, never returning {@code null}. */
    public Member author( UUID authorId ) {
        for ( Member m : members )
            if ( m.id().equals(authorId) )
                return m;
        return Member.UNKNOWN;
    }

    public Room post( Message message ) {
        return withMessages(messages.add(message));
    }

    /** Matches a message against the search box: its body <i>or</i> its author. */
    public boolean matches( Message message, String lowerCaseNeedle ) {
        if ( message.textMatches(lowerCaseNeedle) )
            return true;
        Member author = author(message.authorId());
        return author.name().toLowerCase(Locale.ROOT).contains(lowerCaseNeedle)
            || author.handle().toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }

    public long onlineCount() {
        return members.stream().filter(m -> m.presence() == Presence.ONLINE).count();
    }

    /** A member who is around and is not me — the one who will answer. */
    public Member someoneElseThan( Member me ) {
        Member fallback = Member.UNKNOWN;
        for ( Member m : members ) {
            if ( m.id().equals(me.id()) )
                continue;
            if ( m.presence() == Presence.ONLINE )
                return m;
            if ( fallback == Member.UNKNOWN )
                fallback = m;
        }
        return fallback;
    }
}

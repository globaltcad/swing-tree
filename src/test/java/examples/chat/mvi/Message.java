package examples.chat.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Association;
import sprouts.HasId;
import sprouts.Tuple;
import sprouts.ValueSet;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

/**
 *  A single chat message — and the place where two of Sprouts' <b>persistent
 *  collections</b> earn their keep inside an otherwise flat value object:
 *  <ul>
 *    <li>{@link #reactions()} is an {@link Association} (an immutable
 *        {@code Map}) from emoji to how many people picked it, and</li>
 *    <li>{@link #myReactions()} is a {@link ValueSet} (an immutable
 *        {@code Set}) of the emoji <i>I</i> picked, so a chip can render itself
 *        as "mine" without a second lookup.</li>
 *  </ul>
 *  Both are values, so they compose with Lombok's {@code @With} exactly like the
 *  {@code String} next to them: {@link #toggleReaction(String)} is a pure
 *  function returning a brand-new {@code Message}, and the whole chain up to the
 *  root view model is rebuilt by structural sharing rather than mutation.
 *  <p>
 *  {@link HasId} gives every message a stable identity independent of its
 *  content. That is mandatory here: the view binds
 *  {@code addAll(Var<Tuple<Message>>, entry -> ..)}, and two messages with the
 *  same text would otherwise be indistinguishable to the row binding.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class Message implements HasId<UUID> {

    /**
     *  The reaction palette offered on every message, in display order.
     *  <p>
     *  Deliberately drawn from the <i>basic multilingual plane</i> rather than
     *  from the emoji block: Java 2D renders those from the ordinary logical
     *  fonts, so they look the same on every machine — whereas an astral-plane
     *  emoji falls back to a colour font the JDK cannot always reach, and shows
     *  up as an empty box on a lot of Linux desktops.
     */
    public static final Tuple<String> REACTIONS = Tuple.of(String.class, "★", "♥", "☺", "✿");

    private final UUID                       id;
    private final UUID                       authorId;
    private final String                     text;
    private final LocalDateTime              sentAt;
    private final boolean                    isEditing;
    private final Association<String,Integer> reactions;
    private final ValueSet<String>           myReactions;

    public Message() {
        this(
            UUID.randomUUID(), Member.UNKNOWN.id(), "",
            LocalDateTime.now(ZoneId.systemDefault()), false,
            Association.between(String.class, Integer.class),
            ValueSet.of(String.class)
        );
    }

    public static Message from( Member author, String text, LocalDateTime sentAt ) {
        return new Message().withAuthorId(author.id()).withText(text).withSentAt(sentAt);
    }

    public int reactionCount( String emoji ) {
        return reactions.get(emoji).orElse(0);
    }

    public boolean iReactedWith( String emoji ) {
        return myReactions.contains(emoji);
    }

    /**
     *  Adds or removes <i>my</i> reaction for one emoji, keeping the public
     *  tally and my private set in step. A count that falls to zero drops the
     *  key entirely, so the chip disappears rather than showing a "0".
     */
    public Message toggleReaction( String emoji ) {
        int count = reactionCount(emoji);
        if ( iReactedWith(emoji) ) {
            int next = count - 1;
            return withMyReactions(myReactions.remove(emoji))
                  .withReactions(next <= 0 ? reactions.remove(emoji) : reactions.put(emoji, next));
        }
        return withMyReactions(myReactions.add(emoji))
              .withReactions(reactions.put(emoji, count + 1));
    }

    /** True when the (already lower-cased) needle occurs in the message body. */
    public boolean textMatches( String lowerCaseNeedle ) {
        return text.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }

    public boolean isBlank() {
        return text.trim().isEmpty();
    }
}

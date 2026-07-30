package examples.chat.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Association;
import sprouts.Tuple;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 *  The single immutable value the whole {@link ChatView} is a pure function of.
 *  It imports no Swing class, has no mutable field and no listener — every user
 *  action and every incoming message is just a method returning a <b>new</b>
 *  view model, which the view's one {@code Var<ChatViewModel>} swaps in
 *  atomically.
 *
 *  <h2>Two things worth reading closely</h2>
 *
 *  <h3>1. Rooms own their messages</h3>
 *  There is no "current messages" field to keep in sync with
 *  {@link #selectedRoom()}: {@link #room()} simply indexes into
 *  {@link #rooms()}, and {@link #withRoom(Room)} writes the selected one back.
 *  Switching rooms is therefore a single {@code int}, and the entire
 *  conversation follows through the lenses in the view.
 *
 *  <h3>2. {@link #visibleMessages()} is a <em>computed</em> lens target</h3>
 *  A lens does not have to focus a field. The view zooms
 *  {@code vm.zoomTo(ChatViewModel::visibleMessages, ChatViewModel::withVisibleMessages)}
 *  onto a projection: the getter is "the messages of the selected room that
 *  survive the search box", and the wither merges edits and deletions back into
 *  the full list by {@link Message#id()}. Because the projection is computed
 *  from the root, it re-evaluates whenever the search text, the selected room
 *  <i>or</i> the messages change — one lens, three reactive inputs, zero
 *  listeners.
 *  <p>
 *  ({@link #visibleMessages()} deliberately keeps a message that is being edited
 *  on screen even when it no longer matches the filter — otherwise deleting one
 *  character mid-edit would make the row you are typing in vanish.)
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class ChatViewModel {

    /** The "no such member / no such message" id, so nothing is ever null. */
    public static final UUID NOBODY = new UUID(0, 0);

    private final Tuple<Room>   rooms;
    private final int           selectedRoom;
    private final Member        me;
    private final String        currentMessage;
    private final String        search;
    /** Who is typing right now, or {@link #NOBODY}. */
    private final UUID          typingAuthor;
    /** Which message the mouse is over, or {@link #NOBODY} — drives the hover-revealed reaction chips. */
    private final UUID          hovered;
    private final Theme         theme;
    private final Formfactor    formfactor;
    private final ComposerShape composerShape;
    /** How tall the conversation should be, in developer pixels — see {@link #withViewSize(int, int)}. */
    private final int           conversationHeight;
    private final String        status;

    /** The demo world — a populated Treehouse, so there is something to play with immediately. */
    public ChatViewModel() {
        this(Demo.rooms(), 0, Demo.ME, "", "", NOBODY, NOBODY,
             Theme.DARK, Formfactor.ROOMY, ComposerShape.ROW, 540,
             "Welcome back. Pick a room on the left, or just start typing.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The shape of the window, as ordinary data
    // ════════════════════════════════════════════════════════════════════════

    /**
     *  Folds the view's current size into the model — the only place a pixel
     *  ever enters it, written by the one {@code onResize} handler on the view.
     *  <p>
     *  Two things come out of it. {@link #formfactor()} decides what content the
     *  page has room for, and {@link #conversationHeight()} decides how tall the
     *  message list asks to be. The second is needed because a responsive flow
     *  grid gives a row the height its tallest child <em>prefers</em> — it never
     *  stretches one to fill a tall window. So "use the window's height" has to
     *  be stated, and the honest place to state it is here, as a pure function
     *  of the window, rather than as a resize listener poking at a component.
     *  <p>
     *  The height is quantised to 20-pixel steps and the method returns
     *  {@code this} when nothing meaningful changed, so dragging a window edge
     *  produces a handful of updates rather than one per pixel.
     *
     * @param width  The view's width in developer pixels.
     * @param height The view's height in developer pixels.
     * @return This model, or a new one carrying the new shape.
     */
    public ChatViewModel withViewSize( int width, int height ) {
        Formfactor shape = Formfactor.of(width, formfactor);
        // Stacked, the conversation shares the page with the rail and the
        // roster, so it may claim far less of the window than when it sits in a
        // column of its own. Either way the composer stays on screen.
        // The subtracted chrome is the header, the status line, the card's own
        // title strip, the composer and the room the "is typing" strip needs
        // when it appears — so the composer never slips below the fold.
        int available = shape.isRoomy() ? height - 315 : height - 620;
        int band      = Math.max(200, Math.min(700, available)) / 20 * 20;
        if ( shape == formfactor && band == conversationHeight )
            return this;
        return withFormfactor(shape).withConversationHeight(band);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The selected room
    // ════════════════════════════════════════════════════════════════════════

    public Room room() {
        int i = Math.max(0, Math.min(rooms.size() - 1, selectedRoom));
        return rooms.isEmpty() ? Room.of("void", "", 0, Tuple.of(Member.class)) : rooms.get(i);
    }

    /** Writes a modified version of the currently selected room back into the tuple. */
    public ChatViewModel withRoom( Room updated ) {
        if ( rooms.isEmpty() )
            return this;
        int i = Math.max(0, Math.min(rooms.size() - 1, selectedRoom));
        return withRooms(rooms.setAt(i, updated));
    }

    public ChatViewModel selectRoom( int index ) {
        if ( index < 0 || index >= rooms.size() || index == selectedRoom )
            return this;
        Room opened = rooms.get(index).withUnread(0);   // opening a room marks it read
        return withRooms(rooms.setAt(index, opened))
              .withSelectedRoom(index)
              .withStatus(opened.tag() + " · " + opened.topic());
    }

    public int totalUnread() {
        int sum = 0;
        for ( Room r : rooms )
            sum += r.unread();
        return sum;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The computed lens target: messages of the selected room, filtered
    // ════════════════════════════════════════════════════════════════════════

    private String needle() {
        return search.trim().toLowerCase(Locale.ROOT);
    }

    /** The getter half of the {@code visibleMessages} lens. */
    public Tuple<Message> visibleMessages() {
        String needle = needle();
        Room room = room();
        if ( needle.isEmpty() )
            return room.messages();
        // A message being edited stays put, or the row under the caret would
        // disappear the moment its text stops matching.
        return room.messages().retainIf(m -> m.isEditing() || room.matches(m, needle));
    }

    /**
     *  The wither half of the {@code visibleMessages} lens: fold whatever the
     *  view handed back (edited texts, a deleted row) into the room's full
     *  message list, matching by {@link Message#id()} and leaving the messages
     *  the filter had hidden completely untouched.
     */
    public ChatViewModel withVisibleMessages( Tuple<Message> shown ) {
        Room room = room();
        String needle = needle();
        if ( needle.isEmpty() )
            return withRoom(room.withMessages(shown));

        Association<UUID, Message> shownById = Association.between(UUID.class, Message.class);
        for ( Message m : shown )
            shownById = shownById.put(m.id(), m);

        Tuple<Message> merged = Tuple.of(Message.class);
        for ( Message original : room.messages() ) {
            boolean wasOnScreen = original.isEditing() || room.matches(original, needle);
            if ( !wasOnScreen ) {
                merged = merged.add(original);            // hidden ⇒ untouched
                continue;
            }
            Optional<Message> replacement = shownById.get(original.id());
            if ( replacement.isPresent() )
                merged = merged.add(replacement.get());    // possibly edited
            // absent ⇒ the user deleted it, so it is simply not carried over
        }
        // Anything genuinely new that arrived through the lens goes to the end.
        for ( Message candidate : shown )
            if ( !containsId(room.messages(), candidate.id()) )
                merged = merged.add(candidate);

        return withRoom(room.withMessages(merged));
    }

    private static boolean containsId( Tuple<Message> messages, UUID id ) {
        for ( Message m : messages )
            if ( m.id().equals(id) )
                return true;
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Business logic — every method is a pure function returning a new model
    // ════════════════════════════════════════════════════════════════════════

    public boolean canSend() {
        return !currentMessage.trim().isEmpty();
    }

    /** Posts the draft into the selected room and clears the composer. */
    public ChatViewModel send() {
        if ( !canSend() )
            return withStatus("Nothing to send yet — the composer is empty.");
        Room posted = room().post(Message.from(me, currentMessage.trim(), now()));
        return withRoom(posted)
              .withCurrentMessage("")
              .withStatus("Sent to " + posted.tag());
    }

    /** Someone else says something. Rooms you are not looking at collect an unread badge. */
    public ChatViewModel receive( int roomIndex, Member author, String text ) {
        if ( roomIndex < 0 || roomIndex >= rooms.size() )
            return this;
        Room target = rooms.get(roomIndex).post(Message.from(author, text, now()));
        if ( roomIndex != selectedRoom )
            target = target.withUnread(target.unread() + 1);
        return withRooms(rooms.setAt(roomIndex, target))
              .withTypingAuthor(NOBODY)
              .withStatus(author.firstName() + " wrote in " + target.tag());
    }

    public ChatViewModel toggleReaction( UUID messageId, String emoji ) {
        Room room = room();
        return withRoom(room.withMessages(
            room.messages().map(m -> m.id().equals(messageId) ? m.toggleReaction(emoji) : m)
        ));
    }

    public ChatViewModel startTyping( Member who ) {
        return withTypingAuthor(who.id());
    }

    public boolean isSomeoneTyping() {
        return !typingAuthor.equals(NOBODY);
    }

    /** The member currently typing, resolved against the selected room. */
    public Member typist() {
        return room().author(typingAuthor);
    }

    public ChatViewModel toggleTheme() {
        Theme next = theme.toggled();
        return withTheme(next).withStatus("Switched to the " + next.name().toLowerCase(Locale.ROOT) + " skin.");
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The demo world
    // ════════════════════════════════════════════════════════════════════════

    /** A small, opinionated cast of tree people and the rooms they hang out in. */
    private static final class Demo {

        static final Member ME     = Member.of("Maple Ash",  "@you",    Presence.ONLINE,  95, "That's you.");
        static final Member ROWAN  = Member.of("Rowan Birk", "@rowan",  Presence.ONLINE, 150, "Builds layouts that survive a tiling WM.");
        static final Member JUNO   = Member.of("Juniper Vale","@juno",  Presence.ONLINE, 285, "Paints things. Mostly gradients.");
        static final Member HAZEL  = Member.of("Hazel Thorn","@hazel",  Presence.AWAY,   200, "Away, probably compiling.");
        static final Member ASPEN  = Member.of("Aspen Quill","@aspen",  Presence.OFFLINE, 20, "Nocturnal. Reviews PRs at 3am.");
        static final Member WILLOW = Member.of("Willow Fen", "@willow", Presence.ONLINE,  45, "Keeps the bug hollow tidy.");

        static Tuple<Room> rooms() {
            LocalDateTime t = now().minusHours(3);

            Room swingTree = Room.of("swing-tree", "Declarative Swing, one builder at a time.", 150,
                                     Tuple.of(ME, ROWAN, JUNO, HAZEL, WILLOW))
                .post(Message.from(ROWAN, "Morning! I finally deleted the last GridBagLayout in the codebase ★", t))
                .post(Message.from(JUNO,  "The one with the nested panels? That thing had a minimum width of 1400px.", t.plusMinutes(2)))
                .post(Message.from(ROWAN, "That one. It is a twelve column flow grid now — the window narrows to nothing and everything just stacks.", t.plusMinutes(3)))
                .post(Message.from(ME,    "Convergent by default. That is the whole point of the library ✦", t.plusMinutes(5)))
                .post(Message.from(HAZEL, "Drag the corner and watch it fold. I could not do that in FXML without three files.", t.plusMinutes(9)));

            Room greenhouse = Room.of("the-greenhouse", "Styling, themes and suspiciously pretty pixels.", 285,
                                      Tuple.of(ME, JUNO, ASPEN, WILLOW))
                .post(Message.from(JUNO,  "New skin is up. Noise on the page, a soft glow behind the cards, nothing hard-coded.", t.plusMinutes(20)))
                .post(Message.from(ASPEN, "Does it hot swap? Or do I have to restart like an animal?", t.plusMinutes(24)))
                .post(Message.from(JUNO,  "One StyleSheet, one reconfigure() call, every component repaints. Try the moon button up there.", t.plusMinutes(25)));

            Room hollow = Room.of("bug-hollow", "Reproducers, stack traces and the occasional fix.", 8,
                                  Tuple.of(ME, WILLOW, ROWAN, ASPEN))
                .post(Message.from(WILLOW, "A label deep in a card was pinning the whole window open again.", t.plusMinutes(40)))
                .post(Message.from(ROWAN,  "wmin 0. It is always wmin 0.", t.plusMinutes(41)))
                .post(Message.from(WILLOW, "It was always wmin 0.", t.plusMinutes(41).plusSeconds(20)));

            Room offTopic = Room.of("off-topic", "Coffee, keyboards and questionable puns.", 45,
                                    Tuple.of(ME, ROWAN, JUNO, HAZEL, ASPEN, WILLOW))
                .post(Message.from(ASPEN, "Why did the Swing developer sit on the branch?", t.plusMinutes(55)))
                .post(Message.from(HAZEL, "...why", t.plusMinutes(58)))
                .post(Message.from(ASPEN, "Because that is where the tree was ✿", t.plusMinutes(59)));

            return Tuple.of(swingTree, greenhouse.withUnread(2), hollow, offTopic.withUnread(1));
        }
    }
}

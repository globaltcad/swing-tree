package examples.chat.mvi;

import java.util.Locale;

/**
 *  The other end of the conversation.
 *  <p>
 *  A chat example that never answers is a form with a list stapled to it, so the
 *  Treehouse talks back: send something and a member of the room starts typing,
 *  then replies. Every so often another room gets a message too, which is what
 *  makes the unread badges on the room rail mean something.
 *  <p>
 *  Deliberately <b>pure and deterministic</b> — same input, same reply, no
 *  randomness and no clock. It imports nothing from Swing and nothing from
 *  SwingTree, so the whole "someone answered" path can be exercised in a plain
 *  unit test. The only thing the view contributes is the delay.
 */
public final class ChatBot {

    private ChatBot() {}

    private static final String[] SMALL_TALK = {
        "Ha! Fair.",
        "Agreed — and it survives a portrait monitor, which is the real test.",
        "Push it, I'll take a look after lunch ☕",
        "That is either brilliant or cursed. Possibly both.",
        "Wait, say more about that.",
        "I ran into exactly this yesterday. Misery loves a reproducer.",
        "Nice. Does it still narrow all the way down?",
        "Adding it to the wiki before I forget."
    };

    private static final String[] ON_LAYOUT = {
        "Twelve columns and no breakpoint field anywhere. That still feels like cheating.",
        "If it stops narrowing at some arbitrary width, there is a minimum size hiding in there.",
        "Nest a grid in a grid, never in a MigLayout cell — I have the scar tissue to prove it.",
        "Give the scroll panels a preferred height or the row collapses to one line."
    };

    private static final String[] ON_STYLE = {
        "Try the noise on the page background at scale 2 — subtle, but the flat looks dead without it.",
        "Every colour in here comes from one hue angle in the model. Skins are just two functions of it.",
        "The whole chart is a generated SVG string. It re-renders on every keystroke and stays crisp.",
        "Gradients on the border-to-interior boundary. It is the little glow that sells it."
    };

    private static final String[] ON_BUGS = {
        "Reproducer or it did not happen ☺",
        "Stack trace ends in the paint cycle, so it is a style lambda reading state it should not.",
        "Fixed on main. It was a minimum width propagating up from a single label. Again.",
        "Confirmed. Adding a regression test before I touch anything."
    };

    /** Picks the room-appropriate bank and an entry inside it, deterministically. */
    public static String replyTo( Room room, String yourMessage ) {
        String[] bank = bankFor(room);
        String   text = yourMessage.trim().toLowerCase(Locale.ROOT);
        if ( text.endsWith("?") )
            return "Good question. " + bank[Math.abs(text.hashCode()) % bank.length];
        int index = Math.abs(text.hashCode() + room.messages().size()) % (bank.length + SMALL_TALK.length);
        return index < bank.length ? bank[index] : SMALL_TALK[index - bank.length];
    }

    private static String[] bankFor( Room room ) {
        switch ( room.name() ) {
            case "swing-tree":     return ON_LAYOUT;
            case "the-greenhouse": return ON_STYLE;
            case "bug-hollow":     return ON_BUGS;
            default:               return SMALL_TALK;
        }
    }

    /**
     *  Occasionally somebody says something in a room you are <em>not</em>
     *  looking at — every third message you send. Returns the index of that
     *  room, or {@code -1} when the Treehouse stays quiet.
     */
    public static int gossipRoomIndex( ChatViewModel vm ) {
        if ( vm.rooms().size() < 2 || vm.room().messages().size() % 3 != 0 )
            return -1;
        int offset = 1 + (vm.room().messages().size() / 3) % (vm.rooms().size() - 1);
        return (vm.selectedRoom() + offset) % vm.rooms().size();
    }

    /** What gets said in that other room. */
    public static String gossipIn( Room room ) {
        String[] bank = bankFor(room);
        return bank[Math.abs(room.messages().size() * 31 + room.name().hashCode()) % bank.length];
    }
}

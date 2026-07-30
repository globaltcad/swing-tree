package examples.chat.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;

import java.util.Locale;
import java.util.UUID;

/**
 *  One person in the {@code Treehouse}. An immutable value with a stable
 *  {@link #id()} — {@link HasId} is what lets SwingTree reuse the right roster
 *  row when a member changes, instead of rebuilding the list (see the skill's
 *  §5.2 note on per-item lenses).
 *  <p>
 *  Note the {@link #hue()}: the model stores an <b>angle</b>, not a
 *  {@code Color}. The actual paint is derived by {@link Theme.Palette#hue(int)},
 *  so one and the same member is a soft pastel in the light skin and a
 *  luminous neon in the dark one — without the view model knowing either skin
 *  exists.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class Member implements HasId<UUID> {

    /** Stand-in for "we don't know who wrote this", so nothing is ever null. */
    public static final Member UNKNOWN =
            new Member(new UUID(0, 0), "Someone", "@ghost", Presence.OFFLINE, 0, "");

    private final UUID     id;
    private final String   name;
    private final String   handle;
    private final Presence presence;
    private final int      hue;
    private final String   blurb;

    public static Member of( String name, String handle, Presence presence, int hue, String blurb ) {
        return new Member(UUID.randomUUID(), name, handle, presence, hue, blurb);
    }

    /** One or two letters for the avatar disc. */
    public String initials() {
        String trimmed = name.trim();
        if ( trimmed.isEmpty() )
            return "?";
        String[] parts = trimmed.split("\\s+");
        if ( parts.length == 1 )
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    public String firstName() {
        return name.split("\\s+")[0];
    }
}

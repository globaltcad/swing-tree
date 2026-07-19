package examples.scribe;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import sprouts.HasId;
import sprouts.Tuple;
import swingtree.layout.Bounds;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

/**
 *  A purely functional view model of a "starlit manuscript" — a little scribe's
 *  study in which stars are scattered across a page of text.  The whole
 *  application state lives in a single immutable record-like object: a tuple
 *  of {@link Star}s (each carrying its own position as a {@link Bounds}),
 *  the manuscript body, a {@link Mood} enum, and the id of the currently
 *  selected star.
 *  <p>
 *  Because the positions of the stars are stored in the view model, the GUI
 *  can reconstruct its entire layout from this object alone — giving the
 *  view model 100% authority over where each child component lives on screen.
 *  This is exactly what the companion {@link CelestialScribe} view takes
 *  advantage of: it binds the panel's layout manager to a
 *  {@code Val<Layout>} derived from {@link #stars()}, so that every
 *  wither-based update here results in the stars animating to their new
 *  positions and the body text flowing around them.
 */
@EqualsAndHashCode
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class CosmosViewModel {

    public enum Mood {
        DAWN,
        DUSK,
        MIDNIGHT;

        @SuppressWarnings("EnumOrdinal") // The mood cycle is defined by the declaration order on purpose.
        public Mood next() {
            Mood[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public String pretty() {
            String n = name().toLowerCase();
            return Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
    }

    private final Tuple<Star>    stars;
    private final String         manuscript;
    private final Mood           mood;
    private final @Nullable UUID selectedStarId;

    public CosmosViewModel() {
        this(
            Tuple.of(Star.class,
                new Star(UUID.randomUUID(), "Vega",     Bounds.of(300, 110,  92,  92), 0.58, 0.95),
                new Star(UUID.randomUUID(), "Altair",   Bounds.of( 50, 260,  72,  72), 0.10, 0.75),
                new Star(UUID.randomUUID(), "Polaris",  Bounds.of(410, 360, 110, 110), 0.72, 0.90),
                new Star(UUID.randomUUID(), "Sirius",   Bounds.of(210, 540,  96,  96), 0.53, 0.85),
                new Star(UUID.randomUUID(), "Arcturus", Bounds.of( 70, 740,  64,  64), 0.05, 0.70)
            ),
            DEFAULT_MANUSCRIPT,
            Mood.MIDNIGHT,
            null
        );
    }

    public CosmosViewModel addStar() {
        Random rand = new Random();
        int size = 60 + rand.nextInt(50);
        Star s = new Star(
            UUID.randomUUID(),
            randomName(rand),
            Bounds.of(40 + rand.nextInt(380), 60 + rand.nextInt(820), size, size),
            rand.nextDouble(),
            0.55 + rand.nextDouble() * 0.45
        );
        return this.withStars(stars.add(s)).withSelectedStarId(s.id());
    }

    public CosmosViewModel removeSelected() {
        if ( selectedStarId == null ) return this;
        return this
            .withStars(stars.removeIf(s -> s.id().equals(selectedStarId)))
            .withSelectedStarId(null);
    }

    public CosmosViewModel selectStar( UUID id ) {
        return this.withSelectedStarId(id);
    }

    public CosmosViewModel updateStar( UUID id, Function<Star, Star> f ) {
        for ( int i = 0; i < stars.size(); i++ ) {
            Star s = stars.get(i);
            if ( s.id().equals(id) )
                return this.withStars(stars.setAt(i, f.apply(s)));
        }
        return this;
    }

    public CosmosViewModel nextMood() {
        return this.withMood(mood.next());
    }

    public @Nullable Star selectedStar() {
        if ( selectedStarId == null ) return null;
        for ( Star s : stars )
            if ( s.id().equals(selectedStarId) ) return s;
        return null;
    }

    public CosmosViewModel withSelectedStar( @Nullable Star star ) {
        CosmosViewModel updated = this;
        if ( star == null )
            return updated.withSelectedStarId(null);
        Star oldStar = selectedStar();
        if ( oldStar != null && star != null && oldStar.id().equals(star.id()) && !Objects.equals(oldStar, star) ) {
            // If the star has changed, update the star in the stars tuple as well
            updated = updated.updateStar(oldStar.id(), s -> star);
        }
        return updated.withSelectedStarId(star.id());
    }

    private static String randomName( Random rand ) {
        String[] pool = {
            "Alphard", "Capella", "Rigel", "Bellatrix", "Deneb", "Mira",
            "Antares", "Procyon", "Regulus", "Spica", "Castor", "Pollux",
            "Fomalhaut", "Achernar", "Canopus", "Aldebaran", "Betelgeuse"
        };
        return pool[rand.nextInt(pool.length)];
    }

    // ── The little cosmic myth that flows around the stars ───────────────────

    private static final String DEFAULT_MANUSCRIPT = String.join("\n",
        "# The Celestial Scribe",
        "*a movable manuscript of lights*",
        "",
        "Long before the first quill ever kissed parchment, there were lights — " +
        "lanterns hung in the velvet between worlds, each a witness to a story " +
        "waiting to be written. The ancient scribes would wander the dark fields, " +
        "gaze up at the sparks above, and whisper the names of every flicker they " +
        "could hold inside a single breath.",
        "",
        "## On the Wandering Stars",
        "",
        "They believed each star had its own humour. Some were **kind**, stitching " +
        "the frayed seams of the night; others were *restless*, slipping between " +
        "chapters like a thought you almost wrote down. When you move a star, so " +
        "too moves the sentence that was meant to follow it — words are polite " +
        "things, and they will always step aside for a light.",
        "",
        "## An Invitation",
        "",
        "Take up the scribe's duty. Arrange the stars as you see fit; watch the ink " +
        "flow around their quiet weight like a river around stone. Add new lamps " +
        "to the firmament, dim them, name them, drag them across the parchment. " +
        "You are keeping a constellation alive by writing around it — every " +
        "movement you make becomes a tiny verse in the oldest poem there is."
    );

    @EqualsAndHashCode
    @With @Getter @Accessors(fluent = true) @AllArgsConstructor
    public static final class Star implements HasId<UUID> {
        private final UUID   id;
        private final String name;
        private final Bounds bounds;
        private final double hue;        // 0..1
        private final double brightness; // 0..1
    }
}
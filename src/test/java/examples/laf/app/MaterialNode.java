package examples.laf.app;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;
import sprouts.Tuple;

/**
 *  The store room as a tree: a {@link StoreRoom} holding one {@link Shelf} per
 *  {@link Fibre.Origin}, each holding one {@link Bolt} per fibre kept there.
 *  <p>
 *  This is the value the materials tree in {@link AtelierView} is bound to, and it
 *  is <em>derived</em> from {@link AtelierViewModel#store()} rather than stored, so
 *  a shelf that runs down while cloth ships says so in the tree without anybody
 *  telling the tree about it.
 *  <p>
 *  On Java 21 this would be a sealed interface and three records:
 *  <pre>{@code
 *  public sealed interface MaterialNode extends HasId<String> {
 *      record StoreRoom( Tuple<Shelf> shelves )                 implements MaterialNode {}
 *      record Shelf( Fibre.Origin origin, Tuple<Bolt> bolts )   implements MaterialNode {}
 *      record Bolt( Fibre fibre, int metresInStock )            implements MaterialNode {}
 *  }
 *  }</pre>
 *  The examples compile against Java 8, so the same shape is spelled out with
 *  Lombok value classes — final fields, withers, and value based equality.
 *  <p>
 *  {@link HasId#id()} is what the tree identifies a node by, which is what keeps the
 *  user's open branches and their selection alive when a shipment rewrites the whole
 *  structure. An id only has to be unique among siblings, so a fibre's own name is
 *  plenty.
 */
@SuppressWarnings("SameNameButDifferent") // <- TODO: remove this! (needed for Java 8 to compile)
public interface MaterialNode extends HasId<String>
{
    /** How this node is named in the tree. */
    String label();

    /** Builds the whole tree from the shelves of the view model. */
    static StoreRoom of( Tuple<Yarn> store ) {
        Tuple<Shelf> shelves = Tuple.of(Shelf.class);
        for ( Fibre.Origin origin : Fibre.Origin.values() ) {
            Tuple<Bolt> bolts = Tuple.of(Bolt.class);
            for ( Yarn yarn : store )
                if ( yarn.fibre().origin() == origin )
                    bolts = bolts.add(new Bolt(yarn.fibre(), yarn.metresInStock()));
            shelves = shelves.add(new Shelf(origin, bolts));
        }
        return new StoreRoom(shelves);
    }

    /** The id of the root, which every selection path begins with. */
    String ROOT_ID = "store-room";

    /**
     *  The selection path a filter names, which is how the tree's selection is driven by the
     *  very same {@code String} the menu's radio items and the store-room chips write.
     *  <p>
     *  A tree selection is a <i>position</i>, so it is a path of ids rather than a node: the
     *  empty path for no filter at all, two ids for a shelf, three for a bolt. Note that this
     *  needs no knowledge of the current shelves, because a path is pure identity.
     */
    static Tuple<String> pathFor( String filter ) {
        if ( !filter.isEmpty() ) {
            for ( Fibre.Origin origin : Fibre.Origin.values() )
                if ( origin.label().equals(filter) )
                    return Tuple.of(String.class, ROOT_ID, origin.name());
            for ( Fibre fibre : Fibre.values() )
                if ( fibre.label().equals(filter) )
                    return Tuple.of(String.class, ROOT_ID, fibre.origin().name(), fibre.name());
        }
        return Tuple.of(String.class);
    }

    /**
     *  The filter a selection path means, which is {@link #pathFor(String)} read backwards.
     *  Selecting the store room itself filters by nothing at all, which is what makes the
     *  root of the tree double as an "everything" entry.
     */
    static String filterFor( Tuple<String> path ) {
        if ( path.size() >= 2 ) {
            String id = path.last();
            for ( Fibre.Origin origin : Fibre.Origin.values() )
                if ( origin.name().equals(id) )
                    return origin.label();
            for ( Fibre fibre : Fibre.values() )
                if ( fibre.name().equals(id) )
                    return fibre.label();
        }
        return "";
    }

    /** The root: everything the atelier has to weave with. */
    @With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode
    final class StoreRoom implements MaterialNode
    {
        private final Tuple<Shelf> shelves;

        @Override public String id()     { return ROOT_ID; }
        @Override public String label()  { return "Store room"; }
    }

    /** One half of the store room, plant or animal. */
    @With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode
    final class Shelf implements MaterialNode
    {
        private final Fibre.Origin origin;
        private final Tuple<Bolt>  bolts;

        @Override public String id()     { return origin.name(); }
        @Override public String label()  { return origin.label(); }
    }

    /** What is left of one fibre. A leaf, because no rule ever gives it children. */
    @With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode
    final class Bolt implements MaterialNode
    {
        private final Fibre fibre;
        private final int   metresInStock;

        @Override public String id()     { return fibre.name(); }
        @Override public String label()  { return fibre.label(); }

        /** @return {@code true} when there is less left than a typical commission eats. */
        public boolean isLow() { return metresInStock < 40; }
    }
}

package examples.grids;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import sprouts.Tuple;

/**
 *  One numbered tile of the {@link TileWorkshopView}. The number is the position in which
 *  the tile was added, so a row of tiles shows at a glance in which order a grid fills
 *  its cells.
 */
@Getter @Accessors(fluent = true) @EqualsAndHashCode @ToString
public final class Tile
{
    private final int   number;
    private final Glaze glaze;

    private Tile( int number, Glaze glaze ) {
        this.number = number;
        this.glaze  = glaze;
    }

    public static Tile numbered( int number ) {
        return new Tile(number, Glaze.forTile(number));
    }

    /** The tiles numbered 1 to {@code count}. */
    public static Tuple<Tile> firstTiles( int count ) {
        return resized(Tuple.of(Tile.class), count);
    }

    /**
     *  Returns the given tiles with tiles removed from or added to their end, so that there
     *  are {@code count} of them. Added tiles continue the numbering of the existing ones.
     *  The change happens in a single operation on the tuple, so a bound view receives one
     *  update, however many tiles are added or removed at once.
     */
    public static Tuple<Tile> resized( Tuple<Tile> tiles, int count ) {
        int target = Math.max(0, count);
        if ( target < tiles.size() )
            return tiles.removeRange(target, tiles.size());
        if ( target == tiles.size() )
            return tiles;
        Tile[] added = new Tile[target - tiles.size()];
        for ( int i = 0; i < added.length; i++ )
            added[i] = numbered(tiles.size() + i + 1);
        return tiles.addAll(added);
    }
}

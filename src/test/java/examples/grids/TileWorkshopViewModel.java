package examples.grids;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;

/**
 *  The single immutable root of the {@link TileWorkshopView} (MVI / MVL).
 *  <p>
 *  Every tab of the workshop has its own row of tiles, so moving the slider of one tab
 *  leaves the grids of the other tabs untouched. The view reaches the number of tiles of
 *  a tab through a lens onto the size of its tuple, which adds or removes tiles at the end.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class TileWorkshopViewModel
{
    private final Tuple<Tile> declaredColumns;
    private final Tuple<Tile> emptySpace;
    private final Tuple<Tile> collapseChoices;
    private final Tuple<Tile> likeGridLayout;
    private final Tuple<Tile> openCounts;
    private final Workbench   workbench;

    public static TileWorkshopViewModel initial() {
        return new TileWorkshopViewModel(
                    Tile.firstTiles(8),
                    Tile.firstTiles(3),
                    Tile.firstTiles(3),
                    Tile.firstTiles(7),
                    Tile.firstTiles(5),
                    Workbench.initial()
                );
    }
}

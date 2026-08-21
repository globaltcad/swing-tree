package examples.laf.app;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

/**
 *  How much of one fibre is left on the shelf. The store-room rail in the
 *  materials card renders one chip per entry, and placing a commission draws its
 *  metres down — which is why the stock is model state rather than a constant on
 *  {@link Fibre}.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode
public final class Yarn
{
    private final Fibre fibre;
    private final int   metresInStock;

    /** @return {@code true} when there is less left than a typical commission eats. */
    public boolean isLow() { return metresInStock < 40; }
}

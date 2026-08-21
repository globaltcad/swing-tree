package examples.laf.app;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

import java.util.Locale;

/**
 *  A single commission in the order book: somebody wants so many metres of a
 *  particular cloth by a particular day. Everything the editor on the right of
 *  {@link AtelierView} shows is a field of this value, reached through a lens,
 *  so typing in the client field produces a new {@code Order}, a new
 *  {@code Tuple<Order>} and a new {@link AtelierViewModel} — and the table, the
 *  totals and the docket all follow without a single listener.
 *  <p>
 *  The {@link #ref()} is the workshop's own docket number and serves as the
 *  identity of the order: it is what the table's edit merge matches rows on and
 *  what a loom stores to say which commission it is running.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode
public final class Order
{
    private final String  ref;      // docket number, e.g. "2214" — the identity
    private final String  client;
    private final Fibre   fibre;
    private final Weave   weave;
    private final Finish  finish;
    private final int     metres;
    private final int     sett;     // warp threads per centimetre
    private final int     widthCm;
    private final String  due;      // as written on the docket, e.g. "12 Sep"
    private final Stage   stage;
    private final boolean rush;
    private final String  notes;

    /**
     *  The stand-in the editor binds to while nothing is selected. Its blank
     *  {@link #ref()} is what {@link #exists()} tests, and the editor disables
     *  itself rather than letting anyone type into a commission that is not
     *  there.
     *
     *  @return an order that refers to nothing
     */
    public static Order none() {
        return new Order("", "", Fibre.LINEN, Weave.PLAIN, Finish.RAW,
                         0, 18, 90, "", Stage.DRAFTED, false, "");
    }

    /** @return {@code true} for a real commission, {@code false} for {@link #none()}. */
    public boolean exists() { return !ref.isEmpty(); }

    /** @return what the commission is worth, rush surcharge included. */
    public double total() {
        return metres * fibre.pricePerMetre() * ( rush ? 1.2 : 1.0 );
    }

    /** @return the price with a euro sign, the way the table and the docket print it. */
    public String totalAsMoney() {
        return String.format(Locale.GERMANY, "€%,.2f", total());
    }

    /** @return how the cloth is described in one line: "Linen · Twill · 18 threads/cm". */
    public String cloth() {
        return fibre.label() + " · " + weave.label() + " · " + sett + " threads/cm";
    }

    /** @return the order's title as the editor and the docket head it: "#2214 · Halden & Co". */
    public String title() {
        return exists() ? "#" + ref + " · " + client : "No commission selected";
    }

    /**
     *  Tests whether this order should survive the search box. Matching is
     *  deliberately generous — a docket number, a client, a fibre or a weave all
     *  count — because that is what somebody standing at the loom actually
     *  types.
     *
     *  @param needle the trimmed, lower-cased search text
     *  @return {@code true} when the order matches, or when the needle is empty
     */
    public boolean matches( String needle ) {
        if ( needle.isEmpty() )
            return true;
        return ref.toLowerCase(Locale.ROOT).contains(needle)
            || client.toLowerCase(Locale.ROOT).contains(needle)
            || fibre.label().toLowerCase(Locale.ROOT).contains(needle)
            || weave.label().toLowerCase(Locale.ROOT).contains(needle)
            || stage.label().toLowerCase(Locale.ROOT).contains(needle);
    }
}

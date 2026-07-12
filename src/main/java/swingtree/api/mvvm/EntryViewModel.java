package swingtree.api.mvvm;

import sprouts.Var;

/**
 *  A view model for a single entry in a {@link swingtree.components.JScrollPanels},
 *  which receives its position and selection state <b>from the component</b>:
 *  whenever an entry view is (re)built, the component first writes the current
 *  position and selection flag into these two properties and then invokes the
 *  {@link ViewSupplier}, which may read them back to style the view.
 *
 *  @deprecated This contract is inherently unfriendly to both composition and
 *  thread safety, which is why it is replaced by the tuple based binding:
 *  <ul>
 *      <li>
 *          It forces an inheritance style contract onto your view models: every
 *          entry model must expose two mutable properties it usually does not
 *          care about, just to be displayable.
 *      </li>
 *      <li>
 *          It is incompatible with the decoupled threading mode (see
 *          {@link swingtree.threading.EventProcessor#DECOUPLED}), by design:
 *          the UI thread <i>writes into your view model</i> while building an
 *          entry view and the view supplier immediately reads that state back.
 *          Since your view model belongs to the application thread, this
 *          mid-construction write/read handshake cannot be handed over to the
 *          application event queue without breaking it. Change listeners on
 *          {@link #isSelected()} and {@link #position()} will consequently run
 *          on the UI thread.
 *      </li>
 *  </ul>
 *  <h2>The recommended alternative: tuple based binding</h2>
 *  Model your entries as a {@link sprouts.Var} property holding an immutable
 *  {@link sprouts.Tuple} of plain value objects which implement
 *  {@link sprouts.HasId} (so that entry views can be recycled efficiently
 *  when the tuple changes). Any state an entry view needs, <b>including its
 *  selection flag</b>, is simply data in the value object:
 *  <pre>{@code
 *    public record Entry(String id, String text, boolean selected)
 *    implements HasId<String>
 *    {
 *        public Entry withSelected( boolean selected ) {
 *            return new Entry(id, text, selected);
 *        }
 *    }
 *
 *    Var<Tuple<Entry>> entries = Var.of(Tuple.of(
 *                                    new Entry("a", "Alpha", false),
 *                                    new Entry("b", "Beta",  true )
 *                                ));
 *
 *    UI.scrollPanels()
 *    .addAll(entries, entry ->  // <- every entry is exposed as a Var<Entry> lens
 *        UI.label(entry.viewAsString( e -> (e.selected() ? "> " : "") + e.text() ))
 *        .onMouseClick( it ->
 *            // Selecting one entry and deselecting all others is a single,
 *            // atomic tuple update, executed safely on the application thread:
 *            entries.update( tuple ->
 *                tuple.map( e -> e.withSelected(e.id().equals(entry.get().id())) )
 *            )
 *        )
 *    )
 *  }</pre>
 *  This dissolves the threading problem instead of merely patching it: there is
 *  no UI owned selection state to synchronize at all. The selection is owned by
 *  the application thread like any other view model state, exclusivity ("selecting
 *  one deselects the others") is one atomic tuple update, multi-selection and
 *  "no selection" fall out naturally, and because the entries are identified
 *  through {@link sprouts.HasId#id()}, flipping a flag on an entry recycles its
 *  existing sub-view and channels the new state into it through its item property.
 *  <p>
 *  The position of an entry is intentionally absent from this pattern: you
 *  assemble the tuple, so you already know where each entry sits, and you may
 *  store an index in your value objects if a view needs to display it.
 *  <p>
 *  Note that this legacy interface remains functional in the coupled threading
 *  modes, but binding {@code EntryViewModel}s in a UI declaration which uses the
 *  {@link swingtree.threading.EventProcessor#DECOUPLED} event processor will log
 *  a loud warning, because thread safety cannot be guaranteed for it.
 */
@Deprecated
public interface EntryViewModel
{
    /**
     *  This method implies the existence of a boolean property in this view model
     *  determining whether the entry is currently selected or not.
     *
     * @return A {@link Var} instance representing the selected state of the entry.
     */
    Var<Boolean> isSelected();

    /**
     *  This method implies the existence of an integer property in this view model
     *  determining the position of the entry in the list of all entries.
     *
     * @return A {@link Var} instance representing the position of the entry.
     */
    Var<Integer> position();
}

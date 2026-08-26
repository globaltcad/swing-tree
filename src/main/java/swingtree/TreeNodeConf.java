package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Lens;
import sprouts.Tuple;
import swingtree.api.IconDeclaration;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *  Describes how the nodes of one particular type behave inside a
 *  {@link javax.swing.JTree} built through {@link UI#tree(sprouts.Var, swingtree.api.Configurator)}.
 *  You never create one of these yourself, instead you receive it inside the
 *  {@link swingtree.api.Configurator} lambda passed to
 *  {@link TreeConf#nodesOf(Class, swingtree.api.Configurator)}:
 *  <pre>{@code
 *  UI.tree(fileSystem, conf -> conf
 *      .nodesOf(Dir.class, dir -> dir
 *          .children(Dir::entries, Dir::withEntries)
 *          .text(Dir::name, Dir::withName)
 *          .icon(dir -> Icons.FOLDER)
 *      )
 *      .nodesOf(Doc.class, doc -> doc
 *          .text(Doc::name)
 *      )
 *  );
 *  }</pre>
 *  A configuration block like this is the tree's equivalent of one {@code case} in a
 *  {@code switch} over a sealed type, which is why a sum type based tree usually has
 *  exactly one {@code nodesOf(..)} block per permitted subtype.
 *  <p>
 *  <b>A getter alone is read only, a getter together with a wither is a lens and
 *  therefore two way.</b> This is the same rule the {@link sprouts.Var#zoomTo(Function, BiFunction)}
 *  method follows, and it applies to every aspect declared here: {@link #children(Function)}
 *  makes a branch the user cannot restructure, {@link #children(Function, BiFunction)} makes
 *  one they can, {@link #text(Function)} renders a label and {@link #text(Function, BiFunction)}
 *  additionally permits renaming the node in place.
 *  <p>
 *  Instances of this class are immutable values, so every method returns a new
 *  instance instead of modifying the receiver.
 *
 * @param <N> The common node type of the tree this rule belongs to.
 * @param <B> The concrete node type this rule applies to.
 */
public final class TreeNodeConf<N, B extends N>
{
    /*
        The children rule is erased so the tree machinery can apply it without knowing the
        concrete node type. What makes that sound: the tuple handed to a wither is always the
        very tuple its own getter produced, with at most one entry exchanged.
    */
    private final Class<B>                                                    _type;
    private final @Nullable Function<Object, Tuple<Object>>                   _childrenGetter;
    private final @Nullable BiFunction<Object, Tuple<Object>, Object>         _childrenWither;
    private final @Nullable Function<B, String>                               _textGetter;
    private final @Nullable BiFunction<B, String, B>                          _textWither;
    private final @Nullable Function<B, IconDeclaration>                      _iconGetter;
    private final @Nullable Function<B, String>                               _toolTipGetter;
    private final @Nullable Boolean                                           _isLeaf;

    static <N, B extends N> TreeNodeConf<N, B> of( Class<B> type ) {
        return new TreeNodeConf<>(type, null, null, null, null, null, null, null);
    }

    private TreeNodeConf(
        Class<B>                                            type,
        @Nullable Function<Object, Tuple<Object>>           childrenGetter,
        @Nullable BiFunction<Object, Tuple<Object>, Object> childrenWither,
        @Nullable Function<B, String>                       textGetter,
        @Nullable BiFunction<B, String, B>                  textWither,
        @Nullable Function<B, IconDeclaration>              iconGetter,
        @Nullable Function<B, String>                       toolTipGetter,
        @Nullable Boolean                                   isLeaf
    ) {
        _type           = Objects.requireNonNull(type);
        _childrenGetter = childrenGetter;
        _childrenWither = childrenWither;
        _textGetter     = textGetter;
        _textWither     = textWither;
        _iconGetter     = iconGetter;
        _toolTipGetter  = toolTipGetter;
        _isLeaf         = isLeaf;
    }

    /**
     *  Declares that nodes of this type are branches whose children are read from the
     *  supplied getter, and that the user may not restructure them.
     *  <pre>{@code
     *  .nodesOf(Dir.class, dir -> dir.children(Dir::entries))
     *  }</pre>
     *  Declaring a children rule is also what turns a node type into a branch: a type
     *  without one is a leaf, and a branch stays a branch even while it happens to have
     *  no children (see {@link TreeConf#leafWhenEmpty(boolean)}).
     *
     * @param getter Reads the children of a node of this type.
     * @param <C> The type of the children, which must be assignable to the tree's node type.
     * @return An updated configuration.
     */
    public <C extends N> TreeNodeConf<N, B> children( Function<B, Tuple<C>> getter ) {
        Objects.requireNonNull(getter, "getter");
        return new TreeNodeConf<>(
            _type, _erase(getter), null,
            _textGetter, _textWither, _iconGetter, _toolTipGetter, _isLeaf
        );
    }

    /**
     *  Declares that nodes of this type are branches whose children are read from the
     *  supplied getter and written back through the supplied wither, which makes the
     *  branch structure editable and gives every node below it a writable lens
     *  reaching all the way up into the root property.
     *  <pre>{@code
     *  .nodesOf(Dir.class, dir -> dir.children(Dir::entries, Dir::withEntries))
     *  }</pre>
     *
     * @param getter Reads the children of a node of this type.
     * @param wither Returns a new node of this type with the given children.
     * @param <C> The type of the children, which must be assignable to the tree's node type.
     * @return An updated configuration.
     */
    public <C extends N> TreeNodeConf<N, B> children(
        Function<B, Tuple<C>> getter,
        BiFunction<B, Tuple<C>, B> wither
    ) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(wither, "wither");
        return new TreeNodeConf<>(
            _type, _erase(getter), _erase(wither),
            _textGetter, _textWither, _iconGetter, _toolTipGetter, _isLeaf
        );
    }

    /**
     *  Declares the children of this node type through a {@link Lens}, which is
     *  equivalent to {@link #children(Function, BiFunction)} but lets you reuse a lens
     *  you already have, or write one whose focus needs logic of its own.
     *
     * @param lens Focuses the children collection of a node of this type.
     * @param <C> The type of the children, which must be assignable to the tree's node type.
     * @return An updated configuration.
     */
    public <C extends N> TreeNodeConf<N, B> children( Lens<B, Tuple<C>> lens ) {
        Objects.requireNonNull(lens, "lens");
        return children(
            node -> {
                try {
                    return lens.getter(node);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            },
            (node, children) -> {
                try {
                    return lens.wither(node, children);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        );
    }

    /**
     *  Declares the label shown for nodes of this type. Without a text rule the tree falls
     *  back to {@link Object#toString()}, which is rarely what a record should show.
     *
     * @param getter Produces the label of a node of this type.
     * @return An updated configuration.
     */
    public TreeNodeConf<N, B> text( Function<B, String> getter ) {
        Objects.requireNonNull(getter, "getter");
        return new TreeNodeConf<>(
            _type, _childrenGetter, _childrenWither,
            getter, null, _iconGetter, _toolTipGetter, _isLeaf
        );
    }

    /**
     *  Declares the label shown for nodes of this type together with a wither, which
     *  additionally permits the user to rename the node in place. The edited text is
     *  handed to the wither and the resulting node is written back into the root property.
     *  <pre>{@code
     *  .nodesOf(Dir.class, dir -> dir.text(Dir::name, Dir::withName))
     *  }</pre>
     *  Renaming additionally requires the tree to be bound to a mutable {@link sprouts.Var}
     *  and every branch above the node to declare a children wither, because that is the
     *  chain the new value has to travel back up.
     *
     * @param getter Produces the label of a node of this type.
     * @param wither Returns a new node of this type carrying the edited label.
     * @return An updated configuration.
     */
    public TreeNodeConf<N, B> text( Function<B, String> getter, BiFunction<B, String, B> wither ) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(wither, "wither");
        return new TreeNodeConf<>(
            _type, _childrenGetter, _childrenWither,
            getter, wither, _iconGetter, _toolTipGetter, _isLeaf
        );
    }

    /**
     *  Declares the icon shown next to nodes of this type. The returned
     *  {@link IconDeclaration} is resolved through the regular SwingTree icon cache, so
     *  SVG sources and HiDPI scaling work exactly as they do everywhere else.
     *
     * @param getter Produces the icon declaration of a node of this type.
     * @return An updated configuration.
     */
    public TreeNodeConf<N, B> icon( Function<B, IconDeclaration> getter ) {
        Objects.requireNonNull(getter, "getter");
        return new TreeNodeConf<>(
            _type, _childrenGetter, _childrenWither,
            _textGetter, _textWither, getter, _toolTipGetter, _isLeaf
        );
    }

    /**
     *  Declares the tool tip shown when the pointer rests on a node of this type.
     *
     * @param getter Produces the tool tip text of a node of this type.
     * @return An updated configuration.
     */
    public TreeNodeConf<N, B> toolTip( Function<B, String> getter ) {
        Objects.requireNonNull(getter, "getter");
        return new TreeNodeConf<>(
            _type, _childrenGetter, _childrenWither,
            _textGetter, _textWither, _iconGetter, getter, _isLeaf
        );
    }

    /**
     *  Overrides whether nodes of this type are leaves. By default a node type is a leaf
     *  exactly when it declares no {@code children(..)} rule, which is the answer a sum
     *  type based model wants. Override it when a node has no children <i>yet</i> but must
     *  still show a handle, because expanding it is what triggers the load:
     *  <pre>{@code
     *  .nodesOf(Pending.class, p -> p.text(Pending::label).isLeaf(false))
     *  }</pre>
     *
     * @param isLeaf True to force nodes of this type to be leaves, false to force them to be branches.
     * @return An updated configuration.
     */
    public TreeNodeConf<N, B> isLeaf( boolean isLeaf ) {
        return new TreeNodeConf<>(
            _type, _childrenGetter, _childrenWither,
            _textGetter, _textWither, _iconGetter, _toolTipGetter, isLeaf
        );
    }

    @SuppressWarnings("unchecked")
    private static <B, C> Function<Object, Tuple<Object>> _erase( Function<B, Tuple<C>> getter ) {
        return node -> (Tuple<Object>) (Tuple<?>) getter.apply((B) node);
    }

    @SuppressWarnings("unchecked")
    private static <B, C> BiFunction<Object, Tuple<Object>, Object> _erase( BiFunction<B, Tuple<C>, B> wither ) {
        return (node, children) -> wither.apply((B) node, (Tuple<C>) (Tuple<?>) children);
    }

    Class<B> type() {
        return _type;
    }

    boolean hasChildrenRule() {
        return _childrenGetter != null;
    }

    boolean isStructurallyWritable() {
        return _childrenWither != null;
    }

    Tuple<Object> childrenOf( Object node ) {
        Function<Object, Tuple<Object>> getter = _childrenGetter;
        if ( getter == null )
            return Tuple.of(Object.class);
        return getter.apply(node);
    }

    Object withChildren( Object node, Tuple<Object> children ) {
        BiFunction<Object, Tuple<Object>, Object> wither = _childrenWither;
        if ( wither == null )
            return node;
        return wither.apply(node, children);
    }

    @SuppressWarnings("unchecked")
    @Nullable String textOf( Object node ) {
        Function<B, String> getter = _textGetter;
        if ( getter == null )
            return null;
        return getter.apply((B) node);
    }

    boolean isRenamable() {
        return _textWither != null;
    }

    @SuppressWarnings("unchecked")
    Object withText( Object node, String text ) {
        BiFunction<B, String, B> wither = _textWither;
        if ( wither == null )
            return node;
        return wither.apply((B) node, text);
    }

    @SuppressWarnings("unchecked")
    @Nullable IconDeclaration iconOf( Object node ) {
        Function<B, IconDeclaration> getter = _iconGetter;
        if ( getter == null )
            return null;
        return getter.apply((B) node);
    }

    @SuppressWarnings("unchecked")
    @Nullable String toolTipOf( Object node ) {
        Function<B, String> getter = _toolTipGetter;
        if ( getter == null )
            return null;
        return getter.apply((B) node);
    }

    @Nullable Boolean declaredLeafState() {
        return _isLeaf;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "type="       + _type.getSimpleName() + ", " +
                    "children="   + ( _childrenGetter == null ? "none" : ( _childrenWither == null ? "readOnly" : "writable" ) ) + ", " +
                    "text="       + ( _textGetter == null ? "none" : ( _textWither == null ? "readOnly" : "writable" ) ) + ", " +
                    "icon="       + ( _iconGetter == null ? "none" : "declared" ) + ", " +
                    "isLeaf="     + ( _isLeaf == null ? "derived" : String.valueOf(_isLeaf) ) +
                "]";
    }
}

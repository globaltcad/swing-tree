package examples.chat.mvi;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForPanel;
import swingtree.api.IconDeclaration;
import swingtree.api.Layout;
import swingtree.layout.FlowCell;
import swingtree.layout.MigAddConstraint;
import swingtree.style.ComponentStyleDelegate;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

/**
 *  <h1>Treehouse — a chat app built with SwingTree</h1>
 *
 *  Everything on screen is a pure function of one immutable
 *  {@link ChatViewModel} held in a single {@link Var}. The view owns no state of
 *  its own: it zooms lenses into that root, binds widgets to them, and turns
 *  user gestures back into new view models. There is not a single
 *  {@code setText}, {@code repaint} or listener registration in here.
 *
 *  <h2>What it does</h2>
 *  <ul>
 *    <li>A <b>room rail</b> with generated SVG sigils, live unread badges and a
 *        per-room hue that colours everything downstream of it.</li>
 *    <li>A <b>conversation</b> of message bubbles: yours on the right, theirs on
 *        the left, each editable in place, deletable, and reactable with emoji
 *        chips that appear on hover.</li>
 *    <li>A <b>roster</b> of the people in the room, with presence, plus a
 *        <b>conversation ribbon</b> — an SVG picture of the room's recent
 *        traffic, regenerated from the model on every message.</li>
 *    <li><b>Search</b> that filters the conversation live.</li>
 *    <li>A <b>day/night skin</b> swapped at runtime through one style sheet.</li>
 *    <li>Somebody actually <b>answers</b> when you send something.</li>
 *  </ul>
 *
 *  <h2>The five SwingTree ideas it exists to show</h2>
 *  <ol>
 *    <li><b>A lens onto a computed projection.</b> {@code visibleMessages} is
 *        not a field of the view model — it is "the messages of the selected
 *        room that survive the search box", with a wither that merges edits and
 *        deletions back in by id. One lens, reacting to three different inputs,
 *        no listeners. See {@link ChatViewModel#visibleMessages()}.</li>
 *
 *    <li><b>Tuple + {@code addAll} + {@code HasId}.</b> Rooms, members and
 *        messages are all {@link Tuple}s rendered with {@code addAll}. The two
 *        editable ones bind the <i>mutable</i> overload, which hands the row a
 *        {@code Var<Message>} lens — and that is exactly why {@link Message}
 *        implements {@code HasId<UUID>}.</li>
 *
 *    <li><b>Generated SVG as a value.</b> {@link ChatArt} turns a room into SVG
 *        <i>text</i>, which is handed to a component with
 *        {@code withStyle(svgVal, (svg, it) -> it.image(img -> img.svg(svg)))}.
 *        The picture is therefore just another derived value: it re-renders
 *        crisply at any DPI whenever the data changes.</li>
 *
 *    <li><b>Composite views for styling.</b> A room chip's look depends on the
 *        room, on which room is selected <i>and</i> on the theme. Rather than
 *        chaining three stylers, the three properties are folded into one
 *        {@link ChipLook} item with
 *        {@code Viewable.of(seed, it -> it.join(..)…)} and a single
 *        {@code withStyle} paints the chip.</li>
 *
 *    <li><b>Convergence, four gears of it</b> (see below).</li>
 *  </ol>
 *
 *  <h2>Convergence</h2>
 *  Drag the window as narrow and as short as it goes; nothing is ever lost.
 *  <ul>
 *    <li><b>Gear 0</b> — {@code wmin 0} on every row and {@code withMinSize(0,0)}
 *        on every grid, so no forgotten label can pin the window open.</li>
 *    <li><b>Gear 1</b> — the page is a 12-column {@code AUTO_SPAN} flow grid of
 *        three cards, and the room rail and the roster are <i>nested</i> grids
 *        inside their cards, so their chips become a vertical list when the card
 *        is a narrow sidebar and a flowing strip when it is a wide banner. All of
 *        that is stateless: no breakpoint field, no resize listener.</li>
 *    <li><b>Gear 2</b> — the composer is bound to a {@code Val<Layout>} derived
 *        from {@link ComposerShape}, which the composer measures on <i>itself</i>.
 *        Reflowing never rebuilds, so your half-typed sentence, the caret and the
 *        selection all survive.</li>
 *    <li><b>Gear 4</b> — the header tag line, the {@code @handles} and the member
 *        blurbs step aside when the page is {@link Formfactor#COMPACT}. What
 *        disappears is redundant, never unique.</li>
 *  </ul>
 *  <b>Gear 3</b> (swapping the component tree) is deliberately <em>not</em> used:
 *  a chat window is full of things you must not destroy — a caret in a composer,
 *  a scroll position in a long conversation, a message being edited. Every shape
 *  here is reachable by reflowing, so nothing is ever rebuilt.
 */
public final class ChatView extends JPanel {

    // ════════════════════════════════════════════════════════════════════════
    //  The responsive design, written out as a table (gear 1)
    // ════════════════════════════════════════════════════════════════════════

    /*
     *  Size categories are exact fifths of a grid's *reference width*, so the
     *  three cards converge through four arrangements without a line of state:
     *
     *      OVERSIZE     3 |  5 |  4   rail · conversation · roster, ribbon roomy
     *      VERY_LARGE   3 |  6 |  3   the same three columns, tighter roster
     *      LARGE        3 |  9 | 12   roster drops below as a full-width banner
     *      MEDIUM &     12| 12 | 12   one column, scrolled: a phone
     *      narrower
     */
    private static final int PAGE_REFERENCE_WIDTH   = 1260;
    private static final int ROOMS_REFERENCE_WIDTH  = 760;
    private static final int PEOPLE_REFERENCE_WIDTH = 760;

    private static final FlowCell FULL_ROW = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(12).veryLarge(12).oversize(12) );
    private static final FlowCell ROOMS_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(3).veryLarge(3).oversize(3) );
    private static final FlowCell CHAT_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(9).veryLarge(6).oversize(5) );
    private static final FlowCell PEOPLE_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(12).veryLarge(3).oversize(4) );
    /*
     *  The chips inside the rail and the roster. Their reference width is that
     *  of the *card*, not of the page — which is the whole trick: the same span
     *  table reads "one per row" while the card is a narrow sidebar and "four
     *  per row" once the card is a full-width banner. Same widgets, no state.
     */
    private static final FlowCell CHIP_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(6).large(4).veryLarge(3).oversize(3) );

    // ════════════════════════════════════════════════════════════════════════
    //  Gear 2 — the two composer layouts
    // ════════════════════════════════════════════════════════════════════════

    /*
     *  Both variants address their children by explicit MigLayout cells, which
     *  means (a) every variant spells out a constraint for every child — the
     *  rule that costs the most debugging time when broken — and (b) the visual
     *  order is free of the order in which the children were added.
     *
     *      ROW      [☺★♥✿] [ write something…            ] [ Send ➤ ]
     *      STACK    [ write something…                               ]
     *               [☺★♥✿]                               [ Send ➤ ]
     */
    private static final Layout COMPOSER_ROW =
            Layout.mig("fill, ins 0, gap 8", "[][grow][]", "")
                  .withChildConstraints(
                      MigAddConstraint.of("cell 1 0, growx, wmin 0"),   // the text area
                      MigAddConstraint.of("cell 0 0"),                  // the emoji rail
                      MigAddConstraint.of("cell 2 0")                   // Send
                  );

    private static final Layout COMPOSER_STACK =
            Layout.mig("fill, ins 0, gap 8", "[grow][]", "")
                  .withChildConstraints(
                      MigAddConstraint.of("cell 0 0 2 1, growx, wmin 0"),
                      MigAddConstraint.of("cell 0 1"),
                      MigAddConstraint.of("cell 1 1, align right")
                  );

    // ════════════════════════════════════════════════════════════════════════

    private static final String   FONT       = "SansSerif";
    private static final Color    TRANSPARENT = new Color(0, 0, 0, 0);
    /** Neutral, translucent tints that read correctly on both skins. */
    private static final Color    EDIT_TINT  = new Color(127, 127, 127, 38);
    private static final Color    EDIT_EDGE  = new Color(127, 127, 127, 110);
    private static final Color    REACTED    = new Color(94, 200, 143, 70);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final IconDeclaration LOGO = IconDeclaration.of("img/bubble-tree.svg").withSize(30, 30);

    private final Var<ChatViewModel> vm;
    private final ChatStyle          sheet;

    // ── Lenses into the one immutable root ────────────────────────────────
    // Held as fields, not locals: a lens is observed only weakly by its parent,
    // so anything consumed by a raw subscription (or merely reused across
    // several builder methods) has to be kept alive here. See skill §9c.
    private final Var<Tuple<Room>>    rooms;
    private final Var<Tuple<Message>> visibleMessages;
    private final Var<String>         draft;
    private final Var<String>         search;
    private final Var<UUID>           hovered;
    private final Var<Theme>          theme;
    private final Var<Formfactor>     formfactor;
    private final Var<ComposerShape>  composerShape;
    private final Var<String>         status;

    // ── Derived, read-only views ──────────────────────────────────────────
    private final Val<Room>          currentRoom;
    private final Val<UUID>          currentRoomId;
    private final Val<Tuple<Member>> members;
    private final Val<Boolean>       isRoomy;
    private final Val<Layout>        composerLayout;
    private final Val<String>        themeButtonText;
    private final Val<String>        headline;
    private final Val<String>        roomSigilSvg;
    private final Val<String>        ribbonSvg;
    private final Val<String>        countText;
    private final Val<String>        emptyText;
    private final Val<Boolean>       nothingToShow;
    private final Val<Boolean>       someoneTyping;
    private final Val<String>        typingText;
    private final Val<Boolean>       canSend;
    private final Val<Boolean>       isSearching;
    private final Val<Integer>       conversationHeight;

    /** Loops 0→1 while somebody is typing; drives the three bouncing dots. */
    private final Var<Double> typingPulse = Var.of(0.0);

    public ChatView( Var<ChatViewModel> vm ) {
        this.vm    = vm;
        this.sheet = new ChatStyle(vm.get().theme());

        rooms         = vm.zoomTo(ChatViewModel::rooms,          ChatViewModel::withRooms);
        draft         = vm.zoomTo(ChatViewModel::currentMessage, ChatViewModel::withCurrentMessage);
        search        = vm.zoomTo(ChatViewModel::search,         ChatViewModel::withSearch);
        hovered       = vm.zoomTo(ChatViewModel::hovered,        ChatViewModel::withHovered);
        theme         = vm.zoomTo(ChatViewModel::theme,          ChatViewModel::withTheme);
        formfactor    = vm.zoomTo(ChatViewModel::formfactor,     ChatViewModel::withFormfactor);
        composerShape = vm.zoomTo(ChatViewModel::composerShape,  ChatViewModel::withComposerShape);
        status        = vm.zoomTo(ChatViewModel::status,         ChatViewModel::withStatus);

        // ...and the interesting one: a lens onto something the view model
        // *computes* rather than stores. Because the projection is derived from
        // the root, it re-evaluates when the search text, the selected room or
        // the messages change — and its wither folds edits and deletions back
        // into the room's full list. (Idea #1 in the class docs.)
        visibleMessages = vm.zoomTo(ChatViewModel::visibleMessages, ChatViewModel::withVisibleMessages);

        currentRoom   = vm.viewAs(Room.class, ChatViewModel::room);
        currentRoomId = vm.viewAs(UUID.class, m -> m.room().id());
        members       = vm.viewAs(Tuple.classTyped(Member.class), m -> m.room().members());
        canSend       = vm.viewAs(Boolean.class, ChatViewModel::canSend);
        someoneTyping = vm.viewAs(Boolean.class, ChatViewModel::isSomeoneTyping);
        typingText    = vm.viewAsString(m -> m.isSomeoneTyping() ? m.typist().firstName() + " is typing" : "");
        headline      = vm.viewAsString(ChatView::headlineOf);
        isRoomy       = formfactor.viewAs(Boolean.class, Formfactor::isRoomy);
        isSearching   = search.viewAs(Boolean.class, s -> !s.trim().isEmpty());
        nothingToShow = visibleMessages.viewAs(Boolean.class, Tuple::isEmpty);
        conversationHeight = vm.viewAsInt(ChatViewModel::conversationHeight);

        composerLayout  = composerShape.viewAs(Layout.class, s -> s.isRow() ? COMPOSER_ROW : COMPOSER_STACK);
        themeButtonText = Viewable.of(String.class, theme, formfactor, (t, f) ->
                              f.isRoomy() ? ( t.isDark() ? "☀  Day canopy" : "☾  Night canopy" )
                                          : ( t.isDark() ? "☀" : "☾" ));

        // Both pictures are plain SVG strings derived from the model. (Idea #3.)
        roomSigilSvg = Viewable.of(String.class, currentRoom, theme, (r, t) -> ChatArt.roomSigil(r, t.palette()));
        ribbonSvg    = Viewable.of(String.class, currentRoom, theme, (r, t) -> ChatArt.conversationRibbon(r, t.palette()));

        countText = Viewable.of(String.class, visibleMessages, search, (msgs, s) ->
                        s.trim().isEmpty() ? msgs.size() + " messages"
                                           : msgs.size() + " matching “" + s.trim() + "”");
        emptyText = Viewable.of(String.class, search, currentRoom, (s, r) ->
                        s.trim().isEmpty() ? "Nothing has been said in " + r.tag() + " yet. Go on — say hello."
                                           : "No message in " + r.tag() + " matches “" + s.trim() + "”.");

        // The one raw subscription in this view: swapping the base look-and-feel
        // is not something a style sheet can do for us.
        Viewable.cast(theme).onChange(From.ALL, it -> applyTheme(it.currentValue().orElseThrowUnchecked()));

        // Everything is built inside the sheet's scope, so a theme swap repaints
        // the whole tree without touching a single component.
        UI.use(sheet, () ->
            of(this).group(Skin.FRAME)
            .withLayout(FILL.and(WRAP(1)).and(INS(0)).and("gap 0, hidemode 3"))
            .withPrefSize(1300, 830)
            .withMinSize(0, 0)
            // The single place where the shape of the window enters the model —
            // and it hands both numbers straight to the view model, which decides
            // what they mean. No pixel arithmetic lives in the view.
            .onResize( it -> vm.update(From.VIEW, m -> m.withViewSize(it.getWidth(), it.getHeight())) )
            .add(GROW_X.and("wmin 0"), header())
            .add(GROW.and(PUSH).and("wmin 0"), body())
            .add(GROW_X.and("wmin 0"), label(status).group(Skin.STATUS))
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Top chrome
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> header() {
        return
            panel(FILL).group(Skin.HEADER)
            .withLayout("fill, ins 0, gap 12, hidemode 3", "[][grow][150::320][]")
            .add(GROW_Y, icon(LOGO))
            .add(LEFT.and("wmin 0"),
                box(FILL.and(WRAP(1)).and(INS(0)).and("hidemode 3"))
                .add(GROW_X.and("wmin 0"), label("Treehouse").group(Skin.APP_TITLE))
                // Gear 4: the tag line is the first thing to go, because every
                // number in it is visible somewhere else too.
                .add(GROW_X.and("wmin 0"), label(headline).group(Skin.APP_SUBTITLE).isVisibleIf(isRoomy))
            )
            .add(GROW_X.and("wmin 0"), searchBar())
            .add(RIGHT,
                button(themeButtonText).group(Skin.GHOST_BUTTON)
                .withTooltip("Swap the skin — one style sheet, one reconfigure(), zero rebuilt components")
                .onClick( it -> vm.update(ChatViewModel::toggleTheme) )
            );
    }

    private UIForAnySwing<?,?> searchBar() {
        return
            box(FILL).withLayout("fill, ins 0, gap 5, hidemode 3", "[][grow][]")
            .add(label("⌕").withStyle(theme, (t, it) -> it
                 .componentFont(f -> f.family(FONT).size(17).color(t.palette().subtext))))
            .add(GROW_X.and("wmin 0"),
                textField(search).group(Skin.SEARCH_FIELD)
                .withTooltip("Filter this room by text, name or @handle")
            )
            .add(button("✕").group(Skin.ICON_BUTTON).isVisibleIf(isSearching)
                 .withTooltip("Clear the filter")
                 .onClick( it -> search.set(From.VIEW, "") ));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The page — three cards in one 12-column grid (gear 1)
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> body() {
        return
            scrollPane( conf -> conf.fitWidth(true) ).group(Skin.PAGE_SCROLL)
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withScrollIncrement(26)
            .add(
                // Transparent all the way down, so the frame's gradient and grain
                // stay one continuous backdrop behind every card.
                panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 16, 16)
                .withMinSize(0, 0)                        // a grid's minimum is the SUM of its children's
                .withPrefSize(PAGE_REFERENCE_WIDTH, 0)    // ...and this is where the bands sit
                .withStyle( it -> it.backgroundColor(TRANSPARENT).padding(14, 16, 20, 16) )
                .add(ROOMS_SPAN,  roomsCard())
                .add(CHAT_SPAN,   chatCard())
                .add(PEOPLE_SPAN, peopleCard())
            );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Left — the room rail
    // ════════════════════════════════════════════════════════════════════════

    /**
     *  A grid inside a grid — never a grid inside a MigLayout cell. A wrapping
     *  grid answers "how tall are you at the width you are about to get?", and
     *  only another grid asks that question; a MigLayout parent would read the
     *  literal {@code 0} from {@code withPrefSize(w, 0)} and render this whole
     *  card at zero height.
     */
    private UIForAnySwing<?,?> roomsCard() {
        return
            panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 8, 8).group(Skin.CARD)
            .withMinSize(0, 0)
            .withPrefSize(ROOMS_REFERENCE_WIDTH, 0)
            .withStyle( it -> it.padding(14, 12, 16, 12) )
            .add(FULL_ROW, sectionLabel("ROOMS"))
            .add(FULL_ROW, roomRail());
    }

    /**
     *  The rail gets a container of its own for a hard reason: a bound
     *  {@code addAll(..)} <b>owns</b> the component it binds to and clears
     *  anything that was added to it by hand. So a bound list always lives in
     *  its own panel — never as one child among several.
     */
    private UIForAnySwing<?,?> roomRail() {
        return
            panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 8, 8)
            .withMinSize(0, 0)
            .withPrefSize(ROOMS_REFERENCE_WIDTH, 0)
            .withStyle( it -> it.backgroundColor(TRANSPARENT) )
            .addAll(CHIP_SPAN, rooms, this::roomChip);
    }

    /**
     *  <b>Every row supplier re-enters the style-sheet scope, and has to.</b>
     *  A {@code StyleSheet} only binds components that were built inside its
     *  {@code UI.use(..)} lambda — but a bound {@code addAll} builds its rows
     *  <em>later</em>, every time the tuple changes. Without this wrapper the
     *  initial rows look right and the ones built after the first room switch
     *  come out completely unstyled, which is a wonderfully confusing bug to
     *  chase. {@code UI.use} consumes the builder and returns the component, so
     *  it is wrapped again to keep the declaration flowing.
     */
    private UIForAnySwing<?,?> roomChip( Var<Room> entry ) {
        return UI.of(UI.use(sheet, () -> roomChipBody(entry).get(JPanel.class)));
    }

    /**
     *  Concept #4 — one composite view drives the whole chip. Its look depends
     *  on three independent properties (the room itself, which room is selected,
     *  and the theme), so instead of chaining three stylers they are folded into
     *  a single {@link ChipLook} item that can never disagree with itself.
     */
    private UIForPanel<JPanel> roomChipBody( Var<Room> entry ) {
        UUID roomId = entry.get().id();

        Viewable<ChipLook> look = Viewable.of(ChipLook.seed(), it -> it
            .join(entry,         ChipLook::withRoom)
            .join(currentRoomId, ChipLook::withSelectedId)
            .join(theme,         ChipLook::withTheme));

        Val<String>  sigil     = Viewable.of(String.class, entry, theme, (r, t) -> ChatArt.roomSigil(r, t.palette()));
        Val<Boolean> hasUnread = entry.viewAs(Boolean.class, r -> r.unread() > 0);
        Val<String>  unread    = entry.viewAsString(r -> r.unread() > 0 ? String.valueOf(r.unread()) : "");

        return
            panel(FILL).withLayout("fill, ins 8 10 8 10, gap 9, hidemode 3", "[24!][grow][]")
            .withStyle(look, ChatView::chipStyle)
            .withCursor(UI.Cursor.HAND)
            .withTooltip(entry.get().topic())
            .onMouseClick( it -> vm.update(m -> m.selectRoom(indexOfRoom(m, roomId))) )
            .add(GROW_Y,
                box().withPrefSize(24, 24)
                .withStyle(sigil, (svg, it) -> it.image(img -> img.svg(svg)))
            )
            .add(GROW_X.and("wmin 0"),
                box(FILL.and(WRAP(1)).and(INS(0)).and("hidemode 3"))
                .add(GROW_X.and("wmin 0"),
                    label(entry.viewAsString(Room::tag))
                    .withStyle(look, (l, it) -> it.componentFont(f -> f
                        .family(FONT).size(13).weight(l.selected() ? 2f : 1f).color(l.textColor())))
                )
                // "wmin 0" ⇒ a long topic ellipsizes instead of giving the whole
                // window a minimum width.
                .add(GROW_X.and("wmin 0"), label(entry.viewAsString(Room::topic)).group(Skin.META))
            )
            .add(RIGHT,
                label(unread).isVisibleIf(hasUnread)
                .withStyle(look, (l, it) -> it
                    .backgroundColor(l.accent())
                    .borderRadius(100)
                    .padding(1, 7, 1, 7)
                    .componentFont(f -> f.family(FONT).size(11).weight(2f).color(l.onAccent())))
            );
    }

    private static ComponentStyleDelegate<JPanel> chipStyle( ChipLook l, ComponentStyleDelegate<JPanel> it ) {
        Color hue = l.accent();
        return it
            .backgroundColor(l.selected() ? l.wash() : TRANSPARENT)
            .borderRadius(12)
            .border(1, l.selected() ? hue : TRANSPARENT)
            .borderAt(UI.Edge.LEFT, l.selected() ? 3 : 0, hue)
            .margin(0);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Middle — the conversation
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> chatCard() {
        return
            panel(FILL.and(WRAP(1)).and(INS(0))).group(Skin.CARD)
            .withLayout("fill, wrap 1, ins 0, gap 0, hidemode 3")
            .add(GROW_X.and("wmin 0"), conversationHeader())
            .add(GROW.and(PUSH).and("wmin 0"), messageList())
            .add(GROW_X.and("wmin 0"), label(emptyText).group(Skin.EMPTY).isVisibleIf(nothingToShow))
            .add(GROW_X.and("wmin 0"), typingRow())
            .add(GROW_X.and("wmin 0"), composerBar());
    }

    private UIForAnySwing<?,?> conversationHeader() {
        return
            box(FILL).withLayout("fill, ins 14 16 11 16, gap 11, hidemode 3", "[28!][grow][]")
            .withStyle(theme, (t, it) -> it.borderAt(UI.Edge.BOTTOM, 1, t.palette().border))
            .add(GROW_Y,
                box().withPrefSize(28, 28)
                .withStyle(roomSigilSvg, (svg, it) -> it.image(img -> img.svg(svg)))
            )
            .add(GROW_X.and("wmin 0"),
                box(FILL.and(WRAP(1)).and(INS(0)).and("hidemode 3"))
                .add(GROW_X.and("wmin 0"), label(currentRoom.viewAsString(Room::tag)).group(Skin.CARD_TITLE))
                .add(GROW_X.and("wmin 0"), label(currentRoom.viewAsString(Room::topic)).group(Skin.CARD_SUB))
            )
            .add(RIGHT, label(countText).group(Skin.META));
    }

    /**
     *  {@code scrollPanels} has no natural preferred size, so it gets one — a
     *  flow-grid row is only as tall as its tallest child <i>prefers</i> to be,
     *  and without this the whole conversation would collapse to a single line.
     *  <p>
     *  That height is itself convergent, and it has to be: once the page stacks,
     *  a 540-pixel conversation would push the composer below the fold, and the
     *  primary action has to stay reachable at <em>every</em> size. Binding it
     *  through a property-driven style keeps that a plain function of the form
     *  factor — no listener, nothing rebuilt.
     */
    private UIForAnySwing<?,?> messageList() {
        return
            scrollPanels().withMinSize(0, 150)
            .withStyle(conversationHeight, (tall, it) -> it.prefSize(560, tall))
            .withStyle( it -> it.backgroundColor(TRANSPARENT).border(0, TRANSPARENT).padding(6, 4, 6, 4) )
            .addAll(visibleMessages, this::messageRow)
            // A fresh message at the bottom is worth nothing if you cannot see it.
            .onView(Viewable.cast(visibleMessages), it -> scrollToBottom(it.get()));
    }

    /** Built later, so it re-enters the sheet scope — see {@link #roomChip(Var)}. */
    private UIForAnySwing<?,?> messageRow( Var<Message> entry ) {
        return UI.of(UI.use(sheet, () -> messageRowBody(entry).get(JPanel.class)));
    }

    /**
     *  One row of the conversation. The {@code entry} handed to us is a
     *  <b>lens onto that one message</b> inside the tuple, so zooming further
     *  ({@code entry.zoomTo(Message::text, Message::withText)}) yields a
     *  {@code Var<String>} whose every keystroke rebuilds the message, the room
     *  and the whole view model — immutably, all the way up.
     */
    private UIForPanel<JPanel> messageRowBody( Var<Message> entry ) {
        Message message = entry.get();
        UUID    id      = message.id();
        Room    room    = vm.get().room();
        Member  author  = room.author(message.authorId());
        boolean mine    = message.authorId().equals(vm.get().me().id());
        String  clock   = message.sentAt().format(CLOCK);

        UIForPanel<JPanel> row =
            panel(FILL)
            .withLayout("fill, ins 4 4 4 4, gap 9, hidemode 3", mine ? "[grow][36!]" : "[36!][grow]")
            .withStyle( it -> it.backgroundColor(TRANSPARENT) )
            // Hovering a row is ordinary view-model state, which is what lets the
            // reaction chips of *this* row appear while every other row stays calm.
            .onMouseEnter( it -> hovered.update(From.VIEW, h -> id) )
            .onMouseExit(  it -> hovered.update(From.VIEW, h -> ChatViewModel.NOBODY) );

        UIForAnySwing<?,?> face   = avatar(author, 36);
        UIForAnySwing<?,?> bubble = bubble(entry, id, author, mine, clock);

        return mine
            ? row.add(GROW_X.and("wmin 0"), bubble).add(TOP, face)
            : row.add(TOP, face).add(GROW_X.and("wmin 0"), bubble);
    }

    private UIForAnySwing<?,?> bubble(
        Var<Message> entry, UUID id, Member author, boolean mine, String clock
    ) {
        Var<String>  text      = entry.zoomTo(Message::text,      Message::withText);
        Var<Boolean> isEditing = entry.zoomTo(Message::isEditing, Message::withEditing);
        // Revealed while the mouse is on the row — or always, while editing, so
        // the controls cannot slip away from under the cursor mid-edit.
        Val<Boolean> revealed  = Viewable.of(Boolean.class, entry, hovered,
                                    (m, h) -> m.isEditing() || h.equals(m.id()));

        return
            panel(FILL.and(WRAP(1)).and(INS(0)))
            .withLayout("fill, wrap 1, ins 9 12 7 12, gap 3, hidemode 3")
            .withStyle(theme, (t, it) -> {
                Theme.Palette p = t.palette();
                return it
                    .backgroundColor(mine ? p.mine : p.theirs)
                    .border(1, p.border)
                    .borderRadius(14)
                    // A squared-off corner on the side the avatar is on: the
                    // bubble points at whoever said it.
                    .borderRadiusAt(mine ? UI.Corner.TOP_RIGHT : UI.Corner.TOP_LEFT, 4, 4)
                    .borderAt(mine ? UI.Edge.RIGHT : UI.Edge.LEFT, 3, p.hue(author.hue()))
                    .margin(0);
            })
            .add(GROW_X.and("wmin 0"), bubbleHeader(author, clock))
            .add(GROW_X.and("wmin 0"), bubbleText(text, isEditing))
            .add(GROW_X.and("wmin 0"), bubbleFooter(entry, id, isEditing, revealed));
    }

    private UIForAnySwing<?,?> bubbleHeader( Member author, String clock ) {
        return
            box(FILL).withLayout("fill, ins 0, gap 7, hidemode 3", "[][grow][]")
            .add(label(author.name()).withStyle(theme, (t, it) -> it
                .componentFont(f -> f.family(FONT).size(12).weight(2f).color(t.palette().hue(author.hue())))))
            // Gear 4: the handle is a second spelling of the name right next to it.
            .add(GROW_X.and("wmin 0"), label(author.handle()).group(Skin.META).isVisibleIf(isRoomy))
            .add(RIGHT, label(clock).group(Skin.META));
    }

    /**
     *  Two independent stylers rather than one composite: the font follows the
     *  theme, while the "I am being edited" highlight is a neutral translucent
     *  tint that reads correctly on either skin. Rules that do not need each
     *  other are clearer chained.
     */
    private UIForAnySwing<?,?> bubbleText( Var<String> text, Var<Boolean> isEditing ) {
        return
            textArea(text)
            .isEditableIf(isEditing)
            .peek(ChatView::softWrap)
            .withStyle(theme, (t, it) -> it
                .componentFont(f -> f.family(FONT).size(13).color(t.palette().text))
                .margin(0))
            .withStyle(isEditing, (editing, it) -> it
                .backgroundColor(editing ? EDIT_TINT : TRANSPARENT)
                .border(editing ? 1 : 0, EDIT_EDGE)
                .borderRadius(8)
                .padding(editing ? 4 : 0, editing ? 6 : 0, editing ? 4 : 0, editing ? 6 : 0));
    }

    private UIForAnySwing<?,?> bubbleFooter(
        Var<Message> entry, UUID id, Var<Boolean> isEditing, Val<Boolean> revealed
    ) {
        return
            box(FILL).withLayout("fill, ins 3 0 0 0, gap 4, hidemode 3", "[][][][][grow][][]")
            .add(reactionChip(entry, id, Message.REACTIONS[0]))
            .add(reactionChip(entry, id, Message.REACTIONS[1]))
            .add(reactionChip(entry, id, Message.REACTIONS[2]))
            .add(reactionChip(entry, id, Message.REACTIONS[3]))
            .add(GROW_X, box())
            .add(toggleButton("✎", isEditing).group(Skin.ICON_BUTTON)
                 .isVisibleIf(revealed).withTooltip("Edit this message in place"))
            .add(button("✕").group(Skin.ICON_BUTTON)
                 .isVisibleIf(revealed).withTooltip("Delete this message")
                 // Removing it from the *filtered* tuple is enough: the lens's
                 // wither knows how to take it out of the room's full list.
                 .onClick( it -> visibleMessages.update(t -> t.maybeRemove(entry)) ));
    }

    /**
     *  A reaction chip is visible when somebody picked that emoji, or while the
     *  mouse is on the row — so a quiet conversation stays quiet and the whole
     *  palette is one hover away.
     */
    private UIForAnySwing<?,?> reactionChip( Var<Message> entry, UUID id, String emoji ) {
        Val<String>  chipText = entry.viewAsString(m ->
                                    m.reactionCount(emoji) > 0 ? emoji + " " + m.reactionCount(emoji) : emoji);
        Val<Boolean> visible  = Viewable.of(Boolean.class, entry, hovered, (m, h) ->
                                    m.reactionCount(emoji) > 0 || m.isEditing() || h.equals(m.id()));
        return
            button(chipText).group(Skin.ICON_BUTTON)
            .isVisibleIf(visible)
            .withTooltip("React with " + emoji)
            .withStyle(theme, (t, it) -> it
                .borderRadius(100)
                .border(1, t.palette().border)
                .padding(0, 6, 0, 6)
                .componentFont(f -> f.family(FONT).size(11).color(t.palette().subtext)))
            .withStyle(entry, (m, it) -> it.backgroundColor(m.iReactedWith(emoji) ? REACTED : TRANSPARENT))
            .onClick( it -> vm.update(m -> m.toggleReaction(id, emoji)) );
    }

    // ── The "someone is typing" strip ─────────────────────────────────────

    private UIForAnySwing<?,?> typingRow() {
        return
            box(FILL).withLayout("fill, ins 0 20 6 20, gap 6, hidemode 3", "[][][][][grow]")
            .isVisibleIf(someoneTyping)
            .add(label(typingText).group(Skin.META))
            .add(typingDot(0)).add(typingDot(1)).add(typingDot(2))
            .add(GROW_X, box());
    }

    /**
     *  Two chained stylers again, and deliberately so: the colour is a function
     *  of the theme, the bounce is a function of the animation. Neither needs
     *  the other, so neither has to know about the other.
     */
    private UIForAnySwing<?,?> typingDot( int index ) {
        return
            box().withPrefSize(7, 14)
            .withStyle(theme, (t, it) -> it.borderRadius(100).backgroundColor(t.palette().accent))
            .withStyle(typingPulse, (pulse, it) -> {
                double phase = (pulse + index / 3.0) % 1.0;
                double lift  = 0.5 - 0.5 * Math.cos(phase * 2 * Math.PI);
                return it.margin(7 - 6 * lift, 0, 1 + 6 * lift, 0);
            });
    }

    // ── The composer (gear 2) ─────────────────────────────────────────────

    private UIForAnySwing<?,?> composerBar() {
        return
            box(FILL).withLayout("fill, ins 6 14 14 14")
            .add(GROW_X.and("wmin 0"), composer());
    }

    private UIForAnySwing<?,?> composer() {
        return
            panel(composerLayout).group(Skin.COMPOSER)
            .withMinSize(0, 0)
            // The composer measures *itself*: the window can be very wide while
            // this card is narrow, so the window's width is the wrong number.
            .onResize( it -> composerShape.update(From.VIEW, s -> ComposerShape.of(it.getWidth(), s)) )
            .add(
                // A text *field*, not a text area, and on purpose: `onEnter` is a
                // plain action hook, so pressing return sends. Catching return on
                // a text area with `onKeyPress` instead would not work under
                // `EventProcessor.DECOUPLED` — that handler runs on the
                // application thread, long after Swing has already inserted the
                // newline, so consuming the event there is too late.
                textField(draft).group(Skin.COMPOSER_INPUT)
                .withPrefSize(240, 34).withMinSize(0, 28)
                .withTooltip("Write something and press return")
                .onEnter( it -> send() )
            )
            .add(
                box(FILL).withLayout("ins 0, gap 3")
                .add(quickGlyph("☺")).add(quickGlyph("★")).add(quickGlyph("♥")).add(quickGlyph("✿"))
            )
            .add(
                button("Send  ➤").group(Skin.ACCENT_BUTTON)
                .isEnabledIf(canSend)
                .onClick( it -> send() )
            );
    }

    private UIForAnySwing<?,?> quickGlyph( String glyph ) {
        return
            button(glyph).group(Skin.ICON_BUTTON)
            .withTooltip("Drop " + glyph + " into your message")
            .onClick( it -> draft.set(From.VIEW,
                        draft.get().trim().isEmpty() ? glyph + " " : draft.get().trim() + " " + glyph + " ") );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Right — the roster and the conversation ribbon
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> peopleCard() {
        return
            panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 8, 8).group(Skin.CARD)
            .withMinSize(0, 0)
            .withPrefSize(PEOPLE_REFERENCE_WIDTH, 0)
            .withStyle( it -> it.padding(14, 12, 16, 12) )
            .add(FULL_ROW, sectionLabel("IN THE ROOM"))
            .add(FULL_ROW, roster())
            .add(FULL_ROW, sectionLabel("CONVERSATION RIBBON"))
            .add(FULL_ROW,
                box().withPrefSize(300, 74).withMinSize(0, 74)
                .withStyle(ribbonSvg, (svg, it) -> it.image(img -> img
                    .svg(svg)
                    .fitMode(UI.FitComponent.WIDTH_AND_HEIGHT)
                    .placement(UI.Placement.CENTER)))
            )
            .add(FULL_ROW,
                label("One bar per message, in its author's colour.")
                .group(Skin.META)
            );
    }

    /** Same rule as the room rail: a bound {@code addAll} gets a panel to itself. */
    private UIForAnySwing<?,?> roster() {
        return
            panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 8, 8)
            .withMinSize(0, 0)
            .withPrefSize(PEOPLE_REFERENCE_WIDTH, 0)
            .withStyle( it -> it.backgroundColor(TRANSPARENT) )
            // A read-only tuple ⇒ the supplier gets the value, not a lens, and
            // the item type needs no HasId at all.
            .addAll(CHIP_SPAN, members, this::memberChip);
    }

    /** Built later, so it re-enters the sheet scope — see {@link #roomChip(Var)}. */
    private UIForAnySwing<?,?> memberChip( Member member ) {
        return UI.of(UI.use(sheet, () -> memberChipBody(member).get(JPanel.class)));
    }

    private UIForPanel<JPanel> memberChipBody( Member member ) {
        return
            panel(FILL).withLayout("fill, ins 7 9 7 9, gap 9, hidemode 3", "[34!][grow]")
            .withStyle(theme, (t, it) -> it
                .backgroundColor(t.palette().raised)
                .borderRadius(12)
                .border(1, t.palette().border)
                .margin(0))
            .withTooltip(member.name() + "  " + member.handle() + "  —  " + member.presence().label())
            // TOP, not GROW_Y: a stretched cell would turn the disc into an oval.
            .add(TOP, avatar(member, 34))
            .add(GROW_X.and("wmin 0"),
                box(FILL.and(WRAP(1)).and(INS(0)).and("hidemode 3"))
                .add(GROW_X.and("wmin 0"), label(member.name()).withStyle(theme, (t, it) -> it
                    .componentFont(f -> f.family(FONT).size(12).weight(2f).color(t.palette().text))))
                .add(GROW_X.and("wmin 0"),
                    label(member.presence().dot() + "  " + member.presence().label())
                    .withStyle(theme, (t, it) -> it.componentFont(f -> f
                        .family(FONT).size(11).color(t.palette().presence(member.presence())))))
                // Gear 4 again: nice to have, never the only place it is said.
                .add(GROW_X.and("wmin 0"), label(member.blurb()).group(Skin.META).isVisibleIf(isRoomy))
            );
    }

    /** A disc of initials: filled with the member's washed hue, ringed by their presence. */
    private UIForAnySwing<?,?> avatar( Member member, int diameter ) {
        return
            label(member.initials())
            .withHorizontalAlignment(UI.HorizontalAlignment.CENTER)
            .withStyle(theme, (t, it) -> {
                Theme.Palette p = t.palette();
                return it
                    .prefSize(diameter, diameter)
                    .backgroundColor(p.hueWash(member.hue()))
                    .borderRadius(100)
                    .border(2, p.presence(member.presence()))
                    .componentFont(f -> f
                        .family(FONT)
                        .size(Math.max(10, diameter / 3))
                        .weight(2f)
                        .color(p.hue(member.hue()))
                    );
            });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Actions
    // ════════════════════════════════════════════════════════════════════════

    /** Posts the draft, then lets somebody in the room answer a moment later. */
    private void send() {
        ChatViewModel before = vm.get();
        if ( !before.canSend() )
            return;
        String sentText = before.currentMessage();
        vm.update(ChatViewModel::send);
        answerLater(sentText);
    }

    /**
     *  The only place in this view that touches a clock. Everything it decides
     *  (who answers, what they say, whether another room stirs) is computed by
     *  the Swing-free {@link ChatBot}; the view merely contributes the delay and
     *  hands each result back to the view model.
     */
    private void answerLater( String sentText ) {
        ChatViewModel after     = vm.get();
        Member        responder = after.room().someoneElseThan(after.me());
        if ( responder == Member.UNKNOWN )
            return;

        String reply       = ChatBot.replyTo(after.room(), sentText);
        int    gossipIndex = ChatBot.gossipRoomIndex(after);
        String gossip      = gossipIndex < 0 ? "" : ChatBot.gossipIn(after.rooms().get(gossipIndex));

        vm.update(m -> m.startTyping(responder));
        startTypingAnimation();

        UI.runLater(1.4, TimeUnit.SECONDS, () -> {
            vm.update(m -> m.receive(m.selectedRoom(), responder, reply));
            if ( gossipIndex >= 0 )
                vm.update(m -> m.receive(gossipIndex,
                                         m.rooms().get(gossipIndex).someoneElseThan(m.me()),
                                         gossip));
        });
    }

    /**
     *  An ambient loop that keeps running only while somebody is actually
     *  typing — so nothing is left ticking in a window that is merely open.
     */
    private void startTypingAnimation() {
        UI.animateFor(1.1, TimeUnit.SECONDS)
          .asLongAs( state -> vm.get().isSomeoneTyping() )
          .go( state -> typingPulse.set(state.progress()) );
    }

    private void applyTheme( Theme newTheme ) {
        UI.run(() -> {
            if ( newTheme.isDark() ) FlatDarkLaf.setup(); else FlatLightLaf.setup();
            sheet.setTheme(newTheme);
            FlatLaf.updateUI();
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Small shared helpers
    // ════════════════════════════════════════════════════════════════════════

    private static UIForAnySwing<?,?> sectionLabel( String text ) {
        return label(text).group(Skin.SECTION_LABEL);
    }

    private static String headlineOf( ChatViewModel m ) {
        String head = m.room().tag() + "  ·  " + m.room().onlineCount()
                    + " of " + m.room().members().size() + " here";
        return m.totalUnread() > 0 ? head + "  ·  " + m.totalUnread() + " unread elsewhere" : head;
    }

    private static int indexOfRoom( ChatViewModel m, UUID roomId ) {
        Tuple<Room> all = m.rooms();
        for ( int i = 0; i < all.size(); i++ )
            if ( all.get(i).id().equals(roomId) )
                return i;
        return m.selectedRoom();
    }

    /**
     *  The one raw-component tweak in the whole view. {@code JTextArea} line
     *  wrapping has no {@code with*} equivalent in SwingTree, which is exactly
     *  the case {@code peek} exists for — a niche Swing setter the library does
     *  not wrap. Everything else here goes through the builder.
     */
    private static void softWrap( JTextArea area ) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    private static void scrollToBottom( JScrollPane pane ) {
        UI.runLater(() -> {
            JScrollBar bar = pane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // ════════════════════════════════════════════════════════════════════════

    /**
     *  The merged item behind a room chip's single {@code withStyle}: the room,
     *  whether it is the selected one, and the current skin. Built by the
     *  Sprouts composite view builder, one {@code join} per input — see
     *  {@link ChatView#roomChip(Var)}.
     */
    private static final class ChipLook {

        private final Room    room;
        private final boolean selected;
        private final Theme   theme;

        private ChipLook( Room room, boolean selected, Theme theme ) {
            this.room = room; this.selected = selected; this.theme = theme;
        }

        static ChipLook seed() {
            return new ChipLook(Room.of("", "", 0, Tuple.of(Member.class)), false, Theme.DARK);
        }

        ChipLook withRoom( Room newRoom )        { return new ChipLook(newRoom, selected, theme); }
        ChipLook withSelectedId( UUID id )       { return new ChipLook(room, room.id().equals(id), theme); }
        ChipLook withTheme( Theme newTheme )     { return new ChipLook(room, selected, newTheme); }

        boolean selected()  { return selected; }
        Color   accent()    { return theme.palette().hue(room.hue()); }
        Color   wash()      { return theme.palette().hueWash(room.hue()); }
        Color   onAccent()  { return theme.palette().onAccent; }
        Color   textColor() { return selected ? accent() : theme.palette().text; }
    }

    // ════════════════════════════════════════════════════════════════════════

    public static void main( String[] args ) {
        Var<ChatViewModel> vm = Var.of(new ChatViewModel());
        if ( vm.get().theme().isDark() ) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        UI.show("Treehouse — a SwingTree chat", frame -> new ChatView(vm));
        EventProcessor.DECOUPLED.join();
    }
}

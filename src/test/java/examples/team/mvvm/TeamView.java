package examples.team.mvvm;
import java.util.Locale;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.Val;
import sprouts.Vals;
import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForLabel;
import swingtree.UIForTextField;
import swingtree.layout.FlowCell;
import swingtree.threading.EventProcessor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;

import static swingtree.UI.*;

/**
 *  <h2>People Directory — classical MVVM flavor</h2>
 *
 *  <p>The visual and functional twin of {@code examples.team.mvi.TeamView},
 *  built on the <b>classical MVVM</b> pattern instead of the immutable MVI /
 *  MVL pattern. Both render the same UI; the wiring is what differs.</p>
 *
 *  <p>Key differences from the MVI version:</p>
 *  <ul>
 *    <li><b>No root view-model record.</b> {@link Team} owns its own state
 *        as mutable {@code Var<X>} fields; mutation happens in place.</li>
 *    <li><b>Observable list via {@code Vars<Person>}.</b> The roster is
 *        rendered with {@code addAll(Vals<Person>, viewSupplier)} — adds
 *        and removes on the list fire change events that SwingTree picks
 *        up automatically.</li>
 *    <li><b>No lenses.</b> Each per-person editor field binds directly
 *        to that person's own {@code Var<X>} property — no
 *        {@code zoomTo} chains needed because the model is already
 *        granular.</li>
 *  </ul>
 *
 *  <p>The layout, on the other hand, is <b>identical</b> to the MVI twin, and
 *  that is the point: convergence is a property of the view tree, not of the
 *  state-management pattern underneath it. The page is a responsive 12-column
 *  grid, so the roster sits beside the editor while there is room and stacks
 *  into a single scrolling column when there is not.</p>
 *
 *  <p>Run {@link #main(String[])} to open the window.</p>
 */
public final class TeamView {

    // ── Palette ──────────────────────────────────────────────────────────────

    private static final Color BG          = new Color( 24,  26,  38);
    private static final Color BG_SOFT     = new Color( 34,  38,  54);
    private static final Color BG_SIDEBAR  = new Color( 18,  20,  30);
    private static final Color BG_CARD     = new Color( 42,  46,  64);
    private static final Color BG_CARD_HI  = new Color( 56,  62,  86);
    private static final Color INK         = new Color(232, 236, 252);
    private static final Color INK_FAINT   = new Color(150, 162, 198);
    private static final Color ACCENT      = new Color(120, 176, 238);
    private static final Color ACCENT_LEAD = new Color(255, 200,  90);
    private static final Color DANGER      = new Color(232, 110, 130);
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    // ── Public entry points ─────────────────────────────────────────────────

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        Team team = initialDirectory();
        UI.show("People Directory — MVVM", f -> new TeamView().build(team));
        EventProcessor.DECOUPLED.join();
    }

    public static JPanel createView() {
        return new TeamView().build(initialDirectory());
    }

    private static Team initialDirectory() {
        Team team = new Team("Research");
        Person marry = new Person("Marry Tisdale", 32, "Loves whiteboards and quiet mornings.",
                new Occupation("Architect", "Designs the long-lived shape of the system."), Gender.FEMALE);
        Person sam   = new Person("Sam Okafor",    28, "Will find the one corner case you forgot.",
                new Occupation("Quality Engineer", "Owns the test plan and the bug board."), Gender.MALE);
        Person alex  = new Person("Alex Rivera",   27, "Half engineer, half historian of regressions.",
                new Occupation("Engineer", "Builds, ships, then refactors."), Gender.DIVERSE);
        Person jin   = new Person("Jin Park",      35, "Translates fuzzy ideas into clear specs.",
                new Occupation("Product Lead", "Owns the why; helps the team own the how."), Gender.MALE);
        Person lina  = new Person("Lina Fischer",  30, "Has strong opinions about kerning.",
                new Occupation("Designer", "Cares about how the product feels."), Gender.FEMALE);
        team.members().add(marry);
        team.members().add(sam);
        team.members().add(alex);
        team.members().add(jin);
        team.members().add(lina);
        team.lead().set(marry);
        team.selectedMember().set(marry);
        return team;
    }

    // ── The responsive span table — the whole "convergence" config ───────────
    //
    // A size category is a fraction of a grid's *reference width*, so these are
    // relative bands rather than pixel breakpoints. Roster beside editor from
    // LARGE upwards, one stacked column below that.

    private static final int PAGE_REFERENCE_WIDTH = 900;

    /** The full row at every size — the building block of a stacked layout. */
    private static final FlowCell FULL_ROW = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(12).veryLarge(12).oversize(12) );

    private static final FlowCell ROSTER_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(5).veryLarge(4).oversize(4) );

    private static final FlowCell EDITOR_SPAN = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(12).medium(12).large(7).veryLarge(8).oversize(8) );

    private JPanel build(Team team) {
        return UI.panel("fill, wrap 1, insets 0, gap 0").withPrefSize(960, 650)
            .withStyle( it -> it.backgroundColor(BG) )
            .add("growx, wmin 0",      header(team))
            .add("grow, push, wmin 0", body(team))
            .get(JPanel.class);
    }

    // ── The page: a scrolling 12-column grid holding the two cards ───────────
    //
    // A flow grid gives every row the height of its tallest child, so once the
    // cards stack the page outgrows the window — hence the scroll pane.

    private static UIForAnySwing<?,?> body(Team team) {
        return UI.scrollPane( conf -> conf.fitWidth(true) )
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withVerticalScrollIncrement(24)
            .withStyle( it -> it.backgroundColor(BG).borderWidth(0).padding(0) )
            .add(
                UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 18, 18)
                .withMinSize(0, 0) // a grid reports the SUM of its children's minimums
                .withPrefSize(PAGE_REFERENCE_WIDTH, 0) // the reference width, see above
                .withStyle( it -> it.backgroundColor(BG) )
                .add(ROSTER_SPAN, roster(team))
                .add(EDITOR_SPAN, editor(team))
            );
    }

    // ── Header ───────────────────────────────────────────────────────────────
    //
    // Plain MigLayout, not a grid: a grid declares its reference width through an
    // explicit preferred size, and a MigLayout parent would read that height
    // literally. A grid can only carry a reference width inside another grid.

    private static UIForAnySwing<?,?> header(Team team) {
        Val<Integer> memberCount = team.members().viewSize();
        Val<String>  leadName    = team.lead().viewAsString(p -> p == null ? "— no lead —" : p.name().get());

        return UI.panel("fill, wrap 2, insets 12 26 12 26, gap 12 6", "[grow][]")
            .withStyle( it -> it
                .backgroundColor(BG_SIDEBAR)
                .borderAt(Edge.BOTTOM, 1, new Color(60, 70, 100))
            )
            // Title and subtitle as two labels rather than one html(..) string:
            // html re-wraps instead of ellipsizing, so it just gets clipped.
            .add("split 2",
                UI.label("People Directory").withStyle( it -> it
                    .foregroundColor(new Color(0xF3, 0xF4, 0xFF))
                    .componentFont( f -> f.family("Serif").size(21) )
                )
            )
            .add("pushx, growx, wmin 0",
                UI.label("  —  the classical-MVVM twin of the MVI example").withStyle( it -> it
                    .foregroundColor(INK_FAINT)
                    .componentFont( f -> f.family("SansSerif").size(12).posture(0.15f) )
                )
            )
            .add("aligny center",
                UI.button("＋  Add member")
                .withStyle( it -> it
                    .backgroundColor(ACCENT)
                    .foregroundColor(Color.WHITE)
                    .borderRadius(20)
                    .padding(8, 16, 8, 16)
                    .margin(0)
                    .componentFont( f -> f.family("SansSerif").size(12).weight(2) )
                )
                .onClick( it -> team.addBlankMember() )
            )
            // A row of its own for the chips, so the button cannot squeeze them out.
            .add("span 2, growx, wmin 0",
                UI.panel("insets 0, gap 12").withStyle( it -> it.backgroundColor(TRANSPARENT) )
                .add(
                    UI.label("Team:").withStyle( it -> it
                        .foregroundColor(INK_FAINT)
                        .componentFont( f -> f.family("SansSerif").size(11) )
                    )
                )
                .add("width 90::200", inlineTextField(team.name()))
                .add(chip(memberCount.viewAsString(n -> n + " " + (n == 1 ? "member" : "members")), ACCENT))
                .add("wmin 0", chip(leadName.viewAsString(s -> "★  " + s), ACCENT_LEAD))
            );
    }

    // ── Roster: scrollable list of member cards, driven by Vars<Person> ────

    private static UIForAnySwing<?,?> roster(Team team) {
        return UI.panel("fill, wrap 1, insets 0, gap 0")
            .withStyle( it -> it.backgroundColor(BG_SIDEBAR).borderRadius(14) )
            .add("growx, wmin 0",
                UI.panel("fill, insets 14 18 10 18").withStyle( it -> it.backgroundColor(TRANSPARENT) )
                .add("pushx, growx, wmin 0",
                    UI.label("ROSTER").withStyle( it -> it
                        .foregroundColor(INK_FAINT)
                        .componentFont( f -> f.family("SansSerif").size(10).weight(2).spacing(0.12f) )
                    )
                )
            )
            .add("grow, push, wmin 0",
                // A scroll pane has almost no preferred height of its own, and in a
                // flow grid a card is exactly as tall as it prefers.
                UI.scrollPanels().withPrefSize(340, 470)
                .addAll((Vals<Person>) team.members(), (Person p) -> memberCard(p, team))
            );
    }

    private static UIForAnySwing<?,?> memberCard(Person p, Team team) {
        Val<Boolean> selected = team.selectedMember().viewAs(Boolean.class, sel -> sel != null && sel.id().equals(p.id()));
        Val<Boolean> isLead   = team.lead().viewAs(Boolean.class,           led -> led != null && led.id().equals(p.id()));

        Val<String> displayName = p.name().viewAsString(n -> n.isEmpty() ? "Unnamed" : n);
        Val<String> displayRole = p.occupation().name().viewAsString(n -> n.isEmpty() ? "—" : n);
        Val<String> displayBio  = p.bio().viewAsString(b -> b.isEmpty() ? "" : "“" + b + "”");

        return UI.panel("fill, insets 0, gap 0")
            .withStyle( selected, (isSelected, it) -> it
                .backgroundColor(isSelected ? BG_CARD_HI : BG_CARD)
                .borderRadius(12)
                .borderAt(Edge.LEFT, 3, isSelected ? ACCENT : TRANSPARENT)
                .margin(0, 12, 8, 12)
                .padding(0)
                .cursor(Cursor.HAND)
            )
            .onMouseClick( it -> team.selectedMember().set(p) )
            .add("grow, push",
                UI.panel("fill, insets 12 14 12 14, gap 10")
                .withStyle( it -> it.backgroundColor(TRANSPARENT) )
                .add(avatarDot(p))
                .add("grow, push, wmin 0",
                    UI.panel("fill, wrap 1, insets 0, gap 2")
                    .withStyle( it -> it.backgroundColor(TRANSPARENT) )
                    .add("growx, wmin 0",
                        UI.panel("insets 0, gap 6, hidemode 3").withStyle( it -> it.backgroundColor(TRANSPARENT) )
                        // The name yields before the ★ LEAD chip does, since the chip
                        // is a fixed short badge while a name can be arbitrarily long.
                        .add("wmin 0",
                            UI.label(displayName).withStyle( it -> it
                                .foregroundColor(INK)
                                .componentFont( f -> f.family("SansSerif").size(14).weight(2) )
                            )
                        )
                        // Bound, not `applyIf(isLead.get(), ..)`: promoting a member
                        // does not touch the roster list, so these cards are never
                        // rebuilt and a snapshot of the flag would go stale.
                        .add(chip("★ LEAD", ACCENT_LEAD).isVisibleIf(isLead))
                    )
                    .add("growx, wmin 0",
                        UI.label(displayRole).withStyle( it -> it
                            .foregroundColor(ACCENT)
                            .componentFont( f -> f.family("SansSerif").size(11) )
                        )
                    )
                    .add("growx, wmin 0",
                        UI.label(displayBio).withStyle( it -> it
                            .foregroundColor(INK_FAINT)
                            .componentFont( f -> f.family("SansSerif").size(11).posture(0.15f) )
                        )
                    )
                )
                .add("aligny top",
                    UI.button("×")
                    .withStyle( it -> it
                        .backgroundColor(TRANSPARENT)
                        .foregroundColor(INK_FAINT)
                        .borderRadius(14)
                        .padding(2, 8, 2, 8)
                        .margin(0)
                        .componentFont( f -> f.family("SansSerif").size(14).weight(2) )
                    )
                    .onClick( it -> team.removeMember(p) )
                )
            );
    }

    private static UIForLabel<JLabel> avatarDot(Person p) {
        return UI.label(p.initials())
            .withStyle( p.gender(), (gender, it) -> it
                .prefSize(38, 38)
                .backgroundColor(gender.accent())
                .foregroundColor(Color.WHITE)
                .borderRadius(1000)
                .componentFont( f -> f.family("SansSerif").size(13).weight(2) )
                .padding(8)
            );
    }

    // ── Editor: details + occupation ────────────────────────────────────────
    //
    // The card is a grid rather than a MigLayout panel, because it holds another
    // grid (the form). Only a grid asks a child how tall it wants to be at the
    // width it is about to receive; a MigLayout cell reads the child's preferred
    // height instead, and for a wrapping grid that is a stale answer — which is
    // exactly how the form ends up clipped once the page stacks.

    private static final int EDITOR_REFERENCE_WIDTH = 620;

    private static UIForAnySwing<?,?> editor(Team team) {
        Val<String> selectedName = team.selectedMember().viewAsString(p -> p == null ? "" : p.name().get());

        return UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 0, 0)
            .withMinSize(0, 0)
            .withPrefSize(EDITOR_REFERENCE_WIDTH, 0)
            .withStyle( it -> it
                .backgroundColor(BG_SOFT)
                .borderRadius(14)
                .shadowColor(new Color(0, 0, 0, 120))
                .shadowBlurRadius(14)
                .shadowSpreadRadius(-4)
                .shadowOffset(0, 6)
            )
            .add(FULL_ROW, titleStrip(selectedName))
            // Rebuilds when the selected member changes; empty state otherwise.
            .add(FULL_ROW, team.selectedMember(), p -> p == null ? emptyState() : editorBody(p, team));
    }

    private static UIForAnySwing<?,?> titleStrip(Val<String> selectedName) {
        return UI.panel("fill, wrap 1, insets 16 22 14 22")
            .withStyle( it -> it.backgroundColor(BG_CARD)
                .borderRadiusAt(Corner.TOP_LEFT, 14, 14)
                .borderRadiusAt(Corner.TOP_RIGHT, 14, 14)
            )
            .add("pushx, growx, wmin 0",
                UI.label(selectedName.viewAsString(n -> n.isEmpty() ? "No member selected" : n))
                .withStyle( it -> it
                    .foregroundColor(INK)
                    .componentFont( f -> f.family("Serif").size(18).weight(2) )
                )
            )
            .add("pushx, growx, wmin 0",
                UI.label("Each field binds directly to a Var<X> on the Person view-model — no lenses involved.")
                .withStyle( it -> it
                    .foregroundColor(INK_FAINT)
                    .componentFont( f -> f.family("SansSerif").size(12).posture(0.05f) )
                )
            );
    }

    private static UIForAnySwing<?,?> emptyState() {
        // A declared height, or the card would collapse to a single line of text
        // in the grid, where every child gets exactly its preferred height.
        return UI.panel("fill").withPrefHeight(260)
            .withStyle( it -> it.backgroundColor(BG_SOFT) )
            .add("center, wmin 0",
                UI.label("Select a member from the roster, or add one.")
                .withStyle( it -> it
                    .foregroundColor(INK_FAINT)
                    .componentFont( f -> f.family("SansSerif").size(13).posture(0.1f) )
                )
            );
    }

    private static UIForAnySwing<?,?> editorBody(Person p, Team team) {
        Val<Boolean> isAlreadyLead = team.lead().viewAs(Boolean.class, led -> led != null && led.id().equals(p.id()));

        // A grid of its own, nested inside the editor card. Short fields pair up
        // two per row while the editor is wide and fall into a single column as
        // soon as it is not. Each field keeps its label to its left, so a field is
        // one indivisible unit and only their *arrangement* changes.
        return UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 12, 8)
            .withMinSize(0, 0)
            .withPrefSize(FORM_REFERENCE_WIDTH, 0) // the reference width, see below
            .withStyle( it -> it.backgroundColor(BG_SOFT).padding(12) )
            .add(FULL_ROW,  sectionTitle("Identity"))
            .add(HALF_ROW,  field("Name", inlineTextField(p.name())))
            .add(HALF_ROW,  field("Age",  inlineNumericField(p.age())))
            .add(FULL_ROW,  field("Bio",  inlineTextField(p.bio())))
            .add(FULL_ROW,  field("Gender",
                    UI.comboBox(p.gender(), g -> g.name() + "  ·  " + g.pronouns())
                    .withStyle( it -> it
                        .backgroundColor(BG_CARD)
                        .foregroundColor(INK)
                        .borderRadius(8)
                        .padding(4, 8, 4, 8)
                    )
            ))
            .add(FULL_ROW,  sectionTitle("Occupation"))
            .add(HALF_ROW,  field("Role",        inlineTextField(p.occupation().name())))
            .add(FULL_ROW,  field("Description", inlineTextField(p.occupation().description())))
            .add(FULL_ROW,
                UI.panel("insets 0, gap 8").withStyle( it -> it.backgroundColor(TRANSPARENT) )
                .add("wmin 0",
                    UI.button(isAlreadyLead.viewAsString(b -> b ? "★  Current lead" : "★  Promote to lead"))
                    .isEnabledIf(isAlreadyLead.viewAs(Boolean.class, b -> !b))
                    .withStyle( isAlreadyLead, (isLead, it) -> it
                        .backgroundColor(isLead ? new Color(255, 200, 90, 60) : ACCENT_LEAD)
                        .foregroundColor(isLead ? INK_FAINT : new Color(40, 25, 0))
                        .borderRadius(20)
                        .padding(8, 16, 8, 16)
                        .componentFont( f -> f.family("SansSerif").size(12).weight(2) )
                    )
                    .onClick( it -> team.promote(p) )
                )
                .add("wmin 0",
                    UI.button("×  Remove from team")
                    .withStyle( it -> it
                        .backgroundColor(TRANSPARENT)
                        .foregroundColor(DANGER)
                        .borderRadius(20)
                        .padding(8, 16, 8, 16)
                        .border(1, DANGER)
                        .componentFont( f -> f.family("SansSerif").size(12) )
                    )
                    .onClick( it -> team.removeMember(p) )
                )
            );
    }

    // ── The form grid ────────────────────────────────────────────────────────
    //
    // Its reference width is a little narrower than the editor card ever gets, so
    // the two-per-row band is reached while the page is side by side and left
    // behind once it stacks. Declaring it also stops this grid from reporting
    // "every field in a single row" as its width to the grid above.

    private static final int FORM_REFERENCE_WIDTH = 620;

    private static final FlowCell HALF_ROW = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(12).veryLarge(6).oversize(6) );

    /** One labelled field: an indivisible unit that the form grid arranges. */
    private static UIForAnySwing<?,?> field(String label, UIForAnySwing<?,?> input) {
        return UI.panel("fill, insets 0, gap 10", "[90!][grow]")
            .withStyle( it -> it.backgroundColor(TRANSPARENT) )
            .add(fieldLabel(label))
            .add("growx, pushx, wmin 0", input);
    }

    // ── Small reusable view fragments ────────────────────────────────────────

    private static UIForLabel<JLabel> chip(String text, Color color) {
        return chip(Val.of(text), color);
    }

    private static UIForLabel<JLabel> chip(Val<String> text, Color color) {
        return UI.label(text).withStyle( it -> it
            .backgroundColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55))
            .foregroundColor(color.brighter())
            .borderRadius(10)
            .padding(2, 10, 2, 10)
            .margin(0)
            .componentFont( f -> f.family("SansSerif").size(11).weight(2) )
        );
    }

    private static UIForLabel<JLabel> sectionTitle(String text) {
        return UI.label(text.toUpperCase(Locale.ROOT)).withStyle( it -> it
            .foregroundColor(INK_FAINT)
            .componentFont( f -> f.family("SansSerif").size(10).weight(2).spacing(0.12f) )
        );
    }

    private static UIForLabel<JLabel> fieldLabel(String text) {
        return UI.label(text).withStyle( it -> it
            .foregroundColor(INK_FAINT)
            .componentFont( f -> f.family("SansSerif").size(12) )
        );
    }

    private static UIForTextField<JTextField> inlineTextField(Var<String> property) {
        return UI.textField(property).withStyle( it -> it
            .backgroundColor(BG_CARD)
            .foregroundColor(INK)
            .borderRadius(8)
            .padding(6, 10, 6, 10)
            .border(1, new Color(80, 90, 120, 80))
        );
    }

    private static UIForTextField<JTextField> inlineNumericField(Var<Integer> property) {
        return UI.numericTextField(property).withStyle( it -> it
            .backgroundColor(BG_CARD)
            .foregroundColor(INK)
            .borderRadius(8)
            .padding(6, 10, 6, 10)
            .border(1, new Color(80, 90, 120, 80))
        );
    }
}

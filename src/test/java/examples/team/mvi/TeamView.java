package examples.team.mvi;
import java.util.Locale;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.Tuple;
import sprouts.Val;
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
 *  <h2>People Directory — a richer MVI / MVL demo</h2>
 *
 *  <p>A small team-management UI built entirely from one immutable
 *  {@link TeamViewModel} record, sliced into per-field lenses with
 *  {@code Var.zoomTo(..)} and {@code Var.viewAs(..)}. It demonstrates:</p>
 *
 *  <ul>
 *    <li><b>Dynamic list rendering</b> via {@code addAll(..)} over a
 *        {@code Var<Tuple<Person>>} lens — adding / removing
 *        members atomically updates the immutable model and the UI.</li>
 *    <li><b>Single-selection state</b> kept in the view model itself
 *        (not in any Swing component) so the editor pane on the right
 *        is just a function of the model.</li>
 *    <li><b>Nested lensing</b>: from {@code Var<TeamViewModel>} down to
 *        {@code Var<Team> → Var<Person> → Var<Occupation> → Var<String>}.
 *        Each layer is a one-line {@code zoomTo} call.</li>
 *    <li><b>Reactive styling</b>: cards highlight when selected; the lead
 *        gets a ★ chip; the avatar dot picks its colour from the member's
 *        {@link Gender}.</li>
 *    <li><b>Convergence</b>: the page is a responsive 12-column grid, so the
 *        roster sits beside the editor while there is room and stacks into a
 *        single scrolling column when there is not — no breakpoint state, no
 *        resize listener, no second version of the view.</li>
 *  </ul>
 *
 *  <p>Run {@link #main(String[])} to open the window. Adding a member
 *  selects it automatically; removing the selected member falls back to
 *  the first remaining member.</p>
 *
 *  @see examples.team.mvvm.TeamView The same UI in the classical MVVM flavor.
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

    // ── Public entry points (preserved API: `createView()` is called by tests) ─

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        Var<TeamViewModel> vm = Var.of(TeamViewModel.initialDirectory());
        UI.show("People Directory", f -> new TeamView().build(vm));
        EventProcessor.DECOUPLED.join();
    }

    public static JPanel createView() {
        Var<TeamViewModel> vm = Var.of(TeamViewModel.initialDirectory());
        return new TeamView().build(vm);
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

    private JPanel build(Var<TeamViewModel> vm) {
        Var<Team> team = vm.zoomTo(TeamViewModel::team, TeamViewModel::withTeam);
        Var<String> teamName = team.zoomTo(Team::name, Team::withName);

        return UI.panel("fill, wrap 1, insets 0, gap 0").withPrefSize(960, 650)
            .withStyle( it -> it.backgroundColor(BG) )
            .add("growx, wmin 0",      header(vm, teamName))
            .add("grow, push, wmin 0", body(vm))
            .get(JPanel.class);
    }

    // ── The page: a scrolling 12-column grid holding the two cards ───────────
    //
    // A flow grid gives every row the height of its tallest child, so once the
    // cards stack the page outgrows the window — hence the scroll pane.

    private static UIForAnySwing<?,?> body(Var<TeamViewModel> vm) {
        return UI.scrollPane( conf -> conf.fitWidth(true) )
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withVerticalScrollIncrement(24)
            .withStyle( it -> it.backgroundColor(BG).borderWidth(0).padding(0) )
            .add(
                UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 18, 18)
                .withMinSize(0, 0) // a grid reports the SUM of its children's minimums
                .withPrefSize(PAGE_REFERENCE_WIDTH, 0) // the reference width, see above
                .withStyle( it -> it.backgroundColor(BG) )
                .add(ROSTER_SPAN, roster(vm))
                .add(EDITOR_SPAN, editor(vm))
            );
    }

    // ── Header ───────────────────────────────────────────────────────────────
    //
    // Plain MigLayout, not a grid: a grid declares its reference width through an
    // explicit preferred size, and a MigLayout parent would read that height
    // literally. A grid can only carry a reference width inside another grid.

    private static UIForAnySwing<?,?> header(Var<TeamViewModel> vm, Var<String> teamName) {
        Val<Integer> memberCount = vm.viewAs(Integer.class, m -> m.team().members().size());
        Val<String>  leadName    = vm.viewAsString(m -> m.team().lead().map(Person::name).orElse("— no lead —"));

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
                UI.label("  —  an MVI / MVL team-management demo").withStyle( it -> it
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
                .onClick( it -> vm.update(TeamViewModel::addBlankMember) )
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
                .add("width 90::200", inlineTextField(teamName))
                .add(chip(memberCount.viewAsString(n -> n + " " + (n == 1 ? "member" : "members")), ACCENT))
                .add("wmin 0", chip(leadName.viewAsString(s -> "★  " + s), ACCENT_LEAD))
            );
    }

    // ── Roster: scrollable list of member cards, driven by `addAll` ─────────

    private static UIForAnySwing<?,?> roster(Var<TeamViewModel> vm) {
        Var<Tuple<Person>> members =
                vm.zoomTo(m -> m.team().members(),
                          (m, ms) -> m.withTeam(m.team().withMembers(ms)));

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
                .addAll(members, (Var<Person> personVar) -> memberCard(personVar, vm))
            );
    }

    private static UIForAnySwing<?,?> memberCard(Var<Person> personVar, Var<TeamViewModel> vm) {
        Val<Boolean> selected = vm.viewAs(Boolean.class,
                m -> personVar.get().id().equals(m.selectedMemberId()));
        Val<Boolean> isLead   = vm.viewAs(Boolean.class,
                m -> personVar.get().id().equals(m.team().leadId()));

        Val<String> name = personVar.viewAsString(p -> p.name().isEmpty() ? "Unnamed" : p.name());
        Val<String> role = personVar.viewAsString(p -> p.occupation().name().isEmpty()
                ? "—" : p.occupation().name());
        Val<String> bio  = personVar.viewAsString(Person::bio);

        return UI.panel("fill, insets 0, gap 0")
            .withStyle( selected, (isSelected, it) -> it
                .backgroundColor(isSelected ? BG_CARD_HI : BG_CARD)
                .borderRadius(12)
                .borderAt(Edge.LEFT, 3, isSelected ? ACCENT : TRANSPARENT)
                .margin(0, 12, 8, 12)
                .padding(0)
                .cursor(Cursor.HAND)
            )
            .onMouseClick( it -> vm.update(m -> m.select(personVar.get().id())) )
            .add("grow, push",
                UI.panel("fill, insets 12 14 12 14, gap 10")
                .withStyle( it -> it.backgroundColor(TRANSPARENT) )
                .add(avatarDot(personVar))
                .add("grow, push, wmin 0",
                    UI.panel("fill, wrap 1, insets 0, gap 2")
                    .withStyle( it -> it.backgroundColor(TRANSPARENT) )
                    .add("growx, wmin 0",
                        UI.panel("insets 0, gap 6, hidemode 3").withStyle( it -> it.backgroundColor(TRANSPARENT) )
                        // The name yields before the ★ LEAD chip does, since the chip
                        // is a fixed short badge while a name can be arbitrarily long.
                        .add("wmin 0",
                            UI.label(name).withStyle( it -> it
                                .foregroundColor(INK)
                                .componentFont( f -> f.family("SansSerif").size(14).weight(2) )
                            )
                        )
                        // Bound, not `applyIf(isLead.get(), ..)`: promoting a member
                        // leaves the roster tuple untouched, so these cards are never
                        // rebuilt and a snapshot of the flag would go stale.
                        .add(chip("★ LEAD", ACCENT_LEAD).isVisibleIf(isLead))
                    )
                    .add("growx, wmin 0",
                        UI.label(role).withStyle( it -> it
                            .foregroundColor(ACCENT)
                            .componentFont( f -> f.family("SansSerif").size(11) )
                        )
                    )
                    .add("growx, wmin 0",
                        UI.label(bio.viewAsString(s -> s.isEmpty() ? "" : "“" + s + "”"))
                        .withStyle( it -> it
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
                    .onClick( it -> vm.update(m -> m.removeMember(personVar.get().id())) )
                )
            );
    }

    private static UIForLabel<JLabel> avatarDot(Var<Person> personVar) {
        Val<String> initials = personVar.viewAsString(Person::initials);
        return UI.label(initials)
            .withStyle( personVar, (person, it) -> it
                .prefSize(38, 38)
                .backgroundColor(person.gender().accent())
                .foregroundColor(Color.WHITE)
                .borderRadius(1000)
                .componentFont( f -> f.family("SansSerif").size(13).weight(2) )
                .padding(8)
            );
    }

    // ── Editor: details + occupation, both driven by lenses ─────────────────
    //
    // The card is a grid rather than a MigLayout panel, because it holds another
    // grid (the form). Only a grid asks a child how tall it wants to be at the
    // width it is about to receive; a MigLayout cell reads the child's preferred
    // height instead, and for a wrapping grid that is a stale answer — which is
    // exactly how the form ends up clipped once the page stacks.

    private static final int EDITOR_REFERENCE_WIDTH = 620;

    private static UIForAnySwing<?,?> editor(Var<TeamViewModel> vm) {
        Val<Boolean> hasSelection = vm.viewAs(Boolean.class, m -> m.selectedMemberId() != null);
        Val<String>  selectedName = vm.viewAsString(m -> m.selectedMember().map(Person::name).orElse(""));

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
            // Only render the form when a member is selected, a placeholder otherwise.
            .add(FULL_ROW, hasSelection, has -> has ? editorBody(vm) : emptyState());
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
                UI.label("Edit identity and occupation below — every keystroke updates the immutable view model.")
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

    private static UIForAnySwing<?,?> editorBody(Var<TeamViewModel> vm) {
        // Lens chain: VM → Team → selected Person → its individual fields.
        //
        // The "selected Person" lens uses the selectedMemberId to find the
        // matching person in the team's tuple; updating it rebuilds the
        // tuple with the new person in place.
        Var<Person> selected = vm.zoomTo(
            m -> m.selectedMember().orElseGet(Person::new),
            (m, updated) -> {
                Team t = m.team();
                int idx = -1;
                for (int i = 0; i < t.members().size(); i++)
                    if (t.members().get(i).id().equals(updated.id())) { idx = i; break; }
                if (idx < 0) return m;
                return m.withTeam(t.withMembers(t.members().setAt(idx, updated)));
            }
        );

        Var<String>     name       = selected.zoomTo(Person::name, Person::withName);
        Var<Integer>    age        = selected.zoomTo(Person::age,  Person::withAge);
        Var<String>     bio        = selected.zoomTo(Person::bio,  Person::withBio);
        Var<Gender>     gender     = selected.zoomTo(Person::gender, Person::withGender);
        Var<Occupation> occupation = selected.zoomTo(Person::occupation, Person::withOccupation);
        Var<String>     roleName   = occupation.zoomTo(Occupation::name, Occupation::withName);
        Var<String>     roleDesc   = occupation.zoomTo(Occupation::description, Occupation::withDescription);

        Val<Boolean> isAlreadyLead = vm.viewAs(Boolean.class,
                m -> selected.get().id().equals(m.team().leadId()));

        // A grid of its own, nested inside the editor card. Short fields pair up
        // two per row while the editor is wide and fall into a single column as
        // soon as it is not. Each field keeps its label to its left, so a field is
        // one indivisible unit and only their *arrangement* changes.
        return UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 12, 8)
            .withMinSize(0, 0)
            .withPrefSize(FORM_REFERENCE_WIDTH, 0) // the reference width, see below
            .withStyle( it -> it.backgroundColor(BG_SOFT).padding(12) )
            .add(FULL_ROW,  sectionTitle("Identity"))
            .add(HALF_ROW,  field("Name", inlineTextField(name)))
            .add(HALF_ROW,  field("Age",  inlineNumericField(age)))
            .add(FULL_ROW,  field("Bio",  inlineTextField(bio)))
            .add(FULL_ROW,  field("Gender",
                    UI.comboBox(gender, g -> g.name() + "  ·  " + g.pronouns())
                    .withStyle( it -> it
                        .backgroundColor(BG_CARD)
                        .foregroundColor(INK)
                        .borderRadius(8)
                        .padding(4, 8, 4, 8)
                    )
            ))
            .add(FULL_ROW,  sectionTitle("Occupation"))
            .add(HALF_ROW,  field("Role",        inlineTextField(roleName)))
            .add(FULL_ROW,  field("Description", inlineTextField(roleDesc)))
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
                    .onClick( it -> vm.update(m -> m.promote(selected.get().id())) )
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
                    .onClick( it -> vm.update(m -> m.removeMember(selected.get().id())) )
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

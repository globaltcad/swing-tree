package examples.team.mvi;
import java.util.Locale;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
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
 *  </ul>
 *
 *  <p>Run {@link #main(String[])} to open the window. Adding a member
 *  selects it automatically; removing the selected member falls back to
 *  the first remaining member.</p>
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

    private JPanel build(Var<TeamViewModel> vm) {
        Var<Team> team = vm.zoomTo(TeamViewModel::team, TeamViewModel::withTeam);
        Var<String> teamName = team.zoomTo(Team::name, Team::withName);

        return UI.panel("fill, wrap 1, insets 0, gap 0").withPrefSize(960, 640)
            .withStyle( it -> it.backgroundColor(BG) )
            .add("growx",     header(vm, teamName))
            .add("grow, push",
                UI.panel("fill, insets 18, gap 18")
                .withStyle( it -> it.backgroundColor(BG) )
                .add("growy, width 360!",  roster(vm))
                .add("grow, push",         editor(vm))
            )
            .get(JPanel.class);
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> header(Var<TeamViewModel> vm, Var<String> teamName) {
        Val<Integer> memberCount = vm.viewAs(Integer.class, m -> m.team().members().size());
        Val<String>  leadName    = vm.viewAsString(m -> m.team().lead().map(Person::name).orElse("— no lead —"));

        return UI.panel("fill, insets 16 26 16 26")
            .withStyle( it -> it
                .backgroundColor(BG_SIDEBAR)
                .borderAt(Edge.BOTTOM, 1, new Color(60, 70, 100))
            )
            .add("pushx, growx",
                UI.panel("fill, wrap 1, insets 0").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                .add("growx",
                    UI.html("<span style='color:#f3f4ff;font-family:serif;font-size:21px;'>People Directory</span>" +
                            "<span style='color:#9aa6c8;font-style:italic;font-size:12px;'>" +
                            "&nbsp;&nbsp;—&nbsp;&nbsp;an MVI / MVL team-management demo</span>")
                )
                .add("growx, gaptop 4",
                    UI.panel("insets 0, gap 12").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                    .add(
                        UI.label("Team:").withStyle( it -> it
                            .foregroundColor(INK_FAINT)
                            .componentFont( f -> f.family("SansSerif").size(11) )
                        )
                    )
                    .add("width 200!", inlineTextField(teamName))
                    .add(chip(memberCount.viewAsString(n -> n + " " + (n == 1 ? "member" : "members")), ACCENT))
                    .add(chip(leadName.viewAsString(s -> "★  " + s), ACCENT_LEAD))
                )
            )
            .add("shrinkx",
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
            );
    }

    // ── Roster: scrollable list of member cards, driven by `addAll` ─────────

    private static UIForAnySwing<?,?> roster(Var<TeamViewModel> vm) {
        Var<sprouts.Tuple<Person>> members =
                vm.zoomTo(m -> m.team().members(),
                          (m, ms) -> m.withTeam(m.team().withMembers(ms)));

        return UI.panel("fill, wrap 1, insets 0, gap 0")
            .withStyle( it -> it.backgroundColor(BG_SIDEBAR).borderRadius(14) )
            .add("growx",
                UI.panel("fill, insets 14 18 10 18").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                .add("pushx, growx",
                    UI.label("ROSTER").withStyle( it -> it
                        .foregroundColor(INK_FAINT)
                        .componentFont( f -> f.family("SansSerif").size(10).weight(2).spacing(0.12f) )
                    )
                )
            )
            .add("grow, push",
                UI.scrollPanels()
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
                .borderAt(Edge.LEFT, 3, isSelected ? ACCENT : new Color(0,0,0,0))
                .margin(0, 12, 8, 12)
                .padding(0)
                .cursor(Cursor.HAND)
            )
            .onMouseClick( it -> vm.update(m -> m.select(personVar.get().id())) )
            .add("grow, push",
                UI.panel("fill, insets 12 14 12 14, gap 10")
                .withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                .add(avatarDot(personVar))
                .add("grow, push, wrap",
                    UI.panel("fill, wrap 1, insets 0, gap 2")
                    .withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                    .add("growx",
                        UI.panel("insets 0, gap 6").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                        .add(
                            UI.label(name).withStyle( it -> it
                                .foregroundColor(INK)
                                .componentFont( f -> f.family("SansSerif").size(14).weight(2) )
                            )
                        )
                        .applyIf(isLead.get(),    ui -> ui.add(chip("★ LEAD", ACCENT_LEAD)))
                    )
                    .add("growx",
                        UI.label(role).withStyle( it -> it
                            .foregroundColor(ACCENT)
                            .componentFont( f -> f.family("SansSerif").size(11) )
                        )
                    )
                    .add("growx",
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
                        .backgroundColor(new Color(0,0,0,0))
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

    private static UIForAnySwing<?,?> avatarDot(Var<Person> personVar) {
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

    private static UIForAnySwing<?,?> editor(Var<TeamViewModel> vm) {
        Val<Boolean> hasSelection = vm.viewAs(Boolean.class, m -> m.selectedMemberId() != null);
        Val<String>  selectedName = vm.viewAsString(m -> m.selectedMember().map(Person::name).orElse(""));

        return UI.panel("fill, wrap 1, insets 0, gap 0")
            .withStyle( it -> it
                .backgroundColor(BG_SOFT)
                .borderRadius(14)
                .shadowColor(new Color(0, 0, 0, 120))
                .shadowBlurRadius(14)
                .shadowSpreadRadius(-4)
                .shadowOffset(0, 6)
            )
            // Title strip
            .add("growx",
                UI.panel("fill, insets 16 22 14 22")
                .withStyle( it -> it.backgroundColor(BG_CARD)
                    .borderRadiusAt(Corner.TOP_LEFT, 14, 14)
                    .borderRadiusAt(Corner.TOP_RIGHT, 14, 14)
                )
                .add("pushx, growx, wrap",
                    UI.label(selectedName.viewAsString(n -> n.isEmpty() ? "No member selected" : n))
                    .withStyle( it -> it
                        .foregroundColor(INK)
                        .componentFont( f -> f.family("Serif").size(18).weight(2) )
                    )
                )
                .add("pushx, growx",
                    UI.label("Edit identity and occupation below — every keystroke updates the immutable view model.")
                    .withStyle( it -> it
                        .foregroundColor(INK_FAINT)
                        .componentFont( f -> f.family("SansSerif").size(12).posture(0.05f) )
                    )
                )
            )
            // Body — only render when a member is selected, otherwise show a placeholder.
            .add("grow, push", hasSelection, has -> has ? editorBody(vm) : emptyState());
    }

    private static UIForAnySwing<?,?> emptyState() {
        return UI.panel("fill").withStyle( it -> it.backgroundColor(BG_SOFT) )
            .add("center",
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

        return UI.panel("fill, wrap 1, insets 20 24 20 24, gap 16")
            .withStyle( it -> it.backgroundColor(BG_SOFT) )
            .add("growx", sectionTitle("Identity"))
            .add("growx", fieldGrid()
                .add(fieldLabel("Name"))
                .add("growx, pushx, wrap", inlineTextField(name))
                .add(fieldLabel("Age"))
                .add("growx, pushx, wrap", inlineNumericField(age))
                .add(fieldLabel("Bio"))
                .add("growx, pushx, wrap", inlineTextField(bio))
                .add(fieldLabel("Gender"))
                .add("growx, pushx, wrap",
                    UI.comboBox(gender, g -> g.name() + "  ·  " + g.pronouns())
                    .withStyle( it -> it
                        .backgroundColor(BG_CARD)
                        .foregroundColor(INK)
                        .borderRadius(8)
                        .padding(4, 8, 4, 8)
                    )
                )
            )
            .add("growx, gaptop 6", sectionTitle("Occupation"))
            .add("growx", fieldGrid()
                .add(fieldLabel("Role"))
                .add("growx, pushx, wrap", inlineTextField(roleName))
                .add(fieldLabel("Description"))
                .add("growx, pushx, wrap", inlineTextField(roleDesc))
            )
            .add("growx, gaptop 10",
                UI.panel("insets 0, gap 8").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                .add(
                    UI.button(isAlreadyLead.viewAsString(b -> b ? "★  Current lead" : "★  Promote to lead"))
                    .isEnabledIf(isAlreadyLead.viewAs(Boolean.class, b -> !b))
                    .withStyle( it -> it
                        .backgroundColor(isAlreadyLead.get() ? new Color(255, 200, 90, 60) : ACCENT_LEAD)
                        .foregroundColor(isAlreadyLead.get() ? INK_FAINT : new Color(40, 25, 0))
                        .borderRadius(20)
                        .padding(8, 16, 8, 16)
                        .componentFont( f -> f.family("SansSerif").size(12).weight(2) )
                    )
                    .onClick( it -> vm.update(m -> m.promote(selected.get().id())) )
                )
                .add(
                    UI.button("×  Remove from team")
                    .withStyle( it -> it
                        .backgroundColor(new Color(0,0,0,0))
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

    // ── Small reusable view fragments ────────────────────────────────────────

    private static UIForAnySwing<?,?> chip(Val<String> text, Color color) {
        return UI.label(text).withStyle( it -> it
            .backgroundColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55))
            .foregroundColor(color.brighter())
            .borderRadius(10)
            .padding(2, 10, 2, 10)
            .margin(0)
            .componentFont( f -> f.family("SansSerif").size(11).weight(2) )
        );
    }

    private static UIForAnySwing<?,?> chip(String text, Color color) {
        return UI.label(text).withStyle( it -> it
            .backgroundColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55))
            .foregroundColor(color.brighter())
            .borderRadius(10)
            .padding(2, 10, 2, 10)
            .margin(0)
            .componentFont( f -> f.family("SansSerif").size(11).weight(2) )
        );
    }

    private static UIForAnySwing<?,?> sectionTitle(String text) {
        return UI.label(text.toUpperCase(Locale.ROOT)).withStyle( it -> it
            .foregroundColor(INK_FAINT)
            .componentFont( f -> f.family("SansSerif").size(10).weight(2).spacing(0.12f) )
        );
    }

    private static swingtree.UIForPanel<javax.swing.JPanel> fieldGrid() {
        return UI.panel("fill, wrap 2, insets 0, gap 10 6", "[90!][grow]")
            .withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) );
    }

    private static swingtree.UIForLabel<javax.swing.JLabel> fieldLabel(String text) {
        return UI.label(text).withStyle( it -> it
            .foregroundColor(INK_FAINT)
            .componentFont( f -> f.family("SansSerif").size(12) )
        );
    }

    private static swingtree.UIForTextField<javax.swing.JTextField> inlineTextField(Var<String> property) {
        return UI.textField(property).withStyle( it -> it
            .backgroundColor(BG_CARD)
            .foregroundColor(INK)
            .borderRadius(8)
            .padding(6, 10, 6, 10)
            .border(1, new Color(80, 90, 120, 80))
        );
    }

    private static swingtree.UIForTextField<javax.swing.JTextField> inlineNumericField(Var<Integer> property) {
        return UI.numericTextField(property).withStyle( it -> it
            .backgroundColor(BG_CARD)
            .foregroundColor(INK)
            .borderRadius(8)
            .padding(6, 10, 6, 10)
            .border(1, new Color(80, 90, 120, 80))
        );
    }
}
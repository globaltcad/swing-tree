package examples.team.mvvm;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.Val;
import sprouts.Vals;
import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForLabel;
import swingtree.UIForPanel;
import swingtree.UIForTextField;
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

    private JPanel build(Team team) {
        return UI.panel("fill, wrap 1, insets 0, gap 0").withPrefSize(960, 640)
            .withStyle( it -> it.backgroundColor(BG) )
            .add("growx",     header(team))
            .add("grow, push",
                UI.panel("fill, insets 18, gap 18")
                .withStyle( it -> it.backgroundColor(BG) )
                .add("growy, width 360!",  roster(team))
                .add("grow, push",         editor(team))
            )
            .get(JPanel.class);
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> header(Team team) {
        Val<Integer> memberCount = team.members().viewSize();
        Val<String>  leadName    = team.lead().viewAsString(p -> p == null ? "— no lead —" : p.name().get());

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
                            "&nbsp;&nbsp;—&nbsp;&nbsp;the classical-MVVM twin of the MVI example</span>")
                )
                .add("growx, gaptop 4",
                    UI.panel("insets 0, gap 12").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                    .add(
                        UI.label("Team:").withStyle( it -> it
                            .foregroundColor(INK_FAINT)
                            .componentFont( f -> f.family("SansSerif").size(11) )
                        )
                    )
                    .add("width 200!", inlineTextField(team.name()))
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
                .onClick( it -> team.addBlankMember() )
            );
    }

    // ── Roster: scrollable list of member cards, driven by Vars<Person> ────

    private static UIForAnySwing<?,?> roster(Team team) {
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
                .borderAt(Edge.LEFT, 3, isSelected ? ACCENT : new Color(0,0,0,0))
                .margin(0, 12, 8, 12)
                .padding(0)
                .cursor(Cursor.HAND)
            )
            .onMouseClick( it -> team.selectedMember().set(p) )
            .add("grow, push",
                UI.panel("fill, insets 12 14 12 14, gap 10")
                .withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                .add(avatarDot(p))
                .add("grow, push, wrap",
                    UI.panel("fill, wrap 1, insets 0, gap 2")
                    .withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                    .add("growx",
                        UI.panel("insets 0, gap 6").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
                        .add(
                            UI.label(displayName).withStyle( it -> it
                                .foregroundColor(INK)
                                .componentFont( f -> f.family("SansSerif").size(14).weight(2) )
                            )
                        )
                        .applyIf(isLead.get(), ui -> ui.add(chip("★ LEAD", ACCENT_LEAD)))
                    )
                    .add("growx",
                        UI.label(displayRole).withStyle( it -> it
                            .foregroundColor(ACCENT)
                            .componentFont( f -> f.family("SansSerif").size(11) )
                        )
                    )
                    .add("growx",
                        UI.label(displayBio).withStyle( it -> it
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

    private static UIForAnySwing<?,?> editor(Team team) {
        Val<String>  selectedName = team.selectedMember().viewAsString(p -> p == null ? "" : p.name().get());

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
                    UI.label("Each field binds directly to a Var<X> on the Person view-model — no lenses involved.")
                    .withStyle( it -> it
                        .foregroundColor(INK_FAINT)
                        .componentFont( f -> f.family("SansSerif").size(12).posture(0.05f) )
                    )
                )
            )
            // Body — rebuilds when the selected member changes; empty state otherwise.
            .add("grow, push", team.selectedMember(), p -> p == null ? emptyState() : editorBody(p, team));
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

    private static UIForAnySwing<?,?> editorBody(Person p, Team team) {
        Val<Boolean> isAlreadyLead = team.lead().viewAs(Boolean.class, led -> led != null && led.id().equals(p.id()));

        return UI.panel("fill, wrap 1, insets 20 24 20 24, gap 16")
            .withStyle( it -> it.backgroundColor(BG_SOFT) )
            .add("growx", sectionTitle("Identity"))
            .add("growx", fieldGrid()
                .add(fieldLabel("Name"))
                .add("growx, pushx, wrap", inlineTextField(p.name()))
                .add(fieldLabel("Age"))
                .add("growx, pushx, wrap", inlineNumericField(p.age()))
                .add(fieldLabel("Bio"))
                .add("growx, pushx, wrap", inlineTextField(p.bio()))
                .add(fieldLabel("Gender"))
                .add("growx, pushx, wrap",
                    UI.comboBox(p.gender(), g -> g.name() + "  ·  " + g.pronouns())
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
                .add("growx, pushx, wrap", inlineTextField(p.occupation().name()))
                .add(fieldLabel("Description"))
                .add("growx, pushx, wrap", inlineTextField(p.occupation().description()))
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
                    .onClick( it -> team.promote(p) )
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
                    .onClick( it -> team.removeMember(p) )
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

    private static UIForLabel<JLabel> sectionTitle(String text) {
        return UI.label(text.toUpperCase()).withStyle( it -> it
            .foregroundColor(INK_FAINT)
            .componentFont( f -> f.family("SansSerif").size(10).weight(2).spacing(0.12f) )
        );
    }

    private static UIForPanel<JPanel> fieldGrid() {
        return UI.panel("fill, wrap 2, insets 0, gap 10 6", "[90!][grow]")
            .withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) );
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
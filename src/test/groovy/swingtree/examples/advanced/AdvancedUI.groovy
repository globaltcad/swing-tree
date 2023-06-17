package swingtree.examples.advanced
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme
import swingtree.UI

import javax.swing.*
import java.awt.*
import java.util.function.Function

class AdvancedUI {
    static JPanel of(JFrame frame) {
        UIManager.setLookAndFeel(new FlatMaterialDesignDarkIJTheme())
        UI.panel("fill, ins 5")
        .add("grow",
            UI.splitPane(UI.Align.VERTICAL).withDividerAt(565)
            .add(
                UI.panel("fill, ins 0")
                .add("push, grow, span, wrap",
                    UI.scrollPane().add(
                        UI.panel("fill, insets 2")
                        .add("span, shrinky, growx, wrap",
                            UI.panel("fill, ins 10, ")
                            .add("align left, shrink",
                                UI.button("Help").withProperty("JButton.buttonType", "help")
                                .onClick({
                                    UI.info(
                                        "APIC, sot ron Atvan Protos ron Inficus Contri,\n" +
                                        "sini currentu activus widine ru regi!\n" +
                                        "Ple atice accoto to de APIC specifica bellos."
                                    )
                                })
                            )
                            .add("growx, shrinky, pushx, alignx left", UI.label("Acti proticus: APIC"))
                            .add("growx, shrinky, pushx, alignx right, wrap",
                               UI.label("").doUpdates(100, {
                                   it.component.setText(new Date().toString())
                               })
                            )
                        )
                        .add("push, grow, span, wrap",
                            UI.panel("fill, insets 3")
                            .add("height 50:150:200, width 50:550:550, grow, push",
                               UI.scrollPane()
                               .with(UI.ScrollBarPolicy.NEVER)
                               .with(UI.ScrollBarPolicy.AS_NEEDED)
                               .add(UI.editorPane().withForeground(Color.DARK_GRAY).withText((Data.explain.trim().replace("\n", " "))).isEditableIf(false)))
                        )
                        .add("width 480!, shrink, aligny bottom",
                            UI.of(new JSlider(SwingConstants.HORIZONTAL)).peek({
                                Hashtable labelTable = new Hashtable();
                                9.times {level -> labelTable.put( level*12, new JLabel(String.valueOf(level)) )}
                                it.setLabelTable( labelTable );
                                it.setPaintLabels(true);it.setPaintTicks(true);
                                it.setMajorTickSpacing(12);
                                it.setMinorTickSpacing(6);
                            })
                            .onChange( s -> {
                                s.find(JLabel,"APIC-label")
                                .ifPresent(l->{
                                    l.setText(((8*s.component.value/(8*12))+"   ").substring(0,3))
                                    l.setForeground(new Color((float)(s.component.value/100),(float)(1-s.component.value/100),0f,1f))
                                })
                                s.find(JTabbedPane, "APIC-Tabs")
                                .ifPresent(p -> p.setSelectedIndex((int)(8*s.component.value/(8*12))) )
                            })
                        )
                        .add("growx, shrinky, alignx right, aligny bottom", UI.label("pic"))
                        .add("growx, shrinky, alignx left, aligny bottom", UI.label("6.0").id("APIC-label").withProperty("FlatLaf.styleClass", "h0"))
                        .add("growx, shrinky, pushx, alignx right, aligny bottom", UI.label(""))
                    )
                )
                .add("shrinky, growx, aligny bottom",
                    UI.scrollPane().add(
                        UI.tabbedPane().id("APIC-Tabs")
                        .apply( p -> {
                            Data.pic.size().times {
                                i -> p.add(UI.tab("APIC$i").withHeader(new JButton("x")).add(
                                    UI.panel("fill")
                                    .apply(inner -> {
                                        Data.descr.keySet().each {label -> {
                                            var text = Data.pic[i][label]
                                            text = (text.isBlank() ? "Nullae mensurae." : text)
                                            inner
                                            .add("width 50:425:625, pushx, growx, shrinky",
                                                UI.scrollPane()
                                                .with(UI.ScrollBarPolicy.NEVER)
                                                .add(UI.editorPane().withText(text).isEditableIf(false))
                                            )
                                            .add("align right, shrink", UI.button(label).withProperty("JButton.buttonType", "roundRect").isEnabledIf(false))
                                            .add("align right, shrink, wrap",
                                                UI.button("Help").withProperty("JButton.buttonType", "help")
                                                .onClick({UI.info(Data.descr[label])})
                                            )
                                        }}
                                    })
                                ))
                            }
                        })
                    )
                )
            )
            .add(
                UI.scrollPane().add(
                    UI.panel("fill")
                    .add("east, grow",
                        UI.panel("fill, insets 0")
                        .add("aligny top, shrinky, growx, wrap, span",
                            UI.splitButton("Iunctio").id("con-split-button")
                            .onButtonClick({ it.displayCurrentItemText() } )
                            .onSplitClick({ it.displayCurrentItemText() })
                            .onSelection({it.displayCurrentItemText().unselectAllItems().selectCurrentItem()})
                            .add(UI.splitRadioItem("S-194.245.243.1"))
                            .add(UI.splitRadioItem("S-192.153.122.2"))
                            .add(UI.splitRadioItem("S-194.245.243.6"))
                            .add(UI.splitRadioItem("S-192.153.122.80"))
                        )
                        .add("alignx right, shrinkx, growy, pushy",
                            UI.of(new JSlider(SwingConstants.VERTICAL)).peek({
                                it.setPaintLabels(true);it.setPaintTicks(true);
                                it.setMajorTickSpacing(10);
                                it.setMinorTickSpacing(1);
                            })
                            .doUpdates(1250, it -> it.component.setValue(it.component.value + (new Random().nextInt()%5)-((it.component.value-40)/25) as int))
                        )
                        .add("alignx right, shrinkx, growy, pushy, wrap",
                            UI.progressBar(UI.Align.VERTICAL, 0, 100)
                            .peek({it.setValue(68); it.setString("%"); it.setStringPainted(true)})
                            .doUpdates(25000, it -> it.component.setValue(it.component.value - 1))
                        )
                        .add("aligny bottom, shrinky, growx, span 2",
                            UI.button("Distros").withProperty("JButton.buttonType", "roundRect")
                        )
                        .add("aligny bottom, shrinky, growx, span 1",
                            UI.button("Help").withProperty("JButton.buttonType", "help").onClick({
                                UI.info("NICP - Natio Intradi Contori Portalis, Enforticus Unice Acare Setjim ron NICS - Natio Intradi ContContorirol Setjim.")
                            })
                        )
                    )
                    .add("shrinky, growx", UI.of(new JSlider()).onChange( it -> it.find(JSpinner,"Light-Spinner").ifPresent(s->s.setValue(it.component.value))))
                    .add("shrink", UI.button("Help").isEnabledIf(false).withProperty("JButton.buttonType", "help"))
                    .add("shrinky, growx, wrap", UI.spinner().id("Light-Spinner"))
                    .add("span, wrap, growx, shrinky",
                        UI.panel("fill, insets 10","[grow][shrink]")
                        .withBackground(new Color(100,100,100,100))
                        .add("cell 0 0",
                            UI.boldLabel("Usus AF-8c3ae")
                            .makeLinkTo("https://github.com")
                        )
                        .add("cell 0 1, grow, shrinky",
                            UI.panel("fill, insets 0","[grow][shrink]")
                            .withBackground(new Color(100,100,100,100))
                            .add("cell 0 0, aligny top, grow x, grow y",
                                UI.panel("fill, insets 7","grow")
                                .withBackground(Color.GRAY)
                                .add( "span",
                                    UI.label("<html><div style=\"width:275px;\">Cras sem ex, maximus eu purus rutrum, finibus gravida turpis! Confidicus identare.</div></html>")
                                )
                                .add("shrink", UI.label("Praenomen").withForeground(Color.DARK_GRAY))
                                .add("grow, span 2", UI.textField("Quintus"))
                                .add("gap unrelated, shrink", UI.label("Cognomen").withForeground(Color.DARK_GRAY))
                                .add("wrap, grow, span 2", UI.textField("Maximus"))
                                .add("shrink", UI.label("Signum").withForeground(Color.DARK_GRAY))
                                .add("grow, span 3", UI.passwordField("X829nke=KJ?+++"))
                                .add("grow",
                                    UI.splitButton("LOGOS")
                                    .onSelection(it0 -> it0.displayCurrentItemText().unselectAllItems().selectCurrentItem())
                                    .add(UI.splitRadioItem("LOGOS - RXX"))
                                    .add(UI.splitRadioItem("LOGOS - RWW"))
                                    .add(UI.splitRadioItem("LOGUS"))
                                    .add(UI.splitRadioItem("DESTROS"))
                                )
                            )
                            .add("cell 1 0, growy",
                                UI.panel("fill", "[grow]")
                                .withBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY))
                                .add("cell 0 0, aligny top, growx", UI.button("REFRIDE"))
                                .add("cell 0 1, aligny top, alignx center",
                                    UI.button("<html><i>...</i><html>")
                                    .with(UI.Cursor.HAND)
                                    .makePlain()
                                    .doUpdates(1234, it -> {
                                        var raw = it.component.text
                                        var count = it.component.text.length() - raw.replace('.', '').length()-1
                                        var text = "."*(1+((count+1)%4))
                                        it.component.text = "<html><i>$text</i><html>"
                                    })
                                )
                                .add( "cell 0 2, aligny bottom, span, shrink", UI.label("Permi Laval:"))
                                .add("cell 0 3, aligny bottom, span, grow", UI.textArea("5-RXXC").isEditableIf(false))
                            )
                        )
                        .add("cell 0 2, grow",
                            UI.panel("fill, insets 0 0 0 0","[grow][grow][grow]")
                            .add("cell 1 0", UI.label("U: Aiore-enic-6RWC").withForeground(Color.DARK_GRAY))
                            .add("cell 2 0", UI.label("NICP-v67.2.4").withForeground(Color.DARK_GRAY))
                            .add("cell 3 0", UI.label("04-11-2025").withForeground(Color.DARK_GRAY))
                        )
                    )
                    .add("grow, span",
                        UI.panel("fill, ins 5").withBorderTitled("Kennros 104")
                        .add("grow, span",
                            UI.panel().withFlowLayout()
                            .add(UI.button("1")).add(UI.button("2")).add(UI.button("3")).add(UI.button("4"))
                            .add(UI.button("5")).add(UI.button("6")).add(UI.button("7")).add(UI.button("8"))
                            .add(UI.button("9")).add(UI.button("0")).add(UI.button("#")).add(UI.button("@"))
                        )
                        .add("width 200:400:500, grow, push, span, alignx center",
                           UI.panel().withFlowLayout().withEmptyBorderTitled("Perri:", 1)
                           .withMinSize(200, 100)
                           .apply( p -> {
                               ('A'..'Z').each {l ->  p.add(UI.button(l)) }
                           })
                        )
                    )
                    .add("push, span", UI.separator())
                    .add("aligny bottom, height 10:100%:100, growx, growy, span, wrap",
                        UI.scrollPane().add(
                            UI.textArea("...iuncte...").isEditableIf(false)
                            .doUpdates(1000, it -> {
                                var text = it.component.text;
                                var split = Arrays.stream(text.split("\\n")).filter(s->s.length()>0).findFirst().get()
                                text = text.substring(split.length())+"\n\$>"
                                50.times { ii ->
                                    var a = ('a' as char) as int
                                    Math.abs(new Random().nextInt(30)).times { i ->
                                        a += Math.abs(new Random().nextInt(30))
                                    }
                                    text += a as char
                                }
                                if ( text.length() > 1000 ) text = text.substring(100)
                                it.component.text = text
                            })
                            .onMouseClick({
                                UI.info("Atress deni! Permi laval (5-RXXC) insufficicus.")
                            })
                        )
                    )
                    .add("aligny bottom, shrinky, span, growx, wrap",
                        UI.textField("(base)\$> loginctl unlock-session 3").isEditableIf(false).onMouseClick({
                            UI.info("Atress deni! Permi laval (5-RXXC) insufficicus.")
                        })
                    )
                    .add("align center, span",
                        UI.label("AWACS - Aquisato - Cc - NICS - 03|07|2013|v3.2.7")
                        .withForeground(Color.GRAY)
                    )
                )
            )
        )
        .get(JPanel)
    }

    static class Data
    {
        static descr = [
                'SI':"<html><b>SI - Setjim Identification</b> <br> Temporibus bi mae enforced upon a subjecus in mare bitus on es  varitus<br> APIC laval medres and lower laval medres fo eti recusandae statute type.</html>",
                'QM':"<html><b>QM - Quarantine Temporibus </b> <br> Setjim in mare bi mae put undea quarantine bitus on es  varitus<br> APIC laval medres and lower laval medres fo eti recusandae statute type.</html>",
                'MM':"<html><b>MM - Nam Temporibus   </b> <br> Setjim in mare bi mae reluctus bitus on es  varitus<br> APIC laval medres an lower laval medres fo eti recusandae statute type.</html>",
                'MR':"<html><b>MR - Nam Restrictions</b> <br> Setjim in mare bi mae put undea quarus bitus on es  varitus<br> APIC laval medres an lower laval medres fo eti recusandae statute type.</html>",
                'SC':"<html><b>SC - Sekes Contaminus</b> <br> Setjim bi mae contained or altered bitus on es  varitus<br> APIC laval medresan lower laval medres fo eti recusandae statute type.</html>",
                '*': "<html><b>Addites Temporibus oc notis</b></html>"
        ]
        static pic = [[
                       'SI':"",
                       'QM':"",
                       'MM':"",
                       'MR':"",
                       'SC':"",
                       '*':""
               ], [//1
                   'SI':"Ositi etes resultate bitus on disecu classifi.",
                   'QM':"",
                   'MM':"",
                   'MR':"",
                   'SC':"",
                   '*':"maskes, glofis, sulti, disince, etesing rut, wil mane"
               ], [//2
                   'SI':"Ositi etes resultate bitus on disecu classifi.",
                   'QM':"Ununsivo quarus delectus, sujeca bi mae transitu undea quibusdam  .",
                   'MM':"",
                   'MR':"",
                   'SC':"",
                   '*':""
               ], [//3
                   'SI':"Tractus traci bitus on disecu classificus.",
                   'QM':"Ununsivo quarus delectus  , sujeca bi mae transitu undea quibusdam  .",
                   'MM':"",
                   'MR':"",
                   'SC':"",
                   '*':""
               ], [//4
                   'SI':"Tractus traci bitus on disecu classificus",
                   'QM':"Persed quarus, sujeca bi mae transitu undea quibusdam  , recusandae kedi etu gedu if infecu pectee.",
                   'MM':"Evacus fo hingi dictate nobis  for sujeca with negative etes resultate.",
                   'MR':"Et harum movemento onlea.",
                   'SC':"",
                   '*':""
               ], [//5
                   'SI':"Nemo enim ipsam voluptatem!",
                   'QM':"Persed quarus, sujeca bi mae transitu undea quibusdam  .",
                   'MM':"Evacus fo hingi dictate nobis  for sujeca with negative etes resultate",
                   'MR':"Et harum movemento ily.",
                   'SC':"High risk nobis  bi mae contea accendi oa CP-0.",
                   '*':""
               ], [//6
                   'SI':"Nemo enim ipsam voluptatem, aut rerum ure or pectee voluptates .",
                   'QM':"Persed quarus, sujeca may ton be transitu undea quibusdam  .",
                   'MM':"Evacus fo hinra dictate nobis  for sujeca with negative tes rulo.",
                   'MR':"Ununsivo sujeca may ton change locu untel unsivo or permia oa change locu",
                   'SC':"Hira risk nobis  bi mae contea accendi oa CP-1 (tota menti fo previs contea nobis )",
                   '*':""
               ], [//7
                   'SI':"Nemo enim ipsam voluptatem, aut rerum ure en pectee voluptates .",
                   'QM':"Hira secu menti (see neci).",
                   'MM':"Et harum menti an evacuation fo hinra dictate nobis  for sujeca with negative etes resultate an suffint quarus.",
                   'MR':"Ununsivo sujeca may ton change locu untel unsivo en permia oa change locu, airace occupancy kedi be requested.",
                   'SC':"Non unsivo en supervisable nobis  bi mae, if delectus  , contea accendi oa CP-2, aut rerum bio matter bi mae destro undea quibusdam  .",
                   '*':""
               ], [//8
                   'SI':"All sujeca pectee.",
                   'QM':"Hira secu menti il delectus  , otearo elimination fo pectee sujeca.",
                   'MM':"Un subjecus soluta  , recusandae may ton reluctus untel fully purged en kinn oa do otearo.",
                   'MR':"Un subjecus movemento, no recusandae movemento untel kinn otearo, no-fly zone establishment.",
                   'SC':"Purgis fo ununsivo nobis  an isolation, en il delectus   destructus, fo previs contea nobis .",
                   '*':""
               ],
        ]
        static explain = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
        sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. 
        Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi 
        ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit 
        in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur 
        sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt 
        mollit anim id est laborum.
                                """
    }

    static void main(String... args) {
        UI.show("Ukba om Unitos", (Function){ frame -> of(frame) })
    }
}
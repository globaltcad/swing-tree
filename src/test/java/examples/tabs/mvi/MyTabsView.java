package examples.tabs.mvi;

import sprouts.Event;
import sprouts.From;
import sprouts.Var;
import swingtree.SwingTree;
import swingtree.UI;

import javax.swing.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

/**
 *  A custom tabs view implementation similar to {@link JTabbedPane},
 *  but based on a declarative MVI design and custom styling.
 */
public class MyTabsView extends Panel
{
    private final JPanel           contentPanel = new JPanel();
    private final List<JComponent> allContents  = new ArrayList<>();
    private final Event            repaintEvent = Event.create();

    private JComponent             currentlyVisible = null;


    public MyTabsView( Var<MyTabsViewModel> vm ) {
        Color HIGHLIGHT_COLOR = new Color(153, 243, 206);
        Color DEFAULT_COLOR   = new Color(119, 188, 213);
        Color OUTLINE_COLOR   = new Color( 0,   0,   0, 0.5f);

        Var<MyTabsViewModel.TabModel> currentTab = vm.zoomTo(MyTabsViewModel::currentTab, MyTabsViewModel::withCurrentTab);
        Var<List<MyTabsViewModel.TabModel>> tabs = vm.zoomTo(MyTabsViewModel::tabs, MyTabsViewModel::withTabs);

        UI.of(this).withLayout("fill, ins 0, gap 0, wrap 1")
        .add(GROW_X,
            box().withStyle( it -> it
                .borderColor(OUTLINE_COLOR)
                .borderWidthAt(Edge.BOTTOM, 1)
                .padding(2, 5, -1, 5)
            )
            .add(tabs, tabModels -> {
                return UI.box().apply( ui -> {
                    for (MyTabsViewModel.TabModel tabModel : tabModels) {
                        ui.add(
                            button(tabModel.title()).withMaxHeight(38).withMinHeight(38).withFontSize(12)
                            .withRepaintOn(repaintEvent)
                            .withTooltip(tabModel.tip())
                            .peek( b -> {
                                Optional<ImageIcon> icon = tabModel.iconSource().find();
                                if ( icon.isPresent() ) {
                                    b.setIcon(icon.get());
                                    b.setText("");
                                } else {
                                    b.setIcon(null);
                                    b.setText(tabModel.title());}
                            })
                            .withStyle( it -> it
                                .gradient( gradient -> gradient
                                    .span(Span.TOP_TO_BOTTOM)
                                    .colors(determineTabButtonColorsFor(tabModel, it.component(), vm.get()))
                                )
                                .applyIf(currentTab.is(tabModel), it2 -> it2
                                    .margin( 2, 2, 0, 2 )
                                    .padding(6, 6, 8, 6)
                                    .borderRadiusAt(Corner.TOP_LEFT, 12)
                                    .borderRadiusAt(Corner.TOP_RIGHT, 12)
                                    .border(1, OUTLINE_COLOR)
                                    .borderWidthAt(Edge.BOTTOM, 0)
                                    .backgroundColor(HIGHLIGHT_COLOR)
                                )
                                .applyIf(currentTab.isNot(tabModel), it2 -> it2
                                    .margin( 2, 2, 4, 2 )
                                    .padding(4, 6, 4, 6)
                                    .borderRadius(18)
                                    .border(1, OUTLINE_COLOR)
                                    .backgroundColor(DEFAULT_COLOR)
                                )
                                .applyIf(it.component().getModel().isPressed(), it2 -> it2
                                    .backgroundColor(DEFAULT_COLOR.brighter())
                                )
                            )
                            .onClick( it -> {
                                currentTab.set(From.VIEW, tabModel);
                                select(currentTab.get());
                                repaintEvent.fire();
                            })
                        );
                    }
                });
            })
        )
        .add(GROW.and(PUSH),
            box().withStyle( it -> it
                .borderColor(HIGHLIGHT_COLOR)
                .borderWidthAt(Edge.TOP, 3)
            )
            .add(GROW.and(PUSH),
                box().withStyle( it -> it
                    .borderColor(OUTLINE_COLOR)
                    .borderWidthAt(Edge.TOP, 1)
                )
                .add(GROW.and(PUSH),
                    UI.of(this.contentPanel).withLayout("ins 0, fill, hidemode 3")
                )
            )
        );
        currentTab.ifPresent( this::select );
        currentTab.onChange(From.VIEW_MODEL,  it -> select(it.get()) );
        tabs.onChange(From.ALL, it -> {
            updateContentComponents(
                it.get().stream()
                .map(MyTabsViewModel.TabModel::contentView)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
            );
        });
    }

    private void updateContentComponents(List<JComponent> newList) {
        for ( JComponent c : new ArrayList<>(allContents) ) {
            if ( !newList.contains(c) ) {
                c.setVisible(false);
                allContents.remove(c);
                contentPanel.remove(c);
            }
            if ( !allContents.contains(c) )
                addNewContent(c);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void select( MyTabsViewModel.TabModel model ) {
        JComponent current = model.contentView();
        if ( current == currentlyVisible )
            return;

        if ( currentlyVisible != null )
            currentlyVisible.setVisible(false);

        currentlyVisible = current;

        if ( current != null ) {
            if ( !allContents.contains(current) )
                addNewContent(current);

            current.setVisible(true);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void addNewContent(JComponent current) {
        allContents.add(current);
        contentPanel.add(current, "grow, push");
    }

    private Color[] determineTabButtonColorsFor(MyTabsViewModel.TabModel tab, JButton button, MyTabsViewModel vm) {
        ButtonModel model = button.getModel();
        return new Color[]{
                new Color(1f, 1f, 1f, 0.45f),
                new Color(1f, 1f, 1f, model.isRollover() ? 0.5f : 0.0f),
                new Color(1f, 1f, 1f, vm.currentTab() == tab ? 0f : 0.45f)
        };
    }

    public static void main(String... args) {

        SwingTree.get().setUiScaleFactor(3);

        MyTabsViewModel.TabModel tab1 = new DummyTab("Tab 1", "").getModel();
        MyTabsViewModel.TabModel tab2 = new DummyTab("Tab 2", "img/two-16th-notes.svg").getModel();
        MyTabsViewModel.TabModel tab3 = new DummyTab("Tab 3", "img/funnel.svg").getModel();

        List<MyTabsViewModel.TabModel> tabs = new ArrayList<>();
        tabs.add(tab1);
        tabs.add(tab2);
        tabs.add(tab3);
        MyTabsViewModel vm = new MyTabsViewModel(tab1, tabs);

        vm = vm.withCurrentTab(tab2);

        Var<MyTabsViewModel> state = Var.of(vm);
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new MyTabsView(state));
        frame.pack();
        frame.setVisible(true);
    }

    public static class DummyTab implements MyTabsViewModel.TabContent
    {
        private final String name;
        private final String icon;

        public DummyTab(String s, String icon) {
            this.name = Objects.requireNonNull(s);
            this.icon = Objects.requireNonNull(icon);
        }

        @Override public String getName() { return name; }

        @Override
        public MyTabsViewModel.TabModel getModel() {
            MyTabsViewModel.TabModel model = new MyTabsViewModel.TabModel(this);
            model = model.withIconSource(()->icon);
            JButton b = new JButton(name);
            b.setFont(b.getFont().deriveFont(UI.scale(12f)));
            model = model.withContentView(b);
            model = model.withTip("This is a tip for " + name);
            model = model.withTitle(name);
            return model;
        }
    }
}

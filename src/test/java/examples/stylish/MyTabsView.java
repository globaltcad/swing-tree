package examples.stylish;

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

import static swingtree.UI.*;

public class MyTabsView extends Panel
{
    private final JPanel           contentPanel = new JPanel();
    private final List<JComponent> allContents  = new ArrayList<>();
    private final Event            repaintEvent = Event.create();

    private JComponent             currentlyVisible = null;


    public MyTabsView( MyTabsViewModel vm ) {
        Color HIGHLIGHT_COLOR = new Color(153, 243, 206);
        Color DEFAULT_COLOR   = new Color(119, 188, 213);
        Color OUTLINE_COLOR   = new Color( 0,   0,   0, 0.5f);

        UI.of(this).withLayout("fill, ins 0, gap 0, wrap 1")
        .add(GROW_X,
            box().withStyle( it -> it
                .borderColor(OUTLINE_COLOR)
                .borderWidthAt(Edge.BOTTOM, 1)
                .padding(2, 5, -1, 5)
            )
            .add(vm.getTabs(), tabModel ->
                button(tabModel.title()).withMaxHeight(38).withMinHeight(38).withFontSize(12)
                .withRepaintIf(repaintEvent)
                .withTooltip(tabModel.tip())
                .peek( b ->
                    tabModel.iconSource().onChange(From.VIEW_MODEL, i -> {
                        Optional<ImageIcon> icon = i.get().find();
                        if ( icon.isPresent() ) {
                            b.setIcon(icon.get());
                            b.setText("");
                        } else {
                            b.setIcon(null);
                            b.setText(tabModel.title().get());}
                    })
                )
                .withStyle( it -> it
                    .gradient( gradient -> gradient
                        .transition(Transition.TOP_TO_BOTTOM)
                        .colors(determineTabButtonColorsFor(tabModel, it.component(), vm))
                    )
                    .applyIf(vm.getCurrentTab().is(tabModel), it2 -> it2
                        .margin( 2, 2, 0, 2 )
                        .padding(6, 6, 8, 6)
                        .borderRadiusAt(Corner.TOP_LEFT, 12)
                        .borderRadiusAt(Corner.TOP_RIGHT, 12)
                        .border(1, OUTLINE_COLOR)
                        .borderWidthAt(Edge.BOTTOM, 0)
                        .backgroundColor(HIGHLIGHT_COLOR)
                    )
                    .applyIf(vm.getCurrentTab().isNot(tabModel), it2 -> it2
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
                    vm.getCurrentTab().set(From.VIEW, tabModel);
                    select(vm.getCurrentTab().get());
                    repaintEvent.fire();
                })
            )
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
        vm.getCurrentTab().ifPresent( this::select );
        vm.getCurrentTab().onChange(From.VIEW_MODEL,  it -> select(it.get()) );
        vm.getTabs().onChange( it -> {
            updateContentComponents(
                it.vals().stream()
                .map(MyTabsViewModel.TabModel::contentView)
                .map(Var::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
            );
        });
        for ( MyTabsViewModel.TabModel tab : vm.getTabs() )
            tab.iconSource().fire(From.VIEW_MODEL);
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
        JComponent current = model.contentView().orElseNull();
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

    private Color[] determineTabButtonColorsFor( MyTabsViewModel.TabModel tab, JButton button, MyTabsViewModel vm) {
        ButtonModel model = button.getModel();
        return new Color[]{
                new Color(1f, 1f, 1f, 0.45f),
                new Color(1f, 1f, 1f, model.isRollover() ? 0.5f : 0.0f),
                new Color(1f, 1f, 1f, vm.getCurrentTab().is(tab) ? 0f : 0.45f)
        };
    }

    public static void main(String... args) {

        SwingTree.get().getUIScale().setUserScaleFactor(3);

        MyTabsViewModel.TabModel tab1 = new DummyTab("Tab 1", "").getModel();
        MyTabsViewModel.TabModel tab2 = new DummyTab("Tab 2", "img/two-16th-notes.svg").getModel();
        MyTabsViewModel.TabModel tab3 = new DummyTab("Tab 3", "img/hopper.svg").getModel();

        MyTabsViewModel vm = new MyTabsViewModel();
        vm.getTabs().add(tab1);
        vm.getTabs().add(tab2);
        vm.getTabs().add(tab3);

        vm.getCurrentTab().set(tab2);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new MyTabsView(vm));
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
            model.iconSource().set(()->icon);
            JButton b = new JButton(name);
            b.setFont(b.getFont().deriveFont(UI.scale(12f)));
            model.contentView().set(b);
            model.tip().set("This is a tip for " + name);
            model.title().set(name);
            return model;
        }
    }
}

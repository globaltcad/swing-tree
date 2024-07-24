package examples.tabs.mvvm;

import sprouts.Var;
import sprouts.Vars;
import swingtree.api.IconDeclaration;

import javax.swing.JComponent;

public class MyTabsViewModel
{
    private final Var<TabModel> currentTab = Var.ofNull(TabModel.class);
    private final Vars<TabModel> tabs = Vars.of(TabModel.class);

    public Var<TabModel> getCurrentTab() { return currentTab; }

    public Vars<TabModel> getTabs() { return tabs; }


    interface TabContent
    {
        String   getName();
        TabModel getModel();
    }

    public static class TabModel
    {
        private final Object tabOwner;

        private final Var<String>          title        = Var.of("");
        private final Var<JComponent>      contentView  = Var.ofNull(JComponent.class);
        private final Var<IconDeclaration> iconSource   = Var.of(IconDeclaration.class, ()->"");
        private final Var<String>          tip          = Var.of("");


        TabModel( Object tabOwner ) { this.tabOwner = tabOwner; }


        public Var<String>          title()       { return title; }
        public Var<JComponent>      contentView() { return contentView; }
        public Var<IconDeclaration> iconSource()  { return iconSource; }
        public Var<String>          tip()         { return tip; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TabModel tabModel = (TabModel) o;
            return tabOwner.equals(tabModel.tabOwner);
        }

        @Override
        public int hashCode() { return tabOwner.hashCode(); }
    }

}

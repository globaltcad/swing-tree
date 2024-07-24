package examples.tabs.mvi;

import lombok.*;
import lombok.experimental.Accessors;
import swingtree.api.IconDeclaration;

import javax.swing.*;
import java.util.List;

@With @Getter @ToString @Accessors( fluent = true )
@EqualsAndHashCode @AllArgsConstructor
public class MyTabsViewModel
{
    private final TabModel currentTab;
    private final List<TabModel> tabs;

    interface TabContent
    {
        String   getName();
        TabModel getModel();
    }

    @With @Getter @ToString
    @Accessors( fluent = true )
    @AllArgsConstructor
    public static class TabModel
    {
        private final Object tabOwner;

        private final String          title      ;
        private final JComponent      contentView;
        private final IconDeclaration iconSource ;
        private final String          tip        ;


        TabModel( Object tabOwner ) {
            this(tabOwner, "", null, ()->"", "");
        }

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

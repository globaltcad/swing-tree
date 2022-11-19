package com.globaltcad.swingtree;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

class AbstractDelegate
{
    private final Query _query;

    AbstractDelegate(Component component) { _query = new Query(component); }

    /**
     *  Use this to query the UI tree and find any {@link JComponent}.
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param id The ide of the {@link JComponent} which should be found in the swing tree.
     * @return An {@link Optional} instance which may or may not contain the requested component.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> OptionalUI<T> find( Class<T> type, String id ) {
        return _query.find(type, id);
    }

}

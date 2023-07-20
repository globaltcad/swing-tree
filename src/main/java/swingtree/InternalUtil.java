package swingtree;

import swingtree.components.JBox;

import javax.swing.*;
import java.awt.*;

final class InternalUtil
{
    private InternalUtil() {}

    static void _traverseEnable(Component c, boolean isEnabled ) {
        c.setEnabled( isEnabled );
        if ( JPanel.class.isAssignableFrom( c.getClass() ) || JBox.class.isAssignableFrom( c.getClass() ) )
            for ( Component c2 : ((JComponent)c).getComponents() )
                _traverseEnable( c2, isEnabled );
        /*
            Note:
                We use getClass() here, because we want to stop at subclasses of
                JBox or JPanel because they are likely user defined components and not dumb
                wrappers for other components.
        */
    }
}

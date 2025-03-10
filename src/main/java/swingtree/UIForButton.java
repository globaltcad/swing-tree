package swingtree;

import org.slf4j.Logger;
import sprouts.Val;

import javax.swing.*;
import java.util.Objects;

/**
 * A SwingTree builder node designed for configuring {@link AbstractButton} sub-type instances,
 * usually the {@link JButton} type.
 *
 * @param <B> The type of {@link AbstractButton} that this {@link UIForButton} is configuring.
 */
public final class UIForButton<B extends AbstractButton> extends UIForAnyButton<UIForButton<B>, B>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UIForButton.class);

    private final BuilderState<B> _state;

    UIForButton( BuilderState<B> state ) {
        Objects.requireNonNull(state, "state");
        _state = state;
    }

    @Override
    protected BuilderState<B> _state() {
        return _state;
    }
    
    @Override
    protected UIForButton<B> _newBuilderWithState(BuilderState<B> newState ) {
        return new UIForButton<>(newState);
    }

    /**
     * Make this button the default button for the root pane it is in.
     * @return this
     */
    public final UIForButton<B> makeDefaultButton() {
        return _with( thisButton -> {
                    if ( !(thisButton instanceof JButton) ) {
                        log.warn("Method 'makeDefaultButton()' called on a non JButton component.");
                        return;
                    }
                    UI.runLater(()->{
                        // We do this later because at this point in time the UI is probably not
                        // yet fully built (swing-tree is using the builder-pattern).
                        _trySetDefaultButton((JButton) thisButton, 3);
                    });
                })
                ._this();
    }

    private void _trySetDefaultButton(JButton button, int tries) {
            JRootPane rootPane = SwingUtilities.getRootPane(button);
            if ( rootPane != null )
                rootPane.setDefaultButton(button);
            else {
                if ( tries > 0 )
                    UI.runLater(100, () -> _trySetDefaultButton(button, tries - 1) );
                else
                    log.warn("Method 'makeDefaultButton()' called on a JButton component that is not in a JRootPane.");
            }
    }
}

package swingtree.api.mvvm;

import javax.swing.*;

public interface Viewer<M>
{
    JComponent getView(M viewModel);
}

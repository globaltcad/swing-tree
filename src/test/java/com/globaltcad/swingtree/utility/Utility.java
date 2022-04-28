package com.globaltcad.swingtree.utility;

import com.alexandriasoftware.swing.JSplitButton;
import com.globaltcad.swingtree.UIForSplitButton;

import javax.swing.*;

public class Utility {

    public static <B extends JSplitButton> String getSplitButtonText(UIForSplitButton<B> ui) {
        return ui.getComponent().getText();
    }


    public static <B extends JSplitButton> JPopupMenu getSplitButtonPopup(UIForSplitButton<B> ui) {
        return ui.getComponent().getPopupMenu();
    }

}

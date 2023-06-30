/*
 *   IMPORTANT:
 *   This file is a derived work of the JSplitButton.java class from
 *   https://github.com/rhwood/jsplitbutton/tree/main (com.alexandriasoftware.swing.SplitButtonClickedActionListener),
 *   which is licensed under the Apache License, Version 2.0.
 *   The original author is Randall Wood (2016).
 *   Here the copy of the original license:
 *
 * Copyright (C) 2016, 2018 Randall Wood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package swingtree.components.action;

import java.awt.event.ActionListener;

/**
 * The listener interface for receiving the split clicked
 * {@link java.awt.event.ActionEvent}. The class that is interested in
 * processing an action event implements this interface, and the object created
 * with that class is registered with a component, using the component's
 * {@link swingtree.components.JSplitButton#addSplitButtonClickedActionListener(SplitButtonClickedActionListener)}
 * method. When the action event occurs, that object's
 * {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
 * method is invoked.
 *
 * @author Randall Wood
 */
public interface SplitButtonClickedActionListener extends ActionListener {

}

/**
 *  {@link examples.laf.SwingTreeLookAndFeel}, a configurable look and feel built on the SwingTree
 *  style engine, and the example application that shows it off.
 *  <p>
 *  {@code SwingTreeLookAndFeel} and its nested types are the whole of the public API. The
 *  {@code SwingTreeXxxUI} delegates are {@code public} only because Swing instantiates a UI
 *  delegate reflectively through {@link javax.swing.UIDefaults}, which cannot reach a
 *  package-private class, and each one has a {@code createUI} method for it to call. That method
 *  hands the delegate the {@code SwingTreeLookAndFeel.Theme} of the installed look and feel, and
 *  none of them decides how anything looks: the appearance comes from the style rules of that
 *  theme.
 */
package examples.laf;

/**
 *  {@link examples.laf.SwingTreeLookAndFeel}, a configurable look and feel built on the SwingTree
 *  style engine, and the example application that shows it off.
 *  <p>
 *  {@code SwingTreeLookAndFeel} and its nested types are the whole of the public API. Everything
 *  else here is an implementation detail; the {@code SwingTreeXxxUI} delegates are nevertheless
 *  {@code public} because Swing instantiates a UI delegate reflectively through
 *  {@link javax.swing.UIDefaults}, which cannot reach a package-private class. None of them
 *  decides how anything looks: the appearance comes from the configured style preset and is
 *  reached through {@code SwingTreeLookAndFeel.applyStyle(..)}.
 */
package examples.laf;

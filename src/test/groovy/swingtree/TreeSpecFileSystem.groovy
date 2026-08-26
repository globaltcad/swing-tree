package swingtree

import groovy.transform.EqualsAndHashCode
import sprouts.HasId
import sprouts.Tuple

import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.TreePath

/**
 *  The little immutable file system the tree specifications are written against, together
 *  with the three questions those specifications ask a {@link JTree}: what is on screen,
 *  what is open, and what is selected.
 *
 *  <p>In Java 21 this model would be written as a sealed interface and two records:
 *  <pre>{@code
 *  public sealed interface FsNode extends HasId<String> { String name(); }
 *  public record Dir( String id, String name, Tuple<FsNode> entries ) implements FsNode {}
 *  public record Doc( String id, String name, String body )           implements FsNode {}
 *  }</pre>
 *  SwingTree itself compiles against Java 8, so the specs spell the same shape out by hand:
 *  final fields, wither methods, value based equality, and an {@code id()} which
 *  {@link HasId} exposes to the binding.
 *
 *  <p>A forest built with {@code UI.trees(..)} is made of these very same node types; the
 *  only difference is that its top level is a {@code Tuple<FsNode>} rather than one {@code Dir}.
 *
 *  <p>The three observers below all answer in the labels a user would read off the screen,
 *  rather than in the node handles a bound tree keeps inside its paths. They ask their
 *  questions on the UI thread, which is where a tree may be asked anything at all.
 */
final class TreeSpecFileSystem {

    /**
     *  Every row the tree currently shows, indented by its depth below the topmost visible
     *  level and labelled exactly the way its cell renderer labels it. This is deliberately
     *  not a peek at the model: it asks the same questions the painting code asks, so a
     *  scenario asserting on it is asserting on what a user sees.
     */
    static List<String> visibleRows( JTree tree ) {
        return UI.runAndGet({
            // A root the tree does not draw is no level of its own, so it does not indent:
            var offset = ( tree.isRootVisible() ? 1 : 2 )
            (0..<tree.getRowCount()).collect { int row ->
                var path = tree.getPathForRow(row)
                return ("    " * (path.getPathCount() - offset)) + _labelOf(tree, path, row)
            }
        })
    }

    /** The labels of the branches the user currently has open, top down. */
    static List<String> expandedRows( JTree tree ) {
        return UI.runAndGet({
            (0..<tree.getRowCount())
                .findAll { int row -> tree.isExpanded(tree.getPathForRow(row)) }
                .collect { int row -> _labelOf(tree, tree.getPathForRow(row), row) }
        })
    }

    /** The labels of the rows the tree currently has selected. */
    static List<String> selectedRows( JTree tree ) {
        return UI.runAndGet({
            var paths = tree.getSelectionPaths()
            if ( paths == null )
                return []
            return paths.collect { TreePath path -> _labelOf(tree, path, tree.getRowForPath(path)) }
        })
    }

    private static String _labelOf( JTree tree, TreePath path, int row ) {
        var view = tree.getCellRenderer().getTreeCellRendererComponent(
                        tree, path.getLastPathComponent(), false,
                        tree.isExpanded(path), tree.getModel().isLeaf(path.getLastPathComponent()),
                        row, false
                   )
        return ((JLabel) view).getText()
    }
}

/** The sum type of everything that can sit in the file system. */
interface FsNode extends HasId<String> {
    String name()
}

/** A branch: it carries the entries below it, which is what a {@code children(..)} rule zooms into. */
@EqualsAndHashCode(includeFields = true)
final class Dir implements FsNode {
    private final String        _id
    private final String        _name
    private final Tuple<FsNode> _entries

    Dir( String id, String name, Tuple<FsNode> entries ) {
        _id = id; _name = name; _entries = entries
    }
    @Override String id()   { return _id }
    @Override String name() { return _name }
    Tuple<FsNode> entries() { return _entries }

    Dir withName( String name )              { return new Dir(_id, name, _entries) }
    Dir withEntries( Tuple<FsNode> entries ) { return new Dir(_id, _name, entries) }

    @Override String toString() { return "Dir(" + _name + ")" }
}

/** A leaf: no rule ever gives it children, which is all it takes to be one. */
@EqualsAndHashCode(includeFields = true)
final class Doc implements FsNode {
    private final String _id
    private final String _name
    private final String _body

    Doc( String id, String name, String body = "" ) {
        _id = id; _name = name; _body = body
    }
    @Override String id()   { return _id }
    @Override String name() { return _name }
    String body()           { return _body }

    Doc withName( String name ) { return new Doc(_id, name, _body) }
    Doc withBody( String body ) { return new Doc(_id, _name, body) }

    @Override String toString() { return "Doc(" + _name + ")" }
}

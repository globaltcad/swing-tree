package swingtree.components;

import net.miginfocom.swing.MigLayout;
import sprouts.Tuple;
import swingtree.layout.Bounds;
import swingtree.style.ComponentExtension;
import swingtree.style.StyleConf;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;

final class ComponentDebugInfo {
    private final Class<?> type;
    private final String id;
    private final Bounds bounds;
    private final Tuple<StackTraceElement> sourceCodeLocation;
    private final StyleConf styleConf;
    private final String asString;
    private final String layoutInformation;

    ComponentDebugInfo(Component component) {
        this(
            component.getClass(),
            Optional.ofNullable(component.getName()).orElse(""),
            Bounds.of(component.getBounds()),
            findSourceCodeLocationOf(component),
            findStyleConfOf(component),
            component.toString(),
            findLayoutInfoOf(component)
        );
    }

    private ComponentDebugInfo(
            Class<?> type,
            String id,
            Bounds bounds,
            Tuple<StackTraceElement> sourceCodeLocation,
            StyleConf styleConf,
            String asString,
            String layoutInformation
    ) {
        this.type = type;
        this.id = id;
        this.bounds = bounds;
        this.sourceCodeLocation = sourceCodeLocation;
        this.styleConf = styleConf;
        this.asString = asString;
        this.layoutInformation = layoutInformation;
    }

    public Class<?> type() {
        return type;
    }

    public String id() {
        return id;
    }

    public Bounds bounds() {
        return bounds;
    }

    public Tuple<StackTraceElement> sourceCodeLocation() {
        return sourceCodeLocation;
    }

    public StyleConf styleConf() {
        return styleConf;
    }

    public String asString() {
        return asString;
    }

    public String layoutInformation() {
        return layoutInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ComponentDebugInfo debugInfo = (ComponentDebugInfo) o;
        return Objects.equals(type, debugInfo.type) && Objects.equals(bounds, debugInfo.bounds) && Objects.equals(sourceCodeLocation, debugInfo.sourceCodeLocation) && Objects.equals(styleConf, debugInfo.styleConf) && Objects.equals(asString, debugInfo.asString) && Objects.equals(layoutInformation, debugInfo.layoutInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, bounds, sourceCodeLocation, styleConf, asString, layoutInformation);
    }


    static Tuple<StackTraceElement> findSourceCodeLocationOf(Component component) {
        if ( component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;
            Object property = jComponent.getClientProperty("built-at");
            if ( property instanceof Tuple ) {
                Tuple<StackTraceElement> trace = (Tuple) property;
                return trace;
            }
        }
        return Tuple.of(StackTraceElement.class);
    }

    static StyleConf findStyleConfOf(Component component) {
        if ( component instanceof JComponent ) {
            return ComponentExtension.from((JComponent) component).getStyle();
        }
        return StyleConf.none();
    }

    static String findLayoutInfoOf(Component component) {
        if ( component instanceof JComponent ) {
            LayoutManager layoutManager = ((JComponent) component).getLayout();
            if ( layoutManager instanceof MigLayout) {
                // We pretty-print MigLayout constraints in a more readable way, since they can be quite long and complex:
                MigLayout migLayout = (MigLayout) layoutManager;
                String layoutConstraints = migLayout.getLayoutConstraints().toString();
                String columnConstraints = migLayout.getColumnConstraints().toString();
                String rowConstraints = migLayout.getRowConstraints().toString();
                return "MigLayout[\n" +
                        "  layoutConstraints=\"" + layoutConstraints + "\",\n" +
                        "  columnConstraints=\"" + columnConstraints + "\",\n" +
                        "  rowConstraints=\"" + rowConstraints + "\"\n" +
                        "]";
            }
            return String.valueOf(layoutManager);
        }
        return "";
    }

}

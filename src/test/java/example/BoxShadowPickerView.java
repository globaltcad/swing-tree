package example;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import java.awt.*;
import java.util.Optional;

import static swingtree.UI.*;

public class BoxShadowPickerView extends UI.Panel
{
    BoxShadowPickerView(BockShadowPickerViewModel vm) {
        FlatLightLaf.setup();
        of(this)
        .withLayout(FILL_X.and(WRAP(2)))
        .add(
            panel(FILL.and(WRAP(3)), "[shrink][grow][grow]")
            .add(label("Padding Top:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.paddingTop()))
            .add(label(vm.paddingTop().viewAsString()))
            .add(label("Padding Left:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.paddingLeft()))
            .add(label(vm.paddingLeft().viewAsString()))
            .add(label("Padding Right:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.paddingRight()))
            .add(label(vm.paddingRight().viewAsString()))
            .add(label("Padding Bottom:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.paddingBottom()))
            .add(label(vm.paddingBottom().viewAsString()))
        )
        .add(
            panel(FILL.and(WRAP(3)), "[shrink][grow][grow]")
            .add(label("Border Arc Width:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.borderArcWidth()))
            .add(label(vm.borderArcWidth().viewAsString()))
            .add(label("Border Arc Height:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.borderArcHeight()))
            .add(label(vm.borderArcHeight().viewAsString()))
            .add(label("Border Thickness:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.borderThickness()))
            .add(label(vm.borderThickness().viewAsString()))
            .add(label("Border Color:"))
            .add(ALIGN_CENTER,
                textField(vm.shadowColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                .onContentChange( it -> {
                    parseColor(it.get().getText()).ifPresent(color -> {vm.borderColor().act(color);});
                })
            )
            .add(panel(FILL).withBackground(vm.borderColor()))
        )
        .add(
            panel(FILL.and(WRAP(3)), "[shrink][grow][grow]")
            .add(label("Horizontal Shadow Offset:"))
            .add(slider(Align.HORIZONTAL, -50, 50, vm.horizontalShadowOffset()))
            .add(label(vm.horizontalShadowOffset().viewAsString()))
            .add(label("Vertical Shadow Offset:"))
            .add(slider(Align.HORIZONTAL, -50, 50, vm.verticalShadowOffset()))
            .add(label(vm.verticalShadowOffset().viewAsString()))
            .add(label("Shadow Blur Radius:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.shadowBlurRadius()))
            .add(label(vm.shadowBlurRadius().viewAsString()))
            .add(label("Shadow Spread Radius:"))
            .add(slider(Align.HORIZONTAL, 0, 100, vm.shadowSpreadRadius()))
            .add(label(vm.shadowSpreadRadius().viewAsString()))
            .add(label("Shadow Color:"))
            .add(ALIGN_CENTER,
                textField(vm.shadowColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                .onContentChange(it -> {
                    parseColor(it.get().getText()).ifPresent(color -> {vm.shadowColor().act(color);});
                })
            )
            .add(panel(FILL).withBackground(vm.shadowColor()))
            .add(label("Shadow Background Color:"))
            .add(ALIGN_CENTER,
                textField(vm.shadowBackgroundColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                .onContentChange(it -> {
                    parseColor(it.get().getText()).ifPresent(color -> {vm.shadowBackgroundColor().act(color);});
                })
            )
            .add(panel(FILL).withBackground(vm.shadowBackgroundColor()))
            .add(checkBox("Inset", vm.shadowInset()))
        )
        .add(
            panel(FILL).withPrefSize(1450, 600)
            .withRepaintIf(vm.repaint())
            .withForeground( p -> {
                p.shadowBackgroundColor(vm.shadowBackgroundColor().get())
                .padTop(vm.paddingTop().get())
                .padLeft(vm.paddingLeft().get())
                .padRight(vm.paddingRight().get())
                .padBottom(vm.paddingBottom().get())
                .borderRadius(vm.borderArcWidth().get(), vm.borderArcHeight().get())
                .shadowColor(vm.shadowColor().get())
                .shadowHorizontalOffset(vm.horizontalShadowOffset().get())
                .shadowVerticalOffset(vm.verticalShadowOffset().get())
                .shadowBlurRadius(vm.shadowBlurRadius().get())
                .shadowSpreadRadius(vm.shadowSpreadRadius().get())
                .shadowInset(vm.shadowInset().get())
                .background(vm.backgroundColor().get())
                .border(vm.borderThickness().get())
                .renderShadows();

                p.border(vm.borderColor().get())
                        .drawBorder();
            })
        );

    }

    private static Optional<Color> parseColor(String text) {
        // 1. Try to parse it as a hex color.
        // 2. Try to parse it as a color name.
        // 3. Try to parse it as a color integer.
        // Let's go:
        try {
            return Optional.of(Color.decode(text));
        } catch (NumberFormatException e) {
            // ignore
        }
        try {
            return Optional.of(Color.getColor(text));
        } catch (Exception e) {
            // ignore
        }
        try {
            return Optional.of(new Color(Integer.parseInt(text)));
        } catch (NumberFormatException e) {
            // ignore
        }
        text = text.toLowerCase().trim();
        // Let's try to parse it as a color name.
        if ( text.equals("black") ) return Optional.of(Color.BLACK);
        if ( text.equals("blue") ) return Optional.of(Color.BLUE);
        if ( text.equals("cyan") ) return Optional.of(Color.CYAN);
        if ( text.equals("darkgray") ) return Optional.of(Color.DARK_GRAY);
        if ( text.equals("gray") ) return Optional.of(Color.GRAY);
        if ( text.equals("green") ) return Optional.of(Color.GREEN);
        if ( text.equals("lightgray") ) return Optional.of(Color.LIGHT_GRAY);
        if ( text.equals("magenta") ) return Optional.of(Color.MAGENTA);
        if ( text.equals("orange") ) return Optional.of(Color.ORANGE);
        if ( text.equals("pink") ) return Optional.of(Color.PINK);
        if ( text.equals("red") ) return Optional.of(Color.RED);
        if ( text.equals("white") ) return Optional.of(Color.WHITE);
        if ( text.equals("yellow") ) return Optional.of(Color.YELLOW);

        // Lets try to split it into 4 numbers: (we allow spaces and commas)
        String[] parts = text.split("[,\\s]+");
        if ( parts.length == 4 ) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                int a = Integer.parseInt(parts[3].trim());
                return Optional.of(new Color(r, g, b, a));
            } catch (NumberFormatException e) {
                // ignore
            }
            // maybe float?
            try {
                float r = Float.parseFloat(parts[0].trim());
                float g = Float.parseFloat(parts[1].trim());
                float b = Float.parseFloat(parts[2].trim());
                float a = Float.parseFloat(parts[3].trim());
                return Optional.of(new Color(r, g, b, a));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        if ( parts.length == 3 ) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Optional.of(new Color(r, g, b));
            } catch (NumberFormatException e) {
                // ignore
            }
            // maybe float?
            try {
                float r = Float.parseFloat(parts[0].trim());
                float g = Float.parseFloat(parts[1].trim());
                float b = Float.parseFloat(parts[2].trim());
                return Optional.of(new Color(r, g, b));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return Optional.empty();
    }

    public static void main( String... args ) { UI.show(f ->new BoxShadowPickerView(new BockShadowPickerViewModel())); }
}

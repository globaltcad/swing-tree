package example;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import java.awt.*;
import java.util.Optional;

import static swingtree.UI.*;

public class BoxShadowPickerView extends UI.Panel
{
    public BoxShadowPickerView(BoxShadowPickerViewModel vm) {
        FlatLightLaf.setup();
        of(this).withLayout(WRAP(2).and(INS(12)), "[]12[]")
        .add(panel(WRAP(2), "[grow]12[grow]", "[]12[]")
            .add(GROW_X,
                panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Padding")
                .add(label("Top:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.paddingTop()))
                .add(label(vm.paddingTop().viewAsString()))
                .add(label("Left:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.paddingLeft()))
                .add(label(vm.paddingLeft().viewAsString()))
                .add(label("Right:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.paddingRight()))
                .add(label(vm.paddingRight().viewAsString()))
                .add(label("Bottom:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.paddingBottom()))
                .add(label(vm.paddingBottom().viewAsString()))
            )
            .add(GROW_X,
                panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Border")
                .add(label("Arc Width:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.borderArcWidth()))
                .add(label(vm.borderArcWidth().viewAsString()))
                .add(label("Arc Height:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.borderArcHeight()))
                .add(label(vm.borderArcHeight().viewAsString()))
                .add(label("Thickness:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.borderThickness()))
                .add(label(vm.borderThickness().viewAsString()))
                .add(label("Color:"))
                .add(GROW_X.and(PUSH_X),
                    textField(vm.shadowColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                    .onContentChange( it -> {
                        parseColor(it.get().getText()).ifPresent(color -> {vm.borderColor().act(color);});
                    })
                )
                .add(panel(FILL).withBackground(vm.borderColor()))
            )
            .add(GROW_X,
                panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Shadow")
                .add(label("Horizontal Offset:"))
                .add(GROW_X, slider(Align.HORIZONTAL, -50, 50, vm.horizontalShadowOffset()))
                .add(label(vm.horizontalShadowOffset().viewAsString()))
                .add(label("Vertical Offset:"))
                .add(GROW_X, slider(Align.HORIZONTAL, -50, 50, vm.verticalShadowOffset()))
                .add(label(vm.verticalShadowOffset().viewAsString()))
                .add(label("Blur Radius:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.shadowBlurRadius()))
                .add(label(vm.shadowBlurRadius().viewAsString()))
                .add(label("Spread Radius:"))
                .add(GROW_X, slider(Align.HORIZONTAL, 0, 100, vm.shadowSpreadRadius()))
                .add(label(vm.shadowSpreadRadius().viewAsString()))
                .add(label("Color:"))
                .add(GROW_X.and(PUSH_X),
                    textField(vm.shadowColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                    .onContentChange(it -> {
                        parseColor(it.get().getText()).ifPresent(color -> {vm.shadowColor().act(color);});
                    })
                )
                .add(panel(FILL).withBackground(vm.shadowColor()))
                .add(label("Background Color:"))
                .add(GROW_X.and(PUSH_X),
                    textField(vm.backgroundColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                    .onContentChange(it -> {
                        parseColor(it.get().getText()).ifPresent(color -> {vm.backgroundColor().act(color);});
                    })
                )
                .add(panel(FILL).withBackground(vm.backgroundColor()))
                .add(label("Outer Background Color:"))
                .add(GROW_X.and(PUSH_X),
                    textField(vm.outerBackgroundColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                    .onContentChange(it -> {
                        parseColor(it.get().getText()).ifPresent(color -> {vm.outerBackgroundColor().act(color);});
                    })
                )
                .add(panel(FILL).withBackground(vm.outerBackgroundColor()))
                .add(checkBox("Inset", vm.shadowInset()))
                .add(checkBox("Draw Smiley", vm.drawSmiley()))
            )
            .add(GROW,
                panel(FILL).withBorderTitled("Preview")
                .add(GROW,
                    panel(FILL)
                    .withRepaintIf(vm.repaint())
                    .withStyle( it ->
                        it.style()
                         .innerBackground(vm.backgroundColor().get())
                         .background(vm.outerBackgroundColor().get())
                         .backgroundRenderer(g2d -> {
                             if ( vm.drawSmiley().is(false) ) return;
                             int w = it.component().getWidth() - vm.paddingLeft().get() - vm.paddingRight().get() - 100;
                             int h = it.component().getHeight() - vm.paddingTop().get() - vm.paddingBottom().get() - 100;
                             int x = vm.paddingLeft().get() + 50;
                             int y = vm.paddingTop().get() + 50;
                             drawASmiley(g2d, x, y, w, h);
                         })
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
                         .borderWidth(vm.borderThickness().get())
                         .borderColor(vm.borderColor().get())
                    )
                )
            )
        )
        .add(GROW,
            panel(FILL).withBorderTitled("Code")
            .add(GROW,
                scrollPane()
                .add(
                    textArea(vm.code()).isEditableIf(false)
                    .withStyle( it ->
                        it.style()
                            .font(new Font("Monospaced", Font.PLAIN, 15))
                            .fontColor(Color.BLUE.darker())
                            .fontSelectionColor(Color.CYAN)
                    )
                )
            )
        );
    }

    private void drawASmiley(Graphics2D g2d, int x, int y, int w, int h) {
        // We crop the rectangle so that t is centered and squared:
        int crop = Math.abs(w - h)/2;
        if (w > h) {
            x += crop;
            w = h;
        } else {
            y += crop;
            h = w;
        }
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x, y, w, h);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x, y, w, h);
        g2d.fillOval(x + w/4, y + h/4, w/8, h/8);
        g2d.fillOval((int) (x + 2.5*w/4), y + h/4, w/8, h/8);
        // Now a smile:
        g2d.drawArc(x + w/4, y + 2*h/4, w/2, h/4, 0, -180);
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

    public static void main( String... args ) { UI.show(f ->new BoxShadowPickerView(new BoxShadowPickerViewModel())); }
}

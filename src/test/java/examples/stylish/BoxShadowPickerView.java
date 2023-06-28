package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;
import swingtree.style.Corner;
import swingtree.style.Layer;

import java.awt.*;
import java.util.Optional;

import static swingtree.UI.*;

public class BoxShadowPickerView extends UI.Panel
{
    public BoxShadowPickerView(BoxShadowPickerViewModel vm) {
        FlatLightLaf.setup();
        of(this).withLayout(WRAP(2).and(INS(12)), "[]12[]")
        .add(
            panel(WRAP(2).and(INS(12)))
            .add(GROW_X.and(GROW_Y),
                panel(WRAP(2), "[grow]12[grow]", "[]12[]")
                .add(GROW_X,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Padding")
                    .add(label("Top:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, vm.paddingTop()))
                    .add(label(vm.paddingTop().viewAsString()))
                    .add(label("Left:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, vm.paddingLeft()))
                    .add(label(vm.paddingLeft().viewAsString()))
                    .add(label("Right:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, vm.paddingRight()))
                    .add(label(vm.paddingRight().viewAsString()))
                    .add(label("Bottom:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, vm.paddingBottom()))
                    .add(label(vm.paddingBottom().viewAsString()))
                )
                .add(GROW_X,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Margin")
                    .add(label("Top:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 150, vm.marginTop()))
                    .add(label(vm.marginTop().viewAsString()))
                    .add(label("Left:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 350, vm.marginLeft()))
                    .add(label(vm.marginLeft().viewAsString()))
                    .add(label("Right:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 350, vm.marginRight()))
                    .add(label(vm.marginRight().viewAsString()))
                    .add(label("Bottom:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 150, vm.marginBottom()))
                    .add(label(vm.marginBottom().viewAsString()))
                )
                .add(GROW_X.and(SPAN),
                    panel(FILL.and(INS(0))).withBorderTitled("Shadow")
                    .add(
                        panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]")
                        .add(label("Horizontal Offset:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -50, 50, vm.horizontalShadowOffset()))
                        .add(label(vm.horizontalShadowOffset().viewAsString()))
                        .add(label("Vertical Offset:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -50, 50, vm.verticalShadowOffset()))
                        .add(label(vm.verticalShadowOffset().viewAsString()))
                        .add(label("Blur Radius:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -10, 100, vm.shadowBlurRadius()))
                        .add(label(vm.shadowBlurRadius().viewAsString()))
                        .add(label("Spread Radius:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -10, 100, vm.shadowSpreadRadius()))
                        .add(label(vm.shadowSpreadRadius().viewAsString()))
                    )
                    .add(
                        panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]")
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
                        .add(label("Foundation Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(vm.foundationColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange(it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {vm.foundationColor().act(color);});
                            })
                        )
                        .add(panel(FILL).withBackground(vm.foundationColor()))
                        .add(checkBox("Inset", vm.shadowInset()))
                        .add(checkBox("Draw Smiley", vm.drawSmiley()))
                    )
                )
            )
            .add(GROW_X.and(GROW_Y),
                panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Border")
                .add(label("Color:"))
                .add(GROW_X.and(PUSH_X),
                    textField(vm.shadowColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                    .onContentChange( it -> {
                        parseColor(it.get().getText()).ifPresent(color -> {vm.borderColor().act(color);});
                    })
                )
                .add(WRAP, panel(FILL).withBackground(vm.borderColor()))
                .add(SPAN,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Corners")
                    .add(SPAN, comboBox(vm.borderCorner()))
                    .add(SPAN, vm.currentCornerModel())
                )
                .add(SPAN,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Edges")
                    .add(SPAN, comboBox(vm.borderEdge()))
                    .add(SPAN, vm.currentEdgeModel())
                )
            )
            .add(GROW.and(SPAN),
                panel(FILL).withBorderTitled("Preview")
                .withPrefHeight(350)
                .add(GROW,
                    panel(FILL.and(INS(0)))
                    .withRepaintIf(vm.repaint())
                    .withStyle( it ->
                        it
                         .backgroundColor(vm.backgroundColor().get())
                         .foundationColor(vm.foundationColor().get())
                         .painter(Layer.BACKGROUND, g2d -> {
                             if ( vm.drawSmiley().is(false) ) return;
                             int w = it.component().getWidth() - vm.marginLeft().get() - vm.marginRight().get() - 100;
                             int h = it.component().getHeight() - vm.marginTop().get() - vm.marginBottom().get() - 100;
                             int x = vm.marginLeft().get() + 50;
                             int y = vm.marginTop().get() + 50;
                             drawASmiley(g2d, x, y, w, h);
                         })
                         .padding(vm.paddingTop().get(), vm.paddingRight().get(), vm.paddingBottom().get(), vm.paddingLeft().get())
                         .margin(vm.marginTop().get(), vm.marginRight().get(), vm.marginBottom().get(), vm.marginLeft().get())
                         .borderRadiusAt(Corner.TOP_LEFT, vm.arcWidthAt(Corner.TOP_LEFT), vm.arcHeightAt(Corner.TOP_LEFT))
                         .borderRadiusAt(Corner.TOP_RIGHT, vm.arcWidthAt(Corner.TOP_RIGHT), vm.arcHeightAt(Corner.TOP_RIGHT))
                         .borderRadiusAt(Corner.BOTTOM_RIGHT, vm.arcWidthAt(Corner.BOTTOM_RIGHT), vm.arcHeightAt(Corner.BOTTOM_RIGHT))
                         .borderRadiusAt(Corner.BOTTOM_LEFT, vm.arcWidthAt(Corner.BOTTOM_LEFT), vm.arcHeightAt(Corner.BOTTOM_LEFT))
                         .shadowColor(vm.shadowColor().get())
                         .shadowHorizontalOffset(vm.horizontalShadowOffset().get())
                         .shadowVerticalOffset(vm.verticalShadowOffset().get())
                         .shadowBlurRadius(vm.shadowBlurRadius().get())
                         .shadowSpreadRadius(vm.shadowSpreadRadius().get())
                         .shadowIsInset(vm.shadowInset().get())
                         .borderWidths(vm.topBorderWidth(), vm.rightBorderWidth(), vm.bottomBorderWidth(), vm.leftBorderWidth())
                         .borderColor(vm.borderColor().get())
                    )
                    .add(TOP.and(SPAN).and(ALIGN_CENTER), label("Label"))
                    .add(LEFT, button("Button"))
                    .add(RIGHT.and(WRAP), toggleButton("Toggle"))
                    .add(BOTTOM.and(SPAN).and(ALIGN_CENTER), checkBox("Check"))
                )
            )
        )
        .add(GROW.and(PUSH_X),
            panel(FILL.and(INS(16))).withBorderTitled("Code")
            .add(GROW,
                scrollPane().withStyle( it -> it.borderWidth(0) )
                .withStyle( s -> s.borderWidth(0).borderColor(Color.BLUE) )
                .add(
                    textArea(vm.code()).isEditableIf(false)
                    .withStyle( it ->
                        it.font(new Font("Monospaced", Font.PLAIN, 15))
                          .fontColor(Color.BLUE.darker())
                          .fontSelectionColor(new Color(20, 200, 100, 100))
                          .borderWidth(0)
                          .borderColor(Color.GREEN)
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
        if ( text.equals("blue") )  return Optional.of(Color.BLUE);
        if ( text.equals("cyan") )  return Optional.of(Color.CYAN);
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

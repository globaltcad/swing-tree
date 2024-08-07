package examples.stylepicker.mvvm;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import swingtree.UI;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

import static swingtree.UI.*;

/**
 *  A simple application for configuring a SwingTree style on a panel.
 *  It renders a preview of the style and generates the code for it,
 *  which you can copy and paste into your application.
 */
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
                                parseColor(it.get().getText()).ifPresent(color -> {vm.shadowColor().set(From.VIEW, color);});
                            })
                        )
                        .add(panel(FILL).withBackground(vm.shadowColor()))
                        .add(label("Background Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(vm.backgroundColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange(it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {vm.backgroundColor().set(From.VIEW, color);});
                            })
                        )
                        .add(panel(FILL).withBackground(vm.backgroundColor()))
                        .add(label("Foundation Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(vm.foundationColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange(it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {vm.foundationColor().set(From.VIEW, color);});
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
                .add(SPAN,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Corners")
                    .add(SPAN, comboBox(vm.borderCorner(), BoxShadowPickerView::enumToTitleString))
                    .add(SPAN, vm.currentCornerModel(), viewModel ->
                        UI.box(FILL.and(WRAP(2)), "[shrink][grow]")
                        .add(UI.label("Width:"))
                        .add(GROW_X, UI.slider(UI.Align.HORIZONTAL, 0, 100, viewModel.borderArcWidth()))
                        .add(UI.label("Height:"))
                        .add(GROW_X, UI.slider(UI.Align.HORIZONTAL, 0, 100, viewModel.borderArcHeight()))
                    )
                )
                .add(SPAN,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Edges")
                    .add(SPAN, comboBox(vm.borderEdge(), BoxShadowPickerView::enumToTitleString))
                    .add(SPAN, vm.currentEdgeModel(), viewModel ->
                        UI.box(FILL.and(WRAP(3)), "[shrink][grow]")
                        .add(UI.label("Width:"))
                        .add(GROW_X.and(SPAN(2)), UI.slider(UI.Align.HORIZONTAL, 0, 100, viewModel.borderWidth()))
                        .add(label("Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(viewModel.borderColor().mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange( it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {viewModel.borderColor().set(From.VIEW, color);});
                            })
                        )
                        .add(WRAP, panel(FILL).withBackground(viewModel.borderColor()))
                    )
                )
            )
            .add(GROW.and(SPAN),
                panel(FILL).withBorderTitled("Preview")
                .withPrefHeight(350)
                .add(GROW,
                    panel(FILL.and(INS(0)))
                    .withRepaintOn(vm.repaint())
                    .withStyle( it -> it
                         .backgroundColor(vm.backgroundColor().get())
                         .foundationColor(vm.foundationColor().get())
                         .painter(UI.Layer.BACKGROUND, g2d -> {
                             if ( vm.drawSmiley().is(false) ) return;
                             int w = it.component().getWidth() - vm.marginLeft().get() - vm.marginRight().get() - 100;
                             int h = it.component().getHeight() - vm.marginTop().get() - vm.marginBottom().get() - 100;
                             int x = vm.marginLeft().get() + 50;
                             int y = vm.marginTop().get() + 50;
                             drawASmiley(g2d, x, y, w, h);
                         })
                         .padding(vm.paddingTop().get(), vm.paddingRight().get(), vm.paddingBottom().get(), vm.paddingLeft().get())
                         .margin(vm.marginTop().get(), vm.marginRight().get(), vm.marginBottom().get(), vm.marginLeft().get())
                         .borderRadiusAt(UI.Corner.TOP_LEFT, vm.arcWidthAt(UI.Corner.TOP_LEFT), vm.arcHeightAt(UI.Corner.TOP_LEFT))
                         .borderRadiusAt(UI.Corner.TOP_RIGHT, vm.arcWidthAt(UI.Corner.TOP_RIGHT), vm.arcHeightAt(UI.Corner.TOP_RIGHT))
                         .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, vm.arcWidthAt(UI.Corner.BOTTOM_RIGHT), vm.arcHeightAt(UI.Corner.BOTTOM_RIGHT))
                         .borderRadiusAt(UI.Corner.BOTTOM_LEFT, vm.arcWidthAt(UI.Corner.BOTTOM_LEFT), vm.arcHeightAt(UI.Corner.BOTTOM_LEFT))
                         .shadowColor(vm.shadowColor().get())
                         .shadowHorizontalOffset(vm.horizontalShadowOffset().get())
                         .shadowVerticalOffset(vm.verticalShadowOffset().get())
                         .shadowBlurRadius(vm.shadowBlurRadius().get())
                         .shadowSpreadRadius(vm.shadowSpreadRadius().get())
                         .shadowIsInset(vm.shadowInset().get())
                         .borderWidths(vm.topBorderWidth(), vm.rightBorderWidth(), vm.bottomBorderWidth(), vm.leftBorderWidth())
                         .borderColors(vm.topBorderColor(), vm.rightBorderColor(), vm.bottomBorderColor(), vm.leftBorderColor())
                         .noise( noiseConf -> noiseConf
                             .function(vm.noise().get())
                             .colors(vm.noiseColors().get().split(","))
                             .clipTo(vm.noiseArea().get())
                         )
                    )
                    .add(TOP.and(SPAN).and(ALIGN_CENTER), label("Label"))
                    .add(LEFT, button("Button"))
                    .add(RIGHT.and(WRAP), toggleButton("Toggle"))
                    .add(BOTTOM.and(SPAN).and(ALIGN_CENTER), checkBox("Check"))
                )
            )
        )
        .add(GROW.and(PUSH_X),
            panel(FILL.and(INS(16)).and(WRAP(1)))
            .withPrefHeight(350)
            .add(GROW_X,
                panel(FILL.and(WRAP(2)), "[shrink][grow]").withBorderTitled("Noise")
                .add(label("Type:"))
                .add(GROW_X, comboBox(vm.noise()))
                .add(label("Colors:"))
                .add(GROW_X, textField(vm.noiseColors()))
                .add(label("Area:"))
                .add(GROW_X, comboBox(vm.noiseArea()))
            )
            .add(GROW.and(PUSH),
                panel(FILL).withBorderTitled("Code")
                .add(GROW,
                    scrollPane()
                    .withStyle( it -> it.borderWidth(0).borderColor(Color.BLUE) )
                    .add(
                        textArea(vm.code()).withLayout(FILL)
                        .isEditableIf(false)
                        .withStyle( it ->
                            it.font(new Font("Monospaced", Font.PLAIN, 15))
                              .fontColor(Color.BLUE.darker())
                              .fontSelectionColor(new Color(20, 200, 100, 100))
                              .borderWidth(0)
                              .borderColor(Color.GREEN)
                        )
                        .add(TOP.and(RIGHT),
                            button("Copy").onClick( e -> {
                                String code = vm.code().get();
                                // Copy to clipboard
                                StringSelection stringSelection = new StringSelection(code);
                                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                                clipboard.setContents(stringSelection, null);
                            })
                        )
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
        try {
            return Optional.ofNullable(color(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String enumToTitleString( Enum<?> e ) {
        // Example THIS_IS_AN_ENUM -> This Is An Enum
        String[] parts = e.name().split("_");
        StringBuilder sb = new StringBuilder();
        for ( String part : parts ) {
            sb.append(part.substring(0, 1).toUpperCase());
            sb.append(part.substring(1).toLowerCase());
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public static void main( String... args ) { UI.show(f ->new BoxShadowPickerView(new BoxShadowPickerViewModel())); }
}

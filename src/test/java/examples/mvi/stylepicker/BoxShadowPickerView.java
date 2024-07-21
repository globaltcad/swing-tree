package examples.mvi.stylepicker;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;

import java.awt.Color;
import java.awt.Font;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

/**
 *  A simple application for configuring a SwingTree style on a panel.
 *  It renders a preview of the style and generates the code for it,
 *  which you can copy and paste into your application.
 *  This example goes together with the {@link BoxShadowPickerViewModel}
 *  and it demonstrates the MVI pattern with SwingTree.
 */
public class BoxShadowPickerView extends Panel
{
    public BoxShadowPickerView(Var<BoxShadowPickerViewModel> vm) {
        Var<Integer> paddingTop     = vm.zoomTo(BoxShadowPickerViewModel::paddingTop, BoxShadowPickerViewModel::withPaddingTop);
        Var<Integer> paddingLeft    = vm.zoomTo(BoxShadowPickerViewModel::paddingLeft, BoxShadowPickerViewModel::withPaddingLeft);
        Var<Integer> paddingRight   = vm.zoomTo(BoxShadowPickerViewModel::paddingRight, BoxShadowPickerViewModel::withPaddingRight);
        Var<Integer> paddingBottom  = vm.zoomTo(BoxShadowPickerViewModel::paddingBottom, BoxShadowPickerViewModel::withPaddingBottom);
        Var<Integer> marginTop      = vm.zoomTo(BoxShadowPickerViewModel::marginTop, BoxShadowPickerViewModel::withMarginTop);
        Var<Integer> marginLeft     = vm.zoomTo(BoxShadowPickerViewModel::marginLeft, BoxShadowPickerViewModel::withMarginLeft);
        Var<Integer> marginRight    = vm.zoomTo(BoxShadowPickerViewModel::marginRight, BoxShadowPickerViewModel::withMarginRight);
        Var<Integer> marginBottom   = vm.zoomTo(BoxShadowPickerViewModel::marginBottom, BoxShadowPickerViewModel::withMarginBottom);

        Var<Integer> horizontalShadowOffset = vm.zoomTo(BoxShadowPickerViewModel::horizontalShadowOffset, BoxShadowPickerViewModel::withHorizontalShadowOffset);
        Var<Integer> verticalShadowOffset   = vm.zoomTo(BoxShadowPickerViewModel::verticalShadowOffset, BoxShadowPickerViewModel::withVerticalShadowOffset);
        Var<Integer> shadowBlurRadius       = vm.zoomTo(BoxShadowPickerViewModel::shadowBlurRadius, BoxShadowPickerViewModel::withShadowBlurRadius);
        Var<Integer> shadowSpreadRadius     = vm.zoomTo(BoxShadowPickerViewModel::shadowSpreadRadius, BoxShadowPickerViewModel::withShadowSpreadRadius);
        Var<Color>   shadowColor            = vm.zoomTo(BoxShadowPickerViewModel::shadowColor, BoxShadowPickerViewModel::withShadowColor);
        Var<Boolean> shadowInset            = vm.zoomTo(BoxShadowPickerViewModel::shadowInset, BoxShadowPickerViewModel::withShadowInset);

        Var<Color> backgroundColor = vm.zoomTo(BoxShadowPickerViewModel::backgroundColor, BoxShadowPickerViewModel::withBackgroundColor);
        Var<Color> foundationColor = vm.zoomTo(BoxShadowPickerViewModel::foundationColor, BoxShadowPickerViewModel::withFoundationColor);

        Var<Boolean> drawSmiley     = vm.zoomTo(BoxShadowPickerViewModel::drawSmiley, BoxShadowPickerViewModel::withDrawSmiley);
        Var<UI.Corner> borderCorner = vm.zoomTo(BoxShadowPickerViewModel::borderCorner, BoxShadowPickerViewModel::updateBorderCorner);
        Var<BoxShadowPickerViewModel.BorderCornerViewModel> currentCornerModel = vm.zoomTo(BoxShadowPickerViewModel::currentCornerModel, BoxShadowPickerViewModel::updateCurrentCornerModel);
        Var<Integer> borderArcWidth  = currentCornerModel.zoomTo(BoxShadowPickerViewModel.BorderCornerViewModel::borderArcWidth, BoxShadowPickerViewModel.BorderCornerViewModel::withBorderArcWidth);
        Var<Integer> borderArcHeight = currentCornerModel.zoomTo(BoxShadowPickerViewModel.BorderCornerViewModel::borderArcHeight, BoxShadowPickerViewModel.BorderCornerViewModel::withBorderArcHeight);
        Var<UI.Edge> borderEdge      = vm.zoomTo(BoxShadowPickerViewModel::borderEdge, BoxShadowPickerViewModel::updateBorderEdge);
        Var<BoxShadowPickerViewModel.BorderEdgeViewModel> currentEdgeModel = vm.zoomTo(BoxShadowPickerViewModel::currentEdgeModel, BoxShadowPickerViewModel::updateCurrentEdgeModel);
        Var<Integer> borderWidth     = currentEdgeModel.zoomTo(BoxShadowPickerViewModel.BorderEdgeViewModel::borderWidth, BoxShadowPickerViewModel.BorderEdgeViewModel::withBorderWidth);
        Var<Color> borderColor       = currentEdgeModel.zoomTo(BoxShadowPickerViewModel.BorderEdgeViewModel::borderColor, BoxShadowPickerViewModel.BorderEdgeViewModel::withBorderColor);
        Val<String> code             = vm.viewAsString(BoxShadowPickerViewModel::createCode);

        Var<NoiseType> noise = vm.zoomTo(BoxShadowPickerViewModel::noise, BoxShadowPickerViewModel::withNoise);
        Var<String> noiseColors = vm.zoomTo(BoxShadowPickerViewModel::noiseColors, BoxShadowPickerViewModel::withNoiseColors);
        Var<UI.ComponentArea> noiseArea = vm.zoomTo(BoxShadowPickerViewModel::noiseArea, BoxShadowPickerViewModel::withNoiseArea);

        FlatLightLaf.setup();
        of(this).withLayout(WRAP(2).and(INS(12)), "[]12[]")
        .add(
            panel(WRAP(2).and(INS(12)))
            .add(GROW_X.and(GROW_Y),
                panel(WRAP(2), "[grow]12[grow]", "[]12[]")
                .add(GROW_X,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Padding")
                    .add(label("Top:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, paddingTop))
                    .add(label(paddingTop.viewAsString()))
                    .add(label("Left:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, paddingLeft))
                    .add(label(paddingLeft.viewAsString()))
                    .add(label("Right:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, paddingRight))
                    .add(label(paddingRight.viewAsString()))
                    .add(label("Bottom:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -50, 100, paddingBottom))
                    .add(label(paddingBottom.viewAsString()))
                )
                .add(GROW_X,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Margin")
                    .add(label("Top:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 150, marginTop))
                    .add(label(marginTop.viewAsString()))
                    .add(label("Left:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 350, marginLeft))
                    .add(label(marginLeft.viewAsString()))
                    .add(label("Right:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 350, marginRight))
                    .add(label(marginRight.viewAsString()))
                    .add(label("Bottom:"))
                    .add(GROW_X, slider(Align.HORIZONTAL, -25, 150, marginBottom))
                    .add(label(marginBottom.viewAsString()))
                )
                .add(GROW_X.and(SPAN),
                    panel(FILL.and(INS(0))).withBorderTitled("Shadow")
                    .add(
                        panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]")
                        .add(label("Horizontal Offset:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -50, 50, horizontalShadowOffset))
                        .add(label(horizontalShadowOffset.viewAsString()))
                        .add(label("Vertical Offset:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -50, 50, verticalShadowOffset))
                        .add(label(verticalShadowOffset.viewAsString()))
                        .add(label("Blur Radius:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -10, 100, shadowBlurRadius))
                        .add(label(shadowBlurRadius.viewAsString()))
                        .add(label("Spread Radius:"))
                        .add(GROW_X, slider(Align.HORIZONTAL, -10, 100, shadowSpreadRadius))
                        .add(label(shadowSpreadRadius.viewAsString()))
                    )
                    .add(
                        panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]")
                        .add(label("Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(shadowColor.mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange(it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {shadowColor.set(From.VIEW, color);});
                            })
                        )
                        .add(panel(FILL).withBackground(shadowColor))
                        .add(label("Background Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(backgroundColor.mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange(it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {backgroundColor.set(From.VIEW, color);});
                            })
                        )
                        .add(panel(FILL).withBackground(backgroundColor))
                        .add(label("Foundation Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(foundationColor.mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange(it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {foundationColor.set(From.VIEW, color);});
                            })
                        )
                        .add(panel(FILL).withBackground(foundationColor))
                        .add(checkBox("Inset", shadowInset))
                        .add(checkBox("Draw Smiley", drawSmiley))
                    )
                )
            )
            .add(GROW_X.and(GROW_Y),
                panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Border")
                .add(SPAN,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Corners")
                    .add(SPAN, comboBox(borderCorner, BoxShadowPickerView::enumToTitleString))
                    .add(SPAN,
                        UI.box(FILL.and(WRAP(2)), "[shrink][grow]")
                        .add(UI.label("Width:"))
                        .add(GROW_X, UI.slider(Align.HORIZONTAL, 0, 100, borderArcWidth))
                        .add(UI.label("Height:"))
                        .add(GROW_X, UI.slider(Align.HORIZONTAL, 0, 100, borderArcHeight))
                    )
                )
                .add(SPAN,
                    panel(FILL.and(WRAP(3)), "[shrink][grow][shrink]").withBorderTitled("Edges")
                    .add(SPAN, comboBox(borderEdge, BoxShadowPickerView::enumToTitleString))
                    .add(SPAN,
                        UI.box(FILL.and(WRAP(3)), "[shrink][grow]")
                        .add(UI.label("Width:"))
                        .add(GROW_X.and(SPAN(2)), UI.slider(Align.HORIZONTAL, 0, 100, borderWidth))
                        .add(label("Color:"))
                        .add(GROW_X.and(PUSH_X),
                            textField(borderColor.mapTo(Integer.class, Color::getRGB).itemAsString())
                            .onContentChange( it -> {
                                parseColor(it.get().getText()).ifPresent(color -> {borderColor.set(From.VIEW, color);});
                            })
                        )
                        .add(WRAP, panel(FILL).withBackground(borderColor))
                    )
                )
            )
            .add(GROW.and(SPAN),
                panel(FILL).withBorderTitled("Preview")
                .withPrefHeight(350)
                .add(GROW,
                    panel(FILL.and(INS(0)))
                    .withRepaintOn(vm)
                    .withStyle( it -> it
                         .backgroundColor(backgroundColor.get())
                         .foundationColor(foundationColor.get())
                         .painter(Layer.BACKGROUND, g2d -> {
                             if ( drawSmiley.is(false) ) return;
                             int w = it.component().getWidth() - marginLeft.get() - marginRight.get() - 100;
                             int h = it.component().getHeight() - marginTop.get() - marginBottom.get() - 100;
                             int x = marginLeft.get() + 50;
                             int y = marginTop.get() + 50;
                             drawASmiley(g2d, x, y, w, h);
                         })
                         .padding(paddingTop.get(), paddingRight.get(), paddingBottom.get(), paddingLeft.get())
                         .margin(marginTop.get(), marginRight.get(), marginBottom.get(), marginLeft.get())
                         .borderRadiusAt(Corner.TOP_LEFT, vm.get().arcWidthAt(Corner.TOP_LEFT), vm.get().arcHeightAt(Corner.TOP_LEFT))
                         .borderRadiusAt(Corner.TOP_RIGHT, vm.get().arcWidthAt(Corner.TOP_RIGHT), vm.get().arcHeightAt(Corner.TOP_RIGHT))
                         .borderRadiusAt(Corner.BOTTOM_RIGHT, vm.get().arcWidthAt(Corner.BOTTOM_RIGHT), vm.get().arcHeightAt(Corner.BOTTOM_RIGHT))
                         .borderRadiusAt(Corner.BOTTOM_LEFT, vm.get().arcWidthAt(Corner.BOTTOM_LEFT), vm.get().arcHeightAt(Corner.BOTTOM_LEFT))
                         .shadowColor(shadowColor.get())
                         .shadowHorizontalOffset(horizontalShadowOffset.get())
                         .shadowVerticalOffset(verticalShadowOffset.get())
                         .shadowBlurRadius(shadowBlurRadius.get())
                         .shadowSpreadRadius(shadowSpreadRadius.get())
                         .shadowIsInset(shadowInset.get())
                         .borderWidths(vm.get().topBorderWidth(), vm.get().rightBorderWidth(), vm.get().bottomBorderWidth(), vm.get().leftBorderWidth())
                         .borderColors(vm.get().topBorderColor(), vm.get().rightBorderColor(), vm.get().bottomBorderColor(), vm.get().leftBorderColor())
                         .noise( noiseConf -> noiseConf
                             .function(noise.get())
                             .colors(noiseColors.get().split(","))
                             .clipTo(noiseArea.get())
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
                .add(GROW_X, comboBox(noise))
                .add(label("Colors:"))
                .add(GROW_X, textField(noiseColors))
                .add(label("Area:"))
                .add(GROW_X, comboBox(noiseArea))
            )
            .add(GROW.and(PUSH),
                panel(FILL).withBorderTitled("Code")
                .add(GROW,
                    scrollPane()
                    .withStyle( it -> it.borderWidth(0).borderColor(Color.BLUE) )
                    .add(
                        textArea(code).withLayout(FILL)
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
                                // Copy to clipboard
                                StringSelection stringSelection = new StringSelection(code.get());
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

    public static void main( String... args ) {
        Var<BoxShadowPickerViewModel> state = Var.of(BoxShadowPickerViewModel.ini());
        UI.show(f ->new BoxShadowPickerView(state));
    }
}

package swingtree;

import java.awt.*;

public class StyleCollector 
{

    // Padding
    private int paddingTop = 0;
    private int paddingLeft = 0;
    private int paddingRight = 0;
    private int paddingBottom = 0;

    // Border
    private int borderArcWidth = 0;
    private int borderArcHeight = 0;
    private int borderThickness = 0;
    private Color borderColor = Color.BLACK;

    // Background
    private Color backgroundColor = Color.WHITE;

    // Box Shadow
    private int horizontalShadowOffset = 0;
    private int verticalShadowOffset = 0;
    private int shadowBlurRadius = 5;
    private int shadowSpreadRadius = 5;
    private Color shadowColor = Color.BLACK;
    private Color shadowBackgroundColor = null;
    private boolean shadowInset = false;


    StyleCollector() {}

    public StyleCollector pad(int top, int left, int right, int bottom ) {
        this.paddingTop = top;
        this.paddingLeft = left;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        return this;
    }

    public StyleCollector pad(int padding ) {
        this.paddingTop = padding;
        this.paddingLeft = padding;
        this.paddingRight = padding;
        this.paddingBottom = padding;
        return this;
    }

    public StyleCollector padTop(int padding ) {
        this.paddingTop = padding;
        return this;
    }

    public StyleCollector padLeft(int padding ) {
        this.paddingLeft = padding;
        return this;
    }

    public StyleCollector padRight(int padding ) {
        this.paddingRight = padding;
        return this;
    }

    public StyleCollector padBottom(int padding ) {
        this.paddingBottom = padding;
        return this;
    }

    public StyleCollector border(int thickness, Color color ) {
        this.borderThickness = thickness;
        this.borderColor = color;
        return this;
    }

    public StyleCollector border( int thickness ) {
        this.borderThickness = thickness;
        return this;
    }

    public StyleCollector border(Color color ) {
        this.borderColor = color;
        return this;
    }

    public StyleCollector borderRadius(int radius ) {
        this.borderArcWidth = radius;
        this.borderArcHeight = radius;
        return this;
    }

    public StyleCollector borderRadius(int arcWidth, int arcHeight ) {
        this.borderArcWidth = arcWidth;
        this.borderArcHeight = arcHeight;
        return this;
    }

    public StyleCollector background(Color color ) {
        this.backgroundColor = color;
        return this;
    }

    public StyleCollector shadowHorizontalOffset(int offset ) {
        this.horizontalShadowOffset = offset;
        return this;
    }

    public StyleCollector shadowVerticalOffset(int offset ) {
        this.verticalShadowOffset = offset;
        return this;
    }

    public StyleCollector shadowOffset(int horizontalOffset, int verticalOffset ) {
        this.horizontalShadowOffset = horizontalOffset;
        this.verticalShadowOffset = verticalOffset;
        return this;
    }

    public StyleCollector shadowBlurRadius(int radius) {
        this.shadowBlurRadius = radius;
        return this;
    }

    public StyleCollector shadowSpreadRadius(int radius ) {
        this.shadowSpreadRadius = radius;
        return this;
    }

    public StyleCollector shadowColor(Color color ) {
        this.shadowColor = color;
        return this;
    }

    public StyleCollector shadowBackgroundColor(Color color ) {
        this.shadowBackgroundColor = color;
        return this;
    }

    public StyleCollector shadowInset(boolean b) {
        shadowInset = b;
        return this;
    }

    int getPaddingTop() {
        return paddingTop;
    }

    int getPaddingLeft() {
        return paddingLeft;
    }

    int getPaddingRight() {
        return paddingRight;
    }

    int getPaddingBottom() {
        return paddingBottom;
    }

    int getBorderArcWidth() {
        return borderArcWidth;
    }

    int getBorderArcHeight() {
        return borderArcHeight;
    }

    int getBorderThickness() {
        return borderThickness;
    }

    Color getBorderColor() {
        return borderColor;
    }

    Color getBackgroundColor() {
        return backgroundColor;
    }

    int getHorizontalShadowOffset() {
        return horizontalShadowOffset;
    }

    int getVerticalShadowOffset() {
        return verticalShadowOffset;
    }

    int getShadowBlurRadius() {
        return shadowBlurRadius;
    }

    int getShadowSpreadRadius() {
        return shadowSpreadRadius;
    }

    Color getShadowColor() {
        return shadowColor;
    }

    Color getShadowBackgroundColor() {
        return shadowBackgroundColor;
    }

    boolean isShadowInset() {
        return shadowInset;
    }

}

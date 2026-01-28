package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

import java.util.Objects;

@Immutable
final class RenderConf {
    private final LayerRenderConf _backgroundConf;
    private final LayerRenderConf _contentConf;
    private final LayerRenderConf _borderConf;
    private final LayerRenderConf _foregroundConf;

    RenderConf(
        ComponentConf componentConf
    ) {
        _backgroundConf = LayerRenderConf.of(UI.Layer.BACKGROUND, componentConf);
        _contentConf = LayerRenderConf.of(UI.Layer.CONTENT, componentConf);
        _borderConf = LayerRenderConf.of(UI.Layer.BORDER, componentConf);
        _foregroundConf = LayerRenderConf.of(UI.Layer.FOREGROUND, componentConf);
    }

    public LayerRenderConf backgroundConf() {
        return _backgroundConf;
    }

    public LayerRenderConf contentConf() {
        return _contentConf;
    }

    public LayerRenderConf borderConf() {
        return _borderConf;
    }

    public LayerRenderConf foregroundConf() {
        return _foregroundConf;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RenderConf that = (RenderConf) o;
        return Objects.equals(_backgroundConf, that._backgroundConf) &&
                Objects.equals(_contentConf, that._contentConf) &&
                Objects.equals(_borderConf, that._borderConf) &&
                Objects.equals(_foregroundConf, that._foregroundConf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_backgroundConf, _contentConf, _borderConf, _foregroundConf);
    }
}

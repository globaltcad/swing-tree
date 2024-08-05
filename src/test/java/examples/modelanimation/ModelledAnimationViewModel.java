package examples.modelanimation;

import lombok.*;
import lombok.experimental.Accessors;
import swingtree.animation.Animatable;
import swingtree.animation.LifeTime;
import swingtree.animation.Stride;

import java.util.concurrent.TimeUnit;

@With @Getter @Accessors( fluent = true )
@AllArgsConstructor @EqualsAndHashCode @ToString
public class ModelledAnimationViewModel
{
    private final String buttonText;
    private final Stride animationStride;
    private final double borderWidth;
    private final double borderOpacity;

    public Animatable<ModelledAnimationViewModel> borderAnimation() {
        return Animatable.of(LifeTime.of(3, TimeUnit.SECONDS), this, (status, model) -> {
            boolean isRunning = status.progress() % 1 != 0;
            double localProgress = this.animationStride.applyTo(status.progress());
            Stride nextStride = status.progress() == 1 ? this.animationStride.inverse() : this.animationStride;
            return model.withBorderWidth( localProgress * 10 )
                        .withBorderOpacity( localProgress )
                        .withButtonText( isRunning ? "running" : "not running")
                        .withAnimationStride( nextStride );
        });
    }
}

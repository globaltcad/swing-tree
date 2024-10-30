package examples.mvvm;

import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;

public class SomeComponentsViewModel {

	private final Var<BaseSize> baseSize = Var.of(BaseSize.FACTOR);
	private final Var<Integer> x = Var.of(100);
	private final Var<Double> percent = Var.of(100.0);

	SomeComponentsViewModel() {
		Viewable.cast(baseSize).onChange(From.VIEW, it->baseSizeChanged());
	}

    public Var<BaseSize> getBaseSize() {
        return baseSize;
    }

	public Var<Integer> getX() {
		return x;
	}

	public Var<Double> getPercent() {
		return percent;
	}

	private void baseSizeChanged() {
		System.out.println("Base size changed to " + baseSize.get());
	}

	public enum BaseSize {
		EXACT, FACTOR, MARGINS;

		public String title() {
			return this.name().charAt(0)+this.name().substring(1).toLowerCase();
		}
	}

}


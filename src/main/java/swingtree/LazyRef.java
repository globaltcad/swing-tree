package swingtree;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 	This will simply fetch a variable from a lambda once and then continuously
 * 	return this one value.
 * 	In a sense it is a lazy pass by value!
 *
 * @param <V> The value type parameter of the thing wrapped by this.
 */
final class LazyRef<V>
{
	public static <V> LazyRef<V> of(Supplier<V> source) { return new LazyRef<>(source); }

	private @Nullable Supplier<V> source;
	private @Nullable V variable = null;

	private LazyRef(Supplier<V> source) { this.source = source; }

	public @Nullable V get() {
		if ( this.source == null ) return this.variable;
		else {
			this.variable = this.source.get();
			this.source = null;
		}
		return this.variable;
	}

	@Override public String toString() { return String.valueOf(this.get()); }
}

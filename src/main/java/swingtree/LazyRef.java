package swingtree;

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

	private Supplier<V> source;
	private V variable = null;

	private LazyRef(Supplier<V> source) { this.source = source; }

	public V get() {
		if ( this.source == null ) return this.variable;
		else {
			this.variable = this.source.get();
			this.source = null;
		}
		return this.variable;
	}

	@Override public String toString() { return String.valueOf(this.get()); }
}

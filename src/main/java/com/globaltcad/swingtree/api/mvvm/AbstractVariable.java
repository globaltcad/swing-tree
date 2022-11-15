package com.globaltcad.swingtree.api.mvvm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractVariable<T> implements Var<T> {
	private final List<Consumer<Val<T>>> viewActions = new ArrayList<>();

	private T value = null;
	private final Class<T> type;
	private final String name;

	protected AbstractVariable( Class<T> type, T iniValue, String name ) {
		Objects.requireNonNull(name);
		this.value = iniValue;
		this.type = ( iniValue == null ? type : (Class<T>) iniValue.getClass());
		this.name = name;
	}

	protected AbstractVariable(T iniValue) {
		this.value = iniValue;
		this.type = ( iniValue == null ? null : (Class<T>) iniValue.getClass());
		this.name = UNNAMED;
	}

	@Override
	public Class<T> type() {
		return type;
	}

	@Override
	public String id() {
		return name;
	}

	@Override
	public T get() { return value; }

	@Override
	public Var<T> set(T newValue) { value = newValue; return this; }

	@Override public Val<T> onViewThis(Consumer<Val<T>> displayAction ) {
		this.viewActions.add(displayAction);
		return this;
	}

	@Override
	public Val<T> view() {
		for ( Consumer<Val<T>> action : this.viewActions )
			try {
				action.accept(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return this;
	}

	@Override
	public String toString() {
		String asString = ( this.get() == null ? "null" : this.get().toString() );
		if ( id() == null ) return asString;
		else return
				asString +
						" ( " +
						"type='"+( type() == null ? "?" : type().getSimpleName() )+"', " +
						"name='"+ id()+"' " +
						")";
	}
}

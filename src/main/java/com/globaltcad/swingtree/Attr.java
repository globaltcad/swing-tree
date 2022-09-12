package com.globaltcad.swingtree;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 */
public class Attr
{
    public static Attr of(String... layoutConstraints ) {
        return new Attr(layoutConstraints);
    }

    private final Set<String> _constraints;

    private Attr(String... constraint) {
        _constraints = new HashSet<>();
        for (String c : constraint) {
            _constraints.add(c);
        }
    }

    public Attr and( Attr attr ) {
        Attr newAttr = new Attr();
        newAttr._constraints.addAll(_constraints);
        newAttr._constraints.addAll(attr._constraints);
        return newAttr;
    }

    @Override
    public String toString() {
        return _constraints.stream().collect(Collectors.joining(", "));
    }

}

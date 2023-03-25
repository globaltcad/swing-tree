package swingtree.layout;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 */
abstract class AbstractAttr
{
    private final Set<String> _constraints;


    AbstractAttr( String... constraint ) {
        _constraints = new HashSet<>();
        _constraints.addAll(Arrays.asList(constraint));
    }

    protected AbstractAttr _and( AbstractAttr attr, AbstractAttr newAttr ) {
        newAttr._constraints.addAll(_constraints);
        newAttr._constraints.addAll(attr._constraints);
        return newAttr;
    }

    @Override
    public String toString() { return _constraints.stream().collect(Collectors.joining(", ")); }

}

package swingtree.style;

class Pair<A, B>
{
    private final A _a;
    private final B _b;

    Pair(A a, B b) {
        _a = a;
        _b = b;
    }

    A first() { return _a; }

    B second() { return _b; }
}

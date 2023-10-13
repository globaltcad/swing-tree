package swingtree;

/**
 *  A functional interface for configuring a {@link SwingTree} instance
 *  through {@link SwingTree#initialiseUsing(SwingTreeConfigurator)}.
 *  <p>
 *  The {@link SwingTreeConfigurator} is a function receiving an immutable
 *  {@link SwingTreeInitConfig} instance and returning a new one
 *  with the desired configuration applied.
 */
@FunctionalInterface
public interface SwingTreeConfigurator
{
    /**
     *  Configures the given {@link SwingTreeInitConfig} instance
     *  and returns a new one with the desired configuration applied.
     *
     *  @param config the {@link SwingTreeInitConfig} instance to configure
     */
    SwingTreeInitConfig configure( SwingTreeInitConfig config );
}

package swingtree.api.mvvm;

public interface Noticeable
{
    Noticeable subscribe( Runnable listener );

    Noticeable unsubscribe( Runnable listener );
}

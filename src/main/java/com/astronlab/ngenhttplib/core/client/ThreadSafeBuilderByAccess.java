package com.astronlab.ngenhttplib.core.client;

public abstract class ThreadSafeBuilderByAccess<R, E> {
    private volatile boolean accessFlag = false;
    protected final R builder;
    private E outcome;

    public ThreadSafeBuilderByAccess(R instance, E exp) {
        builder = instance;
        outcome = exp;
    }

    public E update() {
        if (accessFlag) {
            synchronized (this) {
                if (accessFlag) {
                    accessFlag = false;
                    return outcome = build();
                }
            }
        }
        return outcome;
    }

    public E getLastOutcome() {
        return outcome;
    }

    public R getBuilder() {
        //we access the builder with this method to updated access status
        if (!accessFlag) {
            synchronized (this) {
                accessFlag = true;
            }
        }
        return builder;
    }

    protected abstract E build();
}

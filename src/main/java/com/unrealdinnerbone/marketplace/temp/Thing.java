package com.unrealdinnerbone.marketplace.temp;

public class Thing<T> {
    private final T queryResult;

    public Thing(T queryResult) {
        this.queryResult = queryResult;
    }

    public T queryResult() {
        return queryResult;
    }
}

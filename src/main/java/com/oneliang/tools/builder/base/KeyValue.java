package com.oneliang.tools.builder.base;

public class KeyValue<Key, Value> {

    public final Key key;
    public final Value value;

    public KeyValue(Key key, Value value) {
        this.key = key;
        this.value = value;
    }
}

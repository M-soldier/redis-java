package com.maio.redis.dataStructure.dict;

/**
 * @author 念兮
 * @date 2026/6/29
 * @email msj@bupt.edu.cn
 */
public class DictEntry<K, V> {
    private K key;
    private V value;

    DictEntry<K, V> next;

    DictEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}

package com.maio.redis.dataStructure.dict;

import java.util.List;
import java.util.function.Function;

/**
 * @author 念兮
 * @date 2026/6/29
 * @email msj@bupt.edu.cn
 */
public class Dict<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 4;
    private static final int LOAD_FACTOR = 1;
    private static final int SHRINK_FACTOR = 10;

    private int rehashIdx = 0;
    private DictHashTable<K, V>[] ht;
    private final Function<K, Integer> hashFunction ;

    public static <K, V> Dict<K, V> dictCreate() {
        return new Dict<>(key -> key.hashCode() & 0x7FFFFFFF);
    }

    public static <K, V> Dict<K, V> dictCreate(Function<K, Integer> hashFunction) {
        return new Dict<>(hashFunction);
    }

    @SuppressWarnings("unchecked")
    private Dict(Function<K, Integer> hashFunction) {
        this.ht = (DictHashTable<K, V>[]) new DictHashTable[2];
        this.ht[0] = new DictHashTable<>(DEFAULT_INITIAL_CAPACITY);
        this.ht[1] = null;

        this.hashFunction = hashFunction;
    }

    public int dictSize() {

    }

    public boolean isRehashing() {

    }

    public boolean dictAdd(K key, V value) {

    }

    public boolean dictReplace(K key, V value) {

    }

    public DictEntry<K, V> dictFind(K key) {

    }

    public V dictFetchValue(K key) {

    }

    public boolean dictDelete(K key) {

    }

    public boolean dictRefresh(int steps) {

    }

    public DictEntry<K, V> dictGetRandomKey() {

    }

    public List<DictEntry<K, V>> dictGetAll() {

    }

    public void dictResize() {

    }

    public DictEntry<K, V> dictAddRaw(K key) {

    }

    

}

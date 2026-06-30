package com.maio.redis.dataStructure.dict;

/**
 * 字典节点，对应 Redis 源码中的 dictEntry。
 * next 指针用于拉链法解决哈希冲突。
 *
 * @param <K> 键类型
 * @param <V> 值类型
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

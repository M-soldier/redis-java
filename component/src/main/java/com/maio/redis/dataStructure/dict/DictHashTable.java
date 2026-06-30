package com.maio.redis.dataStructure.dict;

/**
 * 字典内部哈希表，对应 Redis 源码中的 dictht。
 * Dict 维护两张哈希表（ht[0] 和 ht[1]），用于渐进式 rehash。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author 念兮
 * @date 2026/6/29
 * @email msj@bupt.edu.cn
 */
class DictHashTable<K, V> {
    /** 桶数组，每个桶是拉链的头节点 */
    DictEntry<K, V>[] table;

    /** 桶数组大小，始终为 2 的幂次 */
    int size;

    /** {@code size - 1}，用于快速取模：{@code hash & sizeMark} */
    int sizeMark;

    /** 当前存储的键值对的数量 */
    int usedSize;


    DictHashTable(int initCapacity) {
        this.table = createTable(initCapacity);
    }

    @SuppressWarnings("unchecked")
    private DictEntry<K, V>[] createTable(int capacity) {
        this.size = capacity;
        this.sizeMark = capacity > 0 ? capacity - 1 : 0;
        this.usedSize = 0;

        return (DictEntry<K, V>[]) new DictEntry[size];
    }
}

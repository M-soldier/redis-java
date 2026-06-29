package com.maio.redis.dataStructure.dict;

/**
 * @author 念兮
 * @date 2026/6/29
 * @email msj@bupt.edu.cn
 */
class DictHashTable<K, V> {
    DictEntry<K, V>[] table;

    int size;
    int sizeMark;
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

package com.maio.redis.dataStructure.dict;

import java.util.List;
import java.util.function.Function;

/**
 * Redis 字典，对应 Redis 源码 dict.h / dict.c。
 *
 * <p>使用链地址法（拉链法）解决哈希冲突，维护两张哈希表（ht[0] 和 ht[1]），
 * 通过渐进式 rehash 将扩容/缩容的开销分摊到每次读写操作中，避免一次性迁移造成的停顿。
 *
 * <p><b>渐进式 rehash 流程：</b>
 * <ol>
 *   <li>负载因子超过阈值时，分配 ht[1] 并设置 {@code rehashIdx = 0}，标志 rehash 开始</li>
 *   <li>每次 add / find / delete 操作时，调用 {@link #dictRehashStep()}，将 ht[0]
 *       中 {@code rehashIdx} 位置的一个桶迁移到 ht[1]，然后 {@code rehashIdx++}</li>
 *   <li>ht[0].used 归零时，rehash 完成：ht[0] = ht[1]，ht[1] 置空，{@code rehashIdx = -1}</li>
 * </ol>
 *
 * <p><b>rehash 期间的读写规则：</b>
 * <ul>
 *   <li>查找：先查 ht[0]，再查 ht[1]</li>
 *   <li>新增：只写入 ht[1]，防止数据回流到待迁移区</li>
 *   <li>删除：两张表都搜索</li>
 * </ul>
 *
 * @param <K> 键类型，不允许为 {@code null}
 * @param <V> 值类型
 * @author 念兮
 * @date 2026-06-29
 * @see DictEntry
 */
public class Dict<K, V> {
    /** 初始桶数组大小 */
    private static final int DEFAULT_INITIAL_CAPACITY = 4;

    /**
     * 扩容负载因子：used >= size * LOAD_FACTOR_EXPAND 时触发扩容。
     * Redis 在非持久化场景下阈值为 1，持久化时为 5，此处简化取 1。
     */
    private static final int LOAD_FACTOR = 1;

    /**
     * 缩容比例：used * SHRINK_RATIO < size 时触发缩容。
     * 对应 Redis 的 0.1 负载因子（used / size < 0.1）。
     */
    private static final int SHRINK_FACTOR = 10;

    /** 每步 rehash 最多跳过的空桶数，防止稀疏表时单步耗时过长 */
    private static final int REHASH_MAX_EMPTY_VISITS = 10;

    /**
     * 当前正在 rehash 的桶下标；-1 表示未在 rehash。
     * 对应 Redis 的 dict.rehashidx。
     */
    private int rehashIdx = -1;

    /**
     * 哈希函数，对应 Redis dictType.hashFunction。
     * 默认使用 key.hashCode()，可通过 {@link #dictCreate(Function)} 自定义。
     * 例如：命令表需要大小写不敏感的哈希，可传入 k -> k.toLowerCase().hashCode()。
     */
    private final Function<K, Integer> hashFunction ;

    private DictHashTable<K, V>[] ht;

    /**
     * 创建并返回一个空字典，使用默认哈希函数（{@code key.hashCode()}）。
     */
    public static <K, V> Dict<K, V> dictCreate() {
        return new Dict<>(key -> key.hashCode() & 0x7FFFFFFF);
    }

    /**
     * 创建并返回一个空字典，使用自定义哈希函数。
     * 对应 Redis 的 dictCreate(dictType *type, void *privDataPtr)。
     *
     * <p>示例——大小写不敏感（用于命令表）：
     * <pre>{@code
     * Dict<String, Command> commandTable =
     *     Dict.dictCreate(k -> k.toLowerCase().hashCode() & 0x7FFFFFFF);
     * }</pre>
     *
     * @param hashFunction 接收 key，返回非负哈希值
     */
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

    /**
     * 返回字典中的键值对总数（ht[0].used + ht[1].used）。
     */
    public int dictSize() {
        int size = ht[0].usedSize;
        size = ht[1] == null ? size : size + ht[1].usedSize;
        return size;
    }

    /**
     * 是否正在进行渐进式 rehash。
     */
    public boolean isRehashing() {
        return rehashIdx != -1;
    }

    /**
     * 添加键值对。key 已存在则不覆盖，返回 false。
     *
     * @param key   键，不允许为 {@code null}
     * @param value 值
     * @return 新增成功返回 {@code true}；key 已存在返回 {@code false}
     */
    public boolean dictAdd(K key, V value) {
        if(key == null) {
            throw new NullPointerException("key is null");
        }


    }

    public boolean dictReplace(K key, V value) {

    }

    public DictEntry<K, V> dictFind(K key) {

    }

    public V dictFetchValue(K key) {

    }

    public boolean dictDelete(K key) {

    }

    public boolean dictReHash(int steps) {

    }

    public DictEntry<K, V> dictGetRandomKey() {

    }

    public List<DictEntry<K, V>> dictGetAll() {

    }

    public void dictResize() {

    }

    /**
     * 为 key 分配新节点并插入，返回该节点（未设值）。
     * key 已存在时返回 null。
     * rehash 期间新节点插入 ht[1]。
     */
    private DictEntry<K, V> dictAddRaw(K key) {
        // 渐进式 rehash，每次哈希一个
        if(isRehashing()) {
            dictReHash(1);
        }

        int hashValue = doHash(key);
        // 在两个桶数组都进行查找，如果找到，就返回 null
        for(int i = 0; i < 2; i++) {
            if(i == 1 && !isRehashing()) {
                break;
            }

            if(ht[i] == null || ht[i].size == 0) {
                break;
            }

            DictEntry<K, V> entry = ht[i].table[hashValue & ht[i].sizeMark];
            while (entry != null) {
                if(entry.getKey().equals(key)) {
                    return null;
                }
                entry = entry.next;
            }
        }

        DictEntry<K, V> newEntry = new DictEntry<>(key, null);
        DictHashTable<K, V> hashTable = isRehashing() ? ht[1] : ht[0];

        // 头插法
        int idx = hashValue & hashTable.sizeMark;
        newEntry.next = hashTable.table[idx];
        hashTable.table[idx] = newEntry;
        hashTable.usedSize++;

        // TODO:需要执行扩缩容
        return newEntry;
    }

    /**
     * 计算 key 的哈希值，委托给外部注入的 hashFunction。
     */
    private int doHash(K key) {
        return hashFunction.apply(key);
    }


    

}

package com.maio.redis.dataStructure.dict;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
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
 *   <li>每次 add / find / delete 操作时，调用 {@link #dictRehash(int 1)}，将 ht[0]
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

    private final DictHashTable<K, V>[] ht;

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
            throw new IllegalArgumentException("key is null");
        }

        // key 已经存在，返回 false 不覆盖
        DictEntry<K, V> entry = dictAddRaw(key);
        if(entry == null) {
            return false;
        }

        entry.setValue(value);
        return true;
    }

    /**
     * 添加或更新键值对。
     *
     * @param key   键，不允许为 {@code null}
     * @param value 值
     * @return {@code true} 表示新增；{@code false} 表示更新了已有 key
     */
    public boolean dictReplace(K key, V value) {
        if(key == null) {
            throw new IllegalArgumentException("key is null");
        }

        DictEntry<K, V> entry = dictAddRaw(key);
        if(entry != null) {
            entry.setValue(value);
            return true;
        }

        Objects.requireNonNull(dictFind(key)).setValue(value);
        return false;
    }

    /**
     * 按 key 查找 DictEntry。
     * rehash 期间同时搜索 ht[0] 和 ht[1]。
     *
     * @return 找到返回 {@link DictEntry}，否则返回 {@code null}
     */
    @Nullable
    public DictEntry<K, V> dictFind(K key) {
        if(key == null || dictSize() == 0) {
            return null;
        }

        if(isRehashing()) {
            dictRehash(1);
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
                    return entry;
                }
                entry = entry.next;
            }
        }

        return null;
    }

    /**
     * 按 key 获取值。
     *
     * @return 找到返回 value，否则返回 {@code null}
     */
    @Nullable
    public V dictFetchValue(K key) {
        DictEntry<K, V> entry = dictFind(key);

        return entry == null ? null : entry.getValue();
    }

    /**
     * 删除 key 对应的键值对。
     * rehash 期间同时搜索 ht[0] 和 ht[1]。
     *
     * @return 删除成功返回 {@code true}；key 不存在返回 {@code false}
     */
    public boolean dictDelete(K key) {
        if(key == null || dictSize() == 0) {
            return false;
        }

        if(isRehashing()) {
            dictRehash(1);
        }

        int hashValue = doHash(key);
        for(int i = 0; i < 2; i++) {
            if(i == 1 && !isRehashing()) {
                break;
            }

            if(ht[i] == null || ht[i].size == 0) {
                break;
            }

            int idx = hashValue & ht[i].sizeMark;
            DictEntry<K, V> entry = ht[i].table[idx];
            DictEntry<K, V> prev = null;
            while(entry != null) {
                if(entry.getKey().equals(key)) {
                    if(prev != null) {
                        prev.next = entry.next;
                    } else {
                        ht[i].table[idx] = entry.next;
                    }

                    entry.next = null;
                    ht[i].usedSize--;

                    shrinkIfNeeded();

                    return true;
                }
                prev = entry;
                entry = entry.next;
            }
        }

        return false;
    }

    /**
     * 手动执行 n 步渐进式 rehash，将 ht[0] 中 n 个非空桶迁移到 ht[1]。
     * 通常由外部定时任务调用（对应 Redis 的 dictRehashMilliseconds）。
     *
     * @param steps 迁移步数
     * @return {@code true} 表示仍在 rehash；{@code false} 表示已完成或未开始
     */
    public boolean dictRehash(int steps) {
        if(!isRehashing()) {
            return false;
        }

        // 每次最多跳过的空桶个数
        int emptyVisits = steps * REHASH_MAX_EMPTY_VISITS;
        while(steps > 0 && ht[0].usedSize > 0) {
            // 此处外层循环控制，一般而言，usedSize 不为0，那么就一定有非空桶，此处就不会越界
            while(rehashIdx < ht[0].size && ht[0].table[rehashIdx] == null) {
                rehashIdx++;
                if(--emptyVisits == 0) {
                    return true;
                }
            }

            // 防止 usedSize 计数错误
            if(rehashIdx >= ht[0].size) {
                break;
            }

            DictEntry<K, V> entry = ht[0].table[rehashIdx];
            while(entry != null) {
                DictEntry<K, V> next = entry.next;

                int hash = doHash(entry.getKey());
                int newIdx = hash & ht[1].sizeMark;

                entry.next = ht[1].table[newIdx];
                ht[1].table[newIdx] = entry;

                ht[0].usedSize--;
                ht[1].usedSize++;

                entry = next;
            }

            ht[0].table[rehashIdx] = null;
            rehashIdx++;

            steps--;
        }

        if(ht[0].usedSize == 0) {
            rehashIdx = -1;
            ht[0] = ht[1];
            ht[1] = null;
            return false;
        }

        return true;
    }

    /**
     * 随机返回一个键值对，用于主动过期（active expire）随机采样。
     *
     * @return 随机 {@link DictEntry}；字典为空时返回 {@code null}
     */
    @Nullable
    public DictEntry<K, V> dictGetRandomKey() {
        if(dictSize() == 0) {
            return null;
        }

        List<DictEntry<K, V>> allEntries = dictGetAll();
        return allEntries.get(ThreadLocalRandom.current().nextInt(allEntries.size()));
    }

    /**
     * 返回字典中所有键值对（遍历 ht[0] 和 ht[1]）。
     * 调用方不应在遍历期间修改字典。
     */
    @NonNull
    public List<DictEntry<K, V>> dictGetAll() {
        List<DictEntry<K, V>> list = new ArrayList<>();
        for(int i = 0; i < 2; i++) {
            if(ht[i] == null || ht[i].size == 0) {
                continue;
            }

            for(DictEntry<K, V> entry : ht[i].table) {
                while(entry != null) {
                    list.add(entry);
                    entry = entry.next;
                }
            }
        }

        return list;
    }

    /**
     * 缩容到最小必要大小（不低于 INITIAL_SIZE）。
     */
    public void dictResize() {
        if(isRehashing()) {
            return;
        }

        int size = Math.max(ht[0].usedSize, DEFAULT_INITIAL_CAPACITY);
        doDictExpand(size);
    }

    /**
     * 为 key 分配新节点并插入，返回该节点（未设值）。
     * key 已存在时返回 null。
     * rehash 期间新节点插入 ht[1]。
     */
    private DictEntry<K, V> dictAddRaw(K key) {
        // 渐进式 rehash，每次哈希一个
        if(isRehashing()) {
            dictRehash(1);
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

        expandIfNeeded();

        return newEntry;
    }

    /**
     * 检查是否需要扩容：used >= size * LOAD_FACTOR_EXPAND 时触发。
     */
    private void expandIfNeeded() {
        if(isRehashing()) {
            return;
        }

        // 到上限之后不扩容
        if(ht[0].size == Integer.MAX_VALUE) {
            return;
        }

        // long 是为了防止范围溢出
        if(ht[0].usedSize >= (long) ht[0].size * LOAD_FACTOR) {
            doDictExpand((long) ht[0].usedSize * 2);
        }
    }

    /**
     * 检查是否需要缩容：used * SHRINK_RATIO < size 时触发。
     * 不缩容到 INITIAL_SIZE 以下。
     */
    private void shrinkIfNeeded() {
        if(isRehashing()) {
            return;
        }

        if(ht[0].size > DEFAULT_INITIAL_CAPACITY && (long) ht[0].usedSize * SHRINK_FACTOR < ht[0].size) {
            doDictExpand(ht[0].usedSize);
        }
    }

    /**
     * 触发扩容或缩容：分配 ht[1] 并启动渐进式 rehash。
     * 目标大小向上取到最近的 2 的幂次。
     */
    private void doDictExpand(long size) {
        if(isRehashing()) {
            return;
        }

        int newSize = nextPowerOfTwo(size);
        // 可能会出现在手动 resize
        if(newSize == ht[0].size) {
            return;
        }

        ht[1] = new DictHashTable<>(newSize);
        rehashIdx = 0;
    }

    /**
     * 返回大于等于 size 的最小 2 的幂次<br/>
     * 最小为 {@code DEFAULT_INITIAL_CAPACITY}，最大为 {@code Integer.MAX_VALUE}
     */
    private int nextPowerOfTwo(long x) {
        if(x < DEFAULT_INITIAL_CAPACITY) {
            return DEFAULT_INITIAL_CAPACITY;
        }

        long n = DEFAULT_INITIAL_CAPACITY;
        while(n < x) {
            n <<= 1;
        }

        return n <= Integer.MAX_VALUE ? (int) n : Integer.MAX_VALUE;
    }

    /**
     * 计算 key 的哈希值，委托给外部注入的 hashFunction。
     */
    private int doHash(K key) {
        return hashFunction.apply(key);
    }
}

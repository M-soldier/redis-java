package com.maio.redis.dataStructure.dict;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 念兮
 * @date 2026-06-29
 * @email msj@bupt.edu.cn
 */
class DictTest {

    private Dict<String, Integer> dict;

    @BeforeEach
    void setUp() {
        dict = Dict.dictCreate();
    }

    // ------------------------------------------------------------------ 基本 CRUD
    @Nested
    class BasicCrud {

        @Test
        void dictAdd_addsEntry() {
            assertTrue(dict.dictAdd("key", 1));
            assertEquals(1, dict.dictSize());
        }

        @Test
        void dictAdd_duplicateKey_returnsFalse_andKeepsOriginalValue() {
            dict.dictAdd("key", 1);
            assertFalse(dict.dictAdd("key", 99));
            assertEquals(1, dict.dictFetchValue("key"));
        }

        @Test
        void dictFind_existingKey_returnsEntry() {
            dict.dictAdd("key", 42);
            DictEntry<String, Integer> entry = dict.dictFind("key");
            assertNotNull(entry);
            assertEquals("key", entry.getKey());
            assertEquals(42, entry.getValue());
        }

        @Test
        void dictFind_missingKey_returnsNull() {
            assertNull(dict.dictFind("missing"));
        }

        @Test
        void dictFetchValue_existingKey() {
            dict.dictAdd("a", 100);
            assertEquals(100, dict.dictFetchValue("a"));
        }

        @Test
        void dictFetchValue_missingKey_returnsNull() {
            assertNull(dict.dictFetchValue("missing"));
        }

        @Test
        void dictDelete_existingKey_removesAndReturnsTrue() {
            dict.dictAdd("key", 1);
            assertTrue(dict.dictDelete("key"));
            assertEquals(0, dict.dictSize());
            assertNull(dict.dictFind("key"));
        }

        @Test
        void dictDelete_missingKey_returnsFalse() {
            assertFalse(dict.dictDelete("missing"));
        }

        @Test
        void dictDelete_updatesSize() {
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictDelete("a");
            assertEquals(1, dict.dictSize());
        }
    }

    // ------------------------------------------------------------------ dictReplace
    @Nested
    class Replace {

        @Test
        void dictReplace_newKey_addsAndReturnsTrue() {
            assertTrue(dict.dictReplace("key", 1));
            assertEquals(1, dict.dictFetchValue("key"));
        }

        @Test
        void dictReplace_existingKey_updatesValueAndReturnsFalse() {
            dict.dictAdd("key", 1);
            assertFalse(dict.dictReplace("key", 99));
            assertEquals(99, dict.dictFetchValue("key"));
        }

        @Test
        void dictReplace_doesNotChangeSizeWhenUpdating() {
            dict.dictAdd("key", 1);
            dict.dictReplace("key", 2);
            assertEquals(1, dict.dictSize());
        }
    }

    // ------------------------------------------------------------------ 参数校验
    @Nested
    class Validation {

        @Test
        void dictAdd_nullKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> dict.dictAdd(null, 1));
        }

        @Test
        void dictReplace_nullKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> dict.dictReplace(null, 1));
        }

        @Test
        void dictFind_nullKey_returnsNull() {
            assertNull(dict.dictFind(null));
        }

        @Test
        void dictDelete_nullKey_returnsFalse() {
            assertFalse(dict.dictDelete(null));
        }
    }

    // ------------------------------------------------------------------ 大小
    @Nested
    class Size {

        @Test
        void dictSize_emptyDict_returnsZero() {
            assertEquals(0, dict.dictSize());
        }

        @Test
        void dictSize_afterMultipleAdds() {
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            assertEquals(3, dict.dictSize());
        }

        @Test
        void dictSize_countsAcrossBothTablesWhenRehashing() {
            // 插入足够多的 key 触发 rehash
            for (int i = 0; i < 10; i++) {
                dict.dictAdd("k" + i, i);
            }
            // 无论是否在 rehash，总大小应始终正确
            assertEquals(10, dict.dictSize());
        }
    }

    // ------------------------------------------------------------------ 渐进式 rehash
    @Nested
    class Rehash {

        @Test
        void expand_triggeredWhenLoadFactorExceeded() {
            // INITIAL_SIZE = 4，插入第 4 个 key 后 used >= size，触发扩容
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            dict.dictAdd("d", 4);
            // 扩容已触发，rehashidx >= 0
            assertTrue(dict.isRehashing());
        }

        @Test
        void allKeysAccessible_duringRehash() {
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            dict.dictAdd("d", 4); // 触发 rehash
            assertTrue(dict.isRehashing());
            // rehash 期间查找，应同时搜索 ht[0] 和 ht[1]
            assertEquals(1, dict.dictFetchValue("a"));
            assertEquals(2, dict.dictFetchValue("b"));
            assertEquals(3, dict.dictFetchValue("c"));
            assertEquals(4, dict.dictFetchValue("d"));
        }

        @Test
        void dictRehash_completesAndAllKeysAccessible() {
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            dict.dictAdd("d", 4);
            assertTrue(dict.isRehashing());

            dict.dictRehash(100); // 强制完成
            assertFalse(dict.isRehashing());
            assertEquals(4, dict.dictSize());
            assertEquals(1, dict.dictFetchValue("a"));
            assertEquals(4, dict.dictFetchValue("d"));
        }

        @Test
        void rehash_completelyTransparentToCallers() {
            // 大量插入会触发多次 expand，对调用方完全透明
            for (int i = 0; i < 100; i++) {
                dict.dictAdd("key" + i, i);
            }
            assertEquals(100, dict.dictSize());
            for (int i = 0; i < 100; i++) {
                assertEquals(i, dict.dictFetchValue("key" + i));
            }
        }

        @Test
        void newEntries_duringRehash_goIntoHt1() {
            // 触发 rehash 后，新增的 key 应进入 ht[1]，通过 dictRehash 完成后仍可找到
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            dict.dictAdd("d", 4); // 触发 rehash
            assertTrue(dict.isRehashing());

            // rehash 期间插入新 key，应写入 ht[1]
            dict.dictAdd("new", 999);
            assertEquals(5, dict.dictSize());

            dict.dictRehash(100);
            assertFalse(dict.isRehashing());
            assertEquals(999, dict.dictFetchValue("new"));
        }

        @Test
        void delete_duringRehash_worksCorrectly() {
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            dict.dictAdd("d", 4); // 触发 rehash

            assertTrue(dict.dictDelete("a"));
            assertNull(dict.dictFind("a"));
            assertEquals(3, dict.dictSize());

            dict.dictRehash(100);
            assertFalse(dict.isRehashing());
            assertNull(dict.dictFetchValue("a"));
            assertEquals(2, dict.dictFetchValue("b"));
        }

        @Test
        void dictIsNotRehashing_initially() {
            assertFalse(dict.isRehashing());
        }
    }

    // ------------------------------------------------------------------ 缩容
    @Nested
    class Shrink {

        @Test
        void shrink_triggeredByLowLoadFactor() {
            // 插入 50 个 key，然后删掉大多数
            for (int i = 0; i < 50; i++) dict.dictAdd("k" + i, i);
            for (int i = 0; i < 46; i++) dict.dictDelete("k" + i);

            // 强制完成任意正在进行的 rehash
            dict.dictRehash(1000);

            // 剩余 4 个 key 仍然可查
            assertEquals(4, dict.dictSize());
            for (int i = 46; i < 50; i++) {
                assertEquals(i, dict.dictFetchValue("k" + i));
            }
        }

        @Test
        void dictResize_shrinkToMinimum() {
            for (int i = 0; i < 20; i++) dict.dictAdd("k" + i, i);
            for (int i = 0; i < 18; i++) dict.dictDelete("k" + i);
            dict.dictRehash(1000); // 完成已触发的缩容 rehash

            dict.dictResize(); // 手动触发缩容到最小
            dict.dictRehash(1000);
            assertFalse(dict.isRehashing());
            assertEquals(2, dict.dictSize());
        }
    }

    // ------------------------------------------------------------------ 随机 key
    @Nested
    class RandomKey {

        @Test
        void dictGetRandomKey_returnsValidEntry() {
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            DictEntry<String, Integer> entry = dict.dictGetRandomKey();
            assertNotNull(entry);
            // 返回的 key 必须在字典中存在
            assertNotNull(dict.dictFind(entry.getKey()));
        }

        @Test
        void dictGetRandomKey_emptyDict_returnsNull() {
            assertNull(dict.dictGetRandomKey());
        }

        @Test
        void dictGetRandomKey_singleEntry_alwaysReturnsThatEntry() {
            dict.dictAdd("only", 42);
            for (int i = 0; i < 10; i++) {
                DictEntry<String, Integer> entry = dict.dictGetRandomKey();
                assertNotNull(entry);
                assertEquals("only", entry.getKey());
            }
        }
    }

    // ------------------------------------------------------------------ dictGetAll
    @Nested
    class GetAll {

        @Test
        void dictGetAll_emptyDict_returnsEmptyList() {
            assertTrue(dict.dictGetAll().isEmpty());
        }

        @Test
        void dictGetAll_returnsAllEntries() {
            dict.dictAdd("x", 10);
            dict.dictAdd("y", 20);
            dict.dictAdd("z", 30);
            List<DictEntry<String, Integer>> all = dict.dictGetAll();
            assertEquals(3, all.size());
        }

        @Test
        void dictGetAll_duringRehash_includesBothTables() {
            dict.dictAdd("a", 1);
            dict.dictAdd("b", 2);
            dict.dictAdd("c", 3);
            dict.dictAdd("d", 4); // 触发 rehash
            assertTrue(dict.isRehashing());
            // dictGetAll 应遍历 ht[0] 和 ht[1]，不遗漏
            List<DictEntry<String, Integer>> all = dict.dictGetAll();
            assertEquals(4, all.size());
        }
    }

    // ------------------------------------------------------------------ 自定义哈希函数
    @Nested
    class CustomHashFunction {

        @Test
        void dictCreate_withCustomHashFunction_usesIt() {
            // 大小写不敏感：命令表场景
            Dict<String, Integer> commandTable = Dict.dictCreate(
                    k -> k.toLowerCase().hashCode() & 0x7FFFFFFF
            );
            commandTable.dictAdd("GET", 1);
            // 用同一个 key 能找到
            assertNotNull(commandTable.dictFind("GET"));
            assertEquals(1, commandTable.dictFetchValue("GET"));
        }

        @Test
        void dictCreate_defaultHashFunction_behavesCorrectly() {
            Dict<String, Integer> d = Dict.dictCreate();
            d.dictAdd("hello", 42);
            assertEquals(42, d.dictFetchValue("hello"));
        }
    }
}

package com.maio.redis.dataStructure.list;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 念兮
 * @date 2026/6/28
 * @email msj@bupt.edu.cn
 */
class RedisListTest {

    private RedisList<Integer> list;

    @BeforeEach
    void setUp() {
        list = RedisList.listCreate();
    }

    // ------------------------------------------------------------------ 创建
    @Test
    void listCreate_returnsEmptyList() {
        assertEquals(0, list.listLength());
        assertNull(list.listFirst());
        assertNull(list.listLast());
    }

    // ------------------------------------------------------------------ 头尾插入
    @Nested
    class AddNode {

        @Test
        void listAddNodeHead_singleNode() {
            list.listAddNodeHead(1);
            assertEquals(1, list.listLength());
            assertEquals(1, Objects.requireNonNull(list.listFirst()).getValue());
            assertEquals(1, Objects.requireNonNull(list.listLast()).getValue());
        }

        @Test
        void listAddNodeHead_multipleNodes_orderedCorrectly() {
            list.listAddNodeHead(1);
            list.listAddNodeHead(2);
            list.listAddNodeHead(3);
            assertEquals(3, list.listLength());
            assertEquals(3, Objects.requireNonNull(list.listFirst()).getValue());
            assertEquals(1, Objects.requireNonNull(list.listLast()).getValue());
        }

        @Test
        void listAddNodeTail_multipleNodes_orderedCorrectly() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);
            assertEquals(3, list.listLength());
            assertEquals(1, Objects.requireNonNull(list.listFirst()).getValue());
            assertEquals(3, Objects.requireNonNull(list.listLast()).getValue());
        }

        @Test
        void listAddNodeHead_nullValue_accepted() {
            RedisList<String> strList = RedisList.listCreate();
            strList.listAddNodeHead(null);
            assertEquals(1, strList.listLength());
            assertNull(Objects.requireNonNull(strList.listFirst()).getValue());
        }
    }

    // ------------------------------------------------------------------ 前后节点
    @Nested
    class PrevNext {

        @Test
        void listPrevNode_firstNode_returnsNull() {
            list.listAddNodeTail(1);
            assertNull(list.listPrevNode(list.listFirst()));
        }

        @Test
        void listNextNode_lastNode_returnsNull() {
            list.listAddNodeTail(1);
            assertNull(list.listNextNode(list.listLast()));
        }

        @Test
        void listPrevNode_middleNode_returnsCorrect() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);
            assertEquals(1, Objects.requireNonNull(list.listPrevNode(list.listIndex(1))).getValue());
            assertEquals(3, Objects.requireNonNull(list.listNextNode(list.listIndex(1))).getValue());
        }

        @Test
        void listPrevNode_null_returnsNull() {
            assertNull(list.listPrevNode(null));
            assertNull(list.listNextNode(null));
        }
    }

    // ------------------------------------------------------------------ 插入
    @Nested
    class InsertNode {

        @Test
        void listInsertBeforeNode_insertsCorrectly() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(3);
            assertTrue(list.listInsertBeforeNode(2, list.listLast()));
            assertEquals(3, list.listLength());
            assertEquals(2, Objects.requireNonNull(list.listIndex(1)).getValue());
        }

        @Test
        void listInsertAfterNode_insertsCorrectly() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(3);
            assertTrue(list.listInsertAfterNode(2, list.listFirst()));
            assertEquals(3, list.listLength());
            assertEquals(2, Objects.requireNonNull(list.listIndex(1)).getValue());
        }

        @Test
        void listInsertBeforeNode_nullNode_returnsFalse() {
            assertFalse(list.listInsertBeforeNode(1, null));
        }

        @Test
        void listInsertAfterNode_nullNode_returnsFalse() {
            assertFalse(list.listInsertAfterNode(1, null));
        }

        @Test
        void listInsertBeforeNode_beforeFirst_becomesFirst() {
            list.listAddNodeTail(2);
            assertTrue(list.listInsertBeforeNode(1, list.listFirst()));
            assertEquals(1, Objects.requireNonNull(list.listFirst()).getValue());
        }

        @Test
        void listInsertAfterNode_afterLast_becomesLast() {
            list.listAddNodeTail(1);
            assertTrue(list.listInsertAfterNode(2, list.listLast()));
            assertEquals(2, Objects.requireNonNull(list.listLast()).getValue());
        }
    }

    // ------------------------------------------------------------------ 查找
    @Nested
    class SearchKey {

        @Test
        void listSearchKey_findsFirstMatch() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(2);
            ListNode<Integer> found = list.listSearchKey(2);
            assertNotNull(found);
            // 返回第一个匹配节点，其前驱值为 1
            assertEquals(1, Objects.requireNonNull(list.listPrevNode(found)).getValue());
        }

        @Test
        void listSearchKey_notFound_returnsNull() {
            list.listAddNodeTail(1);
            assertNull(list.listSearchKey(99));
        }

        @Test
        void listSearchKey_emptyList_returnsNull() {
            assertNull(list.listSearchKey(1));
        }

        @Test
        void listSearchKey_nullValue_supported() {
            RedisList<String> strList = RedisList.listCreate();
            strList.listAddNodeTail(null);
            strList.listAddNodeTail("hello");
            assertNotNull(strList.listSearchKey(null));
            assertNull(strList.listSearchKey("world"));
        }
    }

    // ------------------------------------------------------------------ 下标访问
    @Nested
    class Index {

        @BeforeEach
        void addThreeNodes() {
            list.listAddNodeTail(10);
            list.listAddNodeTail(20);
            list.listAddNodeTail(30);
        }

        @Test
        void listIndex_positiveIndex() {
            assertEquals(10, Objects.requireNonNull(list.listIndex(0)).getValue());
            assertEquals(20, Objects.requireNonNull(list.listIndex(1)).getValue());
            assertEquals(30, Objects.requireNonNull(list.listIndex(2)).getValue());
        }

        @Test
        void listIndex_negativeIndex() {
            assertEquals(30, Objects.requireNonNull(list.listIndex(-1)).getValue());
            assertEquals(20, Objects.requireNonNull(list.listIndex(-2)).getValue());
            assertEquals(10, Objects.requireNonNull(list.listIndex(-3)).getValue());
        }

        @Test
        void listIndex_outOfBounds_returnsNull() {
            assertNull(list.listIndex(3));
            assertNull(list.listIndex(-4));
        }

        @Test
        void listIndex_emptyList_returnsNull() {
            RedisList<Integer> empty = RedisList.listCreate();
            assertNull(empty.listIndex(0));
            assertNull(empty.listIndex(-1));
        }
    }

    // ------------------------------------------------------------------ 删除
    @Nested
    class DelNode {

        @Test
        void listDelNode_middleNode_removesCorrectly() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);
            list.listDelNode(list.listIndex(1));
            assertEquals(2, list.listLength());
            assertEquals(1, Objects.requireNonNull(list.listFirst()).getValue());
            assertEquals(3, Objects.requireNonNull(list.listLast()).getValue());
        }

        @Test
        void listDelNode_firstNode() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listDelNode(list.listFirst());
            assertEquals(1, list.listLength());
            assertEquals(2, Objects.requireNonNull(list.listFirst()).getValue());
        }

        @Test
        void listDelNode_lastNode() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listDelNode(list.listLast());
            assertEquals(1, list.listLength());
            assertEquals(1, Objects.requireNonNull(list.listLast()).getValue());
        }

        @Test
        void listDelNode_onlyNode_listBecomesEmpty() {
            list.listAddNodeTail(42);
            list.listDelNode(list.listFirst());
            assertEquals(0, list.listLength());
            assertNull(list.listFirst());
            assertNull(list.listLast());
        }

        @Test
        void listDelNode_null_returnsTrue() {
            assertTrue(list.listDelNode(null));
        }
    }

    // ------------------------------------------------------------------ 旋转
    @Nested
    class Rotate {

        @Test
        void listRotateTailToHead_movesLastToFirst() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);
            list.listRotateTailToHead();
            assertEquals(3, list.listLength());
            assertEquals(3, Objects.requireNonNull(list.listFirst()).getValue());
            assertEquals(2, Objects.requireNonNull(list.listLast()).getValue());
            // 验证双向链接完整性
            assertNull(list.listPrevNode(list.listFirst()));
            assertNull(list.listNextNode(list.listLast()));
            assertEquals(1, Objects.requireNonNull(list.listNextNode(list.listFirst())).getValue());
        }

        @Test
        void listRotateTailToHead_singleElement_noChange() {
            list.listAddNodeTail(42);
            list.listRotateTailToHead();
            assertEquals(1, list.listLength());
            assertEquals(42, Objects.requireNonNull(list.listFirst()).getValue());
        }

        @Test
        void listRotateTailToHead_emptyList_noOp() {
            list.listRotateTailToHead();
            assertEquals(0, list.listLength());
        }
    }

    // ------------------------------------------------------------------ 清空
    @Nested
    class Empty {

        @Test
        void listEmpty_clearsAllNodes() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);
            list.listEmpty();
            assertEquals(0, list.listLength());
            assertNull(list.listFirst());
            assertNull(list.listLast());
        }

        @Test
        void listEmpty_onEmptyList_noOp() {
            list.listEmpty();
            assertEquals(0, list.listLength());
        }
    }

    // ------------------------------------------------------------------ 复制
    @Nested
    class Dup {

        @Test
        void listDup_createsCorrectCopy() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);
            RedisList<Integer> copy = list.listDup();
            assertEquals(3, copy.listLength());
            assertEquals(1, Objects.requireNonNull(copy.listFirst()).getValue());
            assertEquals(3, Objects.requireNonNull(copy.listLast()).getValue());
        }

        @Test
        void listDup_isIndependent() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            RedisList<Integer> copy = list.listDup();
            list.listEmpty();
            // 原链表清空后，副本不受影响
            assertEquals(2, copy.listLength());
        }

        @Test
        void listDup_emptyList_returnsEmptyList() {
            RedisList<Integer> copy = list.listDup();
            assertEquals(0, copy.listLength());
            assertNull(copy.listFirst());
        }
    }

    // ------------------------------------------------------------------ 合并
    @Nested
    class Join {

        @Test
        void listJoin_appendsAllNodes() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            RedisList<Integer> other = RedisList.listCreate();
            other.listAddNodeTail(3);
            other.listAddNodeTail(4);

            list.listJoin(other);

            assertEquals(4, list.listLength());
            assertEquals(1, Objects.requireNonNull(list.listFirst()).getValue());
            assertEquals(4, Objects.requireNonNull(list.listLast()).getValue());
        }

        @Test
        void listJoin_otherListIsCleared() {
            list.listAddNodeTail(1);
            RedisList<Integer> other = RedisList.listCreate();
            other.listAddNodeTail(2);

            list.listJoin(other);

            assertEquals(0, other.listLength());
            assertNull(other.listFirst());
        }

        @Test
        void listJoin_emptyOther_noChange() {
            list.listAddNodeTail(1);
            list.listJoin(RedisList.listCreate());
            assertEquals(1, list.listLength());
        }

        @Test
        void listJoin_bidirectionalLinksCorrect() {
            list.listAddNodeTail(1);
            RedisList<Integer> other = RedisList.listCreate();
            other.listAddNodeTail(2);
            other.listAddNodeTail(3);

            list.listJoin(other);

            // 验证拼接处双向链接完整
            assertEquals(2, Objects.requireNonNull(list.listNextNode(list.listIndex(0))).getValue());
            assertEquals(1, Objects.requireNonNull(list.listPrevNode(list.listIndex(1))).getValue());
            assertNull(list.listNextNode(list.listLast()));
        }
    }

    // ------------------------------------------------------------------ LinkNode
    @Nested
    class LinkNode {

        @Test
        void listLinkNodeHead_reattachesDetachedNode() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);

            ListNode<Integer> node = list.listFirst(); // 值为 1
            list.listDelNode(node);
            assertEquals(2, list.listLength());

            list.listLinkNodeHead(node);
            assertEquals(3, list.listLength());
            assertEquals(1, Objects.requireNonNull(list.listFirst()).getValue());
            assertNull(list.listPrevNode(list.listFirst()));
        }

        @Test
        void listLinkNodeTail_reattachesDetachedNode() {
            list.listAddNodeTail(1);
            list.listAddNodeTail(2);
            list.listAddNodeTail(3);

            ListNode<Integer> node = list.listLast(); // 值为 3
            list.listDelNode(node);
            assertEquals(2, list.listLength());

            list.listLinkNodeTail(node);
            assertEquals(3, list.listLength());
            assertEquals(3, Objects.requireNonNull(list.listLast()).getValue());
            assertNull(list.listNextNode(list.listLast()));
        }

        @Test
        void listLinkNodeHead_null_noOp() {
            list.listAddNodeTail(1);
            list.listLinkNodeHead(null);
            assertEquals(1, list.listLength());
        }

        @Test
        void listLinkNodeTail_null_noOp() {
            list.listAddNodeTail(1);
            list.listLinkNodeTail(null);
            assertEquals(1, list.listLength());
        }
    }
}

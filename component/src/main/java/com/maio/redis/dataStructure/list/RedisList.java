package com.maio.redis.dataStructure.list;

import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * Redis 双向链表，对应 Redis 源码中的 adlist（A generic doubly linked list）。
 *
 * <p>使用带哨兵节点的双向链表实现：{@code head} 和 {@code tail} 为永久存在的哨兵节点，
 * 不存储用户数据，可简化边界处理逻辑，避免对空链表进行特殊判断。
 *
 * <p>命名风格与 Redis C 源码（adlist.h / adlist.c）保持一致，便于对照学习。
 *
 * @param <V> 链表节点存储的值类型
 *
 * @author 念兮
 * @date 2026-06-26
 * @email msj@bupt.edu.cn
 * @see ListNode
 */
public class RedisList<V> {
    private int len = 0;

    private final ListNode<V> head = new ListNode<>();
    private final ListNode<V> tail = new ListNode<>();

    /**
     * 创建并返回一个空链表。
     *
     * @param <V> 节点值类型
     * @return 新建的空链表
     */
    public static <V> RedisList<V> listCreate() {
        return new RedisList<>();
    }

    RedisList() {
        this.head.setNext(tail);
        this.tail.setPrev(head);
    }

    /**
     * 返回链表的节点数量。
     *
     * @return 节点数量
     */
    public int listLength() {
        return this.len;
    }

    /**
     * 返回链表的第一个节点。
     *
     * @return 第一个节点，链表为空时返回 {@code null}
     */
    @Nullable
    public ListNode<V> listFirst() {
        if(this.head.getNext() == this.tail) {
            return null;
        }

        return this.head.getNext();
    }

    /**
     * 返回链表的最后一个节点。
     *
     * @return 最后一个节点，链表为空时返回 {@code null}
     */
    @Nullable
    public ListNode<V> listLast() {
        if(this.head.getNext() == this.tail) {
            return null;
        }

        return this.tail.getPrev();
    }

    /**
     * 返回给定节点的前驱节点。
     *
     * @param node 目标节点
     * @return 前驱节点；若 {@code node} 为 {@code null} 或已是第一个节点则返回 {@code null}
     */
    @Nullable
    public ListNode<V> listPrevNode(ListNode<V> node) {
        if(node == null) {
            return null;
        }

        return node.getPrev() == this.head ? null : node.getPrev();
    }

    /**
     * 返回给定节点的后继节点。
     *
     * @param node 目标节点
     * @return 后继节点；若 {@code node} 为 {@code null} 或已是最后一个节点则返回 {@code null}
     */
    @Nullable
    public ListNode<V> listNextNode(ListNode<V> node) {
        if(node == null) {
            return null;
        }

        return node.getNext() == this.tail ? null : node.getNext();
    }

    /**
     * 在链表头部插入一个新节点。
     *
     * @param value 新节点的值
     */
    public void listAddNodeHead(V value) {
        ListNode<V> node = new ListNode<>(value);

        node.setNext(this.head.getNext());
        this.head.getNext().setPrev(node);

        node.setPrev(this.head);
        this.head.setNext(node);

        this.len += 1;
    }

    /**
     * 在链表尾部插入一个新节点。
     *
     * @param value 新节点的值
     */
    public void listAddNodeTail(V value) {
        ListNode<V> node = new ListNode<>(value);

        node.setPrev(this.tail.getPrev());
        this.tail.getPrev().setNext(node);

        node.setNext(this.tail);
        this.tail.setPrev(node);

        this.len += 1;
    }

    /**
     * 在指定节点之前插入一个新节点。
     *
     * @param value 新节点的值
     * @param node  基准节点
     * @return 插入成功返回 {@code true}；{@code node} 为 {@code null} 时返回 {@code false}
     */
    public boolean listInsertBeforeNode(V value, ListNode<V> node) {
        if(node == null) {
            return false;
        }

        ListNode<V> newNode = new ListNode<>(value);

        newNode.setNext(node);
        newNode.setPrev(node.getPrev());

        node.getPrev().setNext(newNode);
        node.setPrev(newNode);

        this.len += 1;

        return true;
    }

    /**
     * 在指定节点之后插入一个新节点。
     *
     * @param value 新节点的值
     * @param node  基准节点
     * @return 插入成功返回 {@code true}；{@code node} 为 {@code null} 时返回 {@code false}
     */
    public boolean listInsertAfterNode(V value, ListNode<V> node) {
        if(node == null) {
            return false;
        }

        ListNode<V> newNode = new ListNode<>(value);

        newNode.setNext(node.getNext());
        newNode.setPrev(node);

        node.getNext().setPrev(newNode);
        node.setNext(newNode);

        this.len += 1;

        return true;
    }

    /**
     * 从头部开始遍历，返回第一个值与 {@code value} 相等的节点。
     * 使用 {@link Objects#equals} 比较，支持 {@code null} 值。
     *
     * @param value 目标值
     * @return 匹配的节点；未找到时返回 {@code null}
     */
    @Nullable
    public ListNode<V> listSearchKey(V value) {

        ListNode<V> p = head.getNext();
        while (p != tail) {
            // 注意：p.getValue().equals(value) 可能会出现 NPE
            if(Objects.equals(p.getValue(), value)) {
                return p;
            }
            p = p.getNext();
        }

        return null;
    }

    /**
     * 按下标返回节点，支持正负索引。
     *
     * <ul>
     *   <li>正索引：从 0 开始，0 表示第一个节点</li>
     *   <li>负索引：从 -1 开始，-1 表示最后一个节点</li>
     * </ul>
     *
     * @param index 下标
     * @return 对应节点；越界时返回 {@code null}
     */
    @Nullable
    public ListNode<V> listIndex(int index) {
        if(index >= this.len || index < -this.len) {
            return null;
        }

        ListNode<V> p;
        if(index < 0) {
            p = this.tail.getPrev();
            for(int i = -1; i > index; i--) {
                p = p.getPrev();
            }

        } else {
            p = this.head.getNext();
            for(int i = 0; i < index; i++) {
                p = p.getNext();
            }

        }
        return p;
    }

    /**
     * 从链表中删除节点，并将其 prev/next 置为 {@code null}。
     *
     * <p>若 {@code node} 为 {@code null} 或已经脱链（prev/next 为 {@code null}），
     * 视为无操作，返回 {@code true}。
     *
     * @param node 待删除的节点
     * @return 始终返回 {@code true}
     */
    public boolean listDelNode(ListNode<V> node) {
        if(node == null || node.getPrev() == null || node.getNext() == null) {
            return true;
        }

        node.getPrev().setNext(node.getNext());
        node.getNext().setPrev(node.getPrev());

        node.setPrev(null);
        node.setNext(null);

        this.len -= 1;

        return true;
    }

    /**
     * 将尾节点移动到链表头部（Rotate tail to head）。
     * 链表为空时无操作。
     */
    public void listRotateTailToHead() {
        if(this.head.getNext() == this.tail) {
            return;
        }

        ListNode<V> node = this.tail.getPrev();

        node.getPrev().setNext(node.getNext());
        node.getNext().setPrev(node.getPrev());

        node.setNext(this.head.getNext());
        this.head.getNext().setPrev(node);

        this.head.setNext(node);
        node.setPrev(this.head);
    }

    /**
     * 清空链表中的所有节点，重置长度为 0。
     * 链表为空时无操作。
     */
    public void listEmpty() {
        if(this.head.getNext() == this.tail) {
            return;
        }

        this.head.getNext().setPrev(null);
        this.tail.getPrev().setNext(null);

        this.head.setNext(this.tail);
        this.tail.setPrev(this.head);

        this.len = 0;
    }

    /**
     * 深拷贝当前链表，返回一个包含相同值的新链表。
     * 仅复制节点的值引用，不对值本身做深拷贝。
     *
     * @return 新链表
     */
    public RedisList<V> listDup() {
        RedisList<V> newList = RedisList.listCreate();
        ListNode<V> p = this.head.getNext();

        while (p != this.tail) {
            newList.listAddNodeTail(p.getValue());
            p = p.getNext();
        }

        return newList;
    }

    /**
     * 将 {@code otherList} 的所有节点追加到当前链表尾部。
     * 操作完成后 {@code otherList} 被清空（长度归零）。
     * 若 {@code otherList} 为空则无操作。
     *
     * @param otherList 待合并的链表
     */
    public void listJoin(RedisList<V> otherList) {
        if(otherList.getHead().getNext() == otherList.getTail()) {
            return;
        }

        this.tail.getPrev().setNext(otherList.getHead().getNext());
        otherList.getHead().getNext().setPrev(this.tail.getPrev());

        this.tail.setPrev(otherList.getTail().getPrev());
        otherList.getTail().getPrev().setNext(this.tail);

        otherList.getHead().setNext(otherList.getTail());
        otherList.getTail().setPrev(otherList.getHead());

        this.len += otherList.listLength();
        otherList.setLen(0);
    }


    /**
     * 将一个已脱链的节点挂入链表头部。
     *
     * <p>调用方须保证 {@code node} 已通过 {@link #listDelNode} 从原链表摘除
     * （即 prev/next 均为 {@code null}），否则会破坏原链表结构。
     *
     * @param node 待挂入的节点；为 {@code null} 时无操作
     */
    public void listLinkNodeHead(ListNode<V> node) {
        if(node == null) {
            return;
        }

        // 注意函数的单一职责，此处不应该处理脱链逻辑，会导致 len 错误
        this.head.getNext().setPrev(node);
        node.setNext(this.head.getNext());

        this.head.setNext(node);
        node.setPrev(this.head);

        this.len += 1;
    }

    /**
     * 将一个已脱链的节点挂入链表尾部。
     *
     * <p>调用方须保证 {@code node} 已通过 {@link #listDelNode} 从原链表摘除
     * （即 prev/next 均为 {@code null}），否则会破坏原链表结构。
     *
     * @param node 待挂入的节点；为 {@code null} 时无操作
     */
    public void listLinkNodeTail(ListNode<V> node) {
        if(node == null) {
            return;
        }

        // 注意函数的单一职责，此处不应该处理脱链逻辑，会导致 len 错误
        this.tail.getPrev().setNext(node);
        node.setPrev(this.tail.getPrev());

        this.tail.setPrev(node);
        node.setNext(this.tail);

        this.len += 1;
    }

    protected ListNode<V> getHead() {
        return head;
    }

    protected ListNode<V> getTail() {
        return tail;
    }

    protected void setLen(int len) {
        this.len = len;
    }
}

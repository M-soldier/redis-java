package com.maio.redis.dataStructure.list;

/**
 * @author 念兮
 * @date 2026/6/28
 * @email msj@bupt.edu.cn
 */
public class ListNode<V> {
    private V value;

    private ListNode<V> prev;
    private ListNode<V> next;

    public ListNode() {
    }

    public ListNode(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ListNode{" +
                "value=" + value +
                "}";
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public ListNode<V> getPrev() {
        return prev;
    }

    public void setPrev(ListNode<V> prev) {
        this.prev = prev;
    }

    public ListNode<V> getNext() {
        return next;
    }

    public void setNext(ListNode<V> next) {
        this.next = next;
    }
}

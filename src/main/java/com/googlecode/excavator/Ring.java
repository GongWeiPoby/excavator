package com.googlecode.excavator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Ringά������һ��˫������Ļ��ṹ �̰߳�ȫ
 *
 * @author vlinux
 *
 */
public class Ring<T> {

    /**
     * ���ݽڵ�
     *
     * @author vlinux
     * @param <T>
     */
    private class Node {

        Node front;			//ǰ�ڵ�
        Node next;			//��ڵ�
        T data;				//�ڵ�Я��������
    }

    private Node current;	//��ǰ�ڵ�
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(false);

    /**
     * ��ʼ������ѭ������
     */
    public Ring() {

    }

    /**
     * �ж��Ƿ���һ���յĻ�
     *
     * @return
     */
    public boolean isEmpty() {
        return null == current;
    }

    /**
     * ���ε�next
     *
     * @return
     */
    public T ring() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        rwLock.readLock().lock();
        try {
            current = current.next;
        } finally {
            rwLock.readLock().unlock();
        }
        return current.data;
    }

    /**
     * ����һ������
     *
     * @param t
     */
    public void insert(T t) {
        Node node = new Node();
        node.data = t;

        rwLock.writeLock().lock();
        try {
            // ��һ������Ľڵ�Ҫ��ʼ������ͷ�ڵ�
            if (null == current) {
                current = node;
                current.front = current.next = current;
            } // ������ľͰ������
            else {
                node.front = current;
                node.next = current.next;
                current.next.front = node;
                current.next = node;
            }
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    /**
     * ���������������
     */
    public void clean() {
        current = null;
    }

    public Iterator<T> iterator() {
        final Node _current = current;
        return new Iterator<T>() {

            private Node first = null;
            private Node itP = _current;

            @Override
            public boolean hasNext() {
                return first != itP;
            }

            @Override
            public T next() {
                if (null == first) {
                    first = itP;
                }
                itP = itP.next;
                return itP.data;
            }

            @Override
            public void remove() {
                rwLock.writeLock().lock();
                try {
                    // Ҫ�ɵ����һ��Ԫ�أ���ɿջ�
                    if (itP.next == itP) {
                        first = itP = null;
                        clean();
                    } // �����һ��Ԫ�أ��Ͱ��չ����
                    else {
                        itP.next.front = itP.front;
                        itP.front.next = itP.next;
                    }
                } finally {
                    rwLock.writeLock().unlock();
                }

            }

        };
    }

    public static void main(String... args) {

        Ring<String> ring = new Ring<String>();
        ring.insert("luanjia");

        {
            Iterator<String> it = ring.iterator();
            while (it.hasNext()) {
                String name = it.next();
                if (name.equals("luanjia")) {
                    it.remove();
                    System.out.println("nice!");
                }
            }
        }

        {
            for (int i = 0; i < 10; i++) {
                System.out.println("name=" + ring.ring());
            }
        }

    }

}

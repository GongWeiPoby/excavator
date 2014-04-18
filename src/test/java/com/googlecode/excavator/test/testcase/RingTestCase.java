package com.googlecode.excavator.test.testcase;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import com.googlecode.excavator.Ring;

public class RingTestCase {

    private Ring<Integer> buildRing(int total) {
        final Ring<Integer> ring = new Ring<Integer>();
        for( int index=0;index<total;index++ ) {
            ring.insert(index);
        }
        return ring;
    }
    
    /**
     * ����ring������ѭ��
     * @throws Exception
     */
    @Test
    public void test_ring_ring() throws Exception {
        
        final int total = 100000;
        final Ring<Integer> ring = buildRing(total);
        
        for( int tryTimes=0; tryTimes<100; tryTimes++ ) {
            for( int index=total-1;index>=0;index-- ) {
                Assert.assertEquals(index, ring.ring().intValue());
            }
        }
        
    }
 
    /**
     * ����ring��remove
     * @throws Exception
     */
    @Test
    public void test_ring_remove() throws Exception {
        
        final int total = 100000;
        final Ring<Integer> ring = buildRing(total);
        
        Iterator<Integer> it = ring.iterator();
        for( int index=0;index<total;index++ ) {
            it.next();
            it.remove();
        }
        
        Assert.assertTrue(ring.isEmpty());
        
    }
    
    /**
     * ��ring������ʱ������쳣
     * @throws Exception
     */
    @Test(expected=NoSuchElementException.class)
    public void test_ring_empty_throw_NoSuchElementException() throws Exception {
        new Ring<Integer>().ring();
    }
    
    /**
     * ����ring�����Ҳ���remove
     * @throws Exception
     */
    @Test
    public void test_ring_mutil_remove() throws Exception {
        
        final int total = 10000;
        final Ring<Integer> ring = buildRing(total);
        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final CountDownLatch countDown = new CountDownLatch(total);
        final AtomicInteger counter = new AtomicInteger();
        
        for( int index=0;index<total;index++ ) {
            
            final int i = index;
            executorService.execute(new Runnable(){

                @Override
                public void run() {
                    try {
                        final Iterator<Integer> it = ring.iterator();
                        while( it.hasNext() ) {
                            final int e = it.next();
                            if( e == i ) {
                                it.remove();
                                counter.incrementAndGet();
                            }
                        }
                    } finally {
                        countDown.countDown();
                    }
                }
                
            });
        }
        
        countDown.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(ring.isEmpty());
        Assert.assertEquals(counter.get(), total);
        
    }
    
}

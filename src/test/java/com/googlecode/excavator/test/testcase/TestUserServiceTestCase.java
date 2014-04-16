package com.googlecode.excavator.test.testcase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.excavator.test.common.DaoException;
import com.googlecode.excavator.test.common.ErrorCodeConstants;
import com.googlecode.excavator.test.common.TestException;
import com.googlecode.excavator.test.dao.TestUserDao;
import com.googlecode.excavator.test.domain.SingleResultDO;
import com.googlecode.excavator.test.domain.UserDO;
import com.googlecode.excavator.test.mock.MockTestUserDao;
import com.googlecode.excavator.test.service.TestUserService;
import com.googlecode.excavator.test.service.impl.TestUserServiceImpl;

public class TestUserServiceTestCase extends TestCaseNG {

    @Resource
    private TestUserDao testUserDao;
    
    @Resource(name="testUserService")
    private TestUserService testUserService;
    
    @Resource
    private TestUserServiceImpl testUserServiceTarget;
    
    @Test
    public void test_TestUserService() {
        Assert.assertNotNull(testUserService);
        Assert.assertNotNull(testUserServiceTarget);
        Assert.assertNotNull(testUserDao);
    }
    
    /**
     * ��¼ʧ�ܣ��ײ��׳���DAO�쳣
     * @throws Exception
     */
    @Test(expected=UnsupportedOperationException.class)
    public void test_login_throw_UnsupportedOperationException() throws Exception {
        try {
            testUserServiceTarget.setTestUserDao(new MockTestUserDao());
            testUserService.login("username_100000", "password_200000");   
        } finally {
            testUserServiceTarget.setTestUserDao(testUserDao);
        }
    }
    
    /**
     * ��¼ʧ�ܣ��ײ��׳�DAO�쳣
     * @throws Exception
     */
    @Test(expected=TestException.class)
    public void test_login_throw_TestException() throws Exception {
        try {
            testUserServiceTarget.setTestUserDao(new MockTestUserDao(){

                @Override
                public Long indexUserIdByUsername(String username) throws DaoException {
                    throw new DaoException();
                }
                
            });
            testUserService.login("username_100000", "password_200000");   
        } finally {
            testUserServiceTarget.setTestUserDao(testUserDao);
        }
    }
    
    /**
     * ��¼�ɹ�
     * @throws Exception
     */
    @Test
    public void test_login_success() throws Exception {
        final SingleResultDO<UserDO> result = testUserService.login("username_100000", "password_100000");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
    }
    
    /**
     * ��¼ʧ�ܣ��û���������
     * @throws Exception
     */
    @Test
    public void test_login_username_notfound() throws Exception {
        final SingleResultDO<UserDO> result = testUserService.login("username_000000", "password_100000");
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getErrors().containsKey(ErrorCodeConstants.ER_USER_NOT_EXISITED));
    }
    
    /**
     * ��¼ʧ�ܣ��������
     * @throws Exception
     */
    @Test
    public void test_login_auth_failed() throws Exception {
        final SingleResultDO<UserDO> result = testUserService.login("username_100000", "password_200000");
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getErrors().containsKey(ErrorCodeConstants.ER_LOGIN_AUTH_FAILED));
    }
    
    /**
     * ������¼����¼�ɹ�
     * @throws Exception
     */
    @Test
    public void test_mutil_login_auth_success() throws Exception {
        final int start = 100000;
        final int end = 500000;
        final int total = end - start;
        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final AtomicInteger countDown = new AtomicInteger(total);
        final AtomicInteger totalCounter = new AtomicInteger(0);
        final AtomicInteger successCounter = new AtomicInteger(0);
        for( int i=start; i<end; i++ ) {
            final int index = i;
            executorService.execute(new Runnable(){

                @Override
                public void run() {
                    try {
                        final String username = "username_"+index;
                        final String password = "password_"+index;
                        final SingleResultDO<UserDO> result = testUserService.login(username, password);
                        if( result.isSuccess() ) {
                            final UserDO user = result.getData();
                            if( null != user
                                    && user.getUsername().equals(username)
                                    && user.getPassword().equals(password)) {
                                successCounter.incrementAndGet();
                            }
                        }
                    } catch (TestException e) {
                        //
                    } finally {
                        totalCounter.incrementAndGet();
                        countDown.decrementAndGet();
                    }
                    
                }
                
            });
        }//for
        
        while( countDown.get() != 0 );
        Assert.assertEquals(successCounter.get(), totalCounter.get());
    }
    
}

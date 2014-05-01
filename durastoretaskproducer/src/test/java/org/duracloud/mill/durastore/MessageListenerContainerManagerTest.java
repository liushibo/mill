/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.storage.aop.ContentMessageConverter;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

import javax.jms.ConnectionFactory;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Bernstein
 *	       Date: Jan 10, 2014
 */
@RunWith(PowerMockRunner.class) 
@PrepareForTest(SimpleMessageListenerContainer.class) 
@PowerMockIgnore("javax.management.*")
public class MessageListenerContainerManagerTest {
    
    private TaskQueue taskQueue;
    private DuplicationPolicyManager policyManager;
    private NotificationManager notificationManager;
    private MessageListenerContainerFactory messageListenerContainerFactory;
    private SimpleMessageListenerContainer container;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        taskQueue = EasyMock.createMock(TaskQueue.class);
        policyManager = EasyMock.createMock(DuplicationPolicyManager.class);
        notificationManager = EasyMock.createMock(NotificationManager.class);
        messageListenerContainerFactory = EasyMock
                .createMock(MessageListenerContainerFactory.class);
        container = PowerMock
                .createMock(SimpleMessageListenerContainer.class);
        
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        PowerMock.verify(taskQueue,
                policyManager,
                notificationManager,
                messageListenerContainerFactory,
                container);

    }

    /**
     * Test method for {@link org.duracloud.mill.durastore.MessageListenerContainerManager#init()}.
     */
    @Test
    public void test() {

        int rounds = 2;
        int containersPerSubdomain = 6;
        
        Set<String> accounts = new HashSet<>();
        accounts.add("test");
        EasyMock.expect(policyManager.getDuplicationAccounts())
                .andReturn(accounts).times(rounds);
        policyManager.clearPolicyCache();
        EasyMock.expectLastCall().times(rounds);
        DuplicationStorePolicy storePolicy = new DuplicationStorePolicy();
        storePolicy.setDestStoreId("1");
        storePolicy.setSrcStoreId("0");
        DuplicationPolicy policy = new DuplicationPolicy();
        policy.addDuplicationStorePolicy("spaceA", storePolicy);
        
        
        int times = rounds*containersPerSubdomain;
        EasyMock.expect(messageListenerContainerFactory.create(
                EasyMock.isA(ContentMessageConverter.class), 
                EasyMock.isA(MessageListenerErrorHandler.class),
                EasyMock.isA(String.class),
                EasyMock.isA(ConnectionFactory.class),
                EasyMock.isA(MessageListener.class),
                EasyMock.isA(String.class))).andReturn(container).times(times);

        EasyMock.expect(container.isRunning()).andReturn(false).times(times);
        container.start();
        EasyMock.expectLastCall().times(times);
        EasyMock.expect(container.getDestination()).andReturn(null).times(times);
        EasyMock.expect(container.getConnectionFactory()).andReturn(null).times(times);
        

        EasyMock.expect(this.container.isRunning()).andReturn(true).times(times);
        this.container.stop();
        EasyMock.expectLastCall().times(times);
        this.container.shutdown();
        EasyMock.expectLastCall().times(times);

        EasyMock.expect(container.getDestination()).andReturn(null).times(times);
        EasyMock.expect(container.getConnectionFactory()).andReturn(null).times(times);
        
        replay();

        long period = 3000;
        MessageListenerContainerManager manager = new MessageListenerContainerManager(
                taskQueue, policyManager, notificationManager,
                messageListenerContainerFactory, period);

        manager.init();
        
        sleep(1000);
        
        accounts.clear();
        accounts.add("demo");
        sleep(period + 1000);

        manager.destroy();
    }

    /**
     * 
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    private void replay() {
        PowerMock.replay(taskQueue,
                        policyManager,
                        notificationManager,
                        messageListenerContainerFactory,
                        container);
    }

    @Test
    public void testGetHost() throws Exception {
        replay();

        MessageListenerContainerManager manager =
            new MessageListenerContainerManager(null, null, null, null, 0);
        String host = manager.getHost("demo");
        Assert.assertEquals("54.225.232.106", host);
    }

}

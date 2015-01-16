/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.util.ArrayList;
import java.util.Arrays;

import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.duracloud.mill.test.AbstractTestBase;
import org.easymock.Mock;
import org.junit.Test;

import static org.easymock.EasyMock.*;

/**
 * @author Daniel Bernstein Date: Sep 9, 2014
 */
public class AuditLogGeneratorTest extends AbstractTestBase {

    @Mock
    private JpaAuditLogItemRepo repo;

    @Mock
    private LogManager logManager;

    @Test
    public void test() {

        expect(repo.findByWrittenFalseOrderByTimestampAsc())
                .andReturn(Arrays.asList(new JpaAuditLogItem[] { new JpaAuditLogItem() }));

        expect(repo.findByWrittenFalseOrderByTimestampAsc())
                .andReturn(new ArrayList<JpaAuditLogItem>());

        logManager.purgeExpired();
        expectLastCall();
        
        logManager.write(isA(JpaAuditLogItem.class));
        expectLastCall();

        logManager.flushLogs();
        expectLastCall();

        replayAll();
        AuditLogGenerator generator = new AuditLogGenerator(repo, logManager);
        generator.execute();
    }

}

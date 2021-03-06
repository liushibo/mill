/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.io.File;
import java.util.List;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.impl.ApplicationContextLocator;
import org.duracloud.mill.db.repo.JpaBitIntegrityReportRepo;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.LoopingTaskProducerDriverSupport;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.notification.SESNotificationManager;
import org.duracloud.mill.util.CommonCommandLineOptions;
import org.duracloud.mill.util.PropertyDefinition;
import org.duracloud.mill.util.PropertyDefinitionListBuilder;
import org.duracloud.mill.util.PropertyVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * A main class responsible for parsing command line arguments and launching the
 * Looping Task Producer.
 *
 * @author Daniel Bernstein
 * Date: Nov 4, 2013
 */
public class AppDriver extends LoopingTaskProducerDriverSupport {
    private static Logger log = LoggerFactory.getLogger(AppDriver.class);

    /**
     *
     */
    public AppDriver() {
        super(new CommonCommandLineOptions());
    }

    /**
     *
     */
    public static void main(String[] args) {
        new AppDriver().execute(args);
    }

    /**
     *
     */
    private void processExclusionListOption() {
        String exclusionList = System.getProperty(ConfigConstants.LOOPING_BIT_EXCLUSION_LIST_KEY);
        if (exclusionList != null) {
            File list = new File(exclusionList);
            if (!list.exists()) {
                throw new DuraCloudRuntimeException("exclusion list not found: " + list);
            }
        }
    }

    /**
     *
     */
    private void processInclusionListOption() {
        String inclusionList = System.getProperty(ConfigConstants.LOOPING_BIT_INCLUSION_LIST_KEY);
        if (inclusionList != null) {
            File list = new File(inclusionList);
            if (!list.exists()) {
                throw new DuraCloudRuntimeException("inclusionlist not found: " + list);
            }
        }
    }

    @Override
    protected LoopingTaskProducer buildTaskProducer() {

        List<PropertyDefinition> defintions =
            new PropertyDefinitionListBuilder().addAws()
                                               .addNotifications()
                                               .addMcDb()
                                               .addBitIntegrityQueue()
                                               .addLoopingBitFrequency()
                                               .addLoopingBitMaxQueueSize()
                                               .addBitIntegrityReportQueue()
                                               .addWorkDir()
                                               .build();
        PropertyVerifier verifier = new PropertyVerifier(defintions);
        verifier.verify(System.getProperties());

        processExclusionListOption();
        processInclusionListOption();

        LoopingBitTaskProducerConfigurationManager config = new LoopingBitTaskProducerConfigurationManager();

        ApplicationContext ctx = ApplicationContextLocator.get();
        CredentialsRepo credentialsRepo = ctx.getBean(CredentialsRepo.class);
        JpaBitIntegrityReportRepo bitReportRepo = ctx.getBean(JpaBitIntegrityReportRepo.class);
        StorageProviderFactory storageProviderFactory = new StorageProviderFactory();

        NotificationManager notificationMananger =
            new SESNotificationManager(config.getNotificationRecipients());
        TaskQueue bitTaskQueue = new SQSTaskQueue(
            config.getBitIntegrityQueue());

        TaskQueue bitReportQueue = new SQSTaskQueue(
            config.getBitReportQueueName());

        String stateFilePath = new File(config.getWorkDirectoryPath(),
                                        "bit-producer-state.json").getAbsolutePath();

        StateManager<BitIntegrityMorsel> stateManager = new StateManager<BitIntegrityMorsel>(
            stateFilePath, BitIntegrityMorsel.class);

        LoopingBitIntegrityTaskProducer producer =
            new LoopingBitIntegrityTaskProducer(credentialsRepo,
                                                bitReportRepo,
                                                storageProviderFactory,
                                                bitTaskQueue,
                                                bitReportQueue,
                                                stateManager,
                                                getMaxQueueSize(ConfigConstants.LOOPING_BIT_MAX_TASK_QUEUE_SIZE),
                                                getFrequency(ConfigConstants.LOOPING_BIT_FREQUENCY),
                                                notificationMananger,
                                                config.getPathFilterManager(),
                                                config);
        return producer;
    }

}

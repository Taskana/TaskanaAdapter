package pro.taskana.adapter.impl;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.PostConstruct;

import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pro.taskana.adapter.manager.AdapterConnection;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.mappings.AdapterMapper;

@Component
public class DatabaseCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCleaner.class);

    @Value("${taskana.adapter.scheduler.task.age.for.cleanup.in.hours:600}")
    private long maxTaskAgeBeforeCleanup;

    @Autowired
    private SqlSessionManager sqlSessionManager;

    @Autowired
    AdapterManager adapterManager;

    private AdapterMapper adapterMapper;

    @PostConstruct
    public void init() {
        adapterMapper = sqlSessionManager.getMapper(AdapterMapper.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "${taskana.adapter.scheduler.run.interval.for.cleanup.tasks.cron}")
    public void cleanupTaskanaAdapterTables() {
        synchronized (this.getClass()) {
            if (!adapterManager.isInitialized()) {
                return;
            }
            LOGGER.debug("----------cleanupTaskanaAdapterTables started----------------------------");
            try (AdapterConnection connection = adapterManager.getAdapterConnection(sqlSessionManager)) {
                Instant completedBefore = Instant.now().minus(Duration.ofHours(maxTaskAgeBeforeCleanup));
                adapterMapper.cleanupTasksCompletedBefore(completedBefore);
                adapterMapper.cleanupQueryHistoryEntries(completedBefore);
            } catch (Exception ex) {
                LOGGER.error("Caught {} while cleaning up aged Taskana Adapter tables", ex);
            }
        }
    }

}

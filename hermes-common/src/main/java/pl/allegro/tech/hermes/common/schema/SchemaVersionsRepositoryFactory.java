package pl.allegro.tech.hermes.common.schema;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.schema.CachedSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SchemaVersionsRepositoryFactory implements Factory<SchemaVersionsRepository> {

    private final RawSchemaClient rawSchemaClient;
    private final ConfigFactory configFactory;
    private final InternalNotificationsBus notificationsBus;

    @Inject
    public SchemaVersionsRepositoryFactory(RawSchemaClient rawSchemaClient, ConfigFactory configFactory, InternalNotificationsBus notificationsBus) {
        this.rawSchemaClient = rawSchemaClient;
        this.configFactory = configFactory;
        this.notificationsBus = notificationsBus;
    }

    @Override
    public SchemaVersionsRepository provide() {
        if (configFactory.getBooleanProperty(Configs.SCHEMA_CACHE_ENABLED)) {
            CachedSchemaVersionsRepository repository = new CachedSchemaVersionsRepository(rawSchemaClient,
                    getVersionsReloader(),
                    configFactory.getIntProperty(Configs.SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES),
                    configFactory.getIntProperty(Configs.SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES));

            notificationsBus.registerTopicCallback(new TopicCallback() {
                @Override
                public void onTopicRemoved(Topic topic) {
                    repository.removeFromCache(topic);
                }
            });

            return repository;
        }
        return new DirectSchemaVersionsRepository(rawSchemaClient);
    }

    private ExecutorService getVersionsReloader() {
        return Executors.newFixedThreadPool(
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE),
                new ThreadFactoryBuilder().setNameFormat("schema-source-reloader-%d").build());
    }

    @Override
    public void dispose(SchemaVersionsRepository instance) {

    }
}

package com.breadcost.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * Redis cache configuration.
 * <p>
 * Active when the "redis" profile is enabled AND a Redis connection is available.
 * Falls back to no-op (no caching) when Redis is unavailable so the app still starts.
 */
@Configuration
@EnableCaching
@Profile("redis")
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .activateDefaultTyping(
                        JsonMapper.builder().build().getPolymorphicTypeValidator(),
                        ObjectMapper.DefaultTyping.NON_FINAL)
                .build();
        var jsonSerializer = RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper));

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(jsonSerializer)
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = Map.ofEntries(
                // Reference data — long TTL
                Map.entry("products",        defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("product",         defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("recipes",         defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("activeRecipe",    defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("recipe",          defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("recipeMaterials", defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("items",           defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("itemsActive",     defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("itemsByType",     defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("departments",     defaults.entryTtl(Duration.ofMinutes(15))),
                Map.entry("deptsActive",     defaults.entryTtl(Duration.ofMinutes(15))),
                Map.entry("department",      defaults.entryTtl(Duration.ofMinutes(15))),
                Map.entry("subTiers",        defaults.entryTtl(Duration.ofMinutes(30))),
                Map.entry("subTier",         defaults.entryTtl(Duration.ofMinutes(30))),

                // Subscription feature checks — medium TTL
                Map.entry("activeSub",       defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("subFeature",      defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("subFeatures",     defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("subMaxUsers",     defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("subMaxProducts",  defaults.entryTtl(Duration.ofMinutes(5))),

                // KPI / reporting — moderate TTL
                Map.entry("kpiBlocks",       defaults.entryTtl(Duration.ofMinutes(15))),
                Map.entry("kpiBlocksAll",    defaults.entryTtl(Duration.ofMinutes(15))),
                Map.entry("kpiBlocksByCat",  defaults.entryTtl(Duration.ofMinutes(15))),
                Map.entry("kpiComputed",     defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("customReports",   defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("customReport",    defaults.entryTtl(Duration.ofMinutes(10))),

                // Finance — moderate TTL
                Map.entry("financeRevenue",     defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("financeCogs",        defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("financeMargin",      defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("financeOrderCount",  defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("financeAov",         defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("financeOutstanding",  defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("financeOverdue",      defaults.entryTtl(Duration.ofMinutes(5))),

                // Customers / suppliers — medium TTL
                Map.entry("customers",       defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("customer",        defaults.entryTtl(Duration.ofMinutes(5))),
                Map.entry("suppliers",       defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("supplier",        defaults.entryTtl(Duration.ofMinutes(10))),
                Map.entry("supplierCatalog", defaults.entryTtl(Duration.ofMinutes(10))),

                // Inventory — short TTL (volatile data)
                Map.entry("inventory",              defaults.entryTtl(Duration.ofSeconds(60))),
                Map.entry("inventoryAvailability",   defaults.entryTtl(Duration.ofSeconds(30)))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }
}

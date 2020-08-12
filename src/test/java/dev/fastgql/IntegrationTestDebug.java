package dev.fastgql;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.events.DebeziumEngineSingleton;
import dev.fastgql.integration.*;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.VertxModule;
import io.debezium.engine.ChangeEvent;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ExtendWith(VertxExtension.class)
public class IntegrationTestDebug {
  private final JdbcDatabaseContainer<?> jdbcDatabaseContainer = new MySQLContainer<>("fastgql/mysql-testcontainers:latest")
    .withNetworkAliases("mysql")
    .withUsername("debezium")
    .withPassword("dbz");

  private JsonObject config;
  private JsonObject configMultipleQueries;
  private DatasourceConfig datasourceConfig;
  private DebeziumConfig debeziumConfig;

  @BeforeEach
  public void beforeEach(Vertx vertx, VertxTestContext context) {
    Startables.deepStart(Stream.of(jdbcDatabaseContainer)).join();

    datasourceConfig =
      DatasourceConfig.createDatasourceConfig(
        jdbcDatabaseContainer.getJdbcUrl(),
        jdbcDatabaseContainer.getUsername(),
        jdbcDatabaseContainer.getPassword(),
        jdbcDatabaseContainer.getDatabaseName());

    config =
      new JsonObject()
        .put("debezium", Map.of("embedded", true, "server", "dbserver"))
        .put("http.port", 8081)
        .put(
          "datasource",
          Map.of(
            "jdbcUrl", datasourceConfig.getJdbcUrl(),
            "username", datasourceConfig.getUsername(),
            "password", datasourceConfig.getPassword(),
            "schema", datasourceConfig.getSchema()));

    configMultipleQueries =
      new JsonObject()
        .put("debezium", Map.of("embedded", true, "server", "dbserver"))
        .put("http.port", 8081)
        .put(
          "datasource",
          Map.of(
            "jdbcUrl", String.format("%s?allowMultiQueries=true", jdbcDatabaseContainer.getJdbcUrl()),
            "username", datasourceConfig.getUsername(),
            "password", datasourceConfig.getPassword(),
            "schema", datasourceConfig.getSchema()));

    debeziumConfig = DebeziumConfig.createWithJsonConfig(config.getJsonObject("debezium"));

    context.completeNow();
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.DAYS)
  public void shouldReceiveResponse(Vertx vertx, VertxTestContext context) throws IOException {
    String directory = "subscriptions/simple/referencing";

    Injector injector =
      Guice.createInjector(new VertxModule(vertx, config), new DatabaseModule());
    Pool pool = injector.getInstance(Pool.class);

    Injector injectorMultipleQueries =
      Guice.createInjector(new VertxModule(vertx, configMultipleQueries), new DatabaseModule());
    Pool poolMultipleQueries = injectorMultipleQueries.getInstance(Pool.class);

    DebeziumEngineSingleton debeziumEngineSingleton = new DebeziumEngineSingleton(datasourceConfig, debeziumConfig);
    debeziumEngineSingleton.startNewEngine();

    Flowable<ChangeEvent<String, String>> eventFlowable = debeziumEngineSingleton.getChangeEventFlowable()
      .filter(event -> List.of("dbserver.test.addresses", "dbserver.test.customers").contains(event.destination()));

    eventFlowable
      .delay(2, TimeUnit.SECONDS)
      .parallel().flatMap(event -> pool.rxBegin().toFlowable())
      .flatMap(transaction -> transaction.rxQuery("LOCK TABLES customers as a0 READ, addresses as a1 READ").doOnSuccess(rows -> System.out.println("LOCKED")).toFlowable()
        .flatMap(rows -> transaction.rxQuery("SELECT a0.address AS a0_address, a1.id AS a1_id, a1.street AS a1_street, a0.id AS a0_id FROM customers a0 LEFT JOIN addresses a1 ON a0.address = a1.id ORDER BY a0.id ASC").doOnSuccess(rows2 -> System.out.println("QUERY")).toFlowable())
        .flatMap(rows -> transaction.rxQuery("UNLOCK TABLES").doOnSuccess(rows2 -> System.out.println("UNLOCKED")).toFlowable())
        .flatMap(rows -> transaction.rxCommit().toFlowable())
      )
      .sequential()
      .subscribe();

    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
      .delay(10, TimeUnit.SECONDS)
      .flatMap(rows -> DBTestUtils.executeSQLQuery(String.format("%s/query.sql", directory), pool))
      .doFinally(debeziumEngineSingleton::stopEngine)
      .subscribe(
        rows -> {
          System.out.println("affected rows: " + rows.rowCount());
          context.completeNow();
        }, context::failNow);
  }
}

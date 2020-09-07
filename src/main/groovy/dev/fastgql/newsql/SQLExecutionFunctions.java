package dev.fastgql.newsql;

import dev.fastgql.dsl.OpSpec;
import dev.fastgql.graphql.GraphQLDatabaseSchema;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.Field;
import graphql.language.SelectionSet;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLExecutionFunctions {

  private static Function<Row, Single<Map<String, Object>>> createExecutorForColumn(
      SQLQuery.Table table, GraphQLField graphQLField, SQLQuery sqlQuery) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    SQLQuery.SelectColumn selectColumn = sqlQuery.addSelectColumn(table, columnName);
    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null ? Single.just(Map.of()) : Single.just(Map.of(columnName, value));
    };
  }

  private static Function<Row, Single<Map<String, Object>>> createExecutorForReferencing(
      SQLQuery.Table table,
      Field field,
      GraphQLField graphQLField,
      SQLQuery sqlQuery,
      Transaction transaction,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    SQLQuery.Table foreignTable =
        sqlQuery.createNewTable(graphQLField.getForeignName().getTableName());
    SQLQuery.SelectColumn selectColumnReferencing =
        sqlQuery.addLeftJoin(table, columnName, foreignTable, foreignColumnName);
    List<Function<Row, Single<Map<String, Object>>>> executors =
        createExecutors(
            foreignTable, field.getSelectionSet(), sqlQuery, transaction, graphQLDatabaseSchema);
    return row -> {
      Object value = row.getValue(selectColumnReferencing.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : executorListToSingleMap(executors, row).map(result -> Map.of(field.getName(), result));
    };
  }

  private static Function<Row, Single<Map<String, Object>>> createExecutorForReferenced(
      SQLQuery.Table table,
      Field field,
      GraphQLField graphQLField,
      SQLQuery sqlQuery,
      Transaction transaction,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignTableName = graphQLField.getForeignName().getTableName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    SQLQuery.SelectColumn selectColumn = sqlQuery.addSelectColumn(table, columnName);
    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      if (value == null) {
        return Single.just(Map.of());
      } else {
        OpSpec opSpec = new OpSpec();
        OpSpecUtils.checkColumnIsEqValue(opSpec, foreignColumnName, value);
        return getRootResponse(
                foreignTableName,
                field.getSelectionSet(),
                transaction,
                opSpec,
                graphQLDatabaseSchema)
            .toList()
            .map(result -> Map.of(field.getName(), result));
      }
    };
  }

  private static List<Function<Row, Single<Map<String, Object>>>> createExecutors(
      SQLQuery.Table table,
      SelectionSet selectionSet,
      SQLQuery sqlQuery,
      Transaction transaction,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    Stream<Function<Row, Single<Map<String, Object>>>> executors =
        selectionSet.getSelections().stream()
            .filter(selection -> selection instanceof Field)
            .map(selection -> (Field) selection)
            .map(
                field -> {
                  GraphQLField graphQLField =
                      graphQLDatabaseSchema.fieldAt(table.getTableName(), field.getName());
                  switch (graphQLField.getReferenceType()) {
                    case NONE:
                      return createExecutorForColumn(table, graphQLField, sqlQuery);
                    case REFERENCING:
                      return createExecutorForReferencing(
                          table, field, graphQLField, sqlQuery, transaction, graphQLDatabaseSchema);
                    case REFERENCED:
                      return createExecutorForReferenced(
                          table, field, graphQLField, sqlQuery, transaction, graphQLDatabaseSchema);
                    default:
                      throw new RuntimeException(
                          "Unknown reference type: " + graphQLField.getReferenceType());
                  }
                });
    return executors.collect(Collectors.toList());
  }

  private static Single<Map<String, Object>> executorListToSingleMap(
      List<Function<Row, Single<Map<String, Object>>>> executorList, Row row) {
    return Observable.fromIterable(executorList)
        .flatMapSingle(executor -> executor.apply(row))
        .collect(HashMap::new, Map::putAll);
  }

  public static Observable<Map<String, Object>> getRootResponse(
      String tableName,
      SelectionSet selectionSet,
      Transaction transaction,
      OpSpec opSpec,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    SQLQuery sqlQuery = new SQLQuery(tableName, opSpec);
    List<Function<Row, Single<Map<String, Object>>>> executorList =
        createExecutors(
            sqlQuery.getTable(), selectionSet, sqlQuery, transaction, graphQLDatabaseSchema);
    String query = sqlQuery.createQuery();
    System.out.println(query);
    return transaction
        .rxQuery(query)
        .flatMapObservable(Observable::fromIterable)
        .flatMapSingle(row -> executorListToSingleMap(executorList, row));
  }
}

/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql.metadata;

import dev.fastgql.common.FieldType;
import dev.fastgql.common.QualifiedName;
import dev.fastgql.db.DatabaseSchema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class MetadataUtils {

  private static final Map<Integer, FieldType> sqlDataTypeToFieldType =
      Map.of(
          4, FieldType.INT,
          12, FieldType.STRING);

  public static DatabaseSchema createDatabaseSchema(Connection connection) throws SQLException {
    DatabaseMetaData databaseMetaData = connection.getMetaData();
    Statement statement = connection.createStatement();
    ResultSet tablesResultSet =
        databaseMetaData.getTables(null, null, null, new String[] {"TABLE"});

    DatabaseSchema.Builder databaseSchemaBuilder = DatabaseSchema.newSchema();

    while (tablesResultSet.next()) {
      String tableName = tablesResultSet.getString("TABLE_NAME");
      // Foreign key extraction
      ResultSet foreignKeyResultSet = databaseMetaData.getImportedKeys(null, null, tableName);
      Map<String, String> foreignKeyToRef = new HashMap<>();
      while (foreignKeyResultSet.next()) {
        String columnName = foreignKeyResultSet.getString("FKCOLUMN_NAME");
        String refColumnName = foreignKeyResultSet.getString("PKCOLUMN_NAME");
        String refTableName = foreignKeyResultSet.getString("PKTABLE_NAME");
        foreignKeyToRef.put(
          QualifiedName.generate(tableName, columnName),
          QualifiedName.generate(refTableName, refColumnName));
      }
      foreignKeyResultSet.close();

      // Column extraction
      ResultSet columnsResultSet = databaseMetaData.getColumns(null, null, tableName, null);
      while (columnsResultSet.next()) {
        Integer dataType = columnsResultSet.getInt("DATA_TYPE");
        if (!sqlDataTypeToFieldType.containsKey(dataType)) {
          throw new RuntimeException(
              "Only integer or string class for columns currently supported");
        }
        String columnName = columnsResultSet.getString("COLUMN_NAME");
        String qualifiedName = QualifiedName.generate(tableName, columnName);
        if (foreignKeyToRef.containsKey(qualifiedName)) {
          databaseSchemaBuilder.row(
              qualifiedName,
              sqlDataTypeToFieldType.get(dataType),
              foreignKeyToRef.get(qualifiedName));
        } else {
          databaseSchemaBuilder.row(qualifiedName, sqlDataTypeToFieldType.get(dataType));
        }
      }
      columnsResultSet.close();
    }
    tablesResultSet.close();
    statement.close();
    return databaseSchemaBuilder.build();
  }
}
package dev.fastgql.graphql;

import dev.fastgql.dsl.OpSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SQLQuery {
  static class Table {
    private final String tableName;
    private final String tableAlias;
    private final List<OpSpec.Check> checks;

    Table(String tableName, String tableAlias) {
      this.tableName = tableName;
      this.tableAlias = tableAlias;
      this.checks = new ArrayList<>();
    }

    public String getTableName() {
      return tableName;
    }

    private String getTableAlias() {
      return tableAlias;
    }

    public void addChecks(List<OpSpec.Check> newChecks) {
      checks.addAll(newChecks);
    }

    String sqlString() {
      return String.format("%s %s", tableName, tableAlias);
    }

    static String relationalOperatorToString(OpSpec.RelationalOperator relationalOperator) {
      switch (relationalOperator) {
        case eq:
          return "=";
        case neq:
          return "<>";
        case gt:
          return ">";
        case lt:
          return "<";
        case gte:
          return ">=";
        case lte:
          return "<=";
        default:
          throw new RuntimeException("unknown relational operator: " + relationalOperator);
      }
    }

    String sqlCheckString() {
      return checks.stream()
          .map(
              check -> {
                String columnName = check.getColumn();
                OpSpec.Condition condition = check.getCondition();
                return sqlCheckString(columnName, condition);
              })
          .collect(Collectors.joining(" AND "));
    }

    private String sqlCheckString(String columnName, OpSpec.Condition condition) {
      StringBuilder sqlCheckStringBuilder = new StringBuilder();
      Object value = condition.getValue();
      String valueAsString =
          value instanceof String ? String.format("'%s'", value) : String.valueOf(value);
      sqlCheckStringBuilder.append(
          String.format(
              "%s.%s%s%s",
              tableAlias,
              columnName,
              relationalOperatorToString(condition.getRelationalOperator()),
              valueAsString));
      condition
          .getNext()
          .forEach(
              nextCondition ->
                  sqlCheckStringBuilder
                      .append(
                          String.format(
                              " %s (",
                              nextCondition.getLogicalConnective().toString().toUpperCase()))
                      .append(sqlCheckString(columnName, nextCondition))
                      .append(")"));
      return sqlCheckStringBuilder.toString();
    }
  }

  static class SelectColumn {

    private final String tableAlias;
    private final String columnName;
    private final String resultAlias;

    private SelectColumn(Table table, String columnName, String resultAlias) {
      this.tableAlias = table.getTableAlias();
      this.columnName = columnName;
      this.resultAlias = resultAlias;
    }

    private String sqlString() {
      return String.format("%s.%s AS %s", tableAlias, columnName, resultAlias);
    }

    public String getResultAlias() {
      return resultAlias;
    }
  }

  static class LeftJoin {
    private final Table table;
    private final String columnName;
    private final Table foreignTable;
    private final String foreignColumnName;

    LeftJoin(Table table, String columnName, Table foreignTable, String foreignColumnName) {
      this.table = table;
      this.columnName = columnName;
      this.foreignTable = foreignTable;
      this.foreignColumnName = foreignColumnName;
    }

    private String sqlString() {
      return String.format(
          "LEFT JOIN %s ON %s.%s = %s.%s",
          foreignTable.sqlString(),
          table.getTableAlias(),
          columnName,
          foreignTable.getTableAlias(),
          foreignColumnName);
    }
  }

  private int tableAliasCount = 0;
  private int resultAliasCount = 0;
  private final Table table;
  private final List<SelectColumn> selectColumns;
  private final List<LeftJoin> leftJoins;

  public SQLQuery(String tableName, OpSpec opSpec) {
    this.table = createNewTable(tableName);
    this.selectColumns = new ArrayList<>();
    this.leftJoins = new ArrayList<>();
    this.table.addChecks(opSpec.getChecks());
  }

  public Table createNewTable(String tableName) {
    return new Table(tableName, getNextTableAlias());
  }

  public Table getTable() {
    return table;
  }

  private String getNextTableAlias() {
    tableAliasCount++;
    return String.format("t%d", tableAliasCount);
  }

  private String getNextResultAlias() {
    resultAliasCount++;
    return String.format("v%d", resultAliasCount);
  }

  public SelectColumn addSelectColumn(Table table, String columnName) {
    SelectColumn selectColumn = new SelectColumn(table, columnName, getNextResultAlias());
    selectColumns.add(selectColumn);
    return selectColumn;
  }

  public SelectColumn addLeftJoin(
      Table table, String columnName, Table foreignTable, String foreignColumnName) {
    SelectColumn selectColumn = addSelectColumn(table, columnName);
    leftJoins.add(new LeftJoin(table, columnName, foreignTable, foreignColumnName));
    return selectColumn;
  }

  public String createQuery() {
    String baseString =
        String.format(
            "SELECT %s FROM %s %s",
            selectColumns.stream().map(SelectColumn::sqlString).collect(Collectors.joining(", ")),
            table.sqlString(),
            leftJoins.stream().map(LeftJoin::sqlString).collect(Collectors.joining(" ")));
    String whereConditions = table.sqlCheckString();
    if (!whereConditions.isEmpty()) {
      return String.format("%s WHERE %s", baseString, whereConditions);
    }
    return baseString;
  }
}

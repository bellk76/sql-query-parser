package io.github.sqlparser.model;

import io.github.sqlparser.model.expr.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The parsed representation of a single {@code SELECT} query.
 *
 * <p>{@code WHERE} filters individual rows and is applied before grouping,
 * while {@code HAVING} filters groups produced by {@code GROUP BY}; therefore
 * they are kept in separate fields.
 */
public final class Query {

    private final boolean distinct;
    private final List<SelectItem> columns;
    private final List<Source> fromSources;
    private final List<Join> joins;
    private final List<Expression> whereClauses;
    private final List<Expression> groupByColumns;
    private final List<Expression> havingClauses;
    private final List<Sort> sortColumns;
    private final Integer limit;
    private final Integer offset;

    private Query(Builder b) {
        this.distinct = b.distinct;
        this.columns = List.copyOf(b.columns);
        this.fromSources = List.copyOf(b.fromSources);
        this.joins = List.copyOf(b.joins);
        this.whereClauses = List.copyOf(b.whereClauses);
        this.groupByColumns = List.copyOf(b.groupByColumns);
        this.havingClauses = List.copyOf(b.havingClauses);
        this.sortColumns = List.copyOf(b.sortColumns);
        this.limit = b.limit;
        this.offset = b.offset;
    }

    public boolean distinct() {
        return distinct;
    }

    public List<SelectItem> columns() {
        return columns;
    }

    public List<Source> fromSources() {
        return fromSources;
    }

    public List<Join> joins() {
        return joins;
    }

    public List<Expression> whereClauses() {
        return whereClauses;
    }

    public List<Expression> groupByColumns() {
        return groupByColumns;
    }

    public List<Expression> havingClauses() {
        return havingClauses;
    }

    public List<Sort> sortColumns() {
        return sortColumns;
    }

    public Integer limit() {
        return limit;
    }

    public Integer offset() {
        return offset;
    }

    @Override
    public String toString() {
        String pad = "  ";
        StringBuilder sb = new StringBuilder("Query {\n");
        appendScalar(sb, pad, "distinct", distinct);
        appendList(sb, pad, "columns", columns);
        appendList(sb, pad, "from", fromSources);
        appendList(sb, pad, "joins", joins);
        appendList(sb, pad, "where", whereClauses);
        appendList(sb, pad, "groupBy", groupByColumns);
        appendList(sb, pad, "having", havingClauses);
        appendList(sb, pad, "orderBy", sortColumns);
        appendScalar(sb, pad, "limit", limit);
        appendScalar(sb, pad, "offset", offset);
        sb.append('}');
        return sb.toString();
    }

    private static void appendScalar(StringBuilder sb, String pad, String name, Object value) {
        sb.append(pad).append(name).append(": ").append(value).append('\n');
    }

    private static void appendList(StringBuilder sb, String pad, String name, List<?> values) {
        if (values.isEmpty()) {
            sb.append(pad).append(name).append(": []\n");
            return;
        }
        sb.append(pad).append(name).append(":\n");
        String itemPad = pad + "  ";
        for (Object value : values) {
            String rendered = value.toString().replace("\n", "\n" + itemPad + "  ");
            sb.append(itemPad).append("- ").append(rendered).append('\n');
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Query other)) {
            return false;
        }
        return distinct == other.distinct
                && columns.equals(other.columns)
                && fromSources.equals(other.fromSources)
                && joins.equals(other.joins)
                && whereClauses.equals(other.whereClauses)
                && groupByColumns.equals(other.groupByColumns)
                && havingClauses.equals(other.havingClauses)
                && sortColumns.equals(other.sortColumns)
                && Objects.equals(limit, other.limit)
                && Objects.equals(offset, other.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distinct, columns, fromSources, joins, whereClauses,
                groupByColumns, havingClauses, sortColumns, limit, offset);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Mutable builder for {@link Query}; list fields default to empty lists. */
    public static final class Builder {

        private boolean distinct;
        private final List<SelectItem> columns = new ArrayList<>();
        private final List<Source> fromSources = new ArrayList<>();
        private final List<Join> joins = new ArrayList<>();
        private final List<Expression> whereClauses = new ArrayList<>();
        private final List<Expression> groupByColumns = new ArrayList<>();
        private final List<Expression> havingClauses = new ArrayList<>();
        private final List<Sort> sortColumns = new ArrayList<>();
        private Integer limit;
        private Integer offset;

        public Builder distinct(boolean distinct) {
            this.distinct = distinct;
            return this;
        }

        public Builder addColumn(SelectItem column) {
            this.columns.add(Objects.requireNonNull(column, "column must not be null"));
            return this;
        }

        public Builder columns(List<SelectItem> columns) {
            this.columns.clear();
            this.columns.addAll(columns);
            return this;
        }

        public Builder addFromSource(Source source) {
            this.fromSources.add(Objects.requireNonNull(source, "source must not be null"));
            return this;
        }

        public Builder fromSources(List<Source> sources) {
            this.fromSources.clear();
            this.fromSources.addAll(sources);
            return this;
        }

        public Builder addJoin(Join join) {
            this.joins.add(Objects.requireNonNull(join, "join must not be null"));
            return this;
        }

        public Builder addWhereClause(Expression clause) {
            this.whereClauses.add(Objects.requireNonNull(clause, "clause must not be null"));
            return this;
        }

        public Builder addGroupByColumn(Expression column) {
            this.groupByColumns.add(Objects.requireNonNull(column, "column must not be null"));
            return this;
        }

        public Builder addHavingClause(Expression clause) {
            this.havingClauses.add(Objects.requireNonNull(clause, "clause must not be null"));
            return this;
        }

        public Builder addSortColumn(Sort sort) {
            this.sortColumns.add(Objects.requireNonNull(sort, "sort must not be null"));
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Query build() {
            return new Query(this);
        }
    }
}

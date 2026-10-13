package io.github.sqlparser.model;

/**
 * A base table reference, e.g. {@code book} or {@code book b}.
 */
public record TableSource(String name, String alias) implements Source {

    public TableSource {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("table name must not be empty");
        }
    }

    public TableSource(String name) {
        this(name, null);
    }

    public boolean hasAlias() {
        return alias != null;
    }

    @Override
    public String toString() {
        return alias == null ? name : name + " " + alias;
    }
}

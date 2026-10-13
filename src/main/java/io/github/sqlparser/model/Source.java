package io.github.sqlparser.model;

/**
 * A data source in the FROM clause: either a base table or a derived table
 * (subquery).
 */
public interface Source {

    /** The alias of this source, or {@code null} if it has none. */
    String alias();
}

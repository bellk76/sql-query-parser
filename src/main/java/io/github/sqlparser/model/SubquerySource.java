package io.github.sqlparser.model;

import java.util.Objects;

/**
 * A derived table: a subquery with a mandatory alias, e.g.
 * {@code (SELECT * FROM A) a_alias}.
 */
public record SubquerySource(Query query, String alias) implements Source {

    public SubquerySource {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(alias, "alias must not be null");
    }

    @Override
    public String toString() {
        return "(" + query + ") " + alias;
    }
}

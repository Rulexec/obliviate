package ruliov.obliviate.db.migrations

import ruliov.toJDBCUrl

val DATABASE_URL =
    toJDBCUrl(System.getenv("DATABASE_URL") ?:
    "postgres://ruliov:ruliov@localhost:5432/obliviate")

val JDBC_DATABASE_URL = "jdbc:" + DATABASE_URL
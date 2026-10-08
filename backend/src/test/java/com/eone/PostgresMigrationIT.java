package com.eone;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Runs only with -Ppostgres; requires an explicitly supplied disposable database. */
class PostgresMigrationIT {
    private final String url = Objects.requireNonNull(System.getenv("EONE_TEST_DATABASE_URL"),
            "Set EONE_TEST_DATABASE_URL to a disposable PostgreSQL database");
    private final String username = Objects.requireNonNull(System.getenv("EONE_TEST_DATABASE_USERNAME"));
    private final String password = System.getenv().getOrDefault("EONE_TEST_DATABASE_PASSWORD", "");

    @Test void cleanDatabaseMigratesAndEnforcesRoleConstraints() throws Exception {
        String schema = schema();
        flyway(schema, null).migrate();
        try (var connection = connection(schema)) {
            seedRole(connection);
            assertThatThrownBy(() -> connection.createStatement().executeUpdate("""
                    INSERT INTO role_assignments SELECT gen_random_uuid(), user_id, role, department_id,
                    classroom_id, class_subject_id, active, created_at, updated_at FROM role_assignments
                    """)).isInstanceOf(SQLException.class).satisfies(ex -> assertThat(((SQLException) ex).getSQLState()).isEqualTo("23505"));
            assertThatThrownBy(() -> connection.createStatement().executeUpdate(
                    "UPDATE role_assignments SET department_id = NULL"))
                    .isInstanceOf(SQLException.class).satisfies(ex -> assertThat(((SQLException) ex).getSQLState()).isEqualTo("23514"));
        }
    }

    @Test void v1UpgradePreservesDataAndPermitsHistoricalInactiveRoles() throws Exception {
        String schema = schema();
        flyway(schema, "1").migrate();
        UUID original;
        try (var connection = connection(schema)) {
            seedRole(connection);
            try (var result = connection.createStatement().executeQuery("SELECT id FROM users")) {
                result.next(); original = result.getObject(1, UUID.class);
            }
        }
        flyway(schema, null).migrate();
        try (var connection = connection(schema)) {
            try (var result = connection.createStatement().executeQuery("SELECT id FROM users")) {
                assertThat(result.next()).isTrue(); assertThat(result.getObject(1, UUID.class)).isEqualTo(original);
                assertThat(result.next()).isFalse();
            }
            assertThat(connection.createStatement().executeUpdate("""
                    INSERT INTO role_assignments SELECT gen_random_uuid(), user_id, role, department_id,
                    classroom_id, class_subject_id, false, created_at, updated_at FROM role_assignments
                    """)).isEqualTo(1);
        }
        flyway(schema, null).validate();
    }

    private Flyway flyway(String schema, String target) {
        var config = Flyway.configure().dataSource(url, username, password).schemas(schema).defaultSchema(schema);
        if (target != null) config.target(target);
        return config.load();
    }
    private String schema() { return "migration_" + UUID.randomUUID().toString().replace("-", ""); }
    private Connection connection(String schema) throws SQLException {
        var connection = DriverManager.getConnection(url, username, password); connection.setSchema(schema); return connection;
    }
    private void seedRole(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO departments VALUES (gen_random_uuid(), 'CS', 'Computer Science', now(), now())");
            statement.executeUpdate("""
                    INSERT INTO users (id,email,password_hash,first_name,last_name,account_type,status,department_id,created_at,updated_at)
                    SELECT gen_random_uuid(),'migration@test.invalid','test-only','Migration','Test','STAFF','ACTIVE',id,now(),now() FROM departments
                    """);
            statement.executeUpdate("""
                    INSERT INTO role_assignments (id,user_id,role,department_id,active,created_at,updated_at)
                    SELECT gen_random_uuid(),id,'HOD',department_id,true,now(),now() FROM users
                    """);
        }
    }
}

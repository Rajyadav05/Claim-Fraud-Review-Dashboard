package com.claimguard.dashboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthControllerTest {

    @Test
    @DisplayName("Health endpoint reports in-memory mode when JDBC URL contains :h2:mem:")
    void testInMemoryHealthReporting() throws SQLException {
        DataSource mockDs = mock(DataSource.class);
        Connection mockConn = mock(Connection.class);
        DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

        when(mockDs.getConnection()).thenReturn(mockConn);
        when(mockConn.getMetaData()).thenReturn(mockMeta);
        when(mockMeta.getURL()).thenReturn("jdbc:h2:mem:claimguard_test;DB_CLOSE_DELAY=-1");

        HealthController controller = new HealthController(mockDs);
        ResponseEntity<Map<String, Object>> response = controller.getHealth();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("H2 In-Memory (Operational)", response.getBody().get("database"));
    }

    @Test
    @DisplayName("Health endpoint reports file-backed mode when JDBC URL contains :h2:file:")
    void testFileBackedHealthReporting() throws SQLException {
        DataSource mockDs = mock(DataSource.class);
        Connection mockConn = mock(Connection.class);
        DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

        when(mockDs.getConnection()).thenReturn(mockConn);
        when(mockConn.getMetaData()).thenReturn(mockMeta);
        when(mockMeta.getURL()).thenReturn("jdbc:h2:file:/app/data/claimguard;DB_CLOSE_DELAY=-1");

        HealthController controller = new HealthController(mockDs);
        ResponseEntity<Map<String, Object>> response = controller.getHealth();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("H2 File-Backed (Operational)", response.getBody().get("database"));
    }

    @Test
    @DisplayName("Health endpoint reports degraded database status when connection throws SQLException")
    void testDegradedDatabaseReporting() throws SQLException {
        DataSource mockDs = mock(DataSource.class);
        when(mockDs.getConnection()).thenThrow(new SQLException("Connection refused"));

        HealthController controller = new HealthController(mockDs);
        ResponseEntity<Map<String, Object>> response = controller.getHealth();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("Database Connection Degraded", response.getBody().get("database"));
    }

    @Test
    @DisplayName("Health endpoint reports unknown when DataSource is null")
    void testNullDataSourceReporting() {
        HealthController controller = new HealthController(null);
        ResponseEntity<Map<String, Object>> response = controller.getHealth();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Unknown (No DataSource)", response.getBody().get("database"));
    }
}

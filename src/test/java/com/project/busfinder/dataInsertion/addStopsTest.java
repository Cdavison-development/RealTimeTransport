package com.project.busfinder.dataInsertion;


import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import java.sql.*;

public class addStopsTest {

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private Statement mockStatement;
    private String jsonStopsContent;

    @BeforeEach
    public void setUp() throws Exception {
        // Initialise mocks
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockStatement = mock(Statement.class);

        // Configure mock behavior
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Sample JSON content for testing
        jsonStopsContent = "{"
                + "\"2800S42098D\": {"
                + "\"common_name\": \"Liverpool ONE Bus Station\","
                + "\"coordinates\": {"
                + "\"longitude\": -2.987977,"
                + "\"latitude\": 53.401987"
                + "}"
                + "}"
                + "}";
    }

    @Test
    public void testInsertStops() throws Exception {

        addStops.insertDataIntoDatabase(jsonStopsContent, mockConnection);

        // Verify that the correct SQL statement was prepared
        verify(mockConnection).prepareStatement(
                "INSERT OR REPLACE INTO bus_stops (stop_id, common_name, longitude, latitude) " +
                        "VALUES (?, ?, ?, ?)"
        );

        // Verify the interactions with the prepared statement for the first stop
        verify(mockPreparedStatement).setString(1, "2800S42098D");
        verify(mockPreparedStatement).setString(2, "Liverpool ONE Bus Station");
        verify(mockPreparedStatement).setDouble(3, -2.987977);
        verify(mockPreparedStatement).setDouble(4, 53.401987);
        verify(mockPreparedStatement).executeUpdate();

        // Verify the table creation statement
        verify(mockStatement).execute(
                "CREATE TABLE IF NOT EXISTS bus_stops (" +
                        "stop_id TEXT PRIMARY KEY, " +
                        "common_name TEXT, " +
                        "longitude FLOAT, " +
                        "latitude FLOAT)"
        );

    }
}
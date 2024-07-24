package com.project.busfinder.dataInsertion;

import static com.project.busfinder.dataInsertion.addRoutes.handlePolylines;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import java.sql.*;

public class addRoutesTest {
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private File polylineTestFile;
    private File StopRefTestFile;
    private static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    @BeforeEach
    public void setUp() throws Exception {
        // Initialise mocks.
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);

        // Configure mock behavior.
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Create a sample JSON file for testing polyline parsing functions.
        polylineTestFile = File.createTempFile("AMSY_6_test", ".json");
        polylineTestFile.deleteOnExit();
        String jsonPolylineContent = "[\n" +
                "    \"isieIpdjQ^p@\\\\^^XPDf@d@\\\\RTHZBr@B^B`@HvAl@j@\\\\@M\",\n" +
                "    \"i~meIvnjQWm@AKLOwBiF}@kCsAoDM_@\",\n" +
                "    \"mgneIfziQa@_AiAvBkBxCUf@kBvCeBtBc@d@\"\n" +
                "]";
        Files.write(polylineTestFile.toPath(), jsonPolylineContent.getBytes());

        StopRefTestFile = File.createTempFile("AMSY_6_test", ".json");
        StopRefTestFile.deleteOnExit();
        String jsonStopRefContent = "[\"068000000305\", \"068000000324\"]";

        Files.write(StopRefTestFile.toPath(), jsonStopRefContent.getBytes());


        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS routes (route_id VARCHAR PRIMARY KEY, polyline_data TEXT)");
        }

    }

    @Test
    public void testHandlePolylines() throws Exception {
        String testDirectoryPath = "data/encoded_polylines_testset";

        handlePolylines(testDirectoryPath, DB_URL);

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM routes WHERE route_id = ?")) {

            String expectedRouteId = "10P";
            String expectedPolylineData = "[\"_jbeIpbcPEz@G?o@F_@@CoAQuAQ}@g@{Aa@cC]oCIm@KD\",\"yqbeIhpbPQcCEiAKI{@u@sBqBeAmA\"]";


            pstmt.setString(1, expectedRouteId);
            ResultSet rs = pstmt.executeQuery();

            assertTrue(rs.next(), "Expected data should be present in the database");
            assertEquals(expectedPolylineData, rs.getString("polyline_data"), "Polyline data should match the expected value");
        }
    }

    @Test
    public void testProcessPolylines() throws Exception {

        addRoutes.processPolylines(polylineTestFile, mockConnection);


        verify(mockConnection).prepareStatement(
                "MERGE INTO routes (route_id, polyline_data) KEY (route_id) VALUES (?, ?)"
        );
        verify(mockPreparedStatement).setString(1, "6");
        verify(mockPreparedStatement).setString(2, new JSONArray(Files.readString(polylineTestFile.toPath())).toString());
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testProcessPolylinesFileNotFound() {

        File nonExistentFile = new File("nonexistent.json");

        assertThrows(Exception.class, () -> {
            addRoutes.processPolylines(nonExistentFile, mockConnection);
        });
    }

    @Test
    public void testProcessStopRefs() throws Exception {
        // Call the method under test.
        addRoutes.processStopRefs(StopRefTestFile, mockConnection);

        // Verify the SQL interaction.
        verify(mockConnection).prepareStatement(
                "MERGE INTO routes (route_id, stop_point_refs) KEY (route_id) VALUES (?, ?)"

        );
        verify(mockPreparedStatement).setString(1, "6");
        verify(mockPreparedStatement).setString(2, new JSONArray(Files.readString(StopRefTestFile.toPath())).toString());
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testProcessStopRefFileNotFound() {

        File nonExistentFile = new File("nonexistent.json");

        assertThrows(Exception.class, () -> {
            addRoutes.processPolylines(nonExistentFile, mockConnection);
        });
    }

    @Test
    public void testGetUniqueIdentifier() {
        String identifier = addRoutes.GetUniqueIdentifier("path/to/AMSY_6_test.json");
        assertEquals("6", identifier);
    }
}

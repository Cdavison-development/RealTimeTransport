package com.project.busfinder.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
class PolylineDecoderTest {

    @Test
    public void testDecodePolyline() {
        // Initialise encoded linestring
        String encodedPolyline = "mfp_Iat|uO`KX";
        // Decode into list of coords
        List<PolylineDecoder.Coordinate> coordinates = PolylineDecoder.decodePolyline(encodedPolyline);

        // Verify size of the list
        assertEquals(2, coordinates.size());

        // Expected values found from https://developers.google.com/maps/documentation/utilities/polylineutility
        PolylineDecoder.Coordinate coord1 = coordinates.get(0);
        assertEquals(52.51703, coord1.latitude, 0.00001);
        assertEquals(87.64241, coord1.longitude, 0.00001);

        PolylineDecoder.Coordinate coord2 = coordinates.get(1);
        assertEquals(52.51510, coord2.latitude, 0.00001);
        assertEquals(87.64228, coord2.longitude, 0.00001);
    }

    @Test
    void readPolylineFromFile() throws IOException {
        // specify file path
        String filePath = "data/test_encoded_polyline.txt";
        // write the encoded polyline to file
        Files.write(Paths.get(filePath), "mfp_Iat|uO`KX".getBytes());

        //read polyline from file
        String polyline = PolylineDecoder.readPolylineFromFile(filePath);

        //verify that the read polyline matches the expected value
        assertEquals("mfp_Iat|uO`KX", polyline);

        // Clean up by deleting test file
        Files.delete(Paths.get(filePath));
    }

    @Test
    void writeLinestringToFile() throws IOException {
        // specify file path for LINESTRING output
        String filePath = "data/test_linestring_output.txt";

        // Define the LINESTRING to be written to the file
        String linestring = "LINESTRING (-120.2 38.5, -120.95 40.7)";

        // write LINESTRING to file
        PolylineDecoder.writeLinestringToFile(linestring, filePath);

        // Read LINESTRING from file
        String result = new String(Files.readAllBytes(Paths.get(filePath)));

        // Verify that the read LINESTRING matches the expected value
        assertEquals(linestring, result);

        // Clean up by deleting the test file
        Files.delete(Paths.get(filePath));
    }
}
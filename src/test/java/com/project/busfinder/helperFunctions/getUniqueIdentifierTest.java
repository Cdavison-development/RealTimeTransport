package com.project.busfinder.helperFunctions;

import org.junit.jupiter.api.Test;

import static com.project.busfinder.helperFunctions.getUniqueIdentifer.GetUniqueIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class getUniqueIdentifierTest {

    @Test
    public void testGetUniqueIdentifier() {
        String identifier = GetUniqueIdentifier("path/to/AMSY_6_test.json");
        assertEquals("6", identifier);
    }
}

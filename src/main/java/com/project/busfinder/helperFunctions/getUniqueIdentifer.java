package com.project.busfinder.helperFunctions;

import java.nio.file.Path;
import java.nio.file.Paths;

public class getUniqueIdentifer {

    public static String GetUniqueIdentifier(String filepath) {
        // extract the file name from the file path
        Path path = Paths.get(filepath);
        String fileName = path.getFileName().toString();
        System.out.println("Processing file: " + fileName);

        // split the file name by underscores to extract the unique identifier
        String[] underscoreSplit = fileName.split("_");
        if (underscoreSplit.length > 2) {
            System.out.println("Extracted Route ID: " + underscoreSplit[1]);
            return underscoreSplit[1]; // return the second part as the unique identifier
        } else {
            System.out.println("Filename does not contain enough parts: " + fileName);
            return ""; // return an empty string if the file name is not in the expected format
        }
    }
}

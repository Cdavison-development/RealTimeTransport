package com.project.busfinder.helperFunctions;

import java.nio.file.Path;
import java.nio.file.Paths;

public class getUniqueIdentifer {

    public static String GetUniqueIdentifier(String filepath) {
        Path path = Paths.get(filepath);
        String fileName = path.getFileName().toString();
        System.out.println("Processing file: " + fileName);

        String[] underscoreSplit = fileName.split("_");
        if (underscoreSplit.length > 2) {
            System.out.println("Extracted Route ID: " + underscoreSplit[1]);
            return underscoreSplit[1];
        } else {
            System.out.println("Filename does not contain enough parts: " + fileName);
            return "";
        }
    }
}

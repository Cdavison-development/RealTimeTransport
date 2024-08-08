package com.project.busfinder.Mapping;

import com.sothawo.mapjfx.Coordinate;

import java.util.List;

public class polylineHelpers {
    private static final double TOLERANCE = 0.00001;

    public static int findClosestCoordinateIndex(List<Coordinate> coordinates, Coordinate target) {
        int closestIndex = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate coord = coordinates.get(i);
            double distance = haversineDistance(coord.getLatitude(), coord.getLongitude(), target.getLatitude(), target.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        if (closestIndex == -1) {
            System.out.println("No close coordinate found for target: " + target);
        } else {
            System.out.println("Closest index for target " + target + " is " + closestIndex + " with coordinate " + coordinates.get(closestIndex));
        }

        return closestIndex;
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                + Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }

    public static int[] findClosestSegmentIndices(List<Coordinate> coordinates, Coordinate target) {
        int closestIndex1 = -1;
        int closestIndex2 = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < coordinates.size() - 1; i++) {
            Coordinate coord1 = coordinates.get(i);
            Coordinate coord2 = coordinates.get(i + 1);

            double distance = pointToSegmentDistance(coord1, coord2, target);
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex1 = i;
                closestIndex2 = i + 1;
            }
        }

        System.out.printf("Closest segment for target %s is between indices %d and %d%n", target, closestIndex1, closestIndex2);
        return new int[]{closestIndex1, closestIndex2};
    }

    private static double pointToSegmentDistance(Coordinate coord1, Coordinate coord2, Coordinate target) {
        double x0 = target.getLatitude();
        double y0 = target.getLongitude();
        double x1 = coord1.getLatitude();
        double y1 = coord1.getLongitude();
        double x2 = coord2.getLatitude();
        double y2 = coord2.getLongitude();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double d = dx * dx + dy * dy;
        double t = ((x0 - x1) * dx + (y0 - y1) * dy) / d;

        if (t < 0) {
            dx = x0 - x1;
            dy = y0 - y1;
        } else if (t > 0) {
            dx = x0 - x2;
            dy = y0 - y2;
        } else {
            dx = x0 - (x1 + t * dx);
            dy = y0 - (y1 + t * dy);
        }

        return Math.sqrt(dx * dx + dy * dy);
    }
}

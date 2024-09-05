package com.project.busfinder.Mapping_util;

import com.sothawo.mapjfx.Coordinate;

import java.util.List;

public class polylineHelpers {
    private static final double TOLERANCE = 0.00001;


    public static int[] findClosestSegmentIndices(List<Coordinate> coordinates, Coordinate target) {
        int closestIndex1 = -1;
        int closestIndex2 = -1;
        double minDistance = Double.MAX_VALUE;

        // iterate through each pair of consecutive coordinates in the list
        for (int i = 0; i < coordinates.size() - 1; i++) {
            Coordinate coord1 = coordinates.get(i);
            Coordinate coord2 = coordinates.get(i + 1);

            // calculate the distance from the target coordinate to the current segment
            double distance = pointToSegmentDistance(coord1, coord2, target);
            if (distance < minDistance) {
                // update the minimum distance and the closest segment indices
                minDistance = distance;
                closestIndex1 = i;
                closestIndex2 = i + 1;
            }
        }

        // return the indices of the closest segment
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
            // if t is less than 0, the closest point on the segment is coord1
            dx = x0 - x1;
            dy = y0 - y1;
        } else if (t > 0) {
            // if t is greater than 1, the closest point on the segment is coord2
            dx = x0 - x2;
            dy = y0 - y2;
        } else {
            // if 0 <= t <= 1, the closest point lies on the segment between coord1 and coord2
            dx = x0 - (x1 + t * dx);
            dy = y0 - (y1 + t * dy);
        }

        // return the Euclidean distance from the target to the closest point on the segment
        return Math.sqrt(dx * dx + dy * dy);
    }
}

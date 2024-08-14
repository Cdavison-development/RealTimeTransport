package com.project.busfinder.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class ResourceMonitor {

    public static void printSystemMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        // fet CPU load
        double cpuLoad = osBean.getSystemLoadAverage();
        System.out.println("CPU Load: " + cpuLoad);

        // get memory usage
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        System.out.println("Heap Memory Used: " + heapMemoryUsage.getUsed());
        System.out.println("Heap Memory Max: " + heapMemoryUsage.getMax());
    }
}
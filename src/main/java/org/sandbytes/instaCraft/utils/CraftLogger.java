package org.sandbytes.instaCraft.utils;

import org.bukkit.Bukkit;
import org.sandbytes.instaCraft.InstaCraft;
import org.sandbytes.instaCraft.config.CraftConfig;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CraftLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static volatile File logFile;
    private static final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private static int taskId = -1;

    public static void init() {
        logFile = new File(InstaCraft.getInstance().getDataFolder(), "crafting.log");

        if (!CraftConfig.getInstance().isCraftingLogEnabled()) return;

        // Flush the queue every 5 seconds (100 ticks) asynchronously
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                InstaCraft.getInstance(), CraftLogger::flush, 100L, 100L
        ).getTaskId();
    }

    public static void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        flush();
    }

    public static void log(String playerName, String item, int amount) {
        if (logFile == null) return;

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = "[" + timestamp + "] " + playerName + " crafted " + amount + "x " + item;
        logQueue.add(entry);
    }

    private static void flush() {
        if (logQueue.isEmpty() || logFile == null) return;

        checkRotation();

        // Re-reference logFile after rotation may have changed it
        File currentFile = logFile;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile, true))) {
            String entry;
            while ((entry = logQueue.poll()) != null) {
                writer.write(entry);
                writer.newLine();
            }
        } catch (IOException e) {
            InstaCraft.getInstance().getLogger().warning("Failed to write to crafting.log: " + e.getMessage());
        }
    }

    private static void checkRotation() {
        CraftConfig config = CraftConfig.getInstance();
        if (!config.isLogRotationEnabled()) return;
        if (logFile == null || !logFile.exists()) return;

        long maxBytes = (long) config.getLogMaxSizeMb() * 1024L * 1024L;
        if (logFile.length() >= maxBytes) {
            String dateSuffix = LocalDate.now().format(DATE_FORMATTER);
            File dataFolder = InstaCraft.getInstance().getDataFolder();
            File rotated = new File(dataFolder, "crafting-" + dateSuffix + ".log");

            // If a rotated file for today already exists, append a counter
            int counter = 1;
            while (rotated.exists()) {
                rotated = new File(dataFolder, "crafting-" + dateSuffix + "-" + counter + ".log");
                counter++;
            }

            if (logFile.renameTo(rotated)) {
                logFile = new File(dataFolder, "crafting.log");
                InstaCraft.getInstance().getLogger().info("Rotated crafting.log -> " + rotated.getName());
            }
        }
    }
}

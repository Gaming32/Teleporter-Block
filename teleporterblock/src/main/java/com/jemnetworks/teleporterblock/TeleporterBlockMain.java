package com.jemnetworks.teleporterblock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TeleporterBlockMain {
    public static final String USAGE = "Usage: java -jar TeleporterBlock.jar [extractResources|help]";

    public static void main(String[] args) {
        String action = args.length >= 1 ? args[0] : "extractResources";
        switch (action) {
            case "extractResources":
                if (!extractResources()) {
                    System.exit(2);
                }
                break;
            case "help":
                System.out.println(USAGE);
                break;
            default:
                System.err.println(USAGE);
                System.exit(1);
        }
    }

    public static boolean extractResources() {
        System.out.println("Extracting resources...");

        File resourcesDir = new File("Teleporter Block Resources");
        resourcesDir.mkdirs();

        int count;
        try {
            count = extractResourcesInternal(resourcesDir);
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
        if (count == -1) return false;

        System.out.println(count + " resources extracted!");
        return true;
    }

    private static int extractResourcesInternal(File resourcesDir) throws IOException {
        int count = 0;
        CodeSource src = TeleporterBlockMain.class.getProtectionDomain().getCodeSource();
        if (src == null) {
            System.err.println("Failed to extract resource pack");
            return -1;
        }

        URL jarFile = src.getLocation();
        ZipInputStream zipFile = new ZipInputStream(jarFile.openStream());

        while (true) {
            ZipEntry entry = zipFile.getNextEntry();
            if (entry == null) break;
            if (entry.isDirectory()) continue;
            String name = entry.getName();

            switch (name) {
                case "pack.mcmeta":
                case "pack.png":
                    extractSingle(resourcesDir, name);
                    count++;
                    continue;
            }

            if (name.startsWith("assets/")) {
                String dirName = name.substring(7);
                int lastSlash = dirName.lastIndexOf("/");
                File dir = resourcesDir;
                if (lastSlash != -1) {
                    dirName = dirName.substring(0, lastSlash);
                    dir = new File(resourcesDir, dirName);
                    dir.mkdirs();
                }

                extractSingle(dir, name);
                count++;
            }
        }
        return count;
    }

    private static void extractSingle(File dir, String name) throws IOException {
        int lastSlash = name.lastIndexOf("/");
        String baseName = lastSlash == -1 ? name : name.substring(lastSlash);
        File dest = new File(dir, baseName);
        InputStream stream = TeleporterBlockMain.class.getResourceAsStream("/" + name);
        Files.copy(stream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Extracted " + name);
    }
}

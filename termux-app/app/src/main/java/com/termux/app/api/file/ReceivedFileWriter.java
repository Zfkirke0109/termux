package com.termux.app.api.file;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Saves an incoming share as one new file inside the receive directory. */
final class ReceivedFileWriter {
    private static final int MAX_COLLISIONS = 100;

    private ReceivedFileWriter() {}

    static boolean isSafeFileName(String name) {
        if (name == null || name.isEmpty() || name.trim().isEmpty() || ".".equals(name) || "..".equals(name)
            || new File(name).isAbsolute()) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '/' || c == '\\' || c < 0x20 || c == 0x7f) return false;
        }
        return true;
    }

    static File save(File receiveDir, String name, InputStream input) throws IOException {
        try (InputStream source = input) {
            if (!isSafeFileName(name)) throw new IOException("File name must be a single name without path separators");
            if (source == null) throw new IOException("Shared content could not be opened");
            if (!receiveDir.isDirectory() && !receiveDir.mkdirs())
                throw new IOException("Cannot create directory: " + receiveDir.getAbsolutePath());

            File canonicalDir = receiveDir.getCanonicalFile();
            for (int number = 0; number <= MAX_COLLISIONS; number++) {
                File destination = new File(canonicalDir, nameWithSuffix(name, number));
                // Reject a pre-existing symlink or any path that resolves outside the receive directory.
                File canonicalDestination = destination.getCanonicalFile();
                if (!canonicalDir.equals(canonicalDestination.getParentFile()))
                    throw new IOException("Received file path is outside the downloads directory");
                if (!destination.equals(canonicalDestination))
                    throw new IOException("Received file name is a symbolic link");

                FileDescriptor descriptor;
                try {
                    // O_EXCL prevents overwrites; O_NOFOLLOW also closes the check/open symlink race.
                    descriptor = Os.open(destination.getPath(), OsConstants.O_WRONLY | OsConstants.O_CREAT
                        | OsConstants.O_EXCL | OsConstants.O_NOFOLLOW, 0600);
                } catch (ErrnoException error) {
                    if (error.errno == OsConstants.EEXIST) continue;
                    throw new IOException("Cannot create received file: " + destination.getName(), error);
                }

                boolean complete = false;
                try {
                    try (FileOutputStream output = new FileOutputStream(descriptor)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = source.read(buffer)) != -1) {
                            output.write(buffer, 0, length);
                        }
                    }
                    complete = true;
                } finally {
                    if (!complete) destination.delete();
                }
                return destination;
            }
            throw new IOException("Too many files already have this name");
        }
    }

    private static String nameWithSuffix(String name, int number) {
        if (number == 0) return name;
        int dot = name.lastIndexOf('.');
        if (dot <= 0) dot = name.length();
        return name.substring(0, dot) + " (" + number + ")" + name.substring(dot);
    }
}

package com.softartdev.kvace.feature.agent.data.emulator;

public final class FileLog {
    private FileLog() {
    }

    public static void e(Throwable throwable) {
        throwable.printStackTrace();
    }
}

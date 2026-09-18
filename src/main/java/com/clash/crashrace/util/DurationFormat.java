package com.clash.crashrace.util;

public final class DurationFormat {

    private DurationFormat() {
    }

    /** Formats a duration for display, scaling the format to how long it is (e.g. "3日 04:20:15"). */
    public static String format(long ms) {
        if (ms < 0) {
            ms = 0;
        }
        long totalSeconds = ms / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%d日 %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    /** Formats a whole number of minutes for display (e.g. "1週間" / "3日" / "45分"). */
    public static String formatMinutes(long minutes) {
        if (minutes <= 0) {
            return "0分";
        }
        if (minutes % 10080 == 0) {
            return (minutes / 10080) + "週間";
        }
        if (minutes % 1440 == 0) {
            return (minutes / 1440) + "日";
        }
        if (minutes % 60 == 0) {
            return (minutes / 60) + "時間";
        }
        return minutes + "分";
    }
}

package com.znasibov.powerstats;

import android.content.Context;
import java.text.DateFormat;

import java.util.Date;

/**
 * Created by zaur on 03/04/14.
 */
public class Util {
    public static long secondsToMs(int sec) {
        return hoursToMs(0, 0, sec);
    }

    public static long minutesToMs(int min) {
        return hoursToMs(0, min, 0);
    }

    public static long minutesToMs(int min, int sec) {
        return hoursToMs(0, min, sec);
    }

    public static long daysToMs(int days) {
        return hoursToMs(days * 24);
    }

    public static long hoursToMs(int hrs) {
        return hoursToMs(hrs, 0, 0);
    }

    public static long hoursToMs(int hrs, int min) {
        return hoursToMs(hrs, min, 0);
    }

    public static long hoursToMs(int hrs, int min, int sec) {
        return 1000 * (3600 * hrs + 60 * min + sec);
    }

    public static String timestampToTimeString(long timestamp) {
        Date timestampDate = new Date(timestamp);
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        return timeFormat.format(timestampDate).toString();
    }

    public static String timestampToDateString(long timestamp) {
        Date timestampDate = new Date(timestamp);
        DateFormat dateFo = DateFormat.getDateTimeInstance();
        return dateFo.format(timestampDate).toString();
    }

    public static long precedingTimestamp(long timestamp, long period) {
        for (long t = timestamp - 1; t >= timestamp - period; t--) {
            double dt = (double)t;
            if (dt % period == 0) {
                return t;
            }
        }

        return timestamp;
    }

    public static long followingTimestamp(long timestamp, long period) {
        for (long t = timestamp; t < timestamp + period; t++) {
            double dt = (double)t;
            if (dt % period == 0) {
                return t;
            }
        }

        return timestamp;
    }

}

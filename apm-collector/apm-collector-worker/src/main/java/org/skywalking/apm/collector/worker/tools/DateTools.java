package org.skywalking.apm.collector.worker.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author pengys5
 */
public class DateTools {
    private static final SimpleDateFormat DAY_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat HOUR_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHH");
    private static final SimpleDateFormat MINUTE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

    public static int getSecond(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.SECOND);
    }

    public static long getMinuteSlice(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String timeStr = MINUTE_DATE_FORMAT.format(calendar.getTime());
        return Long.valueOf(timeStr);
    }

    public static long getHourSlice(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String timeStr = HOUR_DATE_FORMAT.format(calendar.getTime()) + "00";
        return Long.valueOf(timeStr);
    }

    public static long getDaySlice(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String timeStr = DAY_DATE_FORMAT.format(calendar.getTime()) + "0000";
        return Long.valueOf(timeStr);
    }

    public static long changeToUTCSlice(long timeSlice) {
        String timeSliceStr = String.valueOf(timeSlice);

        if (TimeZone.getDefault().getID().equals("GMT+08:00") || timeSliceStr.endsWith("0000")) {
            return timeSlice;
        } else {
            return timeSlice - 800;
        }
    }
}

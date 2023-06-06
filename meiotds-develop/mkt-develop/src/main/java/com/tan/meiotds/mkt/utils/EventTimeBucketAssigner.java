package com.tan.meiotds.mkt.utils;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer;
import org.apache.flink.util.Preconditions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EventTimeBucketAssigner<IN> implements BucketAssigner<IN, String> {
    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_FORMAT_STRING = "yyyyMMdd/HH";
    private final String formatString;
    private final ZoneId zoneId;
    private transient DateTimeFormatter dateTimeFormatter;

    public EventTimeBucketAssigner(String formatString) {
        this(formatString, ZoneId.systemDefault());
    }

    public EventTimeBucketAssigner(String formatString, ZoneId zoneId) {
        this.formatString = Preconditions.checkNotNull(formatString);
        this.zoneId = Preconditions.checkNotNull(zoneId);
    }

    public String getBucketId(IN element, Context context) {
        if (this.dateTimeFormatter == null) {
            this.dateTimeFormatter = DateTimeFormatter.ofPattern(this.formatString).withZone(this.zoneId);
        }

        return this.dateTimeFormatter.format(Instant.ofEpochMilli(context.timestamp()));
    }

    public SimpleVersionedSerializer<String> getSerializer() {
        return SimpleVersionedStringSerializer.INSTANCE;
    }

    public String toString() {
        return "EventTimeBucketAssigner{formatString='" + this.formatString + '\'' + ", zoneId=" + this.zoneId + '}';
    }
}
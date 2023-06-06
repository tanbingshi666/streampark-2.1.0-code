package com.tan.meiotds.mkt.utils;

import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;

import java.time.Duration;

/**
 * author name: tanbingshi
 * create time: 2022/10/18 11:30
 * describe content: meiotds-streaming
 */

public class FlinkStreamingSinkFileUtils {

    public static FileSink<String> getStreamSinkFileRow(String outputPath,
                                                        Duration rolloverInterval,
                                                        Duration inactivityInterval,
                                                        MemorySize maxPartSize
    ) {
        return FileSink.forRowFormat(
                        new Path(outputPath),
                        new SimpleStringEncoder<String>("UTF-8"))
                .withRollingPolicy(DefaultRollingPolicy.builder()
                        .withRolloverInterval(rolloverInterval)
                        .withInactivityInterval(inactivityInterval)
                        .withMaxPartSize(maxPartSize)
                        .build())
                .withBucketAssigner(new EventTimeBucketAssigner<String>(EventTimeBucketAssigner.DEFAULT_FORMAT_STRING))
                .build();
    }

}

package com.tan.meiotds.mkt.ods;

import cn.hutool.json.JSONUtil;
import com.tan.meiotds.mkt.utils.FlinkStreamingSinkFileUtils;
import org.apache.flink.api.common.eventtime.*;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 麦科田解析数据 sink 到 HDFS
 */
public class MktHpOdsApp {

    private static final Logger LOG = LoggerFactory.getLogger(MktHpOdsApp.class);

    public static void main(String[] args) {

        System.setProperty("HADOOP_USER_NAME", "hdfs");

        String brokers = "hadoop104:9092,hadoop102:9092,hadoop103:9092";
        List<String> topics = Collections.singletonList("MKT_INFUSION_PUMP_HP_SERIES_JSON");
        String groupId = "MKT_INFUSION_PUMP_HP_SERIES_JSON_23_06_05";
        Properties props = new Properties();

        Configuration conf = new Configuration();
        // conf.setString("execution.savepoint.path", "hdfs://hadoop101:8020/meiotds/chk/mkt/hp/ods/d1353317c29d2cc2f51dd511207d758f/chk-2");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
        env.setParallelism(3);
        // checkpoint setting from: https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/dev/datastream/fault-tolerance/checkpointing/
        env.enableCheckpointing(3 * 60 * 1000L, CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setCheckpointTimeout(60 * 1000L);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(60 * 1000L);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(2);
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        // state backend setting from: https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/ops/state/state_backends/
        env.setStateBackend(new HashMapStateBackend());
        env.getCheckpointConfig().setCheckpointStorage("hdfs://hadoop101:8020/meiotds/chk/mkt/hp/ods");
        env.getConfig().setAutoWatermarkInterval(200L);

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(brokers)
                .setTopics(topics)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setProperty("partition.discovery.interval.ms", "10000") // 动态分区发现策略
                .setProperties(props)
                .build();

        DataStream<String> kafkaSource = env.fromSource(
                source,
                new MktHpWatermarkStrategy().withIdleness(Duration.ofMinutes(1L)),
                "Kafka Source");

        String outputPath = "hdfs://hadoop101:8020/meiotds/warehouse/mkt/hp/ods";
        final FileSink<String> fileSink = FlinkStreamingSinkFileUtils.getStreamSinkFileRow(
                outputPath,
                Duration.ofMinutes(3),
                Duration.ofMinutes(5),
                MemorySize.ofMebiBytes(100));

        kafkaSource.sinkTo(fileSink);
        try {
            env.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // watermark 策略： https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/dev/datastream/event-time/generating_watermarks/#watermark-%E7%AD%96%E7%95%A5%E7%AE%80%E4%BB%8B
    static class MktHpWatermarkStrategy implements WatermarkStrategy<String> {

        @Override
        public TimestampAssigner<String> createTimestampAssigner(TimestampAssignerSupplier.Context context) {
            return new SerializableTimestampAssigner<String>() {
                @Override
                public long extractTimestamp(String line, long processTime) {
                    if (!StringUtils.isNullOrWhitespaceOnly(line)) {
                        return JSONUtil.parseObj(line).getLong("ts");
                    }
                    return 0;
                }
            };
        }

        @Override
        public WatermarkGenerator<String> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context) {
            return new MktHpPeriodWatermarkGenerator();
        }
    }

    // 自定义 watermark: https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/dev/datastream/event-time/generating_watermarks/
    static class MktHpPeriodWatermarkGenerator implements WatermarkGenerator<String> {

        private final Long delayTime = 1000L;
        private Long maxTs = Long.MIN_VALUE + delayTime;

        @Override
        public void onEvent(String line, long eventTimeTs, WatermarkOutput watermarkOutput) {
            maxTs = Math.max(eventTimeTs, maxTs);
        }

        @Override
        public void onPeriodicEmit(WatermarkOutput watermarkOutput) {
            watermarkOutput.emitWatermark(new Watermark(maxTs - delayTime - 1L));
        }
    }

}

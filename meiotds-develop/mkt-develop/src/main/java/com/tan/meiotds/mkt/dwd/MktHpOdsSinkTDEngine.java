package com.tan.meiotds.mkt.dwd;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tan.meiotds.mkt.pojo.MktHpPojo;
import com.tan.meiotds.mkt.utils.FlinkStreamingSinkJdbcUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * mkt hp系列设备工作模式为[输注中] 数据直接输出到 TDEngine
 */
public class MktHpOdsSinkTDEngine {

    public static void main(String[] args) {

        System.setProperty("HADOOP_USER_NAME", "hdfs");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        // checkpoint setting from: https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/dev/datastream/fault-tolerance/checkpointing/
        // env.enableCheckpointing(3 * 60 * 1000L, CheckpointingMode.EXACTLY_ONCE);
        // env.getCheckpointConfig().setCheckpointTimeout(60 * 1000L);
        // env.getCheckpointConfig().setMinPauseBetweenCheckpoints(60 * 1000L);
        // env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        // env.getCheckpointConfig().setTolerableCheckpointFailureNumber(2);
        // env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        // state backend setting from: https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/ops/state/state_backends/
        // env.setStateBackend(new HashMapStateBackend());
        // env.getCheckpointConfig().setCheckpointStorage("hdfs://hadoop101:8020/meiotds/chk/mkt/hp/dwd");

        String brokers = "hj103:9092";
        List<String> topics = Collections.singletonList("MKT_INFUSION_PUMP_HP_SERIES_JSON");
        String groupId = "MKT_INFUSION_PUMP_HP_SERIES_JSON_23_06_07";
        Properties props = new Properties();

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(brokers)
                .setTopics(topics)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setProperty("partition.discovery.interval.ms", "10000") // 动态分区发现策略
                .setProperties(props)
                .build();

        DataStreamSource<String> kafkaDataStream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source");
        kafkaDataStream.filter(line -> {
            try {
                JSONObject json = JSONUtil.parseObj(line);
                String state = json.getJSONObject("data").getStr("state");
                return !StringUtils.isNullOrWhitespaceOnly(state) && "0x02".equals(state);
            } catch (Exception e) {
                return false;
            }
        }).map(line -> {
            JSONObject json = JSONUtil.parseObj(line);
            MktHpPojo pojo = json.getJSONObject("data").toBean(MktHpPojo.class);
            pojo.setTs(json.getLong("ts"));
            pojo.setHost(json.getStr("host"));
            pojo.setPort(json.getInt("port"));

            pojo.setTableName("hp_" + pojo.getFactoryNum().replace("-", "_"));
            return pojo;
        }).addSink(FlinkStreamingSinkJdbcUtils.getPOJOJdbcSink(
                "com.taosdata.jdbc.rs.RestfulDriver",
                "jdbc:TAOS-RS://hj103:6041/mkt_infusion_pump_hp?user=root&password=taosdata",
                "INSERT INTO ? USING infusion_pump_hp TAGS (? ,? ,? ,? ,? ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "root",
                "taosdata",
                3
        ));

        try {
            env.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

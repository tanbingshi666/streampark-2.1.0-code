package com.tan.streampark.example;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class WordCountExample {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        env.socketTextStream("hadoop102", 9999)
                .flatMap((FlatMapFunction<String, String>) (line, out) -> {
                    for (String word : line.split(",")) {
                        out.collect(word);
                    }
                })
                .returns(String.class)
                .map((MapFunction<String, Tuple2<String, Long>>) word -> Tuple2.of(word, 1L))
                .returns(Types.TUPLE(Types.STRING, Types.LONG))
                .keyBy((KeySelector<Tuple2<String, Long>, String>) t -> t.f0)
                .sum(1)
                .print();

        env.execute("StreamPark WordCount Example Job");
    }

}

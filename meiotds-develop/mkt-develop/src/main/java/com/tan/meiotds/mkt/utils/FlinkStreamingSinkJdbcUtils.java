package com.tan.meiotds.mkt.utils;

import com.tan.meiotds.mkt.annotation.TransientSink;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FlinkStreamingSinkJdbcUtils {

    public static <T> SinkFunction<T> getPOJOJdbcSink(String driver,
                                                      String url,
                                                      String sql,
                                                      String username,
                                                      String password,
                                                      int batchSize
    ) {
        return JdbcSink.<T>sink(sql,
                new JdbcStatementBuilder<T>() {
                    @Override
                    public void accept(PreparedStatement preparedStatement, T t) throws SQLException {
                        try {
                            Field[] fields = t.getClass().getDeclaredFields();

                            int offset = 0;
                            for (int i = 0; i < fields.length; i++) {

                                Field field = fields[i];
                                field.setAccessible(true);

                                // 移除 POJO 某个属性上存在 TransientSink 注解, 该属性不作为入库字段
                                TransientSink annotation = field.getAnnotation(TransientSink.class);
                                if (annotation != null) {
                                    offset++;
                                    continue;
                                }

                                Object value = field.get(t);
                                preparedStatement.setObject(i + 1 - offset, value);
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new JdbcExecutionOptions.Builder()
                        .withBatchIntervalMs(200L)
                        .withBatchSize(batchSize)
                        .withMaxRetries(3)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withDriverName(driver)
                        .withUrl(url)
                        .withUsername(username)
                        .withPassword(password)
                        .build()
        );
    }


}

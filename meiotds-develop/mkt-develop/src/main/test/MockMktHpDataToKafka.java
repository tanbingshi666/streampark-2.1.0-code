import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class MockMktHpDataToKafka {

    public static void main(String[] args) {

        int count = 500;

        String message = "{\"host\":\"127.0.0.1\",\"port\":10003,\"code\":-1,\"header\":{},\"data\":{\"factoryNum\":\"mkt-hp-30-1001\",\"equType\":\"0x00\",\"equNum\":\"mkt-hp-30-equ-1001\",\"workSta\":\"work-sta-1001\",\"deptName\":\"手术室:ICU\",\"roomNo\":\"ICU-601\",\"bedNo\":\"601-01\",\"state\":\"0x01\",\"drugName\":\"新冠生物\",\"injectMode\":\"0x02\",\"presetValue\":12.345,\"speed\":6.04,\"alreadyInjectTime\":65,\"remainTime\":35,\"alreadyInjectValue\":10.001,\"residual\":2,\"alarm1\":\"00000000000000000000000000000001\",\"alarm2\":\"00000000000000000000000000000010\",\"alarm3\":\"00000000000000000000000000000011\",\"alarm4\":\"00000000000000000000000000000100\",\"pressureValue\":30.01,\"pressureUint\":\"0x03\"},\"ts\":1685417984666}";

        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", "hadoop104:9092,hadoop102:9092,hadoop103:9092");
        //向kafka集群发送消息,除了消息值本身,还包括key信息,key信息用于消息在partition之间均匀分布。
        //发送消息的key,类型为String,使用String类型的序列化器
        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        //发送消息的value,类型为String,使用String类型的序列化器
        kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        //创建一个KafkaProducer对象，传入上面创建的Properties对象
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(kafkaProps);

        for (int i = 0; i < 9999; i++) {
            producer.send(new ProducerRecord<>("MKT_INFUSION_PUMP_HP_SERIES_JSON", message));
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        producer.close();

    }

}

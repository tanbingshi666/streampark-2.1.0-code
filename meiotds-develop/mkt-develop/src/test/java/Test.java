import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Test {
    public static void main(String[] args) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd/HH").withZone(ZoneId.systemDefault());

        String format = dateTimeFormatter.format(Instant.ofEpochMilli(1685417987352L));
        System.out.println(format);
    }
}

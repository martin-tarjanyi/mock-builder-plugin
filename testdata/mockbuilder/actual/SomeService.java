import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class SomeService {

    public void methodWithoutReturn(int parameter) {

    }

    public String methodWithReturn(LocalDate localDate, Number number) {
        return privateStuff();
    }

    public int soPrimitive(LocalDate localDate, Number number, int parameter) {
        return 5;
    }

    public List<String> aaarrraaay(LocalDate localDate, Number number) {
        return Collections.emptyList();
    }

    public byte[] bytes(int[] kicsiSzamok) {
        return "sad".getBytes();
    }

    private String privateStuff() {
        return null;
    }
}


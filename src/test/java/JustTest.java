import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JustTest {

    @Test
    public void testCardMasking() {
        // Тест 1: Обычный номер карты
        String card1 = "1234567890123456";
        String masked1 = card1.replaceAll("\\d(?=.*\\d{4})", "*");
        assertEquals("************3456", masked1);

        // Тест 2: Номер с пробелами
        String card2 = "1234 5678 9012 3456";
        String masked2 = card2.replaceAll("\\d(?=.*\\d{4})", "*");
        assertEquals("**** **** **** 3456", masked2);

        // Тест 3: Короткий номер
        String card3 = "1234";
        String masked3 = card3.replaceAll("\\d(?=.*\\d{4})", "*");
        assertEquals("1234", masked3); // Не маскируется если меньше 5 символов
    }
}

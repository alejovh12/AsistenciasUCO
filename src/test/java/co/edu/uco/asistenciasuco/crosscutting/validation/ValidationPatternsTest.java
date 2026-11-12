package co.edu.uco.asistenciasuco.crosscutting.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Random;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * VAL-oleada1 EST-02 (java:S5998): las expresiones no usan cuantificadores anidados. Se verifica que aceptan y rechazan lo mismo que las
 * versiones anteriores (referencia solo para el contraste) y que las entradas largas o maliciosas no desbordan la pila ni se cuelgan.
 */
class ValidationPatternsTest {

    private static final Pattern PERSON_NAME_ANTERIOR = Pattern.compile("^[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*$");
    private static final Pattern EMAIL_ANTERIOR = Pattern.compile(
            "^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");

    private static final int LARGO_MALICIOSO = 200_000;
    private static final Duration LIMITE = Duration.ofSeconds(5);

    @ParameterizedTest
    @ValueSource(strings = {"Ana", "José Luis", "María-José", "O'Connor", "D’Artagnan", "Ñandú", "Zoë", "a", "Ana María de los Ángeles"})
    void personNameAceptaNombresValidos(final String nombre) {
        assertThat(ValidationPatterns.PERSON_NAME.matcher(nombre).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Ana ", " Ana", "Ana  Maria", "Ana--Maria", "-Ana", "Ana-", "Ana'", "'Ana", "Ana1", "Ana_Maria", "Ana\tMaria", "Ana\nMaria", "Ana.Maria"})
    void personNameRechazaNombresInvalidos(final String nombre) {
        assertThat(ValidationPatterns.PERSON_NAME.matcher(nombre).matches()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "user.name@uco.edu.co", "u_s%e+r-1@sub.dominio.com", "a-b@x-y.example.org", "a@b.c.com", "1@2.ab", "a.b.c@d.ee"})
    void emailAceptaCorreosValidos(final String correo) {
        assertThat(ValidationPatterns.EMAIL.matcher(correo).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "a@", "@b.co", ".a@b.co", "a.@b.co", "a..b@c.co", "a@b", "a@b.c", "a@b.c1", "a@-b.co", "a@b-.co", "a@b.-co", "a@b..co",
            "a@.b.co", "a@b.co.", "a b@c.co", "a@b@c.co", "a@b_c.co", "a@b.co-m", "a@b."})
    void emailRechazaCorreosInvalidos(final String correo) {
        assertThat(ValidationPatterns.EMAIL.matcher(correo).matches()).isFalse();
    }

    @Test
    void emailRechazaTldDe64Letras() {
        assertThat(ValidationPatterns.EMAIL.matcher("a@b." + "o".repeat(64)).matches()).isFalse();
    }

    @Test
    void emailAceptaTldDeHasta63Letras() {
        assertThat(ValidationPatterns.EMAIL.matcher("a@b." + "o".repeat(63)).matches()).isTrue();
    }

    @Test
    void personNameCoincideConLaVersionAnteriorEnEntradasAleatorias() {
        final String alfabeto = "aB \t'’-1é́_.";
        final Random azar = new Random(20261112L);
        for (int i = 0; i < 60_000; i++) {
            final String entrada = aleatoria(azar, alfabeto, 1 + azar.nextInt(9));
            assertThat(ValidationPatterns.PERSON_NAME.matcher(entrada).matches())
                    .as("entrada: %s", entrada)
                    .isEqualTo(PERSON_NAME_ANTERIOR.matcher(entrada).matches());
            assertThat(ValidationPatterns.PERSON_NAME.matcher(entrada).find())
                    .as("find, entrada: %s", entrada)
                    .isEqualTo(PERSON_NAME_ANTERIOR.matcher(entrada).find());
        }
    }

    @Test
    void emailCoincideConLaVersionAnteriorEnEntradasAleatorias() {
        final String alfabeto = "aZ9._%+-@ ";
        final Random azar = new Random(20261113L);
        for (int i = 0; i < 80_000; i++) {
            final String entrada = aleatoria(azar, alfabeto, 1 + azar.nextInt(12));
            assertThat(ValidationPatterns.EMAIL.matcher(entrada).matches())
                    .as("entrada: %s", entrada)
                    .isEqualTo(EMAIL_ANTERIOR.matcher(entrada).matches());
        }
        // Estructurados: parte local y dominio generados por separado para cubrir mas casos validos.
        final String local = "a._-";
        final String dominio = "a1.-";
        for (int i = 0; i < 80_000; i++) {
            final String entrada = aleatoria(azar, local, 1 + azar.nextInt(5)) + "@" + aleatoria(azar, dominio, 1 + azar.nextInt(7))
                    + (azar.nextBoolean() ? ".co" : ".c");
            assertThat(ValidationPatterns.EMAIL.matcher(entrada).matches())
                    .as("entrada: %s", entrada)
                    .isEqualTo(EMAIL_ANTERIOR.matcher(entrada).matches());
        }
    }

    @Test
    void personNameResisteEntradasLargasYMaliciosas() {
        final String[] entradas = {
                "a".repeat(LARGO_MALICIOSO),
                "a".repeat(LARGO_MALICIOSO) + "1",
                "a ".repeat(LARGO_MALICIOSO / 2) + "1",
                "a-".repeat(LARGO_MALICIOSO / 2),
                " ".repeat(LARGO_MALICIOSO),
                "a" + "-".repeat(LARGO_MALICIOSO) + "a",
                "a'".repeat(LARGO_MALICIOSO / 2) + "!"
        };
        assertTimeoutPreemptively(LIMITE, () -> {
            for (final String entrada : entradas) {
                ValidationPatterns.PERSON_NAME.matcher(entrada).matches();
            }
        });
        assertThat(ValidationPatterns.PERSON_NAME.matcher("a ".repeat(LARGO_MALICIOSO / 2) + "b").matches()).isTrue();
    }

    @Test
    void emailResisteEntradasLargasYMaliciosas() {
        final String[] entradas = {
                "a".repeat(LARGO_MALICIOSO),
                "a".repeat(LARGO_MALICIOSO) + "@",
                "a.".repeat(LARGO_MALICIOSO / 2) + "@b.co",
                "a@" + "a.".repeat(LARGO_MALICIOSO / 2) + "!",
                "a@" + "a-".repeat(LARGO_MALICIOSO / 2) + ".co",
                "a@" + "a".repeat(LARGO_MALICIOSO) + ".c",
                "a@" + "-.".repeat(LARGO_MALICIOSO / 2),
                "a@" + ".".repeat(LARGO_MALICIOSO),
                "a@b." + "o".repeat(LARGO_MALICIOSO),
                "a@b." + "o".repeat(LARGO_MALICIOSO) + "1",
                "@".repeat(LARGO_MALICIOSO)
        };
        assertTimeoutPreemptively(LIMITE, () -> {
            for (final String entrada : entradas) {
                ValidationPatterns.EMAIL.matcher(entrada).matches();
            }
        });
        assertThat(ValidationPatterns.EMAIL.matcher("a.".repeat(LARGO_MALICIOSO / 2) + "b@c.co").matches()).isTrue();
    }

    private static String aleatoria(final Random azar, final String alfabeto, final int largo) {
        final StringBuilder sb = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) {
            sb.append(alfabeto.charAt(azar.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }
}

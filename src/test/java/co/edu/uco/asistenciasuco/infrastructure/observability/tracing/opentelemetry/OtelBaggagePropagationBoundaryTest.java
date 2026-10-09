package co.edu.uco.asistenciasuco.infrastructure.observability.tracing.opentelemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-002 / CVE-2026-45292: dependency behavioral contract, not an integration
 * test with Tomcat or an external trace exporter. The limits are owned by the
 * patched upstream OpenTelemetry W3C propagator, not by application code.
 *
 * <p>Deliberately deterministic, small fixtures without resource-exhaustion.
 */
class OtelBaggagePropagationBoundaryTest {

    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(final Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(final Map<String, String> carrier, final String key) {
            return carrier.get(key);
        }
    };

    private static Baggage extract(final String header) {
        final Context context = W3CBaggagePropagator.getInstance()
                .extract(Context.root(), Map.of("baggage", header), GETTER);
        return Baggage.fromContext(context);
    }

    @Test
    void valid_baggage_header_preserves_all_expected_entries() {
        final Baggage baggage = extract("session=abc,group=G1,user=U1");
        assertEquals(3, baggage.size());
        assertEquals("abc", baggage.getEntryValue("session"));
        assertEquals("G1", baggage.getEntryValue("group"));
        assertEquals("U1", baggage.getEntryValue("user"));
    }

    @Test
    void malicious_header_with_excessive_entries_is_bounded_to_64_entries() {
        final StringJoiner header = new StringJoiner(",");
        for (int entry = 0; entry < 90; entry++) {
            header.add("key" + entry + "=value" + entry);
        }

        final Baggage baggage = extract(header.toString());
        assertTrue(baggage.size() <= 64, "Do not parse unbounded baggage entries");
        assertEquals("value0", baggage.getEntryValue("key0"));
        assertEquals("value63", baggage.getEntryValue("key63"));
        assertNull(baggage.getEntryValue("key64"), "Entries exceeding the limit must be dropped");
    }

    @Test
    void malicious_header_larger_than_8192_bytes_is_rejected() {
        final String header = "oversized=" + "x".repeat(8192);
        final Baggage baggage = extract(header);
        assertTrue(baggage.isEmpty(), "Oversized baggage must not be allocated into context");
    }
}

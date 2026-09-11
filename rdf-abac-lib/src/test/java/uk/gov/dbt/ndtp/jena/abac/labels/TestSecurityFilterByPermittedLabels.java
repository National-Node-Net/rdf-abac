package uk.gov.dbt.ndtp.jena.abac.labels;

import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Test;

public class TestSecurityFilterByPermittedLabels {

    private static final Triple triple1 = parseTriple("(:s :p 123)");
    private static final Quad quad1 = Quad.create(Quad.defaultGraphIRI, triple1);

    private static LabelsGetter fixedLabels(List<String> forTriple1) {
        return t -> t.equals(triple1) ? forTriple1 : List.of();
    }

    @Test
    void permittedLabel_admitsTriple() {
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(fixedLabels(List.of("employee")), null, Set.of("employee", "public"));

        assertTrue(filter.test(quad1));
    }

    @Test
    void nonPermittedLabel_rejectsTriple() {
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(fixedLabels(List.of("military")), null, Set.of("employee", "public"));

        assertFalse(filter.test(quad1));
    }

    @Test
    void multipleLabels_ANDSemantics_allMustBePermitted() {
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(fixedLabels(List.of("employee", "hr")), null, Set.of("employee"));

        assertFalse(filter.test(quad1));
    }

    @Test
    void multipleLabels_allPermitted_admitsTriple() {
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(fixedLabels(List.of("employee", "hr")), null, Set.of("employee", "hr"));

        assertTrue(filter.test(quad1));
    }

    @Test
    void noLabelForTriple_usesDefaultLabel() {
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(fixedLabels(List.of()), "public", Set.of("public"));

        assertTrue(filter.test(quad1));
    }

    @Test
    void noLabelForTriple_defaultNotPermitted_rejectsTriple() {
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(fixedLabels(List.of()), "public", Set.of("employee"));

        assertFalse(filter.test(quad1));
    }

    @Test
    void emptyPermittedSet_rejectsEverything() {
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(fixedLabels(List.of("public")), null, Set.of());

        assertFalse(filter.test(quad1));
    }
}

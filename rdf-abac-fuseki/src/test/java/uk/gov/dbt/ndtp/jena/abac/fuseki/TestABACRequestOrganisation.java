package uk.gov.dbt.ndtp.jena.abac.fuseki;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;

public class TestABACRequestOrganisation {

    @Test
    void extractOrganisationId_findsPermittedOrganisationsAttribute() {
        AttributeValueSet attributes = AttributeValueSet.of(List.of(
                AttributeValue.of(Attribute.create("employee"), ValueTerm.TRUE),
                AttributeValue.of(Attribute.create("permitted_organisations"), ValueTerm.value("Org1"))
        ));

        String result = ABAC_Request.extractOrganisationId(attributes);

        assertEquals("Org1", result);
    }

    @Test
    void extractOrganisationId_missingAttribute_returnsNull() {
        AttributeValueSet attributes = AttributeValueSet.of("employee", "clearance");

        String result = ABAC_Request.extractOrganisationId(attributes);

        assertNull(result);
    }

    @Test
    void extractOrganisationId_emptyAttributes_returnsNull() {
        AttributeValueSet attributes = AttributeValueSet.of(List.of());

        String result = ABAC_Request.extractOrganisationId(attributes);

        assertNull(result);
    }
}

package uk.gov.dbt.ndtp.jena.pbac.fuseki;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;

public class TestPBACRequestOrganisation {

    @Test
    void extractOrganisationId_findsPermittedOrganisationsAttribute() {
        AttributeValueSet attributes = AttributeValueSet.of(List.of(
                AttributeValue.of(Attribute.create("employee"), ValueTerm.TRUE),
                AttributeValue.of(Attribute.create("permitted_organisations"), ValueTerm.value("Org1"))
        ));

        String result = PBAC_Request.extractOrganisationId(attributes);

        assertEquals("Org1", result);
    }

    @Test
    void extractOrganisationId_missingAttribute_returnsNull() {
        AttributeValueSet attributes = AttributeValueSet.of("employee", "clearance");

        String result = PBAC_Request.extractOrganisationId(attributes);

        assertNull(result);
    }

    @Test
    void extractOrganisationId_emptyAttributes_returnsNull() {
        AttributeValueSet attributes = AttributeValueSet.of(List.of());

        String result = PBAC_Request.extractOrganisationId(attributes);

        assertNull(result);
    }
}

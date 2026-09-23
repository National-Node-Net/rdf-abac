package uk.gov.dbt.ndtp.jena.pbac.fuseki;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtServletConstants;

public class TestServerPBACJwt {

    @Test
    void userForRequestFromJwt_extractsEmailFromClaims() {
        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn("alice@example.com");

        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(JwtServletConstants.REQUEST_ATTRIBUTE_RAW_JWT)).thenReturn(jws);

        HttpAction action = mock(HttpAction.class);
        when(action.getRequest()).thenReturn(request);

        String result = ServerPBAC.userForRequestFromJwt().apply(action);

        assertEquals("alice@example.com", result);
    }

    @Test
    void userForRequestFromJwt_noJwtAttribute_fallsBackToLegacy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(JwtServletConstants.REQUEST_ATTRIBUTE_RAW_JWT)).thenReturn(null);
        when(request.getRemoteUser()).thenReturn(null);

        HttpAction action = mock(HttpAction.class);
        when(action.getRequest()).thenReturn(request);
        when(action.getRequestHeader(anyString())).thenReturn(null);

        String result = ServerPBAC.userForRequestFromJwt().apply(action);

        assertNull(result);
    }
}

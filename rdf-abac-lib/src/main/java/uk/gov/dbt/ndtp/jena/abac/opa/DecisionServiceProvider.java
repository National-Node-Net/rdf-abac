package uk.gov.dbt.ndtp.jena.abac.opa;

import java.util.Set;

/**
 * Decision service abstraction: given who is asking, what they're doing, and the full
 * label vocabulary of the dataset, return the subset they may see.
 * <p>
 * Implementations must never permit a label absent from {@code vocabulary} (SAG-04 AC7),
 * even if the underlying decision service's raw response somehow includes one — the
 * intersection is enforced here, not left to the caller.
 */
public interface DecisionServiceProvider {

    /**
     * @throws DecisionServiceUnavailableException if the decision service could not be
     *         reached at all, or if a response was received but couldn't be trusted —
     *         see that class's javadoc for the confirmed split with an empty
     *         {@link DecisionResult}.
     */
    DecisionResult decide(DecisionContext context, Set<String> vocabulary);
}
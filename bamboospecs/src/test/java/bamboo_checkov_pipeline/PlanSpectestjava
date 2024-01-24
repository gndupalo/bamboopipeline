package bamboo_checkov_pipeline;

import org.junit.Test;

import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.util.EntityPropertiesBuilders;

public class PlanSpecTest {
    @Test
    public void checkYourPlanOffline() {
        Plan plan = new PlanSpec().createPlan();

        EntityPropertiesBuilders.build(plan);
    }
}

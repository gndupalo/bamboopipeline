package bamboo_checkov_pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.builders.repository.git.UserPasswordAuthentication;
import com.atlassian.bamboo.specs.builders.repository.github.GitHubRepository;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.DockerBuildImageTask;
import com.atlassian.bamboo.specs.builders.task.DockerPullImageTask;
import com.atlassian.bamboo.specs.builders.task.DockerPushImageTask;
import com.atlassian.bamboo.specs.builders.task.DockerRunContainerTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;
import com.atlassian.bamboo.specs.builders.trigger.RemoteTrigger;
import com.atlassian.bamboo.specs.util.BambooServer;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Plan configuration for Bamboo.
 *
 * @see <a href=
 *      "https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs">Bamboo
 *      Specs</a>
 */
@BambooSpec
public class PlanSpec {

    Dotenv dotenv = Dotenv.load();

    public static void main(String[] args) throws Exception {
        // by default credentials are read from the '.credentials' file

        Dotenv dotenv = Dotenv.load();
        String serverHost = dotenv.get("SERVER_HOST");

        BambooServer bambooServer = new BambooServer(serverHost);

        Plan plan = new PlanSpec().createPlan();
        bambooServer.publish(plan);

        PlanPermissions planPermission = new PlanSpec().createPlanPermission(plan.getIdentifier());
        bambooServer.publish(planPermission);
    }

    PlanPermissions createPlanPermission(PlanIdentifier planIdentifier) {
        Permissions permissions = new Permissions()
                .userPermissions("bamboo", PermissionType.ADMIN)
                .groupPermissions("bamboo-admin", PermissionType.ADMIN)
                .loggedInUserPermissions(PermissionType.BUILD)
                .anonymousUserPermissionView();

        return new PlanPermissions(planIdentifier)
                .permissions(permissions);
    }

    GitHubRepository repository() {
        String username = dotenv.get("GITHUB_USERNAME");
        String token = dotenv.get("GITHUB_TOKEN");
        String repo = dotenv.get("GITHUB_REPOSITORY");

        return new GitHubRepository()
                .branch("main")
                .name("Checkov Pipeline")
                .repository(repo)
                .authentication(new UserPasswordAuthentication(username)
                        .password(token));
    }

    Project project() {
        String planName = dotenv.get("BAMBOO_PROJECT_NAME");
        String planKey = dotenv.get("BAMBOO_PROJECT_KEY");

        return new Project()
                .name(planName)
                .key(planKey)
                .repositories(repository());
    }

    VcsCheckoutTask vcsCheckoutTask() {
        CheckoutItem specificRepository = new CheckoutItem()
                .repository(
                        "Checkov Pipeline");

        return new VcsCheckoutTask()
                .description("Checkout Repository")
                .cleanCheckout(false)
                .checkoutItems(specificRepository);
    }

    DockerBuildImageTask dockerBuildImageTask() {
        Path dockerfilePath = Paths.get("bamboo.Dockerfile");

        String fileContent = "";

        try {
            fileContent = Files.readString(dockerfilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String dockerImage = dotenv.get("DOCKER_IMAGE");

        return new DockerBuildImageTask()
                .description("Build Docker Image")
                .dockerfile(fileContent)
                .imageName(dockerImage);
    }

    DockerPushImageTask dockerPushImageTask() {
        String dockerImage = dotenv.get("DOCKER_IMAGE");

        String dockerhubUsername = dotenv.get("DOCKERHUB_USERNAME");
        String dockerhubPassword = dotenv.get("DOCKERHUB_PASSWORD");

        return new DockerPushImageTask()
                .description("Push Docker Image")
                .authentication(dockerhubUsername, dockerhubPassword)
                .dockerHubImage(dockerImage);
    }

    DockerPullImageTask dockerPullImageTask() {
        String dockerImage = dotenv.get("DOCKER_IMAGE");

        String dockerhubUsername = dotenv.get("DOCKERHUB_USERNAME");
        String dockerhubPassword = dotenv.get("DOCKERHUB_PASSWORD");

        return new DockerPullImageTask()
                .description("Pull Docker Image")
                .authentication(dockerhubUsername, dockerhubPassword)
                .dockerHubImage(dockerImage);
    }

    DockerRunContainerTask dockerRunContainerTask() {
        String dockerImage = dotenv.get("DOCKER_IMAGE");

        return new DockerRunContainerTask()
                .description("Run Checkov Container")
                .containerWorkingDirectory("/app")
                .imageName(dockerImage);
    }

    Job job() {
        return new Job("Default Job", "RUN")
                .tasks(
                        vcsCheckoutTask(),
                        dockerBuildImageTask(),
                        dockerPushImageTask(),
                        dockerPullImageTask(),
                        dockerRunContainerTask());
    }

    Stage stage() {
        return new Stage("Default Stage")
                .jobs(job());
    }

    Plan createPlan() {
        String planName = dotenv.get("BAMBOO_PLAN_NAME");
        String planKey = dotenv.get("BAMBOO_PLAN_KEY");

        return new Plan(project(), planName, planKey)
                .description("Plan created from Bamboo Java Specs")
                .stages(stage())
                .planRepositories(repository())
                .triggers(new RemoteTrigger()
                        .name("GitHub Repo On Push")
                        .description("GitHub On Push Event")
                        .triggerIPAddresses(
                        "140.82.112.0/20,192.30.252.0/22"));
    }
}

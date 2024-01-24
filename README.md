# Bamboo Checkov Pipeline

This is a sample Bamboo CI/CD pipeline to run the Checkov IaC code security analysis tool on terraform config.

## Prerequisites

1. A Linux server (4GB RAM recommended) with java>=11, maven and docker installed, with appropriate enviroment setup (e.g., JAVA_HOME environment variable, adding maven to the system PATH, etc.). You can also refer to [this](https://confluence.atlassian.com/bamboo/bamboo-best-practice-system-requirements-388401170.html) for system requirements.
2. An account on Docker Hub to push the docker image to.
3. Java>=11 and maven installed on your local machine(s) from where you will create Bamboo plans.

## Setup Instructions

1. Install [Bamboo](https://confluence.atlassian.com/bamboo/bamboo-installation-guide-289276785.html) on the server to `/opt/atlassian-bamboo-<version>`.
2. Setup a `systemd service` using the example service config file given in `scripts/bamboo.service` in this repo. Make the necessary changes in the .service file.
3. Before starting the service, run a postgres database instance using docker (feel free to change the username and password, but don't change the database name).

```shell
mkdir db-data
docker container run -d --net host --name bamboo-postgres -v ./db-data:/var/lib/postgresql/data -e POSTGRES_USER=bamboo-admin -e POSTGRES_PASSWORD=bamboo -e POSTGRES_DB=bamboo postgres:latest
```

4. To be able to access the bamboo server from the internet, you need to run it on the public IP or hostname of your server, instead of the default `localhost`. Make sure you have appropriate networking rules set up to allow inbound TCP traffic on port 8085, and 54663 (for remote agents). Then, go to `/opt/atlassian-bamboo-<version>/conf` and open the file `server.xml`. Find the block `<Engine name="Catalina" defaultHost="localhost">` and replace all instances of "localhost" with either `0.0.0.0`, your public IP or hostname.
5. Enable and start the bamboo service.
6. Visit `http://<server_ip_or_hostname>:8085` on your browser and follow the on-screen instructions to complete the initial setup of Bamboo. It will also prompt you for a license, so if you haven't already, get a license by logging into your atlassian account.
7. Once the setup is done, you can proceed further to the agent installation part.

## Agent Setup

### Method 1: Elastic Bamboo

Elastic Bamboo instances are automatically created AWS EC2 instances which are managed directly by Bamboo. When you create a new elastic instance in Bamboo, it automatically runs a Bamboo Agent on the EC2 instance. To get started:

1. Open the configuration page in the Bamboo web UI.
2. In the Elastic Bamboo section in the left navigation bar, go to `Configuration`, and enable EC2. This will ask for your AWS credentials, like access keys, so keep them handy.
3. Once configured, you can go to the `Instances` menu in the left navigation bar and start a new instance, or multiple instances (by specifying the number of instances, this can lead to high AWS and Bamboo costs so use wisely). Once the instance(s) is/are spin up, it will wait for the agents to start on these instances. Once the agents are running, and the status says `Idle`, then they are ready for use.

<b>Note:</b> The elastic instances are ephemeral, and are automatically deleted if not in use. If you see that the plan is not running, you should check whether there is an instance online with an agent running on it.

### Method 2: Manually Installing Remote Agents

We can also download the remote agent program manually and install it on a VM and make it connect to and manageable by our main Bamboo server. To get started:

1. Make sure the VM has atleast 4GB RAM and java>=11, maven, git and docker installed, with appropriate enviroment setup (e.g., JAVA_HOME environment variable, adding maven to the system PATH, etc.).
2. On you Bamboo server, go to `<server_url>/admin/agent/addRemoteAgent.action` and download the `.jar` file for the remote agent onto your agent VM.
3. Then run the following command (replace `<server_url>` with the URL of your Bamboo server and `<version>` with your Bamboo version):

```
java -jar atlassian-bamboo-agent-installer-<version>.jar <server_url>/agentServer/
```

4. To run the above as a background process:

```
nohup java -jar atlassian-bamboo-agent-installer-<version>.jar <server_url>/agentServer/ &
```

5. Authenticate the agent when prompted.
6. Wait for the setup to complete. The agent will be up and running and ready to use.
7. Repeat the above steps for all the agents that you need.

### Method 3: Local Agents
Local agents are the ones that run on the same machine as the Bamboo server, and are managed by the server itself. No additional installation is required. Just go to the `agents` page, click on `Add local agent` on the top-right corner, give it a name and optionally a description, and the agent is good to go.

## Creating and running the plan

1. Clone this repo to your local machine.
2. The `bamboo-specs` directory contains the java code for the Bamboo plan. `cd` into this directory and run the following:

```shell
cp .env.example .env
touch .credentials
```

3. In the `.credentials` file, declare two variables, `username` and `password`, with their values set to the username and password configured while setting up Bamboo, respectively. For example -

```
username=bamboo
password=bamboo
```

4. In the `.env` file, set the appropriate values of the variables.
5. This repo was created with Bamboo version 9.4.2, which is also pinned in the `pom.xml` file present in the `bamboo-specs` directory. If you installed a different version of Bamboo, then you need to make the appropriate changes on line 9 of `pom.xml`, in the `bamboo-specs-parent` artifactId section.
6. Now run the following command to create the plan in Bamboo from this spec:

```shell
mvn -Ppublish-specs
```

7. If the above step is successful, you should now have a working plan in Bamboo, ready to use.
8. To run the plan, first make sure you have an agent up and running. Once you have an agent running, run the plan manually by opening it in the web UI of Bamboo, and clicking on the "Run plan" button on the top-right corner of the page. The logs will be streamed to the UI after every 10 seconds.
9. Wait for the plan to complete. The plan will fail if checkov reports any failed cases in the given terraform config. If no security issues are found, then the plan will report a successful execution.
10. To run the plan after a `github push` event, add the following webhook to your github repo:

```
<BAMBOO_URL>/rest/triggers/latest/remote/changeDetection?planKey=<PLAN-KEY>
```
11. Set the content-type as `application/json`.
12. In your Bamboo server, go to `Security Settings` and enable the `Allow anonymous users to trigger remote repository change detection and Bamboo Specs detection` option to allow triggering the build from webhooks.
13. On the top-most navigation bar, go to the `Specs` dropdown and click on the `Set up Specs repository` option.
14. In project type, select `Build project`, and select your project from the dropdown.
15. Link your repository in repository host, by providing your GitHub username and personal access token.
16. Now your webhook is ready to trigger builds from `github push` events.

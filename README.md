This deme showcases:

* [Red Hat Runtimes](https://www.redhat.com/en/products/runtimes){:target="_blank"} - a collection of cloud-native runtimes like Spring Boot, Node.js, and [Quarkus](https://quarkus.io){:target="_blank"}
* [Red Hat OpenShift](https://www.openshift.com/){:target="_blank"} - You'll use one or more _projects_ (Kubernetes namespaces) that are your own and are isolated from other workshop students
* [Red Hat CodeReady Workspaces](https://developers.redhat.com/products/codeready-workspaces/overview){:target="_blank"} - based on **Eclipse Che**, it's a cloud-based, in-browser IDE (similar to IntelliJ IDEA, VSCode, Eclipse IDE). You've been provisioned your own personal workspace for use with this workshop. You'll write, test, and deploy code from here.
* [Red Hat AMQ Streams](https://www.redhat.com/en/technologies/jboss-middleware/amq){:target="_blank"} - streaming data platform based on **Apache Kafka**
* [Knative](https://knative.dev) - A kubernetes-native way to deploy serverless workloads
* [Camel-k] - A lightweight integration platform, born on Kubernetes, with serverless superpowers, based on [Apache Camel](http://camel.apache.org)
* [Prometheus](https://prometheus.io){:target="_blank"} and [Grafana](https://grafana.com){:target="_blank"}, an application monitoring and alerting stack

How to run
========
To run this you will first need to:

. [Deploy](https://try.openshift.com) an OpenShift 4.x cluster
. Once installed, login as as a `cluster-admin` user (e.g. `kubeadmin`) on the OpenShift console, visit the _Operator Hub_ and install the following Operators using their default configuration values:
* **CodeReady Workspaces** (this provides an IDE - you could technically use a local IDE like VScode but you wouldn't be able to directly contact the Kafka cluster without exposing it to the world)
* **AMQ Streams** (provides Kafka cluster capabilities)
* **OpenShift Serverless** (which provides _Knative serving_ capabilities)
* **Knative Eventing** (this is not part of the _OpenShift Serverless_ operator at this time so you need this separately)
* **Knative Apache Kafka Operator** - this provides a Knative `KafkaSource` type which can send cloud events from Kafka to a Knative _channel_

Wait a few minutes so that the operators are properly downloaded and activated.

Next, create the following _projects_ (Kubernetes namespaces) using the _Project > Create Project_ dropdown menu -- you can actually name these to whatever you want, but suggestions are below:

* `demo` (will hold the components of the demo)
* `codeready` (will hold a deployment of CodeReady Workspaces)

Next, in OCP 4.2, switch to the _Developer_ View, select the `codeready` project in the project selector drop-down menu, then select the **Add** menu on the left, click on **From Catalog**, type in `codeready` in the search box, click on _Red Hat CodeReady Workspaces Cluster_, click _Create_, and then accept the default YAML contents for the CodeReady `CheCluster` custom resource, and click _Create_, This will begin installing CodeReady into the `codeready` namespace and you'll get pods for a Postgres database, Keyclock auth server, and CodeReady itself.

Next, repeat the above process to:

* Install an instance of Apache Kafka cluster in the `knative-eventing` namespace using the `Kafka Cluster` custom resource type from the catalog using the default YAML (so that the name of the cluster is `my-cluster`). This will install a 3-node instance of Kafka in the namespace (so you'll get 3 _zookeeper_ pods and 3 _kafka_ pods)
* Install an instance of the _Knative components for Apache Kafka_ custom resource in the `knative-eventing` namespace using the default YAML, and be sure to set `setAsDefaultChannelProvisioner: false` (so that it points at the above instance) when creating the instance, so that it doesn't try to use Kafka for all your Knative _channel_s. This will allow you to later create an instance of the `KafkaSource` which will be able to generate events from Kafka into the Knative _channel_.
* Install another instance of _Apache Kafka Cluster_ in the `demo` namespace which our demo app will use.
* Create an instance of a `KafkaTopic` in the `demo` namespace, and change the name of the topic to `names` with `metadata.name: names` in the YAML

### Building a custom stack

To build a custom stack, you'll need `docker` **and a _Red Hat Developer_ account**. Login to Docker Hub and create a new repository with a name `e.g. username/che-quarkus`).

In the `src/main/che` directory, create a file called `rhsm.secret` which will hold your Red Hat Developer credentials. This file should look like:

```
RH_USERNAME=your_rh_developer_email
RH_PASSWORD=your_rh_developer_password
```

On a local command line, login to docker using `docker login` with your docker credentials.

Run the following commands from the `src/main/che` directory of your clone of this repository in a local terminal to build and push the custom stack (replace `YOUR_DOCKER_USERNAME` with your docker username)

```sh
DOCKER_BUILDKIT=1 docker build --progress=plain --secret id=rhsm,src=rhsm.secret -t docker.io/YOUR_DOCKER_USERNAME/che-quarkus:latest -f stack.Dockerfile .
docker push docker.io/YOUR_DOCKER_USERNAME/che-quarkus:latest
```

Note the above command references a `Dockerfile` which will in turn reference your Red Hat Developer credentials to install several Linux components necessary to build and run Quarkus and Camel-K apps.

Next, login to your OpenShift cluster as an admin (e.g. `kubeadmin`) using `oc login` and then install an imagestream pointing at this custom stack using the `oc` command (again, replace `YOUR_DOCKER_USERNAME` with your docker username):

```sh
cat <<EOF | oc create -f -
apiVersion: image.openshift.io/v1
kind: ImageStream
metadata:
  name: quarkus-stack
  namespace: openshift
spec:
  tags:
  - annotations:
      description: Quarkus stack for Java and CodeReady Workspaces
      iconClass: icon-java
      supports: java
      tags: builder,java
      version: "1.0"
    from:
      kind: DockerImage
      name: docker.io/YOURR_DOCKER_USERNAME/che-quarkus:latest
    name: "1.0"
EOF
```

### Installing the stack

Next, back in the OpenShift console, switch back to the _Administrator_ video, navigate to the `codeready` namespace, find the _Route_ (url) to CodeReady by going to _Networking > Routes_ screen. Click on it, and login as `admin/admin`. Then, in your browser replace the contents of the URL address at the end to be `CODEREADY_URL/swagger` (where `CODEREADY_URL` is the base route URL clicked on earlier). This brings you to the swagger screen. Expand the `stack` API and find the `POST /stack` menu, and click on it. In the `body` field, copy and paste the contents of the `src/main/che/stack.json` file from this repository (replacing `YOUR_DOCKER_USERNAME` with your docker username), and click the **Try It!** button. You should verify you've received a `HTTP 201` in the respose. This adds a custom Quarkus stack built earlier to the list of possible stacks to use when creating workspaces.

Go back to the main CodeReady Workspaces dashboard, create a new workspace using your custom stack, and click _Create and Open_. This will start a new workspaces in CodeReady.

Once it's up and running, choose _Import Project_ and import this github repository using its URL. Designate the project as a _Maven_ project.


### Installing Knative components

Our sample app reads from Kafka and from a Knative _channel_. To allow this to happen, run the following commands from the `src/main/camelk` directory to create a `KafkaSource` (which will read from Kafka topic `names` and send them to a Knative _channel_):

```sh
oc create -f channel.yml
oc create -f kafkasource.yml
```

### Twilio

This sample app shows how to run a sample Camel-K integration embodied in the `src/main/camelk/sms.groovy` Camel route. It relies on you having a [Twilio](https://twilio.com) account and an SMS phone number provisioned. Obtain your Twilio credentials (your _Account SID_ and _Auth Token_), and create a file called `application.properties.secret` in the `src/main/camelk` directory that looks like:

```
camel.component.twilio.username=YOUR_SID
camel.component.twilio.password=YOUR_AUTH_TOKEN
```

replacing `YOUR_SID` with your account `SID`, and `YOUR_AUTH_TOKEN` with your authentication token.

Next, create a Kubernetes `ConfigMap` using:

`oc create configmap twilio-config --from-file=application.properties=application.properties.secret`

This will create a `ConfigMap` whose _key_ is `application.properties` and value is the contents of the `application.properties.secret` file.

### Running the demo

In CodeReady, you can try to run the application locally using the _Command Palette_ and selecting `Start Live Coding`. This will run the app and begin sending messages to Kafka. Ensure no errors, then close the Terminal window to stop the app.

Then, to deploy the app to OpenShift, use the `Create Executable JAR` in the CodeReady command palette. This will build the executable .JAR file in the `target/` directoryt. Next, login to OpenShift using `oc login $KUBERNETES_SERVICE_HOST:$KUBERNETES_SERVICE_PORT` in a Terminal in CodeReady using your admin credentials (e.g. `kubeadmin` and the password given to you when you installed OpenShift). Then run `oc project demo` to switch to the
`demo` project, and deploy the app using:

```sh
oc new-build registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift:1.5 --binary --name=names -l app=names
oc start-build names --from-file target/*-runner.jar --follow
oc new-app names
oc expose svc/names
```

Your app will now run in OpenShift and also start generating names. Click on the _Route_ for the app to see a word cloud of names.

### Deploying the CamelK route

To deploy the integration, inspect the `sms.groovy` file in the `src/main/camelk` directory. Replate the `TO` and `FROM` phone numbers with your personal SMS phone number in the `TO` field and your Twilio registered phone number in the `FROM` field (each phone number should be in the form`+[COUNTRTY_CODE][NUMBER]` for example, for USA-based numbers `+14074544545`). Then, run this integration by running this command from the `src/main/camelk` directory in CodeReady:

```sh
kamel --config ~/.kube/config run --configmap=twilio-config  sms.groovy --dev -d mvn:com.fasterxml.jackson.core:jackson-databind:2.8.2 -d camel-base64
```

Your integration will be deployed as a Knative service, and begin to listen on the `names` Kafka topic, and for each name it will send an SMS to your phone!


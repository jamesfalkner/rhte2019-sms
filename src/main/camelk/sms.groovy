// camel-k: language=groovy
//
// kamel run --configmap=twilio-config  sms.groovy --dev -d mvn:com.fasterxml.jackson.core:jackson-databind:2.8.2 -d camel-base64
// oc rsh -n demo my-cluster-kafka-0 bin/kafka-console-consumer.sh --topic names --bootstrap-server localhost:9092
//
import com.twilio.type.PhoneNumber;

from('knative:channel/smsout')
    .unmarshal().base64()
  .to('log:info')
  .convertBodyTo(String.class)
  .log('received message: ${body}')
  .setHeader("CamelTwilioTo", constant(new PhoneNumber("TO")))
  .setHeader("CamelTwilioFrom", constant(new PhoneNumber("FROM")))
  .setHeader("CamelTwilioBody", simple('${body}'))
  .to('twilio://message/creator')

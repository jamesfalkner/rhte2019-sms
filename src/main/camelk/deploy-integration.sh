#!/bin/sh
kamel --config ~/.kube/config run --configmap=twilio-config  sms.groovy --dev -d mvn:com.fasterxml.jackson.core:jackson-databind:2.8.2 -d camel-base64


#!/bin/sh

echo "Downloading Mend agent..."
curl -L https://github.com/whitesource/unified-agent-distribution/releases/latest/download/wss-unified-agent.jar -o mend/wss-unified-agent.jar

echo "Running Mend scan..."
java -jar wss-unified-agent.jar -apiKey ${API_KEY} -userKey ${USER_KEY} -c mend/mend.config -d ./

echo "Removing Mend agent..."
rm mend/wss-unified-agent.jar
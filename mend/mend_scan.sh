#!/bin/sh

echo "Downloading Mend agent..."
curl -LJO https://github.com/whitesource/unified-agent-distribution/releases/latest/download/wss-unified-agent.jar

echo "Running Mend scan..."
java -jar wss-unified-agent.jar -apiKey ${API_KEY} -userKey ${USER_KEY} -c mend.config -d ../
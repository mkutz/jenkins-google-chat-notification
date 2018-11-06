#!/usr/bin/env groovy

import static groovy.json.JsonOutput.toJson

void call(final String simpleTextMessage, final String url = env.GOOGLE_CHAT_URL) {
    final String requestBody = toJson([text: simpleTextMessage]
    echo requestBody
    httpRequest(requestBody: requestBody), url: url, httpMode: 'POST', contentType: 'APPLICATION_JSON_UTF8')
}

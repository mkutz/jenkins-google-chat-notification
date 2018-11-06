#!/usr/bin/env groovy

import static groovy.json.JsonOutput.toJson

void call(final String simpleTextMessage, final String url = env.GOOGLE_CHAT_URL) {
    final String body = [text: simpleTextMessage]
    httpRequest(requestBody: toJson(body),
        url: url, httpMode: 'POST', contentType: 'APPLICATION_JSON_UTF8')
}

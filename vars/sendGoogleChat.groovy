#!/usr/bin/env groovy

import static groovy.json.JsonOutput.toJson

void call(final String simpleTextMessage, final String url) {
    final String body = [text: simpleTextMessage]
    httpRequest(requestBody: toJson(body),
        url: url, httpMode: 'POST', contentType: 'APPLICATION_JSON_UTF8')
}

void buildReport(final String url, final Map<String, String> buildProperties = [:]) {
    buildProperties."Build" = "#${env.BUILD_NUMBER}"
    if (env.VERSION) buildProperties."Version" = "v${env.VERSION}"
    buildProperties."Cause" = "${currentBuild.buildCauses.shortDescription.join(", ")}"
    if (currentBuild.changeSets) buildProperties."Changes" = "${currentBuild.changeSets.join(", ")}"

    Map<String, String> actions = [
        "BUILD": env.BUILD_URL,
        "CONSOLE": "${env.BUILD_URL}console",
        "TESTS": "${env.BUILD_URL}testReport"
    ]

    final Map<String, Object> complexMessage = [
        cards: [
            [
                header: [
                    title: "Build Status <b>${env.JOB_NAME}<b>"
                ],
                sections: []
            ]
        ]
    ]

    if (buildProperties) {
        complexMessage.cards[0].sections << [
            header: "Properties",
            widgets: buildProperties.collect { key, value -> [keyValue: [topLabel: key, content: value]] }
        ]
    }

    if (params) {
        complexMessage.cards[0].sections << [
            header: "Parameters",
            widgets: params.collect { key, value -> [keyValue: [topLabel: key, content: value]] }
        ]
    }

    if (actions) {
        complexMessage.cards[0].sections << [
            widgets: [buttons: actions.collect { label, url -> [textButton: [text: label, onClick: [openLink: [url: url]]]]] }
        ]
    }

    httpRequest(requestBody: toJson(complexMessage),
            url: url, httpMode: 'POST', contentType: 'APPLICATION_JSON_UTF8')
}
#!/usr/bin/env groovy

import static groovy.json.JsonOutput.toJson

void call(final String url = env.GOOGLE_CHAT_URL) {
    final Map<String, Object> buildProperties = [:]
    buildProperties."Build" = "#${env.BUILD_NUMBER}"
    if (env.VERSION) buildProperties."Version" = "v${env.VERSION}"
    buildProperties."Cause" = "${currentBuild.buildCauses.shortDescription.join(", ")}"
    if (currentBuild.changeSets.logs) buildProperties."Changes" = "${currentBuild.changeSets.logs.msg.join(", ")}"

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
            widgets: buildProperties.collect { key, value ->
                [keyValue: [topLabel: "${key}", content: "${value}"]]
            }
        ]
    }

    if (params) {
        complexMessage.cards[0].sections << [
            header: "Parameters",
            widgets: params.collect { key, value ->
                [keyValue: [topLabel: "${key}", content: "${value}"]]
            }
        ]
    }

    if (actions) {
        complexMessage.cards[0].sections << [
            widgets: [
                [
                    buttons: 
                        actions.collect { label, href ->
                            [textButton: [text: label, onClick: [openLink: [url: href]]]]
                        }
                ]
            ]
        ]
    }

    final String requestBody = toJson(complexMessage)
    echo requestBody

    httpRequest(requestBody: requestBody, url: url, httpMode: 'POST', contentType: 'APPLICATION_JSON_UTF8')
}

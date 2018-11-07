#!/usr/bin/env groovy

import static groovy.json.JsonOutput.toJson

void call(final Map<String, String> buildProperties = [:], final String url = env.GOOGLE_CHAT_URL) {
    final Map<String, String> RESULT_IMGS = [
        SUCCESS: "https://jenkins.io/doc/book/resources/blueocean/dashboard/status-passed.png",
        UNSTABLE: "https://jenkins.io/doc/book/resources/blueocean/dashboard/status-unstable.png",
        FAILURE: "https://jenkins.io/doc/book/resources/blueocean/dashboard/status-failed.png",
        NOT_BUILT: "https://jenkins.io/doc/book/resources/blueocean/dashboard/status-in-progress.png",
        ABORTED: "https://jenkins.io/doc/book/resources/blueocean/dashboard/status-aborted.png"
    ]
    final Map<String, String> RESULT_TEXT = [
        SUCCESS: "succeeded",
        UNSTABLE: "is unstable",
        FAILURE: "failed",
        NOT_BUILT: "is in progress",
        ABORTED: "is aborted"
    ]
    buildProperties."Cause" = "${currentBuild.buildCauses.shortDescription.join(", ")}"
    if (currentBuild.changeSets.logs) {
        buildProperties."Changes" = "${currentBuild.changeSets.logs.msg.flatten().join(", ")}"
    }
    final Map<String, String> actions = [
        "BUILD": env.BUILD_URL,
        "CONSOLE": "${env.BUILD_URL}console",
        "TESTS": "${env.BUILD_URL}testReport"
    ]

    final Map<String, Object> complexMessage = [
        cards: [
            [
                header: [
                    title: "Build status ${env.JOB_NAME} ${RESULT_TEXT[currentBuild.result]}",
                    subtitle: "#${env.BUILD_NUMBER}",
                    imageUrl: RESULT_IMGS[currentBuild.result],
                    imageStlye: "AVATAR"
                ],
                sections: []
            ]
        ]
    ]

    if (buildProperties.message) {
        final String message = buildProperties.remove("message")
        complexMessage.cards[0].sections << [
            "widgets": [
                [
                    textParagraph: [text: message]
                ]
            ]
        ]
    }

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

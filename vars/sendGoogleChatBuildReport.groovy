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
    if (currentBuild.hasProperty("buildCauses")) {
        buildProperties."Cause" =  "${currentBuild."buildCauses"?.shortDescription?.join(", ")}"
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
                    title: "${env.JOB_NAME}",
                    subtitle: "#${env.BUILD_NUMBER} ${RESULT_TEXT[currentBuild.currentResult]}",
                    imageUrl: RESULT_IMGS[currentBuild.currentResult] ?: RESULT_IMGS["NOT_BUILT"],
                    imageStyle: "AVATAR"
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
                [keyValue: [topLabel: "${key}", content: "${value}", contentMultiline: "true"]]
            }
        ]
    }

    if (currentBuild.changeSets.logs) {
        complexMessage.cards[0].sections << [
            header: "Changes",
            widgets: currentBuild.changeSets.logs.flatten().collect { log ->
                [
                    keyValue: [
                        topLabel: "${new Date(log.timestamp).toLocaleString()}",
                        content: "${log.msg}",
                        bottomLabel: "${log.author.displayName}",
                        contentMultiline: "true"]
                ]
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

    if (currentBuild.rawBuild.artifacts) {
        echo "The build has artifacts: ${currentBuild.rawBuild.artifacts.fileName.join(", ")}"

        complexMessage.cards[0].sections << [
            header: "Artifatcs",
            widgets: [
                currentBuild.rawBuild.artifacts.findAll { it.fileName ==~ /^.*\.(je?pg|png|gif)$/ }.collect { image ->
                    [
                        image: [
                            imageUrl: "${env.BUILD_URL}artifact/${image.href}",
                            onClick: [openLink: [url: "${env.BUILD_URL}artifact/${image.href}"]]
                        ]
                    ]
                }
            ]
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

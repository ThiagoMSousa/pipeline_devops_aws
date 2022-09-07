package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class SnsStack extends Stack {

    // Atríbuto que representa o tópico criado
    private final SnsTopic productEventsTopic;

    public SnsStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SnsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Criando o Tópico
        // Nome do SNS para Identificação na AWS - ProductEventsTopic
        productEventsTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "ProductEventsTopic")
                .topicName("product-events") // Nome do Tópico que sera Identificado no Console AWS
                .build())
                .build();

        // Criando um Subscrição no tópico, para enviar E-mail no formato JSON
        productEventsTopic.getTopic().addSubscription(EmailSubscription.Builder.create("thiagomendes_95@hotmail.com")
                .json(true)
                .build());
    }

    public SnsTopic getProductEventsTopic() {
        return productEventsTopic;
    }
}

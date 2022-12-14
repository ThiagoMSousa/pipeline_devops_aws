package br.com.dev.thiagomds.infra_aws_catalogo_produtos;

import software.amazon.awscdk.Fn;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class Service02Stack extends Stack {
    public Service02Stack(final Construct scope,
                          final String id,
                          Cluster cluster,
                          SnsTopic productEventsTopic,
                          Table productEventsDynamoDb) {
        this(scope, id, null, cluster, productEventsTopic, productEventsDynamoDb);
    }

    public Service02Stack(final Construct scope,
                          final String id,
                          final StackProps props,
                          Cluster cluster,
                          SnsTopic productEventsTopic,
                          Table productEventsDynamoDb) {
        super(scope, id, props);


        // Criando a Fila
        Queue productEventsDlq = Queue.Builder.create(this, "ProductEventsDlq")
                .queueName("product-events-dlq")
                .build();

        // Criando a Fila DLQ
        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                .queue(productEventsDlq) // Nome da Fila de Leitura para DLQ
                .maxReceiveCount(3) // Definindo a quantidade m??xima de tentativas para evoluir para DLQ
                .build();

        Queue productEventsQueue = Queue.Builder.create(this, "ProductEvents")
                .queueName("product-events")
                .deadLetterQueue(deadLetterQueue)
                .build();

        // Escrevendo a FILA no T??pico
        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(productEventsQueue).build();
            productEventsTopic.getTopic().addSubscription(sqsSubscription);

        // Definindo Vari??veis de Ambiente
        Map<String, String> envVariables = new HashMap<>();

        // Regi??o da AWS
        envVariables.put("AWS_REGION", "us-east-1");

        // Nome da Fila
        envVariables.put("AWS_SQS_QUEUE_PRODUCT_EVENTS_NAME", productEventsQueue.getQueueName());

        ApplicationLoadBalancedFargateService service02 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB_consumer_catalogo_produto")
                .serviceName("service-02")
                .cluster(cluster)
                .cpu(512)
                .memoryLimitMiB(1024)
                .desiredCount(2)
                .listenerPort(9090)
                .assignPublicIp(true)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("consumer_catalogo_produtos")
                                .image(ContainerImage.fromRegistry("thiagomdes/consumer_catalogo_produtos:1.0.3"))
                                .containerPort(9090)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this,
                                                        "ConsumerCatalogoProdutosLogGroup")
                                                .logGroupName("Service02")
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix("Service02")
                                        .build()))
                                .environment(envVariables)
                                .build())
                .publicLoadBalancer(true)
                .build();


        service02.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("9090")
                .healthyHttpCodes("200")
                .build());

        // Atribuindo permiss??o ao Service para consumo das mensagens da Fila
        productEventsQueue.grantConsumeMessages(service02.getTaskDefinition().getTaskRole());

        // Definindo Permiss??o de Leitura e Escrita na Tabela DynamoDB
        productEventsDynamoDb.grantReadWriteData(service02.getTaskDefinition().getTaskRole());
    }
}

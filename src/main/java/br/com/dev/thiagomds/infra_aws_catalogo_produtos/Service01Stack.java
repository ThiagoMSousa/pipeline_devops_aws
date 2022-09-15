package br.com.dev.thiagomds.infra_aws_catalogo_produtos;

import software.amazon.awscdk.Fn;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic) {
        this(scope, id, null, cluster, productEventsTopic);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic productEventsTopic) {
        super(scope, id, props);

        // Definindo Variáveis de Ambiente
        Map<String, String> envVariables = new HashMap<>();

        // String de Conexão do Banco de Dados
        envVariables.put("SPRING_DATASOURCE_URL", "jdbc:mariadb://" + Fn.importValue("rds-endpoint")
        + ":3306/catalogo-produtos?createDatabaseIfNotExist=true");
        // String do Usuário de acesso
        envVariables.put("SPRING_DATASOURCE_USERNAME", "admin");
        // String da Password de acesso
        envVariables.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("rds-password"));
        // Região da AWS
        envVariables.put("AWS_REGION", "us-east-1");
        // ARNdo Tópico SNS
        envVariables.put("AWS_SNS_TOPIC_PRODUCT_EVENTS_ARN", productEventsTopic.getTopic().getTopicArn());

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB01")
                .serviceName("service-01")
                .cluster(cluster)
                .cpu(512)
                .memoryLimitMiB(1024)
                .desiredCount(2)
                .listenerPort(8080)
                .assignPublicIp(true)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("catalogo_produtos")
                                .image(ContainerImage.fromRegistry("thiagomdes/catalogo_produtos:1.0.8"))
                                .containerPort(8080)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this,
                                                        "Service01LogGroup")
                                                .logGroupName("Service01")
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix("Service01")
                                        .build()))
                                .environment(envVariables)
                                .build())
                .publicLoadBalancer(true)
                .build();


        service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("8080")
                .healthyHttpCodes("200")
                .build());

        // Dando a permissão de publicar mensagens, a partir da TaskRole da Task Definition
        productEventsTopic.getTopic().grantPublish(service01.getTaskDefinition().getTaskRole());

    }
}

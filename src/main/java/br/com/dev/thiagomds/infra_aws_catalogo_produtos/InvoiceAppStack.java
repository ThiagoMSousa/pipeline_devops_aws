package br.com.dev.thiagomds.infra_aws_catalogo_produtos;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.notifications.SnsDestination;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;


// import software.amazon.awscdk.Duration;

public class InvoiceAppStack extends Stack {

    private final Bucket bucket;
    private final Queue s3InvoiceQueue;

    public InvoiceAppStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InvoiceAppStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        // Criando o Tópico
        SnsTopic s3InvoiceTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "S3InvoiceTopic")
                // Atríbuindo Nome ao Tópico
                .topicName("s3-invoice-events")
                .build())
                .build();

        // Criando o Bucket S3
        bucket = Bucket.Builder.create(this, "S301")
                // Atribuindo Nome ao Bucket
                // O nome deve ser único na region
                .bucketName("pcs-invoice")
                // Define se a Stack for Deletada, o que fazer com o Bucket S3
                // Neste exemplo, DESTRÓI o BUCKET
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Configurando Notificação, no caso de chegar um arquivo no Bucket. gere uma notificação no tópico
        // Quando acontecer a criação de um objeto pelo método PUT (OBJECT_CREATED_PUT), será gerado uma notificação
        // para o tópico s3InvoiceTopic
        bucket.addEventNotification(EventType.OBJECT_CREATED_PUT, new SnsDestination(s3InvoiceTopic.getTopic()));

        // Criando a Fila que será utilizado no DLQ
        Queue s3InvoiceDlq = Queue.Builder.create(this, "S3InvoiceDlq")
                // Definindo o nome da Fila
                .queueName("s3-invoice-events-dlq")
                .build();

        // Criando a Fila DLQ
        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                // Atríbuindo a fila criada
                .queue(s3InvoiceDlq)
                // Criando a politica, em caso de 3 exceções, coloca a mensagem de volta na DLQ
                .maxReceiveCount(3)
                .build();

        // Criando a Fila
        s3InvoiceQueue = Queue.Builder.create(this, "S3InvoiceQueue")
                // Atribuindo o nome da fila
                .queueName("s3-invoice-events")
                // Atribuindo a Fila DLQ
                .deadLetterQueue(deadLetterQueue)
                .build();

        // Criando a Subscrição do SQS na Fila Criada
        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(s3InvoiceQueue).build();
        // Acionando a Subscription do SQS no Tòpico Criado
        s3InvoiceTopic.getTopic().addSubscription(sqsSubscription);
    }

    public Bucket getBucket() {
        return bucket;
    }

    public Queue getS3InvoiceQueue() {
        return s3InvoiceQueue;
    }
}

package br.com.dev.thiagomds.infra_aws_catalogo_produtos;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.constructs.Construct;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class DynamoDbStack extends Stack {
    private final Table productEventDynamoDb;

    public DynamoDbStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamoDbStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        productEventDynamoDb = Table.Builder.create(this, "ProductEventsDynamoDb")
                // Definindo o Nome da Tabela
                .tableName("product-events")
                // Definindo o Modelo de Provisionamento
                .billingMode(BillingMode.PROVISIONED)
                // Definindo a Capacidade de Leitura
                .readCapacity(1)
                // Definindo a Capacidade Escrita
                .writeCapacity(1)
                // Definindo o Campo de Partition Key
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                // Definindo o Campo de Sort Key
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl")
                // Define se a Stack for Deletada, o que fazer com a tabela do Dynamo
                // Neste exemplo, DESTRÓI a tabela
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    public Table getProductEventDynamoDb() {
        return productEventDynamoDb;
    }
}

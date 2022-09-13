package br.com.dev.thiagomds.infra_aws_catalogo_produtos;

import software.amazon.awscdk.App;

public class PipelineDevopsAwsApp {
    public static void main(final String[] args) {
        App app = new App();

        // Criando a VPC
        VpcStack vpcStack = new VpcStack(app, "Vpc");

        // Criando o Cluster atrelando a VPC
        ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
        // Colocando dependencia da Criação da VPC para depois criar o Cluster
        clusterStack.addDependency(vpcStack);

        // Criando o RDS
        RdsStack rdsStack = new RdsStack(app, "Rds", vpcStack.getVpc());
        rdsStack.addDependency(vpcStack);

        // Criando o SNS
        SnsStack snsStack = new SnsStack(app, "Sns");

        // Criando a Stack do Service01Stack referente aos serviços do "Catalogo de Produtos"
        Service01Stack service01Stack = new Service01Stack(
                app,
                "Service01",
                clusterStack.getCluster(),
                snsStack.getProductEventsTopic());
        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);

        DynamoDbStack dynamoDbStack = new DynamoDbStack(app, "DynamoDb");

        // Criando a Stack do Service02Stack referente aos serviços do "Consumer Catalogo de Produtos"
        Service02Stack service02Stack = new Service02Stack(app,
                "Service02",
                clusterStack.getCluster(),
                snsStack.getProductEventsTopic(),
                dynamoDbStack.getProductEventDynamoDb());
        service02Stack.addDependency(clusterStack);
        service02Stack.addDependency(snsStack);
        service02Stack.addDependency(dynamoDbStack);
        app.synth();
    }
}


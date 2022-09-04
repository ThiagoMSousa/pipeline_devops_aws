package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

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


        // Criando a Stack do Service01Stack
        Service01Stack service01Stack = new Service01Stack(app, "Service01", clusterStack.getCluster());
        service01Stack.addDependency(clusterStack);
        
        app.synth();
    }
}


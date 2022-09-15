package br.com.dev.thiagomds.infra_aws_catalogo_produtos;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class RdsStack extends Stack {
    public RdsStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);

        // Definindo Parameters para Salvar a Senha do Banco de Dados
        CfnParameter databasePassword = CfnParameter.Builder.create(this, "databasePassword")
                .type("String")
                .description("The RDS Instance Password")
                .build();

        // Buscando o SecurityGroup Padrão da VCP
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        // Definindo uma Nova Regra de Entrada no Security Group
        // Porta de Entrada do RDS MySQL no Security Group
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));


        // Criando a Instância do Database RDS
        DatabaseInstance databaseInstance = DatabaseInstance.Builder
                .create(this, "Rds01")
                // Definindo o Nome de Identificação da Instância
                .instanceIdentifier("catalogo-produtos-db")
                // Definindo o Tipo e a Versão da Instancia
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                                .version(MysqlEngineVersion.VER_8_0_28)
                        .build()))
               // Incluindo a Instância dentro da VPC
                .vpc(vpc)
                // Criando Credenciação utilizando o mecanismo de Usuário e Senha
                // Definindo Senha Fixa
                .credentials(Credentials.fromUsername("admin",
                        CredentialsFromUsernameOptions.builder()
                                // Pegando a Senha através do Parameter
                                .password(SecretValue.unsafePlainText(databasePassword.getValueAsString()))
                                .build()))
                // Definindo o tamanho da máquina que irá executar a instância
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                // Não criando a instância em mais de uma Zona de Disponibilidade
                // true = cria em várias Zonas de Disponibilidade
                // false = não cria em várias Zonas de Disponibilidade
                .multiAz(false)
                // Definindo o Tamanho do Disco
                // Alocando 10GB no disco
                .allocatedStorage(10)
                // Definindo a Instância dentro do Security Group
                .securityGroups(Collections.singletonList(iSecurityGroup))
                // Definindo quais são as Sub-redes dentro da minha instância.
                .vpcSubnets(SubnetSelection.builder()
                        // O ideial é colocar .getPrivateSubnets
                        // Deixado .getPublicSubnets devido à redução de custos
                        // Por conta do parametro .natGateways(0)
                        .subnets(vpc.getPublicSubnets())
                        .build())
                .build();

        // Exportando Paramêtros para utilização em outros serviços/stack
        // Exportação do Nome da Instância do Banco de Dados
        CfnOutput.Builder.create(this, "rds-endpoint")
                .exportName("rds-endpoint")
                .value(databaseInstance.getDbInstanceEndpointAddress())
                .build();

        // Exportação da Senha da Instância do Banco de Dados
        CfnOutput.Builder.create(this, "rds-password")
                .exportName("rds-password")
                .value(databasePassword.getValueAsString())
                .build();
    }
}

package com.example.aws;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientV2Builder;
import com.amazonaws.services.s3.AmazonS3EncryptionV2;
import com.amazonaws.services.s3.model.CryptoConfigurationV2;
import com.amazonaws.services.s3.model.CryptoMode;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication(scanBasePackages = {"com.example.aws.controller","com.example.aws.service"})
public class AwsApplication {

	@Value("${kms.key}")
	private String kmsKeyId;

	public static void main(String[] args) {
		SpringApplication.run(AwsApplication.class, args);
	}

	@Bean
	@Primary
	public AmazonS3 amazonS3(){
		return AmazonS3ClientBuilder.standard()
				.withCredentials(new ProfileCredentialsProvider())
				.withRegion(Regions.AP_SOUTH_1)
				.build();
	}

	@Bean
	public AmazonS3EncryptionV2 amazonS3EncryptionClient(){
		KMSEncryptionMaterialsProvider materialProvider = new  KMSEncryptionMaterialsProvider(kmsKeyId);
		return new AmazonS3EncryptionClientV2Builder()
				.withCredentials(new ProfileCredentialsProvider())
				.withEncryptionMaterialsProvider(materialProvider)
				.withCryptoConfiguration(new CryptoConfigurationV2(CryptoMode.AuthenticatedEncryption).withAwsKmsRegion(Region.getRegion(Regions.AP_SOUTH_1)))
				.withRegion(Regions.AP_SOUTH_1)
				.build();
	}


	@Bean
	public AmazonEC2 amazonEC2Client(){
		return AmazonEC2ClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
	}

	@Bean
	public AmazonRDS amazonRDSClient(){
		return AmazonRDSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
	}

	@Bean
	public AmazonDynamoDB amazonDynamoDBClient(){
		return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
	}

}

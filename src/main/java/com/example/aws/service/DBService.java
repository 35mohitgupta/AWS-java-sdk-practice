package com.example.aws.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.Tag;
import com.amazonaws.services.rds.model.*;
import com.amazonaws.waiters.WaiterParameters;
import com.example.aws.dtos.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

//import com.example.aws.repository.UserRepository;

@Service
@Slf4j
public class DBService {

    @Autowired
    private AmazonEC2 amazonEC2;

    @Autowired
    private AmazonRDS amazonRDS;

    @Autowired
    private AmazonDynamoDB dynamoDB;


//    @Autowired
//    private UserRepository userRepository;
    String rdsIdentifier = "my-rds-db";
    public String createMySQL(){

        String dbName = "myrdspocdb";
        String sgName = "rds-sg-dev-demo";
        String userName = "masteruser";
        String userPassword = "mymasterpassw0rd1!";
        String adminEmail = "myemail@myemail.com";
        String sgIdNumber = "";
        String rdsEndpoint = "";
        DescribeSecurityGroupsResult securityGroupsResult = amazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(sgName));
        String securityGroupId = securityGroupsResult.getSecurityGroups().get(0).getGroupId();
        List<Tag> tags = new ArrayList<>(2);
        tags.add(new Tag().withKey("POC-Email").withValue(adminEmail));
        tags.add(new Tag().withKey("Purpose").withValue("AWS Developer Study Guide Demo"));
        CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest()
                .withDBInstanceClass("db.t2.micro")
                .withDBInstanceIdentifier(rdsIdentifier)
                .withDBName(dbName)
                .withEngine("mysql")
                .withMasterUsername("masterUser")
                .withMasterUserPassword("m1Passw0rd!")
                .withVpcSecurityGroupIds(securityGroupId)
                .withAllocatedStorage(15)
                .withTags(tags);

        DBInstance dbInstance = amazonRDS.createDBInstance(createDBInstanceRequest);
        log.info("Creating the RDS instance. This may take several minutes...");
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest()
                .withDBInstanceIdentifier(rdsIdentifier);

        WaiterParameters<DescribeDBInstancesRequest> waiterParameters = new WaiterParameters()
                .withRequest(describeDBInstancesRequest);
        amazonRDS.waiters().dBInstanceAvailable().run(waiterParameters);
        log.info("Created db instance with id {}", dbInstance.getDBInstanceIdentifier());
        return  dbInstance.getDBInstanceIdentifier();
    }

    public String getRDSEndpoint(){
        String rdsIdentifier = "my-rds-db";
        DescribeDBInstancesResult describeDBInstancesResult = amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(rdsIdentifier));
        return describeDBInstancesResult.getDBInstances().get(0).getEndpoint().getAddress();
    }

//    public Long addUserDetails(UserDTO userDTO){
//        UserEntity userEntity = new UserEntity();
//        userEntity.setUserId(userDTO.getUserId());
//        userEntity.setEmail(userDTO.getEmail());
//        userEntity.setFirstName(userDTO.getFirstName());
//        userEntity.setLastName(userDTO.getLastName());
//        return userRepository.save(userEntity).getUserId();
//    }
//
//    public UserDTO getUser(Long userId){
//        UserEntity userEntity = userRepository.findById(userId).orElse(null);
//        if(userEntity == null)
//            return null;
//        UserDTO userDto = new UserDTO();
//        userDto.setUserId(userEntity.getUserId());
//        userDto.setEmail(userEntity.getEmail());
//        userDto.setFirstName(userEntity.getFirstName());
//        userDto.setLastName(userEntity.getLastName());
//        return userDto;
//    }

    public void deleteMySQlInstance(){
        DBInstance deletedInstance = amazonRDS.deleteDBInstance(new DeleteDBInstanceRequest()
                .withSkipFinalSnapshot(true)
                .withDBInstanceIdentifier(rdsIdentifier));
        log.info("deleting... db instance {}", deletedInstance.getDBInstanceIdentifier());
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(rdsIdentifier);
        WaiterParameters<DescribeDBInstancesRequest> waiterParameters = new WaiterParameters()
                .withRequest(describeDBInstancesRequest);
        amazonRDS.waiters().dBInstanceDeleted().run(waiterParameters);
        log.info("Deleted db instance");
    }

    public String createDynamoTable(){
        String tableName = "Users";
        CreateTableResult createTableResult = dynamoDB.createTable(new CreateTableRequest()
                .withTableName(tableName)
                .withTags(new com.amazonaws.services.dynamodbv2.model.Tag().withKey("purpose").withValue("poc"))
                .withKeySchema(Arrays.asList(new KeySchemaElement().withKeyType(KeyType.HASH)
                        .withAttributeName("user_id"),
                        new KeySchemaElement().withKeyType(KeyType.RANGE).withAttributeName("user_email")))
        .withAttributeDefinitions(new AttributeDefinition().withAttributeName("user_id")
                .withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName("user_email")
                        .withAttributeType(ScalarAttributeType.S))
        .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(5l).withWriteCapacityUnits(5l)));
        log.info("Dynamo db is being created with id {}", createTableResult.getTableDescription().getTableId());
        WaiterParameters<DescribeTableRequest> tableRequestWaiterParameters = new WaiterParameters<DescribeTableRequest>()
                .withRequest(new DescribeTableRequest().withTableName(tableName));
        dynamoDB.waiters().tableExists().run(tableRequestWaiterParameters);
        log.info("dynamo db table {} is created with id: {}", tableName, createTableResult.getTableDescription().getTableId());
        return createTableResult.getTableDescription().getTableId();
    }

    public void addDynamoDBData(UserDTO userDTO){
        Map<String, AttributeValue> itemMap = prepareMap(userDTO);
        String tableName = "Users";
        BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest()
                .withRequestItems(Collections.singletonMap(tableName,
                        Arrays.asList(new WriteRequest().withPutRequest(new PutRequest().withItem(itemMap)))));
        BatchWriteItemResult batchWriteItemResult = dynamoDB.batchWriteItem(batchWriteItemRequest);
        log.info("Inserted item {}", batchWriteItemResult.getItemCollectionMetrics());
    }

    public UserDTO getDynamoData(String userId){
        String tableName = "Users";
        BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest().withRequestItems(Collections.singletonMap(tableName,
                new KeysAndAttributes().withKeys(
                Collections.singletonMap("user_id", new AttributeValue().withS("my-user2")),
                Collections.singletonMap("user_email", new AttributeValue().withS("asasa@fdfd.ccd"))
        )));
        BatchGetItemResult getItemResult = dynamoDB.batchGetItem(batchGetItemRequest);
        Map<String, AttributeValue> response = getItemResult.getResponses().get(tableName).get(0);
        return prepareDTO(response);
    }

    public List<UserDTO> scanDynamoTable(){
        String tableName = "Users";
        ScanResult scanResult = dynamoDB.scan(new ScanRequest().withTableName(tableName));
        log.info("Fetched {} results", scanResult.getCount());
        int itemCount = 0;
        List<UserDTO> res = new ArrayList<>();
        for(Map<String, AttributeValue> item: scanResult.getItems()){
            log.info("Item no {}", itemCount++);
            res.add(prepareDTO(item));
            for(Map.Entry<String, AttributeValue> attr : item.entrySet()){
                log.info("{}:{}", attr.getKey(), attr.getValue());
            }
        }
        return res;
    }

    private UserDTO prepareDTO(Map<String, AttributeValue> response) {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(response.get("user_email").getS());
        userDTO.setFirstName(response.get("user_fname").getS());
        userDTO.setLastName(response.get("user_lname").getS());
        return userDTO;
    }

    private Map<String, AttributeValue> prepareMap(UserDTO userDTO) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("user_id", new AttributeValue().withS(userDTO.getFirstName()+"1"));
        item.put("user_email", new AttributeValue().withS(userDTO.getEmail()));
        item.put("user_fname", new AttributeValue().withS(userDTO.getFirstName()));
        item.put("user_lname", new AttributeValue().withS(userDTO.getLastName()));
        return item;
    }

    public String deleteDynamoTable(){
        String tableName = "Users";
        DeleteTableResult deleteTableResult = dynamoDB.deleteTable(tableName);
        log.info("Deleted table {}", deleteTableResult.getTableDescription().getTableId());
        return deleteTableResult.getTableDescription().getTableId();
    }
}

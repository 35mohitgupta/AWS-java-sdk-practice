package com.example.aws.controller;

import com.example.aws.dtos.UserDTO;
import com.example.aws.service.DBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/db")
@RestController
@Slf4j
public class DBController {

    @Autowired
    private DBService dbService;

    @PostMapping("/mysql")
    public String createMySqlDB(){
        return dbService.createMySQL();
    }

    @GetMapping("/endpoint/mysql")
    public String getEndPoint(){
        return dbService.getRDSEndpoint();
    }

//    @PutMapping("/user")
//    public String addUser(@RequestBody UserDTO userDTO){
//        return String.format("User created with id: %d", dbService.addUserDetails(userDTO));
//    }
//
//    @GetMapping("/user/{userId}")
//    public UserDTO getUser(@PathVariable Long userId){
//        return dbService.getUser(userId);
//    }

    @DeleteMapping("/mysql")
    public void deleteMySQl(){
        dbService.deleteMySQlInstance();
    }

    @PostMapping("/dynamo")
    private String createDynamoDB(){
        return String.format("Created dynamo table with id: %s",dbService.createDynamoTable());
    }

    @PutMapping("/dynamo")
    private String addDynamoDate(@RequestBody UserDTO userDTO){
        dbService.addDynamoDBData(userDTO);
        return "Data inserted";
    }

    @GetMapping("/dynamo/{userId}")
    public UserDTO queryDynamoData(@PathVariable String userId){
        return dbService.getDynamoData(userId);
    }

    @GetMapping("/dynamo-scan")
    public List<UserDTO> scanDynamoData(){
        return dbService.scanDynamoTable();
    }

    @DeleteMapping("/dynamo")
    public String deleteDynamoTable(){
        return dbService.deleteDynamoTable();
    }
}

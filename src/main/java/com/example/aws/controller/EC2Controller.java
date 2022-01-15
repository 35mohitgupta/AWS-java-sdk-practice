package com.example.aws.controller;

import com.example.aws.service.EC2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ec2")
public class EC2Controller {

    @Autowired
    private EC2Service ec2Service;

    @PostMapping("/sg/{sgName}")
    public Boolean createPublicSG(@PathVariable String sgName){
        return  ec2Service.createSecurityGroup(sgName);
    }

    @DeleteMapping("/sg/{sgName}")
    public Boolean deletePublicSG(@PathVariable String sgName){
        return ec2Service.deleteSecurityGroup(sgName);
    }
}

package com.example.aws.service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EC2Service {

    @Autowired
    private AmazonEC2 amazonEC2;

    public Boolean createSecurityGroup(String sgName){
        String description = "demo api description";
        String cidrIp = "0.0.0.0/0";
        CreateSecurityGroupRequest securityGroupRequest = new CreateSecurityGroupRequest();
        securityGroupRequest.setGroupName(sgName);
        securityGroupRequest.setDescription(description);
        CreateSecurityGroupResult securityGroupResult = amazonEC2.createSecurityGroup(securityGroupRequest);
        log.info("Created SG {}", securityGroupResult.getGroupId());
        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
        authorizeSecurityGroupIngressRequest.setGroupName(sgName);
        authorizeSecurityGroupIngressRequest.setGroupId(securityGroupResult.getGroupId());
        authorizeSecurityGroupIngressRequest.setCidrIp(cidrIp);
        authorizeSecurityGroupIngressRequest.setFromPort(3306);
        authorizeSecurityGroupIngressRequest.setToPort(3306);
        authorizeSecurityGroupIngressRequest.setIpProtocol("tcp");
        AuthorizeSecurityGroupIngressResult securityGroupIngressResult = amazonEC2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
        log.info("authorised");
        return securityGroupIngressResult.getReturn();
    }

    public Boolean deleteSecurityGroup(String sgName){
        DeleteSecurityGroupResult result = amazonEC2.deleteSecurityGroup(new DeleteSecurityGroupRequest().withGroupName(sgName));
        log.info("delete security group {}", sgName);
        return  true;
    }

}

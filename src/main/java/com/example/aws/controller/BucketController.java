package com.example.aws.controller;

import com.example.aws.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@CrossOrigin
//@RestController()
@Controller
@RequestMapping("/api/bucket")
public class BucketController {

    @Autowired
    private S3Service s3Service;

    @PostMapping("/{bucketName}")
    public void createS3Bucket(@PathVariable String bucketName){
        s3Service.createS3Bucket(bucketName);
    }

    @GetMapping("/")
    public ResponseEntity<List<String>> getAllBuckets(){
        List<String> res = s3Service.listAllBuckets();
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @DeleteMapping("/{bucketName}")
    public void deleteBucket(@PathVariable String bucketName){
        s3Service.deleteBucket(bucketName);
    }

}

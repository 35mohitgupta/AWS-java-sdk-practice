package com.example.aws.controller;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.aws.dtos.MyS3Object;
import com.example.aws.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController()
@RequestMapping("/api/object")
public class ObjectController {

    @Autowired
    private S3Service s3Service;

    @GetMapping(value = "/{bucket}")
    public List<S3ObjectSummary> getAllObjects(@PathVariable("bucket") String bucketName){
        return s3Service.getObjectSummariesInBucket(bucketName);
    }

    @PostMapping("/upload")
    public String uploadObject(@RequestParam("object") MultipartFile file, @RequestParam("bucketName") String bucketName){
        MyS3Object s3Object = new MyS3Object();
        s3Object.setBucketName(bucketName);
        return s3Service.uploadS3Object(file,s3Object);
    }

    @PutMapping("/presigned")
    public String getPresignedUrl(@RequestBody MyS3Object s3Object){
        return s3Service.preSignedUrl(s3Object.getBucketName(), s3Object.getKey());
    }

    @PutMapping("/upload-encrypted")
    public String uploadEncryptedObject(@RequestBody MyS3Object s3Object) throws IOException {
        return s3Service.encryptUpload(s3Object);
    }
}

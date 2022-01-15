package com.example.aws.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionV2;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.example.aws.dtos.MyS3Object;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class S3Service {
    
    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private AmazonS3EncryptionV2 amazonS3EncryptionClient;

    public void createS3Bucket(String bucketName){
        if(!(s3Client.doesBucketExistV2(bucketName))){
            // Note that CreateBucketRequest does not specify region. So bucket is
            // created in the region specified in the client.
            s3Client.createBucket(new CreateBucketRequest(bucketName));
            log.info("bucket {} created", bucketName);
        }

// Get location.
        String bucketLocation = s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName));
        log.info("bucket {} created in location {}", bucketName, bucketLocation);
    }

    public List<String> listAllBuckets(){
        return s3Client.listBuckets().stream().map(Bucket::getName).collect(Collectors.toList());
    }

    public List<S3ObjectSummary> getObjectSummariesInBucket(String bucketName){
        if(s3Client.doesBucketExistV2(bucketName)){
            return s3Client.listObjects(bucketName).getObjectSummaries();
        }else{
            throw new RuntimeException("Bucket doesn't exists");
        }
    }

    public void deleteObject(String bucketName, String key){
        s3Client.deleteObject(new DeleteObjectRequest(bucketName,key));
    }

    public void deleteBucket(String bucketName){

        try {
            // Delete all objects from the bucket. This is sufficient

            // for unversioned buckets. For versioned buckets, when you attempt to delete objects, Amazon S3 inserts

            // delete markers for all objects, but doesn't delete the object versions.

            // To delete objects from versioned buckets, delete all of the object versions before deleting

            // the bucket (see below for an example).

            ObjectListing objectListing = s3Client.listObjects(bucketName);
            while (true) {
                Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
                while (objIter.hasNext()) {
                    s3Client.deleteObject(bucketName, objIter.next().getKey());
                }

                // If the bucket contains many objects, the listObjects() call

                // might not return all of the objects in the first listing. Check to

                // see whether the listing was truncated. If so, retrieve the next page of objects

                // and delete them.

                if (objectListing.isTruncated()) {
                    objectListing = s3Client.listNextBatchOfObjects (objectListing);
                } else {
                    break;
                }
            }

            // Delete all object versions (required for versioned buckets).

            VersionListing versionList = s3Client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
            while (true) {
                Iterator<S3VersionSummary> versionIter = versionList.getVersionSummaries().iterator();
                while (versionIter.hasNext()) {
                    S3VersionSummary vs = versionIter.next();
                    s3Client.deleteVersion(bucketName, vs.getKey(), vs.getVersionId());
                }

                if (versionList.isTruncated()) {
                    versionList = s3Client.listNextBatchOfVersions (versionList);
                } else {
                    break;
                }
            }

            // After all objects and object versions are deleted, delete the bucket.

            s3Client.deleteBucket(bucketName);
        }
        catch(AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process

            // it, so it returned an error response.

            e.printStackTrace();
        }
        catch(SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client couldn't

            // parse the response from Amazon S3.

            e.printStackTrace();
        }
    }

    public String uploadS3Object(MultipartFile multipartFile, MyS3Object s3Object){
        ObjectMetadata objectMetadata = new ObjectMetadata();
        if(!CollectionUtils.isEmpty(s3Object.getObjectTags()))
            s3Object.getObjectTags().forEach(tag -> objectMetadata.getUserMetadata().put(tag.getKey(), tag.getValue()));
        try {
            PutObjectResult putObjectResult = s3Client.putObject(s3Object.getBucketName(), multipartFile.getOriginalFilename(),
                    multipartFile.getInputStream(),objectMetadata);
            return putObjectResult.getVersionId();
        }catch (SdkClientException  e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String preSignedUrl(String bucketName, String objectKey){
        java.util.Date expiration = new java.util.Date();
        long msec = expiration.getTime();
        msec += 1000 * 60 * 60; // Add 1 hour.
        expiration.setTime(msec);

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new  GeneratePresignedUrlRequest(bucketName, objectKey);
        generatePresignedUrlRequest.setMethod(HttpMethod.GET);
        generatePresignedUrlRequest.setExpiration(expiration);

        URL presignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return presignedUrl.toString();
    }

    public String encryptUpload(MyS3Object myS3Object) throws IOException {
        // Upload object using the encryption client.

        byte[] plaintext = myS3Object.getContent().getBytes();
        log.info("plaintext's length: {}", plaintext.length);
        amazonS3EncryptionClient.putObject(new PutObjectRequest(myS3Object.getBucketName(), myS3Object.getKey(),
                new ByteArrayInputStream(plaintext), new ObjectMetadata()));

        // Download the object.

        S3Object downloadedObject = amazonS3EncryptionClient.getObject(myS3Object.getBucketName(),
                myS3Object.getKey());
        byte[] decrypted = IOUtils.toByteArray(downloadedObject.getObjectContent());

        // Verify same data.
        log.info("Asserting...");
        Assert.isTrue(Arrays.equals(plaintext, decrypted), "Uploaded and downloaded object didn't matched");
        log.info("Assertion successfully.");
        return  new String(decrypted);
    }
}

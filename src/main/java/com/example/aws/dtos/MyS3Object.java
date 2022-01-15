package com.example.aws.dtos;

import lombok.Data;

import java.util.List;

@Data
public class MyS3Object {


    private String key;
    private String bucketName;

    private List<Tag> objectTags;

    private String content;

}

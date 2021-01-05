//package com.es.entity;
//
//import lombok.Data;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.elasticsearch.annotations.DateFormat;
//import org.springframework.data.elasticsearch.annotations.Document;
//import org.springframework.data.elasticsearch.annotations.Field;
//import org.springframework.data.elasticsearch.annotations.FieldType;
//
//import java.util.Date;
//
//@Data
//@Document(indexName = "wlp-index", type = "doc", useServerConfiguration = true, createIndex = false)
//public class EsOrg {
//    @Id
//    private Integer id;
//    @Field(type = FieldType.Text, analyzer = "ik_max_word")
//    private String orgNo;
//    @Field(type = FieldType.Text, analyzer = "ik_max_word")
//    private String orgName;
//    @Field(type = FieldType.Text, analyzer = "ik_max_word")
//    private String pOrgNo;
//    @Field(type = FieldType.Text, analyzer = "ik_max_word")
//    private String orgType;
//    @Field(type = FieldType.Date, format = DateFormat.custom,
//            pattern = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
//    private Date updateTime;
//    @Field(type = FieldType.Text, analyzer = "ik_max_word")
//    private String sortNo;
//    @Field(type = FieldType.Text, analyzer = "ik_max_word")
//    private String orgShorthand;
//}

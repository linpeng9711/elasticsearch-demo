#### 使用Docker安装ElasticSearch

1. 获取适用于Docker的Elasticsearch就像`docker pull`对Elastic Docker注册表发出命令一样简单。

   ```bash
   docker pull docker.elastic.co/elasticsearch/elasticsearch:7.10.1
   ```

2. 使用Docker启动单节点集群

   ```bash
   docker run -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.10.1
   ```

   如果想启动多节点集群,参考官方文档:https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html

   如果docker拉取太慢,可以尝试下阿里云镜像仓库:https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors

#### 使用Docker安装Kibana

​	Kibana是一款优秀的可视化分析插件,通过内置的devTool等模块,有助于您对ES进行调试与监控.

1. 为Docker获取Kibana就像`docker pull`对Elastic Docker注册表发出命令一样简单。

   ```bash
   docker pull docker.elastic.co/kibana/kibana:7.10.1
   ```

2. 运行Kibana并连接本地ElasticSearch

   ```
   docker run -d --link YOUR_ELASTICSEARCH_CONTAINER_NAME_OR_ID:elasticsearch -p 5601:5601 docker.elastic.co/kibana/kibana:7.10.1
   ```
   
   通过`docker ps`指令查看elasticsearch的containerid **替换**上边指令中的`YOUR_ELASTICSEARCH_CONTAINER_NAME_OR_ID`
   
   官方文档:https://www.elastic.co/guide/en/kibana/current/docker.html#docker
#### 已安装的服务

   * **ElasticSearch服务:** http://192.168.3.56:9200/
   
   * **Kibana服务:** http://192.168.3.56:5601/

#### ElasticSearch基础与原理

​	可以看下这篇博客: https://www.cnblogs.com/dreamroute/p/8484457.html ,写的很好.

#### ElasticSearch DSL语句使用记录

以用户文档(document)增删改查为例

1. **新增用户文档**

   ```json
   POST /wlp-index/msg
   {
     "uid":1,
     "phone":17645088777,
     "message":"登录验证码",
     "msgcode":7651,
     "sendtime":"2020-12-22 16:13:45"
   }
   ```
   
   执行上面的语句,通过创建一个文档而自动创建了一个索引(index),这种方法是可行的,但在实际应用中并不推荐这么做,因为它会使用ES的缺省配置.
   
> 没有创建索引库而通过ES自身生成的这种并不友好，因为它会使用默认的配置，字段结构都是text(text的数据会分词，在存储的时候也会额外的占用空间)，分片和索引副本采用默认值，默认是5和1，ES的分片数在创建之后就不能修改，除非reindex，所以这里我们还是指定数据模板进行创建。

下面介绍如何使用DSL创建索引

2. **创建索引**

   ```json
   PUT wlp-index
   {
     "settings": {
       "number_of_shards": 10,
       "number_of_replicas": 1,
       "refresh_interval": "1s"
     },
     "mappings": {
       "properties": {
         "uid": {
           "type": "long"
         },
         "phone": {
           "type": "long"
         },
         "message": {
           "type": "keyword"
         },
         "msgcode": {
           "type": "long"
         },
         "sendtime": {
           "type": "date",
           "format": "yyyy-MM-dd HH:mm:ss"
         }
       }
     }
   }
   ```

   根据教程创建索引时遇到了 `The mapping definition cannot be nested under a type [_doc] unless include_type_name is set to true` 问题.

   > 最初，我们谈到“索引”类似于SQL数据库中的“数据库”，而“类型”等同于“表”。
   >
   > 这是一个不好的类比，导致了错误的假设。在SQL数据库中，表彼此独立。一个表中的列与另一表中具有相同名称的列无关。映射类型的字段不是这种情况。
   >
   > 在Elasticsearch索引中，在不同映射类型中具有相同名称的字段在内部由相同的Lucene字段支持。换句话说，使用上面的示例，类型中的`user_name`字段`user`存储在与类型中的字段完全相同的`user_name`字段中`tweet`，并且两个 `user_name`字段在两种类型中必须具有相同的映射（定义）。
   >
   > 例如，当您想`deleted`成为 同一索引`date`中的一种类型的`boolean`字段而另一种类型的字段时，这可能会导致挫败感。
   >
   > 最重要的是，存储在同一索引中具有很少或没有共同字段的不同实体会导致数据稀疏并干扰Lucene有效压缩文档的能力。
   >
   > 由于这些原因，我们决定从Elasticsearch中删除映射类型的概念。
   >
   > https://www.elastic.co/guide/en/elasticsearch/reference/current/removal-of-types.html

3. **查询所有索引**

   ```
   GET /_cat/indices
   ```

4. **删除指定索引**

   ```
   DELETE /test-index002
   ```

5. **查询索引下所有文档**

   ```
   GET /wlp-index/_search
   ```

6. **查询指定文档信息**

   ```
   GET /wlp-index/msg/SCuEiXYBRmSkZK_bvp9S
   ```

   格式: 索引/类型/文档id

7. **修改执行文档**

   ```
   POST wlp-index/msg/SCuEiXYBRmSkZK_bvp9S
   {
     "uid":1,
     "phone":17645088765,
     "message":"测试修改",
     "msgcode":1234,
     "sendtime":"2020-12-22 16:13:45"
   }
   ```

8. **删除执行文档**

   ```
   DELETE /wlp-index/msg/SiuQiXYBRmSkZK_bNZ8M
   ```

   **同时也可以通过条件删除符合的文档**

   ```
   POST /wlp-index/msg/_delete_by_query
   {
     "query": {
       "term": {
         "uid": "3"
       }
     }
   }
   ```

   **当然还可以通过条件,删除对应文档某一个字段的数据**

   ```
   POST /wlp-index/msg/_update_by_query
   {
     "query":{
       "term":{
         "uid":5
       }
     },
     "script":{
       "lang":"painless",
       "inline":"ctx._source.remove(\"phone\")"
     }
   }
   ```

   上面的语句清除了uid为5的文档`phone`字段

9. **查询集群所有的索引库信息(包括隐藏索引库信息)**

   ```
   GET _search
   {
     "query": {
       "match_all": {}
     }
   }
   ```

10. **等值查询(term)**

    > term主要用于精确匹配哪些值，比如数字，日期，布尔值或 not_analyzed 的字符串(未经分析的文本数据类型)

    ```
    GET wlp-index/msg/_search
    {
      "query": {
        "term": {
          "phone": 17645088765
        }
      }
    }
    ```

    当然也可以**批量查询**,相当于SQL中的`in`

    ```
    GET wlp-index/msg/_search
    {
      "query": {
        "terms": {
          "phone": [
            17645088777,
            176450881233
          ]
        }
      }
    }
    ```

    注意批量查用的是**<font color=red>terms</font>**哦

11. **范围查询(range)**

    > range可以理解为SQL中的><符号，其中gt是大于，lt是小于，gte是大于等于，lte是小于等于。

    比如要查所有`uid`在100至200之间的(包含100与200)

    ```
    GET /wlp-index/msg/_search
    {
      "query": {
        "range": {
          "uid": {
            "gte": 100,
            "lte": 200
          }
        }
      }
    }
    ```

12. **存在查询(exists)**

    > exists可以理解为SQL中的exists函数，就是判断是否存在该字段。

    ```
    GET /wlp-index/msg/_search
    {
      "query": {
        "exists": {
          "field": "who"
        }
      }
    }
    ```

13. **组合查询(bool)**

    bool可以合并多个过滤条件的查询结果布尔逻辑,包含以下一个操作符

    * must:多个查询条件完全匹配,相当于and
    * must_not:多个查询条件的相反匹配,相当于not
    * should:至少有一个查询条件匹配,相当于or

    ```
    GET /wlp-index/msg/_search
    {
      "query": {
        "bool": {
          "must": {
            "term": {
              "phone": 176450881233
            }
          },
          "must_not": {
            "range": {
              "uid": {
                "lt": 100
              }
            }
          },
          "should": [
            {
              "term": {
                "uid": 150
              }
            },
            {
              "term": {
                "uid": 200
              }
            }
          ]
        }
      }
    }
    ```

    上面语句查询`phone`等于`176450881233`,并且`uid`**不小于**100的文档

14. **模糊查询**

    ```json
    GET /_search
    {
      "query": {
        "match": {
          "org_name": {
            "query": "供电所"
          }
        }
      }
    }
    ```

15. 待补充

#### MySQL与ElasticSearch数据同步

1. 前往[logstash官网](https://www.elastic.co/cn/downloads/logstash)下载logstash的windows版本

2. 将`mysql-connector-java-xx.jar` 放到logstash目录下.

3. conf文件夹下新建`mysql.conf`文件

   ```json
   input {
     jdbc {
       #引用mysql jdbc连接jar包目录
       jdbc_driver_library => "E:\\elasticsearch\\logstash-7.10.1\\mysql-connector-java-8.0.22.jar"
       jdbc_driver_class => "com.mysql.jdbc.Driver"
       jdbc_connection_string => "jdbc:mysql://127.0.0.1:3306/test?serverTimezone=UTC"
       jdbc_user => "root"
       jdbc_password => "root"
       schedule => "* * * * *"
   	#清空上次的sql_last_value记录
   	clean_run => true
       statement => "SELECT * FROM T_ORG WHERE update_time >= :sql_last_value AND update_time < NOW() ORDER BY update_time desc"
     }
   }
   
   output {
       elasticsearch {
           # ES的IP地址及端口
           hosts => ["192.168.3.56:9200"]
           # 索引名称 可自定义
           index => "wlp-index"
           # 需要关联的数据库中有有一个id字段，对应类型中的id
           document_id => "%{id}"
       }
   }
   ```

4. 启动logstash

   ```
   logstash -f ../config/mysql.conf
   ```

   logstash默认会每1分钟轮询一次,进行同步.

   

----



#### Java集成ElatsicSearch

	##### 使用ElasticSearch Rest Client方式集成

​	官方的RestClient, 封装了ES操作, 上手简单

​	分为两个版本:

 * **Java Low Level REST Client**

   低级别es客户端,使用 http 协议与 Elastiicsearch 集群通信,与所有 es 版本兼容

 * **Java High level REST Client**

   高级别es客户端,基于低级别,它会暴露 API 特定的方法

   下面介绍使用`High`版本操作ES

   1. 引入`elasticsearch.client` 依赖

      ```xml
      <dependency>
          <groupId>org.elasticsearch.client</groupId>
          <artifactId>elasticsearch-rest-high-level-client</artifactId>
          <version>7.10.1</version>
      </dependency>
      ```

      **注意版本号要与服务保持一致**

   2. 编写配置类,构件一个client与统一请求项

      ```java
      /**
       * ES配置类,给容器中注入一个RestHighLevelClient
       */
      @Configuration
      public class ElasticSearchConfig {
          @Value("${elasticsearch.hostname}")
          private String hostName;
          @Value("${elasticsearch.port}")
          private int port;
      
          public static final RequestOptions COMMON_OPTIONS;
      
          /**
           * 统一设置请求项
           */
          static {
              RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
              COMMON_OPTIONS = builder.build();
          }
      
          /**
           * 获取ES客户端
           */
          @Bean
          public RestHighLevelClient getClient() {
      
              return new RestHighLevelClient(
                      RestClient.builder(new HttpHost(hostName, port, "http"))
              );
          }
      }
      
      ```

   3. 测试类写一个CRUD

      ```java
      @RunWith(SpringRunner.class)
      @SpringBootTest
      public class ElasticsearchDemoApplicationTests {
      
          @Resource
          private ElasticSearchConfig elasticSearchConfig;
          @Resource
          private RestHighLevelClient client;
      
          /**
           * 测试ES配置类是否被spring容器管理
           */
          @Test
          public void contextLoads() {
              System.out.println(elasticSearchConfig);
          }
      
          /**
           * 新增一个文档
           */
          @Test
          public void indexData() throws IOException {
              IndexRequest indexRequest = new IndexRequest("wlp-index");//参数为索引名称
              indexRequest.id("101");
              User user = new User().setFirst_name("张").setLast_name("三").setAge(30).setAbout("普通人").setInterests(new String[]{"游泳", "学习"});
              String jsonString = JSON.toJSONString(user);
              indexRequest.source(jsonString, XContentType.JSON);//一定要指定数据类型
              /**
               * ElasticSearchConfig.COMMON_OPTIONS:从配置类中获取请求项,默认为RequestOptions.DEFAULT
               */
              //执行操作
              IndexResponse index = client.index(indexRequest, ElasticSearchConfig.COMMON_OPTIONS);
              //提取响应数据
              System.out.println(index);
          }
      
          /**
           * 批量插入文档
           */
          @Test
          public void testBulkIndexDocument() throws IOException {
              BulkRequest bulkRequest = new BulkRequest();
              List<User> documents = new ArrayList<>();
              documents.add(new User().setFirst_name("张").setLast_name("三").setAge(30).setAbout("普通人001").setInterests(new String[]{"游泳", "学习"}));
              documents.add(new User().setFirst_name("李").setLast_name("四").setAge(30).setAbout("普通人002").setInterests(new String[]{"吃饭", "睡觉"}));
              documents.add(new User().setFirst_name("王").setLast_name("五").setAge(30).setAbout("普通人003").setInterests(new String[]{"读书", "唱歌"}));
              documents.forEach(doc -> {
                  //可以通过IndexRequest().id()指定主键,不指定则自增
                  bulkRequest.add(new IndexRequest("wlp-index").source(JSON.toJSONString(doc), XContentType.JSON));
              });
              BulkResponse bulk = client.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);
              //bulk.hasFailures():是否失败
              System.out.println(bulk.hasFailures());
          }
      
          /**
           * 根据id查询一个文档
           */
          @Test
          public void getDocumentById() throws IOException {
              GetRequest getRequest = new GetRequest("wlp-index", "100");
              GetResponse documentFields = client.get(getRequest, ElasticSearchConfig.COMMON_OPTIONS);
              System.out.println(documentFields);
          }
      
          /**
           * 条件查询(精确,不支持模糊)
           */
          @Test
          public void searchData() throws IOException {
              SearchRequest searchRequest = new SearchRequest("wlp-index");
              //searchRequest.indices("wlp-index");//同时也可以这样指定查询的索引
              /**
               * 条件查询 如果查询条件为中文 需要在属性添加 .keyword,不加就查不到
               * 例如： QueryBuilders.termQuery("name.keyword","张三");
               * termQuery:精确查询 matchQuery:模糊查询
               */
              TermQueryBuilder queryBuilder = QueryBuilders.termQuery("about.keyword", "普通人001");
              //构件搜索条件
              SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
              //分页 size:每页几条(默认10) from从第几条开始查
              searchSourceBuilder.size(20);
              searchSourceBuilder.from(0);
              searchSourceBuilder.query(queryBuilder);
              //设置查询超时时间
              searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
              searchRequest.source(searchSourceBuilder);
              SearchResponse searchResponse = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
              //遍历输出
              SearchHit[] hits = searchResponse.getHits().getHits();
              Arrays.stream(hits).forEach(documentFields -> {
                  System.out.println(documentFields.getSourceAsMap());
              });
          }
      
          /**
           * 模糊查询
           */
          @Test
          public void testFuzzyQuery() throws IOException {
              SearchRequest searchRequest = new SearchRequest("wlp-index");
              SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
              searchSourceBuilder.query(QueryBuilders.matchQuery("about", "人之初"));
              //超时时间
              searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
              searchRequest.source(searchSourceBuilder);
              SearchResponse response = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
              SearchHit[] hits = response.getHits().getHits();
              Arrays.stream(hits).forEach(documentFields -> {
                  System.out.println(documentFields.getSourceAsMap());
              });
          }
      
          /**
           * 测试判断文档是否存在
           */
          @Test
          public void testIsDocumentExists() throws IOException {
              GetRequest request = new GetRequest("wlp-index", "101");
              //不获取source上下文
              request.fetchSourceContext(new FetchSourceContext(false));
              boolean exists = client.exists(request, RequestOptions.DEFAULT);
              System.out.println(exists);
          }
      
          /**
           * 根据主键更改文档信息
           */
          @Test
          public void updateDocumentById() throws IOException {
              UpdateRequest request = new UpdateRequest("wlp-index", "101");
              //没有设置的属性,es不会更新
              User user = new User().setFirst_name("TEST").setLast_name("UPDATE");
              request.doc(JSON.toJSONString(user), XContentType.JSON);
              UpdateResponse update = client.update(request, ElasticSearchConfig.COMMON_OPTIONS);
              System.out.println(update);
          }
      
      
          /**
           * 根据id删除一个文档
           */
          @Test
          public void deleteDocumentById() throws IOException {
              DeleteRequest deleteRequest = new DeleteRequest("wlp-index", "100");
              DeleteResponse delete = client.delete(deleteRequest, ElasticSearchConfig.COMMON_OPTIONS);
              System.out.println(delete);
          }
      
          //高亮搜索
          @Test
          public void  testHighlightSearch() throws IOException {
              //搜索条件
              SearchRequest searchRequest = new SearchRequest("wlp-index");
              SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
              searchSourceBuilder .query(QueryBuilders.matchQuery("about.keyword","普通")) //精确匹配
                      .timeout(new TimeValue(60, TimeUnit.SECONDS))
                      .highlighter(new HighlightBuilder()
                              .field("about.keyword") //搜索字段
                              .requireFieldMatch(false) // 只需一个高亮
                              .preTags("<span style='coler:red'>")
                              .postTags("</span>"));
              //执行搜索
              searchRequest.source(searchSourceBuilder);
              SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
              //解析结果
              List<Object> queryList = new LinkedList<>();
              for (SearchHit documentFields : search.getHits().getHits()) {
                  Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();//原来的结果
                  HighlightField field = documentFields.getHighlightFields().get("name.keyword");
                  //解析高亮的字段，将原来的字段替换成高亮字段
                  Text[] texts = field.fragments();
                  String n_text ="";
                  for (Text text : texts) {
                      n_text+= text;
                  }
                  sourceAsMap.put("name",n_text);//高亮字段替换原来的内容
                  queryList.add(sourceAsMap);
      
              }
              queryList.forEach(System.out::println);
          }
      }
      
      ```

      


##### 使用 Spring Data ElasticSearch

存在版本兼容问题, 官方不推荐使用此方式, 将在ES8版本移除
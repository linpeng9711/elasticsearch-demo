package com.es;

import com.alibaba.fastjson.JSON;
import com.es.config.ElasticSearchConfig;
import com.es.entity.Account;
import com.es.entity.User;
import com.es.utils.EsClientUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticsearchDemoApplicationTests {

    @Resource
    private ElasticSearchConfig elasticSearchConfig;
    @Resource
    private RestHighLevelClient client;
//    @Resource
//    private EsOrgRepository esOrgRepository;

    private final static String INDEX_NAME = "account_index";
    @Resource
    private EsClientUtil esClientUtil;

    /**
     * 测试ES配置类是否被spring容器管理
     */
    @Test
    public void contextLoads() {
        System.out.println(elasticSearchConfig);
    }

    /**
     * 新增一个文档
     * IndexRequest request = new IndexRequest(
     * "posts",  // 索引 Index
     * "doc",  // Type
     * "1");  // 文档 Document Id
     * String jsonString = "{" +
     * "\"user\":\"kimchy\"," +
     * "\"postDate\":\"2013-01-30\"," +
     * "\"message\":\"trying out Elasticsearch\"" +
     * "}";
     * request.source(jsonString, XContentType.JSON); // 文档源格式为 json string
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("account_index");//参数为索引名称
        //indexRequest.id("101");
        Account account = new Account()
                .setFirstname("李")
                .setLastname("四")
                .setAge(21)
                .setAccountNumber(65414989)
                .setAddress("金鸡山公园");
        //User user = new User().setFirst_name("张").setLast_name("三").setAge(30).setAbout("普通人").setInterests(new String[]{"游泳", "学习"});
        String jsonString = JSON.toJSONString(account);
        indexRequest.source(jsonString, XContentType.JSON);//一定要指定数据类型
        /**
         * ElasticSearchConfig.COMMON_OPTIONS:从配置类中获取请求项,默认为RequestOptions.DEFAULT
         */
        //执行操作
        //同步索引(同步 API 会导致阻塞，一直等待数据返回)
        IndexResponse index = client.index(indexRequest, ElasticSearchConfig.COMMON_OPTIONS);
        /**
         * 异步索引
         * 异步 API 在命名上会加上 async 后缀，需要有一个 listener 作为参数，等这个请求返回结果或者发生错误时，这个 listener 就会被调用
         * 请求成功后,调用监听器的成功/失败方法
         * 异步方法执行后会立刻返回，在索引操作执行完成后，ActionListener 就会被回调:
         * 执行成功，调用 onResponse 函数
         * 执行失败，调用 onFailure 函数
         */
        //client.indexAsync(indexRequest, listener);
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
        GetRequest getRequest = new GetRequest("account_index", "HlUYy3YBqiYBG7sfSZ9H");
        GetResponse documentFields = client.get(getRequest, ElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(documentFields);
    }

    /**
     * 条件查询(精确,不支持模糊)
     */
    @Test
    public void searchData() throws IOException {
        SearchRequest searchRequest = new SearchRequest("account_index");
        //searchRequest.indices("wlp-index");//同时也可以这样指定查询的索引
        /**
         * 条件查询 如果查询条件为中文 需要在属性添加 .keyword,不加就查不到
         * 例如： QueryBuilders.termQuery("name.keyword","张三");
         * termQuery:精确查询 matchQuery:模糊查询
         */
        TermQueryBuilder queryBuilder = QueryBuilders.termQuery("address.keyword", "880 Holmes Lane");
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
        SearchRequest searchRequest = new SearchRequest("account_index");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "Holmes Lane"));
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
        GetRequest request = new GetRequest("account_index", "1001");
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
    public void testHighlightSearch() throws IOException {
        //搜索条件
        SearchRequest searchRequest = new SearchRequest("wlp-index");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("about.keyword", "普通")) //精确匹配
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
            StringBuilder n_text = new StringBuilder();
            for (Text text : texts) {
                n_text.append(text);
            }
            sourceAsMap.put("name", n_text.toString());//高亮字段替换原来的内容
            queryList.add(sourceAsMap);
        }
        queryList.forEach(System.out::println);
    }

    @Test
    public void test() {
        //新增文档
        /*Account account = new Account()
                .setFirstName("张 小")
                .setLastName("五")
                .setAge(23)
                .setAccountNumber(5642121)
                .setAddress("兆麟公园");
        System.out.println(esClientUtil.addDocument(INDEX_NAME, account));*/
        //批量新增
        /*List<Account> documents = new ArrayList<>();
        documents.add(new Account().setFirstName("zhang").setLastName("san").setAge(65).setAccountNumber(456211).setAddress("市区公园"));
        documents.add(new Account().setFirstName("li").setLastName("si").setAge(54).setAccountNumber(635161).setAddress("市区公园"));
        documents.add(new Account().setFirstName("wang").setLastName("wu").setAge(65).setAccountNumber(654643).setAddress("市区公园"));
        System.out.println(esClientUtil.bulkAddDocument(INDEX_NAME, documents));*/
        //根据id查询指定文档
        //System.out.println(esClientUtil.getDocumentById(INDEX_NAME,"HlUYy3YBqiYBG7sfSZ9H"));
        //精确查询
        //System.out.println(Arrays.toString(esClientUtil.MatchPhraseQueryDocument(INDEX_NAME, "address.keyword", "市区公园", 20, 0)));
        //模糊查询
        //System.out.println(Arrays.toString(esClientUtil.matchQueryDocument(INDEX_NAME, "address", "市区公园", 20, 0)));
        //SearchHit[] hits = esClientUtil.matchQueryDocument(INDEX_NAME, "address", "市区公园", 20, 0);
        //判断文档是否存在
        //System.out.println(esClientUtil.isExistsDocument(INDEX_NAME, "136"));
        //System.out.println(esClientUtil.updateDocumentById(INDEX_NAME,"7",new Account().setFirstname("zhang").setLastname("san")));
        //根据id删除指定文档
        System.out.println(esClientUtil.deleteDocumentById(INDEX_NAME, "6"));
        BoolQueryBuilder queryBuilds = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("address", "Holmes Lane"))
                .must(QueryBuilders.matchPhraseQuery("gender", "M"))
                .must(QueryBuilders.rangeQuery("balance").gte("25000").lte("40000"))
                .mustNot(QueryBuilders.matchQuery("lastname", "Odonnell"));
        System.out.println(Arrays.toString(esClientUtil.boolQueryDocuments(INDEX_NAME, 20, 0, queryBuilds)));

    }
}

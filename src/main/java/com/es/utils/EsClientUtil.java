package com.es.utils;

import com.alibaba.fastjson.JSON;
import com.es.config.ElasticSearchConfig;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ElasticSearch工具类
 */
@Component
public class EsClientUtil {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 新增一条文档
     *
     * @param indexName   索引名称
     * @param documentObj 文档对象
     */
    public IndexResponse addDocument(String indexName, Object documentObj) {
        try {
            IndexRequest indexRequest = new IndexRequest(indexName);
            indexRequest.source(JSON.toJSONString(documentObj), XContentType.JSON);
            //同步索引(同步 API 会导致阻塞，一直等待数据返回)
            return restHighLevelClient.index(indexRequest, ElasticSearchConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 批量新增文档
     *
     * @param indexName    索引名称
     * @param documentObjs 文档集合
     * @return 是否报错
     */
    public BulkResponse bulkAddDocument(String indexName, List documentObjs) {
        try {
            BulkRequest bulkRequest = new BulkRequest();
            documentObjs.forEach(doc -> {
                bulkRequest.add(new IndexRequest(indexName).source(JSON.toJSONString(doc), XContentType.JSON));
            });
            return restHighLevelClient.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据id查询文档
     *
     * @param indexName  索引名称
     * @param documentId 文档id
     */
    public GetResponse getDocumentById(String indexName, String documentId) {
        try {
            GetRequest request = new GetRequest(indexName, documentId);
            return restHighLevelClient.get(request, ElasticSearchConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 短语匹配
     *
     * @param indexName 索引名称
     * @param field     字段名(中文需加.keyword)
     * @param text      检索值
     * @param size      每页条数
     * @param from      查询起点
     * @return 所有命中的文档
     */
    public SearchHit[] MatchPhraseQueryDocument(String indexName, String field, String text, int size, int from) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            /**
             * 条件查询 如果查询条件为中文 需要在属性添加 .keyword,不加就查不到
             * 例如： QueryBuilders.termQuery("name.keyword","张三");
             * termQuery:精确查询 matchQuery:模糊查询
             */
            searchRequest.source(getSearchSourceBuilder(size, from, QueryBuilders.matchPhraseQuery(field, text)));
            SearchResponse search = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
            return search.getHits().getHits();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 模糊匹配文档
     *
     * @param indexName 索引名称
     * @param field     字段名称
     * @param text      检索值
     * @param size      每页条数
     * @param from      检索起点
     * @return 命中的文档
     */
    public SearchHit[] matchQueryDocument(String indexName, String field, String text, int size, int from) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(getSearchSourceBuilder(size, from, QueryBuilders.matchQuery(field, text)));
            SearchResponse search = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
            return search.getHits().getHits();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 查询文档是否存在
     *
     * @param indexName  索引名称
     * @param documentId 文档id
     * @return 是否存在
     */
    public boolean isExistsDocument(String indexName, String documentId) {
        try {
            GetRequest request = new GetRequest(indexName, documentId);
            //不获取source上下文
            request.fetchSourceContext(new FetchSourceContext(false));
            return restHighLevelClient.exists(request, ElasticSearchConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据id更新文档
     *
     * @param indexName   索引名称
     * @param documentId  文档名称
     * @param documentObj 文档实体
     * @return 响应
     */
    public UpdateResponse updateDocumentById(String indexName, String documentId, Object documentObj) {
        try {
            UpdateRequest updateRequest = new UpdateRequest(indexName, documentId);
            updateRequest.doc(JSON.toJSONString(documentObj), XContentType.JSON);
            return restHighLevelClient.update(updateRequest, ElasticSearchConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据id删除文档
     *
     * @param indexName  索引名称
     * @param documentId 文档id
     * @return 是否删除成功
     */
    public boolean deleteDocumentById(String indexName, String documentId) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest(indexName, documentId);
            restHighLevelClient.delete(deleteRequest, ElasticSearchConfig.COMMON_OPTIONS);
            //删除后根据id查询是否存在
            return !this.isExistsDocument(indexName, documentId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 聚合查询
     *
     * @param indexName    索引名称
     * @param size         每页条数
     * @param from         检索起点
     * @param queryBuilder 查询条件构建
     * @return 命中的所有文档
     */
    public SearchHit[] boolQueryDocuments(String indexName, int size, int from, QueryBuilder queryBuilder) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(getSearchSourceBuilder(size, from, queryBuilder));
            SearchResponse search = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
            return search.getHits().getHits();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 构建查询条件
     */
    private SearchSourceBuilder getSearchSourceBuilder(int size, int from, QueryBuilder queryBuilder) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //分页 size:每页几条(默认10) from从第几条开始查
        searchSourceBuilder.size(size);
        searchSourceBuilder.from(from);
        searchSourceBuilder.query(queryBuilder);
        //设置查询超时时间
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        System.out.println("request:" + searchSourceBuilder.toString());
        return searchSourceBuilder;
    }

    /**
     * 对象转json,返回转换接口
     */
    private String ObjToJson(Object obj) {
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return getErrorMsg("Json转换异常");
        }
    }

    /**
     * 错误信息
     */
    private String getErrorMsg(String msg) {
        HashMap<String, Object> errorMsg = new HashMap<>();
        errorMsg.put("error", StringUtils.isEmpty(msg) ? "请求处理失败" : msg);
        return JSON.toJSONString(errorMsg);
    }
}

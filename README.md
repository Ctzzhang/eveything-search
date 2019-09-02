# eveything-search
### 简介
使用 SpringBoot2.0 + ElasticSearch + Jest 实现的电影搜索网站

### 本地部署
* 启动 ElasticSearch 6.X+
* 修改 application.properties 中的 spring.elasticsearch.jest.uris 参数
* 启动 SpringBoot 项目
* 访问 <localhost:8080/spider/movie> 开启movie爬虫
* 访问 <localhost:8080> 开始搜索





### 参考资料
* [SpringBoot 官方文档 ElasticSearch 部分](https://docs.spring.io/spring-boot/docs/2.1.3.RELEASE/reference/htmlsingle/#boot-features-elasticsearch)
* [ElasticSearch TransportClient](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/java-api.html)
* [Jest 官方文档](https://github.com/searchbox-io/Jest/tree/master/jest)
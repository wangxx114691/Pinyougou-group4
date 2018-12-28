package com.itheima.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LuceneFirst {

    @Test
    public void testAdd()throws Exception{
        // 1.原始文档 Mysql数据库

        // 2.获取文档 jdbc
        BookDaoImpl bookDao = new BookDaoImpl();
        List<Book> books = bookDao.queryBookList();
        // 4.分析次词
//        StandardAnalyzer analyzer = new StandardAnalyzer();
        Analyzer analyzer = new IKAnalyzer();

        // 5.创建索引(保存索引)     // 构造器就两个参数, 一个是分词器, 一个是版本
        FSDirectory directory = FSDirectory.open(new File("D:\\327\\temp\\index"));
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST,analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        for (Book book : books) {
            // 创建文档对象
            Document doc = new Document();
            // 域
            // ID   不分   不索  存      // storedField  N  N  Y
            Field idField = new StoredField("id", "" + book.getId());
            // 名称 分     索    存      // TextField    Y  Y  N/Y
            TextField nameField = new TextField("name",book.getName(), Field.Store.YES);
            // 价格 不分   索    存      // FloatField   N  Y  N/Y
            FloatField priceField = new FloatField("price",book.getPrice(), Field.Store.YES);
            // 图片 不分   不索  存
            StoredField picField = new StoredField("pic",book.getPic());
            // 描述 分     索    可存/不存
            TextField descField = new TextField("desc", book.getDesc(), Field.Store.NO);
            // 注: 还有一个StringField N  Y  N/Y, 一般用于身份证号/手机号等这些不需要分词,但是需要索引的

            doc.add(idField);
            doc.add(nameField);
            doc.add(priceField);
            doc.add(picField);
            doc.add(descField);
            // 将文档添加到索引
            indexWriter.addDocument(doc);

        }

        // 关流
//        indexWriter.commit();// 这一步可以不写, 因为关流前会自动提交事务
        indexWriter.close();
    }

    // 查询索引
    @Test
    public void testIndex() throws IOException {
        // 创建查询目录
        FSDirectory directory = FSDirectory.open(new File("D:\\327\\temp\\index"));
        // 创建查询流对象
        IndexReader reader = DirectoryReader.open(directory);
        // 创建查询对象
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        // 执行查询 使用的search方法, 参数需要传入查询条件Query或者是过滤条件Filter
        Query query = new TermQuery(new Term("name","java"));
        TopDocs topDocs = indexSearcher.search(query, 5); // 第二参数是返回的头几条的坐标(文档的ID数组)
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;   // 直接通过调用成员变量获取评分前5条,没有getter???
        if (scoreDocs != null && scoreDocs.length>0){
            for (ScoreDoc scoreDoc : scoreDocs) {
                int docId= scoreDoc.doc;
                Document doc = indexSearcher.doc(docId);
                System.out.println("id:"+doc.get("id"));
                System.out.println("name:"+doc.get("name"));
                System.out.println("price:"+doc.get("price"));
                System.out.println("pic:"+doc.get("pic"));
                System.out.println("desc:"+doc.get("desc"));
            }
        }
    }

    // 删除索引  方法deleteDocument(Term)
    @Test
    public void testDelete() throws Exception {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        FSDirectory directory = FSDirectory.open(new File("D:\\327\\temp\\index"));
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST,analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        // 只有查询使用indexSearch,增删改都是使用indexWriter
        // indexWriter.deleteAll(); //删除后不可恢复,慎用
        indexWriter.deleteDocuments(new Term("name","java")); // new Term (K,V)
        indexWriter.close();
    }

    // 修改索引: 原理: 删除+添加(会把更新前的数据删掉)  方法:updatDocument(Term,document)
    // 删除索引
    @Test
    public void testUpdate() throws Exception {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        FSDirectory directory = FSDirectory.open(new File("D:\\327\\temp\\index"));
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST,analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);


        // 创建文档对象
        Document doc = new Document();
        // 域
        Field idField = new StoredField("Id", "8"); // 域name的对大小写敏感, 当大小写不同时,会创建新的field
        TextField nameField = new TextField("NAME","测试名称2", Field.Store.YES);
        TextField descField = new TextField("DESC", "测试内容", Field.Store.NO);
        doc.add(idField);
        doc.add(nameField);
        doc.add(descField);
        indexWriter.updateDocument(new Term("name","lucene"),doc);
        // 关流
        indexWriter.close();
    }
}

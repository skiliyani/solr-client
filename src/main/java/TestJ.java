import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

public class TestJ {
    public static void main(String[] args) throws SolrServerException, IOException {
        SolrClient crawlerClient = new HttpSolrClient.Builder("http://172.31.13.238:8983/solr/crawler_data").build();
        
        Map<String, Boolean> fieldModifier = new HashMap<String, Boolean>();
        fieldModifier.put("set", true);
        
        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", "ff3eda8ed223f85fbb22684526791caa");
        document.addField("test_b", fieldModifier);
        crawlerClient.add(document);
        crawlerClient.commit();
    }
}

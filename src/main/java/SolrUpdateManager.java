import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

public class SolrUpdateManager {

    private static SolrClient sourceClient = new HttpSolrClient.Builder("http://172.31.13.238:8984/solr/crawler_data")
            .build();
    private static SolrClient destinationClient = new HttpSolrClient.Builder("http://10.0.1.88:8983/solr/crawler_data").build();

    private static Map<String, String> statusVerified = new HashMap<String, String>();
    private static Map<String, String> verifiedBy = new HashMap<String, String>();

    public static void main(String[] args) throws SolrServerException, IOException {

        statusVerified.put("set", "verified");
        verifiedBy.put("set", "automatched@binaryfountain.com");

        String fq = "pass_b:true";

        SolrDocumentList sourceResults;
        int start = 0;
        
        do {

            SolrQuery sourceQuery = new SolrQuery();
            sourceQuery.setQuery("*:*");
            sourceQuery.addFilterQuery(fq);
            sourceQuery.setStart(start);
            sourceQuery.setRows(1);
            
            System.out.println("start:" + start + ",rows:1000");
            
            QueryResponse sourceResponse = sourceClient.query(sourceQuery);
            sourceResults = sourceResponse.getResults();
            for (int i = 0; i < sourceResults.size(); ++i) {
                SolrDocument sourceDoc = sourceResults.get(i);
                String sourceUrl = (String) sourceDoc.get("docurl");

                SolrQuery destinationQuery = new SolrQuery();
                destinationQuery.setQuery("docurl:" + ClientUtils.escapeQueryChars(sourceUrl));
                destinationQuery.setRows(5000);

                QueryResponse destinationResponse = destinationClient.query(destinationQuery);
                SolrDocumentList destinationResults = destinationResponse.getResults();
                if (destinationResults.size() > 0) {
                    // we have found a url match in target index, update the flag as verified
                    SolrDocument document1 = destinationResults.get(0);
                    String id = (String) document1.get("id");

                    SolrInputDocument document2 = new SolrInputDocument();
                    document2.addField("id", id);
                    document2.addField("source_status", statusVerified);
                    document2.addField("verified_by", verifiedBy);

                    destinationClient.add(document2);
                    destinationClient.commit();

                    System.out.println("id:" + id);
                } else {
                    System.err.println("No match for docurl:" + ClientUtils.escapeQueryChars(sourceUrl));
                }
            }
            
            start += sourceResults.size();
            
        } while (sourceResults.size() > 0);

    }

}

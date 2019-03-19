import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrExporter {

    protected static boolean DEBUG = true;

    protected static final String[] sourceFields = { "firstname", "middlename", "lastname", "docname", "degree",
            "spname", "address", "address1", "address2", "city", "state", "zip", "phone", "source", "docurl"};

    protected static void toCSV(SolrDocument source) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < sourceFields.length; i++) {
            builder.append("\"").append(arrayOrString(source.get(sourceFields[i]))).append("\"").append(",");
        }

        System.out.println(builder.toString());
    }

    private static String arrayOrString(Object doc) {
        StringBuilder builder = new StringBuilder();

        if (doc instanceof ArrayList) {
            List list = (ArrayList) doc;
            for (Object object : list) {
                builder.append(object);
                builder.append(",");
            }

            return builder.toString().substring(0, builder.length() - 1);
        } else {
            builder.append(doc);
        }

        return builder.toString();
    }

    public SolrExporter() {
        super();
    }

    public static void main(String[] args) throws Exception {

        String fq = "source:wellness";

        if (args.length > 0) {
            fq = args[0];
        }

        SolrClient crawlerClient = new HttpSolrClient.Builder("http://172.31.13.238:8984/solr/crawler_data").build();

        for (int i = 0; i < sourceFields.length; i++) {
            System.out.print(sourceFields[i]);
            System.out.print(",");
        }

        System.out.println("");

        SolrDocumentList results = null;
        Integer start = 0;
        long total = 0;

        do {
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            if(fq != null) {
                query.setFilterQueries(fq);
            }
            query.setStart(start);
            query.setRows(1000);

            QueryResponse crawlerResponse = crawlerClient.query(query);
            results = crawlerResponse.getResults();
            for (int i = 0; i < results.size(); ++i) {
                try {
                    toCSV(results.get(i));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            start = (int) (start + results.size());
            total += results.size();

        } while (results.size() > 0);

        System.out.println("Total processed=" + total);
    }

}
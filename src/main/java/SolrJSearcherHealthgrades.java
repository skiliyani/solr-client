import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrJSearcherHealthgrades extends SolrSearcher {

    private static final String FQ_CRAWLER = "source:healthgrades AND source_status:pending";
    private static final String FQ_CMS = "Provider_Taxonomies:obstetrics/gynecology";

    public static void main(String[] args) throws Exception {

        if(args.length > 0) {
            isDebug = Boolean.parseBoolean(args[0]);
        }

        if (isDebug) {
            System.err.println("========================================");
            System.err.println("\t\tDEBUG\t\t");
            System.err.println("========================================");
        }

        String fq = FQ_CRAWLER;
        String processedFlag = "source_status";

        SolrClient cmsClient = new HttpSolrClient.Builder("http://10.0.1.88:8983/solr/cms_data").build();
        SolrClient crawlerClient = new HttpSolrClient.Builder("http://10.0.1.88:8983/solr/crawler_data").build();


        System.out.print("S1,");
        for (int i = 0; i < cmsFields.length; i++) {
            System.out.print(cmsFields[i]);
            System.out.print(",");
        }

        System.out.print("S2,");
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
            query.setFilterQueries(FQ_CMS);
            query.setQuery("*:*");
            query.setStart(start);
            if (isDebug) {
                query.setRows(10);
            } else {
                query.setRows(1000);
            }

            QueryResponse response = cmsClient.query(query);
            results = response.getResults();
            for (int i = 0; i < results.size(); ++i) {
                try {
                    SolrDocument cmsDoc = results.get(i);
                    SolrQuery crawlerQuery = new SolrQuery();
                    crawlerQuery.setQuery(getSourceQuery(cmsDoc));
                    crawlerQuery.setFields("score","*");
                    if (fq != null) {
                        crawlerQuery.setFilterQueries(fq);
                    }

                    QueryResponse crawlerResponse = crawlerClient.query(crawlerQuery);
                    SolrDocumentList crawlerResults = crawlerResponse.getResults();

                    long numFound = crawlerResults.getNumFound();
                    if (numFound > 0) {
                        for (int j = 0; j < crawlerResults.size(); j++) {
                            SolrDocument sourceDoc = crawlerResults.get(j);
                            toCSV(cmsDoc, sourceDoc);
                            if (!isDebug) {
                                flagAsProcessed(crawlerClient, sourceDoc, processedFlag);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            start = (int) (start + results.size());
            total += results.size();

        } while (results.size() > 0);

        System.out.println("Total processed=" + total);
    }

    protected static String getSourceQuery(SolrDocument document) {
        StringBuilder builder = new StringBuilder();
        /*
        builder.append("firstname:").append(document.get("Provider_First_Name"));
        builder.append(" AND ").append("lastname:").append(document.get("Provider_Last_Name")).append(",");
        builder.append(" OR ").append("middlename:").append(document.get("Provider_Middle_Name")).append("^10.0");
        builder.append(" OR ").append("degree:(").append(toOr(document.get("Provider_Credentials_str"))).append(")").append("^5.0");
        builder.append(" OR ").append("spname:(").append(toOrEx(document.get("Provider_Taxonomies"))).append(")").append("^4.0");
        builder.append(" OR ").append("city:(").append(toOr(document.get("Provider_Cities_txt"))).append(")").append("^3.0");
        builder.append(" OR ").append("state:(").append(toOr(document.get("Provider_States_txt"))).append(")").append("^2.0");
        */

        builder.append("+firstname:").append(document.get("Provider_First_Name")).append(" ");
        builder.append("+lastname:").append(document.get("Provider_Last_Name")).append(",").append(" ");
        //builder.append("+state:(").append(toOr(document.get("Provider_States_txt"))).append(")").append("^2.0").append(" ");


        if (isDebug) {
            System.err.println("[solr][NPI -> " + document.get("NPI") + "] " + builder.toString());
        }

        return builder.toString();

    }

}
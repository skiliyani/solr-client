import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrJSearcherGeneric extends SolrSearcher {

    private static String FQ_CRAWLER = null;
    private static String FQ_CMS = null;

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("Few number of args (app.jar fq_crawler fq_cms)");
            System.exit(1);
        }
        FQ_CRAWLER = args[0];
        FQ_CMS = args[1];
        Pattern pattern = Pattern.compile("\\+source:(.*)\\s");
        Matcher matcher = pattern.matcher(FQ_CRAWLER);
        String fileBasename = "match";
        if (matcher.find()) {
            fileBasename = matcher.group(1).replaceAll("\\W", "_");
        }
        Pattern pattern2 = Pattern.compile("^Provider_Taxonomies:(.*)$");
        Matcher matcher2 = pattern2.matcher(FQ_CMS);
        if (matcher2.find()) {
            fileBasename += "_" + matcher2.group(1).replaceAll("\\W", "_");
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileBasename + ".csv"));

        try {

            System.out.println("Writing output to " + fileBasename + ".csv");

            String processedFlag = "source_status";
            SolrClient cmsClient = new HttpSolrClient.Builder("http://10.0.1.88:8983/solr/cms_usn_data").build();
            SolrClient crawlerClient = new HttpSolrClient.Builder("http://10.0.1.88:8983/solr/crawler_data_2").build();
            writer.write("S1,");
            for (int i = 0; i < cmsFields.length; i++) {
                writer.write(cmsFields[i]);
                writer.write(",");
            }
            writer.write("S2,");
            for (int i = 0; i < sourceFields.length; i++) {
                writer.write(sourceFields[i]);
                writer.write(",");
            }
            writer.write("\n");
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
                        crawlerQuery.setQuery(getCrawlerQuery(cmsDoc));
                        crawlerQuery.setFields("score", "*");
                        if (FQ_CRAWLER != null) {
                            crawlerQuery.setFilterQueries(FQ_CRAWLER);
                        }

                        QueryResponse crawlerResponse = crawlerClient.query(crawlerQuery);
                        SolrDocumentList crawlerResults = crawlerResponse.getResults();

                        long numFound = crawlerResults.getNumFound();
                        if (numFound > 0) {
                            for (int j = 0; j < crawlerResults.size(); j++) {
                                SolrDocument sourceDoc = crawlerResults.get(j);
                                toCSVFile(writer, cmsDoc, sourceDoc);
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
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    protected static String getCrawlerQuery(SolrDocument document) {
        StringBuilder builder = new StringBuilder();
        /*
         * builder.append("firstname:").append(document.get("Provider_First_Name"));
         * builder.append(" AND ").append("lastname:").append(document.get(
         * "Provider_Last_Name")).append(",");
         * builder.append(" OR ").append("middlename:").append(document.get(
         * "Provider_Middle_Name")).append("^10.0");
         * builder.append(" OR ").append("degree:(").append(toOr(document.get(
         * "Provider_Credentials_str"))).append(")").append("^5.0");
         * builder.append(" OR ").append("spname:(").append(toOrEx(document.get(
         * "Provider_Taxonomies"))).append(")").append("^4.0");
         * builder.append(" OR ").append("city:(").append(toOr(document.get(
         * "Provider_Cities_txt"))).append(")").append("^3.0");
         * builder.append(" OR ").append("state:(").append(toOr(document.get(
         * "Provider_States_txt"))).append(")").append("^2.0");
         */

        builder.append("+firstname:").append(document.get("Provider_First_Name")).append("^10").append(" ");
        builder.append("+lastname:").append(document.get("Provider_Last_Name"));
        if (FQ_CRAWLER.contains("healthgrades")) {
            //builder.append(","); // dirty fix for data issues
        }
        builder.append("^10 ");
        builder.append("+state:(").append(toOr(document.get("Provider_States_txt"))).append(")").append("^10.0")
                .append(" ");
        if (document.get("Provider_Middle_Name") != null) {
            builder.append("middlename:").append(document.get("Provider_Middle_Name")).append("^10.0").append(" ");
        }
        builder.append("degree:(").append(toOr(document.get("Provider_Credentials_str"))).append(")").append("^5.0")
                .append(" ");
        builder.append("city:(").append(toOr(document.get("Provider_Cities_txt"))).append(")").append("^7.5")
                .append(" ");

        if (isDebug) {
            System.out.println("[solr][" + document.get("NPI") + "] " + builder.toString());
        }

        return builder.toString();

    }

}
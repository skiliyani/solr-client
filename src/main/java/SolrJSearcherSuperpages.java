import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

public class SolrJSearcherSuperpages {

    private final static boolean DEBUG = false;

    private final static String[] cmsFields = { "ENTITY_ID", "NPI", "Provider_Credential_Text", "Provider_First_Name",
            "Provider_Middle_Name", "Provider_Last_Name", "Provider_Taxonomies", "Provider_States_txt" };

    private final static String[] sourceFields = { "docname", "firstname", "middlename", "lastname", "degree", "spname",
            "state", "source", "source_status", "docurl" };

    public static void main(String[] args) throws Exception {

        String fq = "source:superpages AND -pass_b:[* TO *]";
        String processedFlag = "pass";

        if (args.length > 0) {
            fq = args[0];
        }

        if (args.length > 1) {
            processedFlag = args[1];
        }

        SolrClient cmsClient = new HttpSolrClient.Builder("http://172.31.13.238:8984/solr/cms_usn").build();
        SolrClient crawlerClient = new HttpSolrClient.Builder("http://172.31.13.238:8984/solr/crawler_data").build();

        for (int i = 0; i < cmsFields.length; i++) {
            System.out.print(cmsFields[i]);
            System.out.print(",");
        }
        for (int i = 0; i < sourceFields.length; i++) {
            System.out.print(sourceFields[i]);
            System.out.print(",");
        }

        SolrDocumentList results = null;
        Integer start = 0;
        long total = 0;

        do {
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            query.setStart(start);
            if (DEBUG) {
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
                    if (fq != null) {
                        crawlerQuery.setFilterQueries(fq);
                    }

                    QueryResponse crawlerResponse = crawlerClient.query(crawlerQuery);
                    SolrDocumentList crawlerResults = crawlerResponse.getResults();

                    long numFound = crawlerResults.getNumFound();
                    if (numFound == 1) {
                        SolrDocument sourceDoc = crawlerResults.get(0);
                        toCSV(cmsDoc, sourceDoc);
                        if (!DEBUG) {
                            flagAsProcessed(crawlerClient, sourceDoc, processedFlag);
                        }
                    }

                } catch (Exception e) {

                }

            }

            start = (int) (start + results.size());
            total += results.size();

            System.err.println(SolrJSearcherSuperpages.class + "=>" + total);

        } while (results.size() > 0);

        System.out.println("Total processed=" + total);
    }

    private static void flagAsProcessed(SolrClient solr, SolrDocument sourceDoc, String flag) throws Exception {
        String id = (String) sourceDoc.get("id");

        Map<String, Boolean> fieldModifier = new HashMap<String, Boolean>();
        fieldModifier.put("set", true);

        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", id);
        document.addField(flag + "_b", fieldModifier);

        solr.add(document);
        solr.commit();

    }

    private static void toCSV(SolrDocument cms, SolrDocument source) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cmsFields.length; i++) {
            builder.append("\"").append(arrayOrString(cms.get(cmsFields[i]))).append("\"").append(",");
        }

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

    private static String toOr(Object field) {
        StringBuilder builder = new StringBuilder();

        if (field instanceof ArrayList) {
            List list = (ArrayList) field;
            for (Object object : list) {
                builder.append(((String) object).replaceAll("[.\\s]", ""));
                builder.append(" || ");
            }

            return (builder.toString().substring(0, builder.length() - 4));
        }

        return ((String) field).replaceAll("[.\\s]", "").replaceAll("[\\W]", " || ");
    }

    private static String toOrEx(Object field) {
        StringBuilder builder = new StringBuilder();

        if (field instanceof ArrayList) {
            List<String> list = (ArrayList) field;
            Object[] array = list.toArray();
            for (int i = 0; i < array.length; i++) {
                String object = (String) array[i];
                builder.append(clean(object));
                if (i != array.length - 1)
                    builder.append(" || ");
            }

        } else if (field instanceof String) {
            builder.append(clean((String) field).replaceAll("[\\W]", "*"));
        } else {
            builder.append(field);
        }

        return builder.toString();
    }

    private static String clean(String text) {
        return wild(text.replaceAll("[.]", ""));
    }

    private static String wild(String replaceAll) {
        StringBuilder builder = new StringBuilder();
        String[] split = replaceAll.split("[\\W]");
        for (int i = 0; i < split.length; i++) {
            builder.append("*").append(split[i]).append("*");
            if (i != split.length - 1)
                builder.append(" || ");
        }
        return builder.toString();
    }

    private static String getSourceQuery(SolrDocument document) {
        StringBuilder builder = new StringBuilder();

        builder.append("docname:\"").append(document.get("Provider_First_Name")).append(" ");
        if (document.get("Provider_Middle_Name") != null)
            builder.append(document.get("Provider_Middle_Name")).append(" ");
        builder.append(document.get("Provider_Last_Name")).append(" ");
        builder.append(toOr(document.get("Provider_Credentials_str"))).append("\"");
        builder.append(" AND ").append("state:(").append(toOr(document.get("Provider_States_txt"))).append(")");

        if (DEBUG) {
            System.out.println("solr -> " + builder.toString());
        }

        return builder.toString();

    }
}
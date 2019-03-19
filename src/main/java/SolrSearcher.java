import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;


public class SolrSearcher {

    protected static boolean isDebug = true;

    protected static final String[] cmsFields = {"NPI", "Provider_Credential_Text", "Provider_First_Name",
                "Provider_Middle_Name", "Provider_Last_Name", "Provider_Taxonomies", "Provider_Cities_txt", "Provider_States_txt" };
    protected static final String[] sourceFields = { "score","docname", "firstname", "middlename", "lastname", "degree", "spname", "city",
                "state", "source", "source_status", "docurl" };

    protected static void flagAsProcessed(SolrClient solr, SolrDocument sourceDoc, String flag) throws Exception {
        String id = (String) sourceDoc.get("id");

        Map<String, String> fieldModifier = new HashMap<String, String>();
        fieldModifier.put("set", "verified");

        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", id);
        document.addField(flag, fieldModifier);

        solr.add(document);
        solr.commit();

    }

    protected static void toCSV(SolrDocument cms, SolrDocument source) {
        StringBuilder builder = new StringBuilder();

        builder.append("\"[CMS->]\",");

        for (int i = 0; i < cmsFields.length; i++) {
            builder.append("\"").append(arrayOrString(cms.get(cmsFields[i]))).append("\"").append(",");
        }

        builder.append("\"[CRAWLER->]\",");

        for (int i = 0; i < sourceFields.length; i++) {
            builder.append("\"").append(arrayOrString(source.get(sourceFields[i]))).append("\"").append(",");
        }

        System.out.println(builder.toString());
    }

    protected static void toCSVFile(BufferedWriter writer, SolrDocument cms, SolrDocument source) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("\"[CMS->]\",");

        for (int i = 0; i < cmsFields.length; i++) {
            builder.append("\"").append(arrayOrString(cms.get(cmsFields[i]))).append("\"").append(",");
        }

        builder.append("\"[CRAWLER->]\",");

        for (int i = 0; i < sourceFields.length; i++) {
            builder.append("\"").append(arrayOrString(source.get(sourceFields[i]))).append("\"").append(",");
        }

        builder.append("\n");

        System.out.println(builder.toString());
        writer.write(builder.toString());
        writer.flush();
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

    protected static String toOr(Object field) {
        if(field == null) {
            return "*";
        }

        StringBuilder builder = new StringBuilder();

        if (field instanceof ArrayList) {
            List list = (ArrayList) field;
            for (Object object : list) {
                builder.append("\"").append(((String) object).replaceAll("[.]", "")).append("\"");
                builder.append(" || ");
            }

            return (builder.toString().substring(0, builder.length() - 4));
        }

        return ((String) field).replaceAll("[.]", "").replaceAll("[\\W]", " || ");
    }

    protected static String clean(String text) {
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

    public SolrSearcher() {
        super();
    }

    protected static String toOrEx(Object field) {
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

}
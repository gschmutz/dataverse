package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountUtil;
import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.text.ParseException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import javax.ws.rs.core.UriInfo;

/**
 * API endpoints for various metrics.
 *
 * These endpoints look a bit heavy because they check for a timely cached value
 * to use before responding. The caching code resides here because the this is
 * the point at which JSON is generated and this JSON was deemed the easiest to
 * cache.
 *
 * @author pdurbin, madunlap
 */
@Path("info/metrics")
public class Metrics extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Metrics.class.getName());

    /** Dataverses */

    @GET
    @Path("dataverses")
    public Response getDataversesAllTime(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        return getDataversesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("dataverses/monthly")
    @Produces("application/json, text/csv")
    public Response getDataversesTimeSeries(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        String requestedType = httpRequest.getHeader("Accept");

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "dataverses";
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save

            jsonArray = metricsSvc.getDataversesTimeSeries(uriInfo, d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        if((requestedType!=null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV)) ;
    }

    @GET
    @Path("dataverses/toMonth/{yyyymm}")
    public Response getDataversesToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.dataversesToMonth(sanitizedyyyymm, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("dataverses/pastDays/{days}")
    public Response getDataversesPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.dataversesPastDays(days, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("dataverses/byCategory")
    @Produces("application/json, text/csv")
    public Response getDataversesByCategory(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        String requestedType = httpRequest.getHeader("Accept");

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesByCategory";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            jsonArray = MetricsUtil.dataversesByCategoryToJson(metricsSvc.dataversesByCategory(d)).build();
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.CATEGORY, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    @GET
    @Path("dataverses/bySubject")
    @Produces("application/json, text/csv")
    public Response getDataversesBySubject(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        String requestedType = httpRequest.getHeader("Accept");

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesBySubject";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            jsonArray = MetricsUtil.dataversesBySubjectToJson(metricsSvc.dataversesBySubject(d)).build();
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }

        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.SUBJECT, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    /** Datasets */

    @GET
    @Path("datasets")
    public Response getDatasetsAllTime(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        return getDatasetsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation, parentAlias);
    }


    @GET
    @Path("datasets/monthly")
    @Produces("application/json, text/csv")
    public Response getDatasetsTimeSeries(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {

        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        String requestedType = httpRequest.getHeader("Accept");
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "datasets";
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save

            jsonArray = metricsSvc.getDatasetsTimeSeries(uriInfo, dataLocation, d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    @GET
    @Path("datasets/toMonth/{yyyymm}")
    public Response getDatasetsToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "datasetsToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.datasetsToMonth(sanitizedyyyymm, validDataLocation, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("datasets/pastDays/{days}")
    public Response getDatasetsPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "datasetsPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), validDataLocation, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.datasetsPastDays(days, validDataLocation, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), validDataLocation, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("datasets/bySubject")
    @Produces("application/json, text/csv")
    public Response getDatasetsBySubject(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        return getDatasetsBySubjectToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation, parentAlias);
    }

    @GET
    @Path("datasets/bySubject/toMonth/{yyyymm}")
    @Produces("application/json, text/csv")
    public Response getDatasetsBySubjectToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        String requestedType = httpRequest.getHeader("Accept");

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "datasetsBySubjectToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation, d));

        if (null == jsonArray) { // run query and save
            jsonArray = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubjectToMonth(sanitizedyyyymm, validDataLocation, d)).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, d, jsonArray.toString()));
        }
        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.SUBJECT, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    /** Files */
    @GET
    @Path("files")
    public Response getFilesAllTime(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        return getFilesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("files/monthly")
    @Produces("application/json, text/csv")
    public Response getFilesTimeSeries(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        String requestedType = httpRequest.getHeader("Accept");
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "files";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save

            jsonArray = metricsSvc.filesTimeSeries(d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    @GET
    @Path("files/toMonth/{yyyymm}")
    public Response getFilesToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "filesToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        logger.fine("yyyymm: " + sanitizedyyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));
        logger.fine("Returned");
        if (null == jsonObj) { // run query and save
            logger.fine("Getting filesToMonth : " + sanitizedyyyymm + " dvId=" + d.getId());
            Long count = metricsSvc.filesToMonth(sanitizedyyyymm, d);
            logger.fine("count = " + count);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("files/pastDays/{days}")
    public Response getFilesPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "filesPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }

        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.filesPastDays(days, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }
    
    @GET
    @Path("/files/byType/monthly")
    @Produces("application/json, text/csv")
    public Response getFilesByTypeTimeSeries(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        String requestedType = httpRequest.getHeader("Accept");
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "filesByType";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            //Only handling published right now
            jsonArray = metricsSvc.filesByTypeTimeSeries(d, true);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.CONTENTTYPE, MetricsUtil.COUNT, MetricsUtil.SIZE), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }
    
    @GET
    @Path("/files/byType")
    @Produces("application/json, text/csv")
    public Response getFilesByType(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        String requestedType = httpRequest.getHeader("Accept");
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "filesByType";


        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            jsonArray = metricsSvc.filesByType(d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }

        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.CONTENTTYPE, MetricsUtil.COUNT, MetricsUtil.SIZE), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    /** Downloads */

    @GET
    @Path("downloads")
    public Response getDownloadsAllTime(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        return getDownloadsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("downloads/monthly")
    @Produces("application/json, text/csv")
    public Response getDownloadsTimeSeries(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        String requestedType = httpRequest.getHeader("Accept");
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "downloads";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            //Only handling published right now
            jsonArray = metricsSvc.downloadsTimeSeries(d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    @GET
    @Path("downloads/toMonth/{yyyymm}")
    public Response getDownloadsToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "downloadsToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonObj) { // run query and save
            Long count;
            try {
                count = metricsSvc.downloadsToMonth(sanitizedyyyymm, d);
            } catch (ParseException e) {
                return error(BAD_REQUEST, "Unable to parse supplied date: " + e.getLocalizedMessage());
            }
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("downloads/pastDays/{days}")
    public Response getDownloadsPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "downloadsPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.downloadsPastDays(days, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }



    @GET
    @Path("makeDataCount/{metric}")
    public Response getMakeDataCountMetricCurrentMonth(@Context UriInfo uriInfo, @PathParam("metric") String metricSupplied, @QueryParam("country") String country, @QueryParam("parentAlias") String parentAlias) {
        return getMakeDataCountMetricToMonth(uriInfo, metricSupplied, MetricsUtil.getCurrentMonth(), country, parentAlias);
    }
    
    @GET
    @Path("makeDataCount/{metric}/monthly")
    @Produces("application/json, text/csv")
    public Response getMakeDataCountMetricTimeSeries(@Context UriInfo uriInfo, @PathParam("metric") String metricSupplied, @QueryParam("country") String country, @QueryParam("parentAlias") String parentAlias) {
        String requestedType = httpRequest.getHeader("Accept");
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        MakeDataCountUtil.MetricType metricType = null;
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias", "country" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        try {
            metricType = MakeDataCountUtil.MetricType.fromString(metricSupplied);
        } catch (IllegalArgumentException ex) {
            return error(Response.Status.BAD_REQUEST, ex.getMessage());
        }
        String metricName = "MDC-" + metricType.toString() + ((country == null) ? "" : "-" + country);

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            //Only handling published right now
            jsonArray = metricsSvc.mdcMetricTimeSeries(metricType, country, d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));
    }

    @GET
    @Path("makeDataCount/{metric}/toMonth/{yyyymm}")
    public Response getMakeDataCountMetricToMonth(@Context UriInfo uriInfo, @PathParam("metric") String metricSupplied, @PathParam("yyyymm") String yyyymm, @QueryParam("country") String country, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        MakeDataCountUtil.MetricType metricType = null;
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias", "country" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        try {
            metricType = MakeDataCountUtil.MetricType.fromString(metricSupplied);
        } catch (IllegalArgumentException ex) {
            return error(Response.Status.BAD_REQUEST, ex.getMessage());
        }
        if (country != null) {
            country = country.toLowerCase();

            if (!MakeDataCountUtil.isValidCountryCode(country)) {
                return error(Response.Status.BAD_REQUEST, "Country must be one of the ISO 1366 Country Codes");
            }
        }
        String metricName = "MDC-" + metricType.toString() + ((country == null) ? "" : "-" + country);

        String sanitizedyyyymm = null;
        if (yyyymm != null) {
            sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        }

        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonObj) { // run query and save
            jsonObj = metricsSvc.getMDCDatasetMetrics(metricType, sanitizedyyyymm, country, d);
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("uniquedownloads")
    public Response getUniqueDownloadsAllTime(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        return getUniqueDownloadsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }
    
    @GET
    @Path("uniquedownloads/monthly")
    @Produces("application/json, text/csv")
    public Response getUniqueDownloadsTimeSeries(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
            String requestedType = httpRequest.getHeader("Accept");
            Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
            try {
                errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias"});
            } catch (IllegalArgumentException ia) {
                return error(BAD_REQUEST, ia.getLocalizedMessage());
            }
            String metricName = "uniqueDownloads";

            JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

            if (null == jsonArray) { // run query and save
                //Only handling published right now
                jsonArray = metricsSvc.uniqueDownloadsTimeSeries(d);
                metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
            }
            if ((requestedType != null) && (requestedType.equalsIgnoreCase(MediaType.APPLICATION_JSON))) {
                return ok(jsonArray);
            }
            return ok(FileUtil.jsonToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.PID, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV));        
     }

    @GET
    @Path("uniquedownloads/toMonth/{yyyymm}")
    public Response getUniqueDownloadsToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "uniqueDownloads";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonObj) { // run query and save
            jsonObj = metricsSvc.uniqueDatasetDownloads(sanitizedyyyymm, d).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("tree")
    public Response getDataversesTree(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        return getDataversesTreeToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("tree/toMonth/{yyyymm}")
    public Response getDataversesTreeToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {

        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "tree";
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);

        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));
        if (null == jsonObj) { // run query and save
            jsonObj = metricsSvc.getDataverseTree(d, sanitizedyyyymm, DatasetVersion.VersionState.RELEASED);
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }
        return ok(jsonObj);
    }

    private void errorIfUnrecongizedQueryParamPassed(UriInfo uriDetails, String[] allowedQueryParams) throws IllegalArgumentException {
        for (String theKey : uriDetails.getQueryParameters().keySet()) {
            if (!Arrays.stream(allowedQueryParams).anyMatch(theKey::equals)) {
                throw new IllegalArgumentException("queryParameter " + theKey + " not supported for this endpont");
            }
        }

    }

    //Throws a WebApplicationException if alias is not null and Dataverse can't be found
    private Dataverse findDataverseOrDieIfNotFound(String alias) {
        Dataverse d = null;
        if (alias != null) {
            d = dataverseSvc.findByAlias(alias);
            if (d == null) {
                throw new NotFoundException("No Dataverse with alias: " + alias);
            }
        }
        //For a public API - we only want released dataverses used as parentAliases
        //Code could be updated to handle draft info as well (with access controls in place)
        if((d!=null) && !d.isReleased()) {
            d=null;
        }
        return d;
    }

}

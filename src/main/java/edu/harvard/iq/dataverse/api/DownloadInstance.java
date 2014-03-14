/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

//import java.io.ByteArrayOutputStream;
import java.util.List;
import edu.harvard.iq.dataverse.dataaccess.OptionalAccessService;
/**
 *
 * @author Leonid Andreev
 */
public class DownloadInstance {
    /*
    private ByteArrayOutputStream outStream = null;

    public ByteArrayOutputStream getOutStream() {
        return outStream;
    }

    public void setOutStream(ByteArrayOutputStream outStream) {
        this.outStream = outStream;
    }
    */
    
    private DownloadInfo downloadInfo = null; 
    private String conversionParam = null; 
    private String conversionParamValue = null; 
    
    public DownloadInstance (DownloadInfo info) {
        this.downloadInfo = info; 
    }
    
    public DownloadInfo getDownloadInfo () {
        return downloadInfo; 
    }
    
    public void setDownloadInfo (DownloadInfo info) {
        this.downloadInfo = info; 
    }
    
    public String getConversionParam () {
        return conversionParam; 
    }
    
    public void setConversionParam (String param) {
        this.conversionParam = param; 
    }
    
    public String getConversionParamValue () {
        return conversionParamValue; 
    }
    
    public void setConversionParamValue (String paramValue) {
        this.conversionParamValue = paramValue; 
    }
    
    // Move this method into the DownloadInfo instead -- ?
    
    public Boolean isDownloadServiceSupported (String serviceArg, String serviceArgValue) {
        if (downloadInfo == null || serviceArg == null) {
            return false;
        }
        
        List<OptionalAccessService> servicesAvailable = downloadInfo.getServicesAvailable();
        
        for (OptionalAccessService dataService : servicesAvailable) {
            if (dataService != null) {
                // Special case for the subsetting parameter (variables=<LIST>):
                //if (serviceArg.equals("variables")) {
                //    if ("subset".equals(dataService.getServiceName())) {
                //        conversionParam = "subset";
                //        conversionParamValue = serviceArgValue; 
                //        return true; 
                //    }
                //} else {
                    if ("imageThumb".equals(serviceArg)) {
                        if ("true".equals(serviceArgValue)) {
                            this.conversionParam = serviceArg; 
                            this.conversionParamValue = "";
                        } else {
                            this.conversionParam = serviceArg;
                            this.conversionParamValue = serviceArgValue;
                        }
                        return true; 
                    }
                    String argValuePair = serviceArg + "=" + serviceArgValue; 
                    if (argValuePair.startsWith(dataService.getServiceArguments())) {
                        conversionParam = serviceArg; 
                        conversionParamValue = serviceArgValue; 
                        return true; 
                    }
                //}
            }
        }
        return false; 
    }
    
    public String getServiceFormatType (String serviceArg, String serviceArgValue) {
        if (downloadInfo == null || serviceArg == null) {
            return null;
        }
        
        List<OptionalAccessService> servicesAvailable = downloadInfo.getServicesAvailable();
        
        for (OptionalAccessService dataService : servicesAvailable) {
            if (dataService != null) {
                // Special case for the subsetting parameter (variables=<LIST>):
                if (serviceArg.equals("variables")) {
                    if ("subset".equals(dataService.getServiceName())) {
                        conversionParam = "subset";
                        conversionParamValue = serviceArgValue; 
                        return dataService.getMimeType(); 
                    }
                } else if (serviceArg.equals("imageThumb")) {
                    return "image/png";
                } else {
                    String argValuePair = serviceArg + "=" + serviceArgValue; 
                    if (argValuePair.equals(dataService.getServiceArguments())) {
                        conversionParam = serviceArg; 
                        conversionParamValue = serviceArgValue; 
                        return dataService.getMimeType(); 
                    }
                }
            }
        }
        return null; 
    }
}

package edu.harvard.iq.dataverse.locality;

import edu.harvard.iq.dataverse.util.SystemConfig;
import javax.json.JsonObject;

public class StorageSiteUtil {

    public static StorageSite parse(JsonObject jsonObject) {
        StorageSite storageSite = new StorageSite();
        storageSite.setHostname(getRequiredString(jsonObject, StorageSite.HOSTNAME));
        storageSite.setName(getRequiredString(jsonObject, StorageSite.NAME));
        try {
            storageSite.setPrimaryStorage(jsonObject.getBoolean(StorageSite.PRIMARY_STORAGE));
        } catch (Exception ex) {
            throw new IllegalArgumentException(StorageSite.PRIMARY_STORAGE + " must be true or false.");
        }
        storageSite.setTransferProtocols(parseTransferProtocolsString(jsonObject));
        return storageSite;
    }

    private static String parseTransferProtocolsString(JsonObject jsonObject) {
        String commaSeparatedInput = getRequiredString(jsonObject, StorageSite.TRANSFER_PROTOCOLS);
        String[] strings = commaSeparatedInput.split(",");
        for (String string : strings) {
            SystemConfig.TransferProtocols.fromString(string);
        }
        return commaSeparatedInput;
    }

    private static String getRequiredString(JsonObject jsonObject, String key) {
        try {
            String value = jsonObject.getString(key);
            return value;
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException("String " + key + " is required!");
        }
    }

}

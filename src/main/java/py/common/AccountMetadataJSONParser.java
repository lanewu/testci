package py.common;

import org.apache.log4j.Logger;

/**
 * A class to composite account id with account metadata json. The format of this composition is
 * "accountId:accountMetadataJSON". As we know parse json and encode json is a very high load job. Sometimes we just
 * need one element of the json, so we take the key element out of json, and composite them.(Comment by zjm)
 */
public class AccountMetadataJSONParser {
    private final Logger logger = Logger.getLogger(AccountMetadataJSONParser.class);
    private long accountId;
    private String accountMetadataJSON;

    public AccountMetadataJSONParser(String compositedAccountMetadataJSON) {
        int index = compositedAccountMetadataJSON.indexOf(':');
        if (index == -1) {
            logger.warn("can not parse the volumeMetadataJSON:" + compositedAccountMetadataJSON);
            return;
        }

        accountId = Long.valueOf(compositedAccountMetadataJSON.substring(0, index));
        accountMetadataJSON = compositedAccountMetadataJSON.substring(index + 1);
    }

    public AccountMetadataJSONParser(long accountId, String accountMetadataJSON) {
        this.accountId = accountId;
        this.accountMetadataJSON = accountMetadataJSON;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getAccountMetadataJSON() {
        return accountMetadataJSON;
    }

    public void setAccountMetadataJSON(String accountMetadataJSON) {
        this.accountMetadataJSON = accountMetadataJSON;
    }

    public String getCompositedAccountMetadataJSON() {
        return accountId + ":" + accountMetadataJSON;
    }
}

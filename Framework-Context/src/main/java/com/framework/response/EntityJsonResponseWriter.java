package com.framework.response;

import com.framework.context.ApplicationContext;
import com.framework.entity.pojo.Entity;
import com.framework.enumeration.CryptEnum;
import com.framework.enumeration.TerminalTypeEnum;
import com.framework.utils.JsonUtils;
import com.frameworkLog.factory.LogFactory;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

/**
 *
 * @author nelson
 */
public class EntityJsonResponseWriter<T extends Entity> extends ResponseWriter {

    private final Logger logger = LogFactory.getInstance().getLogger(EntityJsonResponseWriter.class);

    @Override
    public void toWriteJson(CryptEnum responseSecurityType, String[] returnParameters, TerminalTypeEnum terminalTypeEnum) throws Exception {
        ApplicationContext applicationContext = ApplicationContext.CTX;
        HttpServletResponse response = applicationContext.getHttpServletResponse();
        String jsonData = "{}";
        PrintWriter printWriter = response.getWriter();
        //验证返回字段是否存在
        boolean isSuccess = true;
        T entity = (T) applicationContext.getEnttiyData();
        if (entity != null) {
            Map<String, String> data = entity.toMap();
            for (String parameter : returnParameters) {
                if (!data.containsKey(parameter)) {
                    applicationContext.setEncryptCode(9006);
                    applicationContext.setMsg(parameter.concat("字段没有设置输出"));
                    isSuccess = false;
                    break;
                }
            }
            if (isSuccess) {
                jsonData = JsonUtils.mapToJson(data, returnParameters, this.filterHandler);
                logger.debug("jsonData=".concat(jsonData));
                if (responseSecurityType == CryptEnum.AES) {
                    //keyzhi从那里取
                    String key;
                    if (terminalTypeEnum == TerminalTypeEnum.MOBILE) {
                        String telephoneNum = applicationContext.getTelephoneNum();
                        key = applicationContext.getKey(telephoneNum);
                    } else {
                        String userUuid = applicationContext.getUserUuid();
                        key = applicationContext.getKey(userUuid);
                    }
                    jsonData = applicationContext.getCrypto(responseSecurityType).encrypt(jsonData, key);
                    jsonData = jsonData.replace("\n", "");
                    jsonData = jsonData.replace("\r", "");
                    jsonData = "\"" + jsonData + "\"";
                }
            }
        }
        int resultCode = applicationContext.getResultCode();
        String msg = applicationContext.getMsg();
        StringBuilder resultJson = new StringBuilder();
        String jsonCallback = applicationContext.getJsonCallback();
        String act = applicationContext.getAct();
        if (jsonCallback != null) {
            resultJson.append(jsonCallback).append("(").append("{ \"resultCode\":").append(resultCode).append(",\"encryptCode\":").append(responseSecurityType.toString()).append(",\"act\":\"").append(act).append("\",\"msg\":\"").append(msg).append("\",\"data\":").append(jsonData).append("}").append(")");
        } else {
            resultJson.append("{ \"resultCode\":").append(resultCode).append(",\"encryptCode\":").append(responseSecurityType.toString()).append(",\"act\":\"").append(act).append("\",\"msg\":\"").append(msg).append("\",\"data\":").append(jsonData).append("}");
        }
        logger.debug("resultJson=".concat(resultJson.toString()));
        printWriter.write(resultJson.toString());
        printWriter.flush();
        printWriter.close();
    }
}

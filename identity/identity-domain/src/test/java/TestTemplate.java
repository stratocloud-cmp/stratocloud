import com.stratocloud.exceptions.StratoException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTemplate {

    static String template = """
            <!DOCTYPE html>
            <html lang="en">
                <meta http-equiv="Content-Type" content="text/html" charset="UTF-8">
                <head>
                    <title>
                        工单审批通知
                    </title>
                </head>
                <body>
                    <p>
                        尊敬的用户(账号ID: ${receiverId}, 账号名: ${receiverName})<br>
                        您好, 您的工单已执行成功<br>
                        工单号: ${orderNo}<br>
                        #foreach ($orderItem in $orderItems)
                            #if ($orderItem.jobType=='BATCH_CREATE_RESOURCES')
                                资源列表<br>
                                <table style="border: 1">
                                    #foreach ($resourceName in $runtimeProperties.createdResourceNames)
                                    <tr>
                                        <td>
                                            $resourceName
                                        </td>
                                    </tr>
                                    #end
                                </table>
                            #end
                        #end
                        <!--suppress HtmlUnknownTarget -->
                        请前往<a href="${domainName}">${domainName}</a>查看
                    </p>
                </body>
            </html>
           """;

    public static void main(String[] args) {
        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put("receiverId", 1000L);
        inputParameters.put("receiverName", "测试用户");
        inputParameters.put("domainName", "http://127.0.0.1:8080");
        inputParameters.put("orderNo", "0001");
        inputParameters.put(
                "runtimeProperties",
                Map.of(
                        "createdResourceNames",
                        List.of("r1","r2")
                )
        );
        inputParameters.put("orderItems", List.of(
                Map.of(
                        "jobType",
                        "BATCH_CREATE_RESOURCES"
                )
        ));

        Velocity.init();
        VelocityContext velocityContext = new VelocityContext(inputParameters);

        StringWriter stringWriter = new StringWriter();

        boolean evaluated = Velocity.evaluate(
                velocityContext,
                stringWriter,
                "test",
                template
        );

        if(!evaluated)
            throw new StratoException("Velocity evaluation failed");

        System.out.println(stringWriter);
    }
}

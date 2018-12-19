package pro.taskana.camunda.camundasystemconnector.api;

import org.springframework.http.HttpStatus;

/**
 * encapsulate a response from camunda.
 * @author bbr
 *
 */
public class CamundaResponse {
    private HttpStatus statusCode;
    private Throwable  throwable;

    public CamundaResponse(HttpStatus statusCode, Throwable throwable) {
        this.statusCode = statusCode;
        this.throwable = throwable;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public Throwable getThrowable() {
        return throwable;
    }

}

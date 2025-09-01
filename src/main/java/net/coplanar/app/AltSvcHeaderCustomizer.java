/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

/**
 *
 * @author jjackson
 */
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.http.HttpFields.Mutable;

public class AltSvcHeaderCustomizer implements HttpConfiguration.Customizer {
    private final String headerValue;

    public AltSvcHeaderCustomizer(String headerValue) {
        this.headerValue = headerValue;
    }

    @Override
    public Request customize(Request request, Mutable responseHeaders) {
        //Response response = request.getResponse();
        responseHeaders.put("Alt-Svc", headerValue);
        return request;
    }
}

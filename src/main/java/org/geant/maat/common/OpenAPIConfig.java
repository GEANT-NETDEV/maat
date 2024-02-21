package org.geant.maat.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
@Configuration
public class OpenAPIConfig {

    @Value("${resource.protocol}")
    private String protocol;

    @Value("${resource.address}")
    private String address;

    @Value("${resource.port}")
    private String port;

    @Bean
    public OpenAPI myOpenAPI() {
        String devUrl = protocol+"://"+address+":"+port;
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        //devServer.setDescription("Server URL in Development environment");

        //Server prodServer = new Server();
        //prodServer.setUrl(prodUrl);
        //prodServer.setDescription("Server URL in Production environment");

        Info info = new Info()
                .title("Maat Management API")
                .version("1.0")
                .description("This API exposes endpoints to manage Maat.");


        return new OpenAPI().info(info).servers(List.of(devServer));
    }


}

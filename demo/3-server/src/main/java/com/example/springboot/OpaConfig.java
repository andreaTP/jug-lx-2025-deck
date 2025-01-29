package com.example.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.andreaTP.opa.chicory.Opa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpaConfig implements WebMvcConfigurer {

    private static class OpaInterceptor implements HandlerInterceptor {
        private Opa.OpaPolicy policy = Opa.loadPolicy(OpaInterceptor.class.getResourceAsStream("/policy.wasm"));
        private ObjectMapper mapper = new ObjectMapper();

        private static class Allow {
            public boolean allow;
        }
        private static class Authz {
            public Allow authz;
        }
        private static class Result {
            public Authz result;
        }

        private static class User {
            public String user;

            User(String name) {
                this.user = name;
            }
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (!request.getMethod().equals("GET")) {
                return true;
            }
            var user = request.getHeader("X-User");
            if (user == null) {
                return false;
            }
            var jsonResult = policy.evaluate(mapper.writeValueAsString(new User(user)));

            // example result:
            // [{"result":{"authz":{"allow":false}}}]
            Result[] results = mapper.readValue(jsonResult, Result[].class);
            System.out.println("User: " + user + " allowed: " + results[0].result.authz.allow);

            if (!results[0].result.authz.allow) {
                response.setStatus(401); // HTTP 401 Unauthorized
            }
            return results[0].result.authz.allow;
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new OpaInterceptor());
    }

}

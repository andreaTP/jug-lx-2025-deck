package com.example.springboot;

import com.example.springboot.json.OpaJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.styra.opa.wasm.OpaPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class OpaInterceptor implements HandlerInterceptor {
    private OpaPolicy policy = OpaPolicy.builder()
            .withPolicy(OpaInterceptor.class.getResourceAsStream("/policy.wasm"))
            .build();
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!request.getMethod().equals("GET")) {
            return true;
        }
        var user = request.getHeader("X-User");
        if (user == null) {
            return false;
        }
        var jsonResult = policy.evaluate(mapper.writeValueAsString(new OpaJson.User(user)));

        // example result:
        // [{"result":{"authz":{"allow":false}}}]
        OpaJson.Result[] results = mapper.readValue(jsonResult, OpaJson.Result[].class);
        System.out.println("User: " + user + " allowed: " + results[0].result.authz.allow);

        if (!results[0].result.authz.allow) {
            response.setStatus(401); // HTTP 401 Unauthorized
        }
        return results[0].result.authz.allow;
    }
}

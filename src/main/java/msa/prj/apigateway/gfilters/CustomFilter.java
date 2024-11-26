package msa.prj.apigateway.gfilters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CustomFilter implements GlobalFilter, Ordered {

    private static final String[] WHITELIST = {"/user/register", "/user/login", "/user/test"};

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("pre global filter order -1");


        ServerHttpRequest request = exchange.getRequest();
        String method = String.valueOf(request.getMethod());
        String requestParam = String.valueOf(request.getPath());


        // 요청 origin 을 api gateway 의 origin 으로 변경하여 cors 해결.
        request.mutate().header("Origin", "http://localhost:8080");

        HttpHeaders headers = request.getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);




        //
        if(isWhiteList(requestParam)){
            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    System.out.println("is whitelist. filter end.");
                }));
        }

        switch (requestParam.split("/")[1]){
            case "user" : {
                //jwt 관련 로직
                if(isAuthorized(authHeader, "ROLE_USER")){
                    return chain.filter(exchange)
                        .then(Mono.fromRunnable(() -> {
                        }));
                }else{
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied"));
                }
            }
            case "msa1" : {
                //jwt 관련 로직
                //jwt 관련 로직
                if(isAuthorized(authHeader, "ROLE_MSA")){
                    return chain.filter(exchange)
                            .then(Mono.fromRunnable(() -> {
                            }));
                }else{
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied"));
                }
            }
        }

        // 응답 처리
        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
            System.out.println("post global filter order -1");
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private List<String> getRoles(String token){
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey) // 토큰 생성 시 사용한 비밀키
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // roles 배열에 담긴 권한 정보를 사용하여 권한 검사 등을 수행합니다.
            return claims.get("roles", List.class);

        } catch (JwtException ex) {
            return null;
        }
    }

    private boolean isWhiteList(String path){
        for(int i = 0; i < WHITELIST.length; i++){
            if(path.equals(WHITELIST[i])){
                return true;
            }
        }
        return false;
    }

    private boolean isAuthorized(String authHeader, String role){
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            List<String> roles = getRoles(jwt);
            for (int i = 0; i < roles.size(); i++){
                return roles.get(i).equals(role);
            }
        }
        return false;
    }
}

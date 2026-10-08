package com.eone.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded per-instance throttle. Does not trust client-supplied forwarding headers. */
@Component @Order(Ordered.HIGHEST_PRECEDENCE+10)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long until,int count){}
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String path=req.getRequestURI();
        if(!"POST".equals(req.getMethod()) || !(path.equals("/api/v1/auth/login") || path.startsWith("/api/v1/auth/password/"))){chain.doFilter(req,res);return;}
        boolean recovery=path.endsWith("/request");int limit=recovery?6:30;long duration=recovery?3600000:60000;long now=System.currentTimeMillis();
        String key=req.getRemoteAddr()+":"+(recovery?"request":"auth");
        if(windows.size()>=10000){windows.entrySet().removeIf(e->e.getValue().until<=now);}
        if(windows.size()>=10000 && !windows.containsKey(key)){reject(res,60);return;}
        Window window=windows.compute(key,(k,old)->old==null || old.until<=now?new Window(now+duration,1):new Window(old.until,Math.min(limit+1,old.count+1)));
        if(window.count>limit){reject(res,Math.max(1,(window.until-now+999)/1000));return;}
        chain.doFilter(req,res);
    }
    private void reject(HttpServletResponse response,long seconds)throws IOException{
        response.setStatus(429);response.setHeader("Retry-After",Long.toString(seconds));response.setContentType("application/json");
        response.getWriter().write("{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Too many attempts. Please try again later.\"}");
    }
}

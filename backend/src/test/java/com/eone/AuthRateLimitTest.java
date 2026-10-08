package com.eone;
import com.eone.security.AuthRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;
class AuthRateLimitTest {
    @Test void repeatedRequestsAreLimitedAndDifferentClientsRemainIndependent() throws Exception {
        var filter=new AuthRateLimitFilter();
        for(int i=0;i<7;i++){
            var request=new MockHttpServletRequest("POST","/api/v1/auth/password/request");request.setRemoteAddr("192.0.2.1");
            var response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(i<6?200:429);
            if(i==6)assertThat(response.getHeader("Retry-After")).isNotBlank();
        }
        var other=new MockHttpServletRequest("POST","/api/v1/auth/password/request");other.setRemoteAddr("192.0.2.2");
        var response=new MockHttpServletResponse();filter.doFilter(other,response,new MockFilterChain());assertThat(response.getStatus()).isEqualTo(200);
    }
}

package ltd.wrb.payment.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ltd.wrb.payment.util.IpLimiter;
import ltd.wrb.payment.util.RedisUtils;

@Component
public class IpLimiterInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisUtils cache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // is handler has annotation?
        if (handler instanceof HandlerMethod handlerMethod) {
            if (handlerMethod.getMethod().getAnnotation(IpLimiter.class) != null) {
                String ip = getIpAddress(request);
                String key = handlerMethod.getMethod().getName() + "_locker_" + ip;

                // 获取IpLimiter注解的参数
                IpLimiter ipLimiter = handlerMethod.getMethod().getAnnotation(IpLimiter.class);

                if (!cache.lock(key, 1)) {
                    response.setStatus(429);
                    return false;
                }
                // do something
                Object strCount = cache.get(key);
                int count = strCount == null ? ipLimiter.limit()  : Integer.parseInt(strCount.toString());
                count++;
                if (count > ipLimiter.limit()) {
                    response.setStatus(429);
                    return false;
                } else {
                    cache.set(key, count + "", ipLimiter.time());
                }
                // release lock
                cache.unlock(key);
            }
        }
        return true;
    }

    public String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }
}

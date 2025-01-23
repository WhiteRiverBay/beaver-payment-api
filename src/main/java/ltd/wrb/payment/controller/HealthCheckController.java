package ltd.wrb.payment.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/")
@RestController
public class HealthCheckController {
    
    @RequestMapping("/_health/check")
    public String check() {
        return "ok";
    }

    @RequestMapping("/")
    public String ready() {
        return "ok";
    }
}

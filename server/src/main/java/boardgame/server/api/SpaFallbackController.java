package boardgame.server.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端只有两个入口：大厅与对局页。其余路径交给静态资源（/api 由 Spring 自己处理）。
 */
@Controller
public class SpaFallbackController {

    @GetMapping({"/", "/g/{gameId}"})
    public String index() {
        return "forward:/index.html";
    }
}

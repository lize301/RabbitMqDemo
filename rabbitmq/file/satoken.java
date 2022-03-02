package cn.hfbin.common.redis.util;

/**
 * @Auther meng
 * @Date 2022-03-02 19:19
 */
public class satoken {

    在子服务引入的依赖为：
    <!-- Sa-Token 权限认证, 在线文档：http://sa-token.dev33.cn/ -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.29.0</version>
</dependency>
<!-- Sa-Token 整合 Redis (使用jackson序列化方式) -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>1.29.0</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>





    在子服务添加过滤器校验参数
    /**
     * Sa-Token 权限认证 配置类
     */
    @Configuration
    public class SaTokenConfigure implements WebMvcConfigurer {
        // 注册 Sa-Token 全局过滤器
        @Bean
        public SaServletFilter getSaServletFilter() {
            return new SaServletFilter()
                    .addInclude("/**")
                    .addExclude("/favicon.ico")
                    .setAuth(obj -> {
                        // 校验 Id-Token 身份凭证     —— 以下两句代码可简化为：SaIdUtil.checkCurrentRequestToken();
                        String token = SaHolder.getRequest().getHeader(SaIdUtil.ID_TOKEN);
                        SaIdUtil.checkToken(token);
                    })
                    .setError(e -> {
                        return SaResult.error(e.getMessage());
                    })
                    ;
        }
    }

    首先在调用方添加 FeignInterceptor

    /**
     * feign拦截器, 在feign请求发出之前，加入一些操作
     */
    @Component
    public class FeignInterceptor implements RequestInterceptor {
        // 为 Feign 的 RCP调用 添加请求头Id-Token
        @Override
        public void apply(RequestTemplate requestTemplate) {
            requestTemplate.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
        }
    }
、在调用接口里使用此 Interceptor
    /**
     * 服务调用
     */
    @FeignClient(
            name = "sp-home",                 // 服务名称
            configuration = FeignInterceptor.class,        // 请求拦截器 （关键代码）
            fallbackFactory = SpCfgInterfaceFallback.class    // 服务降级处理
    )
    public interface SpCfgInterface {

        // 获取server端指定配置信息
        @RequestMapping("/SpConfig/getConfig")
        public String getConfig(@RequestParam("key")String key);

    }
    首先我们预览一下此模块的相关API：
            // 获取当前Id-Token
            SaIdUtil.getToken();

// 判断一个Id-Token是否有效
SaIdUtil.isValid(token);

// 校验一个Id-Token是否有效 (如果无效则抛出异常)
SaIdUtil.checkToken(token);

// 校验当前Request提供的Id-Token是否有效 (如果无效则抛出异常)
SaIdUtil.checkCurrentRequestToken();

// 刷新一次Id-Token (注意集群环境中不要多个服务重复调用)
SaIdUtil.refreshToken();

// 在 Request 上储存 Id-Token 时建议使用的key
    SaIdUtil.ID_TOKEN;

}

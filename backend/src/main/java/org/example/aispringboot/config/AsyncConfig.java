package org.example.aispringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置
 * 用于安慰邮件等耗时操作用户无感知地异步执行。
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

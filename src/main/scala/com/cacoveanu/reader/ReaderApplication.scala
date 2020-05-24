package com.cacoveanu.reader

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Description

@SpringBootApplication
@EnableCaching
class ReaderApplication {

  /*import org.springframework.context.annotation.Bean
  import org.thymeleaf.spring5.SpringTemplateEngine
  import org.thymeleaf.templateresolver.ServletContextTemplateResolver

  @Bean
  @Description("Thymeleaf Template Resolver") def templateResolver: ServletContextTemplateResolver = {
    val templateResolver = new ServletContextTemplateResolver()
    templateResolver.setPrefix("/WEB-INF/views/")
    templateResolver.setSuffix(".html")
    templateResolver.setTemplateMode("HTML5")
    templateResolver
  }

  @Bean
  @Description("Thymeleaf Template Engine") def templateEngine: SpringTemplateEngine = {
    val templateEngine = new SpringTemplateEngine
    templateEngine.setTemplateResolver(templateResolver)
    templateEngine.setTemplateEngineMessageSource(messageSource)
    templateEngine
  }*/
}

object ReaderApplication extends App {
  SpringApplication.run(classOf[ReaderApplication])
}
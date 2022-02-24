package com.cacoveanu.reader

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.{EnableWebSecurity, WebSecurityConfigurerAdapter}
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.filter.CommonsRequestLoggingFilter

@SpringBootApplication
@EnableCaching
@EnableScheduling
class ReaderApplication {

}

object ReaderApplication extends App {
  SpringApplication.run(classOf[ReaderApplication])
}

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.{ViewControllerRegistry, WebMvcConfigurer}

@Configuration
class MvcConfig extends WebMvcConfigurer {
  override def addViewControllers(registry: ViewControllerRegistry): Unit = {
    registry.addRedirectViewController("/logout", "/login")
  }
}

/*@Configuration
class RequestLoggingFilterConfig {

  @Bean
  def logFilter(): CommonsRequestLoggingFilter = {
    val filter = new CommonsRequestLoggingFilter()
    filter.setIncludeQueryString(true)
    filter.setIncludePayload(true)
    filter.setMaxPayloadLength(10000)
    filter.setIncludeHeaders(false)
    filter.setAfterMessagePrefix("REQUEST DATA : ")
    filter
  }
}*/

@Configuration
@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  var userService: UserDetailsService = _

  override def configure(http: HttpSecurity): Unit = {
    http
      .csrf().disable().cors().and()
      .authorizeRequests()
      .antMatchers(
        "/book.css",
        "/book.js",
        "/bookNode.js",
        "/comic.css",
        "/comic.js",
        "/fonts.css",
        "/form.css",
        "/gestures.js",
        "/library.css",
        "/library.js",
        "/login.css",
        "/serviceworker.js",
        "/settings.js",
        "/tools.css",
        "/util.js",
        "/Merriweather/*",
        "/Roboto/*",
        "/manifest.json",
        "/history.js",
        "/more.js",
        "/logo.svg",
        "/logo.png*",
        "/help.css",
        "/help.js"
      ).permitAll()
      .antMatchers(
        "/users",
        "/addUser",
        "/deleteUser",
        "/exportProgress",
        "/import",
        "/exportUsers"
      ).hasRole("ADMIN")
      .anyRequest().authenticated()

      .and()
      .rememberMe()
      .key("uniqueAndSecret")
      .tokenValiditySeconds(60*60*24*120) // 120 days
      .userDetailsService(userService)

      .and()
      .formLogin()
      .loginPage("/login")
      .failureUrl("/login?error")
      .successForwardUrl("/")
      .defaultSuccessUrl("/")
      .permitAll()

      .and()
      .logout()
      .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
      .logoutSuccessUrl("/login?logout")
      .deleteCookies("JSESSIONID")
      .permitAll()
  }

  @Bean def passwordEncoder = new BCryptPasswordEncoder

}
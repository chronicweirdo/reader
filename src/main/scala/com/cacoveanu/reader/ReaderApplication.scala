package com.cacoveanu.reader

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.{Bean, Configuration, Description}
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.{EnableWebSecurity, WebSecurityConfigurerAdapter}
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@SpringBootApplication
@EnableCaching
class ReaderApplication {

}

object ReaderApplication extends App {
  SpringApplication.run(classOf[ReaderApplication])
}

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class MvcConfig extends WebMvcConfigurer {
  override def addViewControllers(registry: ViewControllerRegistry): Unit = {
    //registry.addViewController("/home").setViewName("home")
    //registry.addViewController("/").setViewName("collection")
    //registry.addViewController("/hello").setViewName("hello")
    registry.addViewController("/login").setViewName("login")
    registry.addRedirectViewController("/logout", "/login")
  }
}

@Configuration
@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  override def configure(http: HttpSecurity): Unit = {
    http
      .authorizeRequests()
      .anyRequest().fullyAuthenticated()

      .and()
      .formLogin()
      .loginPage("/login")
      .failureUrl("/login?error")
      //.successForwardUrl("/collection")
      .permitAll()

      .and()
      .logout()
      .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
      .logoutSuccessUrl("/login?logout")
      .deleteCookies("JSESSIONID")
      .permitAll();
  }

  @Bean def passwordEncoder = new BCryptPasswordEncoder

  /*@Bean
  override def userDetailsService(): UserDetailsService = {
    val user = User.withDefaultPasswordEncoder
      .username("user")
      .password("password")
      .roles("USER")
      .build

    new InMemoryUserDetailsManager(user)
  }*/
}